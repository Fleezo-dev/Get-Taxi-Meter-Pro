package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DriverProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.driverDataStore: DataStore<Preferences> by preferencesDataStore(name = "driver_profile_prefs")

class DriverProfileRepository(private val context: Context) {

    companion object {
        val KEY_DRIVER_ID = stringPreferencesKey("driver_id")
        val KEY_DRIVER_NAME = stringPreferencesKey("driver_name")
        val KEY_PHONE_NUMBER = stringPreferencesKey("phone_number")
        val KEY_VEHICLE_PLATE = stringPreferencesKey("vehicle_plate")
        val KEY_VEHICLE_TYPE = stringPreferencesKey("vehicle_type")
        val KEY_VEHICLE_MODEL = stringPreferencesKey("vehicle_model")
        val KEY_PHOTO_URI = stringPreferencesKey("photo_uri")
        val KEY_QR_CODE_URI = stringPreferencesKey("qr_code_uri")
        val KEY_IS_ONLINE = booleanPreferencesKey("is_online")
        val KEY_IS_ACTIVE = booleanPreferencesKey("is_active")
        val KEY_ADMIN_PIN = stringPreferencesKey("admin_pin")
        val KEY_FLEET_CODE = stringPreferencesKey("fleet_code")
        val KEY_PROFILE_COMPLETED = booleanPreferencesKey("profile_completed")
        val KEY_IS_ACTIVATED = booleanPreferencesKey("is_activated")
        val KEY_ACTIVATION_KEY = stringPreferencesKey("activation_key")
        val KEY_IS_EMERGENCY = booleanPreferencesKey("is_emergency_one_time")
        val KEY_LOCAL_DRIVER_COUNTER = intPreferencesKey("local_driver_counter")
    }

    val driverProfileFlow: Flow<DriverProfile> = context.driverDataStore.data.map { prefs ->
        val existingId = prefs[KEY_DRIVER_ID] ?: ""
        DriverProfile(
            driverId = existingId.ifBlank { "DRV0011" },
            driverName = prefs[KEY_DRIVER_NAME] ?: "",
            phoneNumber = prefs[KEY_PHONE_NUMBER] ?: "",
            vehiclePlate = prefs[KEY_VEHICLE_PLATE] ?: "TN-01-TX-1001",
            vehicleType = prefs[KEY_VEHICLE_TYPE] ?: "Sedan",
            vehicleModel = prefs[KEY_VEHICLE_MODEL] ?: "Dzire",
            photoUri = prefs[KEY_PHOTO_URI] ?: "",
            qrCodeUri = prefs[KEY_QR_CODE_URI] ?: "",
            isOnline = prefs[KEY_IS_ONLINE] ?: true,
            isActive = prefs[KEY_IS_ACTIVE] ?: true,
            status = if (prefs[KEY_IS_ONLINE] != false) "AVAILABLE" else "OFFLINE",
            fleetNetworkCode = prefs[KEY_FLEET_CODE] ?: "GET-TAXI-NETWORK-1",
            isProfileCompleted = prefs[KEY_PROFILE_COMPLETED] ?: false,
            isActivated = prefs[KEY_IS_ACTIVATED] ?: false,
            activationKey = prefs[KEY_ACTIVATION_KEY] ?: "",
            isEmergencyOneTime = prefs[KEY_IS_EMERGENCY] ?: false
        )
    }

    private suspend fun generateNextLocalDriverId(): String {
        var nextCount = 11
        context.driverDataStore.edit { prefs ->
            val current = prefs[KEY_LOCAL_DRIVER_COUNTER] ?: 11
            nextCount = current + 1
            prefs[KEY_LOCAL_DRIVER_COUNTER] = nextCount
        }
        return "DRV%04d".format(nextCount)
    }

