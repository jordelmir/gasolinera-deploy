package com.gasolinerajsm.shared.resilience

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para gestión y monitoreo de resilience
 */
@RestController
@RequestMapping("/api/resilience")
class ResilienceController(
    private val resilienceService: ResilienceService,
    private val resilienceMetricsService: ResilienceMetricsService,
    private val fallbackService: FallbackService
) {

    /**
     * Obtiene métricas consolidadas de resilience
     */
    @GetMapping("/metrics")
    fun getMetrics(): ResponseEntity<ResilienceMetrics> {
        val metrics = resilienceMetricsService.getResilienceMetrics()
        return ResponseEntity.ok(metrics)
    }

    /**
     * Obtiene resumen de salud de resilience
     */
    @GetMapping("/health")
    fun getHealthSummary(): ResponseEntity<ResilienceHealthSummary> {
        val health = resilienceMetricsService.getResilienceHealthSummary()
        return ResponseEntity.ok(health)
    }

    /**
     * Obtiene métricas específicas de circuit breakers
     */
    @GetMapping("/circuit-breakers")
    fun getCircuitBreakerMetrics(): ResponseEntity<Map<String, CircuitBreakerMetrics>> {
        val metrics = resilienceMetricsService.getCircuitBreakerMetrics()
        return ResponseEntity.ok(metrics)
    }

    /**
     * Obtiene métricas de un circuit breaker específico
     */
    @GetMapping("/circuit-breakers/{name}")
    fun getCircuitBreakerMetrics(@PathVariable name: String): ResponseEntity<CircuitBreakerState> {
        return try {
            val state = resilienceService.getCircuitBreakerState(name)
            ResponseEntity.ok(state)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Obtiene historial de un circuit breaker
     */
    @GetMapping("/circuit-breakers/{name}/history")
    fun getCircuitBreakerHistory(
        @PathVariable name: String,
        @RequestParam(defaultValue = "24") hours: Int
    ): ResponseEntity<List<CircuitBreakerSnapshot>> {
        return try {
            val history = resilienceMetricsService.getCircuitBreakerHistory(name, hours)
            ResponseEntity.ok(history)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Fuerza la transición de un circuit breaker a un estado específico
     */
    @PostMapping("/circuit-breakers/{name}/transition")
    fun transitionCircuitBreaker(
        @PathVariable name: String,
        @RequestBody request: CircuitBreakerTransitionRequest
    ): ResponseEntity<String> {
        return try {
            val state = CircuitBreaker.State.valueOf(request.state.uppercase())
            resilienceService.transitionCircuitBreakerToState(name, state)
            ResponseEntity.ok("Circuit breaker '$name' transitioned to ${request.state}")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body("Invalid state: ${request.state}")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Error transitioning circuit breaker: ${e.message}")
        }
    }

    /**
     * Resetea un circuit breaker
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    fun resetCircuitBreaker(@PathVariable name: String): ResponseEntity<String> {
        return try {
            resilienceService.resetCircuitBreaker(name)
            ResponseEntity.ok("Circuit breaker '$name' reset successfully")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Error resetting circuit breaker: ${e.message}")
        }
    }

    /**
     * Obtiene métricas de retry
     */
    @GetMapping("/retries")
    fun getRetryMetrics(): ResponseEntity<Map<String, RetryMetrics>> {
        val metrics = resilienceMetricsService.getRetryMetrics()
        return ResponseEntity.ok(metrics)
    }

    /**
     * Obtiene métricas de un retry específico
     */
    @GetMapping("/retries/{name}")
    fun getRetryMetrics(@PathVariable name: String): ResponseEntity<RetryState> {
        return try {
            val state = resilienceService.getRetryState(name)
            ResponseEntity.ok(state)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Obtiene métricas de bulkhead
     */
    @GetMapping("/bulkheads")
    fun getBulkheadMetrics(): ResponseEntity<Map<String, BulkheadMetrics>> {
        val metrics = resilienceMetricsService.getBulkheadMetrics()
        return ResponseEntity.ok(metrics)
    }

    /**
     * Obtiene métricas de un bulkhead específico
     */
    @GetMapping("/bulkheads/{name}")
    fun getBulkheadMetrics(@PathVariable name: String): ResponseEntity<BulkheadState> {
        return try {
            val state = resilienceService.getBulkheadState(name)
            ResponseEntity.ok(state)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Obtiene métricas de rate limiter
     */
    @GetMapping("/rate-limiters")
    fun getRateLimiterMetrics(): ResponseEntity<Map<String, RateLimiterMetrics>> {
        val metrics = resilienceMetricsService.getRateLimiterMetrics()
        return ResponseEntity.ok(metrics)
    }

    /**
     * Obtiene métricas de un rate limiter específico
     */
    @GetMapping("/rate-limiters/{name}")
    fun getRateLimiterMetrics(@PathVariable name: String): ResponseEntity<RateLimiterState> {
        return try {
            val state = resilienceService.getRateLimiterState(name)
            ResponseEntity.ok(state)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Obtiene estadísticas del cache de fallbacks
     */
    @GetMapping("/fallbacks/cache/stats")
    fun getFallbackCacheStats(): ResponseEntity<FallbackCacheStats> {
        val stats = fallbackService.getFallbackCacheStats()
        return ResponseEntity.ok(stats)
    }

    /**
     * Limpia el cache de fallbacks
     */
    @PostMapping("/fallbacks/cache/clear")
    fun clearFallbackCache(): ResponseEntity<String> {
        fallbackService.clearFallbackCache()
        return ResponseEntity.ok("Fallback cache cleared successfully")
    }

    /**
     * Limpia fallbacks expirados del cache
     */
    @PostMapping("/fallbacks/cache/cleanup")
    fun cleanupExpiredFallbacks(): ResponseEntity<String> {
        fallbackService.cleanupExpiredFallbacks()
        return ResponseEntity.ok("Expired fallbacks cleaned up successfully")
    }

    /**
     * Ejecuta una operación de prueba con resilience
     */
    @PostMapping("/test")
    fun testResilience(@RequestBody request: ResilienceTestRequest): ResponseEntity<ResilienceTestResponse> {
        return try {
            val startTime = System.currentTimeMillis()

            val result = resilienceService.executeWithResilience(
                config = ResilienceConfig(
                    circuitBreakerName = request.circuitBreakerName,
                    retryName = request.retryName,
                    bulkheadName = request.bulkheadName,
                    rateLimiterName = request.rateLimiterName
                ),
                operation = {
                    // Simular operación que puede fallar
                    if (request.shouldFail) {
                        throw RuntimeException("Simulated failure")
                    }
                    "Operation completed successfully"
                },
                fallback = {
                    "Fallback response"
                }
            )

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            ResponseEntity.ok(
                ResilienceTestResponse(
                    success = true,
                    result = result.toString(),
                    duration = duration,
                    message = "Test completed successfully"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.ok(
                ResilienceTestResponse(
                    success = false,
                    result = null,
                    duration = 0,
                    message = "Test failed: ${e.message}"
                )
            )
        }
    }
}

/**
 * Request para transición de circuit breaker
 */
data class CircuitBreakerTransitionRequest(
    val state: String
)

/**
 * Request para prueba de resilience
 */
data class ResilienceTestRequest(
    val circuitBreakerName: String? = null,
    val retryName: String? = null,
    val bulkheadName: String? = null,
    val rateLimiterName: String? = null,
    val shouldFail: Boolean = false
)

/**
 * Response de prueba de resilience
 */
data class ResilienceTestResponse(
    val success: Boolean,
    val result: String?,
    val duration: Long,
    val message: String
)