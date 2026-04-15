package com.darren.trains.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darren.trains.data.model.ActiveJourney
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "journeys")

@Singleton
class JourneyPersistenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private val ACTIVE_JOURNEY_KEY = stringPreferencesKey("active_journey")
    }

    /**
     * Save the active journey to persistent storage
     */
    suspend fun saveActiveJourney(journey: ActiveJourney) {
        try {
            val journeyJson = gson.toJson(journey)
            context.dataStore.edit { preferences ->
                preferences[ACTIVE_JOURNEY_KEY] = journeyJson
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }

    /**
     * Load the active journey from persistent storage
     */
    suspend fun loadActiveJourney(): ActiveJourney? {
        return try {
            val journeyJson = context.dataStore.data.map { preferences ->
                preferences[ACTIVE_JOURNEY_KEY]
            }.first()

            if (journeyJson != null) {
                gson.fromJson(journeyJson, ActiveJourney::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
            null
        }
    }

    /**
     * Clear the saved journey
     */
    suspend fun clearActiveJourney() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(ACTIVE_JOURNEY_KEY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
