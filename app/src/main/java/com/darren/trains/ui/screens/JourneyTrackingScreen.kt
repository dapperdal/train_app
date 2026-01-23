package com.darren.trains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.ActiveJourney
import com.darren.trains.data.model.AlertPreference
import com.darren.trains.data.model.JourneyAlertSettings
import com.darren.trains.data.model.JourneyProgress
import com.darren.trains.ui.components.JourneyProgressBar
import com.darren.trains.ui.components.StopsRemainingCard
import com.darren.trains.ui.components.WeatherErrorIndicator
import com.darren.trains.ui.components.WeatherIndicator
import com.darren.trains.ui.components.WeatherLoadingIndicator
import com.darren.trains.ui.theme.C2CPrimary
import com.darren.trains.ui.theme.C2CSecondary
import com.darren.trains.ui.theme.CancelledRed
import com.darren.trains.ui.theme.CardBackground
import com.darren.trains.ui.theme.DarkBackground
import com.darren.trains.ui.theme.DelayedAmber
import com.darren.trains.ui.theme.OnTimeGreen
import com.darren.trains.ui.theme.TextPrimary
import com.darren.trains.ui.theme.TextSecondary
import com.darren.trains.ui.theme.TextTertiary
import com.darren.trains.viewmodel.WeatherState

@Composable
fun JourneyTrackingScreen(
    journey: ActiveJourney,
    progress: JourneyProgress,
    weatherState: WeatherState,
    alertSettings: JourneyAlertSettings,
    onEndJourney: () -> Unit,
    onToggleAlert: (Boolean) -> Unit,
    onChangeAlertPreference: (AlertPreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                JourneyHeader(
                    journey = journey,
                    onEndJourney = onEndJourney
                )
            }

            item {
                JourneyProgressBar(
                    currentStop = progress.currentStopIndex,
                    totalStops = progress.totalStops,
                    originName = journey.originName,
                    destinationName = journey.destinationName
                )
            }

            item {
                StopsRemainingCard(
                    stopsRemaining = progress.stopsRemaining,
                    nextStopName = progress.nextStopName
                )
            }

            item {
                ArrivalTimeCard(progress = progress)
            }

            item {
                when (weatherState) {
                    is WeatherState.Loading -> WeatherLoadingIndicator()
                    is WeatherState.Success -> WeatherIndicator(weather = weatherState.weather)
                    is WeatherState.Error -> WeatherErrorIndicator()
                    else -> {}
                }
            }

            item {
                AlertSettingsCard(
                    settings = alertSettings,
                    onToggleAlert = onToggleAlert,
                    onChangePreference = onChangeAlertPreference
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun JourneyHeader(
    journey: ActiveJourney,
    onEndJourney: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "You're on the ${journey.trainDeparture.departureTime}",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "to ${journey.destinationName}",
                style = MaterialTheme.typography.titleMedium,
                color = C2CSecondary
            )
        }

        Button(
            onClick = onEndJourney,
            colors = ButtonDefaults.buttonColors(
                containerColor = CancelledRed.copy(alpha = 0.2f),
                contentColor = CancelledRed
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "End Journey",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ArrivalTimeCard(progress: JourneyProgress) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "ARRIVING IN",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                val minutesText = progress.minutesToArrival?.toString() ?: "--"
                Text(
                    text = minutesText,
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "minutes",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Estimated arrival",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Text(
                        text = progress.estimatedArrivalTime ?: "--:--",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (progress.isDelayed) DelayedAmber else OnTimeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (progress.isDelayed && progress.delayMinutes > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Delay",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                        Text(
                            text = "+${progress.delayMinutes} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = DelayedAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertSettingsCard(
    settings: JourneyAlertSettings,
    onToggleAlert: (Boolean) -> Unit,
    onChangePreference: (AlertPreference) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "ARRIVAL ALERT",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (settings.twoMinuteAlertEnabled)
                            Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (settings.twoMinuteAlertEnabled) C2CPrimary else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "2-minute warning",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Alert me before arriving",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = settings.twoMinuteAlertEnabled,
                    onCheckedChange = onToggleAlert,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = C2CPrimary,
                        checkedTrackColor = C2CPrimary.copy(alpha = 0.5f)
                    )
                )
            }

            if (settings.twoMinuteAlertEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Alert type",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlertOptionButton(
                        text = "Audio",
                        icon = Icons.Default.VolumeUp,
                        isSelected = settings.alertPreference == AlertPreference.AUDIO_IF_BLUETOOTH,
                        onClick = { onChangePreference(AlertPreference.AUDIO_IF_BLUETOOTH) },
                        modifier = Modifier.weight(1f)
                    )

                    AlertOptionButton(
                        text = "Vibrate",
                        icon = Icons.Default.Vibration,
                        isSelected = settings.alertPreference == AlertPreference.SILENT_VIBRATE,
                        onClick = { onChangePreference(AlertPreference.SILENT_VIBRATE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (settings.alertPreference == AlertPreference.AUDIO_IF_BLUETOOTH)
                        "Voice announcement if Bluetooth headphones connected, otherwise vibrate"
                    else
                        "Silent vibration alert only",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AlertOptionButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) C2CPrimary else CardBackground,
            contentColor = if (isSelected) TextPrimary else TextSecondary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun JourneyCompletedScreen(
    journey: ActiveJourney,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "You've arrived!",
                style = MaterialTheme.typography.headlineLarge,
                color = OnTimeGreen,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome to ${journey.destinationName}",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = C2CPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
