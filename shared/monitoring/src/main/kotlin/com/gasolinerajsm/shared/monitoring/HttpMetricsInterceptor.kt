package com.gasolinerajsm.shared.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTP Metrics Interceptor
 * Collects detailed HTTP metrics for all endpoints
 */
@Component
class HttpMetricsInterceptor(
    private val meterRegistry: MeterRegistry,
    private val metricsProperties: MetricsProperties
) : HandlerInterceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(HttpMetricsInterceptor::class.java)
        private const val START_TIME_ATTRIBUTE = "metrics.start.time"
        private const val ENDPOINT_ATTRIBUTE = "metrics.endpoint"
    }

    private val requestCounters = ConcurrentHashMap<String, Counter>()
    private val responseTimers = ConcurrentHashMap<String, Timer>()
    private val errorCounters = ConcurrentHashMap<String, Counter>()

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (!metricsProperties.enabled) {
            return true
        }

        val startTime = System.nanoTime()
        request.setAttribute(START_TIME_ATTRIBUTE, startTime)

        val endpoint = extractEndpoint(request, handler)
        request.setAttribute(ENDPOINT_ATTRIBUTE, endpoint)

        // Record request count
        recordRequestCount(request.method, endpoint, extractUserRole(request))

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        if (!metricsProperties.enabled) {
            return
        }

        val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long ?: return
        val endpoint = request.getAttribute(ENDPOINT_ATTRIBUTE) as? String ?: "unknown"

        val duration = Duration.ofNanos(System.nanoTime() - startTime)
        val statusCode = response.status
        val method = request.method
        val userRole = extractUserRole(request)

        // Record response time
        recordResponseTime(method, endpoint, statusCode, userRole, duration)

        // Record errors if any
        if (ex != null || statusCode >= 400) {
            recordError(method, endpoint, statusCode, ex?.javaClass?.simpleName ?: "HTTP_ERROR")
        }

        // Record specific business metrics based on endpoint
        recordBusinessSpecificMetrics(request, response, endpoint, duration)
    }

    /**
     * Extract endpoint pattern from request
     */
    private fun extractEndpoint(request: HttpServletRequest, handler: Any): String {
        return when (handler) {
            is HandlerMethod -> {
                val controllerName = handler.beanType.simpleName.replace("Controller", "")
                val methodName = handler.method.name
                "$controllerName.$methodName"
            }
            else -> {
                val uri = request.requestURI
                // Normalize URI by replacing IDs with placeholders
                normalizeUri(uri)
            }
        }
    }

    /**
     * Normalize URI by replacing dynamic segments with placeholders
     */
    private fun normalizeUri(uri: String): String {
        return uri
            .replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[a-f0-9-]{36}"), "/{uuid}")
            .replace(Regex("/[a-zA-Z0-9-]{8,}"), "/{param}")
    }

    /**
     * Extract user role from request
     */
    private fun extractUserRole(request: HttpServletRequest): String {
        // Try to get from security context or JWT token
        return try {
            val principal = request.userPrincipal
            if (principal != null) {
                // Extract role from principal
                extractRoleFromPrincipal(principal)
            } else {
                "anonymous"
            }
        } catch (ex: Exception) {
            "unknown"
        }
    }

    private fun extractRoleFromPrincipal(principal: java.security.Principal): String {
        // This would integrate with your security system
        return when {
            principal.name.contains("admin") -> "admin"
            principal.name.contains("manager") -> "manager"
            principal.name.contains("employee") -> "employee"
            principal.name.contains("customer") -> "customer"
            else -> "user"
        }
    }

    /**
     * Record request count metrics
     */
    private fun recordRequestCount(method: String, endpoint: String, userRole: String) {
        val key = "requests:$method:$endpoint:$userRole"
        val counter = requestCounters.computeIfAbsent(key) {
            Counter.builder("http.requests.total")
                .description("Total HTTP requests")
                .tags(
                    "method", method,
                    "endpoint", endpoint,
                    "role", userRole
                )
                .register(meterRegistry)
        }
        counter.increment()
    }

    /**
     * Record response time metrics
     */
    private fun recordResponseTime(
        method: String,
        endpoint: String,
        statusCode: Int,
        userRole: String,
        duration: Duration
    ) {
        val statusClass = "${statusCode / 100}xx"
        val key = "response_time:$method:$endpoint:$statusClass:$userRole"

        val timer = responseTimers.computeIfAbsent(key) {
            Timer.builder("http.requests.duration")
                .description("HTTP request duration")
                .tags(
                    "method", method,
                    "endpoint", endpoint,
                    "status_class", statusClass,
                    "role", userRole
                )
                .register(meterRegistry)
        }
        timer.record(duration)
    }

    /**
     * Record error metrics
     */
    private fun recordError(method: String, endpoint: String, statusCode: Int, errorType: String) {
        val key = "errors:$method:$endpoint:$statusCode:$errorType"
        val counter = errorCounters.computeIfAbsent(key) {
            Counter.builder("http.requests.errors.total")
                .description("Total HTTP request errors")
                .tags(
                    "method", method,
                    "endpoint", endpoint,
                    "status_code", statusCode.toString(),
                    "error_type", errorType
                )
                .register(meterRegistry)
        }
        counter.increment()
    }

    /**
     * Record business-specific metrics based on endpoint
     */
    private fun recordBusinessSpecificMetrics(
        request: HttpServletRequest,
        response: HttpServletResponse,
        endpoint: String,
        duration: Duration
    ) {
        val uri = request.requestURI
        val method = request.method

        when {
            // Authentication metrics
            uri.contains("/auth/login") && method == "POST" -> {
                recordAuthenticationMetric(response.status == 200, duration)
            }

            // Coupon metrics
            uri.contains("/coupons") && method == "POST" -> {
                recordCouponOperationMetric("create", response.status == 201, duration)
            }
            uri.contains("/coupons") && uri.contains("/use") && method == "POST" -> {
                recordCouponOperationMetric("use", response.status == 200, duration)
            }

            // Station metrics
            uri.contains("/stations") && method == "POST" -> {
                recordStationOperationMetric("create", response.status == 201, duration)
            }
            uri.contains("/stations") && uri.contains("/fuel-prices") && method == "PUT" -> {
                recordStationOperationMetric("price_update", response.status == 200, duration)
            }

            // Raffle metrics
            uri.contains("/raffles") && uri.contains("/participate") && method == "POST" -> {
                recordRaffleOperationMetric("participate", response.status == 200, duration)
            }
            uri.contains("/raffles") && uri.contains("/draw") && method == "POST" -> {
                recordRaffleOperationMetric("draw", response.status == 200, duration)
            }

            // Campaign metrics
            uri.contains("/campaigns") && method == "POST" -> {
                recordCampaignOperationMetric("create", response.status == 201, duration)
            }
        }
    }

    private fun recordAuthenticationMetric(success: Boolean, duration: Duration) {
        val status = if (success) "success" else "failure"
        Counter.builder("auth.attempts.total")
            .description("Authentication attempts")
            .tags("status", status)
            .register(meterRegistry)
            .increment()

        Timer.builder("auth.duration")
            .description("Authentication duration")
            .tags("status", status)
            .register(meterRegistry)
            .record(duration)
    }

    private fun recordCouponOperationMetric(operation: String, success: Boolean, duration: Duration) {
        val status = if (success) "success" else "failure"
        Counter.builder("coupon.operations.total")
            .description("Coupon operations")
            .tags("operation", operation, "status", status)
            .register(meterRegistry)
            .increment()

        Timer.builder("coupon.operations.duration")
            .description("Coupon operation duration")
            .tags("operation", operation)
            .register(meterRegistry)
            .record(duration)
    }

    private fun recordStationOperationMetric(operation: String, success: Boolean, duration: Duration) {
        val status = if (success) "success" else "failure"
        Counter.builder("station.operations.total")
            .description("Station operations")
            .tags("operation", operation, "status", status)
            .register(meterRegistry)
            .increment()

        Timer.builder("station.operations.duration")
            .description("Station operation duration")
            .tags("operation", operation)
            .register(meterRegistry)
            .record(duration)
    }

    private fun recordRaffleOperationMetric(operation: String, success: Boolean, duration: Duration) {
        val status = if (success) "success" else "failure"
        Counter.builder("raffle.operations.total")
            .description("Raffle operations")
            .tags("operation", operation, "status", status)
            .register(meterRegistry)
            .increment()

        Timer.builder("raffle.operations.duration")
            .description("Raffle operation duration")
            .tags("operation", operation)
            .register(meterRegistry)
            .record(duration)
    }

    private fun recordCampaignOperationMetric(operation: String, success: Boolean, duration: Duration) {
        val status = if (success) "success" else "failure"
        Counter.builder("campaign.operations.total")
            .description("Campaign operations")
            .tags("operation", operation, "status", status)
            .register(meterRegistry)
            .increment()

        Timer.builder("campaign.operations.duration")
            .description("Campaign operation duration")
            .tags("operation", operation)
            .register(meterRegistry)
            .record(duration)
    }

    /**
     * Get metrics summary
     */
    fun getMetricsSummary(): Map<String, Any> {
        return mapOf(
            "request_counters" to requestCounters.size,
            "response_timers" to responseTimers.size,
            "error_counters" to errorCounters.size,
            "total_http_metrics" to (requestCounters.size + responseTimers.size + errorCounters.size)
        )
    }
}