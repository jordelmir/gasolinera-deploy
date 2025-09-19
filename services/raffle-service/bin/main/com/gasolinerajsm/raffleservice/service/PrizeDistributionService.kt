package com.gasolinerajsm.raffleservice.service

import com.gasolinerajsm.raffleservice.model.RaffleType
import com.gasolinerajsm.raffleservice.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

/**
 * Service for prize distribution and winner selection algorithms
 */
@Service
class PrizeDistributionService {

    private val logger = LoggerFactory.getLogger(PrizeDistributionService::class.java)
    private val random = Random.Default

    /**
     * Distribute prizes among eligible tickets using fair random selection
     */
    fun distributePrizes(
        raffle: Raffle,
        eligibleTickets: List<RaffleTicket>,
        availablePrizes: List<RafflePrize>
    ): List<RaffleWinner> {
        logger.info("Distributing prizes for raffle ${raffle.id}: ${availablePrizes.size} prizes, ${eligibleTickets.size} eligible tickets")

        val winners = mutableListOf<RaffleWinner>()
        val remainingTickets = eligibleTickets.toMutableList()

        // Sort prizes by tier (1st prize first)
        val sortedPrizes = availablePrizes.sortedBy { it.prizeTier }

        for (prize in sortedPrizes) {
            val prizeWinners = selectWinnersForPrize(raffle, prize, remainingTickets)
            winners.addAll(prizeWinners)

            // Remove winning tickets from remaining pool to prevent duplicate wins
            // (unless raffle allows multiple wins per user)
            if (!allowsMultipleWinsPerUser(raffle)) {
                val winningUserIds = prizeWinners.map { it.userId }.toSet()
                remainingTickets.removeAll { it.userId in winningUserIds }
            } else {
                // Remove only the specific winning tickets
                val winningTicketIds = prizeWinners.map { it.ticket.id }.toSet()
                remainingTickets.removeAll { it.id in winningTicketIds }
            }

            logger.info("Selected ${prizeWinners.size} winners for prize '${prize.name}' (Tier ${prize.prizeTier})")
        }

        logger.info("Prize distribution completed: ${winners.size} total winners selected")
        return winners
    }

    /**
     * Select winners for a specific prize
     */
    private fun selectWinnersForPrize(
        raffle: Raffle,
        prize: RafflePrize,
        eligibleTickets: List<RaffleTicket>
    ): List<RaffleWinner> {
        if (eligibleTickets.isEmpty()) {
            logger.warn("No eligible tickets available for prize '${prize.name}'")
            return emptyList()
        }

        val winnersToSelect = minOf(prize.getRemainingQuantity(), eligibleTickets.size)
        if (winnersToSelect <= 0) {
            logger.warn("No remaining quantity for prize '${prize.name}'")
            return emptyList()
        }

        return when (raffle.winnerSelectionMethod.uppercase()) {
            "RANDOM" -> selectRandomWinners(raffle, prize, eligibleTickets, winnersToSelect)
            "PROBABILITY" -> selectProbabilityBasedWinners(raffle, prize, eligibleTickets, winnersToSelect)
            "FIRST_COME_FIRST_SERVED" -> selectFirstComeFirstServedWinners(raffle, prize, eligibleTickets, winnersToSelect)
            "WEIGHTED" -> selectWeightedRandomWinners(raffle, prize, eligibleTickets, winnersToSelect)
            else -> {
                logger.warn("Unknown winner selection method: ${raffle.winnerSelectionMethod}, using RANDOM")
                selectRandomWinners(raffle, prize, eligibleTickets, winnersToSelect)
            }
        }
    }

    /**
     * Select winners using pure random selection
     */
    private fun selectRandomWinners(
        raffle: Raffle,
        prize: RafflePrize,
        eligibleTickets: List<RaffleTicket>,
        count: Int
    ): List<RaffleWinner> {
        val shuffledTickets = eligibleTickets.shuffled(random)
        val selectedTickets = shuffledTickets.take(count)

        return selectedTickets.map { ticket ->
            createRaffleWinner(raffle, prize, ticket)
        }
    }

    /**
     * Select winners based on probability (if prize has winning probability set)
     */
    private fun selectProbabilityBasedWinners(
        raffle: Raffle,
        prize: RafflePrize,
        eligibleTickets: List<RaffleTicket>,
        count: Int
    ): List<RaffleWinner> {
        val probability = prize.winningProbability?.toDouble() ?: return selectRandomWinners(raffle, prize, eligibleTickets, count)

        val winners = mutableListOf<RaffleWinner>()
        val shuffledTickets = eligibleTickets.shuffled(random)

        for (ticket in shuffledTickets) {
            if (winners.size >= count) break

            if (random.nextDouble() < probability) {
                winners.add(createRaffleWinner(raffle, prize, ticket))
            }
        }

        // If we didn't get enough winners through probability, fill remaining with random selection
        if (winners.size < count) {
            val remainingTickets = shuffledTickets.filter { ticket ->
                winners.none { it.ticket.id == ticket.id }
            }
            val additionalWinners = selectRandomWinners(raffle, prize, remainingTickets, count - winners.size)
            winners.addAll(additionalWinners)
        }

        return winners.take(count)
    }

