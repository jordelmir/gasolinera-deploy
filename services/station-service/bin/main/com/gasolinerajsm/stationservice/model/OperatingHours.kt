package com.gasolinerajsm.stationservice.model

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Value object representing operating hours for a station
 */
@Embeddable
data class OperatingHours(
    @Enumerated(EnumType.STRING)
    val dayOfWeek: DayOfWeek,

    val openTime: LocalTime,

    val closeTime: LocalTime,

    val isOpen24Hours: Boolean = false
) {

    init {
        if (!isOpen24Hours) {
            require(openTime.isBefore(closeTime)) {
                "Open time must be before close time for $dayOfWeek"
            }
        }
    }

    /**
     * Check if the station is currently open
     */
    fun isCurrentlyOpen(): Boolean {
        if (isOpen24Hours) return true

        val now = LocalTime.now()
        return now.isAfter(openTime) && now.isBefore(closeTime)
    }

    /**
     * Check if the station is open at a specific time
     */
    fun isOpenAt(time: LocalTime): Boolean {
        if (isOpen24Hours) return true
        return time.isAfter(openTime) && time.isBefore(closeTime)
    }

    /**
     * Get the duration the station is open in hours
     */
    fun getOperatingDurationHours(): Double {
        if (isOpen24Hours) return 24.0

        val duration = java.time.Duration.between(openTime, closeTime)
        return duration.toMinutes() / 60.0
    }

    override fun toString(): String {
        return if (isOpen24Hours) {
            "$dayOfWeek: 24 hours"
        } else {
            "$dayOfWeek: $openTime - $closeTime"
        }
    }

    companion object {
        fun create24Hours(dayOfWeek: DayOfWeek): OperatingHours {
            return OperatingHours(
                dayOfWeek = dayOfWeek,
                openTime = LocalTime.MIDNIGHT,
                closeTime = LocalTime.MIDNIGHT,
                isOpen24Hours = true
            )
        }

        fun create(dayOfWeek: DayOfWeek, openTime: LocalTime, closeTime: LocalTime): OperatingHours {
            return OperatingHours(
                dayOfWeek = dayOfWeek,
                openTime = openTime,
                closeTime = closeTime,
                isOpen24Hours = false
            )
        }
    }
}