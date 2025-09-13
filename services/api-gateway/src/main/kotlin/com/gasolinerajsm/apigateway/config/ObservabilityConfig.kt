package com.gasolinerajsm.apigateway.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for observability, metrics, and tracing
 */
@Configuration
class ObservabilityConfig {

    /**
     * Customize meter registry with common tags and filters
     */
    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config()
                .commonTags(
                    "application", "api-gateway",
                    "service", "gasolinera-jsm",
                    "environment", System.getProperty("spring.profiles.active", "default")
                )
                .meterFilter(MeterFilter.deny { id ->
                    // Filter out noisy metrics
                    val name = id.name
                    name.startsWith("jvm.gc.pause") ||
                    name.startsWith("process.uptime") ||
                    name.startsWith("system.load.average")
                })
                .meterFilter(MeterFilter.maximumExpectedValue("gateway.request.duration", java.time.Duration.ofSeconds(30)))
        }
    }

    /**
     * Custom metrics for business logic
     */
    @Bean
    fun customMetrics(meterRegistry: MeterRegistry): CustomMetrics {
        return CustomMetrics(meterRegistry)
    }
}

/**
 * Custom metrics collector for business-specific metrics
 */
class CustomMetrics(private val meterRegistry: MeterRegistry) {

    /**
     * Record authentication attempt
     */
    fun recordAuthAttempt(success: Boolean, method: String) {
        meterRegistry.counter(
            "gateway.auth.attempts",
            "success", success.toString(),
            "method", method
        ).increment()
    }

    /**
     * Record rate limit hit
     */
    fun recordRateLimitHit(userId: String, endpoint: String) {
        meterRegistry.counter(
            "gateway.rate.limit.hits",
            "user_type", if (userId == "anonymous") "anonymous" else "authenticated",
            "endpoint", endpoint
        ).increment()
    }

    /**
     * Record circuit breaker state change
     */
    fun recordCircuitBreakerStateChange(serviceName: String, state: String) {
        meterRegistry.counter(
            "gateway.circuit.breaker.state.changes",
            "service", serviceName,
            "state", state
        ).increment()
    }

    /**
     * Record security violation
     */
    fun recordSecurityViolation(type: String, userId: String, endpoint: String) {
        meterRegistry.counter(
            "gateway.security.violations",
            "type", type,
            "user_type", if (userId == "anonymous") "anonymous" else "authenticated",
            "endpoint", endpoint
        ).increment()
    }

    /**
     * Record service health check
     */
    fun recordServiceHealth(serviceName: String, healthy: Boolean) {
        meterRegistry.gauge(
            "gateway.service.health",
            listOf(
                io.micrometer.core.instrument.Tag.of("service", serviceName)
            ),
            if (healthy) 1.0 else 0.0
        )
    }
}