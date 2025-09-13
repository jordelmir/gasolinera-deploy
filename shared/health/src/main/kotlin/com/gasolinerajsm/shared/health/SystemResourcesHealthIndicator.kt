package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.stereotype.Component
import java.io.File
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant
import javax.management.MBeanServer
import javax.management.ObjectName

/**
 * Health indicator para recursos del sistema (CPU, memoria, disco)
 */
@Component
class SystemResourcesHealthIndicator(
    private val properties: HealthProperties.SystemResourcesHealthProperties
) : HealthIndicator {

    private val mBeanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer()
    private var lastCheckTime = Instant.now()
    private var cachedMetrics: SystemMetrics? = null

    override fun health(): Health {
        if (!properties.enabled) {
            return Health.up()
                .withDetail("status", "disabled")
                .build()
        }

        return try {
            val currentTime = Instant.now()

            // Usar cache si el último check fue reciente
            val metrics = if (Duration.between(lastCheckTime, currentTime) < properties.checkInterval) {
                cachedMetrics ?: collectSystemMetrics()
            } else {
                collectSystemMetrics().also {
                    cachedMetrics = it
                    lastCheckTime = currentTime
                }
            }

            // Verificar thresholds
            val cpuHealthy = metrics.cpuUsagePercentage <= properties.cpuThreshold
            val memoryHealthy = metrics.memoryUsagePercentage <= properties.memoryThreshold
            val diskHealthy = metrics.diskUsagePercentage <= properties.diskThreshold

            val isHealthy = cpuHealthy && memoryHealthy && diskHealthy

            val healthBuilder = if (isHealthy) Health.up() else Health.down()

            healthBuilder
                .withDetail("systemResources", "System Metrics")
                .withDetail("cpu", mapOf(
                    "usagePercentage" to metrics.cpuUsagePercentage,
                    "threshold" to properties.cpuThreshold,
                    "isHealthy" to cpuHealthy,
                    "availableProcessors" to metrics.availableProcessors,
                    "systemLoadAverage" to metrics.systemLoadAverage
                ))
                .withDetail("memory", mapOf(
                    "usagePercentage" to metrics.memoryUsagePercentage,
                    "threshold" to properties.memoryThreshold,
                    "isHealthy" to memoryHealthy,
                    "usedMemoryMB" to metrics.usedMemoryMB,
                    "totalMemoryMB" to metrics.totalMemoryMB,
                    "freeMemoryMB" to metrics.freeMemoryMB,
                    "maxMemoryMB" to metrics.maxMemoryMB
                ))
                .withDetail("disk", mapOf(
                    "usagePercentage" to metrics.diskUsagePercentage,
                    "threshold" to properties.diskThreshold,
                    "isHealthy" to diskHealthy,
                    "usedSpaceGB" to metrics.usedDiskSpaceGB,
                    "totalSpaceGB" to metrics.totalDiskSpaceGB,
                    "freeSpaceGB" to metrics.freeDiskSpaceGB
                ))
                .withDetail("jvm", mapOf(
                    "version" to metrics.jvmVersion,
                    "vendor" to metrics.jvmVendor,
                    "uptime" to metrics.jvmUptimeMs,
                    "startTime" to metrics.jvmStartTime,
                    "heapUsagePercentage" to metrics.heapUsagePercentage,
                    "nonHeapUsagePercentage" to metrics.nonHeapUsagePercentage,
                    "gcCollections" to metrics.gcCollections,
                    "gcTime" to metrics.gcTimeMs
                ))
                .withDetail("threads", mapOf(
                    "totalThreads" to metrics.totalThreads,
                    "activeThreads" to metrics.activeThreads,
                    "peakThreads" to metrics.peakThreads,
                    "daemonThreads" to metrics.daemonThreads
                ))
                .withDetail("timestamp", currentTime.toString())
                .build()

        } catch (e: Exception) {
            Health.down()
                .withDetail("systemResources", "System Metrics")
                .withDetail("error", e.message ?: "Unknown error")
                .withDetail("errorType", e.javaClass.simpleName)
                .withDetail("timestamp", Instant.now().toString())
                .withException(e)
                .build()
        }
    }

    private fun collectSystemMetrics(): SystemMetrics {
        val runtime = Runtime.getRuntime()
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        val threadMXBean = ManagementFactory.getThreadMXBean()
        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
        val operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()

        // Métricas de CPU
        val cpuUsage = getCpuUsage()
        val availableProcessors = runtime.availableProcessors()
        val systemLoadAverage = operatingSystemMXBean.systemLoadAverage

        // Métricas de memoria JVM
        val heapMemory = memoryMXBean.heapMemoryUsage
        val nonHeapMemory = memoryMXBean.nonHeapMemoryUsage

        val totalMemoryMB = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMB = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMB = totalMemoryMB - freeMemoryMB
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        val memoryUsagePercentage = (usedMemoryMB.toDouble() / maxMemoryMB * 100)

        val heapUsagePercentage = (heapMemory.used.toDouble() / heapMemory.max * 100)
        val nonHeapUsagePercentage = if (nonHeapMemory.max > 0) {
            (nonHeapMemory.used.toDouble() / nonHeapMemory.max * 100)
        } else {
            0.0
        }

        // Métricas de disco
        val diskMetrics = getDiskMetrics()

        // Métricas de threads
        val totalThreads = threadMXBean.threadCount
        val activeThreads = threadMXBean.threadCount
        val peakThreads = threadMXBean.peakThreadCount
        val daemonThreads = threadMXBean.daemonThreadCount

        // Métricas de GC
        val gcMetrics = getGarbageCollectionMetrics()

        // Información de JVM
        val jvmVersion = System.getProperty("java.version")
        val jvmVendor = System.getProperty("java.vendor")
        val jvmUptimeMs = runtimeMXBean.uptime
        val jvmStartTime = Instant.ofEpochMilli(runtimeMXBean.startTime)

        return SystemMetrics(
            cpuUsagePercentage = cpuUsage,
            availableProcessors = availableProcessors,
            systemLoadAverage = systemLoadAverage,
            memoryUsagePercentage = memoryUsagePercentage,
            usedMemoryMB = usedMemoryMB,
            totalMemoryMB = totalMemoryMB,
            freeMemoryMB = freeMemoryMB,
            maxMemoryMB = maxMemoryMB,
            heapUsagePercentage = heapUsagePercentage,
            nonHeapUsagePercentage = nonHeapUsagePercentage,
            diskUsagePercentage = diskMetrics.usagePercentage,
            usedDiskSpaceGB = diskMetrics.usedSpaceGB,
            totalDiskSpaceGB = diskMetrics.totalSpaceGB,
            freeDiskSpaceGB = diskMetrics.freeSpaceGB,
            totalThreads = totalThreads,
            activeThreads = activeThreads,
            peakThreads = peakThreads,
            daemonThreads = daemonThreads,
            jvmVersion = jvmVersion,
            jvmVendor = jvmVendor,
            jvmUptimeMs = jvmUptimeMs,
            jvmStartTime = jvmStartTime,
            gcCollections = gcMetrics.totalCollections,
            gcTimeMs = gcMetrics.totalTimeMs
        )
    }

    private fun getCpuUsage(): Double {
        return try {
            val osBean = ManagementFactory.getOperatingSystemMXBean()

            // Intentar usar com.sun.management.OperatingSystemMXBean si está disponible
            val method = osBean.javaClass.getMethod("getProcessCpuLoad")
            val cpuLoad = method.invoke(osBean) as Double

            if (cpuLoad >= 0.0) {
                cpuLoad * 100
            } else {
                // Fallback: usar system load average
                val loadAverage = osBean.systemLoadAverage
                if (loadAverage >= 0.0) {
                    (loadAverage / osBean.availableProcessors * 100).coerceAtMost(100.0)
                } else {
                    0.0
                }
            }
        } catch (e: Exception) {
            // Si no se puede obtener, usar 0
            0.0
        }
    }

    private fun getDiskMetrics(): DiskMetrics {
        return try {
            val rootFile = File("/")
            val totalSpace = rootFile.totalSpace
            val freeSpace = rootFile.freeSpace
            val usedSpace = totalSpace - freeSpace

            val totalSpaceGB = totalSpace / (1024.0 * 1024.0 * 1024.0)
            val freeSpaceGB = freeSpace / (1024.0 * 1024.0 * 1024.0)
            val usedSpaceGB = usedSpace / (1024.0 * 1024.0 * 1024.0)
            val usagePercentage = if (totalSpace > 0) {
                (usedSpace.toDouble() / totalSpace * 100)
            } else {
                0.0
            }

            DiskMetrics(
                usagePercentage = usagePercentage,
                usedSpaceGB = usedSpaceGB,
                totalSpaceGB = totalSpaceGB,
                freeSpaceGB = freeSpaceGB
            )
        } catch (e: Exception) {
            DiskMetrics(0.0, 0.0, 0.0, 0.0)
        }
    }

    private fun getGarbageCollectionMetrics(): GcMetrics {
        return try {
            val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
            var totalCollections = 0L
            var totalTimeMs = 0L

            for (gcBean in gcBeans) {
                totalCollections += gcBean.collectionCount
                totalTimeMs += gcBean.collectionTime
            }

            GcMetrics(totalCollections, totalTimeMs)
        } catch (e: Exception) {
            GcMetrics(0L, 0L)
        }
    }

    /**
     * Obtiene métricas detalladas del sistema
     */
    fun getDetailedSystemMetrics(): SystemMetrics {
        return collectSystemMetrics()
    }

    /**
     * Verifica si los recursos del sistema están saludables
     */
    fun areResourcesHealthy(): Boolean {
        val metrics = collectSystemMetrics()
        return metrics.cpuUsagePercentage <= properties.cpuThreshold &&
               metrics.memoryUsagePercentage <= properties.memoryThreshold &&
               metrics.diskUsagePercentage <= properties.diskThreshold
    }
}

