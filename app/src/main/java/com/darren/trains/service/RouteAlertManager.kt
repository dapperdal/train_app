package com.darren.trains.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.darren.trains.DarrensTrainsApp
import com.darren.trains.MainActivity
import com.darren.trains.R
import com.darren.trains.data.model.RoutePreference
import com.darren.trains.data.model.TrainStatus
import com.darren.trains.data.repository.TrainData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteAlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences = context.getSharedPreferences("route_alert_delivery", Context.MODE_PRIVATE)

    fun notifyIfNeeded(data: TrainData, route: RoutePreference) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val cancelled = data.departures.firstOrNull { it.status == TrainStatus.CANCELLED }
        val delayed = data.departures.firstOrNull { it.delayMinutes >= DELAY_ALERT_MINUTES }
        val disruption = data.disruptions.firstOrNull()

        val alert = when {
            cancelled != null -> Alert(
                title = "c2c train cancelled",
                message = "The ${cancelled.departureTime} service on your saved route is cancelled.",
                fingerprint = "cancelled:${cancelled.serviceId}"
            )
            disruption != null -> Alert(
                title = "Disruption on your c2c route",
                message = disruption.message,
                fingerprint = "disruption:${disruption.message.hashCode()}"
            )
            delayed != null -> Alert(
                title = "Delay on your c2c route",
                message = "The ${delayed.departureTime} service is ${delayed.delayMinutes} minutes late.",
                fingerprint = "delay:${delayed.serviceId}:${delayed.delayMinutes}"
            )
            else -> return
        }

        val routeKey = "${route.homeStationCrs}:${route.workStationCrs}"
        val deliveryKey = "$routeKey:${alert.fingerprint}"
        if (preferences.getLong(deliveryKey, 0L) > System.currentTimeMillis() - DEDUPE_WINDOW_MS) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DarrensTrainsApp.ROUTE_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(routeKey.hashCode(), notification)
        preferences.edit().putLong(deliveryKey, System.currentTimeMillis()).apply()
    }

    private data class Alert(val title: String, val message: String, val fingerprint: String)

    private companion object {
        const val DELAY_ALERT_MINUTES = 5
        const val DEDUPE_WINDOW_MS = 2 * 60 * 60 * 1000L
    }
}
