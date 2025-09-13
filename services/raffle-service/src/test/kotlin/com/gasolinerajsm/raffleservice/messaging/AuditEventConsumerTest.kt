package com.gasolinerajsm.raffleservice.messaging

import com.gasolinerajsm.messaging.events.AuditEvent
import com.gasolinerajsm.messaging.events.AuditType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension
import java.time.LocalDateTime

/**
 * Integration tests for AuditEventConsumer
 */
@ExtendWith(SpringJUnitExtension::class)
@SpringBootTest
class AuditEventConsumerTest {

    private lateinit var auditEventConsumer: AuditEventConsumer

    @BeforeEach
    fun setUp() {
        auditEventConsumer = AuditEventConsumer()
    }

    @Test
    fun `should handle general audit event successfully`() {
        // Given
        val event = AuditEvent(
            action = "RAFFLE_TICKETS_PROCESSED",
            resource = "raffle_tickets",
            resourceId = "1,2,3,4,5",
            auditType = AuditType.SYSTEM_EVENT,
            userId = 456L,
            userRole = "CUSTOMER",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            stationId = 789L,
            success = true,
            errorMessage = null,
            details = mapOf(
                "ticketCount" to 5,
                "sourceType" to "REDEMPTION",
                "multiplierValue" to 1
            ),
            actionTimestamp = LocalDateTime.now(),
            source = "raffle-service"
        )

        // When & Then - Should not throw exception
        auditEventConsumer.handleGeneralAuditEvent(event)
    }

    @Test
    fun `should handle security violation audit event successfully`() {
        // Given
        val event = AuditEvent(
            action = "MULTIPLE_FAILED_LOGINS",
            resource = "authentication",
            resourceId = "user_456",
            auditType = AuditType.SECURITY_VIOLATION,
            userId = 456L,
            userRole = "CUSTOMER",
            ipAddress = "192.168.1.100",
            userAgent = "Suspicious Bot",
            stationId = null,
            success = false,
            errorMessage = "Multiple failed login attempts detected",
            details = mapOf(
                "attemptCount" to 5,
                "timeWindow" to "5 minutes"
            ),
            actionTimestamp = LocalDateTime.now(),
            source = "auth-service"
        )

        // When & Then - Should not throw exception
        auditEventConsumer.handleSecurityViolationEvent(event)
    }

    @Test
    fun `should handle transaction audit event successfully`() {
        // Given
        val event = AuditEvent(
            action = "REDEMPTION_COMPLETED",
            resource = "redemption",
            resourceId = "123",
            auditType = AuditType.TRANSACTION,
            userId = 456L,
            userRole = "CUSTOMER",
            ipAddress = "192.168.1.1",
            userAgent = "Mobile App",
            stationId = 789L,
            success = true,
            errorMessage = null,
            details = mapOf(
                "referenceNumber" to "REF123456",
                "totalAmount" to 50.00,
                "employeeId" to 101L
            ),
            actionTimestamp = LocalDateTime.now(),
            source = "redemption-service"
        )

        // When & Then - Should not throw exception
        auditEventConsumer.handleTransactionAuditEvent(event)
    }

    @Test
    fun `should handle user action audit event successfully`() {
        // Given
        val event = AuditEvent(
            action = "AD_ENGAGEMENT_QUALIFIED",
            resource = "ad_engagement",
            resourceId = "789",
            auditType = AuditType.USER_ACTION,
            userId = 456L,
            userRole = "CUSTOMER",
            ipAddress = "192.168.1.1",
            userAgent = "Mobile App",
            stationId = null,
            success = true,
            errorMessage = null,
            details = mapOf(
                "advertisementId" to 101L,
                "completionPercentage" to 95,
                "duration" to 30L
            ),
            actionTimestamp = LocalDateTime.now(),
            source = "raffle-service"
        )

        // When & Then - Should not throw exception
        auditEventConsumer.handleUserActionAuditEvent(event)
    }

    @Test
    fun `should handle system event audit event successfully`() {
        // Given
        val event = AuditEvent(
            action = "SERVICE_STARTUP",
            resource = "raffle_service",
            resourceId = null,
            auditType = AuditType.SYSTEM_EVENT,
            userId = null,
            userRole = null,
            ipAddress = null,
            userAgent = null,
            stationId = null,
            success = true,
            errorMessage = null,
            details = mapOf(
                "version" to "1.0.0",
                "environment" to "production"
            ),
            actionTimestamp = LocalDateTime.now(),
            source = "raffle-service"
        )

        // When & Then - Should not throw exception
        auditEventConsumer.handleSystemEventAuditEvent(event)
    }

    @Test
    fun `should handle data access audit event successfully`() {
        // Given
        val event = AuditEvent(
            action = "TICKET_DATA_ACCESSED",
            resource = "raffle_tickets",
            resourceId = "1,2,3",
            auditType = AuditType.DATA_ACCESS,
            userId = 456L,
            userRole = "CUSTOMER",
            ipAddress = "192.168.1.1",
            userAgent = "Web Browser",
            stationId = null,
            success = true,
            errorMessage = null,
            details = mapOf(
                "accessType" to "READ",
                "recordCount" to 3
            ),
            actionTimestamp = LocalDateTime.now(),
            source = "raffle-service"
        )

        // When & Then - Should not throw exception
        auditEventConsumer.handleDataAccessAuditEvent(event)
    }

    @Test
    fun `should handle configuration change audit event successfully`() {
        // Given
        val event = AuditEvent(
            action = "RAFFLE_CONFIG_UPDATED",
            resource = "raffle_configuration",
            resourceId = "raffle_123",
            auditType = AuditType.CONFIGURATION_CHANGE,
            userId = 101L,
            userRole = "ADMIN",
            ipAddress = "192.168.1.10",
            userAgent = "Admin Panel",
            stationId = null,
            success = true,
            errorMessage = null,
            details = mapOf(
                "configKey" to "maxTicketsPerUser",
                "oldValue" to 10,
                "newValue" to 15
            ),
            actionTimestamp = LocalDateTime.now(),
            source = "raffle-service"
        )

        // When & Then - Should not throw exception
        auditEventConsumer.handleConfigurationChangeAuditEvent(event)
    }

    @Test
    fun `should handle audit event with exception gracefully`() {
        // Given
        val event = AuditEvent(
            action = "INVALID_ACTION",
            resource = "unknown_resource",
            resourceId = null,
            auditType = AuditType.SYSTEM_EVENT,
            userId = null,
            userRole = null,
            ipAddress = null,
            userAgent = null,
            stationId = null,
            success = false,
            errorMessage = "Test error message",
            details = emptyMap(),
            actionTimestamp = LocalDateTime.now(),
            source = "test-service"
        )

        // When & Then - Should not throw exception even with invalid data
        auditEventConsumer.handleGeneralAuditEvent(event)
    }
}