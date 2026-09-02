package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DriverProfile
import com.example.viewmodel.DispatchViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    dispatchViewModel: DispatchViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by dispatchViewModel.driverProfile.collectAsState()

    var customDriverId by remember(currentProfile) { mutableStateOf(currentProfile.driverId) }
    var name by remember(currentProfile) { mutableStateOf(currentProfile.driverName) }
    var phone by remember(currentProfile) { mutableStateOf(currentProfile.phoneNumber) }
    var vehiclePlate by remember(currentProfile) { mutableStateOf(currentProfile.vehiclePlate) }
    var vehicleType by remember(currentProfile) { mutableStateOf(currentProfile.vehicleType) }
    var vehicleModel by remember(currentProfile) { mutableStateOf(currentProfile.vehicleModel) }
    var photoUriStr by remember(currentProfile) { mutableStateOf(currentProfile.photoUri) }
    var fleetCode by remember(currentProfile) { mutableStateOf(currentProfile.fleetNetworkCode) }
    var isOnline by remember(currentProfile) { mutableStateOf(currentProfile.isOnline) }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var isSavedToastVisible by remember { mutableStateOf(false) }

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
            } catch (e: Exception) {
                photoUriStr = selectedUri.toString()
            }
        }
    }

    val redBrand = Color(0xFFC62828)

    // Standardized text field colors ensuring 100% dark text visibility
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1E1E1E),
        unfocusedTextColor = Color(0xFF1E1E1E),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = redBrand,
        unfocusedBorderColor = Color(0xFFBDBDBD),
        focusedLabelColor = redBrand,
        unfocusedLabelColor = Color(0xFF616161),
        focusedLeadingIconColor = redBrand,
        unfocusedLeadingIconColor = Color(0xFF616161)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Profile & ID", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = redBrand)
            )
        },
        containerColor = Color(0xFF8A0000),
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DRIVER ID BADGE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Photo / Image Upload Selector (Gallery)
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE))
                            .border(3.dp, redBrand, CircleShape)
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
                            Image(
                                painter = painterResource(id = R.drawable.ic_get_taxi_vector),
                                contentDescription = "Default Avatar",
                                modifier = Modifier.size(70.dp)
                            )
                        }

                        // Upload Icon Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .background(redBrand, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Upload Photo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Explicit Upload Button
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = redBrand),
                        border = BorderStroke(1.dp, redBrand),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("upload_profile_photo_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (photoUriStr.isBlank()) "UPLOAD PHOTO FROM GALLERY" else "CHANGE PHOTO FROM GALLERY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val isIdEditable = dispatchViewModel.isDriverIdEditable(currentProfile.driverId)

                    Text(
                        text = if (isIdEditable) "DRIVER ASSIGNED ID (MASTER ADMIN EDITABLE: DRV001-DRV010)" else "PERMANENT LOCKED DRIVER ID (AUTO-INCREMENTED)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray,
                        letterSpacing = 1.2.sp
                    )

                    if (isIdEditable) {
                        OutlinedTextField(
                            value = customDriverId,
                            onValueChange = { customDriverId = it },
                            label = { Text("Driver ID (DRV001 - DRV010)") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = redBrand) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                        Text(
                            text = "First 10 Driver IDs (DRV001-DRV010) are customizable by Master Admin.",
                            fontSize = 11.sp,
                            color = Color(0xFF616161),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        // Locked Driver ID Badge
                        Surface(
                            color = Color(0xFFFFD600),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentProfile.driverId,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    modifier = Modifier.testTag("driver_assigned_id")
                                )
                            }
                        }
                        Text(
                            text = "Driver IDs from DRV011 onward are permanently locked on frontend and backend.",
                            fontSize = 11.sp,
                            color = Color(0xFF616161),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var showLogoutDialog by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFC62828)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFC62828)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_old_driver_id_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFC62828))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOG OUT OLD ID & CLAIM FRESH DRIVER ID",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            icon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = redBrand) },
                            title = { Text("Log Out Old ID & Reset Driver Registration?", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    "This will unregister your old ID (${currentProfile.driverId}) from this device and generate a brand new, system-locked sequential Driver ID for fresh onboarding.",
                                    fontSize = 13.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showLogoutDialog = false
                                        dispatchViewModel.logoutAndRequestFreshDriverId { fresh ->
                                            customDriverId = fresh.driverId
                                            name = fresh.driverName
                                            phone = fresh.phoneNumber
                                            vehiclePlate = fresh.vehiclePlate
                                            vehicleModel = fresh.vehicleModel
                                            photoUriStr = fresh.photoUri
                                            fleetCode = fresh.fleetNetworkCode
                                            isOnline = fresh.isOnline
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = redBrand)
                                ) {
                                    Text("Yes, Logout & Claim Fresh ID", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogoutDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }

            // DRIVER DETAILS FORM CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "DRIVER & VEHICLE DETAILS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = redBrand,
                        letterSpacing = 1.sp
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Driver Assigned ID field (PERMANENTLY LOCKED & READ-ONLY)
                    OutlinedTextField(
                        value = currentProfile.driverId,
                        onValueChange = { /* Read-Only System Assigned ID */ },
                        readOnly = true,
                        enabled = false,
                        label = { Text("Driver ID (Read-Only System Generated)") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = { Icon(Icons.Default.Lock, contentDescription = "System Locked ID", tint = Color(0xFFC62828)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color(0xFF1E1E1E),
                            disabledBorderColor = Color(0xFFE0E0E0),
                            disabledLabelColor = Color(0xFF616161),
                            disabledLeadingIconColor = Color(0xFF616161),
                            disabledContainerColor = Color(0xFFF5F5F5)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_driver_id")
                    )

                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Driver Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_driver_name")
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_driver_phone")
                    )

                    // Vehicle Plate Number
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it },
                        label = { Text("Vehicle License Plate Number") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_vehicle_plate")
                    )

                    // Vehicle Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = vehicleType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle Type") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            colors = textFieldColors,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("profile_vehicle_type_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            com.example.ui.components.VehicleOptions.vehicleTypes.forEach { typeOption ->
                                DropdownMenuItem(
                                    text = { Text(typeOption, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E1E1E)) },
                                    onClick = {
                                        vehicleType = typeOption
                                        typeDropdownExpanded = false
                                        val models = com.example.ui.components.VehicleOptions.getModelsForType(typeOption)
                                        vehicleModel = models.first()
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    // Vehicle Model Dropdown
                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = vehicleModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle Model ($vehicleType)") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                            colors = textFieldColors,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("profile_vehicle_model_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            com.example.ui.components.VehicleOptions.getModelsForType(vehicleType).forEach { modelOption ->
                                DropdownMenuItem(
                                    text = { Text(modelOption, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E1E1E)) },
                                    onClick = {
                                        vehicleModel = modelOption
                                        modelDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    // Fleet Network Code
                    OutlinedTextField(
                        value = fleetCode,
                        onValueChange = { fleetCode = it },
                        label = { Text("Fleet Network Code (Join Same Fleet)") },
                        leadingIcon = { Icon(Icons.Default.CellTower, contentDescription = null) },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_fleet_code")
                    )

                    // Online Duty Status Switch
                    Surface(
                        color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isOnline) Color(0xFFA5D6A7) else Color(0xFFFFCDD2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF2E7D32) else Color(0xFFD32F2F)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DUTY STATUS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                    )
                                    Text(
                                        text = if (isOnline) "ONLINE / Available for Trips" else "OFFLINE / Off Duty",
                                        fontSize = 11.sp,
                                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { isOnline = it },
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
                                    if (isOnline) {
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Save Profile Button
                    Button(
                        onClick = {
                            val updated = currentProfile.copy(
                                driverId = currentProfile.driverId,
                                driverName = name,
                                phoneNumber = phone,
                                vehiclePlate = vehiclePlate,
                                vehicleType = vehicleType,
                                vehicleModel = vehicleModel,
                                photoUri = photoUriStr,
                                isOnline = isOnline,
                                status = if (isOnline) "AVAILABLE" else "OFFLINE",
                                fleetNetworkCode = fleetCode,
                                isProfileCompleted = true,
                                lastUpdatedTimestamp = System.currentTimeMillis()
                            )
                            dispatchViewModel.updateDriverProfile(updated)
                            isSavedToastVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = redBrand,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVE & UPDATE PROFILE",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (isSavedToastVisible) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ Profile updated and synced with Fleet Network!",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
