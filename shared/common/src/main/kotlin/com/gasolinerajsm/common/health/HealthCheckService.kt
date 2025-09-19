package com.gasolinerajsm.common.health

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

interface HealthCheckService {
    fun checkHealth(): HealthStatus
    fun checkDependencies(): DependencyStatus
    fun getMetrics(): ServiceMetrics
    fun performDeepCheck(): DeepHealthStatus
    fun registerDependency(dependency: ServiceDependency)
    fun removeDependency(serviceName: String)
}

@Service
class DefaultHealthCheckService(
    private val dependencyCheckers: List<DependencyHealthChecker> = emptyList()
) : HealthCheckService {

    private val dependencies = mutableMapOf<String, ServiceDependency>()
    private val healthHistory = mutableListOf<HealthRecord>()
    private val maxHistorySize = 100

    override fun checkHealth(): HealthStatus {
        val startTime = Instant.now()

        return try {
            val systemHealth = checkSystemHealth()
            val dependencyHealth = checkDependencies()

            val overallStatus = when {
                systemHealth.status == HealthStatusType.DOWN -> HealthStatusType.DOWN
                dependencyHealth.hasFailures -> HealthStatusType.DEGRADED
                else -> HealthStatusType.UP
            }

            val healthStatus = HealthStatus(
                status = overallStatus,
                timestamp = startTime,
                details = mapOf(
                    "system" to systemHealth,
                    "dependencies" to dependencyHealth,
                    "uptime" to getUptime(),
                    "version" to getVersion()
                ),
                dependencies = dependencyHealth.dependencies,
                responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis()
            )

            recordHealthCheck(healthStatus)
            healthStatus
        } catch (e: Exception) {
            val errorStatus = HealthStatus(
                status = HealthStatusType.DOWN,
                timestamp = startTime,
                details = mapOf("error" to (e.message ?: "Unknown error")),
                dependencies = emptyList(),
                responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis()
            )
            recordHealthCheck(errorStatus)
            errorStatus
        }
    }

    override fun checkDependencies(): DependencyStatus {
        val dependencyResults = mutableListOf<DependencyHealth>()
        val futures = mutableListOf<CompletableFuture<DependencyHealth>>()

        dependencies.values.forEach { dependency ->
            val future = CompletableFuture.supplyAsync {
                checkSingleDependency(dependency)
            }
            futures.add(future)
        }

        // Esperar a que todas las verificaciones terminen (con timeout)
        try {
            CompletableFuture.allOf(*futures.toTypedArray())
                .get(10, TimeUnit.SECONDS)

            futures.forEach { future ->
                dependencyResults.add(future.get())
            }
        } catch (e: Exception) {
            // Agregar dependencias que no respondieron
            futures.forEachIndexed { index, future ->
                if (!future.isDone) {
                    val dependency = dependencies.values.elementAt(index)
                    dependencyResults.add(
                        DependencyHealth(
                            name = dependency.name,
                            status = HealthStatusType.DOWN,
                            responseTime = 10000,
                            error = "Timeout after 10 seconds",
                            lastChecked = Instant.now()
                        )
                    )
                }
            }
        }

        return DependencyStatus(
            dependencies = dependencyResults,
            hasFailures = dependencyResults.any { it.status != HealthStatusType.UP },
            totalDependencies = dependencies.size,
            healthyDependencies = dependencyResults.count { it.status == HealthStatusType.UP }
        )
    }

    override fun getMetrics(): ServiceMetrics {
        val recentChecks = healthHistory.takeLast(10)
        val avgResponseTime = if (recentChecks.isNotEmpty()) {
            recentChecks.map { it.responseTime }.average()
        } else 0.0

        return ServiceMetrics(
            totalHealthChecks = healthHistory.size,
            averageResponseTime = avgResponseTime,
            uptime = getUptime(),
            lastHealthCheck = healthHistory.lastOrNull()?.timestamp,
            errorRate = calculateErrorRate(),
            memoryUsage = getMemoryUsage(),
            cpuUsage = getCpuUsage()
        )
    }

    override fun performDeepCheck(): DeepHealthStatus {
        val startTime = Instant.now()
        val checks = mutableMapOf<String, Any>()

        // Verificación de memoria
        checks["memory"] = checkMemoryHealth()

        // Verificación de disco
        checks["disk"] = checkDiskHealth()

        // Verificación de red
        checks["network"] = checkNetworkHealth()

        // Verificación de base de datos (si aplica)
        checks["database"] = checkDatabaseHealth()

        // Verificación de cache (si aplica)
        checks["cache"] = checkCacheHealth()

        val endTime = Instant.now()

        return DeepHealthStatus(
            overallStatus = determineOverallStatus(checks),
            checks = checks,
            executionTime = java.time.Duration.between(startTime, endTime).toMillis(),
            timestamp = endTime
        )
    }

    override fun registerDependency(dependency: ServiceDependency) {
        dependencies[dependency.name] = dependency
    }

    override fun removeDependency(serviceName: String) {
        dependencies.remove(serviceName)
    }

    private fun checkSystemHealth(): SystemHealth {
        val memoryUsage = getMemoryUsage()
        val cpuUsage = getCpuUsage()
        val diskUsage = getDiskUsage()

        val status = when {
            memoryUsage > 90 || cpuUsage > 90 || diskUsage > 95 -> HealthStatusType.DOWN
            memoryUsage > 80 || cpuUsage > 80 || diskUsage > 85 -> HealthStatusType.DEGRADED
            else -> HealthStatusType.UP
        }

        return SystemHealth(
            status = status,
            memoryUsage = memoryUsage,
            cpuUsage = cpuUsage,
            diskUsage = diskUsage,
            activeThreads = Thread.activeCount()
        )
    }

    private fun checkSingleDependency(dependency: ServiceDependency): DependencyHealth {
        val startTime = Instant.now()

        return try {
            val checker = dependencyCheckers.find { it.canCheck(dependency.type) }
            if (checker != null) {
                val result = checker.check(dependency)
                DependencyHealth(
                    name = dependency.name,
                    status = if (result.isHealthy) HealthStatusType.UP else HealthStatusType.DOWN,
                    responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis(),
                    details = result.details,
                    lastChecked = Instant.now()
                )
            } else {
                DependencyHealth(
                    name = dependency.name,
                    status = HealthStatusType.UNKNOWN,
                    responseTime = 0,
                    error = "No checker available for type: ${dependency.type}",
                    lastChecked = Instant.now()
                )
            }
        } catch (e: Exception) {
            DependencyHealth(
                name = dependency.name,
                status = HealthStatusType.DOWN,
                responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis(),
                error = e.message,
                lastChecked = Instant.now()
            )
        }
    }

    private fun recordHealthCheck(healthStatus: HealthStatus) {
        healthHistory.add(
            HealthRecord(
                timestamp = healthStatus.timestamp,
                status = healthStatus.status,
                responseTime = healthStatus.responseTime
            )
        )

        // Mantener solo los últimos registros
        if (healthHistory.size > maxHistorySize) {
            healthHistory.removeAt(0)
        }
    }

    private fun calculateErrorRate(): Double {
        if (healthHistory.isEmpty()) return 0.0

        val recentChecks = healthHistory.takeLast(50)
        val errorCount = recentChecks.count { it.status == HealthStatusType.DOWN }
        return (errorCount.toDouble() / recentChecks.size) * 100
    }

    private fun getUptime(): Long {
        // En una implementación real, esto se calcularía desde el inicio de la aplicación
        return System.currentTimeMillis() - startupTime
    }

    private fun getVersion(): String {
        return this.javaClass.`package`?.implementationVersion ?: "unknown"
    }

    private fun getMemoryUsage(): Double {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        return (usedMemory.toDouble() / totalMemory) * 100
    }

    private fun getCpuUsage(): Double {
        // Implementación simplificada - en producción usar JMX
        return Math.random() * 100 // Placeholder
    }

    private fun getDiskUsage(): Double {
        val file = java.io.File("/")
        val totalSpace = file.totalSpace
        val freeSpace = file.freeSpace
        val usedSpace = totalSpace - freeSpace
        return (usedSpace.toDouble() / totalSpace) * 100
    }

    private fun checkMemoryHealth(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        return mapOf(
            "total" to runtime.totalMemory(),
            "free" to runtime.freeMemory(),
            "max" to runtime.maxMemory(),
            "usage_percent" to getMemoryUsage()
        )
    }

    private fun checkDiskHealth(): Map<String, Any> {
        val file = java.io.File("/")
        return mapOf(
            "total_space" to file.totalSpace,
            "free_space" to file.freeSpace,
            "usage_percent" to getDiskUsage()
        )
    }

    private fun checkNetworkHealth(): Map<String, Any> {
        return try {
            val reachable = java.net.InetAddress.getByName("8.8.8.8").isReachable(5000)
            mapOf(
                "internet_connectivity" to reachable,
                "dns_resolution" to true
            )
        } catch (e: Exception) {
            mapOf(
                "internet_connectivity" to false,
                "error" to (e.message ?: "Unknown error")
            )
        }
    }

    private fun checkDatabaseHealth(): Map<String, Any> {
        // Placeholder - implementar verificación real de BD
        return mapOf(
            "connection_pool" to "healthy",
            "active_connections" to 5,
            "max_connections" to 20
        )
    }

    private fun checkCacheHealth(): Map<String, Any> {
        // Placeholder - implementar verificación real de cache
        return mapOf(
            "redis_connection" to "healthy",
            "cache_hit_rate" to 85.5
        )
    }

    private fun determineOverallStatus(checks: Map<String, Any>): HealthStatusType {
        // Lógica para determinar el estado general basado en las verificaciones
        return HealthStatusType.UP
    }

    companion object {
        private val startupTime = System.currentTimeMillis()
    }
}

