package com.gasolinerajsm.redemptionservice.domain.event

import com.gasolinerajsm.redemptionservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

/**
 * Event fired when a new redemption is created
 */
data class RedemptionCreatedEvent(
    val redemptionId: RedemptionId,
    val userId: UserId,
    val stationId: StationId,
    val couponId: CouponId,
    val campaignId: CampaignId,
    val purchaseAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val raffleTicketsEarned: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RedemptionCreated",
    occurredAt = occurredAt
)

/**
 * Event fired when redemption processing starts
 */
data class RedemptionProcessingStartedEvent(
    val redemptionId: RedemptionId,
    val userId: UserId,
    val stationId: StationId,
    val employeeId: EmployeeId,
    val transactionReference: TransactionReference,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RedemptionProcessingStarted",
    occurredAt = occurredAt
)

/**
 * Event fired when a redemption is completed successfully
 */
data class RedemptionCompletedEvent(
    val redemptionId: RedemptionId,
    val userId: UserId,
    val stationId: StationId,
    val couponId: CouponId,
    val campaignId: CampaignId,
    val finalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val raffleTicketsEarned: Int,
    val processingDuration: Duration?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RedemptionCompleted",
    occurredAt = occurredAt
)

/**
 * Event fired when a redemption fails
 */
data class RedemptionFailedEvent(
    val redemptionId: RedemptionId,
    val userId: UserId,
    val stationId: StationId,
    val couponId: CouponId,
    val failureReason: String,
    val attemptedAmount: BigDecimal,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RedemptionFailed",
    occurredAt = occurredAt
)

/**
 * Event fired when a redemption is cancelled
 */
data class RedemptionCancelledEvent(
    val redemptionId: RedemptionId,
    val userId: UserId,
    val stationId: StationId,
    val couponId: CouponId,
    val cancellationReason: String?,
    val cancelledBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RedemptionCancelled",
    occurredAt = occurredAt
)

/**
 * Event fired when redemption validation occurs
 */
data class RedemptionValidatedEvent(
    val redemptionId: RedemptionId,
    val couponId: CouponId,
    val isValid: Boolean,
    val validationReason: String,
    val validatedBy: EmployeeId,
    val stationId: StationId,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RedemptionValidated",
    occurredAt = occurredAt
)