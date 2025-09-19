package com.gasolinerajsm.raffleservice.domain.valueobject

/**
 * Value object representing participation rules for a raffle
 */
data class ParticipationRules(
    val maxParticipants: Int,
    val minTicketsToParticipate: Int,
    val maxTicketsPerUser: Int,
    val allowsMultipleEntries: Boolean = false,
    val requiresVerification: Boolean = false
) {

    init {
        require(maxParticipants > 0) { "Max participants must be positive" }
        require(minTicketsToParticipate > 0) { "Min tickets to participate must be positive" }
        require(maxTicketsPerUser >= minTicketsToParticipate) {
            "Max tickets per user must be >= min tickets to participate"
        }
    }

    /**
     * Check if the rules allow a user to participate with given ticket count
     */
    fun canUserParticipate(ticketCount: Int): Boolean {
        return ticketCount >= minTicketsToParticipate &&
               ticketCount <= maxTicketsPerUser
    }

    /**
     * Get the maximum possible tickets for a user
     */
    fun getMaxTicketsForUser(): Int = maxTicketsPerUser

    /**
     * Check if rules require verification
     */
    fun requiresVerification(): Boolean = requiresVerification

    companion object {
        /**
         * Create standard participation rules
         */
        fun standard(maxParticipants: Int = 1000): ParticipationRules {
            return ParticipationRules(
                maxParticipants = maxParticipants,
                minTicketsToParticipate = 1,
                maxTicketsPerUser = 10,
                allowsMultipleEntries = true,
                requiresVerification = false
            )
        }

        /**
         * Create premium participation rules
         */
        fun premium(maxParticipants: Int = 500): ParticipationRules {
            return ParticipationRules(
                maxParticipants = maxParticipants,
                minTicketsToParticipate = 5,
                maxTicketsPerUser = 50,
                allowsMultipleEntries = true,
                requiresVerification = true
            )
        }
    }
}