package com.gasolinerajsm.raffleservice.service

import com.gasolinerajsm.raffleservice.model.*
import com.gasolinerajsm.raffleservice.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing raffle entries and ticket consumption
 */
@Service
@Transactional
class RaffleEntryService(
    private val raffleRepository: RaffleRepository,
    private val raffleTicketRepository: RaffleTicketRepository,
    private val ticketValidationService: TicketValidationService
) {
    private val logger = LoggerFactory.getLogger(RaffleEntryService::class.java)

    /**
     * Enter a raffle using tickets from coupon redemption
     */
    fun enterRaffleWithCoupon(
        userId: Long,
        raffleId: Long,
        couponId: Long,
        ticketCount: Int,
        stationId: Long? = null,
        transactionReference: String? = null
    ): List<RaffleTicket> {
        logger.info("User $userId entering raffle $raffleId with coupon $couponId for $ticketCount tickets")

        val raffle = getRaffleAndValidateEntry(raffleId, userId, ticketCount)

        // Validate coupon-specific entry
        validateCouponEntry(userId, raffleId, couponId, ticketCount)

        // Create tickets from coupon redemption
        val tickets = createTicketsFromCoupon(
            userId = userId,
            raffle = raffle,
            couponId = couponId,
            ticketCount = ticketCount,
            stationId = stationId,
            transactionReference = transactionReference
        )

        // Save tickets and update raffle participant count
        val savedTickets = raffleTicketRepository.saveAll(tickets)
        updateRaffleParticipantCount(raffleId)

        logger.info("Successfully created ${savedTickets.size} tickets for user $userId in raffle $raffleId")
        return savedTickets
    }

    /**
     * Enter a raffle by purchasing tickets directly
     */
    fun enterRaffleWithPurchase(
        userId: Long,
        raffleId: Long,
        ticketCount: Int,
        purchaseAmount: BigDecimal,
        stationId: Long? = null,
        transactionReference: String? = null
    ): List<RaffleTicket> {
        logger.info("User $userId entering raffle $raffleId with direct purchase for $ticketCount tickets")

        val raffle = getRaffleAndValidateEntry(raffleId, userId, ticketCount)

        // Validate purchase entry
        validatePurchaseEntry(raffle, ticketCount, purchaseAmount)

        // Create tickets from direct purchase
        val tickets = createTicketsFromPurchase(
            userId = userId,
            raffle = raffle,
            ticketCount = ticketCount,
            purchaseAmount = purchaseAmount,
            stationId = stationId,
            transactionReference = transactionReference
        )

        // Save tickets and update raffle participant count
        val savedTickets = raffleTicketRepository.saveAll(tickets)
        updateRaffleParticipantCount(raffleId)

        logger.info("Successfully created ${savedTickets.size} tickets for user $userId in raffle $raffleId")
        return savedTickets
    }

    /**
     * Enter a raffle with promotional tickets
     */
    fun enterRaffleWithPromotionalTickets(
        userId: Long,
        raffleId: Long,
        ticketCount: Int,
        campaignId: Long? = null,
        sourceReference: String? = null,
        issuedBy: String? = null
    ): List<RaffleTicket> {
        logger.info("User $userId entering raffle $raffleId with $ticketCount promotional tickets")

        val raffle = getRaffleAndValidateEntry(raffleId, userId, ticketCount)

        // Create promotional tickets
        val tickets = createPromotionalTickets(
            userId = userId,
            raffle = raffle,
            ticketCount = ticketCount,
            campaignId = campaignId,
            sourceReference = sourceReference
        )

        // Save tickets and update raffle participant count
        val savedTickets = raffleTicketRepository.saveAll(tickets)
        updateRaffleParticipantCount(raffleId)

        logger.info("Successfully created ${savedTickets.size} promotional tickets for user $userId in raffle $raffleId")
        return savedTickets
    }

    /**
     * Get user's tickets for a specific raffle
     */
    @Transactional(readOnly = true)
    fun getUserRaffleTickets(userId: Long, raffleId: Long, pageable: Pageable): Page<RaffleTicket> {
        return raffleTicketRepository.findByUserIdAndRaffleId(userId, raffleId, pageable)
    }

    /**
     * Get user's ticket count for a raffle
     */
    @Transactional(readOnly = true)
    fun getUserTicketCount(userId: Long, raffleId: Long): Long {
        return raffleTicketRepository.countByUserIdAndRaffleId(userId, raffleId)
    }

    /**
     * Get user's active ticket count for a raffle
     */
    @Transactional(readOnly = true)
    fun getUserActiveTicketCount(userId: Long, raffleId: Long): Long {
        return raffleTicketRepository.countByUserIdAndRaffleIdAndStatus(userId, raffleId, TicketStatus.ACTIVE)
    }

    /**
     * Verify a ticket using verification code
     */
    fun verifyTicket(verificationCode: String, verifiedBy: String? = null): RaffleTicket {
        logger.info("Verifying ticket with code: $verificationCode")

        val ticket = raffleTicketRepository.findByVerificationCode(verificationCode)
            ?: throw NoSuchElementException("Ticket not found with verification code: $verificationCode")

        if (ticket.isVerified) {
            throw IllegalStateException("Ticket is already verified")
        }

        if (!ticket.canBeVerified()) {
            throw IllegalStateException("Ticket cannot be verified")
        }

        val verifiedTicket = ticket.verify(verifiedBy)
        return raffleTicketRepository.save(verifiedTicket)
    }

    /**
     * Cancel a ticket (if allowed)
     */
    fun cancelTicket(ticketId: Long, userId: Long, reason: String? = null): RaffleTicket {
        logger.info("Cancelling ticket $ticketId for user $userId")

        val ticket = raffleTicketRepository.findById(ticketId)
            .orElseThrow { NoSuchElementException("Ticket not found with ID: $ticketId") }

        // Verify ownership
        if (ticket.userId != userId) {
            throw IllegalArgumentException("User $userId does not own ticket $ticketId")
        }

        // Check if ticket can be cancelled
        if (ticket.status.isFinalState()) {
            throw IllegalStateException("Ticket in final state cannot be cancelled")
        }

        // Check if raffle allows cancellation
        if (ticket.raffle.isDrawCompleted()) {
            throw IllegalStateException("Cannot cancel ticket after draw completion")
        }

        val cancelledTicket = ticket.cancel().copy(
            notes = reason?.let { "${ticket.notes ?: ""}\nCancelled: $it".trim() } ?: ticket.notes
        )

        val savedTicket = raffleTicketRepository.save(cancelledTicket)
        updateRaffleParticipantCount(ticket.raffle.id!!)

        return savedTicket
    }

    /**
     * Get ticket by ID with ownership validation
     */
    @Transactional(readOnly = true)
    fun getTicketById(ticketId: Long, userId: Long): RaffleTicket {
        val ticket = raffleTicketRepository.findById(ticketId)
            .orElseThrow { NoSuchElementException("Ticket not found with ID: $ticketId") }

        if (ticket.userId != userId) {
            throw IllegalArgumentException("User $userId does not own ticket $ticketId")
        }

        return ticket
    }

    /**
     * Get user's ticket statistics
     */
    @Transactional(readOnly = true)
    fun getUserTicketStatistics(userId: Long): Map<String, Any> {
        return raffleTicketRepository.getUserTicketStatistics(userId)
    }

    /**
     * Check if user can enter raffle
     */
    @Transactional(readOnly = true)
    fun canUserEnterRaffle(userId: Long, raffleId: Long, additionalTickets: Int = 0): Boolean {
        return try {
            val raffle = raffleRepository.findById(raffleId)
                .orElseThrow { NoSuchElementException("Raffle not found") }

            val currentTicketCount = getUserActiveTicketCount(userId, raffleId)
            val totalTickets = currentTicketCount + additionalTickets

            raffle.isRegistrationOpen() &&
            raffle.canAcceptMoreParticipants() &&
            raffle.canUserParticipate(totalTickets.toInt()) &&
            totalTickets >= raffle.minTicketsToParticipate
        } catch (e: Exception) {
            logger.debug("User $userId cannot enter raffle $raffleId: ${e.message}")
            false
        }
    }

    /**
     * Get raffle and validate entry conditions
     */
    private fun getRaffleAndValidateEntry(raffleId: Long, userId: Long, ticketCount: Int): Raffle {
        val raffle = raffleRepository.findById(raffleId)
            .orElseThrow { NoSuchElementException("Raffle not found with ID: $raffleId") }

        // Validate raffle status and timing
        if (!raffle.isRegistrationOpen()) {
            throw IllegalStateException("Raffle registration is not open")
        }

        // Validate participant capacity
        if (!raffle.canAcceptMoreParticipants()) {
            throw IllegalStateException("Raffle has reached maximum participants")
        }

        // Validate ticket count
        if (ticketCount <= 0) {
            throw IllegalArgumentException("Ticket count must be positive")
        }

        // Validate user ticket limits
        val currentUserTickets = getUserActiveTicketCount(userId, raffleId)
        val totalUserTickets = currentUserTickets + ticketCount

        if (!raffle.canUserParticipate(totalUserTickets.toInt())) {
            throw IllegalStateException("User ticket count would exceed raffle limits")
        }

        return raffle
    }

    /**
     * Validate coupon-based entry
     */
    private fun validateCouponEntry(userId: Long, raffleId: Long, couponId: Long, ticketCount: Int) {
        // Check for duplicate entries with same coupon
        val existingTickets = raffleTicketRepository.findDuplicateTickets(
            userId = userId,
            raffleId = raffleId,
            sourceType = TicketSourceType.COUPON_REDEMPTION,
            sourceReference = couponId.toString()
        )

        if (existingTickets.isNotEmpty()) {
            throw IllegalStateException("User has already entered this raffle with coupon $couponId")
        }

        // Additional coupon validation would be done via external service call
        // For now, we assume the coupon is valid
    }

    /**
     * Validate purchase-based entry
     */
    private fun validatePurchaseEntry(raffle: Raffle, ticketCount: Int, purchaseAmount: BigDecimal) {
        // Validate entry fee if required
        raffle.entryFee?.let { entryFee ->
            val expectedAmount = entryFee.multiply(BigDecimal(ticketCount))
            if (purchaseAmount < expectedAmount) {
                throw IllegalArgumentException("Purchase amount insufficient for $ticketCount tickets")
            }
        }

        if (purchaseAmount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Purchase amount must be positive")
        }
    }

    /**
     * Create tickets from coupon redemption
     */
    private fun createTicketsFromCoupon(
        userId: Long,
        raffle: Raffle,
        couponId: Long,
        ticketCount: Int,
        stationId: Long?,
        transactionReference: String?
    ): List<RaffleTicket> {
        return (1..ticketCount).map { index ->
            RaffleTicket(
                userId = userId,
                raffle = raffle,
                ticketNumber = generateTicketNumber(raffle.id!!, userId),
                status = TicketStatus.ACTIVE,
                sourceType = TicketSourceType.COUPON_REDEMPTION,
                sourceReference = couponId.toString(),
                couponId = couponId,
                stationId = stationId,
                transactionReference = transactionReference,
                verificationCode = if (raffle.requiresVerification) generateVerificationCode() else null,
                isVerified = !raffle.requiresVerification
            )
        }
    }

    /**
     * Create tickets from direct purchase
     */
    private fun createTicketsFromPurchase(
        userId: Long,
        raffle: Raffle,
        ticketCount: Int,
        purchaseAmount: BigDecimal,
        stationId: Long?,
        transactionReference: String?
    ): List<RaffleTicket> {
        val amountPerTicket = purchaseAmount.divide(BigDecimal(ticketCount))

        return (1..ticketCount).map { index ->
            RaffleTicket(
                userId = userId,
                raffle = raffle,
                ticketNumber = generateTicketNumber(raffle.id!!, userId),
                status = TicketStatus.ACTIVE,
                sourceType = TicketSourceType.DIRECT_PURCHASE,
                sourceReference = transactionReference,
                stationId = stationId,
                transactionReference = transactionReference,
                purchaseAmount = amountPerTicket,
                verificationCode = if (raffle.requiresVerification) generateVerificationCode() else null,
                isVerified = !raffle.requiresVerification
            )
        }
    }

    /**
     * Create promotional tickets
     */
    private fun createPromotionalTickets(
        userId: Long,
        raffle: Raffle,
        ticketCount: Int,
        campaignId: Long?,
        sourceReference: String?
    ): List<RaffleTicket> {
        return (1..ticketCount).map { index ->
            RaffleTicket(
                userId = userId,
                raffle = raffle,
                ticketNumber = generateTicketNumber(raffle.id!!, userId),
                status = TicketStatus.ACTIVE,
                sourceType = TicketSourceType.PROMOTIONAL,
                sourceReference = sourceReference,
                campaignId = campaignId,
                isVerified = true // Promotional tickets are auto-verified
            )
        }
    }

    /**
     * Generate unique ticket number
     */
    private fun generateTicketNumber(raffleId: Long, userId: Long): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "R${raffleId}U${userId}T${timestamp}${random}"
    }

    /**
     * Generate verification code
     */
    private fun generateVerificationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Update user ticket balance (for event processing)
     */
    fun updateUserTicketBalance(userId: Long, additionalTickets: Int) {
        logger.info("Updating ticket balance for user $userId with $additionalTickets additional tickets")

        // This method is used by event handlers to track user ticket balances
        // In a real implementation, this might update a user statistics table
        // For now, we'll just log the update
        logger.info("User $userId ticket balance updated with $additionalTickets tickets")
    }

    /**
     * Process ad ticket multiplication
     */
    fun processAdTicketMultiplication(
        userId: Long,
        advertisementId: Long,
        engagementId: Long,
        completionPercentage: Int
    ) {
        logger.info(
            "Processing ad ticket multiplication for user $userId, ad $advertisementId, engagement $engagementId"
        )

        try {
            // Calculate multiplier based on completion percentage
            val multiplier = when {
                completionPercentage >= 95 -> 3 // 3x multiplier for 95%+ completion
                completionPercentage >= 90 -> 2 // 2x multiplier for 90%+ completion
                completionPercentage >= 80 -> 1 // 1x bonus ticket for 80%+ completion
                else -> 0 // No bonus for less than 80%
            }

            if (multiplier > 0) {
                // Get user's current active tickets across all raffles
                val currentActiveTickets = raffleTicketRepository.countByUserIdAndStatus(userId, TicketStatus.ACTIVE)

                // Calculate bonus tickets (multiplier applied to current active tickets)
                val bonusTickets = (currentActiveTickets * multiplier).toInt()

                if (bonusTickets > 0) {
                    logger.info(
                        "Awarding $bonusTickets bonus tickets to user $userId (${multiplier}x multiplier on $currentActiveTickets active tickets)"
                    )

                    // Update user statistics
                    updateUserAdEngagementStats(userId, advertisementId, bonusTickets, multiplier)
                }
            }

        } catch (exception: Exception) {
            logger.error("Error processing ad ticket multiplication for user $userId", exception)
            throw exception
        }
    }

    /**
     * Update user ad engagement statistics
     */
    fun updateUserAdEngagementStats(
        userId: Long,
        advertisementId: Long,
        bonusTickets: Int,
        multiplier: Int
    ) {
        logger.info(
            "Updating ad engagement stats for user $userId: ad $advertisementId, $bonusTickets bonus tickets, ${multiplier}x multiplier"
        )

        // In a real implementation, this would update user engagement statistics
        // For now, we'll just log the statistics update
        logger.info(
            "Ad engagement stats updated for user $userId: " +
            "advertisement=$advertisementId, bonusTickets=$bonusTickets, multiplier=$multiplier"
        )
    }

    /**
     * Update raffle participant count
     */
    private fun updateRaffleParticipantCount(raffleId: Long) {
        val activeTicketCount = raffleTicketRepository.countByRaffleIdAndStatus(raffleId, TicketStatus.ACTIVE)
        raffleRepository.updateParticipantCount(raffleId, activeTicketCount.toInt())
    }
}