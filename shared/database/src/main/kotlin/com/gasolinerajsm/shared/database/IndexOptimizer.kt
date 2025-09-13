package com.gasolinerajsm.shared.database

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Optimizador de índices para mejorar el rendimiento de queries
 */
@Component
class IndexOptimizer(
    private val jdbcTemplate: JdbcTemplate,
    private val properties: DatabaseOptimizationProperties
) {

    /**
     * Analiza y sugiere optimizaciones de índices
     */
    fun analyzeIndexOptimizations(): IndexOptimizationReport {
        return IndexOptimizationReport(
            missingIndexes = suggestMissingIndexes(),
            unusedIndexes = findUnusedIndexes(),
            duplicateIndexes = findDuplicateIndexes(),
            inefficientIndexes = findInefficientIndexes(),
            indexMaintenanceNeeded = findIndexesNeedingMaintenance(),
            recommendations = generateIndexRecommendations(),
            timestamp = Instant.now()
        )
    }

    /**
     * Sugiere índices faltantes basado en queries lentas
     */
    fun suggestMissingIndexes(): List<MissingIndexSuggestion> {
        val suggestions = mutableListOf<MissingIndexSuggestion>()

        // Analizar queries lentas para identificar patrones
        val slowQueries = getSlowQueriesForIndexAnalysis()

        for (query in slowQueries) {
            val indexSuggestions = analyzeQueryForMissingIndexes(query)
            suggestions.addAll(indexSuggestions)
        }

        // Analizar patrones de WHERE clauses más comunes
        val commonWherePatterns = analyzeCommonWherePatterns()
        suggestions.addAll(commonWherePatterns)

        return suggestions.distinctBy { "${it.tableName}.${it.columns.joinToString(",")}" }
    }

    /**
     * Encuentra índices no utilizados
     */
    fun findUnusedIndexes(): List<UnusedIndex> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                indexname,
                idx_scan,
                pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
                pg_relation_size(indexrelid) as index_size_bytes,
                pg_get_indexdef(indexrelid) as index_definition
            FROM pg_stat_user_indexes
            WHERE idx_scan < ?
            AND schemaname NOT IN ('information_schema', 'pg_catalog')
            ORDER BY pg_relation_size(indexrelid) DESC
        """.trimIndent()

        val usageThreshold = (properties.optimization.indexUsageThreshold * 1000).toLong()

        return jdbcTemplate.query(sql, { rs, _ ->
            UnusedIndex(
                schemaName = rs.getString("schemaname"),
                tableName = rs.getString("tablename"),
                indexName = rs.getString("indexname"),
                scans = rs.getLong("idx_scan"),
                indexSize = rs.getString("index_size"),
                indexSizeBytes = rs.getLong("index_size_bytes"),
                definition = rs.getString("index_definition"),
                recommendation = if (rs.getLong("idx_scan") == 0L) "DROP" else "MONITOR"
            )
        }, usageThreshold)
    }

    /**
     * Encuentra índices duplicados o redundantes
     */
    fun findDuplicateIndexes(): List<DuplicateIndex> {
        val sql = """
            WITH index_columns AS (
                SELECT
                    schemaname,
                    tablename,
                    indexname,
                    array_agg(attname ORDER BY attnum) as columns,
                    pg_relation_size(indexrelid) as index_size_bytes
                FROM pg_indexes
                JOIN pg_class ON pg_class.relname = indexname
                JOIN pg_index ON pg_index.indexrelid = pg_class.oid
                JOIN pg_attribute ON pg_attribute.attrelid = pg_index.indrelid
                    AND pg_attribute.attnum = ANY(pg_index.indkey)
                WHERE schemaname NOT IN ('information_schema', 'pg_catalog')
                GROUP BY schemaname, tablename, indexname, indexrelid
            )
            SELECT
                ic1.schemaname,
                ic1.tablename,
                ic1.indexname as index1,
                ic2.indexname as index2,
                ic1.columns,
                ic1.index_size_bytes as size1,
                ic2.index_size_bytes as size2
            FROM index_columns ic1
            JOIN index_columns ic2 ON ic1.schemaname = ic2.schemaname
                AND ic1.tablename = ic2.tablename
                AND ic1.columns = ic2.columns
                AND ic1.indexname < ic2.indexname
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql) { rs, _ ->
                val columns = rs.getArray("columns").array as Array<*>
                DuplicateIndex(
                    schemaName = rs.getString("schemaname"),
                    tableName = rs.getString("tablename"),
                    index1 = rs.getString("index1"),
                    index2 = rs.getString("index2"),
                    columns = columns.map { it.toString() },
                    size1Bytes = rs.getLong("size1"),
                    size2Bytes = rs.getLong("size2"),
                    recommendation = if (rs.getLong("size1") > rs.getLong("size2"))
                        "Keep ${rs.getString("index1")}, drop ${rs.getString("index2")}"
                    else
                        "Keep ${rs.getString("index2")}, drop ${rs.getString("index1")}"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Encuentra índices ineficientes
     */
    fun findInefficientIndexes(): List<InefficientIndex> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                indexname,
                idx_tup_read,
                idx_tup_fetch,
                idx_scan,
                CASE
                    WHEN idx_scan > 0 THEN idx_tup_read::float / idx_scan
                    ELSE 0
                END as avg_tuples_per_scan,
                pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
                pg_relation_size(indexrelid) as index_size_bytes
            FROM pg_stat_user_indexes
            WHERE idx_scan > 0
            AND schemaname NOT IN ('information_schema', 'pg_catalog')
            ORDER BY avg_tuples_per_scan DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            val avgTuplesPerScan = rs.getDouble("avg_tuples_per_scan")
            val isInefficient = avgTuplesPerScan > 1000 // Threshold for inefficiency

            if (isInefficient) {
                InefficientIndex(
                    schemaName = rs.getString("schemaname"),
                    tableName = rs.getString("tablename"),
                    indexName = rs.getString("indexname"),
                    scans = rs.getLong("idx_scan"),
                    tuplesRead = rs.getLong("idx_tup_read"),
                    tuplesFetched = rs.getLong("idx_tup_fetch"),
                    avgTuplesPerScan = avgTuplesPerScan,
                    indexSize = rs.getString("index_size"),
                    indexSizeBytes = rs.getLong("index_size_bytes"),
                    inefficiencyReason = when {
                        avgTuplesPerScan > 10000 -> "Very high selectivity - consider composite index"
                        avgTuplesPerScan > 5000 -> "High selectivity - review index effectiveness"
                        else -> "Moderate selectivity - monitor usage"
                    }
                )
            } else null
        }.filterNotNull()
    }

    /**
     * Encuentra índices que necesitan mantenimiento
     */
    fun findIndexesNeedingMaintenance(): List<IndexMaintenanceNeeded> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                indexname,
                pg_stat_get_blocks_fetched(indexrelid) - pg_stat_get_blocks_hit(indexrelid) as blocks_read,
                pg_stat_get_blocks_hit(indexrelid) as blocks_hit,
                CASE
                    WHEN pg_stat_get_blocks_fetched(indexrelid) > 0
                    THEN (pg_stat_get_blocks_hit(indexrelid)::float / pg_stat_get_blocks_fetched(indexrelid) * 100)
                    ELSE 100
                END as hit_ratio,
                pg_relation_size(indexrelid) as index_size_bytes
            FROM pg_stat_user_indexes
            WHERE schemaname NOT IN ('information_schema', 'pg_catalog')
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            val hitRatio = rs.getDouble("hit_ratio")
            val needsMaintenance = hitRatio < 95.0 // Less than 95% hit ratio

            if (needsMaintenance) {
                IndexMaintenanceNeeded(
                    schemaName = rs.getString("schemaname"),
                    tableName = rs.getString("tablename"),
                    indexName = rs.getString("indexname"),
                    blocksRead = rs.getLong("blocks_read"),
                    blocksHit = rs.getLong("blocks_hit"),
                    hitRatio = hitRatio,
                    indexSizeBytes = rs.getLong("index_size_bytes"),
                    maintenanceType = when {
                        hitRatio < 80.0 -> "REINDEX"
                        hitRatio < 90.0 -> "ANALYZE"
                        else -> "MONITOR"
                    },
                    reason = "Low cache hit ratio: ${String.format("%.2f", hitRatio)}%"
                )
            } else null
        }.filterNotNull()
    }

    /**
     * Genera recomendaciones generales de índices
     */
    fun generateIndexRecommendations(): List<IndexRecommendation> {
        val recommendations = mutableListOf<IndexRecommendation>()

        // Recomendaciones basadas en tablas de Gasolinera JSM
        recommendations.addAll(generateGasolineraSpecificRecommendations())

        // Recomendaciones basadas en patrones comunes
        recommendations.addAll(generateCommonPatternRecommendations())

        return recommendations
    }

    private fun generateGasolineraSpecificRecommendations(): List<IndexRecommendation> {
        return listOf(
            // Coupons table
            IndexRecommendation(
                tableName = "coupons",
                indexName = "idx_coupons_user_status_created",
                columns = listOf("user_id", "status", "created_at"),
                indexType = "BTREE",
                reason = "Optimize coupon queries by user and status with time ordering",
                priority = IndexPriority.HIGH,
                estimatedImpact = "High - frequently queried pattern"
            ),
            IndexRecommendation(
                tableName = "coupons",
                indexName = "idx_coupons_campaign_status",
                columns = listOf("campaign_id", "status"),
                indexType = "BTREE",
                reason = "Optimize campaign-based coupon queries",
                priority = IndexPriority.MEDIUM,
                estimatedImpact = "Medium - campaign management queries"
            ),
            IndexRecommendation(
                tableName = "coupons",
                indexName = "idx_coupons_code_unique",
                columns = listOf("code"),
                indexType = "UNIQUE",
                reason = "Ensure coupon code uniqueness and fast lookups",
                priority = IndexPriority.HIGH,
                estimatedImpact = "High - redemption process critical path"
            ),

            // Redemptions table
            IndexRecommendation(
                tableName = "redemptions",
                indexName = "idx_redemptions_user_date",
                columns = listOf("user_id", "redeemed_at"),
                indexType = "BTREE",
                reason = "Optimize user redemption history queries",
                priority = IndexPriority.HIGH,
                estimatedImpact = "High - user activity tracking"
            ),
            IndexRecommendation(
                tableName = "redemptions",
                indexName = "idx_redemptions_station_date",
                columns = listOf("station_id", "redeemed_at"),
                indexType = "BTREE",
                reason = "Optimize station-based redemption analytics",
                priority = IndexPriority.MEDIUM,
                estimatedImpact = "Medium - station performance analysis"
            ),

            // Raffle tickets table
            IndexRecommendation(
                tableName = "raffle_tickets",
                indexName = "idx_raffle_tickets_user_raffle",
                columns = listOf("user_id", "raffle_id", "created_at"),
                indexType = "BTREE",
                reason = "Optimize user raffle participation queries",
                priority = IndexPriority.HIGH,
                estimatedImpact = "High - raffle participation tracking"
            ),

            // Users table
            IndexRecommendation(
                tableName = "users",
                indexName = "idx_users_email_unique",
                columns = listOf("email"),
                indexType = "UNIQUE",
                reason = "Ensure email uniqueness and fast authentication",
                priority = IndexPriority.HIGH,
                estimatedImpact = "High - authentication critical path"
            ),
            IndexRecommendation(
                tableName = "users",
                indexName = "idx_users_phone_unique",
                columns = listOf("phone_number"),
                indexType = "UNIQUE",
                reason = "Ensure phone uniqueness and SMS-based auth",
                priority = IndexPriority.HIGH,
                estimatedImpact = "High - SMS authentication"
            ),

            // Stations table
            IndexRecommendation(
                tableName = "stations",
                indexName = "idx_stations_location_gist",
                columns = listOf("location"),
                indexType = "GIST",
                reason = "Optimize geospatial queries for nearby stations",
                priority = IndexPriority.HIGH,
                estimatedImpact = "High - location-based searches"
            ),
            IndexRecommendation(
                tableName = "stations",
                indexName = "idx_stations_active_region",
                columns = listOf("is_active", "region"),
                indexType = "BTREE",
                reason = "Optimize active station queries by region",
                priority = IndexPriority.MEDIUM,
                estimatedImpact = "Medium - regional station management"
            )
        )
    }

    private fun generateCommonPatternRecommendations(): List<IndexRecommendation> {
        // Analizar patrones comunes en las queries y sugerir índices
        return emptyList() // Implementar basado en análisis de queries reales
    }

    private fun getSlowQueriesForIndexAnalysis(): List<SlowQuery> {
        // Reutilizar el análisis de queries lentas del DatabaseAnalyzer
        return try {
            val sql = """
                SELECT query, calls, mean_time, total_time, rows
                FROM pg_stat_statements
                WHERE mean_time > ?
                ORDER BY mean_time DESC
                LIMIT 20
            """.trimIndent()

            jdbcTemplate.query(sql, { rs, _ ->
                SlowQuery(
                    query = rs.getString("query"),
                    calls = rs.getLong("calls"),
                    totalTimeMs = rs.getDouble("total_time"),
                    meanTimeMs = rs.getDouble("mean_time"),
                    maxTimeMs = 0.0,
                    minTimeMs = 0.0,
                    stddevTimeMs = 0.0,
                    rows = rs.getLong("rows"),
                    hitPercent = 0.0
                )
            }, properties.optimization.slowQueryThreshold.toMillis().toDouble())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun analyzeQueryForMissingIndexes(query: SlowQuery): List<MissingIndexSuggestion> {
        val suggestions = mutableListOf<MissingIndexSuggestion>()

        // Análisis básico de patrones en queries
        val queryText = query.query.lowercase()

        // Buscar patrones WHERE con columnas específicas
        val wherePattern = Regex("""where\s+(\w+)\s*=\s*""")
        val matches = wherePattern.findAll(queryText)

        for (match in matches) {
            val column = match.groupValues[1]
            // Extraer nombre de tabla (simplificado)
            val tablePattern = Regex("""from\s+(\w+)""")
            val tableMatch = tablePattern.find(queryText)

            if (tableMatch != null) {
                val tableName = tableMatch.groupValues[1]
                suggestions.add(
                    MissingIndexSuggestion(
                        tableName = tableName,
                        columns = listOf(column),
                        indexType = "BTREE",
                        reason = "Frequent WHERE clause on $column",
                        estimatedImpact = "Medium",
                        queryPattern = queryText.take(100)
                    )
                )
            }
        }

        return suggestions
    }

    private fun analyzeCommonWherePatterns(): List<MissingIndexSuggestion> {
        // Implementar análisis de patrones comunes en WHERE clauses
        return emptyList()
    }

    /**
     * Crea un índice basado en una recomendación
     */
    fun createIndex(recommendation: IndexRecommendation): IndexCreationResult {
        return try {
            val sql = buildCreateIndexSql(recommendation)
            val startTime = System.currentTimeMillis()

            jdbcTemplate.execute(sql)

            val duration = System.currentTimeMillis() - startTime

            IndexCreationResult(
                success = true,
                indexName = recommendation.indexName,
                tableName = recommendation.tableName,
                sql = sql,
                durationMs = duration,
                message = "Index created successfully"
            )
        } catch (e: Exception) {
            IndexCreationResult(
                success = false,
                indexName = recommendation.indexName,
                tableName = recommendation.tableName,
                sql = "",
                durationMs = 0,
                message = "Failed to create index: ${e.message}",
                error = e.message
            )
        }
    }

    private fun buildCreateIndexSql(recommendation: IndexRecommendation): String {
        val indexType = when (recommendation.indexType.uppercase()) {
            "UNIQUE" -> "UNIQUE"
            "GIST" -> ""
            else -> ""
        }

        val using = when (recommendation.indexType.uppercase()) {
            "GIST" -> "USING GIST"
            "GIN" -> "USING GIN"
            "HASH" -> "USING HASH"
            else -> ""
        }

        return """
            CREATE $indexType INDEX CONCURRENTLY ${recommendation.indexName}
            ON ${recommendation.tableName} $using
            (${recommendation.columns.joinToString(", ")})
        """.trimIndent()
    }
}

// Data classes para optimización de índices

data class IndexOptimizationReport(
    val missingIndexes: List<MissingIndexSuggestion>,
    val unusedIndexes: List<UnusedIndex>,
    val duplicateIndexes: List<DuplicateIndex>,
    val inefficientIndexes: List<InefficientIndex>,
    val indexMaintenanceNeeded: List<IndexMaintenanceNeeded>,
    val recommendations: List<IndexRecommendation>,
    val timestamp: Instant
)

data class MissingIndexSuggestion(
    val tableName: String,
    val columns: List<String>,
    val indexType: String,
    val reason: String,
    val estimatedImpact: String,
    val queryPattern: String? = null
)

data class UnusedIndex(
    val schemaName: String,
    val tableName: String,
    val indexName: String,
    val scans: Long,
    val indexSize: String,
    val indexSizeBytes: Long,
    val definition: String,
    val recommendation: String
)

data class DuplicateIndex(
    val schemaName: String,
    val tableName: String,
    val index1: String,
    val index2: String,
    val columns: List<String>,
    val size1Bytes: Long,
    val size2Bytes: Long,
    val recommendation: String
)

data class InefficientIndex(
    val schemaName: String,
    val tableName: String,
    val indexName: String,
    val scans: Long,
    val tuplesRead: Long,
    val tuplesFetched: Long,
    val avgTuplesPerScan: Double,
    val indexSize: String,
    val indexSizeBytes: Long,
    val inefficiencyReason: String
)

data class IndexMaintenanceNeeded(
    val schemaName: String,
    val tableName: String,
    val indexName: String,
    val blocksRead: Long,
    val blocksHit: Long,
    val hitRatio: Double,
    val indexSizeBytes: Long,
    val maintenanceType: String,
    val reason: String
)

data class IndexRecommendation(
    val tableName: String,
    val indexName: String,
    val columns: List<String>,
    val indexType: String,
    val reason: String,
    val priority: IndexPriority,
    val estimatedImpact: String
)

data class IndexCreationResult(
    val success: Boolean,
    val indexName: String,
    val tableName: String,
    val sql: String,
    val durationMs: Long,
    val message: String,
    val error: String? = null
)

enum class IndexPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}