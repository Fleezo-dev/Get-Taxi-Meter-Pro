package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RemoteConfigRepository {
    private const val TAG = "RemoteConfigRepository"
    private const val PREFS_NAME = "remote_config_prefs"

    private val _configFlow = MutableStateFlow(AppConfig())
    val configFlow: StateFlow<AppConfig> = _configFlow.asStateFlow()

    private var sharedPrefs: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadCachedConfig()
    }

    fun getActiveConfig(): AppConfig {
        return _configFlow.value
    }

    private fun loadCachedConfig() {
        val prefs = sharedPrefs ?: return
        val baseHourlyRate = prefs.getFloat("baseHourlyRate", 350.0f).toDouble()
        val includedKmPerHour = prefs.getFloat("includedKmPerHour", 10.0f).toDouble()
        val extraKmRate = prefs.getFloat("extraKmRate", 25.0f).toDouble()
        val minimumRequiredVersion = prefs.getInt("minimumRequiredVersion", 2)
        val telegramSupportUrl = prefs.getString("telegramSupportUrl", "https://t.me/Gettaxikovai") ?: "https://t.me/Gettaxikovai"
        val isActivationRequired = prefs.getBoolean("isActivationRequired", true)

        val cachedConfig = AppConfig(
            baseHourlyRate = baseHourlyRate,
            includedKmPerHour = includedKmPerHour,
            extraKmRate = extraKmRate,
            minimumRequiredVersion = minimumRequiredVersion,
            telegramSupportUrl = telegramSupportUrl,
            isActivationRequired = isActivationRequired
        )
        _configFlow.value = cachedConfig
    }

    private fun saveCachedConfig(config: AppConfig) {
        sharedPrefs?.edit()?.apply {
            putFloat("baseHourlyRate", config.baseHourlyRate.toFloat())
            putFloat("includedKmPerHour", config.includedKmPerHour.toFloat())
            putFloat("extraKmRate", config.extraKmRate.toFloat())
            putInt("minimumRequiredVersion", config.minimumRequiredVersion)
            putString("telegramSupportUrl", config.telegramSupportUrl)
            putBoolean("isActivationRequired", config.isActivationRequired)
            apply()
        }
    }

    /**
     * Updates active config locally in SharedPreferences
     */
    fun updateConfig(newConfig: AppConfig, onComplete: (Boolean) -> Unit = {}) {
        _configFlow.value = newConfig
        saveCachedConfig(newConfig)
        onComplete(true)
    }
}
