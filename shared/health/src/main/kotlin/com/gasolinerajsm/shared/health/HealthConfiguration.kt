package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * Configuración central de health checks para todos los servicios
 */
@Configuration
@EnableConfigurationProperties(HealthProperties::class)
class HealthConfiguration {

    @Bean
    fun customDatabaseHealthIndicator(
        dataSource: DataSource,
        healthProperties: HealthProperties
    ): HealthIndicator {
        return CustomDatabaseHealthIndicator(
            JdbcTemplate(dataSource),
            healthProperties.database
        )
    }

    @Bean
    fun redisHealthIndicator(
        redisConnectionFactory: RedisConnectionFactory,
        healthProperties: HealthProperties
    ): HealthIndicator {
        return CustomRedisHealthIndicator(
            redisConnectionFactory,
            healthProperties.redis
        )
    }

    @Bean
    fun businessHealthIndicator(
        healthProperties: HealthProperties
    ): HealthIndicator {
        return BusinessHealthIndicator(healthProperties.business)
    }

    @Bean
    fun externalServicesHealthIndicator(
        healthProperties: HealthProperties
    ): HealthIndicator {
        return ExternalServicesHealthIndicator(healthProperties.externalServices)
    }

    @Bean
    fun systemResourcesHealthIndicator(
        healthProperties: HealthProperties
    ): HealthIndicator {
        return SystemResourcesHealthIndicator(healthProperties.systemResources)
    }

    @Bean
    fun aggregatedHealthIndicator(
        customDatabaseHealthIndicator: HealthIndicator,
        redisHealthIndicator: HealthIndicator,
        businessHealthIndicator: HealthIndicator,
        externalServicesHealthIndicator: HealthIndicator,
        systemResourcesHealthIndicator: HealthIndicator
    ): HealthIndicator {
        return AggregatedHealthIndicator(
            mapOf(
                "database" to customDatabaseHealthIndicator,
                "redis" to redisHealthIndicator,
                "business" to businessHealthIndicator,
                "externalServices" to externalServicesHealthIndicator,
                "systemResources" to systemResourcesHealthIndicator
            )
        )
    }

    @Bean
    fun healthCheckService(
        healthProperties: HealthProperties
    ): HealthCheckService {
        return HealthCheckService(healthProperties)
    }
}