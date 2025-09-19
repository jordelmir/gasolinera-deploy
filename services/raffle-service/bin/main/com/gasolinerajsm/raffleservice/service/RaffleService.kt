package com.gasolinerajsm.raffleservice.service

import com.gasolinerajsm.raffleservice.model.*
import com.gasolinerajsm.raffleservice.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Service for managing raffles, draws, and winner selection
 */
@Service
@Transactional
class RaffleService(
    private val raffleRepository: RaffleRepository,
    private val raffleTicketRepository: RaffleTicketRepository,
    private val rafflePrizeRepository: RafflePrizeRepository,
    private val raffleWinnerRepository: RaffleWinnerRepository,
    private val prizeDistributionService: PrizeDistributionService
) {
    private val logger = LoggerFactory.getLogger(RaffleService::class.java)

    /**
     * Create a new raffle
     */
    fun createRaffle(raffle: Raffle): Raffle {
        logger.info("Creating new raffle: ${raffle.name}")

        // Validate raffle data
        validateRaffleData(raffle)

        // Check for name conflicts
        if (raffleRepository.existsByNameIgnoreCase(raffle.name)) {
            throw IllegalArgumentException("Raffle with name '${raffle.name}' already exists")
        }

        val savedRaffle = raffleRepository.save(raffle)
        logger.info("Created raffle with ID: ${savedRaffle.id}")

        return savedRaffle
    }

    /**
     * Update an existing raffle
     */
    fun updateRaffle(id: Long, updatedRaffle: Raffle): Raffle {
        logger.info("Updating raffle with ID: $id")

        val existingRaffle = getRaffleById(id)

        // Check if raffle can be modified
        if (!existingRaffle.allowsModifications()) {
            throw IllegalStateException("Raffle in status '${existingRaffle.status}' cannot be modified")
        }

        // Validate updated data
        validateRaffleData(updatedRaffle)

        // Check for name conflicts (excluding current raffle)
        if (updatedRaffle.name != existingRaffle.name &&
            raffleRepository.existsByNameIgnoreCase(updatedRaffle.name)) {
            throw IllegalArgumentException("Raffle with name '${updatedRaffle.name}' already exists")
        }

        val raffle = updatedRaffle.copy(
            id = id,
            createdAt = existingRaffle.createdAt,
            updatedAt = LocalDateTime.now()
        )

        return raffleRepository.save(raffle)
    }

    /**
     * Get raffle by ID
     */
    @Transactional(readOnly = true)
    fun getRaffleById(id: Long): Raffle {
        return raffleRepository.findById(id)
            .orElseThrow { NoSuchElementException("Raffle not found with ID: $id") }
    }

    /**
     * Get all raffles with pagination
     */
    @Transactional(readOnly = true)
    fun getAllRaffles(pageable: Pageable): Page<Raffle> {
        return raffleRepository.findAll(pageable)
    }

    /**
     * Get raffles by status
     */
    @Transactional(readOnly = true)
    fun getRafflesByStatus(status: RaffleStatus, pageable: Pageable): Page<Raffle> {
        return raffleRepository.findByStatus(status, pageable)
    }

    /**
     * Get active public raffles
     */
    @Transactional(readOnly = true)
    fun getActivePublicRaffles(pageable: Pageable): Page<Raffle> {
        return raffleRepository.findByStatusAndIsPublicTrue(RaffleStatus.ACTIVE, pageable)
    }

    /**
     * Get raffles with open registration
     */
    @Transactional(readOnly = true)
    fun getRafflesWithOpenRegistration(pageable: Pageable): Page<Raffle> {
        return raffleRepository.findRafflesWithOpenRegistration(
            status = RaffleStatus.ACTIVE,
            isPublic = true,
            pageable = pageable
        )
    }

    /**
     * Activate a raffle
     */
    fun activateRaffle(id: Long, activatedBy: String? = null): Raffle {
        logger.info("Activating raffle with ID: $id")

        val raffle = getRaffleById(id)

        if (raffle.status != RaffleStatus.DRAFT) {
            throw IllegalStateException("Only draft raffles can be activated")
        }

        // Validate raffle has prizes
        val prizes = rafflePrizeRepository.findByRaffleIdAndStatus(id, PrizeStatus.ACTIVE)
        if (prizes.isEmpty()) {
            throw IllegalStateException("Raffle must have at least one active prize to be activated")
        }

        val activatedRaffle = raffle.activate()
        return raffleRepository.save(activatedRaffle)
    }

    /**
     * Pause a raffle
     */
    fun pauseRaffle(id: Long, pausedBy: String? = null): Raffle {
        logger.info("Pausing raffle with ID: $id")

        val raffle = getRaffleById(id)

        if (raffle.status != RaffleStatus.ACTIVE) {
            throw IllegalStateException("Only active raffles can be paused")
        }

        val pausedRaffle = raffle.pause()
        return raffleRepository.save(pausedRaffle)
    }

    /**
     * Cancel a raffle
     */
    fun cancelRaffle(id: Long, cancelledBy: String? = null): Raffle {
        logger.info("Cancelling raffle with ID: $id")

        val raffle = getRaffleById(id)

        if (raffle.isFinalState()) {
            throw IllegalStateException("Raffle in final state cannot be cancelled")
        }

        val cancelledRaffle = raffle.cancel()
        return raffleRepository.save(cancelledRaffle)
    }

    /**
     * Execute raffle draw
     */
    fun executeRaffleDraw(raffleId: Long, executedBy: String? = null): List<RaffleWinner> {
        logger.info("Executing draw for raffle ID: $raffleId")

        val raffle = getRaffleById(raffleId)

        // Validate raffle is eligible for draw
        if (!raffle.isEligibleForDraw()) {
            throw IllegalStateException("Raffle is not eligible for draw")
        }

        // Get eligible tickets
        val eligibleTickets = raffleTicketRepository.findEligibleTicketsForDraw(raffleId)
        if (eligibleTickets.isEmpty()) {
            throw IllegalStateException("No eligible tickets found for draw")
        }

        logger.info("Found ${eligibleTickets.size} eligible tickets for draw")

        // Get available prizes
        val availablePrizes = rafflePrizeRepository.findAvailablePrizesByRaffle(raffleId)
        if (availablePrizes.isEmpty()) {
            throw IllegalStateException("No available prizes found for draw")
        }

        logger.info("Found ${availablePrizes.size} available prizes for draw")

        // Execute prize distribution
        val winners = prizeDistributionService.distributePrizes(raffle, eligibleTickets, availablePrizes)

        // Save winners
        val savedWinners = raffleWinnerRepository.saveAll(winners)

        // Mark winning tickets
        winners.forEach { winner ->
            raffleTicketRepository.markAsWinner(winner.ticket.id)
        }

        // Update prize awarded quantities
        winners.groupBy { it.prize.id }.forEach { (prizeId, prizeWinners) ->
            rafflePrizeRepository.findById(prizeId).ifPresent { prize ->
                val updatedPrize = prize.copy(quantityAwarded = prize.quantityAwarded + prizeWinners.size)
                rafflePrizeRepository.save(updatedPrize)
            }
        }

        // Complete the raffle
        val completedRaffle = raffle.complete()
        raffleRepository.save(completedRaffle)

        logger.info("Draw completed for raffle ID: $raffleId, ${savedWinners.size} winners selected")

        return savedWinners
    }

    /**
     * Get raffle statistics
     */
    @Transactional(readOnly = true)
    fun getRaffleStatistics(raffleId: Long): Map<String, Any> {
        val raffle = getRaffleById(raffleId)
        val ticketStats = raffleTicketRepository.getRaffleTicketStatistics(raffleId)
        val prizeStats = rafflePrizeRepository.getPrizeStatisticsByRaffle(raffleId)
        val winnerStats = raffleWinnerRepository.getWinnerStatisticsByRaffle(raffleId)

        return mapOf(
            "raffle" to mapOf(
                "id" to raffle.id,
                "name" to raffle.name,
                "status" to raffle.status,
                "type" to raffle.raffleType,
                "currentParticipants" to raffle.currentParticipants,
                "maxParticipants" to raffle.maxParticipants,
                "registrationStart" to raffle.registrationStart,
                "registrationEnd" to raffle.registrationEnd,
                "drawDate" to raffle.drawDate,
                "isRegistrationOpen" to raffle.isRegistrationOpen(),
                "participationRate" to raffle.getParticipationRate()
            ),
            "tickets" to ticketStats,
            "prizes" to prizeStats,
            "winners" to winnerStats
        )
    }

    /**
     * Search raffles by name
     */
    @Transactional(readOnly = true)
    fun searchRafflesByName(name: String, pageable: Pageable): Page<Raffle> {
        return raffleRepository.findByNameContainingIgnoreCase(name, pageable)
    }

    /**
     * Get raffles ready for draw
     */
    @Transactional(readOnly = true)
    fun getRafflesReadyForDraw(): List<Raffle> {
        return raffleRepository.findRafflesReadyForDraw()
    }

    /**
     * Get raffles expiring soon
     */
    @Transactional(readOnly = true)
    fun getRafflesExpiringSoon(hours: Long = 24): List<Raffle> {
        val deadline = LocalDateTime.now().plusHours(hours)
        return raffleRepository.findRafflesExpiringSoon(deadline = deadline)
    }

    /**
     * Update participant count for raffle
     */
    fun updateParticipantCount(raffleId: Long): Raffle {
        val raffle = getRaffleById(raffleId)
        val participantCount = raffleTicketRepository.countByRaffleIdAndStatus(raffleId, TicketStatus.ACTIVE)

        val updatedRaffle = raffle.updateParticipantCount(participantCount.toInt())
        return raffleRepository.save(updatedRaffle)
    }

    /**
     * Delete raffle (only if in draft status and no tickets)
     */
    fun deleteRaffle(id: Long) {
        logger.info("Deleting raffle with ID: $id")

        val raffle = getRaffleById(id)

        if (raffle.status != RaffleStatus.DRAFT) {
            throw IllegalStateException("Only draft raffles can be deleted")
        }

        val ticketCount = raffleTicketRepository.countByRaffleId(id)
        if (ticketCount > 0) {
            throw IllegalStateException("Cannot delete raffle with existing tickets")
        }

        raffleRepository.deleteById(id)
        logger.info("Deleted raffle with ID: $id")
    }

    /**
     * Validate raffle data
     */
    private fun validateRaffleData(raffle: Raffle) {
        // Validate dates
        if (raffle.registrationStart.isAfter(raffle.registrationEnd)) {
            throw IllegalArgumentException("Registration start date must be before end date")
        }

        if (raffle.registrationEnd.isAfter(raffle.drawDate)) {
            throw IllegalArgumentException("Registration end date must be before draw date")
        }

        // Validate participant limits
        if (raffle.maxParticipants != null && raffle.maxParticipants <= 0) {
            throw IllegalArgumentException("Max participants must be positive")
        }

        if (raffle.minTicketsToParticipate <= 0) {
            throw IllegalArgumentException("Minimum tickets to participate must be positive")
        }

        if (raffle.maxTicketsPerUser != null && raffle.maxTicketsPerUser <= 0) {
            throw IllegalArgumentException("Max tickets per user must be positive")
        }

        if (raffle.maxTicketsPerUser != null && raffle.maxTicketsPerUser < raffle.minTicketsToParticipate) {
            throw IllegalArgumentException("Max tickets per user cannot be less than minimum tickets to participate")
        }
    }
}