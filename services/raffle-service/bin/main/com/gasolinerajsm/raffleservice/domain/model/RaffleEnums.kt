package com.gasolinerajsm.raffleservice.domain.model

/**
 * Raffle type enumeration
 */
enum class RaffleType(val displayName: String, val description: String) {
    DAILY("Daily Raffle", "Daily raffle with 24-hour cycle"),
    WEEKLY("Weekly Raffle", "Weekly raffle with 7-day cycle"),
    MONTHLY("Monthly Raffle", "Monthly raffle with 30-day cycle"),
    SPECIAL("Special Event", "Special event raffle"),
    INSTANT_WIN("Instant Win", "Instant win raffle"),
    TIERED("Tiered", "Tiered raffle with multiple prize levels"),
    PROGRESSIVE("Progressive", "Progressive jackpot raffle"),
    SEASONAL("Seasonal", "Seasonal themed raffle");

    /**
     * Get typical duration in days
     */
    fun getTypicalDurationDays(): Int {
        return when (this) {
            DAILY -> 1
            WEEKLY -> 7
            MONTHLY -> 30
            SPECIAL -> 14
            INSTANT_WIN -> 1
            TIERED -> 7
            PROGRESSIVE -> 30
            SEASONAL -> 90
        }
    }

    /**
     * Check if raffle type is recurring
     */
    fun isRecurring(): Boolean {
        return this in listOf(DAILY, WEEKLY, MONTHLY)
    }

    /**
     * Check if raffle type supports instant wins
     */
    fun supportsInstantWin(): Boolean {
        return this == INSTANT_WIN || this == PROGRESSIVE || this == TIERED
    }
}

/**
 * Raffle status enumeration
 */
enum class RaffleStatus(val displayName: String, val description: String) {
    DRAFT("Draft", "Raffle is being prepared"),
    ACTIVE("Active", "Raffle is running and accepting entries"),
    PAUSED("Paused", "Raffle is temporarily paused"),
    DRAWING("Drawing", "Draw is in progress"),
    COMPLETED("Completed", "Raffle has been completed"),
    CANCELLED("Cancelled", "Raffle has been cancelled");

    /**
     * Check if status allows registration
     */
    fun allowsRegistration(): Boolean {
        return this == ACTIVE
    }

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
        return this == COMPLETED || this == CANCELLED
    }

    /**
     * Check if status can transition to another status
     */
    fun canTransitionTo(newStatus: RaffleStatus): Boolean {
        return when (this) {
            DRAFT -> newStatus in listOf(ACTIVE, CANCELLED)
            ACTIVE -> newStatus in listOf(PAUSED, DRAWING, COMPLETED, CANCELLED)
            PAUSED -> newStatus in listOf(ACTIVE, COMPLETED, CANCELLED)
            DRAWING -> newStatus in listOf(COMPLETED, CANCELLED)
            COMPLETED, CANCELLED -> false // Final states
        }
    }
}

/**
 * Prize type enumeration
 */
enum class PrizeType(val displayName: String, val description: String) {
    CASH("Cash Prize", "Monetary cash prize"),
    GIFT_CARD("Gift Card", "Gift card or voucher"),
    CREDIT("Account Credit", "Credit to user account"),
    PHYSICAL_ITEM("Physical Item", "Physical product or item"),
    MERCHANDISE("Merchandise", "Branded merchandise"),
    SERVICE("Service", "Service or experience"),
    DISCOUNT("Discount", "Discount coupon or offer"),
    POINTS("Points", "Loyalty or reward points"),
    FUEL_CREDIT("Fuel Credit", "Free fuel or fuel discount"),
    OTHER("Other", "Other type of prize");

    /**
     * Check if prize type is digital
     */
    fun isDigital(): Boolean {
        return this in listOf(GIFT_CARD, CREDIT, DISCOUNT, POINTS, FUEL_CREDIT)
    }

    /**
     * Check if prize type requires physical delivery
     */
    fun requiresPhysicalDelivery(): Boolean {
        return this in listOf(PHYSICAL_ITEM, MERCHANDISE)
    }

    /**
     * Check if prize type has monetary value
     */
    fun hasMonetaryValue(): Boolean {
        return this in listOf(CASH, GIFT_CARD, CREDIT, FUEL_CREDIT)
    }
}

/**
 * Prize status enumeration
 */
enum class PrizeStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Prize is active and available"),
    INACTIVE("Inactive", "Prize is temporarily inactive"),
    EXPIRED("Expired", "Prize has expired"),
    EXHAUSTED("Exhausted", "All prize quantities have been awarded"),
    CANCELLED("Cancelled", "Prize has been cancelled");

    /**
     * Check if status allows awarding
     */
    fun allowsAwarding(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this in listOf(EXPIRED, EXHAUSTED, CANCELLED)
    }
}

