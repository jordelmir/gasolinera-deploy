package com.gasolinerajsm.messaging.events

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * Event published when an ad engagement is completed
 */
data class AdEngagementCompletedEvent(
    val engagementId: Long,
    val advertisementId: Long,
    override val userId: Long,
    val engagementType: String, // "VIEW", "CLICK", "COMPLETE"
    val duration: Long?, // Duration in seconds for video ads
    val completionPercentage: Int, // 0-100
    val campaignId: Long?,
    val stationId: Long?,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val completedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "ad-engine",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "AD_ENGAGEMENT_COMPLETED"
    override fun getRoutingKey(): String = "ad.engagement.completed"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when raffle tickets are multiplied due to ad engagement
 */
data class AdTicketsMultipliedEvent(
    val engagementId: Long,
    val advertisementId: Long,
    override val userId: Long,
    val originalTicketCount: Int,
    val multiplier: Int,
    val bonusTicketCount: Int,
    val totalTicketCount: Int,
    val ticketIds: List<Long>,
    val campaignId: Long?,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val multipliedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "ad-engine",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "AD_TICKETS_MULTIPLIED"
    override fun getRoutingKey(): String = "ad.tickets.multiplied"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when an ad engagement is started
 */
data class AdEngagementStartedEvent(
    val engagementId: Long,
    val advertisementId: Long,
    override val userId: Long,
    val engagementType: String,
    val campaignId: Long?,
    val stationId: Long?,
    val deviceInfo: String?,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val startedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "ad-engine",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "AD_ENGAGEMENT_STARTED"
    override fun getRoutingKey(): String = "ad.engagement.started"
    override fun getAuditLevel(): AuditLevel = AuditLevel.DEBUG

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

/**
 * Event published when an ad campaign is updated
 */
data class AdCampaignUpdatedEvent(
    val campaignId: Long,
    val advertisementId: Long,
    val updateType: String, // "ACTIVATED", "DEACTIVATED", "MODIFIED", "BUDGET_UPDATED"
    val previousStatus: String?,
    val newStatus: String?,
    val updatedBy: Long,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val updatedAt: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String = "ad-engine",
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val userId: Long? = updatedBy,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "AD_CAMPAIGN_UPDATED"
    override fun getRoutingKey(): String = "ad.campaign.updated"
    override fun getAuditLevel(): AuditLevel = AuditLevel.INFO

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}