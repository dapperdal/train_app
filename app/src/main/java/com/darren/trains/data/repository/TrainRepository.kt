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
    /**
     * Get departures for a specific route
     * @param fromCrs Origin station code
     * @param toCrs Destination station code
     * @param filterLakesideLoop If true, filters out trains passing through Lakeside stations
     */
    suspend fun getDepartures(
        fromCrs: String,
        toCrs: String,
        filterLakesideLoop: Boolean = true
    ): Result<TrainData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDepartures(fromCrs, toCrs, numRows = 15, expand = true)

            val departures = response.trainServices
                ?.filter { service -> service.isCancelled != true }
                ?.filter { service -> !hasIntermediateStation(service, "GRY") }  // Always filter Grays
                ?.filter { service ->
                    if (filterLakesideLoop) !hasIntermediateStation(service, "GRY", "TIL")
                    else true
                }
                ?.mapNotNull { service -> mapToTrainDeparture(service, toCrs) }
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

    /**
     * Legacy method for backward compatibility
     */
    suspend fun getDepartures(direction: TravelDirection): Result<TrainData> = withContext(Dispatchers.IO) {
        val (fromCrs, toCrs) = when (direction) {
            TravelDirection.TO_FENCHURCH_STREET -> Stations.LEIGH_ON_SEA_CRS to Stations.FENCHURCH_STREET_CRS
            TravelDirection.TO_LEIGH_ON_SEA -> Stations.FENCHURCH_STREET_CRS to Stations.LEIGH_ON_SEA_CRS
        }
        return@withContext getDepartures(fromCrs, toCrs, filterLakesideLoop = true)
    }

    /**
     * Check if service passes through specified intermediate stations
     * @param service The train service
     * @param stationCodes CRS codes to check for
     * @return true if service passes through any of the specified stations
     */
    private fun hasIntermediateStation(service: TrainService, vararg stationCodes: String): Boolean {
        val callingPoints = service.subsequentCallingPoints
            ?.flatMap { it.callingPoints ?: emptyList() }
            ?: emptyList()

        return callingPoints.any { point -> stationCodes.contains(point.crs) }
    }

    /**
     * Map API TrainService to UI TrainDeparture model
     * @param service The train service from API
     * @param destinationCrs The destination station code
     */
    private fun mapToTrainDeparture(service: TrainService, destinationCrs: String): TrainDeparture? {
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

        // Get destination name from station database
        val destination = Stations.getStationName(destinationCrs)

        // Calculate journey time from calling points
        val journeyTime = calculateJourneyTime(service, destinationCrs)

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

    /**
     * Calculate journey time from departure to destination
     * @param service The train service
     * @param destinationCrs The destination station code
     * @return Journey time in minutes, or null if cannot be calculated
     */
    private fun calculateJourneyTime(service: TrainService, destinationCrs: String): Int? {
        val departureTime = service.scheduledDeparture ?: return null

        val arrivalPoint = service.subsequentCallingPoints
            ?.flatMap { it.callingPoints ?: emptyList() }
            ?.find { it.crs == destinationCrs }

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
