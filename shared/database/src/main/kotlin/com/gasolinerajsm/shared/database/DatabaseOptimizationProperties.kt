package com.gasolinerajsm.shared.database

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Propiedades de configuración para optimización de base de datos
 */
@ConfigurationProperties(prefix = "gasolinera.database")
data class DatabaseOptimizationProperties(
    val url: String = "jdbc:postgresql://localhost:5432/gasolinera",
    val username: String = "gasolinera_user",
    val password: String = "gasolinera_password",
    val applicationName: String = "gasolinera-service",
    val hikari: HikariProperties = HikariProperties(),
    val optimization: OptimizationProperties = OptimizationProperties(),
    val partitioning: PartitioningProperties = PartitioningProperties(),
    val maintenance: MaintenanceProperties = MaintenanceProperties(),
    val readReplica: ReadReplicaProperties = ReadReplicaProperties(),
    val primary: PrimaryDataSourceProperties = PrimaryDataSourceProperties()
) {

    data class HikariProperties(
        val maximumPoolSize: Int = 20,
        val minimumIdle: Int = 5,
        val connectionTimeout: Long = 30000, // 30 seconds
        val idleTimeout: Long = 600000, // 10 minutes
        val maxLifetime: Long = 1800000, // 30 minutes
        val leakDetectionThreshold: Long = 60000 // 1 minute
    )

    data class OptimizationProperties(
        val enableAutoIndexing: Boolean = true,
        val enableQueryAnalysis: Boolean = true,
        val slowQueryThreshold: Duration = Duration.ofSeconds(1),
        val indexUsageThreshold: Double = 0.1, // 10% usage minimum
        val maxIndexesPerTable: Int = 10,
        val enableStatisticsUpdate: Boolean = true,
        val statisticsUpdateInterval: Duration = Duration.ofHours(6),
        val performanceMonitoring: Boolean = true,
        val nPlusOneDetection: Boolean = true,
        val queryOptimization: Boolean = true
    )

    data class PartitioningProperties(
        val enableAutoPartitioning: Boolean = true,
        val partitionThreshold: Long = 1000000, // 1M rows
        val partitionStrategy: PartitionStrategy = PartitionStrategy.TIME_BASED,
        val timePartitionInterval: TimePartitionInterval = TimePartitionInterval.MONTHLY,
        val retentionPeriod: Duration = Duration.ofDays(365), // 1 year
        val enablePartitionPruning: Boolean = true
    )

    data class MaintenanceProperties(
        val enableAutoMaintenance: Boolean = true,
        val maintenanceInterval: Duration = Duration.ofHours(24),
        val enableVacuum: Boolean = true,
        val enableAnalyze: Boolean = true,
        val enableReindex: Boolean = false, // Expensive operation
        val maintenanceWindow: MaintenanceWindow = MaintenanceWindow()
    )

    data class MaintenanceWindow(
        val startHour: Int = 2, // 2 AM
        val endHour: Int = 4, // 4 AM
        val timezone: String = "UTC",
        val enabledDays: List<String> = listOf("SUNDAY") // Only on Sundays
    )
}

enum class PartitionStrategy {
    TIME_BASED,
    HASH_BASED,
    RANGE_BASED
}

enum class TimePartitionInterval {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

    /**
     * Configuración para read replicas
     */
    data class ReadReplicaProperties(
        val enabled: Boolean = false,
        val replicas: List<ReadReplicaConfig> = emptyList(),
        val loadBalancingStrategy: LoadBalancingStrategy = LoadBalancingStrategy.ROUND_ROBIN,
        val healthCheckInterval: Duration = Duration.ofSeconds(30),
        val failoverEnabled: Boolean = true,
        val maxRetries: Int = 3
    )

    data class ReadReplicaConfig(
        val name: String = "replica-1",
        val url: String = "",
        val username: String = "",
        val password: String = "",
        val maxPoolSize: Int = 15,
        val minIdle: Int = 3,
        val connectionTimeout: Duration = Duration.ofSeconds(30),
        val idleTimeout: Duration = Duration.ofMinutes(10),
        val maxLifetime: Duration = Duration.ofMinutes(30),
        val weight: Int = 1, // Para weighted load balancing
        val enabled: Boolean = true
    )

    /**
     * Configuración para datasource primario (write)
     */
    data class PrimaryDataSourceProperties(
        val url: String = "jdbc:postgresql://localhost:5432/gasolinera",
        val username: String = "gasolinera_user",
        val password: String = "gasolinera_password",
        val maxPoolSize: Int = 20,
        val minIdle: Int = 5,
        val connectionTimeout: Duration = Duration.ofSeconds(30),
        val idleTimeout: Duration = Duration.ofMinutes(10),
        val maxLifetime: Duration = Duration.ofMinutes(30),
        val leakDetectionThreshold: Duration = Duration.ofMinutes(1)
    )

enum class LoadBalancingStrategy {
    ROUND_ROBIN,
    RANDOM,
    WEIGHTED,
    LEAST_CONNECTIONS,
    RESPONSE_TIME
}