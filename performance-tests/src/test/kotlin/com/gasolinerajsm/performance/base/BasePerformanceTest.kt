package com.gasolinerajsm.performance.base

import com.fasterxml.jackson.databind.ObjectMapper
import com.gasolinerajsm.performance.config.PerformanceTestConfiguration
import com.gasolinerajsm.performance.model.PerformanceMetrics
import com.gasolinerajsm.performance.util.MetricsCollector
import com.gasolinerajsm.performance.util.PerformanceReporter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertTrue

/**
 * Base class for performance tests with infrastructure setup and metrics collection
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.profiles.active=performance-test"
    ]
)
@ActiveProfiles("performance-test")
@Testcontainers
@Import(PerformanceTestConfiguration::class)
abstract class BasePerformanceTest {

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var metricsCollector: MetricsCollector

    @Autowired
    protected lateinit var performanceReporter: PerformanceReporter

    protected lateinit var testStartTime: Instant
    protected lateinit var testEndTime: Instant
    protected val requestCounter = AtomicLong(0)
    protected val errorCounter = AtomicLong(0)

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("gasolinera_perf_db")
            withUsername("perf_user")
            withPassword("perf_password")
            withReuse(true)
            // Performance optimizations
            withCommand(
                "postgres",
                "-c", "shared_buffers=256MB",
                "-c", "effective_cache_size=1GB",
                "-c", "maintenance_work_mem=64MB",
                "-c", "checkpoint_completion_target=0.9",
                "-c", "wal_buffers=16MB",
                "-c", "default_statistics_target=100",
                "-c", "random_page_cost=1.1",
                "-c", "effective_io_concurrency=200",
                "-c", "work_mem=4MB",
                "-c", "min_wal_size=1GB",
                "-c", "max_wal_size=4GB",
                "-c", "max_connections=200"
            )
        }

        @Container
        @JvmStatic
        val redisContainer = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
            withCommand(
                "redis-server",
                "--requirepass", "perf_password",
                "--maxmemory", "512mb",
                "--maxmemory-policy", "allkeys-lru",
                "--save", "60", "1000",
                "--tcp-keepalive", "300"
            )
            withReuse(true)
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Database properties
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)

            // Redis properties
            registry.add("spring.redis.host", redisContainer::getHost)
            registry.add("spring.redis.port", redisContainer::getFirstMappedPort)
            registry.add("spring.redis.password") { "perf_password" }
        }
    }

    @BeforeEach
    fun setUpPerformanceTest() {
        testStartTime = Instant.now()
        requestCounter.set(0)
        errorCounter.set(0)
        metricsCollector.startCollection()
    }

    @AfterEach
    fun tearDownPerformanceTest() {
        testEndTime = Instant.now()
        metricsCollector.stopCollection()

        val metrics = collectTestMetrics()
        performanceReporter.generateReport(metrics)

        // Validate performance thresholds
        validatePerformanceThresholds(metrics)
    }

    /**
     * Get test configuration values
     */
    protected fun getTestDuration(): Duration {
        val seconds = System.getProperty("performance.test.duration", "300").toLong()
        return Duration.ofSeconds(seconds)
    }

    protected fun getConcurrentUsers(): Int {
        return System.getProperty("performance.test.users", "100").toInt()
    }

    protected fun getTestMode(): String {
        return System.getProperty("performance.test.mode", "standard")
    }

    /**
     * Get service URLs for testing
     */
    protected fun getServiceUrl(serviceName: String): String {
        return when (serviceName) {
            "auth-service" -> "http://localhost:8081"
            "coupon-service" -> "http://localhost:8083"
            "redemption-service" -> "http://localhost:8084"
            "api-gateway" -> "http://localhost:8080"
            else -> throw IllegalArgumentException("Unknown service: $serviceName")
        }
    }

    /**
     * Record a successful request
     */
    protected fun recordRequest(responseTime: Long) {
        requestCounter.incrementAndGet()
        metricsCollector.recordResponseTime(responseTime)
    }

    /**
     * Record a failed request
     */
    protected fun recordError(responseTime: Long, error: String) {
        errorCounter.incrementAndGet()
        metricsCollector.recordError(error, responseTime)
    }

    /**
     * Collect test metrics
     */
    private fun collectTestMetrics(): PerformanceMetrics {
        val duration = Duration.between(testStartTime, testEndTime)
        val totalRequests = requestCounter.get()
        val totalErrors = errorCounter.get()

        return PerformanceMetrics(
            testName = this::class.simpleName ?: "UnknownTest",
            startTime = testStartTime,
            endTime = testEndTime,
            duration = duration,
            totalRequests = totalRequests,
            totalErrors = totalErrors,
            successRate = if (totalRequests > 0) ((totalRequests - totalErrors).toDouble() / totalRequests) * 100 else 0.0,
            requestsPerSecond = if (duration.seconds > 0) totalRequests.toDouble() / duration.seconds else 0.0,
            responseTimeMetrics = metricsCollector.getResponseTimeMetrics(),
            resourceMetrics = metricsCollector.getResourceMetrics(),
            errorMetrics = metricsCollector.getErrorMetrics()
        )
    }

    /**
     * Validate performance thresholds
     */
    private fun validatePerformanceThresholds(metrics: PerformanceMetrics) {
        val mode = getTestMode()

        // Response time thresholds
        val p95Threshold = when (mode) {
            "load" -> 1000L // More lenient for load tests
            "stress" -> 2000L // Even more lenient for stress tests
            else -> 500L // Standard threshold
        }

        assertTrue(
            metrics.responseTimeMetrics.p95 <= p95Threshold,
            "P95 response time ${metrics.responseTimeMetrics.p95}ms exceeds threshold ${p95Threshold}ms"
        )

        // Error rate threshold
        val errorRateThreshold = when (mode) {
            "stress" -> 5.0 // Allow higher error rate for stress tests
            else -> 1.0 // Standard threshold
        }

        val errorRate = 100.0 - metrics.successRate
        assertTrue(
            errorRate <= errorRateThreshold,
            "Error rate ${errorRate}% exceeds threshold ${errorRateThreshold}%"
        )

        // Throughput threshold (only for non-stress tests)
        if (mode != "stress") {
            val minThroughput = when (mode) {
                "load" -> 100.0 // Higher expectation for load tests
                else -> 50.0 // Standard threshold
            }

            assertTrue(
                metrics.requestsPerSecond >= minThroughput,
                "Throughput ${metrics.requestsPerSecond} RPS is below threshold ${minThroughput} RPS"
            )
        }
    }

    /**
     * Wait for system to stabilize
     */
    protected fun waitForStabilization(duration: Duration = Duration.ofSeconds(30)) {
        Thread.sleep(duration.toMillis())
    }

    /**
     * Generate test users for performance testing
     */
    protected fun generateTestUsers(count: Int): List<String> {
        return (1..count).map { index ->
            "+525555${String.format("%06d", 100000 + index)}"
        }
    }

    /**
     * Generate test coupon codes
     */
    protected fun generateTestCoupons(count: Int): List<String> {
        return (1..count).map { index ->
            "PERF${String.format("%06d", index)}"
        }
    }
}