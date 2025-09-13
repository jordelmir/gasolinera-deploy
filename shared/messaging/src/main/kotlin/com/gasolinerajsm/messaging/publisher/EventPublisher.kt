package com.gasolinerajsm.messaging.publisher

import com.gasolinerajsm.messaging.config.RabbitMQConfig
import com.gasolinerajsm.messaging.events.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Central event publisher for all domain events in the Gasolinera JSM platform
 */
@Service
class EventPublisher(
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)
    private val auditLogger = LoggerFactory.getLogger("AUDIT")

    /**
     * Publish any domain event
     */
    fun publishEvent(event: BaseEvent) {
        try {
            // Set correlation ID from MDC if not already set
            val eventWithCorrelation = if (event.correlationId == null) {
                val correlationId = MDC.get("correlationId") ?: event.eventId
                event.withCorrelationId(correlationId)
            } else {
                event
            }

            // Determine the exchange based on event source
            val exchange = getExchangeForEvent(eventWithCorrelation)
            val routingKey = eventWithCorrelation.getRoutingKey()

            logger.info(
                "Publishing event: {} with routing key: {} to exchange: {}",
                eventWithCorrelation.getEventType(),
                routingKey,
                exchange
            )

            // Publish the event
            rabbitTemplate.convertAndSend(exchange, routingKey, eventWithCorrelation) { message ->
                message.messageProperties.apply {
                    messageId = eventWithCorrelation.eventId
                    timestamp = java.util.Date.from(
                        eventWithCorrelation.timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant()
                    )
                    correlationId = eventWithCorrelation.correlationId
                    headers["eventType"] = eventWithCorrelation.getEventType()
                    headers["source"] = eventWithCorrelation.source
                    headers["version"] = eventWithCorrelation.version
                    eventWithCorrelation.userId?.let { headers["userId"] = it.toString() }
                    eventWithCorrelation.sessionId?.let { headers["sessionId"] = it }
                }
                message
            }

            // Log audit trail if needed
            if (eventWithCorrelation.shouldAudit()) {
                logAuditTrail(eventWithCorrelation)
            }

            logger.debug("Event published successfully: {}", eventWithCorrelation.eventId)

        } catch (exception: AmqpException) {
            logger.error("Failed to publish event: {} - {}", event.getEventType(), exception.message, exception)

            // Publish audit event for failed event publishing
            publishAuditEvent(
                action = "EVENT_PUBLISH_FAILED",
                resource = "messaging",
                resourceId = event.eventId,
                auditType = AuditType.SYSTEM_ERROR,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = event.source
            )

            throw EventPublishingException("Failed to publish event: ${event.getEventType()}", exception)
        }
    }

    /**
     * Publish redemption events
     */
    fun publishRedemptionCreated(event: RedemptionCreatedEvent) {
        publishEvent(event)
    }

    fun publishRedemptionCompleted(event: RedemptionCompletedEvent) {
        publishEvent(event)
    }

    fun publishRedemptionVoided(event: RedemptionVoidedEvent) {
        publishEvent(event)
    }

    fun publishRedemptionExpired(event: RedemptionExpiredEvent) {
        publishEvent(event)
    }

    /**
     * Publish raffle events
     */
    fun publishRaffleTicketsGenerated(event: RaffleTicketsGeneratedEvent) {
        publishEvent(event)
    }

    fun publishRaffleEntryCreated(event: RaffleEntryCreatedEvent) {
        publishEvent(event)
    }

    fun publishRaffleWinnerSelected(event: RaffleWinnerSelectedEvent) {
        publishEvent(event)
    }

    fun publishRaffleDrawExecuted(event: RaffleDrawExecutedEvent) {
        publishEvent(event)
    }

    /**
     * Publish ad engine events
     */
    fun publishAdEngagementCompleted(event: AdEngagementCompletedEvent) {
        publishEvent(event)
    }

    fun publishAdTicketsMultiplied(event: AdTicketsMultipliedEvent) {
        publishEvent(event)
    }

    fun publishAdEngagementStarted(event: AdEngagementStartedEvent) {
        publishEvent(event)
    }

    fun publishAdCampaignUpdated(event: AdCampaignUpdatedEvent) {
        publishEvent(event)
    }

    /**
     * Publish coupon events
     */
    fun publishCouponValidated(event: CouponValidatedEvent) {
        publishEvent(event)
    }

    fun publishCouponRedeemed(event: CouponRedeemedEvent) {
        publishEvent(event)
    }

    fun publishCouponCreated(event: CouponCreatedEvent) {
        publishEvent(event)
    }

    fun publishCouponExpired(event: CouponExpiredEvent) {
        publishEvent(event)
    }

    /**
     * Publish audit events
     */
    fun publishAuditEvent(
        action: String,
        resource: String,
        resourceId: String?,
        auditType: AuditType,
        userId: Long?,
        userRole: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        stationId: Long? = null,
        success: Boolean = true,
        errorMessage: String? = null,
        details: Map<String, Any> = emptyMap(),
        source: String
    ) {
        val auditEvent = AuditEvent(
            action = action,
            resource = resource,
            resourceId = resourceId,
            auditType = auditType,
            userId = userId,
            userRole = userRole,
            ipAddress = ipAddress,
            userAgent = userAgent,
            stationId = stationId,
            success = success,
            errorMessage = errorMessage,
            details = details,
            actionTimestamp = LocalDateTime.now(),
            source = source,
            correlationId = MDC.get("correlationId"),
            sessionId = MDC.get("sessionId")
        )

        publishEvent(auditEvent)
    }

    /**
     * Get the appropriate exchange for an event based on its source
     */
    private fun getExchangeForEvent(event: BaseEvent): String {
        return when (event.source) {
            "redemption-service" -> RabbitMQConfig.REDEMPTION_EXCHANGE
            "raffle-service" -> RabbitMQConfig.RAFFLE_EXCHANGE
            "ad-engine" -> RabbitMQConfig.AD_EXCHANGE
            "coupon-service" -> RabbitMQConfig.COUPON_EXCHANGE
            else -> when (event) {
                is AuditEvent -> RabbitMQConfig.AUDIT_EXCHANGE
                else -> RabbitMQConfig.GASOLINERA_EVENTS_EXCHANGE
            }
        }
    }

    /**
     * Log audit trail for events
     */
    private fun logAuditTrail(event: BaseEvent) {
        val auditLevel = event.getAuditLevel()
        val logMessage = "EVENT_PUBLISHED - Type: {}, EventId: {}, UserId: {}, Source: {}, CorrelationId: {}"

        when (auditLevel) {
            AuditLevel.DEBUG -> auditLogger.debug(
                logMessage, event.getEventType(), event.eventId, event.userId, event.source, event.correlationId
            )
            AuditLevel.INFO -> auditLogger.info(
                logMessage, event.getEventType(), event.eventId, event.userId, event.source, event.correlationId
            )
            AuditLevel.WARN -> auditLogger.warn(
                logMessage, event.getEventType(), event.eventId, event.userId, event.source, event.correlationId
            )
            AuditLevel.ERROR -> auditLogger.error(
                logMessage, event.getEventType(), event.eventId, event.userId, event.source, event.correlationId
            )
            AuditLevel.CRITICAL -> auditLogger.error(
                "CRITICAL_EVENT - $logMessage", event.getEventType(), event.eventId, event.userId, event.source, event.correlationId
            )
        }
    }
}

/**
 * Exception thrown when event publishing fails
 */
class EventPublishingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)