package com.gasolinerajsm.adengine.messaging

import com.gasolinerajsm.messaging.events.AuditEvent
import com.gasolinerajsm.messaging.events.AuditType
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * Consumer for audit events in the ad engine service
 */
@Component
class AuditEventConsumer {

    private val logger = LoggerFactory.getLogger(AuditEventConsumer::class.java)
    private val auditLogger = LoggerFactory.getLogger("AUDIT")
    private val engagementLogger = LoggerFactory.getLogger("ENGAGEMENT")
    private val analyticsLogger = LoggerFactory.getLogger("ANALYTICS")

    /**
     * Handle ad engagement audit events
     */
    @RabbitListener(queues = ["audit.ad.engagement.queue"])
    fun handleAdEngagementAuditEvent(event: AuditEvent) {
        try {
            logAuditEvent(event)

            // Process ad engagement specific audit logic
            when (event.action) {
                "AD_ENGAGEMENT_STARTED",
                "AD_ENGAGEMENT_COMPLETED",
                "AD_ENGAGEMENT_ABANDONED" -> processEngagementAudit(event)

                "AD_TICKETS_MULTIPLIED",
                "AD_TICKETS_MULTIPLIED_PROCESSED" -> processTicketMultiplicationAudit(event)

                "AD_CAMPAIGN_UPDATED",
                "AD_CAMPAIGN_ACTIVATED",
                "AD_CAMPAIGN_DEACTIVATED" -> processCampaignAudit(event)

                else -> processGeneralAudit(event)
            }

        } catch (exception: Exception) {
            logger.error("Error processing ad engagement audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle ad analytics audit events
     */
    @RabbitListener(queues = ["audit.ad.analytics.queue"])
    fun handleAdAnalyticsAuditEvent(event: AuditEvent) {
        try {
            analyticsLogger.info(
                "AD_ANALYTICS - Action: {}, ResourceId: {}, User: {}, Details: {}, Success: {}",
                event.action,
                event.resourceId ?: "N/A",
                event.userId ?: "SYSTEM",
                event.details,
                event.success
            )

            processAnalyticsAudit(event)

            // Track ad performance metrics
            trackAdPerformanceMetrics(event)

        } catch (exception: Exception) {
            logger.error("Error processing ad analytics audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle ad fraud detection audit events
     */
    @RabbitListener(queues = ["audit.ad.fraud.queue"])
    fun handleAdFraudAuditEvent(event: AuditEvent) {
        try {
            logger.warn(
                "AD_FRAUD_DETECTION - Action: {}, User: {}, Ad: {}, Details: {}, Success: {}",
                event.action,
                event.userId ?: "UNKNOWN",
                event.resourceId ?: "N/A",
                event.details,
                event.success
            )

            processAdFraudAudit(event)

            // Alert on potential ad fraud
            if (isPotentialAdFraud(event)) {
                logger.error("Potential ad fraud detected: ${event.eventId}")
                // Could trigger additional fraud prevention measures
            }

        } catch (exception: Exception) {
            logger.error("Error processing ad fraud audit event: ${event.eventId}", exception)
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
                logger.error("CRITICAL_AD_ENGINE_AUDIT: $logMessage")
            }
        }
    }

    /**
     * Build structured audit log message
     */
    private fun buildAuditLogMessage(event: AuditEvent): String {
        return buildString {
            append("AD_ENGINE_AUDIT - ")
            append("EventId: ${event.eventId}, ")
            append("Action: ${event.action}, ")
            append("Resource: ${event.resource}, ")
            append("ResourceId: ${event.resourceId ?: "N/A"}, ")
            append("User: ${event.userId ?: "SYSTEM"}, ")
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
     * Process engagement audit events
     */
    private fun processEngagementAudit(event: AuditEvent) {
        engagementLogger.info("Processing engagement audit: ${event.action}")

        // Track engagement patterns
        when (event.action) {
            "AD_ENGAGEMENT_STARTED" -> trackEngagementStart(event)
            "AD_ENGAGEMENT_COMPLETED" -> trackEngagementCompletion(event)
            "AD_ENGAGEMENT_ABANDONED" -> trackEngagementAbandonment(event)
        }
    }

    /**
     * Process ticket multiplication audit events
     */
    private fun processTicketMultiplicationAudit(event: AuditEvent) {
        logger.debug("Processing ticket multiplication audit: ${event.action}")

        // Track ticket multiplication patterns
        when (event.action) {
            "AD_TICKETS_MULTIPLIED" -> trackTicketMultiplication(event)
            "AD_TICKETS_MULTIPLIED_PROCESSED" -> trackTicketMultiplicationProcessing(event)
        }
    }

    /**
     * Process campaign audit events
     */
    private fun processCampaignAudit(event: AuditEvent) {
        logger.debug("Processing campaign audit: ${event.action}")

        // Track campaign changes
        when (event.action) {
            "AD_CAMPAIGN_UPDATED" -> trackCampaignUpdate(event)
            "AD_CAMPAIGN_ACTIVATED" -> trackCampaignActivation(event)
            "AD_CAMPAIGN_DEACTIVATED" -> trackCampaignDeactivation(event)
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
     * Process analytics audit events
     */
    private fun processAnalyticsAudit(event: AuditEvent) {
        logger.debug("Processing analytics audit: ${event.action}")

        // Track analytics events
        trackAnalyticsEvent(event)
    }

    /**
     * Process ad fraud audit events
     */
    private fun processAdFraudAudit(event: AuditEvent) {
        logger.debug("Processing ad fraud audit: ${event.action}")

        // Track fraud patterns
        trackAdFraudPattern(event)
    }

    /**
     * Check if event indicates potential ad fraud
     */
    private fun isPotentialAdFraud(event: AuditEvent): Boolean {
        return when (event.action) {
            "SUSPICIOUS_ENGAGEMENT_PATTERN",
            "RAPID_MULTIPLE_ENGAGEMENTS",
            "INVALID_ENGAGEMENT_DATA",
            "BOT_LIKE_BEHAVIOR" -> true
            else -> false
        }
    }

    /**
     * Track engagement start patterns
     */
    private fun trackEngagementStart(event: AuditEvent) {
        engagementLogger.debug("Tracking engagement start for user: ${event.userId}")
        // Implementation for engagement start tracking
    }

    /**
     * Track engagement completion patterns
     */
    private fun trackEngagementCompletion(event: AuditEvent) {
        engagementLogger.info("Tracking engagement completion for user: ${event.userId}")

        // Extract engagement metrics from details
        val completionPercentage = event.details["completionPercentage"] as? Int ?: 0
        val duration = event.details["duration"] as? Long ?: 0
        val advertisementId = event.details["advertisementId"] as? Long

        engagementLogger.info(
            "Engagement completed - User: {}, Ad: {}, Completion: {}%, Duration: {}s",
            event.userId,
            advertisementId,
            completionPercentage,
            duration
        )
    }

    /**
     * Track engagement abandonment patterns
     */
    private fun trackEngagementAbandonment(event: AuditEvent) {
        engagementLogger.debug("Tracking engagement abandonment for user: ${event.userId}")
        // Implementation for engagement abandonment tracking
    }

    /**
     * Track ticket multiplication patterns
     */
    private fun trackTicketMultiplication(event: AuditEvent) {
        logger.info("Tracking ticket multiplication for user: ${event.userId}")

        // Extract multiplication details
        val multiplier = event.details["multiplier"] as? Int ?: 1
        val bonusTickets = event.details["bonusTicketCount"] as? Int ?: 0
        val advertisementId = event.details["advertisementId"] as? Long

        logger.info(
            "Tickets multiplied - User: {}, Ad: {}, Multiplier: {}x, Bonus: {} tickets",
            event.userId,
            advertisementId,
            multiplier,
            bonusTickets
        )
    }

    /**
     * Track ticket multiplication processing
     */
    private fun trackTicketMultiplicationProcessing(event: AuditEvent) {
        logger.debug("Tracking ticket multiplication processing for user: ${event.userId}")
        // Implementation for ticket multiplication processing tracking
    }

    /**
     * Track campaign updates
     */
    private fun trackCampaignUpdate(event: AuditEvent) {
        logger.info("Tracking campaign update for campaign: ${event.resourceId}")
        // Implementation for campaign update tracking
    }

    /**
     * Track campaign activation
     */
    private fun trackCampaignActivation(event: AuditEvent) {
        logger.info("Tracking campaign activation for campaign: ${event.resourceId}")
        // Implementation for campaign activation tracking
    }

    /**
     * Track campaign deactivation
     */
    private fun trackCampaignDeactivation(event: AuditEvent) {
        logger.info("Tracking campaign deactivation for campaign: ${event.resourceId}")
        // Implementation for campaign deactivation tracking
    }

    /**
     * Track ad performance metrics
     */
    private fun trackAdPerformanceMetrics(event: AuditEvent) {
        analyticsLogger.debug("Tracking ad performance metrics for event: ${event.eventId}")
        // Implementation for ad performance metrics tracking
    }

    /**
     * Track analytics events
     */
    private fun trackAnalyticsEvent(event: AuditEvent) {
        analyticsLogger.debug("Tracking analytics event: ${event.action}")
        // Implementation for analytics event tracking
    }

    /**
     * Track ad fraud patterns
     */
    private fun trackAdFraudPattern(event: AuditEvent) {
        logger.debug("Tracking ad fraud pattern for event: ${event.eventId}")
        // Implementation for ad fraud pattern tracking
    }
}