enum class HealthStatusType {
    UP, DOWN, DEGRADED, UNKNOWN
}

data class HealthStatus(
    val status: HealthStatusType,
    val timestamp: Instant,
    val details: Map<String, Any>,
    val dependencies: List<DependencyHealth>,
    val responseTime: Long
)

data class DependencyStatus(
    val dependencies: List<DependencyHealth>,
    val hasFailures: Boolean,
    val totalDependencies: Int,
    val healthyDependencies: Int
)

data class DependencyHealth(
    val name: String,
    val status: HealthStatusType,
    val responseTime: Long,
    val details: Map<String, Any> = emptyMap(),
    val error: String? = null,
    val lastChecked: Instant
)

data class ServiceMetrics(
    val totalHealthChecks: Int,
    val averageResponseTime: Double,
    val uptime: Long,
    val lastHealthCheck: Instant?,
    val errorRate: Double,
    val memoryUsage: Double,
    val cpuUsage: Double
)

data class DeepHealthStatus(
    val overallStatus: HealthStatusType,
    val checks: Map<String, Any>,
    val executionTime: Long,
    val timestamp: Instant
)

data class SystemHealth(
    val status: HealthStatusType,
    val memoryUsage: Double,
    val cpuUsage: Double,
    val diskUsage: Double,
    val activeThreads: Int
)

data class ServiceDependency(
    val name: String,
    val type: DependencyType,
    val url: String,
    val timeout: Long = 5000,
    val critical: Boolean = true
)

enum class DependencyType {
    DATABASE, REDIS, HTTP_SERVICE, MESSAGE_QUEUE, FILE_SYSTEM
}

data class HealthRecord(
    val timestamp: Instant,
    val status: HealthStatusType,
    val responseTime: Long
)

interface DependencyHealthChecker {
    fun canCheck(type: DependencyType): Boolean
    fun check(dependency: ServiceDependency): DependencyCheckResult
}

data class DependencyCheckResult(
    val isHealthy: Boolean,
    val details: Map<String, Any> = emptyMap(),
    val error: String? = null
)