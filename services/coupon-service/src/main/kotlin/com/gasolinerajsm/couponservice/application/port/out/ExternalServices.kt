package com.gasolinerajsm.couponservice.application.port.out

import com.gasolinerajsm.couponservice.domain.event.DomainEvent
import com.gasolinerajsm.couponservice.domain.valueobject.CampaignId
import com.gasolinerajsm.couponservice.domain.valueobject.CouponId
import java.math.BigDecimal

/**
 * Port for publishing domain events
 */
interface EventPublisher {
    suspend fun publish(event: DomainEvent): Result<Unit>
    suspend fun publishAll(events: List<DomainEvent>): Result<Unit>
}

/**
 * Port for QR code generation services
 */
interface QRCodeService {
    suspend fun generateQRCode(
        data: String,
        format: String = "PNG",
        size: Int = 200
    ): Result<ByteArray>

    suspend fun generateQRCodeUrl(
        data: String,
        size: Int = 200
    ): Result<String>

    suspend fun validateQRCodeData(data: String): Result<Boolean>
}

/**
 * Port for notification services
 */
interface NotificationService {
    suspend fun notifyCouponCreated(
        couponId: CouponId,
        campaignId: CampaignId,
        couponCode: String,
        recipientUserId: String? = null
    ): Result<Unit>

    suspend fun notifyCouponUsed(
        couponId: CouponId,
        couponCode: String,
        discountApplied: BigDecimal,
        stationId: String?,
        userId: String?
    ): Result<Unit>

    suspend fun notifyCouponExpiring(
        couponId: CouponId,
        couponCode: String,
        expiresAt: java.time.LocalDateTime,
        userId: String?
    ): Result<Unit>

    suspend fun notifyCampaignCompleted(
        campaignId: CampaignId,
        campaignName: String,
        totalCoupons: Int,
        usedCoupons: Int
    ): Result<Unit>
}

/**
 * Port for analytics and reporting services
 */
interface AnalyticsService {
    suspend fun recordCouponGeneration(
        campaignId: CampaignId,
        quantity: Int,
        timestamp: java.time.LocalDateTime
    ): Result<Unit>

    suspend fun recordCouponUsage(
        couponId: CouponId,
        campaignId: CampaignId,
        purchaseAmount: BigDecimal,
        discountApplied: BigDecimal,
        stationId: String?,
        fuelType: String?,
        timestamp: java.time.LocalDateTime
    ): Result<Unit>

    suspend fun getCampaignMetrics(
        campaignId: CampaignId,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime
    ): Result<CampaignMetrics>

    suspend fun getUsageMetrics(
        campaignId: CampaignId? = null,
        stationId: String? = null,
        startDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime
    ): Result<UsageMetrics>
}

/**
 * Port for external validation services
 */
interface ValidationService {
    suspend fun validatePurchaseTransaction(
        transactionId: String,
        purchaseAmount: BigDecimal,
        stationId: String?
    ): Result<Boolean>

    suspend fun validateUserEligibility(
        userId: String,
        couponId: CouponId
    ): Result<Boolean>

    suspend fun validateStationOperational(
        stationId: String
    ): Result<Boolean>
}

/**
 * Port for security and encryption services
 */
interface SecurityService {
    suspend fun generateSecureSignature(
        data: String,
        secretKey: String
    ): Result<String>

    suspend fun verifySignature(
        data: String,
        signature: String,
        secretKey: String
    ): Result<Boolean>

    suspend fun encryptSensitiveData(data: String): Result<String>

    suspend fun decryptSensitiveData(encryptedData: String): Result<String>
}

/**
 * Campaign metrics data class
 */
data class CampaignMetrics(
    val campaignId: CampaignId,
    val totalCouponsGenerated: Int,
    val totalCouponsUsed: Int,
    val totalDiscountGiven: BigDecimal,
    val usageRate: Double,
    val averageDiscountPerCoupon: BigDecimal,
    val topStations: List<StationUsage>,
    val topFuelTypes: List<FuelTypeUsage>
)

/**
 * Usage metrics data class
 */
data class UsageMetrics(
    val totalUsages: Int,
    val totalDiscountGiven: BigDecimal,
    val averageDiscountPerUsage: BigDecimal,
    val usagesByHour: Map<Int, Int>,
    val usagesByDay: Map<String, Int>,
    val usagesByStation: Map<String, Int>,
    val usagesByFuelType: Map<String, Int>
)

/**
 * Station usage data class
 */
data class StationUsage(
    val stationId: String,
    val usageCount: Int,
    val totalDiscount: BigDecimal
)

/**
 * Fuel type usage data class
 */
data class FuelTypeUsage(
    val fuelType: String,
    val usageCount: Int,
    val totalDiscount: BigDecimal
)