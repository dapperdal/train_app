package com.darren.trains.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.darren.trains.data.model.AlertPreference
import com.darren.trains.data.model.JourneyAlertSettings
import com.darren.trains.data.model.JourneyProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArrivalAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothHelper: BluetoothHelper,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "ArrivalAlertManager"
        private const val ALERT_THRESHOLD_MINUTES = 2
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var hasAlertedForCurrentJourney = false

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    init {
        initializeTts()
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.UK)
                isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                Log.d(TAG, "TTS initialized: $isTtsInitialized")
            } else {
                Log.e(TAG, "TTS initialization failed")
                isTtsInitialized = false
            }
        }
    }

    /**
     * Check if we should trigger the arrival alert and do so if needed
     * @return true if alert was triggered
     */
    fun checkAndTriggerAlert(progress: JourneyProgress): Boolean {
        if (hasAlertedForCurrentJourney) {
            return false
        }

        val minutesToArrival = progress.minutesToArrival ?: return false

        if (minutesToArrival <= ALERT_THRESHOLD_MINUTES && minutesToArrival > 0) {
            triggerTwoMinuteAlert()
            hasAlertedForCurrentJourney = true
            return true
        }

        return false
    }

    /**
     * Trigger the 2-minute arrival alert based on user preferences
     */
    fun triggerTwoMinuteAlert() {
        val settings = runBlocking { preferencesManager.alertSettings.first() }

        if (!settings.twoMinuteAlertEnabled) {
            Log.d(TAG, "Alert disabled by user preference")
            return
        }

        when (settings.alertPreference) {
            AlertPreference.AUDIO_IF_BLUETOOTH -> {
                if (bluetoothHelper.isBluetoothAudioConnected()) {
                    playTtsAlert()
                } else {
                    triggerVibration()
                }
            }
            AlertPreference.SILENT_VIBRATE -> {
                triggerVibration()
            }
        }
    }

    /**
     * Play text-to-speech announcement
     */
    private fun playTtsAlert() {
        if (!isTtsInitialized) {
            Log.w(TAG, "TTS not initialized, falling back to vibration")
            triggerVibration()
            return
        }

        val message = "Arriving in 2 minutes. Please prepare to leave the train."

        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "arrival_alert"
        )

        Log.d(TAG, "TTS alert played")
    }

    /**
     * Trigger vibration pattern for arrival alert
     */
    private fun triggerVibration() {
        if (!vibrator.hasVibrator()) {
            Log.w(TAG, "Device does not have vibrator")
            return
        }

        // Vibration pattern: vibrate, pause, vibrate, pause, vibrate
        // Creates a noticeable pattern for the alert
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }

        Log.d(TAG, "Vibration alert triggered")
    }

    /**
     * Reset alert state for a new journey
     */
    fun resetForNewJourney() {
        hasAlertedForCurrentJourney = false
        Log.d(TAG, "Alert state reset for new journey")
    }

    /**
     * Check if alert has already been triggered for current journey
     */
    fun hasAlerted(): Boolean = hasAlertedForCurrentJourney

    /**
     * Clean up resources
     */
    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsInitialized = false
    }
}
