package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.DriverProfileRepository
import com.example.service.LocationTrackingService
import com.example.ui.theme.TaxiRedPrimary
import com.example.util.TtsManager
import com.example.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val tripState by LocationTrackingService.tripState.collectAsStateWithLifecycle()
    val baseFareState by viewModel.baseFare.collectAsStateWithLifecycle()
    val farePerKmState by viewModel.farePerKm.collectAsStateWithLifecycle()
    val waitFarePerMinState by viewModel.waitFarePerMin.collectAsStateWithLifecycle()
    val speedThresholdState by viewModel.speedThreshold.collectAsStateWithLifecycle()
    val audioEnabledState by viewModel.audioEnabled.collectAsStateWithLifecycle()
    val autoStartEnabledState by viewModel.autoStartEnabled.collectAsStateWithLifecycle()
    val currencyState by viewModel.currency.collectAsStateWithLifecycle()
    val outOfCitySurchargePercentState by viewModel.outOfCitySurchargePercent.collectAsStateWithLifecycle()
    val isDarkModeState by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val ttsVoiceProfileState by viewModel.ttsVoiceProfile.collectAsStateWithLifecycle()
    val ttsSpeechRateState by viewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val ttsPitchState by viewModel.ttsPitch.collectAsStateWithLifecycle()

    var baseFareInput by remember { mutableStateOf("") }
    var farePerKmInput by remember { mutableStateOf("") }
    var waitFarePerMinInput by remember { mutableStateOf("") }
    var speedThresholdInput by remember { mutableStateOf("") }
    var currencyInput by remember { mutableStateOf("") }
    var outOfCitySurchargePercentInput by remember { mutableStateOf("") }
    var audioEnabled by remember { mutableStateOf(true) }
    var autoStartEnabled by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var ttsVoiceProfile by remember { mutableStateOf("FEMALE_1") }
    var ttsSpeechRate by remember { mutableFloatStateOf(1.0f) }
    var ttsPitch by remember { mutableFloatStateOf(1.15f) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Synchronize inputs once preferences load
    LaunchedEffect(
        baseFareState,
        farePerKmState,
        waitFarePerMinState,
        speedThresholdState,
        currencyState,
        outOfCitySurchargePercentState,
        audioEnabledState,
        autoStartEnabledState,
        isDarkModeState,
        ttsVoiceProfileState,
        ttsSpeechRateState,
        ttsPitchState
    ) {
        baseFareInput = baseFareState.toString()
        farePerKmInput = farePerKmState.toString()
        waitFarePerMinInput = waitFarePerMinState.toString()
        speedThresholdInput = speedThresholdState.toString()
        currencyInput = currencyState
        outOfCitySurchargePercentInput = outOfCitySurchargePercentState.toString()
        audioEnabled = audioEnabledState
        autoStartEnabled = autoStartEnabledState
        isDarkMode = isDarkModeState
        ttsVoiceProfile = ttsVoiceProfileState
        ttsSpeechRate = ttsSpeechRateState
        ttsPitch = ttsPitchState
    }

    var showSavedMessage by remember { mutableStateOf(false) }

    // Live Calculator Preview values
    val currentBf = baseFareInput.toDoubleOrNull() ?: 0.0
    val currentFk = farePerKmInput.toDoubleOrNull() ?: 0.0
    val currentWf = waitFarePerMinInput.toDoubleOrNull() ?: 0.0
    val sampleKm = 5.0
    val sampleWaitMin = 3.0
    val sampleTotalFare = currentBf + (sampleKm * currentFk) + (sampleWaitMin * currentWf)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fare Engine Config", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TaxiRedPrimary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val bf = baseFareInput.toDoubleOrNull() ?: baseFareState
                    val fk = farePerKmInput.toDoubleOrNull() ?: farePerKmState
                    val wf = waitFarePerMinInput.toDoubleOrNull() ?: waitFarePerMinState
                    val st = speedThresholdInput.toDoubleOrNull() ?: speedThresholdState
                    val ooc = outOfCitySurchargePercentInput.toDoubleOrNull() ?: outOfCitySurchargePercentState

                    viewModel.updateBaseFare(bf)
                    viewModel.updateFarePerKm(fk)
                    viewModel.updateWaitFarePerMin(wf)
                    viewModel.updateSpeedThreshold(st)
                    viewModel.updateCurrency(currencyInput)
                    viewModel.updateOutOfCitySurchargePercent(ooc)
                    viewModel.updateAudioEnabled(audioEnabled)
                    viewModel.updateAutoStartEnabled(autoStartEnabled)
                    viewModel.updateDarkMode(isDarkMode)
                    viewModel.updateTtsVoiceProfile(ttsVoiceProfile)
                    viewModel.updateTtsSpeechRate(ttsSpeechRate)
                    viewModel.updateTtsPitch(ttsPitch)

                    com.example.service.LocationTrackingService.startMonitoring(
                        context = context,
                        baseFare = bf,
                        farePerKm = fk,
                        waitFarePerMin = wf,
                        currency = currencyInput,
                        speedThreshold = st,
                        autoStartEnabled = autoStartEnabled
                    )

                    showSavedMessage = true
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = Color.White) },
                text = { Text("Save Config", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                containerColor = TaxiRedPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.testTag("save_settings_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (showSavedMessage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF065F46))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Configuration Saved Successfully!", color = Color(0xFF065F46), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }

            // THEME TOGGLE CARD (DARK MODE / LIGHT MODE)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("theme_toggle_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFFFEBEE),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Theme",
                                tint = if (isDarkMode) Color(0xFFFFD54F) else TaxiRedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isDarkMode) "Dark Mode" else "Light Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkMode) "Deep black/dark slate canvas active" else "Clean light gray/off-white canvas active",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { checked ->
                            isDarkMode = checked
                            viewModel.updateDarkMode(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TaxiRedPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("switch_dark_mode")
                    )
                }
            }

            // QUICK PRESET SELECTION CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TARIFF PRESETS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currencyInput == "₹" && baseFareInput == "80.0",
                            onClick = {
                                baseFareInput = "80.0"
                                farePerKmInput = "28.0"
                                waitFarePerMinInput = "2.0"
                                speedThresholdInput = "5.0"
                                currencyInput = "₹"
                            },
                            label = { Text("🚕 Standard Taxi (₹80/₹28)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDCFCE7),
                                selectedLabelColor = Color(0xFF15803D)
                            )
                        )
                    }
                }
            }

            // LIVE CALCULATOR PREVIEW CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE CALCULATOR PREVIEW",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ESTIMATOR",
                                color = Color(0xFF86EFAC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sample Trip: 5.0 km distance + 3 mins wait",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• Base Fare: $currencyInput${String.format(Locale.US, "%.2f", currentBf)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Dist: $currencyInput${String.format(Locale.US, "%.2f", sampleKm * currentFk)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Wait: $currencyInput${String.format(Locale.US, "%.2f", sampleWaitMin * currentWf)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Total Fare:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currencyInput${String.format(Locale.US, "%.2f", sampleTotalFare)}",
                            color = Color(0xFF4ADE80),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Text(
                text = "CUSTOM FARE METRICS",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp
            )

            // Base Fare Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Base Fare Setup ($currencyInput)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "The initial flag drop charge applied when a trip starts.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = baseFareInput,
                        onValueChange = { baseFareInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaxiRedPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = TaxiRedPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_base_fare")
                    )
                }
            }

            // Fare Per Kilometer Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Distance Rate ($currencyInput per KM)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "The rate charged per physical kilometer driven during service.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = farePerKmInput,
                        onValueChange = { farePerKmInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaxiRedPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = TaxiRedPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_fare_per_km")
                    )
                }
            }

            // Waiting Charge Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Wait Charges ($currencyInput per Minute)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "The charge accumulated per minute when speed is below threshold.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = waitFarePerMinInput,
                        onValueChange = { waitFarePerMinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaxiRedPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = TaxiRedPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_wait_fare_per_min")
                    )
                }
            }

            // Speed Threshold Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Wait Speed Threshold (km/h)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Speed boundary (km/h) below which wait billing takes effect.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = speedThresholdInput,
                        onValueChange = { speedThresholdInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaxiRedPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = TaxiRedPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_speed_threshold")
                    )
                }
            }

            // Out of City Surcharge & Mode Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (tripState.isOutOfCity) Color(0xFFEA580C) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (tripState.isOutOfCity) Color(0xFFFFEDD5) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = if (tripState.isOutOfCity) Color(0xFFEA580C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Out of City Charges Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (tripState.isOutOfCity) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ACTIVE",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = Color(0xFFEA580C),
                                            modifier = Modifier
                                                .background(Color(0xFFFFEDD5), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (tripState.isOutOfCity)
                                        "Outstation tariff active (+${outOfCitySurchargePercentInput}% surcharge)"
                                    else
                                        "Enable outstation return tariff for long distance rides",
                                    fontSize = 11.sp,
                                    color = if (tripState.isOutOfCity) Color(0xFFEA580C) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = tripState.isOutOfCity,
                            onCheckedChange = { enabled ->
                                val surchargePct = outOfCitySurchargePercentInput.toDoubleOrNull() ?: outOfCitySurchargePercentState
                                LocationTrackingService.toggleOutOfCity(context, enabled, surchargePct)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("toggle_out_of_city")
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = "Out of City Surcharge Rate (%)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Percentage added to base + distance fare when Out of City mode is enabled.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    OutlinedTextField(
                        value = outOfCitySurchargePercentInput,
                        onValueChange = { outOfCitySurchargePercentInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = Color(0xFFEA580C)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_out_of_city_surcharge")
                    )
                }
            }

            // Currency Symbol Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Currency Symbol",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Select a preset symbol or type a custom currency sign.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("₹", "$", "€", "£", "¥").forEach { symbol ->
                            FilterChip(
                                selected = currencyInput == symbol,
                                onClick = { currencyInput = symbol },
                                label = { Text(symbol, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TaxiRedPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { currencyInput = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaxiRedPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = TaxiRedPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_currency")
                    )
                }
            }

            // Movement Auto-Start Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Start Meter on Movement",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Automatically starts or resumes the meter when vehicle speed exceeds the threshold.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = autoStartEnabled,
                        onCheckedChange = { autoStartEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TaxiRedPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("toggle_autostart")
                    )
                }
            }

            // TEXT-TO-SPEECH (TTS) & VOICE SETTINGS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("tts_settings_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header & Master Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFFFEBEE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Voice Announcements",
                                    tint = TaxiRedPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "TTS Voice Announcements",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Spoken trip start greetings & trip end messages",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = audioEnabled,
                            onCheckedChange = {
                                audioEnabled = it
                                viewModel.updateAudioEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TaxiRedPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("toggle_audio")
                        )
                    }

                    if (audioEnabled) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // Voice Profile Selection (3-4 Voice Profiles)
                        Text(
                            text = "VOICE PROFILES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TaxiRedPrimary,
                            letterSpacing = 1.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TtsManager.VOICE_PROFILES.forEach { profile ->
                                val isSelected = ttsVoiceProfile == profile.id
                                Surface(
                                    color = if (isSelected) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) TaxiRedPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            ttsVoiceProfile = profile.id
                                            ttsPitch = profile.defaultPitch
                                            ttsSpeechRate = profile.defaultSpeechRate
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = profile.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) TaxiRedPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = if (isSelected) TaxiRedPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = profile.gender.uppercase(),
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = profile.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                ttsVoiceProfile = profile.id
                                                ttsPitch = profile.defaultPitch
                                                ttsSpeechRate = profile.defaultSpeechRate
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = TaxiRedPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Pitch Slider
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Voice Pitch",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2fx", ttsPitch),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = TaxiRedPrimary
                                )
                            }
                            Slider(
                                value = ttsPitch,
                                onValueChange = { ttsPitch = it },
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = TaxiRedPrimary,
                                    activeTrackColor = TaxiRedPrimary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("slider_tts_pitch")
                            )
                        }

                        // Speech Rate Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speech Speed Rate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2fx", ttsSpeechRate),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = TaxiRedPrimary
                                )
                            }
                            Slider(
                                value = ttsSpeechRate,
                                onValueChange = { ttsSpeechRate = it },
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = TaxiRedPrimary,
                                    activeTrackColor = TaxiRedPrimary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("slider_tts_speed")
                            )
                        }

                        // Test Voice Button
                        Button(
                            onClick = {
                                TtsManager.getInstance(context).testVoice(
                                    profileId = ttsVoiceProfile,
                                    pitch = ttsPitch,
                                    rate = ttsSpeechRate
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TaxiRedPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_test_voice")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TEST VOICE ANNOUNCEMENT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Mandatory triggers note
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🔊 Mandatory Spoken Greetings:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Trip Start: \"Welcome to Get Taxi. Please wear the seat belt.\"",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• Trip End (Day): \"Thank you for traveling with us. Have a good day.\"",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• Trip End (Night): \"Thank you for traveling with us. Have a good night.\"",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // System Notification Alert Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_sound_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF475569),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notification Alerts",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SYSTEM NOTIFICATION CHANNELS",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Standard Android Sound & Vibration",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Incoming trip alerts leverage high-priority standard Android system notification sounds and physical haptic vibration to eliminate UI thread overhead and maximize reliability.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Admin Utilities Section
            var showAdminPinDialog by remember { mutableStateOf(false) }
            var adminPinInput by remember { mutableStateOf("") }
            var resetStatus by remember { mutableStateOf("") }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ADMIN UTILITIES",
                color = TaxiRedPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Danger Zone", color = TaxiRedPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Reset the fleet driver ID counter back to DRV-0011 and clear all driver mappings.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                    Button(
                        onClick = { showAdminPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TaxiRedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(text = "RESET DRIVER ID COUNTER", fontWeight = FontWeight.Bold)
                    }

                    if (resetStatus.isNotBlank()) {
                        Text(
                            text = resetStatus,
                            color = if (resetStatus.contains("Success")) Color(0xFF2E7D32) else TaxiRedPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showAdminPinDialog) {
                AlertDialog(
                    onDismissRequest = { showAdminPinDialog = false; adminPinInput = "" },
                    title = { Text("Admin PIN Required") },
                    text = {
                        OutlinedTextField(
                            value = adminPinInput,
                            onValueChange = { adminPinInput = it },
                            label = { Text("Enter PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (adminPinInput == "2604" || adminPinInput == "1974" || adminPinInput == "1981" || adminPinInput == "140423") {
                                resetStatus = "Resetting..."
                                coroutineScope.launch {
                                    try {
                                        DriverProfileRepository(context).resetLocalCounter()
                                        resetStatus = "Successfully reset counter!"
                                    } catch (e: Exception) {
                                        resetStatus = "Failed to reset counter: ${e.message}"
                                    }
                                }
                                showAdminPinDialog = false
                                adminPinInput = ""
                            } else {
                                resetStatus = "Invalid PIN"
                                showAdminPinDialog = false
                                adminPinInput = ""
                            }
                        }) {
                            Text("CONFIRM")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAdminPinDialog = false; adminPinInput = "" }) {
                            Text("CANCEL")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

