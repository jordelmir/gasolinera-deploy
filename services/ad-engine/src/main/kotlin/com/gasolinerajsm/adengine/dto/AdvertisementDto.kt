package com.gasolinerajsm.adengine.dto

import com.gasolinerajsm.adengine.model.AdStatus
import com.gasolinerajsm.adengine.model.AdType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for Advertisement entity
 */
data class AdvertisementDto(
    val id: Long = 0,
    val campaignId: Long,
    val title: String,
    val description: String? = null,
    val adType: AdType,
    val status: AdStatus,
    val contentUrl: String? = null,
    val thumbnailUrl: String? = null,
    val clickThroughUrl: String? = null,
    val durationSeconds: Int? = null,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val priority: Int,
    val dailyBudget: BigDecimal? = null,
    val totalBudget: BigDecimal? = null,
    val costPerView: BigDecimal? = null,
    val costPerClick: BigDecimal? = null,
    val maxImpressionsPerUser: Int? = null,
    val maxDailyImpressions: Int? = null,
    val totalImpressions: Long = 0,
    val totalClicks: Long = 0,
    val totalCompletions: Long = 0,
    val totalSpend: BigDecimal = BigDecimal.ZERO,
    val ticketMultiplier: BigDecimal? = null,
    val bonusTicketsOnCompletion: Int = 0,
    val requiresCompletionForBonus: Boolean = true,
    val minViewTimeForBonus: Int? = null,
    val targetAgeMin: Int? = null,
    val targetAgeMax: Int? = null,
    val targetGenders: String? = null,
    val targetLocations: String? = null,
    val targetStations: String? = null,
    val targetUserSegments: String? = null,
    val excludeUserSegments: String? = null,
    val allowedDaysOfWeek: String? = null,
    val allowedHoursStart: Int? = null,
    val allowedHoursEnd: Int? = null,
    val advertiserName: String? = null,
    val advertiserContact: String? = null,
    val tags: String? = null,
    val notes: String? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,

    // Computed fields
    val isActiveAndScheduled: Boolean = false,
    val hasBudgetRemaining: Boolean = true,
    val clickThroughRate: Double = 0.0,
    val completionRate: Double = 0.0,
    val effectiveCPM: BigDecimal = BigDecimal.ZERO,
    val remainingBudget: BigDecimal? = null,
    val providesTicketBonus: Boolean = false
)

/**
 * DTO for creating a new advertisement
 */
data class CreateAdvertisementRequest(
    @field:NotNull(message = "Campaign ID is required")
    val campaignId: Long,

    @field:NotBlank(message = "Title is required")
    @field:Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    val title: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @field:NotNull(message = "Ad type is required")
    val adType: AdType,

    val contentUrl: String? = null,
    val thumbnailUrl: String? = null,
    val clickThroughUrl: String? = null,

    @field:Min(value = 1, message = "Duration must be at least 1 second")
    val durationSeconds: Int? = null,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDateTime,

    @field:NotNull(message = "End date is required")
    val endDate: LocalDateTime,

    @field:Min(value = 1, message = "Priority must be at least 1")
    @field:Max(value = 10, message = "Priority cannot exceed 10")
    val priority: Int = 5,

    @field:DecimalMin(value = "0.0", message = "Daily budget must be positive")
    val dailyBudget: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Total budget must be positive")
    val totalBudget: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cost per view must be positive")
    val costPerView: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cost per click must be positive")
    val costPerClick: BigDecimal? = null,

    @field:Min(value = 1, message = "Max impressions per user must be at least 1")
    val maxImpressionsPerUser: Int? = null,

    @field:Min(value = 1, message = "Max daily impressions must be at least 1")
    val maxDailyImpressions: Int? = null,

    @field:DecimalMin(value = "1.0", message = "Ticket multiplier must be at least 1.0")
    @field:DecimalMax(value = "10.0", message = "Ticket multiplier cannot exceed 10.0")
    val ticketMultiplier: BigDecimal? = null,

    @field:Min(value = 0, message = "Bonus tickets must be non-negative")
    val bonusTicketsOnCompletion: Int = 0,

    val requiresCompletionForBonus: Boolean = true,

    @field:Min(value = 0, message = "Min view time must be non-negative")
    val minViewTimeForBonus: Int? = null,

    @field:Min(value = 13, message = "Minimum age must be at least 13")
    val targetAgeMin: Int? = null,

    @field:Max(value = 120, message = "Maximum age cannot exceed 120")
    val targetAgeMax: Int? = null,

    val targetGenders: String? = null,
    val targetLocations: String? = null,
    val targetStations: String? = null,
    val targetUserSegments: String? = null,
    val excludeUserSegments: String? = null,
    val allowedDaysOfWeek: String? = null,

    @field:Min(value = 0, message = "Start hour must be between 0 and 23")
    @field:Max(value = 23, message = "Start hour must be between 0 and 23")
    val allowedHoursStart: Int? = null,

    @field:Min(value = 0, message = "End hour must be between 0 and 23")
    @field:Max(value = 23, message = "End hour must be between 0 and 23")
    val allowedHoursEnd: Int? = null,

    val advertiserName: String? = null,
    val advertiserContact: String? = null,
    val tags: String? = null,
    val notes: String? = null
)

