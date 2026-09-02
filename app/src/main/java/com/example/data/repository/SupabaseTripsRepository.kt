package com.example.data.repository

import android.util.Log
import com.example.data.model.DriverRemoteEntity
import com.example.data.model.PendingTrip
import com.example.data.remote.SupabaseConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MinimalPendingTrip(
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

class SupabaseTripsRepository {

    private val client = SupabaseConfig.client
    private val tripsTable = "pending_trips"
    private val driversTable = "drivers"

    suspend fun getPendingTrips(): Result<List<PendingTrip>> = withContext(Dispatchers.IO) {
        try {
            val result = client.from(tripsTable)
                .select {
                    filter {
                        eq("status", "pending")
                    }
                    order(column = "id", order = Order.DESCENDING)
                }
                .decodeList<PendingTrip>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error fetching pending trips: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun insertPendingTrip(trip: PendingTrip): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // First attempt full insert with all fields
            client.from(tripsTable).insert(trip)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("SupabaseTripsRepo", "Full insert failed (${e.message}), attempting minimal fallback insert")
            try {
                // Fallback to core columns in case created_by or other optional columns are missing in Supabase schema
                val fallbackTrip = MinimalPendingTrip(
                    customerName = trip.customerName,
                    customerPhone = trip.customerPhone,
                    pickupLocation = trip.pickupLocation,
                    dropLocation = trip.dropLocation,
                    tripOtp = trip.tripOtp,
                    baseFare = trip.baseFare,
                    perKmFare = trip.perKmFare,
                    status = trip.status
                )
                client.from(tripsTable).insert(fallbackTrip)
                Result.success(Unit)
            } catch (e2: Exception) {
                Log.e("SupabaseTripsRepo", "Error inserting pending trip fallback: ${e2.message}", e2)
                Result.failure(e2)
            }
        }
    }

    suspend fun claimTrip(
        tripId: Long,
        driverId: String? = null,
        driverName: String? = null,
        driverPhone: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from(tripsTable).update(
                {
                    set("status", "claimed")
                    driverId?.let { set("claimed_by_driver_id", it) }
                    driverName?.let { set("claimed_by_driver_name", it) }
                    driverPhone?.let { set("claimed_by_driver_phone", it) }
                }
            ) {
                filter {
                    eq("id", tripId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("SupabaseTripsRepo", "Full claim update failed (${e.message}), attempting status-only fallback")
            try {
                client.from(tripsTable).update(
                    {
                        set("status", "claimed")
                    }
                ) {
                    filter {
                        eq("id", tripId)
                    }
                }
                Result.success(Unit)
            } catch (e2: Exception) {
                Log.e("SupabaseTripsRepo", "Error claiming trip $tripId: ${e2.message}", e2)
                Result.failure(e2)
            }
        }
    }

    suspend fun completeTrip(
        tripId: Long,
        finalFare: Double,
        commissionAmount: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from(tripsTable).update(
                {
                    set("status", "completed")
                    set("final_fare", finalFare)
                    set("commission_amount", commissionAmount)
                }
            ) {
                filter {
                    eq("id", tripId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("SupabaseTripsRepo", "Full complete update failed (${e.message}), attempting partial fallback with final_fare")
            try {
                client.from(tripsTable).update(
                    {
                        set("status", "completed")
                        set("final_fare", finalFare)
                    }
                ) {
                    filter {
                        eq("id", tripId)
                    }
                }
                Result.success(Unit)
            } catch (e2: Exception) {
                try {
                    client.from(tripsTable).update(
                        {
                            set("status", "completed")
                        }
                    ) {
                        filter {
                            eq("id", tripId)
                        }
                    }
                    Result.success(Unit)
                } catch (e3: Exception) {
                    Log.e("SupabaseTripsRepo", "Error completing trip $tripId: ${e3.message}", e3)
                    Result.failure(e3)
                }
            }
        }
    }

    suspend fun deleteTrip(tripId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from(tripsTable).delete {
                filter {
                    eq("id", tripId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error deleting trip $tripId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllTripsForAdmin(): Result<List<PendingTrip>> = withContext(Dispatchers.IO) {
        try {
            val result = client.from(tripsTable)
                .select {
                    order(column = "id", order = Order.DESCENDING)
                }
                .decodeList<PendingTrip>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error fetching admin trips: ${e.message}", e)
            Result.failure(e)
        }
    }

    // DRIVERS API
    suspend fun getAllDrivers(): Result<List<DriverRemoteEntity>> = withContext(Dispatchers.IO) {
        try {
            val result = client.from(driversTable)
                .select {
                    order(column = "driver_name", order = Order.ASCENDING)
                }
                .decodeList<DriverRemoteEntity>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error fetching drivers: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setDriverActiveStatus(driverPhone: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from(driversTable).update(
                {
                    set("is_active", isActive)
                }
            ) {
                filter {
                    eq("driver_phone", driverPhone)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error updating driver active status: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun upsertDriver(driver: DriverRemoteEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from(driversTable).upsert(driver) {
                onConflict = "driver_phone"
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("SupabaseTripsRepo", "Full driver upsert failed (${e.message}), attempting minimal fallback")
            try {
                @Serializable
                data class MinimalDriver(
                    @SerialName("driver_name") val driverName: String,
                    @SerialName("driver_phone") val driverPhone: String,
                    @SerialName("is_active") val isActive: Boolean = true,
                    val status: String = "AVAILABLE"
                )
                val minDriver = MinimalDriver(
                    driverName = driver.driverName,
                    driverPhone = driver.driverPhone,
                    isActive = driver.isActive,
                    status = driver.status
                )
                client.from(driversTable).upsert(minDriver) {
                    onConflict = "driver_phone"
                }
                Result.success(Unit)
            } catch (e2: Exception) {
                Log.e("SupabaseTripsRepo", "Error saving driver profile: ${e2.message}", e2)
                Result.failure(e2)
            }
        }
    }

    suspend fun checkDriverActiveStatus(driverPhone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val drivers = client.from(driversTable)
                .select {
                    filter {
                        eq("driver_phone", driverPhone)
                    }
                }
                .decodeList<DriverRemoteEntity>()
            if (drivers.isNotEmpty()) {
                Result.success(drivers.first().isActive)
            } else {
                // If not found in remote DB yet, default to active
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error checking driver status: ${e.message}", e)
            // On network error, allow local cached state
            Result.failure(e)
        }
    }
}

