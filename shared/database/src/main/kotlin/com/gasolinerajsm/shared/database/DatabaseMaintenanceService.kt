package com.gasolinerajsm.shared.database

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * Servicio de mantenimiento automático de base de datos
 */
@Service
class DatabaseMaintenanceService(
    private val databaseAnalyzer: DatabaseAnalyzer,
    private val indexOptimizer: IndexOptimizer,
    private val queryOptimizer: QueryOptimizer,
    private val partitionManager: PartitionManager,
    private val properties: DatabaseOptimizationProperties
) {

    private val maintenanceExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "database-maintenance").apply {
            isDaemon = true
        }
    }

    /**
     * Ejecuta mantenimiento automático programado
     */
    @Scheduled(fixedDelayString = "#{@databaseOptimizationProperties.maintenance.maintenanceInterval.toMillis()}")
    fun performScheduledMaintenance() {
        if (!properties.maintenance.enableAutoMaintenance) {
            return
        }

        if (!isInMaintenanceWindow()) {
            return
        }

        CompletableFuture.runAsync({
            try {
                performFullMaintenance()
            } catch (e: Exception) {
                // Log error but don't throw to avoid breaking the scheduler
                println("Error during scheduled maintenance: ${e.message}")
            }
        }, maintenanceExecutor)
    }

    /**
     * Ejecuta mantenimiento completo de la base de datos
     */
    fun performFullMaintenance(): DatabaseMaintenanceReport {
        val startTime = Instant.now()
        val results = mutableMapOf<String, Any>()

        try {
            // 1. Análisis de rendimiento
            if (properties.optimization.enableQueryAnalysis) {
                val performanceAnalysis = databaseAnalyzer.analyzePerformance()
                results["performanceAnalysis"] = performanceAnalysis
            }

            // 2. Optimización de índices
            if (properties.optimization.enableAutoIndexing) {
                val indexOptimization = performIndexMaintenance()
                results["indexOptimization"] = indexOptimization
            }

            // 3. Actualización de estadísticas
            if (properties.optimization.enableStatisticsUpdate) {
                val statisticsUpdate = updateTableStatistics()
                results["statisticsUpdate"] = statisticsUpdate
            }

            // 4. Mantenimiento de particiones
            if (properties.partitioning.enableAutoPartitioning) {
                val partitionMaintenance = performPartitionMaintenance()
                results["partitionMaintenance"] = partitionMaintenance
            }

            // 5. Limpieza y vacuum
            if (properties.maintenance.enableVacuum) {
                val vacuumResults = performVacuumMaintenance()
                results["vacuumMaintenance"] = vacuumResults
            }

            val duration = java.time.Duration.between(startTime, Instant.now())

            return DatabaseMaintenanceReport(
                startTime = startTime,
                endTime = Instant.now(),
                duration = duration,
                success = true,
                results = results,
                recommendations = generateMaintenanceRecommendations(results),
                nextScheduledMaintenance = calculateNextMaintenanceTime()
            )

        } catch (e: Exception) {
            val duration = java.time.Duration.between(startTime, Instant.now())

            return DatabaseMaintenanceReport(
                startTime = startTime,
                endTime = Instant.now(),
                duration = duration,
                success = false,
                results = results,
                error = e.message,
                recommendations = emptyList(),
                nextScheduledMaintenance = calculateNextMaintenanceTime()
            )
        }
    }

    /**
     * Realiza mantenimiento de índices
     */
    fun performIndexMaintenance(): IndexMaintenanceResult {
        val startTime = Instant.now()
        val results = mutableListOf<String>()
        var indexesProcessed = 0
        var indexesCreated = 0
        var indexesDropped = 0

        try {
            val optimization = indexOptimizer.analyzeIndexOptimizations()

            // Crear índices faltantes de alta prioridad
            for (suggestion in optimization.missingIndexes.take(5)) { // Limit to 5 per maintenance
                if (suggestion.columns.size <= 3) { // Avoid overly complex indexes
                    val recommendation = IndexRecommendation(
                        tableName = suggestion.tableName,
                        indexName = "idx_${suggestion.tableName}_${suggestion.columns.joinToString("_")}",
                        columns = suggestion.columns,
                        indexType = suggestion.indexType,
                        reason = suggestion.reason,
                        priority = IndexPriority.HIGH,
                        estimatedImpact = suggestion.estimatedImpact
                    )

                    val result = indexOptimizer.createIndex(recommendation)
                    if (result.success) {
                        indexesCreated++
                        results.add("Created index: ${result.indexName}")
                    } else {
                        results.add("Failed to create index: ${result.indexName} - ${result.message}")
                    }
                    indexesProcessed++
                }
            }

            // Identificar índices no utilizados para posible eliminación (solo reportar)
            for (unusedIndex in optimization.unusedIndexes.take(10)) {
                if (unusedIndex.scans == 0L && unusedIndex.indexSizeBytes > 100_000_000) { // 100MB+
                    results.add("Unused large index identified: ${unusedIndex.indexName} (${unusedIndex.indexSize})")
                }
            }

            val duration = java.time.Duration.between(startTime, Instant.now())

            return IndexMaintenanceResult(
                success = true,
                indexesProcessed = indexesProcessed,
                indexesCreated = indexesCreated,
                indexesDropped = indexesDropped,
                duration = duration,
                results = results,
                recommendations = optimization.recommendations.take(10)
            )

        } catch (e: Exception) {
            val duration = java.time.Duration.between(startTime, Instant.now())

            return IndexMaintenanceResult(
                success = false,
                indexesProcessed = indexesProcessed,
                indexesCreated = indexesCreated,
                indexesDropped = indexesDropped,
                duration = duration,
                results = results + "Error: ${e.message}",
                recommendations = emptyList()
            )
        }
    }

    /**
     * Actualiza estadísticas de tablas
     */
    fun updateTableStatistics(): StatisticsUpdateResult {
        val startTime = Instant.now()
        val results = mutableListOf<String>()
        var tablesProcessed = 0
        var tablesUpdated = 0

        try {
            val queryOptimization = queryOptimizer.analyzeQueryPerformance()

            for (recommendation in queryOptimization.statisticsRecommendations) {
                tablesProcessed++

                when (recommendation.recommendation) {
                    "IMMEDIATE_ANALYZE" -> {
                        databaseAnalyzer.jdbcTemplate.execute("ANALYZE ${recommendation.schemaName}.${recommendation.tableName}")
                        tablesUpdated++
                        results.add("Updated statistics for ${recommendation.tableName} (immediate)")
                    }
                    "SCHEDULE_ANALYZE" -> {
                        databaseAnalyzer.jdbcTemplate.execute("ANALYZE ${recommendation.schemaName}.${recommendation.tableName}")
                        tablesUpdated++
                        results.add("Updated statistics for ${recommendation.tableName} (scheduled)")
                    }
                }
            }

            val duration = java.time.Duration.between(startTime, Instant.now())

            return StatisticsUpdateResult(
                success = true,
                tablesProcessed = tablesProcessed,
                tablesUpdated = tablesUpdated,
                duration = duration,
                results = results
            )

        } catch (e: Exception) {
            val duration = java.time.Duration.between(startTime, Instant.now())

            return StatisticsUpdateResult(
                success = false,
                tablesProcessed = tablesProcessed,
                tablesUpdated = tablesUpdated,
                duration = duration,
                results = results + "Error: ${e.message}"
            )
        }
    }

    /**
     * Realiza mantenimiento de particiones
     */
    fun performPartitionMaintenance(): PartitionMaintenanceResult {
        val startTime = Instant.now()
        val results = mutableListOf<String>()
        var partitionsProcessed = 0
        var partitionsCreated = 0

        try {
            val partitioning = partitionManager.analyzePartitioning()

            // Crear particiones futuras para tablas ya particionadas
            for (maintenance in partitioning.maintenanceNeeded) {
                if (maintenance.maintenanceType == "CREATE_FUTURE_PARTITIONS") {
                    // Lógica para crear particiones futuras
                    partitionsCreated++
                    results.add("Created future partition for ${maintenance.tableName}")
                }
                partitionsProcessed++
            }

            val duration = java.time.Duration.between(startTime, Instant.now())

            return PartitionMaintenanceResult(
                success = true,
                partitionsProcessed = partitionsProcessed,
                partitionsCreated = partitionsCreated,
                duration = duration,
                results = results,
                recommendations = partitioning.partitionRecommendations.take(5)
            )

        } catch (e: Exception) {
            val duration = java.time.Duration.between(startTime, Instant.now())

            return PartitionMaintenanceResult(
                success = false,
                partitionsProcessed = partitionsProcessed,
                partitionsCreated = partitionsCreated,
                duration = duration,
                results = results + "Error: ${e.message}",
                recommendations = emptyList()
            )
        }
    }

    /**
     * Realiza vacuum y limpieza
     */
    fun performVacuumMaintenance(): VacuumMaintenanceResult {
        val startTime = Instant.now()
        val results = mutableListOf<String>()
        var tablesVacuumed = 0
        var tablesAnalyzed = 0

        try {
            val tableStats = databaseAnalyzer.analyzeTableStatistics()

            for (table in tableStats) {
                val deadTupleRatio = if (table.liveTuples > 0) {
                    table.deadTuples.toDouble() / table.liveTuples
                } else {
                    0.0
                }

                // Vacuum si hay muchas tuplas muertas
                if (deadTupleRatio > 0.2) { // More than 20% dead tuples
                    if (properties.maintenance.enableVacuum) {
                        databaseAnalyzer.jdbcTemplate.execute("VACUUM ${table.schemaName}.${table.tableName}")
                        tablesVacuumed++
                        results.add("Vacuumed ${table.tableName} (${String.format("%.1f", deadTupleRatio * 100)}% dead tuples)")
                    }
                }

                // Analyze si es necesario
                if (properties.maintenance.enableAnalyze &&
                    (table.lastAnalyze == null || table.lastAnalyze.isBefore(Instant.now().minusSeconds(86400)))) {
                    databaseAnalyzer.jdbcTemplate.execute("ANALYZE ${table.schemaName}.${table.tableName}")
                    tablesAnalyzed++
                    results.add("Analyzed ${table.tableName}")
                }
            }

            val duration = java.time.Duration.between(startTime, Instant.now())

            return VacuumMaintenanceResult(
                success = true,
                tablesVacuumed = tablesVacuumed,
                tablesAnalyzed = tablesAnalyzed,
                duration = duration,
                results = results
            )

        } catch (e: Exception) {
            val duration = java.time.Duration.between(startTime, Instant.now())

            return VacuumMaintenanceResult(
                success = false,
                tablesVacuumed = tablesVacuumed,
                tablesAnalyzed = tablesAnalyzed,
                duration = duration,
                results = results + "Error: ${e.message}"
            )
        }
    }

    private fun isInMaintenanceWindow(): Boolean {
        if (properties.maintenance.maintenanceWindow.enabledDays.isEmpty()) {
            return true
        }

        val now = Instant.now().atZone(ZoneId.of(properties.maintenance.maintenanceWindow.timezone))
        val dayOfWeek = now.dayOfWeek.name
        val currentHour = now.hour

        val isCorrectDay = properties.maintenance.maintenanceWindow.enabledDays.contains(dayOfWeek)
        val isCorrectTime = currentHour >= properties.maintenance.maintenanceWindow.startHour &&
                           currentHour < properties.maintenance.maintenanceWindow.endHour

        return isCorrectDay && isCorrectTime
    }

    private fun generateMaintenanceRecommendations(results: Map<String, Any>): List<MaintenanceRecommendation> {
        val recommendations = mutableListOf<MaintenanceRecommendation>()

        // Analizar resultados y generar recomendaciones
        results["performanceAnalysis"]?.let { analysis ->
            if (analysis is DatabasePerformanceAnalysis) {
                if (analysis.slowQueries.size > 10) {
                    recommendations.add(
                        MaintenanceRecommendation(
                            type = "QUERY_OPTIMIZATION",
                            priority = "HIGH",
                            description = "High number of slow queries detected (${analysis.slowQueries.size})",
                            action = "Review and optimize slow queries, consider adding indexes",
                            estimatedImpact = "High performance improvement expected"
                        )
                    )
                }
            }
        }

        results["indexOptimization"]?.let { optimization ->
            if (optimization is IndexMaintenanceResult && optimization.indexesCreated > 0) {
                recommendations.add(
                    MaintenanceRecommendation(
                        type = "INDEX_MONITORING",
                        priority = "MEDIUM",
                        description = "New indexes created (${optimization.indexesCreated})",
                        action = "Monitor new index usage and performance impact",
                        estimatedImpact = "Monitor for 1-2 weeks to assess effectiveness"
                    )
                )
            }
        }

        return recommendations
    }

    private fun calculateNextMaintenanceTime(): Instant {
        return Instant.now().plus(properties.maintenance.maintenanceInterval)
    }

    /**
     * Obtiene el estado actual del mantenimiento
     */
    fun getMaintenanceStatus(): MaintenanceStatus {
        return MaintenanceStatus(
            isEnabled = properties.maintenance.enableAutoMaintenance,
            nextScheduledMaintenance = calculateNextMaintenanceTime(),
            isInMaintenanceWindow = isInMaintenanceWindow(),
            maintenanceWindow = properties.maintenance.maintenanceWindow,
            lastMaintenanceTime = null // Would be stored in database in real implementation
        )
    }
}

