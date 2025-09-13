package com.gasolinerajsm.adengine.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * AdEngagement entity representing user interactions with advertisements
 */
@Entity
@Table(
    name = "ad_engagements",
    schema = "ad_engine_schema",
    indexes = [
        Index(name = "idx_ad_engagements_user_id", columnList = "user_id"),
        Index(name = "idx_ad_engagements_advertisement_id", columnList = "advertisement_id"),
        Index(name = "idx_ad_engagements_engagement_type", columnList = "engagement_type"),
        Index(name = "idx_ad_engagements_status", columnList = "status"),
        Index(name = "idx_ad_engagements_session_id", columnList = "session_id"),
        Index(name = "idx_ad_engagements_station_id", columnList = "station_id"),
        Index(name = "idx_ad_engagements_created_at", columnList = "created_at"),
        Index(name = "idx_ad_engagements_completed_at", columnList = "completed_at")
    ]
)
data class AdEngagement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", nullable = false)
    @field:NotNull(message = "Advertisement is required")
    val advertisement: Advertisement,

    @Column(name = "session_id", length = 100)
    val sessionId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "engagement_type", nullable = false, length = 20)
    val engagementType: EngagementType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: EngagementStatus = EngagementStatus.STARTED,

    @Column(name = "started_at", nullable = false)
    @field:NotNull(message = "Start time is required")
    val startedAt: LocalDateTime,

    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null,

    @Column(name = "view_duration_seconds")
    @field:Min(value = 0, message = "View duration must be non-negative")
    val viewDurationSeconds: Int? = null,

    @Column(name = "completion_percentage", precision = 5, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Completion percentage must be non-negative")
    @field:DecimalMax(value = "100.0", message = "Completion percentage cannot exceed 100")
    val completionPercentage: BigDecimal? = null,

    @Column(name = "clicked", nullable = false)
    val clicked: Boolean = false,

    @Column(name = "clicked_at")
    val clickedAt: LocalDateTime? = null,

    @Column(name = "click_through_url", length = 500)
    val clickThroughUrl: String? = null,

    @Column(name = "station_id")
    val stationId: Long? = null,

    @Column(name = "device_type", length = 50)
    val deviceType: String? = null,

    @Column(name = "device_id", length = 100)
    val deviceId: String? = null,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", length = 500)
    val userAgent: String? = null,

    @Column(name = "location_latitude", precision = 10, scale = 8)
    val locationLatitude: BigDecimal? = null,

    @Column(name = "location_longitude", precision = 11, scale = 8)
    val locationLongitude: BigDecimal? = null,

    @Column(name = "location_accuracy_meters")
    val locationAccuracyMeters: Int? = null,

    // Reward tracking
    @Column(name = "base_tickets_earned", nullable = false)
    val baseTicketsEarned: Int = 0,

    @Column(name = "bonus_tickets_earned", nullable = false)
    val bonusTicketsEarned: Int = 0,

    @Column(name = "total_tickets_earned", nullable = false)
    val totalTicketsEarned: Int = 0,

    @Column(name = "tickets_awarded", nullable = false)
    val ticketsAwarded: Boolean = false,

    @Column(name = "tickets_awarded_at")
    val ticketsAwardedAt: LocalDateTime? = null,

    @Column(name = "raffle_entry_created", nullable = false)
    val raffleEntryCreated: Boolean = false,

    @Column(name = "raffle_entry_id")
    val raffleEntryId: Long? = null,

    // Cost tracking
    @Column(name = "cost_charged", precision = 8, scale = 4)
    @field:DecimalMin(value = "0.0", message = "Cost charged must be non-negative")
    val costCharged: BigDecimal? = null,

    @Column(name = "billing_event", length = 20)
    val billingEvent: String? = null, // IMPRESSION, CLICK, COMPLETION

    // Interaction tracking
    @Column(name = "interactions_count", nullable = false)
    val interactionsCount: Int = 0,

    @Column(name = "pause_count", nullable = false)
    val pauseCount: Int = 0,

    @Column(name = "replay_count", nullable = false)
    val replayCount: Int = 0,

    @Column(name = "skip_attempted", nullable = false)
    val skipAttempted: Boolean = false,

    @Column(name = "skip_allowed", nullable = false)
    val skipAllowed: Boolean = false,

    @Column(name = "skipped_at")
    val skippedAt: LocalDateTime? = null,

    @Column(name = "error_occurred", nullable = false)
    val errorOccurred: Boolean = false,

    @Column(name = "error_message", length = 500)
    val errorMessage: String? = null,

    @Column(name = "error_code", length = 50)
    val errorCode: String? = null,

    // Metadata
    @Column(name = "referrer_url", length = 500)
    val referrerUrl: String? = null,

    @Column(name = "campaign_context", length = 200)
    val campaignContext: String? = null,

    @Column(name = "placement_context", length = 200)
    val placementContext: String? = null,

    @Column(name = "metadata", length = 1000)
    val metadata: String? = null,

    @Column(name = "notes", length = 500)
    @field:Size(max = 500, message = "Notes must not exceed 500 characters")
    val notes: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if the engagement was completed successfully
     */
    fun isCompleted(): Boolean {
        return status == EngagementStatus.COMPLETED && completedAt != null
    }

    /**
     * Check if the engagement was skipped
     */
    fun wasSkipped(): Boolean {
        return status == EngagementStatus.SKIPPED && skippedAt != null
    }

    /**
     * Check if the engagement had errors
     */
    fun hadError(): Boolean {
        return errorOccurred || status == EngagementStatus.ERROR
    }

    /**
     * Check if the engagement qualifies for rewards
     */
    fun qualifiesForRewards(): Boolean {
        return isCompleted() && !hadError() && totalTicketsEarned > 0
    }

    /**
     * Check if tickets have been awarded
     */
    fun hasTicketsAwarded(): Boolean {
        return ticketsAwarded && ticketsAwardedAt != null
    }

    /**
     * Get engagement duration in seconds
     */
    fun getEngagementDurationSeconds(): Long? {
        return completedAt?.let {
            java.time.temporal.ChronoUnit.SECONDS.between(startedAt, it)
        }
    }

    /**
     * Get engagement duration in minutes
     */
    fun getEngagementDurationMinutes(): Double? {
        return getEngagementDurationSeconds()?.let { it / 60.0 }
    }

    /**
     * Check if engagement meets minimum view time requirement
     */
    fun meetsMinimumViewTime(minimumSeconds: Int): Boolean {
        return viewDurationSeconds?.let { it >= minimumSeconds } ?: false
    }

    /**
     * Check if engagement was completed within expected time
     */
    fun wasCompletedWithinExpectedTime(): Boolean {
        val adDuration = advertisement.durationSeconds ?: return true
        return viewDurationSeconds?.let { it >= (adDuration * 0.8).toInt() } ?: false
    }

    /**
     * Calculate engagement quality score (0-100)
     */
    fun getEngagementQualityScore(): Double {
        var score = 0.0

        // Base score for completion
        when (status) {
            EngagementStatus.COMPLETED -> score += 50.0
            EngagementStatus.VIEWED -> score += 30.0
            EngagementStatus.STARTED -> score += 10.0
            else -> score += 0.0
        }

        // Bonus for completion percentage
        completionPercentage?.let {
            score += (it.toDouble() * 0.3)
        }

        // Bonus for clicking
        if (clicked) score += 10.0

        // Bonus for interactions
        score += minOf(interactionsCount * 2.0, 10.0)

        // Penalty for errors
        if (hadError()) score -= 20.0

        // Penalty for skipping
        if (wasSkipped()) score -= 15.0

        return minOf(maxOf(score, 0.0), 100.0)
    }

    /**
     * Mark engagement as completed
     */
    fun complete(
        completionPercentage: BigDecimal? = null,
        viewDuration: Int? = null
    ): AdEngagement {
        return this.copy(
            status = EngagementStatus.COMPLETED,
            completedAt = LocalDateTime.now(),
            completionPercentage = completionPercentage,
            viewDurationSeconds = viewDuration
        )
    }

    /**
     * Mark engagement as clicked
     */
    fun click(clickThroughUrl: String? = null): AdEngagement {
        return this.copy(
            clicked = true,
            clickedAt = LocalDateTime.now(),
            clickThroughUrl = clickThroughUrl
        )
    }

    /**
     * Mark engagement as skipped
     */
    fun skip(): AdEngagement {
        return this.copy(
            status = EngagementStatus.SKIPPED,
            skippedAt = LocalDateTime.now()
        )
    }

    /**
     * Mark engagement as having an error
     */
    fun error(errorMessage: String, errorCode: String? = null): AdEngagement {
        return this.copy(
            status = EngagementStatus.ERROR,
            errorOccurred = true,
            errorMessage = errorMessage,
            errorCode = errorCode
        )
    }

    /**
     * Award tickets for this engagement
     */
    fun awardTickets(
        baseTickets: Int,
        bonusTickets: Int,
        raffleEntryId: Long? = null
    ): AdEngagement {
        return this.copy(
            baseTicketsEarned = baseTickets,
            bonusTicketsEarned = bonusTickets,
            totalTicketsEarned = baseTickets + bonusTickets,
            ticketsAwarded = true,
            ticketsAwardedAt = LocalDateTime.now(),
            raffleEntryCreated = raffleEntryId != null,
            raffleEntryId = raffleEntryId
        )
    }

    /**
     * Update interaction statistics
     */
    fun updateInteractions(
        interactions: Int = 0,
        pauses: Int = 0,
        replays: Int = 0
    ): AdEngagement {
        return this.copy(
            interactionsCount = this.interactionsCount + interactions,
            pauseCount = this.pauseCount + pauses,
            replayCount = this.replayCount + replays
        )
    }

    /**
     * Set billing information
     */
    fun setBilling(cost: BigDecimal, event: String): AdEngagement {
        return this.copy(
            costCharged = cost,
            billingEvent = event
        )
    }

    /**
     * Get formatted location
     */
    fun getFormattedLocation(): String? {
        return if (locationLatitude != null && locationLongitude != null) {
            "${locationLatitude}, ${locationLongitude}"
        } else null
    }

    /**
     * Check if engagement has location data
     */
    fun hasLocationData(): Boolean {
        return locationLatitude != null && locationLongitude != null
    }

    override fun toString(): String {
        return "AdEngagement(id=$id, userId=$userId, adId=${advertisement.id}, type=$engagementType, status=$status, completed=${isCompleted()})"
    }
}

