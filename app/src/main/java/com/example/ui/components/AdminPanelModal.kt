package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.ActivationSecurityManager

@Composable
fun AdminPanelModal(
    onDismiss: () -> Unit,
    onResetProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var inputDeviceId by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf("") }
    var generationHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val currentDeviceId = remember { ActivationSecurityManager.getDeviceId(context) }
    var isDeviceCurrentlyActivated by remember { mutableStateOf(ActivationSecurityManager.isActivated(context)) }

    val redBrand = Color(0xFFC62828)
    val darkRed = Color(0xFF8A0000)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color(0xFFF1F5F9)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar
                Surface(
                    color = redBrand,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = redBrand,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "MASTER ADMIN PANEL",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Offline Key Generator & Security",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFCDD2)
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
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: OFFLINE ACTIVATION KEY GENERATOR TOOL
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                    color = Color(0xFF1E293B)
                                )
                            }

                            Text(
                                text = "Paste a driver's unique Device ID below to instantly generate their matching offline Activation Key.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = redBrand,
                                    focusedLabelColor = redBrand,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
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
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.5.dp, Color(0xFF86EFAC)),
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
                                            color = Color(0xFF166534),
                                            letterSpacing = 1.sp
                                        )

                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = generatedKey,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF15803D),
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
                                            // Copy Button
                                            Button(
                                                onClick = {
                                                    val clip = ClipData.newPlainText("Activation Key", generatedKey)
                                                    clipboardManager.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Copied: $generatedKey", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("copy_generated_key_button")
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("COPY KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            // Share Button
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
                                                border = BorderStroke(1.dp, Color(0xFF16A34A)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
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

                    // SECTION 2: PRESERVED PASSWORD SYSTEM REFERENCE
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                    color = Color(0xFF1E293B)
                                )
                            }

                            Text(
                                text = "All master keys and bypass passwords remain fully functional offline:",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PasswordRow("2604", "Standard Master OTP & Admin PIN")
                                PasswordRow("1404", "Secondary Master Meter PIN")
                                PasswordRow("1981", "Alternate Master PIN")
                                PasswordRow("1974", "Master Admin Key")
                                PasswordRow("Master1974", "Master Admin Key Alpha")
                                PasswordRow("140423", "Date Master PIN")
                            }
                        }
                    }

                    // SECTION 3: THIS DEVICE ADMINISTRATION
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                    color = Color(0xFF1E293B)
                                )
                            }

                            Surface(
                                color = if (isDeviceCurrentlyActivated) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDeviceCurrentlyActivated) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Device Activation Status", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (isDeviceCurrentlyActivated) "ACTIVE (UNLOCKED)" else "LOCKED (UNACTIVATED)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDeviceCurrentlyActivated) Color(0xFF15803D) else Color(0xFFDC2626)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isDeviceCurrentlyActivated) Icons.Default.CheckCircle else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isDeviceCurrentlyActivated) Color(0xFF16A34A) else Color(0xFFDC2626)
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DEACTIVATE THIS DEVICE (LOCK TEST)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
private fun PasswordRow(code: String, description: String) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                color = Color(0xFF0F172A)
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