/**
 * DTO for updating an advertisement
 */
data class UpdateAdvertisementRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    val title: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    val contentUrl: String? = null,
    val thumbnailUrl: String? = null,
    val clickThroughUrl: String? = null,

    @field:Min(value = 1, message = "Duration must be at least 1 second")
    val durationSeconds: Int? = null,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDateTime,

    @field:NotNull(message = "End date is required")
    val endDate: LocalDateTime,

    @field:Min(value = 1, message = "Priority must be at least 1")
    @field:Max(value = 10, message = "Priority cannot exceed 10")
    val priority: Int = 5,

    @field:DecimalMin(value = "0.0", message = "Daily budget must be positive")
    val dailyBudget: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Total budget must be positive")
    val totalBudget: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cost per view must be positive")
    val costPerView: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cost per click must be positive")
    val costPerClick: BigDecimal? = null,

    @field:Min(value = 1, message = "Max impressions per user must be at least 1")
    val maxImpressionsPerUser: Int? = null,

    @field:Min(value = 1, message = "Max daily impressions must be at least 1")
    val maxDailyImpressions: Int? = null,

    @field:DecimalMin(value = "1.0", message = "Ticket multiplier must be at least 1.0")
    @field:DecimalMax(value = "10.0", message = "Ticket multiplier cannot exceed 10.0")
    val ticketMultiplier: BigDecimal? = null,

    @field:Min(value = 0, message = "Bonus tickets must be non-negative")
    val bonusTicketsOnCompletion: Int = 0,

    val requiresCompletionForBonus: Boolean = true,

    @field:Min(value = 0, message = "Min view time must be non-negative")
    val minViewTimeForBonus: Int? = null,

    @field:Min(value = 13, message = "Minimum age must be at least 13")
    val targetAgeMin: Int? = null,

    @field:Max(value = 120, message = "Maximum age cannot exceed 120")
    val targetAgeMax: Int? = null,

    val targetGenders: String? = null,
    val targetLocations: String? = null,
    val targetStations: String? = null,
    val targetUserSegments: String? = null,
    val excludeUserSegments: String? = null,
    val allowedDaysOfWeek: String? = null,

    @field:Min(value = 0, message = "Start hour must be between 0 and 23")
    @field:Max(value = 23, message = "Start hour must be between 0 and 23")
    val allowedHoursStart: Int? = null,

    @field:Min(value = 0, message = "End hour must be between 0 and 23")
    @field:Max(value = 23, message = "End hour must be between 0 and 23")
    val allowedHoursEnd: Int? = null,

    val advertiserName: String? = null,
    val advertiserContact: String? = null,
    val tags: String? = null,
    val notes: String? = null
)

/**
 * DTO for advertisement serving request
 */
data class AdServingRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    val sessionId: String? = null,
    val userAge: Int? = null,
    val userGender: String? = null,
    val userLocation: String? = null,
    val userSegments: List<String> = emptyList(),
    val stationId: Long? = null,
    val adType: AdType? = null,
    val placementContext: String? = null,
    val limit: Int = 1
)

/**
 * DTO for advertisement serving response
 */
data class AdServingResponse(
    val success: Boolean,
    val advertisement: AdvertisementDto? = null,
    val message: String? = null,
    val engagementId: Long? = null,
    val eligibleCount: Int = 0
)

/**
 * DTO for advertisement statistics
 */
data class AdvertisementStatisticsDto(
    val advertisement: AdvertisementBasicInfo,
    val engagements: EngagementStatistics,
    val performance: PerformanceMetrics,
    val targeting: TargetingMetrics
)

data class AdvertisementBasicInfo(
    val id: Long,
    val title: String,
    val status: AdStatus,
    val type: AdType,
    val priority: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val campaignId: Long
)

data class EngagementStatistics(
    val totalEngagements: Long,
    val impressions: Long,
    val clicks: Long,
    val completions: Long,
    val ticketAwards: Long,
    val totalTicketsEarned: Long,
    val totalCost: BigDecimal,
    val avgViewDuration: Double,
    val avgCompletionPercentage: Double
)

data class PerformanceMetrics(
    val clickThroughRate: Double,
    val completionRate: Double,
    val effectiveCPM: BigDecimal,
    val costPerClick: BigDecimal,
    val costPerCompletion: BigDecimal,
    val engagementQualityScore: Double
)

data class TargetingMetrics(
    val uniqueUsers: Long,
    val repeatUsers: Long,
    val avgEngagementsPerUser: Double,
    val topUserSegments: List<String>,
    val topLocations: List<String>,
    val hourlyDistribution: Map<Int, Long>
)