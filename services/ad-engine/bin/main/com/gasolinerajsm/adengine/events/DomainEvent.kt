package com.gasolinerajsm.adengine.events

import java.time.LocalDateTime

/**
 * Base interface for all domain events in the ad engine
 */
interface DomainEvent {
    val aggregateId: String
    val eventType: String
    val occurredAt: LocalDateTime
    val eventVersion: Int
        get() = 1
}