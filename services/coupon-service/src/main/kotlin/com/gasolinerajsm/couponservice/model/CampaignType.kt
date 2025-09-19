package com.gasolinerajsm.couponservice.model

/**
 * Campaign type enumeration
 */
enum class CampaignType(val displayName: String, val description: String) {
    PROMOTIONAL("Promotional", "Promotional discount campaign"),
    SEASONAL("Seasonal", "Seasonal discount campaign"),
    LOYALTY("Loyalty", "Loyalty reward campaign"),
    REFERRAL("Referral", "Referral bonus campaign"),
    WELCOME("Welcome", "Welcome bonus campaign"),
    CLEARANCE("Clearance", "Clearance sale campaign"),
    DISCOUNT("Discount", "General discount campaign"),
    RAFFLE_ONLY("Raffle Only", "Campaign that only provides raffle tickets"),
    GRAND_OPENING("Grand Opening", "Grand opening special campaign");

    /**
     * Check if campaign type provides discounts
     */
    fun providesDiscounts(): Boolean {
        return this != RAFFLE_ONLY
    }

    /**
     * Check if campaign type provides raffle tickets
     */
    fun providesRaffleTickets(): Boolean {
        return true // All campaign types provide raffle tickets
    }
}