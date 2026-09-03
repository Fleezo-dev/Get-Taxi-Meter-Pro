package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

data class SystemPermissionState(
    val isLocationGranted: Boolean = false,
    val isNotificationGranted: Boolean = false,
    val isBatteryOptimizedIgnored: Boolean = false,
    val isOverlayGranted: Boolean = false
) {
    val allGranted: Boolean
        get() = isLocationGranted && isNotificationGranted && isBatteryOptimizedIgnored && isOverlayGranted
}

fun checkCurrentSystemPermissions(context: Context): SystemPermissionState {
    val locationFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED && NotificationManagerCompat.from(context).areNotificationsEnabled()
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }

    val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }

    return SystemPermissionState(
        isLocationGranted = locationFine,
        isNotificationGranted = notifications,
        isBatteryOptimizedIgnored = batteryIgnored,
        isOverlayGranted = overlayGranted
    )
}

@Composable
fun StrictPermissionEnforcementDialog(
    onAllPermissionsGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permState by remember { mutableStateOf(checkCurrentSystemPermissions(context)) }

    // Lifecycle observer to re-check instantly whenever user switches back from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val updated = checkCurrentSystemPermissions(context)
                permState = updated
                if (updated.allGranted) {
                    onAllPermissionsGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permState = checkCurrentSystemPermissions(context)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permState = checkCurrentSystemPermissions(context)
    }

    var isDismissed by remember { mutableStateOf(false) }

    if (isDismissed || permState.allGranted) {
        return
    }

    val brandRed = Color(0xFFE11D48)

    Dialog(
        onDismissRequest = { isDismissed = true },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { isDismissed = true }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close permissions dialog",
                                tint = Color.White
                            )
                        }
                    }

                    // Shield Header Icon
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(brandRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppMaybe,
                            contentDescription = "Permission Required",
                            tint = brandRed,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Text(
                        text = "MANDATORY SYSTEM PERMISSIONS",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "To guarantee real-time trip GPS tracking, instant customer dispatch alerts, and uninterrupted meter calculation, Android strictly requires the following 3 permissions before you can use the Taxi Meter app.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. LOCATION CARD
                    PermissionItemCard(
                        title = "1. High-Accuracy Location",
                        subtitle = "Required for continuous GPS mileage and taximeter fare calculation.",
                        isGranted = permState.isLocationGranted,
                        icon = Icons.Default.LocationOn,
                        buttonText = "GRANT LOCATION",
                        brandColor = brandRed,
                        onAction = {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onOpenSettings = {
                            openAppSettings(context)
                        }
                    )

                    // 2. NOTIFICATIONS CARD
                    PermissionItemCard(
                        title = "2. System Notifications",
                        subtitle = "Required to keep the active meter foreground service running without getting killed.",
                        isGranted = permState.isNotificationGranted,
                        icon = Icons.Default.NotificationsActive,
                        buttonText = "GRANT NOTIFICATIONS",
                        brandColor = brandRed,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openAppNotificationSettings(context)
                            }
                        },
                        onOpenSettings = {
                            openAppNotificationSettings(context)
                        }
                    )

                    // 3. BATTERY OPTIMIZATION CARD
                    PermissionItemCard(
                        title = "3. Unrestricted Battery Usage",
                        subtitle = "Crucial to prevent Android OS from putting the meter to sleep while the phone screen is off.",
                        isGranted = permState.isBatteryOptimizedIgnored,
                        icon = Icons.Default.BatteryChargingFull,
                        buttonText = "DISABLE OPTIMIZATION",
                        brandColor = brandRed,
                        onAction = {
                            requestIgnoreBatteryOptimizations(context)
                        },
                        onOpenSettings = {
                            requestIgnoreBatteryOptimizations(context)
                        }
                    )

                    // 4. FLOATING BUBBLE OVERLAY CARD
                    PermissionItemCard(
                        title = "4. Floating Bubble Overlay (Anti-Sleep)",
                        subtitle = "Keeps a live floating meter bubble on your home screen when minimized so tracking never sleeps.",
                        isGranted = permState.isOverlayGranted,
                        icon = Icons.Default.FlipToFront,
                        buttonText = "GRANT OVERLAY",
                        brandColor = brandRed,
                        onAction = {
                            requestOverlayPermission(context)
                        },
                        onOpenSettings = {
                            requestOverlayPermission(context)
                        }
                    )
                }

                // Bottom Action Info & Refresh Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = checkCurrentSystemPermissions(context)
                            permState = updated
                            if (updated.allGranted) {
                                onAllPermissionsGranted()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (permState.allGranted) Color(0xFF10B981) else brandRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("verify_permissions_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (permState.allGranted) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (permState.allGranted) "ALL PERMISSIONS GRANTED • CONTINUE" else "RE-CHECK PERMISSIONS",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }

                    TextButton(
                        onClick = { openAppSettings(context) }
                    ) {
                        Text(
                            text = "Open App Settings Manually",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = { isDismissed = true }
                    ) {
                        Text(
                            text = "Skip / Continue to Taxi Meter",
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    brandColor: Color,
    onAction: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isGranted) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else brandColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) Color(0xFF10B981) else brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isGranted) "Status: Granted" else "Status: Action Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isGranted) Color(0xFF34D399) else brandColor
                        )
                    }
                }

                if (isGranted) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Granted",
                            tint = Color(0xFF34D399),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )

            if (!isGranted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = buttonText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Settings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun openAppNotificationSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } else {
            openAppSettings(context)
        }
    } catch (e: Exception) {
        openAppSettings(context)
    }
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                openAppSettings(context)
            }
        }
    }
}

private fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }
}
