package com.gasolinerajsm.messaging.events

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event published when a redemption is created
 */
data class RedemptionCreatedEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    override val userId: Long,
    val couponId: Long,
    val stationId: Long,
    val totalAmount: BigDecimal,
    val redemptionType: String,
    val currency: String = "USD",
    val raffleTicketCount: Int = 0,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val redeemedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "redemption-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "REDEMPTION_CREATED"
    override fun getRoutingKey(): String = "redemption.created"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a redemption is completed
 */
data class RedemptionCompletedEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    override val userId: Long,
    val stationId: Long,
    val employeeId: Long?,
    val totalAmount: BigDecimal,
    val currency: String = "USD",

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val completedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "redemption-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "REDEMPTION_COMPLETED"
    override fun getRoutingKey(): String = "redemption.completed"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a redemption is voided
 */
data class RedemptionVoidedEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    override val userId: Long,
    val stationId: Long,
    val voidedBy: Long,
    val voidReason: String,
    val totalAmount: BigDecimal,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val voidedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "redemption-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "REDEMPTION_VOIDED"
    override fun getRoutingKey(): String = "redemption.voided"
    override fun getAuditLevel(): AuditLevel = AuditLevel.WARN

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a redemption expires
 */
data class RedemptionExpiredEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    override val userId: Long,
    val stationId: Long,
    val totalAmount: BigDecimal,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val expiredAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "redemption-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "REDEMPTION_EXPIRED"
    override fun getRoutingKey(): String = "redemption.expired"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}