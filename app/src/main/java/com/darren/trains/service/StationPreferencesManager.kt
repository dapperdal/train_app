package com.darren.trains.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darren.trains.data.model.RoutePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stationPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "station_preferences")

@Singleton
class StationPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val HOME_STATION_KEY = stringPreferencesKey("home_station_crs")
        private val WORK_STATION_KEY = stringPreferencesKey("work_station_crs")
        private val FILTER_LAKESIDE_KEY = booleanPreferencesKey("filter_lakeside_loop")
        private val SETUP_COMPLETE_KEY = booleanPreferencesKey("setup_complete")
    }

    /**
     * Flow of the current route preference
     */
    val routePreference: Flow<RoutePreference?> = context.stationPreferencesDataStore.data.map { preferences ->
        val homeStation = preferences[HOME_STATION_KEY]
        val workStation = preferences[WORK_STATION_KEY]

        if (homeStation != null && workStation != null) {
            RoutePreference(
                homeStationCrs = homeStation,
                workStationCrs = workStation,
                filterLakesideLoop = preferences[FILTER_LAKESIDE_KEY] ?: true,
                autoDetectLocation = true
            )
        } else {
            null
        }
    }

    /**
     * Check if user has completed setup
     */
    suspend fun hasCompletedSetup(): Boolean {
        return try {
            val prefs = context.stationPreferencesDataStore.data.map { preferences ->
                preferences[SETUP_COMPLETE_KEY] ?: false
            }
            prefs.collect { isComplete ->
                return@hasCompletedSetup isComplete
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Save user's route preference from setup screen
     */
    suspend fun saveRoutePreference(preference: RoutePreference) {
        try {
            context.stationPreferencesDataStore.edit { preferences ->
                preferences[HOME_STATION_KEY] = preference.homeStationCrs
                preferences[WORK_STATION_KEY] = preference.workStationCrs
                preferences[FILTER_LAKESIDE_KEY] = preference.filterLakesideLoop
                preferences[SETUP_COMPLETE_KEY] = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Update just the Lakeside filter preference
     */
    suspend fun setFilterLakesideLoop(enabled: Boolean) {
        try {
            context.stationPreferencesDataStore.edit { preferences ->
                preferences[FILTER_LAKESIDE_KEY] = enabled
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clear all preferences (for reset/reconfiguration)
     */
    suspend fun clearPreferences() {
        try {
            context.stationPreferencesDataStore.edit { preferences ->
                preferences.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
