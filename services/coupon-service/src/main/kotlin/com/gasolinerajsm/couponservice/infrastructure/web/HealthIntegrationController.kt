package com.gasolinerajsm.couponservice.infrastructure.web

import com.gasolinerajsm.shared.health.*
import org.springframework.web.bind.annotation.*
import java.time.Duration

/**
 * Controlador que demuestra la integración del sistema de health checks
 * en el Coupon Service
 */
@RestController
@RequestMapping("/api/coupon-health")
class HealthIntegrationController(
    private val businessHealthIndicator: BusinessHealthIndicator,
    private val healthCheckService: HealthCheckService
) {

    /**
     * Ejemplo de operación de negocio con health check integrado
     */
    @PostMapping("/generate-with-health-tracking")
    fun generateCouponsWithHealthTracking(
        @RequestBody request: GenerateCouponsRequest
    ): GenerateCouponsResponse {

        // Iniciar tracking de operación crítica
        val operationContext = businessHealthIndicator.startCriticalOperation(
            BusinessOperationType.COUPON_GENERATION
        )

        return try {
            // Simular lógica de generación de cupones
            val startTime = System.currentTimeMillis()

            // Validaciones de negocio
            validateGenerationRequest(request)

            // Generar cupones (simulado)
            val coupons = generateCoupons(request)

            val duration = Duration.ofMillis(System.currentTimeMillis() - startTime)

            // Registrar operación exitosa
            businessHealthIndicator.completeCriticalOperation(
                operationContext,
                true,
                request.userId
            )

            // También registrar métricas adicionales
            businessHealthIndicator.recordOperation(
                BusinessOperationType.COUPON_GENERATION,
                duration,
                true,
                request.userId
            )

            GenerateCouponsResponse(
                success = true,
                coupons = coupons,
                message = "Cupones generados exitosamente",
                operationId = operationContext.startTime.toEpochMilli().toString()
            )

        } catch (e: Exception) {
            val duration = Duration.ofMillis(System.currentTimeMillis() - operationContext.startTime.toEpochMilli())

            // Registrar operación fallida
            businessHealthIndicator.completeCriticalOperation(
                operationContext,
                false,
                request.userId
            )

            businessHealthIndicator.recordOperation(
                BusinessOperationType.COUPON_GENERATION,
                duration,
                false,
                request.userId
            )

            throw CouponGenerationException("Error generando cupones: ${e.message}", e)
        }
    }

    /**
     * Endpoint para obtener métricas de salud específicas del servicio de cupones
     */
    @GetMapping("/service-metrics")
    fun getCouponServiceHealthMetrics(): CouponServiceHealthMetrics {

        // Obtener métricas de operaciones de cupones
        val couponGenerationMetrics = businessHealthIndicator.getOperationMetrics(
            BusinessOperationType.COUPON_GENERATION
        )

        val couponRedemptionMetrics = businessHealthIndicator.getOperationMetrics(
            BusinessOperationType.COUPON_REDEMPTION
        )

        // Obtener estadísticas de disponibilidad
        val availabilityStats = healthCheckService.getAvailabilityStats(
            "coupon-service",
            Duration.ofHours(24)
        )

        return CouponServiceHealthMetrics(
            serviceName = "coupon-service",
            overallAvailability = availabilityStats.availabilityPercentage,
            couponGeneration = couponGenerationMetrics?.let {
                OperationHealthMetrics(
                    totalOperations = it.totalOperations.get(),
                    successfulOperations = it.successfulOperations.get(),
                    failedOperations = it.failedOperations.get(),
                    successRate = it.getSuccessRate(),
                    averageResponseTime = it.getAverageResponseTime(),
                    lastOperationTime = java.time.Instant.ofEpochMilli(it.lastOperationTime.get())
                )
            },
            couponRedemption = couponRedemptionMetrics?.let {
                OperationHealthMetrics(
                    totalOperations = it.totalOperations.get(),
                    successfulOperations = it.successfulOperations.get(),
                    failedOperations = it.failedOperations.get(),
                    successRate = it.getSuccessRate(),
                    averageResponseTime = it.getAverageResponseTime(),
                    lastOperationTime = java.time.Instant.ofEpochMilli(it.lastOperationTime.get())
                )
            },
            timestamp = java.time.Instant.now()
        )
    }

    /**
     * Endpoint para simular diferentes escenarios de health checks
     */
    @PostMapping("/simulate-scenario")
    fun simulateHealthScenario(@RequestBody scenario: HealthScenarioRequest): HealthScenarioResponse {

        return when (scenario.scenarioType) {
            "SUCCESS" -> {
                // Simular operaciones exitosas
                repeat(scenario.operationCount) {
                    businessHealthIndicator.recordOperation(
                        BusinessOperationType.COUPON_GENERATION,
                        Duration.ofMillis((100..500).random().toLong()),
                        true,
                        "test-user-$it"
                    )
                }
                HealthScenarioResponse(
                    success = true,
                    message = "Simuladas ${scenario.operationCount} operaciones exitosas"
                )
            }

            "FAILURE" -> {
                // Simular operaciones fallidas
                repeat(scenario.operationCount) {
                    businessHealthIndicator.recordOperation(
                        BusinessOperationType.COUPON_GENERATION,
                        Duration.ofMillis((1000..3000).random().toLong()),
                        false,
                        "test-user-$it"
                    )
                }
                HealthScenarioResponse(
                    success = true,
                    message = "Simuladas ${scenario.operationCount} operaciones fallidas"
                )
            }

            "MIXED" -> {
                // Simular mix de operaciones
                repeat(scenario.operationCount) {
                    val success = (1..10).random() > 2 // 80% éxito
                    businessHealthIndicator.recordOperation(
                        BusinessOperationType.COUPON_GENERATION,
                        Duration.ofMillis((100..1000).random().toLong()),
                        success,
                        "test-user-$it"
                    )
                }
                HealthScenarioResponse(
                    success = true,
                    message = "Simuladas ${scenario.operationCount} operaciones mixtas"
                )
            }

            "SLOW" -> {
                // Simular operaciones lentas
                repeat(scenario.operationCount) {
                    businessHealthIndicator.recordOperation(
                        BusinessOperationType.COUPON_GENERATION,
                        Duration.ofMillis((5000..10000).random().toLong()),
                        true,
                        "test-user-$it"
                    )
                }
                HealthScenarioResponse(
                    success = true,
                    message = "Simuladas ${scenario.operationCount} operaciones lentas"
                )
            }

            else -> {
                HealthScenarioResponse(
                    success = false,
                    message = "Tipo de escenario desconocido: ${scenario.scenarioType}"
                )
            }
        }
    }

    /**
     * Endpoint para obtener recomendaciones de salud del servicio
     */
    @GetMapping("/health-recommendations")
    fun getHealthRecommendations(): HealthRecommendationsResponse {

        val recommendations = mutableListOf<HealthRecommendation>()

        // Analizar métricas de generación de cupones
        val generationMetrics = businessHealthIndicator.getOperationMetrics(
            BusinessOperationType.COUPON_GENERATION
        )

        generationMetrics?.let { metrics ->
            val errorRate = metrics.getErrorRate()
            val avgResponseTime = metrics.getAverageResponseTime()

            // Recomendaciones basadas en tasa de error
            when {
                errorRate > 0.1 -> recommendations.add(
                    HealthRecommendation(
                        type = "ERROR_RATE",
                        severity = "HIGH",
                        message = "Tasa de error alta en generación de cupones: ${(errorRate * 100).toInt()}%",
                        recommendation = "Revisar logs de errores y validar lógica de generación",
                        priority = 1
                    )
                )
                errorRate > 0.05 -> recommendations.add(
                    HealthRecommendation(
                        type = "ERROR_RATE",
                        severity = "MEDIUM",
                        message = "Tasa de error moderada en generación de cupones: ${(errorRate * 100).toInt()}%",
                        recommendation = "Monitorear tendencia y considerar optimizaciones",
                        priority = 2
                    )
                )
            }

            // Recomendaciones basadas en tiempo de respuesta
            when {
                avgResponseTime > Duration.ofSeconds(5) -> recommendations.add(
                    HealthRecommendation(
                        type = "PERFORMANCE",
                        severity = "HIGH",
                        message = "Tiempo de respuesta alto: ${avgResponseTime.toMillis()}ms",
                        recommendation = "Optimizar queries de base de datos y lógica de negocio",
                        priority = 1
                    )
                )
                avgResponseTime > Duration.ofSeconds(2) -> recommendations.add(
                    HealthRecommendation(
                        type = "PERFORMANCE",
                        severity = "MEDIUM",
                        message = "Tiempo de respuesta moderado: ${avgResponseTime.toMillis()}ms",
                        recommendation = "Considerar optimizaciones de performance",
                        priority = 3
                    )
                )
            }
        }

        // Obtener alertas activas
        val activeAlerts = healthCheckService.getActiveAlerts()
        activeAlerts.forEach { alert ->
            recommendations.add(
                HealthRecommendation(
                    type = "ALERT",
                    severity = alert.severity.name,
                    message = "Alerta activa: ${alert.message}",
                    recommendation = "Revisar componente ${alert.componentName} y resolver problema subyacente",
                    priority = when (alert.severity) {
                        AlertSeverity.CRITICAL -> 1
                        AlertSeverity.HIGH -> 1
                        AlertSeverity.MEDIUM -> 2
                        AlertSeverity.LOW -> 3
                    }
                )
            )
        }

        // Si no hay recomendaciones, todo está bien
        if (recommendations.isEmpty()) {
            recommendations.add(
                HealthRecommendation(
                    type = "STATUS",
                    severity = "INFO",
                    message = "Servicio funcionando correctamente",
                    recommendation = "Continuar monitoreo regular",
                    priority = 4
                )
            )
        }

        return HealthRecommendationsResponse(
            serviceName = "coupon-service",
            recommendations = recommendations.sortedBy { it.priority },
            totalRecommendations = recommendations.size,
            criticalIssues = recommendations.count { it.severity == "HIGH" || it.severity == "CRITICAL" },
            timestamp = java.time.Instant.now()
        )
    }

    // Métodos auxiliares privados

    private fun validateGenerationRequest(request: GenerateCouponsRequest) {
        if (request.campaignId.isBlank()) {
            throw IllegalArgumentException("Campaign ID es requerido")
        }
        if (request.quantity <= 0) {
            throw IllegalArgumentException("Cantidad debe ser mayor a 0")
        }
        if (request.quantity > 1000) {
            throw IllegalArgumentException("Cantidad máxima es 1000 cupones por operación")
        }
    }

    private fun generateCoupons(request: GenerateCouponsRequest): List<CouponDto> {
        // Simular generación de cupones
        return (1..request.quantity).map { index ->
            CouponDto(
                id = "coupon-${request.campaignId}-$index",
                code = "CODE-${System.currentTimeMillis()}-$index",
                campaignId = request.campaignId,
                userId = request.userId,
                status = "ACTIVE",
                createdAt = java.time.Instant.now()
            )
        }
    }
}

