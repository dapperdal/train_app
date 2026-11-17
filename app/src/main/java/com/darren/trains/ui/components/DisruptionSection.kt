package com.darren.trains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.Disruption
import com.darren.trains.data.model.DisruptionSeverity
import com.darren.trains.ui.theme.*

@Composable
fun DisruptionSection(
    disruptions: List<Disruption>,
    modifier: Modifier = Modifier
) {
    if (disruptions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DisruptionMinor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Disruptions",
                tint = DisruptionMinor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "STATUS & DISRUPTIONS",
                style = MaterialTheme.typography.labelLarge,
                color = DisruptionMinor,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Live National Rail updates for your saved c2c journey",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Disruption messages
        disruptions.forEachIndexed { index, disruption ->
            DisruptionItem(disruption = disruption, expanded = expanded)
            if (index < disruptions.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (expanded) "Show less" else "Read full update", color = C2CSecondary)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = C2CSecondary
            )
        }
    }
}

@Composable
private fun DisruptionItem(disruption: Disruption, expanded: Boolean) {
    val indicatorColor = when (disruption.severity) {
        DisruptionSeverity.MINOR -> DisruptionMinor
        DisruptionSeverity.MAJOR -> DisruptionMajor
        DisruptionSeverity.SEVERE -> DisruptionSevere
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Severity indicator
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = disruption.message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}
