package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.Campaign
import com.gasolinerajsm.couponservice.model.CampaignStatus
import com.gasolinerajsm.couponservice.model.CampaignType
import com.gasolinerajsm.couponservice.repository.CampaignRepository
import com.gasolinerajsm.couponservice.repository.CouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Service class for managing campaigns
 */
@Service
class CampaignService(
    private val campaignRepository: CampaignRepository,
    private val couponRepository: CouponRepository
) {

    /**
     * Create a new campaign
     */
    @Transactional
    fun createCampaign(
        name: String,
        description: String? = null,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        budget: BigDecimal? = null,
        maxCoupons: Int? = null,
        defaultDiscountAmount: BigDecimal? = null,
        defaultDiscountPercentage: BigDecimal? = null,
        defaultRaffleTickets: Int = 0,
        createdBy: String? = null
    ): Campaign {
        // Validate input
        require(name.isNotBlank()) { "Campaign name cannot be blank" }
        require(startDate.isBefore(endDate)) { "Start date must be before end date" }
        require(!startDate.isBefore(LocalDateTime.now())) { "Start date cannot be in the past" }

        if (budget != null) {
            require(budget > BigDecimal.ZERO) { "Budget must be greater than zero" }
        }

        if (defaultDiscountAmount != null && defaultDiscountPercentage != null) {
            throw IllegalArgumentException("Cannot specify both discount amount and percentage")
        }

        if (campaignRepository.existsByNameIgnoreCase(name)) {
            throw IllegalArgumentException("Campaign with name '$name' already exists")
        }

        val campaign = Campaign(
            name = name,
            description = description,
            campaignCode = generateCampaignCode(name),
            discountType = if (defaultDiscountPercentage != null) com.gasolinerajsm.couponservice.model.DiscountType.PERCENTAGE
                           else com.gasolinerajsm.couponservice.model.DiscountType.FIXED_AMOUNT,
            discountValue = defaultDiscountAmount ?: defaultDiscountPercentage ?: BigDecimal("10.00"),
            budget = budget,
            maxCoupons = maxCoupons,
            defaultDiscountAmount = defaultDiscountAmount,
            defaultRaffleTickets = defaultRaffleTickets,
            startDate = startDate,
            endDate = endDate,
            createdBy = createdBy
        )

        return campaignRepository.save(campaign)
    }

    /**
     * Get campaign by ID
     */
    fun getCampaignById(id: Long): Campaign {
        return campaignRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Campaign with ID $id not found") }
    }

    /**
     * Get campaign by name
     */
    fun getCampaignByName(name: String): Campaign {
        return campaignRepository.findByNameIgnoreCase(name)
            ?: throw IllegalArgumentException("Campaign with name '$name' not found")
    }

    /**
     * Get campaigns by status
     */
    fun getCampaignsByStatus(status: CampaignStatus, pageable: Pageable): Page<Campaign> {
        return campaignRepository.findByStatus(status, pageable)
    }

    /**
     * Search campaigns
     */
    fun searchCampaigns(searchTerm: String, pageable: Pageable): Page<Campaign> {
        return campaignRepository.searchByNameOrDescription(searchTerm, pageable)
    }

    /**
     * Update campaign
     */
    @Transactional
    fun updateCampaign(
        id: Long,
        name: String? = null,
        description: String? = null,
        budget: BigDecimal? = null,
        maxCoupons: Int? = null,
        updatedBy: String? = null
    ): Campaign {
        val campaign = getCampaignById(id)

        // Check if campaign allows modifications
        if (!campaign.status.allowsModifications()) {
            throw IllegalStateException("Campaign in status ${campaign.status} does not allow modifications")
        }

        // Check for duplicate name if name is being changed
        if (name != null && name != campaign.name) {
            if (campaignRepository.existsByNameIgnoreCase(name)) {
                throw IllegalArgumentException("Campaign with name '$name' already exists")
            }
        }

        val updatedCampaign = campaign.copy(
            name = name ?: campaign.name,
            description = description ?: campaign.description,
            budget = budget ?: campaign.budget,
            maxCoupons = maxCoupons ?: campaign.maxCoupons,
            updatedBy = updatedBy ?: campaign.updatedBy
        )

        return campaignRepository.save(updatedCampaign)
    }

    /**
     * Activate campaign
     */
    @Transactional
    fun activateCampaign(id: Long, activatedBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        if (!campaign.status.allowsActivation()) {
            throw IllegalStateException("Campaign in status ${campaign.status} cannot be activated")
        }

        if (LocalDateTime.now().isAfter(campaign.endDate)) {
            throw IllegalStateException("Cannot activate expired campaign")
        }

        val updatedCampaign = campaign.copy(
            status = CampaignStatus.ACTIVE,
            updatedBy = activatedBy ?: campaign.updatedBy
        )

        return campaignRepository.save(updatedCampaign)
    }

    /**
     * Pause campaign
     */
    @Transactional
    fun pauseCampaign(id: Long, pausedBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        if (campaign.status != CampaignStatus.ACTIVE) {
            throw IllegalStateException("Only active campaigns can be paused")
        }

        val updatedCampaign = campaign.copy(
            status = CampaignStatus.PAUSED,
            updatedBy = pausedBy ?: campaign.updatedBy
        )

        return campaignRepository.save(updatedCampaign)
    }

    /**
     * Complete campaign
     */
    @Transactional
    fun completeCampaign(id: Long, completedBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        if (campaign.status != CampaignStatus.ACTIVE && campaign.status != CampaignStatus.PAUSED) {
            throw IllegalStateException("Only active or paused campaigns can be completed")
        }

        val updatedCampaign = campaign.copy(
            status = CampaignStatus.COMPLETED,
            updatedBy = completedBy ?: campaign.updatedBy
        )

        return campaignRepository.save(updatedCampaign)
    }

    /**
     * Cancel campaign
     */
    @Transactional
    fun cancelCampaign(id: Long, cancelledBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        if (campaign.status.isFinalState()) {
            throw IllegalStateException("Campaign in final state ${campaign.status} cannot be cancelled")
        }

        val updatedCampaign = campaign.copy(
            status = CampaignStatus.CANCELLED,
            updatedBy = cancelledBy ?: campaign.updatedBy
        )

        return campaignRepository.save(updatedCampaign)
    }

    /**
     * Get campaign statistics
     */
    fun getCampaignStatistics(): Map<String, Any> {
        return campaignRepository.getCampaignStatistics()
    }

    /**
     * Get campaign performance metrics
     */
    fun getCampaignPerformanceMetrics(): List<CampaignPerformanceMetric> {
        return campaignRepository.getCampaignPerformanceMetrics()
            .map { array ->
                CampaignPerformanceMetric(
                    campaign = array[0] as Campaign,
                    usageRate = array[1] as Double,
                    budgetUtilization = array[2] as Double
                )
            }
    }

    /**
     * Get campaign budget utilization
     */
    fun getCampaignBudgetUtilization(id: Long): Double {
        val campaign = getCampaignById(id)
        return if (campaign.budget != null && campaign.budget > BigDecimal.ZERO) {
            (campaign.spentAmount.toDouble() / campaign.budget.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Get campaign usage rate
     */
    fun getCampaignUsageRate(id: Long): Double {
        val campaign = getCampaignById(id)
        return if (campaign.generatedCoupons > 0) {
            (campaign.usedCoupons.toDouble() / campaign.generatedCoupons.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Refresh campaign statistics
     */
    @Transactional
    fun refreshCampaignStatistics(id: Long): Campaign {
        val campaign = getCampaignById(id)
        val generatedCoupons = couponRepository.countByCampaign(campaign)
        val usedCoupons = couponRepository.countUsedCouponsByCampaign(campaign)

        campaignRepository.updateCampaignCouponStats(id, generatedCoupons.toInt(), usedCoupons.toInt())

        return campaign.copy(
            generatedCoupons = generatedCoupons.toInt(),
            usedCoupons = usedCoupons.toInt()
        )
    }

    /**
     * Update expired campaigns status
     */
    @Transactional
    fun updateExpiredCampaignsStatus(): Int {
        return campaignRepository.updateExpiredCampaignsStatus(LocalDateTime.now())
    }

    /**
     * Find campaigns without coupons
     */
    fun findCampaignsWithoutCoupons(): List<Campaign> {
        return campaignRepository.findCampaignsWithoutCoupons()
    }

    /**
     * Check if campaign name exists
     */
    fun campaignNameExists(name: String): Boolean {
        return campaignRepository.existsByNameIgnoreCase(name)
    }

    /**
     * Count campaigns by status
     */
    fun countCampaignsByStatus(status: CampaignStatus): Long {
        return campaignRepository.countByStatus(status)
    }

    /**
     * Update campaign spent amount
     */
    @Transactional
    fun updateCampaignSpentAmount(id: Long, amount: BigDecimal): Campaign {
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }

        campaignRepository.updateCampaignSpentAmount(id, amount)
        return getCampaignById(id).copy(spentAmount = amount)
    }

    /**
     * Get campaigns with budget remaining
     */
    fun getCampaignsWithBudgetRemaining(): List<Campaign> {
        return campaignRepository.findCampaignsWithBudgetRemaining()
    }

    /**
     * Get campaigns over budget
     */
    fun getCampaignsOverBudget(): List<Campaign> {
        return campaignRepository.findCampaignsOverBudget()
    }

    /**
     * Generate unique campaign code
     */
    private fun generateCampaignCode(name: String): String {
        val baseCode = name.replace(Regex("[^A-Za-z0-9]"), "").uppercase().take(10)
        val timestamp = System.currentTimeMillis().toString().takeLast(4)
        return "$baseCode$timestamp"
    }
}

/**
 * Data class for campaign performance metrics
 */
data class CampaignPerformanceMetric(
    val campaign: Campaign,
    val usageRate: Double,
    val budgetUtilization: Double
)