package com.darren.trains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darren.trains.data.model.TrainDeparture
import com.darren.trains.data.model.TrainStatus
import com.darren.trains.ui.theme.*

@Composable
fun TrainCard(
    train: TrainDeparture,
    modifier: Modifier = Modifier
) {
    val statusColor = when (train.status) {
        TrainStatus.ON_TIME -> OnTimeGreen
        TrainStatus.DELAYED -> DelayedAmber
        TrainStatus.CANCELLED -> CancelledRed
    }

    val statusText = when {
        train.isCancelled -> "CANCELLED"
        train.status == TrainStatus.ON_TIME -> "ON TIME"
        train.delayMinutes > 0 -> "+${train.delayMinutes} MIN"
        else -> train.estimatedTime
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // Top row: Time and Platform
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Departure time
                Text(
                    text = train.departureTime,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                // Platform
                if (train.platform != "-") {
                    PlatformBadge(platform = train.platform)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Destination
            Text(
                text = train.destination,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Journey time and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Journey time
                train.journeyTimeMinutes?.let { minutes ->
                    Text(
                        text = "Journey: $minutes mins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                } ?: Spacer(modifier = Modifier.weight(1f))

                // Status badge
                StatusBadge(
                    text = statusText,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun PlatformBadge(platform: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(C2CPrimary.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Platform ",
                style = MaterialTheme.typography.labelMedium,
                color = C2CSecondary
            )
            Text(
                text = platform,
                style = MaterialTheme.typography.titleMedium,
                color = C2CSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
