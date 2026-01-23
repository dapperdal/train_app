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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darren.trains.ui.theme.C2CPrimary
import com.darren.trains.ui.theme.CardBackground
import com.darren.trains.ui.theme.OnTimeGreen
import com.darren.trains.ui.theme.ProgressComplete
import com.darren.trains.ui.theme.ProgressRemaining
import com.darren.trains.ui.theme.TextPrimary
import com.darren.trains.ui.theme.TextSecondary

@Composable
fun JourneyProgressBar(
    currentStop: Int,
    totalStops: Int,
    originName: String,
    destinationName: String,
    modifier: Modifier = Modifier
) {
    val progress = if (totalStops > 1) {
        currentStop.toFloat() / (totalStops - 1).toFloat()
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Column {
            // Station names row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = originName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = destinationName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar with icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Origin dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ProgressComplete)
                )

                // Progress track
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    // Background track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ProgressRemaining)
                    )

                    // Progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ProgressComplete)
                    )

                    // Train icon at current position
                    if (progress < 1f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 0.95f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = "Current position",
                                tint = C2CPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                }

                // Destination flag
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Destination",
                    tint = if (progress >= 1f) OnTimeGreen else ProgressRemaining,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stop count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$currentStop",
                    style = MaterialTheme.typography.titleMedium,
                    color = C2CPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " / $totalStops stops",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StopsRemainingCard(
    stopsRemaining: Int,
    nextStopName: String?,
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
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$stopsRemaining",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (stopsRemaining == 1) "stop remaining" else "stops remaining",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (nextStopName != null && stopsRemaining > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next: $nextStopName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
