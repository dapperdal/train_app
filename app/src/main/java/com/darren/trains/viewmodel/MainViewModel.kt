package com.darren.trains.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darren.trains.data.model.*
import com.darren.trains.data.repository.TrainData
import com.darren.trains.data.repository.TrainRepository
import com.darren.trains.location.LocationHelper
import com.darren.trains.location.LocationResult
import com.darren.trains.service.StationPreferencesManager
import com.darren.trains.service.RouteAlertManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val trainRepository: TrainRepository,
    private val locationHelper: LocationHelper,
    private val stationPreferencesManager: StationPreferencesManager,
    private val routeAlertManager: RouteAlertManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainUiState())
    val uiState: StateFlow<TrainUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    fun loadData(hasLocationPermission: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load user's extended route preference with all advanced settings
            val extendedPreference = stationPreferencesManager.getExtendedPreferences()

            // Get location if we have permission
            val locationResult = if (hasLocationPermission) {
                locationHelper.getCurrentLocation()
            } else {
                null
            }

            val primaryRoute = extendedPreference?.primaryRoute
            val inferredOriginCrs = if (primaryRoute != null && locationResult != null) {
                val homeDistance = locationHelper.distanceToStation(locationResult, primaryRoute.homeStationCrs)
                val workDistance = locationHelper.distanceToStation(locationResult, primaryRoute.workStationCrs)
                when {
                    homeDistance == null -> primaryRoute.workStationCrs
                    workDistance == null -> primaryRoute.homeStationCrs
                    homeDistance <= workDistance -> primaryRoute.homeStationCrs
                    else -> primaryRoute.workStationCrs
                }
            } else {
                primaryRoute?.homeStationCrs
            }

            val inferredOriginDistance = if (locationResult != null && inferredOriginCrs != null) {
                locationHelper.distanceToStation(locationResult, inferredOriginCrs)
            } else {
                null
            }
            val isNearSavedStation = locationResult != null &&
                locationResult.accuracyMeters <= LocationHelper.MAX_ACCEPTABLE_ACCURACY_METRES &&
                inferredOriginDistance != null &&
                inferredOriginDistance <= LocationHelper.NEAR_STATION_THRESHOLD_MILES

            // Update location state
            _uiState.update { state ->
                state.copy(
                    locationResult = locationResult,
                    // Set direction from location if detected, otherwise keep current or default
                    currentDirection = locationResult?.detectedDirection
                        ?: state.currentDirection
                        ?: TravelDirection.TO_FENCHURCH_STREET,
                    isNearStation = isNearSavedStation,
                    currentStationCrs = inferredOriginCrs,
                    extendedPreference = extendedPreference
                )
            }

            // Now fetch trains with smart routing
            fetchTrains()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetchTrains()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun browseForward() {
        val nextOffset = when (_uiState.value.browseOffsetMinutes) {
            0 -> 60
            else -> 119
        }
        if (nextOffset == _uiState.value.browseOffsetMinutes) return
        _uiState.update { it.copy(browseOffsetMinutes = nextOffset, isLoading = true) }
        viewModelScope.launch { fetchTrains() }
    }

    fun browseBack() {
        val previousOffset = when (_uiState.value.browseOffsetMinutes) {
            119 -> 60
            else -> 0
        }
        if (previousOffset == _uiState.value.browseOffsetMinutes) return
        _uiState.update { it.copy(browseOffsetMinutes = previousOffset, isLoading = true) }
        viewModelScope.launch { fetchTrains() }
    }

    fun toggleDirection() {
        _uiState.update { state ->
            val newDirection = when (state.currentDirection) {
                TravelDirection.TO_FENCHURCH_STREET -> TravelDirection.TO_LEIGH_ON_SEA
                TravelDirection.TO_LEIGH_ON_SEA -> TravelDirection.TO_FENCHURCH_STREET
                null -> TravelDirection.TO_FENCHURCH_STREET
            }
            state.copy(currentDirection = newDirection)
        }
        // Fetch trains for new direction
        viewModelScope.launch {
            fetchTrains()
        }
    }

    private suspend fun fetchTrains() {
        val state = _uiState.value
        val extendedPreference = state.extendedPreference
        val currentStationCrs = state.currentStationCrs

        // If user has route preference configured, use smart routing
        if (extendedPreference != null && currentStationCrs != null) {
            // Smart routing: determine destination based on current station
            val primaryRoute = extendedPreference.primaryRoute
            val destinationCrs = when (currentStationCrs) {
                primaryRoute.homeStationCrs -> primaryRoute.workStationCrs
                primaryRoute.workStationCrs -> primaryRoute.homeStationCrs
                else -> primaryRoute.workStationCrs  // Default to work if at other station
            }

            trainRepository.getDepartures(
                fromCrs = currentStationCrs,
                toCrs = destinationCrs,
                filterLakesideLoop = extendedPreference.filterLakesideLoop,
                preferExpressTrains = extendedPreference.preferExpressTrains,
                preferredOperator = extendedPreference.preferredOperator,
                maxChangeTime = extendedPreference.maxChangeTime,
                timeOffsetMinutes = state.browseOffsetMinutes
            ).fold(
                onSuccess = { data ->
                    if (extendedPreference.notifyOnDelay) {
                        routeAlertManager.notifyIfNeeded(data, primaryRoute)
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            trainData = data,
                            error = null,
                            lastUpdated = data.retrievedAt
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "Live c2c information is temporarily unavailable"
                        )
                    }
                }
            )
        } else {
            // Fallback to old direction-based logic if no route preference
            val direction = state.currentDirection ?: TravelDirection.TO_FENCHURCH_STREET

            trainRepository.getDepartures(direction).fold(
                onSuccess = { data ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            trainData = data,
                            error = null,
                            lastUpdated = data.retrievedAt
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "Live c2c information is temporarily unavailable"
                        )
                    }
                }
            )
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                if (_uiState.value.trainData != null) {
                    fetchTrains()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 60_000L // 60 seconds
    }
}

data class TrainUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val trainData: TrainData? = null,
    val error: String? = null,
    val locationResult: LocationResult? = null,
    val currentDirection: TravelDirection? = null,
    val isNearStation: Boolean = false,
    val lastUpdated: Long = 0,
    val currentStationCrs: String? = null,  // Detected current station
    val browseOffsetMinutes: Int = 0,
    val extendedPreference: ExtendedRoutePreference? = null  // User's saved preferences with advanced settings
)
