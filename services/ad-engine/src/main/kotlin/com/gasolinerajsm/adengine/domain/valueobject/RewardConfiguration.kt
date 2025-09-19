package com.gasolinerajsm.adengine.domain.valueobject

import java.math.BigDecimal

/**
 * Value object representing reward configuration for advertisements
 */
data class RewardConfiguration(
    val baseTickets: Int,
    val bonusMultiplier: BigDecimal = BigDecimal.ONE,
    val completionBonus: Int = 0,
    val clickBonus: Int = 0,
    val interactionBonus: Int = 0,
    val timeBasedBonus: TimeBasedBonus? = null,
    val qualityBonus: QualityBonus? = null,
    val frequencyBonus: FrequencyBonus? = null,
    val maxTicketsPerEngagement: Int? = null,
    val minEngagementDurationSeconds: Int = 0,
    val requiresCompletion: Boolean = false,
    val allowsPartialRewards: Boolean = true
) {

    init {
        require(baseTickets >= 0) { "Base tickets must be non-negative" }
        require(bonusMultiplier >= BigDecimal.ZERO) { "Bonus multiplier must be non-negative" }
        require(completionBonus >= 0) { "Completion bonus must be non-negative" }
        require(clickBonus >= 0) { "Click bonus must be non-negative" }
        require(interactionBonus >= 0) { "Interaction bonus must be non-negative" }
        require(minEngagementDurationSeconds >= 0) { "Min engagement duration must be non-negative" }
        maxTicketsPerEngagement?.let {
            require(it > 0) { "Max tickets per engagement must be positive" }
        }
    }

    /**
     * Calculate bonus tickets for engagement
     */
    fun calculateBonusTickets(
        baseTickets: Int,
        engagement: EngagementDetails
    ): Int {
        var bonusTickets = 0

        // Completion bonus
        if (engagement.isCompleted && completionBonus > 0) {
            bonusTickets += completionBonus
        }

        // Click bonus
        if (engagement.hasClicked && clickBonus > 0) {
            bonusTickets += clickBonus
        }

        // Interaction bonus
        if (engagement.interactionCount > 0) {
            bonusTickets += (engagement.interactionCount * interactionBonus)
        }

        // Time-based bonus
        timeBasedBonus?.let {
            bonusTickets += it.calculateBonus(engagement.durationSeconds)
        }

        // Quality bonus
        qualityBonus?.let {
            bonusTickets += it.calculateBonus(engagement.qualityScore)
        }

        // Frequency bonus
        frequencyBonus?.let {
            bonusTickets += it.calculateBonus(engagement.userEngagementCount)
        }

        // Apply bonus multiplier
        val multipliedBonus = bonusTickets.toBigDecimal()
            .multiply(bonusMultiplier)
            .toInt()

        // Apply maximum limit
        val totalBonus = maxTicketsPerEngagement?.let {
            minOf(multipliedBonus, it - baseTickets)
        } ?: multipliedBonus

        return maxOf(0, totalBonus)
    }

    /**
     * Check if engagement qualifies for rewards
     */
    fun qualifiesForRewards(engagement: EngagementDetails): Boolean {
        // Check minimum duration
        if (engagement.durationSeconds < minEngagementDurationSeconds) {
            return false
        }

        // Check completion requirement
        if (requiresCompletion && !engagement.isCompleted) {
            return false
        }

        // Check if any rewards would be earned
        val totalTickets = baseTickets + calculateBonusTickets(baseTickets, engagement)
        return totalTickets > 0
    }

    /**
     * Get total possible tickets for engagement
     */
    fun getTotalPossibleTickets(engagement: EngagementDetails): Int {
        val bonusTickets = calculateBonusTickets(baseTickets, engagement)
        val total = baseTickets + bonusTickets

        return maxTicketsPerEngagement?.let { minOf(total, it) } ?: total
    }

    /**
     * Check if configuration provides bonus rewards
     */
    fun providesBonus(): Boolean {
        return bonusMultiplier > BigDecimal.ONE ||
               completionBonus > 0 ||
               clickBonus > 0 ||
               interactionBonus > 0 ||
               timeBasedBonus != null ||
               qualityBonus != null ||
               frequencyBonus != null
    }

    /**
     * Validate reward configuration
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (baseTickets < 0) {
            errors.add("Base tickets cannot be negative")
        }

        if (bonusMultiplier < BigDecimal.ZERO) {
            errors.add("Bonus multiplier cannot be negative")
        }

        if (completionBonus < 0) {
            errors.add("Completion bonus cannot be negative")
        }

        if (clickBonus < 0) {
            errors.add("Click bonus cannot be negative")
        }

        if (interactionBonus < 0) {
            errors.add("Interaction bonus cannot be negative")
        }

        if (minEngagementDurationSeconds < 0) {
            errors.add("Minimum engagement duration cannot be negative")
        }

        maxTicketsPerEngagement?.let {
            if (it <= 0) {
                errors.add("Max tickets per engagement must be positive")
            }
            if (it < baseTickets) {
                errors.add("Max tickets per engagement cannot be less than base tickets")
            }
        }

        timeBasedBonus?.let {
            val validation = it.validate()
            if (!validation.isSuccess) {
                errors.add("Time-based bonus: ${validation.message}")
            }
        }

        qualityBonus?.let {
            val validation = it.validate()
            if (!validation.isSuccess) {
                errors.add("Quality bonus: ${validation.message}")
            }
        }

        frequencyBonus?.let {
            val validation = it.validate()
            if (!validation.isSuccess) {
                errors.add("Frequency bonus: ${validation.message}")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Reward configuration is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Get reward configuration summary
     */
    fun getRewardSummary(): Map<String, Any?> {
        return mapOf(
            "baseTickets" to baseTickets,
            "bonusMultiplier" to bonusMultiplier,
            "completionBonus" to completionBonus,
            "clickBonus" to clickBonus,
            "interactionBonus" to interactionBonus,
            "maxTicketsPerEngagement" to maxTicketsPerEngagement,
            "minEngagementDurationSeconds" to minEngagementDurationSeconds,
            "requiresCompletion" to requiresCompletion,
            "allowsPartialRewards" to allowsPartialRewards,
            "providesBonus" to providesBonus()
        )
    }

    companion object {
        fun create(baseTickets: Int): RewardConfiguration {
            return RewardConfiguration(baseTickets = baseTickets)
        }

        fun noRewards(): RewardConfiguration {
            return RewardConfiguration(baseTickets = 0)
        }
    }
}

