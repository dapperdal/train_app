package com.darren.trains.data.model

/**
 * Represents an active journey the user is currently on
 */
data class ActiveJourney(
    val serviceId: String,
    val trainDeparture: TrainDeparture,
    val callingPoints: List<CallingPoint>,
    val boardedAt: Long, // timestamp when user clicked "I'm Onboard"
    val originCrs: String,
    val originName: String,
    val destinationCrs: String,
    val destinationName: String
)

/**
 * Calculated progress through the journey
 */
data class JourneyProgress(
    val currentStopIndex: Int,
    val totalStops: Int,
    val stopsRemaining: Int,
    val minutesToArrival: Int?,
    val estimatedArrivalTime: String?,
    val scheduledArrivalTime: String?,
    val delayMinutes: Int,
    val isDelayed: Boolean,
    val nextStopName: String?,
    val previousStopName: String?,
    val hasArrived: Boolean
)

/**
 * User preference for arrival alerts
 */
enum class AlertPreference {
    AUDIO_IF_BLUETOOTH,  // Play TTS audio if Bluetooth headphones connected, otherwise vibrate
    SILENT_VIBRATE       // Always vibrate only
}

/**
 * User settings for journey alerts
 */
data class JourneyAlertSettings(
    val twoMinuteAlertEnabled: Boolean = true,
    val alertPreference: AlertPreference = AlertPreference.AUDIO_IF_BLUETOOTH
)
