package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DriverProfile
import com.example.data.preferences.DriverProfileRepository
import com.example.security.ActivationSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppRole {
    DRIVER
}

class DispatchViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = DriverProfileRepository(application)

    private val _currentRole = MutableStateFlow(AppRole.DRIVER)
    val currentRole: StateFlow<AppRole> = _currentRole.asStateFlow()

    private val _driverProfile = MutableStateFlow(DriverProfile())
    val driverProfile: StateFlow<DriverProfile> = _driverProfile.asStateFlow()

    private val _adminPin = MutableStateFlow("2604")
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    private val _isDispatcherAuthenticated = MutableStateFlow(false)
    val isDispatcherAuthenticated: StateFlow<Boolean> = _isDispatcherAuthenticated.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                profileRepository.ensureDriverIdAssigned()
            } catch (e: Exception) {
                Log.e("DispatchViewModel", "Failed to assign initial Driver ID", e)
            }
            profileRepository.driverProfileFlow.collect { profile ->
                _driverProfile.value = profile
            }
        }

        viewModelScope.launch {
            profileRepository.adminPinFlow.collect { pin ->
                _adminPin.value = pin
            }
        }
    }

    fun isDriverIdEditable(driverId: String): Boolean {
        val num = driverId.filter { it.isDigit() }.toIntOrNull()
        return num != null && num in 1..10
    }

    fun verifyAdminPin(enteredPin: String): Boolean {
        val pin = enteredPin.trim()
        if (pin == _adminPin.value || ActivationSecurityManager.MASTER_PASSWORDS.contains(pin)) {
            _isDispatcherAuthenticated.value = true
            return true
        }
        return false
    }

    fun updateDriverProfile(updatedProfile: DriverProfile) {
        viewModelScope.launch {
            val currentId = _driverProfile.value.driverId
            val finalProfile = if (currentId.isNotBlank() && !isDriverIdEditable(currentId) && updatedProfile.driverId != currentId) {
                updatedProfile.copy(driverId = currentId)
            } else {
                updatedProfile
            }
            profileRepository.saveProfile(finalProfile)
            _driverProfile.value = finalProfile
        }
    }

    fun logoutAndRequestFreshDriverId(onComplete: (DriverProfile) -> Unit = {}) {
        viewModelScope.launch {
            val fresh = profileRepository.resetAndRequestFreshDriverId()
            _driverProfile.value = fresh
            onComplete(fresh)
        }
    }

    fun logoutEmergencyDriver() {
        viewModelScope.launch {
            try {
                profileRepository.clearProfile()
                ActivationSecurityManager.clearActivation(getApplication())
                _driverProfile.value = DriverProfile(
                    driverId = "",
                    driverName = "",
                    phoneNumber = "",
                    isProfileCompleted = false,
                    isActivated = false,
                    isEmergencyOneTime = false
                )
            } catch (e: Exception) {
                Log.e("DispatchViewModel", "Failed to logout emergency driver: ${e.message}")
            }
        }
    }

    fun updateAdminPin(newPin: String) {
        viewModelScope.launch {
            if (newPin.isNotBlank()) {
                profileRepository.updateAdminPin(newPin)
                _adminPin.value = newPin
            }
        }
    }

    fun finishActiveTrip(finalFare: Double = 0.0) {
        // Active meter trip completed
    }
}
