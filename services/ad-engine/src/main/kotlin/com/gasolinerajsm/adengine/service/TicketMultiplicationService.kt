package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.model.*
import com.gasolinerajsm.adengine.repository.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Service for calculating and awarding bonus raffle tickets based on ad engagement
 */
@Service
@Transactional
class TicketMultiplicationService(
    private val adEngagementRepository: AdEngagementRepository,
    private val engagementTrackingService: EngagementTrackingService,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(TicketMultiplicationService::class.java)

    /**
     * Calculate ticket multiplier for an engagement
     */
    fun calculateTicketMultiplier(
        engagementId: Long,
        baseTickets: Int
    ): TicketMultiplierResult {
        logger.debug("Calculating ticket multiplier for engagement $engagementId with base tickets $baseTickets")

        val engagement = engagementTrackingService.getEngagementById(engagementId)
        val advertisement = engagement.advertisement

        // Validate engagement qualifies for rewards
        if (!engagement.qualifiesForRewards()) {
            logger.debug("Engagement $engagementId does not qualify for rewards")
            return TicketMultiplierResult(
                engagementId = engagementId,
                baseTickets = baseTickets,
                multiplier = BigDecimal.ONE,
                bonusTickets = 0,
                totalTickets = baseTickets,
                qualifiesForReward = false,
                reason = "Engagement does not meet reward criteria"
            )
        }

        // Check completion requirements
        if (!engagementTrackingService.validateCompletionRequirements(engagementId, advertisement)) {
            logger.debug("Engagement $engagementId does not meet completion requirements")
            return TicketMultiplierResult(
                engagementId = engagementId,
                baseTickets = baseTickets,
                multiplier = BigDecimal.ONE,
                bonusTickets = 0,
                totalTickets = baseTickets,
                qualifiesForReward = false,
                reason = "Does not meet completion requirements"
            )
        }

        // Calculate multiplier and bonus
        val multiplier = advertisement.ticketMultiplier ?: BigDecimal.ONE
        val multipliedTickets = (baseTickets.toBigDecimal() * multiplier).toInt()
        val multiplierBonus = multipliedTickets - baseTickets

        // Calculate completion bonus
        val completionBonus = if (advertisement.bonusTicketsOnCompletion > 0) {
            val meetsRequirements = if (advertisement.requiresCompletionForBonus) {
                engagement.isCompleted() &&
                (advertisement.minViewTimeForBonus?.let {
                    engagement.viewDurationSeconds != null && engagement.viewDurationSeconds >= it
                } ?: true)
            } else {
                advertisement.minViewTimeForBonus?.let {
                    engagement.viewDurationSeconds != null && engagement.viewDurationSeconds >= it
                } ?: true
            }

            if (meetsRequirements) advertisement.bonusTicketsOnCompletion else 0
        } else 0

        val totalBonusTickets = multiplierBonus + completionBonus
        val totalTickets = baseTickets + totalBonusTickets

        logger.info("Calculated multiplier for engagement $engagementId: base=$baseTickets, multiplier=$multiplier, bonus=$totalBonusTickets, total=$totalTickets")

        return TicketMultiplierResult(
            engagementId = engagementId,
            baseTickets = baseTickets,
            multiplier = multiplier,
            bonusTickets = totalBonusTickets,
            totalTickets = totalTickets,
            qualifiesForReward = true,
            completionBonus = completionBonus,
            multiplierBonus = multiplierBonus,
            advertisementId = advertisement.id,
            campaignId = advertisement.campaignId
        )
    }

    /**
     * Award bonus tickets for completed engagement
     */
    fun awardBonusTickets(
        engagementId: Long,
        baseTickets: Int,
        raffleId: Long? = null
    ): TicketAwardResult {
        logger.info("Awarding bonus tickets for engagement $engagementId")

        val engagement = engagementTrackingService.getEngagementById(engagementId)

        // Check if tickets already awarded
        if (engagement.hasTicketsAwarded()) {
            throw IllegalStateException("Tickets have already been awarded for engagement $engagementId")
        }

        // Calculate multiplier
        val multiplierResult = calculateTicketMultiplier(engagementId, baseTickets)

        if (!multiplierResult.qualifiesForReward) {
            logger.warn("Engagement $engagementId does not qualify for ticket rewards: ${multiplierResult.reason}")
            return TicketAwardResult(
                engagementId = engagementId,
                success = false,
                reason = multiplierResult.reason ?: "Does not qualify for rewards",
                baseTickets = baseTickets,
                bonusTickets = 0,
                totalTickets = baseTickets
            )
        }

        try {
            // Award tickets in engagement
            val updatedEngagement = engagementTrackingService.awardTickets(
                engagementId = engagementId,
                baseTickets = multiplierResult.baseTickets,
                bonusTickets = multiplierResult.bonusTickets
            )

            // Publish event for raffle service integration
            val event = AdCompletionEvent(
                userId = engagement.userId,
                engagementId = engagementId,
                advertisementId = engagement.advertisement.id,
                campaignId = engagement.advertisement.campaignId,
                baseTickets = multiplierResult.baseTickets,
                bonusTickets = multiplierResult.bonusTickets,
                totalTickets = multiplierResult.totalTickets,
                raffleId = raffleId,
                completedAt = engagement.completedAt ?: LocalDateTime.now(),
                stationId = engagement.stationId
            )

            eventPublisher.publishEvent(event)

            logger.info("Successfully awarded ${multiplierResult.totalTickets} tickets for engagement $engagementId")

            return TicketAwardResult(
                engagementId = engagementId,
                success = true,
                baseTickets = multiplierResult.baseTickets,
                bonusTickets = multiplierResult.bonusTickets,
                totalTickets = multiplierResult.totalTickets,
                multiplier = multiplierResult.multiplier,
                eventPublished = true
            )

        } catch (e: Exception) {
            logger.error("Failed to award tickets for engagement $engagementId", e)
            return TicketAwardResult(
                engagementId = engagementId,
                success = false,
                reason = "Failed to award tickets: ${e.message}",
                baseTickets = baseTickets,
                bonusTickets = 0,
                totalTickets = baseTickets
            )
        }
    }

    /**
     * Process pending ticket awards
     */
    fun processPendingTicketAwards(raffleId: Long? = null): List<TicketAwardResult> {
        logger.info("Processing pending ticket awards")

        val pendingEngagements = engagementTrackingService.getEngagementsPendingTicketAward()
        logger.info("Found ${pendingEngagements.size} engagements pending ticket award")

        val results = mutableListOf<TicketAwardResult>()

        for (engagement in pendingEngagements) {
            try {
                // Use base tickets from engagement or default to 1
                val baseTickets = maxOf(engagement.baseTicketsEarned, 1)

                val result = awardBonusTickets(
                    engagementId = engagement.id,
                    baseTickets = baseTickets,
                    raffleId = raffleId
                )

                results.add(result)

            } catch (e: Exception) {
                logger.error("Failed to process pending award for engagement ${engagement.id}", e)
                results.add(
                    TicketAwardResult(
                        engagementId = engagement.id,
                        success = false,
                        reason = "Processing failed: ${e.message}",
                        baseTickets = engagement.baseTicketsEarned,
                        bonusTickets = 0,
                        totalTickets = engagement.baseTicketsEarned
                    )
                )
            }
        }

        val successCount = results.count { it.success }
        logger.info("Processed ${results.size} pending awards: $successCount successful, ${results.size - successCount} failed")

        return results
    }

    /**
     * Get ticket multiplication statistics
     */
    @Transactional(readOnly = true)
    fun getTicketMultiplicationStatistics(
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): TicketMultiplicationStats {
        val start = startDate ?: LocalDateTime.now().minusDays(30)
        val end = endDate ?: LocalDateTime.now()

        val engagements = adEngagementRepository.findByCreatedAtBetween(start, end, org.springframework.data.domain.Pageable.unpaged())

        val totalEngagements = engagements.totalElements
        val completedEngagements = engagements.content.count { it.isCompleted() }
        val ticketAwardedEngagements = engagements.content.count { it.hasTicketsAwarded() }
        val totalBaseTickets = engagements.content.sumOf { it.baseTicketsEarned }
        val totalBonusTickets = engagements.content.sumOf { it.bonusTicketsEarned }
        val totalTickets = engagements.content.sumOf { it.totalTicketsEarned }

        val averageMultiplier = if (totalBaseTickets > 0) {
            totalTickets.toDouble() / totalBaseTickets.toDouble()
        } else 0.0

        return TicketMultiplicationStats(
            periodStart = start,
            periodEnd = end,
            totalEngagements = totalEngagements,
            completedEngagements = completedEngagements.toLong(),
            ticketAwardedEngagements = ticketAwardedEngagements.toLong(),
            totalBaseTickets = totalBaseTickets.toLong(),
            totalBonusTickets = totalBonusTickets.toLong(),
            totalTickets = totalTickets.toLong(),
            averageMultiplier = BigDecimal.valueOf(averageMultiplier),
            completionRate = if (totalEngagements > 0) (completedEngagements.toDouble() / totalEngagements.toDouble()) * 100 else 0.0,
            awardRate = if (completedEngagements > 0) (ticketAwardedEngagements.toDouble() / completedEngagements.toDouble()) * 100 else 0.0
        )
    }

    /**
     * Get user ticket multiplication history
     */
    @Transactional(readOnly = true)
    fun getUserTicketMultiplicationHistory(
        userId: Long,
        limit: Int = 50
    ): List<UserTicketHistory> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, limit,
            org.springframework.data.domain.Sort.by("createdAt").descending())

        val engagements = adEngagementRepository.findByUserIdAndTicketsAwardedTrue(userId, pageable)

        return engagements.content.map { engagement ->
            UserTicketHistory(
                engagementId = engagement.id,
                advertisementId = engagement.advertisement.id,
                advertisementTitle = engagement.advertisement.title,
                campaignId = engagement.advertisement.campaignId,
                baseTickets = engagement.baseTicketsEarned,
                bonusTickets = engagement.bonusTicketsEarned,
                totalTickets = engagement.totalTicketsEarned,
                multiplier = engagement.advertisement.ticketMultiplier,
                completedAt = engagement.completedAt,
                awardedAt = engagement.ticketsAwardedAt,
                stationId = engagement.stationId
            )
        }
    }

    /**
     * Validate ticket multiplication rules
     */
    fun validateMultiplicationRules(advertisement: Advertisement): List<String> {
        val issues = mutableListOf<String>()

        // Check multiplier range
        advertisement.ticketMultiplier?.let { multiplier ->
            if (multiplier < BigDecimal.ONE) {
                issues.add("Ticket multiplier cannot be less than 1.0")
            }
            if (multiplier > BigDecimal.TEN) {
                issues.add("Ticket multiplier cannot exceed 10.0")
            }
        }

        // Check bonus tickets
        if (advertisement.bonusTicketsOnCompletion < 0) {
            issues.add("Bonus tickets on completion cannot be negative")
        }

        if (advertisement.bonusTicketsOnCompletion > 100) {
            issues.add("Bonus tickets on completion seems excessive (>100)")
        }

        // Check minimum view time
        advertisement.minViewTimeForBonus?.let { minTime ->
            if (minTime < 0) {
                issues.add("Minimum view time for bonus cannot be negative")
            }

            advertisement.durationSeconds?.let { duration ->
                if (minTime > duration) {
                    issues.add("Minimum view time for bonus cannot exceed advertisement duration")
                }
            }
        }

        return issues
    }

    /**
     * Calculate potential ticket earnings for user
     */
    @Transactional(readOnly = true)
    fun calculatePotentialEarnings(
        userId: Long,
        advertisementId: Long,
        baseTickets: Int
    ): PotentialEarnings {
        val advertisement = engagementTrackingService.getEngagementById(advertisementId).advertisement

        // Calculate maximum possible earnings
        val multiplier = advertisement.ticketMultiplier ?: BigDecimal.ONE
        val multipliedTickets = (baseTickets.toBigDecimal() * multiplier).toInt()
        val multiplierBonus = multipliedTickets - baseTickets
        val completionBonus = advertisement.bonusTicketsOnCompletion

        val maxTotalTickets = baseTickets + multiplierBonus + completionBonus

        // Calculate minimum earnings (if only base requirements met)
        val minTotalTickets = if (advertisement.requiresCompletionForBonus) {
            baseTickets + multiplierBonus
        } else {
            maxTotalTickets
        }

        return PotentialEarnings(
            advertisementId = advertisementId,
            baseTickets = baseTickets,
            multiplier = multiplier,
            multiplierBonus = multiplierBonus,
            completionBonus = completionBonus,
            minTotalTickets = minTotalTickets,
            maxTotalTickets = maxTotalTickets,
            requiresCompletion = advertisement.requiresCompletionForBonus,
            minViewTimeSeconds = advertisement.minViewTimeForBonus
        )
    }
}

