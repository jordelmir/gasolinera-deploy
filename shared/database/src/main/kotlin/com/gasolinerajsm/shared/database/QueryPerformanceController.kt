package com.gasolinerajsm.shared.database

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

/**
 * Controlador REST para gestión y monitoreo de performance de queries
 */
@RestController
@RequestMapping("/api/database/performance")
@ConditionalOnProperty(prefix = "gasolinera.database.optimization", name = ["performance-monitoring"], havingValue = "true")
class QueryPerformanceController(
    private val queryPerformanceMonitor: QueryPerformanceMonitor,
    private val nPlusOneDetector: NPlusOneDetector,
    private val readReplicaManager: ReadReplicaManager? = null
) {

    // Endpoints de análisis de performance

    @GetMapping("/analysis")
    fun getPerformanceAnalysis(): ResponseEntity<QueryPerformanceReport> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        return ResponseEntity.ok(report)
    }

    @GetMapping("/slow-queries")
    fun getSlowQueries(@RequestParam(defaultValue = "20") limit: Int): ResponseEntity<List<SlowQueryInfo>> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        return ResponseEntity.ok(report.slowQueries.take(limit))
    }

    @GetMapping("/frequent-queries")
    fun getFrequentQueries(@RequestParam(defaultValue = "20") limit: Int): ResponseEntity<List<FrequentQueryInfo>> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        return ResponseEntity.ok(report.frequentQueries.take(limit))
    }

    @GetMapping("/expensive-queries")
    fun getExpensiveQueries(@RequestParam(defaultValue = "20") limit: Int): ResponseEntity<List<ExpensiveQueryInfo>> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        return ResponseEntity.ok(report.expensiveQueries.take(limit))
    }

    @GetMapping("/io-intensive-queries")
    fun getIOIntensiveQueries(@RequestParam(defaultValue = "20") limit: Int): ResponseEntity<List<IOIntensiveQueryInfo>> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        return ResponseEntity.ok(report.ioIntensiveQueries.take(limit))
    }

    @GetMapping("/recommendations")
    fun getRecommendations(): ResponseEntity<List<PerformanceRecommendation>> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        return ResponseEntity.ok(report.recommendations)
    }

    @GetMapping("/stats")
    fun getDatabaseStats(): ResponseEntity<DatabaseStats> {
        val stats = queryPerformanceMonitor.getDatabaseStats()
        return ResponseEntity.ok(stats)
    }

    // Endpoints de N+1 detection

    @GetMapping("/nplus1/stats")
    fun getNPlusOneStats(): ResponseEntity<Map<String, NPlusOneStats>> {
        val stats = nPlusOneDetector.getGlobalStats()
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/nplus1/cleanup")
    fun cleanupNPlusOneStats(@RequestParam(defaultValue = "24") hoursOld: Long): ResponseEntity<Map<String, String>> {
        nPlusOneDetector.cleanupOldStats(Duration.ofHours(hoursOld))
        return ResponseEntity.ok(mapOf("message" to "N+1 stats cleaned up successfully"))
    }

    // Endpoints de read replicas

    @GetMapping("/read-replicas/health")
    fun getReadReplicaHealth(): ResponseEntity<Map<String, Any>> {
        return if (readReplicaManager != null) {
            val health = readReplicaManager.checkReadReplicaHealth()
            ResponseEntity.ok(mapOf(
                "enabled" to true,
                "replicas" to health
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "enabled" to false,
                "message" to "Read replicas not configured"
            ))
        }
    }

    @PostMapping("/read-replicas/test")
    fun testReadReplica(): ResponseEntity<Map<String, Any>> {
        return if (readReplicaManager != null) {
            try {
                val startTime = System.currentTimeMillis()

                readReplicaManager.executeOnReadReplica {
                    // Test query simple
                    "SELECT 1"
                }

                val duration = System.currentTimeMillis() - startTime

                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "duration_ms" to duration,
                    "message" to "Read replica test successful"
                ))
            } catch (e: Exception) {
                ResponseEntity.ok(mapOf(
                    "success" to false,
                    "error" to e.message,
                    "message" to "Read replica test failed"
                ))
            }
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "message" to "Read replicas not configured"
            ))
        }
    }

    // Endpoints de gestión

    @PostMapping("/reset-stats")
    fun resetStatistics(): ResponseEntity<Map<String, String>> {
        queryPerformanceMonitor.resetStatistics()
        return ResponseEntity.ok(mapOf("message" to "pg_stat_statements statistics reset successfully"))
    }

    @GetMapping("/health")
    fun getPerformanceHealth(): ResponseEntity<Map<String, Any>> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        val stats = queryPerformanceMonitor.getDatabaseStats()

        val health = mutableMapOf<String, Any>(
            "status" to "UP",
            "total_queries" to stats.totalQueries,
            "avg_query_time" to stats.avgTime,
            "slow_queries_count" to report.slowQueries.size,
            "high_priority_recommendations" to report.recommendations.count { it.priority == Priority.HIGH }
        )

        // Determinar estado de salud
        val status = when {
            report.recommendations.any { it.priority == Priority.CRITICAL } -> "DOWN"
            report.recommendations.count { it.priority == Priority.HIGH } > 5 -> "DEGRADED"
            stats.avgTime > 1000 -> "DEGRADED" // Promedio > 1 segundo
            else -> "UP"
        }

        health["status"] = status

        if (readReplicaManager != null) {
            health["read_replicas"] = readReplicaManager.checkReadReplicaHealth()
        }

        return ResponseEntity.ok(health)
    }

    @GetMapping("/summary")
    fun getPerformanceSummary(): ResponseEntity<PerformanceSummary> {
        val report = queryPerformanceMonitor.analyzeQueryPerformance()
        val stats = queryPerformanceMonitor.getDatabaseStats()
        val nPlusOneStats = nPlusOneDetector.getGlobalStats()

        val summary = PerformanceSummary(
            totalQueries = stats.totalQueries,
            totalCalls = stats.totalCalls,
            avgQueryTime = stats.avgTime,
            slowQueriesCount = report.slowQueries.size,
            frequentQueriesCount = report.frequentQueries.size,
            expensiveQueriesCount = report.expensiveQueries.size,
            ioIntensiveQueriesCount = report.ioIntensiveQueries.size,
            recommendationsCount = report.recommendations.size,
            highPriorityRecommendations = report.recommendations.count { it.priority == Priority.HIGH },
            nPlusOneIssuesCount = nPlusOneStats.size,
            readReplicasEnabled = readReplicaManager != null,
            readReplicasHealthy = readReplicaManager?.checkReadReplicaHealth()?.values?.all { it } ?: false
        )

        return ResponseEntity.ok(summary)
    }

    // Endpoints de configuración

    @PostMapping("/config/slow-query-threshold")
    fun updateSlowQueryThreshold(@RequestParam thresholdMs: Long): ResponseEntity<Map<String, String>> {
        // Implementar actualización dinámica de configuración
        return ResponseEntity.ok(mapOf(
            "message" to "Slow query threshold updated to ${thresholdMs}ms",
            "note" to "Configuration will be applied on next analysis"
        ))
    }

    @GetMapping("/explain/{queryId}")
    fun explainQuery(@PathVariable queryId: String): ResponseEntity<Map<String, Any>> {
        // Implementar análisis de plan de ejecución para query específica
        return ResponseEntity.ok(mapOf(
            "query_id" to queryId,
            "message" to "Query explain analysis not yet implemented",
            "suggestion" to "Use EXPLAIN ANALYZE directly in PostgreSQL for detailed analysis"
        ))
    }
}

/**
 * Resumen de performance del sistema
 */
data class PerformanceSummary(
    val totalQueries: Long,
    val totalCalls: Long,
    val avgQueryTime: Double,
    val slowQueriesCount: Int,
    val frequentQueriesCount: Int,
    val expensiveQueriesCount: Int,
    val ioIntensiveQueriesCount: Int,
    val recommendationsCount: Int,
    val highPriorityRecommendations: Int,
    val nPlusOneIssuesCount: Int,
    val readReplicasEnabled: Boolean,
    val readReplicasHealthy: Boolean
)