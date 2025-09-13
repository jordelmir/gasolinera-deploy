package com.gasolinerajsm.monitoring

import io.micrometer.core.instrument.*
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * World-Class Database Performance Metrics
 *
 * Monitors database performance, connection health, and query optimization:
 * - Connection pool metrics
 * - Query performance and slow query detection
 * - Transaction metrics and deadlock detection
 * - Database health and availability
 */
@Component
class DatabaseMetrics(private val meterRegistry: MeterRegistry) {

    // ==================== CONNECTION POOL METRICS ====================

    private val connectionPoolActive = Gauge.builder("database.connection.pool.active")
        .description("Number of active database connections")
        .register(meterRegistry, this) { getActiveConnections() }

    private val connectionPoolIdle = Gauge.builder("database.connection.pool.idle")
        .description("Number of idle database connections")
        .register(meterRegistry, this) { getIdleConnections() }

    private val connectionPoolMax = Gauge.builder("database.connection.pool.max")
        .description("Maximum number of database connections")
        .register(meterRegistry, this) { getMaxConnections() }

    private val connectionPoolUsage = Gauge.builder("database.connection.pool.usage")
        .description("Database connection pool usage percentage")
        .register(meterRegistry, this) { getConnectionPoolUsage() }

    private val connectionWaitTime = Timer.builder("database.connection.wait.time")
        .description("Time waiting for database connection")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    private val connectionLeaks = Counter.builder("database.connection.leaks.total")
        .description("Total number of connection leaks detected")
        .register(meterRegistry)

    // ==================== QUERY PERFORMANCE METRICS ====================

    private val queryExecutionTime = Timer.builder("database.query.execution.time")
        .description("Database query execution time")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    private val slowQueries = Counter.builder("database.query.slow.total")
        .description("Total number of slow queries (>1s)")
        .register(meterRegistry)

    private val queryErrors = Counter.builder("database.query.errors.total")
        .description("Total number of query errors")
        .register(meterRegistry)

    private val queryCache = Counter.builder("database.query.cache.total")
        .description("Query cache hits and misses")
        .register(meterRegistry)

    // ==================== TRANSACTION METRICS ====================

    private val transactionDuration = Timer.builder("database.transaction.duration")
        .description("Database transaction duration")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    private val transactionRollbacks = Counter.builder("database.transaction.rollbacks.total")
        .description("Total number of transaction rollbacks")
        .register(meterRegistry)

    private val deadlocks = Counter.builder("database.deadlocks.total")
        .description("Total number of deadlocks detected")
        .register(meterRegistry)

    private val lockWaitTime = Timer.builder("database.lock.wait.time")
        .description("Time waiting for database locks")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    // ==================== CRUD OPERATION METRICS ====================

    private val selectOperations = Counter.builder("database.operations.select.total")
        .description("Total number of SELECT operations")
        .register(meterRegistry)

    private val insertOperations = Counter.builder("database.operations.insert.total")
        .description("Total number of INSERT operations")
        .register(meterRegistry)

    private val updateOperations = Counter.builder("database.operations.update.total")
        .description("Total number of UPDATE operations")
        .register(meterRegistry)

    private val deleteOperations = Counter.builder("database.operations.delete.total")
        .description("Total number of DELETE operations")
        .register(meterRegistry)

    // ==================== TABLE-SPECIFIC METRICS ====================

    private val tableSize = Gauge.builder("database.table.size")
        .description("Table size in bytes")
        .baseUnit("bytes")
        .register(meterRegistry, this) { getTableSize() }

    private val indexUsage = Gauge.builder("database.index.usage")
        .description("Index usage percentage")
        .register(meterRegistry, this) { getIndexUsage() }

    private val tableScans = Counter.builder("database.table.scans.total")
        .description("Total number of full table scans")
        .register(meterRegistry)

    // ==================== REPLICATION METRICS ====================

    private val replicationLag = Gauge.builder("database.replication.lag")
        .description("Replication lag in seconds")
        .baseUnit("seconds")
        .register(meterRegistry, this) { getReplicationLag() }

    private val replicationErrors = Counter.builder("database.replication.errors.total")
        .description("Total number of replication errors")
        .register(meterRegistry)

    // ==================== BACKUP METRICS ====================

    private val backupDuration = Timer.builder("database.backup.duration")
        .description("Database backup duration")
        .register(meterRegistry)

    private val backupSize = Gauge.builder("database.backup.size")
        .description("Database backup size in bytes")
        .baseUnit("bytes")
        .register(meterRegistry, this) { getBackupSize() }

    private val lastBackupAge = Gauge.builder("database.backup.last.age")
        .description("Age of last backup in hours")
        .baseUnit("hours")
        .register(meterRegistry, this) { getLastBackupAge() }

    // ==================== INTERNAL COUNTERS ====================

    private val activeConnections = AtomicLong(0)
    private val idleConnections = AtomicLong(5)
    private val maxConnections = AtomicLong(20)
    private val tableSize = AtomicLong(0)
    private val indexUsage = AtomicLong(85)
    private val replicationLag = AtomicLong(0)
    private val backupSize = AtomicLong(0)
    private val lastBackupAge = AtomicLong(2)

    // ==================== PUBLIC METHODS ====================

