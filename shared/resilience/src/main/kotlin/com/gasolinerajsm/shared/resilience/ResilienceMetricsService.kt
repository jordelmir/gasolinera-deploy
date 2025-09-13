package com.gasolinerajsm.shared.resilience

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.retry.RetryRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Servicio para recopilar y exponer métricas de resilience
 */
@Service
class ResilienceMetricsService(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry,
    private val bulkheadRegistry: BulkheadRegistry,
    private val rateLimiterRegistry: RateLimiterRegistry,
    private val meterRegistry: MeterRegistry
) {

    private val metricsCache = ConcurrentHashMap<String, ResilienceMetrics>()
    private val lastUpdateTime = ConcurrentHashMap<String, Instant>()

    init {
        // Registrar métricas automáticamente
        registerCircuitBreakerMetrics()
        registerRetryMetrics()
        registerBulkheadMetrics()
        registerRateLimiterMetrics()
    }

    /**
     * Obtiene métricas consolidadas de resilience
     */
    fun getResilienceMetrics(): ResilienceMetrics {
        val circuitBreakerMetrics = getCircuitBreakerMetrics()
        val retryMetrics = getRetryMetrics()
        val bulkheadMetrics = getBulkheadMetrics()
        val rateLimiterMetrics = getRateLimiterMetrics()

        return ResilienceMetrics(
            circuitBreakers = circuitBreakerMetrics,
            retries = retryMetrics,
            bulkheads = bulkheadMetrics,
            rateLimiters = rateLimiterMetrics,
            timestamp = Instant.now()
        )
    }

    /**
     * Obtiene métricas de circuit breakers
     */
    fun getCircuitBreakerMetrics(): Map<String, CircuitBreakerMetrics> {
        return circuitBreakerRegistry.allCircuitBreakers.associate { circuitBreaker ->
            val metrics = circuitBreaker.metrics
            val name = circuitBreaker.name

            name to CircuitBreakerMetrics(
                name = name,
                state = circuitBreaker.state.name,
                failureRate = metrics.failureRate,
                slowCallRate = metrics.slowCallRate,
                numberOfBufferedCalls = metrics.numberOfBufferedCalls,
                numberOfFailedCalls = metrics.numberOfFailedCalls,
                numberOfSlowCalls = metrics.numberOfSlowCalls,
                numberOfSuccessfulCalls = metrics.numberOfSuccessfulCalls,
                numberOfNotPermittedCalls = metrics.numberOfNotPermittedCalls,
                timestamp = Instant.now()
            )
        }
    }

    /**
     * Obtiene métricas de retry
     */
    fun getRetryMetrics(): Map<String, RetryMetrics> {
        return retryRegistry.allRetries.associate { retry ->
            val metrics = retry.metrics
            val name = retry.name

            name to RetryMetrics(
                name = name,
                numberOfAttempts = metrics.numberOfAttempts,
                numberOfSuccessfulCallsWithoutRetryAttempt = metrics.numberOfSuccessfulCallsWithoutRetryAttempt,
                numberOfSuccessfulCallsWithRetryAttempt = metrics.numberOfSuccessfulCallsWithRetryAttempt,
                numberOfFailedCallsWithoutRetryAttempt = metrics.numberOfFailedCallsWithoutRetryAttempt,
                numberOfFailedCallsWithRetryAttempt = metrics.numberOfFailedCallsWithRetryAttempt,
                timestamp = Instant.now()
            )
        }
    }

    /**
     * Obtiene métricas de bulkhead
     */
    fun getBulkheadMetrics(): Map<String, BulkheadMetrics> {
        return bulkheadRegistry.allBulkheads.associate { bulkhead ->
            val metrics = bulkhead.metrics
            val name = bulkhead.name

            name to BulkheadMetrics(
                name = name,
                availableConcurrentCalls = metrics.availableConcurrentCalls,
                maxAllowedConcurrentCalls = metrics.maxAllowedConcurrentCalls,
                timestamp = Instant.now()
            )
        }
    }

    /**
     * Obtiene métricas de rate limiter
     */
    fun getRateLimiterMetrics(): Map<String, RateLimiterMetrics> {
        return rateLimiterRegistry.allRateLimiters.associate { rateLimiter ->
            val metrics = rateLimiter.metrics
            val name = rateLimiter.name

            name to RateLimiterMetrics(
                name = name,
                availablePermissions = metrics.availablePermissions,
                numberOfWaitingThreads = metrics.numberOfWaitingThreads,
                timestamp = Instant.now()
            )
        }
    }

    /**
     * Obtiene métricas históricas de un circuit breaker específico
     */
    fun getCircuitBreakerHistory(name: String, hours: Int = 24): List<CircuitBreakerSnapshot> {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(name)
        val metrics = circuitBreaker.metrics

        // En una implementación real, esto vendría de una base de datos o sistema de métricas
        // Por ahora, devolvemos un snapshot actual
        return listOf(
            CircuitBreakerSnapshot(
                timestamp = Instant.now(),
                state = circuitBreaker.state.name,
                failureRate = metrics.failureRate,
                slowCallRate = metrics.slowCallRate,
                numberOfCalls = metrics.numberOfBufferedCalls
            )
        )
    }

    /**
     * Obtiene un resumen de salud de resilience
     */
    fun getResilienceHealthSummary(): ResilienceHealthSummary {
        val circuitBreakers = getCircuitBreakerMetrics()
        val retries = getRetryMetrics()
        val bulkheads = getBulkheadMetrics()
        val rateLimiters = getRateLimiterMetrics()

        val openCircuitBreakers = circuitBreakers.values.count { it.state == "OPEN" }
        val halfOpenCircuitBreakers = circuitBreakers.values.count { it.state == "HALF_OPEN" }

        val highFailureRateCircuitBreakers = circuitBreakers.values.count { it.failureRate > 50.0f }
        val saturatedBulkheads = bulkheads.values.count {
            it.availableConcurrentCalls == 0 && it.maxAllowedConcurrentCalls > 0
        }
        val throttledRateLimiters = rateLimiters.values.count { it.numberOfWaitingThreads > 0 }

        val overallHealth = when {
            openCircuitBreakers > 0 || saturatedBulkheads > 0 -> ResilienceHealth.CRITICAL
            halfOpenCircuitBreakers > 0 || highFailureRateCircuitBreakers > 0 || throttledRateLimiters > 0 -> ResilienceHealth.WARNING
            else -> ResilienceHealth.HEALTHY
        }

        return ResilienceHealthSummary(
            overallHealth = overallHealth,
            totalCircuitBreakers = circuitBreakers.size,
            openCircuitBreakers = openCircuitBreakers,
            halfOpenCircuitBreakers = halfOpenCircuitBreakers,
            highFailureRateCircuitBreakers = highFailureRateCircuitBreakers,
            totalBulkheads = bulkheads.size,
            saturatedBulkheads = saturatedBulkheads,
            totalRateLimiters = rateLimiters.size,
            throttledRateLimiters = throttledRateLimiters,
            timestamp = Instant.now()
        )
    }

    /**
     * Registra métricas de circuit breakers en Micrometer
     */
    private fun registerCircuitBreakerMetrics() {
        circuitBreakerRegistry.allCircuitBreakers.forEach { circuitBreaker ->
            val tags = Tags.of("name", circuitBreaker.name)

            meterRegistry.gauge("resilience.circuitbreaker.failure_rate", tags, circuitBreaker) { cb ->
                cb.metrics.failureRate.toDouble()
            }

            meterRegistry.gauge("resilience.circuitbreaker.slow_call_rate", tags, circuitBreaker) { cb ->
                cb.metrics.slowCallRate.toDouble()
            }

            meterRegistry.gauge("resilience.circuitbreaker.buffered_calls", tags, circuitBreaker) { cb ->
                cb.metrics.numberOfBufferedCalls.toDouble()
            }

            meterRegistry.gauge("resilience.circuitbreaker.failed_calls", tags, circuitBreaker) { cb ->
                cb.metrics.numberOfFailedCalls.toDouble()
            }

            meterRegistry.gauge("resilience.circuitbreaker.successful_calls", tags, circuitBreaker) { cb ->
                cb.metrics.numberOfSuccessfulCalls.toDouble()
            }

            meterRegistry.gauge("resilience.circuitbreaker.not_permitted_calls", tags, circuitBreaker) { cb ->
                cb.metrics.numberOfNotPermittedCalls.toDouble()
            }
        }
    }

    /**
     * Registra métricas de retry en Micrometer
     */
    private fun registerRetryMetrics() {
        retryRegistry.allRetries.forEach { retry ->
            val tags = Tags.of("name", retry.name)

            meterRegistry.gauge("resilience.retry.attempts", tags, retry) { r ->
                r.metrics.numberOfAttempts.toDouble()
            }

            meterRegistry.gauge("resilience.retry.successful_without_retry", tags, retry) { r ->
                r.metrics.numberOfSuccessfulCallsWithoutRetryAttempt.toDouble()
            }

            meterRegistry.gauge("resilience.retry.successful_with_retry", tags, retry) { r ->
                r.metrics.numberOfSuccessfulCallsWithRetryAttempt.toDouble()
            }

            meterRegistry.gauge("resilience.retry.failed_without_retry", tags, retry) { r ->
                r.metrics.numberOfFailedCallsWithoutRetryAttempt.toDouble()
            }

            meterRegistry.gauge("resilience.retry.failed_with_retry", tags, retry) { r ->
                r.metrics.numberOfFailedCallsWithRetryAttempt.toDouble()
            }
        }
    }

    /**
     * Registra métricas de bulkhead en Micrometer
     */
    private fun registerBulkheadMetrics() {
        bulkheadRegistry.allBulkheads.forEach { bulkhead ->
            val tags = Tags.of("name", bulkhead.name)

            meterRegistry.gauge("resilience.bulkhead.available_concurrent_calls", tags, bulkhead) { b ->
                b.metrics.availableConcurrentCalls.toDouble()
            }

            meterRegistry.gauge("resilience.bulkhead.max_allowed_concurrent_calls", tags, bulkhead) { b ->
                b.metrics.maxAllowedConcurrentCalls.toDouble()
            }
        }
    }

    /**
     * Registra métricas de rate limiter en Micrometer
     */
    private fun registerRateLimiterMetrics() {
        rateLimiterRegistry.allRateLimiters.forEach { rateLimiter ->
            val tags = Tags.of("name", rateLimiter.name)

            meterRegistry.gauge("resilience.ratelimiter.available_permissions", tags, rateLimiter) { rl ->
                rl.metrics.availablePermissions.toDouble()
            }

            meterRegistry.gauge("resilience.ratelimiter.waiting_threads", tags, rateLimiter) { rl ->
                rl.metrics.numberOfWaitingThreads.toDouble()
            }
        }
    }
}

