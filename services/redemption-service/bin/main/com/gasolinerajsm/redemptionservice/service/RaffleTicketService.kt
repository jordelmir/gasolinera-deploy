package com.gasolinerajsm.redemptionservice.service

import com.gasolinerajsm.redemptionservice.model.*
import com.gasolinerajsm.redemptionservice.repository.RaffleTicketRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Service for managing raffle tickets and user balances
 */
@Service
@Transactional
class RaffleTicketService(
    private val raffleTicketRepository: RaffleTicketRepository,
    private val eventPublishingService: EventPublishingService
) {

    private val logger = LoggerFactory.getLogger(RaffleTicketService::class.java)
    private val secureRandom = SecureRandom()

    /**
     * Generate raffle tickets for a redemption
     */
    fun generateTicketsForRedemption(
        redemption: Redemption,
        ticketCount: Int,
        multiplierValue: Int = 1,
        campaignId: Long? = null
    ): List<RaffleTicket> {
        logger.info("Generating $ticketCount raffle tickets for redemption ${redemption.id}")

        val batchId = generateBatchId()
        val tickets = mutableListOf<RaffleTicket>()

        repeat(ticketCount) {
            val ticket = RaffleTicket(
                userId = redemption.userId,
                redemption = redemption,
                ticketNumber = generateTicketNumber(),
                status = TicketStatus.ACTIVE,
                sourceType = "REDEMPTION",
                sourceReference = redemption.transactionReference,
                campaignId = redemption.campaignId,
                stationId = redemption.stationId,
                expiryDate = calculateExpirationDate(),
                metadata = buildTicketMetadata(redemption, multiplierValue)
            )
            tickets.add(ticket)
        }

        val savedTickets = raffleTicketRepository.saveAll(tickets)

        logger.info("Generated ${savedTickets.size} raffle tickets for user ${redemption.userId}")

        // Publish ticket generation event
        eventPublishingService.publishRaffleTicketsGeneratedEvent(redemption, savedTickets)

        return savedTickets
    }

    /**
     * Generate tickets for ad engagement
     */
    fun generateTicketsForAdEngagement(
        userId: Long,
        adEngagementId: Long,
        ticketCount: Int,
        multiplierValue: Int = 1,
        campaignId: Long? = null
    ): List<RaffleTicket> {
        logger.info("Generating $ticketCount raffle tickets for ad engagement $adEngagementId")

        val batchId = generateBatchId()
        val tickets = mutableListOf<RaffleTicket>()

        repeat(ticketCount) {
            // TODO: Fix this - need to handle ad engagement tickets differently
            // For now, skip ad engagement tickets
            // tickets.add(ticket)
        }

        val savedTickets = raffleTicketRepository.saveAll(tickets)

        logger.info("Generated ${savedTickets.size} raffle tickets for user $userId from ad engagement")

        return savedTickets
    }

    /**
     * Get user ticket balance (active tickets)
     */
    @Transactional(readOnly = true)
    fun getUserTicketBalance(userId: Long): Long {
        return raffleTicketRepository.getUserTicketBalance(userId)
    }

    /**
     * Get user tickets with pagination
     */
    @Transactional(readOnly = true)
    fun getUserTickets(userId: Long, pageable: Pageable): Page<RaffleTicket> {
        return raffleTicketRepository.findByUserIdOrderByGeneratedAtDesc(userId, pageable)
    }

    /**
     * Get user active tickets
     */
    @Transactional(readOnly = true)
    fun getUserActiveTickets(userId: Long, pageable: Pageable): Page<RaffleTicket> {
        return raffleTicketRepository.findByUserIdAndStatusOrderByGeneratedAtDesc(
            userId, TicketStatus.ACTIVE, pageable
        )
    }

    /**
     * Get user ticket history
     */
    @Transactional(readOnly = true)
    fun getUserTicketHistory(userId: Long, pageable: Pageable): Page<RaffleTicket> {
        return raffleTicketRepository.findByUserIdOrderByGeneratedAtDesc(userId, pageable)
    }

    /**
     * Get user ticket statistics
     */
    @Transactional(readOnly = true)
    fun getUserTicketStatistics(userId: Long): UserTicketStatistics {
        val stats = raffleTicketRepository.getUserTicketStatistics(userId)

        return UserTicketStatistics(
            userId = userId,
            totalGenerated = stats["totalCount"] ?: 0L,
            activeCount = stats["activeCount"] ?: 0L,
            consumedCount = stats["consumedCount"] ?: 0L,
            wonCount = stats["wonCount"] ?: 0L,
            expiredCount = stats["expiredCount"] ?: 0L
        )
    }

    /**
     * Consume tickets for raffle entry
     */
    fun consumeTicketsForRaffle(userId: Long, raffleId: Long, ticketCount: Int): TicketConsumptionResult {
        logger.info("Consuming $ticketCount tickets for user $userId in raffle $raffleId")

        // Get user's active tickets
        val activeTickets = raffleTicketRepository.findByUserIdAndStatusOrderByGeneratedAtDesc(
            userId, TicketStatus.ACTIVE
        )

        if (activeTickets.size < ticketCount) {
            return TicketConsumptionResult.failure(
                "Insufficient tickets. User has ${activeTickets.size} active tickets, but $ticketCount required"
            )
        }

        // Select tickets to consume (oldest first for fairness)
        val ticketsToConsume = activeTickets.sortedBy { it.createdAt }.take(ticketCount)

        // Update ticket status
        val consumedTickets = ticketsToConsume.map { ticket ->
            ticket.use(raffleId)
        }

        val savedTickets = raffleTicketRepository.saveAll(consumedTickets)

        logger.info("Consumed ${savedTickets.size} tickets for user $userId in raffle $raffleId")

        return TicketConsumptionResult.success(
            consumedTickets = savedTickets,
            message = "Successfully consumed $ticketCount tickets"
        )
    }

    /**
     * Mark tickets as won
     */
    fun markTicketsAsWon(ticketIds: List<Long>, prizeId: Long): List<RaffleTicket> {
        logger.info("Marking ${ticketIds.size} tickets as won for prize $prizeId")

        val tickets = raffleTicketRepository.findAllById(ticketIds)

        val wonTickets = tickets.map { ticket ->
            ticket.markAsWinner("Prize #$prizeId", null)
        }

        val savedTickets = raffleTicketRepository.saveAll(wonTickets)

        logger.info("Marked ${savedTickets.size} tickets as won")

        return savedTickets
    }

    /**
     * Process expired tickets
     */
    fun processExpiredTickets(): Int {
        logger.info("Processing expired tickets")

        val expiredTickets = raffleTicketRepository.findExpiredTickets(LocalDateTime.now())
        var processedCount = 0

        expiredTickets.forEach { ticket ->
            try {
                val expiredTicket = ticket.copy(
                    status = TicketStatus.EXPIRED,
                    updatedAt = LocalDateTime.now()
                )
                raffleTicketRepository.save(expiredTicket)
                processedCount++
            } catch (exception: Exception) {
                logger.error("Error processing expired ticket ${ticket.id}", exception)
            }
        }

        logger.info("Processed $processedCount expired tickets")
        return processedCount
    }

    /**
     * Get tickets expiring soon
     */
    @Transactional(readOnly = true)
    fun getTicketsExpiringSoon(hours: Long = 24): List<RaffleTicket> {
        val currentTime = LocalDateTime.now()
        val futureTime = currentTime.plusHours(hours)

        return raffleTicketRepository.findTicketsExpiringSoon(currentTime, futureTime)
    }

    /**
     * Get ticket by ticket number
     */
    @Transactional(readOnly = true)
    fun getTicketByNumber(ticketNumber: String): RaffleTicket? {
        return raffleTicketRepository.findByTicketNumber(ticketNumber)
    }

    /**
     * Get tickets by redemption
     */
    @Transactional(readOnly = true)
    fun getTicketsByRedemption(redemptionId: Long): List<RaffleTicket> {
        return raffleTicketRepository.findByRedemptionIdOrderByGeneratedAtDesc(redemptionId)
    }

    /**
     * Get tickets by batch ID
     */
    @Transactional(readOnly = true)
    fun getTicketsByBatch(batchId: String): List<RaffleTicket> {
        return raffleTicketRepository.findByBatchIdOrderByGeneratedAtDesc(batchId)
    }

    /**
     * Get ticket statistics for date range
     */
    @Transactional(readOnly = true)
    fun getTicketStatistics(startDate: LocalDateTime, endDate: LocalDateTime): TicketStatistics {
        val stats = raffleTicketRepository.getTicketStatistics(startDate, endDate)

        return TicketStatistics(
            totalGenerated = stats["totalCount"] ?: 0L,
            activeCount = stats["activeCount"] ?: 0L,
            consumedCount = stats["consumedCount"] ?: 0L,
            wonCount = stats["wonCount"] ?: 0L,
            expiredCount = stats["expiredCount"] ?: 0L,
            periodStart = startDate,
            periodEnd = endDate
        )
    }

    /**
     * Validate ticket for raffle entry
     */
    fun validateTicketForRaffle(ticketNumber: String, userId: Long): TicketValidationResult {
        val ticket = raffleTicketRepository.findByTicketNumber(ticketNumber)
            ?: return TicketValidationResult.failure("Ticket not found")

        if (ticket.userId != userId) {
            return TicketValidationResult.failure("Ticket does not belong to user")
        }

        if (ticket.status != TicketStatus.ACTIVE) {
            return TicketValidationResult.failure("Ticket is not active (status: ${ticket.status})")
        }

        if (ticket.isExpired()) {
            return TicketValidationResult.failure("Ticket has expired")
        }

        return TicketValidationResult.success(ticket, "Ticket is valid")
    }

    private fun generateTicketNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = secureRandom.nextInt(99999).toString().padStart(5, '0')
        return "TKT-$timestamp-$random"
    }

    private fun generateBatchId(): String {
        val dateString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val random = secureRandom.nextInt(9999).toString().padStart(4, '0')
        return "BATCH-$dateString-$random"
    }

    private fun calculateExpirationDate(): LocalDateTime {
        // Tickets expire after 90 days by default
        return LocalDateTime.now().plusDays(90)
    }

    private fun buildTicketMetadata(redemption: Redemption, multiplierValue: Int): String {
        return """
            {
                "source": "redemption",
                "redemptionId": ${redemption.id},
                "transactionReference": "${redemption.transactionReference}",
                "stationId": ${redemption.stationId},
                "multiplier": $multiplierValue,
                "purchaseAmount": ${redemption.purchaseAmount},
                "finalAmount": ${redemption.finalAmount}
            }
        """.trimIndent()
    }

    private fun buildAdEngagementMetadata(adEngagementId: Long, multiplierValue: Int): String {
        return """
            {
                "source": "ad_engagement",
                "adEngagementId": $adEngagementId,
                "multiplier": $multiplierValue
            }
        """.trimIndent()
    }
}

