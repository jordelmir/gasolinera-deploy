package com.gasolinerajsm.couponservice.model

/**
 * Enumeration representing different types of discounts
 */
enum class DiscountType(val displayName: String, val description: String) {
    PERCENTAGE("Percentage", "Percentage discount from total amount"),
    FIXED_AMOUNT("Fixed Amount", "Fixed amount discount"),
    NONE("No Discount", "No discount applied");

    /**
     * Check if discount type requires percentage value
     */
    fun requiresPercentage(): Boolean {
        return this == PERCENTAGE
    }

    /**
     * Check if discount type requires fixed amount value
     */
    fun requiresFixedAmount(): Boolean {
        return this == FIXED_AMOUNT
    }

    /**
     * Check if discount type provides discount
     */
    fun providesDiscount(): Boolean {
        return this != NONE
    }
}