/**
 * Data class for ticket multiplier calculation result
 */
data class TicketMultiplierResult(
    val engagementId: Long,
    val baseTickets: Int,
    val multiplier: BigDecimal,
    val bonusTickets: Int,
    val totalTickets: Int,
    val qualifiesForReward: Boolean,
    val reason: String? = null,
    val completionBonus: Int = 0,
    val multiplierBonus: Int = 0,
    val advertisementId: Long? = null,
    val campaignId: Long? = null
)

/**
 * Data class for ticket award result
 */
data class TicketAwardResult(
    val engagementId: Long,
    val success: Boolean,
    val reason: String? = null,
    val baseTickets: Int,
    val bonusTickets: Int,
    val totalTickets: Int,
    val multiplier: BigDecimal? = null,
    val eventPublished: Boolean = false
)

/**
 * Data class for ticket multiplication statistics
 */
data class TicketMultiplicationStats(
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val totalEngagements: Long,
    val completedEngagements: Long,
    val ticketAwardedEngagements: Long,
    val totalBaseTickets: Long,
    val totalBonusTickets: Long,
    val totalTickets: Long,
    val averageMultiplier: BigDecimal,
    val completionRate: Double,
    val awardRate: Double
)

/**
 * Data class for user ticket history
 */
data class UserTicketHistory(
    val engagementId: Long,
    val advertisementId: Long,
    val advertisementTitle: String,
    val campaignId: Long,
    val baseTickets: Int,
    val bonusTickets: Int,
    val totalTickets: Int,
    val multiplier: BigDecimal?,
    val completedAt: LocalDateTime?,
    val awardedAt: LocalDateTime?,
    val stationId: Long?
)

/**
 * Data class for potential earnings calculation
 */
data class PotentialEarnings(
    val advertisementId: Long,
    val baseTickets: Int,
    val multiplier: BigDecimal,
    val multiplierBonus: Int,
    val completionBonus: Int,
    val minTotalTickets: Int,
    val maxTotalTickets: Int,
    val requiresCompletion: Boolean,
    val minViewTimeSeconds: Int?
)

/**
 * Event published when ad engagement is completed and tickets are awarded
 */
data class AdCompletionEvent(
    val userId: Long,
    val engagementId: Long,
    val advertisementId: Long,
    val campaignId: Long,
    val baseTickets: Int,
    val bonusTickets: Int,
    val totalTickets: Int,
    val raffleId: Long?,
    val completedAt: LocalDateTime,
    val stationId: Long?
)