package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.updater.AppUpdater
import com.example.updater.UpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onOpenDriverProfile: () -> Unit = {}
) {
    var isBypassedByAdmin by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    if ((!updateInfo.updateAvailable && !updateInfo.forceUpdate) || isBypassedByAdmin) return

    val context = LocalContext.current
    val redBrand = Color(0xFFD32F2F)
    val telegramBlue = Color(0xFF0088CC)

    val deviceId = remember {
        try {
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "DEV-7A82F90B"
        } catch (e: Exception) {
            "DEV-7A82F90B"
        }
    }

    // Allow hardware back button to dismiss/bypass update lock if driver needs to activate
    BackHandler(enabled = true) {
        isBypassedByAdmin = true
        onOpenDriverProfile()
    }

    // Master Admin Password Verification Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordInput = ""
                passwordError = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1976D2))
                    Text("Master Admin Verification", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the Master Admin Password to bypass the version gate update lockout.",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = ""
                        },
                        label = { Text("Master Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        isError = passwordError.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("update_master_password_input")
                    )
                    if (passwordError.isNotBlank()) {
                        Text(passwordError, color = redBrand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = passwordInput.trim()
                        if (input == "2604" || input == "1404" || input == "140423" || input.equals("Master1974", ignoreCase = true) || input == "1981" || input == "1974") {
                            isBypassedByAdmin = true
                            showPasswordDialog = false
                            android.widget.Toast.makeText(context, "Master Admin Override Granted! Gate Bypassed.", android.widget.Toast.LENGTH_LONG).show()
                            onOpenDriverProfile()
                        } else {
                            passwordError = "Incorrect password! Access denied."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("UNLOCK & BYPASS", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        passwordInput = ""
                        passwordError = ""
                    }
                ) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Dialog(
        onDismissRequest = {
            isBypassedByAdmin = true
            onOpenDriverProfile()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                border = BorderStroke(2.dp, redBrand),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Lockout Badge Header (Triple-tap to bypass as Master Admin)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, redBrand),
                            modifier = Modifier.clickable {
                                tapCount++
                                if (tapCount >= 3) {
                                    tapCount = 0
                                    showPasswordDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, "Tap ${3 - tapCount} more times for Master Admin Passcode", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = redBrand,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "SYSTEM UPDATE REQUIRED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = redBrand
                                )
                            }
                        }

                        // Master Admin Shortcut Button
                        Surface(
                            onClick = { showPasswordDialog = true },
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = redBrand, modifier = Modifier.size(12.dp))
                                Text("🔑 Admin Login", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = redBrand)
                            }
                        }
                    }

                    // Icon Visual Focus
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE3F2FD),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "System Update Required",
                                tint = telegramBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Dialog Title
                    Text(
                        text = "MANDATORY SYSTEM UPDATE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    // Version Lockout Info Card
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CURRENT VERSION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("v${updateInfo.currentVersion}.0", fontSize = 13.sp, fontWeight = FontWeight.Black, color = redBrand)
                            }
                            Divider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp),
                                color = Color(0xFFCBD5E1)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("REQUIRED MINIMUM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("v${updateInfo.minRequiredVersion}.0", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                            }
                        }
                    }

                    // Hardware Device ID Display & One-Tap Copy Card
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("HARDWARE DEVICE ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                Text(deviceId, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            }
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
                                    clipboard?.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Device ID copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = telegramBlue)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Device ID", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Telegram Redirection Button
                    Button(
                        onClick = {
                            AppUpdater.launchTelegram(context, updateInfo.telegramUrl)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = telegramBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Telegram",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REQUEST KEY / APK ON TELEGRAM",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Open Driver Profile Starting Page Button
                    OutlinedButton(
                        onClick = {
                            isBypassedByAdmin = true
                            onOpenDriverProfile()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF64748B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OPEN DRIVER PROFILE & ACTIVATION PAGE",
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}



