package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PendingTrip
import com.example.security.ActivationSecurityManager
import com.example.viewmodel.PendingTripsViewModel
import kotlin.random.Random

@Composable
fun AdminPanelModal(
    onDismiss: () -> Unit,
    pendingTripsViewModel: PendingTripsViewModel = viewModel(),
    onResetProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Load Trip Form States
    var custPhone by remember { mutableStateOf("") }
    var custName by remember { mutableStateOf("") }
    var pickupLoc by remember { mutableStateOf("") }
    var dropLoc by remember { mutableStateOf("") }
    var tripOtp by remember { mutableStateOf((1000 + Random.nextInt(9000)).toString()) }
    var baseFareInput by remember { mutableStateOf("80.0") }
    var perKmFareInput by remember { mutableStateOf("28.0") }
    var loadTripStatusMessage by remember { mutableStateOf<String?>(null) }
    var isLoadTripError by remember { mutableStateOf(false) }

    val adminTrips by pendingTripsViewModel.adminTrips.collectAsState()
    val isSupabaseLoading by pendingTripsViewModel.isLoading.collectAsState()

    // Offline Key Generator States
    var inputDeviceId by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf("") }
    var generationHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val currentDeviceId = remember { ActivationSecurityManager.getDeviceId(context) }
    var isDeviceCurrentlyActivated by remember { mutableStateOf(ActivationSecurityManager.isActivated(context)) }

    val redBrand = Color(0xFFE11D48)
    val darkRed = Color(0xFF9F1239)

    LaunchedEffect(Unit) {
        pendingTripsViewModel.refreshAdminTrips()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color(0xFF0B1120)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar
                Surface(
                    color = Color(0xFF0F172A),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(redBrand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "MASTER ADMIN DISPATCH",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Supabase Cloud Dispatch & Security",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_admin_panel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        // Tab Row
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color(0xFF0F172A),
                            contentColor = redBrand,
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(
                                        text = "LOAD TRIP",
                                        fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Text(
                                        text = "KEY GENERATOR",
                                        fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Text(
                                        text = "SECURITY",
                                        fontWeight = if (selectedTab == 2) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // 🚀 TAB 0: LOAD TRIP MODULE (SUPABASE DISPATCH)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = redBrand)
                                        Text(
                                            text = "LOAD NEW TRIP TO SUPABASE",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = "Create and broadcast a pending customer trip globally to all active driver taxi meters.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 16.sp
                                    )

                                    // Customer Phone (Mandatory)
                                    OutlinedTextField(
                                        value = custPhone,
                                        onValueChange = { custPhone = it },
                                        label = { Text("Customer Phone Number * (Mandatory)") },
                                        placeholder = { Text("+91 9043743777") },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = redBrand) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        colors = adminTextFieldColors(redBrand),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_cust_phone_input")
                                    )

                                    // Customer Name (Optional)
                                    OutlinedTextField(
                                        value = custName,
                                        onValueChange = { custName = it },
                                        label = { Text("Customer Name (Optional)") },
                                        placeholder = { Text("e.g. Ramesh Kumar") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = redBrand) },
                                        singleLine = true,
                                        colors = adminTextFieldColors(redBrand),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_cust_name_input")
                                    )

                                    // Pickup Location (Optional)
                                    OutlinedTextField(
                                        value = pickupLoc,
                                        onValueChange = { pickupLoc = it },
                                        label = { Text("Pickup Location (Optional)") },
                                        placeholder = { Text("e.g. Coimbatore Airport") },
                                        leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981)) },
                                        singleLine = true,
                                        colors = adminTextFieldColors(redBrand),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_pickup_loc_input")
                                    )

                                    // Drop Location (Optional)
                                    OutlinedTextField(
                                        value = dropLoc,
                                        onValueChange = { dropLoc = it },
                                        label = { Text("Drop Location (Optional)") },
                                        placeholder = { Text("e.g. Gandhipuram Bus Stand") },
                                        leadingIcon = { Icon(Icons.Default.PinDrop, contentDescription = null, tint = redBrand) },
                                        singleLine = true,
                                        colors = adminTextFieldColors(redBrand),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_drop_loc_input")
                                    )

                                    // Trip OTP (Mandatory with quick random generator)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = tripOtp,
                                            onValueChange = { tripOtp = it },
                                            label = { Text("Trip OTP * (Mandatory)") },
                                            placeholder = { Text("4-digit OTP") },
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            colors = adminTextFieldColors(redBrand),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("admin_trip_otp_input")
                                        )

                                        Button(
                                            onClick = {
                                                tripOtp = (1000 + Random.nextInt(9000)).toString()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                                        ) {
                                            Icon(Icons.Default.Shuffle, contentDescription = "Randomize", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Random", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Custom Base Fare & Per KM Fare (Numeric)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = baseFareInput,
                                            onValueChange = { baseFareInput = it },
                                            label = { Text("Base Fare (₹)") },
                                            placeholder = { Text("80.0") },
                                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = redBrand, modifier = Modifier.size(18.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            colors = adminTextFieldColors(redBrand),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("admin_base_fare_input")
                                        )

                                        OutlinedTextField(
                                            value = perKmFareInput,
                                            onValueChange = { perKmFareInput = it },
                                            label = { Text("Per KM Fare (₹)") },
                                            placeholder = { Text("28.0") },
                                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = redBrand, modifier = Modifier.size(18.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            colors = adminTextFieldColors(redBrand),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("admin_per_km_fare_input")
                                        )
                                    }

                                    if (loadTripStatusMessage != null) {
                                        Surface(
                                            color = if (isLoadTripError) Color(0xFF7F1D1D) else Color(0xFF064E3B),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isLoadTripError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = loadTripStatusMessage ?: "",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Save Button
                                    Button(
                                        enabled = !isSupabaseLoading,
                                        onClick = {
                                            val bFare = baseFareInput.toDoubleOrNull() ?: 80.0
                                            val kmFare = perKmFareInput.toDoubleOrNull() ?: 28.0

                                            pendingTripsViewModel.loadTripToSupabase(
                                                customerPhone = custPhone,
                                                customerName = custName,
                                                pickupLocation = pickupLoc,
                                                dropLocation = dropLoc,
                                                tripOtp = tripOtp,
                                                baseFare = bFare,
                                                perKmFare = kmFare,
                                                onSuccess = {
                                                    isLoadTripError = false
                                                    loadTripStatusMessage = "✅ Trip Broadcasted Successfully! OTP: $tripOtp | Base: ₹$bFare | Per KM: ₹$kmFare"
                                                    Toast.makeText(context, "Trip Loaded to Supabase!", Toast.LENGTH_LONG).show()
                                                    custPhone = ""
                                                    custName = ""
                                                    pickupLoc = ""
                                                    dropLoc = ""
                                                    tripOtp = (1000 + Random.nextInt(9000)).toString()
                                                },
                                                onError = { err ->
                                                    isLoadTripError = true
                                                    loadTripStatusMessage = "❌ Error: $err"
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("broadcast_trip_button")
                                    ) {
                                        if (isSupabaseLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("BROADCASTING TO SUPABASE...", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                                        } else {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "BROADCAST TRIP TO DRIVER APPS",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Active & Recent Supabase Trips History
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SUPABASE PENDING TRIPS (${adminTrips.size})",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        IconButton(
                                            onClick = { pendingTripsViewModel.refreshAdminTrips() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    if (adminTrips.isEmpty()) {
                                        Text(
                                            text = "No trips in Supabase database yet. Load one above to broadcast.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    } else {
                                        adminTrips.take(6).forEach { trip ->
                                            AdminTripRow(trip)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // 🔑 TAB 1: OFFLINE ACTIVATION KEY GENERATOR TOOL
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = redBrand)
                                        Text(
                                            text = "OFFLINE ACTIVATION KEY GENERATOR",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = "Paste a driver's unique Device ID below to instantly generate their matching offline Activation Key.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 16.sp
                                    )

                                    // Input Device ID Field with Paste Button
                                    OutlinedTextField(
                                        value = inputDeviceId,
                                        onValueChange = { inputDeviceId = it },
                                        label = { Text("Driver's Hardware Device ID") },
                                        placeholder = { Text("e.g., 9774D56D682E549C") },
                                        singleLine = true,
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (inputDeviceId.isNotBlank()) {
                                                    IconButton(onClick = { inputDeviceId = "" }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clip = clipboardManager.primaryClip
                                                        if (clip != null && clip.itemCount > 0) {
                                                            val text = clip.getItemAt(0).text?.toString() ?: ""
                                                            if (text.isNotBlank()) {
                                                                inputDeviceId = text.trim()
                                                                Toast.makeText(context, "Device ID pasted!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentPaste,
                                                        contentDescription = "Paste from clipboard",
                                                        tint = redBrand
                                                    )
                                                }
                                            }
                                        },
                                        colors = adminTextFieldColors(redBrand),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_device_id_input")
                                    )

                                    // Quick Fill with Current Device ID
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { inputDeviceId = currentDeviceId },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "USE THIS PHONE'S ID ($currentDeviceId)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = redBrand
                                            )
                                        }
                                    }

                                    // Generate Key Button
                                    Button(
                                        onClick = {
                                            if (inputDeviceId.trim().isBlank()) {
                                                Toast.makeText(context, "Please enter or paste a Device ID", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val key = ActivationSecurityManager.generateActivationKeyForDevice(inputDeviceId)
                                            generatedKey = key
                                            generationHistory = listOf(Pair(inputDeviceId.trim(), key)) + generationHistory.take(4)
                                            Toast.makeText(context, "Activation Key Generated!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("generate_activation_key_button")
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "GENERATE ACTIVATION KEY",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }

                                    // Display Generated Key Result Card
                                    if (generatedKey.isNotBlank()) {
                                        Surface(
                                            color = Color(0xFF064E3B),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = "MATCHING ACTIVATION KEY:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF34D399),
                                                    letterSpacing = 1.sp
                                                )

                                                Surface(
                                                    color = Color(0xFF022C22),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF059669)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = generatedKey,
                                                        fontSize = 22.sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF6EE7B7),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier
                                                            .padding(vertical = 12.dp)
                                                            .testTag("generated_key_text")
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val clip = ClipData.newPlainText("Activation Key", generatedKey)
                                                            clipboardManager.setPrimaryClip(clip)
                                                            Toast.makeText(context, "Copied: $generatedKey", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.weight(1f).testTag("copy_generated_key_button")
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("COPY KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(
                                                                    Intent.EXTRA_TEXT,
                                                                    "Your GetTaxi Offline Activation Key is: $generatedKey\n\nEnter this key in your GetTaxi app to unlock full driver access."
                                                                )
                                                            }
                                                            context.startActivity(Intent.createChooser(shareIntent, "Share Activation Key"))
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("SHARE KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // 🔒 TAB 2: PRESERVED PASSWORD SYSTEM REFERENCE & LOCAL DEVICE
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = redBrand)
                                        Text(
                                            text = "PRESERVED PASSWORD SYSTEM",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = "All master keys and bypass passwords remain fully functional offline:",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PasswordRowDark("2604", "Standard Master OTP & Admin PIN")
                                        PasswordRowDark("1404", "Secondary Master Meter PIN")
                                        PasswordRowDark("1981", "Alternate Master PIN")
                                        PasswordRowDark("1974", "Master Admin Key")
                                        PasswordRowDark("Master1974", "Master Admin Key Alpha")
                                        PasswordRowDark("140423", "Date Master PIN")
                                    }
                                }
                            }

                            // THIS DEVICE ADMINISTRATION
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = redBrand)
                                        Text(
                                            text = "THIS DEVICE CONTROLS",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }

                                    Surface(
                                        color = if (isDeviceCurrentlyActivated) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isDeviceCurrentlyActivated) Color(0xFF10B981) else Color(0xFFEF4444)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Device Activation Status", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (isDeviceCurrentlyActivated) "ACTIVE (UNLOCKED)" else "LOCKED (UNACTIVATED)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isDeviceCurrentlyActivated) Color(0xFF34D399) else Color(0xFFFCA5A5)
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isDeviceCurrentlyActivated) Icons.Default.CheckCircle else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = if (isDeviceCurrentlyActivated) Color(0xFF34D399) else Color(0xFFFCA5A5)
                                            )
                                        }
                                    }

                                    // Deactivate Device Option
                                    OutlinedButton(
                                        onClick = {
                                            ActivationSecurityManager.clearActivation(context)
                                            isDeviceCurrentlyActivated = false
                                            Toast.makeText(context, "Device Deactivated. App will require Activation Key on restart.", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DEACTIVATE THIS DEVICE (LOCK TEST)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AdminTripRow(trip: PendingTrip) {
    val isPending = trip.status.lowercase() == "pending"
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isPending) Color(0xFFE11D48).copy(alpha = 0.4f) else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.customerPhone,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Surface(
                    color = if (isPending) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = trip.status.uppercase(),
                        color = if (isPending) Color(0xFFFB7185) else Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (!trip.customerName.isNullOrBlank()) {
                Text(
                    text = "Name: ${trip.customerName}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (!trip.pickupLocation.isNullOrBlank() || !trip.dropLocation.isNullOrBlank()) {
                Text(
                    text = "${trip.pickupLocation ?: "Pickup"} ➔ ${trip.dropLocation ?: "Drop"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "OTP: ${trip.tripOtp}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = "Base ₹${trip.baseFare} | Rate ₹${trip.perKmFare}/KM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}

@Composable
private fun PasswordRowDark(code: String, description: String) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = code,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun adminTextFieldColors(brandColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF0F172A),
    unfocusedContainerColor = Color(0xFF0F172A),
    focusedBorderColor = brandColor,
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = brandColor,
    unfocusedLabelColor = Color(0xFF94A3B8)
)