// DTOs para el controlador

data class GenerateCouponsRequest(
    val campaignId: String,
    val userId: String,
    val quantity: Int
)

data class GenerateCouponsResponse(
    val success: Boolean,
    val coupons: List<CouponDto>,
    val message: String,
    val operationId: String
)

data class CouponDto(
    val id: String,
    val code: String,
    val campaignId: String,
    val userId: String,
    val status: String,
    val createdAt: java.time.Instant
)

data class CouponServiceHealthMetrics(
    val serviceName: String,
    val overallAvailability: Double,
    val couponGeneration: OperationHealthMetrics?,
    val couponRedemption: OperationHealthMetrics?,
    val timestamp: java.time.Instant
)

data class OperationHealthMetrics(
    val totalOperations: Long,
    val successfulOperations: Long,
    val failedOperations: Long,
    val successRate: Double,
    val averageResponseTime: Duration,
    val lastOperationTime: java.time.Instant
)

data class HealthScenarioRequest(
    val scenarioType: String, // SUCCESS, FAILURE, MIXED, SLOW
    val operationCount: Int = 10
)

data class HealthScenarioResponse(
    val success: Boolean,
    val message: String
)

data class HealthRecommendationsResponse(
    val serviceName: String,
    val recommendations: List<HealthRecommendation>,
    val totalRecommendations: Int,
    val criticalIssues: Int,
    val timestamp: java.time.Instant
)

data class HealthRecommendation(
    val type: String,
    val severity: String,
    val message: String,
    val recommendation: String,
    val priority: Int
)

class CouponGenerationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)