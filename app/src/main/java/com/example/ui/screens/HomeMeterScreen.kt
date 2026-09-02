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
import com.example.ui.components.TripMapView
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

    var showExtraChargesModal by remember { mutableStateOf(false) }
    var tollInput by remember(tripState.tollCharges) { mutableStateOf(if (tripState.tollCharges > 0) tripState.tollCharges.toInt().toString() else "") }
    var permitInput by remember(tripState.permitCharges) { mutableStateOf(if (tripState.permitCharges > 0) tripState.permitCharges.toInt().toString() else "") }
    var parkingInput by remember(tripState.parkingCharges) { mutableStateOf(if (tripState.parkingCharges > 0) tripState.parkingCharges.toInt().toString() else "") }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        val currentTime = System.currentTimeMillis()
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
            activeClaimedTrip?.id?.let { tripId ->
                pendingTripsViewModel.completeClaimedTrip(tripId, finalFareToSave)
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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
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
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onNavigateToProfile() },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (driverProfile.photoUri.isNotBlank()) {
                                    val topImageModel = if (driverProfile.photoUri.startsWith("data:image/jpeg;base64,")) {
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
                                        model = topImageModel,
                                        contentDescription = "Driver Photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_get_taxi_vector),
                                        contentDescription = "Get Taxi Logo",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GET TAXI",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    // Driver Profile Button
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("driver_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Driver Profile",
                            tint = Color.White
                        )
                    }

                    // Floating Bubble Mini-Mode Quick Button
                    IconButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                com.example.service.FloatingBubbleService.start(context)
                                // Send app to home screen / background
                                val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                    addCategory(android.content.Intent.CATEGORY_HOME)
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(homeIntent)
                            }
                        },
                        modifier = Modifier.testTag("floating_bubble_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipToFront,
                            contentDescription = "Pop out Floating Bubble",
                            tint = Color.White
                        )
                    }

                    // Admin Panel Button with Master Key Security Check
                    Button(
                        onClick = {
                            showAdminAuthModal = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = brandRed
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("admin_panel_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ADMIN",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            settingsPinInput = ""
                            isSettingsPinError = false
                            showSettingsPinModal = true
                        },
                        modifier = Modifier
                            .testTag("settings_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = brandRed
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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

            // DUTY STATUS TOGGLE CARD
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (driverProfile.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, if (driverProfile.isOnline) Color(0xFFA5D6A7) else Color(0xFFFFCDD2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (driverProfile.isOnline) Color(0xFF2E7D32) else Color(0xFFD32F2F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (driverProfile.isOnline) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DUTY STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (driverProfile.isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            )
                            Text(
                                text = if (driverProfile.isOnline) "ONLINE / Available for Trips" else "OFFLINE / Off Duty",
                                fontSize = 12.sp,
                                color = if (driverProfile.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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
                            checkedTrackColor = Color(0xFF2E7D32),
                            checkedBorderColor = Color(0xFF1B5E20),
                            checkedIconColor = Color(0xFF2E7D32),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD32F2F),
                            uncheckedBorderColor = Color(0xFFB71C1C),
                            uncheckedIconColor = Color(0xFFD32F2F)
                        ),
                        thumbContent = {
                            if (driverProfile.isOnline) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = Color(0xFF2E7D32)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    )
                }
            }

            // PENDING TRIPS DISPATCH BOARD CARD (SUPABASE CLOUD)
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

            // LIVE ROUTE MAP VIEW (PROMINENT AT TOP OF HOME SCREEN)
            TripMapView(
                tripState = tripState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(vertical = 4.dp)
            )

            // GIGANTIC DARK BLUE / ALMOST BLACK FARE DISPLAY CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MainFareCardDarkBlue),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = BorderStroke(1.5.dp, MainFareCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    val fareHeaderLabel = when (tripState.rideType) {
                        "HOURLY_RENTAL" -> "HOURLY RENTAL (${tripState.currency}${tripState.ratePerHour.toInt()}/HR)"
                        "OUTSTATION" -> "OUTSTATION TRIP"
                        else -> "TOTAL RIDE FARE"
                    }
                    Text(
                        text = fareHeaderLabel,
                        color = brandRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 2.0.sp
                    )

                    Box(
                        modifier = Modifier.padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = String.format(Locale.US, "%.2f", tripState.currentFare),
                                color = Color.White,
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("fare_text")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tripState.currency,
                                color = brandRed,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Distance & Wait time boxes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Distance Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MainFareCardSurface, RoundedCornerShape(20.dp))
                                .border(1.dp, MainFareCardBorder, RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DISTANCE", color = brandRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", tripState.distanceKm),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("KM", color = brandRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Wait Time Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MainFareCardSurface, RoundedCornerShape(20.dp))
                                .border(1.dp, MainFareCardBorder, RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WAIT TIME", color = brandRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    val formattedWait = formatDuration(tripState.waitingSeconds).substring(3)
                                    Text(
                                        text = formattedWait,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("MIN", color = brandRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Extra Charges Trigger Card (Toll, Permit, Parking)
                    if (tripState.status == TripStatus.RUNNING || tripState.status == TripStatus.PAUSED) {
                        Surface(
                            color = MainFareCardSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MainFareCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .clickable { showExtraChargesModal = true }
                                .testTag("btn_add_extra_charges")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = brandRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "EXTRA CHARGES (Toll/Permit/Parking)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                    val totalExtra = tripState.tollCharges + tripState.permitCharges + tripState.parkingCharges
                                    Text(
                                        text = "Toll: ${tripState.currency}${tripState.tollCharges.toInt()} | Permit: ${tripState.currency}${tripState.permitCharges.toInt()} | Parking: ${tripState.currency}${tripState.parkingCharges.toInt()} (Total: ${tripState.currency}${totalExtra.toInt()})",
                                        fontSize = 11.sp,
                                        color = if (totalExtra > 0.0) brandRed else Color.LightGray,
                                        fontWeight = if (totalExtra > 0.0) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Extra Charges",
                                    tint = brandRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // TRIP CATEGORY SELECTOR FOR MANUAL METER
            if (tripState.status == TripStatus.IDLE || tripState.status == TripStatus.FINISHED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SELECT TRIP CATEGORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = brandRed,
                            letterSpacing = 1.0.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modes = listOf(
                                "LOCAL_RIDE" to "🚕 Local",
                                "OUTSTATION" to "🛣️ Outstation",
                                "HOURLY_RENTAL" to "⏱️ Hourly"
                            )
                            modes.forEach { (typeKey, label) ->
                                val isSelected = selectedManualRideType == typeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedManualRideType = typeKey },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = brandRed,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CUSTOM TARIFF RATES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (selectedManualRideType == "HOURLY_RENTAL") "Per Hour Billing" else if (selectedManualRideType == "OUTSTATION") "Return Surcharge Applied" else "Standard Fare",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandRed
                            )
                        }

                        val tfColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = brandRed,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )

                        when (selectedManualRideType) {
                            "LOCAL_RIDE" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualBaseFareInput,
                                        onValueChange = { if (activeClaimedTrip == null) manualBaseFareInput = it },
                                        readOnly = activeClaimedTrip != null,
                                        label = { Text(if (activeClaimedTrip != null) "🔒 Locked Base (${tripState.currency})" else "Base Fare (${tripState.currency})", fontSize = 11.sp) },
                                        placeholder = { Text(String.format(Locale.US, "%.0f", tripState.baseFare), fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp)) },
                                        trailingIcon = { if (activeClaimedTrip != null) Icon(Icons.Default.Lock, contentDescription = "Locked by Dispatch", tint = brandRed) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors,
                                        modifier = Modifier.weight(1f).testTag("home_input_local_base_fare")
                                    )

                                    OutlinedTextField(
                                        value = manualRatePerKmInput,
                                        onValueChange = { if (activeClaimedTrip == null) manualRatePerKmInput = it },
                                        readOnly = activeClaimedTrip != null,
                                        label = { Text(if (activeClaimedTrip != null) "🔒 Locked Rate/KM (${tripState.currency})" else "Rate / KM (${tripState.currency})", fontSize = 11.sp) },
                                        placeholder = { Text(String.format(Locale.US, "%.0f", tripState.farePerKm), fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp)) },
                                        trailingIcon = { if (activeClaimedTrip != null) Icon(Icons.Default.Lock, contentDescription = "Locked by Dispatch", tint = brandRed) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors,
                                        modifier = Modifier.weight(1f).testTag("home_input_local_per_km")
                                    )
                                }
                                Text(
                                    text = if (activeClaimedTrip != null) "🔒 Tariff strictly locked to Dispatch Trip (Base ₹${activeClaimedTrip?.baseFare} + Rate ₹${activeClaimedTrip?.perKmFare}/KM)." else "💡 Local Tariff: Default Base ${tripState.currency}${String.format(Locale.US, "%.0f", tripState.baseFare)}, Rate ${tripState.currency}${String.format(Locale.US, "%.0f", tripState.farePerKm)}/KM. Enter custom values to override.",
                                    fontSize = 10.sp,
                                    color = if (activeClaimedTrip != null) brandRed else Color.DarkGray,
                                    fontWeight = if (activeClaimedTrip != null) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }

                            "OUTSTATION" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualDriverBetaInput,
                                        onValueChange = { manualDriverBetaInput = it },
                                        label = { Text("Driver Beta / Base (${tripState.currency})", fontSize = 11.sp) },
                                        placeholder = { Text("500", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors,
                                        modifier = Modifier.weight(1f).testTag("home_input_outstation_bata")
                                    )

                                    OutlinedTextField(
                                        value = manualRatePerKmInput,
                                        onValueChange = { manualRatePerKmInput = it },
                                        label = { Text("Per KM Rate (${tripState.currency})", fontSize = 11.sp) },
                                        placeholder = { Text("15", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors,
                                        modifier = Modifier.weight(1f).testTag("home_input_outstation_per_km")
                                    )
                                }
                                Text(
                                    text = "💡 Outstation Tariff: Default Driver Beta ${tripState.currency}500 + Rate ${tripState.currency}15/KM. Enter custom values to set custom price.",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }

                            "HOURLY_RENTAL" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualRatePerHourInput,
                                        onValueChange = { manualRatePerHourInput = it },
                                        label = { Text("Hourly Rate (${tripState.currency}/Hr)", fontSize = 11.sp) },
                                        placeholder = { Text("375", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors,
                                        modifier = Modifier.weight(1f).testTag("home_input_hourly_rate")
                                    )

                                    OutlinedTextField(
                                        value = manualRatePerKmInput,
                                        onValueChange = { manualRatePerKmInput = it },
                                        label = { Text("Extra Rate/KM (${tripState.currency})", fontSize = 11.sp) },
                                        placeholder = { Text("20", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors,
                                        modifier = Modifier.weight(1f).testTag("home_input_hourly_extra_per_km")
                                    )
                                }
                                Text(
                                    text = "💡 Hourly Rental: Default ${tripState.currency}375/Hr (10km free/hr) + Extra ${tripState.currency}20/KM. Enter custom values to set custom price.",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ACTION BUTTONS (START METER / PAUSE / END TRIP)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 4.dp)
            ) {
                AnimatedContent(
                    targetState = tripState.status,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    },
                    label = "MainActionControls"
                ) { status ->
                    when (status) {
                        TripStatus.IDLE, TripStatus.FINISHED -> {
                            Button(
                                onClick = {
                                    otpInputText = ""
                                    isOtpError = false
                                    showMasterOtpModal = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brandRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("start_trip_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "START THE RIDE",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                            }
                        }

                        TripStatus.RUNNING, TripStatus.PAUSED -> {
                            Button(
                                onClick = {
                                    val finalFareToSave = if (tripState.currentFare > 0.0) tripState.currentFare else tripState.baseFare
                                    activeClaimedTrip?.id?.let { tripId ->
                                        pendingTripsViewModel.completeClaimedTrip(tripId, finalFareToSave)
                                    }
                                    dispatchViewModel.finishActiveTrip(finalFareToSave)
                                    viewModel.stopTrip()
                                    if (driverProfile.isEmergencyOneTime) {
                                        dispatchViewModel.logoutEmergencyDriver()
                                        android.widget.Toast.makeText(
                                            context,
                                            "⚡ Emergency One-Time Session Completed. Profile logged out. Contact Master Admin to set up full profile.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brandRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("stop_trip_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("COMPLETE THE TRIP", fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.5.sp)
                                }
                            }
                        }
                    }
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
