package com.darren.trains.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darren.trains.data.model.*
import com.darren.trains.data.repository.TrainData
import com.darren.trains.data.repository.TrainRepository
import com.darren.trains.location.LocationHelper
import com.darren.trains.location.LocationResult
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
    private val locationHelper: LocationHelper
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

            // Get location if we have permission
            val locationResult = if (hasLocationPermission) {
                locationHelper.getCurrentLocation()
            } else {
                null
            }

            // Update location state
            _uiState.update { state ->
                state.copy(
                    locationResult = locationResult,
                    // Set direction from location if detected, otherwise keep current or default
                    currentDirection = locationResult?.detectedDirection
                        ?: state.currentDirection
                        ?: TravelDirection.TO_FENCHURCH_STREET,
                    isNearStation = locationResult?.detectedDirection != null
                )
            }

            // Now fetch trains
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
        val direction = _uiState.value.currentDirection ?: TravelDirection.TO_FENCHURCH_STREET

        trainRepository.getDepartures(direction).fold(
            onSuccess = { data ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        trainData = data,
                        error = null,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load train data"
                    )
                }
            }
        )
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
    val lastUpdated: Long = 0
)
