package com.gasolinerajsm.couponservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.couponservice.model.Campaign
import com.gasolinerajsm.couponservice.model.CampaignStatus
import com.gasolinerajsm.couponservice.model.CampaignType
import com.gasolinerajsm.couponservice.model.DiscountType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Request DTO for creating a campaign
 */
data class CreateCampaignRequest(
    @field:NotBlank(message = "Campaign name is required")
    @field:Size(min = 2, max = 200, message = "Campaign name must be between 2 and 200 characters")
    @JsonProperty("name")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("campaign_type")
    val campaignType: CampaignType = CampaignType.DISCOUNT,

    @field:NotNull(message = "Start date is required")
    @JsonProperty("start_date")
    val startDate: LocalDateTime,

    @field:NotNull(message = "End date is required")
    @JsonProperty("end_date")
    val endDate: LocalDateTime,

    @field:DecimalMin(value = "0.0", message = "Budget must be positive")
    @JsonProperty("budget")
    val budget: BigDecimal? = null,

    @field:Min(value = 1, message = "Max coupons must be at least 1")
    @JsonProperty("max_coupons")
    val maxCoupons: Int? = null,

    @field:Size(max = 500, message = "Target audience must not exceed 500 characters")
    @JsonProperty("target_audience")
    val targetAudience: String? = null,

    @JsonProperty("applicable_stations")
    val applicableStations: String? = null,

    @JsonProperty("applicable_fuel_types")
    val applicableFuelTypes: String? = null,

    @field:DecimalMin(value = "0.0", message = "Minimum purchase amount must be positive")
    @JsonProperty("minimum_purchase_amount")
    val minimumPurchaseAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Default discount amount must be positive")
    @JsonProperty("default_discount_amount")
    val defaultDiscountAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Default discount percentage must be positive")
    @field:DecimalMax(value = "100.0", message = "Default discount percentage cannot exceed 100%")
    @JsonProperty("default_discount_percentage")
    val defaultDiscountPercentage: BigDecimal? = null,

    @field:Min(value = 1, message = "Default raffle tickets must be at least 1")
    @JsonProperty("default_raffle_tickets")
    val defaultRaffleTickets: Int = 1,

    @field:Min(value = 1, message = "Max uses per coupon must be at least 1")
    @JsonProperty("max_uses_per_coupon")
    val maxUsesPerCoupon: Int? = null,

    @field:Min(value = 1, message = "Max uses per user must be at least 1")
    @JsonProperty("max_uses_per_user")
    val maxUsesPerUser: Int? = null,

    @field:Size(max = 2000, message = "Terms and conditions must not exceed 2000 characters")
    @JsonProperty("terms_and_conditions")
    val termsAndConditions: String? = null,

    @JsonProperty("created_by")
    val createdBy: String? = null
)

/**
 * Request DTO for updating a campaign
 */
data class UpdateCampaignRequest(
    @field:Size(min = 2, max = 200, message = "Campaign name must be between 2 and 200 characters")
    @JsonProperty("name")
    val name: String? = null,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("start_date")
    val startDate: LocalDateTime? = null,

    @JsonProperty("end_date")
    val endDate: LocalDateTime? = null,

    @field:DecimalMin(value = "0.0", message = "Budget must be positive")
    @JsonProperty("budget")
    val budget: BigDecimal? = null,

    @field:Min(value = 1, message = "Max coupons must be at least 1")
    @JsonProperty("max_coupons")
    val maxCoupons: Int? = null,

    @field:Size(max = 500, message = "Target audience must not exceed 500 characters")
    @JsonProperty("target_audience")
    val targetAudience: String? = null,

    @JsonProperty("applicable_stations")
    val applicableStations: String? = null,

    @JsonProperty("applicable_fuel_types")
    val applicableFuelTypes: String? = null,

    @field:DecimalMin(value = "0.0", message = "Minimum purchase amount must be positive")
    @JsonProperty("minimum_purchase_amount")
    val minimumPurchaseAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Default discount amount must be positive")
    @JsonProperty("default_discount_amount")
    val defaultDiscountAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Default discount percentage must be positive")
    @field:DecimalMax(value = "100.0", message = "Default discount percentage cannot exceed 100%")
    @JsonProperty("default_discount_percentage")
    val defaultDiscountPercentage: BigDecimal? = null,

    @field:Min(value = 1, message = "Default raffle tickets must be at least 1")
    @JsonProperty("default_raffle_tickets")
    val defaultRaffleTickets: Int? = null,

    @field:Min(value = 1, message = "Max uses per coupon must be at least 1")
    @JsonProperty("max_uses_per_coupon")
    val maxUsesPerCoupon: Int? = null,

    @field:Min(value = 1, message = "Max uses per user must be at least 1")
    @JsonProperty("max_uses_per_user")
    val maxUsesPerUser: Int? = null,

    @field:Size(max = 2000, message = "Terms and conditions must not exceed 2000 characters")
    @JsonProperty("terms_and_conditions")
    val termsAndConditions: String? = null,

    @JsonProperty("updated_by")
    val updatedBy: String? = null
)

