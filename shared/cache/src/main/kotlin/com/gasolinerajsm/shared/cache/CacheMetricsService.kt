package com.gasolinerajsm.shared.cache

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Servicio para métricas y monitoreo avanzado de cache
 */
@Service
class CacheMetricsService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val cacheManager: CacheManager,
    private val meterRegistry: MeterRegistry? = null
) {

    private val logger = LoggerFactory.getLogger(CacheMetricsService::class.java)
    private val cacheMetrics = ConcurrentHashMap<String, CacheMetrics>()
    private val performanceHistory = ConcurrentHashMap<String, MutableList<PerformanceSnapshot>>()

    /**
     * Registra métricas de operación de cache
     */
    fun recordCacheOperation(
        cacheName: String,
        operation: CacheOperation,
        duration: Duration,
        success: Boolean = true
    ) {
        val metrics = cacheMetrics.computeIfAbsent(cacheName) { CacheMetrics(cacheName) }

        when (operation) {
            CacheOperation.GET -> {
                metrics.totalGets.incrementAndGet()
                if (success) metrics.hits.incrementAndGet() else metrics.misses.incrementAndGet()
            }
            CacheOperation.PUT -> {
                metrics.totalPuts.incrementAndGet()
                if (!success) metrics.putErrors.incrementAndGet()
            }
            CacheOperation.EVICT -> {
                metrics.totalEvictions.incrementAndGet()
                if (!success) metrics.evictionErrors.incrementAndGet()
            }
            CacheOperation.CLEAR -> {
                metrics.totalClears.incrementAndGet()
            }
        }

        // Registrar duración
        metrics.totalOperationTime.addAndGet(duration.toMillis())

        // Registrar en Micrometer si está disponible
        meterRegistry?.let { registry ->
            registry.timer(
                "cache.operation.duration",
                "cache", cacheName,
                "operation", operation.name.lowercase(),
                "success", success.toString()
            ).record(duration.toMillis(), TimeUnit.MILLISECONDS)

            registry.counter(
                "cache.operation.count",
                "cache", cacheName,
                "operation", operation.name.lowercase(),
                "success", success.toString()
            ).increment()
        }
    }

    /**
     * Obtiene métricas de un cache específico
     */
    fun getCacheMetrics(cacheName: String): CacheMetrics? {
        return cacheMetrics[cacheName]
    }

    /**
     * Obtiene métricas de todos los caches
     */
    fun getAllCacheMetrics(): Map<String, CacheMetrics> {
        return cacheMetrics.toMap()
    }

    /**
     * Obtiene métricas agregadas del sistema
     */
    fun getSystemMetrics(): SystemCacheMetrics {
        val allMetrics = cacheMetrics.values

        return SystemCacheMetrics(
            totalCaches = allMetrics.size,
            totalHits = allMetrics.sumOf { it.hits.get() },
            totalMisses = allMetrics.sumOf { it.misses.get() },
            totalPuts = allMetrics.sumOf { it.totalPuts.get() },
            totalEvictions = allMetrics.sumOf { it.totalEvictions.get() },
            totalErrors = allMetrics.sumOf { it.putErrors.get() + it.evictionErrors.get() },
            averageHitRate = calculateAverageHitRate(),
            totalMemoryUsage = calculateTotalMemoryUsage(),
            healthStatus = calculateHealthStatus()
        )
    }

    /**
     * Obtiene historial de rendimiento
     */
    fun getPerformanceHistory(cacheName: String): List<PerformanceSnapshot> {
        return performanceHistory[cacheName]?.toList() ?: emptyList()
    }

    /**
     * Obtiene análisis de rendimiento
     */
    fun getPerformanceAnalysis(cacheName: String): PerformanceAnalysis {
        val metrics = cacheMetrics[cacheName] ?: return PerformanceAnalysis.empty(cacheName)
        val history = performanceHistory[cacheName] ?: emptyList()

        return PerformanceAnalysis(
            cacheName = cacheName,
            currentHitRate = metrics.getHitRate(),
            averageHitRate = history.map { it.hitRate }.average().takeIf { !it.isNaN() } ?: 0.0,
            hitRateTrend = calculateHitRateTrend(history),
            averageResponseTime = calculateAverageResponseTime(metrics),
            responseTimeTrend = calculateResponseTimeTrend(history),
            memoryEfficiency = calculateMemoryEfficiency(cacheName),
            recommendations = generateRecommendations(cacheName, metrics, history)
        )
    }

    /**
     * Detecta anomalías en el rendimiento
     */
    fun detectAnomalies(): List<CacheAnomaly> {
        val anomalies = mutableListOf<CacheAnomaly>()

        cacheMetrics.forEach { (cacheName, metrics) ->
            // Detectar baja tasa de aciertos
            val hitRate = metrics.getHitRate()
            if (hitRate < 0.5) {
                anomalies.add(
                    CacheAnomaly(
                        cacheName = cacheName,
                        type = AnomalyType.LOW_HIT_RATE,
                        severity = if (hitRate < 0.2) Severity.HIGH else Severity.MEDIUM,
                        description = "Hit rate is ${String.format("%.2f", hitRate * 100)}%",
                        detectedAt = Instant.now()
                    )
                )
            }

            // Detectar alta tasa de errores
            val errorRate = metrics.getErrorRate()
            if (errorRate > 0.05) {
                anomalies.add(
                    CacheAnomaly(
                        cacheName = cacheName,
                        type = AnomalyType.HIGH_ERROR_RATE,
                        severity = if (errorRate > 0.1) Severity.HIGH else Severity.MEDIUM,
                        description = "Error rate is ${String.format("%.2f", errorRate * 100)}%",
                        detectedAt = Instant.now()
                    )
                )
            }

            // Detectar tiempo de respuesta alto
            val avgResponseTime = calculateAverageResponseTime(metrics)
            if (avgResponseTime > Duration.ofMillis(100)) {
                anomalies.add(
                    CacheAnomaly(
                        cacheName = cacheName,
                        type = AnomalyType.HIGH_RESPONSE_TIME,
                        severity = if (avgResponseTime > Duration.ofMillis(500)) Severity.HIGH else Severity.MEDIUM,
                        description = "Average response time is ${avgResponseTime.toMillis()}ms",
                        detectedAt = Instant.now()
                    )
                )
            }
        }

        return anomalies
    }

    /**
     * Genera reporte de salud del cache
     */
    fun generateHealthReport(): CacheHealthReport {
        val systemMetrics = getSystemMetrics()
        val anomalies = detectAnomalies()
        val recommendations = generateSystemRecommendations()

        return CacheHealthReport(
            timestamp = Instant.now(),
            overallHealth = systemMetrics.healthStatus,
            systemMetrics = systemMetrics,
            cacheMetrics = getAllCacheMetrics(),
            anomalies = anomalies,
            recommendations = recommendations
        )
    }

    /**
     * Tarea programada para capturar snapshots de rendimiento
     */
    @Scheduled(fixedRate = 60000) // Cada minuto
    fun capturePerformanceSnapshot() {
        cacheMetrics.forEach { (cacheName, metrics) ->
            val snapshot = PerformanceSnapshot(
                timestamp = Instant.now(),
                hitRate = metrics.getHitRate(),
                missRate = metrics.getMissRate(),
                averageResponseTime = calculateAverageResponseTime(metrics),
                operationsPerSecond = calculateOperationsPerSecond(metrics),
                memoryUsage = getCacheMemoryUsage(cacheName),
                errorRate = metrics.getErrorRate()
            )

            val history = performanceHistory.computeIfAbsent(cacheName) { mutableListOf() }
            history.add(snapshot)

            // Mantener solo las últimas 24 horas de datos (1440 minutos)
            if (history.size > 1440) {
                history.removeAt(0)
            }
        }
    }

    /**
     * Limpia métricas antiguas
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM diariamente
    fun cleanupOldMetrics() {
        val cutoffTime = Instant.now().minus(Duration.ofDays(7))

        performanceHistory.values.forEach { history ->
            history.removeIf { it.timestamp.isBefore(cutoffTime) }
        }

        logger.info("Cleaned up old cache metrics")
    }

    // Métodos auxiliares

    private fun calculateAverageHitRate(): Double {
        val allMetrics = cacheMetrics.values
        if (allMetrics.isEmpty()) return 0.0

        return allMetrics.map { it.getHitRate() }.average()
    }

    private fun calculateTotalMemoryUsage(): Long {
        return cacheMetrics.keys.sumOf { getCacheMemoryUsage(it) }
    }

    private fun calculateHealthStatus(): HealthStatus {
        val avgHitRate = calculateAverageHitRate()
        val anomalies = detectAnomalies()

        return when {
            anomalies.any { it.severity == Severity.HIGH } -> HealthStatus.UNHEALTHY
            avgHitRate < 0.5 || anomalies.any { it.severity == Severity.MEDIUM } -> HealthStatus.DEGRADED
            else -> HealthStatus.HEALTHY
        }
    }

    private fun calculateAverageResponseTime(metrics: CacheMetrics): Duration {
        val totalOps = metrics.totalGets.get() + metrics.totalPuts.get()
        return if (totalOps > 0) {
            Duration.ofMillis(metrics.totalOperationTime.get() / totalOps)
        } else {
            Duration.ZERO
        }
    }

    private fun calculateOperationsPerSecond(metrics: CacheMetrics): Double {
        // Implementar cálculo basado en ventana de tiempo
        return 0.0 // Placeholder
    }

    private fun getCacheMemoryUsage(cacheName: String): Long {
        return try {
            // Implementar cálculo de uso de memoria específico para Redis
            0L // Placeholder
        } catch (e: Exception) {
            logger.warn("Error calculating memory usage for cache: $cacheName", e)
            0L
        }
    }

    private fun calculateHitRateTrend(history: List<PerformanceSnapshot>): Trend {
        if (history.size < 2) return Trend.STABLE

        val recent = history.takeLast(10).map { it.hitRate }
        val older = history.dropLast(10).takeLast(10).map { it.hitRate }

        if (recent.isEmpty() || older.isEmpty()) return Trend.STABLE

        val recentAvg = recent.average()
        val olderAvg = older.average()

        return when {
            recentAvg > olderAvg + 0.05 -> Trend.IMPROVING
            recentAvg < olderAvg - 0.05 -> Trend.DEGRADING
            else -> Trend.STABLE
        }
    }

    private fun calculateResponseTimeTrend(history: List<PerformanceSnapshot>): Trend {
        if (history.size < 2) return Trend.STABLE

        val recent = history.takeLast(10).map { it.averageResponseTime.toMillis() }
        val older = history.dropLast(10).takeLast(10).map { it.averageResponseTime.toMillis() }

        if (recent.isEmpty() || older.isEmpty()) return Trend.STABLE

        val recentAvg = recent.average()
        val olderAvg = older.average()

        return when {
            recentAvg < olderAvg - 10 -> Trend.IMPROVING
            recentAvg > olderAvg + 10 -> Trend.DEGRADING
            else -> Trend.STABLE
        }
    }

    private fun calculateMemoryEfficiency(cacheName: String): Double {
        // Implementar cálculo de eficiencia de memoria
        return 0.8 // Placeholder
    }

    private fun generateRecommendations(
        cacheName: String,
        metrics: CacheMetrics,
        history: List<PerformanceSnapshot>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        val hitRate = metrics.getHitRate()
        if (hitRate < 0.5) {
            recommendations.add("Consider reviewing cache TTL settings - low hit rate detected")
        }

        val errorRate = metrics.getErrorRate()
        if (errorRate > 0.05) {
            recommendations.add("Investigate cache errors - high error rate detected")
        }

        val avgResponseTime = calculateAverageResponseTime(metrics)
        if (avgResponseTime > Duration.ofMillis(100)) {
            recommendations.add("Consider optimizing cache serialization - high response time detected")
        }

        return recommendations
    }

    private fun generateSystemRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val systemMetrics = getSystemMetrics()

        if (systemMetrics.averageHitRate < 0.6) {
            recommendations.add("Overall cache hit rate is low - review caching strategy")
        }

        if (systemMetrics.totalErrors > systemMetrics.totalPuts * 0.05) {
            recommendations.add("High error rate detected - check Redis connectivity and configuration")
        }

        return recommendations
    }
}

// Clases de datos para métricas

data class CacheMetrics(
    val cacheName: String,
    val hits: AtomicLong = AtomicLong(0),
    val misses: AtomicLong = AtomicLong(0),
    val totalGets: AtomicLong = AtomicLong(0),
    val totalPuts: AtomicLong = AtomicLong(0),
    val totalEvictions: AtomicLong = AtomicLong(0),
    val totalClears: AtomicLong = AtomicLong(0),
    val putErrors: AtomicLong = AtomicLong(0),
    val evictionErrors: AtomicLong = AtomicLong(0),
    val totalOperationTime: AtomicLong = AtomicLong(0)
) {
    fun getHitRate(): Double {
        val total = totalGets.get()
        return if (total > 0) hits.get().toDouble() / total else 0.0
    }

    fun getMissRate(): Double = 1.0 - getHitRate()

    fun getErrorRate(): Double {
        val totalOps = totalPuts.get() + totalEvictions.get()
        val totalErrors = putErrors.get() + evictionErrors.get()
        return if (totalOps > 0) totalErrors.toDouble() / totalOps else 0.0
    }
}

data class SystemCacheMetrics(
    val totalCaches: Int,
    val totalHits: Long,
    val totalMisses: Long,
    val totalPuts: Long,
    val totalEvictions: Long,
    val totalErrors: Long,
    val averageHitRate: Double,
    val totalMemoryUsage: Long,
    val healthStatus: HealthStatus
)

data class PerformanceSnapshot(
    val timestamp: Instant,
    val hitRate: Double,
    val missRate: Double,
    val averageResponseTime: Duration,
    val operationsPerSecond: Double,
    val memoryUsage: Long,
    val errorRate: Double
)

data class PerformanceAnalysis(
    val cacheName: String,
    val currentHitRate: Double,
    val averageHitRate: Double,
    val hitRateTrend: Trend,
    val averageResponseTime: Duration,
    val responseTimeTrend: Trend,
    val memoryEfficiency: Double,
    val recommendations: List<String>
) {
    companion object {
        fun empty(cacheName: String) = PerformanceAnalysis(
            cacheName, 0.0, 0.0, Trend.STABLE, Duration.ZERO, Trend.STABLE, 0.0, emptyList()
        )
    }
}

data class CacheAnomaly(
    val cacheName: String,
    val type: AnomalyType,
    val severity: Severity,
    val description: String,
    val detectedAt: Instant
)

data class CacheHealthReport(
    val timestamp: Instant,
    val overallHealth: HealthStatus,
    val systemMetrics: SystemCacheMetrics,
    val cacheMetrics: Map<String, CacheMetrics>,
    val anomalies: List<CacheAnomaly>,
    val recommendations: List<String>
)

enum class CacheOperation {
    GET, PUT, EVICT, CLEAR
}

enum class HealthStatus {
    HEALTHY, DEGRADED, UNHEALTHY
}

enum class Trend {
    IMPROVING, STABLE, DEGRADING
}

enum class AnomalyType {
    LOW_HIT_RATE, HIGH_ERROR_RATE, HIGH_RESPONSE_TIME, MEMORY_LEAK
}

enum class Severity {
    LOW, MEDIUM, HIGH
}