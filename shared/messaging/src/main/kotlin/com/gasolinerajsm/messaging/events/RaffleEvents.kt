package com.gasolinerajsm.messaging.events

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event published when raffle tickets are generated
 */
data class RaffleTicketsGeneratedEvent(
    val redemptionId: Long?,
    val adEngagementId: Long?,
    override val userId: Long,
    val ticketCount: Int,
    val ticketIds: List<Long>,
    val multiplierValue: Int = 1,
    val sourceType: String, // "REDEMPTION" or "AD_ENGAGEMENT"
    val campaignId: Long? = null,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val generatedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "redemption-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "RAFFLE_TICKETS_GENERATED"
    override fun getRoutingKey(): String = "raffle.tickets.generated"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a raffle entry is created
 */
data class RaffleEntryCreatedEvent(
    val entryId: Long,
    val raffleId: Long,
    override val userId: Long,
    val ticketCount: Int,
    val ticketIds: List<Long>,
    val entryNumber: String,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val enteredAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "raffle-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "RAFFLE_ENTRY_CREATED"
    override fun getRoutingKey(): String = "raffle.entry.created"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a raffle winner is selected
 */
data class RaffleWinnerSelectedEvent(
    val winnerId: Long,
    val raffleId: Long,
    val prizeId: Long,
    override val userId: Long,
    val winningTicketId: Long,
    val winningTicketNumber: String,
    val prizeDescription: String,
    val prizeValue: BigDecimal?,
    val drawNumber: Int,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val selectedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "raffle-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "RAFFLE_WINNER_SELECTED"
    override fun getRoutingKey(): String = "raffle.winner.selected"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when a raffle draw is executed
 */
data class RaffleDrawExecutedEvent(
    val raffleId: Long,
    val drawNumber: Int,
    val totalEntries: Int,
    val totalTickets: Int,
    val winnersCount: Int,
    val winnerIds: List<Long>,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val executedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "raffle-service",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val userId: Long? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "RAFFLE_DRAW_EXECUTED"
    override fun getRoutingKey(): String = "raffle.draw.executed"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}