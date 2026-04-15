package com.darren.trains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.RoutePreference
import com.darren.trains.data.model.Station
import com.darren.trains.data.model.Stations
import com.darren.trains.ui.theme.*

@Composable
fun SetupScreen(
    onSetupComplete: (RoutePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    var homeStationCrs by remember { mutableStateOf<String?>(null) }
    var workStationCrs by remember { mutableStateOf<String?>(null) }
    var filterLakeside by remember { mutableStateOf(true) }
    var currentStep by remember { mutableStateOf(SetupStep.HOME_STATION) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            item {
                Column {
                    Text(
                        text = "DARREN'S TRAINS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Let's get you set up!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            // Progress indicator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    color = if (index <= currentStep.ordinal) C2CPrimary else CardBackground,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            // Current step content
            when (currentStep) {
                SetupStep.HOME_STATION -> {
                    item {
                        SetupStepContent(
                            title = "Where do you live?",
                            subtitle = "Select your home station",
                            stations = Stations.C2C_STATIONS,
                            selectedCrs = homeStationCrs,
                            onSelect = { homeStationCrs = it }
                        )
                    }
                }
                SetupStep.WORK_STATION -> {
                    item {
                        SetupStepContent(
                            title = "Where do you work?",
                            subtitle = "Select your work station",
                            stations = Stations.C2C_STATIONS,
                            selectedCrs = workStationCrs,
                            onSelect = { workStationCrs = it }
                        )
                    }
                }
                SetupStep.PREFERENCES -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Preferences",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Filter Lakeside Loop",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Skip trains via Grays/Tilbury",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Switch(
                                        checked = filterLakeside,
                                        onCheckedChange = { filterLakeside = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = C2CPrimary,
                                            checkedTrackColor = C2CPrimary.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigation buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Back button
                    if (currentStep.ordinal > 0) {
                        Button(
                            onClick = {
                                currentStep = SetupStep.values()[currentStep.ordinal - 1]
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardBackground,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Back", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Next/Complete button
                    Button(
                        onClick = {
                            if (currentStep.ordinal < SetupStep.values().size - 1) {
                                currentStep = SetupStep.values()[currentStep.ordinal + 1]
                            } else {
                                // Complete setup
                                if (homeStationCrs != null && workStationCrs != null) {
                                    onSetupComplete(
                                        RoutePreference(
                                            homeStationCrs = homeStationCrs!!,
                                            workStationCrs = workStationCrs!!,
                                            filterLakesideLoop = filterLakeside
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = C2CPrimary,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = when (currentStep) {
                            SetupStep.HOME_STATION -> homeStationCrs != null
                            SetupStep.WORK_STATION -> workStationCrs != null && homeStationCrs != workStationCrs
                            SetupStep.PREFERENCES -> true
                        }
                    ) {
                        Text(
                            text = if (currentStep.ordinal == SetupStep.values().size - 1) "Complete" else "Next",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SetupStepContent(
    title: String,
    subtitle: String,
    stations: List<Station>,
    selectedCrs: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stations) { station ->
                StationOption(
                    station = station,
                    isSelected = station.crs == selectedCrs,
                    onClick = { onSelect(station.crs) }
                )
            }
        }
    }
}

@Composable
private fun StationOption(
    station: Station,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) C2CPrimary.copy(alpha = 0.2f) else CardBackground
        ),
        border = if (isSelected) androidx.compose.material3.CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = station.crs,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = C2CPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

enum class SetupStep {
    HOME_STATION,
    WORK_STATION,
    PREFERENCES
}
