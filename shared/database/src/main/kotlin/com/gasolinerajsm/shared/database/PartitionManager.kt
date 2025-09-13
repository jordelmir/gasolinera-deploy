package com.gasolinerajsm.shared.database

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Gestor de particionado de tablas para mejorar el rendimiento
 */
@Component
class PartitionManager(
    private val jdbcTemplate: JdbcTemplate,
    private val properties: DatabaseOptimizationProperties
) {

    /**
     * Analiza y gestiona el particionado de tablas
     */
    fun analyzePartitioning(): PartitioningReport {
        return PartitioningReport(
            partitionCandidates = identifyPartitionCandidates(),
            existingPartitions = analyzeExistingPartitions(),
            partitionRecommendations = generatePartitionRecommendations(),
            maintenanceNeeded = identifyPartitionMaintenance(),
            timestamp = Instant.now()
        )
    }

    /**
     * Identifica tablas candidatas para particionado
     */
    fun identifyPartitionCandidates(): List<PartitionCandidate> {
        val candidates = mutableListOf<PartitionCandidate>()

        // Analizar tablas grandes
        val largeTablesQuery = """
            SELECT
                schemaname,
                tablename,
                n_live_tup as row_count,
                pg_total_relation_size(schemaname||'.'||tablename) as size_bytes,
                pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size_pretty
            FROM pg_stat_user_tables
            WHERE n_live_tup > ? OR pg_total_relation_size(schemaname||'.'||tablename) > ?
            ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
        """.trimIndent()

        val largeTables = jdbcTemplate.query(largeTablesQuery, { rs, _ ->
            TableSizeInfo(
                schemaName = rs.getString("schemaname"),
                tableName = rs.getString("tablename"),
                rowCount = rs.getLong("row_count"),
                sizeBytes = rs.getLong("size_bytes"),
                sizePretty = rs.getString("size_pretty")
            )
        }, properties.partitioning.partitionThreshold, 1_000_000_000L) // 1GB threshold

        // Evaluar cada tabla grande
        for (table in largeTables) {
            val strategy = determineOptimalPartitionStrategy(table)
            if (strategy != null) {
                candidates.add(
                    PartitionCandidate(
                        schemaName = table.schemaName,
                        tableName = table.tableName,
                        rowCount = table.rowCount,
                        sizeBytes = table.sizeBytes,
                        recommendedStrategy = strategy,
                        reason = buildPartitionReason(table, strategy)
                    )
                )
            }
        }

        // Agregar candidatos específicos de Gasolinera JSM
        candidates.addAll(getGasolineraSpecificCandidates())

        return candidates
    }

    /**
     * Analiza particiones existentes
     */
    fun analyzeExistingPartitions(): List<ExistingPartition> {
        val sql = """
            SELECT
                schemaname,
                tablename,
                pg_get_expr(c.relpartbound, c.oid) as partition_expression,
                pg_size_pretty(pg_total_relation_size(c.oid)) as size,
                pg_total_relation_size(c.oid) as size_bytes,
                n_live_tup as row_count
            FROM pg_class c
            JOIN pg_inherits i ON c.oid = i.inhrelid
            JOIN pg_class p ON i.inhparent = p.oid
            JOIN pg_stat_user_tables s ON c.relname = s.tablename
            WHERE c.relkind = 'r'
            ORDER BY schemaname, tablename
        """.trimIndent()

        return try {
            jdbcTemplate.query(sql) { rs, _ ->
                ExistingPartition(
                    schemaName = rs.getString("schemaname"),
                    tableName = rs.getString("tablename"),
                    partitionExpression = rs.getString("partition_expression"),
                    size = rs.getString("size"),
                    sizeBytes = rs.getLong("size_bytes"),
                    rowCount = rs.getLong("row_count")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Genera recomendaciones de particionado
     */
    fun generatePartitionRecommendations(): List<PartitionRecommendation> {
        val recommendations = mutableListOf<PartitionRecommendation>()

        // Recomendaciones específicas para Gasolinera JSM
        recommendations.addAll(generateGasolineraPartitionRecommendations())

        return recommendations
    }

    /**
     * Identifica particiones que necesitan mantenimiento
     */
    fun identifyPartitionMaintenance(): List<PartitionMaintenance> {
        val maintenance = mutableListOf<PartitionMaintenance>()

        val existingPartitions = analyzeExistingPartitions()

        for (partition in existingPartitions) {
            // Verificar si la partición está muy llena
            if (partition.sizeBytes > 10_000_000_000L) { // 10GB
                maintenance.add(
                    PartitionMaintenance(
                        schemaName = partition.schemaName,
                        tableName = partition.tableName,
                        maintenanceType = "SPLIT_PARTITION",
                        reason = "Partition too large: ${partition.size}",
                        priority = MaintenancePriority.MEDIUM,
                        estimatedImpact = "Medium - may require maintenance window"
                    )
                )
            }

            // Verificar particiones antiguas para archivado
            if (isOldPartition(partition)) {
                maintenance.add(
                    PartitionMaintenance(
                        schemaName = partition.schemaName,
                        tableName = partition.tableName,
                        maintenanceType = "ARCHIVE_PARTITION",
                        reason = "Old partition candidate for archival",
                        priority = MaintenancePriority.LOW,
                        estimatedImpact = "Low - can be done during off-peak hours"
                    )
                )
            }
        }

        return maintenance
    }

    /**
     * Crea particiones para una tabla
     */
    fun createPartitions(recommendation: PartitionRecommendation): PartitionCreationResult {
        return try {
            val sql = generatePartitionSQL(recommendation)
            val startTime = System.currentTimeMillis()

            // Ejecutar en transacción
            jdbcTemplate.execute("BEGIN")

            for (statement in sql) {
                jdbcTemplate.execute(statement)
            }

            jdbcTemplate.execute("COMMIT")

            val duration = System.currentTimeMillis() - startTime

            PartitionCreationResult(
                success = true,
                tableName = recommendation.tableName,
                partitionStrategy = recommendation.strategy,
                partitionsCreated = sql.size,
                durationMs = duration,
                message = "Partitions created successfully"
            )
        } catch (e: Exception) {
            jdbcTemplate.execute("ROLLBACK")

            PartitionCreationResult(
                success = false,
                tableName = recommendation.tableName,
                partitionStrategy = recommendation.strategy,
                partitionsCreated = 0,
                durationMs = 0,
                message = "Failed to create partitions: ${e.message}",
                error = e.message
            )
        }
    }

    private fun determineOptimalPartitionStrategy(table: TableSizeInfo): PartitionStrategy? {
        // Lógica para determinar la mejor estrategia basada en el nombre y uso de la tabla
        return when {
            // Tablas con datos temporales
            table.tableName.contains("log") ||
            table.tableName.contains("audit") ||
            table.tableName.contains("event") ||
            table.tableName.contains("redemption") -> PartitionStrategy.TIME_BASED

            // Tablas muy grandes con distribución uniforme
            table.rowCount > 50_000_000 -> PartitionStrategy.HASH_BASED

            // Tablas con rangos naturales
            table.tableName.contains("coupon") ||
            table.tableName.contains("user") -> PartitionStrategy.RANGE_BASED

            else -> null
        }
    }

    private fun buildPartitionReason(table: TableSizeInfo, strategy: PartitionStrategy): String {
        val reasons = mutableListOf<String>()

        if (table.rowCount > properties.partitioning.partitionThreshold) {
            reasons.add("High row count: ${table.rowCount}")
        }

        if (table.sizeBytes > 1_000_000_000) {
            reasons.add("Large size: ${table.sizePretty}")
        }

        reasons.add("Recommended strategy: ${strategy.name}")

        return reasons.joinToString(", ")
    }

    private fun getGasolineraSpecificCandidates(): List<PartitionCandidate> {
        return listOf(
            PartitionCandidate(
                schemaName = "public",
                tableName = "redemptions",
                rowCount = 0, // Will be filled by actual analysis
                sizeBytes = 0,
                recommendedStrategy = PartitionStrategy.TIME_BASED,
                reason = "High-volume transactional table with time-based queries"
            ),
            PartitionCandidate(
                schemaName = "public",
                tableName = "audit_logs",
                rowCount = 0,
                sizeBytes = 0,
                recommendedStrategy = PartitionStrategy.TIME_BASED,
                reason = "Audit table with time-based retention requirements"
            ),
            PartitionCandidate(
                schemaName = "public",
                tableName = "user_activities",
                rowCount = 0,
                sizeBytes = 0,
                recommendedStrategy = PartitionStrategy.TIME_BASED,
                reason = "Activity tracking with time-based analysis patterns"
            )
        )
    }

    private fun generateGasolineraPartitionRecommendations(): List<PartitionRecommendation> {
        return listOf(
            PartitionRecommendation(
                tableName = "redemptions",
                strategy = PartitionStrategy.TIME_BASED,
                partitionColumn = "redeemed_at",
                partitionInterval = "MONTHLY",
                retentionPeriod = "2 YEARS",
                reason = "High-volume redemption data with time-based queries and reporting",
                priority = PartitionPriority.HIGH,
                estimatedBenefit = "Significant improvement in query performance and maintenance operations",
                implementationSteps = listOf(
                    "1. Create partitioned table structure",
                    "2. Create monthly partitions for current and future months",
                    "3. Migrate existing data using pg_dump/restore",
                    "4. Update application queries if needed",
                    "5. Set up automated partition management"
                )
            ),
            PartitionRecommendation(
                tableName = "coupons",
                strategy = PartitionStrategy.RANGE_BASED,
                partitionColumn = "user_id",
                partitionInterval = "HASH_4",
                retentionPeriod = "INDEFINITE",
                reason = "Large coupon table with user-based access patterns",
                priority = PartitionPriority.MEDIUM,
                estimatedBenefit = "Improved performance for user-specific coupon queries",
                implementationSteps = listOf(
                    "1. Analyze user_id distribution",
                    "2. Create hash-partitioned table with 4 partitions",
                    "3. Migrate data in batches",
                    "4. Update indexes on each partition",
                    "5. Monitor query performance"
                )
            ),
            PartitionRecommendation(
                tableName = "raffle_tickets",
                strategy = PartitionStrategy.TIME_BASED,
                partitionColumn = "created_at",
                partitionInterval = "QUARTERLY",
                retentionPeriod = "5 YEARS",
                reason = "Raffle ticket data with seasonal patterns and long-term retention",
                priority = PartitionPriority.MEDIUM,
                estimatedBenefit = "Better performance for raffle analytics and historical reporting",
                implementationSteps = listOf(
                    "1. Create quarterly partitioned structure",
                    "2. Set up constraint exclusion",
                    "3. Migrate historical data",
                    "4. Create partition-wise indexes",
                    "5. Implement automated cleanup"
                )
            )
        )
    }

    private fun isOldPartition(partition: ExistingPartition): Boolean {
        // Lógica simplificada para determinar si una partición es antigua
        // En una implementación real, esto analizaría las fechas en partition_expression
        return partition.partitionExpression?.contains("2022") == true ||
               partition.partitionExpression?.contains("2021") == true
    }

    private fun generatePartitionSQL(recommendation: PartitionRecommendation): List<String> {
        val statements = mutableListOf<String>()

        when (recommendation.strategy) {
            PartitionStrategy.TIME_BASED -> {
                statements.addAll(generateTimeBasedPartitionSQL(recommendation))
            }
            PartitionStrategy.HASH_BASED -> {
                statements.addAll(generateHashBasedPartitionSQL(recommendation))
            }
            PartitionStrategy.RANGE_BASED -> {
                statements.addAll(generateRangeBasedPartitionSQL(recommendation))
            }
        }

        return statements
    }

    private fun generateTimeBasedPartitionSQL(recommendation: PartitionRecommendation): List<String> {
        val statements = mutableListOf<String>()

        // Crear tabla padre particionada
        statements.add("""
            CREATE TABLE ${recommendation.tableName}_partitioned (
                LIKE ${recommendation.tableName} INCLUDING ALL
            ) PARTITION BY RANGE (${recommendation.partitionColumn})
        """.trimIndent())

        // Crear particiones para los próximos meses
        val currentDate = LocalDate.now()
        for (i in 0..11) { // 12 months
            val partitionDate = currentDate.plusMonths(i.toLong())
            val partitionName = "${recommendation.tableName}_${partitionDate.format(DateTimeFormatter.ofPattern("yyyy_MM"))}"
            val startDate = partitionDate.withDayOfMonth(1)
            val endDate = startDate.plusMonths(1)

            statements.add("""
                CREATE TABLE $partitionName PARTITION OF ${recommendation.tableName}_partitioned
                FOR VALUES FROM ('$startDate') TO ('$endDate')
            """.trimIndent())
        }

        return statements
    }

    private fun generateHashBasedPartitionSQL(recommendation: PartitionRecommendation): List<String> {
        val statements = mutableListOf<String>()
        val partitionCount = 4 // Default hash partition count

        // Crear tabla padre particionada
        statements.add("""
            CREATE TABLE ${recommendation.tableName}_partitioned (
                LIKE ${recommendation.tableName} INCLUDING ALL
            ) PARTITION BY HASH (${recommendation.partitionColumn})
        """.trimIndent())

        // Crear particiones hash
        for (i in 0 until partitionCount) {
            val partitionName = "${recommendation.tableName}_hash_$i"
            statements.add("""
                CREATE TABLE $partitionName PARTITION OF ${recommendation.tableName}_partitioned
                FOR VALUES WITH (modulus $partitionCount, remainder $i)
            """.trimIndent())
        }

        return statements
    }

    private fun generateRangeBasedPartitionSQL(recommendation: PartitionRecommendation): List<String> {
        val statements = mutableListOf<String>()

        // Crear tabla padre particionada
        statements.add("""
            CREATE TABLE ${recommendation.tableName}_partitioned (
                LIKE ${recommendation.tableName} INCLUDING ALL
            ) PARTITION BY RANGE (${recommendation.partitionColumn})
        """.trimIndent())

        // Crear particiones por rangos (ejemplo simplificado)
        val ranges = listOf(
            "0" to "1000000",
            "1000000" to "2000000",
            "2000000" to "3000000",
            "3000000" to "MAXVALUE"
        )

        for ((index, range) in ranges.withIndex()) {
            val partitionName = "${recommendation.tableName}_range_$index"
            statements.add("""
                CREATE TABLE $partitionName PARTITION OF ${recommendation.tableName}_partitioned
                FOR VALUES FROM (${range.first}) TO (${range.second})
            """.trimIndent())
        }

        return statements
    }
}

// Data classes para particionado

data class PartitioningReport(
    val partitionCandidates: List<PartitionCandidate>,
    val existingPartitions: List<ExistingPartition>,
    val partitionRecommendations: List<PartitionRecommendation>,
    val maintenanceNeeded: List<PartitionMaintenance>,
    val timestamp: Instant
)

data class TableSizeInfo(
    val schemaName: String,
    val tableName: String,
    val rowCount: Long,
    val sizeBytes: Long,
    val sizePretty: String
)

data class ExistingPartition(
    val schemaName: String,
    val tableName: String,
    val partitionExpression: String,
    val size: String,
    val sizeBytes: Long,
    val rowCount: Long
)

data class PartitionRecommendation(
    val tableName: String,
    val strategy: PartitionStrategy,
    val partitionColumn: String,
    val partitionInterval: String,
    val retentionPeriod: String,
    val reason: String,
    val priority: PartitionPriority,
    val estimatedBenefit: String,
    val implementationSteps: List<String>
)

data class PartitionMaintenance(
    val schemaName: String,
    val tableName: String,
    val maintenanceType: String,
    val reason: String,
    val priority: MaintenancePriority,
    val estimatedImpact: String
)

data class PartitionCreationResult(
    val success: Boolean,
    val tableName: String,
    val partitionStrategy: PartitionStrategy,
    val partitionsCreated: Int,
    val durationMs: Long,
    val message: String,
    val error: String? = null
)

enum class PartitionPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class MaintenancePriority {
    LOW, MEDIUM, HIGH, URGENT
}