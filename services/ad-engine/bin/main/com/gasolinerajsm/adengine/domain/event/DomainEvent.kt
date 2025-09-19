package com.gasolinerajsm.adengine.domain.event

import java.time.LocalDateTime

/**
 * Base interface for domain events
 */
interface DomainEvent {
    val occurredAt: LocalDateTime
}