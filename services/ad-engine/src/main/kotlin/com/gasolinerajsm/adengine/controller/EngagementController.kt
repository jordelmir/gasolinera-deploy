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
 * REST Controller for advertisement engagement tracking
 */
@RestController
@RequestMapping("/api/v1/engagements")
@CrossOrigin(origins = ["*"])
class EngagementController(
    private val engagementTrackingService: EngagementTrackingService,
    private val ticketMultiplicationService: TicketMultiplicationService
) {
    private val logger = LoggerFactory.getLogger(EngagementController::class.java)

    /**
     * Track advertisement view
     */
    @PostMapping("/view")
    fun trackView(@Valid @RequestBody request: TrackViewRequest): ResponseEntity<EngagementTrackingResponse> {
        logger.debug("Tracking view for engagement ${request.engagementId}")

        return try {
            val engagement = engagementTrackingService.trackView(
                engagementId = request.engagementId,
                viewDurationSeconds = request.viewDurationSeconds
            )

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "View tracked successfully"
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to track view for engagement ${request.engagementId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to track view: ${e.message}"
                )
            )
        }
    }

    /**
     * Track advertisement click
     */
    @PostMapping("/click")
    fun trackClick(@Valid @RequestBody request: TrackClickRequest): ResponseEntity<EngagementTrackingResponse> {
        logger.debug("Tracking click for engagement ${request.engagementId}")

        return try {
            val engagement = engagementTrackingService.trackClick(
                engagementId = request.engagementId,
                clickThroughUrl = request.clickThroughUrl
            )

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "Click tracked successfully"
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to track click for engagement ${request.engagementId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to track click: ${e.message}"
                )
            )
        }
    }

    /**
     * Track advertisement interaction
     */
    @PostMapping("/interaction")
    fun trackInteraction(@Valid @RequestBody request: TrackInteractionRequest): ResponseEntity<EngagementTrackingResponse> {
        logger.debug("Tracking interaction for engagement ${request.engagementId}")

        return try {
            val engagement = engagementTrackingService.trackInteraction(
                engagementId = request.engagementId,
                interactionType = request.interactionType
            )

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "Interaction tracked successfully"
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to track interaction for engagement ${request.engagementId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to track interaction: ${e.message}"
                )
            )
        }
    }

    /**
     * Track advertisement completion
     */
    @PostMapping("/completion")
    fun trackCompletion(@Valid @RequestBody request: TrackCompletionRequest): ResponseEntity<EngagementTrackingResponse> {
        logger.info("Tracking completion for engagement ${request.engagementId}")

        return try {
            val engagement = engagementTrackingService.trackCompletion(
                engagementId = request.engagementId,
                completionPercentage = request.completionPercentage,
                viewDurationSeconds = request.viewDurationSeconds
            )

            // Calculate and award tickets if eligible
            val ticketResult = try {
                ticketMultiplicationService.awardBonusTickets(
                    engagementId = request.engagementId,
                    baseTickets = 1 // Default base tickets
                )
            } catch (e: Exception) {
                logger.warn("Failed to award tickets for engagement ${request.engagementId}: ${e.message}")
                null
            }

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "Completion tracked successfully",
                    ticketsAwarded = ticketResult?.success ?: false,
                    totalTickets = ticketResult?.totalTickets ?: 0
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to track completion for engagement ${request.engagementId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to track completion: ${e.message}"
                )
            )
        }
    }

    /**
     * Track engagement progress
     */
    @PostMapping("/progress")
    fun trackProgress(@Valid @RequestBody request: TrackProgressRequest): ResponseEntity<EngagementTrackingResponse> {
        logger.debug("Tracking progress for engagement ${request.engagementId}")

        return try {
            val engagement = engagementTrackingService.updateEngagementProgress(
                engagementId = request.engagementId,
                viewDurationSeconds = request.viewDurationSeconds,
                completionPercentage = request.completionPercentage,
                interactions = request.interactions,
                pauses = request.pauses,
                replays = request.replays
            )

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "Progress tracked successfully"
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to track progress for engagement ${request.engagementId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to track progress: ${e.message}"
                )
            )
        }
    }

    /**
     * Track engagement skip
     */
    @PostMapping("/{engagementId}/skip")
    fun trackSkip(@PathVariable engagementId: Long): ResponseEntity<EngagementTrackingResponse> {
        logger.debug("Tracking skip for engagement $engagementId")

        return try {
            val engagement = engagementTrackingService.trackSkip(engagementId)

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "Skip tracked successfully"
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to track skip for engagement $engagementId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to track skip: ${e.message}"
                )
            )
        }
    }

    /**
     * Track engagement error
     */
    @PostMapping("/error")
    fun trackError(@Valid @RequestBody request: TrackErrorRequest): ResponseEntity<EngagementTrackingResponse> {
        logger.warn("Tracking error for engagement ${request.engagementId}: ${request.errorMessage}")

        return try {
            val engagement = engagementTrackingService.trackError(
                engagementId = request.engagementId,
                errorMessage = request.errorMessage,
                errorCode = request.errorCode
            )

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "Error tracked successfully"
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to track error for engagement ${request.engagementId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to track error: ${e.message}"
                )
            )
        }
    }

    /**
     * Award tickets manually
     */
    @PostMapping("/award-tickets")
    fun awardTickets(@Valid @RequestBody request: AwardTicketsRequest): ResponseEntity<EngagementTrackingResponse> {
        logger.info("Manually awarding tickets for engagement ${request.engagementId}")

        return try {
            val engagement = engagementTrackingService.awardTickets(
                engagementId = request.engagementId,
                baseTickets = request.baseTickets,
                bonusTickets = request.bonusTickets,
                raffleEntryId = request.raffleEntryId
            )

            ResponseEntity.ok(
                EngagementTrackingResponse(
                    success = true,
                    engagement = engagement.toDto(),
                    message = "Tickets awarded successfully",
                    ticketsAwarded = true,
                    totalTickets = request.baseTickets + request.bonusTickets
                )
            )

        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot award tickets for engagement ${request.engagementId}: ${e.message}")
            ResponseEntity.badRequest().body(
                EngagementTrackingResponse(
                    success = false,
                    message = e.message ?: "Cannot award tickets"
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to award tickets for engagement ${request.engagementId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                EngagementTrackingResponse(
                    success = false,
                    message = "Failed to award tickets: ${e.message}"
                )
            )
        }
    }

    /**
     * Get engagement by ID
     */
    @GetMapping("/{id}")
    fun getEngagementById(@PathVariable id: Long): ResponseEntity<AdEngagementDto> {
        logger.debug("Getting engagement with ID: $id")

        return try {
            val engagement = engagementTrackingService.getEngagementById(id)
            ResponseEntity.ok(engagement.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get user engagements
     */
    @GetMapping("/users/{userId}")
    fun getUserEngagements(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<Page<AdEngagementDto>> {
        logger.debug("Getting engagements for user $userId")

        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }

        val pageable = PageRequest.of(page, size, sort)
        val engagements = engagementTrackingService.getUserEngagements(userId, pageable)
        val engagementDtos = engagements.map { it.toDto() }

        return ResponseEntity.ok(engagementDtos)
    }

    /**
     * Get advertisement engagements
     */
    @GetMapping("/advertisements/{advertisementId}")
    fun getAdvertisementEngagements(
        @PathVariable advertisementId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<Page<AdEngagementDto>> {
        logger.debug("Getting engagements for advertisement $advertisementId")

        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }

        val pageable = PageRequest.of(page, size, sort)
        val engagements = engagementTrackingService.getAdvertisementEngagements(advertisementId, pageable)
        val engagementDtos = engagements.map { it.toDto() }

        return ResponseEntity.ok(engagementDtos)
    }

    /**
     * Get user engagement statistics
     */
    @GetMapping("/users/{userId}/statistics")
    fun getUserEngagementStatistics(@PathVariable userId: Long): ResponseEntity<UserEngagementStatisticsDto> {
        logger.debug("Getting engagement statistics for user $userId")

        val stats = engagementTrackingService.getUserEngagementStatistics(userId)
        val statsDto = stats.toUserEngagementStatisticsDto()

        return ResponseEntity.ok(statsDto)
    }

    /**
     * Get daily engagement statistics
     */
    @GetMapping("/statistics/daily")
    fun getDailyEngagementStatistics(): ResponseEntity<DailyEngagementStatisticsDto> {
        logger.debug("Getting daily engagement statistics")

        val stats = engagementTrackingService.getDailyEngagementStatistics()
        val statsDto = stats.toDailyEngagementStatisticsDto()

        return ResponseEntity.ok(statsDto)
    }

    /**
     * Get engagement conversion funnel
     */
    @GetMapping("/advertisements/{advertisementId}/funnel")
    fun getEngagementFunnel(
        @PathVariable advertisementId: Long,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<EngagementFunnelDto> {
        logger.debug("Getting engagement funnel for advertisement $advertisementId")

        return try {
            val start = java.time.LocalDateTime.parse(startDate)
            val end = java.time.LocalDateTime.parse(endDate)

            val funnel = engagementTrackingService.getEngagementFunnel(advertisementId, start, end)
            val funnelDto = funnel.toEngagementFunnelDto(advertisementId, start, end)

            ResponseEntity.ok(funnelDto)
        } catch (e: Exception) {
            logger.error("Failed to get engagement funnel for advertisement $advertisementId", e)
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Process pending ticket awards
     */
    @PostMapping("/process-pending-awards")
    fun processPendingTicketAwards(
        @RequestParam(required = false) raffleId: Long?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Processing pending ticket awards")

        val results = ticketMultiplicationService.processPendingTicketAwards(raffleId)
        val successCount = results.count { it.success }

        val response = mapOf(
            "success" to true,
            "message" to "Processed ${results.size} pending awards",
            "totalProcessed" to results.size,
            "successful" to successCount,
            "failed" to (results.size - successCount),
            "results" to results
        )

        return ResponseEntity.ok(response)
    }
}

// Extension functions for DTO conversion
private fun AdEngagement.toDto(): AdEngagementDto {
    return AdEngagementDto(
        id = this.id,
        userId = this.userId,
        advertisementId = this.advertisement.id,
        advertisementTitle = this.advertisement.title,
        sessionId = this.sessionId,
        engagementType = this.engagementType,
        status = this.status,
        startedAt = this.startedAt,
        completedAt = this.completedAt,
        viewDurationSeconds = this.viewDurationSeconds,
        completionPercentage = this.completionPercentage,
        clicked = this.clicked,
        clickedAt = this.clickedAt,
        clickThroughUrl = this.clickThroughUrl,
        stationId = this.stationId,
        deviceType = this.deviceType,
        deviceId = this.deviceId,
        ipAddress = this.ipAddress,
        userAgent = this.userAgent,
        locationLatitude = this.locationLatitude,
        locationLongitude = this.locationLongitude,
        locationAccuracyMeters = this.locationAccuracyMeters,
        baseTicketsEarned = this.baseTicketsEarned,
        bonusTicketsEarned = this.bonusTicketsEarned,
        totalTicketsEarned = this.totalTicketsEarned,
        ticketsAwarded = this.ticketsAwarded,
        ticketsAwardedAt = this.ticketsAwardedAt,
        raffleEntryCreated = this.raffleEntryCreated,
        raffleEntryId = this.raffleEntryId,
        costCharged = this.costCharged,
        billingEvent = this.billingEvent,
        interactionsCount = this.interactionsCount,
        pauseCount = this.pauseCount,
        replayCount = this.replayCount,
        skipAttempted = this.skipAttempted,
        skipAllowed = this.skipAllowed,
        skippedAt = this.skippedAt,
        errorOccurred = this.errorOccurred,
        errorMessage = this.errorMessage,
        errorCode = this.errorCode,
        referrerUrl = this.referrerUrl,
        campaignContext = this.campaignContext,
        placementContext = this.placementContext,
        metadata = this.metadata,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isCompleted = this.isCompleted(),
        wasSkipped = this.wasSkipped(),
        hadError = this.hadError(),
        qualifiesForRewards = this.qualifiesForRewards(),
        hasTicketsAwarded = this.hasTicketsAwarded(),
        engagementDurationSeconds = this.getEngagementDurationSeconds(),
        engagementDurationMinutes = this.getEngagementDurationMinutes(),
        engagementQualityScore = this.getEngagementQualityScore(),
        formattedLocation = this.getFormattedLocation(),
        hasLocationData = this.hasLocationData()
    )
}

private fun Map<String, Any>.toUserEngagementStatisticsDto(): UserEngagementStatisticsDto {
    return UserEngagementStatisticsDto(
        totalEngagements = this["totalEngagements"] as Long,
        completedEngagements = this["completedEngagements"] as Long,
        clickedEngagements = this["clickedEngagements"] as Long,
        totalTicketsEarned = this["totalTicketsEarned"] as Long,
        uniqueAdsEngaged = this["uniqueAdsEngaged"] as Long,
        avgViewDuration = this["avgViewDuration"] as Double
    )
}

private fun Map<String, Any>.toDailyEngagementStatisticsDto(): DailyEngagementStatisticsDto {
    return DailyEngagementStatisticsDto(
        date = java.time.LocalDate.now(),
        totalEngagements = this["totalEngagements"] as Long,
        impressions = this["impressions"] as Long,
        clicks = this["clicks"] as Long,
        completions = this["completions"] as Long,
        uniqueUsers = this["uniqueUsers"] as Long,
        totalTicketsEarned = this["totalTicketsEarned"] as Long,
        totalCost = this["totalCost"] as java.math.BigDecimal,
        avgEngagementDuration = 0.0
    )
}

private fun Map<String, Any>.toEngagementFunnelDto(
    advertisementId: Long,
    startDate: java.time.LocalDateTime,
    endDate: java.time.LocalDateTime
): EngagementFunnelDto {
    val impressions = this["impressions"] as Long
    val views = this["views"] as Long
    val clicks = this["clicks"] as Long
    val completions = this["completions"] as Long

    return EngagementFunnelDto(
        advertisementId = advertisementId,
        advertisementTitle = "Advertisement $advertisementId",
        periodStart = startDate,
        periodEnd = endDate,
        impressions = impressions,
        views = views,
        clicks = clicks,
        completions = completions,
        viewRate = if (impressions > 0) (views.toDouble() / impressions.toDouble()) * 100 else 0.0,
        clickThroughRate = if (impressions > 0) (clicks.toDouble() / impressions.toDouble()) * 100 else 0.0,
        completionRate = if (impressions > 0) (completions.toDouble() / impressions.toDouble()) * 100 else 0.0
    )
}