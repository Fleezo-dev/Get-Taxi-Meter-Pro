package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    currentRentalBaseHours: Double = 1.0,
    currentRentalExtraKmRate: Double = 25.0,
    currentRentalExtraHourRate: Double = 350.0,
    currentOutstationTripType: String = "ROUNDTRIP",
    currentOutstationDriverBeta: Double = 500.0,
    currentOutstationMinKm: Double = 250.0,
    currentOutstationPerKmRate: Double = 15.0,
    currentWaitingFreeMinutes: Int = 5,
    currency: String = "₹",
    onDismiss: () -> Unit,
    onSave: (baseFare: Double, farePerKm: Double, waitFarePerMin: Double) -> Unit,
    onSaveExtended: (
        baseFare: Double,
        farePerKm: Double,
        waitFarePerMin: Double,
        rentalBaseHours: Double,
        rentalExtraKm: Double,
        rentalExtraHour: Double,
        outstationTripType: String,
        outstationDriverBeta: Double,
        outstationMinKm: Double,
        outstationPerKm: Double,
        waitingFreeMinutes: Int
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // 1. Local Charges
    var baseFareInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentBaseFare)) }
    var farePerKmInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentFarePerKm)) }
    var waitFarePerMinInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentWaitFarePerMin)) }

    // 2. Hourly Rental
    var rentalBaseHoursInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", currentRentalBaseHours)) }
    var rentalExtraKmInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentRentalExtraKmRate)) }
    var rentalExtraHourInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentRentalExtraHourRate)) }

    // 3. Outstation
    var outstationTripType by remember { mutableStateOf(currentOutstationTripType) } // "ROUNDTRIP" or "ONEWAY"
    var outstationDriverBetaInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentOutstationDriverBeta)) }
    var outstationMinKmInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", currentOutstationMinKm)) }
    var outstationPerKmInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentOutstationPerKmRate)) }

    // 4. Waiting Charges
    var waitingFreeMinutesInput by remember { mutableStateOf(currentWaitingFreeMinutes.toString()) }
    var waitingCostPerMinInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentWaitFarePerMin)) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val brandRed = Color(0xFFE11D48)

    val tabs = listOf("Local", "Hourly Rental", "Outstation", "Waiting Charges")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .testTag("tariff_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 580.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Modal Header Icon
                Surface(
                    color = brandRed.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tariff Settings",
                            tint = brandRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "TARIFF SETTINGS",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Configure rates across Local, Rental & Outstation services",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Tab Row for 4 categories
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = brandRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tariff_tabs")
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == index) brandRed else Color(0xFF64748B)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content for Selected Tab
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        // TAB 0: LOCAL CHARGES
                        0 -> {
                            Text(
                                text = "Local City Meter Rates",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )

                            // Base Fare
                            OutlinedTextField(
                                value = baseFareInput,
                                onValueChange = {
                                    baseFareInput = it
                                    errorMessage = null
                                },
                                label = { Text("Base Fare ($currency)") },
                                leadingIcon = {
                                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_tariff_base_fare")
                            )

                            // Per-KM Rate
                            OutlinedTextField(
                                value = farePerKmInput,
                                onValueChange = {
                                    farePerKmInput = it
                                    errorMessage = null
                                },
                                label = { Text("Per KM Rate ($currency / KM)") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_tariff_rate_per_km")
                            )

                            // Wait Time Rate
                            OutlinedTextField(
                                value = waitFarePerMinInput,
                                onValueChange = {
                                    waitFarePerMinInput = it
                                    waitingCostPerMinInput = it
                                    errorMessage = null
                                },
                                label = { Text("Wait Time Rate ($currency / Min)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_tariff_wait_fare")
                            )
                        }

                        // TAB 1: HOURLY RENTAL
                        1 -> {
                            Text(
                                text = "Hourly Package Rates",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )

                            // Base Hours
                            OutlinedTextField(
                                value = rentalBaseHoursInput,
                                onValueChange = {
                                    rentalBaseHoursInput = it
                                    errorMessage = null
                                },
                                label = { Text("Base Hours (e.g. 1.0, 2.0, 4.0 hrs)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_rental_base_hours")
                            )

                            // Extra KM Rate
                            OutlinedTextField(
                                value = rentalExtraKmInput,
                                onValueChange = {
                                    rentalExtraKmInput = it
                                    errorMessage = null
                                },
                                label = { Text("Extra KM Rate ($currency / KM)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_rental_extra_km")
                            )

                            // Extra Hour Rate
                            OutlinedTextField(
                                value = rentalExtraHourInput,
                                onValueChange = {
                                    rentalExtraHourInput = it
                                    errorMessage = null
                                },
                                label = { Text("Extra Hour Rate ($currency / Hour)") },
                                leadingIcon = {
                                    Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_rental_extra_hour")
                            )
                        }

                        // TAB 2: OUTSTATION
                        2 -> {
                            Text(
                                text = "Outstation Journey Parameters",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )

                            // Trip Type: Roundtrip vs Oneway Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (outstationTripType == "ROUNDTRIP") brandRed else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { outstationTripType = "ROUNDTRIP" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.SyncAlt,
                                            contentDescription = null,
                                            tint = if (outstationTripType == "ROUNDTRIP") Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Round Trip",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (outstationTripType == "ROUNDTRIP") Color.White else Color(0xFF475569)
                                        )
                                    }
                                }

                                Surface(
                                    color = if (outstationTripType == "ONEWAY") brandRed else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { outstationTripType = "ONEWAY" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (outstationTripType == "ONEWAY") Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "One Way",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (outstationTripType == "ONEWAY") Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }

                            // Driver Beta / Day
                            OutlinedTextField(
                                value = outstationDriverBetaInput,
                                onValueChange = {
                                    outstationDriverBetaInput = it
                                    errorMessage = null
                                },
                                label = { Text("Driver Beta / Day ($currency)") },
                                leadingIcon = {
                                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_outstation_driver_beta")
                            )

                            // Min KM / Day
                            OutlinedTextField(
                                value = outstationMinKmInput,
                                onValueChange = {
                                    outstationMinKmInput = it
                                    errorMessage = null
                                },
                                label = { Text("Min KM / Day (e.g. 250 KM)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_outstation_min_km")
                            )

                            // Outstation KM Rate
                            OutlinedTextField(
                                value = outstationPerKmInput,
                                onValueChange = {
                                    outstationPerKmInput = it
                                    errorMessage = null
                                },
                                label = { Text("Outstation Rate ($currency / KM)") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_outstation_rate_per_km")
                            )
                        }

                        // TAB 3: WAITING CHARGES
                        3 -> {
                            Text(
                                text = "Waiting Time Policy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )

                            // Free Minutes Threshold
                            OutlinedTextField(
                                value = waitingFreeMinutesInput,
                                onValueChange = {
                                    waitingFreeMinutesInput = it
                                    errorMessage = null
                                },
                                label = { Text("Free Minutes Threshold (Mins)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_waiting_free_minutes")
                            )

                            // Cost Per Additional Minute
                            OutlinedTextField(
                                value = waitingCostPerMinInput,
                                onValueChange = {
                                    waitingCostPerMinInput = it
                                    waitFarePerMinInput = it
                                    errorMessage = null
                                },
                                label = { Text("Cost Per Additional Minute ($currency / Min)") },
                                leadingIcon = {
                                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = brandRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedFieldColors(brandRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_waiting_cost_per_min")
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = brandRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Modal Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
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

                            val rBaseHours = rentalBaseHoursInput.toDoubleOrNull() ?: 1.0
                            val rExtraKm = rentalExtraKmInput.toDoubleOrNull() ?: 25.0
                            val rExtraHour = rentalExtraHourInput.toDoubleOrNull() ?: 350.0

                            val oBeta = outstationDriverBetaInput.toDoubleOrNull() ?: 500.0
                            val oMinKm = outstationMinKmInput.toDoubleOrNull() ?: 250.0
                            val oPerKm = outstationPerKmInput.toDoubleOrNull() ?: 15.0

                            val wFreeMins = waitingFreeMinutesInput.toIntOrNull() ?: 5

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
                            onSaveExtended(
                                base,
                                perKm,
                                wait,
                                rBaseHours,
                                rExtraKm,
                                rExtraHour,
                                outstationTripType,
                                oBeta,
                                oMinKm,
                                oPerKm,
                                wFreeMins
                            )
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

@Composable
private fun outlinedFieldColors(brandRed: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC),
    focusedBorderColor = brandRed,
    unfocusedBorderColor = Color(0xFFCBD5E1)
)
