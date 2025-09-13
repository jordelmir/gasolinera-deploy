package com.gasolinerajsm.couponservice.domain.event

import com.gasolinerajsm.couponservice.domain.model.CouponStatus
import com.gasolinerajsm.couponservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event fired when a new coupon is created
 */
data class CouponCreatedEvent(
    val couponId: CouponId,
    val campaignId: CampaignId,
    val couponCode: CouponCode,
    val discountValue: DiscountValue,
    val validityPeriod: ValidityPeriod,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CouponCreated",
    occurredAt = occurredAt
)

/**
 * Event fired when a coupon is used
 */
data class CouponUsedEvent(
    val couponId: CouponId,
    val campaignId: CampaignId,
    val purchaseAmount: BigDecimal,
    val discountApplied: BigDecimal,
    val fuelType: String?,
    val stationId: String?,
    val usedBy: String?,
    val remainingUses: Int?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CouponUsed",
    occurredAt = occurredAt
)

/**
 * Event fired when coupon status changes
 */
data class CouponStatusChangedEvent(
    val couponId: CouponId,
    val oldStatus: CouponStatus,
    val newStatus: CouponStatus,
    val reason: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CouponStatusChanged",
    occurredAt = occurredAt
)

/**
 * Event fired when a coupon expires
 */
data class CouponExpiredEvent(
    val couponId: CouponId,
    val campaignId: CampaignId,
    val couponCode: CouponCode,
    val wasUsed: Boolean,
    val totalUses: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CouponExpired",
    occurredAt = occurredAt
)

/**
 * Event fired when a coupon is validated (successful or failed)
 */
data class CouponValidatedEvent(
    val couponId: CouponId,
    val couponCode: CouponCode,
    val isValid: Boolean,
    val validationReason: String,
    val purchaseAmount: BigDecimal?,
    val stationId: String?,
    val validatedBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CouponValidated",
    occurredAt = occurredAt
)

/**
 * Event fired when a QR code is scanned
 */
data class QRCodeScannedEvent(
    val couponId: CouponId,
    val qrCodeData: String,
    val scannedBy: String?,
    val stationId: String?,
    val scanResult: String, // SUCCESS, INVALID, EXPIRED, etc.
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "QRCodeScanned",
    occurredAt = occurredAt
)

/**
 * Event fired when coupon usage fails
 */
data class CouponUsageFailedEvent(
    val couponId: CouponId,
    val couponCode: CouponCode,
    val failureReason: String,
    val purchaseAmount: BigDecimal?,
    val stationId: String?,
    val attemptedBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CouponUsageFailed",
    occurredAt = occurredAt
)