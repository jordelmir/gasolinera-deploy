package com.gasolinerajsm.raffleservice.domain.valueobject

import com.gasolinerajsm.raffleservice.domain.model.ValidationResult
import java.time.LocalDateTime

/**
 * Value object representing the schedule configuration for a raffle
 */
data class RaffleSchedule(
    val registrationStart: LocalDateTime,
    val registrationEnd: LocalDateTime,
    val drawDate: LocalDateTime
) {

    init {
        require(registrationStart.isBefore(registrationEnd)) {
            "Registration start must be before registration end"
        }
        require(registrationEnd.isBefore(drawDate)) {
            "Registration end must be before draw date"
        }
    }

    /**
     * Check if registration is currently open
     */
    fun isRegistrationOpen(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return now.isAfter(registrationStart) && now.isBefore(registrationEnd)
    }

    /**
     * Check if registration has ended
     */
    fun isRegistrationClosed(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return now.isAfter(registrationEnd)
    }

    /**
     * Check if draw date has passed
     */
    fun isDrawDatePassed(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return now.isAfter(drawDate)
    }

    /**
     * Validate the schedule
     */
    fun validate(): ValidationResult {
        val now = LocalDateTime.now()
        val errors = mutableListOf<String>()

        if (registrationStart.isBefore(now)) {
            errors.add("Registration start cannot be in the past")
        }

        if (registrationEnd.isBefore(registrationStart.plusMinutes(30))) {
            errors.add("Registration period must be at least 30 minutes")
        }

        if (drawDate.isBefore(registrationEnd.plusMinutes(5))) {
            errors.add("Draw date must be at least 5 minutes after registration end")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Schedule is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    companion object {
        /**
         * Create a schedule for a weekly raffle
         */
        fun weekly(startDate: LocalDateTime): RaffleSchedule {
            return RaffleSchedule(
                registrationStart = startDate,
                registrationEnd = startDate.plusDays(6).withHour(23).withMinute(59),
                drawDate = startDate.plusDays(7).withHour(12).withMinute(0)
            )
        }

        /**
         * Create a schedule for a daily raffle
         */
        fun daily(startDate: LocalDateTime): RaffleSchedule {
            return RaffleSchedule(
                registrationStart = startDate,
                registrationEnd = startDate.withHour(23).withMinute(45),
                drawDate = startDate.plusDays(1).withHour(0).withMinute(0)
            )
        }
    }
}