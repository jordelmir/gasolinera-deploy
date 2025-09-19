package com.gasolinerajsm.shared.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class StructuredLogger(private val logger: Logger) {

    companion object {
        fun getLogger(clazz: Class<*>): StructuredLogger {
            return StructuredLogger(LoggerFactory.getLogger(clazz))
        }

        fun getLogger(name: String): StructuredLogger {
            return StructuredLogger(LoggerFactory.getLogger(name))
        }
    }

    fun info(message: String, keyValues: Map<String, Any?> = emptyMap()) {
        logWithContext("INFO", message, null, keyValues)
    }

    fun warn(message: String, keyValues: Map<String, Any?> = emptyMap()) {
        logWithContext("WARN", message, null, keyValues)
    }

    fun error(message: String, throwable: Throwable? = null, keyValues: Map<String, Any?> = emptyMap()) {
        logWithContext("ERROR", message, throwable, keyValues)
    }

    fun debug(message: String, keyValues: Map<String, Any?> = emptyMap()) {
        logWithContext("DEBUG", message, null, keyValues)
    }

    private fun logWithContext(level: String, message: String, throwable: Throwable? = null, keyValues: Map<String, Any?>) {
        val originalMdc = MDC.getCopyOfContextMap() ?: emptyMap()

        try {
            // Add structured data to MDC
            keyValues.forEach { (key, value) ->
                MDC.put(key, value?.toString())
            }

            // Add timestamp
            MDC.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

            // Log the message
            when (level) {
                "INFO" -> logger.info(message)
                "WARN" -> logger.warn(message)
                "ERROR" -> if (throwable != null) logger.error(message, throwable) else logger.error(message)
                "DEBUG" -> logger.debug(message)
            }
        } finally {
            // Restore original MDC
            MDC.clear()
            originalMdc.forEach { (key, value) -> MDC.put(key, value) }
        }
    }
}

// Utility class for MDC operations
object MdcUtils {
    fun setCorrelationId(correlationId: String) {
        MDC.put("correlationId", correlationId)
    }

    fun getCorrelationId(): String? = MDC.get("correlationId")

    fun setUserId(userId: String) {
        MDC.put("userId", userId)
    }

    fun getUserId(): String? = MDC.get("userId")

    fun setRequestId(requestId: String) {
        MDC.put("requestId", requestId)
    }

    fun getRequestId(): String? = MDC.get("requestId")

    fun clear() {
        MDC.clear()
    }
}

// Correlation ID generator
object CorrelationIdGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}