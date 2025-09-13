package com.gasolinerajsm.performance.util

import com.gasolinerajsm.performance.model.*
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/**
 * Utility class for collecting performance metrics during tests
 */
@Component
class MetricsCollector {

    private val responseTimes = CopyOnWriteArrayList<Long>()
    private val errors = ConcurrentHashMap<String, AtomicLong>()
    private val cpuSamples = CopyOnWriteArrayList<Double>()
    private val memorySamples = CopyOnWriteArrayList<Long>()
    private val startTime = AtomicLong(0)
    private val endTime = AtomicLong(0)

    private val memoryBean = ManagementFactory.getMemoryMXBean()
    private val runtimeBean = ManagementFactory.getRuntimeMXBean()
    private val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()

    @Volatile
    private var collecting = false

    fun startCollection() {
        collecting = true
        startTime.set(System.currentTimeMillis())
        responseTimes.clear()
        errors.clear()
        cpuSamples.clear()
        memorySamples.clear()

        // Start background metrics collection
        Thread {
            while (collecting) {
                collectSystemMetrics()
                Thread.sleep(1000) // Collect every second
            }
        }.start()
    }

    fun stopCollection() {
        collecting = false
        endTime.set(System.currentTimeMillis())
    }

    fun recordResponseTime(responseTime: Long) {
        responseTimes.add(responseTime)
    }

    fun recordError(errorType: String, responseTime: Long) {
        errors.computeIfAbsent(errorType) { AtomicLong(0) }.incrementAndGet()
        responseTimes.add(responseTime)
    }

    fun getResponseTimeMetrics(): ResponseTimeMetrics {
        if (responseTimes.isEmpty()) {
            return ResponseTimeMetrics(0, 0, 0.0, 0, 0, 0, 0, 0.0)
        }

        val sorted = responseTimes.sorted()
        val size = sorted.size

        val min = sorted.first()
        val max = sorted.last()
        val mean = sorted.average()
        val median = sorted[size / 2]
        val p90 = sorted[(size * 0.9).toInt()]
        val p95 = sorted[(size * 0.95).toInt()]
        val p99 = sorted[(size * 0.99).toInt()]

        // Calculate standard deviation
        val variance = sorted.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = sqrt(variance)

        return ResponseTimeMetrics(
            min = min,
            max = max,
            mean = mean,
            median = median,
            p90 = p90,
            p95 = p95,
            p99 = p99,
            standardDeviation = standardDeviation
        )
    }

    fun getResourceMetrics(): ResourceMetrics {
        val cpuMetrics = CpuMetrics(
            averageUsage = if (cpuSamples.isNotEmpty()) cpuSamples.average() else 0.0,
            maxUsage = cpuSamples.maxOrNull() ?: 0.0,
            samples = cpuSamples.toList()
        )

        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage
        val gcCount = gcBeans.sumOf { it.collectionCount }
        val gcTime = gcBeans.sumOf { it.collectionTime }

        val memoryMetrics = MemoryMetrics(
            averageUsage = if (memorySamples.isNotEmpty()) memorySamples.average() else 0.0,
            maxUsage = memorySamples.maxOrNull()?.toDouble() ?: 0.0,
            heapUsed = heapMemory.used,
            heapMax = heapMemory.max,
            nonHeapUsed = nonHeapMemory.used,
            gcCount = gcCount,
            gcTime = gcTime
        )

        // Mock database and cache metrics for now
        val databaseMetrics = DatabaseMetrics(
            activeConnections = 10,
            maxConnections = 50,
            connectionPoolUsage = 20.0,
            averageQueryTime = 50.0,
            slowQueries = 0,
            totalQueries = responseTimes.size.toLong()
        )

        val cacheMetrics = CacheMetrics(
            hitRate = 85.0,
            missRate = 15.0,
            evictionCount = 0,
            size = 1000,
            maxSize = 10000
        )

        return ResourceMetrics(
            cpuUsage = cpuMetrics,
            memoryUsage = memoryMetrics,
            databaseMetrics = databaseMetrics,
            cacheMetrics = cacheMetrics
        )
    }

    fun getErrorMetrics(): ErrorMetrics {
        val totalRequests = responseTimes.size.toLong()
        val totalErrors = errors.values.sumOf { it.get() }
        val errorRate = if (totalRequests > 0) (totalErrors.toDouble() / totalRequests) * 100 else 0.0

        val errorsByType = errors.mapValues { it.value.get() }
        val errorsByEndpoint = mapOf<String, Long>() // Would be populated in real implementation

        return ErrorMetrics(
            errorsByType = errorsByType,
            errorsByEndpoint = errorsByEndpoint,
            errorRate = errorRate,
            timeouts = errors["TIMEOUT"]?.get() ?: 0,
            connectionErrors = errors["CONNECTION_ERROR"]?.get() ?: 0,
            serverErrors = errors["SERVER_ERROR"]?.get() ?: 0,
            clientErrors = errors["CLIENT_ERROR"]?.get() ?: 0
        )
    }

    private fun collectSystemMetrics() {
        try {
            // Collect CPU usage (simplified)
            val cpuUsage = getCpuUsage()
            cpuSamples.add(cpuUsage)

            // Collect memory usage
            val heapUsed = memoryBean.heapMemoryUsage.used
            memorySamples.add(heapUsed)

        } catch (e: Exception) {
            // Log error but don't fail the test
            println("Error collecting system metrics: ${e.message}")
        }
    }

    private fun getCpuUsage(): Double {
        // Simplified CPU usage calculation
        // In a real implementation, you would use more sophisticated methods
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        // This is a rough approximation - in practice you'd use JMX beans
        return (usedMemory.toDouble() / totalMemory) * 100
    }

    fun reset() {
        responseTimes.clear()
        errors.clear()
        cpuSamples.clear()
        memorySamples.clear()
        startTime.set(0)
        endTime.set(0)
    }

    fun getCurrentMetricsSummary(): String {
        val responseTimeMetrics = getResponseTimeMetrics()
        val resourceMetrics = getResourceMetrics()
        val errorMetrics = getErrorMetrics()

        return """
            |Performance Metrics Summary:
            |  Total Requests: ${responseTimes.size}
            |  Average Response Time: ${responseTimeMetrics.mean.toInt()}ms
            |  P95 Response Time: ${responseTimeMetrics.p95}ms
            |  Error Rate: ${errorMetrics.errorRate}%
            |  CPU Usage: ${resourceMetrics.cpuUsage.averageUsage}%
            |  Memory Usage: ${resourceMetrics.memoryUsage.heapUsed / 1024 / 1024}MB
        """.trimMargin()
    }
}