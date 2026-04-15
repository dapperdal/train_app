package com.darren.trains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.RoutePreference
import com.darren.trains.data.model.Station
import com.darren.trains.data.model.Stations
import com.darren.trains.ui.theme.*

@Composable
fun SettingsScreen(
    currentPreference: RoutePreference?,
    onBackClick: () -> Unit,
    onPreferenceUpdate: (RoutePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSection by remember { mutableStateOf<SettingSection?>(null) }
    var tempHomeStationCrs by remember { mutableStateOf(currentPreference?.homeStationCrs) }
    var tempWorkStationCrs by remember { mutableStateOf(currentPreference?.workStationCrs) }
    var tempFilterLakeside by remember { mutableStateOf(currentPreference?.filterLakesideLoop ?: true) }

    if (editingSection != null) {
        EditSettingDialog(
            section = editingSection!!,
            currentHomeStation = tempHomeStationCrs,
            currentWorkStation = tempWorkStationCrs,
            onConfirm = { home, work ->
                if (home != null && work != null && home != work) {
                    tempHomeStationCrs = home
                    tempWorkStationCrs = work
                    editingSection = null
                }
            },
            onDismiss = { editingSection = null }
        )
    }

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
            // Header with back button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp))  // Balance the back button
                }
            }

            // Route Settings Section
            item {
                Text(
                    text = "Your Route",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingCard(
                    title = "Home Station",
                    value = Stations.getStationName(tempHomeStationCrs ?: ""),
                    crs = tempHomeStationCrs,
                    onEditClick = { editingSection = SettingSection.HOME_STATION }
                )
            }

            item {
                SettingCard(
                    title = "Work Station",
                    value = Stations.getStationName(tempWorkStationCrs ?: ""),
                    crs = tempWorkStationCrs,
                    onEditClick = { editingSection = SettingSection.WORK_STATION }
                )
            }

            // Filter Settings Section
            item {
                Text(
                    text = "Train Filtering",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
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
                                text = "Hide trains via Grays & Tilbury",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = tempFilterLakeside,
                            onCheckedChange = { tempFilterLakeside = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = C2CPrimary,
                                checkedTrackColor = C2CPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Save Changes Button
            item {
                Button(
                    onClick = {
                        if (tempHomeStationCrs != null && tempWorkStationCrs != null) {
                            onPreferenceUpdate(
                                RoutePreference(
                                    homeStationCrs = tempHomeStationCrs!!,
                                    workStationCrs = tempWorkStationCrs!!,
                                    filterLakesideLoop = tempFilterLakeside
                                )
                            )
                            onBackClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = C2CPrimary,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = tempHomeStationCrs != null && tempWorkStationCrs != null && tempHomeStationCrs != tempWorkStationCrs
                ) {
                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                }
            }

            // About Section
            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow(label = "App Name", value = "Darren's Trains")
                        Divider(color = DarkSurface, thickness = 1.dp)
                        InfoRow(label = "Version", value = "2.0.0")
                        Divider(color = DarkSurface, thickness = 1.dp)
                        InfoRow(label = "Network", value = "C2C (23 stations)")
                        Divider(color = DarkSurface, thickness = 1.dp)
                        InfoRow(label = "Data Source", value = "National Rail API")
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
private fun SettingCard(
    title: String,
    value: String,
    crs: String?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
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
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (crs != null) {
                    Text(
                        text = crs,
                        style = MaterialTheme.typography.labelSmall,
                        color = C2CPrimary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EditSettingDialog(
    section: SettingSection,
    currentHomeStation: String?,
    currentWorkStation: String?,
    onConfirm: (String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var homeStation by remember { mutableStateOf(currentHomeStation) }
    var workStation by remember { mutableStateOf(currentWorkStation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (section) {
                    SettingSection.HOME_STATION -> "Select Home Station"
                    SettingSection.WORK_STATION -> "Select Work Station"
                },
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(Stations.C2C_STATIONS) { station ->
                    val isSelected = when (section) {
                        SettingSection.HOME_STATION -> station.crs == homeStation
                        SettingSection.WORK_STATION -> station.crs == workStation
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (section) {
                                    SettingSection.HOME_STATION -> homeStation = station.crs
                                    SettingSection.WORK_STATION -> workStation = station.crs
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) C2CPrimary.copy(alpha = 0.2f) else CardBackground
                        )
                    ) {
                        Text(
                            text = "${station.name} (${station.crs})",
                            modifier = Modifier.padding(12.dp),
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(homeStation, workStation) },
                enabled = when (section) {
                    SettingSection.HOME_STATION -> homeStation != null && homeStation != workStation
                    SettingSection.WORK_STATION -> workStation != null && homeStation != workStation
                },
                colors = ButtonDefaults.buttonColors(containerColor = C2CPrimary)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground)
            ) {
                Text("Cancel")
            }
        },
        containerColor = DarkBackground,
        textContentColor = TextPrimary
    )
}

enum class SettingSection {
    HOME_STATION,
    WORK_STATION
}
