package com.gasolinerajsm.couponservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CouponStatus
import com.gasolinerajsm.couponservice.model.DiscountType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Request DTO for generating a coupon
 */
data class GenerateCouponRequest(
    @field:NotNull(message = "Campaign ID is required")
    @JsonProperty("campaign_id")
    val campaignId: Long,

    @field:Size(max = 50, message = "Coupon code must not exceed 50 characters")
    @JsonProperty("coupon_code")
    val couponCode: String? = null,

    @field:DecimalMin(value = "0.0", message = "Discount amount must be positive")
    @JsonProperty("discount_amount")
    val discountAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Discount percentage must be positive")
    @field:DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100%")
    @JsonProperty("discount_percentage")
    val discountPercentage: BigDecimal? = null,

    @field:Min(value = 1, message = "Raffle tickets must be at least 1")
    @JsonProperty("raffle_tickets")
    val raffleTickets: Int? = null,

    @JsonProperty("valid_from")
    val validFrom: LocalDateTime? = null,

    @JsonProperty("valid_until")
    val validUntil: LocalDateTime? = null,

    @field:Min(value = 1, message = "Max uses must be at least 1")
    @JsonProperty("max_uses")
    val maxUses: Int? = null,

    @field:DecimalMin(value = "0.0", message = "Minimum purchase amount must be positive")
    @JsonProperty("minimum_purchase_amount")
    val minimumPurchaseAmount: BigDecimal? = null,

    @JsonProperty("applicable_fuel_types")
    val applicableFuelTypes: String? = null,

    @JsonProperty("applicable_stations")
    val applicableStations: String? = null,

    @field:Size(max = 2000, message = "Terms and conditions must not exceed 2000 characters")
    @JsonProperty("terms_and_conditions")
    val termsAndConditions: String? = null
)

/**
 * Request DTO for generating multiple coupons
 */
data class GenerateMultipleCouponsRequest(
    @field:NotNull(message = "Campaign ID is required")
    @JsonProperty("campaign_id")
    val campaignId: Long,

    @field:Min(value = 1, message = "Count must be at least 1")
    @field:Max(value = 1000, message = "Count cannot exceed 1000")
    @JsonProperty("count")
    val count: Int,

    @field:DecimalMin(value = "0.0", message = "Base discount amount must be positive")
    @JsonProperty("base_discount_amount")
    val baseDiscountAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Base discount percentage must be positive")
    @field:DecimalMax(value = "100.0", message = "Base discount percentage cannot exceed 100%")
    @JsonProperty("base_discount_percentage")
    val baseDiscountPercentage: BigDecimal? = null,

    @field:Min(value = 1, message = "Base raffle tickets must be at least 1")
    @JsonProperty("base_raffle_tickets")
    val baseRaffleTickets: Int? = null
)

/**
 * Response DTO for coupon information
 */
data class CouponResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("campaign_id")
    val campaignId: Long,

    @JsonProperty("campaign_name")
    val campaignName: String,

    @JsonProperty("qr_code")
    val qrCode: String,

    @JsonProperty("coupon_code")
    val couponCode: String,

    @JsonProperty("status")
    val status: CouponStatus,

    @JsonProperty("status_display_name")
    val statusDisplayName: String,

    @JsonProperty("discount_amount")
    val discountAmount: BigDecimal?,

    @JsonProperty("discount_percentage")
    val discountPercentage: BigDecimal?,

    @JsonProperty("discount_type")
    val discountType: DiscountType,

    @JsonProperty("raffle_tickets")
    val raffleTickets: Int,

    @JsonProperty("valid_from")
    val validFrom: LocalDateTime,

    @JsonProperty("valid_until")
    val validUntil: LocalDateTime,

    @JsonProperty("max_uses")
    val maxUses: Int?,

    @JsonProperty("current_uses")
    val currentUses: Int,

    @JsonProperty("remaining_uses")
    val remainingUses: Int?,

    @JsonProperty("minimum_purchase_amount")
    val minimumPurchaseAmount: BigDecimal?,

    @JsonProperty("applicable_fuel_types")
    val applicableFuelTypes: String?,

    @JsonProperty("applicable_fuel_types_list")
    val applicableFuelTypesList: List<String>,

    @JsonProperty("applicable_stations")
    val applicableStations: String?,

    @JsonProperty("applicable_stations_list")
    val applicableStationsList: List<Long>,

    @JsonProperty("terms_and_conditions")
    val termsAndConditions: String?,

    @JsonProperty("is_valid")
    val isValid: Boolean,

    @JsonProperty("is_expired")
    val isExpired: Boolean,

    @JsonProperty("is_max_uses_reached")
    val isMaxUsesReached: Boolean,

    @JsonProperty("can_be_used")
    val canBeUsed: Boolean,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromCoupon(coupon: Coupon): CouponResponse {
            return CouponResponse(
                id = coupon.id,
                campaignId = coupon.campaign.id,
                campaignName = coupon.campaign.name,
                qrCode = coupon.qrCode,
                couponCode = coupon.couponCode,
                status = coupon.status,
                statusDisplayName = coupon.status.displayName,
                discountAmount = coupon.discountAmount,
                discountPercentage = coupon.discountPercentage,
                discountType = coupon.getDiscountType(),
                raffleTickets = coupon.raffleTickets,
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                maxUses = coupon.maxUses,
                currentUses = coupon.currentUses,
                remainingUses = coupon.getRemainingUses(),
                minimumPurchaseAmount = coupon.minimumPurchaseAmount,
                applicableFuelTypes = coupon.applicableFuelTypes,
                applicableFuelTypesList = coupon.getApplicableFuelTypesList(),
                applicableStations = coupon.applicableStations,
                applicableStationsList = coupon.getApplicableStationsList(),
                termsAndConditions = coupon.termsAndConditions,
                isValid = coupon.isValid(),
                isExpired = coupon.isExpired(),
                isMaxUsesReached = coupon.isMaxUsesReached(),
                canBeUsed = coupon.canBeUsed(),
                createdAt = coupon.createdAt,
                updatedAt = coupon.updatedAt
            )
        }
    }
}

