package com.gasolinerajsm.adengine.controller

import com.gasolinerajsm.adengine.dto.*
import com.gasolinerajsm.adengine.model.*
import com.gasolinerajsm.adengine.service.*
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for advertisement serving and engagement endpoints
 */
@RestController
@RequestMapping("/api/v1/ads")
@CrossOrigin(origins = ["*"])
class AdController(
    private val adService: AdService,
    private val engagementTrackingService: EngagementTrackingService,
    private val ticketMultiplicationService: TicketMultiplicationService
) {
    private val logger = LoggerFactory.getLogger(AdController::class.java)

    /**
     * Serve an advertisement to a user
     */
    @PostMapping("/serve")
    fun serveAdvertisement(
        @Valid @RequestBody request: AdServingRequest
    ): ResponseEntity<AdServingResponse> {
        logger.info("Serving advertisement to user ${request.userId}")

        return try {
            val advertisement = adService.serveAdvertisement(
                userId = request.userId,
                sessionId = request.sessionId,
                userAge = request.userAge,
                userGender = request.userGender,
                userLocation = request.userLocation,
                userSegments = request.userSegments,
                stationId = request.stationId,
                adType = request.adType,
                placementContext = request.placementContext
            )

            if (advertisement != null) {
                // Get engagement ID from the impression that was just created
                val recentEngagements = engagementTrackingService.getRecentUserEngagements(
                    request.userId,
                    PageRequest.of(0, 1)
                )
                val engagementId = recentEngagements.content.firstOrNull()?.id

                ResponseEntity.ok(
                    AdServingResponse(
                        success = true,
                        advertisement = advertisement.toDto(),
                        engagementId = engagementId,
                        eligibleCount = 1
                    )
                )
            } else {
                ResponseEntity.ok(
                    AdServingResponse(
                        success = false,
                        message = "No eligible advertisements found",
                        eligibleCount = 0
                    )
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to serve advertisement to user ${request.userId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AdServingResponse(
                    success = false,
                    message = "Failed to serve advertisement: ${e.message}"
                )
            )
        }
    }

    /**
     * Get eligible advertisements for a user
     */
    @GetMapping("/eligible")
    fun getEligibleAdvertisements(
        @RequestParam userId: Long,
        @RequestParam(required = false) userAge: Int?,
        @RequestParam(required = false) userGender: String?,
        @RequestParam(required = false) userLocation: String?,
        @RequestParam(required = false) userSegments: List<String>?,
        @RequestParam(required = false) stationId: Long?,
        @RequestParam(required = false) adType: AdType?,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<AdvertisementDto>> {
        logger.debug("Getting eligible advertisements for user $userId")

        val advertisements = adService.getEligibleAdvertisements(
            userId = userId,
            userAge = userAge,
            userGender = userGender,
            userLocation = userLocation,
            userSegments = userSegments ?: emptyList(),
            stationId = stationId,
            adType = adType,
            limit = limit
        )

        val advertisementDtos = advertisements.map { it.toDto() }
        return ResponseEntity.ok(advertisementDtos)
    }

    /**
     * Create a new advertisement
     */
    @PostMapping
    fun createAdvertisement(
        @Valid @RequestBody request: CreateAdvertisementRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Creating new advertisement: ${request.title}")

        return try {
            val advertisement = request.toEntity(createdBy = userId)
            val savedAd = adService.createAdvertisement(advertisement)

            ResponseEntity.status(HttpStatus.CREATED).body(savedAd.toDto())

        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid advertisement creation request: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Failed to create advertisement", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Update an existing advertisement
     */
    @PutMapping("/{id}")
    fun updateAdvertisement(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAdvertisementRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Updating advertisement with ID: $id")

        return try {
            val advertisement = request.toEntity(updatedBy = userId)
            val updatedAd = adService.updateAdvertisement(id, advertisement)

            ResponseEntity.ok(updatedAd.toDto())

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot update advertisement $id: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Failed to update advertisement $id", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get advertisement by ID
     */
    @GetMapping("/{id}")
    fun getAdvertisementById(@PathVariable id: Long): ResponseEntity<AdvertisementDto> {
        logger.debug("Getting advertisement with ID: $id")

        val advertisement = adService.getAdvertisementById(id)
        return if (advertisement != null) {
            ResponseEntity.ok(advertisement.toDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get advertisements by campaign
     */
    @GetMapping("/campaigns/{campaignId}")
    fun getAdvertisementsByCampaign(
        @PathVariable campaignId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<Page<AdvertisementDto>> {
        logger.debug("Getting advertisements for campaign $campaignId")

        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }

        val pageable = PageRequest.of(page, size, sort)
        val advertisements = adService.getAdvertisementsByCampaign(campaignId, pageable)
        val advertisementDtos = advertisements.map { it.toDto() }

        return ResponseEntity.ok(advertisementDtos)
    }

    /**
     * Activate an advertisement
     */
    @PostMapping("/{id}/activate")
    fun activateAdvertisement(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Activating advertisement with ID: $id")

        return try {
            val advertisement = adService.activateAdvertisement(id, userId)
            ResponseEntity.ok(advertisement.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot activate advertisement $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Pause an advertisement
     */
    @PostMapping("/{id}/pause")
    fun pauseAdvertisement(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Pausing advertisement with ID: $id")

        return try {
            val advertisement = adService.pauseAdvertisement(id, userId)
            ResponseEntity.ok(advertisement.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot pause advertisement $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Complete an advertisement
     */
    @PostMapping("/{id}/complete")
    fun completeAdvertisement(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Completing advertisement with ID: $id")

        return try {
            val advertisement = adService.completeAdvertisement(id, userId)
            ResponseEntity.ok(advertisement.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get advertisement statistics
     */
    @GetMapping("/{id}/statistics")
    fun getAdvertisementStatistics(@PathVariable id: Long): ResponseEntity<AdvertisementStatisticsDto> {
        logger.debug("Getting statistics for advertisement with ID: $id")

        return try {
            val stats = adService.getAdvertisementStatistics(id)
            val statsDto = stats.toStatisticsDto()
            ResponseEntity.ok(statsDto)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Search advertisements
     */
    @GetMapping("/search")
    fun searchAdvertisements(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<AdvertisementDto>> {
        logger.debug("Searching advertisements with query: $query")

        val pageable = PageRequest.of(page, size, Sort.by("title").ascending())
        val advertisements = adService.searchAdvertisements(query, pageable)
        val advertisementDtos = advertisements.map { it.toDto() }

        return ResponseEntity.ok(advertisementDtos)
    }

    /**
     * Get advertisements expiring soon
     */
    @GetMapping("/expiring")
    fun getAdvertisementsExpiringSoon(
        @RequestParam(defaultValue = "24") hours: Long
    ): ResponseEntity<List<AdvertisementDto>> {
        logger.debug("Getting advertisements expiring in $hours hours")

        val advertisements = adService.getAdvertisementsExpiringSoon(hours)
        val advertisementDtos = advertisements.map { it.toDto() }

        return ResponseEntity.ok(advertisementDtos)
    }

    /**
     * Calculate potential ticket earnings
     */
    @GetMapping("/{id}/potential-earnings")
    fun calculatePotentialEarnings(
        @PathVariable id: Long,
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "1") baseTickets: Int
    ): ResponseEntity<PotentialEarnings> {
        logger.debug("Calculating potential earnings for ad $id, user $userId")

        return try {
            val earnings = ticketMultiplicationService.calculatePotentialEarnings(userId, id, baseTickets)
            ResponseEntity.ok(earnings)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Delete an advertisement
     */
    @DeleteMapping("/{id}")
    fun deleteAdvertisement(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("Deleting advertisement with ID: $id")

        return try {
            adService.deleteAdvertisement(id)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot delete advertisement $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }
}

// Extension functions for DTO conversion
private fun CreateAdvertisementRequest.toEntity(createdBy: String? = null): Advertisement {
    return Advertisement(
        campaignId = this.campaignId,
        title = this.title,
        description = this.description,
        adType = this.adType,
        contentUrl = this.contentUrl,
        thumbnailUrl = this.thumbnailUrl,
        clickThroughUrl = this.clickThroughUrl,
        durationSeconds = this.durationSeconds,
        startDate = this.startDate,
        endDate = this.endDate,
        priority = this.priority,
        dailyBudget = this.dailyBudget,
        totalBudget = this.totalBudget,
        costPerView = this.costPerView,
        costPerClick = this.costPerClick,
        maxImpressionsPerUser = this.maxImpressionsPerUser,
        maxDailyImpressions = this.maxDailyImpressions,
        ticketMultiplier = this.ticketMultiplier,
        bonusTicketsOnCompletion = this.bonusTicketsOnCompletion,
        requiresCompletionForBonus = this.requiresCompletionForBonus,
        minViewTimeForBonus = this.minViewTimeForBonus,
        targetAgeMin = this.targetAgeMin,
        targetAgeMax = this.targetAgeMax,
        targetGenders = this.targetGenders,
        targetLocations = this.targetLocations,
        targetStations = this.targetStations,
        targetUserSegments = this.targetUserSegments,
        excludeUserSegments = this.excludeUserSegments,
        allowedDaysOfWeek = this.allowedDaysOfWeek,
        allowedHoursStart = this.allowedHoursStart,
        allowedHoursEnd = this.allowedHoursEnd,
        advertiserName = this.advertiserName,
        advertiserContact = this.advertiserContact,
        tags = this.tags,
        notes = this.notes,
        createdBy = createdBy
    )
}

private fun UpdateAdvertisementRequest.toEntity(updatedBy: String? = null): Advertisement {
    return Advertisement(
        title = this.title,
        description = this.description,
        adType = AdType.BANNER, // Default, will be overridden by existing
        contentUrl = this.contentUrl,
        thumbnailUrl = this.thumbnailUrl,
        clickThroughUrl = this.clickThroughUrl,
        durationSeconds = this.durationSeconds,
        startDate = this.startDate,
        endDate = this.endDate,
        priority = this.priority,
        dailyBudget = this.dailyBudget,
        totalBudget = this.totalBudget,
        costPerView = this.costPerView,
        costPerClick = this.costPerClick,
        maxImpressionsPerUser = this.maxImpressionsPerUser,
        maxDailyImpressions = this.maxDailyImpressions,
        ticketMultiplier = this.ticketMultiplier,
        bonusTicketsOnCompletion = this.bonusTicketsOnCompletion,
        requiresCompletionForBonus = this.requiresCompletionForBonus,
        minViewTimeForBonus = this.minViewTimeForBonus,
        targetAgeMin = this.targetAgeMin,
        targetAgeMax = this.targetAgeMax,
        targetGenders = this.targetGenders,
        targetLocations = this.targetLocations,
        targetStations = this.targetStations,
        targetUserSegments = this.targetUserSegments,
        excludeUserSegments = this.excludeUserSegments,
        allowedDaysOfWeek = this.allowedDaysOfWeek,
        allowedHoursStart = this.allowedHoursStart,
        allowedHoursEnd = this.allowedHoursEnd,
        advertiserName = this.advertiserName,
        advertiserContact = this.advertiserContact,
        tags = this.tags,
        notes = this.notes,
        updatedBy = updatedBy,
        campaignId = 0 // Will be set from existing
    )
}

private fun Advertisement.toDto(): AdvertisementDto {
    return AdvertisementDto(
        id = this.id,
        campaignId = this.campaignId,
        title = this.title,
        description = this.description,
        adType = this.adType,
        status = this.status,
        contentUrl = this.contentUrl,
        thumbnailUrl = this.thumbnailUrl,
        clickThroughUrl = this.clickThroughUrl,
        durationSeconds = this.durationSeconds,
        startDate = this.startDate,
        endDate = this.endDate,
        priority = this.priority,
        dailyBudget = this.dailyBudget,
        totalBudget = this.totalBudget,
        costPerView = this.costPerView,
        costPerClick = this.costPerClick,
        maxImpressionsPerUser = this.maxImpressionsPerUser,
        maxDailyImpressions = this.maxDailyImpressions,
        totalImpressions = this.totalImpressions,
        totalClicks = this.totalClicks,
        totalCompletions = this.totalCompletions,
        totalSpend = this.totalSpend,
        ticketMultiplier = this.ticketMultiplier,
        bonusTicketsOnCompletion = this.bonusTicketsOnCompletion,
        requiresCompletionForBonus = this.requiresCompletionForBonus,
        minViewTimeForBonus = this.minViewTimeForBonus,
        targetAgeMin = this.targetAgeMin,
        targetAgeMax = this.targetAgeMax,
        targetGenders = this.targetGenders,
        targetLocations = this.targetLocations,
        targetStations = this.targetStations,
        targetUserSegments = this.targetUserSegments,
        excludeUserSegments = this.excludeUserSegments,
        allowedDaysOfWeek = this.allowedDaysOfWeek,
        allowedHoursStart = this.allowedHoursStart,
        allowedHoursEnd = this.allowedHoursEnd,
        advertiserName = this.advertiserName,
        advertiserContact = this.advertiserContact,
        tags = this.tags,
        notes = this.notes,
        createdBy = this.createdBy,
        updatedBy = this.updatedBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isActiveAndScheduled = this.isActiveAndScheduled(),
        hasBudgetRemaining = this.hasBudgetRemaining(),
        clickThroughRate = this.getClickThroughRate(),
        completionRate = this.getCompletionRate(),
        effectiveCPM = this.getEffectiveCPM(),
        remainingBudget = this.getRemainingBudget(),
        providesTicketBonus = this.providesTicketBonus()
    )
}

private fun Map<String, Any>.toStatisticsDto(): AdvertisementStatisticsDto {
    val adInfo = this["advertisement"] as Map<String, Any>
    val engagementStats = this["engagements"] as Map<String, Any>

    return AdvertisementStatisticsDto(
        advertisement = AdvertisementBasicInfo(
            id = adInfo["id"] as Long,
            title = adInfo["title"] as String,
            status = adInfo["status"] as AdStatus,
            type = adInfo["type"] as AdType,
            priority = adInfo["priority"] as Int,
            startDate = adInfo["startDate"] as java.time.LocalDateTime,
            endDate = adInfo["endDate"] as java.time.LocalDateTime,
            campaignId = 0 // Would need to be included in stats
        ),
        engagements = EngagementStatistics(
            totalEngagements = engagementStats["totalEngagements"] as Long,
            impressions = engagementStats["impressions"] as Long,
            clicks = engagementStats["clicks"] as Long,
            completions = engagementStats["completions"] as Long,
            ticketAwards = engagementStats["ticketAwards"] as Long,
            totalTicketsEarned = engagementStats["totalTicketsEarned"] as Long,
            totalCost = engagementStats["totalCost"] as java.math.BigDecimal,
            avgViewDuration = engagementStats["avgViewDuration"] as Double,
            avgCompletionPercentage = engagementStats["avgCompletionPercentage"] as Double
        ),
        performance = PerformanceMetrics(
            clickThroughRate = adInfo["clickThroughRate"] as Double,
            completionRate = adInfo["completionRate"] as Double,
            effectiveCPM = adInfo["effectiveCPM"] as java.math.BigDecimal,
            costPerClick = java.math.BigDecimal.ZERO,
            costPerCompletion = java.math.BigDecimal.ZERO,
            engagementQualityScore = 0.0
        ),
        targeting = TargetingMetrics(
            uniqueUsers = 0,
            repeatUsers = 0,
            avgEngagementsPerUser = 0.0,
            topUserSegments = emptyList(),
            topLocations = emptyList(),
            hourlyDistribution = emptyMap()
        )
    )
}rtisements.map { it.toDto() }

        return ResponseEntity.ok(advertisementDtos)
    }

    /**
     * Activate an advertisement
     */
    @PostMapping("/{id}/activate")
    fun activateAdvertisement(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Activating advertisement with ID: $id")

        return try {
            val advertisement = adService.activateAdvertisement(id, userId)
            ResponseEntity.ok(advertisement.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot activate advertisement $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Pause an advertisement
     */
    @PostMapping("/{id}/pause")
    fun pauseAdvertisement(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Pausing advertisement with ID: $id")

        return try {
            val advertisement = adService.pauseAdvertisement(id, userId)
            ResponseEntity.ok(advertisement.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot pause advertisement $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Complete an advertisement
     */
    @PostMapping("/{id}/complete")
    fun completeAdvertisement(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<AdvertisementDto> {
        logger.info("Completing advertisement with ID: $id")

        return try {
            val advertisement = adService.completeAdvertisement(id, userId)
            ResponseEntity.ok(advertisement.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get advertisement statistics
     */
    @GetMapping("/{id}/statistics")
    fun getAdvertisementStatistics(@PathVariable id: Long): ResponseEntity<AdvertisementStatisticsDto> {
        logger.debug("Getting statistics for advertisement with ID: $id")

        return try {
            val stats = adService.getAdvertisementStatistics(id)
            val statsDto = stats.toStatisticsDto()
            ResponseEntity.ok(statsDto)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Delete an advertisement
     */
    @DeleteMapping("/{id}")
    fun deleteAdvertisement(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("Deleting advertisement with ID: $id")

        return try {
            adService.deleteAdvertisement(id)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot delete advertisement $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Get advertisements expiring soon
     */
    @GetMapping("/expiring")
    fun getAdvertisementsExpiringSoon(
        @RequestParam(defaultValue = "24") hours: Long
    ): ResponseEntity<List<AdvertisementDto>> {
        logger.debug("Getting advertisements expiring in $hours hours")

        val advertisements = adService.getAdvertisementsExpiringSoon(hours)
        val advertisementDtos = advertisements.map { it.toDto() }

        return ResponseEntity.ok(advertisementDtos)
    }
}

// Extension functions for DTO conversion
private fun CreateAdvertisementRequest.toEntity(createdBy: String? = null): Advertisement {
    return Advertisement(
        campaignId = this.campaignId,
        title = this.title,
        description = this.description,
        adType = this.adType,
        contentUrl = this.contentUrl,
        thumbnailUrl = this.thumbnailUrl,
        clickThroughUrl = this.clickThroughUrl,
        durationSeconds = this.durationSeconds,
        startDate = this.startDate,
        endDate = this.endDate,
        priority = this.priority,
        dailyBudget = this.dailyBudget,
        totalBudget = this.totalBudget,
        costPerView = this.costPerView,
        costPerClick = this.costPerClick,
        maxImpressionsPerUser = this.maxImpressionsPerUser,
        maxDailyImpressions = this.maxDailyImpressions,
        ticketMultiplier = this.ticketMultiplier,
        bonusTicketsOnCompletion = this.bonusTicketsOnCompletion,
        requiresCompletionForBonus = this.requiresCompletionForBonus,
        minViewTimeForBonus = this.minViewTimeForBonus,
        targetAgeMin = this.targetAgeMin,
        targetAgeMax = this.targetAgeMax,
        targetGenders = this.targetGenders,
        targetLocations = this.targetLocations,
        targetStations = this.targetStations,
        targetUserSegments = this.targetUserSegments,
        excludeUserSegments = this.excludeUserSegments,
        allowedDaysOfWeek = this.allowedDaysOfWeek,
        allowedHoursStart = this.allowedHoursStart,
        allowedHoursEnd = this.allowedHoursEnd,
        advertiserName = this.advertiserName,
        advertiserContact = this.advertiserContact,
        tags = this.tags,
        notes = this.notes,
        createdBy = createdBy
    )
}

private fun UpdateAdvertisementRequest.toEntity(updatedBy: String? = null): Advertisement {
    return Advertisement(
        title = this.title,
        description = this.description,
        adType = AdType.BANNER, // Default, will be overridden
        contentUrl = this.contentUrl,
        thumbnailUrl = this.thumbnailUrl,
        clickThroughUrl = this.clickThroughUrl,
        durationSeconds = this.durationSeconds,
        startDate = this.startDate,
        endDate = this.endDate,
        priority = this.priority,
        dailyBudget = this.dailyBudget,
        totalBudget = this.totalBudget,
        costPerView = this.costPerView,
        costPerClick = this.costPerClick,
        maxImpressionsPerUser = this.maxImpressionsPerUser,
        maxDailyImpressions = this.maxDailyImpressions,
        ticketMultiplier = this.ticketMultiplier,
        bonusTicketsOnCompletion = this.bonusTicketsOnCompletion,
        requiresCompletionForBonus = this.requiresCompletionForBonus,
        minViewTimeForBonus = this.minViewTimeForBonus,
        targetAgeMin = this.targetAgeMin,
        targetAgeMax = this.targetAgeMax,
        targetGenders = this.targetGenders,
        targetLocations = this.targetLocations,
        targetStations = this.targetStations,
        targetUserSegments = this.targetUserSegments,
        excludeUserSegments = this.excludeUserSegments,
        allowedDaysOfWeek = this.allowedDaysOfWeek,
        allowedHoursStart = this.allowedHoursStart,
        allowedHoursEnd = this.allowedHoursEnd,
        advertiserName = this.advertiserName,
        advertiserContact = this.advertiserContact,
        tags = this.tags,
        notes = this.notes,
        updatedBy = updatedBy,
        campaignId = 0 // Will be set from existing
    )
}

private fun Advertisement.toDto(): AdvertisementDto {
    return AdvertisementDto(
        id = this.id,
        campaignId = this.campaignId,
        title = this.title,
        description = this.description,
        adType = this.adType,
        status = this.status,
        contentUrl = this.contentUrl,
        thumbnailUrl = this.thumbnailUrl,
        clickThroughUrl = this.clickThroughUrl,
        durationSeconds = this.durationSeconds,
        startDate = this.startDate,
        endDate = this.endDate,
        priority = this.priority,
        dailyBudget = this.dailyBudget,
        totalBudget = this.totalBudget,
        costPerView = this.costPerView,
        costPerClick = this.costPerClick,
        maxImpressionsPerUser = this.maxImpressionsPerUser,
        maxDailyImpressions = this.maxDailyImpressions,
        totalImpressions = this.totalImpressions,
        totalClicks = this.totalClicks,
        totalCompletions = this.totalCompletions,
        totalSpend = this.totalSpend,
        ticketMultiplier = this.ticketMultiplier,
        bonusTicketsOnCompletion = this.bonusTicketsOnCompletion,
        requiresCompletionForBonus = this.requiresCompletionForBonus,
        minViewTimeForBonus = this.minViewTimeForBonus,
        targetAgeMin = this.targetAgeMin,
        targetAgeMax = this.targetAgeMax,
        targetGenders = this.targetGenders,
        targetLocations = this.targetLocations,
        targetStations = this.targetStations,
        targetUserSegments = this.targetUserSegments,
        excludeUserSegments = this.excludeUserSegments,
        allowedDaysOfWeek = this.allowedDaysOfWeek,
        allowedHoursStart = this.allowedHoursStart,
        allowedHoursEnd = this.allowedHoursEnd,
        advertiserName = this.advertiserName,
        advertiserContact = this.advertiserContact,
        tags = this.tags,
        notes = this.notes,
        createdBy = this.createdBy,
        updatedBy = this.updatedBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isActiveAndScheduled = this.isActiveAndScheduled(),
        hasBudgetRemaining = this.hasBudgetRemaining(),
        clickThroughRate = this.getClickThroughRate(),
        completionRate = this.getCompletionRate(),
        effectiveCPM = this.getEffectiveCPM(),
        remainingBudget = this.getRemainingBudget(),
        providesTicketBonus = this.providesTicketBonus()
    )
}

private fun Map<String, Any>.toStatisticsDto(): AdvertisementStatisticsDto {
    val adInfo = this["advertisement"] as Map<String, Any>
    val engagementStats = this["engagements"] as Map<String, Any>

    return AdvertisementStatisticsDto(
        advertisement = AdvertisementBasicInfo(
            id = adInfo["id"] as Long,
            title = adInfo["title"] as String,
            status = adInfo["status"] as AdStatus,
            type = adInfo["type"] as AdType,
            priority = adInfo["priority"] as Int,
            startDate = adInfo["startDate"] as java.time.LocalDateTime,
            endDate = adInfo["endDate"] as java.time.LocalDateTime,
            campaignId = adInfo["campaignId"] as Long
        ),
        engagements = EngagementStatistics(
            totalEngagements = engagementStats["totalEngagements"] as Long,
            impressions = engagementStats["impressions"] as Long,
            clicks = engagementStats["clicks"] as Long,
            completions = engagementStats["completions"] as Long,
            ticketAwards = engagementStats["ticketAwards"] as Long,
            totalTicketsEarned = engagementStats["totalTicketsEarned"] as Long,
            totalCost = engagementStats["totalCost"] as java.math.BigDecimal,
            avgViewDuration = engagementStats["avgViewDuration"] as Double,
            avgCompletionPercentage = engagementStats["avgCompletionPercentage"] as Double
        ),
        performance = PerformanceMetrics(
            clickThroughRate = adInfo["clickThroughRate"] as Double,
            completionRate = adInfo["completionRate"] as Double,
            effectiveCPM = adInfo["effectiveCPM"] as java.math.BigDecimal,
            costPerClick = java.math.BigDecimal.ZERO,
            costPerCompletion = java.math.BigDecimal.ZERO,
            engagementQualityScore = 0.0
        ),
        targeting = TargetingMetrics(
            uniqueUsers = 0L,
            repeatUsers = 0L,
            avgEngagementsPerUser = 0.0,
            topUserSegments = emptyList(),
            topLocations = emptyList(),
            hourlyDistribution = emptyMap()
        )
    )
}