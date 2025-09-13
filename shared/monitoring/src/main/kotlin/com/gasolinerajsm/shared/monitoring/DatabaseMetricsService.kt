package com.gasolinerajsm.shared.monitoring

import io.micrometer.core.instrument.*
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.sql.DataSource
import java.sql.Connection
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Database Metrics Service
 * Monitors database performance and connection health
 */
@Service
class DatabaseMetricsService(
    private val dataSource: DataSource,
    private val meterRegistry: MeterRegistry,
    private val metricsProperties: MetricsProperties
) : HealthIndicator {

    private val connectionCounters = ConcurrentHashMap<String, Counter>()
    private val queryTimers = ConcurrentHashMap<String, Timer>()
    private val connectionPoolGauges = ConcurrentHashMap<String, AtomicLong>()

    init {
        initializeDatabaseMetrics()
    }

    /**
     * Initialize database metrics
     */
    private fun initializeDatabaseMetrics() {
        if (!metricsProperties.enabled) return

        // Connection metrics
        getOrCreateCounter("db.connections.created", "Database connections created")
        getOrCreateCounter("db.connections.closed", "Database connections closed")
        getOrCreateCounter("db.connections.failed", "Failed database connections")

        // Query metrics
        getOrCreateCounter("db.queries.total", "Total database queries")
        getOrCreateCounter("db.queries.success", "Successful database queries")
        getOrCreateCounter("db.queries.failed", "Failed database queries")
        getOrCreateCounter("db.queries.by_type", "Queries by type", "query_type")

        // Transaction metrics
        getOrCreateCounter("db.transactions.total", "Total database transactions")
        getOrCreateCounter("db.transactions.committed", "Committed transactions")
        getOrCreateCounter("db.transactions.rolled_back", "Rolled back transactions")

        // Connection pool gauges
        registerGauge("db.pool.active", "Active connections in pool")
        registerGauge("db.pool.idle", "Idle connections in pool")
        registerGauge("db.pool.total", "Total connections in pool")
        registerGauge("db.pool.waiting", "Threads waiting for connection")

        // Query performance timers
        getOrCreateTimer("db.query.duration", "Database query duration")
        getOrCreateTimer("db.connection.acquisition.duration", "Connection acquisition duration")
        getOrCreateTimer("db.transaction.duration", "Transaction duration")

        // Slow query metrics
        getOrCreateCounter("db.queries.slow", "Slow database queries")
        getOrCreateCounter("db.queries.timeout", "Query timeouts")
    }

    /**
     * Record database connection event
     */
    fun recordConnectionCreated() {
        incrementCounter("db.connections.created")
    }

    fun recordConnectionClosed() {
        incrementCounter("db.connections.closed")
    }

    fun recordConnectionFailed() {
        incrementCounter("db.connections.failed")
    }

    /**
     * Record database query metrics
     */
    fun recordQuery(queryType: String, success: Boolean, duration: Duration) {
        incrementCounter("db.queries.total")
        incrementCounter("db.queries.by_type", "query_type", queryType)

        if (success) {
            incrementCounter("db.queries.success")
        } else {
            incrementCounter("db.queries.failed")
        }

        recordTimer("db.query.duration", duration, "query_type", queryType)

        // Record slow queries (> 1 second)
        if (duration.toMillis() > 1000) {
            incrementCounter("db.queries.slow", "query_type", queryType)
        }
    }

    /**
     * Record transaction metrics
     */
    fun recordTransaction(committed: Boolean, duration: Duration) {
        incrementCounter("db.transactions.total")

        if (committed) {
            incrementCounter("db.transactions.committed")
        } else {
            incrementCounter("db.transactions.rolled_back")
        }

        recordTimer("db.transaction.duration", duration)
    }

    /**
     * Record connection acquisition time
     */
    fun recordConnectionAcquisition(duration: Duration) {
        recordTimer("db.connection.acquisition.duration", duration)
    }

    /**
     * Record query timeout
     */
    fun recordQueryTimeout(queryType: String) {
        incrementCounter("db.queries.timeout", "query_type", queryType)
    }

    /**
     * Update connection pool metrics
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    fun updateConnectionPoolMetrics() {
        try {
            val poolStats = getConnectionPoolStats()
            updateGauge("db.pool.active", poolStats.active)
            updateGauge("db.pool.idle", poolStats.idle)
            updateGauge("db.pool.total", poolStats.total)
            updateGauge("db.pool.waiting", poolStats.waiting)
        } catch (ex: Exception) {
            // Log error but don't fail
        }
    }

    /**
     * Get connection pool statistics
     */
    private fun getConnectionPoolStats(): ConnectionPoolStats {
        return try {
            // This would integrate with your connection pool (HikariCP, etc.)
            // For now, return mock data
            ConnectionPoolStats(
                active = 5,
                idle = 3,
                total = 8,
                waiting = 0
            )
        } catch (ex: Exception) {
            ConnectionPoolStats(0, 0, 0, 0)
        }
    }

    /**
     * Health check for database
     */
    override fun health(): Health {
        return try {
            val startTime = System.nanoTime()

            dataSource.connection.use { connection ->
                val isValid = connection.isValid(5) // 5 second timeout
                val duration = Duration.ofNanos(System.nanoTime() - startTime)

                if (isValid) {
                    Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("connection_time_ms", duration.toMillis())
                        .withDetail("pool_stats", getConnectionPoolStats())
                        .build()
                } else {
                    Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("error", "Connection validation failed")
                        .build()
                }
            }
        } catch (ex: Exception) {
            Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", ex.message)
                .build()
        }
    }

    /**
     * Get database performance metrics
     */
    fun getDatabaseMetrics(): Map<String, Any> {
        val poolStats = getConnectionPoolStats()

        return mapOf(
            "connection_pool" to mapOf(
                "active" to poolStats.active,
                "idle" to poolStats.idle,
                "total" to poolStats.total,
                "waiting" to poolStats.waiting
            ),
            "queries" to mapOf(
                "total" to getCounterValue("db.queries.total"),
                "success" to getCounterValue("db.queries.success"),
                "failed" to getCounterValue("db.queries.failed"),
                "slow" to getCounterValue("db.queries.slow")
            ),
            "connections" to mapOf(
                "created" to getCounterValue("db.connections.created"),
                "closed" to getCounterValue("db.connections.closed"),
                "failed" to getCounterValue("db.connections.failed")
            ),
            "transactions" to mapOf(
                "total" to getCounterValue("db.transactions.total"),
                "committed" to getCounterValue("db.transactions.committed"),
                "rolled_back" to getCounterValue("db.transactions.rolled_back")
            )
        )
    }

    // ==================== UTILITY METHODS ====================

    private fun getOrCreateCounter(name: String, description: String, vararg tags: String): Counter {
        val key = "$name:${tags.joinToString(":")}"
        return connectionCounters.computeIfAbsent(key) {
            Counter.builder(name)
                .description(description)
                .tags(*tags)
                .register(meterRegistry)
        }
    }

    private fun incrementCounter(name: String, vararg tags: String) {
        getOrCreateCounter(name, "", *tags).increment()
    }

    private fun registerGauge(name: String, description: String, vararg tags: String): AtomicLong {
        val key = "$name:${tags.joinToString(":")}"
        return connectionPoolGauges.computeIfAbsent(key) { _ ->
            val atomicValue = AtomicLong(0)
            Gauge.builder(name)
                .description(description)
                .tags(*tags)
                .register(meterRegistry, atomicValue) { it.get().toDouble() }
            atomicValue
        }
    }

    private fun updateGauge(name: String, value: Long, vararg tags: String) {
        val key = "$name:${tags.joinToString(":")}"
        connectionPoolGauges[key]?.set(value)
    }

    private fun getOrCreateTimer(name: String, description: String, vararg tags: String): Timer {
        val key = "$name:${tags.joinToString(":")}"
        return queryTimers.computeIfAbsent(key) {
            Timer.builder(name)
                .description(description)
                .tags(*tags)
                .register(meterRegistry)
        }
    }

    private fun recordTimer(name: String, duration: Duration, vararg tags: String) {
        getOrCreateTimer(name, "", *tags).record(duration)
    }

    private fun getCounterValue(name: String): Double {
        return meterRegistry.find(name).counter()?.count() ?: 0.0
    }

    data class ConnectionPoolStats(
        val active: Long,
        val idle: Long,
        val total: Long,
        val waiting: Long
    )
}