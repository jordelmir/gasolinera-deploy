package com.gasolinerajsm.shared.monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Metrics Configuration for Prometheus Integration
 * Configures Micrometer with Prometheus registry and custom metrics
 */
@Configuration
@EnableConfigurationProperties(MetricsProperties::class)
class MetricsConfiguration(
    private val environment: Environment,
    private val metricsProperties: MetricsProperties
) {

    @Bean
    fun prometheusMeterRegistry(): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config()
                .commonTags(
                    "application", getApplicationName(),
                    "environment", getEnvironment(),
                    "version", getApplicationVersion(),
                    "instance", getInstanceId()
                )
                .meterFilter(MeterFilter.deny { id ->
                    // Filter out noisy metrics
                    val name = id.name
                    metricsProperties.excludePatterns.any { pattern ->
                        name.matches(Regex(pattern))
                    }
                })
                .meterFilter(MeterFilter.maximumExpectedValue("http.server.requests",
                    java.time.Duration.ofSeconds(30)))
        }
    }

    private fun getApplicationName(): String {
        return environment.getProperty("spring.application.name") ?: "gasolinera-service"
    }

    private fun getEnvironment(): String {
        return environment.getProperty("spring.profiles.active") ?: "unknown"
    }

    private fun getApplicationVersion(): String {
        return environment.getProperty("app.version") ?: "unknown"
    }

    private fun getInstanceId(): String {
        return environment.getProperty("app.instance.id") ?:
               java.net.InetAddress.getLocalHost().hostName
    }
}

/**
 * Metrics Properties Configuration
 */
@ConfigurationProperties(prefix = "metrics")
data class MetricsProperties(
    val enabled: Boolean = true,
    val prometheus: PrometheusProperties = PrometheusProperties(),
    val business: BusinessMetricsProperties = BusinessMetricsProperties(),
    val excludePatterns: List<String> = listOf(
        "jvm\\.gc\\..*",
        "process\\..*",
        "system\\..*"
    )
) {

    data class PrometheusProperties(
        val enabled: Boolean = true,
        val endpoint: String = "/actuator/prometheus",
        val pushgateway: PushGatewayProperties = PushGatewayProperties()
    )

    data class PushGatewayProperties(
        val enabled: Boolean = false,
        val baseUrl: String = "http://localhost:9091",
        val job: String = "gasolinera-services",
        val pushRate: String = "30s"
    )

    data class BusinessMetricsProperties(
        val enabled: Boolean = true,
        val coupons: CouponMetricsProperties = CouponMetricsProperties(),
        val raffles: RaffleMetricsProperties = RaffleMetricsProperties(),
        val stations: StationMetricsProperties = StationMetricsProperties(),
        val users: UserMetricsProperties = UserMetricsProperties()
    )

    data class CouponMetricsProperties(
        val enabled: Boolean = true,
        val trackUsage: Boolean = true,
        val trackGeneration: Boolean = true,
        val trackValidation: Boolean = true
    )

    data class RaffleMetricsProperties(
        val enabled: Boolean = true,
        val trackParticipation: Boolean = true,
        val trackDraws: Boolean = true,
        val trackWinners: Boolean = true
    )

    data class StationMetricsProperties(
        val enabled: Boolean = true,
        val trackFuelPrices: Boolean = true,
        val trackInventory: Boolean = true,
        val trackTransactions: Boolean = true
    )

    data class UserMetricsProperties(
        val enabled: Boolean = true,
        val trackRegistrations: Boolean = true,
        val trackLogins: Boolean = true,
        val trackActivity: Boolean = true
    )
}