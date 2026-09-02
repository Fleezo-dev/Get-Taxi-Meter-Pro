package com.example.util

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Calendar
import java.util.Locale

data class TtsVoiceOption(
    val id: String,
    val name: String,
    val gender: String,
    val description: String,
    val defaultPitch: Float,
    val defaultSpeechRate: Float
)

/**
 * Text-to-Speech (TTS) Manager for Get Taxi Meter.
 * Provides rich voice profiles (Male 1, Male 2, Female 1, Female 2),
 * automated trip lifecycle greetings (Day/Night contextual speech),
 * and audio settings integration.
 */
class TtsManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TtsManager"

        val VOICE_PROFILES = listOf(
            TtsVoiceOption(
                id = "FEMALE_1",
                name = "Female 1 (Natural Warm)",
                gender = "Female",
                description = "Warm, natural feminine voice with balanced cadence",
                defaultPitch = 1.15f,
                defaultSpeechRate = 1.0f
            ),
            TtsVoiceOption(
                id = "FEMALE_2",
                name = "Female 2 (Clear Studio)",
                gender = "Female",
                description = "Crisp, bright feminine tone for clear announcements",
                defaultPitch = 1.25f,
                defaultSpeechRate = 1.05f
            ),
            TtsVoiceOption(
                id = "MALE_1",
                name = "Male 1 (Deep Radio)",
                gender = "Male",
                description = "Deep, resonant masculine tone with steady pace",
                defaultPitch = 0.82f,
                defaultSpeechRate = 0.95f
            ),
            TtsVoiceOption(
                id = "MALE_2",
                name = "Male 2 (Dynamic Pro)",
                gender = "Male",
                description = "Energetic, clear masculine tone optimized for vehicle interiors",
                defaultPitch = 0.92f,
                defaultSpeechRate = 1.05f
            )
        )

        @Volatile
        private var INSTANCE: TtsManager? = null

        fun getInstance(context: Context): TtsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TtsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentProfile: String = "FEMALE_1"
    private var currentPitch: Float = 1.15f
    private var currentSpeechRate: Float = 1.0f
    private var isAudioEnabled: Boolean = true

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isInitialized = true
                applyVoiceSettings(currentProfile, currentPitch, currentSpeechRate)
                Log.d(TAG, "TTS Engine Initialized successfully with language: ${Locale.getDefault()}")
            } else {
                Log.e(TAG, "TTS Engine Initialization failed with status: $status")
                isInitialized = false
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS Started utterance: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS Completed utterance: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS Error playing utterance: $utteranceId")
            }
        })
    }

    fun setAudioEnabled(enabled: Boolean) {
        isAudioEnabled = enabled
    }

    fun applyVoiceSettings(profileId: String, pitch: Float? = null, rate: Float? = null) {
        currentProfile = profileId
        val profile = VOICE_PROFILES.find { it.id == profileId } ?: VOICE_PROFILES[0]
        currentPitch = pitch ?: profile.defaultPitch
        currentSpeechRate = rate ?: profile.defaultSpeechRate

        tts?.let { engine ->
            engine.setPitch(currentPitch)
            engine.setSpeechRate(currentSpeechRate)

            // Attempt to select specific system voice if available
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val availableVoices = engine.voices
                    if (!availableVoices.isNullOrEmpty()) {
                        val matchingVoice = findBestMatchingVoice(availableVoices, profile.gender)
                        if (matchingVoice != null) {
                            engine.voice = matchingVoice
                            Log.d(TAG, "Selected system voice: ${matchingVoice.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set custom system voice object: ${e.message}")
            }
        }
    }

    private fun findBestMatchingVoice(voices: Set<Voice>, targetGender: String): Voice? {
        val defaultLocale = Locale.getDefault()
        val langVoices = voices.filter {
            it.locale.language == defaultLocale.language || it.locale.language == "en"
        }

        val genderKeyword = if (targetGender.equals("Female", ignoreCase = true)) "female" else "male"
        return langVoices.firstOrNull { voice ->
            voice.name.contains(genderKeyword, ignoreCase = true) ||
            voice.features?.any { it.contains(genderKeyword, ignoreCase = true) } == true
        } ?: langVoices.firstOrNull { !it.isNetworkConnectionRequired } ?: langVoices.firstOrNull()
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isAudioEnabled) {
            Log.d(TAG, "Audio announcements disabled in settings. Skipping: $text")
            return
        }

        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet. Re-attempting init...")
            initializeTts()
            return
        }

        try {
            val utteranceId = "utterance_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(text, queueMode, params, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error during TTS speak", e)
        }
    }

    /**
     * Mandatory Trip Start Speech Trigger:
     * Says: "Welcome to Get Taxi. Please wear the seat belt."
     */
    fun speakTripStart() {
        speak("Welcome to Get Taxi. Please wear the seat belt.")
    }

    /**
     * Mandatory Trip End Speech Trigger:
     * Evaluates the current device time:
     * - Daytime (05:00 - 17:59): "Thank you for traveling with us. Have a good day."
     * - Nighttime (18:00 - 04:59): "Thank you for traveling with us. Have a good night."
     */
    fun speakTripEnd() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = if (hour in 5..17) {
            "Thank you for traveling with us. Have a good day."
        } else {
            "Thank you for traveling with us. Have a good night."
        }
        speak(greeting)
    }

    /**
     * Test voice sample for Settings Screen voice configuration.
     */
    fun testVoice(profileId: String, pitch: Float? = null, rate: Float? = null) {
        applyVoiceSettings(profileId, pitch, rate)
        speak("Welcome to Get Taxi. Please wear the seat belt.")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
