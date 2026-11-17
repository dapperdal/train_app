package com.darren.trains.data.repository

import androidx.core.text.HtmlCompat
import com.darren.trains.data.api.HuxleyApi
import com.darren.trains.data.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainRepository @Inject constructor(
    private val api: HuxleyApi
) {
    private data class CachedBoard(val data: TrainData, val cachedAt: Long)

    private val boardCache = ConcurrentHashMap<String, CachedBoard>()

    /**
     * Get departures for a specific route with advanced filtering
     * @param fromCrs Origin station code
     * @param toCrs Destination station code
     * @param filterLakesideLoop If true, filters out trains passing through Lakeside stations
     * @param preferExpressTrains If true, prioritize trains with fewer stops
     * @param preferredOperator If set, prioritize trains from this operator
     * @param maxChangeTime If set, filter out trains with long connection times
     */
    suspend fun getDepartures(
        fromCrs: String,
        toCrs: String,
        filterLakesideLoop: Boolean = true,
        preferExpressTrains: Boolean = false,
        preferredOperator: String? = null,
        maxChangeTime: Int? = null,
        timeOffsetMinutes: Int = 0
    ): Result<TrainData> = withContext(Dispatchers.IO) {
        val cacheKey = "$fromCrs:$toCrs:$filterLakesideLoop:$preferExpressTrains:$preferredOperator:$maxChangeTime:$timeOffsetMinutes"
        try {
            val (response, feedQuality) = fetchBoardWithFallback(fromCrs, toCrs, timeOffsetMinutes)
            val shouldFilterLakeside = filterLakesideLoop &&
                fromCrs !in LAKESIDE_BRANCH_STATIONS &&
                toCrs !in LAKESIDE_BRANCH_STATIONS

            var departures = response.trainServices
                ?.filter { service ->
                    if (shouldFilterLakeside && feedQuality != FeedQuality.REDUCED) {
                        !hasIntermediateStation(service, "GRY", "TIL")
                    }
                    else true
                }
                ?.mapNotNull { service -> mapToTrainDeparture(service, toCrs) }
                ?: emptyList()

            // Apply smart filtering
            if (preferExpressTrains) {
                // Sort by number of calling points (fewer = faster)
                departures = departures.sortedBy { it.callingPoints.size }
            }

            if (preferredOperator != null) {
                // This would require operator info from the API response
                // For now, we just sort so operator matches appear first
                // (Actual filtering would need operator field in TrainDeparture)
            }

            if (maxChangeTime != null) {
                // Filter based on journey time (e.g., no journeys over 90 minutes for connections)
                departures = departures.filter { train ->
                    train.journeyTimeMinutes == null || train.journeyTimeMinutes <= maxChangeTime * 6
                }
            }

            val disruptions = response.nrccMessages
                ?.mapNotNull { mapToDisruption(it) }
                ?: emptyList()

            val data = TrainData(
                    departures = departures,
                    disruptions = disruptions,
                    fromStation = response.locationName ?: fromCrs,
                    toStation = response.filterLocationName ?: toCrs,
                    generatedAt = response.generatedAt,
                    feedQuality = feedQuality,
                    retrievedAt = System.currentTimeMillis()
                )
            boardCache[cacheKey] = CachedBoard(data, data.retrievedAt)
            Result.success(data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = boardCache[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.cachedAt <= CACHE_TTL_MS) {
                Result.success(
                    cached.data.copy(
                        feedQuality = FeedQuality.STALE,
                        retrievedAt = cached.cachedAt
                    )
                )
            } else {
                Result.failure(TrainFeedException("Live c2c information is temporarily unavailable", e))
            }
        }
    }

    private suspend fun fetchBoardWithFallback(
        fromCrs: String,
        toCrs: String,
        timeOffsetMinutes: Int
    ): Pair<DeparturesResponse, FeedQuality> {
        var firstFailure: Exception? = null

        repeat(EXPANDED_ATTEMPTS) { attempt ->
            try {
                val response = api.getDepartures(
                    fromCrs = fromCrs,
                    toCrs = toCrs,
                    numRows = 15,
                    expand = true,
                    timeOffset = timeOffsetMinutes.takeIf { it > 0 },
                    timeWindow = BROWSE_WINDOW_MINUTES.takeIf { timeOffsetMinutes > 0 }
                )
                return response to if (timeOffsetMinutes > 0) FeedQuality.LOOKAHEAD else FeedQuality.LIVE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!e.isRetryableFeedFailure()) throw e
                firstFailure = e
                if (attempt < EXPANDED_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
            }
        }

        return try {
            val response = api.getDepartures(
                fromCrs = fromCrs,
                toCrs = toCrs,
                numRows = 15,
                expand = false,
                timeOffset = timeOffsetMinutes.takeIf { it > 0 },
                timeWindow = BROWSE_WINDOW_MINUTES.takeIf { timeOffsetMinutes > 0 }
            )
            response to FeedQuality.REDUCED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            firstFailure?.let(e::addSuppressed)
            throw e
        }
    }

    private fun Exception.isRetryableFeedFailure(): Boolean =
        this is IOException || (this is HttpException && (code() == 429 || code() in 500..599))

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
        val text = message.xhtmlMessage?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        }
            ?.removeNationalRailPageInstructions()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val severity = when (message.severity) {
            0, 1 -> DisruptionSeverity.MINOR
            2 -> DisruptionSeverity.MAJOR
            else -> DisruptionSeverity.SEVERE
        }

        return Disruption(message = text, severity = severity)
    }

    /**
     * NRCC messages are written for the National Rail website and can contain
     * directions to its "Status and Disruptions" page. That page does not exist
     * in this app, so keep the useful service update and remove the misleading
     * website navigation sentence.
     */
    private fun String.removeNationalRailPageInstructions(): String =
        replace(
            Regex(
                """(?i)\s*More details and the full impact to your journey can be found in (?:the )?Status (?:and|&) Disruptions(?: page)?\.?"""
            ),
            ""
        )
            .replace(
                Regex(
                    """(?i)\s*(?:For )?more (?:details|information),? please (?:visit|see) (?:the )?Status (?:and|&) Disruptions(?: page)?\.?"""
                ),
                ""
            )
            .trim()
}

data class TrainData(
    val departures: List<TrainDeparture>,
    val disruptions: List<Disruption>,
    val fromStation: String,
    val toStation: String,
    val generatedAt: String?,
    val feedQuality: FeedQuality = FeedQuality.LIVE,
    val retrievedAt: Long = System.currentTimeMillis()
)

enum class FeedQuality {
    LIVE,
    LOOKAHEAD,
    REDUCED,
    STALE
}

class TrainFeedException(message: String, cause: Throwable) : IOException(message, cause)

private const val EXPANDED_ATTEMPTS = 2
private const val RETRY_DELAY_MS = 350L
private const val CACHE_TTL_MS = 10 * 60 * 1000L
private const val BROWSE_WINDOW_MINUTES = 120
private val LAKESIDE_BRANCH_STATIONS = setOf("DDK", "RNM", "PFL", "CFH", "GRY", "OCK", "TIL", "ETL", "SOH")