/**
 * Métricas consolidadas de resilience
 */
data class ResilienceMetrics(
    val circuitBreakers: Map<String, CircuitBreakerMetrics>,
    val retries: Map<String, RetryMetrics>,
    val bulkheads: Map<String, BulkheadMetrics>,
    val rateLimiters: Map<String, RateLimiterMetrics>,
    val timestamp: Instant
)

/**
 * Métricas de circuit breaker
 */
data class CircuitBreakerMetrics(
    val name: String,
    val state: String,
    val failureRate: Float,
    val slowCallRate: Float,
    val numberOfBufferedCalls: Int,
    val numberOfFailedCalls: Int,
    val numberOfSlowCalls: Int,
    val numberOfSuccessfulCalls: Int,
    val numberOfNotPermittedCalls: Long,
    val timestamp: Instant
)

/**
 * Métricas de retry
 */
data class RetryMetrics(
    val name: String,
    val numberOfAttempts: Long,
    val numberOfSuccessfulCallsWithoutRetryAttempt: Long,
    val numberOfSuccessfulCallsWithRetryAttempt: Long,
    val numberOfFailedCallsWithoutRetryAttempt: Long,
    val numberOfFailedCallsWithRetryAttempt: Long,
    val timestamp: Instant
)

/**
 * Métricas de bulkhead
 */
