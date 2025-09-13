package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.model.*
import com.gasolinerajsm.adengine.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Service for advertisement serving with targeting logic and availability filtering
 */
@Service
@Transactional
class AdService(
    private val advertisementRepository: AdvertisementRepository,
    private val adEngagementRepository: AdEngagementRepository,
    private val engagementTrackingService: EngagementTrackingService
) {
    private val logger = LoggerFactory.getLogger(AdService::class.java)

    /**
     * Get advertisements eligible for serving to a user
     */
    @Transactional(readOnly = true)
    fun getEligibleAdvertisements(
        userId: Long,
        userAge: Int? = null,
        userGender: String? = null,
        userLocation: String? = null,
        userSegments: List<String> = emptyList(),
        stationId: Long? = null,
        adType: AdType? = null,
        limit: Int = 10
    ): List<Advertisement> {
        logger.debug("Getting eligible advertisements for user $userId")

        // Get base eligible advertisements
        val baseEligible = advertisementRepository.findEligibleAdvertisements()

        // Apply targeting filters
        val targeted = baseEligible.filter { ad ->
            isAdEligibleForUser(ad, userId, userAge, userGender, userLocation, userSegments, stationId)
        }

        // Apply type filter if specified
        val typeFiltered = if (adType != null) {
            targeted.filter { it.adType == adType }
        } else targeted

        // Apply scheduling filter
        val scheduled = typeFiltered.filter { it.isActiveAndScheduled() }

        // Apply impression limits
        val impressionFiltered = scheduled.filter { ad ->
            !hasUserReachedImpressionLimit(userId, ad.id) && !hasDailyImpressionLimitReached(ad.id)
        }

        // Sort by priority and take limit
        val result = impressionFiltered
            .sortedWith(compareByDescending<Advertisement> { it.priority }
                .thenBy { it.createdAt })
            .take(limit)

        logger.info("Found ${result.size} eligible advertisements for user $userId")
        return result
    }

    /**
     * Serve an advertisement to a user
     */
    fun serveAdvertisement(
        userId: Long,
        sessionId: String? = null,
        userAge: Int? = null,
        userGender: String? = null,
        userLocation: String? = null,
        userSegments: List<String> = emptyList(),
        stationId: Long? = null,
        adType: AdType? = null,
        placementContext: String? = null
    ): Advertisement? {
        logger.info("Serving advertisement to user $userId")

        val eligibleAds = getEligibleAdvertisements(
            userId = userId,
            userAge = userAge,
            userGender = userGender,
            userLocation = userLocation,
            userSegments = userSegments,
            stationId = stationId,
            adType = adType,
            limit = 5
        )

        if (eligibleAds.isEmpty()) {
            logger.info("No eligible advertisements found for user $userId")
            return null
        }

        // Select advertisement using weighted random selection based on priority
        val selectedAd = selectAdvertisementWeighted(eligibleAds)

        // Create impression engagement
        engagementTrackingService.createImpression(
            userId = userId,
            advertisement = selectedAd,
            sessionId = sessionId,
            stationId = stationId,
            placementContext = placementContext
        )

        logger.info("Served advertisement ${selectedAd.id} to user $userId")
        return selectedAd
    }

    /**
     * Get advertisements by campaign
     */
    @Transactional(readOnly = true)
    fun getAdvertisementsByCampaign(campaignId: Long, pageable: Pageable): Page<Advertisement> {
        return advertisementRepository.findByCampaignId(campaignId, pageable)
    }

    /**
     * Get advertisement by ID
     */
    @Transactional(readOnly = true)
    fun getAdvertisementById(id: Long): Advertisement? {
        return advertisementRepository.findById(id).orElse(null)
    }

    /**
     * Create a new advertisement
     */
    fun createAdvertisement(advertisement: Advertisement): Advertisement {
        logger.info("Creating new advertisement: ${advertisement.title}")

        // Validate advertisement data
        validateAdvertisementData(advertisement)

        // Check for title conflicts
        if (advertisementRepository.existsByTitleIgnoreCase(advertisement.title)) {
            throw IllegalArgumentException("Advertisement with title '${advertisement.title}' already exists")
        }

        val savedAd = advertisementRepository.save(advertisement)
        logger.info("Created advertisement with ID: ${savedAd.id}")

        return savedAd
    }

    /**
     * Update an existing advertisement
     */
    fun updateAdvertisement(id: Long, updatedAd: Advertisement): Advertisement {
        logger.info("Updating advertisement with ID: $id")

        val existingAd = advertisementRepository.findById(id)
            .orElseThrow { NoSuchElementException("Advertisement not found with ID: $id") }

        // Check if advertisement can be modified
        if (!existingAd.status.allowsModifications()) {
            throw IllegalStateException("Advertisement in status '${existingAd.status}' cannot be modified")
        }

        // Validate updated data
        validateAdvertisementData(updatedAd)

        // Check for title conflicts (excluding current ad)
        if (updatedAd.title != existingAd.title &&
            advertisementRepository.existsByTitleIgnoreCase(updatedAd.title)) {
            throw IllegalArgumentException("Advertisement with title '${updatedAd.title}' already exists")
        }

        val advertisement = updatedAd.copy(
            id = id,
            createdAt = existingAd.createdAt,
            updatedAt = LocalDateTime.now()
        )

        return advertisementRepository.save(advertisement)
    }

    /**
     * Activate an advertisement
     */
    fun activateAdvertisement(id: Long, activatedBy: String? = null): Advertisement {
        logger.info("Activating advertisement with ID: $id")

        val advertisement = advertisementRepository.findById(id)
            .orElseThrow { NoSuchElementException("Advertisement not found with ID: $id") }

        if (!advertisement.status.allowsModifications()) {
            throw IllegalStateException("Advertisement in status '${advertisement.status}' cannot be activated")
        }

        val activatedAd = advertisement.activate(activatedBy)
        return advertisementRepository.save(activatedAd)
    }

    /**
     * Pause an advertisement
     */
    fun pauseAdvertisement(id: Long, pausedBy: String? = null): Advertisement {
        logger.info("Pausing advertisement with ID: $id")

        val advertisement = advertisementRepository.findById(id)
            .orElseThrow { NoSuchElementException("Advertisement not found with ID: $id") }

        if (advertisement.status != AdStatus.ACTIVE) {
            throw IllegalStateException("Only active advertisements can be paused")
        }

        val pausedAd = advertisement.pause(pausedBy)
        return advertisementRepository.save(pausedAd)
    }

    /**
     * Complete an advertisement
     */
    fun completeAdvertisement(id: Long, completedBy: String? = null): Advertisement {
        logger.info("Completing advertisement with ID: $id")

        val advertisement = advertisementRepository.findById(id)
            .orElseThrow { NoSuchElementException("Advertisement not found with ID: $id") }

        val completedAd = advertisement.complete(completedBy)
        return advertisementRepository.save(completedAd)
    }

    /**
     * Get advertisement statistics
     */
    @Transactional(readOnly = true)
    fun getAdvertisementStatistics(id: Long): Map<String, Any> {
        val advertisement = advertisementRepository.findById(id)
            .orElseThrow { NoSuchElementException("Advertisement not found with ID: $id") }

        val engagementStats = adEngagementRepository.getEngagementStatisticsByAdvertisement(id)

        return mapOf(
            "advertisement" to mapOf(
                "id" to advertisement.id,
                "title" to advertisement.title,
                "status" to advertisement.status,
                "type" to advertisement.adType,
                "priority" to advertisement.priority,
                "startDate" to advertisement.startDate,
                "endDate" to advertisement.endDate,
                "totalImpressions" to advertisement.totalImpressions,
                "totalClicks" to advertisement.totalClicks,
                "totalCompletions" to advertisement.totalCompletions,
                "totalSpend" to advertisement.totalSpend,
                "clickThroughRate" to advertisement.getClickThroughRate(),
                "completionRate" to advertisement.getCompletionRate(),
                "effectiveCPM" to advertisement.getEffectiveCPM(),
                "remainingBudget" to advertisement.getRemainingBudget()
            ),
            "engagements" to engagementStats
        )
    }

    /**
     * Search advertisements
     */
    @Transactional(readOnly = true)
    fun searchAdvertisements(query: String, pageable: Pageable): Page<Advertisement> {
        return advertisementRepository.findByTitleContainingIgnoreCase(query, pageable)
    }

    /**
     * Get advertisements expiring soon
     */
    @Transactional(readOnly = true)
    fun getAdvertisementsExpiringSoon(hours: Long = 24): List<Advertisement> {
        val deadline = LocalDateTime.now().plusHours(hours)
        return advertisementRepository.findAdvertisementsExpiringSoon(deadline = deadline)
    }

    /**
     * Check if advertisement is eligible for user
     */
    private fun isAdEligibleForUser(
        advertisement: Advertisement,
        userId: Long,
        userAge: Int?,
        userGender: String?,
        userLocation: String?,
        userSegments: List<String>,
        stationId: Long?
    ): Boolean {
        // Check basic targeting
        if (!advertisement.matchesUserTargeting(userAge, userGender, userLocation, userSegments)) {
            return false
        }

        // Check station targeting
        if (advertisement.targetStations != null && stationId != null) {
            val targetStations = advertisement.targetStations.split(",").map { it.trim() }
            if (!targetStations.contains(stationId.toString())) {
                return false
            }
        }

        // Check budget
        if (!advertisement.hasBudgetRemaining()) {
            return false
        }

        return true
    }

    /**
     * Check if user has reached impression limit for advertisement
     */
    private fun hasUserReachedImpressionLimit(userId: Long, advertisementId: Long): Boolean {
        val advertisement = advertisementRepository.findById(advertisementId).orElse(null) ?: return true

        advertisement.maxImpressionsPerUser?.let { maxImpressions ->
            val userImpressions = adEngagementRepository.countByUserIdAndAdvertisementId(userId, advertisementId)
            return userImpressions >= maxImpressions
        }

        return false
    }

    /**
     * Check if daily impression limit is reached for advertisement
     */
    private fun hasDailyImpressionLimitReached(advertisementId: Long): Boolean {
        val advertisement = advertisementRepository.findById(advertisementId).orElse(null) ?: return true

        advertisement.maxDailyImpressions?.let { maxDaily ->
            val startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay()
            val todayImpressions = adEngagementRepository.countDailyImpressions(advertisementId, startOfDay)
            return todayImpressions >= maxDaily
        }

        return false
    }

    /**
     * Select advertisement using weighted random selection
     */
    private fun selectAdvertisementWeighted(advertisements: List<Advertisement>): Advertisement {
        if (advertisements.size == 1) return advertisements.first()

        // Calculate weights based on priority (higher priority = higher weight)
        val weights = advertisements.map { it.priority.toDouble() }
        val totalWeight = weights.sum()

        val random = Random.nextDouble() * totalWeight
        var currentWeight = 0.0

        for (i in advertisements.indices) {
            currentWeight += weights[i]
            if (random <= currentWeight) {
                return advertisements[i]
            }
        }

        // Fallback to first advertisement
        return advertisements.first()
    }

    /**
     * Validate advertisement data
     */
    private fun validateAdvertisementData(advertisement: Advertisement) {
        // Validate dates
        if (advertisement.startDate.isAfter(advertisement.endDate)) {
            throw IllegalArgumentException("Start date must be before end date")
        }

        // Validate budget
        if (advertisement.totalBudget != null && advertisement.dailyBudget != null) {
            val daysInCampaign = java.time.temporal.ChronoUnit.DAYS.between(
                advertisement.startDate.toLocalDate(),
                advertisement.endDate.toLocalDate()
            )
            val estimatedTotalFromDaily = advertisement.dailyBudget.multiply(java.math.BigDecimal(daysInCampaign))

            if (estimatedTotalFromDaily > advertisement.totalBudget) {
                logger.warn("Daily budget may exceed total budget over campaign duration")
            }
        }

        // Validate targeting
        if (advertisement.targetAgeMin != null && advertisement.targetAgeMax != null) {
            if (advertisement.targetAgeMin > advertisement.targetAgeMax) {
                throw IllegalArgumentException("Minimum target age cannot be greater than maximum target age")
            }
        }

        // Validate scheduling
        if (advertisement.allowedHoursStart != null && advertisement.allowedHoursEnd != null) {
            if (advertisement.allowedHoursStart < 0 || advertisement.allowedHoursStart > 23 ||
                advertisement.allowedHoursEnd < 0 || advertisement.allowedHoursEnd > 23) {
                throw IllegalArgumentException("Allowed hours must be between 0 and 23")
            }
        }
    }

    /**
     * Delete advertisement (only if in draft status)
     */
    fun deleteAdvertisement(id: Long) {
        logger.info("Deleting advertisement with ID: $id")

        val advertisement = advertisementRepository.findById(id)
            .orElseThrow { NoSuchElementException("Advertisement not found with ID: $id") }

        if (advertisement.status != AdStatus.DRAFT) {
            throw IllegalStateException("Only draft advertisements can be deleted")
        }

        val engagementCount = adEngagementRepository.countByAdvertisementId(id)
        if (engagementCount > 0) {
            throw IllegalStateException("Cannot delete advertisement with existing engagements")
        }

        advertisementRepository.deleteById(id)
        logger.info("Deleted advertisement with ID: $id")
    }
}