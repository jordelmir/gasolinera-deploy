package com.gasolinerajsm.monitoring

import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance metrics collection for Gasolinera JSM services
 * Provides comprehensive monitoring of application performance, business metrics, and system health
 */
@Component
class PerformanceMetrics(
    private val meterRegistry: MeterRegistry
) {

    // Business Metrics Counters
    private val couponCreatedCounter = Counter.builder("coupon.created.total")
        .description("Total number of coupons created")
        .register(meterRegistry)

    private val couponRedeemedCounter = Counter.builder("coupon.redeemed.total")
        .description("Total number of coupons redeemed")
        .register(meterRegistry)

    private val couponRedemptionFailedCounter = Counter.builder("coupon.redemption.failed.total")
        .description("Total number of failed coupon redemptions")
        .register(meterRegistry)

    private val raffleTicketsGeneratedCounter = Counter.builder("raffle.tickets.generated.total")
        .description("Total number of raffle tickets generated")
        .register(meterRegistry)

    private val userRegisteredCounter = Counter.builder("user.registered.total")
        .description("Total number of users registered")
        .register(meterRegistry)

    private val userLoginCounter = Counter.builder("user.login.total")
        .description("Total number of user logins")
        .register(meterRegistry)

    // Revenue Metrics
    private val couponPurchaseAmountCounter = Counter.builder("coupon.purchase.amount.total")
        .description("Total amount of coupon purchases")
        .baseUnit("MXN")
        .register(meterRegistry)

    // Performance Timers
    private val couponCreationTimer = Timer.builder("coupon.creation.duration")
        .description("Time taken to create a coupon")
        .register(meterRegistry)

    private val couponRedemptionTimer = Timer.builder("coupon.redemption.duration")
        .description("Time taken to redeem a coupon")
        .register(meterRegistry)

    private val databaseQueryTimer = Timer.builder("database.query.duration")
        .description("Time taken for database queries")
        .register(meterRegistry)

    private val externalApiTimer = Timer.builder("external.api.duration")
        .description("Time taken for external API calls")
        .register(meterRegistry)

    // Cache Metrics
    private val cacheHitCounter = Counter.builder("cache.hit.total")
        .description("Total number of cache hits")
        .register(meterRegistry)

    private val cacheMissCounter = Counter.builder("cache.miss.total")
        .description("Total number of cache misses")
        .register(meterRegistry)

    // Queue Metrics
    private val messagePublishedCounter = Counter.builder("message.published.total")
        .description("Total number of messages published")
        .register(meterRegistry)

    private val messageConsumedCounter = Counter.builder("message.consumed.total")
        .description("Total number of messages consumed")
        .register(meterRegistry)

    private val messageProcessingTimer = Timer.builder("message.processing.duration")
        .description("Time taken to process messages")
        .register(meterRegistry)

    // Gauges for current state
    private val activeUsersGauge = AtomicLong(0)
    private val activeCouponsGauge = AtomicLong(0)
    private val queueSizeGauge = AtomicLong(0)

    init {
        // Register gauges
        Gauge.builder("users.active.current")
            .description("Current number of active users")
            .register(meterRegistry) { activeUsersGauge.get().toDouble() }

        Gauge.builder("coupons.active.current")
            .description("Current number of active coupons")
            .register(meterRegistry) { activeCouponsGauge.get().toDouble() }

        Gauge.builder("queue.size.current")
            .description("Current queue size")
            .register(meterRegistry) { queueSizeGauge.get().toDouble() }
    }

    // Business Metrics Methods
    fun recordCouponCreated(stationId: String, fuelType: String, amount: Double) {
        couponCreatedCounter.increment(
            Tags.of(
                Tag.of("station_id", stationId),
                Tag.of("fuel_type", fuelType)
            )
        )
        couponPurchaseAmountCounter.increment(amount)
    }

    fun recordCouponRedeemed(stationId: String, fuelType: String, userId: String, ticketsGenerated: Int) {
        couponRedeemedCounter.increment(
            Tags.of(
                Tag.of("station_id", stationId),
                Tag.of("fuel_type", fuelType),
                Tag.of("user_id", userId)
            )
        )

        raffleTicketsGeneratedCounter.increment(
            ticketsGenerated.toDouble(),
            Tags.of(
                Tag.of("source_event", "COUPON_REDEEMED"),
                Tag.of("station_id", stationId)
            )
        )
    }

    fun recordCouponRedemptionFailed(stationId: String, fuelType: String, reason: String) {
        couponRedemptionFailedCounter.increment(
            Tags.of(
                Tag.of("station_id", stationId),
                Tag.of("fuel_type", fuelType),
                Tag.of("failure_reason", reason)
            )
        )
    }

    fun recordUserRegistered(registrationMethod: String) {
        userRegisteredCounter.increment(
            Tags.of(Tag.of("method", registrationMethod))
        )
    }

    fun recordUserLogin(loginMethod: String, success: Boolean) {
        userLoginCounter.increment(
            Tags.of(
                Tag.of("method", loginMethod),
                Tag.of("success", success.toString())
            )
        )
    }

    fun recordRaffleTicketsGenerated(count: Int, sourceEvent: String, multiplier: Double = 1.0) {
        raffleTicketsGeneratedCounter.increment(
            count.toDouble(),
            Tags.of(
                Tag.of("source_event", sourceEvent),
                Tag.of("multiplier", multiplier.toString())
            )
        )
    }

    // Performance Timing Methods
    fun <T> timeCouponCreation(operation: () -> T): T {
        return couponCreationTimer.recordCallable(operation)!!
    }

    fun <T> timeCouponRedemption(operation: () -> T): T {
        return couponRedemptionTimer.recordCallable(operation)!!
    }

    fun <T> timeDatabaseQuery(queryType: String, operation: () -> T): T {
        return Timer.Sample.start(meterRegistry).let { sample ->
            try {
                operation()
            } finally {
                sample.stop(
                    Timer.builder("database.query.duration")
                        .tag("query_type", queryType)
                        .register(meterRegistry)
                )
            }
        }
    }

    fun <T> timeExternalApiCall(apiName: String, operation: () -> T): T {
        return Timer.Sample.start(meterRegistry).let { sample ->
            try {
                operation()
            } finally {
                sample.stop(
                    Timer.builder("external.api.duration")
                        .tag("api_name", apiName)
                        .register(meterRegistry)
                )
            }
        }
    }

    fun recordDatabaseQueryDuration(queryType: String, duration: Duration) {
        Timer.builder("database.query.duration")
            .tag("query_type", queryType)
            .register(meterRegistry)
            .record(duration)
    }

    // Cache Metrics Methods
    fun recordCacheHit(cacheName: String, key: String) {
        cacheHitCounter.increment(
            Tags.of(
                Tag.of("cache_name", cacheName),
                Tag.of("operation", "get")
            )
        )
    }

    fun recordCacheMiss(cacheName: String, key: String) {
        cacheMissCounter.increment(
            Tags.of(
                Tag.of("cache_name", cacheName),
                Tag.of("operation", "get")
            )
        )
    }

    fun getCacheHitRatio(cacheName: String): Double {
        val hits = cacheHitCounter.count()
        val misses = cacheMissCounter.count()
        val total = hits + misses
        return if (total > 0) hits / total else 0.0
    }

    // Message Queue Metrics Methods
    fun recordMessagePublished(queueName: String, messageType: String) {
        messagePublishedCounter.increment(
            Tags.of(
                Tag.of("queue_name", queueName),
                Tag.of("message_type", messageType)
            )
        )
    }

    fun recordMessageConsumed(queueName: String, messageType: String, success: Boolean) {
        messageConsumedCounter.increment(
            Tags.of(
                Tag.of("queue_name", queueName),
                Tag.of("message_type", messageType),
                Tag.of("success", success.toString())
            )
        )
    }

    fun <T> timeMessageProcessing(queueName: String, messageType: String, operation: () -> T): T {
        return Timer.Sample.start(meterRegistry).let { sample ->
            try {
                operation()
            } finally {
                sample.stop(
                    Timer.builder("message.processing.duration")
                        .tag("queue_name", queueName)
                        .tag("message_type", messageType)
                        .register(meterRegistry)
                )
            }
        }
    }

    // Gauge Update Methods
    fun updateActiveUsers(count: Long) {
        activeUsersGauge.set(count)
    }

    fun updateActiveCoupons(count: Long) {
        activeCouponsGauge.set(count)
    }

    fun updateQueueSize(queueName: String, size: Long) {
        queueSizeGauge.set(size)

        // Also create a tagged gauge for this specific queue
        Gauge.builder("queue.size.current")
            .tag("queue_name", queueName)
            .register(meterRegistry) { size.toDouble() }
    }

    // Custom Distribution Summary for tracking distributions
    fun recordCouponAmount(amount: Double, fuelType: String) {
        DistributionSummary.builder("coupon.amount.distribution")
            .description("Distribution of coupon amounts")
            .baseUnit("MXN")
            .tag("fuel_type", fuelType)
            .register(meterRegistry)
            .record(amount)
    }

    fun recordRaffleTicketMultiplier(multiplier: Double, sourceEvent: String) {
        DistributionSummary.builder("raffle.ticket.multiplier.distribution")
            .description("Distribution of raffle ticket multipliers")
            .tag("source_event", sourceEvent)
            .register(meterRegistry)
            .record(multiplier)
    }

    // Health Check Metrics
    fun recordHealthCheck(serviceName: String, healthy: Boolean, responseTime: Duration) {
        Counter.builder("health.check.total")
            .tag("service", serviceName)
            .tag("status", if (healthy) "healthy" else "unhealthy")
            .register(meterRegistry)
            .increment()

        Timer.builder("health.check.duration")
            .tag("service", serviceName)
            .register(meterRegistry)
            .record(responseTime)
    }

    // Error Tracking
    fun recordError(errorType: String, service: String, operation: String) {
        Counter.builder("errors.total")
            .tag("error_type", errorType)
            .tag("service", service)
            .tag("operation", operation)
            .register(meterRegistry)
            .increment()
    }

    // SLA Metrics
    fun recordSLAViolation(slaType: String, service: String, actualValue: Double, threshold: Double) {
        Counter.builder("sla.violations.total")
            .tag("sla_type", slaType)
            .tag("service", service)
            .register(meterRegistry)
            .increment()

        Gauge.builder("sla.actual.value")
            .tag("sla_type", slaType)
            .tag("service", service)
            .register(meterRegistry) { actualValue }

        Gauge.builder("sla.threshold.value")
            .tag("sla_type", slaType)
            .tag("service", service)
            .register(meterRegistry) { threshold }
    }
}