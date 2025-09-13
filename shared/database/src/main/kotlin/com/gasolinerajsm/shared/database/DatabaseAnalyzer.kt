package com.gasolinerajsm.shared.database

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Analizador de base de datos para identificar oportunidades de optimización
 */
@Component
class DatabaseAnalyzer(
    private val jdbcTemplate: JdbcTemplate,
    private val properties: DatabaseOptimizationProperties
) {

    /**
     * Analiza el rendimiento general de la base de datos
     */
    fun analyzePerformance(): DatabasePerformanceAnalysis {
        return DatabasePerformanceAnalysis(
            slowQueries = analyzeSlowQueries(),
            tableStats = analyzeTableStatistics(),
            indexUsage = analyzeIndexUsage(),
            connectionStats = analyzeConnectionStatistics(),
            lockAnalysis = analyzeLocks(),
            diskUsage = analyzeDiskUsage(),
            timestamp = Instant.now()
        )
    }

    /**
     * Analiza queries lentas usando pg_stat_statements
     */
    fun analyzeSlowQueries(): List<SlowQuery> {
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
                100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
            FROM pg_stat_statements
            WHERE mean_time > ?
            ORDER BY mean_time DESC
            LIMIT 50
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql, { rs, _ ->
                SlowQuery(
                    query = rs.getString("query"),
                    calls = rs.getLong("calls"),
                    totalTimeMs = rs.getDouble("total_time"),
                    meanTimeMs = rs.getDouble("mean_time"),
                    maxTimeMs = rs.getDouble("max_time"),
                    minTimeMs = rs.getDouble("min_time"),
                    stddevTimeMs = rs.getDouble("stddev_time"),
                    rows = rs.getLong("rows"),
                    hitPercent = rs.getDouble("hit_percent")
                )
            }, properties.optimization.slowQueryThreshold.toMillis().toDouble())
        } catch (e: Exception) {
            // pg_stat_statements might not be enabled
            emptyList()
        }
    }

    /**
     * Analiza estadísticas de tablas
     */
    fun analyzeTableStatistics(): List<TableStatistics> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                n_tup_ins as inserts,
                n_tup_upd as updates,
                n_tup_del as deletes,
                n_live_tup as live_tuples,
                n_dead_tup as dead_tuples,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                last_autoanalyze,
                vacuum_count,
                autovacuum_count,
                analyze_count,
                autoanalyze_count
            FROM pg_stat_user_tables
            ORDER BY n_live_tup DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            TableStatistics(
                schemaName = rs.getString("schemaname"),
                tableName = rs.getString("tablename"),
                inserts = rs.getLong("inserts"),
                updates = rs.getLong("updates"),
                deletes = rs.getLong("deletes"),
                liveTuples = rs.getLong("live_tuples"),
                deadTuples = rs.getLong("dead_tuples"),
                lastVacuum = rs.getTimestamp("last_vacuum")?.toInstant(),
                lastAutovacuum = rs.getTimestamp("last_autovacuum")?.toInstant(),
                lastAnalyze = rs.getTimestamp("last_analyze")?.toInstant(),
                lastAutoanalyze = rs.getTimestamp("last_autoanalyze")?.toInstant(),
                vacuumCount = rs.getLong("vacuum_count"),
                autovacuumCount = rs.getLong("autovacuum_count"),
                analyzeCount = rs.getLong("analyze_count"),
                autoanalyzeCount = rs.getLong("autoanalyze_count")
            )
        }
    }

    /**
     * Analiza el uso de índices
     */
    fun analyzeIndexUsage(): List<IndexUsage> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                indexname,
                idx_tup_read,
                idx_tup_fetch,
                idx_scan,
                pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
                pg_relation_size(indexrelid) as index_size_bytes
            FROM pg_stat_user_indexes
            ORDER BY idx_scan DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            IndexUsage(
                schemaName = rs.getString("schemaname"),
                tableName = rs.getString("tablename"),
                indexName = rs.getString("indexname"),
                tuplesRead = rs.getLong("idx_tup_read"),
                tuplesFetched = rs.getLong("idx_tup_fetch"),
                scans = rs.getLong("idx_scan"),
                indexSize = rs.getString("index_size"),
                indexSizeBytes = rs.getLong("index_size_bytes")
            )
        }
    }

    /**
     * Analiza estadísticas de conexiones
     */
    fun analyzeConnectionStatistics(): ConnectionStatistics {
        val activeConnections = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'",
            Int::class.java
        ) ?: 0

        val idleConnections = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_stat_activity WHERE state = 'idle'",
            Int::class.java
        ) ?: 0

        val totalConnections = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_stat_activity",
            Int::class.java
        ) ?: 0

        val maxConnections = jdbcTemplate.queryForObject(
            "SHOW max_connections",
            String::class.java
        )?.toIntOrNull() ?: 100

        return ConnectionStatistics(
            activeConnections = activeConnections,
            idleConnections = idleConnections,
            totalConnections = totalConnections,
            maxConnections = maxConnections,
            connectionUsagePercent = (totalConnections.toDouble() / maxConnections * 100).toInt()
        )
    }

    /**
     * Analiza locks activos
     */
    fun analyzeLocks(): List<LockInfo> {
        val sql = """
            SELECT
                pg_class.relname,
                pg_locks.locktype,
                pg_locks.mode,
                pg_locks.granted,
                pg_stat_activity.query,
                pg_stat_activity.state,
                pg_stat_activity.query_start,
                pg_stat_activity.pid
            FROM pg_locks
            JOIN pg_class ON pg_locks.relation = pg_class.oid
            JOIN pg_stat_activity ON pg_locks.pid = pg_stat_activity.pid
            WHERE NOT pg_locks.granted
            ORDER BY pg_stat_activity.query_start
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql) { rs, _ ->
                LockInfo(
                    relationName = rs.getString("relname"),
                    lockType = rs.getString("locktype"),
                    mode = rs.getString("mode"),
                    granted = rs.getBoolean("granted"),
                    query = rs.getString("query"),
                    state = rs.getString("state"),
                    queryStart = rs.getTimestamp("query_start")?.toInstant(),
                    pid = rs.getInt("pid")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Analiza el uso de disco
     */
    fun analyzeDiskUsage(): List<TableDiskUsage> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
                pg_total_relation_size(schemaname||'.'||tablename) as total_size_bytes,
                pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
                pg_relation_size(schemaname||'.'||tablename) as table_size_bytes,
                pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size,
                pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename) as index_size_bytes
            FROM pg_tables
            WHERE schemaname NOT IN ('information_schema', 'pg_catalog')
            ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            TableDiskUsage(
                schemaName = rs.getString("schemaname"),
                tableName = rs.getString("tablename"),
                totalSize = rs.getString("total_size"),
                totalSizeBytes = rs.getLong("total_size_bytes"),
                tableSize = rs.getString("table_size"),
                tableSizeBytes = rs.getLong("table_size_bytes"),
                indexSize = rs.getString("index_size"),
                indexSizeBytes = rs.getLong("index_size_bytes")
            )
        }
    }

    /**
     * Identifica tablas candidatas para particionado
     */
    fun identifyPartitionCandidates(): List<PartitionCandidate> {
        val candidates = mutableListOf<PartitionCandidate>()

        val tableStats = analyzeTableStatistics()
        val diskUsage = analyzeDiskUsage()

        for (table in tableStats) {
            val usage = diskUsage.find { it.tableName == table.tableName }

            if (table.liveTuples > properties.partitioning.partitionThreshold ||
                (usage?.tableSizeBytes ?: 0) > 1_000_000_000) { // 1GB

                candidates.add(
                    PartitionCandidate(
                        schemaName = table.schemaName,
                        tableName = table.tableName,
                        rowCount = table.liveTuples,
                        sizeBytes = usage?.tableSizeBytes ?: 0,
                        recommendedStrategy = determinePartitionStrategy(table),
                        reason = buildPartitionReason(table, usage)
                    )
                )
            }
        }

        return candidates
    }

    private fun determinePartitionStrategy(table: TableStatistics): PartitionStrategy {
        // Lógica para determinar la mejor estrategia de particionado
        return when {
            table.tableName.contains("log") ||
            table.tableName.contains("event") ||
            table.tableName.contains("audit") -> PartitionStrategy.TIME_BASED

            table.liveTuples > 10_000_000 -> PartitionStrategy.HASH_BASED

            else -> PartitionStrategy.RANGE_BASED
        }
    }

    private fun buildPartitionReason(table: TableStatistics, usage: TableDiskUsage?): String {
        val reasons = mutableListOf<String>()

        if (table.liveTuples > properties.partitioning.partitionThreshold) {
            reasons.add("High row count: ${table.liveTuples}")
        }

        if ((usage?.tableSizeBytes ?: 0) > 1_000_000_000) {
            reasons.add("Large table size: ${usage?.tableSize}")
        }

        if (table.deadTuples > table.liveTuples * 0.2) {
            reasons.add("High dead tuple ratio: ${table.deadTuples}/${table.liveTuples}")
        }

        return reasons.joinToString(", ")
    }
}

// Data classes para los resultados del análisis

data class DatabasePerformanceAnalysis(
    val slowQueries: List<SlowQuery>,
    val tableStats: List<TableStatistics>,
    val indexUsage: List<IndexUsage>,
    val connectionStats: ConnectionStatistics,
    val lockAnalysis: List<LockInfo>,
    val diskUsage: List<TableDiskUsage>,
    val timestamp: Instant
)

data class SlowQuery(
    val query: String,
    val calls: Long,
    val totalTimeMs: Double,
    val meanTimeMs: Double,
    val maxTimeMs: Double,
    val minTimeMs: Double,
    val stddevTimeMs: Double,
    val rows: Long,
    val hitPercent: Double
)

data class TableStatistics(
    val schemaName: String,
    val tableName: String,
    val inserts: Long,
    val updates: Long,
    val deletes: Long,
    val liveTuples: Long,
    val deadTuples: Long,
    val lastVacuum: Instant?,
    val lastAutovacuum: Instant?,
    val lastAnalyze: Instant?,
    val lastAutoanalyze: Instant?,
    val vacuumCount: Long,
    val autovacuumCount: Long,
    val analyzeCount: Long,
    val autoanalyzeCount: Long
)

data class IndexUsage(
    val schemaName: String,
    val tableName: String,
    val indexName: String,
    val tuplesRead: Long,
    val tuplesFetched: Long,
    val scans: Long,
    val indexSize: String,
    val indexSizeBytes: Long
)

data class ConnectionStatistics(
    val activeConnections: Int,
    val idleConnections: Int,
    val totalConnections: Int,
    val maxConnections: Int,
    val connectionUsagePercent: Int
)

data class LockInfo(
    val relationName: String,
    val lockType: String,
    val mode: String,
    val granted: Boolean,
    val query: String,
    val state: String,
    val queryStart: Instant?,
    val pid: Int
)

data class TableDiskUsage(
    val schemaName: String,
    val tableName: String,
    val totalSize: String,
    val totalSizeBytes: Long,
    val tableSize: String,
    val tableSizeBytes: Long,
    val indexSize: String,
    val indexSizeBytes: Long
)

data class PartitionCandidate(
    val schemaName: String,
    val tableName: String,
    val rowCount: Long,
    val sizeBytes: Long,
    val recommendedStrategy: PartitionStrategy,
    val reason: String
)