    suspend fun ensureDriverIdAssigned(): DriverProfile {
        val prefs = context.driverDataStore.data.first()
        val existingId = prefs[KEY_DRIVER_ID]
        if (existingId.isNullOrBlank() || existingId == "DRV-0011" || existingId == "DRV0011") {
            val finalId = generateNextLocalDriverId()
            val defaultName = if (prefs[KEY_DRIVER_NAME].isNullOrBlank() || prefs[KEY_DRIVER_NAME]?.startsWith("Driver ") == true) {
                "Driver ${finalId.takeLast(4)}"
            } else {
                prefs[KEY_DRIVER_NAME]!!
            }
            context.driverDataStore.edit { p ->
                p[KEY_DRIVER_ID] = finalId
                p[KEY_DRIVER_NAME] = defaultName
            }
        }
        return driverProfileFlow.first()
    }

    val adminPinFlow: Flow<String> = context.driverDataStore.data.map { prefs ->
        prefs[KEY_ADMIN_PIN] ?: "2604"
    }

    suspend fun saveProfile(profile: DriverProfile) {
        var resolvedId = profile.driverId
        if (resolvedId.isBlank() || resolvedId == "DRV-0011" || resolvedId == "DRV0011") {
            resolvedId = generateNextLocalDriverId()
        }

        val updatedProfile = profile.copy(driverId = resolvedId)

        context.driverDataStore.edit { prefs ->
            prefs[KEY_DRIVER_ID] = updatedProfile.driverId
            prefs[KEY_DRIVER_NAME] = updatedProfile.driverName
            prefs[KEY_PHONE_NUMBER] = updatedProfile.phoneNumber
            prefs[KEY_VEHICLE_PLATE] = updatedProfile.vehiclePlate
            prefs[KEY_VEHICLE_TYPE] = updatedProfile.vehicleType
            prefs[KEY_VEHICLE_MODEL] = updatedProfile.vehicleModel
            prefs[KEY_PHOTO_URI] = updatedProfile.photoUri
            prefs[KEY_QR_CODE_URI] = updatedProfile.qrCodeUri
            prefs[KEY_IS_ONLINE] = updatedProfile.isOnline
            prefs[KEY_IS_ACTIVE] = updatedProfile.isActive
            prefs[KEY_FLEET_CODE] = updatedProfile.fleetNetworkCode
            prefs[KEY_PROFILE_COMPLETED] = updatedProfile.isProfileCompleted
            prefs[KEY_IS_ACTIVATED] = updatedProfile.isActivated
            prefs[KEY_ACTIVATION_KEY] = updatedProfile.activationKey
            prefs[KEY_IS_EMERGENCY] = updatedProfile.isEmergencyOneTime
        }
    }

    suspend fun updateAdminPin(pin: String) {
        context.driverDataStore.edit { prefs ->
            prefs[KEY_ADMIN_PIN] = pin
        }
    }

    suspend fun clearProfile() {
        context.driverDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun resetLocalCounter() {
        context.driverDataStore.edit { prefs ->
            prefs[KEY_LOCAL_DRIVER_COUNTER] = 11
        }
    }

    suspend fun resetAndRequestFreshDriverId(): DriverProfile {
        clearProfile()
        val freshId = generateNextLocalDriverId()
        val defaultName = "Driver ${freshId.takeLast(4)}"
        context.driverDataStore.edit { prefs ->
            prefs[KEY_DRIVER_ID] = freshId
            prefs[KEY_DRIVER_NAME] = defaultName
            prefs[KEY_PHONE_NUMBER] = "+91 9043743777"
            prefs[KEY_VEHICLE_PLATE] = "TN-${(10..99).random()}AF-${(1000..9999).random()}"
            prefs[KEY_VEHICLE_MODEL] = "Sedan Taxi"
            prefs[KEY_IS_ONLINE] = true
            prefs[KEY_IS_ACTIVE] = true
            prefs[KEY_FLEET_CODE] = "GET-TAXI-NETWORK-1"
        }
        return driverProfileFlow.first()
    }

    suspend fun getOrInitProfile(): DriverProfile {
        val current = driverProfileFlow.first()
        saveProfile(current)
        return current
    }
}
