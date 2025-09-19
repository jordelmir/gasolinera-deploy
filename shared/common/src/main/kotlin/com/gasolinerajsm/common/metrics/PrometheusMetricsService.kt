package com.gasolinerajsm.common.metrics

import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

interface MetricsService {
    fun incrementCounter(name: String, tags: Map<String, String> = emptyMap())
    fun recordTimer(name: String, duration: Duration, tags: Map<String, String> = emptyMap())
    fun recordGauge(name: String, value: Double, tags: Map<String, String> = emptyMap())
    fun recordHistogram(name: String, value: Double, tags: Map<String, String> = emptyMap())
    fun getMetricsSnapshot(): Map<String, Any>
}

@Service
class PrometheusMetricsService(
    private val meterRegistry: MeterRegistry
) : MetricsService {

    private val gaugeValues = ConcurrentHashMap<String, AtomicLong>()

    override fun incrementCounter(name: String, tags: Map<String, String>) {
        Counter.builder(name)
            .tags(tags.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)
            .increment()
    }

    override fun recordTimer(name: String, duration: Duration, tags: Map<String, String>) {
        Timer.builder(name)
            .tags(tags.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)
            .record(duration)
    }

    override fun recordGauge(name: String, value: Double, tags: Map<String, String>) {
        val key = "$name:${tags.hashCode()}"
        val atomicValue = gaugeValues.computeIfAbsent(key) { AtomicLong(0) }
        atomicValue.set(value.toLong())

        Gauge.builder(name, atomicValue) { it.get().toDouble() }
            .tags(tags.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)
    }

    override fun recordHistogram(name: String, value: Double, tags: Map<String, String>) {
        DistributionSummary.builder(name)
            .tags(tags.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)
            .record(value)
    }

    override fun getMetricsSnapshot(): Map<String, Any> {
        val metrics = mutableMapOf<String, Any>()

        meterRegistry.meters.forEach { meter ->
            when (meter) {
                is Counter -> {
                    metrics[meter.id.name] = mapOf(
                        "type" to "counter",
                        "value" to meter.count(),
                        "tags" to meter.id.tags.associate { it.key to it.value }
                    )
                }
                is Timer -> {
                    metrics[meter.id.name] = mapOf(
                        "type" to "timer",
                        "count" to meter.count(),
                        "totalTime" to meter.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS),
                        "mean" to meter.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
                        "max" to meter.max(java.util.concurrent.TimeUnit.MILLISECONDS),
                        "tags" to meter.id.tags.associate { it.key to it.value }
                    )
                }
                is Gauge -> {
                    metrics[meter.id.name] = mapOf(
                        "type" to "gauge",
                        "value" to meter.value(),
                        "tags" to meter.id.tags.associate { it.key to it.value }
                    )
                }
                is DistributionSummary -> {
                    metrics[meter.id.name] = mapOf(
                        "type" to "histogram",
                        "count" to meter.count(),
                        "totalAmount" to meter.totalAmount(),
                        "mean" to meter.mean(),
                        "max" to meter.max(),
                        "tags" to meter.id.tags.associate { it.key to it.value }
                    )
                }
            }
        }

        return metrics
    }
}

