package com.gasolinerajsm.coupon.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime
import com.gasolinerajsm.coupon.entity.CouponStatus

/**
 * Coupon entity representing a digital coupon with QR code and validation
 */
@Entity
@Table(
    name = "coupons",
    schema = "coupon_schema",
    indexes = [
        Index(name = "idx_coupons_qr_code", columnList = "qr_code", unique = true),
        Index(name = "idx_coupons_campaign_id", columnList = "campaign_id"),
        Index(name = "idx_coupons_status", columnList = "status"),
        Index(name = "idx_coupons_valid_from", columnList = "valid_from"),
        Index(name = "idx_coupons_valid_until", columnList = "valid_until"),
        Index(name = "idx_coupons_created_at", columnList = "created_at")
    ]
)
data class Coupon(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    @field:NotNull(message = "Campaign is required")
    val campaign: Campaign,

    @Column(name = "qr_code", unique = true, nullable = false, length = 500)
    @field:NotBlank(message = "QR code is required")
    @field:Size(max = 500, message = "QR code must not exceed 500 characters")
    val qrCode: String,

    @Column(name = "qr_signature", nullable = false, length = 1000)
    @field:NotBlank(message = "QR signature is required")
    @field:Size(max = 1000, message = "QR signature must not exceed 1000 characters")
    val qrSignature: String,

    @Column(name = "coupon_code", unique = true, nullable = false, length = 50)
    @field:NotBlank(message = "Coupon code is required")
    @field:Size(max = 50, message = "Coupon code must not exceed 50 characters")
    val couponCode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: CouponStatus = CouponStatus.ACTIVE,

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Discount amount must be positive")
    val discountAmount: BigDecimal? = null,

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Discount percentage must be positive")
    @field:DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100%")
    val discountPercentage: BigDecimal? = null,

    @Column(name = "raffle_tickets", nullable = false)
    @field:Min(value = 1, message = "Raffle tickets must be at least 1")
    val raffleTickets: Int = 1,

    @Column(name = "valid_from", nullable = false)
    @field:NotNull(message = "Valid from date is required")
    val validFrom: LocalDateTime,

    @Column(name = "valid_until", nullable = false)
    @field:NotNull(message = "Valid until date is required")
    val validUntil: LocalDateTime,

    @Column(name = "max_uses")
    @field:Min(value = 1, message = "Max uses must be at least 1")
    val maxUses: Int? = null,

    @Column(name = "current_uses", nullable = false)
    val currentUses: Int = 0,

    @Column(name = "minimum_purchase_amount", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Minimum purchase amount must be positive")
    val minimumPurchaseAmount: BigDecimal? = null,

    @Column(name = "applicable_fuel_types", length = 200)
    val applicableFuelTypes: String? = null,

    @Column(name = "applicable_stations", length = 1000)
    val applicableStations: String? = null,

    @Column(name = "terms_and_conditions", length = 2000)
    val termsAndConditions: String? = null,

    @Column(name = "metadata", length = 1000)
    val metadata: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * Check if the coupon is currently valid
     */
    fun isValid(): Boolean {
        val now = LocalDateTime.now()
        return status == CouponStatus.ACTIVE &&
                now.isAfter(validFrom) &&
                now.isBefore(validUntil) &&
                !isMaxUsesReached()
    }

    /**
     * Check if the coupon has expired
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(validUntil)
    }

    /**
     * Check if the coupon is not yet valid
     */
    fun isNotYetValid(): Boolean {
        return LocalDateTime.now().isBefore(validFrom)
    }

    /**
     * Check if maximum uses have been reached
     */
    fun isMaxUsesReached(): Boolean {
        return maxUses != null && currentUses >= maxUses
    }

    /**
     * Check if the coupon can be used (is valid and not used up)
     */
    fun canBeUsed(): Boolean {
        return isValid() && !isMaxUsesReached()
    }

    /**
     * Get remaining uses
     */
    fun getRemainingUses(): Int? {
        return maxUses?.let { it - currentUses }
    }

    /**
     * Check if coupon applies to specific fuel type
     */
    fun appliesTo(fuelType: String): Boolean {
        return applicableFuelTypes?.split(",")?.map { it.trim() }?.contains(fuelType) ?: true
    }

    /**
     * Check if coupon applies to specific station
     */
    fun appliesTo(stationId: Long): Boolean {
        return applicableStations?.split(",")?.map { it.trim().toLongOrNull() }?.contains(stationId) ?: true
    }

    /**
     * Check if purchase amount meets minimum requirement
     */
    fun meetsMinimumPurchase(purchaseAmount: BigDecimal): Boolean {
        return minimumPurchaseAmount?.let { purchaseAmount >= it } ?: true
    }

    /**
     * Calculate discount amount for given purchase
     */
    fun calculateDiscount(purchaseAmount: BigDecimal): BigDecimal {
        return when {
            discountAmount != null -> discountAmount
            discountPercentage != null -> purchaseAmount * (discountPercentage / BigDecimal("100"))
            else -> BigDecimal.ZERO
        }
    }

    /**
     * Get discount type
     */
    fun getDiscountType(): DiscountType {
        return when {
            discountAmount != null -> DiscountType.FIXED_AMOUNT
            discountPercentage != null -> DiscountType.PERCENTAGE
            else -> DiscountType.NONE
        }
    }

    /**
     * Get applicable fuel types as list
     */
    fun getApplicableFuelTypesList(): List<String> {
        return applicableFuelTypes?.split(",")?.map { it.trim() } ?: emptyList()
    }

    /**
     * Get applicable stations as list
     */
    fun getApplicableStationsList(): List<Long> {
        return applicableStations?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    }

    /**
     * Use the coupon (increment usage count)
     */
    fun use(): Coupon {
        return this.copy(currentUses = currentUses + 1)
    }

    /**
     * Activate the coupon
     */
    fun activate(): Coupon {
        return this.copy(status = CouponStatus.ACTIVE)
    }

    /**
     * Deactivate the coupon
     */
    fun deactivate(): Coupon {
        return this.copy(status = CouponStatus.INACTIVE)
    }

    /**
     * Expire the coupon
     */
    fun expire(): Coupon {
        return this.copy(status = CouponStatus.EXPIRED)
    }

    override fun toString(): String {
        return "Coupon(id=$id, couponCode='$couponCode', status=$status, campaign=${campaign.name}, validFrom=$validFrom, validUntil=$validUntil)"
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