/**
 * Response DTO for campaign information
 */
data class CampaignResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("description")
    val description: String?,

    @JsonProperty("status")
    val status: CampaignStatus,

    @JsonProperty("status_display_name")
    val statusDisplayName: String,

    @JsonProperty("campaign_type")
    val campaignType: CampaignType,

    @JsonProperty("campaign_type_display_name")
    val campaignTypeDisplayName: String,

    @JsonProperty("start_date")
    val startDate: LocalDateTime,

    @JsonProperty("end_date")
    val endDate: LocalDateTime,

    @JsonProperty("budget")
    val budget: BigDecimal?,

    @JsonProperty("spent_amount")
    val spentAmount: BigDecimal,

    @JsonProperty("remaining_budget")
    val remainingBudget: BigDecimal?,

    @JsonProperty("budget_utilization_rate")
    val budgetUtilizationRate: Double?,

    @JsonProperty("max_coupons")
    val maxCoupons: Int?,

    @JsonProperty("generated_coupons")
    val generatedCoupons: Int,

    @JsonProperty("used_coupons")
    val usedCoupons: Int,

    @JsonProperty("remaining_coupon_slots")
    val remainingCouponSlots: Int?,

    @JsonProperty("usage_rate")
    val usageRate: Double,

    @JsonProperty("target_audience")
    val targetAudience: String?,

    @JsonProperty("applicable_stations")
    val applicableStations: String?,

    @JsonProperty("applicable_stations_list")
    val applicableStationsList: List<Long>,

    @JsonProperty("applicable_fuel_types")
    val applicableFuelTypes: String?,

    @JsonProperty("applicable_fuel_types_list")
    val applicableFuelTypesList: List<String>,

    @JsonProperty("minimum_purchase_amount")
    val minimumPurchaseAmount: BigDecimal?,

    @JsonProperty("default_discount_amount")
    val defaultDiscountAmount: BigDecimal?,

    @JsonProperty("default_discount_percentage")
    val defaultDiscountPercentage: BigDecimal?,

    @JsonProperty("default_discount_type")
    val defaultDiscountType: DiscountType,

    @JsonProperty("default_raffle_tickets")
    val defaultRaffleTickets: Int,

    @JsonProperty("max_uses_per_coupon")
    val maxUsesPerCoupon: Int?,

    @JsonProperty("max_uses_per_user")
    val maxUsesPerUser: Int?,

    @JsonProperty("terms_and_conditions")
    val termsAndConditions: String?,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("is_expired")
    val isExpired: Boolean,

    @JsonProperty("is_not_yet_started")
    val isNotYetStarted: Boolean,

    @JsonProperty("can_generate_more_coupons")
    val canGenerateMoreCoupons: Boolean,

    @JsonProperty("has_budget_remaining")
    val hasBudgetRemaining: Boolean,

    @JsonProperty("created_by")
    val createdBy: String?,

    @JsonProperty("updated_by")
    val updatedBy: String?,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromCampaign(campaign: Campaign): CampaignResponse {
            return CampaignResponse(
                id = campaign.id,
                name = campaign.name,
                description = campaign.description,
                status = campaign.status,
                statusDisplayName = campaign.status.displayName,
                campaignType = campaign.campaignType,
                campaignTypeDisplayName = campaign.campaignType.displayName,
                startDate = campaign.startDate,
                endDate = campaign.endDate,
                budget = campaign.budget,
                spentAmount = campaign.spentAmount,
                remainingBudget = campaign.getRemainingBudget(),
                budgetUtilizationRate = campaign.getBudgetUtilizationRate(),
                maxCoupons = campaign.maxCoupons,
                generatedCoupons = campaign.generatedCoupons,
                usedCoupons = campaign.usedCoupons,
                remainingCouponSlots = campaign.getRemainingCouponSlots(),
                usageRate = campaign.getUsageRate(),
                targetAudience = campaign.targetAudience,
                applicableStations = campaign.applicableStations,
                applicableStationsList = campaign.getApplicableStationsList(),
                applicableFuelTypes = campaign.applicableFuelTypes,
                applicableFuelTypesList = campaign.getApplicableFuelTypesList(),
                minimumPurchaseAmount = campaign.minimumPurchaseAmount,
                defaultDiscountAmount = campaign.defaultDiscountAmount,
                defaultDiscountPercentage = campaign.defaultDiscountPercentage,
                defaultDiscountType = campaign.getDefaultDiscountType(),
                defaultRaffleTickets = campaign.defaultRaffleTickets,
                maxUsesPerCoupon = campaign.maxUsesPerCoupon,
                maxUsesPerUser = campaign.maxUsesPerUser,
                termsAndConditions = campaign.termsAndConditions,
                isActive = campaign.isActive(),
                isExpired = campaign.isExpired(),
                isNotYetStarted = campaign.isNotYetStarted(),
                canGenerateMoreCoupons = campaign.canGenerateMoreCoupons(),
                hasBudgetRemaining = campaign.hasBudgetRemaining(),
                createdBy = campaign.createdBy,
                updatedBy = campaign.updatedBy,
                createdAt = campaign.createdAt,
                updatedAt = campaign.updatedAt
            )
        }
    }
}

