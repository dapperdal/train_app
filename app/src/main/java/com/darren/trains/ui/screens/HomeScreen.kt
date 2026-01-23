package com.darren.trains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.TrainDeparture
import com.darren.trains.ui.components.DisruptionSection
import com.darren.trains.ui.components.LocationStatus
import com.darren.trains.ui.components.TrainCard
import com.darren.trains.ui.theme.*
import com.darren.trains.viewmodel.TrainUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    uiState: TrainUiState,
    onRefresh: () -> Unit,
    onToggleDirection: () -> Unit,
    onBoardTrain: (TrainDeparture) -> Unit,
    activeJourneyServiceId: String? = null,
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
                    Text(
                        text = "DARREN'S TRAINS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location and direction status
                    LocationStatus(
                        locationResult = uiState.locationResult,
                        currentDirection = uiState.currentDirection,
                        isNearStation = uiState.isNearStation,
                        onToggleDirection = onToggleDirection
                    )
                }
            }

            // Error message
            if (uiState.error != null) {
                item {
                    ErrorCard(message = uiState.error)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NEXT TRAINS",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )

                        // Last updated time
                        if (uiState.lastUpdated > 0) {
                            Text(
                                text = "Updated ${formatTime(uiState.lastUpdated)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary
                            )
                        }
                    }
                }

                if (data.departures.isEmpty()) {
                    item {
                        NoTrainsMessage()
                    }
                } else {
                    items(data.departures) { train ->
                        TrainCard(
                            train = train,
                            onBoardTrain = onBoardTrain,
                            isOnboardEnabled = activeJourneyServiceId == null
                        )
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
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CancelledRed.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = CancelledRed,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun NoTrainsMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No trains available\n(Grays services excluded)",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.UK)
    return formatter.format(Date(timestamp))
}