/**
 * Engagement type enumeration
 */
enum class EngagementType(val displayName: String, val description: String) {
    IMPRESSION("Impression", "Advertisement was displayed to user"),
    VIEW("View", "User actively viewed the advertisement"),
    CLICK("Click", "User clicked on the advertisement"),
    INTERACTION("Interaction", "User interacted with the advertisement"),
    COMPLETION("Completion", "User completed viewing the advertisement");

    /**
     * Check if engagement type is billable
     */
    fun isBillable(): Boolean {
        return this == IMPRESSION || this == CLICK || this == COMPLETION
    }

    /**
     * Check if engagement type indicates user interest
     */
    fun indicatesInterest(): Boolean {
        return this == CLICK || this == INTERACTION || this == COMPLETION
    }
}

/**
 * Engagement status enumeration
 */
enum class EngagementStatus(val displayName: String, val description: String) {
    STARTED("Started", "Engagement has started"),
    VIEWED("Viewed", "Advertisement was viewed"),
    INTERACTED("Interacted", "User interacted with advertisement"),
    COMPLETED("Completed", "Engagement was completed successfully"),
    SKIPPED("Skipped", "User skipped the advertisement"),
    ABANDONED("Abandoned", "User abandoned the engagement"),
    ERROR("Error", "An error occurred during engagement"),
    TIMEOUT("Timeout", "Engagement timed out");

    /**
     * Check if status indicates successful engagement
     */
    fun isSuccessful(): Boolean {
        return this == COMPLETED || this == VIEWED || this == INTERACTED
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == SKIPPED || this == ABANDONED ||
               this == ERROR || this == TIMEOUT
    }

    /**
     * Check if status allows further interaction
     */
    fun allowsInteraction(): Boolean {
        return this == STARTED || this == VIEWED || this == INTERACTED
    }
}