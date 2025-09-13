package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.boot.actuator.health.Status
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Health indicator agregado que combina múltiples health checks
 */
@Component
class AggregatedHealthIndicator(
    private val healthIndicators: Map<String, HealthIndicator>
) : HealthIndicator {

    override fun health(): Health {
        val startTime = Instant.now()
        val results = mutableMapOf<String, Health>()
        val futures = mutableMapOf<String, CompletableFuture<Health>>()

        // Ejecutar health checks en paralelo
        for ((name, indicator) in healthIndicators) {
            futures[name] = CompletableFuture.supplyAsync {
                try {
                    indicator.health()
                } catch (e: Exception) {
                    Health.down()
                        .withDetail("error", e.message ?: "Unknown error")
                        .withDetail("errorType", e.javaClass.simpleName)
                        .withException(e)
                        .build()
                }
            }
        }

        // Recopilar resultados con timeout
        for ((name, future) in futures) {
            try {
                results[name] = future.get(30, TimeUnit.SECONDS)
            } catch (e: Exception) {
                results[name] = Health.down()
                    .withDetail("error", "Health check timeout or failed")
                    .withDetail("errorType", e.javaClass.simpleName)
                    .build()
            }
        }

        // Determinar estado general
        val overallStatus = determineOverallStatus(results)
        val endTime = Instant.now()
        val totalDuration = java.time.Duration.between(startTime, endTime)

        // Crear resumen
        val summary = createHealthSummary(results)

        return Health.Builder(overallStatus)
            .withDetail("gasolineraJsm", "Aggregated Health Check")
            .withDetail("summary", summary)
            .withDetail("components", results.mapValues { (_, health) ->
                mapOf(
                    "status" to health.status.code,
                    "details" to health.details
                )
            })
            .withDetail("execution", mapOf(
                "totalDurationMs" to totalDuration.toMillis(),
                "componentsChecked" to results.size,
                "timestamp" to endTime.toString()
            ))
            .build()
    }

    private fun determineOverallStatus(results: Map<String, Health>): Status {
        val statuses = results.values.map { it.status }

        return when {
            statuses.any { it == Status.DOWN } -> Status.DOWN
            statuses.any { it == Status.OUT_OF_SERVICE } -> Status.OUT_OF_SERVICE
            statuses.any { it.code == "DEGRADED" } -> Status("DEGRADED")
            statuses.all { it == Status.UP } -> Status.UP
            else -> Status.UNKNOWN
        }
    }

    private fun createHealthSummary(results: Map<String, Health>): Map<String, Any> {
        val statusCounts = results.values
            .groupBy { it.status.code }
            .mapValues { it.value.size }

        val criticalComponents = listOf("database", "redis", "business")
        val criticalHealth = criticalComponents.associateWith { component ->
            results[component]?.status?.code ?: "UNKNOWN"
        }

        return mapOf(
            "overallStatus" to determineOverallStatus(results).code,
            "totalComponents" to results.size,
            "statusBreakdown" to statusCounts,
            "criticalComponents" to criticalHealth,
            "healthyComponents" to results.count { it.value.status == Status.UP },
            "unhealthyComponents" to results.count { it.value.status != Status.UP },
            "criticalComponentsHealthy" to criticalComponents.all {
                results[it]?.status == Status.UP
            }
        )
    }

    /**
     * Obtiene el estado de salud de un componente específico
     */
    fun getComponentHealth(componentName: String): Health? {
        return healthIndicators[componentName]?.health()
    }

    /**
     * Obtiene el estado de todos los componentes críticos
     */
    fun getCriticalComponentsHealth(): Map<String, Health> {
        val criticalComponents = listOf("database", "redis", "business", "externalServices")
        return criticalComponents.mapNotNull { component ->
            healthIndicators[component]?.let { indicator ->
                component to indicator.health()
            }
        }.toMap()
    }

    /**
     * Verifica si todos los componentes críticos están saludables
     */
    fun areCriticalComponentsHealthy(): Boolean {
        val criticalHealth = getCriticalComponentsHealth()
        return criticalHealth.values.all { it.status == Status.UP }
    }

    /**
     * Obtiene un resumen rápido del estado de salud
     */
    fun getHealthSummary(): HealthSummary {
        val results = mutableMapOf<String, Health>()

        for ((name, indicator) in healthIndicators) {
            try {
                results[name] = indicator.health()
            } catch (e: Exception) {
                results[name] = Health.down().build()
            }
        }

        val overallStatus = determineOverallStatus(results)
        val summary = createHealthSummary(results)

        return HealthSummary(
            overallStatus = overallStatus.code,
            totalComponents = results.size,
            healthyComponents = results.count { it.value.status == Status.UP },
            unhealthyComponents = results.count { it.value.status != Status.UP },
            criticalComponentsHealthy = summary["criticalComponentsHealthy"] as Boolean,
            timestamp = Instant.now()
        )
    }
}

/**
 * Resumen de salud del sistema
 */
data class HealthSummary(
    val overallStatus: String,
    val totalComponents: Int,
    val healthyComponents: Int,
    val unhealthyComponents: Int,
    val criticalComponentsHealthy: Boolean,
    val timestamp: Instant
)