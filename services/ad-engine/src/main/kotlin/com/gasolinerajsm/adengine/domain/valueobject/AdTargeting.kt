package com.gasolinerajsm.adengine.domain.valueobject

import com.gasolinerajsm.adengine.domain.model.DeviceType
import com.gasolinerajsm.adengine.domain.model.Gender
import com.gasolinerajsm.adengine.domain.model.UserSegment
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Value object representing advertisement targeting configuration
 */
data class AdTargeting(
    val ageRange: AgeRange? = null,
    val gender: Gender? = null,
    val userSegments: Set<UserSegment> = emptySet(),
    val deviceTypes: Set<DeviceType> = emptySet(),
    val locations: Set<String> = emptySet(), // Location codes or names
    val languages: Set<String> = setOf("es"), // ISO 639-1 language codes
    val interests: Set<String> = emptySet(),
    val excludedUserIds: Set<String> = emptySet(),
    val includedUserIds: Set<String> = emptySet(),
    val behavioralTargeting: BehavioralTargeting? = null,
    val contextualTargeting: ContextualTargeting? = null,
    val retargeting: RetargetingConfig? = null,
    val frequencyCap: FrequencyCap? = null,
    val dayParting: DayParting? = null
) {

    /**
     * Check if user profile matches targeting criteria
     */
    fun matchesUser(userProfile: UserProfile): Boolean {
        // Check included users (whitelist)
        if (includedUserIds.isNotEmpty()) {
            if (userProfile.userId.toString() !in includedUserIds) return false
        }

        // Check excluded users (blacklist)
        if (userProfile.userId.toString() in excludedUserIds) return false

        // Check age range
        ageRange?.let {
            val userAge = userProfile.age
            if (userAge != null && !it.contains(userAge)) return false
        }

        // Check gender
        gender?.let {
            if (userProfile.gender != it.toString()) return false
        }

        // Check user segments
        if (userSegments.isNotEmpty()) {
            val userSegment = userProfile.userSegment
            if (userSegment !in userSegments.map { it.toString() }) return false
        }

        // Check device types
        if (deviceTypes.isNotEmpty()) {
            val userDeviceType = userProfile.deviceType
            if (userDeviceType !in deviceTypes.map { it.toString() }) return false
        }

        // Check locations
        if (locations.isNotEmpty()) {
            val userLocation = userProfile.location
            if (userLocation != null && userLocation !in locations) return false
        }

        // Check languages
        if (languages.isNotEmpty()) {
            val userLanguage = userProfile.language
            if (userLanguage !in languages) return false
        }

        // Check interests
        if (interests.isNotEmpty()) {
            val userInterests = userProfile.interests
            if (userInterests.none { it in interests }) return false
        }

        // Check behavioral targeting
        behavioralTargeting?.let {
            if (!it.matchesUserBehavior(userProfile)) return false
        }

        // Check contextual targeting
        contextualTargeting?.let {
            // This would need context from the current request/page
            // For now, assume it matches if configured
        }

        // Check retargeting
        retargeting?.let {
            if (!it.shouldRetargetUser(userProfile)) return false
        }

        // Check frequency cap
        frequencyCap?.let {
            if (!it.canShowToUser(userProfile)) return false
        }

        // Check day parting
        dayParting?.let {
            if (!it.isActiveNow()) return false
        }

        return true
    }

    /**
     * Get targeting score for user (0.0 to 1.0)
     */
    fun getTargetingScore(userProfile: UserProfile): Double {
        if (!matchesUser(userProfile)) return 0.0

        var score = 1.0
        var criteriaCount = 0

        // Age match bonus
        ageRange?.let {
            criteriaCount++
            val userAge = userProfile.age
            if (userAge != null && it.contains(userAge)) {
                score += 0.1
            }
        }

        // Gender match bonus
        gender?.let {
            criteriaCount++
            if (userProfile.gender == it.toString()) {
                score += 0.1
            }
        }

        // User segment match bonus
        if (userSegments.isNotEmpty()) {
            criteriaCount++
            val userSegment = userProfile.userSegment
            if (userSegment in userSegments.map { it.toString() }) {
                score += 0.2
            }
        }

        // Interest match bonus
        if (interests.isNotEmpty()) {
            criteriaCount++
            val userInterests = userProfile.interests
            val matchingInterests = userInterests.count { it in interests }
            score += (matchingInterests.toDouble() / interests.size) * 0.3
        }

        // Behavioral targeting bonus
        behavioralTargeting?.let {
            criteriaCount++
            if (it.matchesUserBehavior(userProfile)) {
                score += 0.2
            }
        }

        return minOf(score, 1.0)
    }

    /**
     * Validate targeting configuration
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate age range
        ageRange?.let {
            if (it.minAge < 0 || it.maxAge < it.minAge) {
                errors.add("Invalid age range: min=${it.minAge}, max=${it.maxAge}")
            }
        }

        // Validate languages
        languages.forEach { lang ->
            if (lang.length != 2) {
                errors.add("Invalid language code: $lang (must be ISO 639-1)")
            }
        }

        // Validate frequency cap
        frequencyCap?.let {
            if (it.maxImpressionsPerHour <= 0) {
                errors.add("Frequency cap max impressions must be positive")
            }
        }

        // Validate day parting
        dayParting?.let {
            if (it.timeSlots.isEmpty()) {
                errors.add("Day parting must have at least one time slot")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Targeting is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Get targeting summary
     */
    fun getTargetingSummary(): Map<String, Any?> {
        return mapOf(
            "ageRange" to ageRange,
            "gender" to gender,
            "userSegments" to userSegments.map { it.toString() },
            "deviceTypes" to deviceTypes.map { it.toString() },
            "locations" to locations,
            "languages" to languages,
            "interests" to interests,
            "hasBehavioralTargeting" to (behavioralTargeting != null),
            "hasRetargeting" to (retargeting != null),
            "hasFrequencyCap" to (frequencyCap != null),
            "hasDayParting" to (dayParting != null)
        )
    }

    companion object {
        fun create(
            ageRange: AgeRange? = null,
            gender: Gender? = null,
            userSegments: Set<UserSegment> = emptySet()
        ): AdTargeting {
            return AdTargeting(
                ageRange = ageRange,
                gender = gender,
                userSegments = userSegments
            )
        }

        fun broad(): AdTargeting {
            return AdTargeting()
        }
    }
}

