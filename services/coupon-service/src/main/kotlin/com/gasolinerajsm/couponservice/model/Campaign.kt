package com.gasolinerajsm.couponservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime
import com.gasolinerajsm.couponservice.model.CampaignStatus
import com.gasolinerajsm.couponservice.model.CampaignType

/**
 * Campaign entity representing a discount campaign in the system
 */
@Entity
@Table(name = "campaigns")
data class Campaign(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false)
    @field:NotBlank(message = "Campaign name is required")
    val name: String,

    @Column(name = "description")
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: CampaignStatus = CampaignStatus.DRAFT,

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false)
    val campaignType: CampaignType = CampaignType.PROMOTIONAL,

    @Column(name = "campaign_code", unique = true, nullable = false)
    @field:NotBlank(message = "Campaign code is required")
    val campaignCode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    val discountType: DiscountType = DiscountType.PERCENTAGE,

    @Column(name = "discount_value", nullable = false)
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    val discountValue: BigDecimal,

    @Column(name = "budget")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Budget must be non-negative")
    val budget: BigDecimal? = null,

    @Column(name = "spent_amount", nullable = false)
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Spent amount must be non-negative")
    val spentAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "max_coupons")
    @field:Min(value = 0, message = "Max coupons must be non-negative")
    val maxCoupons: Int? = null,

    @Column(name = "generated_coupons", nullable = false)
    @field:Min(value = 0, message = "Generated coupons must be non-negative")
    val generatedCoupons: Int = 0,

    @Column(name = "used_coupons", nullable = false)
    @field:Min(value = 0, message = "Used coupons must be non-negative")
    val usedCoupons: Int = 0,

    @Column(name = "default_discount_amount")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Default discount amount must be non-negative")
    val defaultDiscountAmount: BigDecimal? = null,

    @Column(name = "default_raffle_tickets", nullable = false)
    @field:Min(value = 0, message = "Default raffle tickets must be non-negative")
    val defaultRaffleTickets: Int = 0,

    @Column(name = "minimum_purchase")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Minimum purchase must be non-negative")
    val minimumPurchase: BigDecimal? = null,

    @Column(name = "maximum_discount")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Maximum discount must be non-negative")
    val maximumDiscount: BigDecimal? = null,

    @Column(name = "usage_limit_per_user")
    @field:Min(value = 1, message = "Usage limit per user must be at least 1")
    val usageLimitPerUser: Int? = null,

    @Column(name = "total_usage_limit")
    @field:Min(value = 1, message = "Total usage limit must be at least 1")
    val totalUsageLimit: Int? = null,

    @Column(name = "current_usage_count", nullable = false)
    val currentUsageCount: Int = 0,

    @Column(name = "raffle_tickets_per_coupon", nullable = false)
    @field:Min(value = 0, message = "Raffle tickets per coupon must be non-negative")
    val raffleTicketsPerCoupon: Int = 1,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDateTime,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDateTime,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "target_audience")
    val targetAudience: String? = null,

    @Column(name = "applicable_stations")
    val applicableStations: String? = "ALL", // JSON array or 'ALL'

    @Column(name = "terms_and_conditions")
    val termsAndConditions: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by")
    val createdBy: String? = null,

    @Column(name = "updated_by")
    val updatedBy: String? = null
) {

    fun isActiveAndScheduled(): Boolean {
        val now = LocalDateTime.now()
        return isActive && now.isAfter(startDate) && now.isBefore(endDate)
    }

    fun hasAvailableUses(): Boolean {
        return totalUsageLimit == null || currentUsageCount < totalUsageLimit
    }

    fun canBeUsedByUser(userUsageCount: Int): Boolean {
        return usageLimitPerUser == null || userUsageCount < usageLimitPerUser
    }

    // Additional methods needed by tests
    fun getRemainingCouponSlots(): Int? {
        return maxCoupons?.let { it - generatedCoupons }
    }

    fun getUsageRate(): Double {
        return if (generatedCoupons > 0) {
            (usedCoupons.toDouble() / generatedCoupons.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    fun updateCouponStats(generated: Int, used: Int): Campaign {
        return this.copy(
            generatedCoupons = generated,
            usedCoupons = used,
            updatedAt = LocalDateTime.now()
        )
    }

    fun appliesTo(stationId: Long): Boolean {
        return applicableStations == "ALL" || applicableStations?.contains(stationId.toString()) == true
    }

    fun appliesTo(fuelType: String): Boolean {
        return applicableStations == "ALL" || applicableStations?.contains(fuelType) == true
    }

    fun getApplicableStationsList(): List<Long> {
        return if (applicableStations == "ALL") {
            emptyList()
        } else {
            applicableStations?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
        }
    }

    fun getApplicableFuelTypesList(): List<String> {
        return if (applicableStations == "ALL") {
            emptyList()
        } else {
            applicableStations?.split(",")?.map { it.trim() } ?: emptyList()
        }
    }

    fun getDefaultDiscountType(): DiscountType {
        return when {
            defaultDiscountAmount != null && defaultDiscountAmount > BigDecimal.ZERO -> DiscountType.FIXED_AMOUNT
            discountType == DiscountType.PERCENTAGE -> DiscountType.PERCENTAGE
            else -> DiscountType.NONE
        }
    }

    fun calculateDefaultDiscount(purchaseAmount: BigDecimal): BigDecimal {
        return when (getDefaultDiscountType()) {
            DiscountType.FIXED_AMOUNT -> defaultDiscountAmount ?: BigDecimal.ZERO
            DiscountType.PERCENTAGE -> purchaseAmount * (discountValue / BigDecimal("100"))
            DiscountType.NONE -> BigDecimal.ZERO
        }
    }

    fun activate(activatedBy: String): Campaign {
        return this.copy(
            status = CampaignStatus.ACTIVE,
            updatedBy = activatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    fun pause(pausedBy: String): Campaign {
        return this.copy(
            status = CampaignStatus.PAUSED,
            updatedBy = pausedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    fun complete(completedBy: String): Campaign {
        return this.copy(
            status = CampaignStatus.COMPLETED,
            updatedBy = completedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    fun cancel(cancelledBy: String): Campaign {
        return this.copy(
            status = CampaignStatus.CANCELLED,
            updatedBy = cancelledBy,
            updatedAt = LocalDateTime.now()
        )
    }
}