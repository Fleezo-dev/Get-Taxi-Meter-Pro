package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.model.PendingTrip
import com.example.viewmodel.PendingTripsViewModel

@Composable
fun PendingTripsBoardModal(
    pendingTripsViewModel: PendingTripsViewModel,
    onDismiss: () -> Unit,
    onClaimTripSuccess: (PendingTrip) -> Unit
) {
    val context = LocalContext.current
    val pendingTrips by pendingTripsViewModel.pendingTrips.collectAsState()
    val isLoading by pendingTripsViewModel.isLoading.collectAsState()
    val errorMessage by pendingTripsViewModel.errorMessage.collectAsState()

    var tripToClaim by remember { mutableStateOf<PendingTrip?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var otpErrorMessage by remember { mutableStateOf<String?>(null) }

    val brandRed = Color(0xFFE11D48)

    LaunchedEffect(Unit) {
        pendingTripsViewModel.refreshPendingTrips()
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
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar
                Surface(
                    color = Color(0xFF1E293B),
                    shadowElevation = 8.dp,
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(brandRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Pending Trips",
                                    tint = brandRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "PENDING DISPATCH TRIPS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                Text(
                                    text = "Supabase Cloud Real-time Feed",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { pendingTripsViewModel.refreshPendingTrips() },
                                modifier = Modifier.testTag("refresh_pending_trips_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_pending_trips_modal")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Main Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (isLoading && pendingTrips.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = brandRed)
                            Text(
                                text = "Fetching available trips from Supabase...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    } else if (pendingTrips.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "No Pending Trips Right Now",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "All dispatch trips have been claimed. When dispatch loads a new trip, it will appear here automatically.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Button(
                                onClick = { pendingTripsViewModel.refreshPendingTrips() },
                                colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check for New Trips", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Available Trips (${pendingTrips.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "🔒 OTP Required to Claim",
                                            fontSize = 11.sp,
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            items(pendingTrips, key = { it.id ?: it.tripOtp }) { trip ->
                                DriverPendingTripCard(
                                    trip = trip,
                                    brandColor = brandRed,
                                    onClaimClick = {
                                        tripToClaim = trip
                                        otpInput = ""
                                        otpErrorMessage = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 🔒 OTP CLAIM MODAL
    if (tripToClaim != null) {
        val currentTrip = tripToClaim!!
        Dialog(
            onDismissRequest = { tripToClaim = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.5.dp, brandRed)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(brandRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = brandRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "ENTER CUSTOMER TRIP OTP",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    Text(
                        text = "Ask customer for the OTP sent by dispatch to claim this trip and unlock the custom meter tariff.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    // Trip Summary Preview
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Phone: ${currentTrip.customerPhone}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            if (!currentTrip.pickupLocation.isNullOrBlank()) {
                                Text(
                                    text = "Pickup: ${currentTrip.pickupLocation}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF34D399)
                                )
                            }
                            if (!currentTrip.dropLocation.isNullOrBlank()) {
                                Text(
                                    text = "Drop: ${currentTrip.dropLocation}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF87171)
                                )
                            }
                            Text(
                                text = "Tariff: Base ₹${currentTrip.baseFare} | Rate ₹${currentTrip.perKmFare}/KM",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // OTP Input Field
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            otpInput = it
                            otpErrorMessage = null
                        },
                        label = { Text("Trip OTP") },
                        placeholder = { Text("Enter 4-digit OTP") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = brandRed) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = brandRed,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_otp_input")
                    )

                    if (otpErrorMessage != null) {
                        Text(
                            text = otpErrorMessage ?: "",
                            color = Color(0xFFF87171),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { tripToClaim = null },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (otpInput.trim().isBlank()) {
                                    otpErrorMessage = "Please enter OTP"
                                    return@Button
                                }
                                pendingTripsViewModel.claimTripWithOtp(
                                    trip = currentTrip,
                                    enteredOtp = otpInput,
                                    onSuccess = { claimedTrip ->
                                        tripToClaim = null
                                        onDismiss()
                                        onClaimTripSuccess(claimedTrip)
                                        Toast.makeText(context, "✅ Trip Claimed! Custom rates applied to meter.", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { err ->
                                        otpErrorMessage = err
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("verify_and_claim_button")
                        ) {
                            Text("VERIFY & CLAIM", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverPendingTripCard(
    trip: PendingTrip,
    brandColor: Color,
    onClaimClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Customer Phone & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = trip.customerPhone,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DISPATCH PENDING",
                        color = Color(0xFFFCD34D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (!trip.customerName.isNullOrBlank()) {
                Text(
                    text = "Customer: ${trip.customerName}",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
            }

            // Pickup & Drop
            if (!trip.pickupLocation.isNullOrBlank() || !trip.dropLocation.isNullOrBlank()) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!trip.pickupLocation.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = trip.pickupLocation,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        if (!trip.dropLocation.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PinDrop, contentDescription = null, tint = brandColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = trip.dropLocation,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Fare details & Claim Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pre-set Fare Rate", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = "Base ₹${trip.baseFare} • ₹${trip.perKmFare}/KM",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFF38BDF8)
                    )
                }

                Button(
                    onClick = onClaimClick,
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("claim_trip_button_${trip.id ?: 0}")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CLAIM (OTP)",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
