package com.gasolinerajsm.monitoring

import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * World-Class Business Metrics for Gasolinera JSM
 *
 * Tracks critical business KPIs and operational metrics:
 * - Revenue and transaction metrics
 * - User engagement and behavior
 * - System performance and reliability
 * - Security and compliance metrics
 */
@Component
class BusinessMetrics(private val meterRegistry: MeterRegistry) {

    // ==================== REVENUE METRICS ====================

    private val couponPurchaseCounter = Counter.builder("business.coupon.purchases.total")
        .description("Total number of coupon purchases")
        .register(meterRegistry)

    private val couponPurchaseAmount = DistributionSummary.builder("business.coupon.purchases.amount")
        .description("Distribution of coupon purchase amounts in MXN")
        .baseUnit("MXN")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    private val dailyRevenue = Gauge.builder("business.revenue.daily")
        .description("Daily revenue in MXN")
        .baseUnit("MXN")
        .register(meterRegistry, this) { getDailyRevenue() }

    private val monthlyRevenue = Gauge.builder("business.revenue.monthly")
        .description("Monthly revenue in MXN")
        .baseUnit("MXN")
        .register(meterRegistry, this) { getMonthlyRevenue() }

    // ==================== COUPON METRICS ====================

    private val couponRedemptionCounter = Counter.builder("business.coupon.redemptions.total")
        .description("Total number of coupon redemptions")
        .register(meterRegistry)

    private val couponRedemptionTimer = Timer.builder("business.coupon.redemption.duration")
        .description("Time taken to process coupon redemption")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    private val activeCouponsGauge = Gauge.builder("business.coupons.active")
        .description("Number of active coupons in the system")
        .register(meterRegistry, this) { getActiveCouponsCount() }

    private val couponConversionRate = Gauge.builder("business.coupon.conversion.rate")
        .description("Coupon redemption rate (redeemed/purchased)")
        .register(meterRegistry, this) { getCouponConversionRate() }

    // ==================== USER METRICS ====================

    private val userRegistrationCounter = Counter.builder("business.users.registrations.total")
        .description("Total number of user registrations")
        .register(meterRegistry)

    private val activeUsersGauge = Gauge.builder("business.users.active")
        .description("Number of active users (last 30 days)")
        .register(meterRegistry, this) { getActiveUsersCount() }

    private val userEngagementScore = Gauge.builder("business.users.engagement.score")
        .description("Average user engagement score")
        .register(meterRegistry, this) { getUserEngagementScore() }

    // ==================== RAFFLE METRICS ====================

    private val raffleTicketsGenerated = Counter.builder("business.raffle.tickets.generated.total")
        .description("Total number of raffle tickets generated")
        .register(meterRegistry)

    private val raffleParticipationRate = Gauge.builder("business.raffle.participation.rate")
        .description("Percentage of users participating in raffles")
        .register(meterRegistry, this) { getRaffleParticipationRate() }

    private val activeRafflesGauge = Gauge.builder("business.raffles.active")
        .description("Number of active raffles")
        .register(meterRegistry, this) { getActiveRafflesCount() }

    // ==================== STATION METRICS ====================

    private val stationSearchCounter = Counter.builder("business.stations.searches.total")
        .description("Total number of station searches")
        .register(meterRegistry)

    private val averageSearchRadius = Gauge.builder("business.stations.search.radius.average")
        .description("Average search radius in kilometers")
        .baseUnit("km")
        .register(meterRegistry, this) { getAverageSearchRadius() }

    private val popularStationsGauge = Gauge.builder("business.stations.popular.count")
        .description("Number of stations with high activity")
        .register(meterRegistry, this) { getPopularStationsCount() }

    // ==================== PERFORMANCE METRICS ====================

    private val apiResponseTime = Timer.builder("business.api.response.time")
        .description("API response time for business operations")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    private val errorRate = Gauge.builder("business.error.rate")
        .description("Business operation error rate")
        .register(meterRegistry, this) { getErrorRate() }

