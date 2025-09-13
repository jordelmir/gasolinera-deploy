package com.gasolinerajsm.messaging.events

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event published when a coupon is validated
 */
data class CouponValidatedEvent(
    val couponId: Long,
    val couponCode: String,
    override val userId: Long,
    val stationId: Long,
    val campaignId: Long,
    val validationResult: String, // "VALID", "INVALID", "EXPIRED", "USED"
    val validationReason: String?,
    val discountType: String?,
    val discountValue: BigDecimal?,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val validatedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "coupon-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "COUPON_VALIDATED"
    override fun getRoutingKey(): String = "coupon.validated"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a coupon is redeemed
 */
data class CouponRedeemedEvent(
    val couponId: Long,
    val couponCode: String,
    override val userId: Long,
    val stationId: Long,
    val campaignId: Long,
    val redemptionId: Long,
    val discountType: String,
    val discountValue: BigDecimal,
    val originalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val finalAmount: BigDecimal,
    val currency: String = "USD",

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val redeemedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "coupon-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "COUPON_REDEEMED"
    override fun getRoutingKey(): String = "coupon.redeemed"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a coupon is created
 */
data class CouponCreatedEvent(
    val couponId: Long,
    val couponCode: String,
    val campaignId: Long,
    val discountType: String,
    val discountValue: BigDecimal,
    val minimumPurchase: BigDecimal?,
    val maximumDiscount: BigDecimal?,
    val usageLimit: Int?,
    val createdBy: Long,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val validFrom: LocalDateTime,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val validUntil: LocalDateTime,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val createdAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "coupon-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val userId: Long? = createdBy,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "COUPON_CREATED"
    override fun getRoutingKey(): String = "coupon.created"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a coupon expires
 */
data class CouponExpiredEvent(
    val couponId: Long,
    val couponCode: String,
    val campaignId: Long,
    val usageCount: Int,
    val totalValue: BigDecimal,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val expiredAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "coupon-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val userId: Long? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "COUPON_EXPIRED"
    override fun getRoutingKey(): String = "coupon.expired"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}