package com.gasolinerajsm.raffleservice.domain.valueobject

/**
 * Value object representing eligibility criteria for raffle participation
 */
data class EligibilityCriteria(
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val requiredUserTypes: Set<UserType> = emptySet(),
    val excludedUserTypes: Set<UserType> = emptySet(),
    val geographicRestrictions: Set<String> = emptySet(),
    val requiresActiveSubscription: Boolean = false,
    val minAccountAgeDays: Int? = null
) {

    /**
     * Check if a user profile meets the eligibility criteria
     */
    fun isUserEligible(userProfile: UserProfile): Boolean {
        // Check age requirements
        userProfile.age?.let { age ->
            minAge?.let { if (age < it) return false }
            maxAge?.let { if (age > it) return false }
        }

        // Check user type requirements
        if (requiredUserTypes.isNotEmpty() && userProfile.userType !in requiredUserTypes) {
            return false
        }

        if (userProfile.userType in excludedUserTypes) {
            return false
        }

        // Check geographic restrictions
        if (geographicRestrictions.isNotEmpty() &&
            userProfile.location?.country !in geographicRestrictions) {
            return false
        }

        // Check subscription requirement
        if (requiresActiveSubscription && !userProfile.hasActiveSubscription) {
            return false
        }

        // Check account age
        minAccountAgeDays?.let { minDays ->
            val accountAgeDays = java.time.Duration.between(
                userProfile.createdAt,
                java.time.LocalDateTime.now()
            ).toDays()
            if (accountAgeDays < minDays) return false
        }

        return true
    }

    companion object {
        /**
         * Create open eligibility criteria (no restrictions)
         */
        fun open(): EligibilityCriteria {
            return EligibilityCriteria()
        }

        /**
         * Create criteria for premium users only
         */
        fun premiumOnly(): EligibilityCriteria {
            return EligibilityCriteria(
                requiredUserTypes = setOf(UserType.PREMIUM),
                requiresActiveSubscription = true
            )
        }
    }
}

/**
 * User type enumeration
 */
enum class UserType {
    BASIC,
    PREMIUM,
    VIP,
    EMPLOYEE,
    ADMIN
}