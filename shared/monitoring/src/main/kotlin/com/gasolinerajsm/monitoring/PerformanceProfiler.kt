package com.gasolinerajsm.monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.ThreadMXBean
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Advanced performance profiler for Gasolinera JSM services
 * Provides detailed profiling of memory usage, CPU usage, thread analysis, and method-level performance
 */
@Component
class PerformanceProfiler(
    private val meterRegistry: MeterRegistry,
    private val performanceMetrics: PerformanceMetrics
) {

    private val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    private val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
    private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val activeProfiles = ConcurrentHashMap<String, ProfileSession>()

    // Method execution tracking
    private val methodExecutionTimes = ConcurrentHashMap<String, Timer>()
    private val slowMethodThreshold = Duration.ofMillis(100)

    @PostConstruct
    fun initialize() {
        // Start periodic profiling tasks
        startMemoryProfiling()
        startThreadProfiling()
        startGCProfiling()
        startCPUProfiling()
    }

    @PreDestroy
    fun cleanup() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Profile a specific operation with detailed metrics
     */
    fun <T> profile(operationName: String, operation: () -> T): T {
        val sessionId = startProfiling(operationName)
        return try {
            operation()
        } finally {
            stopProfiling(sessionId)
        }
    }

    /**
     * Start profiling session for long-running operations
     */
    fun startProfiling(operationName: String): String {
        val sessionId = "${operationName}_${System.currentTimeMillis()}"
        val session = ProfileSession(
            sessionId = sessionId,
            operationName = operationName,
            startTime = Instant.now(),
            startMemory = getCurrentMemoryUsage(),
            startCpuTime = getCurrentCpuTime(),
            startThreadCount = threadMXBean.threadCount
        )

        activeProfiles[sessionId] = session
        return sessionId
    }

    /**
     * Stop profiling session and record metrics
     */
    fun stopProfiling(sessionId: String) {
        val session = activeProfiles.remove(sessionId) ?: return

        val endTime = Instant.now()
        val duration = Duration.between(session.startTime, endTime)
        val endMemory = getCurrentMemoryUsage()
        val endCpuTime = getCurrentCpuTime()
        val endThreadCount = threadMXBean.threadCount

        // Record profiling metrics
        recordProfilingMetrics(session, duration, endMemory, endCpuTime, endThreadCount)

        // Check for performance issues
        analyzePerformanceIssues(session, duration, endMemory - session.startMemory)
    }

    /**
     * Profile method execution with automatic slow method detection
     */
    fun <T> profileMethod(className: String, methodName: String, operation: () -> T): T {
        val methodKey = "$className.$methodName"
        val timer = methodExecutionTimes.computeIfAbsent(methodKey) {
            Timer.builder("method.execution.duration")
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry)
        }

        val sample = Timer.Sample.start(meterRegistry)
        val startTime = System.nanoTime()

        return try {
            operation()
        } finally {
            val duration = Duration.ofNanos(System.nanoTime() - startTime)
            sample.stop(timer)

            // Record slow method if threshold exceeded
            if (duration > slowMethodThreshold) {
                recordSlowMethod(className, methodName, duration)
            }
        }
    }

    /**
     * Analyze memory allocation patterns
     */
    fun analyzeMemoryAllocation(): MemoryAnalysis {
        val heapMemory = memoryMXBean.heapMemoryUsage
        val nonHeapMemory = memoryMXBean.nonHeapMemoryUsage

        return MemoryAnalysis(
            heapUsed = heapMemory.used,
            heapMax = heapMemory.max,
            heapCommitted = heapMemory.committed,
            nonHeapUsed = nonHeapMemory.used,
            nonHeapMax = nonHeapMemory.max,
            nonHeapCommitted = nonHeapMemory.committed,
            heapUtilization = heapMemory.used.toDouble() / heapMemory.max.toDouble(),
            gcCollections = getGCCollections(),
            gcTime = getGCTime()
        )
    }

    /**
     * Analyze thread usage patterns
     */
    fun analyzeThreadUsage(): ThreadAnalysis {
        val threadCount = threadMXBean.threadCount
        val peakThreadCount = threadMXBean.peakThreadCount
        val daemonThreadCount = threadMXBean.daemonThreadCount
        val totalStartedThreadCount = threadMXBean.totalStartedThreadCount

        // Analyze thread states
        val threadInfos = threadMXBean.getThreadInfo(threadMXBean.allThreadIds)
        val threadStates = threadInfos.filterNotNull()
            .groupBy { it.threadState }
            .mapValues { it.value.size }

        // Detect blocked threads
        val blockedThreads = threadInfos.filterNotNull()
            .filter { it.threadState == Thread.State.BLOCKED }
            .map { ThreadInfo(it.threadId, it.threadName, it.blockedTime, it.blockedCount) }

        return ThreadAnalysis(
            currentThreadCount = threadCount,
            peakThreadCount = peakThreadCount,
            daemonThreadCount = daemonThreadCount,
            totalStartedThreadCount = totalStartedThreadCount,
            threadStates = threadStates,
            blockedThreads = blockedThreads
        )
    }

    /**
     * Generate performance report
     */
    fun generatePerformanceReport(): PerformanceReport {
        val memoryAnalysis = analyzeMemoryAllocation()
        val threadAnalysis = analyzeThreadUsage()
        val cpuUsage = getCurrentCpuUsage()
        val uptime = Duration.ofMillis(runtimeMXBean.uptime)

        // Get top slow methods
        val slowMethods = getTopSlowMethods(10)

        // Get active profiles
        val activeProfilesCount = activeProfiles.size

        return PerformanceReport(
            timestamp = Instant.now(),
            uptime = uptime,
            memoryAnalysis = memoryAnalysis,
            threadAnalysis = threadAnalysis,
            cpuUsage = cpuUsage,
            slowMethods = slowMethods,
            activeProfilesCount = activeProfilesCount,
            recommendations = generateRecommendations(memoryAnalysis, threadAnalysis, cpuUsage)
        )
    }

    private fun startMemoryProfiling() {
        scheduler.scheduleAtFixedRate({
            try {
                val memoryAnalysis = analyzeMemoryAllocation()
                recordMemoryMetrics(memoryAnalysis)

                // Check for memory leaks
                if (memoryAnalysis.heapUtilization > 0.9) {
                    performanceMetrics.recordError("HIGH_MEMORY_USAGE", "profiler", "memory_monitoring")
                }
            } catch (e: Exception) {
                // Log error but don't fail
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

    private fun startThreadProfiling() {
        scheduler.scheduleAtFixedRate({
            try {
                val threadAnalysis = analyzeThreadUsage()
                recordThreadMetrics(threadAnalysis)

                // Check for thread leaks
                if (threadAnalysis.currentThreadCount > 200) {
                    performanceMetrics.recordError("HIGH_THREAD_COUNT", "profiler", "thread_monitoring")
                }

                // Check for blocked threads
                if (threadAnalysis.blockedThreads.isNotEmpty()) {
                    performanceMetrics.recordError("BLOCKED_THREADS", "profiler", "thread_monitoring")
                }
            } catch (e: Exception) {
                // Log error but don't fail
            }
        }, 0, 60, TimeUnit.SECONDS)
    }

    private fun startGCProfiling() {
        scheduler.scheduleAtFixedRate({
            try {
                val gcCollections = getGCCollections()
                val gcTime = getGCTime()

                meterRegistry.gauge("gc.collections.total", gcCollections.toDouble())
                meterRegistry.gauge("gc.time.total", gcTime.toDouble())

                // Check for excessive GC
                val gcTimeRatio = gcTime.toDouble() / runtimeMXBean.uptime.toDouble()
                if (gcTimeRatio > 0.1) { // More than 10% time in GC
                    performanceMetrics.recordError("EXCESSIVE_GC", "profiler", "gc_monitoring")
                }
            } catch (e: Exception) {
                // Log error but don't fail
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

    private fun startCPUProfiling() {
        scheduler.scheduleAtFixedRate({
            try {
                val cpuUsage = getCurrentCpuUsage()
                meterRegistry.gauge("cpu.usage.current", cpuUsage)

                // Check for high CPU usage
                if (cpuUsage > 0.8) {
                    performanceMetrics.recordError("HIGH_CPU_USAGE", "profiler", "cpu_monitoring")
                }
            } catch (e: Exception) {
                // Log error but don't fail
            }
        }, 0, 15, TimeUnit.SECONDS)
    }

    private fun recordProfilingMetrics(
        session: ProfileSession,
        duration: Duration,
        endMemory: Long,
        endCpuTime: Long,
        endThreadCount: Int
    ) {
        Timer.builder("operation.duration")
            .tag("operation", session.operationName)
            .register(meterRegistry)
            .record(duration)

        meterRegistry.gauge("operation.memory.delta", endMemory - session.startMemory)
        meterRegistry.gauge("operation.cpu.delta", endCpuTime - session.startCpuTime)
        meterRegistry.gauge("operation.thread.delta", endThreadCount - session.startThreadCount)
    }

    private fun analyzePerformanceIssues(session: ProfileSession, duration: Duration, memoryDelta: Long) {
        // Check for slow operations
        if (duration > Duration.ofSeconds(5)) {
            performanceMetrics.recordError("SLOW_OPERATION", "profiler", session.operationName)
        }

        // Check for memory leaks
        if (memoryDelta > 100 * 1024 * 1024) { // 100MB
            performanceMetrics.recordError("HIGH_MEMORY_ALLOCATION", "profiler", session.operationName)
        }
    }

    private fun recordSlowMethod(className: String, methodName: String, duration: Duration) {
        meterRegistry.counter("slow.methods.total",
            "class", className,
            "method", methodName
        ).increment()

        meterRegistry.gauge("slow.methods.duration",
            "class", className,
            "method", methodName,
            duration.toMillis().toDouble()
        )
    }

    private fun recordMemoryMetrics(analysis: MemoryAnalysis) {
        meterRegistry.gauge("memory.heap.used", analysis.heapUsed.toDouble())
        meterRegistry.gauge("memory.heap.max", analysis.heapMax.toDouble())
        meterRegistry.gauge("memory.heap.utilization", analysis.heapUtilization)
        meterRegistry.gauge("memory.nonheap.used", analysis.nonHeapUsed.toDouble())
        meterRegistry.gauge("gc.collections.rate", analysis.gcCollections.toDouble())
        meterRegistry.gauge("gc.time.rate", analysis.gcTime.toDouble())
    }

    private fun recordThreadMetrics(analysis: ThreadAnalysis) {
        meterRegistry.gauge("threads.current", analysis.currentThreadCount.toDouble())
        meterRegistry.gauge("threads.peak", analysis.peakThreadCount.toDouble())
        meterRegistry.gauge("threads.daemon", analysis.daemonThreadCount.toDouble())
        meterRegistry.gauge("threads.blocked", analysis.blockedThreads.size.toDouble())

        // Record thread states
        analysis.threadStates.forEach { (state, count) ->
            meterRegistry.gauge("threads.state", "state", state.name, count.toDouble())
        }
    }

    private fun getCurrentMemoryUsage(): Long {
        return memoryMXBean.heapMemoryUsage.used
    }

    private fun getCurrentCpuTime(): Long {
        return threadMXBean.getCurrentThreadCpuTime()
    }

    private fun getCurrentCpuUsage(): Double {
        // This is a simplified CPU usage calculation
        // In production, you might want to use a more sophisticated approach
        return (runtimeMXBean.systemLoadAverage / Runtime.getRuntime().availableProcessors()).coerceIn(0.0, 1.0)
    }

    private fun getGCCollections(): Long {
        return ManagementFactory.getGarbageCollectorMXBeans()
            .sumOf { it.collectionCount }
    }

    private fun getGCTime(): Long {
        return ManagementFactory.getGarbageCollectorMXBeans()
            .sumOf { it.collectionTime }
    }

    private fun getTopSlowMethods(limit: Int): List<SlowMethodInfo> {
        return methodExecutionTimes.entries
            .map { (methodKey, timer) ->
                SlowMethodInfo(
                    methodName = methodKey,
                    averageDuration = Duration.ofNanos(timer.mean(TimeUnit.NANOSECONDS).toLong()),
                    maxDuration = Duration.ofNanos(timer.max(TimeUnit.NANOSECONDS).toLong()),
                    callCount = timer.count()
                )
            }
            .sortedByDescending { it.averageDuration }
            .take(limit)
    }

    private fun generateRecommendations(
        memoryAnalysis: MemoryAnalysis,
        threadAnalysis: ThreadAnalysis,
        cpuUsage: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (memoryAnalysis.heapUtilization > 0.8) {
            recommendations.add("High heap memory usage detected. Consider increasing heap size or optimizing memory usage.")
        }

        if (threadAnalysis.currentThreadCount > 100) {
            recommendations.add("High thread count detected. Consider using thread pools or async processing.")
        }

        if (threadAnalysis.blockedThreads.isNotEmpty()) {
            recommendations.add("Blocked threads detected. Review synchronization and locking mechanisms.")
        }

        if (cpuUsage > 0.8) {
            recommendations.add("High CPU usage detected. Consider optimizing algorithms or scaling horizontally.")
        }

        if (memoryAnalysis.gcTime > 1000) { // More than 1 second in GC
            recommendations.add("High GC time detected. Consider tuning GC parameters or reducing object allocation.")
        }

        return recommendations
    }
}

// Data classes for analysis results
data class ProfileSession(
    val sessionId: String,
    val operationName: String,
    val startTime: Instant,
    val startMemory: Long,
    val startCpuTime: Long,
    val startThreadCount: Int
)

data class MemoryAnalysis(
    val heapUsed: Long,
    val heapMax: Long,
    val heapCommitted: Long,
    val nonHeapUsed: Long,
    val nonHeapMax: Long,
    val nonHeapCommitted: Long,
    val heapUtilization: Double,
    val gcCollections: Long,
    val gcTime: Long
)

data class ThreadAnalysis(
    val currentThreadCount: Int,
    val peakThreadCount: Int,
    val daemonThreadCount: Int,
    val totalStartedThreadCount: Long,
    val threadStates: Map<Thread.State, Int>,
    val blockedThreads: List<ThreadInfo>
)

data class ThreadInfo(
    val threadId: Long,
    val threadName: String,
    val blockedTime: Long,
    val blockedCount: Long
)

data class SlowMethodInfo(
    val methodName: String,
    val averageDuration: Duration,
    val maxDuration: Duration,
    val callCount: Long
)

data class PerformanceReport(
    val timestamp: Instant,
    val uptime: Duration,
    val memoryAnalysis: MemoryAnalysis,
    val threadAnalysis: ThreadAnalysis,
    val cpuUsage: Double,
    val slowMethods: List<SlowMethodInfo>,
    val activeProfilesCount: Int,
    val recommendations: List<String>
)