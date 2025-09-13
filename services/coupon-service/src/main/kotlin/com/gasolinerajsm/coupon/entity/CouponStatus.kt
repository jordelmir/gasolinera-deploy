package com.gasolinerajsm.coupon.entity

enum class CouponStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Coupon is active and can be used"),
    INACTIVE("Inactive", "Coupon is temporarily disabled"),
    EXPIRED("Expired", "Coupon has passed its expiration date"),
    USED_UP("Used Up", "Coupon has reached its maximum usage limit"),
    CANCELLED("Cancelled", "Coupon has been cancelled by administrator");

    /**
     * Check if the status allows coupon usage
     */
    fun allowsUsage(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if the status is a final state
     */
    fun isFinalState(): Boolean {
        return this == EXPIRED || this == USED_UP || this == CANCELLED
    }
}