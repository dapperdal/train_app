package com.darren.trains

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DarrensTrainsApp : Application() {

    companion object {
        const val JOURNEY_TRACKING_CHANNEL_ID = "journey_tracking"
        const val ARRIVAL_ALERT_CHANNEL_ID = "arrival_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Journey tracking channel - low importance for ongoing notification
            val journeyChannel = NotificationChannel(
                JOURNEY_TRACKING_CHANNEL_ID,
                "Journey Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your current journey progress"
                setShowBadge(false)
            }

            // Arrival alerts channel - high importance for the 2-minute warning
            val alertChannel = NotificationChannel(
                ARRIVAL_ALERT_CHANNEL_ID,
                "Arrival Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "2-minute arrival warnings"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }

            notificationManager.createNotificationChannel(journeyChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
