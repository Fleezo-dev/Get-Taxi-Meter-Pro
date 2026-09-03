package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.*
import com.example.data.database.TripEntity
import com.example.data.model.DispatchOrder
import com.example.data.model.PendingTrip
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import com.example.ui.components.AdminAuthPinModal
import com.example.ui.components.AdminPanelModal
import com.example.ui.components.PendingTripsBoardModal
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.TariffSettingsModal
import com.example.ui.components.TripMapView
import com.example.ui.components.VoiceTtsSettingsModal
import com.example.util.TtsManager
import com.example.viewmodel.AppRole
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.PendingTripsViewModel
import com.example.viewmodel.SettingsViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMeterScreen(
    viewModel: MeterViewModel,
    dispatchViewModel: DispatchViewModel,
    settingsViewModel: SettingsViewModel? = null,
    pendingTripsViewModel: PendingTripsViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReceipt: (Int) -> Unit
) {
    val context = LocalContext.current
    val selectedRingtoneId by (settingsViewModel?.ilaiyaraajaRingtone ?: kotlinx.coroutines.flow.flowOf("ACCORDION_GROOVE"))
        .collectAsStateWithLifecycle(initialValue = "ACCORDION_GROOVE")
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val hasBackup by viewModel.hasActiveSessionBackup.collectAsStateWithLifecycle()

    val driverProfile by dispatchViewModel.driverProfile.collectAsStateWithLifecycle()
    val isDispatcherAuthenticated by dispatchViewModel.isDispatcherAuthenticated.collectAsStateWithLifecycle()

    val pendingTripsList by pendingTripsViewModel.pendingTrips.collectAsStateWithLifecycle()
    var showPendingTripsModal by remember { mutableStateOf(false) }
    var activeClaimedTrip by remember { mutableStateOf<PendingTrip?>(null) }

    var brandTapCount by remember { mutableIntStateOf(0) }
    var lastBrandTapTime by remember { mutableLongStateOf(0L) }

    var showAdminAuthModal by remember { mutableStateOf(false) }
    var showAdminPanelModal by remember { mutableStateOf(false) }

    var showMasterOtpModal by remember { mutableStateOf(false) }
    var selectedManualRideType by remember { mutableStateOf("LOCAL_RIDE") }
    var manualBaseFareInput by remember { mutableStateOf("") }
    var manualRatePerKmInput by remember { mutableStateOf("") }
    var manualRatePerHourInput by remember { mutableStateOf("") }
    var manualDriverBetaInput by remember { mutableStateOf("") }
    var otpInputText by remember { mutableStateOf("") }
    var isOtpError by remember { mutableStateOf(false) }

    var showSettingsPinModal by remember { mutableStateOf(false) }
    var settingsPinInput by remember { mutableStateOf("") }
    var isSettingsPinError by remember { mutableStateOf(false) }

    var headerMenuExpanded by remember { mutableStateOf(false) }
    var showTariffSettingsModal by remember { mutableStateOf(false) }
    var showVoiceTtsModal by remember { mutableStateOf(false) }

    var showExtraChargesModal by remember { mutableStateOf(false) }
    var tollInput by remember(tripState.tollCharges) { mutableStateOf(if (tripState.tollCharges > 0) tripState.tollCharges.toInt().toString() else "") }
    var permitInput by remember(tripState.permitCharges) { mutableStateOf(if (tripState.permitCharges > 0) tripState.permitCharges.toInt().toString() else "") }
    var parkingInput by remember(tripState.parkingCharges) { mutableStateOf(if (tripState.parkingCharges > 0) tripState.parkingCharges.toInt().toString() else "") }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        val currentTime = System.currentTimeMillis()
        if (showTariffSettingsModal) {
            showTariffSettingsModal = false
            return@BackHandler
        }
        if (showVoiceTtsModal) {
            showVoiceTtsModal = false
            return@BackHandler
        }
        if (showSettingsPinModal) {
            showSettingsPinModal = false
            return@BackHandler
        }
        if (showMasterOtpModal) {
            showMasterOtpModal = false
            return@BackHandler
        }
        if (showAdminAuthModal) {
            showAdminAuthModal = false
            return@BackHandler
        }
        if (showAdminPanelModal) {
            showAdminPanelModal = false
            return@BackHandler
        }

        if (currentTime - lastBackPressTime < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val brandRed = Color(0xFFC62828)

    // Permission handling states
    var locationPermissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    // Register permissions on initial load
    LaunchedEffect(key1 = true) {
        viewModel.checkForActiveBackup()

        val required = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            launcher.launch(required.toTypedArray())
        }
    }

    // When a claimed trip ends, complete it on Supabase Live Cloud
    LaunchedEffect(tripState.status) {
        if (tripState.status == TripStatus.FINISHED && activeClaimedTrip != null) {
            val finalFareToSave = if (tripState.currentFare > 0.0) tripState.currentFare else tripState.baseFare
            val waitMins = tripState.waitingSeconds / 60.0
            val totalDist = tripState.distanceKm
            activeClaimedTrip?.id?.let { tripId ->
                pendingTripsViewModel.completeClaimedTrip(
                    tripId = tripId,
                    finalFare = finalFareToSave,
                    waitTimeMinutes = waitMins,
                    totalDistanceKm = totalDist
                )
            }
            activeClaimedTrip = null
        }
    }

    // ADMIN AUTH PIN MODAL (FOR MASTER AND REGULAR ADMIN AUTH)
    if (showAdminAuthModal) {
        AdminAuthPinModal(
            onDismiss = { showAdminAuthModal = false },
            onPinSuccess = {
                showAdminAuthModal = false
                showAdminPanelModal = true
            },
            onVerifyPin = { enteredPin ->
                dispatchViewModel.verifyAdminPin(enteredPin)
            }
        )
    }

    // ADMIN PANEL MODAL (SUPABASE LOAD TRIP & OFFLINE KEY GENERATOR)
    if (showAdminPanelModal) {
        AdminPanelModal(
            onDismiss = {
                showAdminPanelModal = false
                dispatchViewModel.logoutAdmin()
            },
            pendingTripsViewModel = pendingTripsViewModel,
            dispatchViewModel = dispatchViewModel
        )
    }

    // PENDING TRIPS DISPATCH BOARD MODAL (SUPABASE REALTIME FEED)
    if (showPendingTripsModal) {
        PendingTripsBoardModal(
            pendingTripsViewModel = pendingTripsViewModel,
            driverProfile = driverProfile,
            onDismiss = { showPendingTripsModal = false },
            onClaimTripSuccess = { trip ->
                activeClaimedTrip = trip
                manualBaseFareInput = trip.baseFare.toString()
                manualRatePerKmInput = trip.perKmFare.toString()
            }
        )
    }

    // TARIFF SETTINGS MODAL (ADJUST BASE, PER-KM, AND WAIT TIMES)
    if (showTariffSettingsModal) {
        val currentBase = settingsViewModel?.baseFare?.collectAsStateWithLifecycle()?.value ?: tripState.baseFare
        val currentKmRate = settingsViewModel?.farePerKm?.collectAsStateWithLifecycle()?.value ?: tripState.farePerKm
        val currentWaitRate = settingsViewModel?.waitFarePerMin?.collectAsStateWithLifecycle()?.value ?: tripState.waitFarePerMin
        TariffSettingsModal(
            currentBaseFare = currentBase,
            currentFarePerKm = currentKmRate,
            currentWaitFarePerMin = currentWaitRate,
            currency = tripState.currency,
            onDismiss = { showTariffSettingsModal = false },
            onSave = { base, perKm, wait ->
                settingsViewModel?.updateBaseFare(base)
                settingsViewModel?.updateFarePerKm(perKm)
                settingsViewModel?.updateWaitFarePerMin(wait)
                manualBaseFareInput = base.toString()
                manualRatePerKmInput = perKm.toString()
                Toast.makeText(context, "Tariff updated: Base ₹$base | ₹$perKm/KM | ₹$wait/min", Toast.LENGTH_SHORT).show()
                showTariffSettingsModal = false
            }
        )
    }

    // VOICE TTS SETTINGS MODAL (SELECT FROM 4 VOICES: 2 MALE, 2 FEMALE)
    if (showVoiceTtsModal) {
        val currentVoiceProfile = settingsViewModel?.ttsVoiceProfile?.collectAsStateWithLifecycle()?.value ?: "FEMALE_1"
        VoiceTtsSettingsModal(
            currentProfileId = currentVoiceProfile,
            onDismiss = { showVoiceTtsModal = false },
            onSelectProfile = { profileId, pitch, speechRate ->
                settingsViewModel?.updateTtsVoiceProfile(profileId)
                settingsViewModel?.updateTtsPitch(pitch)
                settingsViewModel?.updateTtsSpeechRate(speechRate)
                Toast.makeText(context, "Voice updated to $profileId", Toast.LENGTH_SHORT).show()
                showVoiceTtsModal = false
            }
        )
    }

    // MASTER OTP INPUT DIALOG TO START TAXIMETER
    if (showMasterOtpModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showMasterOtpModal = false
                isOtpError = false
            }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Master OTP Required",
                                tint = brandRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "MASTER OTP REQUIRED (2604)",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF1E1E1E)
                    )

                    Text(
                        text = "Driver must enter Master OTP (2604) to unlock taximeter and start ride.",
                        fontSize = 13.sp,
                        color = Color(0xFF616161),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    OutlinedTextField(
                        value = otpInputText,
                        onValueChange = {
                            otpInputText = it
                            isOtpError = false
                        },
                        label = { Text("Enter Master OTP (2604)") },
                        singleLine = true,
                        isError = isOtpError,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = brandRed,
                            focusedTextColor = Color(0xFF1E1E1E),
                            unfocusedTextColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_otp_input")
                    )

                    if (isOtpError) {
                        Text(
                            text = "❌ Invalid OTP! Enter 2604.",
                            color = brandRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showMasterOtpModal = false
                                isOtpError = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (otpInputText.trim() == "2604" || otpInputText.trim() == "1404" || otpInputText.trim() == "1981" || otpInputText.trim() == "140423") {
                                    val customBf = manualBaseFareInput.toDoubleOrNull() ?: 0.0
                                    val customRkm = manualRatePerKmInput.toDoubleOrNull() ?: 0.0
                                    val rateHr = manualRatePerHourInput.toDoubleOrNull() ?: 0.0
                                    val dBeta = manualDriverBetaInput.toDoubleOrNull() ?: 0.0

                                    viewModel.startTripWithCustomRates(
                                        customBaseFare = customBf,
                                        customRatePerKm = customRkm,
                                        rideType = selectedManualRideType,
                                        ratePerHour = rateHr,
                                        driverBeta = dBeta
                                    )
                                    showMasterOtpModal = false
                                    isOtpError = false
                                } else {
                                    isOtpError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verify_otp_start_meter_button")
                        ) {
                            Text("START METER", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // EXTRA CHARGES MODAL (TOLL, PERMIT, PARKING)
    if (showExtraChargesModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showExtraChargesModal = false }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ADD / UPDATE EXTRA CHARGES",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = brandRed,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Enter extra charges incurred during trip",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = tollInput,
                        onValueChange = { tollInput = it },
                        label = { Text("Toll Charges (${tripState.currency})") },
                        placeholder = { Text("e.g. 120") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandRed,
                            focusedLabelColor = brandRed
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("input_extra_toll")
                    )

                    OutlinedTextField(
                        value = permitInput,
                        onValueChange = { permitInput = it },
                        label = { Text("Permit Charges (${tripState.currency})") },
                        placeholder = { Text("e.g. 200") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandRed,
                            focusedLabelColor = brandRed
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("input_extra_permit")
                    )

                    OutlinedTextField(
                        value = parkingInput,
                        onValueChange = { parkingInput = it },
                        label = { Text("Parking Charges (${tripState.currency})") },
                        placeholder = { Text("e.g. 50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandRed,
                            focusedLabelColor = brandRed
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("input_extra_parking")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExtraChargesModal = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val t = tollInput.toDoubleOrNull() ?: 0.0
                                val p = permitInput.toDoubleOrNull() ?: 0.0
                                val pk = parkingInput.toDoubleOrNull() ?: 0.0
                                viewModel.updateExtraCharges(t, p, pk)
                                showExtraChargesModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_extra_charges_button")
                        ) {
                            Text("APPLY", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // DRIVER MODE - TAXI METER
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Red circle icon with white taxi image (Hidden 5-tap gesture on brand icon)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(brandRed)
                            .clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastBrandTapTime > 2000L) {
                                    brandTapCount = 1
                                } else {
                                    brandTapCount++
                                }
                                lastBrandTapTime = now
                                if (brandTapCount >= 5) {
                                    brandTapCount = 0
                                    showAdminAuthModal = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Taxi Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Brand Title & Driver info (Clickable 5 taps for hidden admin gesture)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastBrandTapTime > 2000L) {
                                    brandTapCount = 1
                                } else {
                                    brandTapCount++
                                }
                                lastBrandTapTime = now
                                if (brandTapCount >= 5) {
                                    brandTapCount = 0
                                    showAdminAuthModal = true
                                }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Taxi Meter ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "By Get Taxi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = brandRed
                            )
                        }
                        Text(
                            text = "${driverProfile.vehiclePlate.ifBlank { "TN-38-BZ-4411" }} • ${driverProfile.driverName.ifBlank { "Driver" }} (${driverProfile.driverId.ifBlank { "DRV-001" }})",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1
                        )
                    }

                    // Compact Online/Offline Duty Status Toggle Switch
                    Surface(
                        color = if (driverProfile.isOnline) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (driverProfile.isOnline) Color(0xFF86EFAC) else Color(0xFFFECACA)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable {
                                val newOnline = !driverProfile.isOnline
                                dispatchViewModel.updateDriverProfile(
                                    driverProfile.copy(
                                        isOnline = newOnline,
                                        status = if (newOnline) "AVAILABLE" else "OFFLINE"
                                    )
                                )
                            }
                            .testTag("compact_duty_status_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (driverProfile.isOnline) Color(0xFF16A34A) else Color(0xFFDC2626))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (driverProfile.isOnline) "ONLINE" else "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (driverProfile.isOnline) Color(0xFF15803D) else Color(0xFFB91C1C)
                            )
                            Switch(
                                checked = driverProfile.isOnline,
                                onCheckedChange = { isOnline ->
                                    dispatchViewModel.updateDriverProfile(
                                        driverProfile.copy(
                                            isOnline = isOnline,
                                            status = if (isOnline) "AVAILABLE" else "OFFLINE"
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF16A34A),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFDC2626)
                                ),
                                modifier = Modifier
                                    .scale(0.65f)
                                    .testTag("header_duty_switch")
                            )
                        }
                    }

                    // Hamburger Menu (≡)
                    Box {
                        IconButton(
                            onClick = { headerMenuExpanded = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("header_hamburger_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Main Menu",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = headerMenuExpanded,
                            onDismissRequest = { headerMenuExpanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .width(220.dp)
                        ) {
                            // 1. Admin Login (triggering PIN pad: 1005 Master / 1404 Regular)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Admin Login", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                                            Text("PIN: 1005 / 1404", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                },
                                onClick = {
                                    headerMenuExpanded = false
                                    showAdminAuthModal = true
                                },
                                modifier = Modifier.testTag("menu_admin_login")
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 2. Tariff Settings (to adjust base, per-km, and wait times)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Tariff Settings", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                                            Text("Base, Per-KM & Wait rates", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                },
                                onClick = {
                                    headerMenuExpanded = false
                                    showTariffSettingsModal = true
                                },
                                modifier = Modifier.testTag("menu_tariff_settings")
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 3. Interface Settings (toggle between Dark background and Light/White background)
                            val isDark = settingsViewModel?.isDarkMode?.collectAsStateWithLifecycle()?.value ?: false
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Interface Settings", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                                            Text(if (isDark) "Switch to Light Theme" else "Switch to Dark Theme", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                },
                                onClick = {
                                    headerMenuExpanded = false
                                    settingsViewModel?.let { vm ->
                                        vm.updateIsDarkMode(!vm.isDarkMode.value)
                                    }
                                },
                                modifier = Modifier.testTag("menu_interface_settings")
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // 4. Voice TTS Settings (select from 4 voices: 2 Male, 2 Female for announcements)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Voice TTS Settings", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                                            Text("4 Voices (2 Male, 2 Female)", fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                },
                                onClick = {
                                    headerMenuExpanded = false
                                    showVoiceTtsModal = true
                                },
                                modifier = Modifier.testTag("menu_voice_tts_settings")
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // SECURE SETTINGS ACCESS PIN DIALOG (PIN 2481)
            if (showSettingsPinModal) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = {
                        showSettingsPinModal = false
                        isSettingsPinError = false
                    }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Settings Lock",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = "SETTINGS ACCESS LOCKED",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color(0xFF1E1E1E)
                            )

                            Text(
                                text = "Enter Security PIN to manage base fares, per-km rates, and system parameters.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = settingsPinInput,
                                onValueChange = {
                                    settingsPinInput = it
                                    if (isSettingsPinError) isSettingsPinError = false
                                },
                                label = { Text("Enter Security PIN") },
                                singleLine = true,
                                isError = isSettingsPinError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E1E1E),
                                    unfocusedTextColor = Color(0xFF1E1E1E),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = brandRed,
                                    unfocusedBorderColor = Color(0xFFBDBDBD)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_pin_input")
                            )

                            if (isSettingsPinError) {
                                Text(
                                    text = "⚠️ Invalid Security PIN. Please try again.",
                                    color = brandRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showSettingsPinModal = false
                                        isSettingsPinError = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("CANCEL")
                                }

                                Button(
                                    onClick = {
                                        if (settingsPinInput.trim() == "2604" || settingsPinInput.trim() == "1974" || settingsPinInput.trim() == "1981" || settingsPinInput.trim() == "140423") {
                                            showSettingsPinModal = false
                                            isSettingsPinError = false
                                            onNavigateToSettings()
                                        } else {
                                            isSettingsPinError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("unlock_settings_button")
                                ) {
                                    Text("UNLOCK", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // DRIVER ASSIGNED ID BANNER CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
                    .clickable { onNavigateToProfile() }
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // DRIVER AVATAR
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE))
                                .border(1.5.dp, brandRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (driverProfile.photoUri.isNotBlank()) {
                                val imageModel = if (driverProfile.photoUri.startsWith("data:image/jpeg;base64,")) {
                                    val b64 = driverProfile.photoUri.substringAfter("base64,")
                                    try {
                                        android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                    } catch (e: Exception) {
                                        driverProfile.photoUri
                                    }
                                } else {
                                    driverProfile.photoUri
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "My Profile Photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = brandRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = driverProfile.driverName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Plate: ${driverProfile.vehiclePlate} • ${driverProfile.phoneNumber}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFFFD600),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = driverProfile.driverId,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // DRIVER SUSPENSION ALERT BANNER
            if (!driverProfile.isActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Suspended",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "DRIVER ACCOUNT SUSPENDED",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "Your account is temporarily suspended by Master Admin. You cannot claim new dispatch trips.",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            // PENDING TRIPS DISPATCH BOARD / CLAIMED TRIP ACTIVE CARD (SUPABASE CLOUD)
            // Hidden once the ride starts to conserve screen real estate
            val isRideActive = tripState.status == TripStatus.RUNNING || tripState.status == TripStatus.PAUSED
            if (!isRideActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, if (activeClaimedTrip != null) Color(0xFF10B981) else brandRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("pending_trips_dispatch_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (activeClaimedTrip != null) Color(0xFF10B981).copy(alpha = 0.2f) else brandRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (activeClaimedTrip != null) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = if (activeClaimedTrip != null) Color(0xFF10B981) else brandRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (activeClaimedTrip != null) "CLAIMED TRIP ACTIVE" else "PENDING DISPATCH TRIPS",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (activeClaimedTrip == null && pendingTripsList.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = brandRed,
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = "${pendingTripsList.size}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (activeClaimedTrip != null) "Custom base & km rates loaded into meter" else if (pendingTripsList.isNotEmpty()) "${pendingTripsList.size} trip(s) available to claim" else "Bash Cloud Live • No pending trips",
                                        fontSize = 11.sp,
                                        color = if (activeClaimedTrip != null) Color(0xFF10B981) else Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = { showPendingTripsModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_pending_trips_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (pendingTripsList.isNotEmpty()) "VIEW (${pendingTripsList.size})" else "VIEW BOARD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (activeClaimedTrip != null) {
                            val trip = activeClaimedTrip!!
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Customer: ${trip.customerPhone} ${trip.customerName?.let { "($it)" } ?: ""}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!trip.pickupLocation.isNullOrBlank() || !trip.dropLocation.isNullOrBlank()) {
                                            Text(
                                                text = "${trip.pickupLocation ?: "Pickup"} ➔ ${trip.dropLocation ?: "Drop"}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Text(
                                            text = "Tariff Loaded: Base ₹${trip.baseFare} | Rate ₹${trip.perKmFare}/KM",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = brandRed
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val cleanNum = trip.customerPhone.trim().replace(" ", "")
                                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNum")).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(dialIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("call_claimed_customer_button")
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("CALL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF10B981))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = "Locked Active Trip", tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("LOCKED", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status & GPS indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GPS DIAGNOSTICS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (tripState.latitude != null) Color(0xFF00E676) else Color(0xFFFF5252),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tripState.latitude != null) "GPS Signal Active" else "Acquiring GPS...",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Vacant / Hired badge
                Surface(
                    color = when (tripState.status) {
                        TripStatus.RUNNING -> brandRed
                        TripStatus.PAUSED -> Color(0xFFFFD600)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = when (tripState.status) {
                            TripStatus.RUNNING -> "HIRED / ON RIDE"
                            TripStatus.PAUSED -> "TRIP PAUSED"
                            else -> "VACANT / READY"
                        },
                        color = when (tripState.status) {
                            TripStatus.RUNNING -> Color.White
                            TripStatus.PAUSED -> Color.Black
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // WHITE MAIN CONTAINER CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // CATEGORY SELECTOR CONTAINER BAR
                    Surface(
                        color = Color(0xFF3F4855),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LOCAL Tab
                            Surface(
                                color = if (selectedManualRideType == "LOCAL_RIDE") brandRed else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedManualRideType = "LOCAL_RIDE" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "LOCAL",
                                        color = if (selectedManualRideType == "LOCAL_RIDE") Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Base ₹${manualBaseFareInput.ifBlank { tripState.baseFare.toInt().toString() }}",
                                        color = if (selectedManualRideType == "LOCAL_RIDE") Color.White else Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // HOURLY Tab
                            Surface(
                                color = if (selectedManualRideType == "HOURLY_RENTAL") brandRed else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedManualRideType = "HOURLY_RENTAL" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "HOURLY",
                                        color = if (selectedManualRideType == "HOURLY_RENTAL") Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "₹350/hr",
                                        color = if (selectedManualRideType == "HOURLY_RENTAL") Color.White else Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // OUTSTATION Tab
                            Surface(
                                color = if (selectedManualRideType == "OUTSTATION") brandRed else Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedManualRideType = "OUTSTATION" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "OUTSTATION",
                                        color = if (selectedManualRideType == "OUTSTATION") Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "₹18/km",
                                        color = if (selectedManualRideType == "OUTSTATION") Color.White else Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Quick Settings / Tariff Toggle Icon Button
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { showTariffSettingsModal = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Tune, contentDescription = "Custom Tariff", tint = brandRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // MAIN FARE METER READOUT BOX (DEEP DARK CHARCOAL NAVY)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Top status header line inside meter box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (tripState.status == TripStatus.RUNNING) brandRed else Color(0xFF94A3B8))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (tripState.status) {
                                            TripStatus.RUNNING -> "HIRED • RIDE IN PROGRESS"
                                            TripStatus.PAUSED -> "PAUSED • METER HOLD"
                                            else -> "VACANT • LOCAL READY"
                                        },
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Surface(
                                    color = Color(0xFF064E3B),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Satellite, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("GPS LIVE", color = Color(0xFF34D399), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Center giant fare display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tripState.currency,
                                    color = brandRed,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2f", tripState.currentFare),
                                    color = Color.White,
                                    fontSize = 58.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.testTag("fare_text")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom stats row inside meter box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    Text("Base: ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text("${tripState.currency}${String.format(Locale.US, "%.2f", tripState.baseFare)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row {
                                    Text("Dist: ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text("${tripState.currency}${String.format(Locale.US, "%.2f", tripState.distanceKm * tripState.farePerKm)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row {
                                    Text("Wait: ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text("${tripState.currency}${String.format(Locale.US, "%.2f", (tripState.waitingSeconds / 60.0) * tripState.waitFarePerMin)}", color = brandRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // PRE-RIDE LOCK: Speedometer and distance trackers remain completely hidden until ride starts
                    if (isRideActive) {
                        // 2x2 GRID STATISTICS CARDS (ACTIVE TRIP)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Card 1: SPEED
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = brandRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SPEED", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f", tripState.speedKmH),
                                            color = Color(0xFF0F172A),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("KM/h", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("GPS Active", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                            }

                            // Card 2: DIST
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DIST", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = String.format(Locale.US, "%.2f", tripState.distanceKm),
                                            color = Color(0xFF0F172A),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("KM", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Calibrated", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Card 3: TIME
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("TIME", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatDuration(tripState.durationSeconds).substring(3),
                                        color = Color(0xFF0F172A),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Duration", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                            }

                            // Card 4: WAIT
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Sensors, contentDescription = null, tint = brandRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("WAIT", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatDuration(tripState.waitingSeconds).substring(3),
                                        color = brandRed,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Stationary", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        // PRE-RIDE LOCK: Inactive speedometer and trackers until "Start the Ride" is pressed
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .testTag("pre_ride_locked_panel")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(
                                        text = "PRE-RIDE LOCK ACTIVE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Speedometer & distance tracking will unlock upon starting trip",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // ADD-ON CHARGES ACCORDION ROW
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable { showExtraChargesModal = true }
                            .testTag("btn_add_extra_charges")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add-on Charges (Tolls / Permit / Parking)",
                                    color = Color(0xFF334155),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }

                    // MAIN VIBRANT RED ACTION BUTTON BAR
                    Button(
                        onClick = {
                            if (tripState.status == TripStatus.IDLE || tripState.status == TripStatus.FINISHED) {
                                otpInputText = ""
                                isOtpError = false
                                showMasterOtpModal = true
                            } else {
                                val finalFareToSave = if (tripState.currentFare > 0.0) tripState.currentFare else tripState.baseFare
                                activeClaimedTrip?.id?.let { tripId ->
                                    pendingTripsViewModel.completeClaimedTrip(tripId, finalFareToSave)
                                }
                                dispatchViewModel.finishActiveTrip(finalFareToSave)
                                viewModel.stopTrip()
                                if (driverProfile.isEmergencyOneTime) {
                                    dispatchViewModel.logoutEmergencyDriver()
                                    Toast.makeText(
                                        context,
                                        "⚡ Emergency One-Time Session Completed. Profile logged out.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(top = 14.dp)
                            .testTag(if (tripState.status == TripStatus.IDLE || tripState.status == TripStatus.FINISHED) "start_trip_button" else "stop_trip_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (tripState.status == TripStatus.IDLE || tripState.status == TripStatus.FINISHED) Icons.Default.PlayArrow else Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (tripState.status == TripStatus.IDLE || tripState.status == TripStatus.FINISHED) "START THE RIDE" else "COMPLETE THE TRIP",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            }

                            Surface(
                                color = Color(0xFF991B1B),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("OTP VERIFIED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

            // LIVE ROUTE MAP VIEW (AT BOTTOM OF MAIN CARD)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔴 GPS MAP LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = brandRed,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (tripState.latitude != null) "Accurate GPS tracking" else "Acquiring satellites...",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    TripMapView(
                        tripState = tripState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // RIDE TIMINGS PANEL
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ELAPSED RIDE TIME",
                            color = brandRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.0.sp
                        )
                        Text(
                            text = formatDuration(tripState.durationSeconds),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (tripState.status == TripStatus.RUNNING) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFEBEE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Active ride indicator",
                                tint = brandRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TRIP HISTORIES HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT COMPLETED TRIPS",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.0.sp
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ROOM LOCAL DB (${allTrips.size})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // TRIPS HISTORY LIST
            if (allTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recorded trips yet.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trips_history_list")
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allTrips.forEach { trip ->
                        HistoryTripRow(
                            trip = trip,
                            currency = tripState.currency,
                            onRowClick = { onNavigateToReceipt(trip.id) },
                            onDelete = { viewModel.deleteTrip(trip.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTripRow(
    trip: TripEntity,
    currency: String,
    onRowClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(trip.startTime))
    val brandRed = TaxiRedPrimary

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", trip.distanceKm)} KM",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${trip.durationSeconds / 60}m ${trip.durationSeconds % 60}s",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currency${String.format(Locale.US, "%.2f", trip.totalFare)}",
                    color = brandRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(totalSec: Long): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
}
