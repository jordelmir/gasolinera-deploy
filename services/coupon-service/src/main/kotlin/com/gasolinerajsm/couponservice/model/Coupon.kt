package com.gasolinerajsm.couponservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime
import com.gasolinerajsm.couponservice.model.CampaignStatus

/**
 * Coupon entity representing a discount coupon in the system
 */
@Entity
@Table(name = "coupons")
data class Coupon(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "code", unique = true, nullable = false)
    @field:NotBlank(message = "Coupon code is required")
    val code: String,

    @Column(name = "coupon_code", unique = true, nullable = false)
    @field:NotBlank(message = "Coupon code is required")
    val couponCode: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    val campaign: Campaign,

    @Column(name = "name", nullable = false)
    @field:NotBlank(message = "Coupon name is required")
    val name: String,

    @Column(name = "description")
    val description: String? = null,

    @Column(name = "discount_value", nullable = false)
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    val discountValue: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    val discountType: DiscountType = DiscountType.PERCENTAGE,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: CouponStatus = CouponStatus.ACTIVE,

    @Column(name = "valid_from", nullable = false)
    val validFrom: LocalDateTime,

    @Column(name = "valid_until", nullable = false)
    val validUntil: LocalDateTime,

    @Column(name = "max_uses", nullable = false)
    @field:Min(value = 1, message = "Max uses must be at least 1")
    val maxUses: Int = 1,

    @Column(name = "current_uses", nullable = false)
    val currentUses: Int = 0,

    @Column(name = "qr_code", unique = true)
    val qrCode: String? = null,

    @Column(name = "qr_signature")
    val qrSignature: String? = null,

    @Column(name = "discount_amount", nullable = false)
    val discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_percentage")
    val discountPercentage: BigDecimal? = null,

    @Column(name = "raffle_tickets", nullable = false)
    val raffleTickets: Int = 1,

    @Column(name = "terms_and_conditions")
    val termsAndConditions: String? = null,

    @Column(name = "minimum_purchase_amount")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Minimum purchase amount must be non-negative")
    val minimumPurchaseAmount: BigDecimal? = null,

    @Column(name = "applicable_fuel_types")
    val applicableFuelTypes: String? = "ALL",

    @Column(name = "applicable_stations")
    val applicableStations: String? = "ALL",

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    fun isValid(): Boolean {
        val now = LocalDateTime.now()
        return status == CouponStatus.ACTIVE &&
               now.isAfter(validFrom) &&
               now.isBefore(validUntil) &&
               currentUses < maxUses
    }

    fun canBeUsed(): Boolean {
        return isValid() && status == CouponStatus.ACTIVE
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(validUntil)
    }

    fun isUsedUp(): Boolean {
        return currentUses >= maxUses
    }

    fun getRemainingUses(): Int {
        return (maxUses - currentUses).coerceAtLeast(0)
    }

    fun isNotYetValid(): Boolean {
        return LocalDateTime.now().isBefore(validFrom)
    }

    fun isMaxUsesReached(): Boolean {
        return currentUses >= maxUses
    }

    fun calculateDiscountType(): DiscountType {
        return when {
            discountPercentage != null -> DiscountType.PERCENTAGE
            discountAmount > BigDecimal.ZERO -> DiscountType.FIXED_AMOUNT
            else -> DiscountType.NONE
        }
    }

    fun calculateDiscount(purchaseAmount: BigDecimal): BigDecimal {
        return when (calculateDiscountType()) {
            DiscountType.PERCENTAGE -> {
                discountPercentage?.let { purchaseAmount * (it / BigDecimal("100")) } ?: BigDecimal.ZERO
            }
            DiscountType.FIXED_AMOUNT -> discountAmount
            DiscountType.NONE -> BigDecimal.ZERO
        }
    }

    fun meetsMinimumPurchase(purchaseAmount: BigDecimal): Boolean {
        return minimumPurchaseAmount?.let { purchaseAmount >= it } ?: true
    }

    fun appliesTo(fuelType: String): Boolean {
        return applicableFuelTypes == "ALL" || applicableFuelTypes?.contains(fuelType) == true
    }

    fun appliesTo(stationId: Long): Boolean {
        return applicableStations == "ALL" || applicableStations?.contains(stationId.toString()) == true
    }

    fun getApplicableFuelTypesList(): List<String> {
        return applicableFuelTypes?.split(",")?.map { it.trim() } ?: emptyList()
    }

    fun getApplicableStationsList(): List<Long> {
        return applicableStations?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    }

    fun use(): Coupon {
        return this.copy(currentUses = currentUses + 1)
    }

    fun activate(): Coupon {
        return this.copy(status = CouponStatus.ACTIVE)
    }

    fun deactivate(): Coupon {
        return this.copy(status = CouponStatus.INACTIVE)
    }

    fun expire(): Coupon {
        return this.copy(status = CouponStatus.EXPIRED)
    }

    fun allowsUsage(): Boolean {
        return status.canBeUsed()
    }

    override fun toString(): String {
        return "Coupon(id=$id, couponCode='$couponCode', status=$status, campaign=${campaign.name})"
    }
}


/**
 * Coupon status enumeration
 */
enum class CouponStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Coupon is active and can be used"),
    INACTIVE("Inactive", "Coupon is temporarily inactive"),
    EXPIRED("Expired", "Coupon has expired"),
    USED_UP("Used Up", "Coupon has reached maximum uses"),
    CANCELLED("Cancelled", "Coupon has been cancelled");

    fun canBeUsed(): Boolean {
        return this == ACTIVE
    }

    fun isFinalState(): Boolean {
        return this == EXPIRED || this == USED_UP || this == CANCELLED
    }
}