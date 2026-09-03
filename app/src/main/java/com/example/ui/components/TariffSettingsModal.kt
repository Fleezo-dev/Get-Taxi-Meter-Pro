package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

@Composable
fun TariffSettingsModal(
    currentBaseFare: Double,
    currentFarePerKm: Double,
    currentWaitFarePerMin: Double,
    currency: String = "₹",
    onDismiss: () -> Unit,
    onSave: (baseFare: Double, farePerKm: Double, waitFarePerMin: Double) -> Unit
) {
    var baseFareInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentBaseFare)) }
    var farePerKmInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentFarePerKm)) }
    var waitFarePerMinInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentWaitFarePerMin)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val brandRed = Color(0xFFE11D48)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("tariff_settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = brandRed.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tariff Settings",
                            tint = brandRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TARIFF SETTINGS",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Adjust meter base fare, per-KM rate, and wait charge",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }

                // 1. Base Fare Input
                OutlinedTextField(
                    value = baseFareInput,
                    onValueChange = {
                        baseFareInput = it
                        errorMessage = null
                    },
                    label = { Text("Base Minimum Fare ($currency)") },
                    leadingIcon = {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = brandRed,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tariff_base_fare")
                )

                // 2. Per-KM Rate Input
                OutlinedTextField(
                    value = farePerKmInput,
                    onValueChange = {
                        farePerKmInput = it
                        errorMessage = null
                    },
                    label = { Text("Distance Rate ($currency / KM)") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = brandRed,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tariff_rate_per_km")
                )

                // 3. Wait Fare Input
                OutlinedTextField(
                    value = waitFarePerMinInput,
                    onValueChange = {
                        waitFarePerMinInput = it
                        errorMessage = null
                    },
                    label = { Text("Waiting Fare ($currency / Min)") },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = brandRed,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tariff_wait_fare")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = brandRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val base = baseFareInput.toDoubleOrNull()
                            val perKm = farePerKmInput.toDoubleOrNull()
                            val wait = waitFarePerMinInput.toDoubleOrNull()

                            if (base == null || base < 0) {
                                errorMessage = "Please enter a valid base fare"
                                return@Button
                            }
                            if (perKm == null || perKm <= 0) {
                                errorMessage = "Please enter a valid rate per KM"
                                return@Button
                            }
                            if (wait == null || wait < 0) {
                                errorMessage = "Please enter a valid waiting rate"
                                return@Button
                            }

                            onSave(base, perKm, wait)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_tariff")
                    ) {
                        Text("SAVE TARIFF", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