/**
 * Métricas completas del sistema
 */
data class SystemMetrics(
    val cpuUsagePercentage: Double,
    val availableProcessors: Int,
    val systemLoadAverage: Double,
    val memoryUsagePercentage: Double,
    val usedMemoryMB: Long,
    val totalMemoryMB: Long,
    val freeMemoryMB: Long,
    val maxMemoryMB: Long,
    val heapUsagePercentage: Double,
    val nonHeapUsagePercentage: Double,
    val diskUsagePercentage: Double,
    val usedDiskSpaceGB: Double,
    val totalDiskSpaceGB: Double,
    val freeDiskSpaceGB: Double,
    val totalThreads: Int,
    val activeThreads: Int,
    val peakThreads: Int,
    val daemonThreads: Int,
    val jvmVersion: String,
    val jvmVendor: String,
    val jvmUptimeMs: Long,
    val jvmStartTime: Instant,
    val gcCollections: Long,
    val gcTimeMs: Long
)

/**
 * Métricas de disco
 */
data class DiskMetrics(
    val usagePercentage: Double,
    val usedSpaceGB: Double,
    val totalSpaceGB: Double,
    val freeSpaceGB: Double
)

/**
 * Métricas de garbage collection
 */
data class GcMetrics(
    val totalCollections: Long,
    val totalTimeMs: Long
)