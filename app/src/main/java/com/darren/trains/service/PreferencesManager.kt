package com.darren.trains.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darren.trains.data.model.AlertPreference
import com.darren.trains.data.model.JourneyAlertSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "journey_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val ALERT_PREFERENCE_KEY = stringPreferencesKey("alert_preference")
        private val TWO_MIN_ALERT_KEY = booleanPreferencesKey("two_min_alert_enabled")
    }

    /**
     * Flow of current alert settings
     */
    val alertSettings: Flow<JourneyAlertSettings> = dataStore.data.map { preferences ->
        val preferenceStr = preferences[ALERT_PREFERENCE_KEY] ?: AlertPreference.AUDIO_IF_BLUETOOTH.name
        val alertPreference = try {
            AlertPreference.valueOf(preferenceStr)
        } catch (e: Exception) {
            AlertPreference.AUDIO_IF_BLUETOOTH
        }

        val twoMinuteAlertEnabled = preferences[TWO_MIN_ALERT_KEY] ?: true

        JourneyAlertSettings(
            twoMinuteAlertEnabled = twoMinuteAlertEnabled,
            alertPreference = alertPreference
        )
    }

    /**
     * Update the alert preference (audio vs vibrate)
     */
    suspend fun updateAlertPreference(preference: AlertPreference) {
        dataStore.edit { preferences ->
            preferences[ALERT_PREFERENCE_KEY] = preference.name
        }
    }

    /**
     * Enable or disable the 2-minute arrival alert
     */
    suspend fun setTwoMinuteAlertEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TWO_MIN_ALERT_KEY] = enabled
        }
    }
}