/**
 * Request DTO for coupon validation
 */
data class CouponValidationRequest(
    @field:NotBlank(message = "QR code is required")
    @JsonProperty("qr_code")
    val qrCode: String,

    @field:NotNull(message = "Station ID is required")
    @JsonProperty("station_id")
    val stationId: Long,

    @JsonProperty("fuel_type")
    val fuelType: String? = null,

    @field:DecimalMin(value = "0.0", message = "Purchase amount must be positive")
    @JsonProperty("purchase_amount")
    val purchaseAmount: BigDecimal? = null
)

/**
 * Response DTO for coupon validation
 */
data class CouponValidationResponse(
    @JsonProperty("is_valid")
    val isValid: Boolean,

    @JsonProperty("can_be_used")
    val canBeUsed: Boolean,

    @JsonProperty("coupon")
    val coupon: CouponResponse?,

    @JsonProperty("errors")
    val errors: List<String>,

    @JsonProperty("discount_amount")
    val discountAmount: BigDecimal?,

    @JsonProperty("raffle_tickets")
    val raffleTickets: Int?,

    @JsonProperty("validation_timestamp")
    val validationTimestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Request DTO for coupon usage
 */
data class UseCouponRequest(
    @field:NotNull(message = "Coupon ID is required")
    @JsonProperty("coupon_id")
    val couponId: Long,

    @field:NotNull(message = "Station ID is required")
    @JsonProperty("station_id")
    val stationId: Long,

    @field:NotNull(message = "User ID is required")
    @JsonProperty("user_id")
    val userId: Long,

    @JsonProperty("fuel_type")
    val fuelType: String? = null,

    @field:DecimalMin(value = "0.0", message = "Purchase amount must be positive")
    @JsonProperty("purchase_amount")
    val purchaseAmount: BigDecimal? = null,

    @JsonProperty("transaction_reference")
    val transactionReference: String? = null
)

/**
 * Response DTO for coupon usage
 */
data class UseCouponResponse(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("coupon")
    val coupon: CouponResponse,

    @JsonProperty("discount_applied")
    val discountApplied: BigDecimal,

    @JsonProperty("raffle_tickets_earned")
    val raffleTicketsEarned: Int,

    @JsonProperty("remaining_uses")
    val remainingUses: Int?,

    @JsonProperty("usage_timestamp")
    val usageTimestamp: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("message")
    val message: String
)

/**
 * Request DTO for coupon search
 */
data class CouponSearchRequest(
    @JsonProperty("campaign_id")
    val campaignId: Long? = null,

    @JsonProperty("status")
    val status: CouponStatus? = null,

    @JsonProperty("coupon_code")
    val couponCode: String? = null,

    @JsonProperty("valid_from")
    val validFrom: LocalDateTime? = null,

    @JsonProperty("valid_until")
    val validUntil: LocalDateTime? = null,

    @JsonProperty("station_id")
    val stationId: Long? = null,

    @JsonProperty("fuel_type")
    val fuelType: String? = null,

    @JsonProperty("min_discount_amount")
    val minDiscountAmount: BigDecimal? = null,

    @JsonProperty("max_discount_amount")
    val maxDiscountAmount: BigDecimal? = null,

    @JsonProperty("created_from")
    val createdFrom: LocalDateTime? = null,

    @JsonProperty("created_until")
    val createdUntil: LocalDateTime? = null
)

/**
 * Response DTO for coupon statistics
 */
data class CouponStatisticsResponse(
    @JsonProperty("total_coupons")
    val totalCoupons: Long,

    @JsonProperty("active_coupons")
    val activeCoupons: Long,

    @JsonProperty("expired_coupons")
    val expiredCoupons: Long,

    @JsonProperty("used_up_coupons")
    val usedUpCoupons: Long,

    @JsonProperty("used_coupons")
    val usedCoupons: Long,

    @JsonProperty("total_uses")
    val totalUses: Long,

    @JsonProperty("average_uses")
    val averageUses: Double,

    @JsonProperty("total_campaigns")
    val totalCampaigns: Long,

    @JsonProperty("usage_rate")
    val usageRate: Double
)

/**
 * Response DTO for QR code data
 */
data class QrCodeDataResponse(
    @JsonProperty("qr_code")
    val qrCode: String,

    @JsonProperty("signature")
    val signature: String,

    @JsonProperty("coupon_code")
    val couponCode: String,

    @JsonProperty("campaign_id")
    val campaignId: Long,

    @JsonProperty("campaign_name")
    val campaignName: String,

    @JsonProperty("discount_amount")
    val discountAmount: BigDecimal?,

    @JsonProperty("discount_percentage")
    val discountPercentage: BigDecimal?,

    @JsonProperty("raffle_tickets")
    val raffleTickets: Int,

    @JsonProperty("valid_from")
    val validFrom: LocalDateTime,

    @JsonProperty("valid_until")
    val validUntil: LocalDateTime,

    @JsonProperty("terms_and_conditions")
    val termsAndConditions: String?
)