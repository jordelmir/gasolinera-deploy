package com.gasolinerajsm.couponservice.domain.valueobject

import com.gasolinerajsm.couponservice.domain.model.DiscountType
import com.gasolinerajsm.couponservice.domain.model.FuelType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * QR Code Value Object
 * Encapsulates QR code data and signature for security
 */
data class QRCode(
    val data: String,
    val signature: String
) {
    init {
        require(data.isNotBlank()) { "QR code data cannot be blank" }
        require(signature.isNotBlank()) { "QR code signature cannot be blank" }
        require(data.length <= MAX_DATA_LENGTH) { "QR code data exceeds maximum length" }
    }

    companion object {
        private const val MAX_DATA_LENGTH = 2000
        private const val HMAC_ALGORITHM = "HmacSHA256"

        /**
         * Create QR code with signature
         */
        @OptIn(ExperimentalEncodingApi::class)
        fun create(data: String, secretKey: String): QRCode {
            val signature = generateSignature(data, secretKey)
            return QRCode(data, signature)
        }

        @OptIn(ExperimentalEncodingApi::class)
        private fun generateSignature(data: String, secretKey: String): String {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            val keySpec = SecretKeySpec(secretKey.toByteArray(), HMAC_ALGORITHM)
            mac.init(keySpec)
            val signature = mac.doFinal(data.toByteArray())
            return Base64.encode(signature)
        }
    }

    /**
     * Verify QR code signature
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun verifySignature(secretKey: String): Boolean {
        val expectedSignature = generateSignature(data, secretKey)
        return signature == expectedSignature
    }

    /**
     * Get QR code URL for generation
     */
    fun getQRCodeUrl(baseUrl: String = "https://api.qrserver.com/v1/create-qr-code/"): String {
        val encodedData = java.net.URLEncoder.encode(data, "UTF-8")
        return "${baseUrl}?size=200x200&data=$encodedData"
    }

    private fun generateSignature(data: String, secretKey: String): String {
        return QRCode.generateSignature(data, secretKey)
    }
}

/**
 * Discount Value Object
 * Encapsulates discount calculation logic
 */
data class DiscountValue(
    val type: DiscountType,
    val amount: BigDecimal? = null,
    val percentage: BigDecimal? = null
) {
    init {
        when (type) {
            DiscountType.FIXED_AMOUNT -> {
                require(amount != null && amount > BigDecimal.ZERO) {
                    "Fixed amount discount must have a positive amount"
                }
                require(percentage == null) {
                    "Fixed amount discount cannot have percentage"
                }
            }
            DiscountType.PERCENTAGE -> {
                require(percentage != null && percentage > BigDecimal.ZERO && percentage <= BigDecimal(100)) {
                    "Percentage discount must be between 0 and 100"
                }
                require(amount == null) {
                    "Percentage discount cannot have fixed amount"
                }
            }
            DiscountType.NONE -> {
                require(amount == null && percentage == null) {
                    "No discount type cannot have amount or percentage"
                }
            }
        }
    }

    companion object {
        /**
         * Create fixed amount discount
         */
        fun fixedAmount(amount: BigDecimal): DiscountValue {
            return DiscountValue(DiscountType.FIXED_AMOUNT, amount = amount)
        }

        /**
         * Create percentage discount
         */
        fun percentage(percentage: BigDecimal): DiscountValue {
            return DiscountValue(DiscountType.PERCENTAGE, percentage = percentage)
        }

        /**
         * Create no discount
         */
        fun none(): DiscountValue {
            return DiscountValue(DiscountType.NONE)
        }
    }

    /**
     * Calculate discount for given purchase amount
     */
    fun calculateDiscount(purchaseAmount: BigDecimal): BigDecimal {
        return when (type) {
            DiscountType.FIXED_AMOUNT -> amount ?: BigDecimal.ZERO
            DiscountType.PERCENTAGE -> {
                val discountAmount = purchaseAmount * (percentage!! / BigDecimal(100))
                discountAmount.setScale(2, java.math.RoundingMode.HALF_UP)
            }
            DiscountType.NONE -> BigDecimal.ZERO
        }
    }

    /**
     * Get display string for discount
     */
    fun getDisplayString(): String {
        return when (type) {
            DiscountType.FIXED_AMOUNT -> "$${amount?.setScale(2)}"
            DiscountType.PERCENTAGE -> "${percentage?.setScale(1)}%"
            DiscountType.NONE -> "No discount"
        }
    }

    /**
     * Check if discount provides value
     */
    fun providesDiscount(): Boolean {
        return type.providesDiscount()
    }
}

/**
 * Validity Period Value Object
 * Encapsulates time-based validity logic
 */
data class ValidityPeriod(
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime
) {
    init {
        require(!validFrom.isAfter(validUntil)) {
            "Valid from date cannot be after valid until date"
        }
    }

    /**
     * Check if period is valid at specific time
     */
    fun isValidAt(dateTime: LocalDateTime): Boolean {
        return !dateTime.isBefore(validFrom) && !dateTime.isAfter(validUntil)
    }

    /**
     * Check if period is currently valid
     */
    fun isCurrentlyValid(): Boolean {
        return isValidAt(LocalDateTime.now())
    }

    /**
     * Check if period is expired
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(validUntil)
    }

    /**
     * Check if period is not yet valid
     */
    fun isNotYetValid(): Boolean {
        return LocalDateTime.now().isBefore(validFrom)
    }

    /**
     * Get duration in days
     */
    fun getDurationInDays(): Long {
        return java.time.Duration.between(validFrom, validUntil).toDays()
    }

    /**
     * Get remaining days
     */
    fun getRemainingDays(): Long {
        val now = LocalDateTime.now()
        return if (now.isAfter(validUntil)) {
            0
        } else {
            java.time.Duration.between(now, validUntil).toDays()
        }
    }

    /**
     * Check if period overlaps with another period
     */
    fun overlapsWith(other: ValidityPeriod): Boolean {
        return validFrom.isBefore(other.validUntil) && validUntil.isAfter(other.validFrom)
    }
}

