package com.gasolinerajsm.shared.logging

import java.time.LocalDateTime
import java.util.*

data class BusinessOperation(
    val id: String = UUID.randomUUID().toString(),
    val type: BusinessOperationType,
    val description: String,
    val userId: String? = null,
    val correlationId: String? = null,
    val startTime: LocalDateTime = LocalDateTime.now(),
    var endTime: LocalDateTime? = null,
    var status: OperationStatus = OperationStatus.IN_PROGRESS,
    var errorMessage: String? = null,
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    fun complete() {
        endTime = LocalDateTime.now()
        status = OperationStatus.COMPLETED
    }

    fun fail(error: String) {
        endTime = LocalDateTime.now()
        status = OperationStatus.FAILED
        errorMessage = error
    }

    fun getDurationMs(): Long? {
        return endTime?.let { end ->
            java.time.Duration.between(startTime, end).toMillis()
        }
    }
}

enum class BusinessOperationType {
    USER_LOGIN,
    USER_LOGOUT,
    USER_REGISTRATION,
    COUPON_CREATION,
    COUPON_REDEMPTION,
    CAMPAIGN_CREATION,
    CAMPAIGN_ACTIVATION,
    STATION_REGISTRATION,
    PAYMENT_PROCESSING,
    QR_CODE_SCAN,
    RAFFLE_ENTRY,
    REWARD_CLAIM,
    DATA_EXPORT,
    SYSTEM_BACKUP,
    SECURITY_AUDIT
}

enum class OperationStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

// Logging annotations
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogBusinessOperation(
    val type: BusinessOperationType,
    val description: String = ""
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogPerformance(
    val threshold: Long = 1000L // milliseconds
)

// Log event data class
data class LogEvent(
    val level: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val correlationId: String? = null,
    val userId: String? = null,
    val operation: BusinessOperation? = null,
    val metadata: Map<String, Any> = emptyMap()
)