/**
 * Engagement details for reward calculation
 */
data class EngagementDetails(
    val durationSeconds: Int,
    val isCompleted: Boolean,
    val hasClicked: Boolean,
    val interactionCount: Int,
    val qualityScore: Double,
    val userEngagementCount: Int
)

/**
 * Time-based bonus configuration
 */
data class TimeBasedBonus(
    val thresholds: List<TimeThreshold>
) {
    fun calculateBonus(durationSeconds: Int): Int {
        return thresholds
            .filter { durationSeconds >= it.minDurationSeconds }
            .maxByOrNull { it.bonusTickets }
            ?.bonusTickets ?: 0
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        thresholds.forEach { threshold ->
            if (threshold.minDurationSeconds < 0) {
                errors.add("Time threshold duration cannot be negative")
            }
            if (threshold.bonusTickets < 0) {
                errors.add("Time threshold bonus cannot be negative")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Time-based bonus is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }
}

/**
 * Time threshold for bonuses
 */
data class TimeThreshold(
    val minDurationSeconds: Int,
    val bonusTickets: Int
)

/**
 * Quality-based bonus configuration
 */
data class QualityBonus(
    val thresholds: List<QualityThreshold>
) {
    fun calculateBonus(qualityScore: Double): Int {
        return thresholds
            .filter { qualityScore >= it.minQualityScore }
            .maxByOrNull { it.bonusTickets }
            ?.bonusTickets ?: 0
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        thresholds.forEach { threshold ->
            if (threshold.minQualityScore < 0.0 || threshold.minQualityScore > 1.0) {
                errors.add("Quality score must be between 0.0 and 1.0")
            }
            if (threshold.bonusTickets < 0) {
                errors.add("Quality bonus cannot be negative")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Quality bonus is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }
}

/**
 * Quality threshold for bonuses
 */
data class QualityThreshold(
    val minQualityScore: Double,
    val bonusTickets: Int
)

/**
 * Frequency-based bonus configuration
 */
data class FrequencyBonus(
    val thresholds: List<FrequencyThreshold>
) {
    fun calculateBonus(userEngagementCount: Int): Int {
        return thresholds
            .filter { userEngagementCount >= it.minEngagements }
            .maxByOrNull { it.bonusTickets }
            ?.bonusTickets ?: 0
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        thresholds.forEach { threshold ->
            if (threshold.minEngagements < 0) {
                errors.add("Frequency threshold engagements cannot be negative")
            }
            if (threshold.bonusTickets < 0) {
                errors.add("Frequency bonus cannot be negative")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Frequency bonus is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }
}

/**
 * Frequency threshold for bonuses
 */
data class FrequencyThreshold(
    val minEngagements: Int,
    val bonusTickets: Int
)