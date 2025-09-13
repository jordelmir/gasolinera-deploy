package com.gasolinerajsm.shared.monitoring

import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.*
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * System Metrics Service
 * Monitors JVM, system resources, and application health
 */
@Service
class SystemMetricsService(
    private val meterRegistry: MeterRegistry,
    private val metricsProperties: MetricsProperties
) : HealthIndicator {

    private val memoryGauges = mutableMapOf<String, AtomicLong>()
    private val cpuGauges = mutableMapOf<String, AtomicLong>()
    private val diskGauges = mutableMapOf<String, AtomicLong>()

    init {
        initializeSystemMetrics()
        registerJvmMetrics()
    }

    /**
     * Initialize system metrics
     */
    private fun initializeSystemMetrics() {
        if (!metricsProperties.enabled) return

        // Memory metrics
        registerGauge("system.memory.used", "Used system memory")
        registerGauge("system.memory.free", "Free system memory")
        registerGauge("system.memory.total", "Total system memory")
        registerGauge("system.memory.usage.percent", "Memory usage percentage")

        // CPU metrics
        registerGauge("system.cpu.usage.percent", "CPU usage percentage")
        registerGauge("system.cpu.load.1m", "CPU load average 1 minute")
        registerGauge("system.cpu.load.5m", "CPU load average 5 minutes")
        registerGauge("system.cpu.load.15m", "CPU load average 15 minutes")

        // Disk metrics
        registerGauge("system.disk.used", "Used disk space")
        registerGauge("system.disk.free", "Free disk space")
        registerGauge("system.disk.total", "Total disk space")
        registerGauge("system.disk.usage.percent", "Disk usage percentage")

        // JVM specific metrics
        registerGauge("jvm.memory.heap.used", "JVM heap memory used")
        registerGauge("jvm.memory.heap.max", "JVM heap memory max")
        registerGauge("jvm.memory.nonheap.used", "JVM non-heap memory used")
        registerGauge("jvm.memory.nonheap.max", "JVM non-heap memory max")

        // Thread metrics
        registerGauge("jvm.threads.live", "Live JVM threads")
        registerGauge("jvm.threads.daemon", "Daemon JVM threads")
        registerGauge("jvm.threads.peak", "Peak JVM threads")

        // GC metrics
        Counter.builder("jvm.gc.collections.total")
            .description("Total GC collections")
            .tags("gc", "all")
            .register(meterRegistry)

        Timer.builder("jvm.gc.pause")
            .description("GC pause duration")
            .register(meterRegistry)
    }

    /**
     * Register JVM metrics with Micrometer binders
     */
    private fun registerJvmMetrics() {
        // JVM Memory metrics
        JvmMemoryMetrics().bindTo(meterRegistry)

        // JVM GC metrics
        JvmGcMetrics().bindTo(meterRegistry)

        // JVM Thread metrics
        JvmThreadMetrics().bindTo(meterRegistry)

        // JVM Class loading metrics
        ClassLoaderMetrics().bindTo(meterRegistry)

        // System CPU and memory metrics
        ProcessorMetrics().bindTo(meterRegistry)

        // File descriptor metrics
        FileDescriptorMetrics().bindTo(meterRegistry)

        // Uptime metrics
        UptimeMetrics().bindTo(meterRegistry)
    }

    /**
     * Update system metrics periodically
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    fun updateSystemMetrics() {
        try {
            updateMemoryMetrics()
            updateCpuMetrics()
            updateDiskMetrics()
            updateJvmMetrics()
        } catch (ex: Exception) {
            // Log error but don't fail
        }
    }

    /**
     * Update memory metrics
     */
    private fun updateMemoryMetrics() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        updateGauge("system.memory.used", usedMemory)
        updateGauge("system.memory.free", freeMemory)
        updateGauge("system.memory.total", totalMemory)

        val usagePercent = if (maxMemory > 0) (usedMemory * 100.0 / maxMemory).toLong() else 0L
        updateGauge("system.memory.usage.percent", usagePercent)
    }

    /**
     * Update CPU metrics
     */
    private fun updateCpuMetrics() {
        val osBean = ManagementFactory.getOperatingSystemMXBean()

        if (osBean is com.sun.management.OperatingSystemMXBean) {
            val cpuUsage = (osBean.processCpuLoad * 100).toLong()
            updateGauge("system.cpu.usage.percent", cpuUsage)

            val systemLoad = osBean.systemLoadAverage
            if (systemLoad >= 0) {
                updateGauge("system.cpu.load.1m", (systemLoad * 100).toLong())
            }
        }
    }

    /**
     * Update disk metrics
     */
    private fun updateDiskMetrics() {
        val rootPath = java.io.File("/")
        val totalSpace = rootPath.totalSpace
        val freeSpace = rootPath.freeSpace
        val usedSpace = totalSpace - freeSpace

        updateGauge("system.disk.used", usedSpace)
        updateGauge("system.disk.free", freeSpace)
        updateGauge("system.disk.total", totalSpace)

        val usagePercent = if (totalSpace > 0) (usedSpace * 100 / totalSpace) else 0L
        updateGauge("system.disk.usage.percent", usagePercent)
    }

    /**
     * Update JVM-specific metrics
     */
    private fun updateJvmMetrics() {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage

        updateGauge("jvm.memory.heap.used", heapMemory.used)
        updateGauge("jvm.memory.heap.max", heapMemory.max)
        updateGauge("jvm.memory.nonheap.used", nonHeapMemory.used)
        updateGauge("jvm.memory.nonheap.max", nonHeapMemory.max)

        val threadBean = ManagementFactory.getThreadMXBean()
        updateGauge("jvm.threads.live", threadBean.threadCount.toLong())
        updateGauge("jvm.threads.daemon", threadBean.daemonThreadCount.toLong())
        updateGauge("jvm.threads.peak", threadBean.peakThreadCount.toLong())
    }

    /**
     * Health check for system resources
     */
    override fun health(): Health {
        return try {
            val memoryUsage = getMemoryUsagePercent()
            val cpuUsage = getCpuUsagePercent()
            val diskUsage = getDiskUsagePercent()

            val healthBuilder = when {
                memoryUsage > 90 || cpuUsage > 90 || diskUsage > 90 -> Health.down()
                memoryUsage > 80 || cpuUsage > 80 || diskUsage > 80 -> Health.status("WARNING")
                else -> Health.up()
            }

            healthBuilder
                .withDetail("memory_usage_percent", memoryUsage)
                .withDetail("cpu_usage_percent", cpuUsage)
                .withDetail("disk_usage_percent", diskUsage)
                .withDetail("jvm_version", System.getProperty("java.version"))
                .withDetail("os_name", System.getProperty("os.name"))
                .withDetail("available_processors", Runtime.getRuntime().availableProcessors())
                .build()

        } catch (ex: Exception) {
            Health.down()
                .withDetail("error", ex.message)
                .build()
        }
    }

    /**
     * Get current memory usage percentage
     */
    private fun getMemoryUsagePercent(): Long {
        return memoryGauges["system.memory.usage.percent"]?.get() ?: 0L
    }

    /**
     * Get current CPU usage percentage
     */
    private fun getCpuUsagePercent(): Long {
        return cpuGauges["system.cpu.usage.percent"]?.get() ?: 0L
    }

    /**
     * Get current disk usage percentage
     */
    private fun getDiskUsagePercent(): Long {
        return diskGauges["system.disk.usage.percent"]?.get() ?: 0L
    }

    /**
     * Get comprehensive system metrics
     */
    fun getSystemMetrics(): Map<String, Any> {
        return mapOf(
            "memory" to mapOf(
                "used_bytes" to memoryGauges["system.memory.used"]?.get(),
                "free_bytes" to memoryGauges["system.memory.free"]?.get(),
                "total_bytes" to memoryGauges["system.memory.total"]?.get(),
                "usage_percent" to memoryGauges["system.memory.usage.percent"]?.get()
            ),
            "cpu" to mapOf(
                "usage_percent" to cpuGauges["system.cpu.usage.percent"]?.get(),
                "load_1m" to cpuGauges["system.cpu.load.1m"]?.get(),
                "available_processors" to Runtime.getRuntime().availableProcessors()
            ),
            "disk" to mapOf(
                "used_bytes" to diskGauges["system.disk.used"]?.get(),
                "free_bytes" to diskGauges["system.disk.free"]?.get(),
                "total_bytes" to diskGauges["system.disk.total"]?.get(),
                "usage_percent" to diskGauges["system.disk.usage.percent"]?.get()
            ),
            "jvm" to mapOf(
                "heap_used" to memoryGauges["jvm.memory.heap.used"]?.get(),
                "heap_max" to memoryGauges["jvm.memory.heap.max"]?.get(),
                "threads_live" to memoryGauges["jvm.threads.live"]?.get(),
                "threads_peak" to memoryGauges["jvm.threads.peak"]?.get(),
                "version" to System.getProperty("java.version"),
                "uptime_ms" to ManagementFactory.getRuntimeMXBean().uptime
            )
        )
    }

    /**
     * Get performance alerts
     */
    fun getPerformanceAlerts(): List<PerformanceAlert> {
        val alerts = mutableListOf<PerformanceAlert>()

        val memoryUsage = getMemoryUsagePercent()
        val cpuUsage = getCpuUsagePercent()
        val diskUsage = getDiskUsagePercent()

        if (memoryUsage > 90) {
            alerts.add(PerformanceAlert("CRITICAL", "Memory usage is critically high: $memoryUsage%"))
        } else if (memoryUsage > 80) {
            alerts.add(PerformanceAlert("WARNING", "Memory usage is high: $memoryUsage%"))
        }

        if (cpuUsage > 90) {
            alerts.add(PerformanceAlert("CRITICAL", "CPU usage is critically high: $cpuUsage%"))
        } else if (cpuUsage > 80) {
            alerts.add(PerformanceAlert("WARNING", "CPU usage is high: $cpuUsage%"))
        }

        if (diskUsage > 90) {
            alerts.add(PerformanceAlert("CRITICAL", "Disk usage is critically high: $diskUsage%"))
        } else if (diskUsage > 80) {
            alerts.add(PerformanceAlert("WARNING", "Disk usage is high: $diskUsage%"))
        }

        return alerts
    }

    // ==================== UTILITY METHODS ====================

    private fun registerGauge(name: String, description: String): AtomicLong {
        val atomicValue = AtomicLong(0)

        Gauge.builder(name)
            .description(description)
            .register(meterRegistry, atomicValue) { it.get().toDouble() }

        // Store in appropriate map based on metric type
        when {
            name.contains("memory") -> memoryGauges[name] = atomicValue
            name.contains("cpu") -> cpuGauges[name] = atomicValue
            name.contains("disk") -> diskGauges[name] = atomicValue
            else -> memoryGauges[name] = atomicValue // Default to memory map
        }

        return atomicValue
    }

    private fun updateGauge(name: String, value: Long) {
        memoryGauges[name]?.set(value)
            ?: cpuGauges[name]?.set(value)
            ?: diskGauges[name]?.set(value)
    }

    data class PerformanceAlert(
        val level: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )
}