package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        refreshPendingTrips()
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

    fun loadTripToSupabase(
        customerPhone: String,
        customerName: String?,
        pickupLocation: String?,
        dropLocation: String?,
        tripOtp: String,
        baseFare: Double,
        perKmFare: Double,
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

        viewModelScope.launch {
            _isLoading.value = true
            repository.claimTrip(tripId)
                .onSuccess {
                    _isLoading.value = false
                    refreshPendingTrips()
                    onSuccess(trip.copy(status = "claimed"))
                }
                .onFailure { e ->
                    _isLoading.value = false
                    onError(e.message ?: "Failed to claim trip on Supabase")
                }
        }
    }
}
