package com.darren.trains.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darren.trains.data.model.ActiveJourney
import com.darren.trains.data.model.AlertPreference
import com.darren.trains.data.model.ArrivalWeather
import com.darren.trains.data.model.JourneyAlertSettings
import com.darren.trains.data.model.JourneyProgress
import com.darren.trains.data.model.Stations
import com.darren.trains.data.model.TrainDeparture
import com.darren.trains.data.model.TravelDirection
import com.darren.trains.data.repository.JourneyRepository
import com.darren.trains.data.repository.WeatherRepository
import com.darren.trains.service.ArrivalAlertManager
import com.darren.trains.service.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JourneyViewModel @Inject constructor(
    private val journeyRepository: JourneyRepository,
    private val weatherRepository: WeatherRepository,
    private val preferencesManager: PreferencesManager,
    private val arrivalAlertManager: ArrivalAlertManager
) : ViewModel() {

    companion object {
        private const val UPDATE_INTERVAL_MS = 30_000L // 30 seconds
    }

    private val _journeyState = MutableStateFlow<JourneyState>(JourneyState.Idle)
    val journeyState: StateFlow<JourneyState> = _journeyState.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    val alertSettings: StateFlow<JourneyAlertSettings> = preferencesManager.alertSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = JourneyAlertSettings()
        )

    private var updateJob: Job? = null

    /**
     * Start tracking a journey when user clicks "I'm Onboard"
     */
    fun startJourney(trainDeparture: TrainDeparture, direction: TravelDirection) {
        val (originCrs, originName, destCrs, destName) = when (direction) {
            TravelDirection.TO_FENCHURCH_STREET -> listOf(
                Stations.LEIGH_ON_SEA_CRS,
                Stations.LEIGH_ON_SEA_NAME,
                Stations.FENCHURCH_STREET_CRS,
                Stations.FENCHURCH_STREET_NAME
            )
            TravelDirection.TO_LEIGH_ON_SEA -> listOf(
                Stations.FENCHURCH_STREET_CRS,
                Stations.FENCHURCH_STREET_NAME,
                Stations.LEIGH_ON_SEA_CRS,
                Stations.LEIGH_ON_SEA_NAME
            )
        }

        val journey = ActiveJourney(
            serviceId = trainDeparture.serviceId,
            trainDeparture = trainDeparture,
            callingPoints = trainDeparture.callingPoints,
            boardedAt = System.currentTimeMillis(),
            originCrs = originCrs,
            originName = originName,
            destinationCrs = destCrs,
            destinationName = destName
        )

        // Reset alert state for new journey
        arrivalAlertManager.resetForNewJourney()

        // Calculate initial progress
        val progress = journeyRepository.calculateProgress(journey)

        _journeyState.value = JourneyState.Active(
            journey = journey,
            progress = progress
        )

        // Start live updates
        startLiveUpdates(journey)

        // Fetch weather for arrival time
        progress.estimatedArrivalTime?.let { arrivalTime ->
            fetchWeatherAtDestination(arrivalTime)
        }
    }

    /**
     * End the current journey
     */
    fun endJourney() {
        updateJob?.cancel()
        updateJob = null
        _journeyState.value = JourneyState.Idle
        _weatherState.value = WeatherState.Idle
    }

    /**
     * Update alert preference
     */
    fun updateAlertPreference(preference: AlertPreference) {
        viewModelScope.launch {
            preferencesManager.updateAlertPreference(preference)
        }
    }

    /**
     * Toggle 2-minute alert on/off
     */
    fun toggleTwoMinuteAlert(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setTwoMinuteAlertEnabled(enabled)
        }
    }

    /**
     * Start periodic updates for journey progress
     */
    private fun startLiveUpdates(journey: ActiveJourney) {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (true) {
                delay(UPDATE_INTERVAL_MS)

                val currentState = _journeyState.value
                if (currentState !is JourneyState.Active) break

                // Recalculate progress
                val progress = journeyRepository.calculateProgress(currentState.journey)

                // Check for arrival alert
                arrivalAlertManager.checkAndTriggerAlert(progress)

                // Check if journey is complete
                if (progress.hasArrived) {
                    _journeyState.value = JourneyState.Completed(currentState.journey)
                    break
                }

                // Update state with new progress
                _journeyState.value = JourneyState.Active(
                    journey = currentState.journey,
                    progress = progress
                )

                // Update weather if arrival time changed
                if (progress.estimatedArrivalTime != currentState.progress.estimatedArrivalTime) {
                    progress.estimatedArrivalTime?.let { fetchWeatherAtDestination(it) }
                }
            }
        }
    }

    /**
     * Fetch weather forecast for Fenchurch Street at arrival time
     */
    private fun fetchWeatherAtDestination(arrivalTime: String) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading

            val result = weatherRepository.getWeatherAtFenchurchStreet(arrivalTime)

            _weatherState.value = result.fold(
                onSuccess = { weather -> WeatherState.Success(weather) },
                onFailure = { error -> WeatherState.Error(error.message ?: "Unknown error") }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        arrivalAlertManager.release()
    }
}

/**
 * Sealed class representing journey tracking states
 */
sealed class JourneyState {
    object Idle : JourneyState()

    data class Active(
        val journey: ActiveJourney,
        val progress: JourneyProgress
    ) : JourneyState()

    data class Completed(
        val journey: ActiveJourney
    ) : JourneyState()
}

/**
 * Sealed class representing weather fetch states
 */
sealed class WeatherState {
    object Idle : WeatherState()
    object Loading : WeatherState()
    data class Success(val weather: ArrivalWeather) : WeatherState()
    data class Error(val message: String) : WeatherState()
}
