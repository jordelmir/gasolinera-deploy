package com.gasolinerajsm.performance.util

import com.gasolinerajsm.performance.model.*
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Utility class for executing load test scenarios
 */
@Component
class LoadTestExecutor {

    suspend fun executeLoadTestScenario(
        scenario: LoadTestScenario,
        requestExecutor: suspend (userIndex: Int) -> TestResponse
    ): ConcurrencyTestResult = coroutineScope {

        println("Executing load test scenario: ${scenario.name}")
        println("Concurrent users: ${scenario.concurrentUsers}")
        println("Duration: ${scenario.duration.seconds} seconds")
        println("Pattern: ${scenario.requestPattern.type}")

        val requestCounter = AtomicLong(0)
        val successCounter = AtomicLong(0)
        val errorCounter = AtomicLong(0)
        val totalResponseTime = AtomicLong(0)
        val responseTimes = mutableListOf<Long>()

        val startTime = System.currentTimeMillis()

        // Execute ramp-up phase
        if (scenario.rampUpTime.seconds > 0) {
            executeRampUp(scenario, requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes)
        }

        // Execute main load phase
        val mainPhaseJobs = (1..scenario.concurrentUsers).map { userIndex ->
            async(Dispatchers.IO) {
                executeUserLoad(
                    userIndex, scenario.duration, scenario.requestPattern,
                    requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes
                )
            }
        }

        mainPhaseJobs.awaitAll()

        // Execute ramp-down phase
        if (scenario.rampDownTime.seconds > 0) {
            executeRampDown(scenario, requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes)
        }

        val endTime = System.currentTimeMillis()
        val actualDuration = Duration.ofMillis(endTime - startTime)

        // Calculate metrics
        val totalRequests = requestCounter.get()
        val successfulRequests = successCounter.get()
        val failedRequests = errorCounter.get()
        val averageResponseTime = if (totalRequests > 0) totalResponseTime.get().toDouble() / totalRequests else 0.0
        val throughput = totalRequests.toDouble() / actualDuration.seconds
        val errorRate = if (totalRequests > 0) (failedRequests.toDouble() / totalRequests) * 100 else 0.0

        // Calculate percentiles
        val sortedResponseTimes = responseTimes.sorted()
        val p95ResponseTime = if (sortedResponseTimes.isNotEmpty()) {
            sortedResponseTimes[(sortedResponseTimes.size * 0.95).toInt()]
        } else 0L

        ConcurrencyTestResult(
            scenario = scenario.name,
            concurrentUsers = scenario.concurrentUsers,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            averageResponseTime = averageResponseTime,
            p95ResponseTime = p95ResponseTime,
            throughput = throughput,
            errorRate = errorRate,
            resourceUsage = ResourceMetrics(
                cpuUsage = CpuMetrics(0.0, 0.0, emptyList()),
                memoryUsage = MemoryMetrics(0.0, 0.0, 0, 0, 0, 0, 0),
                databaseMetrics = DatabaseMetrics(0, 0, 0.0, 0.0, 0, 0),
                cacheMetrics = CacheMetrics(0.0, 0.0, 0, 0, 0)
            )
        )
    }

    private suspend fun executeRampUp(
        scenario: LoadTestScenario,
        requestExecutor: suspend (userIndex: Int) -> TestResponse,
        requestCounter: AtomicLong,
        successCounter: AtomicLong,
        errorCounter: AtomicLong,
        totalResponseTime: AtomicLong,
        responseTimes: MutableList<Long>
    ) {
        val rampUpDurationMs = scenario.rampUpTime.toMillis()
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < rampUpDurationMs) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = elapsed.toDouble() / rampUpDurationMs
            val currentUsers = (scenario.concurrentUsers * progress).toInt()

            val rampUpJobs = (1..currentUsers).map { userIndex ->
                async(Dispatchers.IO) {
                    executeRequest(userIndex, requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes)
                }
            }

            rampUpJobs.awaitAll()
            delay(1000) // 1 second intervals during ramp up
        }
    }

    private suspend fun executeUserLoad(
        userIndex: Int,
        duration: Duration,
        pattern: RequestPattern,
        requestExecutor: suspend (userIndex: Int) -> TestResponse,
        requestCounter: AtomicLong,
        successCounter: AtomicLong,
        errorCounter: AtomicLong,
        totalResponseTime: AtomicLong,
        responseTimes: MutableList<Long>
    ) {
        val endTime = System.currentTimeMillis() + duration.toMillis()

        while (System.currentTimeMillis() < endTime) {
            when (pattern.type) {
                PatternType.CONSTANT -> {
                    executeRequest(userIndex, requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes)
                    delay((1000.0 / pattern.requestsPerSecond).toLong())
                }
                PatternType.BURST -> {
                    repeat(pattern.burstSize) {
                        executeRequest(userIndex, requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes)
                    }
                    delay(pattern.burstInterval.toMillis())
                }
                else -> {
                    executeRequest(userIndex, requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes)
                    delay(100) // Default delay
                }
            }
        }
    }

    private suspend fun executeRampDown(
        scenario: LoadTestScenario,
        requestExecutor: suspend (userIndex: Int) -> TestResponse,
        requestCounter: AtomicLong,
        successCounter: AtomicLong,
        errorCounter: AtomicLong,
        totalResponseTime: AtomicLong,
        responseTimes: MutableList<Long>
    ) {
        val rampDownDurationMs = scenario.rampDownTime.toMillis()
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < rampDownDurationMs) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = elapsed.toDouble() / rampDownDurationMs
            val currentUsers = (scenario.concurrentUsers * (1.0 - progress)).toInt().coerceAtLeast(1)

            val rampDownJobs = (1..currentUsers).map { userIndex ->
                async(Dispatchers.IO) {
                    executeRequest(userIndex, requestExecutor, requestCounter, successCounter, errorCounter, totalResponseTime, responseTimes)
                }
            }

            rampDownJobs.awaitAll()
            delay(1000) // 1 second intervals during ramp down
        }
    }

    private suspend fun executeRequest(
        userIndex: Int,
        requestExecutor: suspend (userIndex: Int) -> TestResponse,
        requestCounter: AtomicLong,
        successCounter: AtomicLong,
        errorCounter: AtomicLong,
        totalResponseTime: AtomicLong,
        responseTimes: MutableList<Long>
    ) {
        try {
            val response = requestExecutor(userIndex)

            requestCounter.incrementAndGet()
            totalResponseTime.addAndGet(response.responseTime)

            synchronized(responseTimes) {
                responseTimes.add(response.responseTime)
            }

            if (response.statusCode in 200..299) {
                successCounter.incrementAndGet()
            } else {
                errorCounter.incrementAndGet()
            }

        } catch (e: Exception) {
            errorCounter.incrementAndGet()
            println("Request execution error for user $userIndex: ${e.message}")
        }
    }
}