    private val throughput = Gauge.builder("business.throughput.rps")
        .description("Business operations per second")
        .baseUnit("ops/sec")
        .register(meterRegistry, this) { getThroughput() }

    // ==================== SECURITY METRICS ====================

    private val failedLoginAttempts = Counter.builder("business.security.login.failures.total")
        .description("Total number of failed login attempts")
        .register(meterRegistry)

    private val suspiciousActivities = Counter.builder("business.security.suspicious.activities.total")
        .description("Total number of suspicious activities detected")
        .register(meterRegistry)

    // ==================== INTERNAL COUNTERS ====================

    private val dailyRevenueCounter = AtomicLong(0)
    private val monthlyRevenueCounter = AtomicLong(0)
    private val activeCouponsCounter = AtomicLong(0)
    private val activeUsersCounter = AtomicLong(0)
    private val activeRafflesCounter = AtomicLong(0)
    private val popularStationsCounter = AtomicLong(0)
    private val totalRequests = AtomicLong(0)
    private val totalErrors = AtomicLong(0)

    // ==================== PUBLIC METHODS ====================

    fun recordCouponPurchase(
        amount: BigDecimal,
        fuelType: String,
        stationId: String,
        userId: String,
        paymentMethod: String
    ) {
        couponPurchaseCounter.increment(
            Tags.of(
                Tag.of("fuel_type", fuelType),
                Tag.of("station_id", stationId),
                Tag.of("payment_method", paymentMethod),
                Tag.of("amount_range", getAmountRange(amount))
            )
        )
        couponPurchaseAmount.record(amount.toDouble())
        dailyRevenueCounter.addAndGet(amount.toLong())
        monthlyRevenueCounter.addAndGet(amount.toLong())
    }

    fun recordCouponRedemption(
        couponId: String,
        stationId: String,
        userId: String,
        fuelAmount: BigDecimal,
        duration: Duration
    ) {
        couponRedemptionCounter.increment(
            Tags.of(
                Tag.of("station_id", stationId),
                Tag.of("fuel_amount_range", getFuelAmountRange(fuelAmount))
            )
        )
        couponRedemptionTimer.record(duration)
    }

    fun recordUserRegistration(source: String, userType: String) {
        userRegistrationCounter.increment(
            Tags.of(
                Tag.of("source", source),
                Tag.of("user_type", userType)
            )
        )
        activeUsersCounter.incrementAndGet()
    }

    fun recordRaffleTicketGeneration(
        userId: String,
        ticketCount: Int,
        source: String,
        multiplier: Double
    ) {
        raffleTicketsGenerated.increment(
            Tags.of(
                Tag.of("source", source),
                Tag.of("multiplier_range", getMultiplierRange(multiplier))
            ),
            ticketCount.toDouble()
        )
    }

    fun recordStationSearch(
        latitude: Double,
        longitude: Double,
        radius: Double,
        resultsCount: Int
    ) {
        stationSearchCounter.increment(
            Tags.of(
                Tag.of("radius_range", getRadiusRange(radius)),
                Tag.of("results_range", getResultsRange(resultsCount))
            )
        )
    }

    fun recordApiCall(operation: String, duration: Duration, success: Boolean) {
        apiResponseTime.record(duration, Tags.of(
            Tag.of("operation", operation),
            Tag.of("success", success.toString())
        ))

        totalRequests.incrementAndGet()
        if (!success) {
            totalErrors.incrementAndGet()
        }
    }

    fun recordFailedLogin(username: String, reason: String, ipAddress: String) {
        failedLoginAttempts.increment(
            Tags.of(
                Tag.of("reason", reason),
                Tag.of("ip_range", getIpRange(ipAddress))
            )
        )
    }

    fun recordSuspiciousActivity(activityType: String, severity: String, userId: String?) {
        suspiciousActivities.increment(
            Tags.of(
                Tag.of("activity_type", activityType),
                Tag.of("severity", severity),
                Tag.of("has_user", (userId != null).toString())
            )
        )
    }

