package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriverRemoteEntity(
    val id: String? = null,
    @SerialName("driver_id")
    val driverId: String? = null,
    @SerialName("driver_name")
    val driverName: String,
    @SerialName("driver_phone")
    val driverPhone: String,
    @SerialName("vehicle_number")
    val vehicleNumber: String? = null,
    @SerialName("vehicle_type")
    val vehicleType: String? = "Sedan",
    @SerialName("is_active")
    val isActive: Boolean = true,
    val status: String = "AVAILABLE", // "AVAILABLE", "ON_TRIP", "OFFLINE"
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("last_heartbeat")
    val lastHeartbeat: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
