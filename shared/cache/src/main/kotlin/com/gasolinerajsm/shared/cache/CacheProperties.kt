package com.gasolinerajsm.shared.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Propiedades de configuración para el sistema de caching
 */
@ConfigurationProperties(prefix = "gasolinera.cache")
data class CacheProperties(
    val enabled: Boolean = true,
    val defaultTtl: Duration = Duration.ofMinutes(30),
    val keyPrefix: String = "gasolinera",
    val caches: Map<String, CacheConfig> = getDefaultCaches(),
    val clustering: ClusteringConfig = ClusteringConfig(),
    val warmup: WarmupConfig = WarmupConfig(),
    val invalidation: InvalidationConfig = InvalidationConfig(),
    val monitoring: MonitoringConfig = MonitoringConfig()
) {

    data class CacheConfig(
        val ttl: Duration = Duration.ofMinutes(30),
        val keyPrefix: String = "",
        val maxSize: Long = 10000,
        val warmupEnabled: Boolean = false,
        val warmupStrategy: WarmupStrategy = WarmupStrategy.LAZY,
        val invalidationStrategy: InvalidationStrategy = InvalidationStrategy.TTL_BASED,
        val compressionEnabled: Boolean = false,
        val serializationFormat: SerializationFormat = SerializationFormat.JSON
    )

    data class ClusteringConfig(
        val enabled: Boolean = false,
        val nodes: List<String> = emptyList(),
        val masterName: String = "mymaster",
        val password: String = "",
        val database: Int = 0,
        val connectionPoolSize: Int = 10,
        val connectionMinimumIdleSize: Int = 5,
        val timeout: Duration = Duration.ofSeconds(3),
        val retryAttempts: Int = 3,
        val retryInterval: Duration = Duration.ofMillis(1500)
    )

    data class WarmupConfig(
        val enabled: Boolean = true,
        val onStartup: Boolean = true,
        val scheduled: Boolean = true,
        val scheduleCron: String = "0 0 6 * * ?", // 6 AM daily
        val batchSize: Int = 100,
        val parallelism: Int = 4,
        val timeout: Duration = Duration.ofMinutes(10)
    )

    data class InvalidationConfig(
        val enabled: Boolean = true,
        val patterns: Map<String, List<String>> = getDefaultInvalidationPatterns(),
        val cascadeInvalidation: Boolean = true,
        val asyncInvalidation: Boolean = true,
        val batchSize: Int = 50
    )

    data class MonitoringConfig(
        val enabled: Boolean = true,
        val metricsEnabled: Boolean = true,
        val healthCheckEnabled: Boolean = true,
        val slowOperationThreshold: Duration = Duration.ofMillis(100),
        val alertOnHighMissRate: Boolean = true,
        val missRateThreshold: Double = 0.8
    )

    enum class WarmupStrategy {
        LAZY,
        EAGER,
        SCHEDULED,
        ON_DEMAND
    }

    enum class InvalidationStrategy {
        TTL_BASED,
        EVENT_DRIVEN,
        MANUAL,
        HYBRID
    }

    enum class SerializationFormat {
        JSON,
        BINARY,
        COMPRESSED_JSON
    }
}

/**
 * Función para obtener configuraciones de cache por defecto
 */
