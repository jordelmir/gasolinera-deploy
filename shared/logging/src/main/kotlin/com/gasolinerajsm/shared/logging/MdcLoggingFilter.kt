package com.gasolinerajsm.shared.logging

import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Filtro que configura el MDC (Mapped Diagnostic Context) para cada request
 * Permite agregar contexto automático a todos los logs
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcLoggingFilter : OncePerRequestFilter() {

    companion object {
        const val CORRELATION_ID_KEY = "correlationId"
        const val USER_ID_KEY = "userId"
        const val SESSION_ID_KEY = "sessionId"
        const val REQUEST_ID_KEY = "requestId"
        const val IP_ADDRESS_KEY = "ipAddress"
        const val USER_AGENT_KEY = "userAgent"
        const val REQUEST_URI_KEY = "requestUri"
        const val HTTP_METHOD_KEY = "httpMethod"
        const val TRACE_ID_KEY = "traceId"
        const val SPAN_ID_KEY = "spanId"
        const val SERVICE_NAME_KEY = "serviceName"

        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val USER_ID_HEADER = "X-User-ID"
        const val SESSION_ID_HEADER = "X-Session-ID"
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val SPAN_ID_HEADER = "X-Span-Id"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            setupMdc(request, response)
            filterChain.doFilter(request, response)
        } finally {
            clearMdc()
        }
    }

    private fun setupMdc(request: HttpServletRequest, response: HttpServletResponse) {
        // Correlation ID - generar si no existe
        val correlationId = request.getHeader(CORRELATION_ID_HEADER)
            ?: generateCorrelationId()
        MDC.put(CORRELATION_ID_KEY, correlationId)
        response.setHeader(CORRELATION_ID_HEADER, correlationId)

        // Request ID único para este request específico
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        MDC.put(REQUEST_ID_KEY, requestId)

        // User ID desde header de autenticación
        request.getHeader(USER_ID_HEADER)?.let { userId ->
            MDC.put(USER_ID_KEY, userId)
        }

        // Session ID
        request.getHeader(SESSION_ID_HEADER)?.let { sessionId ->
            MDC.put(SESSION_ID_KEY, sessionId)
        } ?: run {
            request.session?.id?.let { sessionId ->
                MDC.put(SESSION_ID_KEY, sessionId)
            }
        }

        // Información de tracing distribuido
        request.getHeader(TRACE_ID_HEADER)?.let { traceId ->
            MDC.put(TRACE_ID_KEY, traceId)
        }

        request.getHeader(SPAN_ID_HEADER)?.let { spanId ->
            MDC.put(SPAN_ID_KEY, spanId)
        }

        // Información de request HTTP
        MDC.put(REQUEST_URI_KEY, request.requestURI)
        MDC.put(HTTP_METHOD_KEY, request.method)

        // Información de cliente
        MDC.put(IP_ADDRESS_KEY, getClientIpAddress(request))
        request.getHeader("User-Agent")?.let { userAgent ->
            MDC.put(USER_AGENT_KEY, userAgent.take(100)) // Limitar longitud
        }

        // Nombre del servicio desde propiedades de aplicación
        getServiceName()?.let { serviceName ->
            MDC.put(SERVICE_NAME_KEY, serviceName)
        }
    }

    private fun clearMdc() {
        MDC.clear()
    }

    private fun generateCorrelationId(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }

    private fun getServiceName(): String? {
        return System.getProperty("spring.application.name")
            ?: System.getenv("SERVICE_NAME")
            ?: "gasolinera-service"
    }
}

/**
 * Utilidades para trabajar con MDC en el código de aplicación
 */
object MdcUtils {

    /**
     * Ejecuta un bloque de código con contexto MDC adicional
     */
    fun <T> withMdcContext(context: Map<String, String>, block: () -> T): T {
        val originalContext = MDC.getCopyOfContextMap() ?: emptyMap()

        try {
            context.forEach { (key, value) ->
                MDC.put(key, value)
            }
            return block()
        } finally {
            MDC.clear()
            originalContext.forEach { (key, value) ->
                MDC.put(key, value)
            }
        }
    }

    /**
     * Agrega contexto de usuario al MDC
     */
    fun setUserContext(userId: String, sessionId: String? = null) {
        MDC.put(MdcLoggingFilter.USER_ID_KEY, userId)
        sessionId?.let { MDC.put(MdcLoggingFilter.SESSION_ID_KEY, it) }
    }

    /**
     * Agrega contexto de operación de negocio al MDC
     */
    fun setBusinessContext(operation: BusinessOperation, entityId: String? = null) {
        MDC.put("businessOperation", operation.name)
        entityId?.let { MDC.put("entityId", it) }
    }

    /**
     * Obtiene el correlation ID actual del MDC
     */
    fun getCurrentCorrelationId(): String? {
        return MDC.get(MdcLoggingFilter.CORRELATION_ID_KEY)
    }

    /**
     * Obtiene el user ID actual del MDC
     */
    fun getCurrentUserId(): String? {
        return MDC.get(MdcLoggingFilter.USER_ID_KEY)
    }
}