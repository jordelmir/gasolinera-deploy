package com.gasolinerajsm.couponservice.domain.model

import com.gasolinerajsm.couponservice.domain.event.DomainEvent
import com.gasolinerajsm.couponservice.domain.event.CouponCreatedEvent
import com.gasolinerajsm.couponservice.domain.event.CouponUsedEvent
import com.gasolinerajsm.couponservice.domain.event.CouponStatusChangedEvent
import com.gasolinerajsm.couponservice.domain.valueobject.CouponId
import com.gasolinerajsm.couponservice.domain.valueobject.CouponCode
import com.gasolinerajsm.couponservice.domain.valueobject.QRCode
import com.gasolinerajsm.couponservice.domain.valueobject.DiscountValue
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Coupon Domain Entity - Core business logic
 * Represents a digital coupon with QR code and validation rules
 */
data class Coupon(
    val id: CouponId,
    val campaignId: CampaignId,
    val couponCode: CouponCode,
    val qrCode: QRCode,
    val status: CouponStatus = CouponStatus.ACTIVE,
    val discountValue: DiscountValue,
    val raffleTickets: Int = 1,
    val validityPeriod: ValidityPeriod,
    val usageRules: UsageRules,
    val applicabilityRules: ApplicabilityRules,
    val termsAndConditions: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new coupon
         */
        fun create(
            campaignId: CampaignId,
            couponCode: CouponCode,
            qrCode: QRCode,
            discountValue: DiscountValue,
            raffleTickets: Int,
            validityPeriod: ValidityPeriod,
            usageRules: UsageRules,
            applicabilityRules: ApplicabilityRules,
            termsAndConditions: String? = null,
            metadata: Map<String, String> = emptyMap()
        ): Coupon {
            val coupon = Coupon(
                id = CouponId.generate(),
                campaignId = campaignId,
                couponCode = couponCode,
                qrCode = qrCode,
                discountValue = discountValue,
                raffleTickets = raffleTickets,
                validityPeriod = validityPeriod,
                usageRules = usageRules,
                applicabilityRules = applicabilityRules,
                termsAndConditions = termsAndConditions,
                metadata = metadata
            )

            coupon.addDomainEvent(
                CouponCreatedEvent(
                    couponId = coupon.id,
                    campaignId = coupon.campaignId,
                    couponCode = coupon.couponCode,
                    discountValue = coupon.discountValue,
                    validityPeriod = coupon.validityPeriod,
                    occurredAt = LocalDateTime.now()
                )
            )

            return coupon
        }
    }

    /**
     * Check if the coupon is currently valid
     */
    fun isValid(): Boolean {
        val now = LocalDateTime.now()
        return status == CouponStatus.ACTIVE &&
                validityPeriod.isValidAt(now) &&
                !usageRules.isMaxUsesReached()
    }

    /**
     * Check if the coupon can be used for a specific purchase
     */
    fun canBeUsedFor(
        purchaseAmount: BigDecimal,
        fuelType: String? = null,
        stationId: String? = null
    ): ValidationResult {
        if (!isValid()) {
            return ValidationResult.failure("Coupon is not valid")
        }

        if (!usageRules.canBeUsed()) {
            return ValidationResult.failure("Coupon usage limit reached")
        }

        if (!applicabilityRules.appliesTo(purchaseAmount, fuelType, stationId)) {
            return ValidationResult.failure("Coupon is not applicable to this purchase")
        }

        return ValidationResult.success("Coupon can be used")
    }

    /**
     * Use the coupon for a purchase
     */
    fun use(
        purchaseAmount: BigDecimal,
        fuelType: String? = null,
        stationId: String? = null,
        usedBy: String? = null
    ): Coupon {
        val validationResult = canBeUsedFor(purchaseAmount, fuelType, stationId)
        if (!validationResult.isSuccess) {
            throw IllegalStateException(validationResult.message)
        }

        val updatedUsageRules = usageRules.incrementUsage()
        val newStatus = if (updatedUsageRules.isMaxUsesReached()) {
            CouponStatus.USED_UP
        } else {
            status
        }

        val updatedCoupon = this.copy(
            usageRules = updatedUsageRules,
            status = newStatus,
            updatedAt = LocalDateTime.now()
        )

        updatedCoupon.addDomainEvent(
            CouponUsedEvent(
                couponId = id,
                campaignId = campaignId,
                purchaseAmount = purchaseAmount,
                discountApplied = discountValue.calculateDiscount(purchaseAmount),
                fuelType = fuelType,
                stationId = stationId,
                usedBy = usedBy,
                remainingUses = updatedUsageRules.getRemainingUses(),
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedCoupon
    }

    /**
     * Calculate discount for a given purchase amount
     */
    fun calculateDiscount(purchaseAmount: BigDecimal): BigDecimal {
        return discountValue.calculateDiscount(purchaseAmount)
    }

    /**
     * Change coupon status
     */
    fun changeStatus(newStatus: CouponStatus, reason: String? = null): Coupon {
        if (status == newStatus) {
            return this
        }

        val updatedCoupon = this.copy(
            status = newStatus,
            updatedAt = LocalDateTime.now()
        )

        updatedCoupon.addDomainEvent(
            CouponStatusChangedEvent(
                couponId = id,
                oldStatus = status,
                newStatus = newStatus,
                reason = reason,
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedCoupon
    }

    /**
     * Activate the coupon
     */
    fun activate(): Coupon = changeStatus(CouponStatus.ACTIVE, "Coupon activated")

    /**
     * Deactivate the coupon
     */
    fun deactivate(): Coupon = changeStatus(CouponStatus.INACTIVE, "Coupon deactivated")

    /**
     * Cancel the coupon
     */
    fun cancel(reason: String? = null): Coupon = changeStatus(CouponStatus.CANCELLED, reason)

    /**
     * Expire the coupon
     */
    fun expire(): Coupon = changeStatus(CouponStatus.EXPIRED, "Coupon expired")

    /**
     * Check if coupon is expired
     */
    fun isExpired(): Boolean {
        return validityPeriod.isExpired() || status == CouponStatus.EXPIRED
    }

    /**
     * Check if coupon is not yet valid
     */
    fun isNotYetValid(): Boolean {
        return validityPeriod.isNotYetValid()
    }

    /**
     * Get remaining uses
     */
    fun getRemainingUses(): Int? {
        return usageRules.getRemainingUses()
    }

    /**
     * Get usage percentage
     */
    fun getUsagePercentage(): Double {
        return usageRules.getUsagePercentage()
    }

    /**
     * Check if coupon provides raffle tickets
     */
    fun providesRaffleTickets(): Boolean {
        return raffleTickets > 0
    }

    /**
     * Update metadata
     */
    fun updateMetadata(newMetadata: Map<String, String>): Coupon {
        return this.copy(
            metadata = metadata + newMetadata,
            updatedAt = LocalDateTime.now()
        )
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "Coupon(id=$id, code=${couponCode.value}, status=$status, campaign=$campaignId)"
    }
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