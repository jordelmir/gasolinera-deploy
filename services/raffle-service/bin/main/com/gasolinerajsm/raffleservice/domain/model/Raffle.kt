package com.gasolinerajsm.raffleservice.domain.model

import com.gasolinerajsm.raffleservice.domain.event.DomainEvent
import com.gasolinerajsm.raffleservice.domain.event.RaffleCreatedEvent
import com.gasolinerajsm.raffleservice.domain.event.RaffleActivatedEvent
import com.gasolinerajsm.raffleservice.domain.event.RaffleDrawCompletedEvent
import com.gasolinerajsm.raffleservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Raffle Domain Entity - Core business logic
 * Represents a raffle with complete lifecycle management and draw algorithms
 */
data class Raffle(
    val id: RaffleId,
    val name: String,
    val description: String? = null,
    val raffleType: RaffleType = RaffleType.WEEKLY,
    val status: RaffleStatus = RaffleStatus.DRAFT,
    val schedule: RaffleSchedule,
    val participationRules: ParticipationRules,
    val prizePool: PrizePool,
    val drawConfiguration: DrawConfiguration,
    val eligibilityCriteria: EligibilityCriteria,
    val statistics: RaffleStatistics = RaffleStatistics.initial(),
    val metadata: RaffleMetadata,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new raffle
         */
        fun create(
            name: String,
            description: String?,
            raffleType: RaffleType,
            schedule: RaffleSchedule,
            participationRules: ParticipationRules,
            prizePool: PrizePool,
            drawConfiguration: DrawConfiguration,
            eligibilityCriteria: EligibilityCriteria,
            metadata: RaffleMetadata
        ): Raffle {
            val raffle = Raffle(
                id = RaffleId.generate(),
                name = name,
                description = description,
                raffleType = raffleType,
                schedule = schedule,
                participationRules = participationRules,
                prizePool = prizePool,
                drawConfiguration = drawConfiguration,
                eligibilityCriteria = eligibilityCriteria,
                metadata = metadata
            )

            raffle.addDomainEvent(
                RaffleCreatedEvent(
                    aggregateId = raffle.id.toString(),
                    raffleId = raffle.id.toString(),
                    name = raffle.name,
                    raffleType = raffle.raffleType.toString(),
                    schedule = raffle.schedule,
                    prizePoolValue = raffle.prizePool.totalValue,
                    maxParticipants = raffle.participationRules.maxParticipants,
                    occurredAt = LocalDateTime.now()
                )
            )

            return raffle
        }
    }

    /**
     * Check if registration is currently open
     */
    fun isRegistrationOpen(): Boolean {
        val now = LocalDateTime.now()
        return status == RaffleStatus.ACTIVE &&
                schedule.isRegistrationOpen(now) &&
                statistics.canAcceptMoreParticipants(participationRules.maxParticipants)
    }

    /**
     * Check if raffle is eligible for draw
     */
    fun isEligibleForDraw(): Boolean {
        val now = LocalDateTime.now()
        return status == RaffleStatus.ACTIVE &&
                schedule.isRegistrationClosed(now) &&
                statistics.currentParticipants > 0 &&
                now.isAfter(schedule.drawDate.minusMinutes(5)) // Allow 5 min buffer
    }

    /**
     * Check if user can participate
     */
    fun canUserParticipate(
        userId: UserId,
        userTicketCount: Int,
        userProfile: UserProfile
    ): ValidationResult {
        if (!isRegistrationOpen()) {
            return ValidationResult.failure("Registration is not open")
        }

        if (!eligibilityCriteria.isUserEligible(userProfile)) {
            return ValidationResult.failure("User does not meet eligibility criteria")
        }

        if (userTicketCount < participationRules.minTicketsToParticipate) {
            return ValidationResult.failure("Insufficient tickets to participate")
        }

        if (userTicketCount > participationRules.maxTicketsPerUser) {
            return ValidationResult.failure("Exceeds maximum tickets per user")
        }

        return ValidationResult.success("User can participate")
    }

    /**
     * Activate the raffle
     */
    fun activate(activatedBy: String? = null): Raffle {
        if (status != RaffleStatus.DRAFT) {
            throw IllegalStateException("Can only activate draft raffles")
        }

        val validationResult = validateForActivation()
        if (!validationResult.isSuccess) {
            throw IllegalStateException(validationResult.message)
        }

        val activatedRaffle = this.copy(
            status = RaffleStatus.ACTIVE,
            metadata = metadata.copy(updatedBy = activatedBy),
            updatedAt = LocalDateTime.now()
        )

        activatedRaffle.addDomainEvent(
            RaffleActivatedEvent(
                aggregateId = id.toString(),
                raffleId = id.toString(),
                name = name,
                registrationStart = schedule.registrationStart,
                registrationEnd = schedule.registrationEnd,
                drawDate = schedule.drawDate,
                activatedBy = activatedBy,
                occurredAt = LocalDateTime.now()
            )
        )

        return activatedRaffle
    }

    /**
     * Pause the raffle
     */
    fun pause(pausedBy: String? = null): Raffle {
        if (status != RaffleStatus.ACTIVE) {
            throw IllegalStateException("Can only pause active raffles")
        }

        return this.copy(
            status = RaffleStatus.PAUSED,
            metadata = metadata.copy(updatedBy = pausedBy),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Cancel the raffle
     */
    fun cancel(reason: String? = null, cancelledBy: String? = null): Raffle {
        if (status.isFinalState()) {
            throw IllegalStateException("Cannot cancel raffle in final state")
        }

        return this.copy(
            status = RaffleStatus.CANCELLED,
            metadata = metadata.copy(
                updatedBy = cancelledBy,
                notes = reason?.let { "${metadata.notes ?: ""}\nCancelled: $it".trim() } ?: metadata.notes
            ),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Complete the draw
     */
    fun completeDraw(
        drawResults: DrawResults,
        completedBy: String? = null
    ): Raffle {
        if (!isEligibleForDraw()) {
            throw IllegalStateException("Raffle is not eligible for draw")
        }

        val completedRaffle = this.copy(
            status = RaffleStatus.COMPLETED,
            statistics = statistics.recordDrawCompletion(drawResults),
            metadata = metadata.copy(updatedBy = completedBy),
            updatedAt = LocalDateTime.now()
        )

        completedRaffle.addDomainEvent(
            RaffleDrawCompletedEvent(
                aggregateId = id.toString(),
                raffleId = id.toString(),
                name = name,
                drawResults = drawResults,
                totalParticipants = statistics.currentParticipants,
                totalTicketsUsed = statistics.totalTicketsUsed,
                completedBy = completedBy,
                occurredAt = LocalDateTime.now()
            )
        )

        return completedRaffle
    }

    /**
     * Add participant to raffle
     */
    fun addParticipant(
        userId: UserId,
        ticketCount: Int,
        entryDetails: EntryDetails
    ): Raffle {
        val validationResult = canUserParticipate(userId, ticketCount, entryDetails.userProfile)
        if (!validationResult.isSuccess) {
            throw IllegalStateException(validationResult.message)
        }

        return this.copy(
            statistics = statistics.addParticipant(ticketCount),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Remove participant from raffle
     */
    fun removeParticipant(userId: UserId, ticketCount: Int): Raffle {
        return this.copy(
            statistics = statistics.removeParticipant(ticketCount),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update prize pool
     */
    fun updatePrizePool(newPrizePool: PrizePool): Raffle {
        if (status != RaffleStatus.DRAFT) {
            throw IllegalStateException("Can only update prize pool for draft raffles")
        }

        return this.copy(
            prizePool = newPrizePool,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update participation rules
     */
    fun updateParticipationRules(newRules: ParticipationRules): Raffle {
        if (status != RaffleStatus.DRAFT) {
            throw IllegalStateException("Can only update rules for draft raffles")
        }

        return this.copy(
            participationRules = newRules,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Get remaining participant slots
     */
    fun getRemainingParticipantSlots(): Int {
        return participationRules.maxParticipants - statistics.currentParticipants
    }

    /**
     * Get participation rate
     */
    fun getParticipationRate(): Double {
        return if (participationRules.maxParticipants > 0) {
            statistics.currentParticipants.toDouble() / participationRules.maxParticipants
        } else 0.0
    }

    /**
     * Check if raffle is oversubscribed
     */
    fun isOversubscribed(): Boolean {
        return statistics.currentParticipants > participationRules.maxParticipants
    }

    /**
     * Get time until registration closes
     */
    fun getTimeUntilRegistrationCloses(): java.time.Duration? {
        val now = LocalDateTime.now()
        return if (schedule.registrationEnd.isAfter(now)) {
            java.time.Duration.between(now, schedule.registrationEnd)
        } else null
    }

    /**
     * Get time until draw
     */
    fun getTimeUntilDraw(): java.time.Duration? {
        val now = LocalDateTime.now()
        return if (schedule.drawDate.isAfter(now)) {
            java.time.Duration.between(now, schedule.drawDate)
        } else null
    }

    /**
     * Validate raffle for activation
     */
    private fun validateForActivation(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate schedule
        val scheduleValidation = schedule.validate()
        if (!scheduleValidation.isSuccess) {
            errors.add(scheduleValidation.message)
        }

        // Validate prize pool
        if (prizePool.prizes.isEmpty()) {
            errors.add("Raffle must have at least one prize")
        }

        // Validate participation rules
        if (participationRules.maxParticipants <= 0) {
            errors.add("Max participants must be positive")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Raffle is valid for activation")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Update metadata
     */
    fun updateMetadata(newMetadata: Map<String, String>): Raffle {
        return this.copy(
            metadata = metadata.copy(additionalData = metadata.additionalData + newMetadata),
            updatedAt = LocalDateTime.now()
        )
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "Raffle(id=$id, name='$name', type=$raffleType, status=$status, participants=${statistics.currentParticipants})"
    }
}

/**
 * Validation result for domain operations
 */
data class ValidationResult(
    val isSuccess: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = ValidationResult(true, message)
        fun failure(message: String) = ValidationResult(false, message)
    }
}