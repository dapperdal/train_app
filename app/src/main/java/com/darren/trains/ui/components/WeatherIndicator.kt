package com.darren.trains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.ArrivalWeather
import com.darren.trains.ui.theme.CardBackground
import com.darren.trains.ui.theme.CloudGray
import com.darren.trains.ui.theme.RainBlue
import com.darren.trains.ui.theme.SunYellow
import com.darren.trains.ui.theme.TextPrimary
import com.darren.trains.ui.theme.TextSecondary

@Composable
fun WeatherIndicator(
    weather: ArrivalWeather,
    modifier: Modifier = Modifier
) {
    val weatherIcon = getWeatherIcon(weather.weatherCode)
    val iconColor = getWeatherIconColor(weather.weatherCode)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "WEATHER AT FENCHURCH ST",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = weather.description,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = weather.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "${weather.precipitationProbability}% chance of rain",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (weather.shouldBringUmbrella) {
                Spacer(modifier = Modifier.height(12.dp))
                UmbrellaWarning()
            }
        }
    }
}

@Composable
private fun UmbrellaWarning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RainBlue.copy(alpha = 0.2f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Umbrella,
            contentDescription = null,
            tint = RainBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Bring an umbrella!",
            style = MaterialTheme.typography.bodyMedium,
            color = RainBlue,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun WeatherLoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "WEATHER AT FENCHURCH ST",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading weather...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun WeatherErrorIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "WEATHER AT FENCHURCH ST",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Weather unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun getWeatherIcon(weatherCode: Int): ImageVector {
    return when (weatherCode) {
        0, 1 -> Icons.Default.WbSunny // Clear
        2, 3, 45, 48 -> Icons.Default.Cloud // Cloudy/Foggy
        else -> Icons.Default.WaterDrop // Rain/Snow
    }
}

private fun getWeatherIconColor(weatherCode: Int): androidx.compose.ui.graphics.Color {
    return when (weatherCode) {
        0, 1 -> SunYellow
        2, 3, 45, 48 -> CloudGray
        else -> RainBlue
    }
}
