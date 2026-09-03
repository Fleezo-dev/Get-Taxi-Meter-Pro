package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taxi_meter_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_BASE_FARE = doublePreferencesKey("base_fare")
        val KEY_FARE_PER_KM = doublePreferencesKey("fare_per_km")
        val KEY_WAIT_FARE_PER_MIN = doublePreferencesKey("wait_fare_per_min")
        val KEY_SPEED_THRESHOLD = doublePreferencesKey("speed_threshold") // in km/h
        val KEY_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val KEY_AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_OUT_OF_CITY_SURCHARGE_TYPE = stringPreferencesKey("out_of_city_surcharge_type") // "FIXED_AMOUNT" or "PERCENTAGE"
        val KEY_OUT_OF_CITY_SURCHARGE_FIXED_AMOUNT = doublePreferencesKey("out_of_city_surcharge_fixed_amount")
        val KEY_OUT_OF_CITY_SURCHARGE_PERCENT = doublePreferencesKey("out_of_city_surcharge_percent")
        val KEY_ILAIYARAAJA_RINGTONE = stringPreferencesKey("ilaiyaraaja_ringtone")
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_TTS_VOICE_PROFILE = stringPreferencesKey("tts_voice_profile")
        val KEY_TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val KEY_TTS_PITCH = floatPreferencesKey("tts_pitch")
        val KEY_FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")

        // Expanded Tariff Settings Keys
        val KEY_RENTAL_BASE_HOURS = doublePreferencesKey("rental_base_hours")
        val KEY_RENTAL_EXTRA_KM_RATE = doublePreferencesKey("rental_extra_km_rate")
        val KEY_RENTAL_EXTRA_HOUR_RATE = doublePreferencesKey("rental_extra_hour_rate")
        val KEY_OUTSTATION_TRIP_TYPE = stringPreferencesKey("outstation_trip_type") // "ROUNDTRIP" or "ONEWAY"
        val KEY_OUTSTATION_DRIVER_BETA = doublePreferencesKey("outstation_driver_beta")
        val KEY_OUTSTATION_MIN_KM = doublePreferencesKey("outstation_min_km")
        val KEY_OUTSTATION_PER_KM = doublePreferencesKey("outstation_per_km")
        val KEY_WAITING_FREE_MINUTES = intPreferencesKey("waiting_free_minutes")
    }

    val floatingBubbleEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FLOATING_BUBBLE_ENABLED] ?: true // Default to true so minimized app shows bubble
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_DARK_MODE] ?: false // Default to Light Mode layout matching Taxi Meter Pro design
    }

    val ttsVoiceProfile: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TTS_VOICE_PROFILE] ?: "FEMALE_1"
    }

    val ttsSpeechRate: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_TTS_SPEECH_RATE] ?: 1.0f
    }

    val ttsPitch: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_TTS_PITCH] ?: 1.15f
    }

    val ilaiyaraajaRingtone: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ILAIYARAAJA_RINGTONE] ?: "ACCORDION_GROOVE"
    }

    // Default configuration values
    val baseFare: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASE_FARE] ?: 80.00
    }

    val farePerKm: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_FARE_PER_KM] ?: 28.00
    }

    val waitFarePerMin: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_WAIT_FARE_PER_MIN] ?: 2.00
    }

    val speedThreshold: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_SPEED_THRESHOLD] ?: 5.0
    }

    val audioEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_ENABLED] ?: true
    }

    val autoStartEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_START_ENABLED] ?: false
    }

    val currency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY] ?: "₹"
    }

    val outOfCitySurchargeType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUT_OF_CITY_SURCHARGE_TYPE] ?: "FIXED_AMOUNT"
    }

    val outOfCitySurchargeFixedAmount: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUT_OF_CITY_SURCHARGE_FIXED_AMOUNT] ?: 50.0
    }

    val outOfCitySurchargePercent: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUT_OF_CITY_SURCHARGE_PERCENT] ?: 25.0
    }

    val rentalBaseHours: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_BASE_HOURS] ?: 1.0
    }

    val rentalExtraKmRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_EXTRA_KM_RATE] ?: 25.0
    }

    val rentalExtraHourRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_RENTAL_EXTRA_HOUR_RATE] ?: 350.0
    }

    val outstationTripType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_TRIP_TYPE] ?: "ROUNDTRIP"
    }

    val outstationDriverBeta: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_DRIVER_BETA] ?: 500.0
    }

    val outstationMinKm: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_MIN_KM] ?: 250.0
    }

    val outstationPerKmRate: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_OUTSTATION_PER_KM] ?: 15.0
    }

    val waitingFreeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_WAITING_FREE_MINUTES] ?: 5
    }

    // Modern setter functions
    suspend fun updateBaseFare(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASE_FARE] = value
        }
    }

    suspend fun updateFarePerKm(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FARE_PER_KM] = value
        }
    }

    suspend fun updateWaitFarePerMin(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WAIT_FARE_PER_MIN] = value
        }
    }

    suspend fun updateSpeedThreshold(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SPEED_THRESHOLD] = value
        }
    }

    suspend fun updateAudioEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_ENABLED] = value
        }
    }

    suspend fun updateAutoStartEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_ENABLED] = value
        }
    }

    suspend fun updateCurrency(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY] = value
        }
    }

    suspend fun updateOutOfCitySurchargeType(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUT_OF_CITY_SURCHARGE_TYPE] = value
        }
    }

    suspend fun updateOutOfCitySurchargeFixedAmount(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUT_OF_CITY_SURCHARGE_FIXED_AMOUNT] = value
        }
    }

    suspend fun updateOutOfCitySurchargePercent(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUT_OF_CITY_SURCHARGE_PERCENT] = value
        }
    }

    suspend fun updateIlaiyaraajaRingtone(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ILAIYARAAJA_RINGTONE] = value
        }
    }

    suspend fun updateIsDarkMode(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_MODE] = value
        }
    }

    suspend fun updateTtsVoiceProfile(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TTS_VOICE_PROFILE] = value
        }
    }

    suspend fun updateTtsSpeechRate(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TTS_SPEECH_RATE] = value
        }
    }

    suspend fun updateTtsPitch(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TTS_PITCH] = value
        }
    }

    suspend fun updateFloatingBubbleEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FLOATING_BUBBLE_ENABLED] = value
        }
    }

    suspend fun updateRentalBaseHours(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RENTAL_BASE_HOURS] = value
        }
    }

    suspend fun updateRentalExtraKmRate(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RENTAL_EXTRA_KM_RATE] = value
        }
    }

    suspend fun updateRentalExtraHourRate(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RENTAL_EXTRA_HOUR_RATE] = value
        }
    }

    suspend fun updateOutstationTripType(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUTSTATION_TRIP_TYPE] = value
        }
    }

    suspend fun updateOutstationDriverBeta(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUTSTATION_DRIVER_BETA] = value
        }
    }

    suspend fun updateOutstationMinKm(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUTSTATION_MIN_KM] = value
        }
    }

    suspend fun updateOutstationPerKmRate(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUTSTATION_PER_KM] = value
        }
    }

    suspend fun updateWaitingFreeMinutes(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WAITING_FREE_MINUTES] = value
        }
    }
}