private fun getDefaultCaches(): Map<String, CacheProperties.CacheConfig> {
    return mapOf(
        // Cache de usuarios - TTL corto por seguridad
        "users" to CacheProperties.CacheConfig(
            ttl = Duration.ofMinutes(15),
            keyPrefix = "user",
            maxSize = 5000,
            warmupEnabled = true,
            warmupStrategy = CacheProperties.WarmupStrategy.LAZY,
            invalidationStrategy = CacheProperties.InvalidationStrategy.EVENT_DRIVEN
        ),

        // Cache de cupones - TTL medio
        "coupons" to CacheProperties.CacheConfig(
            ttl = Duration.ofMinutes(30),
            keyPrefix = "coupon",
            maxSize = 20000,
            warmupEnabled = true,
            warmupStrategy = CacheProperties.WarmupStrategy.SCHEDULED,
            invalidationStrategy = CacheProperties.InvalidationStrategy.HYBRID
        ),

        // Cache de estaciones - TTL largo (datos estáticos)
        "stations" to CacheProperties.CacheConfig(
            ttl = Duration.ofHours(2),
            keyPrefix = "station",
            maxSize = 1000,
            warmupEnabled = true,
            warmupStrategy = CacheProperties.WarmupStrategy.EAGER,
            invalidationStrategy = CacheProperties.InvalidationStrategy.TTL_BASED
        ),

        // Cache de campañas - TTL largo
        "campaigns" to CacheProperties.CacheConfig(
            ttl = Duration.ofHours(1),
            keyPrefix = "campaign",
            maxSize = 500,
            warmupEnabled = true,
            warmupStrategy = CacheProperties.WarmupStrategy.SCHEDULED,
            invalidationStrategy = CacheProperties.InvalidationStrategy.EVENT_DRIVEN
        ),

        // Cache de redenciones - TTL corto
        "redemptions" to CacheProperties.CacheConfig(
            ttl = Duration.ofMinutes(10),
            keyPrefix = "redemption",
            maxSize = 10000,
            warmupEnabled = false,
            invalidationStrategy = CacheProperties.InvalidationStrategy.EVENT_DRIVEN
        ),

        // Cache de rifas - TTL medio
        "raffles" to CacheProperties.CacheConfig(
            ttl = Duration.ofMinutes(45),
            keyPrefix = "raffle",
            maxSize = 2000,
            warmupEnabled = true,
            warmupStrategy = CacheProperties.WarmupStrategy.LAZY,
            invalidationStrategy = CacheProperties.InvalidationStrategy.HYBRID
        ),

        // Cache de anuncios - TTL corto (contenido dinámico)
        "advertisements" to CacheProperties.CacheConfig(
            ttl = Duration.ofMinutes(20),
            keyPrefix = "ad",
            maxSize = 5000,
            warmupEnabled = false,
            invalidationStrategy = CacheProperties.InvalidationStrategy.TTL_BASED
        ),

        // Cache de sesiones - TTL muy corto
        "sessions" to CacheProperties.CacheConfig(
            ttl = Duration.ofMinutes(30),
            keyPrefix = "session",
            maxSize = 50000,
            warmupEnabled = false,
            invalidationStrategy = CacheProperties.InvalidationStrategy.TTL_BASED
        ),

        // Cache de configuración - TTL muy largo
        "config" to CacheProperties.CacheConfig(
            ttl = Duration.ofHours(6),
            keyPrefix = "config",
            maxSize = 100,
            warmupEnabled = true,
            warmupStrategy = CacheProperties.WarmupStrategy.EAGER,
            invalidationStrategy = CacheProperties.InvalidationStrategy.MANUAL
        ),

        // Cache de métricas - TTL corto
        "metrics" to CacheProperties.CacheConfig(
            ttl = Duration.ofMinutes(5),
            keyPrefix = "metrics",
            maxSize = 1000,
            warmupEnabled = false,
            invalidationStrategy = CacheProperties.InvalidationStrategy.TTL_BASED
        )
    )
}

/**
 * Función para obtener patrones de invalidación por defecto
 */
private fun getDefaultInvalidationPatterns(): Map<String, List<String>> {
    return mapOf(
        // Cuando se actualiza un usuario, invalidar caches relacionados
        "user.updated" to listOf("users:*", "sessions:*"),
        "user.deleted" to listOf("users:*", "sessions:*", "coupons:user:*"),

        // Cuando se actualiza un cupón, invalidar caches relacionados
        "coupon.created" to listOf("coupons:*", "campaigns:*"),
        "coupon.updated" to listOf("coupons:*", "redemptions:*"),
        "coupon.redeemed" to listOf("coupons:*", "redemptions:*", "users:*"),

        // Cuando se actualiza una estación
        "station.updated" to listOf("stations:*", "config:*"),
        "station.created" to listOf("stations:*"),

        // Cuando se actualiza una campaña
        "campaign.updated" to listOf("campaigns:*", "coupons:*"),
        "campaign.ended" to listOf("campaigns:*", "coupons:*"),

        // Cuando se actualiza una rifa
        "raffle.updated" to listOf("raffles:*", "redemptions:*"),
        "raffle.ended" to listOf("raffles:*"),

        // Cuando se actualiza un anuncio
        "advertisement.updated" to listOf("advertisements:*"),
        "advertisement.expired" to listOf("advertisements:*")
    )
}