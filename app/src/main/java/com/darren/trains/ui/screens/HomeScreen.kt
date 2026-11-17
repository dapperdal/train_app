package com.darren.trains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.Stations
import com.darren.trains.data.model.DisruptionSeverity
import com.darren.trains.data.repository.FeedQuality
import com.darren.trains.data.repository.TrainData
import com.darren.trains.ui.components.DisruptionSection
import com.darren.trains.ui.components.LocationStatus
import com.darren.trains.ui.components.TrainCard
import com.darren.trains.ui.theme.*
import com.darren.trains.viewmodel.TrainUiState
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    uiState: TrainUiState,
    onRefresh: () -> Unit,
    onToggleDirection: () -> Unit,
    onBrowseBack: () -> Unit,
    onBrowseForward: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DARREN'S TRAINS",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = "C2C NETWORK ONLY",
                        style = MaterialTheme.typography.labelMedium,
                        color = C2CPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Smart routing info
                    if (uiState.currentStationCrs != null && uiState.extendedPreference != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (uiState.isNearStation) {
                                    "Near your saved station:"
                                } else {
                                    "Based on your location, showing:"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${Stations.getStationName(uiState.currentStationCrs!!)} (${uiState.currentStationCrs})",
                                style = MaterialTheme.typography.titleMedium,
                                color = C2CPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Trains towards:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            val primaryRoute = uiState.extendedPreference?.primaryRoute
                            val destinationCrs = when (uiState.currentStationCrs) {
                                primaryRoute?.homeStationCrs -> primaryRoute.workStationCrs
                                primaryRoute?.workStationCrs -> primaryRoute.homeStationCrs
                                else -> primaryRoute?.workStationCrs
                            }
                            Text(
                                text = "${Stations.getStationName(destinationCrs!!)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = C2CPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Fallback to location status if no route preference yet
                        LocationStatus(
                            locationResult = uiState.locationResult,
                            currentDirection = uiState.currentDirection,
                            isNearStation = uiState.isNearStation,
                            onToggleDirection = onToggleDirection
                        )
                    }
                }
            }

            uiState.trainData?.let { data ->
                item { ServiceStatusCard(data = data) }
            }

            // Error message
            if (uiState.error != null) {
                item {
                    ErrorCard(message = uiState.error, onRetry = onRefresh)
                }
            }

            // Disruptions section
            uiState.trainData?.disruptions?.let { disruptions ->
                if (disruptions.isNotEmpty()) {
                    item {
                        DisruptionSection(disruptions = disruptions)
                    }
                }
            }

            // Loading state
            if (uiState.isLoading && uiState.trainData == null) {
                item {
                    LoadingContent()
                }
            }

            // Trains section
            uiState.trainData?.let { data ->
                item {
                    BrowseHeader(
                        offsetMinutes = uiState.browseOffsetMinutes,
                        lastUpdated = uiState.lastUpdated,
                        onBack = onBrowseBack,
                        onForward = onBrowseForward
                    )
                }

                if (data.departures.isEmpty()) {
                    item {
                        val route = uiState.extendedPreference?.primaryRoute
                        val originCrs = uiState.currentStationCrs ?: route?.homeStationCrs.orEmpty()
                        val destinationCrs = when (originCrs) {
                            route?.homeStationCrs -> route.workStationCrs
                            else -> route?.homeStationCrs.orEmpty()
                        }
                        NoTrainsMessage(
                            originCrs = originCrs,
                            destinationCrs = destinationCrs,
                            canBrowseForward = uiState.browseOffsetMinutes < 119,
                            onBrowseForward = onBrowseForward,
                            onRetry = onRefresh
                        )
                    }
                } else {
                    items(data.departures) { train ->
                        TrainCard(train = train)
                    }
                }
            }

            // Bottom padding for comfortable scrolling
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = DarkSurface,
            contentColor = C2CPrimary
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = C2CPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading trains...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CancelledRed.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Try again", color = C2CSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NoTrainsMessage(
    originCrs: String,
    destinationCrs: String,
    canBrowseForward: Boolean,
    onBrowseForward: () -> Unit,
    onRetry: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val earlyMorningDate = remember {
        if (LocalTime.now().hour < 4) LocalDate.now() else LocalDate.now().plusDays(1)
    }
    val journeyPlannerUrl = remember(originCrs, destinationCrs, earlyMorningDate) {
        val date = earlyMorningDate.format(DateTimeFormatter.ofPattern("ddMMyy"))
        "https://www.nationalrail.co.uk/journey-planner/" +
            "?type=single&origin=$originCrs&destination=$destinationCrs" +
            "&leavingDate=$date&leavingHour=04&leavingMin=00&leavingType=departing&adults=1"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = C2CPrimary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No more trains tonight",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "We’ve checked the full live window. Advance to the official timetable to find the first morning service.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onRetry) {
                Text("Check again", color = C2CSecondary, fontWeight = FontWeight.Bold)
            }
            if (canBrowseForward) {
                OutlinedButton(onClick = onBrowseForward) {
                    Text("Browse later", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
            Button(
                onClick = { uriHandler.openUri(journeyPlannerUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = C2CPrimary),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Find first train", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun BrowseHeader(
    offsetMinutes: Int,
    lastUpdated: Long,
    onBack: () -> Unit,
    onForward: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEXT TRAINS", style = MaterialTheme.typography.titleMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
            if (lastUpdated > 0) {
                Text("Updated ${formatTime(lastUpdated)}", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = offsetMinutes > 0) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Earlier trains",
                    tint = if (offsetMinutes > 0) C2CSecondary else TextTertiary.copy(alpha = 0.35f)
                )
            }
            Text(
                text = when (offsetMinutes) {
                    0 -> "Live now"
                    60 -> "About 1 hour ahead"
                    else -> "About 2–4 hours ahead"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (offsetMinutes == 0) OnTimeGreen else C2CSecondary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onForward, enabled = offsetMinutes < 119) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Later trains",
                    tint = if (offsetMinutes < 119) C2CSecondary else TextTertiary.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(data: TrainData) {
    val worstDisruption = data.disruptions.maxByOrNull { it.severity.ordinal }
    val (title, detail, color, icon) = when {
        data.feedQuality == FeedQuality.STALE -> StatusPresentation(
            "LIVE FEED INTERRUPTED",
            "Showing the most recent saved departure board",
            DelayedAmber,
            Icons.Default.CloudOff
        )
        data.feedQuality == FeedQuality.REDUCED -> StatusPresentation(
            "LIVE • BASIC DETAIL",
            "Times are live; calling-point detail is temporarily limited",
            C2CSecondary,
            Icons.Default.CloudOff
        )
        data.feedQuality == FeedQuality.LOOKAHEAD -> StatusPresentation(
            "LOOKING FURTHER AHEAD",
            "No immediate trains; showing the next published services",
            C2CSecondary,
            Icons.Default.Train
        )
        worstDisruption?.severity == DisruptionSeverity.SEVERE -> StatusPresentation(
            "SEVERE DISRUPTION",
            "Your saved c2c route is affected — see Status & disruptions below",
            DisruptionSevere,
            Icons.Default.Warning
        )
        worstDisruption != null -> StatusPresentation(
            "SERVICE DISRUPTION",
            "See Status & disruptions below before you travel",
            DisruptionMinor,
            Icons.Default.Warning
        )
        else -> StatusPresentation(
            "GOOD SERVICE",
            "No disruption is currently reported for this journey",
            OnTimeGreen,
            Icons.Default.CheckCircle
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

private data class StatusPresentation(
    val title: String,
    val detail: String,
    val color: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.UK)
    return formatter.format(Date(timestamp))
}
