package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Health indicator para operaciones críticas de negocio
 */
@Component
class BusinessHealthIndicator(
    private val properties: HealthProperties.BusinessHealthProperties
) : HealthIndicator {

    private val operationMetrics = ConcurrentHashMap<String, OperationMetrics>()
    private val lastHealthCheck = AtomicLong(System.currentTimeMillis())

    override fun health(): Health {
        if (!properties.enabled) {
            return Health.up()
                .withDetail("status", "disabled")
                .build()
        }

        return try {
            val currentTime = Instant.now()
            lastHealthCheck.set(currentTime.toEpochMilli())

            // Verificar operaciones críticas
            val criticalOperationsHealth = checkCriticalOperations()

            // Verificar tasas de error
            val errorRateHealth = checkErrorRates()

            // Verificar tiempos de respuesta
            val responseTimeHealth = checkResponseTimes()

            // Determinar salud general
            val isHealthy = criticalOperationsHealth.isHealthy &&
                           errorRateHealth.isHealthy &&
                           responseTimeHealth.isHealthy

            val healthBuilder = if (isHealthy) Health.up() else Health.down()

            healthBuilder
                .withDetail("businessOperations", "Gasolinera JSM")
                .withDetail("criticalOperations", criticalOperationsHealth.details)
                .withDetail("errorRates", errorRateHealth.details)
                .withDetail("responseTimes", responseTimeHealth.details)
                .withDetail("overallHealth", mapOf(
                    "isHealthy" to isHealthy,
                    "criticalOperationsOk" to criticalOperationsHealth.isHealthy,
                    "errorRatesOk" to errorRateHealth.isHealthy,
                    "responseTimesOk" to responseTimeHealth.isHealthy
                ))
                .withDetail("metrics", getOperationsSummary())
                .withDetail("timestamp", currentTime.toString())
                .build()

        } catch (e: Exception) {
            Health.down()
                .withDetail("businessOperations", "Gasolinera JSM")
                .withDetail("error", e.message ?: "Unknown error")
                .withDetail("errorType", e.javaClass.simpleName)
                .withDetail("timestamp", Instant.now().toString())
                .withException(e)
                .build()
        }
    }

    /**
     * Registra una operación de negocio
     */
    fun recordOperation(
        operationType: BusinessOperationType,
        duration: Duration,
        success: Boolean,
        userId: String? = null
    ) {
        val metrics = operationMetrics.computeIfAbsent(operationType.name) {
            OperationMetrics(operationType.name)
        }

        metrics.recordOperation(duration, success, userId)
    }

    /**
     * Registra el inicio de una operación crítica
     */
    fun startCriticalOperation(operationType: BusinessOperationType): OperationContext {
        return OperationContext(operationType, Instant.now())
    }

    /**
     * Completa una operación crítica
     */
    fun completeCriticalOperation(context: OperationContext, success: Boolean, userId: String? = null) {
        val duration = Duration.between(context.startTime, Instant.now())
        recordOperation(context.operationType, duration, success, userId)
    }

    private fun checkCriticalOperations(): HealthCheckResult {
        if (!properties.checkCriticalOperations) {
            return HealthCheckResult(true, mapOf("status" to "disabled"))
        }

        val criticalOperations = BusinessOperationType.values().filter { it.isCritical }
        val results = mutableMapOf<String, Any>()
        var allHealthy = true

        for (operation in criticalOperations) {
            val metrics = operationMetrics[operation.name]
            if (metrics != null) {
                val recentFailures = metrics.getRecentFailures(Duration.ofMinutes(5))
                val isOperationHealthy = recentFailures < 3 // Máximo 3 fallos en 5 minutos

                results[operation.name] = mapOf(
                    "isHealthy" to isOperationHealthy,
                    "recentFailures" to recentFailures,
                    "totalOperations" to metrics.totalOperations.get(),
                    "successRate" to metrics.getSuccessRate()
                )

                if (!isOperationHealthy) {
                    allHealthy = false
                }
            } else {
                results[operation.name] = mapOf(
                    "isHealthy" to true,
                    "status" to "no_recent_operations"
                )
            }
        }

        return HealthCheckResult(allHealthy, results)
    }

    private fun checkErrorRates(): HealthCheckResult {
        val results = mutableMapOf<String, Any>()
        var allHealthy = true

        for ((operationName, metrics) in operationMetrics) {
            val errorRate = metrics.getErrorRate()
            val isHealthy = errorRate <= properties.errorRateThreshold

            results[operationName] = mapOf(
                "errorRate" to errorRate,
                "threshold" to properties.errorRateThreshold,
                "isHealthy" to isHealthy,
                "totalOperations" to metrics.totalOperations.get(),
                "failedOperations" to metrics.failedOperations.get()
            )

            if (!isHealthy) {
                allHealthy = false
            }
        }

        return HealthCheckResult(allHealthy, results)
    }

    private fun checkResponseTimes(): HealthCheckResult {
        val results = mutableMapOf<String, Any>()
        var allHealthy = true

        for ((operationName, metrics) in operationMetrics) {
            val avgResponseTime = metrics.getAverageResponseTime()
            val isHealthy = avgResponseTime <= properties.responseTimeThreshold

            results[operationName] = mapOf(
                "averageResponseTimeMs" to avgResponseTime.toMillis(),
                "thresholdMs" to properties.responseTimeThreshold.toMillis(),
                "isHealthy" to isHealthy,
                "minResponseTimeMs" to metrics.getMinResponseTime().toMillis(),
                "maxResponseTimeMs" to metrics.getMaxResponseTime().toMillis()
            )

            if (!isHealthy) {
                allHealthy = false
            }
        }

        return HealthCheckResult(allHealthy, results)
    }

    private fun getOperationsSummary(): Map<String, Any> {
        return mapOf(
            "totalOperationTypes" to operationMetrics.size,
            "totalOperations" to operationMetrics.values.sumOf { it.totalOperations.get() },
            "totalSuccessfulOperations" to operationMetrics.values.sumOf { it.successfulOperations.get() },
            "totalFailedOperations" to operationMetrics.values.sumOf { it.failedOperations.get() },
            "overallSuccessRate" to calculateOverallSuccessRate(),
            "lastOperationTime" to operationMetrics.values
                .mapNotNull { it.lastOperationTime.get() }
                .maxOrNull()
                ?.let { Instant.ofEpochMilli(it).toString() }
        )
    }

    private fun calculateOverallSuccessRate(): Double {
        val totalOperations = operationMetrics.values.sumOf { it.totalOperations.get() }
        val successfulOperations = operationMetrics.values.sumOf { it.successfulOperations.get() }

        return if (totalOperations > 0) {
            successfulOperations.toDouble() / totalOperations
        } else {
            1.0
        }
    }

    /**
     * Obtiene métricas detalladas de una operación específica
     */
    fun getOperationMetrics(operationType: BusinessOperationType): OperationMetrics? {
        return operationMetrics[operationType.name]
    }

    /**
     * Limpia métricas antiguas (llamar periódicamente)
     */
    fun cleanupOldMetrics(olderThan: Duration = Duration.ofHours(24)) {
        val cutoffTime = System.currentTimeMillis() - olderThan.toMillis()

        operationMetrics.values.forEach { metrics ->
            metrics.cleanupOldOperations(cutoffTime)
        }
    }
}

