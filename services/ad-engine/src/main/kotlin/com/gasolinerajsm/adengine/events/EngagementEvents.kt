package com.gasolinerajsm.adengine.events

import com.gasolinerajsm.adengine.domain.model.EngagementType
import com.gasolinerajsm.adengine.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event fired when an ad engagement starts
 */
data class EngagementStartedEvent(
    override val aggregateId: String,
    val engagementId: EngagementId,
    val userId: UserId,
    val advertisementId: AdvertisementId,
    val engagementType: EngagementType,
    val sessionId: SessionId?,
    val locationData: LocationData?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventType: String = "EngagementStarted"
}

/**
 * Event fired when an ad engagement is completed
 */
data class EngagementCompletedEvent(
    override val aggregateId: String,
    val engagementId: EngagementId,
    val userId: UserId,
    val advertisementId: AdvertisementId,
    val engagementType: EngagementType,
    val completionPercentage: BigDecimal?,
    val viewDurationSeconds: Int?,
    val totalInteractions: Int,
    val qualifiesForRewards: Boolean,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventType: String = "EngagementCompleted"
}

/**
 * Event fired when tickets are awarded for engagement
 */
data class TicketsAwardedEvent(
    override val aggregateId: String,
    val engagementId: EngagementId,
    val userId: UserId,
    val advertisementId: AdvertisementId,
    val baseTickets: Int,
    val bonusTickets: Int,
    val totalTickets: Int,
    val raffleEntryId: RaffleEntryId?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : DomainEvent {
    override val eventType: String = "TicketsAwarded"
}