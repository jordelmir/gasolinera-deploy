package com.gasolinerajsm.tests.infrastructure

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PerformanceTestSuite {

    companion object {
        private val performanceResults = mutableMapOf<String, PerformanceResult>()

        @JvmStatic
        @AfterAll
        fun generatePerformanceReport() {
            println("\n" + "=".repeat(80))
            println("PERFORMANCE TEST REPORT")
            println("=".repeat(80))

            performanceResults.forEach { (testName, result) ->
                println("$testName:")
                println("  Total requests: ${result.totalRequests}")
                println("  Successful requests: ${result.successfulRequests}")
                println("  Failed requests: ${result.failedRequests}")
                println("  Success rate: ${(result.successfulRequests.toDouble() / result.totalRequests * 100).toInt()}%")
                println("  Average response time: ${result.averageResponseTime}ms")
                println("  Min response time: ${result.minResponseTime}ms")
                println("  Max response time: ${result.maxResponseTime}ms")
                println("  Requests per second: ${result.requestsPerSecond}")
                println()
            }
            println("=".repeat(80))
        }
    }

    @Test
    @Order(1)
    fun `test api gateway performance under load`() = runBlocking {
        val testName = "API Gateway Load Test"
        val baseUrl = "http://localhost:${System.getenv("API_GATEWAY_PORT") ?: "8080"}"

        try {
            // Warm up
            repeat(10) {
                makeHttpRequest("$baseUrl/health")
                delay(100)
            }

            val concurrentUsers = 50
            val requestsPerUser = 20
            val totalRequests = concurrentUsers * requestsPerUser

            val successCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)
            val responseTimes = mutableListOf<Long>()

            val startTime = Instant.now()

            // Launch concurrent requests
            val jobs = (1..concurrentUsers).map { userId ->
                async {
                    repeat(requestsPerUser) { requestId ->
                        val requestStart = Instant.now()
                        try {
                            val responseCode = makeHttpRequest("$baseUrl/health")
                            val responseTime = Duration.between(requestStart, Instant.now()).toMillis()

                            synchronized(responseTimes) {
                                responseTimes.add(responseTime)
                            }

                            if (responseCode in 200..299) {
                                successCount.incrementAndGet()
                            } else {
                                failureCount.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            failureCount.incrementAndGet()
                        }

                        delay(50) // Small delay between requests
                    }
                }
            }

            jobs.awaitAll()

            val endTime = Instant.now()
            val totalDuration = Duration.between(startTime, endTime)

            val avgResponseTime = if (responseTimes.isNotEmpty()) {
                responseTimes.average().toLong()
            } else 0L

            val minResponseTime = responseTimes.minOrNull() ?: 0L
            val maxResponseTime = responseTimes.maxOrNull() ?: 0L
            val requestsPerSecond = if (totalDuration.seconds > 0) {
                totalRequests / totalDuration.seconds
            } else 0L

            val result = PerformanceResult(
                totalRequests = totalRequests,
                successfulRequests = successCount.get(),
                failedRequests = failureCount.get(),
                averageResponseTime = avgResponseTime,
                minResponseTime = minResponseTime,
                maxResponseTime = maxResponseTime,
                requestsPerSecond = requestsPerSecond
            )

            performanceResults[testName] = result

            // Assertions
            assertTrue(successCount.get() > 0, "Should have at least some successful requests")
            assertTrue(avgResponseTime < 5000, "Average response time should be under 5 seconds")
            assertTrue(result.successRate > 80, "Success rate should be above 80%")

        } catch (e: Exception) {
            println("Performance test failed: ${e.message}")
            throw e
        }
    }

    @Test
    @Order(2)
    fun `test database connection pool performance`() = runBlocking {
        val testName = "Database Connection Pool Test"

        try {
            val concurrentConnections = 20
            val operationsPerConnection = 10
            val totalOperations = concurrentConnections * operationsPerConnection

            val successCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)
            val operationTimes = mutableListOf<Long>()

            val startTime = Instant.now()

            val jobs = (1..concurrentConnections).map { connectionId ->
                async {
                    repeat(operationsPerConnection) { opId ->
                        val opStart = Instant.now()
                        try {
                            // Simulate database operation
                            val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/gasolinera_db"
                            val dbUser = System.getenv("DB_USERNAME") ?: "gasolinera_user"
                            val dbPassword = System.getenv("DB_PASSWORD") ?: "gasolinera_password"

                            java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword).use { connection ->
                                connection.createStatement().use { statement ->
                                    statement.executeQuery("SELECT 1").use { resultSet ->
                                        resultSet.next()
                                    }
                                }
                            }

                            val opTime = Duration.between(opStart, Instant.now()).toMillis()
                            synchronized(operationTimes) {
                                operationTimes.add(opTime)
                            }

                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            failureCount.incrementAndGet()
                        }

                        delay(100) // Small delay between operations
                    }
                }
            }

            jobs.awaitAll()

            val endTime = Instant.now()
            val totalDuration = Duration.between(startTime, endTime)

            val avgOperationTime = if (operationTimes.isNotEmpty()) {
                operationTimes.average().toLong()
            } else 0L

            val minOperationTime = operationTimes.minOrNull() ?: 0L
            val maxOperationTime = operationTimes.maxOrNull() ?: 0L
            val operationsPerSecond = if (totalDuration.seconds > 0) {
                totalOperations / totalDuration.seconds
            } else 0L

            val result = PerformanceResult(
                totalRequests = totalOperations,
                successfulRequests = successCount.get(),
                failedRequests = failureCount.get(),
                averageResponseTime = avgOperationTime,
                minResponseTime = minOperationTime,
                maxResponseTime = maxOperationTime,
                requestsPerSecond = operationsPerSecond
            )

            performanceResults[testName] = result

            // Assertions
            assertTrue(successCount.get() > 0, "Should have successful database operations")
            assertTrue(avgOperationTime < 1000, "Average database operation time should be under 1 second")
            assertTrue(result.successRate > 95, "Database success rate should be above 95%")

        } catch (e: Exception) {
            println("Database performance test failed: ${e.message}")
            throw e
        }
    }

    @Test
    @Order(3)
    fun `test memory usage under load`() = runBlocking {
        val testName = "Memory Usage Test"

        try {
            val runtime = Runtime.getRuntime()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()

            // Create memory load
            val dataList = mutableListOf<ByteArray>()

            repeat(100) {
                dataList.add(ByteArray(1024 * 1024)) // 1MB chunks
                delay(10)
            }

            val peakMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = peakMemory - initialMemory

            // Clear memory
            dataList.clear()
            System.gc()
            delay(1000) // Wait for GC

            val finalMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryRecovered = peakMemory - finalMemory

            println("Memory test results:")
            println("  Initial memory: ${initialMemory / 1024 / 1024}MB")
            println("  Peak memory: ${peakMemory / 1024 / 1024}MB")
            println("  Memory increase: ${memoryIncrease / 1024 / 1024}MB")
            println("  Final memory: ${finalMemory / 1024 / 1024}MB")
            println("  Memory recovered: ${memoryRecovered / 1024 / 1024}MB")

            // Assertions
            assertTrue(memoryIncrease > 0, "Memory should increase under load")
            assertTrue(memoryRecovered > memoryIncrease * 0.5, "Should recover at least 50% of allocated memory")

        } catch (e: Exception) {
            println("Memory test failed: ${e.message}")
            throw e
        }
    }

    private suspend fun makeHttpRequest(url: String): Int {
        return withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
    }

    data class PerformanceResult(
        val totalRequests: Int,
        val successfulRequests: Int,
        val failedRequests: Int,
        val averageResponseTime: Long,
        val minResponseTime: Long,
        val maxResponseTime: Long,
        val requestsPerSecond: Long
    ) {
        val successRate: Double
            get() = if (totalRequests > 0) {
                (successfulRequests.toDouble() / totalRequests) * 100
            } else 0.0
    }
}