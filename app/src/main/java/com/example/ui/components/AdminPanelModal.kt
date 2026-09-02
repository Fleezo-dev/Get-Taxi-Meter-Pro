package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.DriverRemoteEntity
import com.example.data.model.PendingTrip
import com.example.security.ActivationSecurityManager
import com.example.viewmodel.AdminRole
import com.example.viewmodel.DispatchViewModel
import com.example.viewmodel.PendingTripsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Composable
fun AdminPanelModal(
    onDismiss: () -> Unit,
    pendingTripsViewModel: PendingTripsViewModel,
    dispatchViewModel: DispatchViewModel? = null,
    onResetProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val adminRole = dispatchViewModel?.authenticatedAdminRole?.collectAsState()?.value ?: AdminRole.MASTER_ADMIN
    val adminName = dispatchViewModel?.authenticatedAdminName?.collectAsState()?.value ?: "Master Admin"

    val isMasterAdmin = adminRole == AdminRole.MASTER_ADMIN

    var selectedTab by remember { mutableIntStateOf(0) }

    // Load Trip Form States
    var custPhone by remember { mutableStateOf("") }
    var custName by remember { mutableStateOf("") }
    var pickupLoc by remember { mutableStateOf("") }
    var dropLoc by remember { mutableStateOf("") }
    var tripOtp by remember { mutableStateOf((1000 + Random.nextInt(9000)).toString()) }
    var baseFareInput by remember { mutableStateOf("80.0") }
    var perKmFareInput by remember { mutableStateOf("28.0") }
    var createdByInput by remember(adminName) { mutableStateOf(adminName) }
    var loadTripStatusMessage by remember { mutableStateOf<String?>(null) }
    var isLoadTripError by remember { mutableStateOf(false) }

    val allTrips by pendingTripsViewModel.adminTrips.collectAsState()
    val allDrivers by pendingTripsViewModel.allDrivers.collectAsState()
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
        pendingTripsViewModel.refreshDrivers()
    }

    val sqlSchemaText = """
-- ========================================================
-- BASH CLOUD TAXI METER - SUPABASE DATABASE SCHEMA
-- ========================================================

-- 1. Create Drivers Table (Driver Directory & Remote Suspension)
CREATE TABLE IF NOT EXISTS public.drivers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    driver_id TEXT,
    driver_name TEXT NOT NULL,
    driver_phone TEXT UNIQUE NOT NULL,
    vehicle_number TEXT,
    vehicle_type TEXT DEFAULT 'Sedan',
    is_active BOOLEAN DEFAULT TRUE,
    status TEXT DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Create Pending Trips Table (Dispatch Orders & Commission)
CREATE TABLE IF NOT EXISTS public.pending_trips (
    id BIGSERIAL PRIMARY KEY,
    customer_name TEXT,
    customer_phone TEXT NOT NULL,
    pickup_location TEXT,
    drop_location TEXT,
    trip_otp TEXT NOT NULL,
    base_fare DOUBLE PRECISION NOT NULL DEFAULT 80.0,
    per_km_fare DOUBLE PRECISION NOT NULL DEFAULT 28.0,
    status TEXT NOT NULL DEFAULT 'pending', -- 'pending', 'claimed', 'completed'
    claimed_by_driver_id TEXT,
    claimed_by_driver_name TEXT,
    claimed_by_driver_phone TEXT,
    created_by TEXT DEFAULT 'Master Admin',
    final_fare DOUBLE PRECISION DEFAULT 0.0,
    commission_amount DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Enable Realtime & RLS Policies (Allow App Access)
ALTER TABLE public.drivers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pending_trips ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public full access to drivers" ON public.drivers FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public full access to pending_trips" ON public.pending_trips FOR ALL USING (true) WITH CHECK (true);
    """.trimIndent()

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
                                        .background(if (isMasterAdmin) redBrand else Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMasterAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = "Admin",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isMasterAdmin) "MASTER ADMIN DISPATCH" else "REGULAR ADMIN DISPATCH",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isMasterAdmin) redBrand.copy(alpha = 0.2f) else Color(0xFF2563EB).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isMasterAdmin) "👑 MASTER" else "👤 REGULAR",
                                                color = if (isMasterAdmin) redBrand else Color(0xFF60A5FA),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Logged in as: $adminName | Bash Cloud Live",
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
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Navigation Tabs
                        val tabs = if (isMasterAdmin) {
                            listOf(
                                "📋 Load Trip",
                                "👥 Drivers",
                                "📊 Commission",
                                "🔐 Key Gen",
                                "🗄️ SQL"
                            )
                        } else {
                            listOf(
                                "📋 Load Trip",
                                "📦 My Trips",
                                "🔐 Key Gen"
                            )
                        }

                        ScrollableTabRow(
                            selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex),
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White,
                            edgePadding = 16.dp,
                            indicator = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Color(0xFF64748B)
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                        .background(
                                            color = if (isSelected) (if (isMasterAdmin) redBrand else Color(0xFF2563EB)) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // TAB CONTENT
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (isMasterAdmin) {
                        when (selectedTab) {
                            0 -> LoadTripTabContent(
                                custPhone = custPhone,
                                onCustPhoneChange = { custPhone = it },
                                custName = custName,
                                onCustNameChange = { custName = it },
                                pickupLoc = pickupLoc,
                                onPickupLocChange = { pickupLoc = it },
                                dropLoc = dropLoc,
                                onDropLocChange = { dropLoc = it },
                                tripOtp = tripOtp,
                                onRegenerateOtp = { tripOtp = (1000 + Random.nextInt(9000)).toString() },
                                baseFareInput = baseFareInput,
                                onBaseFareChange = { baseFareInput = it },
                                perKmFareInput = perKmFareInput,
                                onPerKmFareChange = { perKmFareInput = it },
                                createdByInput = createdByInput,
                                onCreatedByChange = { createdByInput = it },
                                isSupabaseLoading = isSupabaseLoading,
                                loadTripStatusMessage = loadTripStatusMessage,
                                isLoadTripError = isLoadTripError,
                                onLoadTrip = {
                                    val bf = baseFareInput.toDoubleOrNull() ?: 80.0
                                    val pk = perKmFareInput.toDoubleOrNull() ?: 28.0
                                    pendingTripsViewModel.loadTripToSupabase(
                                        customerPhone = custPhone,
                                        customerName = custName,
                                        pickupLocation = pickupLoc,
                                        dropLocation = dropLoc,
                                        tripOtp = tripOtp,
                                        baseFare = bf,
                                        perKmFare = pk,
                                        createdBy = createdByInput,
                                        onSuccess = {
                                            isLoadTripError = false
                                            loadTripStatusMessage = "✅ Trip broadcasted to Bash Cloud Live for driver claiming!"
                                            custPhone = ""
                                            custName = ""
                                            pickupLoc = ""
                                            dropLoc = ""
                                            tripOtp = (1000 + Random.nextInt(9000)).toString()
                                        },
                                        onError = { err ->
                                            isLoadTripError = true
                                            loadTripStatusMessage = "❌ $err"
                                        }
                                    )
                                },
                                redBrand = redBrand
                            )

                            1 -> DriversManagementTabContent(
                                drivers = allDrivers,
                                isLoading = isSupabaseLoading,
                                onRefresh = { pendingTripsViewModel.refreshDrivers() },
                                onToggleActive = { driver, newActive ->
                                    pendingTripsViewModel.toggleDriverSuspension(driver.driverPhone, newActive) {
                                        Toast.makeText(context, if (newActive) "Driver Activated" else "Driver Suspended", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                redBrand = redBrand
                            )

                            2 -> CommissionTabContent(
                                trips = allTrips,
                                onRefresh = { pendingTripsViewModel.refreshAdminTrips() },
                                redBrand = redBrand
                            )

                            3 -> KeyGenTabContent(
                                currentDeviceId = currentDeviceId,
                                inputDeviceId = inputDeviceId,
                                onInputDeviceIdChange = { inputDeviceId = it },
                                generatedKey = generatedKey,
                                onGenerateKey = {
                                    val target = inputDeviceId.ifBlank { currentDeviceId }
                                    val key = ActivationSecurityManager.generateActivationKeyForDevice(target)
                                    generatedKey = key
                                    generationHistory = listOf(target to key) + generationHistory
                                },
                                isDeviceCurrentlyActivated = isDeviceCurrentlyActivated,
                                onActivateSelf = {
                                    val key = ActivationSecurityManager.generateActivationKeyForDevice(currentDeviceId)
                                    ActivationSecurityManager.setActivated(context, true, key)
                                    isDeviceCurrentlyActivated = true
                                    Toast.makeText(context, "✅ Activated Current Device!", Toast.LENGTH_SHORT).show()
                                },
                                onDeactivateSelf = {
                                    ActivationSecurityManager.clearActivation(context)
                                    isDeviceCurrentlyActivated = false
                                    Toast.makeText(context, "⚠️ Current Device Deactivated", Toast.LENGTH_SHORT).show()
                                },
                                onResetProfile = onResetProfile,
                                clipboardManager = clipboardManager,
                                context = context,
                                redBrand = redBrand
                            )

                            4 -> SqlSchemaTabContent(
                                sqlText = sqlSchemaText,
                                clipboardManager = clipboardManager,
                                context = context,
                                redBrand = redBrand
                            )
                        }
                    } else {
                        // Regular Admin View
                        when (selectedTab) {
                            0 -> LoadTripTabContent(
                                custPhone = custPhone,
                                onCustPhoneChange = { custPhone = it },
                                custName = custName,
                                onCustNameChange = { custName = it },
                                pickupLoc = pickupLoc,
                                onPickupLocChange = { pickupLoc = it },
                                dropLoc = dropLoc,
                                onDropLocChange = { dropLoc = it },
                                tripOtp = tripOtp,
                                onRegenerateOtp = { tripOtp = (1000 + Random.nextInt(9000)).toString() },
                                baseFareInput = baseFareInput,
                                onBaseFareChange = { baseFareInput = it },
                                perKmFareInput = perKmFareInput,
                                onPerKmFareChange = { perKmFareInput = it },
                                createdByInput = adminName,
                                onCreatedByChange = {},
                                isSupabaseLoading = isSupabaseLoading,
                                loadTripStatusMessage = loadTripStatusMessage,
                                isLoadTripError = isLoadTripError,
                                onLoadTrip = {
                                    val bf = baseFareInput.toDoubleOrNull() ?: 80.0
                                    val pk = perKmFareInput.toDoubleOrNull() ?: 28.0
                                    pendingTripsViewModel.loadTripToSupabase(
                                        customerPhone = custPhone,
                                        customerName = custName,
                                        pickupLocation = pickupLoc,
                                        dropLocation = dropLoc,
                                        tripOtp = tripOtp,
                                        baseFare = bf,
                                        perKmFare = pk,
                                        createdBy = adminName,
                                        onSuccess = {
                                            isLoadTripError = false
                                            loadTripStatusMessage = "✅ Trip loaded to Bash Cloud Live under $adminName!"
                                            custPhone = ""
                                            custName = ""
                                            pickupLoc = ""
                                            dropLoc = ""
                                            tripOtp = (1000 + Random.nextInt(9000)).toString()
                                        },
                                        onError = { err ->
                                            isLoadTripError = true
                                            loadTripStatusMessage = "❌ $err"
                                        }
                                    )
                                },
                                redBrand = Color(0xFF2563EB)
                            )

                            1 -> MyDispatchedTripsTabContent(
                                trips = allTrips.filter { it.createdBy == adminName || it.createdBy.isNullOrBlank() },
                                onRefresh = { pendingTripsViewModel.refreshAdminTrips() },
                                adminName = adminName
                            )

                            2 -> KeyGenTabContent(
                                currentDeviceId = currentDeviceId,
                                inputDeviceId = inputDeviceId,
                                onInputDeviceIdChange = { inputDeviceId = it },
                                generatedKey = generatedKey,
                                onGenerateKey = {
                                    val target = inputDeviceId.ifBlank { currentDeviceId }
                                    val key = ActivationSecurityManager.generateActivationKeyForDevice(target)
                                    generatedKey = key
                                    generationHistory = listOf(target to key) + generationHistory
                                },
                                isDeviceCurrentlyActivated = isDeviceCurrentlyActivated,
                                onActivateSelf = {
                                    val key = ActivationSecurityManager.generateActivationKeyForDevice(currentDeviceId)
                                    ActivationSecurityManager.setActivated(context, true, key)
                                    isDeviceCurrentlyActivated = true
                                    Toast.makeText(context, "✅ Activated Current Device!", Toast.LENGTH_SHORT).show()
                                },
                                onDeactivateSelf = {
                                    ActivationSecurityManager.clearActivation(context)
                                    isDeviceCurrentlyActivated = false
                                    Toast.makeText(context, "⚠️ Current Device Deactivated", Toast.LENGTH_SHORT).show()
                                },
                                onResetProfile = onResetProfile,
                                clipboardManager = clipboardManager,
                                context = context,
                                redBrand = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: LOAD TRIP CONTENT
// -------------------------------------------------------------
@Composable
private fun LoadTripTabContent(
    custPhone: String,
    onCustPhoneChange: (String) -> Unit,
    custName: String,
    onCustNameChange: (String) -> Unit,
    pickupLoc: String,
    onPickupLocChange: (String) -> Unit,
    dropLoc: String,
    onDropLocChange: (String) -> Unit,
    tripOtp: String,
    onRegenerateOtp: () -> Unit,
    baseFareInput: String,
    onBaseFareChange: (String) -> Unit,
    perKmFareInput: String,
    onPerKmFareChange: (String) -> Unit,
    createdByInput: String,
    onCreatedByChange: (String) -> Unit,
    isSupabaseLoading: Boolean,
    loadTripStatusMessage: String?,
    isLoadTripError: Boolean,
    onLoadTrip: () -> Unit,
    redBrand: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = redBrand, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISPATCH NEW TRIP TO DRIVERS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "Trip will broadcast instantly to all online drivers. Tariff rates will be locked to prevent driver tampering.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = custPhone,
                    onValueChange = onCustPhoneChange,
                    label = { Text("Customer Mobile Number *") },
                    placeholder = { Text("e.g. 9043743777") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = redBrand) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = adminTextFieldColors(redBrand),
                    modifier = Modifier.fillMaxWidth().testTag("admin_input_cust_phone")
                )

                OutlinedTextField(
                    value = custName,
                    onValueChange = onCustNameChange,
                    label = { Text("Customer Name (Optional)") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    colors = adminTextFieldColors(redBrand),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pickupLoc,
                        onValueChange = onPickupLocChange,
                        label = { Text("Pickup Location") },
                        placeholder = { Text("e.g. Gandhipuram") },
                        leadingIcon = { Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color(0xFF10B981)) },
                        singleLine = true,
                        colors = adminTextFieldColors(redBrand),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dropLoc,
                        onValueChange = onDropLocChange,
                        label = { Text("Drop Location") },
                        placeholder = { Text("e.g. Airport") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = redBrand) },
                        singleLine = true,
                        colors = adminTextFieldColors(redBrand),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = baseFareInput,
                        onValueChange = onBaseFareChange,
                        label = { Text("Locked Base Fare (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = adminTextFieldColors(redBrand),
                        modifier = Modifier.weight(1f).testTag("admin_input_base_fare")
                    )

                    OutlinedTextField(
                        value = perKmFareInput,
                        onValueChange = onPerKmFareChange,
                        label = { Text("Locked Rate/KM (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = adminTextFieldColors(redBrand),
                        modifier = Modifier.weight(1f).testTag("admin_input_rate_km")
                    )
                }

                // TRIP OTP CARD
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, redBrand.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SECURITY CLAIM OTP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text(
                                text = tripOtp,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = redBrand,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Text("Customer gives OTP to driver to unlock claim", fontSize = 10.sp, color = Color(0xFF64748B))
                        }

                        Button(
                            onClick = onRegenerateOtp,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New OTP", fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = onLoadTrip,
                    enabled = !isSupabaseLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_broadcast_trip_button")
                ) {
                    if (isSupabaseLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BROADCAST TRIP TO BASH CLOUD", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                if (loadTripStatusMessage != null) {
                    Surface(
                        color = if (isLoadTripError) Color(0xFF450A0A) else Color(0xFF064E3B),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = loadTripStatusMessage,
                            color = if (isLoadTripError) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: DRIVERS MANAGEMENT (SUSPENSION & STATUS)
// -------------------------------------------------------------
@Composable
private fun DriversManagementTabContent(
    drivers: List<DriverRemoteEntity>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onToggleActive: (DriverRemoteEntity, Boolean) -> Unit,
    redBrand: Color
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "SUSPENDED"

    val activeCount = drivers.count { it.isActive }
    val suspendedCount = drivers.count { !it.isActive }

    val filteredDrivers = drivers.filter { driver ->
        val matchesSearch = searchQuery.isBlank() ||
                driver.driverName.contains(searchQuery, ignoreCase = true) ||
                driver.driverPhone.contains(searchQuery, ignoreCase = true) ||
                (driver.vehicleNumber ?: "").contains(searchQuery, ignoreCase = true) ||
                (driver.driverId ?: "").contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "ACTIVE" -> driver.isActive
            "SUSPENDED" -> !driver.isActive
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DRIVER DIRECTORY (${drivers.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "Remote suspension, termination, and fleet access management.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_drivers_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        // Live Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, phone, or plate...", color = Color(0xFF64748B), fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedBorderColor = redBrand,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("driver_search_input")
        )

        // Filter Chips (All, Active, Suspended)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = "All (${drivers.size})",
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                activeColor = Color(0xFF3B82F6)
            )
            FilterChipItem(
                label = "Active ($activeCount)",
                selected = selectedFilter == "ACTIVE",
                onClick = { selectedFilter = "ACTIVE" },
                activeColor = Color(0xFF10B981)
            )
            FilterChipItem(
                label = "Suspended ($suspendedCount)",
                selected = selectedFilter == "SUSPENDED",
                onClick = { selectedFilter = "SUSPENDED" },
                activeColor = Color(0xFFEF4444)
            )
        }

        if (isLoading && drivers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = redBrand)
            }
        } else if (drivers.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                    Text("No Drivers Registered Yet", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "Drivers appear here when they register their name and phone in the app.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredDrivers.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                    Text("No Matching Drivers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("No drivers matched '$searchQuery'", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDrivers, key = { it.driverPhone }) { driver ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (driver.isActive) Color(0xFF334155) else Color(0xFF7F1D1D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = driver.driverName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (driver.isActive) Color(0xFF065F46) else Color(0xFF991B1B),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (driver.isActive) "ACTIVE" else "SUSPENDED",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "📞 ${driver.driverPhone} | 🚗 ${driver.vehicleNumber ?: "No Plate"}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Text(
                                    text = "Duty: ${driver.status} • Type: ${driver.vehicleType ?: "Sedan"}",
                                    color = if (driver.status == "AVAILABLE") Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (driver.isActive) "Active" else "Suspended",
                                    fontSize = 10.sp,
                                    color = if (driver.isActive) Color(0xFF34D399) else Color(0xFFF87171),
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = driver.isActive,
                                    onCheckedChange = { onToggleActive(driver, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFDC2626)
                                    ),
                                    modifier = Modifier.testTag("driver_suspension_switch_${driver.driverPhone}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color
) {
    Surface(
        color = if (selected) activeColor.copy(alpha = 0.2f) else Color(0xFF1E293B),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) activeColor else Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) activeColor else Color(0xFF94A3B8),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// -------------------------------------------------------------
// TAB 4: COMMISSION & TRIP HISTORY
// -------------------------------------------------------------
@Composable
private fun CommissionTabContent(
    trips: List<PendingTrip>,
    onRefresh: () -> Unit,
    redBrand: Color
) {
    val completed = trips.filter { it.status == "completed" }
    val totalRevenue = completed.sumOf { it.finalFare ?: 0.0 }
    val totalCommission = completed.sumOf { it.commissionAmount ?: ((it.finalFare ?: 0.0) * 0.10) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("COMMISSION & REVENUE TRACKING", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("10% Dispatch Commission automatically computed per ride.", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TOTAL FARES", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("₹${String.format(Locale.US, "%.1f", totalRevenue)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${completed.size} completed rides", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }

            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, redBrand.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("COMMISSION OWED", color = redBrand, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("₹${String.format(Locale.US, "%.1f", totalCommission)}", color = redBrand, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("10% Dispatch Share", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        }

        Text("ALL DISPATCHED TRIPS (${trips.size})", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (trips.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No trips recorded on Bash Cloud yet.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trips, key = { it.id ?: (it.tripOtp + (it.customerPhone)) }) { trip ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cust: ${trip.customerPhone}",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Surface(
                                    color = when (trip.status) {
                                        "completed" -> Color(0xFF065F46)
                                        "claimed" -> Color(0xFF1E40AF)
                                        else -> Color(0xFF92400E)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = trip.status.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "From: ${trip.pickupLocation ?: "Anywhere"} → ${trip.dropLocation ?: "Destination"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Base: ₹${trip.baseFare.toInt()} | Rate: ₹${trip.perKmFare.toInt()}/KM",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                                if (trip.status == "completed") {
                                    val fare = trip.finalFare ?: 0.0
                                    val comm = trip.commissionAmount ?: (fare * 0.10)
                                    Text(
                                        text = "Fare: ₹${fare.toInt()} (Comm: ₹${comm.toInt()})",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (!trip.claimedByDriverName.isNullOrBlank()) {
                                Text(
                                    text = "Claimed by: ${trip.claimedByDriverName} (${trip.claimedByDriverPhone ?: ""})",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: REGULAR ADMIN MY DISPATCHED TRIPS
// -------------------------------------------------------------
@Composable
private fun MyDispatchedTripsTabContent(
    trips: List<PendingTrip>,
    onRefresh: () -> Unit,
    adminName: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MY DISPATCHED TRIPS (${trips.size})", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("Trips loaded by $adminName", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        if (trips.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(44.dp))
                    Text("No Dispatched Trips", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Load a new trip from the 'Load Trip' tab to dispatch to drivers.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trips, key = { it.id ?: (it.tripOtp + (it.customerPhone)) }) { trip ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cust: ${trip.customerPhone}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                                Surface(
                                    color = when (trip.status) {
                                        "completed" -> Color(0xFF065F46)
                                        "claimed" -> Color(0xFF1E40AF)
                                        else -> Color(0xFF92400E)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = trip.status.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("OTP: ${trip.tripOtp} | Base ₹${trip.baseFare.toInt()} | Rate ₹${trip.perKmFare.toInt()}/KM", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            if (!trip.claimedByDriverName.isNullOrBlank()) {
                                Text("Claimed by Driver: ${trip.claimedByDriverName}", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB: OFFLINE KEY GENERATOR
// -------------------------------------------------------------
@Composable
private fun KeyGenTabContent(
    currentDeviceId: String,
    inputDeviceId: String,
    onInputDeviceIdChange: (String) -> Unit,
    generatedKey: String,
    onGenerateKey: () -> Unit,
    isDeviceCurrentlyActivated: Boolean,
    onActivateSelf: () -> Unit,
    onDeactivateSelf: () -> Unit,
    onResetProfile: () -> Unit,
    clipboardManager: ClipboardManager,
    context: Context,
    redBrand: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("OFFLINE ACTIVATION KEY GENERATOR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("Generates 100% offline SHA-256 cryptographic keys bound to hardware Device IDs.", color = Color(0xFF94A3B8), fontSize = 11.sp)

                OutlinedTextField(
                    value = inputDeviceId,
                    onValueChange = onInputDeviceIdChange,
                    label = { Text("Enter Target Device ID") },
                    placeholder = { Text("e.g. $currentDeviceId") },
                    singleLine = true,
                    colors = adminTextFieldColors(redBrand),
                    modifier = Modifier.fillMaxWidth().testTag("input_target_device_id")
                )

                Button(
                    onClick = onGenerateKey,
                    colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("generate_key_button")
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GENERATE ACTIVATION KEY", fontWeight = FontWeight.Black)
                }

                if (generatedKey.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GENERATED KEY", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(generatedKey, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399), fontFamily = FontFamily.Monospace)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Activation Key", generatedKey))
                                    Toast.makeText(context, "Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Quick self activation controls
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("THIS DEVICE STATUS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("Device ID: $currentDeviceId", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = if (isDeviceCurrentlyActivated) "Status: ✅ ACTIVATED" else "Status: ❌ NOT ACTIVATED",
                    color = if (isDeviceCurrentlyActivated) Color(0xFF34D399) else Color(0xFFF87171),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onActivateSelf,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Activate Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDeactivateSelf,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deactivate", fontSize = 11.sp, color = Color(0xFFF87171))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB: SQL SCHEMA CODE
// -------------------------------------------------------------
@Composable
private fun SqlSchemaTabContent(
    sqlText: String,
    clipboardManager: ClipboardManager,
    context: Context,
    redBrand: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SUPABASE SQL CODE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Button(
                        onClick = {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Supabase SQL", sqlText))
                            Toast.makeText(context, "SQL Code Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY SQL CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Copy and run this SQL script in Supabase Dashboard → SQL Editor to set up `drivers` and `pending_trips` tables.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sqlText,
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
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
