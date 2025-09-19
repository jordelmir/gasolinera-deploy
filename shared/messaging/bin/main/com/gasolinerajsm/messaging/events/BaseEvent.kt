package com.gasolinerajsm.messaging.events

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime
import java.util.*

/**
 * Base class for all domain events in the Gasolinera JSM platform
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RedemptionCreatedEvent::class, name = "REDEMPTION_CREATED"),
    JsonSubTypes.Type(value = RedemptionCompletedEvent::class, name = "REDEMPTION_COMPLETED"),
    JsonSubTypes.Type(value = RedemptionVoidedEvent::class, name = "REDEMPTION_VOIDED"),
    JsonSubTypes.Type(value = RedemptionExpiredEvent::class, name = "REDEMPTION_EXPIRED"),
    JsonSubTypes.Type(value = RaffleTicketsGeneratedEvent::class, name = "RAFFLE_TICKETS_GENERATED"),
    JsonSubTypes.Type(value = RaffleEntryCreatedEvent::class, name = "RAFFLE_ENTRY_CREATED"),
    JsonSubTypes.Type(value = RaffleWinnerSelectedEvent::class, name = "RAFFLE_WINNER_SELECTED"),
    JsonSubTypes.Type(value = AdEngagementCompletedEvent::class, name = "AD_ENGAGEMENT_COMPLETED"),
    JsonSubTypes.Type(value = AdTicketsMultipliedEvent::class, name = "AD_TICKETS_MULTIPLIED"),
    JsonSubTypes.Type(value = CouponValidatedEvent::class, name = "COUPON_VALIDATED"),
    JsonSubTypes.Type(value = CouponRedeemedEvent::class, name = "COUPON_REDEEMED"),
    JsonSubTypes.Type(value = AuditEvent::class, name = "AUDIT_EVENT")
)
abstract class BaseEvent(
    open val eventId: String = UUID.randomUUID().toString(),

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    open val timestamp: LocalDateTime = LocalDateTime.now(),

    open val version: String = "1.0",

    open val source: String,

    open val correlationId: String? = null,

    open val causationId: String? = null,

    open val userId: Long? = null,

    open val sessionId: String? = null,

    open val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Get the event type name for routing
     */
    abstract fun getEventType(): String

    /**
     * Get the routing key for this event
     */
    abstract fun getRoutingKey(): String

    /**
     * Check if this event should be audited
     */
    open fun shouldAudit(): Boolean = true

    /**
     * Get audit level for this event
     */
    open fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    /**
     * Create a copy of this event with new correlation ID
     */
    abstract fun withCorrelationId(correlationId: String): BaseEvent

    /**
     * Create a copy of this event with new causation ID
     */
    abstract fun withCausationId(causationId: String): BaseEvent
}

/**
 * Audit levels for events
 */
enum class AuditLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    CRITICAL
}