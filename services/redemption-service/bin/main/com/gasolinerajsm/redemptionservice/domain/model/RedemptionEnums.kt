package com.gasolinerajsm.redemptionservice.domain.model

/**
 * Redemption status enumeration
 */
enum class RedemptionStatus(val displayName: String, val description: String) {
    PENDING("Pending", "Redemption is waiting to be processed"),
    IN_PROGRESS("In Progress", "Redemption is currently being processed"),
    COMPLETED("Completed", "Redemption has been successfully completed"),
    FAILED("Failed", "Redemption has failed due to an error"),
    CANCELLED("Cancelled", "Redemption has been cancelled");

    /**
     * Check if the status is a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == FAILED || this == CANCELLED
    }

    /**
     * Check if the status allows modifications
     */
    fun allowsModifications(): Boolean {
        return this == PENDING
    }

    /**
     * Check if the status indicates success
     */
    fun isSuccessful(): Boolean {
        return this == COMPLETED
    }

    /**
     * Check if status can transition to another status
     */
    fun canTransitionTo(newStatus: RedemptionStatus): Boolean {
        return when (this) {
            PENDING -> newStatus in listOf(IN_PROGRESS, CANCELLED)
            IN_PROGRESS -> newStatus in listOf(COMPLETED, FAILED, CANCELLED)
            COMPLETED, FAILED, CANCELLED -> false // Final states
        }
    }
}

/**
 * Ticket status enumeration
 */
enum class TicketStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Ticket is active and can be used"),
    USED("Used", "Ticket has been used in a raffle"),
    EXPIRED("Expired", "Ticket has expired"),
    CANCELLED("Cancelled", "Ticket has been cancelled"),
    TRANSFERRED("Transferred", "Ticket has been transferred to another user"),
    SUSPENDED("Suspended", "Ticket is temporarily suspended");

    /**
     * Check if status allows ticket usage
     */
    fun allowsUsage(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status is final (cannot be changed)
     */
    fun isFinalStatus(): Boolean {
        return this == USED || this == EXPIRED || this == CANCELLED
    }

    /**
     * Check if status allows transfer
     */
    fun allowsTransfer(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status can transition to another status
     */
    fun canTransitionTo(newStatus: TicketStatus): Boolean {
        return when (this) {
            ACTIVE -> newStatus in listOf(USED, EXPIRED, CANCELLED, TRANSFERRED, SUSPENDED)
            SUSPENDED -> newStatus in listOf(ACTIVE, EXPIRED, CANCELLED)
            TRANSFERRED -> newStatus in listOf(USED, EXPIRED, CANCELLED, SUSPENDED)
            USED, EXPIRED, CANCELLED -> false // Final states
        }
    }
}

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

/**
 * Fuel type enumeration for redemptions
 */
enum class FuelType(val displayName: String, val code: String) {
    REGULAR("Regular", "REG"),
    PREMIUM("Premium", "PREM"),
    SUPER_PREMIUM("Super Premium", "SUPER"),
    DIESEL("Diesel", "DIESEL"),
    E85("E85 Ethanol", "E85"),
    ELECTRIC("Electric Charging", "ELEC");

    companion object {
        fun fromCode(code: String): FuelType? {
            return values().find { it.code.equals(code, ignoreCase = true) }
        }
    }

    /**
     * Check if fuel type is gasoline-based
     */
    fun isGasoline(): Boolean {
        return this in listOf(REGULAR, PREMIUM, SUPER_PREMIUM, E85)
    }

    /**
     * Check if fuel type requires special handling
     */
    fun requiresSpecialHandling(): Boolean {
        return this in listOf(DIESEL, E85, ELECTRIC)
    }
}

/**
 * Payment method enumeration
 */
enum class PaymentMethod(val displayName: String, val code: String) {
    CASH("Cash", "CASH"),
    CREDIT_CARD("Credit Card", "CC"),
    DEBIT_CARD("Debit Card", "DC"),
    MOBILE_PAYMENT("Mobile Payment", "MOBILE"),
    DIGITAL_WALLET("Digital Wallet", "WALLET"),
    BANK_TRANSFER("Bank Transfer", "TRANSFER"),
    LOYALTY_POINTS("Loyalty Points", "POINTS");

    /**
     * Check if payment method is digital
     */
    fun isDigital(): Boolean {
        return this in listOf(MOBILE_PAYMENT, DIGITAL_WALLET, BANK_TRANSFER)
    }

    /**
     * Check if payment method is card-based
     */
    fun isCardBased(): Boolean {
        return this in listOf(CREDIT_CARD, DEBIT_CARD)
    }
}

/**
 * Ticket source type enumeration
 */
enum class TicketSourceType(val displayName: String) {
    COUPON_REDEMPTION("Coupon Redemption"),
    PURCHASE_BONUS("Purchase Bonus"),
    LOYALTY_REWARD("Loyalty Reward"),
    PROMOTIONAL_GIFT("Promotional Gift"),
    REFERRAL_BONUS("Referral Bonus"),
    SPECIAL_EVENT("Special Event");

    /**
     * Check if source type is purchase-related
     */
    fun isPurchaseRelated(): Boolean {
        return this in listOf(COUPON_REDEMPTION, PURCHASE_BONUS)
    }

    /**
     * Check if source type is promotional
     */
    fun isPromotional(): Boolean {
        return this in listOf(PROMOTIONAL_GIFT, SPECIAL_EVENT)
    }
}

/**
 * Discount type enumeration
 */
enum class DiscountType(val displayName: String) {
    FIXED_AMOUNT("Fixed Amount"),
    PERCENTAGE("Percentage"),
    FUEL_DISCOUNT("Fuel Discount"),
    CASHBACK("Cashback"),
    NONE("No Discount");

    /**
     * Check if discount type provides monetary benefit
     */
    fun providesDiscount(): Boolean {
        return this != NONE
    }

    /**
     * Check if discount type is fuel-specific
     */
    fun isFuelSpecific(): Boolean {
        return this == FUEL_DISCOUNT
    }
}