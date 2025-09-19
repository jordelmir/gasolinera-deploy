package com.gasolinerajsm.adengine.domain.valueobject

import com.gasolinerajsm.adengine.domain.model.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Value object representing advertisement scheduling configuration
 */
data class AdSchedule(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime? = null,
    val timeSlots: List<TimeSlot> = emptyList(),
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val timezone: String = "America/Costa_Rica",
    val isRecurring: Boolean = true,
    val maxDailyImpressions: Int? = null,
    val maxHourlyImpressions: Int? = null,
    val frequencyCap: FrequencyCap? = null
) {

    init {
        require(startDate.isBefore(endDate ?: LocalDateTime.MAX)) {
            "Start date must be before end date"
        }
        endDate?.let {
            require(startDate.isBefore(it)) { "Start date must be before end date" }
        }
        maxDailyImpressions?.let {
            require(it > 0) { "Max daily impressions must be positive" }
        }
        maxHourlyImpressions?.let {
            require(it > 0) { "Max hourly impressions must be positive" }
        }
    }

    /**
     * Check if the schedule is currently active
     */
    fun isActiveAt(dateTime: LocalDateTime = LocalDateTime.now()): Boolean {
        // Check date range
        if (dateTime.isBefore(startDate)) return false
        endDate?.let { if (dateTime.isAfter(it)) return false }

        // Check day of week
        if (daysOfWeek.isNotEmpty()) {
            val dayOfWeek = DayOfWeek.valueOf(dateTime.dayOfWeek.name)
            if (dayOfWeek !in daysOfWeek) return false
        }

        // Check time slots
        if (timeSlots.isNotEmpty()) {
            val currentTime = dateTime.toLocalTime()
            val inTimeSlot = timeSlots.any { it.contains(currentTime) }
            if (!inTimeSlot) return false
        }

        return true
    }

    /**
     * Check if the schedule is expired
     */
    fun isExpired(): Boolean {
        val now = LocalDateTime.now()
        return endDate?.isBefore(now) ?: false
    }

    /**
     * Get the remaining duration of the schedule
     */
    fun getRemainingDuration(): java.time.Duration? {
        val now = LocalDateTime.now()
        return endDate?.let {
            if (now.isBefore(it)) java.time.Duration.between(now, it) else null
        }
    }

    /**
     * Check if impression limits are reached
     */
    fun canServeImpression(currentDailyCount: Int, currentHourlyCount: Int): Boolean {
        maxDailyImpressions?.let {
            if (currentDailyCount >= it) return false
        }
        maxHourlyImpressions?.let {
            if (currentHourlyCount >= it) return false
        }
        return true
    }

    /**
     * Validate schedule configuration
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (startDate.isAfter(endDate ?: LocalDateTime.MAX)) {
            errors.add("Start date cannot be after end date")
        }

        if (timeSlots.isNotEmpty()) {
            timeSlots.forEachIndexed { index, slot ->
                if (slot.startTime.isAfter(slot.endTime)) {
                    errors.add("Time slot ${index + 1}: start time cannot be after end time")
                }
            }
        }

        frequencyCap?.let {
            if (it.maxImpressionsPerHour <= 0) {
                errors.add("Frequency cap max impressions per hour must be positive")
            }
            if (it.timeWindowHours <= 0) {
                errors.add("Frequency cap time window must be positive")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Schedule is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Get next available time slot
     */
    fun getNextAvailableSlot(from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!isRecurring && isExpired()) return null

        var current = from
        val maxLookAheadDays = 7 // Look ahead up to 7 days

        for (day in 0..maxLookAheadDays) {
            val checkDate = current.plusDays(day.toLong())

            // Check if day of week matches
            if (daysOfWeek.isNotEmpty()) {
                val dayOfWeek = DayOfWeek.valueOf(checkDate.dayOfWeek.name)
                if (dayOfWeek !in daysOfWeek) continue
            }

            // Check time slots
            if (timeSlots.isNotEmpty()) {
                for (slot in timeSlots) {
                    val slotStart = checkDate.toLocalDate().atTime(slot.startTime)
                    if (slotStart.isAfter(current)) {
                        return slotStart
                    }
                }
            } else {
                // No time slots specified, return start of day
                val dayStart = checkDate.toLocalDate().atStartOfDay()
                if (dayStart.isAfter(current)) {
                    return dayStart
                }
            }
        }

        return null
    }

    companion object {
        fun create(
            startDate: LocalDateTime,
            endDate: LocalDateTime? = null,
            daysOfWeek: Set<DayOfWeek> = emptySet(),
            timeSlots: List<TimeSlot> = emptyList()
        ): AdSchedule {
            return AdSchedule(
                startDate = startDate,
                endDate = endDate,
                daysOfWeek = daysOfWeek,
                timeSlots = timeSlots
            )
        }

        fun immediate(): AdSchedule {
            return AdSchedule(
                startDate = LocalDateTime.now(),
                isRecurring = false
            )
        }
    }
}

/**
 * Time slot for scheduling
 */
data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    init {
        require(startTime.isBefore(endTime)) { "Start time must be before end time" }
    }

    fun contains(time: LocalTime): Boolean {
        return !time.isBefore(startTime) && time.isBefore(endTime)
    }

    fun getDuration(): java.time.Duration {
        return java.time.Duration.between(startTime, endTime)
    }
}

/**
 * Frequency capping configuration
 */
data class FrequencyCap(
    val maxImpressionsPerHour: Int,
    val timeWindowHours: Int = 24
) {
    init {
        require(maxImpressionsPerHour > 0) { "Max impressions per hour must be positive" }
        require(timeWindowHours > 0) { "Time window must be positive" }
    }

    fun canShowToUser(userProfile: UserProfile): Boolean {
        // This would check user's impression history against frequency cap
        // For now, return true as a simplified implementation
        return true
    }
}