    // ==================== GAUGE METHODS ====================

    private fun getDailyRevenue(): Double = dailyRevenueCounter.get().toDouble()
    private fun getMonthlyRevenue(): Double = monthlyRevenueCounter.get().toDouble()
    private fun getActiveCouponsCount(): Double = activeCouponsCounter.get().toDouble()
    private fun getActiveUsersCount(): Double = activeUsersCounter.get().toDouble()
    private fun getActiveRafflesCount(): Double = activeRafflesCounter.get().toDouble()
    private fun getPopularStationsCount(): Double = popularStationsCounter.get().toDouble()

    private fun getCouponConversionRate(): Double {
        val purchases = couponPurchaseCounter.count()
        val redemptions = couponRedemptionCounter.count()
        return if (purchases > 0) redemptions / purchases else 0.0
    }

    private fun getUserEngagementScore(): Double {
        // Calculate based on user activity metrics
        return 0.85 // Placeholder - implement actual calculation
    }

    private fun getRaffleParticipationRate(): Double {
        val activeUsers = activeUsersCounter.get()
        val participatingUsers = activeUsers * 0.65 // Placeholder
        return if (activeUsers > 0) participatingUsers / activeUsers else 0.0
    }

    private fun getAverageSearchRadius(): Double {
        return 5.2 // Placeholder - implement actual calculation
    }

    private fun getErrorRate(): Double {
        val total = totalRequests.get()
        val errors = totalErrors.get()
        return if (total > 0) errors.toDouble() / total.toDouble() else 0.0
    }

    private fun getThroughput(): Double {
        // Calculate requests per second over last minute
        return totalRequests.get() / 60.0 // Simplified calculation
    }

    // ==================== HELPER METHODS ====================

    private fun getAmountRange(amount: BigDecimal): String = when {
        amount < BigDecimal("100") -> "small"
        amount < BigDecimal("500") -> "medium"
        amount < BigDecimal("1000") -> "large"
        else -> "xlarge"
    }

    private fun getFuelAmountRange(amount: BigDecimal): String = when {
        amount < BigDecimal("20") -> "small"
        amount < BigDecimal("50") -> "medium"
        amount < BigDecimal("100") -> "large"
        else -> "xlarge"
    }

    private fun getMultiplierRange(multiplier: Double): String = when {
        multiplier <= 1.0 -> "none"
        multiplier <= 2.0 -> "low"
        multiplier <= 5.0 -> "medium"
        else -> "high"
    }

    private fun getRadiusRange(radius: Double): String = when {
        radius <= 1.0 -> "very_close"
        radius <= 5.0 -> "close"
        radius <= 10.0 -> "medium"
        radius <= 25.0 -> "far"
        else -> "very_far"
    }

    private fun getResultsRange(count: Int): String = when {
        count == 0 -> "none"
        count <= 5 -> "few"
        count <= 20 -> "some"
        count <= 50 -> "many"
        else -> "lots"
    }

    private fun getIpRange(ipAddress: String): String {
        // Anonymize IP for privacy while maintaining useful metrics
        val parts = ipAddress.split(".")
        return if (parts.size >= 2) "${parts[0]}.${parts[1]}.x.x" else "unknown"
    }

    // ==================== ADMIN METHODS ====================

    fun resetDailyCounters() {
        dailyRevenueCounter.set(0)
    }

    fun resetMonthlyCounters() {
        monthlyRevenueCounter.set(0)
    }

    fun updateActiveCouponsCount(count: Long) {
        activeCouponsCounter.set(count)
    }

    fun updateActiveUsersCount(count: Long) {
        activeUsersCounter.set(count)
    }

    fun updateActiveRafflesCount(count: Long) {
        activeRafflesCounter.set(count)
    }

    fun updatePopularStationsCount(count: Long) {
        popularStationsCounter.set(count)
    }
}