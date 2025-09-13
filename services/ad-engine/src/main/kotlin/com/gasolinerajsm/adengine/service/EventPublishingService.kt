package com.gasolinerajsm.adengine.service

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for publishing events to message queue for inter-service communication
 */
@Service
@Transactional
class EventPublishingService(
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(EventPublishingService::class.java)

    companion object {
        const val AD_COMPLETION_EXCHANGE = "ad.completion"
        const val AD_COMPLETION_ROUTING_KEY = "ad.completion.ticket.award"
        const val AD_ENGAGEMENT_EXCHANGE = "ad.engagement"
        const val AD_ENGAGEMENT_ROUTING_KEY = "ad.engagement.created"
    }

    /**
     * Handle ad completion events and publish to message queue
     */
    @EventListener
    fun handleAdCompletionEvent(event: AdCompletionEvent) {
        logger.info("Publishing ad completion event for engagement ${event.engagementId}")

        try {
            val message = AdCompletionMessage(
                userId = event.userId,
                engagementId = event.engagementId,
                advertisementId = event.advertisementId,
                campaignId = event.campaignId,
                baseTickets = event.baseTickets,
                bonusTickets = event.bonusTickets,
                totalTickets = event.totalTickets,
                raffleId = event.raffleId,
                completedAt = event.completedAt,
                stationId = event.stationId,
                eventType = "AD_COMPLETION_TICKET_AWARD"
            )

            rabbitTemplate.convertAndSend(
                AD_COMPLETION_EXCHANGE,
                AD_COMPLETION_ROUTING_KEY,
                message
            )

            logger.info("Successfully published ad completion event for engagement ${event.engagementId}")

        } catch (e: Exception) {
            logger.error("Failed to publish ad completion event for engagement ${event.engagementId}", e)
            // Consider implementing retry logic or dead letter queue handling
        }
    }

    /**
     * Publish ad engagement event
     */
    fun publishAdEngagementEvent(
        userId: Long,
        engagementId: Long,
        advertisementId: Long,
        engagementType: String,
        stationId: Long? = null
    ) {
        logger.debug("Publishing ad engagement event for engagement $engagementId")

        try {
            val message = AdEngagementMessage(
                userId = userId,
                engagementId = engagementId,
                advertisementId = advertisementId,
                engagementType = engagementType,
                stationId = stationId,
                timestamp = java.time.LocalDateTime.now(),
                eventType = "AD_ENGAGEMENT_CREATED"
            )

            rabbitTemplate.convertAndSend(
                AD_ENGAGEMENT_EXCHANGE,
                AD_ENGAGEMENT_ROUTING_KEY,
                message
            )

            logger.debug("Successfully published ad engagement event for engagement $engagementId")

        } catch (e: Exception) {
            logger.error("Failed to publish ad engagement event for engagement $engagementId", e)
        }
    }

    /**
     * Publish ticket multiplication event
     */
    fun publishTicketMultiplicationEvent(
        userId: Long,
        engagementId: Long,
        baseTickets: Int,
        bonusTickets: Int,
        totalTickets: Int,
        multiplier: java.math.BigDecimal
    ) {
        logger.info("Publishing ticket multiplication event for user $userId")

        try {
            val message = TicketMultiplicationMessage(
                userId = userId,
                engagementId = engagementId,
                baseTickets = baseTickets,
                bonusTickets = bonusTickets,
                totalTickets = totalTickets,
                multiplier = multiplier,
                timestamp = java.time.LocalDateTime.now(),
                eventType = "TICKET_MULTIPLICATION"
            )

            rabbitTemplate.convertAndSend(
                "ticket.multiplication",
                "ticket.multiplication.awarded",
                message
            )

            logger.info("Successfully published ticket multiplication event for user $userId")

        } catch (e: Exception) {
            logger.error("Failed to publish ticket multiplication event for user $userId", e)
        }
    }
}

/**
 * Message for ad completion events
 */
data class AdCompletionMessage(
    val userId: Long,
    val engagementId: Long,
    val advertisementId: Long,
    val campaignId: Long,
    val baseTickets: Int,
    val bonusTickets: Int,
    val totalTickets: Int,
    val raffleId: Long?,
    val completedAt: java.time.LocalDateTime,
    val stationId: Long?,
    val eventType: String,
    val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

/**
 * Message for ad engagement events
 */
data class AdEngagementMessage(
    val userId: Long,
    val engagementId: Long,
    val advertisementId: Long,
    val engagementType: String,
    val stationId: Long?,
    val timestamp: java.time.LocalDateTime,
    val eventType: String
)

/**
 * Message for ticket multiplication events
 */
data class TicketMultiplicationMessage(
    val userId: Long,
    val engagementId: Long,
    val baseTickets: Int,
    val bonusTickets: Int,
    val totalTickets: Int,
    val multiplier: java.math.BigDecimal,
    val timestamp: java.time.LocalDateTime,
    val eventType: String
)