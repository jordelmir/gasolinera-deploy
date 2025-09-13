package com.gasolinerajsm.couponservice.domain.model

/**
 * Coupon status enumeration
 */
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

    /**
     * Check if status can be changed to another status
     */
    fun canChangeTo(newStatus: CouponStatus): Boolean {
        return when (this) {
            ACTIVE -> newStatus in listOf(INACTIVE, EXPIRED, USED_UP, CANCELLED)
            INACTIVE -> newStatus in listOf(ACTIVE, EXPIRED, CANCELLED)
            EXPIRED, USED_UP, CANCELLED -> false // Final states
        }
    }
}

/**
 * Discount type enumeration
 */
enum class DiscountType(val displayName: String) {
    FIXED_AMOUNT("Fixed Amount"),
    PERCENTAGE("Percentage"),
    NONE("No Discount");

    /**
     * Check if discount type provides monetary benefit
     */
    fun providesDiscount(): Boolean {
        return this != NONE
    }
}

/**
 * Campaign status enumeration
 */
enum class CampaignStatus(val displayName: String, val description: String) {
    DRAFT("Draft", "Campaign is being prepared"),
    ACTIVE("Active", "Campaign is running and generating coupons"),
    PAUSED("Paused", "Campaign is temporarily paused"),
    COMPLETED("Completed", "Campaign has ended successfully"),
    CANCELLED("Cancelled", "Campaign has been cancelled");

    /**
     * Check if campaign can generate coupons
     */
    fun canGenerateCoupons(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if campaign is in a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == CANCELLED
    }
}

/**
 * QR Code format enumeration
 */
enum class QRCodeFormat(val displayName: String, val mimeType: String) {
    PNG("PNG Image", "image/png"),
    SVG("SVG Vector", "image/svg+xml"),
    PDF("PDF Document", "application/pdf");

    /**
     * Check if format is image-based
     */
    fun isImageFormat(): Boolean {
        return this == PNG || this == SVG
    }
}

/**
 * Coupon generation strategy enumeration
 */
enum class CouponGenerationStrategy(val displayName: String) {
    BULK_GENERATION("Bulk Generation"),
    ON_DEMAND("On Demand"),
    SCHEDULED("Scheduled Generation");

    /**
     * Check if strategy requires immediate generation
     */
    fun requiresImmediateGeneration(): Boolean {
        return this == BULK_GENERATION
    }
}

/**
 * Fuel type enumeration for coupon applicability
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
}

/**
 * Coupon usage context enumeration
 */
enum class UsageContext(val displayName: String) {
    FUEL_PURCHASE("Fuel Purchase"),
    CONVENIENCE_STORE("Convenience Store"),
    CAR_WASH("Car Wash"),
    MAINTENANCE("Vehicle Maintenance"),
    ANY("Any Purchase");

    /**
     * Check if context is fuel-related
     */
    fun isFuelRelated(): Boolean {
        return this == FUEL_PURCHASE
    }
}