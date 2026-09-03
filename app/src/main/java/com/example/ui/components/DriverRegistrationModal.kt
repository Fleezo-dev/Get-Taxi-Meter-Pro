package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.security.ActivationSecurityManager

object VehicleOptions {
    val vehicleTypes = listOf("Mini", "Sedan", "SUV", "Prime SUV", "Premium")

    fun getModelsForType(type: String): List<String> {
        return when (type) {
            "Mini" -> listOf("Grand i10", "Tata Indica", "Maruti WagonR", "Other")
            "Sedan" -> listOf("Dzire", "Aura", "Zest", "Etios", "Other")
            "SUV" -> listOf("Ertiga", "Triber", "Lodgy", "Other")
            "Prime SUV" -> listOf("Innova", "Kia Carens", "Rumion", "Marazzo", "Other")
            "Premium" -> listOf("Innova Crysta", "Innova Hycross", "Fortuner", "Other")
            else -> listOf("Dzire", "Aura", "Zest", "Etios", "Other")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRegistrationModal(
    driverId: String,
    initialName: String = "",
    initialPhone: String = "",
    initialPlate: String = "",
    initialType: String = "Sedan",
    initialModel: String = "Dzire",
    initialPhotoUri: String = "",
    onRegister: (
        name: String,
        phone: String,
        vehiclePlate: String,
        vehicleType: String,
        vehicleModel: String,
        photoUri: String,
        isEmergencyOneTime: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var vehiclePlate by remember { mutableStateOf(initialPlate.ifBlank { "TN99AF5313" }) }

    var selectedType by remember { mutableStateOf(if (VehicleOptions.vehicleTypes.contains(initialType)) initialType else "Mini") }
    val availableModels = remember(selectedType) { VehicleOptions.getModelsForType(selectedType) }
    var selectedModel by remember(selectedType) {
        mutableStateOf(if (availableModels.contains(initialModel)) initialModel else availableModels.first())
    }
    var customModelText by remember { mutableStateOf("") }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var photoUriStr by remember { mutableStateOf(initialPhotoUri) }
    var errorMessage by remember { mutableStateOf("") }

    val deviceId = remember { ActivationSecurityManager.getDeviceId(context) }
    var activationKeyInput by remember { mutableStateOf("") }
    var isValidatingKey by remember { mutableStateOf(false) }

    var emergencyActivationKeyInput by remember { mutableStateOf("") }
    var isEmergencyValidating by remember { mutableStateOf(false) }
    var emergencyError by remember { mutableStateOf("") }
    var showMasterAdminDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordError by remember { mutableStateOf("") }
    var isAdminPasswordVisible by remember { mutableStateOf(false) }

    var isLocationGranted by remember { mutableStateOf(false) }
    var isBatteryIgnored by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val bg = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        isLocationGranted = fine && bg

        isBatteryIgnored = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissions()
        if (!isLocationGranted) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {}
        }
    }

    val redBrand = Color(0xFFE53935)
    val backgroundLight = Color(0xFFF8F9FA)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    val maxDim = 360
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scale = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height)
                    val targetW = if (scale < 1.0f) (width * scale).toInt().coerceAtLeast(1) else width
                    val targetH = if (scale < 1.0f) (height * scale).toInt().coerceAtLeast(1) else height
                    val resized = android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
                    val baos = java.io.ByteArrayOutputStream()
                    resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, baos)
                    val imageBytes = baos.toByteArray()
                    val base64String = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
                    photoUriStr = "data:image/jpeg;base64,$base64String"
                } else {
                    photoUriStr = selectedUri.toString()
                }
                errorMessage = ""
            } catch (e: Exception) {
                photoUriStr = selectedUri.toString()
            }
        }
    }

    val inputShape = RoundedCornerShape(16.dp)
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1E1E1E),
        unfocusedTextColor = Color(0xFF1E1E1E),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = redBrand,
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedLabelColor = redBrand,
        unfocusedLabelColor = Color(0xFF616161),
        focusedLeadingIconColor = redBrand,
        unfocusedLeadingIconColor = Color(0xFF757575)
    )

    // Master Admin Password Verification Dialog
    if (showMasterAdminDialog) {
        AlertDialog(
            onDismissRequest = {
                showMasterAdminDialog = false
                adminPasswordInput = ""
                adminPasswordError = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = redBrand)
                    Text("Master Admin Login", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the Master Admin Password to access the application dashboard immediately.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = {
                            adminPasswordInput = it
                            adminPasswordError = ""
                        },
                        label = { Text("Master Password") },
                        singleLine = true,
                        visualTransformation = if (isAdminPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isAdminPasswordVisible = !isAdminPasswordVisible }) {
                                Icon(
                                    imageVector = if (isAdminPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        isError = adminPasswordError.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                    )
                    if (adminPasswordError.isNotBlank()) {
                        Text(adminPasswordError, color = redBrand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = adminPasswordInput.trim()
                        if (input == "2604" || input == "1404" || input == "140423" || input.equals("Master1974", ignoreCase = true) || input == "1981" || input == "1974") {
                            showMasterAdminDialog = false
                            ActivationSecurityManager.setActivated(context, true, input, "PERMANENT")
                            android.widget.Toast.makeText(context, "Master Admin Login Granted! Welcome back.", android.widget.Toast.LENGTH_LONG).show()
                            onRegister(
                                "Master Admin",
                                "9043743777",
                                "ADMIN-01",
                                "Sedan",
                                "Dzire",
                                "",
                                false
                            )
                        } else {
                            adminPasswordError = "Incorrect password! Access denied."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("LOGIN AS MASTER ADMIN", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMasterAdminDialog = false
                        adminPasswordInput = ""
                        adminPasswordError = ""
                    }
                ) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        color = backgroundLight
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
                // Top Action Bar: Master Admin Login Shortcut & Skip to Meter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onRegister(
                                if (name.isNotBlank()) name else "Driver 001",
                                if (phone.isNotBlank()) phone else "9876543210",
                                if (vehiclePlate.isNotBlank()) vehiclePlate else "TN99AF5313",
                                selectedType,
                                selectedModel,
                                photoUriStr,
                                false
                            )
                        }
                    ) {
                        Text(
                            text = "Skip to Taxi Meter ➔",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        onClick = { showMasterAdminDialog = true },
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = redBrand, modifier = Modifier.size(14.dp))
                            Text("🔑 Master Admin Login", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = redBrand)
                        }
                    }
                }
                // Top Header Area: Title & Subtitle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val headerTitle = when (currentStep) {
                        1 -> "Driver Profile"
                        2 -> "Vehicle Details"
                        3 -> "Identity Verification"
                        else -> "Account Activation"
                    }
                    val headerSubtitle = when (currentStep) {
                        1 -> "Let's start with your basic information"
                        2 -> "What type of vehicle are you driving?"
                        3 -> "Upload a photo to verify your identity and secure your account."
                        else -> "Review your details & activate driver account"
                    }

                    Text(
                        text = headerTitle,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = headerSubtitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5A6978),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4-Segment Progress Indicator Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (step in 1..4) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(if (step <= currentStep) redBrand else Color(0xFFE5E7EB))
                            )
                        }
                    }
                }

                // Error Message Notice if any
                if (errorMessage.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = redBrand, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                color = redBrand,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Scrollable Body Content for Steps
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        when (currentStep) {
                            // STEP 1: DRIVER PROFILE (Device ID, Name, Phone, Plate, Activation Key)
                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // INSTANT ONE TIME LOGIN (EMERGENCY DRIVER ACCESS)
                                    Surface(
                                        color = Color(0xFFFFF8E1),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                                        modifier = Modifier.fillMaxWidth().testTag("instant_login_card")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE65100),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "INSTANT ONE TIME LOGIN",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFFE65100)
                                                )
                                            }

                                            Text(
                                                text = "Emergency Mode: Copy your Device ID below, paste the activation key from Master Admin to use the taximeter immediately for 1 trip. Will auto logout after meter trip.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF424242)
                                            )

                                            // COPY DEVICE ID BOX
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFFE082)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("COPY YOUR DEVICE ID HERE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                        Text(deviceId, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
                                                            clipboard.setPrimaryClip(clip)
                                                            android.widget.Toast.makeText(context, "Device ID copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.testTag("copy_device_id_button")
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }

                                            // PASTE ACTIVATION KEY FIELD
                                            OutlinedTextField(
                                                value = emergencyActivationKeyInput,
                                                onValueChange = {
                                                    emergencyActivationKeyInput = it
                                                    emergencyError = ""
                                                },
                                                label = { Text("Paste Activation Key") },
                                                placeholder = { Text("Paste key provided by Master Admin") },
                                                singleLine = true,
                                                trailingIcon = {
                                                    IconButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            val clipData = clipboard.primaryClip
                                                            if (clipData != null && clipData.itemCount > 0) {
                                                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                                                if (text.isNotBlank()) {
                                                                    emergencyActivationKeyInput = text
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Key", tint = Color(0xFFE65100))
                                                    }
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFFE65100),
                                                    unfocusedBorderColor = Color(0xFFFFB300)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("emergency_key_field")
                                            )

                                            if (emergencyError.isNotBlank()) {
                                                Text(emergencyError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // USE METER NOW BUTTON
                                            Button(
                                                onClick = {
                                                    if (emergencyActivationKeyInput.isBlank()) {
                                                        emergencyError = "Please paste or enter the Activation Key."
                                                        return@Button
                                                    }
                                                    isEmergencyValidating = true
                                                    val isValid = com.example.security.ActivationSecurityManager.validateActivationKey(deviceId, emergencyActivationKeyInput)
                                                    isEmergencyValidating = false
                                                    if (isValid) {
                                                        com.example.security.ActivationSecurityManager.setActivated(context, true, emergencyActivationKeyInput, "EMERGENCY_ONE_TIME")
                                                        android.widget.Toast.makeText(context, "⚡ Instant One-Time Emergency Access Granted!", android.widget.Toast.LENGTH_LONG).show()
                                                        onRegister(
                                                            "Emergency Driver",
                                                            "+91 9000000000",
                                                            "EMERGENCY-TX",
                                                            "Sedan",
                                                            "Emergency Mode",
                                                            "",
                                                            true
                                                        )
                                                    } else {
                                                        emergencyError = "Invalid Activation Key for Device ID: $deviceId"
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("instant_login_submit_button")
                                            ) {
                                                if (isEmergencyValidating) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("USE METER NOW (INSTANT LOGIN)", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Prominent Device ID Box with One-Tap Copy
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("HARDWARE DEVICE ID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                                Text("Device ID: $deviceId", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                            }
                                            TextButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
                                                    clipboard.setPrimaryClip(clip)
                                                    android.widget.Toast.makeText(context, "Device ID copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = redBrand)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Device ID", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy Device ID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Full-width Telegram Redirection Button for Drivers
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, "Device ID copied! Opening Telegram...", android.widget.Toast.LENGTH_SHORT).show()
                                            com.example.updater.AppUpdater.launchTelegram(context, "https://t.me/Gettaxikovai")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("telegram_request_key_button")
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Get Activation Key on Telegram (@Gettaxikovai)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    // Full Name Field
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Full Name",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF111827)
                                        )
                                        OutlinedTextField(
                                            value = name,
                                            onValueChange = {
                                                name = it
                                                errorMessage = ""
                                            },
                                            placeholder = { Text("e.g. Basheer", color = Color(0xFF9CA3AF)) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF9CA3AF))
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = inputColors,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Mobile Number Field
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Mobile Number",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF111827)
                                        )
                                        OutlinedTextField(
                                            value = phone,
                                            onValueChange = {
                                                phone = it
                                                errorMessage = ""
                                            },
                                            placeholder = { Text("e.g. 9043743777", color = Color(0xFF9CA3AF)) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF9CA3AF))
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = inputColors,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Vehicle Registration Number Field
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Vehicle Registration Number",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF111827)
                                        )
                                        OutlinedTextField(
                                            value = vehiclePlate,
                                            onValueChange = {
                                                vehiclePlate = it
                                                errorMessage = ""
                                            },
                                            placeholder = { Text("e.g. TN99AF5313", color = Color(0xFF9CA3AF)) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF9CA3AF))
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = inputColors,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // STEP 2: VEHICLE DETAILS (Mini, Sedan, SUV, Prime SUV, Premium)
                            2 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    VehicleOptions.vehicleTypes.forEach { typeOption ->
                                        val isSelected = selectedType == typeOption
                                        val icon = when (typeOption) {
                                            "Premium" -> Icons.Default.Star
                                            "SUV", "Prime SUV" -> Icons.Default.AirportShuttle
                                            else -> Icons.Default.DirectionsCar
                                        }

                                        Surface(
                                            onClick = {
                                                selectedType = typeOption
                                                val newModels = VehicleOptions.getModelsForType(typeOption)
                                                selectedModel = newModels.first()
                                            },
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (isSelected) Color(0xFFFFF5F5) else Color.White,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) redBrand else Color(0xFFE0E0E0)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("vehicle_type_card_$typeOption")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = typeOption,
                                                    tint = if (isSelected) redBrand else Color(0xFF5A6978),
                                                    modifier = Modifier.size(26.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = typeOption,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 17.sp,
                                                    color = if (isSelected) redBrand else Color(0xFF111827),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = redBrand,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Vehicle Model Dropdown & Custom Model Entry
                                    ExposedDropdownMenuBox(
                                        expanded = modelDropdownExpanded,
                                        onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = selectedModel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Vehicle Model ($selectedType)") },
                                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                                            shape = inputShape,
                                            colors = inputColors,
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = modelDropdownExpanded,
                                            onDismissRequest = { modelDropdownExpanded = false }
                                        ) {
                                            availableModels.forEach { modelOption ->
                                                DropdownMenuItem(
                                                    text = { Text(modelOption, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E1E1E)) },
                                                    onClick = {
                                                        selectedModel = modelOption
                                                        modelDropdownExpanded = false
                                                    },
                                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                )
                                            }
                                        }
                                    }

                                    if (selectedModel == "Other") {
                                        OutlinedTextField(
                                            value = customModelText,
                                            onValueChange = { customModelText = it },
                                            label = { Text("Specify Custom Vehicle Model") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            singleLine = true,
                                            shape = inputShape,
                                            colors = inputColors,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // STEP 3: IDENTITY VERIFICATION (Profile Photo Upload)
                            3 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Big Circle Photo Container
                                    Box(
                                        modifier = Modifier
                                            .size(180.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF0F4F8))
                                            .border(3.dp, if (photoUriStr.isNotBlank()) redBrand else Color(0xFFCFD8DC), CircleShape)
                                            .clickable { photoPickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (photoUriStr.isNotBlank()) {
                                            val imageModel = remember(photoUriStr) {
                                                if (photoUriStr.startsWith("data:image")) {
                                                    try {
                                                        val b64 = photoUriStr.substringAfter(",")
                                                        android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                                    } catch (e: Exception) {
                                                        photoUriStr
                                                    }
                                                } else {
                                                    photoUriStr
                                                }
                                            }
                                            AsyncImage(
                                                model = imageModel,
                                                contentDescription = "Driver Profile Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Upload Photo",
                                                tint = Color(0xFF8E9AAF),
                                                modifier = Modifier.size(56.dp)
                                            )
                                        }

                                        // Overlay Camera/Upload Icon Badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .size(36.dp)
                                                .background(redBrand, CircleShape)
                                                .border(2.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Upload",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Upload Button
                                    Button(
                                        onClick = { photoPickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileUpload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (photoUriStr.isNotBlank()) "CHANGE PROFILE PHOTO" else "UPLOAD PROFILE PHOTO",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = "A profile photo is mandatory to ensure identity and security of your account. It will be used for driver identification on the platform.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF5A6978),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            // STEP 4: ACCOUNT ACTIVATION & FINAL REVIEW
                            4 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                    // Assigned Driver ID High-Visibility Banner
                                    Surface(
                                        color = Color(0xFFFFD600),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "ASSIGNED SYSTEM DRIVER ID",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                                Text(
                                                    text = driverId,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    // MANDATORY DRIVER PERMISSIONS CARD (LOCATION & BATTERY)
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, if (isLocationGranted && isBatteryIgnored) Color(0xFF4CAF50) else redBrand),
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
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = if (isLocationGranted && isBatteryIgnored) Color(0xFF2E7D32) else redBrand,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    text = "MANDATORY APP PERMISSIONS",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF111827)
                                                )
                                            }

                                            Text(
                                                text = "To guarantee real-time trip dispatches and background location telemetry, Android mandates 'Always Allow Location' and 'Unrestricted Battery Usage'. Registration is locked until both are granted.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF5A6978),
                                                lineHeight = 15.sp
                                            )

                                            Divider(color = Color(0xFFEEEEEE))

                                            // Permission 1: Background Location
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "1. Location Access",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF111827)
                                                    )
                                                    Text(
                                                        text = if (isLocationGranted) "Status: Always Allowed" else "Required: Set to 'Allow all the time'",
                                                        fontSize = 11.sp,
                                                        color = if (isLocationGranted) Color(0xFF2E7D32) else redBrand,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                if (isLocationGranted) {
                                                    Surface(
                                                        color = Color(0xFFE8F5E9),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                                            Text("GRANTED", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            locationPermissionLauncher.launch(
                                                                arrayOf(
                                                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                                )
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("GRANT 'ALWAYS'", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                    }
                                                }
                                            }

                                            Divider(color = Color(0xFFEEEEEE))

                                            // Permission 2: Battery Saver Exemption
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "2. Battery Usage",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF111827)
                                                    )
                                                    Text(
                                                        text = if (isBatteryIgnored) "Status: Unrestricted" else "Required: Set to 'Unrestricted'",
                                                        fontSize = 11.sp,
                                                        color = if (isBatteryIgnored) Color(0xFF2E7D32) else redBrand,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                if (isBatteryIgnored) {
                                                    Surface(
                                                        color = Color(0xFFE8F5E9),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                                            Text("UNRESTRICTED", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                                                try {
                                                                    val intent = android.content.Intent(
                                                                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                                        android.net.Uri.parse("package:${context.packageName}")
                                                                    ).apply {
                                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                    }
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    try {
                                                                        val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                        }
                                                                        context.startActivity(fallbackIntent)
                                                                    } catch (e2: Exception) {}
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("ALLOW UNRESTRICTED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Hardware Device ID & Master Admin Activation Key Entry
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.PhonelinkSetup, contentDescription = null, tint = redBrand)
                                                Text(
                                                    text = "DEVICE ACTIVATION CONTROL",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1E1E1E),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }

                                            Surface(
                                                color = Color(0xFFF5F5F5),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("HARDWARE DEVICE ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                        Text(deviceId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
                                                            clipboard.setPrimaryClip(clip)
                                                            android.widget.Toast.makeText(context, "Device ID Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Device ID", tint = redBrand, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Provide your Device ID to Master Admin to receive your single-use Activation Key.",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )

                                            OutlinedTextField(
                                                value = activationKeyInput,
                                                onValueChange = {
                                                    activationKeyInput = it
                                                    errorMessage = ""
                                                },
                                                label = { Text("Master Admin Activation Key") },
                                                placeholder = { Text("e.g. ACT-849201") },
                                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = redBrand) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = inputColors,
                                                modifier = Modifier.fillMaxWidth().testTag("activation_key_input")
                                            )
                                        }
                                    }

                                    // Summary Card
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFF0F4F8)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (photoUriStr.isNotBlank()) {
                                                        val imageModel = remember(photoUriStr) {
                                                            if (photoUriStr.startsWith("data:image")) {
                                                                try {
                                                                    val b64 = photoUriStr.substringAfter(",")
                                                                    android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                                                } catch (e: Exception) {
                                                                    photoUriStr
                                                                }
                                                            } else {
                                                                photoUriStr
                                                            }
                                                        }
                                                        AsyncImage(
                                                            model = imageModel,
                                                            contentDescription = "Avatar",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = name.ifBlank { "Driver Name" },
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 18.sp,
                                                        color = Color(0xFF111827)
                                                    )
                                                    Text(
                                                        text = phone.ifBlank { "+91 9043743777" },
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF5A6978)
                                                    )
                                                }
                                            }

                                            Divider(color = Color(0xFFEEEEEE))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Vehicle Type", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                    Text(selectedType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                                }
                                                Column {
                                                    Text("Model", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                    val finalM = if (selectedModel == "Other") customModelText.ifBlank { "Custom" } else selectedModel
                                                    Text(finalM, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                                }
                                                Column {
                                                    Text("Registration No", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                    Text(vehiclePlate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                                }
                                            }
                                        }
                                    }

                                     Text(
                                        text = "By completing registration, your account will be activated on the dispatch server for trip requests.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // Fixed Elevated Bottom Bar: Back button on Left, Red "Continue" Button on Right
                Surface(
                    color = Color.White,
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    if (currentStep > 1) {
                        TextButton(
                            onClick = {
                                errorMessage = ""
                                currentStep--
                            }
                        ) {
                            Text(
                                text = "Back",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5A6978)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        enabled = !isValidatingKey,
                        onClick = {
                            when (currentStep) {
                                1 -> {
                                    if (name.trim().isBlank()) {
                                        errorMessage = "Please enter your Full Name"
                                    } else if (phone.trim().isBlank()) {
                                        errorMessage = "Please enter your Mobile Number"
                                    } else if (vehiclePlate.trim().isBlank()) {
                                        errorMessage = "Please enter Vehicle Registration Number"
                                    } else {
                                        errorMessage = ""
                                        currentStep = 2
                                    }
                                }
                                2 -> {
                                    errorMessage = ""
                                    currentStep = 3
                                }
                                3 -> {
                                    errorMessage = ""
                                    currentStep = 4
                                }
                                4 -> {
                                    refreshPermissions()
                                    if (!isLocationGranted || !isBatteryIgnored) {
                                        errorMessage = "MANDATORY PERMISSIONS REQUIRED: You must grant 'Always Allow Location' and 'Unrestricted Battery Usage' before activating account."
                                    } else if (activationKeyInput.isBlank()) {
                                        errorMessage = "Please enter Activation Key provided by Master Admin."
                                    } else {
                                        val finalModel = if (selectedModel == "Other") {
                                            customModelText.trim().ifBlank { "Other Vehicle" }
                                        } else {
                                            selectedModel
                                        }
                                        isValidatingKey = true
                                        val isValid = com.example.security.ActivationSecurityManager.validateActivationKey(deviceId, activationKeyInput)
                                        isValidatingKey = false
                                        if (isValid) {
                                            com.example.security.ActivationSecurityManager.setActivated(context, true, activationKeyInput, "PERMANENT")
                                            onRegister(
                                                name.trim(),
                                                phone.trim(),
                                                vehiclePlate.trim(),
                                                selectedType,
                                                finalModel,
                                                photoUriStr,
                                                false
                                            )
                                        } else {
                                            errorMessage = "Invalid Activation Key for Device ID: $deviceId. Please contact Master Admin via Telegram @Gettaxikovai for key."
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = redBrand),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("onboarding_continue_button")
                    ) {
                        if (isValidatingKey) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating Key...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else if (currentStep < 4) {
                            Text(
                                text = "Continue",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "REGISTER & START DRIVING",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
