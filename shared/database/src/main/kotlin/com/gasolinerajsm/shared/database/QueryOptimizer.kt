package com.gasolinerajsm.shared.database

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Optimizador de queries para mejorar el rendimiento de consultas SQL
 */
@Component
class QueryOptimizer(
    private val jdbcTemplate: JdbcTemplate,
    private val properties: DatabaseOptimizationProperties
) {

    /**
     * Analiza y optimiza queries problemáticas
     */
    fun analyzeQueryPerformance(): QueryOptimizationReport {
        return QueryOptimizationReport(
            slowQueries = analyzeSlowQueries(),
            queryRecommendations = generateQueryRecommendations(),
            statisticsRecommendations = analyzeStatisticsNeeds(),
            configurationRecommendations = analyzeConfigurationNeeds(),
            timestamp = Instant.now()
        )
    }

    /**
     * Analiza queries lentas con detalles de ejecución
     */
    fun analyzeSlowQueries(): List<SlowQueryAnalysis> {
        val sql = """
            SELECT
                query,
                calls,
                total_time,
                mean_time,
                max_time,
                min_time,
                stddev_time,
                rows,
                100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent,
                shared_blks_hit,
                shared_blks_read,
                shared_blks_dirtied,
                shared_blks_written,
                local_blks_hit,
                local_blks_read,
                temp_blks_read,
                temp_blks_written
            FROM pg_stat_statements
            WHERE mean_time > ?
            ORDER BY total_time DESC
            LIMIT 50
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql, { rs, _ ->
                val query = rs.getString("query")
                SlowQueryAnalysis(
                    query = query,
                    calls = rs.getLong("calls"),
                    totalTimeMs = rs.getDouble("total_time"),
                    meanTimeMs = rs.getDouble("mean_time"),
                    maxTimeMs = rs.getDouble("max_time"),
                    minTimeMs = rs.getDouble("min_time"),
                    stddevTimeMs = rs.getDouble("stddev_time"),
                    rows = rs.getLong("rows"),
                    hitPercent = rs.getDouble("hit_percent"),
                    sharedBlksHit = rs.getLong("shared_blks_hit"),
                    sharedBlksRead = rs.getLong("shared_blks_read"),
                    sharedBlksDirtied = rs.getLong("shared_blks_dirtied"),
                    sharedBlksWritten = rs.getLong("shared_blks_written"),
                    localBlksHit = rs.getLong("local_blks_hit"),
                    localBlksRead = rs.getLong("local_blks_read"),
                    tempBlksRead = rs.getLong("temp_blks_read"),
                    tempBlksWritten = rs.getLong("temp_blks_written"),
                    optimizationSuggestions = analyzeQueryForOptimization(query),
                    severity = calculateQuerySeverity(rs.getDouble("mean_time"), rs.getLong("calls"))
                )
            }, properties.optimization.slowQueryThreshold.toMillis().toDouble())
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Genera recomendaciones específicas de optimización de queries
     */
    fun generateQueryRecommendations(): List<QueryRecommendation> {
        val recommendations = mutableListOf<QueryRecommendation>()

        // Recomendaciones basadas en patrones comunes de Gasolinera JSM
        recommendations.addAll(generateGasolineraQueryRecommendations())

        // Recomendaciones basadas en análisis de queries actuales
        recommendations.addAll(analyzeCurrentQueriesForRecommendations())

        return recommendations
    }

    /**
     * Analiza necesidades de actualización de estadísticas
     */
    fun analyzeStatisticsNeeds(): List<StatisticsRecommendation> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                n_tup_ins + n_tup_upd + n_tup_del as total_changes,
                n_live_tup,
                last_analyze,
                last_autoanalyze,
                CASE
                    WHEN n_live_tup > 0
                    THEN (n_tup_ins + n_tup_upd + n_tup_del)::float / n_live_tup
                    ELSE 0
                END as change_ratio
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('information_schema', 'pg_catalog')
            ORDER BY change_ratio DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            val changeRatio = rs.getDouble("change_ratio")
            val lastAnalyze = rs.getTimestamp("last_analyze")?.toInstant()
            val lastAutoanalyze = rs.getTimestamp("last_autoanalyze")?.toInstant()

            val needsUpdate = changeRatio > 0.1 || // More than 10% changes
                    (lastAnalyze == null && lastAutoanalyze == null) ||
                    (lastAnalyze?.isBefore(Instant.now().minusSeconds(86400)) == true) // More than 1 day old

            if (needsUpdate) {
                StatisticsRecommendation(
                    schemaName = rs.getString("schemaname"),
                    tableName = rs.getString("tablename"),
                    totalChanges = rs.getLong("total_changes"),
                    liveTuples = rs.getLong("n_live_tup"),
                    changeRatio = changeRatio,
                    lastAnalyze = lastAnalyze,
                    lastAutoanalyze = lastAutoanalyze,
                    recommendation = when {
                        changeRatio > 0.5 -> "IMMEDIATE_ANALYZE"
                        changeRatio > 0.2 -> "SCHEDULE_ANALYZE"
                        else -> "MONITOR"
                    },
                    reason = when {
                        changeRatio > 0.5 -> "High change ratio: ${String.format("%.2f", changeRatio * 100)}%"
                        lastAnalyze == null -> "Never analyzed"
                        else -> "Outdated statistics"
                    }
                )
            } else null
        }.filterNotNull()
    }

    /**
     * Analiza necesidades de configuración de PostgreSQL
     */
    fun analyzeConfigurationNeeds(): List<ConfigurationRecommendation> {
        val recommendations = mutableListOf<ConfigurationRecommendation>()

        // Analizar configuración actual
        val currentConfig = getCurrentConfiguration()

        // Recomendaciones basadas en workload
        recommendations.addAll(analyzeWorkloadBasedConfig(currentConfig))

        // Recomendaciones basadas en recursos del sistema
        recommendations.addAll(analyzeResourceBasedConfig(currentConfig))

        return recommendations
    }

    private fun analyzeQueryForOptimization(query: String): List<String> {
        val suggestions = mutableListOf<String>()
        val queryLower = query.lowercase()

        // Análisis de patrones problemáticos
        when {
            queryLower.contains("select *") -> {
                suggestions.add("Avoid SELECT * - specify only needed columns")
            }
            queryLower.contains("like '%") && queryLower.contains("%'") -> {
                suggestions.add("Leading wildcard LIKE patterns cannot use indexes - consider full-text search")
            }
            queryLower.contains("order by") && !queryLower.contains("limit") -> {
                suggestions.add("ORDER BY without LIMIT can be expensive - consider pagination")
            }
            queryLower.contains("distinct") -> {
                suggestions.add("DISTINCT can be expensive - verify if really needed")
            }
            queryLower.contains("group by") && queryLower.contains("having") -> {
                suggestions.add("Consider moving conditions from HAVING to WHERE when possible")
            }
            queryLower.contains("not in") -> {
                suggestions.add("NOT IN can be slow with NULLs - consider NOT EXISTS or LEFT JOIN")
            }
            queryLower.contains("or") -> {
                suggestions.add("OR conditions can prevent index usage - consider UNION or separate queries")
            }
            queryLower.contains("function(") && queryLower.contains("where") -> {
                suggestions.add("Functions in WHERE clause prevent index usage - consider functional indexes")
            }
        }

        // Análisis específico para Gasolinera JSM
        when {
            queryLower.contains("coupons") && queryLower.contains("user_id") -> {
                suggestions.add("Consider composite index on (user_id, status, created_at) for coupon queries")
            }
            queryLower.contains("redemptions") && queryLower.contains("redeemed_at") -> {
                suggestions.add("Consider partitioning redemptions table by date for better performance")
            }
            queryLower.contains("stations") && queryLower.contains("location") -> {
                suggestions.add("Use PostGIS spatial indexes for location-based queries")
            }
        }

        return suggestions
    }

    private fun calculateQuerySeverity(meanTime: Double, calls: Long): QuerySeverity {
        val totalImpact = meanTime * calls

        return when {
            meanTime > 10000 || totalImpact > 1000000 -> QuerySeverity.CRITICAL
            meanTime > 5000 || totalImpact > 500000 -> QuerySeverity.HIGH
            meanTime > 1000 || totalImpact > 100000 -> QuerySeverity.MEDIUM
            else -> QuerySeverity.LOW
        }
    }

    private fun generateGasolineraQueryRecommendations(): List<QueryRecommendation> {
        return listOf(
            QueryRecommendation(
                category = "Coupon Queries",
                title = "Optimize user coupon retrieval",
                description = "Use composite indexes for user-based coupon queries",
                example = """
                    -- Instead of:
                    SELECT * FROM coupons WHERE user_id = ? ORDER BY created_at DESC;

                    -- Use:
                    SELECT id, code, status, created_at FROM coupons
                    WHERE user_id = ? AND status IN ('ACTIVE', 'USED')
                    ORDER BY created_at DESC LIMIT 50;
                """.trimIndent(),
                impact = "High",
                effort = "Low"
            ),
            QueryRecommendation(
                category = "Redemption Analytics",
                title = "Optimize redemption reporting queries",
                description = "Use date partitioning and proper indexes for analytics",
                example = """
                    -- Instead of:
                    SELECT COUNT(*) FROM redemptions WHERE redeemed_at BETWEEN ? AND ?;

                    -- Use with partitioned table:
                    SELECT COUNT(*) FROM redemptions_2024_01
                    WHERE redeemed_at BETWEEN ? AND ?
                    UNION ALL
                    SELECT COUNT(*) FROM redemptions_2024_02
                    WHERE redeemed_at BETWEEN ? AND ?;
                """.trimIndent(),
                impact = "High",
                effort = "Medium"
            ),
            QueryRecommendation(
                category = "Station Queries",
                title = "Optimize nearby station searches",
                description = "Use PostGIS spatial indexes for location queries",
                example = """
                    -- Instead of:
                    SELECT * FROM stations WHERE
                    ST_Distance(location, ST_Point(?, ?)) < 5000;

                    -- Use:
                    SELECT id, name, address, location FROM stations
                    WHERE location && ST_Expand(ST_Point(?, ?), 0.05)
                    AND ST_DWithin(location, ST_Point(?, ?), 5000)
                    ORDER BY location <-> ST_Point(?, ?) LIMIT 10;
                """.trimIndent(),
                impact = "High",
                effort = "Medium"
            ),
            QueryRecommendation(
                category = "User Authentication",
                title = "Optimize user lookup queries",
                description = "Use proper indexes for authentication queries",
                example = """
                    -- Ensure unique indexes exist:
                    CREATE UNIQUE INDEX CONCURRENTLY idx_users_email ON users(email);
                    CREATE UNIQUE INDEX CONCURRENTLY idx_users_phone ON users(phone_number);

                    -- Then use:
                    SELECT id, email, password_hash, status FROM users
                    WHERE email = ? AND status = 'ACTIVE';
                """.trimIndent(),
                impact = "Critical",
                effort = "Low"
            )
        )
    }

    private fun analyzeCurrentQueriesForRecommendations(): List<QueryRecommendation> {
        // Analizar queries actuales y generar recomendaciones específicas
        return emptyList() // Implementar basado en queries reales
    }

    private fun getCurrentConfiguration(): Map<String, String> {
        val config = mutableMapOf<String, String>()

        val configParams = listOf(
            "shared_buffers", "effective_cache_size", "maintenance_work_mem",
            "checkpoint_completion_target", "wal_buffers", "default_statistics_target",
            "random_page_cost", "effective_io_concurrency", "work_mem", "max_connections"
        )

        for (param in configParams) {
            try {
                val value = jdbcTemplate.queryForObject("SHOW $param", String::class.java)
                if (value != null) {
                    config[param] = value
                }
            } catch (e: Exception) {
                // Parameter might not exist in this PostgreSQL version
            }
        }

        return config
    }

    private fun analyzeWorkloadBasedConfig(currentConfig: Map<String, String>): List<ConfigurationRecommendation> {
        val recommendations = mutableListOf<ConfigurationRecommendation>()

        // Analizar workload actual
        val connectionStats = analyzeConnectionUsage()
        val queryStats = analyzeQueryPatterns()

        // Recomendaciones basadas en conexiones
        if (connectionStats.avgActiveConnections > connectionStats.maxConnections * 0.8) {
            recommendations.add(
                ConfigurationRecommendation(
                    parameter = "max_connections",
                    currentValue = connectionStats.maxConnections.toString(),
                    recommendedValue = (connectionStats.maxConnections * 1.5).toInt().toString(),
                    reason = "High connection usage detected",
                    impact = "Medium",
                    requiresRestart = true
                )
            )
        }

        // Recomendaciones basadas en memoria
        val workMem = parseMemoryValue(currentConfig["work_mem"] ?: "4MB")
        if (queryStats.avgSortOperations > 100 && workMem < 16 * 1024 * 1024) { // 16MB
            recommendations.add(
                ConfigurationRecommendation(
                    parameter = "work_mem",
                    currentValue = currentConfig["work_mem"] ?: "4MB",
                    recommendedValue = "16MB",
                    reason = "High number of sort operations detected",
                    impact = "High",
                    requiresRestart = false
                )
            )
        }

        return recommendations
    }

    private fun analyzeResourceBasedConfig(currentConfig: Map<String, String>): List<ConfigurationRecommendation> {
        val recommendations = mutableListOf<ConfigurationRecommendation>()

        // Obtener información del sistema
        val systemMemory = getSystemMemory()
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Recomendaciones basadas en memoria del sistema
        val sharedBuffers = parseMemoryValue(currentConfig["shared_buffers"] ?: "128MB")
        val recommendedSharedBuffers = (systemMemory * 0.25).toLong() // 25% of system memory

        if (sharedBuffers < recommendedSharedBuffers * 0.8) {
            recommendations.add(
                ConfigurationRecommendation(
                    parameter = "shared_buffers",
                    currentValue = currentConfig["shared_buffers"] ?: "128MB",
                    recommendedValue = formatMemoryValue(recommendedSharedBuffers),
                    reason = "Shared buffers too small for available system memory",
                    impact = "High",
                    requiresRestart = true
                )
            )
        }

        // Recomendaciones basadas en CPU
        val effectiveIoConcurrency = currentConfig["effective_io_concurrency"]?.toIntOrNull() ?: 1
        if (effectiveIoConcurrency < cpuCores && cpuCores > 2) {
            recommendations.add(
                ConfigurationRecommendation(
                    parameter = "effective_io_concurrency",
                    currentValue = effectiveIoConcurrency.toString(),
                    recommendedValue = cpuCores.toString(),
                    reason = "IO concurrency can be increased based on available CPU cores",
                    impact = "Medium",
                    requiresRestart = false
                )
            )
        }

        return recommendations
    }

    private fun analyzeConnectionUsage(): ConnectionUsageStats {
        val maxConnections = jdbcTemplate.queryForObject("SHOW max_connections", String::class.java)?.toIntOrNull() ?: 100
        val currentConnections = jdbcTemplate.queryForObject("SELECT count(*) FROM pg_stat_activity", Int::class.java) ?: 0
        val activeConnections = jdbcTemplate.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE state = 'active'", Int::class.java) ?: 0

        return ConnectionUsageStats(
            maxConnections = maxConnections,
            currentConnections = currentConnections,
            avgActiveConnections = activeConnections,
            peakConnections = currentConnections // Simplificado
        )
    }

    private fun analyzeQueryPatterns(): QueryPatternStats {
        // Análisis simplificado de patrones de queries
        return QueryPatternStats(
            avgSortOperations = 50, // Placeholder
            avgJoinOperations = 25,
            avgAggregateOperations = 15
        )
    }

    private fun parseMemoryValue(value: String): Long {
        val numericPart = value.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0
        return when {
            value.contains("GB", ignoreCase = true) -> numericPart * 1024 * 1024 * 1024
            value.contains("MB", ignoreCase = true) -> numericPart * 1024 * 1024
            value.contains("KB", ignoreCase = true) -> numericPart * 1024
            else -> numericPart
        }
    }

    private fun formatMemoryValue(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)}GB"
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }

    private fun getSystemMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            runtime.maxMemory()
        } catch (e: Exception) {
            8L * 1024 * 1024 * 1024 // Default 8GB
        }
    }
}

// Data classes para optimización de queries

data class QueryOptimizationReport(
    val slowQueries: List<SlowQueryAnalysis>,
    val queryRecommendations: List<QueryRecommendation>,
    val statisticsRecommendations: List<StatisticsRecommendation>,
    val configurationRecommendations: List<ConfigurationRecommendation>,
    val timestamp: Instant
)

data class SlowQueryAnalysis(
    val query: String,
    val calls: Long,
    val totalTimeMs: Double,
    val meanTimeMs: Double,
    val maxTimeMs: Double,
    val minTimeMs: Double,
    val stddevTimeMs: Double,
    val rows: Long,
    val hitPercent: Double,
    val sharedBlksHit: Long,
    val sharedBlksRead: Long,
    val sharedBlksDirtied: Long,
    val sharedBlksWritten: Long,
    val localBlksHit: Long,
    val localBlksRead: Long,
    val tempBlksRead: Long,
    val tempBlksWritten: Long,
    val optimizationSuggestions: List<String>,
    val severity: QuerySeverity
)

data class QueryRecommendation(
    val category: String,
    val title: String,
    val description: String,
    val example: String,
    val impact: String,
    val effort: String
)

data class StatisticsRecommendation(
    val schemaName: String,
    val tableName: String,
    val totalChanges: Long,
    val liveTuples: Long,
    val changeRatio: Double,
    val lastAnalyze: Instant?,
    val lastAutoanalyze: Instant?,
    val recommendation: String,
    val reason: String
)

data class ConfigurationRecommendation(
    val parameter: String,
    val currentValue: String,
    val recommendedValue: String,
    val reason: String,
    val impact: String,
    val requiresRestart: Boolean
)

data class ConnectionUsageStats(
    val maxConnections: Int,
    val currentConnections: Int,
    val avgActiveConnections: Int,
    val peakConnections: Int
)

data class QueryPatternStats(
    val avgSortOperations: Int,
    val avgJoinOperations: Int,
    val avgAggregateOperations: Int
)

enum class QuerySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}