package com.darren.trains.data.repository

import com.darren.trains.data.model.ActiveJourney
import com.darren.trains.data.model.CallingPoint
import com.darren.trains.data.model.JourneyProgress
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor() {

    /**
     * Calculate journey progress based on calling points and current time
     */
    fun calculateProgress(
        journey: ActiveJourney,
        currentTimeMinutes: Int = getCurrentTimeMinutes()
    ): JourneyProgress {
        val callingPoints = journey.callingPoints
        val destinationCrs = journey.destinationCrs

        // Find destination index
        val destinationIndex = callingPoints.indexOfFirst { it.crs == destinationCrs }
        if (destinationIndex == -1) {
            return createDefaultProgress(callingPoints.size)
        }

        val totalStops = destinationIndex + 1 // Include destination

        // Determine current position based on actual times and estimated times
        val currentStopIndex = findCurrentStopIndex(callingPoints, destinationIndex, currentTimeMinutes)
        val stopsRemaining = maxOf(0, destinationIndex - currentStopIndex)

        // Get arrival time info
        val destinationPoint = callingPoints[destinationIndex]
        val scheduledArrival = destinationPoint.scheduledTime
        val estimatedArrival = destinationPoint.estimatedTime ?: destinationPoint.actualTime ?: scheduledArrival

        // Calculate minutes to arrival
        val minutesToArrival = calculateMinutesToArrival(estimatedArrival, currentTimeMinutes)

        // Calculate delay
        val delayMinutes = calculateDelay(scheduledArrival, estimatedArrival)

        // Get next stop name
        val nextStopIndex = minOf(currentStopIndex + 1, destinationIndex)
        val nextStopName = callingPoints.getOrNull(nextStopIndex)?.locationName

        // Get previous stop name
        val previousStopName = if (currentStopIndex > 0) {
            callingPoints.getOrNull(currentStopIndex - 1)?.locationName
        } else {
            journey.originName
        }

        // Check if arrived
        val hasArrived = destinationPoint.actualTime != null ||
            (minutesToArrival != null && minutesToArrival <= 0)

        return JourneyProgress(
            currentStopIndex = currentStopIndex,
            totalStops = totalStops,
            stopsRemaining = stopsRemaining,
            minutesToArrival = minutesToArrival,
            estimatedArrivalTime = estimatedArrival,
            scheduledArrivalTime = scheduledArrival,
            delayMinutes = delayMinutes,
            isDelayed = delayMinutes > 0,
            nextStopName = nextStopName,
            previousStopName = previousStopName,
            hasArrived = hasArrived
        )
    }

    /**
     * Find the current stop index based on actual departure times
     */
    private fun findCurrentStopIndex(
        callingPoints: List<CallingPoint>,
        destinationIndex: Int,
        currentTimeMinutes: Int
    ): Int {
        // Go through calling points and find the last one we've passed
        for (i in (0..destinationIndex).reversed()) {
            val point = callingPoints[i]

            // If there's an actual time, we've passed this station
            if (point.actualTime != null) {
                return i + 1 // We're past this station, moving to next
            }

            // Check if estimated/scheduled time is in the past
            val pointTime = point.estimatedTime ?: point.scheduledTime
            if (pointTime != null) {
                val pointMinutes = timeToMinutes(pointTime)
                if (pointMinutes != null && pointMinutes < currentTimeMinutes) {
                    return i + 1
                }
            }
        }

        return 0 // Still at/before first calling point
    }

    private fun calculateMinutesToArrival(arrivalTime: String?, currentTimeMinutes: Int): Int? {
        if (arrivalTime == null) return null
        if (arrivalTime.equals("On time", ignoreCase = true) ||
            arrivalTime.equals("Delayed", ignoreCase = true)) {
            return null
        }

        val arrivalMinutes = timeToMinutes(arrivalTime) ?: return null
        return maxOf(0, arrivalMinutes - currentTimeMinutes)
    }

    private fun calculateDelay(scheduled: String?, estimated: String?): Int {
        if (scheduled == null || estimated == null) return 0
        if (estimated.equals("On time", ignoreCase = true)) return 0
        if (estimated.equals("Delayed", ignoreCase = true)) return 0 // Unknown delay

        val scheduledMinutes = timeToMinutes(scheduled) ?: return 0
        val estimatedMinutes = timeToMinutes(estimated) ?: return 0

        return maxOf(0, estimatedMinutes - scheduledMinutes)
    }

    private fun timeToMinutes(time: String): Int? {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return null
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentTimeMinutes(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    private fun createDefaultProgress(totalStops: Int): JourneyProgress {
        return JourneyProgress(
            currentStopIndex = 0,
            totalStops = totalStops,
            stopsRemaining = totalStops,
            minutesToArrival = null,
            estimatedArrivalTime = null,
            scheduledArrivalTime = null,
            delayMinutes = 0,
            isDelayed = false,
            nextStopName = null,
            previousStopName = null,
            hasArrived = false
        )
    }
}
