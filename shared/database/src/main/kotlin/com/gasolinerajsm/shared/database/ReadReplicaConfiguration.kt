package com.gasolinerajsm.shared.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

/**
 * Configuración para read replicas de PostgreSQL
 */
@Configuration
@EnableConfigurationProperties(DatabaseOptimizationProperties::class)
@ConditionalOnProperty(prefix = "gasolinera.database.read-replica", name = ["enabled"], havingValue = "true")
class ReadReplicaConfiguration(
    private val properties: DatabaseOptimizationProperties
) {

    @Bean
    @Primary
    fun routingDataSource(): DataSource {
        val routingDataSource = ReadWriteRoutingDataSource()

        val dataSources = mutableMapOf<Any, Any>()

        // Primary (write) datasource
        dataSources[DataSourceType.WRITE] = createWriteDataSource()

        // Read replica datasources
        properties.readReplica.replicas.forEachIndexed { index, replica ->
            dataSources[DataSourceType.READ(index)] = createReadDataSource(replica)
        }

        routingDataSource.setTargetDataSources(dataSources)
        routingDataSource.setDefaultTargetDataSource(dataSources[DataSourceType.WRITE]!!)

        return routingDataSource
    }

    @Bean
    fun readOnlyJdbcTemplate(routingDataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(routingDataSource)
    }

    @Bean
    fun readReplicaManager(routingDataSource: DataSource): ReadReplicaManager {
        return ReadReplicaManager(routingDataSource as ReadWriteRoutingDataSource, properties)
    }

    private fun createWriteDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = properties.primary.url
            username = properties.primary.username
            password = properties.primary.password
            driverClassName = "org.postgresql.Driver"

            // Configuración optimizada para escritura
            maximumPoolSize = properties.primary.maxPoolSize
            minimumIdle = properties.primary.minIdle
            connectionTimeout = properties.primary.connectionTimeout.toMillis()
            idleTimeout = properties.primary.idleTimeout.toMillis()
            maxLifetime = properties.primary.maxLifetime.toMillis()
            leakDetectionThreshold = properties.primary.leakDetectionThreshold.toMillis()

            // Configuraciones específicas para PostgreSQL
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("reWriteBatchedInserts", "true")

            poolName = "GasolineraWritePool"
        }

        return HikariDataSource(config)
    }

    private fun createReadDataSource(replica: DatabaseOptimizationProperties.ReadReplicaConfig): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = replica.url
            username = replica.username
            password = replica.password
            driverClassName = "org.postgresql.Driver"

            // Configuración optimizada para lectura
            maximumPoolSize = replica.maxPoolSize
            minimumIdle = replica.minIdle
            connectionTimeout = replica.connectionTimeout.toMillis()
            idleTimeout = replica.idleTimeout.toMillis()
            maxLifetime = replica.maxLifetime.toMillis()

            // Configuraciones específicas para read replicas
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "500") // Más cache para reads
            addDataSourceProperty("prepStmtCacheSqlLimit", "4096")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("defaultRowFetchSize", "1000") // Optimizado para reads
            addDataSourceProperty("readOnly", "true")

            poolName = "GasolineraReadPool-${replica.name}"
        }

        return HikariDataSource(config)
    }
}

/**
 * DataSource que enruta automáticamente entre write y read replicas
 */
class ReadWriteRoutingDataSource : AbstractRoutingDataSource() {

    override fun determineCurrentLookupKey(): Any? {
        return ReadReplicaContext.getDataSourceType()
    }
}

/**
 * Context para determinar qué tipo de datasource usar
 */
object ReadReplicaContext {
    private val contextHolder = ThreadLocal<DataSourceType>()

    fun setDataSourceType(dataSourceType: DataSourceType) {
        contextHolder.set(dataSourceType)
    }

    fun getDataSourceType(): DataSourceType {
        return contextHolder.get() ?: DataSourceType.WRITE
    }

    fun clear() {
        contextHolder.remove()
    }

    fun isReadOnly(): Boolean {
        return getDataSourceType() != DataSourceType.WRITE
    }
}

/**
 * Tipos de datasource
 */
sealed class DataSourceType {
    object WRITE : DataSourceType()
    data class READ(val index: Int) : DataSourceType()

    companion object {
        fun read(index: Int = 0) = READ(index)
    }
}

/**
 * Gestor de read replicas con load balancing
 */
class ReadReplicaManager(
    private val routingDataSource: ReadWriteRoutingDataSource,
    private val properties: DatabaseOptimizationProperties
) {

    private var currentReadReplicaIndex = 0

    /**
     * Selecciona la siguiente read replica usando round-robin
     */
    fun selectNextReadReplica(): DataSourceType {
        if (properties.readReplica.replicas.isEmpty()) {
            return DataSourceType.WRITE
        }

        val replica = DataSourceType.read(currentReadReplicaIndex)
        currentReadReplicaIndex = (currentReadReplicaIndex + 1) % properties.readReplica.replicas.size

        return replica
    }

    /**
     * Ejecuta una operación en read replica
     */
    fun <T> executeOnReadReplica(operation: () -> T): T {
        val originalType = ReadReplicaContext.getDataSourceType()

        return try {
            ReadReplicaContext.setDataSourceType(selectNextReadReplica())
            operation()
        } finally {
            ReadReplicaContext.setDataSourceType(originalType)
        }
    }

    /**
     * Ejecuta una operación en write datasource
     */
    fun <T> executeOnWrite(operation: () -> T): T {
        val originalType = ReadReplicaContext.getDataSourceType()

        return try {
            ReadReplicaContext.setDataSourceType(DataSourceType.WRITE)
            operation()
        } finally {
            ReadReplicaContext.setDataSourceType(originalType)
        }
    }

    /**
     * Verifica la salud de las read replicas
     */
    fun checkReadReplicaHealth(): Map<String, Boolean> {
        return properties.readReplica.replicas.mapIndexed { index, replica ->
            replica.name to checkReplicaHealth(replica)
        }.toMap()
    }

    private fun checkReplicaHealth(replica: DatabaseOptimizationProperties.ReadReplicaConfig): Boolean {
        return try {
            // Implementar health check específico para la replica
            true // Placeholder
        } catch (e: Exception) {
            false
        }
    }
}