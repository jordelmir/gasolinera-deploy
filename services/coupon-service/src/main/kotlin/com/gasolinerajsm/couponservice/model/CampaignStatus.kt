package com.gasolinerajsm.couponservice.model

/**
 * Enumeration representing the status of a campaign
 */
enum class CampaignStatus(val displayName: String, val description: String) {
    DRAFT("Draft", "Campaign is being prepared"),
    ACTIVE("Active", "Campaign is running and accepting coupons"),
    PAUSED("Paused", "Campaign is temporarily paused"),
    COMPLETED("Completed", "Campaign has ended successfully"),
    CANCELLED("Cancelled", "Campaign has been cancelled"),
    EXPIRED("Expired", "Campaign has expired");

    /**
     * Check if status allows modifications
     */
    fun allowsModifications(): Boolean {
        return this == DRAFT || this == PAUSED
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED
    }

    /**
     * Check if status allows activation
     */
    fun allowsActivation(): Boolean {
        return this == DRAFT || this == PAUSED
    }

    /**
     * Check if status allows coupon generation
     */
    fun allowsCouponGeneration(): Boolean {
        return this == DRAFT || this == ACTIVE
    }

    /**
     * Check if status allows coupon usage
     */
    fun allowsCouponUsage(): Boolean {
        return this == ACTIVE
    }
}