/**
 * Age range for targeting
 */
data class AgeRange(
    val minAge: Int,
    val maxAge: Int
) {
    init {
        require(minAge >= 0) { "Min age must be non-negative" }
        require(maxAge >= minAge) { "Max age must be greater than or equal to min age" }
    }

    fun contains(age: Int): Boolean {
        return age in minAge..maxAge
    }

    override fun toString(): String = "$minAge-$maxAge"
}

/**
 * Behavioral targeting configuration
 */
data class BehavioralTargeting(
    val behaviors: Set<String>,
    val timeWindowDays: Int = 30,
    val minOccurrences: Int = 1
) {
    fun matchesUserBehavior(userProfile: UserProfile): Boolean {
        // This would check user's past behavior against required behaviors
        // For now, return true if behavioral targeting is configured
        return behaviors.isNotEmpty()
    }
}

/**
 * Contextual targeting configuration
 */
data class ContextualTargeting(
    val keywords: Set<String>,
    val categories: Set<String>,
    val contentTypes: Set<String>
) {
    fun matchesContext(context: Map<String, Any>): Boolean {
        // Check if current context matches targeting criteria
        val contextKeywords = context["keywords"] as? Set<String> ?: emptySet()
        val contextCategories = context["categories"] as? Set<String> ?: emptySet()
        val contextContentTypes = context["contentTypes"] as? Set<String> ?: emptySet()

        return keywords.any { it in contextKeywords } ||
               categories.any { it in contextCategories } ||
               contentTypes.any { it in contextContentTypes }
    }
}

/**
 * Retargeting configuration
 */
data class RetargetingConfig(
    val lookbackDays: Int = 30,
    val minEngagements: Int = 1,
    val excludedActions: Set<String> = emptySet()
) {
    fun shouldRetargetUser(userProfile: UserProfile): Boolean {
        // Check if user has engaged recently enough
        // This would typically check user's engagement history
        return true // Simplified for now
    }
}


/**
 * Day parting configuration
 */
data class DayParting(
    val timeSlots: List<TimeSlot>,
    val timezone: String = "America/Costa_Rica"
) {
    fun isActiveNow(): Boolean {
        val now = java.time.LocalDateTime.now(java.time.ZoneId.of(timezone)).toLocalTime()
        return timeSlots.any { it.contains(now) }
    }
}