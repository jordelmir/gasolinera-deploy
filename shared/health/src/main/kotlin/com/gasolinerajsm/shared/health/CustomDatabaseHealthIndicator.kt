package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Health indicator personalizado para base de datos con métricas avanzadas
 */
@Component
class CustomDatabaseHealthIndicator(
    private val jdbcTemplate: JdbcTemplate,
    private val properties: HealthProperties.DatabaseHealthProperties
) : HealthIndicator {

    override fun health(): Health {
        if (!properties.enabled) {
            return Health.up()
                .withDetail("status", "disabled")
                .build()
        }

        return try {
            val startTime = Instant.now()

            // Ejecutar query de validación
            val result = executeValidationQuery()
            val queryDuration = Duration.between(startTime, Instant.now())

            // Obtener métricas de conexión
            val connectionMetrics = getConnectionPoolMetrics()

            // Verificar performance
            val isSlowQuery = queryDuration > properties.slowQueryThreshold
            val isConnectionPoolHealthy = connectionMetrics.usagePercentage < properties.connectionPoolThreshold

            val healthBuilder = if (isConnectionPoolHealthy && !isSlowQuery) {
                Health.up()
            } else {
                Health.down()
            }

            healthBuilder
                .withDetail("database", "PostgreSQL")
                .withDetail("validationQuery", properties.validationQuery)
                .withDetail("queryResult", result)
                .withDetail("queryDurationMs", queryDuration.toMillis())
                .withDetail("isSlowQuery", isSlowQuery)
                .withDetail("slowQueryThresholdMs", properties.slowQueryThreshold.toMillis())
                .withDetail("connectionPool", mapOf(
                    "active" to connectionMetrics.activeConnections,
                    "idle" to connectionMetrics.idleConnections,
                    "total" to connectionMetrics.totalConnections,
                    "usagePercentage" to connectionMetrics.usagePercentage,
                    "isHealthy" to isConnectionPoolHealthy,
                    "threshold" to properties.connectionPoolThreshold
                ))
                .withDetail("timestamp", Instant.now().toString())
                .build()

        } catch (e: Exception) {
            Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.message ?: "Unknown error")
                .withDetail("errorType", e.javaClass.simpleName)
                .withDetail("timestamp", Instant.now().toString())
                .withException(e)
                .build()
        }
    }

    private fun executeValidationQuery(): Any? {
        return try {
            jdbcTemplate.queryForObject(properties.validationQuery, Any::class.java)
        } catch (e: Exception) {
            throw DatabaseHealthException("Validation query failed: ${e.message}", e)
        }
    }

    private fun getConnectionPoolMetrics(): ConnectionPoolMetrics {
        return try {
            // Obtener métricas del pool de conexiones (HikariCP)
            val activeConnections = getActiveConnectionsCount()
            val idleConnections = getIdleConnectionsCount()
            val totalConnections = activeConnections + idleConnections
            val maxConnections = getMaxConnectionsCount()

            val usagePercentage = if (maxConnections > 0) {
                (totalConnections.toDouble() / maxConnections * 100).toInt()
            } else {
                0
            }

            ConnectionPoolMetrics(
                activeConnections = activeConnections,
                idleConnections = idleConnections,
                totalConnections = totalConnections,
                maxConnections = maxConnections,
                usagePercentage = usagePercentage
            )
        } catch (e: Exception) {
            // Valores por defecto si no se pueden obtener métricas
            ConnectionPoolMetrics(
                activeConnections = -1,
                idleConnections = -1,
                totalConnections = -1,
                maxConnections = -1,
                usagePercentage = 0
            )
        }
    }

    private fun getActiveConnectionsCount(): Int {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'",
                Int::class.java
            ) ?: 0
        } catch (e: Exception) {
            -1
        }
    }

    private fun getIdleConnectionsCount(): Int {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE state = 'idle'",
                Int::class.java
            ) ?: 0
        } catch (e: Exception) {
            -1
        }
    }

    private fun getMaxConnectionsCount(): Int {
        return try {
            jdbcTemplate.queryForObject(
                "SHOW max_connections",
                String::class.java
            )?.toIntOrNull() ?: 100
        } catch (e: Exception) {
            100 // Valor por defecto
        }
    }

    /**
     * Verifica la salud de la base de datos con métricas adicionales
     */
    fun getDetailedHealth(): DatabaseHealthDetails {
        val startTime = Instant.now()

        return try {
            // Query de validación básica
            val validationResult = executeValidationQuery()
            val validationDuration = Duration.between(startTime, Instant.now())

            // Métricas de performance
            val performanceMetrics = getDatabasePerformanceMetrics()

            // Métricas de conexión
            val connectionMetrics = getConnectionPoolMetrics()

            DatabaseHealthDetails(
                isHealthy = true,
                validationResult = validationResult,
                validationDurationMs = validationDuration.toMillis(),
                connectionMetrics = connectionMetrics,
                performanceMetrics = performanceMetrics,
                timestamp = Instant.now()
            )
        } catch (e: Exception) {
            DatabaseHealthDetails(
                isHealthy = false,
                error = e.message,
                errorType = e.javaClass.simpleName,
                timestamp = Instant.now()
            )
        }
    }

    private fun getDatabasePerformanceMetrics(): DatabasePerformanceMetrics {
        return try {
            val slowQueries = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM pg_stat_statements
                WHERE mean_time > ?
                """.trimIndent(),
                Int::class.java,
                properties.slowQueryThreshold.toMillis()
            ) ?: 0

            val totalQueries = jdbcTemplate.queryForObject(
                "SELECT sum(calls) FROM pg_stat_statements",
                Long::class.java
            ) ?: 0L

            val avgQueryTime = jdbcTemplate.queryForObject(
                "SELECT avg(mean_time) FROM pg_stat_statements",
                Double::class.java
            ) ?: 0.0

            DatabasePerformanceMetrics(
                slowQueriesCount = slowQueries,
                totalQueries = totalQueries,
                averageQueryTimeMs = avgQueryTime,
                slowQueryThresholdMs = properties.slowQueryThreshold.toMillis()
            )
        } catch (e: Exception) {
            // pg_stat_statements puede no estar habilitado
            DatabasePerformanceMetrics(
                slowQueriesCount = -1,
                totalQueries = -1L,
                averageQueryTimeMs = -1.0,
                slowQueryThresholdMs = properties.slowQueryThreshold.toMillis()
            )
        }
    }
}

/**
 * Métricas del pool de conexiones
 */
data class ConnectionPoolMetrics(
    val activeConnections: Int,
    val idleConnections: Int,
    val totalConnections: Int,
    val maxConnections: Int,
    val usagePercentage: Int
)

/**
 * Métricas de performance de la base de datos
 */
data class DatabasePerformanceMetrics(
    val slowQueriesCount: Int,
    val totalQueries: Long,
    val averageQueryTimeMs: Double,
    val slowQueryThresholdMs: Long
)

/**
 * Detalles completos de salud de la base de datos
 */
data class DatabaseHealthDetails(
    val isHealthy: Boolean,
    val validationResult: Any? = null,
    val validationDurationMs: Long? = null,
    val connectionMetrics: ConnectionPoolMetrics? = null,
    val performanceMetrics: DatabasePerformanceMetrics? = null,
    val error: String? = null,
    val errorType: String? = null,
    val timestamp: Instant
)

/**
 * Excepción específica para problemas de salud de base de datos
 */
class DatabaseHealthException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)