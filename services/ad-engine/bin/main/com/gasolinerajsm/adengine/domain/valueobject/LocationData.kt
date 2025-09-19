package com.gasolinerajsm.adengine.domain.valueobject

import java.math.BigDecimal

/**
 * Value object representing location data
 */
data class LocationData(
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val accuracyMeters: Int? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val timezone: String? = null
) {
    init {
        latitude?.let {
            require(it in BigDecimal("-90.0")..BigDecimal("90.0")) { "Latitude must be between -90 and 90" }
        }
        longitude?.let {
            require(it in BigDecimal("-180.0")..BigDecimal("180.0")) { "Longitude must be between -180 and 180" }
        }
    }

    fun hasCoordinates(): Boolean = latitude != null && longitude != null

    fun getLocationString(): String {
        return listOfNotNull(city, region, country).joinToString(", ")
    }

    fun getFormattedCoordinates(): String? {
        return if (hasCoordinates()) {
            "${latitude}, ${longitude}"
        } else null
    }
}