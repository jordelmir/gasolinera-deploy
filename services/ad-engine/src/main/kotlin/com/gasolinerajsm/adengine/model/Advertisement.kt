package com.gasolinerajsm.adengine.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Advertisement entity representing an advertisement campaign with targeting and multiplier rules
 */
@Entity
@Table(
    name = "advertisements",
    schema = "ad_engine_schema",
    indexes = [
        Index(name = "idx_advertisements_campaign_id", columnList = "campaign_id"),
        Index(name = "idx_advertisements_status", columnList = "status"),
        Index(name = "idx_advertisements_ad_type", columnList = "ad_type"),
        Index(name = "idx_advertisements_start_date", columnList = "start_date"),
        Index(name = "idx_advertisements_end_date", columnList = "end_date"),
        Index(name = "idx_advertisements_priority", columnList = "priority"),
        Index(name = "idx_advertisements_created_at", columnList = "created_at")
    ]
)
data class Advertisement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "campaign_id", nullable = false)
    @field:NotNull(message = "Campaign ID is required")
    val campaignId: Long,

    @Column(name = "title", nullable = false, length = 200)
    @field:NotBlank(message = "Advertisement title is required")
    @field:Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    val title: String,

    @Column(name = "description", length = 1000)
    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = false, length = 30)
    val adType: AdType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: AdStatus = AdStatus.DRAFT,

    @Column(name = "content_url", length = 500)
    val contentUrl: String? = null,

    @Column(name = "thumbnail_url", length = 500)
    val thumbnailUrl: String? = null,

    @Column(name = "click_through_url", length = 500)
    val clickThroughUrl: String? = null,

    @Column(name = "duration_seconds")
    @field:Min(value = 1, message = "Duration must be at least 1 second")
    val durationSeconds: Int? = null,

    @Column(name = "start_date", nullable = false)
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDateTime,

    @Column(name = "end_date", nullable = false)
    @field:NotNull(message = "End date is required")
    val endDate: LocalDateTime,

    @Column(name = "priority", nullable = false)
    @field:Min(value = 1, message = "Priority must be at least 1")
    @field:Max(value = 10, message = "Priority cannot exceed 10")
    val priority: Int = 5,

    @Column(name = "daily_budget", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Daily budget must be positive")
    val dailyBudget: BigDecimal? = null,

    @Column(name = "total_budget", precision = 12, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Total budget must be positive")
    val totalBudget: BigDecimal? = null,

    @Column(name = "cost_per_view", precision = 8, scale = 4)
    @field:DecimalMin(value = "0.0", message = "Cost per view must be positive")
    val costPerView: BigDecimal? = null,

    @Column(name = "cost_per_click", precision = 8, scale = 4)
    @field:DecimalMin(value = "0.0", message = "Cost per click must be positive")
    val costPerClick: BigDecimal? = null,

    @Column(name = "max_impressions_per_user")
    @field:Min(value = 1, message = "Max impressions per user must be at least 1")
    val maxImpressionsPerUser: Int? = null,

    @Column(name = "max_daily_impressions")
    @field:Min(value = 1, message = "Max daily impressions must be at least 1")
    val maxDailyImpressions: Int? = null,

    @Column(name = "total_impressions", nullable = false)
    val totalImpressions: Long = 0,

    @Column(name = "total_clicks", nullable = false)
    val totalClicks: Long = 0,

    @Column(name = "total_completions", nullable = false)
    val totalCompletions: Long = 0,

    @Column(name = "total_spend", precision = 12, scale = 2, nullable = false)
    val totalSpend: BigDecimal = BigDecimal.ZERO,

    // Ticket multiplier configuration
    @Column(name = "ticket_multiplier", precision = 4, scale = 2)
    @field:DecimalMin(value = "1.0", message = "Ticket multiplier must be at least 1.0")
    @field:DecimalMax(value = "10.0", message = "Ticket multiplier cannot exceed 10.0")
    val ticketMultiplier: BigDecimal? = null,

    @Column(name = "bonus_tickets_on_completion")
    @field:Min(value = 0, message = "Bonus tickets must be non-negative")
    val bonusTicketsOnCompletion: Int = 0,

    @Column(name = "requires_completion_for_bonus", nullable = false)
    val requiresCompletionForBonus: Boolean = true,

    @Column(name = "min_view_time_for_bonus")
    @field:Min(value = 0, message = "Min view time must be non-negative")
    val minViewTimeForBonus: Int? = null,

    // Targeting configuration
    @Column(name = "target_age_min")
    @field:Min(value = 13, message = "Minimum age must be at least 13")
    val targetAgeMin: Int? = null,

    @Column(name = "target_age_max")
    @field:Max(value = 120, message = "Maximum age cannot exceed 120")
    val targetAgeMax: Int? = null,

    @Column(name = "target_genders", length = 100)
    val targetGenders: String? = null, // Comma-separated: MALE,FEMALE,OTHER

    @Column(name = "target_locations", length = 1000)
    val targetLocations: String? = null, // Comma-separated location IDs

    @Column(name = "target_stations", length = 1000)
    val targetStations: String? = null, // Comma-separated station IDs

    @Column(name = "target_user_segments", length = 500)
    val targetUserSegments: String? = null, // Comma-separated segments

    @Column(name = "exclude_user_segments", length = 500)
    val excludeUserSegments: String? = null, // Comma-separated segments to exclude

    // Scheduling configuration
    @Column(name = "allowed_days_of_week", length = 20)
    val allowedDaysOfWeek: String? = null, // Comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN

    @Column(name = "allowed_hours_start")
    @field:Min(value = 0, message = "Start hour must be between 0 and 23")
    @field:Max(value = 23, message = "Start hour must be between 0 and 23")
    val allowedHoursStart: Int? = null,

    @Column(name = "allowed_hours_end")
    @field:Min(value = 0, message = "End hour must be between 0 and 23")
    @field:Max(value = 23, message = "End hour must be between 0 and 23")
    val allowedHoursEnd: Int? = null,

    // Metadata and tracking
    @Column(name = "advertiser_name", length = 200)
    val advertiserName: String? = null,

    @Column(name = "advertiser_contact", length = 200)
    val advertiserContact: String? = null,

    @Column(name = "tags", length = 500)
    val tags: String? = null, // Comma-separated tags

    @Column(name = "notes", length = 1000)
    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    @Column(name = "created_by", length = 100)
    val createdBy: String? = null,

    @Column(name = "updated_by", length = 100)
    val updatedBy: String? = null,

    @OneToMany(mappedBy = "advertisement", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val engagements: List<AdEngagement> = emptyList(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if the advertisement is currently active and within schedule
     */
    fun isActiveAndScheduled(): Boolean {
        val now = LocalDateTime.now()
        return status == AdStatus.ACTIVE &&
                now.isAfter(startDate) &&
                now.isBefore(endDate) &&
                isWithinSchedule(now)
    }

    /**
     * Check if current time is within allowed schedule
     */
    fun isWithinSchedule(dateTime: LocalDateTime = LocalDateTime.now()): Boolean {
        // Check day of week
        allowedDaysOfWeek?.let { days ->
            val dayOfWeek = dateTime.dayOfWeek.name.take(3)
            if (!days.contains(dayOfWeek)) return false
        }

        // Check hour range
        if (allowedHoursStart != null && allowedHoursEnd != null) {
            val hour = dateTime.hour
            return if (allowedHoursStart <= allowedHoursEnd) {
                hour in allowedHoursStart..allowedHoursEnd
            } else {
                // Overnight range (e.g., 22:00 to 06:00)
                hour >= allowedHoursStart || hour <= allowedHoursEnd
            }
        }

        return true
    }

    /**
     * Check if advertisement has budget remaining
     */
    fun hasBudgetRemaining(): Boolean {
        return when {
            totalBudget != null -> totalSpend < totalBudget
            dailyBudget != null -> getDailySpend() < dailyBudget
            else -> true
        }
    }

    /**
     * Get daily spend (would need to be calculated from engagements)
     */
    fun getDailySpend(): BigDecimal {
        // This would typically be calculated from today's engagements
        // For now, return a placeholder
        return BigDecimal.ZERO
    }

    /**
     * Check if user can see this ad based on targeting
     */
    fun matchesUserTargeting(
        userAge: Int? = null,
        userGender: String? = null,
        userLocation: String? = null,
        userSegments: List<String> = emptyList()
    ): Boolean {
        // Age targeting
        if (targetAgeMin != null && userAge != null && userAge < targetAgeMin) return false
        if (targetAgeMax != null && userAge != null && userAge > targetAgeMax) return false

        // Gender targeting
        if (targetGenders != null && userGender != null) {
            if (!targetGenders.split(",").map { it.trim() }.contains(userGender)) return false
        }

        // Location targeting
        if (targetLocations != null && userLocation != null) {
            if (!targetLocations.split(",").map { it.trim() }.contains(userLocation)) return false
        }

        // User segment targeting
        if (targetUserSegments != null) {
            val targetSegments = targetUserSegments.split(",").map { it.trim() }
            if (targetSegments.none { it in userSegments }) return false
        }

        // Exclude segments
        if (excludeUserSegments != null) {
            val excludeSegments = excludeUserSegments.split(",").map { it.trim() }
            if (excludeSegments.any { it in userSegments }) return false
        }

        return true
    }

    /**
     * Check if user has reached impression limit
     */
    fun hasUserReachedImpressionLimit(userImpressions: Int): Boolean {
        return maxImpressionsPerUser?.let { userImpressions >= it } ?: false
    }

    /**
     * Check if daily impression limit is reached
     */
    fun hasDailyImpressionLimitReached(todayImpressions: Long): Boolean {
        return maxDailyImpressions?.let { todayImpressions >= it } ?: false
    }

    /**
     * Calculate click-through rate
     */
    fun getClickThroughRate(): Double {
        return if (totalImpressions > 0) {
            (totalClicks.toDouble() / totalImpressions.toDouble()) * 100
        } else 0.0
    }

    /**
     * Calculate completion rate
     */
    fun getCompletionRate(): Double {
        return if (totalImpressions > 0) {
            (totalCompletions.toDouble() / totalImpressions.toDouble()) * 100
        } else 0.0
    }

    /**
     * Calculate effective cost per mille (CPM)
     */
    fun getEffectiveCPM(): BigDecimal {
        return if (totalImpressions > 0) {
            totalSpend.divide(BigDecimal(totalImpressions / 1000.0), 4, java.math.RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    /**
     * Get remaining budget
     */
    fun getRemainingBudget(): BigDecimal? {
        return totalBudget?.subtract(totalSpend)
    }

    /**
     * Check if ad provides ticket bonuses
     */
    fun providesTicketBonus(): Boolean {
        return ticketMultiplier != null || bonusTicketsOnCompletion > 0
    }

    /**
     * Calculate bonus tickets for engagement
     */
    fun calculateBonusTickets(
        baseTickets: Int,
        wasCompleted: Boolean,
        viewTimeSeconds: Int? = null
    ): Int {
        var bonus = 0

        // Apply multiplier to base tickets
        ticketMultiplier?.let { multiplier ->
            val multipliedTickets = (baseTickets * multiplier.toDouble()).toInt()
            bonus += multipliedTickets - baseTickets
        }

        // Add completion bonus
        if (bonusTicketsOnCompletion > 0) {
            val canGetBonus = if (requiresCompletionForBonus) {
                wasCompleted && (minViewTimeForBonus?.let { viewTimeSeconds != null && viewTimeSeconds >= it } ?: true)
            } else {
                minViewTimeForBonus?.let { viewTimeSeconds != null && viewTimeSeconds >= it } ?: true
            }

            if (canGetBonus) {
                bonus += bonusTicketsOnCompletion
            }
        }

        return bonus
    }

    /**
     * Activate the advertisement
     */
    fun activate(activatedBy: String? = null): Advertisement {
        return this.copy(
            status = AdStatus.ACTIVE,
            updatedBy = activatedBy
        )
    }

    /**
     * Pause the advertisement
     */
    fun pause(pausedBy: String? = null): Advertisement {
        return this.copy(
            status = AdStatus.PAUSED,
            updatedBy = pausedBy
        )
    }

    /**
     * Complete the advertisement
     */
    fun complete(completedBy: String? = null): Advertisement {
        return this.copy(
            status = AdStatus.COMPLETED,
            updatedBy = completedBy
        )
    }

    /**
     * Update statistics
     */
    fun updateStats(
        impressions: Long = 0,
        clicks: Long = 0,
        completions: Long = 0,
        spend: BigDecimal = BigDecimal.ZERO
    ): Advertisement {
        return this.copy(
            totalImpressions = this.totalImpressions + impressions,
            totalClicks = this.totalClicks + clicks,
            totalCompletions = this.totalCompletions + completions,
            totalSpend = this.totalSpend.add(spend)
        )
    }

    override fun toString(): String {
        return "Advertisement(id=$id, title='$title', type=$adType, status=$status, campaignId=$campaignId)"
    }
}

/**
 * Advertisement type enumeration
 */
enum class AdType(val displayName: String, val description: String) {
    BANNER("Banner Ad", "Static or animated banner advertisement"),
    VIDEO("Video Ad", "Video advertisement with play controls"),
    INTERSTITIAL("Interstitial Ad", "Full-screen advertisement"),
    NATIVE("Native Ad", "Advertisement that matches app content style"),
    REWARDED_VIDEO("Rewarded Video", "Video ad that provides rewards upon completion"),
    PLAYABLE("Playable Ad", "Interactive advertisement with mini-game"),
    AUDIO("Audio Ad", "Audio-only advertisement"),
    RICH_MEDIA("Rich Media", "Interactive multimedia advertisement");

    /**
     * Check if ad type supports video content
     */
    fun supportsVideo(): Boolean {
        return this == VIDEO || this == INTERSTITIAL || this == REWARDED_VIDEO || this == PLAYABLE
    }

    /**
     * Check if ad type supports interaction
     */
    fun supportsInteraction(): Boolean {
        return this == INTERSTITIAL || this == NATIVE || this == PLAYABLE || this == RICH_MEDIA
    }

    /**
     * Check if ad type typically provides rewards
     */
    fun typicallyProvidesRewards(): Boolean {
        return this == REWARDED_VIDEO || this == PLAYABLE
    }
}

/**
 * Advertisement status enumeration
 */
enum class AdStatus(val displayName: String, val description: String) {
    DRAFT("Draft", "Advertisement is being prepared"),
    PENDING_APPROVAL("Pending Approval", "Advertisement is awaiting approval"),
    ACTIVE("Active", "Advertisement is running"),
    PAUSED("Paused", "Advertisement is temporarily paused"),
    COMPLETED("Completed", "Advertisement campaign has ended"),
    CANCELLED("Cancelled", "Advertisement has been cancelled"),
    REJECTED("Rejected", "Advertisement was rejected during approval"),
    EXPIRED("Expired", "Advertisement has expired");

    /**
     * Check if status allows serving ads
     */
    fun allowsServing(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status allows modifications
     */
    fun allowsModifications(): Boolean {
        return this == DRAFT || this == PAUSED || this == REJECTED
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == CANCELLED || this == REJECTED || this == EXPIRED
    }
}