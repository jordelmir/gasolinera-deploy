package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Health indicator personalizado para Redis con métricas avanzadas
 */
@Component
class CustomRedisHealthIndicator(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val properties: HealthProperties.RedisHealthProperties
) : HealthIndicator {

    override fun health(): Health {
        if (!properties.enabled) {
            return Health.up()
                .withDetail("status", "disabled")
                .build()
        }

        return try {
            val startTime = Instant.now()

            // Obtener conexión Redis
            val connection = redisConnectionFactory.connection

            try {
                // Test básico de conectividad
                val pingResult = connection.ping()
                val pingDuration = Duration.between(startTime, Instant.now())

                // Test de escritura/lectura
                val testKey = "${properties.testKey}:${UUID.randomUUID()}"
                val testValue = "health-check-${System.currentTimeMillis()}"

                val writeStartTime = Instant.now()
                connection.set(testKey.toByteArray(), testValue.toByteArray())
                val writeTime = Duration.between(writeStartTime, Instant.now())

                val readStartTime = Instant.now()
                val readValue = connection.get(testKey.toByteArray())
                val readTime = Duration.between(readStartTime, Instant.now())

                // Limpiar clave de test
                connection.del(testKey.toByteArray())

                // Obtener información del servidor
                val serverInfo = getRedisServerInfo(connection)

                // Verificar métricas de memoria
                val memoryUsage = serverInfo.memoryUsagePercentage
                val isMemoryHealthy = memoryUsage < properties.memoryThreshold

                val totalDuration = Duration.between(startTime, Instant.now())
                val isPerformanceGood = totalDuration < properties.timeout

                val healthBuilder = if (isMemoryHealthy && isPerformanceGood && readValue != null) {
                    Health.up()
                } else {
                    Health.down()
                }

                healthBuilder
                    .withDetail("redis", "Redis")
                    .withDetail("ping", pingResult ?: "PONG")
                    .withDetail("pingDurationMs", pingDuration.toMillis())
                    .withDetail("writeDurationMs", writeTime.toMillis())
                    .withDetail("readDurationMs", readTime.toMillis())
                    .withDetail("totalDurationMs", totalDuration.toMillis())
                    .withDetail("readWriteTest", readValue != null && String(readValue) == testValue)
                    .withDetail("server", mapOf(
                        "version" to serverInfo.version,
                        "mode" to serverInfo.mode,
                        "role" to serverInfo.role,
                        "connectedClients" to serverInfo.connectedClients,
                        "usedMemory" to serverInfo.usedMemoryHuman,
                        "memoryUsagePercentage" to serverInfo.memoryUsagePercentage,
                        "isMemoryHealthy" to isMemoryHealthy,
                        "memoryThreshold" to properties.memoryThreshold,
                        "uptime" to serverInfo.uptimeInSeconds
                    ))
                    .withDetail("performance", mapOf(
                        "isGood" to isPerformanceGood,
                        "timeoutMs" to properties.timeout.toMillis()
                    ))
                    .withDetail("timestamp", Instant.now().toString())
                    .build()

            } finally {
                connection.close()
            }

        } catch (e: Exception) {
            Health.down()
                .withDetail("redis", "Redis")
                .withDetail("error", e.message ?: "Unknown error")
                .withDetail("errorType", e.javaClass.simpleName)
                .withDetail("timestamp", Instant.now().toString())
                .withException(e)
                .build()
        }
    }

    private fun getRedisServerInfo(connection: org.springframework.data.redis.connection.RedisConnection): RedisServerInfo {
        return try {
            val info = connection.info()

            RedisServerInfo(
                version = extractInfoValue(info, "redis_version") ?: "unknown",
                mode = extractInfoValue(info, "redis_mode") ?: "standalone",
                role = extractInfoValue(info, "role") ?: "master",
                connectedClients = extractInfoValue(info, "connected_clients")?.toIntOrNull() ?: 0,
                usedMemory = extractInfoValue(info, "used_memory")?.toLongOrNull() ?: 0L,
                usedMemoryHuman = extractInfoValue(info, "used_memory_human") ?: "0B",
                maxMemory = extractInfoValue(info, "maxmemory")?.toLongOrNull() ?: 0L,
                uptimeInSeconds = extractInfoValue(info, "uptime_in_seconds")?.toLongOrNull() ?: 0L
            )
        } catch (e: Exception) {
            RedisServerInfo()
        }
    }

    private fun extractInfoValue(info: Properties, key: String): String? {
        return info.getProperty(key)
    }

    /**
     * Obtiene información detallada de salud de Redis
     */
    fun getDetailedHealth(): RedisHealthDetails {
        val startTime = Instant.now()

        return try {
            val connection = redisConnectionFactory.connection

            try {
                // Test de conectividad
                val pingResult = connection.ping()
                val pingDuration = Duration.between(startTime, Instant.now())

                // Test de performance con múltiples operaciones
                val performanceMetrics = runPerformanceTests(connection)

                // Información del servidor
                val serverInfo = getRedisServerInfo(connection)

                // Métricas de cluster (si aplica)
                val clusterInfo = getClusterInfo(connection)

                RedisHealthDetails(
                    isHealthy = true,
                    pingResult = pingResult,
                    pingDurationMs = pingDuration.toMillis(),
                    serverInfo = serverInfo,
                    performanceMetrics = performanceMetrics,
                    clusterInfo = clusterInfo,
                    timestamp = Instant.now()
                )
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            RedisHealthDetails(
                isHealthy = false,
                error = e.message,
                errorType = e.javaClass.simpleName,
                timestamp = Instant.now()
            )
        }
    }

    private fun runPerformanceTests(connection: org.springframework.data.redis.connection.RedisConnection): RedisPerformanceMetrics {
        val testOperations = 10
        val testKey = "${properties.testKey}:perf"

        // Test de escritura
        val writeStartTime = Instant.now()
        repeat(testOperations) { i ->
            connection.set("$testKey:$i".toByteArray(), "value$i".toByteArray())
        }
        val writeDuration = Duration.between(writeStartTime, Instant.now())

        // Test de lectura
        val readStartTime = Instant.now()
        repeat(testOperations) { i ->
            connection.get("$testKey:$i".toByteArray())
        }
        val readDuration = Duration.between(readStartTime, Instant.now())

        // Limpiar claves de test
        repeat(testOperations) { i ->
            connection.del("$testKey:$i".toByteArray())
        }

        return RedisPerformanceMetrics(
            writeOperationsPerSecond = (testOperations * 1000.0 / writeDuration.toMillis()).toInt(),
            readOperationsPerSecond = (testOperations * 1000.0 / readDuration.toMillis()).toInt(),
            averageWriteTimeMs = writeDuration.toMillis().toDouble() / testOperations,
            averageReadTimeMs = readDuration.toMillis().toDouble() / testOperations
        )
    }

    private fun getClusterInfo(connection: org.springframework.data.redis.connection.RedisConnection): RedisClusterInfo? {
        return try {
            // Intentar obtener información de cluster
            val clusterInfo = connection.clusterGetNodes()
            RedisClusterInfo(
                isClusterEnabled = true,
                nodesCount = clusterInfo?.size ?: 0,
                masterNodes = clusterInfo?.count { it.isMaster } ?: 0,
                slaveNodes = clusterInfo?.count { !it.isMaster } ?: 0
            )
        } catch (e: Exception) {
            // No es un cluster o no está habilitado
            RedisClusterInfo(
                isClusterEnabled = false,
                nodesCount = 1,
                masterNodes = 1,
                slaveNodes = 0
            )
        }
    }
}

/**
 * Información del servidor Redis
 */
data class RedisServerInfo(
    val version: String = "unknown",
    val mode: String = "standalone",
    val role: String = "master",
    val connectedClients: Int = 0,
    val usedMemory: Long = 0L,
    val usedMemoryHuman: String = "0B",
    val maxMemory: Long = 0L,
    val uptimeInSeconds: Long = 0L
) {
    val memoryUsagePercentage: Long
        get() = if (maxMemory > 0) (usedMemory * 100 / maxMemory) else 0L
}

/**
 * Métricas de performance de Redis
 */
data class RedisPerformanceMetrics(
    val writeOperationsPerSecond: Int,
    val readOperationsPerSecond: Int,
    val averageWriteTimeMs: Double,
    val averageReadTimeMs: Double
)

/**
 * Información de cluster Redis
 */
data class RedisClusterInfo(
    val isClusterEnabled: Boolean,
    val nodesCount: Int,
    val masterNodes: Int,
    val slaveNodes: Int
)

/**
 * Detalles completos de salud de Redis
 */
data class RedisHealthDetails(
    val isHealthy: Boolean,
    val pingResult: String? = null,
    val pingDurationMs: Long? = null,
    val serverInfo: RedisServerInfo? = null,
    val performanceMetrics: RedisPerformanceMetrics? = null,
    val clusterInfo: RedisClusterInfo? = null,
    val error: String? = null,
    val errorType: String? = null,
    val timestamp: Instant
)