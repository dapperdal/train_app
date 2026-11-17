package com.darren.trains

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darren.trains.service.StationPreferencesManager
import com.darren.trains.ui.screens.HomeScreen
import com.darren.trains.ui.screens.SetupScreen
import com.darren.trains.ui.screens.SettingsScreen
import com.darren.trains.ui.theme.DarrensTrainsTheme
import com.darren.trains.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

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
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val setupComplete = remember { mutableStateOf<Boolean?>(null) }
    val showSettings = remember { mutableStateOf(false) }
    val showRouteSetup = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get StationPreferencesManager from context
    val context = androidx.compose.ui.platform.LocalContext.current

    // Location permission
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    // Check if setup is complete on launch
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val preferencesManager = StationPreferencesManager(context)
            setupComplete.value = preferencesManager.hasCompletedSetup()
        }
    }

    // Keep the first-run experience focused; ask for location only after the route is saved.
    LaunchedEffect(setupComplete.value) {
        if (setupComplete.value == true && !locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Load data when permission state changes or on initial launch (only after setup)
    LaunchedEffect(locationPermissionState.status.isGranted, setupComplete.value) {
        if (setupComplete.value == true) {
            mainViewModel.loadData(hasLocationPermission = locationPermissionState.status.isGranted)
        }
    }

    // Display content based on setup status and journey state
    when (setupComplete.value) {
        null -> {
            // Still loading setup status
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        false -> {
            // Show setup screen
            SetupScreen(
                onSetupComplete = { routePreference ->
                    // Save preferences and reload
                    coroutineScope.launch {
                        val preferencesManager = StationPreferencesManager(context)
                        preferencesManager.saveRoutePreference(routePreference)
                        setupComplete.value = true
                        mainViewModel.loadData(hasLocationPermission = locationPermissionState.status.isGranted)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        true -> {
            when {
                showRouteSetup.value -> {
                    SetupScreen(
                        initialPreference = uiState.extendedPreference?.primaryRoute,
                        isReconfiguration = true,
                        onCancel = { showRouteSetup.value = false },
                        onSetupComplete = { routePreference ->
                            coroutineScope.launch {
                                val preferencesManager = StationPreferencesManager(context)
                                preferencesManager.saveRoutePreference(routePreference)
                                mainViewModel.loadData(hasLocationPermission = locationPermissionState.status.isGranted)
                                showRouteSetup.value = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                showSettings.value -> {
                    SettingsScreen(
                        currentPreference = uiState.extendedPreference?.primaryRoute,
                        currentNotifyOnDelay = uiState.extendedPreference?.notifyOnDelay ?: false,
                        currentPreferExpress = uiState.extendedPreference?.preferExpressTrains ?: false,
                        onBackClick = { showSettings.value = false },
                        onPreferenceUpdate = { newPreference ->
                            coroutineScope.launch {
                                val preferencesManager = StationPreferencesManager(context)
                                preferencesManager.saveRoutePreference(newPreference)
                                mainViewModel.loadData(hasLocationPermission = locationPermissionState.status.isGranted)
                            }
                        },
                        onAdvancedSettingsUpdate = { notifyOnDelay, preferExpress ->
                            coroutineScope.launch {
                                val preferencesManager = StationPreferencesManager(context)
                                preferencesManager.updateAdvancedSettings(
                                    notifyOnDelay = notifyOnDelay,
                                    preferExpress = preferExpress,
                                    preferredOperator = uiState.extendedPreference?.preferredOperator,
                                    maxChangeTime = uiState.extendedPreference?.maxChangeTime ?: 15
                                )
                                mainViewModel.loadData(hasLocationPermission = locationPermissionState.status.isGranted)
                            }
                        },
                        onRouteAlertsEnabled = {
                            notificationPermissionState?.let { permission ->
                                if (!permission.status.isGranted) permission.launchPermissionRequest()
                            }
                        },
                        onRedoSetup = {
                            showSettings.value = false
                            showRouteSetup.value = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    HomeScreen(
                        uiState = uiState,
                        onRefresh = { mainViewModel.refresh() },
                        onToggleDirection = { mainViewModel.toggleDirection() },
                        onBrowseBack = { mainViewModel.browseBack() },
                        onBrowseForward = { mainViewModel.browseForward() },
                        onSettingsClick = { showSettings.value = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
