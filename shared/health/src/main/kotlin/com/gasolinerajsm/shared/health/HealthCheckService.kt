package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.Status
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Servicio central para gestión de health checks
 */
@Service
class HealthCheckService(
    private val properties: HealthProperties
) {

    private val scheduledExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val healthHistory = ConcurrentHashMap<String, MutableList<HealthRecord>>()
    private val alertThresholds = ConcurrentHashMap<String, AlertThreshold>()

    init {
        // Configurar alertas por defecto
        setupDefaultAlertThresholds()

        // Iniciar limpieza periódica del historial
        startHistoryCleanup()
    }

    /**
     * Registra un resultado de health check
     */
    fun recordHealthCheck(componentName: String, health: Health) {
        val record = HealthRecord(
            componentName = componentName,
            status = health.status.code,
            timestamp = Instant.now(),
            details = health.details
        )

        healthHistory.computeIfAbsent(componentName) { mutableListOf() }.add(record)

        // Mantener solo los últimos 100 registros por componente
        val records = healthHistory[componentName]!!
        if (records.size > 100) {
            records.removeAt(0)
        }

        // Verificar alertas
        checkAlerts(componentName, record)
    }

    /**
     * Obtiene el historial de health checks de un componente
     */
    fun getHealthHistory(componentName: String, limit: Int = 50): List<HealthRecord> {
        return healthHistory[componentName]
            ?.takeLast(limit)
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    /**
     * Obtiene estadísticas de disponibilidad de un componente
     */
    fun getAvailabilityStats(componentName: String, period: Duration = Duration.ofHours(24)): AvailabilityStats {
        val cutoffTime = Instant.now().minus(period)
        val records = healthHistory[componentName]
            ?.filter { it.timestamp.isAfter(cutoffTime) }
            ?: emptyList()

        if (records.isEmpty()) {
            return AvailabilityStats(
                componentName = componentName,
                period = period,
                totalChecks = 0,
                successfulChecks = 0,
                failedChecks = 0,
                availabilityPercentage = 100.0,
                averageResponseTime = Duration.ZERO,
                lastFailure = null
            )
        }

        val successfulChecks = records.count { it.status == "UP" }
        val failedChecks = records.size - successfulChecks
        val availabilityPercentage = (successfulChecks.toDouble() / records.size) * 100

        val responseTimes = records.mapNotNull { record ->
            record.details["responseTimeMs"]?.toString()?.toLongOrNull()?.let { Duration.ofMillis(it) }
        }
        val averageResponseTime = if (responseTimes.isNotEmpty()) {
            Duration.ofMillis(responseTimes.map { it.toMillis() }.average().toLong())
        } else {
            Duration.ZERO
        }

        val lastFailure = records
            .filter { it.status != "UP" }
            .maxByOrNull { it.timestamp }

        return AvailabilityStats(
            componentName = componentName,
            period = period,
            totalChecks = records.size,
            successfulChecks = successfulChecks,
            failedChecks = failedChecks,
            availabilityPercentage = availabilityPercentage,
            averageResponseTime = averageResponseTime,
            lastFailure = lastFailure
        )
    }

    /**
     * Obtiene el estado actual de todos los componentes
     */
    fun getCurrentStatus(): Map<String, ComponentStatus> {
        return healthHistory.mapValues { (componentName, records) ->
            val latestRecord = records.lastOrNull()
            ComponentStatus(
                componentName = componentName,
                currentStatus = latestRecord?.status ?: "UNKNOWN",
                lastCheckTime = latestRecord?.timestamp,
                isHealthy = latestRecord?.status == "UP",
                consecutiveFailures = getConsecutiveFailures(componentName)
            )
        }
    }

    /**
     * Configura un threshold de alerta para un componente
     */
    fun setAlertThreshold(
        componentName: String,
        maxConsecutiveFailures: Int = 3,
        maxFailureRate: Double = 0.1, // 10%
        timeWindow: Duration = Duration.ofMinutes(5)
    ) {
        alertThresholds[componentName] = AlertThreshold(
            componentName = componentName,
            maxConsecutiveFailures = maxConsecutiveFailures,
            maxFailureRate = maxFailureRate,
            timeWindow = timeWindow
        )
    }

    /**
     * Obtiene alertas activas
     */
    fun getActiveAlerts(): List<HealthAlert> {
        val alerts = mutableListOf<HealthAlert>()
        val currentTime = Instant.now()

        for ((componentName, threshold) in alertThresholds) {
            val records = healthHistory[componentName] ?: continue

            // Verificar fallos consecutivos
            val consecutiveFailures = getConsecutiveFailures(componentName)
            if (consecutiveFailures >= threshold.maxConsecutiveFailures) {
                alerts.add(
                    HealthAlert(
                        componentName = componentName,
                        alertType = AlertType.CONSECUTIVE_FAILURES,
                        message = "Component has $consecutiveFailures consecutive failures",
                        severity = AlertSeverity.HIGH,
                        timestamp = currentTime,
                        details = mapOf(
                            "consecutiveFailures" to consecutiveFailures,
                            "threshold" to threshold.maxConsecutiveFailures
                        )
                    )
                )
            }

            // Verificar tasa de fallos en ventana de tiempo
            val windowStart = currentTime.minus(threshold.timeWindow)
            val windowRecords = records.filter { it.timestamp.isAfter(windowStart) }
            if (windowRecords.isNotEmpty()) {
                val failureRate = windowRecords.count { it.status != "UP" }.toDouble() / windowRecords.size
                if (failureRate > threshold.maxFailureRate) {
                    alerts.add(
                        HealthAlert(
                            componentName = componentName,
                            alertType = AlertType.HIGH_FAILURE_RATE,
                            message = "Component has high failure rate: ${(failureRate * 100).toInt()}%",
                            severity = AlertSeverity.MEDIUM,
                            timestamp = currentTime,
                            details = mapOf(
                                "failureRate" to failureRate,
                                "threshold" to threshold.maxFailureRate,
                                "windowMinutes" to threshold.timeWindow.toMinutes()
                            )
                        )
                    )
                }
            }
        }

        return alerts
    }

    /**
     * Genera un reporte de salud completo
     */
    fun generateHealthReport(period: Duration = Duration.ofHours(24)): HealthReport {
        val currentTime = Instant.now()
        val componentStats = healthHistory.keys.associateWith { componentName ->
            getAvailabilityStats(componentName, period)
        }

        val overallAvailability = if (componentStats.isNotEmpty()) {
            componentStats.values.map { it.availabilityPercentage }.average()
        } else {
            100.0
        }

        val criticalComponents = listOf("database", "redis", "business")
        val criticalAvailability = criticalComponents.mapNotNull { componentName ->
            componentStats[componentName]?.availabilityPercentage
        }.let { availabilities ->
            if (availabilities.isNotEmpty()) availabilities.average() else 100.0
        }

        return HealthReport(
            reportPeriod = period,
            generatedAt = currentTime,
            overallAvailability = overallAvailability,
            criticalComponentsAvailability = criticalAvailability,
            componentStats = componentStats,
            activeAlerts = getActiveAlerts(),
            totalHealthChecks = componentStats.values.sumOf { it.totalChecks },
            totalFailures = componentStats.values.sumOf { it.failedChecks }
        )
    }

    private fun setupDefaultAlertThresholds() {
        // Componentes críticos con thresholds más estrictos
        setAlertThreshold("database", 2, 0.05, Duration.ofMinutes(3))
        setAlertThreshold("redis", 2, 0.05, Duration.ofMinutes(3))
        setAlertThreshold("business", 3, 0.1, Duration.ofMinutes(5))

        // Componentes menos críticos
        setAlertThreshold("externalServices", 5, 0.2, Duration.ofMinutes(10))
        setAlertThreshold("systemResources", 3, 0.15, Duration.ofMinutes(5))
    }

    private fun getConsecutiveFailures(componentName: String): Int {
        val records = healthHistory[componentName] ?: return 0
        var consecutiveFailures = 0

        for (i in records.size - 1 downTo 0) {
            if (records[i].status != "UP") {
                consecutiveFailures++
            } else {
                break
            }
        }

        return consecutiveFailures
    }

    private fun checkAlerts(componentName: String, record: HealthRecord) {
        // Esta función podría expandirse para enviar notificaciones
        // Por ahora solo registra las alertas para consulta posterior
        val threshold = alertThresholds[componentName] ?: return

        if (record.status != "UP") {
            val consecutiveFailures = getConsecutiveFailures(componentName)
            if (consecutiveFailures >= threshold.maxConsecutiveFailures) {
                // Aquí se podría integrar con un sistema de notificaciones
                println("ALERT: Component $componentName has $consecutiveFailures consecutive failures")
            }
        }
    }

    private fun startHistoryCleanup() {
        scheduledExecutor.scheduleAtFixedRate({
            try {
                val cutoffTime = Instant.now().minus(Duration.ofDays(7))
                healthHistory.values.forEach { records ->
                    records.removeIf { it.timestamp.isBefore(cutoffTime) }
                }
            } catch (e: Exception) {
                println("Error during health history cleanup: ${e.message}")
            }
        }, 1, 24, TimeUnit.HOURS)
    }
}

/**
 * Registro de un health check
 */
data class HealthRecord(
    val componentName: String,
    val status: String,
    val timestamp: Instant,
    val details: Map<String, Any>
)

/**
 * Estadísticas de disponibilidad de un componente
 */
data class AvailabilityStats(
    val componentName: String,
    val period: Duration,
    val totalChecks: Int,
    val successfulChecks: Int,
    val failedChecks: Int,
    val availabilityPercentage: Double,
    val averageResponseTime: Duration,
    val lastFailure: HealthRecord?
)

/**
 * Estado actual de un componente
 */
data class ComponentStatus(
    val componentName: String,
    val currentStatus: String,
    val lastCheckTime: Instant?,
    val isHealthy: Boolean,
    val consecutiveFailures: Int
)

/**
 * Threshold de alerta para un componente
 */
data class AlertThreshold(
    val componentName: String,
    val maxConsecutiveFailures: Int,
    val maxFailureRate: Double,
    val timeWindow: Duration
)

/**
 * Alerta de salud
 */
data class HealthAlert(
    val componentName: String,
    val alertType: AlertType,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Instant,
    val details: Map<String, Any>
)

/**
 * Tipos de alertas
 */
enum class AlertType {
    CONSECUTIVE_FAILURES,
    HIGH_FAILURE_RATE,
    RESPONSE_TIME_DEGRADATION,
    RESOURCE_THRESHOLD_EXCEEDED
}

/**
 * Severidad de alertas
 */
enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Reporte completo de salud
 */
data class HealthReport(
    val reportPeriod: Duration,
    val generatedAt: Instant,
    val overallAvailability: Double,
    val criticalComponentsAvailability: Double,
    val componentStats: Map<String, AvailabilityStats>,
    val activeAlerts: List<HealthAlert>,
    val totalHealthChecks: Int,
    val totalFailures: Int
)