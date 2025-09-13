package com.gasolinerajsm.couponservice.application.port.`in`

import com.gasolinerajsm.couponservice.domain.model.*
import com.gasolinerajsm.couponservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Command to create a new campaign
 */
data class CreateCampaignCommand(
    val name: String,
    val description: String?,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val discountType: DiscountType,
    val discountAmount: BigDecimal? = null,
    val discountPercentage: BigDecimal? = null,
    val defaultRaffleTickets: Int = 1,
    val generationStrategy: CouponGenerationStrategy = CouponGenerationStrategy.ON_DEMAND,
    val targetCouponCount: Int? = null,
    val minimumPurchaseAmount: BigDecimal? = null,
    val maximumPurchaseAmount: BigDecimal? = null,
    val applicableFuelTypes: Set<FuelType> = emptySet(),
    val applicableStationIds: Set<String> = emptySet(),
    val excludedStationIds: Set<String> = emptySet(),
    val maxUses: Int? = null,
    val maxUsesPerUser: Int? = null,
    val cooldownPeriodMinutes: Int? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    fun getValidityPeriod(): ValidityPeriod = ValidityPeriod(validFrom, validUntil)

    fun getDiscountValue(): DiscountValue {
        return when (discountType) {
            DiscountType.FIXED_AMOUNT -> DiscountValue.fixedAmount(discountAmount!!)
            DiscountType.PERCENTAGE -> DiscountValue.percentage(discountPercentage!!)
            DiscountType.NONE -> DiscountValue.none()
        }
    }

    fun getUsageRules(): UsageRules {
        return UsageRules(
            maxUses = maxUses,
            maxUsesPerUser = maxUsesPerUser,
            cooldownPeriodMinutes = cooldownPeriodMinutes
        )
    }

    fun getApplicabilityRules(): ApplicabilityRules {
        return ApplicabilityRules(
            minimumPurchaseAmount = minimumPurchaseAmount,
            maximumPurchaseAmount = maximumPurchaseAmount,
            applicableFuelTypes = applicableFuelTypes,
            applicableStationIds = applicableStationIds,
            excludedStationIds = excludedStationIds
        )
    }
}

/**
 * Command to generate coupons for a campaign
 */
data class GenerateCouponsCommand(
    val campaignId: CampaignId,
    val quantity: Int,
    val customValidityPeriod: ValidityPeriod? = null,
    val customDiscountValue: DiscountValue? = null,
    val customRaffleTickets: Int? = null,
    val batchId: String? = null
)

/**
 * Command to use/redeem a coupon
 */
data class UseCouponCommand(
    val couponCode: String,
    val purchaseAmount: BigDecimal,
    val fuelType: String? = null,
    val stationId: String? = null,
    val userId: String? = null,
    val transactionId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Command to validate a coupon without using it
 */
data class ValidateCouponCommand(
    val couponCode: String,
    val purchaseAmount: BigDecimal? = null,
    val fuelType: String? = null,
    val stationId: String? = null,
    val userId: String? = null
)

/**
 * Command to scan QR code
 */
data class ScanQRCodeCommand(
    val qrCodeData: String,
    val stationId: String? = null,
    val scannedBy: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Command to update campaign status
 */
data class UpdateCampaignStatusCommand(
    val campaignId: CampaignId,
    val newStatus: CampaignStatus,
    val reason: String? = null
)

/**
 * Command to update coupon status
 */
data class UpdateCouponStatusCommand(
    val couponId: CouponId,
    val newStatus: CouponStatus,
    val reason: String? = null
)

/**
 * Command to expire coupons
 */
data class ExpireCouponsCommand(
    val campaignId: CampaignId? = null,
    val asOf: LocalDateTime = LocalDateTime.now(),
    val batchSize: Int = 100
)

/**
 * Query to get campaign details
 */
data class GetCampaignQuery(
    val campaignId: CampaignId
)

/**
 * Query to get coupon details
 */
data class GetCouponQuery(
    val couponId: CouponId? = null,
    val couponCode: String? = null
)

/**
 * Query to search campaigns
 */
data class SearchCampaignsQuery(
    val name: String? = null,
    val status: CampaignStatus? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Query to search coupons
 */
data class SearchCouponsQuery(
    val campaignId: CampaignId? = null,
    val status: CouponStatus? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val userId: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Query to get campaign analytics
 */
data class GetCampaignAnalyticsQuery(
    val campaignId: CampaignId,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null
)

/**
 * Query to get coupon usage analytics
 */
data class GetCouponUsageAnalyticsQuery(
    val campaignId: CampaignId? = null,
    val stationId: String? = null,
    val fuelType: String? = null,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val groupBy: AnalyticsGroupBy = AnalyticsGroupBy.DAY
)

/**
 * Analytics grouping options
 */
enum class AnalyticsGroupBy {
    HOUR,
    DAY,
    WEEK,
    MONTH
}