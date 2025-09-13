package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

/**
 * Controlador REST para endpoints de health checks personalizados
 */
@RestController
@RequestMapping("/api/health")
class HealthController(
    private val aggregatedHealthIndicator: AggregatedHealthIndicator,
    private val healthCheckService: HealthCheckService,
    private val customDatabaseHealthIndicator: CustomDatabaseHealthIndicator,
    private val customRedisHealthIndicator: CustomRedisHealthIndicator,
    private val businessHealthIndicator: BusinessHealthIndicator,
    private val externalServicesHealthIndicator: ExternalServicesHealthIndicator,
    private val systemResourcesHealthIndicator: SystemResourcesHealthIndicator
) {

    /**
     * Health check completo y detallado
     */
    @GetMapping("/detailed")
    fun getDetailedHealth(): ResponseEntity<Map<String, Any>> {
        val health = aggregatedHealthIndicator.health()
        val summary = aggregatedHealthIndicator.getHealthSummary()

        return ResponseEntity.ok(mapOf(
            "status" to health.status.code,
            "summary" to summary,
            "details" to health.details,
            "timestamp" to java.time.Instant.now().toString()
        ))
    }

    /**
     * Health check rápido para load balancers
     */
    @GetMapping("/quick")
    fun getQuickHealth(): ResponseEntity<Map<String, String>> {
        val criticalHealthy = aggregatedHealthIndicator.areCriticalComponentsHealthy()

        return if (criticalHealthy) {
            ResponseEntity.ok(mapOf(
                "status" to "UP",
                "timestamp" to java.time.Instant.now().toString()
            ))
        } else {
            ResponseEntity.status(503).body(mapOf(
                "status" to "DOWN",
                "timestamp" to java.time.Instant.now().toString()
            ))
        }
    }

    /**
     * Health check de un componente específico
     */
    @GetMapping("/component/{componentName}")
    fun getComponentHealth(@PathVariable componentName: String): ResponseEntity<Map<String, Any>> {
        val health = aggregatedHealthIndicator.getComponentHealth(componentName)

        return if (health != null) {
            ResponseEntity.ok(mapOf(
                "component" to componentName,
                "status" to health.status.code,
                "details" to health.details,
                "timestamp" to java.time.Instant.now().toString()
            ))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Métricas detalladas de base de datos
     */
    @GetMapping("/database/detailed")
    fun getDatabaseDetailedHealth(): ResponseEntity<DatabaseHealthDetails> {
        val details = customDatabaseHealthIndicator.getDetailedHealth()
        return ResponseEntity.ok(details)
    }

    /**
     * Métricas detalladas de Redis
     */
    @GetMapping("/redis/detailed")
    fun getRedisDetailedHealth(): ResponseEntity<RedisHealthDetails> {
        val details = customRedisHealthIndicator.getDetailedHealth()
        return ResponseEntity.ok(details)
    }

    /**
     * Métricas detalladas del sistema
     */
    @GetMapping("/system/detailed")
    fun getSystemDetailedHealth(): ResponseEntity<SystemMetrics> {
        val metrics = systemResourcesHealthIndicator.getDetailedSystemMetrics()
        return ResponseEntity.ok(metrics)
    }

    /**
     * Estado de todos los servicios externos
     */
    @GetMapping("/external-services")
    fun getExternalServicesHealth(): ResponseEntity<Map<String, ServiceHealthDetails>> {
        val servicesHealth = externalServicesHealthIndicator.getAllServicesHealth()
        return ResponseEntity.ok(servicesHealth)
    }

    /**
     * Health check de un servicio externo específico
     */
    @GetMapping("/external-services/{serviceName}")
    fun getSpecificExternalServiceHealth(@PathVariable serviceName: String): ResponseEntity<ServiceHealthDetails> {
        val serviceHealth = externalServicesHealthIndicator.checkSpecificService(serviceName)

        return if (serviceHealth != null) {
            ResponseEntity.ok(serviceHealth)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Estadísticas de disponibilidad de un componente
     */
    @GetMapping("/availability/{componentName}")
    fun getAvailabilityStats(
        @PathVariable componentName: String,
        @RequestParam(defaultValue = "24") periodHours: Long
    ): ResponseEntity<AvailabilityStats> {
        val period = Duration.ofHours(periodHours)
        val stats = healthCheckService.getAvailabilityStats(componentName, period)
        return ResponseEntity.ok(stats)
    }

    /**
     * Historial de health checks de un componente
     */
    @GetMapping("/history/{componentName}")
    fun getHealthHistory(
        @PathVariable componentName: String,
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<HealthRecord>> {
        val history = healthCheckService.getHealthHistory(componentName, limit)
        return ResponseEntity.ok(history)
    }

    /**
     * Estado actual de todos los componentes
     */
    @GetMapping("/status")
    fun getCurrentStatus(): ResponseEntity<Map<String, ComponentStatus>> {
        val status = healthCheckService.getCurrentStatus()
        return ResponseEntity.ok(status)
    }

    /**
     * Alertas activas
     */
    @GetMapping("/alerts")
    fun getActiveAlerts(): ResponseEntity<List<HealthAlert>> {
        val alerts = healthCheckService.getActiveAlerts()
        return ResponseEntity.ok(alerts)
    }

    /**
     * Reporte completo de salud
     */
    @GetMapping("/report")
    fun getHealthReport(@RequestParam(defaultValue = "24") periodHours: Long): ResponseEntity<HealthReport> {
        val period = Duration.ofHours(periodHours)
        val report = healthCheckService.generateHealthReport(period)
        return ResponseEntity.ok(report)
    }

    /**
     * Métricas de operaciones de negocio
     */
    @GetMapping("/business/operations/{operationType}")
    fun getBusinessOperationMetrics(@PathVariable operationType: String): ResponseEntity<OperationMetrics> {
        val operationTypeEnum = try {
            BusinessOperationType.valueOf(operationType.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val metrics = businessHealthIndicator.getOperationMetrics(operationTypeEnum)
        return if (metrics != null) {
            ResponseEntity.ok(metrics)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Registrar una operación de negocio (para testing o integración manual)
     */
    @PostMapping("/business/operations/{operationType}")
    fun recordBusinessOperation(
        @PathVariable operationType: String,
        @RequestBody request: RecordOperationRequest
    ): ResponseEntity<Map<String, String>> {
        val operationTypeEnum = try {
            BusinessOperationType.valueOf(operationType.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Invalid operation type: $operationType"
            ))
        }

        businessHealthIndicator.recordOperation(
            operationTypeEnum,
            Duration.ofMillis(request.durationMs),
            request.success,
            request.userId
        )

        return ResponseEntity.ok(mapOf(
            "message" to "Operation recorded successfully",
            "operationType" to operationType,
            "timestamp" to java.time.Instant.now().toString()
        ))
    }

    /**
     * Configurar threshold de alerta para un componente
     */
    @PostMapping("/alerts/threshold/{componentName}")
    fun setAlertThreshold(
        @PathVariable componentName: String,
        @RequestBody request: AlertThresholdRequest
    ): ResponseEntity<Map<String, String>> {
        healthCheckService.setAlertThreshold(
            componentName,
            request.maxConsecutiveFailures,
            request.maxFailureRate,
            Duration.ofMinutes(request.timeWindowMinutes)
        )

        return ResponseEntity.ok(mapOf(
            "message" to "Alert threshold configured successfully",
            "componentName" to componentName,
            "timestamp" to java.time.Instant.now().toString()
        ))
    }

    /**
     * Endpoint para readiness probe de Kubernetes
     */
    @GetMapping("/readiness")
    fun readinessProbe(): ResponseEntity<Map<String, String>> {
        // Verificar solo componentes críticos para readiness
        val criticalHealthy = aggregatedHealthIndicator.areCriticalComponentsHealthy()

        return if (criticalHealthy) {
            ResponseEntity.ok(mapOf(
                "status" to "READY",
                "timestamp" to java.time.Instant.now().toString()
            ))
        } else {
            ResponseEntity.status(503).body(mapOf(
                "status" to "NOT_READY",
                "timestamp" to java.time.Instant.now().toString()
            ))
        }
    }

    /**
     * Endpoint para liveness probe de Kubernetes
     */
    @GetMapping("/liveness")
    fun livenessProbe(): ResponseEntity<Map<String, String>> {
        // Verificar que la aplicación esté funcionando básicamente
        val systemHealthy = systemResourcesHealthIndicator.areResourcesHealthy()

        return if (systemHealthy) {
            ResponseEntity.ok(mapOf(
                "status" to "ALIVE",
                "timestamp" to java.time.Instant.now().toString()
            ))
        } else {
            ResponseEntity.status(503).body(mapOf(
                "status" to "NOT_ALIVE",
                "timestamp" to java.time.Instant.now().toString()
            ))
        }
    }
}

/**
 * Request para registrar una operación de negocio
 */
data class RecordOperationRequest(
    val durationMs: Long,
    val success: Boolean,
    val userId: String? = null
)

/**
 * Request para configurar threshold de alerta
 */
data class AlertThresholdRequest(
    val maxConsecutiveFailures: Int = 3,
    val maxFailureRate: Double = 0.1,
    val timeWindowMinutes: Long = 5
)