package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val baseFare: StateFlow<Double> = settingsRepository.baseFare
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80.00)

    val farePerKm: StateFlow<Double> = settingsRepository.farePerKm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28.00)

    val waitFarePerMin: StateFlow<Double> = settingsRepository.waitFarePerMin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.00)

    val speedThreshold: StateFlow<Double> = settingsRepository.speedThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5.0)

    val audioEnabled: StateFlow<Boolean> = settingsRepository.audioEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoStartEnabled: StateFlow<Boolean> = settingsRepository.autoStartEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹")

    val outOfCitySurchargeType: StateFlow<String> = settingsRepository.outOfCitySurchargeType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FIXED_AMOUNT")

    val outOfCitySurchargeFixedAmount: StateFlow<Double> = settingsRepository.outOfCitySurchargeFixedAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50.0)

    val outOfCitySurchargePercent: StateFlow<Double> = settingsRepository.outOfCitySurchargePercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25.0)

    val ilaiyaraajaRingtone: StateFlow<String> = settingsRepository.ilaiyaraajaRingtone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ACCORDION_GROOVE")

    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val ttsVoiceProfile: StateFlow<String> = settingsRepository.ttsVoiceProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FEMALE_1")

    val ttsSpeechRate: StateFlow<Float> = settingsRepository.ttsSpeechRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val ttsPitch: StateFlow<Float> = settingsRepository.ttsPitch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.15f)

    val floatingBubbleEnabled: StateFlow<Boolean> = settingsRepository.floatingBubbleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun updateBaseFare(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateBaseFare(value)
        }
    }

    fun updateFarePerKm(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateFarePerKm(value)
        }
    }

    fun updateWaitFarePerMin(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateWaitFarePerMin(value)
        }
    }

    fun updateSpeedThreshold(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateSpeedThreshold(value)
        }
    }

    fun updateAudioEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAudioEnabled(value)
        }
    }

    fun updateAutoStartEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoStartEnabled(value)
        }
    }

    fun updateCurrency(value: String) {
        viewModelScope.launch {
            settingsRepository.updateCurrency(value)
        }
    }

    fun updateOutOfCitySurchargeType(value: String) {
        viewModelScope.launch {
            settingsRepository.updateOutOfCitySurchargeType(value)
        }
    }

    fun updateOutOfCitySurchargeFixedAmount(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateOutOfCitySurchargeFixedAmount(value)
        }
    }

    fun updateOutOfCitySurchargePercent(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateOutOfCitySurchargePercent(value)
        }
    }

    fun updateIlaiyaraajaRingtone(value: String) {
        viewModelScope.launch {
            settingsRepository.updateIlaiyaraajaRingtone(value)
        }
    }

    fun updateIsDarkMode(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateIsDarkMode(value)
        }
    }

    fun updateDarkMode(value: Boolean) = updateIsDarkMode(value)

    fun updateTtsVoiceProfile(value: String) {
        viewModelScope.launch {
            settingsRepository.updateTtsVoiceProfile(value)
        }
    }

    fun updateTtsSpeechRate(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateTtsSpeechRate(value)
        }
    }

    fun updateTtsPitch(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateTtsPitch(value)
        }
    }

    fun updateFloatingBubbleEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFloatingBubbleEnabled(value)
        }
    }
}
