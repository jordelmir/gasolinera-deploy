package com.gasolinerajsm.common.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Configuration
@ConfigurationProperties(prefix = "metrics.prometheus")
class PrometheusMetricsProperties {
    var enabled: Boolean = true
    var endpoint: String = "/actuator/prometheus"
    var includeHostTag: Boolean = true
    var includeApplicationTag: Boolean = true
    var commonTags: Map<String, String> = emptyMap()
    var step: String = "PT1M" // 1 minuto
    var descriptions: Boolean = true
}

@Configuration
class MetricsConfiguration(
    private val metricsProperties: PrometheusMetricsProperties
) {

    @Bean
    fun prometheusMeterRegistry(): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            // Tags comunes para todas las métricas
            val commonTags = mutableMapOf<String, String>()

            if (metricsProperties.includeApplicationTag) {
                commonTags["application"] = "gasolinera-jsm"
            }

            if (metricsProperties.includeHostTag) {
                commonTags["host"] = getHostname()
            }

            // Agregar environment
            commonTags["environment"] = System.getenv("ENVIRONMENT") ?: "development"

            // Agregar tags personalizados
            commonTags.putAll(metricsProperties.commonTags)

            // Aplicar tags
            commonTags.forEach { (key, value) ->
                registry.config().commonTags(key, value)
            }
        }
    }

    @Bean
    fun customMeterBinder(businessMetricsCollector: BusinessMetricsCollector): CustomMeterBinder {
        return CustomMeterBinder(businessMetricsCollector)
    }

    private fun getHostname(): String {
        return try {
            java.net.InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown"
        }
    }
}

@RestController
@RequestMapping("/metrics")
class MetricsController(
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
    private val metricsService: MetricsService,
    private val businessMetricsCollector: BusinessMetricsCollector
) {

    @GetMapping("/prometheus")
    fun prometheus(): String {
        return prometheusMeterRegistry.scrape()
    }

    @GetMapping("/snapshot")
    fun getMetricsSnapshot(): Map<String, Any> {
        return metricsService.getMetricsSnapshot()
    }

    @GetMapping("/health-summary")
    fun getHealthSummary(): Map<String, Any> {
        val snapshot = metricsService.getMetricsSnapshot()

        return mapOf(
            "timestamp" to java.time.Instant.now().toString(),
            "summary" to mapOf(
                "total_requests" to extractCounterValue(snapshot, "gasolinera.api.requests.total"),
                "error_rate" to calculateErrorRate(snapshot),
                "average_response_time" to extractTimerMean(snapshot, "gasolinera.api.request_duration"),
                "active_connections" to extractGaugeValue(snapshot, "gasolinera.database.active_connections"),
                "memory_usage_percent" to calculateMemoryUsagePercent(snapshot),
                "cpu_usage_percent" to extractGaugeValue(snapshot, "gasolinera.system.cpu.usage")
            ),
            "business_metrics" to mapOf(
                "fuel_transactions" to extractCounterValue(snapshot, "gasolinera.fuel.transactions.total"),
                "coupon_redemptions" to extractCounterValue(snapshot, "gasolinera.coupon.redemption.total"),
                "raffle_participations" to extractCounterValue(snapshot, "gasolinera.raffle.participation.total"),
                "rate_limit_hits" to extractCounterValue(snapshot, "gasolinera.api.rate_limit_hits.total")
            )
        )
    }

    @GetMapping("/business")
    fun getBusinessMetrics(): Map<String, Any> {
        val snapshot = metricsService.getMetricsSnapshot()

        return mapOf(
            "fuel" to mapOf(
                "total_transactions" to extractCounterValue(snapshot, "gasolinera.fuel.transactions.total"),
                "average_transaction_amount" to extractHistogramMean(snapshot, "gasolinera.fuel.transaction_amount"),
                "qr_generations" to extractCounterValue(snapshot, "gasolinera.qr.generation.total")
            ),
            "coupons" to mapOf(
                "total_redemptions" to extractCounterValue(snapshot, "gasolinera.coupon.redemption.total"),
                "average_discount" to extractHistogramMean(snapshot, "gasolinera.coupon.discount_amount"),
                "validation_attempts" to extractCounterValue(snapshot, "gasolinera.coupon.validation.total")
            ),
            "raffles" to mapOf(
                "total_participations" to extractCounterValue(snapshot, "gasolinera.raffle.participation.total"),
                "total_draws" to extractCounterValue(snapshot, "gasolinera.raffle.draw.total")
            ),
            "auth" to mapOf(
                "total_logins" to extractCounterValue(snapshot, "gasolinera.auth.login.total"),
                "token_validations" to extractCounterValue(snapshot, "gasolinera.auth.token_validation.total")
            )
        )
    }

    private fun extractCounterValue(snapshot: Map<String, Any>, metricName: String): Double {
        val metric = snapshot[metricName] as? Map<*, *>
        return (metric?.get("value") as? Number)?.toDouble() ?: 0.0
    }

    private fun extractTimerMean(snapshot: Map<String, Any>, metricName: String): Double {
        val metric = snapshot[metricName] as? Map<*, *>
        return (metric?.get("mean") as? Number)?.toDouble() ?: 0.0
    }

    private fun extractGaugeValue(snapshot: Map<String, Any>, metricName: String): Double {
        val metric = snapshot[metricName] as? Map<*, *>
        return (metric?.get("value") as? Number)?.toDouble() ?: 0.0
    }

    private fun extractHistogramMean(snapshot: Map<String, Any>, metricName: String): Double {
        val metric = snapshot[metricName] as? Map<*, *>
        return (metric?.get("mean") as? Number)?.toDouble() ?: 0.0
    }

    private fun calculateErrorRate(snapshot: Map<String, Any>): Double {
        val totalRequests = extractCounterValue(snapshot, "gasolinera.api.requests.total")
        if (totalRequests == 0.0) return 0.0

        // Buscar requests con códigos de error (4xx, 5xx)
        var errorRequests = 0.0
        snapshot.forEach { (key, value) ->
            if (key.startsWith("gasolinera.api.requests.total") && value is Map<*, *>) {
                val tags = value["tags"] as? Map<*, *>
                val statusClass = tags?.get("status_class") as? String
                if (statusClass == "4xx" || statusClass == "5xx") {
                    errorRequests += (value["value"] as? Number)?.toDouble() ?: 0.0
                }
            }
        }

        return (errorRequests / totalRequests) * 100
    }

    private fun calculateMemoryUsagePercent(snapshot: Map<String, Any>): Double {
        val used = extractGaugeValue(snapshot, "gasolinera.jvm.memory.heap.used")
        val max = extractGaugeValue(snapshot, "gasolinera.jvm.memory.heap.max")

        return if (max > 0) (used / max) * 100 else 0.0
    }
}