    fun recordQueryExecution(
        queryType: String,
        table: String,
        duration: Duration,
        success: Boolean,
        rowsAffected: Long = 0
    ) {
        val tags = Tags.of(
            Tag.of("query_type", queryType.uppercase()),
            Tag.of("table", table),
            Tag.of("success", success.toString()),
            Tag.of("rows_range", getRowsRange(rowsAffected))
        )

        queryExecutionTime.record(duration, tags)

        // Record operation type
        when (queryType.uppercase()) {
            "SELECT" -> selectOperations.increment(tags)
            "INSERT" -> insertOperations.increment(tags)
            "UPDATE" -> updateOperations.increment(tags)
            "DELETE" -> deleteOperations.increment(tags)
        }

        // Record slow queries
        if (duration.toMillis() > 1000) {
            slowQueries.increment(tags)
        }

        // Record errors
        if (!success) {
            queryErrors.increment(tags)
        }
    }

    fun recordConnectionWait(duration: Duration, success: Boolean) {
        connectionWaitTime.record(duration, Tags.of(
            Tag.of("success", success.toString())
        ))
    }

    fun recordConnectionLeak(source: String) {
        connectionLeaks.increment(Tags.of(
            Tag.of("source", source)
        ))
    }

    fun recordTransaction(
        duration: Duration,
        operationCount: Int,
        rollback: Boolean,
        isolation: String
    ) {
        val tags = Tags.of(
            Tag.of("rollback", rollback.toString()),
            Tag.of("isolation", isolation),
            Tag.of("operation_count_range", getOperationCountRange(operationCount))
        )

        transactionDuration.record(duration, tags)

        if (rollback) {
            transactionRollbacks.increment(tags)
        }
    }

    fun recordDeadlock(table1: String, table2: String, operation1: String, operation2: String) {
        deadlocks.increment(Tags.of(
            Tag.of("table1", table1),
            Tag.of("table2", table2),
            Tag.of("operation1", operation1),
            Tag.of("operation2", operation2)
        ))
    }

    fun recordLockWait(table: String, lockType: String, duration: Duration) {
        lockWaitTime.record(duration, Tags.of(
            Tag.of("table", table),
            Tag.of("lock_type", lockType)
        ))
    }

    fun recordQueryCache(hit: Boolean, queryType: String) {
        queryCache.increment(Tags.of(
            Tag.of("hit", hit.toString()),
            Tag.of("query_type", queryType)
        ))
    }

    fun recordTableScan(table: String, rowsScanned: Long, reason: String) {
        tableScans.increment(Tags.of(
            Tag.of("table", table),
            Tag.of("rows_range", getRowsRange(rowsScanned)),
            Tag.of("reason", reason)
        ))
    }

    fun recordReplicationError(errorType: String, severity: String) {
        replicationErrors.increment(Tags.of(
            Tag.of("error_type", errorType),
            Tag.of("severity", severity)
        ))
    }

    fun recordBackup(duration: Duration, size: Long, success: Boolean, type: String) {
        backupDuration.record(duration, Tags.of(
            Tag.of("success", success.toString()),
            Tag.of("type", type)
        ))

        if (success) {
            backupSize.set(size)
            lastBackupAge.set(0)
        }
    }

    // ==================== GAUGE METHODS ====================

    private fun getActiveConnections(): Double = activeConnections.get().toDouble()
    private fun getIdleConnections(): Double = idleConnections.get().toDouble()
    private fun getMaxConnections(): Double = maxConnections.get().toDouble()
    private fun getTableSize(): Double = tableSize.get().toDouble()
    private fun getIndexUsage(): Double = indexUsage.get().toDouble()
    private fun getReplicationLag(): Double = replicationLag.get().toDouble()
    private fun getBackupSize(): Double = backupSize.get().toDouble()
    private fun getLastBackupAge(): Double = lastBackupAge.get().toDouble()

    private fun getConnectionPoolUsage(): Double {
        val active = activeConnections.get().toDouble()
        val max = maxConnections.get().toDouble()
        return if (max > 0) (active / max) * 100.0 else 0.0
    }

    // ==================== HELPER METHODS ====================

    private fun getRowsRange(rows: Long): String = when {
        rows == 0L -> "none"
        rows <= 10 -> "few"
        rows <= 100 -> "some"
        rows <= 1000 -> "many"
        rows <= 10000 -> "lots"
        else -> "massive"
    }

    private fun getOperationCountRange(count: Int): String = when {
        count <= 1 -> "single"
        count <= 5 -> "few"
        count <= 20 -> "some"
        count <= 100 -> "many"
        else -> "batch"
    }

    // ==================== ADMIN METHODS ====================

    fun updateConnectionPoolMetrics(active: Long, idle: Long, max: Long) {
        activeConnections.set(active)
        idleConnections.set(idle)
        maxConnections.set(max)
    }

    fun updateTableMetrics(tableName: String, sizeBytes: Long, indexUsagePercent: Long) {
        tableSize.set(sizeBytes)
        indexUsage.set(indexUsagePercent)
    }

    fun updateReplicationLag(lagSeconds: Long) {
        replicationLag.set(lagSeconds)
    }

    fun updateBackupAge(ageHours: Long) {
        lastBackupAge.set(ageHours)
    }

    // ==================== HEALTH CHECK METHODS ====================

    fun isConnectionPoolHealthy(): Boolean {
        val usage = getConnectionPoolUsage()
        return usage < 90.0 // Alert if pool usage > 90%
    }

    fun isQueryPerformanceHealthy(): Boolean {
        val slowQueryRate = slowQueries.count() / queryExecutionTime.count()
        return slowQueryRate < 0.05 // Alert if > 5% of queries are slow
    }

    fun isReplicationHealthy(): Boolean {
        val lag = replicationLag.get()
        return lag < 60 // Alert if replication lag > 1 minute
    }

    fun isBackupHealthy(): Boolean {
        val age = lastBackupAge.get()
        return age < 24 // Alert if last backup > 24 hours old
    }
}