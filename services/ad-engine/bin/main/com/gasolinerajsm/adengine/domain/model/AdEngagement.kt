package com.gasolinerajsm.adengine.domain.model

import com.gasolinerajsm.adengine.domain.valueobject.*
import com.gasolinerajsm.adengine.events.DomainEvent
import com.gasolinerajsm.adengine.events.EngagementStartedEvent
import com.gasolinerajsm.adengine.events.EngagementCompletedEvent
import com.gasolinerajsm.adengine.events.TicketsAwardedEvent
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Ad Engagement Domain Entity
 * Represents user interaction with an advertisement
 */
data class AdEngagement(
    val id: EngagementId,
    val userId: UserId,
    val advertisementId: AdvertisementId,
    val sessionId: SessionId?,
    val engagementType: EngagementType,
    val status: EngagementStatus = EngagementStatus.STARTED,
    val timestamps: EngagementTimestamps,
    val interactionData: InteractionData,
    val locationData: LocationData? = null,
    val deviceInfo: DeviceInfo? = null,
    val rewardData: RewardData = RewardData.initial(),
    val billingData: BillingData? = null,
    val errorInfo: ErrorInfo? = null,
    val metadata: EngagementMetadata = EngagementMetadata(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new engagement
         */
        fun start(
            userId: UserId,
            advertisementId: AdvertisementId,
            sessionId: SessionId?,
            engagementType: EngagementType,
            locationData: LocationData? = null,
            deviceInfo: DeviceInfo? = null,
            metadata: EngagementMetadata = EngagementMetadata()
        ): AdEngagement {
            val engagement = AdEngagement(
                id = EngagementId.generate(),
                userId = userId,
                advertisementId = advertisementId,
                sessionId = sessionId,
                engagementType = engagementType,
                timestamps = EngagementTimestamps.start(),
                interactionData = InteractionData.initial(),
                locationData = locationData,
                deviceInfo = deviceInfo,
                metadata = metadata
            )

            engagement.addDomainEvent(
                EngagementStartedEvent(
                    aggregateId = engagement.id.toString(),
                    engagementId = engagement.id,
                    userId = engagement.userId,
                    advertisementId = engagement.advertisementId,
                    engagementType = engagement.engagementType,
                    sessionId = engagement.sessionId,
                    locationData = engagement.locationData,
                    occurredAt = LocalDateTime.now()
                )
            )

            return engagement
        }
    }

    /**
     * Check if the engagement was completed successfully
     */
    fun isCompleted(): Boolean {
        return status == EngagementStatus.COMPLETED && timestamps.completedAt != null
    }

    /**
     * Check if the engagement was skipped
     */
    fun wasSkipped(): Boolean {
        return status == EngagementStatus.SKIPPED
    }

    /**
     * Check if the engagement had errors
     */
    fun hadError(): Boolean {
        return errorInfo != null || status == EngagementStatus.ERROR
    }

    /**
     * Check if the engagement qualifies for rewards
     */
    fun qualifiesForRewards(): Boolean {
        return isCompleted() && !hadError() && rewardData.totalTicketsEarned > 0
    }

    /**
     * Complete the engagement
     */
    fun complete(
        completionPercentage: BigDecimal? = null,
        viewDuration: Int? = null
    ): AdEngagement {
        if (status.isFinalState()) {
            throw IllegalStateException("Cannot complete engagement in final state: $status")
        }

        val completedEngagement = this.copy(
            status = EngagementStatus.COMPLETED,
            timestamps = timestamps.markCompleted(),
            interactionData = interactionData.copy(
                completionPercentage = completionPercentage,
                viewDurationSeconds = viewDuration
            ),
            updatedAt = LocalDateTime.now()
        )

        completedEngagement.addDomainEvent(
            EngagementCompletedEvent(
                aggregateId = id.toString(),
                engagementId = id,
                userId = userId,
                advertisementId = advertisementId,
                engagementType = engagementType,
                completionPercentage = completionPercentage,
                viewDurationSeconds = viewDuration,
                totalInteractions = interactionData.interactionsCount,
                qualifiesForRewards = completedEngagement.qualifiesForRewards(),
                occurredAt = LocalDateTime.now()
            )
        )

        return completedEngagement
    }

    /**
     * Record a click
     */
    fun click(clickThroughUrl: String? = null): AdEngagement {
        return this.copy(
            interactionData = interactionData.recordClick(clickThroughUrl),
            status = EngagementStatus.INTERACTED,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Skip the engagement
     */
    fun skip(): AdEngagement {
        if (status.isFinalState()) {
            throw IllegalStateException("Cannot skip engagement in final state: $status")
        }

        return this.copy(
            status = EngagementStatus.SKIPPED,
            timestamps = timestamps.markSkipped(),
            interactionData = interactionData.recordSkip(),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Mark engagement as having an error
     */
    fun error(errorMessage: String, errorCode: String? = null): AdEngagement {
        return this.copy(
            status = EngagementStatus.ERROR,
            interactionData = interactionData.recordError(errorMessage, errorCode),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Award tickets for this engagement
     */
    fun awardTickets(
        baseTickets: Int,
        bonusTickets: Int,
        raffleEntryId: RaffleEntryId? = null
    ): AdEngagement {
        val updatedRewardData = rewardData.copy(
            baseTicketsEarned = baseTickets,
            bonusTicketsEarned = bonusTickets,
            totalTicketsEarned = baseTickets + bonusTickets,
            ticketsAwarded = true,
            ticketsAwardedAt = LocalDateTime.now(),
            raffleEntryCreated = raffleEntryId != null,
            raffleEntryId = raffleEntryId
        )

        val updatedEngagement = this.copy(
            rewardData = updatedRewardData,
            updatedAt = LocalDateTime.now()
        )

        updatedEngagement.addDomainEvent(
            TicketsAwardedEvent(
                aggregateId = id.toString(),
                engagementId = id,
                userId = userId,
                advertisementId = advertisementId,
                baseTickets = baseTickets,
                bonusTickets = bonusTickets,
                totalTickets = baseTickets + bonusTickets,
                raffleEntryId = raffleEntryId,
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedEngagement
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
            interactionData = interactionData.updateInteractions(interactions, pauses, replays),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Set billing information
     */
    fun setBilling(cost: BigDecimal, event: BillingEvent): AdEngagement {
        return this.copy(
            rewardData = rewardData.copy(
                costCharged = cost,
                billingEvent = event
            ),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Get engagement duration in seconds
     */
    fun getEngagementDurationSeconds(): Long? {
        return timestamps.getDurationSeconds()
    }

    /**
     * Check if engagement meets minimum view time requirement
     */
    fun meetsMinimumViewTime(minimumSeconds: Int): Boolean {
        return interactionData.viewDurationSeconds?.let { it >= minimumSeconds } ?: false
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
        interactionData.completionPercentage?.let {
            score += (it.toDouble() * 0.3)
        }

        // Bonus for clicking
        if (interactionData.clicked) score += 10.0

        // Bonus for interactions
        score += minOf(interactionData.interactionsCount * 2.0, 10.0)

        // Penalty for errors
        if (hadError()) score -= 20.0

        // Penalty for skipping
        if (wasSkipped()) score -= 15.0

        return minOf(maxOf(score, 0.0), 100.0)
    }

    /**
     * Check if engagement has location data
     */
    fun hasLocationData(): Boolean {
        return locationData != null
    }

    /**
     * Get formatted location
     */
    fun getFormattedLocation(): String? {
        return locationData?.getFormattedCoordinates()
    }

    /**
     * Update metadata
     */
    fun updateMetadata(newMetadata: Map<String, String>): AdEngagement {
        return this.copy(
            metadata = metadata.copy(additionalData = metadata.additionalData + newMetadata),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Validate engagement business rules
     */
    fun validateBusinessRules(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate timestamps
        if (timestamps.startedAt.isAfter(LocalDateTime.now())) {
            errors.add("Start time cannot be in the future")
        }

        // Validate completion data
        if (status == EngagementStatus.COMPLETED && timestamps.completedAt == null) {
            errors.add("Completed engagement must have completion timestamp")
        }

        // Validate reward data
        if (rewardData.ticketsAwarded && rewardData.ticketsAwardedAt == null) {
            errors.add("Awarded tickets must have award timestamp")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Engagement is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "AdEngagement(id=$id, userId=$userId, adId=$advertisementId, type=$engagementType, status=$status)"
    }
}