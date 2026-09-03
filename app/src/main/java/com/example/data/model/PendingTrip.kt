package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PendingTrip(
    val id: Long? = null,
    @SerialName("customer_name")
    val customerName: String? = null,
    @SerialName("customer_phone")
    val customerPhone: String,
    @SerialName("pickup_location")
    val pickupLocation: String? = null,
    @SerialName("drop_location")
    val dropLocation: String? = null,
    @SerialName("trip_otp")
    val tripOtp: String,
    @SerialName("base_fare")
    val baseFare: Double,
    @SerialName("per_km_fare")
    val perKmFare: Double,
    val status: String = "pending", // "pending", "claimed", "completed"
    @SerialName("claimed_by_driver_id")
    val claimedByDriverId: String? = null,
    @SerialName("claimed_by_driver_name")
    val claimedByDriverName: String? = null,
    @SerialName("claimed_by_driver_phone")
    val claimedByDriverPhone: String? = null,
    @SerialName("created_by")
    val createdBy: String? = "Master Admin",
    @SerialName("final_fare")
    val finalFare: Double? = 0.0,
    @SerialName("commission_amount")
    val commissionAmount: Double? = 0.0,
    @SerialName("wait_time_minutes")
    val waitTimeMinutes: Double? = 0.0,
    @SerialName("total_distance_km")
    val totalDistanceKm: Double? = 0.0,
    @SerialName("created_at")
    val createdAt: String? = null
)
