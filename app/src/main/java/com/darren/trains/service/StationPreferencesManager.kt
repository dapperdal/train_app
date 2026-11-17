package com.darren.trains.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darren.trains.data.model.ExtendedRoutePreference
import com.darren.trains.data.model.FavoriteRoute
import com.darren.trains.data.model.RoutePreference
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stationPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "station_preferences")

@Singleton
class StationPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson? = null
) {
    companion object {
        private val HOME_STATION_KEY = stringPreferencesKey("home_station_crs")
        private val WORK_STATION_KEY = stringPreferencesKey("work_station_crs")
        private val FILTER_LAKESIDE_KEY = booleanPreferencesKey("filter_lakeside_loop")
        private val SETUP_COMPLETE_KEY = booleanPreferencesKey("setup_complete")

        // Advanced settings keys
        private val FAVORITE_ROUTES_KEY = stringPreferencesKey("favorite_routes_json")
        private val NOTIFY_DELAY_KEY = booleanPreferencesKey("notify_on_delay")
        private val PREFER_EXPRESS_KEY = booleanPreferencesKey("prefer_express_trains")
        private val PREFERRED_OPERATOR_KEY = stringPreferencesKey("preferred_operator")
        private val MAX_CHANGE_TIME_KEY = stringPreferencesKey("max_change_time")
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
            context.stationPreferencesDataStore.data.first()[SETUP_COMPLETE_KEY] ?: false
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

    /**
     * Add a new favorite route
     */
    suspend fun addFavoriteRoute(route: FavoriteRoute) {
        try {
            context.stationPreferencesDataStore.edit { preferences ->
                val currentJson = preferences[FAVORITE_ROUTES_KEY] ?: "[]"
                val currentRoutes = try {
                    gson?.fromJson(currentJson, Array<FavoriteRoute>::class.java)?.toMutableList() ?: mutableListOf()
                } catch (e: Exception) {
                    mutableListOf()
                }

                // Remove existing route with same ID
                currentRoutes.removeAll { it.id == route.id }
                currentRoutes.add(route)

                preferences[FAVORITE_ROUTES_KEY] = gson?.toJson(currentRoutes) ?: "[]"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get all favorite routes
     */
    suspend fun getFavoriteRoutes(): List<FavoriteRoute> {
        return try {
            val json = context.stationPreferencesDataStore.data.first()[FAVORITE_ROUTES_KEY] ?: "[]"
            try {
                gson?.fromJson(json, Array<FavoriteRoute>::class.java)?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Update advanced preferences
     */
    suspend fun updateAdvancedSettings(
        notifyOnDelay: Boolean = false,
        preferExpress: Boolean = false,
        preferredOperator: String? = null,
        maxChangeTime: Int = 15
    ) {
        try {
            context.stationPreferencesDataStore.edit { preferences ->
                preferences[NOTIFY_DELAY_KEY] = notifyOnDelay
                preferences[PREFER_EXPRESS_KEY] = preferExpress
                if (preferredOperator != null) {
                    preferences[PREFERRED_OPERATOR_KEY] = preferredOperator
                }
                preferences[MAX_CHANGE_TIME_KEY] = maxChangeTime.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get extended preferences with all advanced settings
     */
    suspend fun getExtendedPreferences(): ExtendedRoutePreference? {
        return try {
            val preferences = context.stationPreferencesDataStore.data.first()
            val homeStation = preferences[HOME_STATION_KEY]
            val workStation = preferences[WORK_STATION_KEY]

            if (homeStation != null && workStation != null) {
                val favorites = try {
                    val json = preferences[FAVORITE_ROUTES_KEY] ?: "[]"
                    gson?.fromJson(json, Array<FavoriteRoute>::class.java)?.toList() ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                ExtendedRoutePreference(
                    primaryRoute = RoutePreference(
                        homeStationCrs = homeStation,
                        workStationCrs = workStation,
                        filterLakesideLoop = preferences[FILTER_LAKESIDE_KEY] ?: true
                    ),
                    favoriteRoutes = favorites,
                    filterLakesideLoop = preferences[FILTER_LAKESIDE_KEY] ?: true,
                    notifyOnDelay = preferences[NOTIFY_DELAY_KEY] ?: false,
                    preferExpressTrains = preferences[PREFER_EXPRESS_KEY] ?: false,
                    preferredOperator = preferences[PREFERRED_OPERATOR_KEY],
                    maxChangeTime = preferences[MAX_CHANGE_TIME_KEY]?.toIntOrNull() ?: 15
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
