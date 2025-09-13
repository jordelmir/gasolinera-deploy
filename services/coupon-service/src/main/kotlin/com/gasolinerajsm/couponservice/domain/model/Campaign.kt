package com.gasolinerajsm.couponservice.domain.model

import com.gasolinerajsm.couponservice.domain.event.DomainEvent
import com.gasolinerajsm.couponservice.domain.event.CampaignCreatedEvent
import com.gasolinerajsm.couponservice.domain.event.CampaignStatusChangedEvent
import com.gasolinerajsm.couponservice.domain.valueobject.CampaignId
import com.gasolinerajsm.couponservice.domain.valueobject.DiscountValue
import java.time.LocalDateTime

/**
 * Campaign Domain Entity
 * Represents a marketing campaign that generates coupons
 */
data class Campaign(
    val id: CampaignId,
    val name: String,
    val description: String?,
    val status: CampaignStatus = CampaignStatus.DRAFT,
    val validityPeriod: ValidityPeriod,
    val defaultDiscountValue: DiscountValue,
    val defaultRaffleTickets: Int = 1,
    val generationStrategy: CouponGenerationStrategy = CouponGenerationStrategy.ON_DEMAND,
    val targetCouponCount: Int? = null,
    val generatedCouponCount: Int = 0,
    val usedCouponCount: Int = 0,
    val applicabilityRules: ApplicabilityRules,
    val usageRules: UsageRules,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new campaign
         */
        fun create(
            name: String,
            description: String?,
            validityPeriod: ValidityPeriod,
            defaultDiscountValue: DiscountValue,
            defaultRaffleTickets: Int = 1,
            generationStrategy: CouponGenerationStrategy = CouponGenerationStrategy.ON_DEMAND,
            targetCouponCount: Int? = null,
            applicabilityRules: ApplicabilityRules,
            usageRules: UsageRules,
            metadata: Map<String, String> = emptyMap()
        ): Campaign {
            val campaign = Campaign(
                id = CampaignId.generate(),
                name = name,
                description = description,
                validityPeriod = validityPeriod,
                defaultDiscountValue = defaultDiscountValue,
                defaultRaffleTickets = defaultRaffleTickets,
                generationStrategy = generationStrategy,
                targetCouponCount = targetCouponCount,
                applicabilityRules = applicabilityRules,
                usageRules = usageRules,
                metadata = metadata
            )

            campaign.addDomainEvent(
                CampaignCreatedEvent(
                    campaignId = campaign.id,
                    name = campaign.name,
                    validityPeriod = campaign.validityPeriod,
                    discountValue = campaign.defaultDiscountValue,
                    targetCouponCount = campaign.targetCouponCount,
                    occurredAt = LocalDateTime.now()
                )
            )

            return campaign
        }
    }

    /**
     * Check if campaign is currently active
     */
    fun isActive(): Boolean {
        return status == CampaignStatus.ACTIVE && validityPeriod.isValidAt(LocalDateTime.now())
    }

    /**
     * Check if campaign can generate coupons
     */
    fun canGenerateCoupons(): Boolean {
        return isActive() && !hasReachedTargetCount()
    }

    /**
     * Check if campaign has reached its target coupon count
     */
    fun hasReachedTargetCount(): Boolean {
        return targetCouponCount?.let { generatedCouponCount >= it } ?: false
    }

    /**
     * Change campaign status
     */
    fun changeStatus(newStatus: CampaignStatus, reason: String? = null): Campaign {
        if (status == newStatus) {
            return this
        }

        if (!status.canChangeTo(newStatus)) {
            throw IllegalStateException("Cannot change status from $status to $newStatus")
        }

        val updatedCampaign = this.copy(
            status = newStatus,
            updatedAt = LocalDateTime.now()
        )

        updatedCampaign.addDomainEvent(
            CampaignStatusChangedEvent(
                campaignId = id,
                oldStatus = status,
                newStatus = newStatus,
                reason = reason,
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedCampaign
    }

    /**
     * Activate the campaign
     */
    fun activate(): Campaign = changeStatus(CampaignStatus.ACTIVE, "Campaign activated")

    /**
     * Pause the campaign
     */
    fun pause(): Campaign = changeStatus(CampaignStatus.PAUSED, "Campaign paused")

    /**
     * Complete the campaign
     */
    fun complete(): Campaign = changeStatus(CampaignStatus.COMPLETED, "Campaign completed")

    /**
     * Cancel the campaign
     */
    fun cancel(reason: String? = null): Campaign = changeStatus(CampaignStatus.CANCELLED, reason)

    /**
     * Increment generated coupon count
     */
    fun incrementGeneratedCount(count: Int = 1): Campaign {
        return this.copy(
            generatedCouponCount = generatedCouponCount + count,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Increment used coupon count
     */
    fun incrementUsedCount(count: Int = 1): Campaign {
        return this.copy(
            usedCouponCount = usedCouponCount + count,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Get campaign progress percentage
     */
    fun getProgressPercentage(): Double {
        return targetCouponCount?.let {
            (generatedCouponCount.toDouble() / it * 100).coerceAtMost(100.0)
        } ?: 0.0
    }

    /**
     * Get usage rate percentage
     */
    fun getUsageRate(): Double {
        return if (generatedCouponCount > 0) {
            (usedCouponCount.toDouble() / generatedCouponCount * 100)
        } else {
            0.0
        }
    }

    /**
     * Get remaining coupon capacity
     */
    fun getRemainingCapacity(): Int? {
        return targetCouponCount?.let { it - generatedCouponCount }
    }

    /**
     * Check if campaign is expired
     */
    fun isExpired(): Boolean {
        return validityPeriod.isExpired()
    }

    /**
     * Check if campaign is not yet valid
     */
    fun isNotYetValid(): Boolean {
        return validityPeriod.isNotYetValid()
    }

    /**
     * Update metadata
     */
    fun updateMetadata(newMetadata: Map<String, String>): Campaign {
        return this.copy(
            metadata = metadata + newMetadata,
            updatedAt = LocalDateTime.now()
        )
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "Campaign(id=$id, name='$name', status=$status, generated=$generatedCouponCount, used=$usedCouponCount)"
    }
}

/**
 * Extension for CampaignStatus to define valid transitions
 */
private fun CampaignStatus.canChangeTo(newStatus: CampaignStatus): Boolean {
    return when (this) {
        CampaignStatus.DRAFT -> newStatus in listOf(CampaignStatus.ACTIVE, CampaignStatus.CANCELLED)
        CampaignStatus.ACTIVE -> newStatus in listOf(CampaignStatus.PAUSED, CampaignStatus.COMPLETED, CampaignStatus.CANCELLED)
        CampaignStatus.PAUSED -> newStatus in listOf(CampaignStatus.ACTIVE, CampaignStatus.COMPLETED, CampaignStatus.CANCELLED)
        CampaignStatus.COMPLETED, CampaignStatus.CANCELLED -> false // Final states
    }
}