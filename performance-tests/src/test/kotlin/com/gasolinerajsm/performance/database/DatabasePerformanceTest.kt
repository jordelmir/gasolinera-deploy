package com.gasolinerajsm.performance.database

import com.gasolinerajsm.performance.base.BasePerformanceTest
import com.gasolinerajsm.performance.model.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource
import kotlin.test.assertTrue

/**
 * Performance tests for database queries and caching effectiveness
 *
 * Tests database performance under various load conditions and
 * validates caching mechanisms are working effectively.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=performance-test"])
@Tag("performance")
@Tag("database")
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest : BasePerformanceTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    @DisplayName("Database query performance under concurrent load")
    fun `should maintain acceptable query performance under concurrent load`() = runBlocking {
        val concurrentQueries = getConcurrentUsers()
        val testDuration = getTestDuration()

        println("Starting database query performance test")
        println("Concurrent queries: $concurrentQueries")
        println("Test duration: ${testDuration.seconds} seconds")

        val queryCounter = AtomicInteger(0)
        val successCounter = AtomicInteger(0)
        val errorCounter = AtomicInteger(0)
        val totalQueryTime = AtomicLong(0)

        // Define test queries with different complexity levels
        val testQueries = listOf(
            // Simple queries
            "SELECT COUNT(*) FROM auth_schema.users" to "simple_count",
            "SELECT * FROM station_schema.stations WHERE status = 'ACTIVE' LIMIT 10" to "simple_select",

            // Medium complexity queries
            "SELECT u.*, s.name as station_name FROM auth_schema.users u LEFT JOIN station_schema.employees e ON u.id = e.user_id LEFT JOIN station_schema.stations s ON e.station_id = s.id WHERE u.role = 'EMPLOYEE' LIMIT 20" to "medium_join",
            "SELECT c.*, cp.name as campaign_name FROM coupon_schema.coupons c JOIN coupon_schema.campaigns cp ON c.campaign_id = cp.id WHERE c.status = 'ACTIVE' AND cp.is_active = true LIMIT 50" to "medium_filter",

            // Complex queries
            "SELECT u.phone_number, COUNT(r.id) as redemption_count, SUM(r.final_amount) as total_spent FROM auth_schema.users u LEFT JOIN redemption_schema.redemptions r ON u.id = r.user_id WHERE r.status = 'COMPLETED' GROUP BY u.id, u.phone_number HAVING COUNT(r.id) > 0 ORDER BY total_spent DESC LIMIT 25" to "complex_aggregation",
            "SELECT s.name, s.city, COUNT(DISTINCT e.id) as employee_count, COUNT(DISTINCT r.id) as redemption_count FROM station_schema.stations s LEFT JOIN station_schema.employees e ON s.id = e.station_id LEFT JOIN redemption_schema.redemptions r ON s.id = r.station_id WHERE s.status = 'ACTIVE' GROUP BY s.id, s.name, s.city ORDER BY redemption_count DESC LIMIT 15" to "complex_multi_join"
        )

        // Execute concurrent queries
        val queryJobs = (1..concurrentQueries).map { queryIndex ->
            async(Dispatchers.IO) {
                val endTime = System.currentTimeMillis() + testDuration.toMillis()
                var localQueryCount = 0

                while (System.currentTimeMillis() < endTime) {
                    val (query, queryType) = testQueries[localQueryCount % testQueries.size]

                    try {
                        val startTime = System.currentTimeMillis()

                        val result = jdbcTemplate.queryForList(query)

                        val queryTime = System.currentTimeMillis() - startTime

                        queryCounter.incrementAndGet()
                        successCounter.incrementAndGet()
                        totalQueryTime.addAndGet(queryTime)
                        recordRequest(queryTime)

                        // Log slow queries
                        if (queryTime > 1000) {
                            println("Slow query detected: $queryType took ${queryTime}ms")
                        }

                        localQueryCount++

                        // Small delay to prevent overwhelming the database
                        delay(10)

                    } catch (e: Exception) {
                        errorCounter.incrementAndGet()
                        recordError(0, "QUERY_ERROR")
                        println("Query error for $queryType: ${e.message}")
                        delay(100) // Longer delay on error
                    }
                }
            }
        }

        queryJobs.awaitAll()

        val totalQueries = queryCounter.get()
        val averageQueryTime = if (totalQueries > 0) totalQueryTime.get().toDouble() / totalQueries else 0.0
        val successRate = if (totalQueries > 0) (successCounter.get().toDouble() / totalQueries) * 100 else 0.0
        val queriesPerSecond = totalQueries.toDouble() / testDuration.seconds

        println("Database query performance test completed:")
        println("  Total queries: $totalQueries")
        println("  Successful queries: ${successCounter.get()}")
        println("  Failed queries: ${errorCounter.get()}")
        println("  Average query time: ${String.format("%.2f", averageQueryTime)}ms")
        println("  Queries per second: ${String.format("%.2f", queriesPerSecond)}")
        println("  Success rate: ${String.format("%.2f", successRate)}%")

        // Validate database performance
        assertTrue(totalQueries > 0, "No queries were executed")
        assertTrue(successRate >= 95.0, "Success rate $successRate% is below 95%")
        assertTrue(averageQueryTime <= 500.0, "Average query time ${averageQueryTime}ms exceeds 500ms threshold")
        assertTrue(queriesPerSecond >= 10.0, "Query throughput ${queriesPerSecond} QPS is below 10 QPS")

        // Test connection pool metrics
        validateConnectionPoolPerformance()
    }

    @Test
    @DisplayName("Database connection pool stress test")
    fun `should handle connection pool exhaustion gracefully`() = runBlocking {
        val maxConnections = 50 // Based on HikariCP configuration
        val overloadFactor = 2 // Try to use 2x max connections
        val concurrentConnections = maxConnections * overloadFactor

        println("Starting connection pool stress test")
        println("Max connections: $maxConnections")
        println("Concurrent connection attempts: $concurrentConnections")

        val connectionCounter = AtomicInteger(0)
        val successCounter = AtomicInteger(0)
        val timeoutCounter = AtomicInteger(0)
        val errorCounter = AtomicInteger(0)

        // Attempt to use more connections than available
        val connectionJobs = (1..concurrentConnections).map { connectionIndex ->
            async(Dispatchers.IO) {
                try {
                    val startTime = System.currentTimeMillis()

                    // Hold connection for a period to stress the pool
                    dataSource.connection.use { connection ->
                        connectionCounter.incrementAndGet()

                        // Execute a query that takes some time
                        val statement = connection.prepareStatement(
                            "SELECT pg_sleep(0.1), COUNT(*) FROM auth_schema.users"
                        )
                        val resultSet = statement.executeQuery()

                        if (resultSet.next()) {
                            successCounter.incrementAndGet()
                        }

                        val connectionTime = System.currentTimeMillis() - startTime
                        recordRequest(connectionTime)

                        // Hold connection briefly to stress pool
                        delay(100)
                    }

                } catch (e: Exception) {
                    val errorType = when {
                        e.message?.contains("timeout") == true -> {
                            timeoutCounter.incrementAndGet()
                            "CONNECTION_TIMEOUT"
                        }
                        e.message?.contains("pool") == true -> {
                            timeoutCounter.incrementAndGet()
                            "POOL_EXHAUSTED"
                        }
                        else -> {
                            errorCounter.incrementAndGet()
                            "CONNECTION_ERROR"
                        }
                    }
                    recordError(0, errorType)
                    println("Connection error: ${e.message}")
                }
            }
        }

        connectionJobs.awaitAll()

        val totalAttempts = connectionCounter.get() + timeoutCounter.get() + errorCounter.get()
        val successRate = if (totalAttempts > 0) (successCounter.get().toDouble() / totalAttempts) * 100 else 0.0
        val timeoutRate = if (totalAttempts > 0) (timeoutCounter.get().toDouble() / totalAttempts) * 100 else 0.0

        println("Connection pool stress test completed:")
        println("  Total connection attempts: $totalAttempts")
        println("  Successful connections: ${successCounter.get()}")
        println("  Connection timeouts: ${timeoutCounter.get()}")
        println("  Connection errors: ${errorCounter.get()}")
        println("  Success rate: ${String.format("%.2f", successRate)}%")
        println("  Timeout rate: ${String.format("%.2f", timeoutRate)}%")

        // Validate connection pool behavior
        assertTrue(totalAttempts > 0, "No connection attempts were made")

        // Under stress, we expect some timeouts but not complete failure
        assertTrue(successCounter.get() > 0, "No successful connections - pool may be misconfigured")

        // Some timeouts are expected when overloading the pool
        if (concurrentConnections > maxConnections) {
            assertTrue(
                timeoutCounter.get() > 0,
                "No connection timeouts occurred - pool limits may not be enforced"
            )
        }

        // But success rate shouldn't be too low
        assertTrue(
            successRate >= 30.0,
            "Success rate $successRate% is too low - pool may be too restrictive"
        )
    }

    @Test
    @DisplayName("Cache performance and effectiveness test")
    fun `should demonstrate effective caching performance`() = runBlocking {
        val cacheTestIterations = 1000
        val uniqueKeys = 100 // Test cache hit/miss ratios

        println("Starting cache performance test")
        println("Test iterations: $cacheTestIterations")
        println("Unique cache keys: $uniqueKeys")

        // Test queries that should benefit from caching
        val cacheableQueries = listOf(
            "SELECT * FROM station_schema.stations WHERE status = 'ACTIVE'" to "active_stations",
            "SELECT * FROM coupon_schema.campaigns WHERE is_active = true" to "active_campaigns",
            "SELECT COUNT(*) FROM auth_schema.users WHERE role = 'CUSTOMER'" to "customer_count",
            "SELECT * FROM raffle_schema.raffles WHERE status = 'OPEN'" to "open_raffles"
        )

        val cacheHits = AtomicInteger(0)
        val cacheMisses = AtomicInteger(0)
        val totalCacheTime = AtomicLong(0)
        val totalDbTime = AtomicLong(0)

        // First pass - populate cache (all misses)
        println("Populating cache...")
        cacheableQueries.forEach { (query, queryType) ->
            repeat(10) {
                try {
                    val startTime = System.currentTimeMillis()
                    jdbcTemplate.queryForList(query)
                    val queryTime = System.currentTimeMillis() - startTime
                    totalDbTime.addAndGet(queryTime)
                    cacheMisses.incrementAndGet()
                } catch (e: Exception) {
                    println("Cache population error for $queryType: ${e.message}")
                }
            }
        }

        // Wait for cache to be populated
        delay(1000)

        // Second pass - test cache hits
        println("Testing cache effectiveness...")
        val cacheTestJobs = (1..cacheTestIterations).map { iteration ->
            async(Dispatchers.IO) {
                val (query, queryType) = cacheableQueries[iteration % cacheableQueries.size]

                try {
                    val startTime = System.currentTimeMillis()

                    // Execute query (should hit cache for repeated queries)
                    val result = jdbcTemplate.queryForList(query)

                    val queryTime = System.currentTimeMillis() - startTime

                    // Classify as cache hit or miss based on response time
                    // This is a simplified heuristic - in practice you'd use cache metrics
                    if (queryTime < 50) { // Fast response likely indicates cache hit
                        cacheHits.incrementAndGet()
                        totalCacheTime.addAndGet(queryTime)
                    } else {
                        cacheMisses.incrementAndGet()
                        totalDbTime.addAndGet(queryTime)
                    }

                    recordRequest(queryTime)

                } catch (e: Exception) {
                    recordError(0, "CACHE_TEST_ERROR")
                    println("Cache test error for $queryType: ${e.message}")
                }

                // Small delay between requests
                delay(5)
            }
        }

        cacheTestJobs.awaitAll()

        val totalRequests = cacheHits.get() + cacheMisses.get()
        val cacheHitRate = if (totalRequests > 0) (cacheHits.get().toDouble() / totalRequests) * 100 else 0.0
        val averageCacheTime = if (cacheHits.get() > 0) totalCacheTime.get().toDouble() / cacheHits.get() else 0.0
        val averageDbTime = if (cacheMisses.get() > 0) totalDbTime.get().toDouble() / cacheMisses.get() else 0.0

        println("Cache performance test completed:")
        println("  Total requests: $totalRequests")
        println("  Cache hits: ${cacheHits.get()}")
        println("  Cache misses: ${cacheMisses.get()}")
        println("  Cache hit rate: ${String.format("%.2f", cacheHitRate)}%")
        println("  Average cache response time: ${String.format("%.2f", averageCacheTime)}ms")
        println("  Average database response time: ${String.format("%.2f", averageDbTime)}ms")

        if (averageCacheTime > 0 && averageDbTime > 0) {
            val speedupFactor = averageDbTime / averageCacheTime
            println("  Cache speedup factor: ${String.format("%.2f", speedupFactor)}x")
        }

        // Validate cache effectiveness
        assertTrue(totalRequests > 0, "No cache test requests were executed")

        // Cache hit rate should be reasonable for repeated queries
        assertTrue(
            cacheHitRate >= 50.0,
            "Cache hit rate $cacheHitRate% is below 50% - caching may not be effective"
        )

        // Cache should be faster than database
        if (cacheHits.get() > 0 && cacheMisses.get() > 0) {
            assertTrue(
                averageCacheTime < averageDbTime,
                "Cache response time ${averageCacheTime}ms is not faster than database time ${averageDbTime}ms"
            )
        }
    }

    private fun validateConnectionPoolPerformance() {
        try {
            // Get connection pool metrics (simplified)
            val activeConnections = getActiveConnectionCount()
            val maxConnections = getMaxConnectionCount()
            val poolUsage = (activeConnections.toDouble() / maxConnections) * 100

            println("Connection pool metrics:")
            println("  Active connections: $activeConnections")
            println("  Max connections: $maxConnections")
            println("  Pool usage: ${String.format("%.2f", poolUsage)}%")

            // Validate pool is not over-utilized
            assertTrue(
                poolUsage <= 90.0,
                "Connection pool usage ${poolUsage}% is above 90% - may need tuning"
            )

        } catch (e: Exception) {
            println("Could not retrieve connection pool metrics: ${e.message}")
        }
    }

    private fun getActiveConnectionCount(): Int {
        return try {
            // This is a simplified approach - in practice you'd use HikariCP metrics
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'",
                Int::class.java
            ) ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getMaxConnectionCount(): Int {
        return try {
            jdbcTemplate.queryForObject(
                "SHOW max_connections",
                String::class.java
            )?.toInt() ?: 100
        } catch (e: Exception) {
            100 // Default assumption
        }
    }
}