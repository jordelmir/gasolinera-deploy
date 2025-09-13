package com.gasolinerajsm.authservice.infrastructure.web

import com.gasolinerajsm.shared.logging.*
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

/**
 * Controlador de ejemplo que demuestra el uso del sistema de logging estructurado
 */
@RestController
@RequestMapping("/api/logging-examples")
class LoggingExampleController(
    private val structuredLogger: StructuredLogger
) {

    private val logger = LoggerFactory.getLogger(LoggingExampleController::class.java)

    /**
     * Ejemplo de logging básico con contexto
     */
    @GetMapping("/basic")
    fun basicLoggingExample(@RequestParam userId: String?): Map<String, Any> {
        structuredLogger.info(
            logger,
            "Basic logging example executed",
            context = mapOf(
                "endpoint" to "/basic",
                "hasUserId" to (userId != null)
            )
        )

        return mapOf(
            "message" to "Basic logging example",
            "timestamp" to System.currentTimeMillis(),
            "userId" to userId
        )
    }

    /**
     * Ejemplo de logging de negocio con anotación
     */
    @PostMapping("/business")
    @LogBusinessOperation(
        operation = BusinessOperation.USER_AUTHENTICATION,
        includeParameters = true,
        includeResult = true
    )
    fun businessLoggingExample(@RequestBody request: Map<String, Any>): Map<String, Any> {
        // Simular operación de negocio
        Thread.sleep(100)

        val result = mapOf(
            "operationId" to "op-${System.currentTimeMillis()}",
            "success" to true,
            "data" to request
        )

        // Log manual adicional
        structuredLogger.business(
            logger,
            BusinessOperation.USER_AUTHENTICATION,
            "Business operation completed with custom logging",
            userId = request["userId"]?.toString(),
            entityId = result["operationId"]?.toString(),
            additionalContext = mapOf(
                "requestSize" to request.size,
                "processingTime" to "100ms"
            )
        )

        return result
    }

    /**
     * Ejemplo de logging de performance con anotación
     */
    @GetMapping("/performance")
    @LogPerformance(operation = "PERFORMANCE_TEST", threshold = 500, includeParameters = true)
    fun performanceLoggingExample(@RequestParam delay: Long = 200): Map<String, Any> {
        val startTime = System.currentTimeMillis()

        // Simular operación que puede ser lenta
        Thread.sleep(delay)

        val duration = System.currentTimeMillis() - startTime

        // Log manual de performance
        structuredLogger.performance(
            logger,
            "CUSTOM_PERFORMANCE_OPERATION",
            duration,
            true,
            mapOf(
                "requestedDelay" to delay,
                "actualDuration" to duration,
                "efficiency" to if (duration <= delay + 50) "good" else "poor"
            )
        )

        return mapOf(
            "requestedDelay" to delay,
            "actualDuration" to duration,
            "timestamp" to System.currentTimeMillis()
        )
    }

    /**
     * Ejemplo de logging de seguridad
     */
    @PostMapping("/security")
    fun securityLoggingExample(
        @RequestBody credentials: Map<String, String>,
        request: HttpServletRequest
    ): Map<String, Any> {
        val username = credentials["username"]
        val success = credentials["password"] == "correct"

        structuredLogger.security(
            logger,
            "LOGIN_ATTEMPT_EXAMPLE",
            userId = username,
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent"),
            success = success,
            additionalContext = mapOf(
                "loginMethod" to "PASSWORD",
                "endpoint" to "/security",
                "attemptTime" to System.currentTimeMillis()
            )
        )

        return if (success) {
            mapOf(
                "success" to true,
                "message" to "Login successful",
                "userId" to username
            )
        } else {
            mapOf(
                "success" to false,
                "message" to "Invalid credentials"
            )
        }
    }

    /**
     * Ejemplo de logging con contexto MDC
     */
    @GetMapping("/mdc-context")
    fun mdcContextExample(@RequestParam operationType: String): Map<String, Any> {
        return MdcUtils.withMdcContext(
            mapOf(
                "businessOperation" to operationType,
                "customContext" to "example-value"
            )
        ) {
            structuredLogger.info(
                logger,
                "Operation executed with MDC context"
            )

            // Todos los logs dentro de este bloque tendrán el contexto MDC
            structuredLogger.debug(
                logger,
                "Debug message with automatic MDC context"
            )

            mapOf(
                "operationType" to operationType,
                "correlationId" to MdcUtils.getCurrentCorrelationId(),
                "userId" to MdcUtils.getCurrentUserId(),
                "message" to "Operation completed with MDC context"
            )
        }
    }

    /**
     * Ejemplo de logging de errores
     */
    @GetMapping("/error")
    fun errorLoggingExample(@RequestParam simulateError: Boolean = true): Map<String, Any> {
        return try {
            if (simulateError) {
                throw RuntimeException("Simulated error for logging demonstration")
            }

            structuredLogger.info(
                logger,
                "No error occurred",
                context = mapOf("simulateError" to false)
            )

            mapOf("success" to true, "message" to "No error")
        } catch (e: Exception) {
            structuredLogger.error(
                logger,
                "Error occurred in example endpoint",
                LogEvent(
                    type = "APPLICATION_ERROR",
                    category = "ERROR",
                    action = "ERROR_SIMULATION",
                    resource = "/error"
                ),
                mapOf(
                    "simulateError" to simulateError,
                    "errorType" to e.javaClass.simpleName
                ),
                e
            )

            mapOf(
                "success" to false,
                "error" to e.message,
                "type" to e.javaClass.simpleName
            )
        }
    }

    /**
     * Ejemplo de logging con diferentes niveles
     */
    @GetMapping("/levels")
    fun loggingLevelsExample(): Map<String, Any> {
        val context = mapOf(
            "endpoint" to "/levels",
            "timestamp" to System.currentTimeMillis()
        )

        structuredLogger.debug(
            logger,
            "Debug level message",
            context = context
        )

        structuredLogger.info(
            logger,
            "Info level message",
            context = context
        )

        structuredLogger.warn(
            logger,
            "Warning level message",
            context = context
        )

        // No ejecutamos error para no generar ruido en los logs

        return mapOf(
            "message" to "Different log levels demonstrated",
            "levels" to listOf("DEBUG", "INFO", "WARN", "ERROR")
        )
    }

    /**
     * Ejemplo de logging con correlation ID personalizado
     */
    @GetMapping("/correlation")
    fun correlationIdExample(
        @RequestParam customCorrelationId: String?,
        correlationIdGenerator: CorrelationIdGenerator
    ): Map<String, Any> {
        val correlationId = customCorrelationId
            ?: correlationIdGenerator.generateForBusinessOperation(BusinessOperation.USER_REGISTRATION)

        structuredLogger.info(
            logger,
            "Using custom correlation ID",
            context = mapOf(
                "customCorrelationId" to (customCorrelationId != null),
                "correlationId" to correlationId,
                "isValid" to correlationIdGenerator.isValid(correlationId)
            )
        )

        return mapOf(
            "correlationId" to correlationId,
            "isCustom" to (customCorrelationId != null),
            "isValid" to correlationIdGenerator.isValid(correlationId),
            "baseId" to correlationIdGenerator.extractBaseId(correlationId)
        )
    }
}