package com.darren.trains.data.repository

import com.darren.trains.data.api.HuxleyApi
import com.darren.trains.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainRepository @Inject constructor(
    private val api: HuxleyApi
) {
    suspend fun getDepartures(direction: TravelDirection): Result<TrainData> = withContext(Dispatchers.IO) {
        try {
            val (fromCrs, toCrs) = when (direction) {
                TravelDirection.TO_FENCHURCH_STREET -> Stations.LEIGH_ON_SEA_CRS to Stations.FENCHURCH_STREET_CRS
                TravelDirection.TO_LEIGH_ON_SEA -> Stations.FENCHURCH_STREET_CRS to Stations.LEIGH_ON_SEA_CRS
            }

            val response = api.getDepartures(fromCrs, toCrs, numRows = 15, expand = true)

            val departures = response.trainServices
                ?.filter { service -> !isGraysService(service) }
                ?.filter { service -> service.isCancelled != true }
                ?.mapNotNull { service -> mapToTrainDeparture(service, direction) }
                ?: emptyList()

            val disruptions = response.nrccMessages
                ?.mapNotNull { mapToDisruption(it) }
                ?: emptyList()

            Result.success(
                TrainData(
                    departures = departures,
                    disruptions = disruptions,
                    fromStation = response.locationName ?: fromCrs,
                    toStation = response.filterLocationName ?: toCrs,
                    generatedAt = response.generatedAt
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isGraysService(service: TrainService): Boolean {
        // Check if any calling point is Grays (GRY)
        val callingPoints = service.subsequentCallingPoints
            ?.flatMap { it.callingPoints ?: emptyList() }
            ?: emptyList()

        return callingPoints.any { it.crs == Stations.GRAYS_CRS }
    }

    private fun mapToTrainDeparture(service: TrainService, direction: TravelDirection): TrainDeparture? {
        val serviceId = service.serviceId ?: return null
        val departureTime = service.scheduledDeparture ?: return null
        val estimatedTime = service.estimatedDeparture ?: "Unknown"

        val status = when {
            service.isCancelled == true -> TrainStatus.CANCELLED
            estimatedTime.equals("On time", ignoreCase = true) -> TrainStatus.ON_TIME
            estimatedTime.equals("Delayed", ignoreCase = true) -> TrainStatus.DELAYED
            estimatedTime.matches(Regex("\\d{2}:\\d{2}")) -> {
                if (estimatedTime != departureTime) TrainStatus.DELAYED else TrainStatus.ON_TIME
            }
            else -> TrainStatus.ON_TIME
        }

        val delayMinutes = calculateDelay(departureTime, estimatedTime)

        val destination = when (direction) {
            TravelDirection.TO_FENCHURCH_STREET -> Stations.FENCHURCH_STREET_NAME
            TravelDirection.TO_LEIGH_ON_SEA -> Stations.LEIGH_ON_SEA_NAME
        }

        // Calculate journey time from calling points
        val journeyTime = calculateJourneyTime(service, direction)

        // Extract calling points for journey tracking
        val callingPoints = service.subsequentCallingPoints
            ?.flatMap { it.callingPoints ?: emptyList() }
            ?: emptyList()

        return TrainDeparture(
            serviceId = serviceId,
            departureTime = departureTime,
            estimatedTime = estimatedTime,
            platform = service.platform ?: "-",
            destination = destination,
            journeyTimeMinutes = journeyTime,
            status = status,
            delayMinutes = delayMinutes,
            isCancelled = service.isCancelled == true,
            callingPoints = callingPoints
        )
    }

    private fun calculateDelay(scheduled: String, estimated: String): Int {
        if (estimated.equals("On time", ignoreCase = true) || estimated.equals("Delayed", ignoreCase = true)) {
            return 0
        }

        return try {
            val scheduledMinutes = timeToMinutes(scheduled)
            val estimatedMinutes = timeToMinutes(estimated)
            maxOf(0, estimatedMinutes - scheduledMinutes)
        } catch (e: Exception) {
            0
        }
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) return 0
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun calculateJourneyTime(service: TrainService, direction: TravelDirection): Int? {
        val targetCrs = when (direction) {
            TravelDirection.TO_FENCHURCH_STREET -> Stations.FENCHURCH_STREET_CRS
            TravelDirection.TO_LEIGH_ON_SEA -> Stations.LEIGH_ON_SEA_CRS
        }

        val departureTime = service.scheduledDeparture ?: return null

        val arrivalPoint = service.subsequentCallingPoints
            ?.flatMap { it.callingPoints ?: emptyList() }
            ?.find { it.crs == targetCrs }

        val arrivalTime = arrivalPoint?.scheduledTime ?: return null

        return try {
            val depMinutes = timeToMinutes(departureTime)
            val arrMinutes = timeToMinutes(arrivalTime)
            if (arrMinutes >= depMinutes) {
                arrMinutes - depMinutes
            } else {
                // Handle overnight trains (not relevant for C2C but safe)
                (24 * 60 - depMinutes) + arrMinutes
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapToDisruption(message: NrccMessage): Disruption? {
        val text = message.xhtmlMessage?.replace(Regex("<[^>]*>"), "")?.trim() ?: return null

        val severity = when (message.severity) {
            0, 1 -> DisruptionSeverity.MINOR
            2 -> DisruptionSeverity.MAJOR
            else -> DisruptionSeverity.SEVERE
        }

        return Disruption(message = text, severity = severity)
    }
}

data class TrainData(
    val departures: List<TrainDeparture>,
    val disruptions: List<Disruption>,
    val fromStation: String,
    val toStation: String,
    val generatedAt: String?
)
