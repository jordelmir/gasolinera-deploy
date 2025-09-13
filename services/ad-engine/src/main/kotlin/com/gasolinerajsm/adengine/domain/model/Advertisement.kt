package com.gasolinerajsm.adengine.domain.model

import com.gasolinerajsm.adengine.domain.event.DomainEvent
import com.gasolinerajsm.adengine.domain.event.AdvertisementCreatedEvent
import com.gasolinerajsm.adengine.domain.event.AdvertisementActivatedEvent
import com.gasolinerajsm.adengine.domain.event.AdvertisementCompletedEvent
import com.gasolinerajsm.adengine.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Advertisement Domain Entity - Core business logic
 * Represents an advertisement with targeting, scheduling, and reward mechanics
 */
data class Advertisement(
    val id: AdvertisementId,
    val campaignId: CampaignId,
    val title: String,
    val description: String? = null,
    val adType: AdType,
    val status: AdStatus = AdStatus.DRAFT,
    val content: AdContent,
    val schedule: AdSchedule,
    val budget: AdBudget,
    val targeting: AdTargeting,
    val rewardConfig: RewardConfiguration,
    val statistics: AdStatistics = AdStatistics.initial(),
    val metadata: AdMetadata,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new advertisement
         */
        fun create(
            campaignId: CampaignId,
            title: String,
            description: String?,
            adType: AdType,
            content: AdContent,
            schedule: AdSchedule,
            budget: AdBudget,
            targeting: AdTargeting,
            rewardConfig: RewardConfiguration,
            metadata: AdMetadata
        ): Advertisement {
            val advertisement = Advertisement(
                id = AdvertisementId.generate(),
                campaignId = campaignId,
                title = title,
                description = description,
                adType = adType,
                content = content,
                schedule = schedule,
                budget = budget,
                targeting = targeting,
                rewardConfig = rewardConfig,
                metadata = metadata
            )

            advertisement.addDomainEvent(
                AdvertisementCreatedEvent(
                    advertisementId = advertisement.id,
                    campaignId = advertisement.campaignId,
                    title = advertisement.title,
                    adType = advertisement.adType,
                    schedule = advertisement.schedule,
                    budget = advertisement.budget,
                    occurredAt = LocalDateTime.now()
                )
            )

            return advertisement
        }
    }

    /**
     * Check if the advertisement is currently active and within schedule
     */
    fun isActiveAndScheduled(): Boolean {
        val now = LocalDateTime.now()
        return status == AdStatus.ACTIVE &&
                schedule.isActiveAt(now) &&
                budget.hasRemainingBudget()
    }

    /**
     * Check if advertisement can be served to a user
     */
    fun canServeToUser(userProfile: UserProfile): ValidationResult {
        if (!isActiveAndScheduled()) {
            return ValidationResult.failure("Advertisement is not active or scheduled")
        }

        if (!targeting.matchesUser(userProfile)) {
            return ValidationResult.failure("User does not match targeting criteria")
        }

        if (statistics.hasReachedDailyLimit()) {
            return ValidationResult.failure("Daily impression limit reached")
        }

        return ValidationResult.success("Advertisement can be served")
    }

    /**
     * Activate the advertisement
     */
    fun activate(activatedBy: String? = null): Advertisement {
        if (status != AdStatus.DRAFT && status != AdStatus.PAUSED) {
            throw IllegalStateException("Cannot activate advertisement in status: $status")
        }

        val activatedAd = this.copy(
            status = AdStatus.ACTIVE,
            metadata = metadata.copy(updatedBy = activatedBy),
            updatedAt = LocalDateTime.now()
        )

        activatedAd.addDomainEvent(
            AdvertisementActivatedEvent(
                advertisementId = id,
                campaignId = campaignId,
                title = title,
                activatedBy = activatedBy,
                occurredAt = LocalDateTime.now()
            )
        )

        return activatedAd
    }

    /**
     * Pause the advertisement
     */
    fun pause(pausedBy: String? = null): Advertisement {
        if (status != AdStatus.ACTIVE) {
            throw IllegalStateException("Cannot pause advertisement in status: $status")
        }

        return this.copy(
            status = AdStatus.PAUSED,
            metadata = metadata.copy(updatedBy = pausedBy),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Complete the advertisement
     */
    fun complete(completedBy: String? = null): Advertisement {
        if (status.isFinalState()) {
            throw IllegalStateException("Advertisement is already in final state: $status")
        }

        val completedAd = this.copy(
            status = AdStatus.COMPLETED,
            metadata = metadata.copy(updatedBy = completedBy),
            updatedAt = LocalDateTime.now()
        )

        completedAd.addDomainEvent(
            AdvertisementCompletedEvent(
                advertisementId = id,
                campaignId = campaignId,
                title = title,
                finalStatistics = statistics,
                totalSpend = budget.totalSpend,
                completedBy = completedBy,
                occurredAt = LocalDateTime.now()
            )
        )

        return completedAd
    }

    /**
     * Cancel the advertisement
     */
    fun cancel(reason: String? = null, cancelledBy: String? = null): Advertisement {
        if (status.isFinalState()) {
            throw IllegalStateException("Advertisement is already in final state: $status")
        }

        return this.copy(
            status = AdStatus.CANCELLED,
            metadata = metadata.copy(
                updatedBy = cancelledBy,
                notes = reason?.let { "${metadata.notes ?: ""}\nCancelled: $it".trim() } ?: metadata.notes
            ),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Record an impression
     */
    fun recordImpression(cost: BigDecimal = BigDecimal.ZERO): Advertisement {
        return this.copy(
            statistics = statistics.recordImpression(),
            budget = budget.addSpend(cost),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Record a click
     */
    fun recordClick(cost: BigDecimal = BigDecimal.ZERO): Advertisement {
        return this.copy(
            statistics = statistics.recordClick(),
            budget = budget.addSpend(cost),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Record a completion
     */
    fun recordCompletion(cost: BigDecimal = BigDecimal.ZERO): Advertisement {
        return this.copy(
            statistics = statistics.recordCompletion(),
            budget = budget.addSpend(cost),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Calculate bonus tickets for engagement
     */
    fun calculateBonusTickets(
        baseTickets: Int,
        engagement: EngagementDetails
    ): Int {
        return rewardConfig.calculateBonusTickets(baseTickets, engagement)
    }

    /**
     * Check if advertisement provides ticket bonuses
     */
    fun providesTicketBonus(): Boolean {
        return rewardConfig.providesBonus()
    }

    /**
     * Update targeting configuration
     */
    fun updateTargeting(newTargeting: AdTargeting): Advertisement {
        return this.copy(
            targeting = newTargeting,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update reward configuration
     */
    fun updateRewardConfig(newRewardConfig: RewardConfiguration): Advertisement {
        return this.copy(
            rewardConfig = newRewardConfig,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update budget
     */
    fun updateBudget(newBudget: AdBudget): Advertisement {
        return this.copy(
            budget = newBudget,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            impressions = statistics.totalImpressions,
            clicks = statistics.totalClicks,
            completions = statistics.totalCompletions,
            clickThroughRate = statistics.getClickThroughRate(),
            completionRate = statistics.getCompletionRate(),
            totalSpend = budget.totalSpend,
            effectiveCPM = statistics.getEffectiveCPM(budget.totalSpend),
            remainingBudget = budget.getRemainingBudget()
        )
    }

    /**
     * Check if advertisement is expired
     */
    fun isExpired(): Boolean {
        return schedule.isExpired()
    }

    /**
     * Check if advertisement is within budget
     */
    fun isWithinBudget(): Boolean {
        return budget.hasRemainingBudget()
    }

    /**
     * Validate advertisement business rules
     */
    fun validateBusinessRules(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate title
        if (title.isBlank() || title.length < 2) {
            errors.add("Title must be at least 2 characters")
        }

        // Validate schedule
        val scheduleValidation = schedule.validate()
        if (!scheduleValidation.isSuccess) {
            errors.add(scheduleValidation.message)
        }

        // Validate budget
        val budgetValidation = budget.validate()
        if (!budgetValidation.isSuccess) {
            errors.add(budgetValidation.message)
        }

        // Validate content
        val contentValidation = content.validate(adType)
        if (!contentValidation.isSuccess) {
            errors.add(contentValidation.message)
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Advertisement is valid")
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
        return "Advertisement(id=$id, title='$title', type=$adType, status=$status, campaign=$campaignId)"
    }
}

/**
 * Validation result for domain operations
 */
data class ValidationResult(
    val isSuccess: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = ValidationResult(true, message)
        fun failure(message: String) = ValidationResult(false, message)
    }
}

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val impressions: Long,
    val clicks: Long,
    val completions: Long,
    val clickThroughRate: Double,
    val completionRate: Double,
    val totalSpend: BigDecimal,
    val effectiveCPM: BigDecimal,
    val remainingBudget: BigDecimal?
)