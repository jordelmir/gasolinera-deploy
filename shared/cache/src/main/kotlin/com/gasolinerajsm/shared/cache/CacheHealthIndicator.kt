package com.gasolinerajsm.shared.cache

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Health indicator para el sistema de caching
 */
@Component("cacheHealthIndicator")
class CacheHealthIndicator(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val cacheMetricsService: CacheMetricsService
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val healthBuilder = Health.up()

            // Verificar conectividad con Redis
            val redisHealth = checkRedisHealth()
            healthBuilder.withDetail("redis", redisHealth)

            // Verificar métricas del sistema de cache
            val systemMetrics = cacheMetricsService.getSystemMetrics()
            healthBuilder.withDetail("systemMetrics", mapOf(
                "totalCaches" to systemMetrics.totalCaches,
                "averageHitRate" to String.format("%.2f%%", systemMetrics.averageHitRate * 100),
                "totalMemoryUsage" to "${systemMetrics.totalMemoryUsage / 1024 / 1024}MB",
                "healthStatus" to systemMetrics.healthStatus.name
            ))

            // Verificar anomalías
            val anomalies = cacheMetricsService.detectAnomalies()
            if (anomalies.isNotEmpty()) {
                healthBuilder.withDetail("anomalies", anomalies.map { anomaly ->
                    mapOf(
                        "cache" to anomaly.cacheName,
                        "type" to anomaly.type.name,
                        "severity" to anomaly.severity.name,
                        "description" to anomaly.description
                    )
                })

                // Si hay anomalías de alta severidad, marcar como degradado
                if (anomalies.any { it.severity == Severity.HIGH }) {
                    healthBuilder.down()
                } else {
                    healthBuilder.unknown()
                }
            }

            // Verificar rendimiento de caches individuales
            val cacheDetails = mutableMapOf<String, Any>()
            cacheMetricsService.getAllCacheMetrics().forEach { (cacheName, metrics) ->
                cacheDetails[cacheName] = mapOf(
                    "hitRate" to String.format("%.2f%%", metrics.getHitRate() * 100),
                    "totalOperations" to metrics.totalGets.get() + metrics.totalPuts.get(),
                    "errorRate" to String.format("%.2f%%", metrics.getErrorRate() * 100)
                )
            }
            healthBuilder.withDetail("caches", cacheDetails)

            // Determinar estado final basado en métricas del sistema
            when (systemMetrics.healthStatus) {
                HealthStatus.UNHEALTHY -> healthBuilder.down()
                HealthStatus.DEGRADED -> healthBuilder.unknown()
                HealthStatus.HEALTHY -> healthBuilder.up()
            }

            healthBuilder.build()

        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message)
                .withDetail("timestamp", Instant.now().toString())
                .build()
        }
    }

    private fun checkRedisHealth(): Map<String, Any> {
        return try {
            val startTime = Instant.now()

            // Test básico de conectividad
            redisTemplate.opsForValue().set("health:check", "test", Duration.ofSeconds(10))
            val value = redisTemplate.opsForValue().get("health:check")
            redisTemplate.delete("health:check")

            val responseTime = Duration.between(startTime, Instant.now())

            if (value == "test") {
                mapOf(
                    "status" to "UP",
                    "responseTime" to "${responseTime.toMillis()}ms",
                    "connection" to "OK"
                )
            } else {
                mapOf(
                    "status" to "DOWN",
                    "error" to "Redis connectivity test failed",
                    "responseTime" to "${responseTime.toMillis()}ms"
                )
            }
        } catch (e: Exception) {
            mapOf(
                "status" to "DOWN",
                "error" to e.message,
                "connection" to "FAILED"
            )
        }
    }
}