package com.darren.trains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darren.trains.data.model.RoutePreference
import com.darren.trains.data.model.Stations
import com.darren.trains.ui.theme.*

@Composable
fun SettingsScreen(
    currentPreference: RoutePreference?,
    currentNotifyOnDelay: Boolean,
    currentPreferExpress: Boolean,
    onBackClick: () -> Unit,
    onPreferenceUpdate: (RoutePreference) -> Unit,
    onAdvancedSettingsUpdate: (notifyOnDelay: Boolean, preferExpress: Boolean) -> Unit,
    onRouteAlertsEnabled: () -> Unit,
    onRedoSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterLakeside by remember(currentPreference) {
        mutableStateOf(currentPreference?.filterLakesideLoop ?: true)
    }
    var notifyOnDelay by remember(currentNotifyOnDelay) { mutableStateOf(currentNotifyOnDelay) }
    var preferExpress by remember(currentPreferExpress) { mutableStateOf(currentPreferExpress) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Set up your route again?") },
            text = {
                Text("Your saved route will stay active until you complete the setup. You can cancel without changing anything.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        onRedoSetup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C2CPrimary)
                ) {
                    Text("Start setup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Keep my route", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsHeader(onBackClick = onBackClick)
            }

            item {
                SectionHeading("SAVED ROUTE", "Locked and ready every time you open the app")
            }

            item {
                LockedRouteCard(currentPreference = currentPreference)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetConfirmation = true },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(C2CPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = C2CPrimary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Set up my route again",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Replace both saved stations in the guided setup",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
                    }
                }
            }

            item {
                SectionHeading("TRAIN FILTER", "Applied automatically to your saved route")
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Skip Lakeside Loop services", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Hide services via Grays and Tilbury", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = filterLakeside,
                            onCheckedChange = { filterLakeside = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = C2CPrimary
                            )
                        )
                    }
                }
            }

            item {
                SectionHeading("JOURNEY PREFERENCES", "Small choices the app remembers for you")
            }

            item {
                PreferenceSwitchCard(
                    title = "Personalised route alerts",
                    subtitle = "Cancellations, disruption and delays over 5 minutes",
                    checked = notifyOnDelay,
                    onCheckedChange = {
                        notifyOnDelay = it
                        if (it) onRouteAlertsEnabled()
                    }
                )
            }

            item {
                PreferenceSwitchCard(
                    title = "Prefer express trains",
                    subtitle = "Prioritise faster services with fewer stops",
                    checked = preferExpress,
                    onCheckedChange = { preferExpress = it }
                )
            }

            val preferencesChanged = currentPreference != null && (
                filterLakeside != currentPreference.filterLakesideLoop ||
                    notifyOnDelay != currentNotifyOnDelay ||
                    preferExpress != currentPreferExpress
                )
            if (preferencesChanged) {
                item {
                    Button(
                        onClick = {
                            onPreferenceUpdate(currentPreference!!.copy(filterLakesideLoop = filterLakeside))
                            onAdvancedSettingsUpdate(notifyOnDelay, preferExpress)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = C2CPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save preferences", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SectionHeading("WHY THIS APP EXISTS", "Built for the moment you just need to get home")
            }

            item {
                StoryCard()
            }

            item {
                SectionHeading("ACCESS", "What to expect as the app develops")
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = C2CPrimary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "FREE FOR NOW",
                                style = MaterialTheme.typography.labelMedium,
                                color = C2CPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(
                                color = C2CPrimary,
                                shape = CircleShape
                            ) {
                                Text(
                                    "£0",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            "There’s no subscription or payment required during early access.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "A paid subscription is planned for a later release. We’ll share the price, benefits and start date clearly before it launches — nothing will be charged automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Darren’s Trains  •  Early access",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StoryCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, C2CPrimary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SIMPLICITY, BY DESIGN",
                style = MaterialTheme.typography.labelMedium,
                color = C2CPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "“I built this after a few Christmas nights out, when all I wanted was to get to the station and know which fast train would get me home.”",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No complicated route planning. No setting up the same journey every day. The app remembers the two stations that matter, uses your location to work out which way you’re travelling, and puts the useful train and platform in front of you.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "On the way home, it can hide services around the Lakeside and Tilbury loop, making it easier to spot the faster mainline train. And because early starts, late trains and post-party journeys don’t need a bright screen, the whole experience is dark by design.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "Open it. See your train. Get moving.",
                style = MaterialTheme.typography.bodyLarge,
                color = C2CSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PreferenceSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = C2CPrimary
                )
            )
        }
    }
}

@Composable
private fun SettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Text(
            text = "SETTINGS",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = C2CPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun LockedRouteCard(currentPreference: RoutePreference?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = C2CPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "STATIONS LOCKED",
                    style = MaterialTheme.typography.labelMedium,
                    color = C2CPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(18.dp))
            RouteStation("HOME", currentPreference?.homeStationCrs)
            Box(
                Modifier
                    .padding(start = 12.dp)
                    .width(2.dp)
                    .height(28.dp)
                    .background(C2CPrimary.copy(alpha = 0.5f))
            )
            RouteStation("DESTINATION", currentPreference?.workStationCrs)
        }
    }
}

@Composable
private fun RouteStation(label: String, crs: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(C2CPrimary),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(TextPrimary))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextTertiary, fontWeight = FontWeight.Bold)
            Text(
                Stations.getStationName(crs.orEmpty()),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (crs != null) {
                Text(crs, style = MaterialTheme.typography.labelMedium, color = C2CPrimary)
            }
        }
    }
}
