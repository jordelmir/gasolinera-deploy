package com.gasolinerajsm.redemptionservice.model

/**
 * Redemption type enumeration
 */
enum class RedemptionType(val displayName: String, val description: String) {
    FUEL("Fuel", "Fuel purchase redemption"),
    MERCHANDISE("Merchandise", "Store merchandise redemption"),
    SERVICE("Service", "Service redemption"),
    DISCOUNT("Discount", "General discount redemption"),
    CASHBACK("Cashback", "Cashback redemption");

    /**
     * Check if the redemption type is fuel-related
     */
    fun isFuelRelated(): Boolean {
        return this == FUEL
    }

    /**
     * Check if the redemption type is merchandise-related
     */
    fun isMerchandiseRelated(): Boolean {
        return this == MERCHANDISE
    }

    /**
     * Check if the redemption type provides cashback
     */
    fun providesCashback(): Boolean {
        return this == CASHBACK
    }
}