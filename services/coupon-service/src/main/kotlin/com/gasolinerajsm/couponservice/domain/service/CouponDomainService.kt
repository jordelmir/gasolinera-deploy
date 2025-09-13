package com.gasolinerajsm.couponservice.domain.service

import com.gasolinerajsm.couponservice.domain.model.*
import com.gasolinerajsm.couponservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Coupon Domain Service
 * Contains complex business logic that doesn't belong to a single entity
 */
class CouponDomainService {

    companion object {
        private const val MIN_VALIDITY_HOURS = 1
        private const val MAX_VALIDITY_DAYS = 365
        private const val MAX_DISCOUNT_PERCENTAGE = 50.0
        private const val MIN_PURCHASE_AMOUNT = 1.0
        private const val MAX_PURCHASE_AMOUNT = 10000.0
    }

    /**
     * Validate coupon creation data
     */
    fun validateCouponCreation(
        campaignId: CampaignId,
        discountValue: DiscountValue,
        validityPeriod: ValidityPeriod,
        usageRules: UsageRules,
        applicabilityRules: ApplicabilityRules
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate discount value
        val discountValidation = validateDiscountValue(discountValue)
        if (!discountValidation.isSuccess) {
            errors.add(discountValidation.message)
        }

        // Validate validity period
        val validityValidation = validateValidityPeriod(validityPeriod)
        if (!validityValidation.isSuccess) {
            errors.add(validityValidation.message)
        }

        // Validate usage rules
        val usageValidation = validateUsageRules(usageRules)
        if (!usageValidation.isSuccess) {
            errors.add(usageValidation.message)
        }

        // Validate applicability rules
        val applicabilityValidation = validateApplicabilityRules(applicabilityRules)
        if (!applicabilityValidation.isSuccess) {
            errors.add(applicabilityValidation.message)
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Coupon creation data is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Validate discount value
     */
    fun validateDiscountValue(discountValue: DiscountValue): ValidationResult {
        return when (discountValue.type) {
            DiscountType.FIXED_AMOUNT -> {
                val amount = discountValue.amount!!
                when {
                    amount <= BigDecimal.ZERO ->
                        ValidationResult.failure("Discount amount must be positive")
                    amount > BigDecimal(MAX_PURCHASE_AMOUNT) ->
                        ValidationResult.failure("Discount amount is too large")
                    else -> ValidationResult.success("Fixed amount discount is valid")
                }
            }
            DiscountType.PERCENTAGE -> {
                val percentage = discountValue.percentage!!
                when {
                    percentage <= BigDecimal.ZERO ->
                        ValidationResult.failure("Discount percentage must be positive")
                    percentage > BigDecimal(MAX_DISCOUNT_PERCENTAGE) ->
                        ValidationResult.failure("Discount percentage cannot exceed $MAX_DISCOUNT_PERCENTAGE%")
                    else -> ValidationResult.success("Percentage discount is valid")
                }
            }
            DiscountType.NONE -> ValidationResult.success("No discount is valid")
        }
    }

    /**
     * Validate validity period
     */
    fun validateValidityPeriod(validityPeriod: ValidityPeriod): ValidationResult {
        val now = LocalDateTime.now()
        val durationHours = java.time.Duration.between(
            validityPeriod.validFrom,
            validityPeriod.validUntil
        ).toHours()

        return when {
            validityPeriod.validUntil.isBefore(now) ->
                ValidationResult.failure("Validity period cannot end in the past")
            durationHours < MIN_VALIDITY_HOURS ->
                ValidationResult.failure("Validity period must be at least $MIN_VALIDITY_HOURS hour(s)")
            validityPeriod.getDurationInDays() > MAX_VALIDITY_DAYS ->
                ValidationResult.failure("Validity period cannot exceed $MAX_VALIDITY_DAYS days")
            else -> ValidationResult.success("Validity period is valid")
        }
    }

    /**
     * Validate usage rules
     */
    fun validateUsageRules(usageRules: UsageRules): ValidationResult {
        val errors = mutableListOf<String>()

        usageRules.maxUses?.let { maxUses ->
            if (maxUses <= 0) {
                errors.add("Max uses must be positive")
            }
            if (maxUses > 1000) {
                errors.add("Max uses cannot exceed 1000")
            }
        }

        usageRules.maxUsesPerUser?.let { maxUsesPerUser ->
            if (maxUsesPerUser <= 0) {
                errors.add("Max uses per user must be positive")
            }
            usageRules.maxUses?.let { maxUses ->
                if (maxUsesPerUser > maxUses) {
                    errors.add("Max uses per user cannot exceed total max uses")
                }
            }
        }

        usageRules.cooldownPeriodMinutes?.let { cooldown ->
            if (cooldown < 1) {
                errors.add("Cooldown period must be at least 1 minute")
            }
            if (cooldown > 1440) { // 24 hours
                errors.add("Cooldown period cannot exceed 24 hours")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Usage rules are valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Validate applicability rules
     */
    fun validateApplicabilityRules(applicabilityRules: ApplicabilityRules): ValidationResult {
        val errors = mutableListOf<String>()

        applicabilityRules.minimumPurchaseAmount?.let { minAmount ->
            if (minAmount < BigDecimal(MIN_PURCHASE_AMOUNT)) {
                errors.add("Minimum purchase amount is too low")
            }
        }

        applicabilityRules.maximumPurchaseAmount?.let { maxAmount ->
            if (maxAmount > BigDecimal(MAX_PURCHASE_AMOUNT)) {
                errors.add("Maximum purchase amount is too high")
            }
        }

        if (applicabilityRules.applicableStationIds.intersect(applicabilityRules.excludedStationIds).isNotEmpty()) {
            errors.add("Station cannot be both applicable and excluded")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Applicability rules are valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Calculate optimal discount for competitive pricing
     */
    fun calculateOptimalDiscount(
        basePrice: BigDecimal,
        competitorDiscounts: List<BigDecimal>,
        strategy: DiscountStrategy = DiscountStrategy.COMPETITIVE
    ): DiscountValue {
        return when (strategy) {
            DiscountStrategy.COMPETITIVE -> {
                val avgCompetitorDiscount = if (competitorDiscounts.isNotEmpty()) {
                    competitorDiscounts.fold(BigDecimal.ZERO) { acc, discount ->
                        acc + discount
                    } / BigDecimal(competitorDiscounts.size)
                } else {
                    BigDecimal("5.0") // Default 5% if no competitor data
                }

                val competitivePercentage = (avgCompetitorDiscount + BigDecimal("1.0"))
                    .coerceAtMost(BigDecimal(MAX_DISCOUNT_PERCENTAGE))

                DiscountValue.percentage(competitivePercentage)
            }
            DiscountStrategy.AGGRESSIVE -> {
                val maxCompetitorDiscount = competitorDiscounts.maxOrNull() ?: BigDecimal("10.0")
                val aggressivePercentage = (maxCompetitorDiscount + BigDecimal("2.0"))
                    .coerceAtMost(BigDecimal(MAX_DISCOUNT_PERCENTAGE))

                DiscountValue.percentage(aggressivePercentage)
            }
            DiscountStrategy.CONSERVATIVE -> {
                val conservativePercentage = BigDecimal("3.0")
                DiscountValue.percentage(conservativePercentage)
            }
            DiscountStrategy.FIXED_VALUE -> {
                val fixedAmount = basePrice * BigDecimal("0.05") // 5% of base price
                DiscountValue.fixedAmount(fixedAmount.setScale(2, java.math.RoundingMode.HALF_UP))
            }
        }
    }

    /**
     * Determine coupon expiration strategy
     */
    fun determineExpirationStrategy(
        campaignDuration: Long, // in days
        targetUsageRate: Double = 0.8
    ): ValidityPeriod {
        val now = LocalDateTime.now()

        // Calculate optimal coupon validity based on campaign duration and target usage
        val couponValidityDays = when {
            campaignDuration <= 7 -> campaignDuration // Short campaigns: full duration
            campaignDuration <= 30 -> (campaignDuration * 0.8).toLong() // Medium: 80% of campaign
            else -> 30L // Long campaigns: max 30 days per coupon
        }

        val validFrom = now.plusHours(1) // Start 1 hour from now
        val validUntil = validFrom.plusDays(couponValidityDays)

        return ValidityPeriod(validFrom, validUntil)
    }

    /**
     * Calculate raffle tickets based on purchase amount and discount
     */
    fun calculateRaffleTickets(
        purchaseAmount: BigDecimal,
        discountApplied: BigDecimal,
        baseTickets: Int = 1,
        bonusThreshold: BigDecimal = BigDecimal("50.0")
    ): Int {
        var tickets = baseTickets

        // Bonus tickets for large purchases
        if (purchaseAmount >= bonusThreshold) {
            val bonusMultiplier = (purchaseAmount / bonusThreshold).toInt()
            tickets += bonusMultiplier
        }

        // Bonus tickets for significant discounts
        if (discountApplied >= BigDecimal("10.0")) {
            tickets += 1
        }

        return tickets.coerceAtMost(10) // Cap at 10 tickets per coupon
    }

    /**
     * Validate coupon usage context
     */
    fun validateUsageContext(
        coupon: Coupon,
        purchaseAmount: BigDecimal,
        fuelType: String?,
        stationId: String?,
        userId: String?,
        userPreviousUsages: Int = 0
    ): ValidationResult {
        // Basic coupon validity
        if (!coupon.isValid()) {
            return ValidationResult.failure("Coupon is not valid")
        }

        // Check applicability rules
        if (!coupon.applicabilityRules.appliesTo(purchaseAmount, fuelType, stationId)) {
            return ValidationResult.failure("Coupon is not applicable to this purchase")
        }

        // Check user-specific usage limits
        if (!coupon.usageRules.canUserUse(userPreviousUsages)) {
            return ValidationResult.failure("User has exceeded usage limit for this coupon")
        }

        return ValidationResult.success("Coupon can be used in this context")
    }
}

/**
 * Discount strategy enumeration
 */
enum class DiscountStrategy {
    COMPETITIVE,
    AGGRESSIVE,
    CONSERVATIVE,
    FIXED_VALUE
}

/**
 * Validation result for domain operations
 */
data class ValidationResult(
    val isSuccess: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = ValidationResult(true, message)
        fun failure(message: String) = ValidationResult(false, message)
    }
}