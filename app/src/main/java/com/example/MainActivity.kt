package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DriverRegistrationModal
import com.example.ui.components.StrictPermissionEnforcementDialog
import com.example.ui.components.UpdateDialog
import com.example.updater.AppUpdater
import com.example.ui.navigation.GetTaxiNavGraph
import com.example.ui.theme.GetTaxiTheme
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.PendingTripsViewModel
import com.example.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val meterViewModel: MeterViewModel by viewModels()
    private val dispatchViewModel: DispatchViewModel by viewModels()
    private val pendingTripsViewModel: PendingTripsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.repository.RemoteConfigRepository.initialize(applicationContext)
        enableEdgeToEdge()
        AppUpdater.checkForUpdates()

        checkAndRequestSystemPermissions()

        // Start background service for taxi meter tracking
        try {
            com.example.service.LocationTrackingService.startMonitoring(
                context = this,
                baseFare = 80.0,
                farePerKm = 28.0,
                waitFarePerMin = 2.0,
                currency = "₹",
                speedThreshold = 5.0,
                autoStartEnabled = false
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start monitoring service: ${e.message}")
        }

        setContent {
            val updateInfo by AppUpdater.updateInfo.collectAsStateWithLifecycle()
            val driverProfile by dispatchViewModel.driverProfile.collectAsStateWithLifecycle()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()

            LaunchedEffect(driverProfile.driverName, driverProfile.phoneNumber, driverProfile.driverId) {
                if (driverProfile.driverName.isNotBlank() || driverProfile.phoneNumber.isNotBlank() || driverProfile.driverId.isNotBlank()) {
                    pendingTripsViewModel.registerOrUpdateDriver(driverProfile)
                }
            }

            GetTaxiTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GetTaxiNavGraph(
                        meterViewModel = meterViewModel,
                        settingsViewModel = settingsViewModel,
                        dispatchViewModel = dispatchViewModel,
                        pendingTripsViewModel = pendingTripsViewModel
                    )

                    // Mandatory Strict System Permissions Enforcement (Location, Notifications, Unrestricted Battery)
                    StrictPermissionEnforcementDialog()

                    // Mandatory Driver Registration & Device Activation Modal (Forced for ALL drivers if not activated)
                    if (!driverProfile.isActivated || !driverProfile.isProfileCompleted || driverProfile.driverName.isBlank() || driverProfile.phoneNumber.isBlank()) {
                        DriverRegistrationModal(
                            driverId = driverProfile.driverId,
                            initialName = driverProfile.driverName,
                            initialPhone = driverProfile.phoneNumber,
                            initialPlate = driverProfile.vehiclePlate,
                            initialType = driverProfile.vehicleType,
                            initialModel = driverProfile.vehicleModel,
                            initialPhotoUri = driverProfile.photoUri,
                            onRegister = { registeredName, registeredPhone, registeredPlate, registeredType, registeredModel, registeredPhotoUri, isEmergency ->
                                val updated = driverProfile.copy(
                                    driverName = registeredName,
                                    phoneNumber = registeredPhone,
                                    vehiclePlate = registeredPlate,
                                    vehicleType = registeredType,
                                    vehicleModel = registeredModel,
                                    photoUri = registeredPhotoUri,
                                    isProfileCompleted = true,
                                    isActivated = true,
                                    isEmergencyOneTime = isEmergency,
                                    lastUpdatedTimestamp = System.currentTimeMillis()
                                )
                                dispatchViewModel.updateDriverProfile(updated)
                            }
                        )
                    } else if (updateInfo.updateAvailable) {
                        UpdateDialog(
                            updateInfo = updateInfo,
                            onOpenDriverProfile = {
                                // Bypasses update dialog to give full access to driver profile starting screen
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // When app is in foreground, dismiss the floating bubble overlay
        com.example.service.FloatingBubbleService.stop(this)
    }

    override fun onStop() {
        super.onStop()
        // When app is minimized / sent to home screen, show the floating bubble if enabled
        val tripState = com.example.service.LocationTrackingService.tripState.value
        val isBubbleEnabled = settingsViewModel.floatingBubbleEnabled.value
        if (isBubbleEnabled && (tripState.status == com.example.data.model.TripStatus.RUNNING || tripState.status == com.example.data.model.TripStatus.PAUSED || tripState.autoStartEnabled)) {
            com.example.service.FloatingBubbleService.start(this)
        }
    }

    private fun checkAndRequestSystemPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1001)
        }
    }
}
