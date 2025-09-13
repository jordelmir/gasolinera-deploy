package com.gasolinerajsm.apigateway.controller

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Controller for monitoring and health check endpoints
 */
@RestController
@RequestMapping("/monitoring")
class MonitoringController(
    private val meterRegistry: MeterRegistry,
    private val routeLocator: RouteLocator
) {

    /**
     * Get gateway health status
     */
    @GetMapping("/health")
    fun getHealth(): ResponseEntity<Map<String, Any>> {
        val health = mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "gateway" to "api-gateway",
            "version" to "1.0.0",
            "uptime" to getUptime()
        )

        return ResponseEntity.ok(health)
    }

    /**
     * Get gateway metrics summary
     */
    @GetMapping("/metrics")
    fun getMetrics(): ResponseEntity<Map<String, Any>> {
        val metrics = mutableMapOf<String, Any>()

        // Request metrics
        val requestCounter = meterRegistry.find("gateway.requests.total").counter()
        val requestTimer = meterRegistry.find("gateway.request.duration").timer()
        val errorCounter = meterRegistry.find("gateway.errors.total").counter()

        metrics["requests"] = mapOf(
            "total" to (requestCounter?.count() ?: 0.0),
            "errors" to (errorCounter?.count() ?: 0.0),
            "averageResponseTime" to (requestTimer?.mean() ?: 0.0),
            "maxResponseTime" to (requestTimer?.max() ?: 0.0)
        )

        // Service metrics
        val serviceMetrics = mutableMapOf<String, Any>()
        listOf("auth-service", "station-service", "coupon-service", "redemption-service", "ad-engine", "raffle-service")
            .forEach { service ->
                val serviceCounter = meterRegistry.find("gateway.service.requests.total")
                    .tag("service", service)
                    .counter()

                serviceMetrics[service] = mapOf(
                    "requests" to (serviceCounter?.count() ?: 0.0)
                )
            }

        metrics["services"] = serviceMetrics
        metrics["timestamp"] = LocalDateTime.now()

        return ResponseEntity.ok(metrics)
    }

    /**
     * Get active routes
     */
    @GetMapping("/routes")
    fun getRoutes(): ResponseEntity<Map<String, Any>> {
        val routes = routeLocator.routes
            .collectList()
            .block()
            ?.map { route ->
                mapOf(
                    "id" to route.id,
                    "uri" to route.uri.toString(),
                    "predicates" to route.predicate.toString(),
                    "filters" to route.filters.map { it.toString() }
                )
            } ?: emptyList()

        val response = mapOf(
            "routes" to routes,
            "totalRoutes" to routes.size,
            "timestamp" to LocalDateTime.now()
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get circuit breaker status
     */
    @GetMapping("/circuit-breakers")
    fun getCircuitBreakers(): ResponseEntity<Map<String, Any>> {
        val circuitBreakers = mutableMapOf<String, Any>()

        // Get circuit breaker metrics from registry
        listOf("auth-service-cb", "station-service-cb", "coupon-service-cb",
               "redemption-service-cb", "ad-engine-cb", "raffle-service-cb")
            .forEach { cbName ->
                val stateGauge = meterRegistry.find("resilience4j.circuitbreaker.state")
                    .tag("name", cbName)
                    .gauge()

                val callsGauge = meterRegistry.find("resilience4j.circuitbreaker.calls")
                    .tag("name", cbName)
                    .gauge()

                circuitBreakers[cbName] = mapOf(
                    "state" to (stateGauge?.value() ?: 0.0),
                    "calls" to (callsGauge?.value() ?: 0.0)
                )
            }

        val response = mapOf(
            "circuitBreakers" to circuitBreakers,
            "timestamp" to LocalDateTime.now()
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get rate limiting status
     */
    @GetMapping("/rate-limits")
    fun getRateLimits(): ResponseEntity<Map<String, Any>> {
        val rateLimits = mutableMapOf<String, Any>()

        // Get rate limiting metrics
        val rateLimitGauge = meterRegistry.find("spring.cloud.gateway.requests")
            .gauge()

        rateLimits["requests"] = mapOf(
            "current" to (rateLimitGauge?.value() ?: 0.0)
        )

        val response = mapOf(
            "rateLimits" to rateLimits,
            "timestamp" to LocalDateTime.now()
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get system information
     */
    @GetMapping("/system")
    fun getSystemInfo(): ResponseEntity<Map<String, Any>> {
        val runtime = Runtime.getRuntime()

        val systemInfo = mapOf(
            "jvm" to mapOf(
                "version" to System.getProperty("java.version"),
                "vendor" to System.getProperty("java.vendor"),
                "maxMemory" to runtime.maxMemory(),
                "totalMemory" to runtime.totalMemory(),
                "freeMemory" to runtime.freeMemory(),
                "usedMemory" to (runtime.totalMemory() - runtime.freeMemory()),
                "availableProcessors" to runtime.availableProcessors()
            ),
            "os" to mapOf(
                "name" to System.getProperty("os.name"),
                "version" to System.getProperty("os.version"),
                "arch" to System.getProperty("os.arch")
            ),
            "timestamp" to LocalDateTime.now()
        )

        return ResponseEntity.ok(systemInfo)
    }

    /**
     * Reset metrics (admin endpoint)
     */
    @GetMapping("/reset-metrics")
    fun resetMetrics(): ResponseEntity<Map<String, Any>> {
        // Clear specific meters
        meterRegistry.clear()

        val response = mapOf(
            "message" to "Metrics reset successfully",
            "timestamp" to LocalDateTime.now()
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get application uptime
     */
    private fun getUptime(): String {
        val uptimeMs = System.currentTimeMillis() - startTime
        val seconds = uptimeMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return "${days}d ${hours % 24}h ${minutes % 60}m ${seconds % 60}s"
    }

    companion object {
        private val startTime = System.currentTimeMillis()
    }
}