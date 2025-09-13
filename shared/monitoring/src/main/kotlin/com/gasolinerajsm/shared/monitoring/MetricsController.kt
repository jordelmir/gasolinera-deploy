package com.gasolinerajsm.shared.monitoring

import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * Metrics Controller
 * Provides endpoints for metrics access and management
 */
@RestController
@RequestMapping("/actuator")
class MetricsController(
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
    private val businessMetricsService: BusinessMetricsService,
    private val systemMetricsService: SystemMetricsService,
    private val databaseMetricsService: DatabaseMetricsService,
    private val httpMetricsInterceptor: HttpMetricsInterceptor,
    private val healthEndpoint: HealthEndpoint
) {

    /**
     * Prometheus metrics endpoint
     */
    @GetMapping("/prometheus", produces = ["text/plain"])
    fun prometheus(): ResponseEntity<String> {
        return ResponseEntity.ok(prometheusMeterRegistry.scrape())
    }

    /**
     * Custom metrics summary
     */
    @GetMapping("/metrics/summary")
    fun getMetricsSummary(): ResponseEntity<MetricsSummary> {
        val summary = MetricsSummary(
            timestamp = LocalDateTime.now(),
            business = businessMetricsService.getMetricsSummary(),
            system = systemMetricsService.getSystemMetrics(),
            database = databaseMetricsService.getDatabaseMetrics(),
            http = httpMetricsInterceptor.getMetricsSummary(),
            health = healthEndpoint.health()
        )

        return ResponseEntity.ok(summary)
    }

    /**
     * Business metrics endpoint
     */
    @GetMapping("/metrics/business")
    fun getBusinessMetrics(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(businessMetricsService.getMetricsSummary())
    }

    /**
     * System metrics endpoint
     */
    @GetMapping("/metrics/system")
    fun getSystemMetrics(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(systemMetricsService.getSystemMetrics())
    }

    /**
     * Database metrics endpoint
     */
    @GetMapping("/metrics/database")
    fun getDatabaseMetrics(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(databaseMetricsService.getDatabaseMetrics())
    }

    /**
     * HTTP metrics endpoint
     */
    @GetMapping("/metrics/http")
    fun getHttpMetrics(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(httpMetricsInterceptor.getMetricsSummary())
    }

    /**
     * Performance alerts endpoint
     */
    @GetMapping("/metrics/alerts")
    fun getPerformanceAlerts(): ResponseEntity<List<SystemMetricsService.PerformanceAlert>> {
        return ResponseEntity.ok(systemMetricsService.getPerformanceAlerts())
    }

    /**
     * Metrics configuration endpoint
     */
    @GetMapping("/metrics/config")
    fun getMetricsConfig(): ResponseEntity<MetricsConfigInfo> {
        val config = MetricsConfigInfo(
            enabled = true,
            prometheusEnabled = true,
            businessMetricsEnabled = true,
            systemMetricsEnabled = true,
            databaseMetricsEnabled = true,
            httpMetricsEnabled = true,
            availableMetrics = getAvailableMetrics()
        )

        return ResponseEntity.ok(config)
    }

    /**
     * Reset metrics (for testing)
     */
    @PostMapping("/metrics/reset")
    fun resetMetrics(): ResponseEntity<Map<String, String>> {
        businessMetricsService.resetMetrics()

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Metrics reset successfully",
            "timestamp" to LocalDateTime.now().toString()
        ))
    }

    /**
     * Get available metrics list
     */
    private fun getAvailableMetrics(): List<String> {
        return prometheusMeterRegistry.meters.map { it.id.name }.distinct().sorted()
    }

    data class MetricsSummary(
        val timestamp: LocalDateTime,
        val business: Map<String, Any>,
        val system: Map<String, Any>,
        val database: Map<String, Any>,
        val http: Map<String, Any>,
        val health: Any
    )

    data class MetricsConfigInfo(
        val enabled: Boolean,
        val prometheusEnabled: Boolean,
        val businessMetricsEnabled: Boolean,
        val systemMetricsEnabled: Boolean,
        val databaseMetricsEnabled: Boolean,
        val httpMetricsEnabled: Boolean,
        val availableMetrics: List<String>
    )
}

/**
 * Metrics Management Controller
 * Administrative endpoints for metrics management
 */