/**
 * User ticket statistics
 */
data class UserTicketStatistics(
    val userId: Long,
    val totalGenerated: Long,
    val activeCount: Long,
    val consumedCount: Long,
    val wonCount: Long,
    val expiredCount: Long
) {
    fun getWinRate(): Double {
        return if (consumedCount > 0) {
            wonCount.toDouble() / consumedCount.toDouble() * 100
        } else {
            0.0
        }
    }

    fun getExpirationRate(): Double {
        return if (totalGenerated > 0) {
            expiredCount.toDouble() / totalGenerated.toDouble() * 100
        } else {
            0.0
        }
    }
}

/**
 * Ticket statistics
 */
data class TicketStatistics(
    val totalGenerated: Long,
    val activeCount: Long,
    val consumedCount: Long,
    val wonCount: Long,
    val expiredCount: Long,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime
) {
    fun getConsumptionRate(): Double {
        return if (totalGenerated > 0) {
            consumedCount.toDouble() / totalGenerated.toDouble() * 100
        } else {
            0.0
        }
    }

    fun getWinRate(): Double {
        return if (consumedCount > 0) {
            wonCount.toDouble() / consumedCount.toDouble() * 100
        } else {
            0.0
        }
    }
}

/**
 * Ticket consumption result
 */
data class TicketConsumptionResult(
    val success: Boolean,
    val message: String,
    val consumedTickets: List<RaffleTicket> = emptyList(),
    val errorCode: String? = null
) {
    companion object {
        fun success(consumedTickets: List<RaffleTicket>, message: String): TicketConsumptionResult {
            return TicketConsumptionResult(
                success = true,
                message = message,
                consumedTickets = consumedTickets
            )
        }

        fun failure(message: String, errorCode: String? = null): TicketConsumptionResult {
            return TicketConsumptionResult(
                success = false,
                message = message,
                errorCode = errorCode
            )
        }
    }
}

/**
 * Ticket validation result
 */
data class TicketValidationResult(
    val isValid: Boolean,
    val message: String,
    val ticket: RaffleTicket? = null,
    val errorCode: String? = null
) {
    companion object {
        fun success(ticket: RaffleTicket, message: String): TicketValidationResult {
            return TicketValidationResult(
                isValid = true,
                message = message,
                ticket = ticket
            )
        }

        fun failure(message: String, errorCode: String? = null): TicketValidationResult {
            return TicketValidationResult(
                isValid = false,
                message = message,
                errorCode = errorCode
            )
        }
    }
}