package com.gasolinerajsm.couponservice.domain.event

import com.gasolinerajsm.couponservice.domain.model.CampaignStatus
import com.gasolinerajsm.couponservice.domain.valueobject.CampaignId
import com.gasolinerajsm.couponservice.domain.valueobject.DiscountValue
import com.gasolinerajsm.couponservice.domain.valueobject.ValidityPeriod
import java.time.LocalDateTime

/**
 * Event fired when a new campaign is created
 */
data class CampaignCreatedEvent(
    val campaignId: CampaignId,
    val name: String,
    val validityPeriod: ValidityPeriod,
    val discountValue: DiscountValue,
    val targetCouponCount: Int?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CampaignCreated",
    occurredAt = occurredAt
)

/**
 * Event fired when campaign status changes
 */
data class CampaignStatusChangedEvent(
    val campaignId: CampaignId,
    val oldStatus: CampaignStatus,
    val newStatus: CampaignStatus,
    val reason: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CampaignStatusChanged",
    occurredAt = occurredAt
)

/**
 * Event fired when a campaign is activated
 */
data class CampaignActivatedEvent(
    val campaignId: CampaignId,
    val name: String,
    val validityPeriod: ValidityPeriod,
    val targetCouponCount: Int?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CampaignActivated",
    occurredAt = occurredAt
)

/**
 * Event fired when a campaign is completed
 */
data class CampaignCompletedEvent(
    val campaignId: CampaignId,
    val name: String,
    val totalCouponsGenerated: Int,
    val totalCouponsUsed: Int,
    val usageRate: Double,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CampaignCompleted",
    occurredAt = occurredAt
)

/**
 * Event fired when campaign reaches target coupon count
 */
data class CampaignTargetReachedEvent(
    val campaignId: CampaignId,
    val name: String,
    val targetCount: Int,
    val actualCount: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CampaignTargetReached",
    occurredAt = occurredAt
)

/**
 * Event fired when campaign expires
 */
data class CampaignExpiredEvent(
    val campaignId: CampaignId,
    val name: String,
    val totalCouponsGenerated: Int,
    val totalCouponsUsed: Int,
    val unusedCoupons: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "CampaignExpired",
    occurredAt = occurredAt
)