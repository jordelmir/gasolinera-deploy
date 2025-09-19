package com.gasolinerajsm.stationservice.model

import jakarta.persistence.Embeddable
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Value object representing geographic coordinates
 */
@Embeddable
data class Location(
    val latitude: BigDecimal,
    val longitude: BigDecimal
) {

    init {
        require(latitude >= BigDecimal("-90") && latitude <= BigDecimal("90")) {
            "Latitude must be between -90 and 90 degrees"
        }
        require(longitude >= BigDecimal("-180") && longitude <= BigDecimal("180")) {
            "Longitude must be between -180 and 180 degrees"
        }
    }

    /**
     * Calculate distance to another location using Haversine formula (approximate)
     */
    fun distanceTo(other: Location): Double {
        val lat1 = Math.toRadians(latitude.toDouble())
        val lon1 = Math.toRadians(longitude.toDouble())
        val lat2 = Math.toRadians(other.latitude.toDouble())
        val lon2 = Math.toRadians(other.longitude.toDouble())

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        // Earth's radius in kilometers
        return 6371 * c
    }

    /**
     * Check if this location is within a certain radius of another location
     */
    fun isWithinRadius(other: Location, radiusKm: Double): Boolean {
        return distanceTo(other) <= radiusKm
    }

    override fun toString(): String {
        return "Location(latitude=${latitude.setScale(6, RoundingMode.HALF_UP)}, longitude=${longitude.setScale(6, RoundingMode.HALF_UP)})"
    }

    companion object {
        fun of(latitude: BigDecimal, longitude: BigDecimal): Location {
            return Location(latitude, longitude)
        }

        fun of(latitude: Double, longitude: Double): Location {
            return Location(
                BigDecimal.valueOf(latitude).setScale(8, RoundingMode.HALF_UP),
                BigDecimal.valueOf(longitude).setScale(8, RoundingMode.HALF_UP)
            )
        }
    }
}