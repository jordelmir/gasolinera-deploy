package com.gasolinerajsm.shared.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * Configuración optimizada de base de datos para todos los servicios
 */
@Configuration
@EnableConfigurationProperties(DatabaseOptimizationProperties::class)
class DatabaseOptimizationConfiguration(
    private val properties: DatabaseOptimizationProperties
) {

    @Bean
    @Primary
    fun optimizedDataSource(): DataSource {
        val config = HikariConfig()

        // Configuración básica de conexión
        config.jdbcUrl = properties.url
        config.username = properties.username
        config.password = properties.password
        config.driverClassName = "org.postgresql.Driver"

        // Optimizaciones de pool de conexiones
        config.maximumPoolSize = properties.hikari.maximumPoolSize
        config.minimumIdle = properties.hikari.minimumIdle
        config.connectionTimeout = properties.hikari.connectionTimeout
        config.idleTimeout = properties.hikari.idleTimeout
        config.maxLifetime = properties.hikari.maxLifetime
        config.leakDetectionThreshold = properties.hikari.leakDetectionThreshold

        // Configuraciones específicas de PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.addDataSourceProperty("useServerPrepStmts", "true")
        config.addDataSourceProperty("useLocalSessionState", "true")
        config.addDataSourceProperty("rewriteBatchedStatements", "true")
        config.addDataSourceProperty("cacheResultSetMetadata", "true")
        config.addDataSourceProperty("cacheServerConfiguration", "true")
        config.addDataSourceProperty("elideSetAutoCommits", "true")
        config.addDataSourceProperty("maintainTimeStats", "false")

        // Configuraciones específicas de PostgreSQL para performance
        config.addDataSourceProperty("ApplicationName", properties.applicationName)
        config.addDataSourceProperty("tcpKeepAlive", "true")
        config.addDataSourceProperty("socketTimeout", "30")
        config.addDataSourceProperty("loginTimeout", "10")
        config.addDataSourceProperty("connectTimeout", "10")

        // Pool name para identificación
        config.poolName = "${properties.applicationName}-HikariCP"

        return HikariDataSource(config)
    }

    @Bean
    fun databaseAnalyzer(dataSource: DataSource): DatabaseAnalyzer {
        return DatabaseAnalyzer(JdbcTemplate(dataSource), properties)
    }

    @Bean
    fun indexOptimizer(dataSource: DataSource): IndexOptimizer {
        return IndexOptimizer(JdbcTemplate(dataSource), properties)
    }

    @Bean
    fun queryOptimizer(dataSource: DataSource): QueryOptimizer {
        return QueryOptimizer(JdbcTemplate(dataSource), properties)
    }

    @Bean
    fun partitionManager(dataSource: DataSource): PartitionManager {
        return PartitionManager(JdbcTemplate(dataSource), properties)
    }

    @Bean
    fun databaseMaintenanceService(
        databaseAnalyzer: DatabaseAnalyzer,
        indexOptimizer: IndexOptimizer,
        queryOptimizer: QueryOptimizer,
        partitionManager: PartitionManager
    ): DatabaseMaintenanceService {
        return DatabaseMaintenanceService(
            databaseAnalyzer,
            indexOptimizer,
            queryOptimizer,
            partitionManager,
            properties
        )
    }
}