@RestController
@RequestMapping("/api/v1/admin/metrics")
class MetricsManagementController(
    private val businessMetricsService: BusinessMetricsService,
    private val systemMetricsService: SystemMetricsService,
    private val databaseMetricsService: DatabaseMetricsService
) {

    /**
     * Get comprehensive metrics dashboard data
     */
    @GetMapping("/dashboard")
    fun getMetricsDashboard(): ResponseEntity<MetricsDashboard> {
        val dashboard = MetricsDashboard(
            timestamp = LocalDateTime.now(),
            overview = getMetricsOverview(),
            businessMetrics = getBusinessMetricsDetails(),
            systemHealth = getSystemHealthDetails(),
            databaseHealth = getDatabaseHealthDetails(),
            alerts = systemMetricsService.getPerformanceAlerts()
        )

        return ResponseEntity.ok(dashboard)
    }

    /**
     * Get metrics for specific time range
     */
    @GetMapping("/range")
    fun getMetricsRange(
        @RequestParam startTime: String,
        @RequestParam endTime: String,
        @RequestParam(defaultValue = "business") type: String
    ): ResponseEntity<Map<String, Any>> {
        // This would integrate with a time-series database like InfluxDB
        // For now, return current metrics
        val metrics = when (type) {
            "business" -> businessMetricsService.getMetricsSummary()
            "system" -> systemMetricsService.getSystemMetrics()
            "database" -> databaseMetricsService.getDatabaseMetrics()
            else -> emptyMap()
        }

        return ResponseEntity.ok(mapOf(
            "type" to type,
            "start_time" to startTime,
            "end_time" to endTime,
            "data" to metrics
        ))
    }

    /**
     * Export metrics data
     */
    @GetMapping("/export")
    fun exportMetrics(
        @RequestParam(defaultValue = "json") format: String
    ): ResponseEntity<String> {
        val allMetrics = mapOf(
            "business" to businessMetricsService.getMetricsSummary(),
            "system" to systemMetricsService.getSystemMetrics(),
            "database" to databaseMetricsService.getDatabaseMetrics(),
            "timestamp" to LocalDateTime.now()
        )

        return when (format.lowercase()) {
            "json" -> ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .header("Content-Disposition", "attachment; filename=metrics-export.json")
                .body(com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(allMetrics))

            "csv" -> ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=metrics-export.csv")
                .body(convertToCsv(allMetrics))

            else -> ResponseEntity.badRequest().body("Unsupported format: $format")
        }
    }

    private fun getMetricsOverview(): Map<String, Any> {
        return mapOf(
            "total_requests" to 0, // Would get from actual metrics
            "error_rate" to 0.0,
            "avg_response_time" to 0.0,
            "active_users" to 0,
            "system_health" to "healthy"
        )
    }

    private fun getBusinessMetricsDetails(): Map<String, Any> {
        return mapOf(
            "coupons" to mapOf(
                "generated_today" to 0,
                "used_today" to 0,
                "validation_success_rate" to 0.0
            ),
            "raffles" to mapOf(
                "active_raffles" to 0,
                "participations_today" to 0,
                "draws_completed" to 0
            ),
            "stations" to mapOf(
                "active_stations" to 0,
                "transactions_today" to 0,
                "fuel_price_updates" to 0
            )
        )
    }

    private fun getSystemHealthDetails(): Map<String, Any> {
        return systemMetricsService.getSystemMetrics()
    }

    private fun getDatabaseHealthDetails(): Map<String, Any> {
        return databaseMetricsService.getDatabaseMetrics()
    }

    private fun convertToCsv(data: Map<String, Any>): String {
        val csv = StringBuilder()
        csv.append("Category,Metric,Value\n")

        fun flattenMap(map: Map<String, Any>, prefix: String = "") {
            map.forEach { (key, value) ->
                val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
                when (value) {
                    is Map<*, *> -> flattenMap(value as Map<String, Any>, fullKey)
                    else -> csv.append("$prefix,$key,$value\n")
                }
            }
        }

        flattenMap(data)
        return csv.toString()
    }

    data class MetricsDashboard(
        val timestamp: LocalDateTime,
        val overview: Map<String, Any>,
        val businessMetrics: Map<String, Any>,
        val systemHealth: Map<String, Any>,
        val databaseHealth: Map<String, Any>,
        val alerts: List<SystemMetricsService.PerformanceAlert>
    )
}