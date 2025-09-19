package com.gasolinerajsm.messaging.events

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * Event published for audit trail and security monitoring
 */
data class AuditEvent(
    val action: String,
    val resource: String,
    val resourceId: String?,
    val auditType: AuditType,
    override val userId: Long?,
    val userRole: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val stationId: Long?,
    val success: Boolean,
    val errorMessage: String?,
    val reason: String? = null,
    val details: Map<String, Any> = emptyMap(),

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val actionTimestamp: LocalDateTime,

    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    override val version: String = "1.0",
    override val source: String,
    override val correlationId: String? = null,
    override val causationId: String? = null,
    override val sessionId: String? = null,
    override val metadata: Map<String, Any> = emptyMap()
) : BaseEvent(eventId, timestamp, version, source, correlationId, causationId, userId, sessionId, metadata) {

    override fun getEventType(): String = "AUDIT_EVENT"

    override fun getRoutingKey(): String = when (auditType) {
        AuditType.USER_REGISTRATION -> "audit.user.registration"
        AuditType.USER_LOGIN -> "audit.user.login"
        AuditType.USER_LOGOUT -> "audit.user.logout"
        AuditType.COUPON_CREATED -> "audit.coupon.created"
        AuditType.COUPON_REDEEMED -> "audit.coupon.redeemed"
        AuditType.COUPON_VALIDATED -> "audit.coupon.validated"
        AuditType.RAFFLE_CREATED -> "audit.raffle.created"
        AuditType.RAFFLE_ENTRY -> "audit.raffle.entry"
        AuditType.RAFFLE_DRAW -> "audit.raffle.draw"
        AuditType.AD_VIEWED -> "audit.ad.viewed"
        AuditType.AD_CLICKED -> "audit.ad.clicked"
        AuditType.TICKET_GENERATED -> "audit.ticket.generated"
        AuditType.TICKET_MULTIPLIED -> "audit.ticket.multiplied"
        AuditType.PRIZE_AWARDED -> "audit.prize.awarded"
        AuditType.SYSTEM_ERROR -> "audit.system.error"
        AuditType.SECURITY_VIOLATION -> "audit.security.violation"
        AuditType.DATA_EXPORT -> "audit.data.export"
        AuditType.DATA_IMPORT -> "audit.data.import"
        AuditType.CONFIGURATION_CHANGE -> "audit.config.change"
        AuditType.PAYMENT_PROCESSED -> "audit.payment.processed"
        AuditType.USER_ACTION -> "audit.user.action"
        AuditType.SYSTEM_EVENT -> "audit.system.event"
        AuditType.TRANSACTION -> "audit.transaction.completed"
        AuditType.DATA_ACCESS -> "audit.data.access"
    }

    override fun getAuditLevel(): AuditLevel = when (auditType) {
        AuditType.SECURITY_VIOLATION -> AuditLevel.CRITICAL
        AuditType.SYSTEM_ERROR -> AuditLevel.CRITICAL
        AuditType.CONFIGURATION_CHANGE -> AuditLevel.WARN
        AuditType.DATA_EXPORT, AuditType.DATA_IMPORT -> AuditLevel.WARN
        AuditType.USER_REGISTRATION, AuditType.USER_LOGIN, AuditType.USER_LOGOUT -> if (success) AuditLevel.INFO else AuditLevel.WARN
        AuditType.COUPON_CREATED, AuditType.COUPON_REDEEMED, AuditType.COUPON_VALIDATED -> AuditLevel.INFO
        AuditType.RAFFLE_CREATED, AuditType.RAFFLE_ENTRY, AuditType.RAFFLE_DRAW -> AuditLevel.INFO
        AuditType.AD_VIEWED, AuditType.AD_CLICKED -> AuditLevel.DEBUG
        AuditType.TICKET_GENERATED, AuditType.TICKET_MULTIPLIED -> AuditLevel.INFO
        AuditType.PRIZE_AWARDED -> AuditLevel.INFO
        AuditType.PAYMENT_PROCESSED -> AuditLevel.INFO
        AuditType.USER_ACTION -> AuditLevel.INFO
        AuditType.SYSTEM_EVENT -> AuditLevel.INFO
        AuditType.TRANSACTION -> AuditLevel.INFO
        AuditType.DATA_ACCESS -> AuditLevel.DEBUG
    }

    override fun shouldAudit(): Boolean = true

    override fun withCorrelationId(correlationId: String): BaseEvent {
        return this.copy(correlationId = correlationId)
    }

    override fun withCausationId(causationId: String): BaseEvent {
        return this.copy(causationId = causationId)
    }
}