/**
 * Draw method enumeration
 */
enum class DrawMethod(val displayName: String, val description: String) {
    RANDOM("Random", "Pure random selection"),
    WEIGHTED("Weighted", "Weighted by ticket count"),
    PROBABILITY("Probability", "Based on probability distribution"),
    SEQUENTIAL("Sequential", "Sequential selection"),
    LOTTERY("Lottery", "Traditional lottery style"),
    INSTANT("Instant", "Instant win determination");

    /**
     * Check if method is deterministic
     */
    fun isDeterministic(): Boolean {
        return this == SEQUENTIAL
    }

    /**
     * Check if method supports weighting
     */
    fun supportsWeighting(): Boolean {
        return this in listOf(WEIGHTED, PROBABILITY)
    }

    /**
     * Check if method is instant
     */
    fun isInstant(): Boolean {
        return this == INSTANT
    }
}

/**
 * Winner selection criteria enumeration
 */
enum class WinnerSelectionCriteria(val displayName: String) {
    FIRST_DRAWN("First Drawn"),
    HIGHEST_TICKETS("Highest Ticket Count"),
    LONGEST_PARTICIPATION("Longest Participation"),
    RANDOM_FROM_POOL("Random from Pool"),
    PROBABILITY_BASED("Probability Based");

    /**
     * Check if criteria is ticket-based
     */
    fun isTicketBased(): Boolean {
        return this in listOf(HIGHEST_TICKETS, PROBABILITY_BASED)
    }

    /**
     * Check if criteria considers participation history
     */
    fun considersHistory(): Boolean {
        return this == LONGEST_PARTICIPATION
    }
}

/**
 * Entry method enumeration
 */
enum class EntryMethod(val displayName: String, val description: String) {
    TICKET_REDEMPTION("Ticket Redemption", "Entry via raffle tickets"),
    DIRECT_ENTRY("Direct Entry", "Direct entry without tickets"),
    PURCHASE_BASED("Purchase Based", "Entry based on purchases"),
    ACTIVITY_BASED("Activity Based", "Entry based on activities"),
    INVITATION_ONLY("Invitation Only", "Entry by invitation only"),
    SOCIAL_MEDIA("Social Media", "Entry via social media actions");

    /**
     * Check if method requires tickets
     */
    fun requiresTickets(): Boolean {
        return this == TICKET_REDEMPTION
    }

    /**
     * Check if method is activity-based
     */
    fun isActivityBased(): Boolean {
        return this in listOf(PURCHASE_BASED, ACTIVITY_BASED, SOCIAL_MEDIA)
    }

    /**
     * Check if method is restricted
     */
    fun isRestricted(): Boolean {
        return this == INVITATION_ONLY
    }
}

/**
 * Verification level enumeration
 */
enum class VerificationLevel(val displayName: String, val description: String) {
    NONE("No Verification", "No verification required"),
    EMAIL("Email Verification", "Email verification required"),
    PHONE("Phone Verification", "Phone number verification required"),
    IDENTITY("Identity Verification", "Government ID verification required"),
    ADDRESS("Address Verification", "Address verification required"),
    FULL("Full Verification", "Complete identity and address verification");

    /**
     * Get verification strength score
     */
    fun getStrengthScore(): Int {
        return when (this) {
            NONE -> 0
            EMAIL -> 1
            PHONE -> 2
            IDENTITY -> 4
            ADDRESS -> 3
            FULL -> 5
        }
    }

    /**
     * Check if verification includes identity
     */
    fun includesIdentity(): Boolean {
        return this in listOf(IDENTITY, FULL)
    }

    /**
     * Check if verification includes contact info
     */
    fun includesContactInfo(): Boolean {
        return this in listOf(EMAIL, PHONE, FULL)
    }
}

/**
 * Age group enumeration for eligibility
 */
enum class AgeGroup(val displayName: String, val minAge: Int, val maxAge: Int?) {
    MINOR("Minor (Under 18)", 0, 17),
    YOUNG_ADULT("Young Adult (18-25)", 18, 25),
    ADULT("Adult (26-54)", 26, 54),
    SENIOR("Senior (55+)", 55, null),
    ALL_ADULTS("All Adults (18+)", 18, null),
    ALL_AGES("All Ages", 0, null);

    /**
     * Check if age falls within this group
     */
    fun includesAge(age: Int): Boolean {
        return age >= minAge && (maxAge == null || age <= maxAge)
    }

    /**
     * Check if group allows minors
     */
    fun allowsMinors(): Boolean {
        return minAge < 18
    }
}