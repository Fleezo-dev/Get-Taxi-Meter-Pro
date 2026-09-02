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
    val status: String = "pending"
)
