package com.darren.trains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier,
    initialPreference: RoutePreference? = null,
    isReconfiguration: Boolean = false,
    onCancel: (() -> Unit)? = null
) {
    var homeStationCrs by remember(initialPreference) {
        mutableStateOf(initialPreference?.homeStationCrs)
    }
    var workStationCrs by remember(initialPreference) {
        mutableStateOf(initialPreference?.workStationCrs)
    }
    var filterLakeside by remember(initialPreference) {
        mutableStateOf(initialPreference?.filterLakesideLoop ?: true)
    }
    var currentStep by remember { mutableStateOf(SetupStep.WELCOME) }

    val canContinue = when (currentStep) {
        SetupStep.WELCOME -> true
        SetupStep.HOME_STATION -> homeStationCrs != null
        SetupStep.WORK_STATION -> workStationCrs != null && workStationCrs != homeStationCrs
        SetupStep.CONFIRM -> homeStationCrs != null && workStationCrs != null && homeStationCrs != workStationCrs
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        SetupHeader(
            currentStep = currentStep,
            isReconfiguration = isReconfiguration,
            onBack = when {
                currentStep != SetupStep.WELCOME -> {
                    { currentStep = SetupStep.values()[currentStep.ordinal - 1] }
                }
                onCancel != null -> onCancel
                else -> null
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (currentStep) {
                SetupStep.WELCOME -> item {
                    WelcomeStep(isReconfiguration = isReconfiguration)
                }

                SetupStep.HOME_STATION -> {
                    item {
                        StepIntroduction(
                            eyebrow = "1 / 3  HOME",
                            title = "Where do you start?",
                            subtitle = "Choose the station you usually travel from. This is a one-time selection, not a daily dropdown."
                        )
                    }
                    items(Stations.C2C_STATIONS, key = { it.crs }) { station ->
                        StationOption(
                            station = station,
                            isSelected = station.crs == homeStationCrs,
                            enabled = station.crs != workStationCrs,
                            onClick = { homeStationCrs = station.crs }
                        )
                    }
                }

                SetupStep.WORK_STATION -> {
                    item {
                        StepIntroduction(
                            eyebrow = "2 / 3  DESTINATION",
                            title = "Where are you heading?",
                            subtitle = "Choose your regular destination. The app will automatically reverse the route for the journey home."
                        )
                    }
                    items(Stations.C2C_STATIONS, key = { it.crs }) { station ->
                        StationOption(
                            station = station,
                            isSelected = station.crs == workStationCrs,
                            enabled = station.crs != homeStationCrs,
                            onClick = { workStationCrs = station.crs }
                        )
                    }
                }

                SetupStep.CONFIRM -> item {
                    ConfirmStep(
                        homeStationCrs = homeStationCrs.orEmpty(),
                        workStationCrs = workStationCrs.orEmpty(),
                        filterLakeside = filterLakeside,
                        onFilterChange = { filterLakeside = it }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep != SetupStep.WELCOME) {
                Button(
                    onClick = { currentStep = SetupStep.values()[currentStep.ordinal - 1] },
                    modifier = Modifier
                        .weight(0.42f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardBackground,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Back", fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = {
                    if (currentStep == SetupStep.CONFIRM) {
                        onSetupComplete(
                            RoutePreference(
                                homeStationCrs = homeStationCrs!!,
                                workStationCrs = workStationCrs!!,
                                filterLakesideLoop = filterLakeside
                            )
                        )
                    } else {
                        currentStep = SetupStep.values()[currentStep.ordinal + 1]
                    }
                },
                enabled = canContinue,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = C2CPrimary,
                    contentColor = TextPrimary,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = when (currentStep) {
                        SetupStep.WELCOME -> if (isReconfiguration) "Choose stations" else "Set up my route"
                        SetupStep.CONFIRM -> "Lock in my route"
                        else -> "Continue"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SetupHeader(
    currentStep: SetupStep,
    isReconfiguration: Boolean,
    onBack: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Text(
                text = if (isReconfiguration) "RESET YOUR ROUTE" else "DARREN'S TRAINS",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(48.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SetupStep.values().forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (index <= currentStep.ordinal) C2CPrimary else CardBorder)
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(isReconfiguration: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(C2CPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = C2CPrimary,
                modifier = Modifier.size(42.dp)
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = if (isReconfiguration) "A fresh route, same simple app." else "Your train. Your platform. No faff.",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isReconfiguration) {
                "Run the short setup again to replace your saved stations. Nothing changes until you confirm at the end."
            } else {
                "Choose your usual stations once. From then on, the app uses where you are to work out whether you’re heading out or home — no daily journey planning."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureLine("Purpose-built for the c2c network")
                FeatureLine("Knows which way you’re travelling")
                FeatureLine("Shows the train and platform you need")
                FeatureLine("Can avoid the slower Lakeside and Tilbury loop")
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = C2CPrimary.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "FREE DURING EARLY ACCESS",
                    style = MaterialTheme.typography.labelMedium,
                    color = C2CPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "The app is free to use for now. A subscription is planned for a later release, and we’ll explain the price and choices clearly before anything changes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(C2CPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = C2CPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun StepIntroduction(eyebrow: String, title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelMedium,
            color = C2CPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun StationOption(
    station: Station,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) C2CPrimary.copy(alpha = 0.16f) else CardBackground
        ),
        border = CardDefaults.outlinedCardBorder().takeIf { isSelected },
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    station.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) TextPrimary else TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (enabled) station.crs else "Already selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) C2CPrimary else TextTertiary
                )
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = C2CPrimary)
            }
        }
    }
}

@Composable
private fun ConfirmStep(
    homeStationCrs: String,
    workStationCrs: String,
    filterLakeside: Boolean,
    onFilterChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepIntroduction(
            eyebrow = "3 / 3  CONFIRM",
            title = "Lock in your route",
            subtitle = "These stations stay fixed so the app can be ready as soon as you open it."
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                SavedStation("HOME", homeStationCrs)
                Box(
                    Modifier
                        .padding(start = 12.dp)
                        .width(2.dp)
                        .height(28.dp)
                        .background(C2CPrimary.copy(alpha = 0.5f))
                )
                SavedStation("DESTINATION", workStationCrs)
            }
        }

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
                    onCheckedChange = onFilterChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = C2CPrimary
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "To change either station later, open Settings and choose “Set up my route again”.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SavedStation(label: String, crs: String) {
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
            Text(Stations.getStationName(crs), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(crs, style = MaterialTheme.typography.labelMedium, color = C2CPrimary)
        }
    }
}

private enum class SetupStep {
    WELCOME,
    HOME_STATION,
    WORK_STATION,
    CONFIRM
}
