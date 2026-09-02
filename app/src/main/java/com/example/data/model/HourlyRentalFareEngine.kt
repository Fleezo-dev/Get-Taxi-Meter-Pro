package com.example.data.model

import java.util.Locale
import kotlin.math.floor
import kotlin.math.max

data class HourlyRentalFareResult(
    val billedHours: Double,
    val baseRatePerHr: Double = 350.00,
    val baseFare: Double,
    val allowedKm: Double,
    val extraKm: Double,
    val extraDistanceCharge: Double,
    val grandTotal: Double
)

object HourlyRentalFareEngine {
    const val BASE_HOURLY_RATE = 350.00
    const val INCLUDED_KM_PER_HOUR = 10.0
    const val EXTRA_KM_RATE = 25.00

    fun calculateHourlyRentalFare(
        durationInSeconds: Long,
        distanceInKm: Double,
        extraTolls: Double = 0.0,
        overrideRatePerHour: Double = 0.0,
        overrideExtraKmRate: Double = 0.0
    ): HourlyRentalFareResult {
        val activeConfig = com.example.data.repository.RemoteConfigRepository.getActiveConfig()
        val ratePerHour = if (overrideRatePerHour > 0.0) overrideRatePerHour else activeConfig.baseHourlyRate
        val extraKmRate = if (overrideExtraKmRate > 0.0) overrideExtraKmRate else activeConfig.extraKmRate
        val includedKmPerHr = activeConfig.includedKmPerHour

        val minutes = durationInSeconds / 60.0
        val billedHours = if (minutes <= 71.0) {
            // Up to 1 hr 11 mins -> 1.0 hour
            1.0
        } else {
            val hoursPart = floor(minutes / 60.0)
            val remainingMins = minutes % 60.0

            if (remainingMins <= 11.0) {
                hoursPart
            } else if (remainingMins <= 40.0) {
                // 12 mins to 40 mins -> rounds to 0.5 hours
                hoursPart + 0.5
            } else {
                // > 40 mins -> rounds to next full hour
                hoursPart + 1.0
            }
        }

        val baseFare = billedHours * ratePerHour
        val allowedKm = billedHours * includedKmPerHr
        val extraKm = max(0.0, distanceInKm - allowedKm)
        val extraDistanceCharge = extraKm * extraKmRate
        val grandTotal = baseFare + extraDistanceCharge + extraTolls

        return HourlyRentalFareResult(
            billedHours = billedHours,
            baseRatePerHr = ratePerHour,
            baseFare = baseFare,
            allowedKm = allowedKm,
            extraKm = extraKm,
            extraDistanceCharge = extraDistanceCharge,
            grandTotal = grandTotal
        )
    }
}
