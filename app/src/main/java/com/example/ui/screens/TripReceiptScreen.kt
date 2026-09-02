package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.TripEntity
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.DispatchViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripReceiptScreen(
    viewModel: MeterViewModel,
    dispatchViewModel: DispatchViewModel,
    tripId: Int,
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val trip = remember(allTrips, tripId) { 
        if (tripId == 0 && allTrips.isNotEmpty()) allTrips.first() else allTrips.find { it.id == tripId } 
    }

    var customerPhoneInput by remember { mutableStateOf("") }
    var passengerNameInput by remember { mutableStateOf("") }
    var noteStatusMessage by remember { mutableStateOf("") }
    val driverProfile by dispatchViewModel.driverProfile.collectAsStateWithLifecycle()

    LaunchedEffect(trip) {
        if (trip != null && trip.passengerNotes.isNotEmpty()) {
            passengerNameInput = trip.passengerNotes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GET TAXI INVOICE", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("receipt_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFC62828))
            )
        },
        containerColor = Color(0xFF8A0000)
    ) { innerPadding ->
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Receipt session not found.", color = Color(0xFF64748B))
            }
        } else {
            val dateOnlyFormatter = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
            val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
            
            val formattedDate = dateOnlyFormatter.format(Date(trip.startTime))
            val startTimeStr = timeFormatter.format(Date(trip.startTime))
            val endTimeStr = if (trip.endTime > 0) timeFormatter.format(Date(trip.endTime)) else timeFormatter.format(Date(trip.startTime + (trip.durationSeconds * 1000)))
            
            val durationMin = trip.durationSeconds / 60
            val durationSec = trip.durationSeconds % 60
            val waitingMin = trip.waitingSeconds / 60
            val waitingSec = trip.waitingSeconds % 60

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TRUSTY YELLOW CAB INVOICE STYLE
                val driverProfile by dispatchViewModel.driverProfile.collectAsStateWithLifecycle()
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFFE53935)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("GET", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                    Text("TAXI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                }
                            }
                             Column(horizontalAlignment = Alignment.End) {
                                  Text("GET TAXI KOVAI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                                  Text("No 286, Diwan Bahadur Rd,", fontSize = 10.sp, color = Color.DarkGray)
                                  Text("R.S. Puram, Coimbatore,", fontSize = 10.sp, color = Color.DarkGray)
                                  Text("Tamil Nadu 641001", fontSize = 10.sp, color = Color.DarkGray)
                                  Text("kovai@gettaxi.in", fontSize = 10.sp, color = Color.DarkGray)
                             }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text("INVOICE", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black, letterSpacing = 1.sp)
                                val tid = trip.id.toString()
                                val safeTid = if (tid.length >= 6) tid.takeLast(6).uppercase() else tid.uppercase()
                                Text("Invoice No: GT-$safeTid", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Text("Date: $formattedDate, $endTimeStr", fontSize = 11.sp, color = Color.Black)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
                        
                        val customerDisplayName = passengerNameInput.ifBlank { trip.passengerNotes.ifBlank { "Valued Customer" } }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PASSENGER DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                                Text(customerDisplayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("Mobile: ${customerPhoneInput.ifBlank { "N/A" }}", fontSize = 12.sp, color = Color.Black)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("TRIP TIMINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                                Text("Start: $formattedDate, $startTimeStr", fontSize = 11.sp, color = Color.Black)
                                Text("End: $formattedDate, $endTimeStr", fontSize = 11.sp, color = Color.Black)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
                        
                        Text("TRIP ROUTE ADDRESSES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp, end = 8.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                Box(modifier = Modifier.width(2.dp).height(24.dp).background(Color.LightGray))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF44336)))
                            }
                            Column {
                                Text("PICKUP POINT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                val pLoc = if (trip.pickupAddress.isNotBlank()) trip.pickupAddress else if (trip.startLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.startLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.startLongitude)}" else "GPS Location"
                                Text(pLoc, fontSize = 11.sp, color = Color.Black, modifier = Modifier.padding(bottom = 4.dp))
                                Text("DROPOFF POINT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                val dLoc = if (trip.dropAddress.isNotBlank()) trip.dropAddress else if (trip.endLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.endLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.endLongitude)}" else "GPS Location"
                                Text(dLoc, fontSize = 11.sp, color = Color.Black)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
                        
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("VEHICLE DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 0.5.sp)
                                Text("Vehicle: ${driverProfile.vehicleModel}", fontSize = 11.sp, color = Color.DarkGray)
                                Text("Reg No: ${driverProfile.vehiclePlate}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TOTAL DISTANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                                Text("${String.format(Locale.US, "%.2f", trip.distanceKm)} KM", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                Text("Duration: ${String.format("%02d:%02d:%02d", durationMin/60, durationMin%60, durationSec)}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
                        
                        Text("CHARGES BREAKDOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        when (trip.rideType) {
                            "HOURLY_RENTAL" -> {
                                val rentalResult = com.example.data.model.HourlyRentalFareEngine.calculateHourlyRentalFare(
                                    durationInSeconds = trip.durationSeconds,
                                    distanceInKm = trip.distanceKm,
                                    extraTolls = 0.0,
                                    overrideRatePerHour = trip.ratePerHour,
                                    overrideExtraKmRate = trip.farePerKm
                                )

                                val billedHrLabel = if (rentalResult.billedHours % 1.0 == 0.0) "${rentalResult.billedHours.toInt()} Hr" else "${rentalResult.billedHours} Hr"
                                val extraKmRate = if (trip.farePerKm > 0.0) trip.farePerKm else com.example.data.model.HourlyRentalFareEngine.EXTRA_KM_RATE
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Hourly Rental Package ($billedHrLabel @ $currencySymbol${String.format(Locale.US, "%.0f", rentalResult.baseRatePerHr)}/Hr)", fontSize = 11.sp, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", rentalResult.baseFare)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Extra Distance Coverage (${String.format(Locale.US, "%.2f", rentalResult.extraKm)} KM @ $currencySymbol${String.format(Locale.US, "%.0f", extraKmRate)}/KM)", fontSize = 11.sp, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", rentalResult.extraDistanceCharge)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                            "OUTSTATION" -> {
                                val driverBeta = if (trip.baseFare > 0.0 || trip.driverBeta > 0.0) (trip.baseFare + trip.driverBeta) else 500.0
                                val perKmRate = if (trip.farePerKm > 0.0) trip.farePerKm else 15.0
                                val distFare = trip.distanceKm * perKmRate

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Driver Beta / Allowance", fontSize = 11.sp, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", driverBeta)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Outstation Distance (${String.format(Locale.US, "%.2f", trip.distanceKm)} KM @ $currencySymbol${String.format(Locale.US, "%.1f", perKmRate)}/KM)", fontSize = 11.sp, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", distFare)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                            else -> { // LOCAL_RIDE
                                val estBase = trip.baseFare
                                val estDistFare = trip.distanceKm * trip.farePerKm
                                val estWaitFare = (trip.waitingSeconds / 60.0) * trip.waitFarePerMin

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Base Fare Minimum", fontSize = 11.sp, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", estBase)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Distance Fare (${String.format(Locale.US, "%.2f", trip.distanceKm)} KM @ $currencySymbol${String.format(Locale.US, "%.1f", trip.farePerKm)}/KM)", fontSize = 11.sp, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", estDistFare)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Standby Waiting Charge (${waitingMin} Mins @ $currencySymbol${String.format(Locale.US, "%.1f", trip.waitFarePerMin)}/Min)", fontSize = 11.sp, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", estWaitFare)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        if (trip.tollCharges > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Toll Charges", fontSize = 11.sp, color = Color.Black)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", trip.tollCharges)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        if (trip.permitCharges > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Permit Charges", fontSize = 11.sp, color = Color.Black)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", trip.permitCharges)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        if (trip.parkingCharges > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Parking Charges", fontSize = 11.sp, color = Color.Black)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", trip.parkingCharges)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1.5f).padding(end = 8.dp)) {
                                Text("TERMS & CONDITIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("Above fare given based on travel distance and waiting. Toll, Parking, Permit charges may applicable extra. T&C apply.", fontSize = 8.sp, color = Color.Gray, lineHeight = 10.sp)
                                Text("Fare includes additional charges for out of the city limit pickup/drop.", fontSize = 8.sp, color = Color.Gray, lineHeight = 10.sp)
                            }
                            Column(modifier = Modifier.weight(1f).background(Color(0xFFF0F4F8), RoundedCornerShape(8.dp)).padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("GRAND TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", trip.totalFare)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFD9E2EC))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Balance Payable", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                                    Text("$currencySymbol${String.format(Locale.US, "%.2f", trip.totalFare)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
                                }
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray)
                        
                        Text("THANK YOU FOR TRAVELLING WITH US", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("This is a computer generated invoice. No physical signature is required.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
                    }
                }
                
                // Captain & Vehicle Information
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text("Get Taxi Kovai - Verified Partner", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        Text("Captain & Vehicle Information", fontSize = 11.sp, color = Color.DarkGray)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                                if (driverProfile.photoUri.isNotBlank()) {
                                    val imageModel = remember(driverProfile.photoUri) {
                                        if (driverProfile.photoUri.startsWith("data:image")) {
                                            try {
                                                val b64 = driverProfile.photoUri.substringAfter(",")
                                                android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                            } catch (e: Exception) {
                                                driverProfile.photoUri
                                            }
                                        } else {
                                            driverProfile.photoUri
                                        }
                                    }
                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = "Driver",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(painter = painterResource(id = R.drawable.ic_get_taxi_vector), contentDescription = null, modifier = Modifier.fillMaxSize().padding(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Name:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(driverProfile.driverName, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Vehicle:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(driverProfile.vehicleModel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("License Plate:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(driverProfile.vehiclePlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Secure & Verified by Get Taxi", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                    }
                }

                // OFFLINE DRIVER PAYMENT QR CODE CARD
                if (driverProfile.qrCodeUri.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "SCAN & PAY DRIVER (UPI / QR)",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Scan with GPay / PhonePe / Paytm to settle trip fare",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }

                                Surface(
                                    color = Color(0xFFE0F2FE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "AMOUNT: $currencySymbol${String.format(Locale.US, "%.2f", trip.totalFare)}",
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val qrModel = remember(driverProfile.qrCodeUri) {
                                    if (driverProfile.qrCodeUri.startsWith("data:image")) {
                                        try {
                                            val b64 = driverProfile.qrCodeUri.substringAfter(",")
                                            android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                        } catch (e: Exception) {
                                            driverProfile.qrCodeUri
                                        }
                                    } else {
                                        driverProfile.qrCodeUri
                                    }
                                }
                                AsyncImage(
                                    model = qrModel,
                                    contentDescription = "Driver UPI QR Code",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Direct Instant Settlement to Driver Account",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // DIRECT WHATSAPP BILL SENDING CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF25D366), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Send Bill Directly to Customer",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Enter customer WhatsApp number to send invoice instantly.",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = customerPhoneInput,
                            onValueChange = { customerPhoneInput = it },
                            label = { Text("Customer Mobile / WhatsApp Number") },
                            placeholder = { Text("e.g. 9043743777") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF25D366),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                cursorColor = Color(0xFF25D366)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                sendBillViaWhatsApp(
                                    context = context,
                                    phone = customerPhoneInput,
                                    trip = trip,
                                    currency = currencySymbol,
                                    startTimeStr = startTimeStr,
                                    endTimeStr = endTimeStr,
                                    durationMin = durationMin,
                                    durationSec = durationSec,
                                    waitingMin = waitingMin,
                                    waitingSec = waitingSec
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "SEND BILL VIA WHATSAPP",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // PASSENGER NOTES CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Passenger Name / Ref Notes",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passengerNameInput,
                            onValueChange = { passengerNameInput = it },
                            placeholder = { Text("e.g. John Doe / Booking #102") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val db = com.example.data.database.TripDatabase.getDatabase(context)
                                    db.tripDao().insertTrip(trip.copy(passengerNotes = passengerNameInput))
                                    noteStatusMessage = "Note saved successfully!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BorderStroke(1.5.dp, Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        }

                        if (noteStatusMessage.isNotEmpty()) {
                            Text(
                                text = noteStatusMessage,
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // GENERAL SHARE BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            shareReceiptNative(
                                context = context,
                                trip = trip,
                                currency = currencySymbol,
                                startTimeStr = startTimeStr,
                                endTimeStr = endTimeStr,
                                durationMin = durationMin,
                                durationSec = durationSec,
                                waitingMin = waitingMin,
                                waitingSec = waitingSec
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SHARE TEXT",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = {
                            PdfGenerator.generateAndSharePdf(
                                context = context,
                                trip = trip,
                                currency = currencySymbol,
                                startTimeStr = startTimeStr,
                                endTimeStr = endTimeStr,
                                durationMin = durationMin,
                                durationSec = durationSec,
                                waitingMin = waitingMin,
                                waitingSec = waitingSec,
                                driverName = driverProfile.driverName,
                                vehicleModel = driverProfile.vehicleModel,
                                vehiclePlate = driverProfile.vehiclePlate,
                                customerPhone = customerPhoneInput
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("share_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SHARE PDF",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ReceiptLineItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DottedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

private fun buildBillTextMessage(
    trip: TripEntity,
    currency: String,
    startTimeStr: String,
    endTimeStr: String,
    durationMin: Long,
    durationSec: Long,
    waitingMin: Long,
    waitingSec: Long
): String {
    val dateOnlyFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = dateOnlyFormatter.format(Date(trip.startTime))
    val passenger = if (trip.passengerNotes.isNotEmpty()) "\n👤 Passenger: ${trip.passengerNotes}" else ""

    val pLoc = if (trip.pickupAddress.isNotBlank()) trip.pickupAddress else if (trip.startLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.startLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.startLongitude)}" else "GPS Location"
    val dLoc = if (trip.dropAddress.isNotBlank()) trip.dropAddress else if (trip.endLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.endLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.endLongitude)}" else "GPS Location"

    val outOfCityText = if (trip.isOutOfCity) {
        "\n🛣️ Out of City Surcharge: $currency${String.format(Locale.US, "%.2f", trip.outOfCitySurcharge)}"
    } else ""

    val mapLinkText = if (trip.startLatitude != 0.0 && trip.endLatitude != 0.0) {
        "\n🗺️ Google Maps Route: https://www.google.com/maps/dir/?api=1&origin=${trip.startLatitude},${trip.startLongitude}&destination=${trip.endLatitude},${trip.endLongitude}"
    } else ""

    return """
🚕 *GET TAXI - OFFICIAL TRIP BILL* 🚕
WELCOME TO GET TAXI!

📅 Date: $dateStr
⏰ Start Time: $startTimeStr
⏰ End Time: $endTimeStr
⏱️ Ride Duration: ${durationMin}m ${durationSec}s
⏳ Waiting Time: ${waitingMin}m ${waitingSec}s
📍 Pick up Location: $pLoc
🏁 Drop Location: $dLoc
🛣️ Total Distance: ${String.format(Locale.US, "%.2f", trip.distanceKm)} km$passenger$mapLinkText

----------------------------------
💰 Base Fare: $currency${String.format(Locale.US, "%.2f", trip.baseFare)}
🛣️ Distance Charge: $currency${String.format(Locale.US, "%.2f", trip.distanceKm * trip.farePerKm)}
⌛ Wait Charge: $currency${String.format(Locale.US, "%.2f", (trip.waitingSeconds / 60.0) * trip.waitFarePerMin)}$outOfCityText
----------------------------------
💳 *TOTAL FARE DUE: $currency${String.format(Locale.US, "%.2f", trip.totalFare)}*
==================================

🙏 Thanks for riding with us! Have a safe & wonderful journey!
    """.trimIndent()
}

private fun sendBillViaWhatsApp(
    context: Context,
    phone: String,
    trip: TripEntity,
    currency: String,
    startTimeStr: String,
    endTimeStr: String,
    durationMin: Long,
    durationSec: Long,
    waitingMin: Long,
    waitingSec: Long
) {
    val text = buildBillTextMessage(
        trip = trip,
        currency = currency,
        startTimeStr = startTimeStr,
        endTimeStr = endTimeStr,
        durationMin = durationMin,
        durationSec = durationSec,
        waitingMin = waitingMin,
        waitingSec = waitingSec
    )

    try {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val encodedText = Uri.encode(text)
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Receipt Bill"))
    }
}

private fun shareReceiptNative(
    context: Context,
    trip: TripEntity,
    currency: String,
    startTimeStr: String,
    endTimeStr: String,
    durationMin: Long,
    durationSec: Long,
    waitingMin: Long,
    waitingSec: Long
) {
    val text = buildBillTextMessage(
        trip = trip,
        currency = currency,
        startTimeStr = startTimeStr,
        endTimeStr = endTimeStr,
        durationMin = durationMin,
        durationSec = durationSec,
        waitingMin = waitingMin,
        waitingSec = waitingSec
    )

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Taxi Invoice Receipt")
    shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(shareIntent)
}
