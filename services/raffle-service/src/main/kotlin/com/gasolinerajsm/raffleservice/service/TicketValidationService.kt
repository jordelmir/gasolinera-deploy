package com.gasolinerajsm.raffleservice.service

import com.gasolinerajsm.raffleservice.model.*
import com.gasolinerajsm.raffleservice.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for validating raffle tickets and entry conditions
 */
@Service
@Transactional(readOnly = true)
class TicketValidationService(
    private val raffleRepository: RaffleRepository,
    private val raffleTicketRepository: RaffleTicketRepository
) {
    private val logger = LoggerFactory.getLogger(TicketValidationService::class.java)

    /**
     * Validate if user can enter a raffle with specified parameters
     */
    fun validateRaffleEntry(
        userId: Long,
        raffleId: Long,
        ticketCount: Int,
        sourceType: TicketSourceType,
        sourceReference: String? = null
    ): ValidationResult {
        logger.debug("Validating raffle entry for user $userId, raffle $raffleId, $ticketCount tickets")

        try {
            val raffle = raffleRepository.findById(raffleId)
                .orElseThrow { NoSuchElementException("Raffle not found with ID: $raffleId") }

            // Validate raffle status and timing
            validateRaffleStatus(raffle)

            // Validate ticket count
            validateTicketCount(ticketCount)

            // Validate user eligibility
            validateUserEligibility(userId, raffle, ticketCount)

            // Validate source-specific rules
            validateSourceSpecificRules(userId, raffle, sourceType, sourceReference)

            // Validate raffle capacity
            validateRaffleCapacity(raffle)

            return ValidationResult.success()

        } catch (e: Exception) {
            logger.debug("Validation failed for user $userId, raffle $raffleId: ${e.message}")
            return ValidationResult.failure(e.message ?: "Validation failed")
        }
    }

    /**
     * Validate ticket for draw eligibility
     */
    fun validateTicketForDraw(ticket: RaffleTicket): ValidationResult {
        logger.debug("Validating ticket ${ticket.id} for draw eligibility")

        try {
            // Check ticket status
            if (!ticket.status.allowsParticipation()) {
                return ValidationResult.failure("Ticket status '${ticket.status}' does not allow participation")
            }

            // Check verification requirements
            if (ticket.raffle.requiresVerification && !ticket.isVerified) {
                return ValidationResult.failure("Ticket requires verification")
            }

            // Check if raffle is eligible for draw
            if (!ticket.raffle.isEligibleForDraw()) {
                return ValidationResult.failure("Raffle is not eligible for draw")
            }

            // Check ticket age (if there are any age restrictions)
            validateTicketAge(ticket)

            return ValidationResult.success()

        } catch (e: Exception) {
            logger.debug("Draw validation failed for ticket ${ticket.id}: ${e.message}")
            return ValidationResult.failure(e.message ?: "Draw validation failed")
        }
    }

    /**
     * Validate ticket verification code
     */
    fun validateVerificationCode(verificationCode: String): ValidationResult {
        logger.debug("Validating verification code: $verificationCode")

        try {
            if (verificationCode.isBlank()) {
                return ValidationResult.failure("Verification code cannot be empty")
            }

            if (verificationCode.length != 6) {
                return ValidationResult.failure("Verification code must be 6 characters")
            }

            val ticket = raffleTicketRepository.findByVerificationCode(verificationCode)
                ?: return ValidationResult.failure("Invalid verification code")

            if (ticket.isVerified) {
                return ValidationResult.failure("Ticket is already verified")
            }

            if (!ticket.canBeVerified()) {
                return ValidationResult.failure("Ticket cannot be verified")
            }

            return ValidationResult.success()

        } catch (e: Exception) {
            logger.debug("Verification code validation failed: ${e.message}")
            return ValidationResult.failure(e.message ?: "Verification validation failed")
        }
    }

    /**
     * Validate duplicate entry prevention
     */
    fun validateNoDuplicateEntry(
        userId: Long,
        raffleId: Long,
        sourceType: TicketSourceType,
        sourceReference: String?
    ): ValidationResult {
        logger.debug("Validating no duplicate entry for user $userId, raffle $raffleId")

        try {
            val existingTickets = raffleTicketRepository.findDuplicateTickets(
                userId = userId,
                raffleId = raffleId,
                sourceType = sourceType,
                sourceReference = sourceReference
            )

            if (existingTickets.isNotEmpty()) {
                val sourceDesc = when (sourceType) {
                    TicketSourceType.COUPON_REDEMPTION -> "coupon $sourceReference"
                    TicketSourceType.DIRECT_PURCHASE -> "transaction $sourceReference"
                    else -> sourceType.displayName.lowercase()
                }
                return ValidationResult.failure("User has already entered this raffle with $sourceDesc")
            }

            return ValidationResult.success()

        } catch (e: Exception) {
            logger.debug("Duplicate entry validation failed: ${e.message}")
            return ValidationResult.failure(e.message ?: "Duplicate entry validation failed")
        }
    }

    /**
     * Validate raffle status and timing
     */
    private fun validateRaffleStatus(raffle: Raffle) {
        if (!raffle.status.allowsRegistration()) {
            throw IllegalStateException("Raffle status '${raffle.status}' does not allow registration")
        }

        if (!raffle.isRegistrationOpen()) {
            val now = LocalDateTime.now()
            when {
                now.isBefore(raffle.registrationStart) ->
                    throw IllegalStateException("Raffle registration has not started yet")
                now.isAfter(raffle.registrationEnd) ->
                    throw IllegalStateException("Raffle registration has ended")
                else ->
                    throw IllegalStateException("Raffle registration is not open")
            }
        }
    }

    /**
     * Validate ticket count
     */
    private fun validateTicketCount(ticketCount: Int) {
        if (ticketCount <= 0) {
            throw IllegalArgumentException("Ticket count must be positive")
        }

        if (ticketCount > 100) { // Reasonable upper limit
            throw IllegalArgumentException("Ticket count cannot exceed 100 per transaction")
        }
    }

    /**
     * Validate user eligibility
     */
    private fun validateUserEligibility(userId: Long, raffle: Raffle, additionalTickets: Int) {
        val currentUserTickets = raffleTicketRepository.countByUserIdAndRaffleIdAndStatus(
            userId, raffle.id, TicketStatus.ACTIVE
        )

        val totalUserTickets = currentUserTickets + additionalTickets

        // Check minimum tickets requirement
        if (totalUserTickets < raffle.minTicketsToParticipate) {
            throw IllegalStateException(
                "User needs at least ${raffle.minTicketsToParticipate} tickets to participate"
            )
        }

        // Check maximum tickets per user limit
        raffle.maxTicketsPerUser?.let { maxTickets ->
            if (totalUserTickets > maxTickets) {
                throw IllegalStateException(
                    "User cannot have more than $maxTickets tickets in this raffle"
                )
            }
        }
    }

    /**
     * Validate source-specific rules
     */
    private fun validateSourceSpecificRules(
        userId: Long,
        raffle: Raffle,
        sourceType: TicketSourceType,
        sourceReference: String?
    ) {
        when (sourceType) {
            TicketSourceType.COUPON_REDEMPTION -> {
                if (sourceReference.isNullOrBlank()) {
                    throw IllegalArgumentException("Coupon ID is required for coupon redemption")
                }
                // Additional coupon validation would be done via external service
            }

            TicketSourceType.DIRECT_PURCHASE -> {
                if (raffle.entryFee == null) {
                    throw IllegalStateException("This raffle does not allow direct purchase")
                }
            }

            TicketSourceType.PROMOTIONAL -> {
                // Promotional tickets might have campaign-specific rules
                // These would be validated via external service
            }

            else -> {
                // Other source types might have their own validation rules
            }
        }
    }

    /**
     * Validate raffle capacity
     */
    private fun validateRaffleCapacity(raffle: Raffle) {
        if (!raffle.canAcceptMoreParticipants()) {
            throw IllegalStateException("Raffle has reached maximum participant capacity")
        }
    }

    /**
     * Validate ticket age for draw eligibility
     */
    private fun validateTicketAge(ticket: RaffleTicket) {
        // Some raffles might require tickets to be created before a certain time
        // For example, tickets must be created at least 1 hour before draw
        val minAgeHours = 1L
        val ticketAgeHours = ticket.getAgeInHours()

        if (ticketAgeHours < minAgeHours) {
            throw IllegalStateException("Ticket must be at least $minAgeHours hour(s) old for draw eligibility")
        }
    }

    /**
     * Validate generated tickets for event processing
     */
    fun validateGeneratedTickets(ticketIds: List<Long>, userId: Long): ValidationResult {
        logger.debug("Validating generated tickets: $ticketIds for user $userId")

        try {
            if (ticketIds.isEmpty()) {
                return ValidationResult.failure("No tickets provided for validation")
            }

            // Validate that all tickets exist and belong to the user
            val tickets = raffleTicketRepository.findAllById(ticketIds)

            if (tickets.size != ticketIds.size) {
                return ValidationResult.failure("Some tickets not found")
            }

            // Validate ownership
            val invalidTickets = tickets.filter { it.userId != userId }
            if (invalidTickets.isNotEmpty()) {
                return ValidationResult.failure("User does not own all specified tickets")
            }

            // Validate ticket status
            val inactiveTickets = tickets.filter { !it.status.allowsParticipation() }
            if (inactiveTickets.isNotEmpty()) {
                return ValidationResult.failure("Some tickets are not in valid status for processing")
            }

            return ValidationResult.success()

        } catch (e: Exception) {
            logger.debug("Generated tickets validation failed: ${e.message}")
            return ValidationResult.failure(e.message ?: "Generated tickets validation failed")
        }
    }

    /**
     * Get validation summary for user and raffle
     */
    fun getValidationSummary(userId: Long, raffleId: Long): ValidationSummary {
        try {
            val raffle = raffleRepository.findById(raffleId)
                .orElseThrow { NoSuchElementException("Raffle not found") }

            val userTicketCount = raffleTicketRepository.countByUserIdAndRaffleIdAndStatus(
                userId, raffleId, TicketStatus.ACTIVE
            )

            return ValidationSummary(
                canEnter = raffle.isRegistrationOpen() && raffle.canAcceptMoreParticipants(),
                registrationOpen = raffle.isRegistrationOpen(),
                hasCapacity = raffle.canAcceptMoreParticipants(),
                userTicketCount = userTicketCount.toInt(),
                minTicketsRequired = raffle.minTicketsToParticipate,
                maxTicketsAllowed = raffle.maxTicketsPerUser,
                remainingSlots = raffle.getRemainingParticipantSlots(),
                registrationEndsAt = raffle.registrationEnd,
                drawDate = raffle.drawDate
            )

        } catch (e: Exception) {
            logger.error("Failed to get validation summary for user $userId, raffle $raffleId", e)
            return ValidationSummary(
                canEnter = false,
                registrationOpen = false,
                hasCapacity = false,
                userTicketCount = 0,
                minTicketsRequired = 1,
                maxTicketsAllowed = null,
                remainingSlots = null,
                registrationEndsAt = LocalDateTime.now(),
                drawDate = LocalDateTime.now()
            )
        }
    }
}

/**
 * Validation result data class
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(message: String) = ValidationResult(false, message)
    }
}

/**
 * Validation summary data class
 */
data class ValidationSummary(
    val canEnter: Boolean,
    val registrationOpen: Boolean,
    val hasCapacity: Boolean,
    val userTicketCount: Int,
    val minTicketsRequired: Int,
    val maxTicketsAllowed: Int?,
    val remainingSlots: Int?,
    val registrationEndsAt: LocalDateTime,
    val drawDate: LocalDateTime
)