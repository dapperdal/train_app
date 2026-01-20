package com.darren.trains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.Stations
import com.darren.trains.data.model.TravelDirection
import com.darren.trains.location.LocationResult
import com.darren.trains.ui.theme.*

@Composable
fun LocationStatus(
    locationResult: LocationResult?,
    currentDirection: TravelDirection?,
    isNearStation: Boolean,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Location info
        if (locationResult != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = C2CSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "%.1f miles from ${locationResult.nearestStation.name}".format(
                        locationResult.nearestStation.distanceMiles
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Direction indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Current direction
            val directionText = when (currentDirection) {
                TravelDirection.TO_FENCHURCH_STREET -> "Heading to ${Stations.FENCHURCH_STREET_NAME}"
                TravelDirection.TO_LEIGH_ON_SEA -> "Heading to ${Stations.LEIGH_ON_SEA_NAME}"
                null -> "Select direction"
            }

            Text(
                text = directionText,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            // Toggle button (only show when not near a station)
            if (!isNearStation) {
                DirectionToggleButton(onClick = onToggleDirection)
            }
        }
    }
}

@Composable
private fun DirectionToggleButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(C2CPrimary.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Toggle direction",
                tint = C2CSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Switch",
                style = MaterialTheme.typography.labelMedium,
                color = C2CSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