    /**
     * Select winners based on first-come-first-served (earliest ticket creation)
     */
    private fun selectFirstComeFirstServedWinners(
        raffle: Raffle,
        prize: RafflePrize,
        eligibleTickets: List<RaffleTicket>,
        count: Int
    ): List<RaffleWinner> {
        val sortedTickets = eligibleTickets.sortedBy { it.createdAt }
        val selectedTickets = sortedTickets.take(count)

        return selectedTickets.map { ticket ->
            createRaffleWinner(raffle, prize, ticket)
        }
    }

    /**
     * Select winners using weighted random selection (based on ticket source type)
     */
    private fun selectWeightedRandomWinners(
        raffle: Raffle,
        prize: RafflePrize,
        eligibleTickets: List<RaffleTicket>,
        count: Int
    ): List<RaffleWinner> {
        // Assign weights based on ticket source type
        val weightedTickets = eligibleTickets.map { ticket ->
            val weight = getTicketWeight(ticket.sourceType)
            WeightedTicket(ticket, weight)
        }

        val selectedTickets = selectWeightedRandom(weightedTickets, count)

        return selectedTickets.map { ticket ->
            createRaffleWinner(raffle, prize, ticket)
        }
    }

    /**
     * Get weight for ticket based on source type
     */
    private fun getTicketWeight(sourceType: TicketSourceType): Double {
        return when (sourceType) {
            TicketSourceType.DIRECT_PURCHASE -> 2.0  // Higher weight for purchased tickets
            TicketSourceType.COUPON_REDEMPTION -> 1.5
            TicketSourceType.LOYALTY_REWARD -> 1.3
            TicketSourceType.REFERRAL -> 1.2
            TicketSourceType.PROMOTIONAL -> 1.0
            TicketSourceType.BONUS -> 1.0
            TicketSourceType.ADMIN_ISSUED -> 0.5  // Lower weight for admin issued
        }
    }

    /**
     * Select tickets using weighted random algorithm
     */
    private fun selectWeightedRandom(weightedTickets: List<WeightedTicket>, count: Int): List<RaffleTicket> {
        if (weightedTickets.isEmpty()) return emptyList()

        val totalWeight = weightedTickets.sumOf { it.weight }
        val selected = mutableListOf<RaffleTicket>()
        val remaining = weightedTickets.toMutableList()

        repeat(minOf(count, weightedTickets.size)) {
            if (remaining.isEmpty()) return@repeat

            val randomValue = random.nextDouble() * remaining.sumOf { it.weight }
            var currentWeight = 0.0

            for (i in remaining.indices) {
                currentWeight += remaining[i].weight
                if (randomValue <= currentWeight) {
                    selected.add(remaining[i].ticket)
                    remaining.removeAt(i)
                    break
                }
            }
        }

        return selected
    }

    /**
     * Create a RaffleWinner instance
     */
    private fun createRaffleWinner(
        raffle: Raffle,
        prize: RafflePrize,
        ticket: RaffleTicket
    ): RaffleWinner {
        val now = LocalDateTime.now()
        val claimDeadline = calculateClaimDeadline(prize)

        return RaffleWinner(
            raffle = raffle,
            userId = ticket.userId,
            ticket = ticket,
            prize = prize,
            status = WinnerStatus.PENDING_CLAIM,
            wonAt = now,
            claimDeadline = claimDeadline,
            verificationCode = generateVerificationCode(),
            isVerified = !prize.requiresIdentityVerification // Auto-verify if no verification required
        )
    }

    /**
     * Calculate claim deadline for a prize
     */
    private fun calculateClaimDeadline(prize: RafflePrize): LocalDateTime {
        val baseDays = when (prize.prizeType) {
            PrizeType.CASH -> 30
            PrizeType.GIFT_CARD -> 90
            PrizeType.CREDIT -> 60
            PrizeType.PHYSICAL_ITEM -> 14
            PrizeType.MERCHANDISE -> 14
            PrizeType.SERVICE -> 30
            PrizeType.DISCOUNT -> 60
            PrizeType.POINTS -> 90
            PrizeType.FUEL_CREDIT -> 60
            PrizeType.OTHER -> 30
        }

        return LocalDateTime.now().plusDays(baseDays.toLong())
    }

    /**
     * Generate verification code for winner
     */
    private fun generateVerificationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random(random) }
            .joinToString("")
    }

    /**
     * Check if raffle allows multiple wins per user
     */
    private fun allowsMultipleWinsPerUser(raffle: Raffle): Boolean {
        // For now, assume most raffles don't allow multiple wins per user
        // This could be a configurable property on the Raffle entity
        return raffle.raffleType == RaffleType.INSTANT_WIN ||
               raffle.raffleType == RaffleType.TIERED
    }

    /**
     * Data class for weighted ticket selection
     */
    private data class WeightedTicket(
        val ticket: RaffleTicket,
        val weight: Double
    )
}