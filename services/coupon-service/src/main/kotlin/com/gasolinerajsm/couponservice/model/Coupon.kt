package com.gasolinerajsm.couponservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Coupon entity representing a digital coupon
 */
@Entity
@Table(
    name = "coupons",
    schema = "coupon_schema",
    indexes = [
        Index(name = "idx_coupons_campaign_id", columnList = "campaign_id"),
        Index(name = "idx_coupons_status", columnList = "status"),
        Index(name = "idx_coupons_qr_code", columnList = "qr_code"),
        Index(name = "idx_coupons_valid_from", columnList = "valid_from"),
        Index(name = "idx_coupons_valid_until", columnList = "valid_until")
    ]
)
data class Coupon(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "campaign_id", nullable = false)
    @field:NotNull(message = "Campaign ID is required")
    val campaignId: Long,

    @Column(name = "qr_code", unique = true, nullable = false, length = 500)
    @field:NotBlank(message = "QR code is required")
    @field:Size(max = 500, message = "QR code must not exceed 500 characters")
    val qrCode: String,

    @Column(name = "qr_signature", nullable = false, length = 1000)
    @field:NotBlank(message = "QR signature is required")
    @field:Size(max = 1000, message = "QR signature must not exceed 1000 characters")
    val qrSignature: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: CouponStatus = CouponStatus.ACTIVE,

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @field:NotNull(message = "Discount amount is required")
    @field:DecimalMin(value = "0.0", message = "Discount amount must be positive")
    val discountAmount: BigDecimal,

    @Column(name = "raffle_tickets", nullable = false)
    @field:NotNull(message = "Raffle tickets is required")
    @field:Min(value = 0, message = "Raffle tickets must be non-negative")
    val raffleTickets: Int,

    @Column(name = "valid_from", nullable = false)
    @field:NotNull(message = "Valid from date is required")
    val validFrom: LocalDateTime,

    @Column(name = "valid_until", nullable = false)
    @field:NotNull(message = "Valid until date is required")
    val validUntil: LocalDateTime,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if the coupon is currently valid
     */
    fun isValid(): Boolean {
        val now = LocalDateTime.now()
        return status == CouponStatus.ACTIVE &&
               now.isAfter(validFrom) &&
               now.isBefore(validUntil)
    }

    /**
     * Check if the coupon is expired
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(validUntil)
    }

    /**
     * Check if the coupon is used
     */
    fun isUsed(): Boolean {
        return status == CouponStatus.USED
    }

    /**
     * Mark coupon as used
     */
    fun markAsUsed(): Coupon {
        return this.copy(status = CouponStatus.USED)
    }

    /**
     * Cancel the coupon
     */
    fun cancel(): Coupon {
        return this.copy(status = CouponStatus.CANCELLED)
    }
}

/**
 * Coupon status enumeration
 */
enum class CouponStatus {
    ACTIVE,
    USED,
    EXPIRED,
    CANCELLED
}