package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DriverProfile
import com.example.data.model.DriverRemoteEntity
import com.example.data.model.PendingTrip
import com.example.data.repository.SupabaseTripsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PendingTripsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SupabaseTripsRepository()

    private val _pendingTrips = MutableStateFlow<List<PendingTrip>>(emptyList())
    val pendingTrips: StateFlow<List<PendingTrip>> = _pendingTrips.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _adminTrips = MutableStateFlow<List<PendingTrip>>(emptyList())
    val adminTrips: StateFlow<List<PendingTrip>> = _adminTrips.asStateFlow()

    private val _allDrivers = MutableStateFlow<List<DriverRemoteEntity>>(emptyList())
    val allDrivers: StateFlow<List<DriverRemoteEntity>> = _allDrivers.asStateFlow()

    private val _isSuspensionChecking = MutableStateFlow(false)
    val isSuspensionChecking: StateFlow<Boolean> = _isSuspensionChecking.asStateFlow()

    init {
        refreshPendingTrips()
        refreshAdminTrips()
        refreshDrivers()
    }

    fun refreshPendingTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getPendingTrips()
                .onSuccess { trips ->
                    _pendingTrips.value = trips
                    _isLoading.value = false
                }
                .onFailure { e ->
                    Log.e("PendingTripsVM", "Failed to load pending trips: ${e.message}", e)
                    _errorMessage.value = e.message ?: "Failed to connect to Supabase database"
                    _isLoading.value = false
                }
        }
    }

    fun refreshAdminTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllTripsForAdmin()
                .onSuccess { trips ->
                    _adminTrips.value = trips
                    _isLoading.value = false
                }
                .onFailure {
                    _isLoading.value = false
                }
        }
    }

    fun refreshDrivers() {
        viewModelScope.launch {
            repository.getAllDrivers()
                .onSuccess { drivers ->
                    _allDrivers.value = drivers
                }
                .onFailure { e ->
                    Log.e("PendingTripsVM", "Failed to fetch drivers: ${e.message}")
                }
        }
    }

    fun toggleDriverSuspension(driverPhone: String, newActiveState: Boolean, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.setDriverActiveStatus(driverPhone, newActiveState)
                .onSuccess {
                    _isLoading.value = false
                    refreshDrivers()
                    onSuccess()
                }
                .onFailure { e ->
                    _isLoading.value = false
                    Log.e("PendingTripsVM", "Failed to toggle suspension: ${e.message}")
                }
        }
    }

    fun registerOrUpdateDriver(profile: DriverProfile) {
        val phone = profile.phoneNumber.trim().ifBlank {
            if (profile.driverId.isNotBlank() && profile.driverId != "0") "DEV-${profile.driverId}" else "Device-Authorized"
        }
        val name = profile.driverName.trim().ifBlank {
            if (profile.driverId.isNotBlank() && profile.driverId != "0") "Driver ${profile.driverId}" else "Registered Driver"
        }
        val driverId = profile.driverId.trim().ifBlank { "DRV-001" }

        viewModelScope.launch {
            val entity = DriverRemoteEntity(
                driverId = driverId,
                driverName = name,
                driverPhone = phone,
                vehicleNumber = profile.vehiclePlate.ifBlank { profile.vehicleModel.ifBlank { "TN-38-BZ-4411" } },
                vehicleType = profile.vehicleType.ifBlank { "Sedan" },
                isActive = profile.isActive,
                status = if (profile.isOnline) "AVAILABLE" else "OFFLINE"
            )
            repository.upsertDriver(entity).onSuccess {
                refreshDrivers()
            }
        }
    }

    fun loadTripToSupabase(
        customerPhone: String,
        customerName: String?,
        pickupLocation: String?,
        dropLocation: String?,
        tripOtp: String,
        baseFare: Double,
        perKmFare: Double,
        createdBy: String = "Master Admin",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (customerPhone.isBlank()) {
            onError("Customer Phone number is mandatory")
            return
        }
        if (tripOtp.isBlank()) {
            onError("Trip OTP is mandatory")
            return
        }
        if (baseFare < 0 || perKmFare <= 0) {
            onError("Please enter valid Base Fare and Per-KM Fare")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val newTrip = PendingTrip(
                customerName = customerName?.takeIf { it.isNotBlank() },
                customerPhone = customerPhone.trim(),
                pickupLocation = pickupLocation?.takeIf { it.isNotBlank() },
                dropLocation = dropLocation?.takeIf { it.isNotBlank() },
                tripOtp = tripOtp.trim(),
                baseFare = baseFare,
                perKmFare = perKmFare,
                createdBy = createdBy,
                status = "pending"
            )

            repository.insertPendingTrip(newTrip)
                .onSuccess {
                    _isLoading.value = false
                    refreshPendingTrips()
                    refreshAdminTrips()
                    onSuccess()
                }
                .onFailure { e ->
                    _isLoading.value = false
                    onError(e.message ?: "Failed to broadcast trip to Supabase")
                }
        }
    }

    fun claimTripWithOtp(
        trip: PendingTrip,
        enteredOtp: String,
        driverProfile: DriverProfile,
        onSuccess: (PendingTrip) -> Unit,
        onError: (String) -> Unit
    ) {
        val tripId = trip.id
        if (tripId == null) {
            onError("Invalid trip ID")
            return
        }

        if (enteredOtp.trim() != trip.tripOtp.trim()) {
            onError("❌ Incorrect OTP! Please enter the exact OTP sent to the customer.")
            return
        }

        // Verify driver suspension / remote active status before claiming
        viewModelScope.launch {
            _isLoading.value = true
            if (driverProfile.phoneNumber.isNotBlank()) {
                val activeCheck = repository.checkDriverActiveStatus(driverProfile.phoneNumber)
                if (activeCheck.isSuccess && activeCheck.getOrNull() == false) {
                    _isLoading.value = false
                    onError("⛔ ACCOUNT SUSPENDED: Your driver account has been suspended by Master Admin. You cannot claim trips. Please contact dispatch.")
                    return@launch
                }
            }

            val effectiveDriverName = if (driverProfile.driverName.isNotBlank()) driverProfile.driverName else "Master Admin"
            val effectiveDriverPhone = if (driverProfile.phoneNumber.isNotBlank()) driverProfile.phoneNumber else "Admin"
            val effectiveDriverId = if (driverProfile.driverId.isNotBlank()) driverProfile.driverId else "ADMIN_MASTER"

            repository.claimTrip(
                tripId = tripId,
                driverId = effectiveDriverId,
                driverName = effectiveDriverName,
                driverPhone = effectiveDriverPhone
            ).onSuccess {
                _isLoading.value = false
                refreshPendingTrips()
                refreshAdminTrips()
                onSuccess(trip.copy(
                    status = "claimed",
                    claimedByDriverId = effectiveDriverId,
                    claimedByDriverName = effectiveDriverName,
                    claimedByDriverPhone = effectiveDriverPhone
                ))
            }.onFailure { e ->
                _isLoading.value = false
                onError(e.message ?: "Failed to claim trip on Supabase")
            }
        }
    }

    fun deletePendingTrip(
        tripId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteTrip(tripId)
                .onSuccess {
                    _isLoading.value = false
                    refreshPendingTrips()
                    refreshAdminTrips()
                    onSuccess()
                }
                .onFailure { e ->
                    _isLoading.value = false
                    onError(e.message ?: "Failed to delete trip")
                }
        }
    }

    fun completeClaimedTrip(
        tripId: Long,
        finalFare: Double,
        commissionRate: Double = 0.10, // 10% dispatcher commission
        waitTimeMinutes: Double = 0.0,
        totalDistanceKm: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val commAmount = finalFare * commissionRate
            repository.completeTrip(tripId, finalFare, commAmount, waitTimeMinutes, totalDistanceKm)
                .onSuccess {
                    refreshAdminTrips()
                    onSuccess()
                }
        }
    }
}
