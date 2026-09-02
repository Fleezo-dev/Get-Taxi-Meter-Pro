package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DriverProfile
import com.example.data.model.DriverRemoteEntity
import com.example.data.preferences.DriverProfileRepository
import com.example.data.repository.SupabaseTripsRepository
import com.example.security.ActivationSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppRole {
    DRIVER
}

enum class AdminRole {
    MASTER_ADMIN,
    REGULAR_ADMIN
}

class DispatchViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = DriverProfileRepository(application)
    private val supabaseRepository = SupabaseTripsRepository()

    private val _currentRole = MutableStateFlow(AppRole.DRIVER)
    val currentRole: StateFlow<AppRole> = _currentRole.asStateFlow()

    private val _driverProfile = MutableStateFlow(DriverProfile())
    val driverProfile: StateFlow<DriverProfile> = _driverProfile.asStateFlow()

    private val _adminPin = MutableStateFlow("2604")
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    private val _isDispatcherAuthenticated = MutableStateFlow(false)
    val isDispatcherAuthenticated: StateFlow<Boolean> = _isDispatcherAuthenticated.asStateFlow()

    private val _authenticatedAdminRole = MutableStateFlow(AdminRole.REGULAR_ADMIN)
    val authenticatedAdminRole: StateFlow<AdminRole> = _authenticatedAdminRole.asStateFlow()

    private val _authenticatedAdminName = MutableStateFlow("Master Admin")
    val authenticatedAdminName: StateFlow<String> = _authenticatedAdminName.asStateFlow()

    companion object {
        val MASTER_ADMIN_PASSWORDS = setOf(
            "2903",
            "1005",
            "1974",
            "Master1974"
        )
    }

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
        if (MASTER_ADMIN_PASSWORDS.contains(pin)) {
            _authenticatedAdminRole.value = AdminRole.MASTER_ADMIN
            _authenticatedAdminName.value = "Master Admin"
            _isDispatcherAuthenticated.value = true
            return true
        }

        if (pin == _adminPin.value || ActivationSecurityManager.MASTER_PASSWORDS.contains(pin)) {
            _authenticatedAdminRole.value = AdminRole.REGULAR_ADMIN
            _authenticatedAdminName.value = "Regular Admin (${pin.takeLast(4)})"
            _isDispatcherAuthenticated.value = true
            return true
        }

        return false
    }

    fun setAdminDetails(role: AdminRole, name: String) {
        _authenticatedAdminRole.value = role
        _authenticatedAdminName.value = name
    }

    fun logoutAdmin() {
        _isDispatcherAuthenticated.value = false
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

            val phone = finalProfile.phoneNumber.trim().ifBlank {
                if (finalProfile.driverId.isNotBlank()) "ID-${finalProfile.driverId}" else ""
            }
            val name = finalProfile.driverName.ifBlank {
                if (finalProfile.driverId.isNotBlank()) "Driver ${finalProfile.driverId}" else "Registered Driver"
            }
            if (phone.isNotBlank()) {
                val entity = DriverRemoteEntity(
                    driverId = finalProfile.driverId,
                    driverName = name,
                    driverPhone = phone,
                    vehicleNumber = finalProfile.vehiclePlate.ifBlank { finalProfile.vehicleModel },
                    vehicleType = finalProfile.vehicleType,
                    isActive = finalProfile.isActive,
                    status = if (finalProfile.isOnline) "AVAILABLE" else "OFFLINE"
                )
                supabaseRepository.upsertDriver(entity)
            }
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
