package com.gasolinerajsm.adengine.dto

import com.gasolinerajsm.adengine.domain.model.EngagementStatus
import com.gasolinerajsm.adengine.domain.model.EngagementType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for AdEngagement entity
 */
data class AdEngagementDto(
    val id: Long = 0,
    val userId: Long,
    val advertisementId: Long,
    val advertisementTitle: String? = null,
    val sessionId: String? = null,
    val engagementType: EngagementType,
    val status: EngagementStatus,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val viewDurationSeconds: Int? = null,
    val completionPercentage: BigDecimal? = null,
    val clicked: Boolean = false,
    val clickedAt: LocalDateTime? = null,
    val clickThroughUrl: String? = null,
    val stationId: Long? = null,
    val deviceType: String? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val locationLatitude: BigDecimal? = null,
    val locationLongitude: BigDecimal? = null,
    val locationAccuracyMeters: Int? = null,
    val baseTicketsEarned: Int = 0,
    val bonusTicketsEarned: Int = 0,
    val totalTicketsEarned: Int = 0,
    val ticketsAwarded: Boolean = false,
    val ticketsAwardedAt: LocalDateTime? = null,
    val raffleEntryCreated: Boolean = false,
    val raffleEntryId: Long? = null,
    val costCharged: BigDecimal? = null,
    val billingEvent: String? = null,
    val interactionsCount: Int = 0,
    val pauseCount: Int = 0,
    val replayCount: Int = 0,
    val skipAttempted: Boolean = false,
    val skipAllowed: Boolean = false,
    val skippedAt: LocalDateTime? = null,
    val errorOccurred: Boolean = false,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val referrerUrl: String? = null,
    val campaignContext: String? = null,
    val placementContext: String? = null,
    val metadata: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,

    // Computed fields
    val isCompleted: Boolean = false,
    val wasSkipped: Boolean = false,
    val hadError: Boolean = false,
    val qualifiesForRewards: Boolean = false,
    val hasTicketsAwarded: Boolean = false,
    val engagementDurationSeconds: Long? = null,
    val engagementDurationMinutes: Double? = null,
    val engagementQualityScore: Double = 0.0,
    val formattedLocation: String? = null,
    val hasLocationData: Boolean = false
)

/**
 * DTO for tracking advertisement view
 */
data class TrackViewRequest(
    @field:NotNull(message = "Engagement ID is required")
    val engagementId: Long,

    @field:Min(value = 0, message = "View duration must be non-negative")
    val viewDurationSeconds: Int? = null
)

/**
 * DTO for tracking advertisement click
 */
data class TrackClickRequest(
    @field:NotNull(message = "Engagement ID is required")
    val engagementId: Long,

    val clickThroughUrl: String? = null
)

/**
 * DTO for tracking advertisement interaction
 */
data class TrackInteractionRequest(
    @field:NotNull(message = "Engagement ID is required")
    val engagementId: Long,

    val interactionType: String? = null
)

/**
 * DTO for tracking advertisement completion
 */
data class TrackCompletionRequest(
    @field:NotNull(message = "Engagement ID is required")
    val engagementId: Long,

    @field:DecimalMin(value = "0.0", message = "Completion percentage must be non-negative")
    @field:DecimalMax(value = "100.0", message = "Completion percentage cannot exceed 100")
    val completionPercentage: BigDecimal? = null,

    @field:Min(value = 0, message = "View duration must be non-negative")
    val viewDurationSeconds: Int? = null
)

/**
 * DTO for tracking engagement progress
 */
data class TrackProgressRequest(
    @field:NotNull(message = "Engagement ID is required")
    val engagementId: Long,

    @field:NotNull(message = "View duration is required")
    @field:Min(value = 0, message = "View duration must be non-negative")
    val viewDurationSeconds: Int,

    @field:DecimalMin(value = "0.0", message = "Completion percentage must be non-negative")
    @field:DecimalMax(value = "100.0", message = "Completion percentage cannot exceed 100")
    val completionPercentage: BigDecimal? = null,

    @field:Min(value = 0, message = "Interactions count must be non-negative")
    val interactions: Int = 0,

    @field:Min(value = 0, message = "Pauses count must be non-negative")
    val pauses: Int = 0,

    @field:Min(value = 0, message = "Replays count must be non-negative")
    val replays: Int = 0
)

/**
 * DTO for tracking engagement error
 */
data class TrackErrorRequest(
    @field:NotNull(message = "Engagement ID is required")
    val engagementId: Long,

    @field:NotBlank(message = "Error message is required")
    val errorMessage: String,

    val errorCode: String? = null
)

/**
 * DTO for awarding tickets
 */
data class AwardTicketsRequest(
    @field:NotNull(message = "Engagement ID is required")
    val engagementId: Long,

    @field:Min(value = 0, message = "Base tickets must be non-negative")
    val baseTickets: Int,

    @field:Min(value = 0, message = "Bonus tickets must be non-negative")
    val bonusTickets: Int,

    val raffleEntryId: Long? = null
)

/**
 * DTO for engagement tracking response
 */
data class EngagementTrackingResponse(
    val success: Boolean,
    val engagement: AdEngagementDto? = null,
    val message: String? = null,
    val ticketsAwarded: Boolean = false,
    val totalTickets: Int = 0
)

/**
 * DTO for user engagement statistics
 */
data class UserEngagementStatisticsDto(
    val totalEngagements: Long,
    val completedEngagements: Long,
    val clickedEngagements: Long,
    val totalTicketsEarned: Long,
    val uniqueAdsEngaged: Long,
    val avgViewDuration: Double,
    val engagementsByType: Map<EngagementType, Long> = emptyMap(),
    val engagementsByStatus: Map<EngagementStatus, Long> = emptyMap(),
    val recentEngagements: List<AdEngagementDto> = emptyList()
)

/**
 * DTO for daily engagement statistics
 */
data class DailyEngagementStatisticsDto(
    val date: java.time.LocalDate,
    val totalEngagements: Long,
    val impressions: Long,
    val clicks: Long,
    val completions: Long,
    val uniqueUsers: Long,
    val totalTicketsEarned: Long,
    val totalCost: BigDecimal,
    val avgEngagementDuration: Double,
    val topAdvertisements: List<TopAdvertisementDto> = emptyList()
)

/**
 * DTO for top performing advertisements
 */
data class TopAdvertisementDto(
    val advertisementId: Long,
    val title: String,
    val engagements: Long,
    val completions: Long,
    val completionRate: Double,
    val ticketsAwarded: Long
)

/**
 * DTO for engagement conversion funnel
 */
data class EngagementFunnelDto(
    val advertisementId: Long,
    val advertisementTitle: String,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val impressions: Long,
    val views: Long,
    val clicks: Long,
    val completions: Long,
    val viewRate: Double,
    val clickThroughRate: Double,
    val completionRate: Double
)