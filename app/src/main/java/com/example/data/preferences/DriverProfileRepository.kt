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

    fun isReservedAdminId(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        val clean = id.trim()
        val num = clean.filter { it.isDigit() }.toIntOrNull() ?: return false
        return num in 1..10
    }

    private fun isGenericOrOldRandomId(id: String?): Boolean {
        if (id.isNullOrBlank()) return true
        val upper = id.uppercase().trim()
        if (upper.startsWith("DRV") || upper.contains("-") || upper.length > 3) {
            return true
        }
        val num = upper.filter { it.isDigit() }.toIntOrNull() ?: return true
        // If it's a random 4-digit ID or number >= 1000
        return num >= 1000
    }

    suspend fun getNextStandardDriverId(): String {
        val prefs = context.driverDataStore.data.first()
        val currentCounter = prefs[KEY_LOCAL_DRIVER_COUNTER] ?: 11
        val nextVal = if (currentCounter < 11) 11 else currentCounter
        context.driverDataStore.edit { p ->
            p[KEY_LOCAL_DRIVER_COUNTER] = nextVal + 1
        }
        return "%03d".format(nextVal)
    }

    val driverProfileFlow: Flow<DriverProfile> = context.driverDataStore.data.map { prefs ->
        val phone = prefs[KEY_PHONE_NUMBER] ?: ""
        val storedId = prefs[KEY_DRIVER_ID] ?: ""
        val resolvedId = if (storedId.isBlank() || isGenericOrOldRandomId(storedId)) {
            val name = prefs[KEY_DRIVER_NAME] ?: ""
            if (name.contains("Admin", ignoreCase = true)) "001" else "011"
        } else {
            storedId
        }
        DriverProfile(
            driverId = resolvedId,
            driverName = prefs[KEY_DRIVER_NAME] ?: "",
            phoneNumber = phone,
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

    suspend fun ensureDriverIdAssigned(): DriverProfile {
        val prefs = context.driverDataStore.data.first()
        val existingId = prefs[KEY_DRIVER_ID]
        val name = prefs[KEY_DRIVER_NAME] ?: ""
        val isMaster = name.contains("Admin", ignoreCase = true) || prefs[KEY_ACTIVATION_KEY]?.equals("Master1974", ignoreCase = true) == true

        if (isMaster) {
            val adminId = if (isReservedAdminId(existingId)) {
                "%03d".format(existingId!!.filter { it.isDigit() }.toInt())
            } else {
                "001"
            }
            context.driverDataStore.edit { p ->
                p[KEY_DRIVER_ID] = adminId
                if (p[KEY_DRIVER_NAME].isNullOrBlank()) p[KEY_DRIVER_NAME] = "Master Admin"
            }
        } else if (existingId.isNullOrBlank() || isGenericOrOldRandomId(existingId)) {
            val standardId = getNextStandardDriverId()
            val defaultName = if (name.isBlank() || name.startsWith("Driver ")) {
                "Driver $standardId"
            } else {
                name
            }
            context.driverDataStore.edit { p ->
                p[KEY_DRIVER_ID] = standardId
                p[KEY_DRIVER_NAME] = defaultName
            }
        }
        return driverProfileFlow.first()
    }

    val adminPinFlow: Flow<String> = context.driverDataStore.data.map { prefs ->
        prefs[KEY_ADMIN_PIN] ?: "2604"
    }

    suspend fun saveProfile(profile: DriverProfile) {
        val isMaster = profile.driverName.contains("Admin", ignoreCase = true)
        val resolvedId = if (isMaster) {
            val num = profile.driverId.filter { it.isDigit() }.toIntOrNull()
            if (num != null && num in 1..10) "%03d".format(num) else "001"
        } else {
            val num = profile.driverId.filter { it.isDigit() }.toIntOrNull()
            if (num != null && num >= 11 && num < 1000) {
                "%03d".format(num)
            } else if (profile.driverId.isBlank() || isGenericOrOldRandomId(profile.driverId)) {
                getNextStandardDriverId()
            } else {
                profile.driverId
            }
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
        val defaultPhone = "+91 9043743777"
        val freshId = getNextStandardDriverId()
        val defaultName = "Driver $freshId"
        context.driverDataStore.edit { prefs ->
            prefs[KEY_DRIVER_ID] = freshId
            prefs[KEY_DRIVER_NAME] = defaultName
            prefs[KEY_PHONE_NUMBER] = defaultPhone
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
