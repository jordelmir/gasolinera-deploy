package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.model.*
import com.gasolinerajsm.adengine.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Service for tracking advertisement engagement and completion monitoring
 */
@Service
@Transactional
class EngagementTrackingService(
    private val adEngagementRepository: AdEngagementRepository,
    private val advertisementRepository: AdvertisementRepository
) {
    private val logger = LoggerFactory.getLogger(EngagementTrackingService::class.java)

    /**
     * Create an impression engagement
     */
    fun createImpression(
        userId: Long,
        advertisement: Advertisement,
        sessionId: String? = null,
        stationId: Long? = null,
        deviceType: String? = null,
        deviceId: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        placementContext: String? = null,
        locationLatitude: BigDecimal? = null,
        locationLongitude: BigDecimal? = null
    ): AdEngagement {
        logger.debug("Creating impression engagement for user $userId, ad ${advertisement.id}")

        val engagement = AdEngagement(
            userId = userId,
            advertisement = advertisement,
            sessionId = sessionId,
            engagementType = EngagementType.IMPRESSION,
            status = EngagementStatus.STARTED,
            startedAt = LocalDateTime.now(),
            stationId = stationId,
            deviceType = deviceType,
            deviceId = deviceId,
            ipAddress = ipAddress,
            userAgent = userAgent,
            placementContext = placementContext,
            locationLatitude = locationLatitude,
            locationLongitude = locationLongitude
        )

        val savedEngagement = adEngagementRepository.save(engagement)

        // Update advertisement statistics
        updateAdvertisementStatistics(advertisement.id, impressions = 1)

        logger.info("Created impression engagement ${savedEngagement.id} for user $userId")
        return savedEngagement
    }

    /**
     * Track advertisement view
     */
    fun trackView(
        engagementId: Long,
        viewDurationSeconds: Int? = null
    ): AdEngagement {
        logger.debug("Tracking view for engagement $engagementId")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.copy(
            engagementType = EngagementType.VIEW,
            status = EngagementStatus.VIEWED,
            viewDurationSeconds = viewDurationSeconds
        )

        return adEngagementRepository.save(updatedEngagement)
    }

    /**
     * Track advertisement click
     */
    fun trackClick(
        engagementId: Long,
        clickThroughUrl: String? = null
    ): AdEngagement {
        logger.debug("Tracking click for engagement $engagementId")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.click(clickThroughUrl)

        val savedEngagement = adEngagementRepository.save(updatedEngagement)

        // Update advertisement statistics
        updateAdvertisementStatistics(engagement.advertisement.id, clicks = 1)

        logger.info("Tracked click for engagement $engagementId")
        return savedEngagement
    }

    /**
     * Track advertisement interaction
     */
    fun trackInteraction(
        engagementId: Long,
        interactionType: String? = null
    ): AdEngagement {
        logger.debug("Tracking interaction for engagement $engagementId")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.copy(
            engagementType = EngagementType.INTERACTION,
            status = EngagementStatus.INTERACTED,
            interactionsCount = engagement.interactionsCount + 1,
            metadata = interactionType?.let { "${engagement.metadata ?: ""}|interaction:$it" } ?: engagement.metadata
        )

        return adEngagementRepository.save(updatedEngagement)
    }

    /**
     * Track advertisement completion
     */
    fun trackCompletion(
        engagementId: Long,
        completionPercentage: BigDecimal? = null,
        viewDurationSeconds: Int? = null
    ): AdEngagement {
        logger.debug("Tracking completion for engagement $engagementId")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.complete(completionPercentage, viewDurationSeconds)

        val savedEngagement = adEngagementRepository.save(updatedEngagement)

        // Update advertisement statistics
        updateAdvertisementStatistics(engagement.advertisement.id, completions = 1)

        logger.info("Tracked completion for engagement $engagementId")
        return savedEngagement
    }

    /**
     * Track advertisement skip
     */
    fun trackSkip(engagementId: Long): AdEngagement {
        logger.debug("Tracking skip for engagement $engagementId")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.skip()

        return adEngagementRepository.save(updatedEngagement)
    }

    /**
     * Track engagement error
     */
    fun trackError(
        engagementId: Long,
        errorMessage: String,
        errorCode: String? = null
    ): AdEngagement {
        logger.debug("Tracking error for engagement $engagementId: $errorMessage")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.error(errorMessage, errorCode)

        return adEngagementRepository.save(updatedEngagement)
    }

    /**
     * Update engagement progress
     */
    fun updateEngagementProgress(
        engagementId: Long,
        viewDurationSeconds: Int,
        completionPercentage: BigDecimal? = null,
        interactions: Int = 0,
        pauses: Int = 0,
        replays: Int = 0
    ): AdEngagement {
        logger.debug("Updating progress for engagement $engagementId")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.copy(
            viewDurationSeconds = viewDurationSeconds,
            completionPercentage = completionPercentage,
            interactionsCount = engagement.interactionsCount + interactions,
            pauseCount = engagement.pauseCount + pauses,
            replayCount = engagement.replayCount + replays
        )

        return adEngagementRepository.save(updatedEngagement)
    }

    /**
     * Award tickets for engagement
     */
    fun awardTickets(
        engagementId: Long,
        baseTickets: Int,
        bonusTickets: Int,
        raffleEntryId: Long? = null
    ): AdEngagement {
        logger.info("Awarding tickets for engagement $engagementId: base=$baseTickets, bonus=$bonusTickets")

        val engagement = getEngagementById(engagementId)

        if (!engagement.qualifiesForRewards()) {
            throw IllegalStateException("Engagement does not qualify for rewards")
        }

        if (engagement.hasTicketsAwarded()) {
            throw IllegalStateException("Tickets have already been awarded for this engagement")
        }

        val updatedEngagement = engagement.awardTickets(baseTickets, bonusTickets, raffleEntryId)

        return adEngagementRepository.save(updatedEngagement)
    }

    /**
     * Get engagement by ID
     */
    @Transactional(readOnly = true)
    fun getEngagementById(id: Long): AdEngagement {
        return adEngagementRepository.findById(id)
            .orElseThrow { NoSuchElementException("Engagement not found with ID: $id") }
    }

    /**
     * Get user engagements
     */
    @Transactional(readOnly = true)
    fun getUserEngagements(userId: Long, pageable: Pageable): Page<AdEngagement> {
        return adEngagementRepository.findByUserId(userId, pageable)
    }

    /**
     * Get advertisement engagements
     */
    @Transactional(readOnly = true)
    fun getAdvertisementEngagements(advertisementId: Long, pageable: Pageable): Page<AdEngagement> {
        return adEngagementRepository.findByAdvertisementId(advertisementId, pageable)
    }

    /**
     * Get engagements pending ticket award
     */
    @Transactional(readOnly = true)
    fun getEngagementsPendingTicketAward(): List<AdEngagement> {
        return adEngagementRepository.findEngagementsPendingTicketAward()
    }

    /**
     * Get user engagement statistics
     */
    @Transactional(readOnly = true)
    fun getUserEngagementStatistics(userId: Long): Map<String, Any> {
        return adEngagementRepository.getUserEngagementStatistics(userId)
    }

    /**
     * Get advertisement engagement statistics
     */
    @Transactional(readOnly = true)
    fun getAdvertisementEngagementStatistics(advertisementId: Long): Map<String, Any> {
        return adEngagementRepository.getEngagementStatisticsByAdvertisement(advertisementId)
    }

    /**
     * Get daily engagement statistics
     */
    @Transactional(readOnly = true)
    fun getDailyEngagementStatistics(): Map<String, Any> {
        val startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay()
        return adEngagementRepository.getDailyEngagementStatistics(startOfDay)
    }

    /**
     * Calculate engagement quality score
     */
    @Transactional(readOnly = true)
    fun calculateEngagementQuality(engagementId: Long): Double {
        val engagement = getEngagementById(engagementId)
        return engagement.getEngagementQualityScore()
    }

    /**
     * Get high-performing engagements
     */
    @Transactional(readOnly = true)
    fun getHighPerformingEngagements(
        minCompletionPercentage: BigDecimal = BigDecimal("80.0"),
        minViewDuration: Int = 30
    ): List<AdEngagement> {
        return adEngagementRepository.findHighPerformingEngagements(minCompletionPercentage, minViewDuration)
    }

    /**
     * Get engagement conversion funnel
     */
    @Transactional(readOnly = true)
    fun getEngagementFunnel(
        advertisementId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Map<String, Any> {
        return adEngagementRepository.getEngagementFunnel(advertisementId, startDate, endDate)
    }

    /**
     * Process abandoned engagements
     */
    fun processAbandonedEngagements(timeoutMinutes: Long = 30): List<AdEngagement> {
        logger.info("Processing abandoned engagements older than $timeoutMinutes minutes")

        val cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes)
        val abandonedEngagements = adEngagementRepository.findAbandonedEngagements(cutoffTime)

        val processedEngagements = abandonedEngagements.map { engagement ->
            val updatedEngagement = engagement.copy(status = EngagementStatus.ABANDONED)
            adEngagementRepository.save(updatedEngagement)
        }

        logger.info("Processed ${processedEngagements.size} abandoned engagements")
        return processedEngagements
    }

    /**
     * Get hourly engagement distribution
     */
    @Transactional(readOnly = true)
    fun getHourlyEngagementDistribution(days: Long = 7): Map<Int, Long> {
        val startDate = LocalDateTime.now().minusDays(days)
        val distribution = adEngagementRepository.getHourlyEngagementDistribution(startDate)

        return distribution.associate {
            (it[0] as Number).toInt() to (it[1] as Number).toLong()
        }
    }

    /**
     * Validate engagement completion requirements
     */
    fun validateCompletionRequirements(
        engagementId: Long,
        advertisement: Advertisement
    ): Boolean {
        val engagement = getEngagementById(engagementId)

        // Check minimum view time
        advertisement.minViewTimeForBonus?.let { minTime ->
            if (engagement.viewDurationSeconds == null || engagement.viewDurationSeconds < minTime) {
                return false
            }
        }

        // Check completion percentage
        if (advertisement.requiresCompletionForBonus) {
            if (!engagement.isCompleted()) {
                return false
            }
        }

        return true
    }

    /**
     * Calculate ticket rewards for engagement
     */
    fun calculateTicketRewards(
        engagementId: Long,
        baseTickets: Int
    ): Pair<Int, Int> { // Returns (baseTickets, bonusTickets)
        val engagement = getEngagementById(engagementId)
        val advertisement = engagement.advertisement

        if (!validateCompletionRequirements(engagementId, advertisement)) {
            return Pair(0, 0)
        }

        val bonusTickets = advertisement.calculateBonusTickets(
            baseTickets = baseTickets,
            wasCompleted = engagement.isCompleted(),
            viewTimeSeconds = engagement.viewDurationSeconds
        )

        return Pair(baseTickets, bonusTickets)
    }

    /**
     * Update advertisement statistics
     */
    private fun updateAdvertisementStatistics(
        advertisementId: Long,
        impressions: Long = 0,
        clicks: Long = 0,
        completions: Long = 0,
        spend: BigDecimal = BigDecimal.ZERO
    ) {
        advertisementRepository.updateStatistics(advertisementId, impressions, clicks, completions, spend)
    }

    /**
     * Set billing information for engagement
     */
    fun setBillingInfo(
        engagementId: Long,
        cost: BigDecimal,
        billingEvent: String
    ): AdEngagement {
        logger.debug("Setting billing info for engagement $engagementId: $cost for $billingEvent")

        val engagement = getEngagementById(engagementId)

        val updatedEngagement = engagement.setBilling(cost, billingEvent)
        val savedEngagement = adEngagementRepository.save(updatedEngagement)

        // Update advertisement spend
        updateAdvertisementStatistics(engagement.advertisement.id, spend = cost)

        return savedEngagement
    }

    /**
     * Get engagements by session
     */
    @Transactional(readOnly = true)
    fun getEngagementsBySession(sessionId: String): List<AdEngagement> {
        return adEngagementRepository.findBySessionId(sessionId)
    }

    /**
     * Get recent user engagements
     */
    @Transactional(readOnly = true)
    fun getRecentUserEngagements(userId: Long, pageable: Pageable): Page<AdEngagement> {
        return adEngagementRepository.findRecentEngagementsByUser(userId, pageable)
    }
}