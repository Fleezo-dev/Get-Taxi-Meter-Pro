package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.TripDatabase
import com.example.data.repository.TripRepository
import com.example.service.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Device boot completed, checking for active trip recovery...")
            val db = TripDatabase.getDatabase(context)
            val repository = TripRepository(db.tripDao())
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeTrip = repository.getActiveTrip()
                    if (activeTrip != null) {
                        Log.i("BootReceiver", "Active trip found on boot. Triggering recovery service...")
                        val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                            action = LocationTrackingService.ACTION_RECOVER
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error during boot trip check", e)
                }
            }
        }
    }
}
