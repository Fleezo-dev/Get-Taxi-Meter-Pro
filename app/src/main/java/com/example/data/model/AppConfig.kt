package com.example.data.model

data class AppConfig(
    val baseHourlyRate: Double = 350.00,
    val includedKmPerHour: Double = 10.00,
    val extraKmRate: Double = 25.00,
    val minimumRequiredVersion: Int = 2,
    val telegramSupportUrl: String = "https://t.me/Gettaxikovai",
    val isActivationRequired: Boolean = true
)