/**
 * Usage Rules Value Object
 * Encapsulates coupon usage limitations
 */
data class UsageRules(
    val maxUses: Int? = null,
    val currentUses: Int = 0,
    val maxUsesPerUser: Int? = null,
    val cooldownPeriodMinutes: Int? = null
) {
    init {
        require(currentUses >= 0) { "Current uses cannot be negative" }
        require(maxUses?.let { it > 0 } ?: true) { "Max uses must be positive" }
        require(maxUsesPerUser?.let { it > 0 } ?: true) { "Max uses per user must be positive" }
        require(cooldownPeriodMinutes?.let { it > 0 } ?: true) { "Cooldown period must be positive" }
        require(maxUses?.let { currentUses <= it } ?: true) { "Current uses cannot exceed max uses" }
    }

    /**
     * Check if coupon can be used
     */
    fun canBeUsed(): Boolean {
        return !isMaxUsesReached()
    }

    /**
     * Check if maximum uses have been reached
     */
    fun isMaxUsesReached(): Boolean {
        return maxUses?.let { currentUses >= it } ?: false
    }

    /**
     * Get remaining uses
     */
    fun getRemainingUses(): Int? {
        return maxUses?.let { it - currentUses }
    }

    /**
     * Get usage percentage
     */
    fun getUsagePercentage(): Double {
        return maxUses?.let { (currentUses.toDouble() / it * 100) } ?: 0.0
    }

    /**
     * Increment usage count
     */
    fun incrementUsage(count: Int = 1): UsageRules {
        return this.copy(currentUses = currentUses + count)
    }

    /**
     * Check if user can use coupon based on per-user limits
     */
    fun canUserUse(userUsageCount: Int): Boolean {
        return maxUsesPerUser?.let { userUsageCount < it } ?: true
    }
}

/**
 * Applicability Rules Value Object
 * Encapsulates rules for when coupon can be applied
 */
data class ApplicabilityRules(
    val minimumPurchaseAmount: BigDecimal? = null,
    val maximumPurchaseAmount: BigDecimal? = null,
    val applicableFuelTypes: Set<FuelType> = emptySet(),
    val applicableStationIds: Set<String> = emptySet(),
    val excludedStationIds: Set<String> = emptySet(),
    val applicableTimeRanges: List<TimeRange> = emptyList()
) {
    init {
        minimumPurchaseAmount?.let { min ->
            require(min >= BigDecimal.ZERO) { "Minimum purchase amount cannot be negative" }
        }
        maximumPurchaseAmount?.let { max ->
            require(max > BigDecimal.ZERO) { "Maximum purchase amount must be positive" }
            minimumPurchaseAmount?.let { min ->
                require(max >= min) { "Maximum purchase amount cannot be less than minimum" }
            }
        }
        require(applicableStationIds.intersect(excludedStationIds).isEmpty()) {
            "Station cannot be both applicable and excluded"
        }
    }

    /**
     * Check if coupon applies to given purchase parameters
     */
    fun appliesTo(
        purchaseAmount: BigDecimal,
        fuelType: String? = null,
        stationId: String? = null,
        purchaseTime: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        // Check purchase amount
        if (!isPurchaseAmountValid(purchaseAmount)) return false

        // Check fuel type
        if (!isFuelTypeApplicable(fuelType)) return false

        // Check station
        if (!isStationApplicable(stationId)) return false

        // Check time ranges
        if (!isTimeApplicable(purchaseTime)) return false

        return true
    }

    private fun isPurchaseAmountValid(amount: BigDecimal): Boolean {
        minimumPurchaseAmount?.let { if (amount < it) return false }
        maximumPurchaseAmount?.let { if (amount > it) return false }
        return true
    }

    private fun isFuelTypeApplicable(fuelType: String?): Boolean {
        if (applicableFuelTypes.isEmpty()) return true
        return fuelType?.let { type ->
            FuelType.fromCode(type)?.let { applicableFuelTypes.contains(it) } ?: false
        } ?: true
    }

    private fun isStationApplicable(stationId: String?): Boolean {
        stationId?.let { id ->
            if (excludedStationIds.contains(id)) return false
            if (applicableStationIds.isNotEmpty() && !applicableStationIds.contains(id)) return false
        }
        return true
    }

    private fun isTimeApplicable(purchaseTime: LocalDateTime): Boolean {
        if (applicableTimeRanges.isEmpty()) return true
        return applicableTimeRanges.any { it.contains(purchaseTime) }
    }
}

/**
 * Time Range Value Object
 * Represents a time range for coupon applicability
 */
data class TimeRange(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
) {
    init {
        require(!startTime.isAfter(endTime)) {
            "Start time cannot be after end time"
        }
    }

    /**
     * Check if time range contains given date time
     */
    fun contains(dateTime: LocalDateTime): Boolean {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime)
    }

    /**
     * Check if this range overlaps with another
     */
    fun overlapsWith(other: TimeRange): Boolean {
        return startTime.isBefore(other.endTime) && endTime.isAfter(other.startTime)
    }
}