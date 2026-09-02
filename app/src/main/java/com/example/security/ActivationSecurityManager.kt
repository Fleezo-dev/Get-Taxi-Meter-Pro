package com.example.security

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

object ActivationSecurityManager {
    private const val PREFS_NAME = "get_taxi_offline_activation_prefs"
    private const val KEY_IS_ACTIVATED = "is_device_activated"
    private const val KEY_ACTIVATION_KEY = "stored_activation_key"
    private const val KEY_ACTIVATION_DATE = "activation_date_ms"
    private const val KEY_ACTIVATION_TYPE = "activation_type" // "PERMANENT" or "EMERGENCY_ONE_TIME"

    // Preserved Master Admin Passwords / Universal Override Keys (6-password system)
    val MASTER_PASSWORDS = setOf(
        "2604",        // Standard Master OTP & Admin PIN
        "1404",        // Secondary Master PIN
        "1981",        // Alternate Admin PIN
        "1974",        // Master Admin Key
        "Master1974",  // Master Admin Key Alpha
        "140423"       // Date Master PIN
    )

    /**
     * Gets the unique Hardware / Android ID for this device.
     */
    fun getDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank()) {
                androidId.uppercase(Locale.ROOT)
            } else {
                "DEV-7A82F90B"
            }
        } catch (e: Exception) {
            "DEV-7A82F90B"
        }
    }

    /**
     * Deterministic, 100% offline algorithm that derives a unique Activation Key
     * from any Device ID.
     * Example: "9774D56D682E549C" -> "ACT-9F4B2A1C"
     */
    fun generateActivationKeyForDevice(rawDeviceId: String): String {
        val cleanId = rawDeviceId.trim().uppercase(Locale.ROOT)
            .replace("DEVICE ID:", "")
            .replace("DEVICE ID", "")
            .replace("DEV-", "")
            .replace("-", "")
            .replace(" ", "")
            .trim()

        if (cleanId.isBlank()) return "ACT-00000000"

        // Cryptographic deterministic hash using SHA-256 and internal secret salt
        val salt = "GET_TAXI_OFFLINE_SECRET_KEY_SALT_2026_KOVAI_STANDALONE"
        val input = "$cleanId-$salt"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray(Charsets.UTF_8))

        // Extract 8 hexadecimal characters
        val hexChars = hash.take(4).joinToString("") { "%02X".format(it) }
        return "ACT-$hexChars"
    }

    /**
     * Validates whether a provided Activation Key matches the device ID or is a Master Key.
     */
    fun validateActivationKey(deviceId: String, inputKey: String): Boolean {
        val cleanInput = inputKey.trim().uppercase(Locale.ROOT)
            .replace(" ", "")
            .replace("-", "")

        if (cleanInput.isBlank()) return false

        // Check against Master Passwords
        for (master in MASTER_PASSWORDS) {
            val cleanMaster = master.uppercase(Locale.ROOT).replace("-", "").replace(" ", "")
            if (cleanInput == cleanMaster || cleanInput == "ACT$cleanMaster" || cleanInput == "MASTERACT$cleanMaster") {
                return true
            }
        }

        val expectedKey = generateActivationKeyForDevice(deviceId).uppercase(Locale.ROOT).replace("-", "")
        if (cleanInput == expectedKey) return true

        val expectedSuffix = expectedKey.removePrefix("ACT")
        if (cleanInput == expectedSuffix) return true

        return false
    }

    /**
     * Checks if the app is currently activated on this device.
     */
    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_ACTIVATED, false)
    }

    /**
     * Saves activation status permanently to local storage.
     */
    fun setActivated(context: Context, activated: Boolean, key: String = "", type: String = "PERMANENT") {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVATED, activated)
            .putString(KEY_ACTIVATION_KEY, key)
            .putLong(KEY_ACTIVATION_DATE, System.currentTimeMillis())
            .putString(KEY_ACTIVATION_TYPE, type)
            .apply()
    }

    /**
     * Checks if current activation is one-time emergency mode.
     */
    fun isEmergencyOneTimeMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVATION_TYPE, "PERMANENT") == "EMERGENCY_ONE_TIME"
    }

    /**
     * Clears activation status (used for testing or resetting).
     */
    fun clearActivation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
