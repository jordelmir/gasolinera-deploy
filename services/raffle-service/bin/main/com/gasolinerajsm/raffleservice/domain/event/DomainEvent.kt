package com.gasolinerajsm.raffleservice.domain.event

import java.time.LocalDateTime
import java.util.*

/**
 * Base interface for domain events
 */
interface DomainEvent {
    val eventId: UUID
    val eventType: String
    val aggregateId: String
    val occurredAt: LocalDateTime
    val eventVersion: Int
}

/**
 * Raffle created event
 */
data class RaffleCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val eventType: String = "RaffleCreated",
    override val aggregateId: String,
    val raffleId: String,
    val name: String,
    val raffleType: String,
    val schedule: Any, // RaffleSchedule
    val prizePoolValue: java.math.BigDecimal,
    val maxParticipants: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventVersion: Int = 1
) : DomainEvent

/**
 * Raffle activated event
 */
data class RaffleActivatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val eventType: String = "RaffleActivated",
    override val aggregateId: String,
    val raffleId: String,
    val name: String,
    val registrationStart: LocalDateTime,
    val registrationEnd: LocalDateTime,
    val drawDate: LocalDateTime,
    val activatedBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventVersion: Int = 1
) : DomainEvent

/**
 * Raffle draw completed event
 */
data class RaffleDrawCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val eventType: String = "RaffleDrawCompleted",
    override val aggregateId: String,
    val raffleId: String,
    val name: String,
    val drawResults: Any, // DrawResults
    val totalParticipants: Int,
    val totalTicketsUsed: Int,
    val completedBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventVersion: Int = 1
) : DomainEvent