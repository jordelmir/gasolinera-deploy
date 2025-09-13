package com.gasolinerajsm.shared.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.semconv.SemanticAttributes
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.util.*

/**
 * Tracing Interceptor for HTTP requests
 * Creates spans for incoming HTTP requests and propagates trace context
 */
@Component
class TracingInterceptor(
    private val openTelemetry: OpenTelemetry,
    private val tracingProperties: TracingProperties
) : HandlerInterceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(TracingInterceptor::class.java)
        private const val SPAN_ATTRIBUTE = "tracing.span"
        private const val SCOPE_ATTRIBUTE = "tracing.scope"
        private const val START_TIME_ATTRIBUTE = "tracing.start.time"

        // Custom attribute keys
        private val USER_ID_KEY = AttributeKey.stringKey("user.id")
        private val USER_ROLE_KEY = AttributeKey.stringKey("user.role")
        private val STATION_ID_KEY = AttributeKey.stringKey("station.id")
        private val CORRELATION_ID_KEY = AttributeKey.stringKey("correlation.id")
        private val SESSION_ID_KEY = AttributeKey.stringKey("session.id")
        private val DEVICE_ID_KEY = AttributeKey.stringKey("device.id")
        private val BUSINESS_OPERATION_KEY = AttributeKey.stringKey("business.operation")
        private val ENDPOINT_KEY = AttributeKey.stringKey("endpoint.name")
    }

    private val tracer = openTelemetry.getTracer("gasolinera-jsm-http-interceptor")

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (!tracingProperties.enabled) {
            return true
        }

        val startTime = System.nanoTime()
        request.setAttribute(START_TIME_ATTRIBUTE, startTime)

        // Extract or generate correlation ID
        val correlationId = extractOrGenerateCorrelationId(request)

        // Create span for the request
        val spanBuilder = tracer.spanBuilder(getOperationName(request, handler))
            .setSpanKind(SpanKind.SERVER)
            .setAttribute(SemanticAttributes.HTTP_METHOD, request.method)
            .setAttribute(SemanticAttributes.HTTP_URL, request.requestURL.toString())
            .setAttribute(SemanticAttributes.HTTP_SCHEME, request.scheme)
            .setAttribute(SemanticAttributes.HTTP_HOST, request.serverName)
            .setAttribute(SemanticAttributes.HTTP_TARGET, request.requestURI)
            .setAttribute(SemanticAttributes.USER_AGENT_ORIGINAL, request.getHeader("User-Agent") ?: "")
            .setAttribute(CORRELATION_ID_KEY, correlationId)

        // Add custom attributes
        addCustomAttributes(spanBuilder, request, handler)

        val span = spanBuilder.startSpan()
        val scope = span.makeCurrent()

        // Store span and scope for cleanup
        request.setAttribute(SPAN_ATTRIBUTE, span)
        request.setAttribute(SCOPE_ATTRIBUTE, scope)

        // Set MDC for logging
        setMDCContext(span, correlationId, request)

        // Set response headers for tracing
        setTracingHeaders(response, span, correlationId)

        logger.debug("Started span: {} for request: {} {}",
            span.getSpanContext().getSpanId(), request.method, request.requestURI)

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        if (!tracingProperties.enabled) {
            return
        }

        val span = request.getAttribute(SPAN_ATTRIBUTE) as? Span
        val scope = request.getAttribute(SCOPE_ATTRIBUTE) as? Scope
        val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long

        if (span != null && scope != null) {
            try {
                // Add response attributes
                span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, response.status.toLong())

                // Calculate duration
                if (startTime != null) {
                    val duration = System.nanoTime() - startTime
                    span.setAttribute(SemanticAttributes.HTTP_REQUEST_DURATION, duration / 1_000_000.0) // Convert to milliseconds
                }

                // Set span status based on response
                when {
                    ex != null -> {
                        span.setStatus(StatusCode.ERROR, ex.message ?: "Unknown error")
                        span.recordException(ex)
                    }
                    response.status >= 400 -> {
                        span.setStatus(StatusCode.ERROR, "HTTP ${response.status}")
                    }
                    else -> {
                        span.setStatus(StatusCode.OK)
                    }
                }

                // Add business context if available
                addBusinessContext(span, request, response, handler)

                logger.debug("Completed span: {} with status: {}",
                    span.getSpanContext().getSpanId(), response.status)

            } finally {
                span.end()
                scope.close()
                MDC.clear()
            }
        }
    }

    /**
     * Extract or generate correlation ID
     */
    private fun extractOrGenerateCorrelationId(request: HttpServletRequest): String {
        return request.getHeader("X-Correlation-ID")
            ?: request.getHeader("X-Request-ID")
            ?: request.getHeader("X-Trace-ID")
            ?: UUID.randomUUID().toString()
    }

    /**
     * Get operation name for the span
     */
    private fun getOperationName(request: HttpServletRequest, handler: Any): String {
        return when (handler) {
            is HandlerMethod -> {
                val controllerName = handler.beanType.simpleName.replace("Controller", "")
                val methodName = handler.method.name
                "${request.method} /$controllerName/$methodName"
            }
            else -> {
                "${request.method} ${normalizeUri(request.requestURI)}"
            }
        }
    }

    /**
     * Normalize URI by replacing dynamic segments
     */
    private fun normalizeUri(uri: String): String {
        return uri
            .replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[a-f0-9-]{36}"), "/{uuid}")
            .replace(Regex("/[a-zA-Z0-9-]{8,}"), "/{param}")
    }

    /**
     * Add custom attributes to span
     */
    private fun addCustomAttributes(
        spanBuilder: io.opentelemetry.api.trace.SpanBuilder,
        request: HttpServletRequest,
        handler: Any
    ) {
        // Add endpoint information
        if (handler is HandlerMethod) {
            val controllerName = handler.beanType.simpleName
            val methodName = handler.method.name
            spanBuilder.setAttribute(ENDPOINT_KEY, "$controllerName.$methodName")
        }

        // Add user context if available
        extractUserContext(request)?.let { userContext ->
            spanBuilder.setAttribute(USER_ID_KEY, userContext.userId)
            spanBuilder.setAttribute(USER_ROLE_KEY, userContext.role)
            userContext.stationId?.let {
                spanBuilder.setAttribute(STATION_ID_KEY, it)
            }
        }

        // Add session and device information
        request.getHeader("X-Session-ID")?.let {
            spanBuilder.setAttribute(SESSION_ID_KEY, it)
        }

        request.getHeader("X-Device-ID")?.let {
            spanBuilder.setAttribute(DEVICE_ID_KEY, it)
        }

        // Add business operation context
        extractBusinessOperation(request)?.let {
            spanBuilder.setAttribute(BUSINESS_OPERATION_KEY, it)
        }
    }

    /**
     * Extract user context from request
     */
    private fun extractUserContext(request: HttpServletRequest): UserContext? {
        return try {
            // This would integrate with your security context
            val principal = request.userPrincipal
            if (principal != null) {
                UserContext(
                    userId = extractUserIdFromPrincipal(principal),
                    role = extractRoleFromPrincipal(principal),
                    stationId = extractStationIdFromPrincipal(principal)
                )
            } else null
        } catch (ex: Exception) {
            logger.debug("Could not extract user context: ${ex.message}")
            null
        }
    }

    /**
     * Extract business operation from request
     */
    private fun extractBusinessOperation(request: HttpServletRequest): String? {
        val uri = request.requestURI
        val method = request.method

        return when {
            uri.contains("/coupons") && method == "POST" -> "coupon.create"
            uri.contains("/coupons") && uri.contains("/use") -> "coupon.use"
            uri.contains("/coupons") && uri.contains("/validate") -> "coupon.validate"
            uri.contains("/raffles") && uri.contains("/participate") -> "raffle.participate"
            uri.contains("/raffles") && uri.contains("/draw") -> "raffle.draw"
            uri.contains("/stations") && uri.contains("/fuel-prices") -> "station.price.update"
            uri.contains("/auth/login") -> "user.login"
            uri.contains("/auth/register") -> "user.register"
            uri.contains("/redemptions") && method == "POST" -> "redemption.process"
            else -> null
        }
    }

    /**
     * Set MDC context for logging
     */
    private fun setMDCContext(span: Span, correlationId: String, request: HttpServletRequest) {
        val spanContext = span.getSpanContext()
        MDC.put("traceId", spanContext.getTraceId())
        MDC.put("spanId", spanContext.getSpanId())
        MDC.put("correlationId", correlationId)
        MDC.put("operation", getOperationName(request, ""))

        // Add user context to MDC
        extractUserContext(request)?.let { userContext ->
            MDC.put("userId", userContext.userId)
            MDC.put("userRole", userContext.role)
            userContext.stationId?.let { MDC.put("stationId", it) }
        }
    }

    /**
     * Set tracing headers in response
     */
    private fun setTracingHeaders(response: HttpServletResponse, span: Span, correlationId: String) {
        val spanContext = span.getSpanContext()
        response.setHeader("X-Trace-ID", spanContext.getTraceId())
        response.setHeader("X-Span-ID", spanContext.getSpanId())
        response.setHeader("X-Correlation-ID", correlationId)
    }

    /**
     * Add business context to span after completion
     */
    private fun addBusinessContext(
        span: Span,
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ) {
        // Add response size if available
        response.getHeader("Content-Length")?.let { contentLength ->
            span.setAttribute(SemanticAttributes.HTTP_RESPONSE_SIZE, contentLength.toLong())
        }

        // Add business metrics based on operation
        val businessOperation = extractBusinessOperation(request)
        if (businessOperation != null) {
            span.setAttribute("business.operation.type", businessOperation)

            // Add operation-specific attributes
            when (businessOperation) {
                "coupon.use" -> {
                    if (response.status == 200) {
                        span.setAttribute("coupon.usage.success", true)
                    }
                }
                "raffle.participate" -> {
                    if (response.status == 200) {
                        span.setAttribute("raffle.participation.success", true)
                    }
                }
                "user.login" -> {
                    span.setAttribute("auth.login.success", response.status == 200)
                }
            }
        }
    }

    // Helper methods for user context extraction
    private fun extractUserIdFromPrincipal(principal: java.security.Principal): String {
        // Implement based on your security setup
        return principal.name
    }

    private fun extractRoleFromPrincipal(principal: java.security.Principal): String {
        // Implement based on your security setup
        return "user" // Default role
    }

    private fun extractStationIdFromPrincipal(principal: java.security.Principal): String? {
        // Implement based on your security setup
        return null
    }

    data class UserContext(
        val userId: String,
        val role: String,
        val stationId: String? = null
    )
}