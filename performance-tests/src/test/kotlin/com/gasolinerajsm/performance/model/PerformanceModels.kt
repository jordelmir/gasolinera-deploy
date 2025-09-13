package com.gasolinerajsm.performance.model

import java.time.Duration
import java.time.Instant

/**
 * Performance test data models
 */

data class PerformanceMetrics(
    val testName: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val totalRequests: Long,
    val totalErrors: Long,
    val successRate: Double,
    val requestsPerSecond: Double,
    val responseTimeMetrics: ResponseTimeMetrics,
    val resourceMetrics: ResourceMetrics,
    val errorMetrics: ErrorMetrics
)

data class ResponseTimeMetrics(
    val min: Long,
    val max: Long,
    val mean: Double,
    val median: Long,
    val p90: Long,
    val p95: Long,
    val p99: Long,
    val standardDeviation: Double
)

data class ResourceMetrics(
    val cpuUsage: CpuMetrics,
    val memoryUsage: MemoryMetrics,
    val databaseMetrics: DatabaseMetrics,
    val cacheMetrics: CacheMetrics
)

data class CpuMetrics(
    val averageUsage: Double,
    val maxUsage: Double,
    val samples: List<Double>
)

data class MemoryMetrics(
    val averageUsage: Double,
    val maxUsage: Double,
    val heapUsed: Long,
    val heapMax: Long,
    val nonHeapUsed: Long,
    val gcCount: Long,
    val gcTime: Long
)

data class DatabaseMetrics(
    val activeConnections: Int,
    val maxConnections: Int,
    val connectionPoolUsage: Double,
    val averageQueryTime: Double,
    val slowQueries: Int,
    val totalQueries: Long
)

data class CacheMetrics(
    val hitRate: Double,
    val missRate: Double,
    val evictionCount: Long,
    val size: Long,
    val maxSize: Long
)

data class ErrorMetrics(
    val errorsByType: Map<String, Long>,
    val errorsByEndpoint: Map<String, Long>,
    val errorRate: Double,
    val timeouts: Long,
    val connectionErrors: Long,
    val serverErrors: Long,
    val clientErrors: Long
)

data class LoadTestScenario(
    val name: String,
    val description: String,
    val concurrentUsers: Int,
    val duration: Duration,
    val rampUpTime: Duration,
    val rampDownTime: Duration,
    val requestPattern: RequestPattern,
    val thresholds: PerformanceThresholds
)

data class RequestPattern(
    val type: PatternType,
    val requestsPerSecond: Double,
    val burstSize: Int = 1,
    val burstInterval: Duration = Duration.ofSeconds(1)
)

enum class PatternType {
    CONSTANT,
    RAMP_UP,
    SPIKE,
    BURST,
    WAVE
}

data class PerformanceThresholds(
    val maxResponseTimeP95: Long,
    val maxResponseTimeP99: Long,
    val minThroughput: Double,
    val maxErrorRate: Double,
    val maxCpuUsage: Double,
    val maxMemoryUsage: Double
)

data class TestRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val expectedStatus: Int = 200,
    val timeout: Duration = Duration.ofSeconds(30)
)

data class TestResponse(
    val statusCode: Int,
    val responseTime: Long,
    val body: String?,
    val headers: Map<String, String>,
    val error: String? = null
)

data class UserSession(
    val userId: String,
    val phoneNumber: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val role: String = "CUSTOMER",
    val sessionData: MutableMap<String, Any> = mutableMapOf()
)

data class ConcurrencyTestResult(
    val scenario: String,
    val concurrentUsers: Int,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTime: Double,
    val p95ResponseTime: Long,
    val throughput: Double,
    val errorRate: Double,
    val resourceUsage: ResourceMetrics
)

data class StressTestResult(
    val breakingPoint: Int,
    val maxThroughput: Double,
    val degradationPoint: Int,
    val recoveryTime: Duration,
    val errorsByLoad: Map<Int, Long>,
    val responseTimesByLoad: Map<Int, ResponseTimeMetrics>
)

data class EndpointPerformance(
    val endpoint: String,
    val method: String,
    val totalRequests: Long,
    val averageResponseTime: Double,
    val p95ResponseTime: Long,
    val errorRate: Double,
    val throughput: Double
)

data class DatabasePerformanceResult(
    val queryPerformance: Map<String, QueryMetrics>,
    val connectionPoolMetrics: ConnectionPoolMetrics,
    val transactionMetrics: TransactionMetrics,
    val lockMetrics: LockMetrics
)

data class QueryMetrics(
    val queryType: String,
    val executionCount: Long,
    val averageExecutionTime: Double,
    val maxExecutionTime: Long,
    val slowQueryCount: Long,
    val errorCount: Long
)

data class ConnectionPoolMetrics(
    val activeConnections: Int,
    val idleConnections: Int,
    val totalConnections: Int,
    val maxConnections: Int,
    val connectionWaitTime: Double,
    val connectionLeaks: Int
)

data class TransactionMetrics(
    val totalTransactions: Long,
    val committedTransactions: Long,
    val rolledBackTransactions: Long,
    val averageTransactionTime: Double,
    val deadlocks: Long
)

data class LockMetrics(
    val lockWaitTime: Double,
    val lockTimeouts: Long,
    val deadlockCount: Long
)

data class CachePerformanceResult(
    val hitRate: Double,
    val missRate: Double,
    val averageGetTime: Double,
    val averageSetTime: Double,
    val evictionRate: Double,
    val memoryUsage: Long,
    val keyCount: Long
)