/**
 * Tipos de operaciones de negocio críticas
 */
enum class BusinessOperationType(val isCritical: Boolean) {
    USER_REGISTRATION(true),
    USER_AUTHENTICATION(true),
    COUPON_GENERATION(true),
    COUPON_REDEMPTION(true),
    RAFFLE_PARTICIPATION(false),
    RAFFLE_DRAW(true),
    STATION_OPERATION(false),
    AD_ENGAGEMENT(false),
    PAYMENT_PROCESSING(true)
}

/**
 * Contexto de una operación en ejecución
 */
data class OperationContext(
    val operationType: BusinessOperationType,
    val startTime: Instant,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Resultado de un health check
 */
data class HealthCheckResult(
    val isHealthy: Boolean,
    val details: Map<String, Any>
)

/**
 * Métricas de una operación específica
 */
class OperationMetrics(val operationName: String) {
    val totalOperations = AtomicLong(0)
    val successfulOperations = AtomicLong(0)
    val failedOperations = AtomicLong(0)
    val lastOperationTime = AtomicLong(0)

    private val recentOperations = ConcurrentHashMap<Long, OperationRecord>()
    private val responseTimes = mutableListOf<Duration>()

    fun recordOperation(duration: Duration, success: Boolean, userId: String?) {
        val timestamp = System.currentTimeMillis()

        totalOperations.incrementAndGet()
        if (success) {
            successfulOperations.incrementAndGet()
        } else {
            failedOperations.incrementAndGet()
        }
        lastOperationTime.set(timestamp)

        // Registrar operación reciente
        recentOperations[timestamp] = OperationRecord(timestamp, duration, success, userId)

        // Mantener lista de tiempos de respuesta (últimos 100)
        synchronized(responseTimes) {
            responseTimes.add(duration)
            if (responseTimes.size > 100) {
                responseTimes.removeAt(0)
            }
        }

        // Limpiar operaciones muy antiguas
        cleanupOldOperations(timestamp - Duration.ofHours(1).toMillis())
    }

    fun getSuccessRate(): Double {
        val total = totalOperations.get()
        return if (total > 0) {
            successfulOperations.get().toDouble() / total
        } else {
            1.0
        }
    }

    fun getErrorRate(): Double {
        return 1.0 - getSuccessRate()
    }

    fun getRecentFailures(within: Duration): Int {
        val cutoffTime = System.currentTimeMillis() - within.toMillis()
        return recentOperations.values.count {
            it.timestamp >= cutoffTime && !it.success
        }
    }

    fun getAverageResponseTime(): Duration {
        synchronized(responseTimes) {
            return if (responseTimes.isNotEmpty()) {
                val avgMillis = responseTimes.map { it.toMillis() }.average()
                Duration.ofMillis(avgMillis.toLong())
            } else {
                Duration.ZERO
            }
        }
    }

    fun getMinResponseTime(): Duration {
        synchronized(responseTimes) {
            return responseTimes.minOrNull() ?: Duration.ZERO
        }
    }

    fun getMaxResponseTime(): Duration {
        synchronized(responseTimes) {
            return responseTimes.maxOrNull() ?: Duration.ZERO
        }
    }

    fun cleanupOldOperations(cutoffTime: Long) {
        recentOperations.entries.removeIf { it.key < cutoffTime }
    }
}

/**
 * Registro de una operación individual
 */
data class OperationRecord(
    val timestamp: Long,
    val duration: Duration,
    val success: Boolean,
    val userId: String?
)