data class BulkheadMetrics(
    val name: String,
    val availableConcurrentCalls: Int,
    val maxAllowedConcurrentCalls: Int,
    val timestamp: Instant
)

/**
 * Métricas de rate limiter
 */
data class RateLimiterMetrics(
    val name: String,
    val availablePermissions: Int,
    val numberOfWaitingThreads: Int,
    val timestamp: Instant
)

/**
 * Snapshot histórico de circuit breaker
 */
data class CircuitBreakerSnapshot(
    val timestamp: Instant,
    val state: String,
    val failureRate: Float,
    val slowCallRate: Float,
    val numberOfCalls: Int
)

/**
 * Resumen de salud de resilience
 */
data class ResilienceHealthSummary(
    val overallHealth: ResilienceHealth,
    val totalCircuitBreakers: Int,
    val openCircuitBreakers: Int,
    val halfOpenCircuitBreakers: Int,
    val highFailureRateCircuitBreakers: Int,
    val totalBulkheads: Int,
    val saturatedBulkheads: Int,
    val totalRateLimiters: Int,
    val throttledRateLimiters: Int,
    val timestamp: Instant
)

/**
 * Estados de salud de resilience
 */
enum class ResilienceHealth {
    HEALTHY,
    WARNING,
    CRITICAL
}