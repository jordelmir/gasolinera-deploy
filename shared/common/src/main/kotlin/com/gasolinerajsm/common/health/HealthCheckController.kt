package com.gasolinerajsm.common.health

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/health")
class HealthCheckController(
    private val healthCheckService: HealthCheckService
) {

    @GetMapping
    fun getHealth(): ResponseEntity<HealthResponse> {
        val healthStatus = healthCheckService.checkHealth()

        val httpStatus = when (healthStatus.status) {
            HealthStatusType.UP -> HttpStatus.OK
            HealthStatusType.DEGRADED -> HttpStatus.OK
            HealthStatusType.DOWN -> HttpStatus.SERVICE_UNAVAILABLE
            HealthStatusType.UNKNOWN -> HttpStatus.SERVICE_UNAVAILABLE
        }

        val response = HealthResponse(
            status = healthStatus.status.name,
            timestamp = healthStatus.timestamp,
            details = healthStatus.details,
            dependencies = healthStatus.dependencies.map { dep ->
                DependencyResponse(
                    name = dep.name,
                    status = dep.status.name,
                    responseTime = dep.responseTime,
                    error = dep.error
                )
            },
            responseTime = healthStatus.responseTime
        )

        return ResponseEntity.status(httpStatus).body(response)
    }

    @GetMapping("/dependencies")
    fun getDependencies(): ResponseEntity<DependencyStatusResponse> {
        val dependencyStatus = healthCheckService.checkDependencies()

        val response = DependencyStatusResponse(
            totalDependencies = dependencyStatus.totalDependencies,
            healthyDependencies = dependencyStatus.healthyDependencies,
            hasFailures = dependencyStatus.hasFailures,
            dependencies = dependencyStatus.dependencies.map { dep ->
                DependencyResponse(
                    name = dep.name,
                    status = dep.status.name,
                    responseTime = dep.responseTime,
                    error = dep.error,
                    lastChecked = dep.lastChecked
                )
            }
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/metrics")
    fun getMetrics(): ResponseEntity<ServiceMetricsResponse> {
        val metrics = healthCheckService.getMetrics()

        val response = ServiceMetricsResponse(
            totalHealthChecks = metrics.totalHealthChecks,
            averageResponseTime = metrics.averageResponseTime,
            uptime = metrics.uptime,
            lastHealthCheck = metrics.lastHealthCheck,
            errorRate = metrics.errorRate,
            memoryUsage = metrics.memoryUsage,
            cpuUsage = metrics.cpuUsage
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/deep")
    fun getDeepHealth(): ResponseEntity<DeepHealthResponse> {
        val deepHealth = healthCheckService.performDeepCheck()

        val response = DeepHealthResponse(
            overallStatus = deepHealth.overallStatus.name,
            checks = deepHealth.checks,
            executionTime = deepHealth.executionTime,
            timestamp = deepHealth.timestamp
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/dependencies")
    fun registerDependency(@RequestBody request: RegisterDependencyRequest): ResponseEntity<String> {
        val dependency = ServiceDependency(
            name = request.name,
            type = DependencyType.valueOf(request.type.uppercase()),
            url = request.url,
            timeout = request.timeout ?: 5000,
            critical = request.critical ?: true
        )

        healthCheckService.registerDependency(dependency)
        return ResponseEntity.ok("Dependency registered successfully")
    }

    @DeleteMapping("/dependencies/{serviceName}")
    fun removeDependency(@PathVariable serviceName: String): ResponseEntity<String> {
        healthCheckService.removeDependency(serviceName)
        return ResponseEntity.ok("Dependency removed successfully")
    }

    @GetMapping("/liveness")
    fun getLiveness(): ResponseEntity<Map<String, Any>> {
        // Verificación básica de que la aplicación está viva
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "timestamp" to Instant.now()
        ))
    }

    @GetMapping("/readiness")
    fun getReadiness(): ResponseEntity<Map<String, Any>> {
        val healthStatus = healthCheckService.checkHealth()

        val isReady = healthStatus.status == HealthStatusType.UP ||
                     healthStatus.status == HealthStatusType.DEGRADED

        val httpStatus = if (isReady) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE

        return ResponseEntity.status(httpStatus).body(mapOf(
            "status" to if (isReady) "READY" else "NOT_READY",
            "timestamp" to Instant.now(),
            "dependencies" to healthStatus.dependencies.map { it.name to it.status.name }.toMap()
        ))
    }
}

data class HealthResponse(
    val status: String,
    val timestamp: Instant,
    val details: Map<String, Any>,
    val dependencies: List<DependencyResponse>,
    val responseTime: Long
)

data class DependencyResponse(
    val name: String,
    val status: String,
    val responseTime: Long,
    val error: String? = null,
    val lastChecked: Instant? = null
)

data class DependencyStatusResponse(
    val totalDependencies: Int,
    val healthyDependencies: Int,
    val hasFailures: Boolean,
    val dependencies: List<DependencyResponse>
)

data class ServiceMetricsResponse(
    val totalHealthChecks: Int,
    val averageResponseTime: Double,
    val uptime: Long,
    val lastHealthCheck: Instant?,
    val errorRate: Double,
    val memoryUsage: Double,
    val cpuUsage: Double
)

data class DeepHealthResponse(
    val overallStatus: String,
    val checks: Map<String, Any>,
    val executionTime: Long,
    val timestamp: Instant
)

data class RegisterDependencyRequest(
    val name: String,
    val type: String,
    val url: String,
    val timeout: Long? = null,
    val critical: Boolean? = null
)