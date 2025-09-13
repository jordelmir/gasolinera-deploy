package com.gasolinerajsm.shared.logging

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Interceptor que registra información detallada de requests HTTP
 */
@Component
class LoggingInterceptor(
    private val correlationIdGenerator: CorrelationIdGenerator,
    private val structuredLogger: StructuredLogger
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    companion object {
        private const val START_TIME_ATTRIBUTE = "startTime"
        private const val CORRELATION_ID_ATTRIBUTE = "correlationId"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val startTime = System.currentTimeMillis()
        request.setAttribute(START_TIME_ATTRIBUTE, startTime)

        // Obtener o generar correlation ID
        val correlationId = request.getHeader("X-Correlation-ID")
            ?: correlationIdGenerator.generate()
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId)

        // Log de inicio de request
        val requestContext = mapOf(
            "method" to request.method,
            "uri" to request.requestURI,
            "queryString" to (request.queryString ?: ""),
            "remoteAddr" to request.remoteAddr,
            "userAgent" to (request.getHeader("User-Agent") ?: ""),
            "contentType" to (request.contentType ?: ""),
            "contentLength" to request.contentLength,
            "correlationId" to correlationId
        )

        val event = LogEvent(
            type = "HTTP_REQUEST_START",
            category = "HTTP",
            action = "REQUEST_START",
            resource = request.requestURI
        )

        structuredLogger.info(
            logger,
            "HTTP request started: ${request.method} ${request.requestURI}",
            event,
            requestContext
        )

        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        // Este método se ejecuta después del handler pero antes de la vista
        // Útil para logging de información adicional si es necesario
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long ?: return
        val correlationId = request.getAttribute(CORRELATION_ID_ATTRIBUTE) as? String
        val duration = System.currentTimeMillis() - startTime

        // Determinar el nivel de log basado en el status code y excepción
        val isError = ex != null || response.status >= 400
        val isWarning = response.status >= 300 && response.status < 400

        val responseContext = mapOf(
            "method" to request.method,
            "uri" to request.requestURI,
            "statusCode" to response.status,
            "durationMs" to duration,
            "correlationId" to (correlationId ?: "unknown"),
            "contentType" to (response.contentType ?: ""),
            "success" to !isError
        )

        val event = LogEvent(
            type = "HTTP_REQUEST_COMPLETE",
            category = "HTTP",
            action = "REQUEST_COMPLETE",
            resource = request.requestURI,
            metadata = mapOf(
                "statusCode" to response.status,
                "durationMs" to duration
            )
        )

        val message = "HTTP request completed: ${request.method} ${request.requestURI} " +
                "- Status: ${response.status}, Duration: ${duration}ms"

        when {
            isError -> {
                structuredLogger.error(
                    logger,
                    message,
                    event,
                    responseContext,
                    ex
                )
            }
            isWarning -> {
                structuredLogger.warn(
                    logger,
                    message,
                    event,
                    responseContext
                )
            }
            else -> {
                structuredLogger.info(
                    logger,
                    message,
                    event,
                    responseContext
                )
            }
        }

        // Log de performance si la request fue lenta
        if (duration > 1000) { // Más de 1 segundo
            structuredLogger.performance(
                logger,
                "HTTP_REQUEST",
                duration,
                !isError,
                mapOf(
                    "endpoint" to "${request.method} ${request.requestURI}",
                    "statusCode" to response.status,
                    "threshold" to "SLOW_REQUEST"
                )
            )
        }

        // Log de seguridad para requests de autenticación
        if (isAuthenticationEndpoint(request.requestURI)) {
            structuredLogger.security(
                logger,
                "AUTHENTICATION_ATTEMPT",
                extractUserIdFromRequest(request),
                request.remoteAddr,
                request.getHeader("User-Agent"),
                !isError,
                mapOf(
                    "endpoint" to request.requestURI,
                    "statusCode" to response.status
                )
            )
        }
    }

    private fun isAuthenticationEndpoint(uri: String): Boolean {
        val authEndpoints = listOf(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/logout"
        )
        return authEndpoints.any { uri.contains(it) }
    }

    private fun extractUserIdFromRequest(request: HttpServletRequest): String? {
        // Intentar extraer user ID de headers o parámetros
        return request.getHeader("X-User-ID")
            ?: request.getParameter("userId")
            ?: request.getParameter("username")
    }
}

/**
 * Anotación para marcar métodos que requieren logging de negocio automático
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogBusinessOperation(
    val operation: BusinessOperation,
    val includeParameters: Boolean = false,
    val includeResult: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO
)

/**
 * Anotación para marcar métodos que requieren logging de performance
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogPerformance(
    val operation: String = "",
    val threshold: Long = 1000, // milliseconds
    val includeParameters: Boolean = false
)