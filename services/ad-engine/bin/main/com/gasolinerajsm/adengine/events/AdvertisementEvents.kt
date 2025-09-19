package com.gasolinerajsm.adengine.events

import java.time.LocalDateTime

/**
 * Event fired when an advertisement is created
 */
data class AdvertisementCreatedEvent(
    override val aggregateId: String,
    val advertisementId: String,
    val campaignId: String,
    val name: String,
    val type: String,
    val budget: Double,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventType: String = "AdvertisementCreated"
}

/**
 * Event fired when an advertisement is activated
 */
data class AdvertisementActivatedEvent(
    override val aggregateId: String,
    val advertisementId: String,
    val campaignId: String,
    val activatedBy: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventType: String = "AdvertisementActivated"
}

/**
 * Event fired when an advertisement is completed
 */
data class AdvertisementCompletedEvent(
    override val aggregateId: String,
    val advertisementId: String,
    val campaignId: String,
    val totalImpressions: Long,
    val totalClicks: Long,
    val totalEngagements: Long,
    val completionReason: String,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventType: String = "AdvertisementCompleted"
}