package com.gasolinerajsm.raffleservice.messaging

import com.gasolinerajsm.messaging.events.AuditEvent
import com.gasolinerajsm.messaging.events.AuditType
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Consumer for audit events to provide centralized audit logging and monitoring
 */
@Component
class AuditEventConsumer {

    private val logger = LoggerFactory.getLogger(AuditEventConsumer::class.java)
    private val auditLogger = LoggerFactory.getLogger("AUDIT")
    private val securityLogger = LoggerFactory.getLogger("SECURITY")
    private val transactionLogger = LoggerFactory.getLogger("TRANSACTION")

    /**
     * Handle general audit events
     */
    @RabbitListener(queues = ["audit.general.queue"])
    fun handleGeneralAuditEvent(event: AuditEvent) {
        try {
            logAuditEvent(event)

            // Process audit event based on type
            when (event.auditType) {
                AuditType.USER_ACTION -> processUserActionAudit(event)
                AuditType.SYSTEM_EVENT -> processSystemEventAudit(event)
                AuditType.TRANSACTION -> processTransactionAudit(event)
                AuditType.DATA_ACCESS -> processDataAccessAudit(event)
                AuditType.CONFIGURATION_CHANGE -> processConfigurationChangeAudit(event)
                AuditType.SECURITY_VIOLATION -> processSecurityViolationAudit(event)
            }

        } catch (exception: Exception) {
            logger.error("Error processing audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle security-specific audit events
     */
    @RabbitListener(queues = ["audit.security.violation.queue"])
    fun handleSecurityViolationEvent(event: AuditEvent) {
        try {
            securityLogger.error(
                "SECURITY_VIOLATION - Action: {}, Resource: {}, User: {}, IP: {}, Success: {}, Error: {}",
                event.action,
                event.resource,
                event.userId ?: "UNKNOWN",
                event.ipAddress ?: "UNKNOWN",
                event.success,
                event.errorMessage ?: "N/A"
            )

            // Additional security processing
            processSecurityViolationAudit(event)

            // Could trigger additional security measures here
            if (isHighRiskSecurityEvent(event)) {
                logger.warn("High-risk security event detected: ${event.eventId}")
                // Could send alerts, temporarily block user, etc.
            }

        } catch (exception: Exception) {
            logger.error("Error processing security audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle transaction-specific audit events
     */
    @RabbitListener(queues = ["audit.transaction.completed.queue"])
    fun handleTransactionAuditEvent(event: AuditEvent) {
        try {
            transactionLogger.info(
                "TRANSACTION - Action: {}, Resource: {}, ResourceId: {}, User: {}, Station: {}, Success: {}",
                event.action,
                event.resource,
                event.resourceId ?: "N/A",
                event.userId ?: "SYSTEM",
                event.stationId ?: "N/A",
                event.success
            )

            processTransactionAudit(event)

            // Track transaction patterns for analytics
            trackTransactionPattern(event)

        } catch (exception: Exception) {
            logger.error("Error processing transaction audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle user action audit events
     */
    @RabbitListener(queues = ["audit.user.action.queue"])
    fun handleUserActionAuditEvent(event: AuditEvent) {
        try {
            auditLogger.info(
                "USER_ACTION - Action: {}, Resource: {}, User: {}, Success: {}, Details: {}",
                event.action,
                event.resource,
                event.userId ?: "ANONYMOUS",
                event.success,
                event.details
            )

            processUserActionAudit(event)

            // Track user behavior patterns
            trackUserBehaviorPattern(event)

        } catch (exception: Exception) {
            logger.error("Error processing user action audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle system event audit events
     */
    @RabbitListener(queues = ["audit.system.event.queue"])
    fun handleSystemEventAuditEvent(event: AuditEvent) {
        try {
            auditLogger.info(
                "SYSTEM_EVENT - Action: {}, Resource: {}, Source: {}, Success: {}, Error: {}",
                event.action,
                event.resource,
                event.source,
                event.success,
                event.errorMessage ?: "N/A"
            )

            processSystemEventAudit(event)

            // Monitor system health patterns
            monitorSystemHealth(event)

        } catch (exception: Exception) {
            logger.error("Error processing system event audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle data access audit events
     */
    @RabbitListener(queues = ["audit.data.access.queue"])
    fun handleDataAccessAuditEvent(event: AuditEvent) {
        try {
            auditLogger.debug(
                "DATA_ACCESS - Action: {}, Resource: {}, ResourceId: {}, User: {}, Success: {}",
                event.action,
                event.resource,
                event.resourceId ?: "N/A",
                event.userId ?: "SYSTEM",
                event.success
            )

            processDataAccessAudit(event)

            // Track data access patterns for compliance
            trackDataAccessPattern(event)

        } catch (exception: Exception) {
            logger.error("Error processing data access audit event: ${event.eventId}", exception)
        }
    }

    /**
     * Handle configuration change audit events
     */
    @RabbitListener(queues = ["audit.config.change.queue"])
    fun handleConfigurationChangeAuditEvent(event: AuditEvent) {
        try {
            auditLogger.warn(
                "CONFIG_CHANGE - Action: {}, Resource: {}, User: {}, Success: {}, Details: {}",
                event.action,
                event.resource,
                event.userId ?: "SYSTEM",
                event.success,
                event.details
            )

            processConfigurationChangeAudit(event)

            // Track configuration changes for compliance
            trackConfigurationChange(event)

        } catch (exception: Exception) {
            logger.error("Error processing configuration change audit event: ${event.eventId}", exception)
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
                securityLogger.error("CRITICAL_AUDIT: $logMessage")
            }
        }
    }

    /**
     * Build structured audit log message
     */
    private fun buildAuditLogMessage(event: AuditEvent): String {
        return buildString {
            append("AUDIT_EVENT - ")
            append("EventId: ${event.eventId}, ")
            append("Type: ${event.auditType}, ")
            append("Action: ${event.action}, ")
            append("Resource: ${event.resource}, ")
            append("ResourceId: ${event.resourceId ?: "N/A"}, ")
            append("User: ${event.userId ?: "SYSTEM"}, ")
            append("Role: ${event.userRole ?: "N/A"}, ")
            append("Station: ${event.stationId ?: "N/A"}, ")
            append("Success: ${event.success}, ")
            append("Source: ${event.source}, ")
            append("Timestamp: ${event.actionTimestamp}, ")
            append("IP: ${event.ipAddress ?: "N/A"}, ")
            append("UserAgent: ${event.userAgent ?: "N/A"}, ")
            append("CorrelationId: ${event.correlationId ?: "N/A"}, ")
            append("SessionId: ${event.sessionId ?: "N/A"}")

            if (event.errorMessage != null) {
                append(", Error: ${event.errorMessage}")
            }

            if (event.details.isNotEmpty()) {
                append(", Details: ${event.details}")
            }
        }
    }

    /**
     * Process user action audit events
     */
    private fun processUserActionAudit(event: AuditEvent) {
        // Track user activity patterns
        logger.debug("Processing user action audit: ${event.action} by user ${event.userId}")

        // Could implement user behavior analytics here
        // For example, tracking login patterns, failed attempts, etc.
    }

    /**
     * Process system event audit events
     */
    private fun processSystemEventAudit(event: AuditEvent) {
        // Track system events for monitoring
        logger.debug("Processing system event audit: ${event.action} from ${event.source}")

        // Could implement system health monitoring here
        // For example, tracking error rates, performance issues, etc.
    }

    /**
     * Process transaction audit events
     */
    private fun processTransactionAudit(event: AuditEvent) {
        // Track transaction patterns
        logger.debug("Processing transaction audit: ${event.action} for resource ${event.resource}")

        // Could implement transaction analytics here
        // For example, tracking transaction volumes, success rates, etc.
    }

    /**
     * Process data access audit events
     */
    private fun processDataAccessAudit(event: AuditEvent) {
        // Track data access for compliance
        logger.debug("Processing data access audit: ${event.action} on ${event.resource}")

        // Could implement data access monitoring here
        // For example, tracking sensitive data access, unusual patterns, etc.
    }

    /**
     * Process configuration change audit events
     */
    private fun processConfigurationChangeAudit(event: AuditEvent) {
        // Track configuration changes
        logger.debug("Processing configuration change audit: ${event.action} on ${event.resource}")

        // Could implement configuration change tracking here
        // For example, maintaining configuration history, approval workflows, etc.
    }

    /**
     * Process security violation audit events
     */
    private fun processSecurityViolationAudit(event: AuditEvent) {
        // Handle security violations
        logger.warn("Processing security violation audit: ${event.action} by user ${event.userId}")

        // Could implement security response here
        // For example, alerting security team, blocking suspicious IPs, etc.
    }

    /**
     * Check if security event is high risk
     */
    private fun isHighRiskSecurityEvent(event: AuditEvent): Boolean {
        return when (event.action) {
            "MULTIPLE_FAILED_LOGINS",
            "UNAUTHORIZED_ACCESS_ATTEMPT",
            "PRIVILEGE_ESCALATION_ATTEMPT",
            "DATA_BREACH_ATTEMPT" -> true
            else -> false
        }
    }

    /**
     * Track transaction patterns for analytics
     */
    private fun trackTransactionPattern(event: AuditEvent) {
        // Implementation for transaction pattern tracking
        logger.debug("Tracking transaction pattern for event: ${event.eventId}")
    }

    /**
     * Track user behavior patterns
     */
    private fun trackUserBehaviorPattern(event: AuditEvent) {
        // Implementation for user behavior pattern tracking
        logger.debug("Tracking user behavior pattern for user: ${event.userId}")
    }

    /**
     * Monitor system health based on events
     */
    private fun monitorSystemHealth(event: AuditEvent) {
        // Implementation for system health monitoring
        if (!event.success) {
            logger.debug("System health issue detected: ${event.action} failed")
        }
    }

    /**
     * Track data access patterns for compliance
     */
    private fun trackDataAccessPattern(event: AuditEvent) {
        // Implementation for data access pattern tracking
        logger.debug("Tracking data access pattern for resource: ${event.resource}")
    }

    /**
     * Track configuration changes
     */
    private fun trackConfigurationChange(event: AuditEvent) {
        // Implementation for configuration change tracking
        logger.debug("Tracking configuration change: ${event.action} on ${event.resource}")
    }
}