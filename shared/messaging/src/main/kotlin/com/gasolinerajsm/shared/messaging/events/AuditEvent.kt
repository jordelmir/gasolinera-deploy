package com.gasolinerajsm.shared.messaging.events

import java.time.LocalDateTime

/**
 * Audit event for tracking system activities
 */
data class AuditEvent(
    val id: String,
    val type: AuditType,
    val userId: String?,
    val entityId: String?,
    val entityType: String?,
    val action: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val source: String,
    val correlationId: String?
)

/**
 * Types of audit events
 */
enum class AuditType {
    USER_ACTION,
    SYSTEM_EVENT,
    SECURITY_EVENT,
    DATA_CHANGE,
    ERROR_EVENT,
    PERFORMANCE_EVENT
}