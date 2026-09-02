package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.database.TripEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    fun generateAndSharePdf(
        context: Context,
        trip: TripEntity,
        currency: String,
        startTimeStr: String,
        endTimeStr: String,
        durationMin: Long,
        durationSec: Long,
        waitingMin: Long,
        waitingSec: Long,
        driverName: String = "Basheer Ahamed",
        vehicleModel: String = "Sedan Taxi",
        vehiclePlate: String = "TN99AF5313",
        customerPhone: String = "9043743777"
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 width/height
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val leftMargin = 40f
        val rightMargin = 555f
        val brandRed = Color.parseColor("#E53935")
        val ltGray = Color.parseColor("#CCCCCC")

        var currentY = 40f

        // ==================== HEADER (Top Banner) ====================
        paint.color = brandRed
        canvas.drawRoundRect(leftMargin, currentY, leftMargin + 80f, currentY + 80f, 10f, 10f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("GET", leftMargin + 10f, currentY + 34f, paint)
        canvas.drawText("TAXI", leftMargin + 10f, currentY + 58f, paint)

        // Invoice Title on Right
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("TAX INVOICE", 360f, currentY + 32f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("Official Trip Receipt & Driver Verification Board", 360f, currentY + 50f, paint)

        currentY += 95f
        paint.color = ltGray
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 12f

        // ==================== COMPANY & INVOICE META ====================
        val metaStartY = currentY
        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Get Taxi Kovai", leftMargin, currentY, paint); currentY += 15f

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#424242")
        canvas.drawText("No 286, Diwan Bahadur Rd, R.S. Puram,", leftMargin, currentY, paint); currentY += 13f
        canvas.drawText("Coimbatore, Tamil Nadu 641001", leftMargin, currentY, paint); currentY += 13f
        canvas.drawText("Email: kovai@gettaxi.in", leftMargin, currentY, paint)

        // Right Meta
        var rightMetaY = metaStartY
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText("Bill No:", 360f, rightMetaY, paint)
        paint.isFakeBoldText = false
        val tripIdStr = trip.id.toString()
        val safeTripId = if (tripIdStr.length >= 6) tripIdStr.takeLast(6).uppercase() else tripIdStr.uppercase()
        canvas.drawText("GT-$safeTripId", 430f, rightMetaY, paint); rightMetaY += 15f

        paint.isFakeBoldText = true
        canvas.drawText("Date:", 360f, rightMetaY, paint)
        paint.isFakeBoldText = false
        val dateOnlyFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateStr = dateOnlyFormatter.format(Date(trip.startTime))
        canvas.drawText(dateStr, 430f, rightMetaY, paint); rightMetaY += 15f

        paint.isFakeBoldText = true
        canvas.drawText("Payment:", 360f, rightMetaY, paint)
        paint.color = Color.parseColor("#2E7D32")
        paint.isFakeBoldText = true
        canvas.drawText("PAID", 430f, rightMetaY, paint)
        paint.color = Color.BLACK

        currentY = maxOf(currentY, rightMetaY) + 12f
        paint.color = ltGray
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 15f

        // ==================== PASSENGER & TIMINGS ====================
        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.color = Color.GRAY
        canvas.drawText("PASSENGER DETAILS", leftMargin, currentY, paint)
        canvas.drawText("TRIP TIMINGS", 360f, currentY, paint); currentY += 15f

        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        val passengerName = if (trip.passengerNotes.isNotBlank()) trip.passengerNotes else if (customerPhone.isNotBlank() && customerPhone != "9043743777") "Customer ($customerPhone)" else "Valued Customer"
        canvas.drawText(passengerName, leftMargin, currentY, paint)
        paint.isFakeBoldText = false
        canvas.drawText("Start: $startTimeStr", 360f, currentY, paint); currentY += 15f

        paint.textSize = 10f
        canvas.drawText("Mobile: $customerPhone", leftMargin, currentY, paint)
        canvas.drawText("End: $endTimeStr", 360f, currentY, paint); currentY += 16f

        paint.color = ltGray
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 15f

        // ==================== ROUTE ADDRESSES ====================
        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.color = Color.GRAY
        canvas.drawText("TRIP ROUTE ADDRESSES", leftMargin, currentY, paint); currentY += 15f

        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#4CAF50")
        canvas.drawCircle(leftMargin + 4f, currentY - 3f, 3f, paint)
        paint.color = Color.BLACK
        canvas.drawText("PICKUP:", leftMargin + 14f, currentY, paint)
        paint.isFakeBoldText = false
        val pLoc = if (trip.pickupAddress.isNotBlank()) trip.pickupAddress else "GPS Location"
        canvas.drawText(pLoc, leftMargin + 70f, currentY, paint); currentY += 16f

        paint.isFakeBoldText = true
        paint.color = brandRed
        canvas.drawRect(leftMargin + 1f, currentY - 6f, leftMargin + 7f, currentY, paint)
        paint.color = Color.BLACK
        canvas.drawText("DROP:", leftMargin + 14f, currentY, paint)
        paint.isFakeBoldText = false
        val dLoc = if (trip.dropAddress.isNotBlank()) trip.dropAddress else "GPS Location"
        canvas.drawText(dLoc, leftMargin + 70f, currentY, paint); currentY += 18f

        paint.color = ltGray
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 15f

        // ==================== VEHICLE & DISTANCE ====================
        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText("Vehicle: $vehicleModel", leftMargin, currentY, paint)
        canvas.drawText("Total Distance: ${String.format(Locale.US, "%.2f", trip.distanceKm)} KM", 360f, currentY, paint); currentY += 15f

        paint.isFakeBoldText = false
        paint.textSize = 10f
        paint.color = Color.DKGRAY
        canvas.drawText("Reg No: $vehiclePlate", leftMargin, currentY, paint)
        canvas.drawText("Duration: ${durationMin}m ${durationSec}s | Waiting: ${waitingMin}m ${waitingSec}s", 360f, currentY, paint); currentY += 18f

        paint.color = ltGray
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)
        currentY += 15f

        // ==================== FARE BREAKDOWN ====================
        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText("FARE BREAKDOWN", leftMargin, currentY, paint); currentY += 15f

        paint.textSize = 10f
        paint.isFakeBoldText = false

        fun drawFareRow(label: String, amount: String) {
            canvas.drawText(label, leftMargin, currentY, paint)
            canvas.drawText(amount, rightMargin - paint.measureText(amount), currentY, paint)
            currentY += 15f
        }

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
                drawFareRow("Hourly Rental Package ($billedHrLabel @ $currency${String.format(Locale.US, "%.0f", rentalResult.baseRatePerHr)}/Hr)", "$currency${String.format(Locale.US, "%.2f", rentalResult.baseFare)}")
                drawFareRow("Extra Distance (${String.format(Locale.US, "%.2f", rentalResult.extraKm)} KM @ $currency${String.format(Locale.US, "%.0f", extraKmRate)}/KM)", "$currency${String.format(Locale.US, "%.2f", rentalResult.extraDistanceCharge)}")
            }
            "OUTSTATION" -> {
                val driverBeta = if (trip.baseFare > 0.0 || trip.driverBeta > 0.0) (trip.baseFare + trip.driverBeta) else 500.0
                val perKmRate = if (trip.farePerKm > 0.0) trip.farePerKm else 15.0
                val distFare = trip.distanceKm * perKmRate

                drawFareRow("Driver Beta / Allowance", "$currency${String.format(Locale.US, "%.2f", driverBeta)}")
                drawFareRow("Outstation Distance (${String.format(Locale.US, "%.2f", trip.distanceKm)} KM @ $currency${String.format(Locale.US, "%.1f", perKmRate)}/KM)", "$currency${String.format(Locale.US, "%.2f", distFare)}")
            }
            else -> {
                drawFareRow("Base Fare Minimum", "$currency${String.format(Locale.US, "%.2f", trip.baseFare)}")
                drawFareRow("Distance Fare (${String.format(Locale.US, "%.2f", trip.distanceKm)} KM @ $currency${trip.farePerKm}/KM)", "$currency${String.format(Locale.US, "%.2f", trip.distanceKm * trip.farePerKm)}")
                drawFareRow("Standby Waiting Charge (${waitingMin}m ${waitingSec}s @ $currency${trip.waitFarePerMin}/Min)", "$currency${String.format(Locale.US, "%.2f", (trip.waitingSeconds / 60.0) * trip.waitFarePerMin)}")
            }
        }

        if (trip.tollCharges > 0) {
            drawFareRow("Toll Charges", "$currency${String.format(Locale.US, "%.2f", trip.tollCharges)}")
        }
        if (trip.permitCharges > 0) {
            drawFareRow("Permit Charges", "$currency${String.format(Locale.US, "%.2f", trip.permitCharges)}")
        }
        if (trip.parkingCharges > 0) {
            drawFareRow("Parking Charges", "$currency${String.format(Locale.US, "%.2f", trip.parkingCharges)}")
        }
        if (trip.isOutOfCity && trip.outOfCitySurcharge > 0) {
            drawFareRow("Out of City Surcharge", "$currency${String.format(Locale.US, "%.2f", trip.outOfCitySurcharge)}")
        }

        currentY += 4f
        paint.color = ltGray
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint); currentY += 16f

        // ==================== TOTAL AMOUNT DUE ====================
        paint.textSize = 13f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText("GRAND TOTAL DUE", leftMargin, currentY, paint)

        val totalStr = "$currency${String.format(Locale.US, "%.2f", trip.totalFare)}"
        paint.textSize = 15f
        paint.color = brandRed
        canvas.drawText(totalStr, rightMargin - paint.measureText(totalStr), currentY, paint)
        currentY += 22f

        // ==================== DRIVER PROFILE BOARD ====================
        paint.color = Color.parseColor("#F8F9FA")
        canvas.drawRoundRect(leftMargin, currentY, rightMargin, currentY + 58f, 6f, 6f, paint)
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 1f
        canvas.drawRoundRect(leftMargin, currentY, rightMargin, currentY + 58f, 6f, 6f, paint)

        currentY += 12f
        paint.textSize = 9f
        paint.isFakeBoldText = true
        paint.color = brandRed
        canvas.drawText("GET TAXI KOVAI - VERIFIED PARTNER • CAPTAIN & VEHICLE INFO", leftMargin + 10f, currentY, paint)
        currentY += 15f

        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText("Name: $driverName", leftMargin + 10f, currentY, paint)
        canvas.drawText("Vehicle: $vehicleModel", leftMargin + 260f, currentY, paint); currentY += 14f

        paint.textSize = 9f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("License Plate: $vehiclePlate", leftMargin + 10f, currentY, paint)
        canvas.drawText("Secure & Verified by Get Taxi", leftMargin + 260f, currentY, paint)

        currentY += 28f

        // ==================== FOOTER ====================
        paint.textSize = 8f
        paint.color = Color.GRAY
        val footer1 = "Above fare given based on travel distance and waiting. Toll, parking, permit charges apply extra. T&C apply."
        canvas.drawText(footer1, (595f - paint.measureText(footer1)) / 2, currentY, paint); currentY += 11f
        val footer2 = "Thank you for travelling with us! This is a computer-generated invoice. No physical signature is required."
        canvas.drawText(footer2, (595f - paint.measureText(footer2)) / 2, currentY, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "GetTaxi_Invoice_${trip.id}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Here is your Get Taxi Kovai Official Invoice & Driver Verification Receipt.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Invoice"))
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }
}
