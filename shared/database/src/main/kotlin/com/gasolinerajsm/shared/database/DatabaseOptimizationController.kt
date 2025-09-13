package com.gasolinerajsm.shared.database

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

/**
 * Controlador REST para gestión de optimizaciones de base de datos
 */
@RestController
@RequestMapping("/api/database")
class DatabaseOptimizationController(
    private val databaseAnalyzer: DatabaseAnalyzer,
    private val indexOptimizer: IndexOptimizer,
    private val queryOptimizer: QueryOptimizer,
    private val partitionManager: PartitionManager,
    private val maintenanceService: DatabaseMaintenanceService
) {

    /**
     * Análisis completo de rendimiento de la base de datos
     */
    @GetMapping("/analysis/performance")
    fun analyzePerformance(): ResponseEntity<DatabasePerformanceAnalysis> {
        val analysis = databaseAnalyzer.analyzePerformance()
        return ResponseEntity.ok(analysis)
    }

    /**
     * Análisis de optimización de índices
     */
    @GetMapping("/analysis/indexes")
    fun analyzeIndexes(): ResponseEntity<IndexOptimizationReport> {
        val report = indexOptimizer.analyzeIndexOptimizations()
        return ResponseEntity.ok(report)
    }

    /**
     * Análisis de optimización de queries
     */
    @GetMapping("/analysis/queries")
    fun analyzeQueries(): ResponseEntity<QueryOptimizationReport> {
        val report = queryOptimizer.analyzeQueryPerformance()
        return ResponseEntity.ok(report)
    }

    /**
     * Análisis de particionado
     */
    @GetMapping("/analysis/partitioning")
    fun analyzePartitioning(): ResponseEntity<PartitioningReport> {
        val report = partitionManager.analyzePartitioning()
        return ResponseEntity.ok(report)
    }

    /**
     * Obtiene queries lentas
     */
    @GetMapping("/queries/slow")
    fun getSlowQueries(@RequestParam(defaultValue = "50") limit: Int): ResponseEntity<List<SlowQuery>> {
        val slowQueries = databaseAnalyzer.analyzeSlowQueries().take(limit)
        return ResponseEntity.ok(slowQueries)
    }

    /**
     * Obtiene estadísticas de tablas
     */
    @GetMapping("/tables/statistics")
    fun getTableStatistics(): ResponseEntity<List<TableStatistics>> {
        val stats = databaseAnalyzer.analyzeTableStatistics()
        return ResponseEntity.ok(stats)
    }

    /**
     * Obtiene uso de índices
     */
    @GetMapping("/indexes/usage")
    fun getIndexUsage(): ResponseEntity<List<IndexUsage>> {
        val usage = databaseAnalyzer.analyzeIndexUsage()
        return ResponseEntity.ok(usage)
    }

    /**
     * Obtiene uso de disco por tabla
     */
    @GetMapping("/tables/disk-usage")
    fun getTableDiskUsage(): ResponseEntity<List<TableDiskUsage>> {
        val usage = databaseAnalyzer.analyzeDiskUsage()
        return ResponseEntity.ok(usage)
    }

    /**
     * Identifica candidatos para particionado
     */
    @GetMapping("/partitioning/candidates")
    fun getPartitionCandidates(): ResponseEntity<List<PartitionCandidate>> {
        val candidates = databaseAnalyzer.identifyPartitionCandidates()
        return ResponseEntity.ok(candidates)
    }

    /**
     * Obtiene índices no utilizados
     */
    @GetMapping("/indexes/unused")
    fun getUnusedIndexes(): ResponseEntity<List<UnusedIndex>> {
        val unused = indexOptimizer.findUnusedIndexes()
        return ResponseEntity.ok(unused)
    }

    /**
     * Obtiene índices duplicados
     */
    @GetMapping("/indexes/duplicates")
    fun getDuplicateIndexes(): ResponseEntity<List<DuplicateIndex>> {
        val duplicates = indexOptimizer.findDuplicateIndexes()
        return ResponseEntity.ok(duplicates)
    }

    /**
     * Sugiere índices faltantes
     */
    @GetMapping("/indexes/missing")
    fun getMissingIndexes(): ResponseEntity<List<MissingIndexSuggestion>> {
        val missing = indexOptimizer.suggestMissingIndexes()
        return ResponseEntity.ok(missing)
    }

    /**
     * Crea un índice basado en recomendación
     */
    @PostMapping("/indexes/create")
    fun createIndex(@RequestBody recommendation: IndexRecommendation): ResponseEntity<IndexCreationResult> {
        val result = indexOptimizer.createIndex(recommendation)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Crea particiones para una tabla
     */
    @PostMapping("/partitioning/create")
    fun createPartitions(@RequestBody recommendation: PartitionRecommendation): ResponseEntity<PartitionCreationResult> {
        val result = partitionManager.createPartitions(recommendation)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    /**
     * Ejecuta mantenimiento completo
     */
    @PostMapping("/maintenance/full")
    fun performFullMaintenance(): ResponseEntity<DatabaseMaintenanceReport> {
        val report = maintenanceService.performFullMaintenance()
        return ResponseEntity.ok(report)
    }

    /**
     * Ejecuta mantenimiento de índices
     */
    @PostMapping("/maintenance/indexes")
    fun performIndexMaintenance(): ResponseEntity<IndexMaintenanceResult> {
        val result = maintenanceService.performIndexMaintenance()
        return ResponseEntity.ok(result)
    }

    /**
     * Actualiza estadísticas de tablas
     */
    @PostMapping("/maintenance/statistics")
    fun updateStatistics(): ResponseEntity<StatisticsUpdateResult> {
        val result = maintenanceService.updateTableStatistics()
        return ResponseEntity.ok(result)
    }

    /**
     * Ejecuta vacuum en tablas necesarias
     */
    @PostMapping("/maintenance/vacuum")
    fun performVacuum(): ResponseEntity<VacuumMaintenanceResult> {
        val result = maintenanceService.performVacuumMaintenance()
        return ResponseEntity.ok(result)
    }

    /**
     * Obtiene estado del mantenimiento
     */
    @GetMapping("/maintenance/status")
    fun getMaintenanceStatus(): ResponseEntity<MaintenanceStatus> {
        val status = maintenanceService.getMaintenanceStatus()
        return ResponseEntity.ok(status)
    }

    /**
     * Obtiene recomendaciones de configuración
     */
    @GetMapping("/configuration/recommendations")
    fun getConfigurationRecommendations(): ResponseEntity<List<ConfigurationRecommendation>> {
        val recommendations = queryOptimizer.analyzeConfigurationNeeds()
        return ResponseEntity.ok(recommendations)
    }

    /**
     * Obtiene métricas de conexiones
     */
    @GetMapping("/connections/statistics")
    fun getConnectionStatistics(): ResponseEntity<ConnectionStatistics> {
        val stats = databaseAnalyzer.analyzeConnectionStatistics()
        return ResponseEntity.ok(stats)
    }

    /**
     * Obtiene información de locks activos
     */
    @GetMapping("/locks/active")
    fun getActiveLocks(): ResponseEntity<List<LockInfo>> {
        val locks = databaseAnalyzer.analyzeLocks()
        return ResponseEntity.ok(locks)
    }

    /**
     * Genera reporte de optimización completo
     */
    @GetMapping("/report/optimization")
    fun getOptimizationReport(): ResponseEntity<DatabaseOptimizationReport> {
        val performanceAnalysis = databaseAnalyzer.analyzePerformance()
        val indexOptimization = indexOptimizer.analyzeIndexOptimizations()
        val queryOptimization = queryOptimizer.analyzeQueryPerformance()
        val partitioning = partitionManager.analyzePartitioning()
        val maintenanceStatus = maintenanceService.getMaintenanceStatus()

        val report = DatabaseOptimizationReport(
            performanceAnalysis = performanceAnalysis,
            indexOptimization = indexOptimization,
            queryOptimization = queryOptimization,
            partitioning = partitioning,
            maintenanceStatus = maintenanceStatus,
            overallScore = calculateOverallScore(performanceAnalysis, indexOptimization),
            priorityRecommendations = getPriorityRecommendations(indexOptimization, queryOptimization, partitioning),
            timestamp = java.time.Instant.now()
        )

        return ResponseEntity.ok(report)
    }

    /**
     * Obtiene métricas específicas de Gasolinera JSM
     */
    @GetMapping("/gasolinera/metrics")
    fun getGasolineraMetrics(): ResponseEntity<GasolineraDbMetrics> {
        val tableStats = databaseAnalyzer.analyzeTableStatistics()
        val diskUsage = databaseAnalyzer.analyzeDiskUsage()

        // Métricas específicas de las tablas principales
        val couponsStats = tableStats.find { it.tableName == "coupons" }
        val redemptionsStats = tableStats.find { it.tableName == "redemptions" }
        val usersStats = tableStats.find { it.tableName == "users" }
        val stationsStats = tableStats.find { it.tableName == "stations" }

        val couponsUsage = diskUsage.find { it.tableName == "coupons" }
        val redemptionsUsage = diskUsage.find { it.tableName == "redemptions" }

        val metrics = GasolineraDbMetrics(
            totalCoupons = couponsStats?.liveTuples ?: 0,
            totalRedemptions = redemptionsStats?.liveTuples ?: 0,
            totalUsers = usersStats?.liveTuples ?: 0,
            totalStations = stationsStats?.liveTuples ?: 0,
            couponsTableSize = couponsUsage?.tableSizeBytes ?: 0,
            redemptionsTableSize = redemptionsUsage?.tableSizeBytes ?: 0,
            dailyRedemptions = calculateDailyRedemptions(redemptionsStats),
            avgCouponsPerUser = if ((usersStats?.liveTuples ?: 0) > 0) {
                (couponsStats?.liveTuples ?: 0).toDouble() / (usersStats?.liveTuples ?: 1)
            } else 0.0,
            redemptionRate = if ((couponsStats?.liveTuples ?: 0) > 0) {
                (redemptionsStats?.liveTuples ?: 0).toDouble() / (couponsStats?.liveTuples ?: 1)
            } else 0.0,
            timestamp = java.time.Instant.now()
        )

        return ResponseEntity.ok(metrics)
    }

    private fun calculateOverallScore(
        performance: DatabasePerformanceAnalysis,
        indexOptimization: IndexOptimizationReport
    ): Int {
        var score = 100

        // Penalizar por queries lentas
        score -= (performance.slowQueries.size * 2).coerceAtMost(30)

        // Penalizar por índices no utilizados
        score -= (indexOptimization.unusedIndexes.size * 3).coerceAtMost(20)

        // Penalizar por índices faltantes
        score -= (indexOptimization.missingIndexes.size * 5).coerceAtMost(25)

        // Penalizar por índices duplicados
        score -= (indexOptimization.duplicateIndexes.size * 4).coerceAtMost(15)

        return score.coerceAtLeast(0)
    }

    private fun getPriorityRecommendations(
        indexOptimization: IndexOptimizationReport,
        queryOptimization: QueryOptimizationReport,
        partitioning: PartitioningReport
    ): List<PriorityRecommendation> {
        val recommendations = mutableListOf<PriorityRecommendation>()

        // Recomendaciones de índices de alta prioridad
        indexOptimization.recommendations
            .filter { it.priority == IndexPriority.HIGH || it.priority == IndexPriority.CRITICAL }
            .take(3)
            .forEach { rec ->
                recommendations.add(
                    PriorityRecommendation(
                        type = "INDEX",
                        priority = rec.priority.name,
                        title = "Create index: ${rec.indexName}",
                        description = rec.reason,
                        estimatedImpact = rec.estimatedImpact,
                        action = "Create composite index on ${rec.tableName}(${rec.columns.joinToString(", ")})"
                    )
                )
            }

        // Recomendaciones de particionado de alta prioridad
        partitioning.partitionRecommendations
            .filter { it.priority == PartitionPriority.HIGH || it.priority == PartitionPriority.CRITICAL }
            .take(2)
            .forEach { rec ->
                recommendations.add(
                    PriorityRecommendation(
                        type = "PARTITION",
                        priority = rec.priority.name,
                        title = "Partition table: ${rec.tableName}",
                        description = rec.reason,
                        estimatedImpact = rec.estimatedBenefit,
                        action = "Implement ${rec.strategy} partitioning on ${rec.partitionColumn}"
                    )
                )
            }

        // Recomendaciones de configuración críticas
        queryOptimization.configurationRecommendations
            .filter { it.impact == "High" }
            .take(2)
            .forEach { rec ->
                recommendations.add(
                    PriorityRecommendation(
                        type = "CONFIGURATION",
                        priority = "HIGH",
                        title = "Update configuration: ${rec.parameter}",
                        description = rec.reason,
                        estimatedImpact = rec.impact,
                        action = "Change ${rec.parameter} from ${rec.currentValue} to ${rec.recommendedValue}"
                    )
                )
            }

        return recommendations.sortedBy {
            when (it.priority) {
                "CRITICAL" -> 0
                "HIGH" -> 1
                "MEDIUM" -> 2
                else -> 3
            }
        }.take(5)
    }

    private fun calculateDailyRedemptions(redemptionsStats: TableStatistics?): Long {
        // Estimación simplificada basada en inserts recientes
        return redemptionsStats?.inserts?.div(30) ?: 0 // Aproximación mensual a diaria
    }
}

// Data classes adicionales

data class DatabaseOptimizationReport(
    val performanceAnalysis: DatabasePerformanceAnalysis,
    val indexOptimization: IndexOptimizationReport,
    val queryOptimization: QueryOptimizationReport,
    val partitioning: PartitioningReport,
    val maintenanceStatus: MaintenanceStatus,
    val overallScore: Int,
    val priorityRecommendations: List<PriorityRecommendation>,
    val timestamp: java.time.Instant
)

data class PriorityRecommendation(
    val type: String,
    val priority: String,
    val title: String,
    val description: String,
    val estimatedImpact: String,
    val action: String
)

data class GasolineraDbMetrics(
    val totalCoupons: Long,
    val totalRedemptions: Long,
    val totalUsers: Long,
    val totalStations: Long,
    val couponsTableSize: Long,
    val redemptionsTableSize: Long,
    val dailyRedemptions: Long,
    val avgCouponsPerUser: Double,
    val redemptionRate: Double,
    val timestamp: java.time.Instant
)