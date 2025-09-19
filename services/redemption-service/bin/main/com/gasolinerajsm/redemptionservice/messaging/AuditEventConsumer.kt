package com.gasolinerajsm.redemptionservice.messaging

import com.gasolinerajsm.messaging.events.AuditEvent
import com.gasolinerajsm.messaging.events.AuditType
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * Consumer for audit events in the redemption service
 */
@Component
class AuditEventConsumer {

    private val logger = LoggerFactory.getLogger(AuditEventConsumer::class.java)
    private val auditLogger = LoggerFactory.getLogger("AUDIT")
    private val transactionLogger = LoggerFactory.getLogger("TRANSACTION")
    private val securityLogger = LoggerFactory.getLogger("SECURITY")

    /**
     * Handle redemption-specific audit events
     */
    @RabbitListener(queues = ["audit.redemption.queue"])
    fun handleRedemptionAuditEvent(event: AuditEvent) {
        try {
            logAuditEvent(event)

            // Process redemption-specific audit logic
            when (event.action) {
                "REDEMPTION_CREATED",
                "REDEMPTION_COMPLETED",
                "REDEMPTION_VOIDED",
                "REDEMPTION_EXPIRED" -> processRedemptionAudit(event)

                "COUPON_VALIDATED",
                "COUPON_REDEEMED" -> processCouponAudit(event)

                "RAFFLE_TICKETS_GENERATED",
                "RAFFLE_TICKETS_PROCESSED" -> processTicketAudit(event)

                else -> processGeneralAudit(event)
            }

        } catch (exception: Exception) {
            logger.error("Error processing redemption audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle transaction audit events
     */
    @RabbitListener(queues = ["audit.transaction.redemption.queue"])
    fun handleTransactionAuditEvent(event: AuditEvent) {
        try {
            transactionLogger.info(
                "REDEMPTION_TRANSACTION - Action: {}, ResourceId: {}, User: {}, Station: {}, Amount: {}, Success: {}",
                event.action,
                event.resourceId ?: "N/A",
                event.userId ?: "SYSTEM",
                event.stationId ?: "N/A",
                event.details["totalAmount"] ?: "N/A",
                event.success
            )

            processTransactionAudit(event)

            // Track redemption transaction patterns
            trackRedemptionTransactionPattern(event)

        } catch (exception: Exception) {
            logger.error("Error processing transaction audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle fraud detection audit events
     */
    @RabbitListener(queues = ["audit.fraud.detection.queue"])
    fun handleFraudDetectionAuditEvent(event: AuditEvent) {
        try {
            securityLogger.warn(
                "FRAUD_DETECTION - Action: {}, User: {}, Station: {}, Details: {}, Success: {}",
                event.action,
                event.userId ?: "UNKNOWN",
                event.stationId ?: "N/A",
                event.details,
                event.success
            )

            processFraudDetectionAudit(event)

            // Alert on potential fraud
            if (isPotentialFraud(event)) {
                logger.error("Potential fraud detected: ${event.eventId}")
                // Could trigger additional fraud prevention measures
            }

        } catch (exception: Exception) {
            logger.error("Error processing fraud detection audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Log audit event with appropriate level
     */
    private fun logAuditEvent(event: AuditEvent) {
        val logMessage = buildAuditLogMessage(event)

        when (event.getAuditLevel()) {
            com.gasolinerajsm.messaging.events.AuditLevel.DEBUG -> auditLogger.debug(logMessage)
            com.gasolinerajsm.messaging.events.AuditLevel.INFO -> auditLogger.info(logMessage)
            com.gasolinerajsm.messaging.events.AuditLevel.WARN -> auditLogger.warn(logMessage)
            com.gasolinerajsm.messaging.events.AuditLevel.ERROR -> auditLogger.error(logMessage)
            com.gasolinerajsm.messaging.events.AuditLevel.CRITICAL -> {
                auditLogger.error("CRITICAL: $logMessage")
                securityLogger.error("CRITICAL_REDEMPTION_AUDIT: $logMessage")
            }
        }
    }

    /**
     * Build structured audit log message
     */
    private fun buildAuditLogMessage(event: AuditEvent): String {
        return buildString {
            append("REDEMPTION_AUDIT - ")
            append("EventId: ${event.eventId}, ")
            append("Action: ${event.action}, ")
            append("Resource: ${event.resource}, ")
            append("ResourceId: ${event.resourceId ?: "N/A"}, ")
            append("User: ${event.userId ?: "SYSTEM"}, ")
            append("Station: ${event.stationId ?: "N/A"}, ")
            append("Success: ${event.success}, ")
            append("Timestamp: ${event.actionTimestamp}")

            if (event.errorMessage != null) {
                append(", Error: ${event.errorMessage}")
            }

            if (event.details.isNotEmpty()) {
                append(", Details: ${event.details}")
            }
        }
    }

    /**
     * Process redemption-specific audit events
     */
    private fun processRedemptionAudit(event: AuditEvent) {
        logger.debug("Processing redemption audit: ${event.action}")

        // Track redemption patterns
        when (event.action) {
            "REDEMPTION_CREATED" -> trackRedemptionCreation(event)
            "REDEMPTION_COMPLETED" -> trackRedemptionCompletion(event)
            "REDEMPTION_VOIDED" -> trackRedemptionVoid(event)
            "REDEMPTION_EXPIRED" -> trackRedemptionExpiration(event)
        }
    }

    /**
     * Process coupon-specific audit events
     */
    private fun processCouponAudit(event: AuditEvent) {
        logger.debug("Processing coupon audit: ${event.action}")

        // Track coupon usage patterns
        when (event.action) {
            "COUPON_VALIDATED" -> trackCouponValidation(event)
            "COUPON_REDEEMED" -> trackCouponRedemption(event)
        }
    }

    /**
     * Process ticket-specific audit events
     */
    private fun processTicketAudit(event: AuditEvent) {
        logger.debug("Processing ticket audit: ${event.action}")

        // Track ticket generation patterns
        when (event.action) {
            "RAFFLE_TICKETS_GENERATED" -> trackTicketGeneration(event)
            "RAFFLE_TICKETS_PROCESSED" -> trackTicketProcessing(event)
        }
    }

    /**
     * Process general audit events
     */
    private fun processGeneralAudit(event: AuditEvent) {
        logger.debug("Processing general audit: ${event.action}")

        // Handle other audit events
    }

    /**
     * Process transaction audit events
     */
    private fun processTransactionAudit(event: AuditEvent) {
        logger.debug("Processing transaction audit: ${event.action}")

        // Track transaction metrics
        trackTransactionMetrics(event)
    }

    /**
     * Process fraud detection audit events
     */
    private fun processFraudDetectionAudit(event: AuditEvent) {
        logger.debug("Processing fraud detection audit: ${event.action}")

        // Track fraud patterns
        trackFraudPattern(event)
    }

    /**
     * Check if event indicates potential fraud
     */
    private fun isPotentialFraud(event: AuditEvent): Boolean {
        return when (event.action) {
            "SUSPICIOUS_REDEMPTION_PATTERN",
            "MULTIPLE_RAPID_REDEMPTIONS",
            "INVALID_COUPON_USAGE",
            "UNUSUAL_STATION_ACTIVITY" -> true
            else -> false
        }
    }

    /**
     * Track redemption creation patterns
     */
    private fun trackRedemptionCreation(event: AuditEvent) {
        logger.debug("Tracking redemption creation for user: ${event.userId}")
        // Implementation for redemption creation tracking
    }

    /**
     * Track redemption completion patterns
     */
    private fun trackRedemptionCompletion(event: AuditEvent) {
        logger.debug("Tracking redemption completion for user: ${event.userId}")
        // Implementation for redemption completion tracking
    }

    /**
     * Track redemption void patterns
     */
    private fun trackRedemptionVoid(event: AuditEvent) {
        logger.debug("Tracking redemption void for user: ${event.userId}")
        // Implementation for redemption void tracking
    }

    /**
     * Track redemption expiration patterns
     */
    private fun trackRedemptionExpiration(event: AuditEvent) {
        logger.debug("Tracking redemption expiration for user: ${event.userId}")
        // Implementation for redemption expiration tracking
    }

    /**
     * Track coupon validation patterns
     */
    private fun trackCouponValidation(event: AuditEvent) {
        logger.debug("Tracking coupon validation for user: ${event.userId}")
        // Implementation for coupon validation tracking
    }

    /**
     * Track coupon redemption patterns
     */
    private fun trackCouponRedemption(event: AuditEvent) {
        logger.debug("Tracking coupon redemption for user: ${event.userId}")
        // Implementation for coupon redemption tracking
    }

    /**
     * Track ticket generation patterns
     */
    private fun trackTicketGeneration(event: AuditEvent) {
        logger.debug("Tracking ticket generation for user: ${event.userId}")
        // Implementation for ticket generation tracking
    }

    /**
     * Track ticket processing patterns
     */
    private fun trackTicketProcessing(event: AuditEvent) {
        logger.debug("Tracking ticket processing for user: ${event.userId}")
        // Implementation for ticket processing tracking
    }

    /**
     * Track redemption transaction patterns
     */
    private fun trackRedemptionTransactionPattern(event: AuditEvent) {
        logger.debug("Tracking redemption transaction pattern for event: ${event.eventId}")
        // Implementation for transaction pattern tracking
    }

    /**
     * Track transaction metrics
     */
    private fun trackTransactionMetrics(event: AuditEvent) {
        logger.debug("Tracking transaction metrics for event: ${event.eventId}")
        // Implementation for transaction metrics tracking
    }

    /**
     * Track fraud patterns
     */
    private fun trackFraudPattern(event: AuditEvent) {
        logger.debug("Tracking fraud pattern for event: ${event.eventId}")
        // Implementation for fraud pattern tracking
    }
}