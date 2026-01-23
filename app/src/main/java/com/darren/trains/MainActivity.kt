package com.darren.trains

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darren.trains.ui.screens.HomeScreen
import com.darren.trains.ui.screens.JourneyCompletedScreen
import com.darren.trains.ui.screens.JourneyTrackingScreen
import com.darren.trains.ui.theme.DarrensTrainsTheme
import com.darren.trains.viewmodel.JourneyState
import com.darren.trains.viewmodel.JourneyViewModel
import com.darren.trains.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DarrensTrainsTheme {
                MainContent()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MainContent(
    mainViewModel: MainViewModel = hiltViewModel(),
    journeyViewModel: JourneyViewModel = hiltViewModel()
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val journeyState by journeyViewModel.journeyState.collectAsStateWithLifecycle()
    val weatherState by journeyViewModel.weatherState.collectAsStateWithLifecycle()
    val alertSettings by journeyViewModel.alertSettings.collectAsStateWithLifecycle()

    // Location permission
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Notification permission for Android 13+
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    // Request location permission on launch
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Load data when permission state changes or on initial launch
    LaunchedEffect(locationPermissionState.status.isGranted) {
        mainViewModel.loadData(hasLocationPermission = locationPermissionState.status.isGranted)
    }

    // Display content based on journey state
    when (val state = journeyState) {
        is JourneyState.Idle -> {
            HomeScreen(
                uiState = uiState,
                onRefresh = { mainViewModel.refresh() },
                onToggleDirection = { mainViewModel.toggleDirection() },
                onBoardTrain = { train ->
                    // Request notification permission when boarding
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionState?.let {
                            if (!it.status.isGranted) {
                                it.launchPermissionRequest()
                            }
                        }
                    }
                    // Start journey tracking
                    uiState.currentDirection?.let { direction ->
                        journeyViewModel.startJourney(train, direction)
                    }
                },
                activeJourneyServiceId = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        is JourneyState.Active -> {
            JourneyTrackingScreen(
                journey = state.journey,
                progress = state.progress,
                weatherState = weatherState,
                alertSettings = alertSettings,
                onEndJourney = { journeyViewModel.endJourney() },
                onToggleAlert = { enabled -> journeyViewModel.toggleTwoMinuteAlert(enabled) },
                onChangeAlertPreference = { pref -> journeyViewModel.updateAlertPreference(pref) },
                modifier = Modifier.fillMaxSize()
            )
        }

        is JourneyState.Completed -> {
            JourneyCompletedScreen(
                journey = state.journey,
                onDismiss = { journeyViewModel.endJourney() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