@Service
class BusinessMetricsCollector(
    private val metricsService: MetricsService
) {

    // Métricas de autenticación
    fun recordLogin(successful: Boolean, authType: String) {
        val tags = mapOf(
            "success" to successful.toString(),
            "auth_type" to authType
        )
        metricsService.incrementCounter("gasolinera.auth.login.total", tags)
    }

    fun recordTokenValidation(valid: Boolean, tokenType: String) {
        val tags = mapOf(
            "valid" to valid.toString(),
            "token_type" to tokenType
        )
        metricsService.incrementCounter("gasolinera.auth.token_validation.total", tags)
    }

    // Métricas de estaciones
    fun recordFuelTransaction(stationId: String, fuelType: String, amount: Double) {
        val tags = mapOf(
            "station_id" to stationId,
            "fuel_type" to fuelType
        )
        metricsService.incrementCounter("gasolinera.fuel.transactions.total", tags)
        metricsService.recordHistogram("gasolinera.fuel.transaction_amount", amount, tags)
    }

    fun recordQrGeneration(stationId: String, successful: Boolean) {
        val tags = mapOf(
            "station_id" to stationId,
            "success" to successful.toString()
        )
        metricsService.incrementCounter("gasolinera.qr.generation.total", tags)
    }

    // Métricas de cupones
    fun recordCouponRedemption(campaignId: String, successful: Boolean, discountAmount: Double) {
        val tags = mapOf(
            "campaign_id" to campaignId,
            "success" to successful.toString()
        )
        metricsService.incrementCounter("gasolinera.coupon.redemption.total", tags)

        if (successful) {
            metricsService.recordHistogram("gasolinera.coupon.discount_amount", discountAmount, tags)
        }
    }

    fun recordCouponValidation(valid: Boolean, reason: String) {
        val tags = mapOf(
            "valid" to valid.toString(),
            "reason" to reason
        )
        metricsService.incrementCounter("gasolinera.coupon.validation.total", tags)
    }

    // Métricas de sorteos
    fun recordRaffleParticipation(raffleId: String, successful: Boolean) {
        val tags = mapOf(
            "raffle_id" to raffleId,
            "success" to successful.toString()
        )
        metricsService.incrementCounter("gasolinera.raffle.participation.total", tags)
    }

    fun recordRaffleDraw(raffleId: String, participantCount: Int, winnersCount: Int) {
        val tags = mapOf("raffle_id" to raffleId)
        metricsService.incrementCounter("gasolinera.raffle.draw.total", tags)
        metricsService.recordGauge("gasolinera.raffle.participants", participantCount.toDouble(), tags)
        metricsService.recordGauge("gasolinera.raffle.winners", winnersCount.toDouble(), tags)
    }

    // Métricas de API Gateway
    fun recordApiRequest(path: String, method: String, statusCode: Int, duration: Duration) {
        val tags = mapOf(
            "path" to sanitizePath(path),
            "method" to method,
            "status_code" to statusCode.toString(),
            "status_class" to "${statusCode / 100}xx"
        )

        metricsService.incrementCounter("gasolinera.api.requests.total", tags)
        metricsService.recordTimer("gasolinera.api.request_duration", duration, tags)
    }

    fun recordRateLimitHit(key: String, limit: Int) {
        val tags = mapOf(
            "key_type" to extractKeyType(key),
            "limit" to limit.toString()
        )
        metricsService.incrementCounter("gasolinera.api.rate_limit_hits.total", tags)
    }

    // Métricas de sistema
    fun recordDatabaseConnection(poolName: String, activeConnections: Int, maxConnections: Int) {
        val tags = mapOf("pool" to poolName)
        metricsService.recordGauge("gasolinera.database.active_connections", activeConnections.toDouble(), tags)
        metricsService.recordGauge("gasolinera.database.max_connections", maxConnections.toDouble(), tags)
    }

    fun recordCacheOperation(operation: String, hit: Boolean, duration: Duration) {
        val tags = mapOf(
            "operation" to operation,
            "hit" to hit.toString()
        )
        metricsService.incrementCounter("gasolinera.cache.operations.total", tags)
        metricsService.recordTimer("gasolinera.cache.operation_duration", duration, tags)
    }

    private fun sanitizePath(path: String): String {
        // Reemplazar IDs numéricos con placeholder para reducir cardinalidad
        return path.replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[a-f0-9-]{36}"), "/{uuid}")
            .replace(Regex("/[a-f0-9]{24}"), "/{objectId}")
    }

    private fun extractKeyType(key: String): String {
        return when {
            key.contains("user:") -> "user"
            key.contains("ip:") -> "ip"
            key.contains("api_key:") -> "api_key"
            else -> "unknown"
        }
    }
}

@org.springframework.stereotype.Component
class CustomMeterBinder(
    private val businessMetricsCollector: BusinessMetricsCollector
) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        // Registrar métricas personalizadas de JVM
        Gauge.builder("gasolinera.jvm.memory.heap.used", registry) {
                val memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean()
                memoryBean.heapMemoryUsage.used.toDouble()
            }.register(registry)

        Gauge.builder("gasolinera.jvm.memory.heap.max", registry) {
                val memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean()
                memoryBean.heapMemoryUsage.max.toDouble()
            }.register(registry)

        Gauge.builder("gasolinera.jvm.threads.active", registry) {
                val threadBean = java.lang.management.ManagementFactory.getThreadMXBean()
                threadBean.threadCount.toDouble()
            }.register(registry)

        // Métricas de sistema operativo
        Gauge.builder("gasolinera.system.cpu.usage", registry) {
                val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()
                if (osBean is com.sun.management.OperatingSystemMXBean) {
                    osBean.processCpuLoad * 100
                } else {
                    -1.0
                }
            }.register(registry)

        Gauge.builder("gasolinera.system.load.average", registry) {
                val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()
                osBean.systemLoadAverage
            }.register(registry)
    }
}