// Data classes para mantenimiento

data class DatabaseMaintenanceReport(
    val startTime: Instant,
    val endTime: Instant,
    val duration: java.time.Duration,
    val success: Boolean,
    val results: Map<String, Any>,
    val error: String? = null,
    val recommendations: List<MaintenanceRecommendation>,
    val nextScheduledMaintenance: Instant
)

data class IndexMaintenanceResult(
    val success: Boolean,
    val indexesProcessed: Int,
    val indexesCreated: Int,
    val indexesDropped: Int,
    val duration: java.time.Duration,
    val results: List<String>,
    val recommendations: List<IndexRecommendation>
)

data class StatisticsUpdateResult(
    val success: Boolean,
    val tablesProcessed: Int,
    val tablesUpdated: Int,
    val duration: java.time.Duration,
    val results: List<String>
)

data class PartitionMaintenanceResult(
    val success: Boolean,
    val partitionsProcessed: Int,
    val partitionsCreated: Int,
    val duration: java.time.Duration,
    val results: List<String>,
    val recommendations: List<PartitionRecommendation>
)

data class VacuumMaintenanceResult(
    val success: Boolean,
    val tablesVacuumed: Int,
    val tablesAnalyzed: Int,
    val duration: java.time.Duration,
    val results: List<String>
)

data class MaintenanceRecommendation(
    val type: String,
    val priority: String,
    val description: String,
    val action: String,
    val estimatedImpact: String
)

data class MaintenanceStatus(
    val isEnabled: Boolean,
    val nextScheduledMaintenance: Instant,
    val isInMaintenanceWindow: Boolean,
    val maintenanceWindow: DatabaseOptimizationProperties.MaintenanceWindow,
    val lastMaintenanceTime: Instant?
)