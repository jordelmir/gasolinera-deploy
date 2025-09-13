package com.gasolinerajsm.shared.database

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Monitor de performance de queries con integración a pg_stat_statements
 */
@Service
@ConditionalOnProperty(prefix = "gasolinera.database.optimization", name = ["performance-monitoring"], havingValue = "true", matchIfMissing = true)
class QueryPerformanceMonitor(
    private val jdbcTemplate: JdbcTemplate,
    private val meterRegistry: MeterRegistry? = null
) {

    private val logger = LoggerFactory.getLogger(QueryPerformanceMonitor::class.java)

    // Cache de estadísticas de queries
    private val queryStats = ConcurrentHashMap<String, QueryPerformanceStats>()

    // Contadores globales
    private val totalQueries = AtomicLong(0)
    private val slowQueries = AtomicLong(0)

    /**
     * Analiza performance usando pg_stat_statements
     */
    fun analyzeQueryPerformance(): QueryPerformanceReport {
        logger.debug("Analyzing query performance using pg_stat_statements")

        return try {
            val slowQueries = getSlowQueries()
            val frequentQueries = getFrequentQueries()
            val expensiveQueries = getExpensiveQueries()
            val ioIntensiveQueries = getIOIntensiveQueries()

            val report = QueryPerformanceReport(
                timestamp = Instant.now(),
                slowQueries = slowQueries,
                frequentQueries = frequentQueries,
                expensiveQueries = expensiveQueries,
                ioIntensiveQueries = ioIntensiveQueries,
                recommendations = generateRecommendations(slowQueries, frequentQueries, expensiveQueries)
            )

            // Registrar métricas
            recordPerformanceMetrics(report)

            report

        } catch (e: Exception) {
            logger.error("Error analyzing query performance", e)
            QueryPerformanceReport.empty()
        }
    }

    /**
     * Obtiene queries lentas desde pg_stat_statements
     */
    private fun getSlowQueries(limit: Int = 20): List<SlowQueryInfo> {
        val sql = """
            SELECT
                query,
                calls,
                total_exec_time,
                mean_exec_time,
                max_exec_time,
                min_exec_time,
                stddev_exec_time,
                rows,
                100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
            FROM pg_stat_statements
            WHERE mean_exec_time > 100  -- Queries con tiempo promedio > 100ms
            ORDER BY mean_exec_time DESC
            LIMIT ?
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql, { rs, _ ->
                SlowQueryInfo(
                    query = normalizeQuery(rs.getString("query")),
                    calls = rs.getLong("calls"),
                    totalTime = rs.getDouble("total_exec_time"),
                    meanTime = rs.getDouble("mean_exec_time"),
                    maxTime = rs.getDouble("max_exec_time"),
                    minTime = rs.getDouble("min_exec_time"),
                    stddevTime = rs.getDouble("stddev_exec_time"),
                    rows = rs.getLong("rows"),
                    hitPercent = rs.getDouble("hit_percent")
                )
            }, limit)
        } catch (e: Exception) {
            logger.warn("Could not retrieve slow queries from pg_stat_statements", e)
            emptyList()
        }
    }

    /**
     * Obtiene queries más frecuentes
     */
    private fun getFrequentQueries(limit: Int = 20): List<FrequentQueryInfo> {
        val sql = """
            SELECT
                query,
                calls,
                total_exec_time,
                mean_exec_time,
                rows,
                100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
            FROM pg_stat_statements
            WHERE calls > 100  -- Queries ejecutadas más de 100 veces
            ORDER BY calls DESC
            LIMIT ?
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql, { rs, _ ->
                FrequentQueryInfo(
                    query = normalizeQuery(rs.getString("query")),
                    calls = rs.getLong("calls"),
                    totalTime = rs.getDouble("total_exec_time"),
                    meanTime = rs.getDouble("mean_exec_time"),
                    rows = rs.getLong("rows"),
                    hitPercent = rs.getDouble("hit_percent")
                )
            }, limit)
        } catch (e: Exception) {
            logger.warn("Could not retrieve frequent queries from pg_stat_statements", e)
            emptyList()
        }
    }

    /**
     * Obtiene queries más costosas (por tiempo total)
     */
    private fun getExpensiveQueries(limit: Int = 20): List<ExpensiveQueryInfo> {
        val sql = """
            SELECT
                query,
                calls,
                total_exec_time,
                mean_exec_time,
                (total_exec_time / sum(total_exec_time) OVER()) * 100 AS percent_total_time
            FROM pg_stat_statements
            WHERE total_exec_time > 1000  -- Queries con tiempo total > 1 segundo
            ORDER BY total_exec_time DESC
            LIMIT ?
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql, { rs, _ ->
                ExpensiveQueryInfo(
                    query = normalizeQuery(rs.getString("query")),
                    calls = rs.getLong("calls"),
                    totalTime = rs.getDouble("total_exec_time"),
                    meanTime = rs.getDouble("mean_exec_time"),
                    percentTotalTime = rs.getDouble("percent_total_time")
                )
            }, limit)
        } catch (e: Exception) {
            logger.warn("Could not retrieve expensive queries from pg_stat_statements", e)
            emptyList()
        }
    }

    /**
     * Obtiene queries intensivas en I/O
     */
    private fun getIOIntensiveQueries(limit: Int = 20): List<IOIntensiveQueryInfo> {
        val sql = """
            SELECT
                query,
                calls,
                shared_blks_read,
                shared_blks_hit,
                shared_blks_dirtied,
                shared_blks_written,
                temp_blks_read,
                temp_blks_written,
                100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
            FROM pg_stat_statements
            WHERE shared_blks_read + shared_blks_written > 1000  -- Queries con mucho I/O
            ORDER BY (shared_blks_read + shared_blks_written) DESC
            LIMIT ?
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql, { rs, _ ->
                IOIntensiveQueryInfo(
                    query = normalizeQuery(rs.getString("query")),
                    calls = rs.getLong("calls"),
                    sharedBlksRead = rs.getLong("shared_blks_read"),
                    sharedBlksHit = rs.getLong("shared_blks_hit"),
                    sharedBlksDirtied = rs.getLong("shared_blks_dirtied"),
                    sharedBlksWritten = rs.getLong("shared_blks_written"),
                    tempBlksRead = rs.getLong("temp_blks_read"),
                    tempBlksWritten = rs.getLong("temp_blks_written"),
                    hitPercent = rs.getDouble("hit_percent")
                )
            }, limit)
        } catch (e: Exception) {
            logger.warn("Could not retrieve I/O intensive queries from pg_stat_statements", e)
            emptyList()
        }
    }

    /**
     * Genera recomendaciones basadas en el análisis
     */
    private fun generateRecommendations(
        slowQueries: List<SlowQueryInfo>,
        frequentQueries: List<FrequentQueryInfo>,
        expensiveQueries: List<ExpensiveQueryInfo>
    ): List<PerformanceRecommendation> {
        val recommendations = mutableListOf<PerformanceRecommendation>()

        // Recomendaciones para queries lentas
        slowQueries.forEach { query ->
            if (query.meanTime > 1000) { // > 1 segundo
                recommendations.add(
                    PerformanceRecommendation(
                        type = RecommendationType.SLOW_QUERY,
                        priority = Priority.HIGH,
                        query = query.query,
                        description = "Query muy lenta detectada (${query.meanTime.toInt()}ms promedio)",
                        suggestion = "Revisar plan de ejecución y considerar índices adicionales",
                        potentialImpact = "Reducción de ${(query.meanTime * 0.7).toInt()}ms por ejecución"
                    )
                )
            }

            if (query.hitPercent < 90) { // Baja tasa de hit en cache
                recommendations.add(
                    PerformanceRecommendation(
                        type = RecommendationType.LOW_CACHE_HIT,
                        priority = Priority.MEDIUM,
                        query = query.query,
                        description = "Baja tasa de hit en buffer cache (${query.hitPercent.toInt()}%)",
                        suggestion = "Considerar aumentar shared_buffers o revisar patrones de acceso",
                        potentialImpact = "Mejora en tiempo de respuesta del 20-40%"
                    )
                )
            }
        }

        // Recomendaciones para queries frecuentes
        frequentQueries.forEach { query ->
            if (query.calls > 10000 && query.meanTime > 50) {
                recommendations.add(
                    PerformanceRecommendation(
                        type = RecommendationType.FREQUENT_SLOW_QUERY,
                        priority = Priority.HIGH,
                        query = query.query,
                        description = "Query frecuente y lenta (${query.calls} ejecuciones, ${query.meanTime.toInt()}ms promedio)",
                        suggestion = "Optimizar urgentemente - considerar caching o índices",
                        potentialImpact = "Ahorro de ${(query.calls * query.meanTime * 0.5 / 1000).toInt()} segundos por período"
                    )
                )
            }
        }

        // Recomendaciones para queries costosas
        expensiveQueries.forEach { query ->
            if (query.percentTotalTime > 10) { // Más del 10% del tiempo total
                recommendations.add(
                    PerformanceRecommendation(
                        type = RecommendationType.EXPENSIVE_QUERY,
                        priority = Priority.HIGH,
                        query = query.query,
                        description = "Query consume ${query.percentTotalTime.toInt()}% del tiempo total de CPU",
                        suggestion = "Optimización crítica requerida - revisar algoritmo y estructura",
                        potentialImpact = "Reducción significativa en carga del servidor"
                    )
                )
            }
        }

        return recommendations.sortedByDescending { it.priority.ordinal }
    }

    /**
     * Registra métricas de performance
     */
    private fun recordPerformanceMetrics(report: QueryPerformanceReport) {
        meterRegistry?.let { registry ->
            // Métricas de queries lentas
            registry.gauge("database.slow_queries.count", report.slowQueries.size)

            if (report.slowQueries.isNotEmpty()) {
                val avgSlowTime = report.slowQueries.map { it.meanTime }.average()
                registry.gauge("database.slow_queries.avg_time", avgSlowTime)
            }

            // Métricas de queries frecuentes
            registry.gauge("database.frequent_queries.count", report.frequentQueries.size)

            if (report.frequentQueries.isNotEmpty()) {
                val totalCalls = report.frequentQueries.sumOf { it.calls }
                registry.gauge("database.frequent_queries.total_calls", totalCalls)
            }

            // Métricas de recomendaciones
            registry.gauge("database.recommendations.count", report.recommendations.size)

            val highPriorityRecommendations = report.recommendations.count { it.priority == Priority.HIGH }
            registry.gauge("database.recommendations.high_priority", highPriorityRecommendations)
        }
    }

    /**
     * Normaliza query para agrupación y análisis
     */
    private fun normalizeQuery(query: String): String {
        return query
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\$\\d+"), "?") // Parámetros PostgreSQL
            .replace(Regex("\\b\\d+\\b"), "?") // Números literales
            .replace(Regex("'[^']*'"), "?") // Strings literales
            .trim()
    }

    /**
     * Resetea estadísticas de pg_stat_statements
     */
    fun resetStatistics() {
        try {
            jdbcTemplate.execute("SELECT pg_stat_statements_reset()")
            logger.info("pg_stat_statements statistics reset successfully")
        } catch (e: Exception) {
            logger.error("Error resetting pg_stat_statements", e)
        }
    }

    /**
     * Obtiene estadísticas generales de la base de datos
     */
    fun getDatabaseStats(): DatabaseStats {
        return try {
            val stats = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT count(*) FROM pg_stat_statements) as total_queries,
                    (SELECT sum(calls) FROM pg_stat_statements) as total_calls,
                    (SELECT sum(total_exec_time) FROM pg_stat_statements) as total_time,
                    (SELECT avg(mean_exec_time) FROM pg_stat_statements) as avg_time
            """.trimIndent())

            DatabaseStats(
                totalQueries = (stats["total_queries"] as Number?)?.toLong() ?: 0,
                totalCalls = (stats["total_calls"] as Number?)?.toLong() ?: 0,
                totalTime = (stats["total_time"] as Number?)?.toDouble() ?: 0.0,
                avgTime = (stats["avg_time"] as Number?)?.toDouble() ?: 0.0
            )
        } catch (e: Exception) {
            logger.error("Error getting database stats", e)
            DatabaseStats(0, 0, 0.0, 0.0)
        }
    }

    /**
     * Tarea programada para análisis periódico
     */
    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    fun scheduledPerformanceAnalysis() {
        try {
            val report = analyzeQueryPerformance()

            if (report.recommendations.any { it.priority == Priority.HIGH }) {
                logger.warn("High priority performance issues detected: ${report.recommendations.count { it.priority == Priority.HIGH }} issues")
            }

            // Limpiar estadísticas antiguas si es necesario
            cleanupOldStats()

        } catch (e: Exception) {
            logger.error("Error in scheduled performance analysis", e)
        }
    }

    /**
     * Limpia estadísticas antiguas
     */
    private fun cleanupOldStats() {
        val cutoff = Instant.now().minus(Duration.ofHours(24))
        queryStats.entries.removeIf { (_, stats) ->
            stats.lastUpdated.isBefore(cutoff)
        }
    }
}

// Clases de datos

data class QueryPerformanceReport(
    val timestamp: Instant,
    val slowQueries: List<SlowQueryInfo>,
    val frequentQueries: List<FrequentQueryInfo>,
    val expensiveQueries: List<ExpensiveQueryInfo>,
    val ioIntensiveQueries: List<IOIntensiveQueryInfo>,
    val recommendations: List<PerformanceRecommendation>
) {
    companion object {
        fun empty() = QueryPerformanceReport(
            Instant.now(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
    }
}

data class SlowQueryInfo(
    val query: String,
    val calls: Long,
    val totalTime: Double,
    val meanTime: Double,
    val maxTime: Double,
    val minTime: Double,
    val stddevTime: Double,
    val rows: Long,
    val hitPercent: Double
)

data class FrequentQueryInfo(
    val query: String,
    val calls: Long,
    val totalTime: Double,
    val meanTime: Double,
    val rows: Long,
    val hitPercent: Double
)

data class ExpensiveQueryInfo(
    val query: String,
    val calls: Long,
    val totalTime: Double,
    val meanTime: Double,
    val percentTotalTime: Double
)

data class IOIntensiveQueryInfo(
    val query: String,
    val calls: Long,
    val sharedBlksRead: Long,
    val sharedBlksHit: Long,
    val sharedBlksDirtied: Long,
    val sharedBlksWritten: Long,
    val tempBlksRead: Long,
    val tempBlksWritten: Long,
    val hitPercent: Double
)

data class PerformanceRecommendation(
    val type: RecommendationType,
    val priority: Priority,
    val query: String,
    val description: String,
    val suggestion: String,
    val potentialImpact: String
)

data class DatabaseStats(
    val totalQueries: Long,
    val totalCalls: Long,
    val totalTime: Double,
    val avgTime: Double
)

data class QueryPerformanceStats(
    val query: String,
    val executionCount: AtomicLong,
    val totalTime: AtomicLong,
    val lastUpdated: Instant
)

enum class RecommendationType {
    SLOW_QUERY,
    FREQUENT_SLOW_QUERY,
    EXPENSIVE_QUERY,
    LOW_CACHE_HIT,
    HIGH_IO_USAGE,
    MISSING_INDEX,
    INEFFICIENT_JOIN
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}