package com.example.data.model

data class DriverProfile(
    val driverId: String = "DRV-104",
    val driverName: String = "",
    val phoneNumber: String = "",
    val vehiclePlate: String = "TN-01-TX-1001",
    val vehicleType: String = "Sedan",
    val vehicleModel: String = "Dzire",
    val photoUri: String = "",
    val isOnline: Boolean = true,
    val status: String = "AVAILABLE", // "AVAILABLE", "ON_TRIP", "BUSY", "OFFLINE"
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastLocationName: String = "Locating...",
    val batteryPercent: Int = 98,
    val completedTripsCount: Int = 12,
    val rating: Double = 4.9,
    val fleetNetworkCode: String = "GET-TAXI-NETWORK-1",
    val isProfileCompleted: Boolean = false,
    val isActivated: Boolean = false,
    val activationKey: String = "",
    val isEmergencyOneTime: Boolean = false,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
