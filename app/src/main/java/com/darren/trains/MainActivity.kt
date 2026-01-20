package com.darren.trains

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darren.trains.ui.screens.HomeScreen
import com.darren.trains.ui.theme.DarrensTrainsTheme
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
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Location permission
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Request permission and load data on launch
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Load data when permission state changes or on initial launch
    LaunchedEffect(locationPermissionState.status.isGranted) {
        viewModel.loadData(hasLocationPermission = locationPermissionState.status.isGranted)
    }

    HomeScreen(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onToggleDirection = { viewModel.toggleDirection() },
        modifier = Modifier.fillMaxSize()
    )
}
