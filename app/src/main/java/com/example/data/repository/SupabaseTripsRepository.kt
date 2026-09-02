package com.example.data.repository

import android.util.Log
import com.example.data.model.PendingTrip
import com.example.data.remote.SupabaseConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseTripsRepository {

    private val client = SupabaseConfig.client
    private val tableName = "pending_trips"

    suspend fun getPendingTrips(): Result<List<PendingTrip>> = withContext(Dispatchers.IO) {
        try {
            val result = client.from(tableName)
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
            client.from(tableName).insert(trip)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error inserting pending trip: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun claimTrip(tripId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from(tableName).update(
                {
                    set("status", "claimed")
                }
            ) {
                filter {
                    eq("id", tripId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseTripsRepo", "Error claiming trip $tripId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllTripsForAdmin(): Result<List<PendingTrip>> = withContext(Dispatchers.IO) {
        try {
            val result = client.from(tableName)
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
}
