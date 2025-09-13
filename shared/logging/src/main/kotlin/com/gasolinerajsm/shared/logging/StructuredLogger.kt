package com.gasolinerajsm.shared.logging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Logger estructurado que genera logs en formato JSON con contexto enriquecido
 */
@Component
class StructuredLogger {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    /**
     * Log de información estructurada
     */
    fun info(
        logger: Logger,
        message: String,
        event: LogEvent? = null,
        context: Map<String, Any> = emptyMap()
    ) {
        logStructured(logger, LogLevel.INFO, message, event, context)
    }

    /**
     * Log de warning estructurado
     */
    fun warn(
        logger: Logger,
        message: String,
        event: LogEvent? = null,
        context: Map<String, Any> = emptyMap(),
        throwable: Throwable? = null
    ) {
        logStructured(logger, LogLevel.WARN, message, event, context, throwable)
    }

    /**
     * Log de error estructurado
     */
    fun error(
        logger: Logger,
        message: String,
        event: LogEvent? = null,
        context: Map<String, Any> = emptyMap(),
        throwable: Throwable? = null
    ) {
        logStructured(logger, LogLevel.ERROR, message, event, context, throwable)
    }

    /**
     * Log de debug estructurado
     */
    fun debug(
        logger: Logger,
        message: String,
        event: LogEvent? = null,
        context: Map<String, Any> = emptyMap()
    ) {
        logStructured(logger, LogLevel.DEBUG, message, event, context)
    }

    /**
     * Log de operaciones de negocio
     */
    fun business(
        logger: Logger,
        operation: BusinessOperation,
        message: String,
        userId: String? = null,
        entityId: String? = null,
        additionalContext: Map<String, Any> = emptyMap()
    ) {
        val businessContext = mutableMapOf<String, Any>(
            "operation" to operation.name,
            "operationType" to "BUSINESS"
        )

        userId?.let { businessContext["userId"] = it }
        entityId?.let { businessContext["entityId"] = it }
        businessContext.putAll(additionalContext)

        val event = LogEvent(
            type = "BUSINESS_OPERATION",
            category = "BUSINESS",
            action = operation.name,
            resource = operation.prefix
        )

        info(logger, message, event, businessContext)
    }

    /**
     * Log de auditoría de seguridad
     */
    fun security(
        logger: Logger,
        action: String,
        userId: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        success: Boolean = true,
        additionalContext: Map<String, Any> = emptyMap()
    ) {
        val securityContext = mutableMapOf<String, Any>(
            "action" to action,
            "success" to success,
            "category" to "SECURITY"
        )

        userId?.let { securityContext["userId"] = it }
        ipAddress?.let { securityContext["ipAddress"] = it }
        userAgent?.let { securityContext["userAgent"] = it }
        securityContext.putAll(additionalContext)

        val event = LogEvent(
            type = "SECURITY_EVENT",
            category = "SECURITY",
            action = action,
            resource = "AUTH"
        )

        val level = if (success) LogLevel.INFO else LogLevel.WARN
        val message = if (success) "Security action completed successfully" else "Security action failed"

        logStructured(logger, level, message, event, securityContext)
    }

    /**
     * Log de performance
     */
    fun performance(
        logger: Logger,
        operation: String,
        durationMs: Long,
        success: Boolean = true,
        additionalContext: Map<String, Any> = emptyMap()
    ) {
        val performanceContext = mutableMapOf<String, Any>(
            "operation" to operation,
            "durationMs" to durationMs,
            "success" to success,
            "category" to "PERFORMANCE"
        )
        performanceContext.putAll(additionalContext)

        val event = LogEvent(
            type = "PERFORMANCE_METRIC",
            category = "PERFORMANCE",
            action = operation,
            resource = "TIMING"
        )

        val level = if (success) LogLevel.INFO else LogLevel.WARN
        val message = "Operation completed in ${durationMs}ms"

        logStructured(logger, level, message, event, performanceContext)
    }

    private fun logStructured(
        logger: Logger,
        level: LogLevel,
        message: String,
        event: LogEvent?,
        context: Map<String, Any>,
        throwable: Throwable? = null
    ) {
        val logEntry = createLogEntry(message, event, context, throwable)
        val jsonLog = objectMapper.writeValueAsString(logEntry)

        when (level) {
            LogLevel.DEBUG -> logger.debug(jsonLog, throwable)
            LogLevel.INFO -> logger.info(jsonLog, throwable)
            LogLevel.WARN -> logger.warn(jsonLog, throwable)
            LogLevel.ERROR -> logger.error(jsonLog, throwable)
        }
    }

    private fun createLogEntry(
        message: String,
        event: LogEvent?,
        context: Map<String, Any>,
        throwable: Throwable?
    ): Map<String, Any> {
        val logEntry = mutableMapOf<String, Any>(
            "timestamp" to Instant.now().toString(),
            "message" to message,
            "level" to getCurrentLogLevel(),
            "thread" to Thread.currentThread().name
        )

        // Agregar contexto MDC
        MDC.getCopyOfContextMap()?.let { mdcContext ->
            logEntry["mdc"] = mdcContext
        }

        // Agregar evento si existe
        event?.let { logEntry["event"] = it }

        // Agregar contexto adicional
        if (context.isNotEmpty()) {
            logEntry["context"] = context
        }

        // Agregar información de excepción si existe
        throwable?.let {
            logEntry["exception"] = mapOf(
                "class" to it.javaClass.simpleName,
                "message" to (it.message ?: "No message"),
                "stackTrace" to it.stackTrace.take(10).map { frame -> frame.toString() }
            )
        }

        return logEntry
    }

    private fun getCurrentLogLevel(): String {
        // Obtener el nivel actual del logger desde MDC o contexto
        return MDC.get("logLevel") ?: "INFO"
    }

    companion object {
        fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
        fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)
    }
}

/**
 * Niveles de log
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * Estructura de evento de log
 */
data class LogEvent(
    val type: String,
    val category: String,
    val action: String,
    val resource: String,
    val metadata: Map<String, Any> = emptyMap()
)