/**
 * Request DTO for campaign search
 */
data class CampaignSearchRequest(
    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("status")
    val status: CampaignStatus? = null,

    @JsonProperty("campaign_type")
    val campaignType: CampaignType? = null,

    @JsonProperty("created_by")
    val createdBy: String? = null,

    @JsonProperty("start_date_from")
    val startDateFrom: LocalDateTime? = null,

    @JsonProperty("start_date_to")
    val startDateTo: LocalDateTime? = null,

    @JsonProperty("end_date_from")
    val endDateFrom: LocalDateTime? = null,

    @JsonProperty("end_date_to")
    val endDateTo: LocalDateTime? = null,

    @JsonProperty("station_id")
    val stationId: Long? = null,

    @JsonProperty("fuel_type")
    val fuelType: String? = null,

    @JsonProperty("min_budget")
    val minBudget: BigDecimal? = null,

    @JsonProperty("max_budget")
    val maxBudget: BigDecimal? = null,

    @JsonProperty("created_from")
    val createdFrom: LocalDateTime? = null,

    @JsonProperty("created_until")
    val createdUntil: LocalDateTime? = null
)

/**
 * Response DTO for campaign statistics
 */
data class CampaignStatisticsResponse(
    @JsonProperty("total_campaigns")
    val totalCampaigns: Long,

    @JsonProperty("active_campaigns")
    val activeCampaigns: Long,

    @JsonProperty("draft_campaigns")
    val draftCampaigns: Long,

    @JsonProperty("completed_campaigns")
    val completedCampaigns: Long,

    @JsonProperty("cancelled_campaigns")
    val cancelledCampaigns: Long,

    @JsonProperty("total_budget")
    val totalBudget: BigDecimal,

    @JsonProperty("total_spent")
    val totalSpent: BigDecimal,

    @JsonProperty("total_coupons_generated")
    val totalCouponsGenerated: Long,

    @JsonProperty("total_coupons_used")
    val totalCouponsUsed: Long,

    @JsonProperty("average_coupons_per_campaign")
    val averageCouponsPerCampaign: Double,

    @JsonProperty("overall_usage_rate")
    val overallUsageRate: Double,

    @JsonProperty("overall_budget_utilization")
    val overallBudgetUtilization: Double
)

/**
 * Response DTO for campaign performance metrics
 */
data class CampaignPerformanceResponse(
    @JsonProperty("campaign")
    val campaign: CampaignResponse,

    @JsonProperty("usage_rate")
    val usageRate: Double,

    @JsonProperty("budget_utilization")
    val budgetUtilization: Double,

    @JsonProperty("performance_score")
    val performanceScore: Double,

    @JsonProperty("recommendations")
    val recommendations: List<String>
)

/**
 * Request DTO for updating campaign budget
 */
data class UpdateCampaignBudgetRequest(
    @field:DecimalMin(value = "0.0", message = "Amount must be positive")
    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("description")
    val description: String? = null
)

/**
 * Request DTO for campaign status update
 */
data class UpdateCampaignStatusRequest(
    @JsonProperty("status")
    val status: CampaignStatus,

    @JsonProperty("updated_by")
    val updatedBy: String? = null,

    @JsonProperty("reason")
    val reason: String? = null
)