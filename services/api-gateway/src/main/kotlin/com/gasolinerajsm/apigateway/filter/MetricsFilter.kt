package com.gasolinerajsm.apigateway.filter

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Metrics collection filter for API Gateway
 * Collects metrics for monitoring and observability
 */
@Component
class MetricsFilter(
    private val meterRegistry: MeterRegistry
) : GlobalFilter, Ordered {

    private val requestCounter: Counter = Counter.builder("gateway.requests.total")
        .description("Total number of requests processed by the gateway")
        .register(meterRegistry)

    private val requestTimer: Timer = Timer.builder("gateway.request.duration")
        .description("Request processing time")
        .register(meterRegistry)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()
        val method = request.method.name()
        val userId = request.headers.getFirst("X-User-ID") ?: "anonymous"

        val timer = Timer.Sample.start(meterRegistry)

        // Increment request counter
        requestCounter.increment(
            "method", method,
            "path", sanitizePath(path),
            "user_type", if (userId == "anonymous") "anonymous" else "authenticated"
        )

        return chain.filter(exchange)
            .doOnSuccess {
                val statusCode = exchange.response.statusCode?.value()?.toString() ?: "unknown"
                recordMetrics(timer, method, path, statusCode, userId, "success")
            }
            .doOnError { error ->
                recordMetrics(timer, method, path, "error", userId, "error")

                // Record error metrics
                Counter.builder("gateway.errors.total")
                    .description("Total number of errors in the gateway")
                    .tag("method", method)
                    .tag("path", sanitizePath(path))
                    .tag("error_type", error.javaClass.simpleName)
                    .register(meterRegistry)
                    .increment()
            }
    }

    /**
     * Record metrics for completed requests
     */
    private fun recordMetrics(
        timer: Timer.Sample,
        method: String,
        path: String,
        statusCode: String,
        userId: String,
        outcome: String
    ) {
        timer.stop(
            Timer.builder("gateway.request.duration")
                .description("Request processing time")
                .tag("method", method)
                .tag("path", sanitizePath(path))
                .tag("status", statusCode)
                .tag("user_type", if (userId == "anonymous") "anonymous" else "authenticated")
                .tag("outcome", outcome)
                .register(meterRegistry)
        )

        // Record status code metrics
        Counter.builder("gateway.responses.total")
            .description("Total number of responses by status code")
            .tag("method", method)
            .tag("path", sanitizePath(path))
            .tag("status", statusCode)
            .tag("status_class", getStatusClass(statusCode))
            .register(meterRegistry)
            .increment()

        // Record service-specific metrics
        val serviceName = extractServiceName(path)
        if (serviceName != "unknown") {
            Counter.builder("gateway.service.requests.total")
                .description("Total requests per service")
                .tag("service", serviceName)
                .tag("method", method)
                .tag("status", statusCode)
                .register(meterRegistry)
                .increment()
        }
    }

    /**
     * Sanitize path for metrics (remove dynamic segments)
     */
    private fun sanitizePath(path: String): String {
        return path
            .replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"), "/{uuid}")
            .replace(Regex("/[a-zA-Z0-9]{20,}"), "/{token}")
    }

    /**
     * Extract service name from path
     */
    private fun extractServiceName(path: String): String {
        return when {
            path.startsWith("/api/auth") -> "auth-service"
            path.startsWith("/api/stations") || path.startsWith("/api/employees") -> "station-service"
            path.startsWith("/api/coupons") || path.startsWith("/api/campaigns") -> "coupon-service"
            path.startsWith("/api/redemptions") || path.startsWith("/api/tickets") -> "redemption-service"
            path.startsWith("/api/ads") || path.startsWith("/api/engagements") -> "ad-engine"
            path.startsWith("/api/raffles") || path.startsWith("/api/entries") ||
            path.startsWith("/api/prizes") || path.startsWith("/api/winners") -> "raffle-service"
            else -> "unknown"
        }
    }

    /**
     * Get status class (2xx, 3xx, 4xx, 5xx)
     */
    private fun getStatusClass(statusCode: String): String {
        return when {
            statusCode.startsWith("2") -> "2xx"
            statusCode.startsWith("3") -> "3xx"
            statusCode.startsWith("4") -> "4xx"
            statusCode.startsWith("5") -> "5xx"
            else -> "unknown"
        }
    }

    override fun getOrder(): Int {
        return -10 // Execute after authentication but before business logic
    }
}