package com.gasolinerajsm.authservice.domain.event

import java.time.LocalDateTime
import java.util.*

/**
 * Base interface for all domain events
 */
interface DomainEvent {
    val eventId: String
    val eventType: String
    val occurredAt: LocalDateTime
    val version: Int
}

/**
 * Abstract base class for domain events
 */
abstract class BaseDomainEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val eventType: String,
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val version: Int = 1
) : DomainEvent