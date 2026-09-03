package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received boot/system intent action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            try {
                LocationTrackingService.recoveryTrip(context)
                Log.d("BootReceiver", "Successfully dispatched LocationTrackingService recovery")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error triggering trip recovery on boot", e)
            }
        }
    }
}
