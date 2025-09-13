package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.*
import com.gasolinerajsm.couponservice.repository.CampaignRepository
import com.gasolinerajsm.couponservice.repository.CouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Service for managing campaigns - creation, lifecycle management, and analytics
 */
@Service
@Transactional
class CampaignService(
    private val campaignRepository: CampaignRepository,
    private val couponRepository: CouponRepository
) {

    /**
     * Create a new campaign
     */
    fun createCampaign(
        name: String,
        description: String? = null,
        campaignType: CampaignType = CampaignType.DISCOUNT,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        budget: BigDecimal? = null,
        maxCoupons: Int? = null,
        targetAudience: String? = null,
        applicableStations: String? = null,
        applicableFuelTypes: String? = null,
        minimumPurchaseAmount: BigDecimal? = null,
        defaultDiscountAmount: BigDecimal? = null,
        defaultDiscountPercentage: BigDecimal? = null,
        defaultRaffleTickets: Int = 1,
        maxUsesPerCoupon: Int? = null,
        maxUsesPerUser: Int? = null,
        termsAndConditions: String? = null,
        createdBy: String? = null
    ): Campaign {
        // Validate campaign name uniqueness
        if (campaignRepository.existsByNameIgnoreCase(name)) {
            throw IllegalArgumentException("Campaign with name '$name' already exists")
        }

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw IllegalArgumentException("Start date must be before end date")
        }

        // Validate discount configuration
        if (defaultDiscountAmount != null && defaultDiscountPercentage != null) {
            throw IllegalArgumentException("Cannot have both default fixed amount and percentage discount")
        }

        // Validate budget
        if (budget != null && budget <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Budget must be positive")
        }

        // Validate max coupons
        if (maxCoupons != null && maxCoupons <= 0) {
            throw IllegalArgumentException("Max coupons must be positive")
        }

        val campaign = Campaign(
            name = name,
            description = description,
            campaignType = campaignType,
            startDate = startDate,
            endDate = endDate,
            budget = budget,
            maxCoupons = maxCoupons,
            targetAudience = targetAudience,
            applicableStations = applicableStations,
            applicableFuelTypes = applicableFuelTypes,
            minimumPurchaseAmount = minimumPurchaseAmount,
            defaultDiscountAmount = defaultDiscountAmount,
            defaultDiscountPercentage = defaultDiscountPercentage,
            defaultRaffleTickets = defaultRaffleTickets,
            maxUsesPerCoupon = maxUsesPerCoupon,
            maxUsesPerUser = maxUsesPerUser,
            termsAndConditions = termsAndConditions,
            createdBy = createdBy
        )

        return campaignRepository.save(campaign)
    }

    /**
     * Get campaign by ID
     */
    @Transactional(readOnly = true)
    fun getCampaignById(id: Long): Campaign {
        return campaignRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Campaign not found with id: $id") }
    }

    /**
     * Get campaign by name
     */
    @Transactional(readOnly = true)
    fun getCampaignByName(name: String): Campaign? {
        return campaignRepository.findByNameIgnoreCase(name)
    }

    /**
     * Get all campaigns with pagination
     */
    @Transactional(readOnly = true)
    fun getAllCampaigns(pageable: Pageable): Page<Campaign> {
        return campaignRepository.findAll(pageable)
    }

    /**
     * Get campaigns by status
     */
    @Transactional(readOnly = true)
    fun getCampaignsByStatus(status: CampaignStatus, pageable: Pageable): Page<Campaign> {
        return campaignRepository.findByStatus(status, pageable)
    }

    /**
     * Get campaigns by type
     */
    @Transactional(readOnly = true)
    fun getCampaignsByType(campaignType: CampaignType, pageable: Pageable): Page<Campaign> {
        return campaignRepository.findByCampaignType(campaignType, pageable)
    }

    /**
     * Get active campaigns
     */
    @Transactional(readOnly = true)
    fun getActiveCampaigns(pageable: Pageable): Page<Campaign> {
        return campaignRepository.findActiveCampaigns(LocalDateTime.now(), pageable)
    }

    /**
     * Get campaigns by creator
     */
    @Transactional(readOnly = true)
    fun getCampaignsByCreator(createdBy: String, pageable: Pageable): Page<Campaign> {
        return campaignRepository.findByCreatedBy(createdBy, pageable)
    }

    /**
     * Search campaigns by name or description
     */
    @Transactional(readOnly = true)
    fun searchCampaigns(searchTerm: String, pageable: Pageable): Page<Campaign> {
        return campaignRepository.searchByNameOrDescription(searchTerm, pageable)
    }

    /**
     * Update campaign
     */
    fun updateCampaign(
        id: Long,
        name: String? = null,
        description: String? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        budget: BigDecimal? = null,
        maxCoupons: Int? = null,
        targetAudience: String? = null,
        applicableStations: String? = null,
        applicableFuelTypes: String? = null,
        minimumPurchaseAmount: BigDecimal? = null,
        defaultDiscountAmount: BigDecimal? = null,
        defaultDiscountPercentage: BigDecimal? = null,
        defaultRaffleTickets: Int? = null,
        maxUsesPerCoupon: Int? = null,
        maxUsesPerUser: Int? = null,
        termsAndConditions: String? = null,
        updatedBy: String? = null
    ): Campaign {
        val campaign = getCampaignById(id)

        // Check if campaign can be modified
        if (!campaign.status.allowsModifications()) {
            throw IllegalStateException("Campaign cannot be modified in status: ${campaign.status}")
        }

        // Validate name uniqueness if changing name
        if (name != null && name != campaign.name && campaignRepository.existsByNameIgnoreCase(name)) {
            throw IllegalArgumentException("Campaign with name '$name' already exists")
        }

        // Validate date range if changing dates
        val newStartDate = startDate ?: campaign.startDate
        val newEndDate = endDate ?: campaign.endDate
        if (newStartDate.isAfter(newEndDate)) {
            throw IllegalArgumentException("Start date must be before end date")
        }

        // Validate discount configuration
        val newDiscountAmount = defaultDiscountAmount ?: campaign.defaultDiscountAmount
        val newDiscountPercentage = defaultDiscountPercentage ?: campaign.defaultDiscountPercentage
        if (newDiscountAmount != null && newDiscountPercentage != null) {
            throw IllegalArgumentException("Cannot have both default fixed amount and percentage discount")
        }

        val updatedCampaign = campaign.copy(
            name = name ?: campaign.name,
            description = description ?: campaign.description,
            startDate = newStartDate,
            endDate = newEndDate,
            budget = budget ?: campaign.budget,
            maxCoupons = maxCoupons ?: campaign.maxCoupons,
            targetAudience = targetAudience ?: campaign.targetAudience,
            applicableStations = applicableStations ?: campaign.applicableStations,
            applicableFuelTypes = applicableFuelTypes ?: campaign.applicableFuelTypes,
            minimumPurchaseAmount = minimumPurchaseAmount ?: campaign.minimumPurchaseAmount,
            defaultDiscountAmount = newDiscountAmount,
            defaultDiscountPercentage = newDiscountPercentage,
            defaultRaffleTickets = defaultRaffleTickets ?: campaign.defaultRaffleTickets,
            maxUsesPerCoupon = maxUsesPerCoupon ?: campaign.maxUsesPerCoupon,
            maxUsesPerUser = maxUsesPerUser ?: campaign.maxUsesPerUser,
            termsAndConditions = termsAndConditions ?: campaign.termsAndConditions,
            updatedBy = updatedBy
        )

        return campaignRepository.save(updatedCampaign)
    }

    /**
     * Activate campaign
     */
    fun activateCampaign(id: Long, updatedBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        // Validate campaign can be activated
        if (campaign.status.isFinalState()) {
            throw IllegalStateException("Cannot activate campaign in final state: ${campaign.status}")
        }

        // Check if campaign dates are valid for activation
        val now = LocalDateTime.now()
        if (campaign.endDate.isBefore(now)) {
            throw IllegalStateException("Cannot activate expired campaign")
        }

        val activatedCampaign = campaign.activate(updatedBy)
        return campaignRepository.save(activatedCampaign)
    }

    /**
     * Pause campaign
     */
    fun pauseCampaign(id: Long, updatedBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        if (campaign.status != CampaignStatus.ACTIVE) {
            throw IllegalStateException("Can only pause active campaigns")
        }

        val pausedCampaign = campaign.pause(updatedBy)
        return campaignRepository.save(pausedCampaign)
    }

    /**
     * Complete campaign
     */
    fun completeCampaign(id: Long, updatedBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        if (campaign.status != CampaignStatus.ACTIVE && campaign.status != CampaignStatus.PAUSED) {
            throw IllegalStateException("Can only complete active or paused campaigns")
        }

        val completedCampaign = campaign.complete(updatedBy)
        return campaignRepository.save(completedCampaign)
    }

    /**
     * Cancel campaign
     */
    fun cancelCampaign(id: Long, updatedBy: String? = null): Campaign {
        val campaign = getCampaignById(id)

        if (campaign.status.isFinalState()) {
            throw IllegalStateException("Cannot cancel campaign in final state: ${campaign.status}")
        }

        val cancelledCampaign = campaign.cancel(updatedBy)
        return campaignRepository.save(cancelledCampaign)
    }

    /**
     * Delete campaign (soft delete by cancelling)
     */
    fun deleteCampaign(id: Long, updatedBy: String? = null) {
        cancelCampaign(id, updatedBy)
    }

    /**
     * Update campaign spent amount
     */
    fun updateCampaignSpentAmount(id: Long, amount: BigDecimal): Campaign {
        if (amount < BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount must be non-negative")
        }

        campaignRepository.updateCampaignSpentAmount(id, amount)
        return getCampaignById(id)
    }

    /**
     * Get expired campaigns
     */
    @Transactional(readOnly = true)
    fun getExpiredCampaigns(): List<Campaign> {
        return campaignRepository.findExpiredCampaigns(LocalDateTime.now())
    }

    /**
     * Get campaigns expiring soon
     */
    @Transactional(readOnly = true)
    fun getCampaignsExpiringSoon(hours: Long = 24): List<Campaign> {
        val threshold = LocalDateTime.now().plusHours(hours)
        return campaignRepository.findCampaignsExpiringSoon(LocalDateTime.now(), threshold)
    }

    /**
     * Get campaigns starting soon
     */
    @Transactional(readOnly = true)
    fun getCampaignsStartingSoon(hours: Long = 24): List<Campaign> {
        val threshold = LocalDateTime.now().plusHours(hours)
        return campaignRepository.findCampaignsStartingSoon(LocalDateTime.now(), threshold)
    }

    /**
     * Get campaigns for station
     */
    @Transactional(readOnly = true)
    fun getCampaignsForStation(stationId: Long): List<Campaign> {
        return campaignRepository.findCampaignsForStation(stationId, LocalDateTime.now())
    }

    /**
     * Get campaigns with budget remaining
     */
    @Transactional(readOnly = true)
    fun getCampaignsWithBudgetRemaining(): List<Campaign> {
        return campaignRepository.findCampaignsWithBudgetRemaining()
    }

    /**
     * Get campaigns over budget
     */
    @Transactional(readOnly = true)
    fun getCampaignsOverBudget(): List<Campaign> {
        return campaignRepository.findCampaignsOverBudget()
    }

    /**
     * Get campaigns with low budget
     */
    @Transactional(readOnly = true)
    fun getCampaignsWithLowBudget(threshold: Double = 80.0): List<Campaign> {
        return campaignRepository.findCampaignsWithLowBudget(threshold)
    }

    /**
     * Get campaigns that can generate more coupons
     */
    @Transactional(readOnly = true)
    fun getCampaignsCanGenerateMoreCoupons(): List<Campaign> {
        return campaignRepository.findCampaignsCanGenerateMoreCoupons()
    }

    /**
     * Get campaigns at coupon limit
     */
    @Transactional(readOnly = true)
    fun getCampaignsAtCouponLimit(): List<Campaign> {
        return campaignRepository.findCampaignsAtCouponLimit()
    }

    /**
     * Update expired campaigns status
     */
    fun updateExpiredCampaignsStatus(): Int {
        return campaignRepository.updateExpiredCampaignsStatus(LocalDateTime.now())
    }

    /**
     * Get campaign statistics
     */
    @Transactional(readOnly = true)
    fun getCampaignStatistics(): Map<String, Any> {
        return campaignRepository.getCampaignStatistics()
    }

    /**
     * Get campaign performance metrics
     */
    @Transactional(readOnly = true)
    fun getCampaignPerformanceMetrics(): List<CampaignPerformanceMetric> {
        val results = campaignRepository.getCampaignPerformanceMetrics()
        return results.map { result ->
            val campaign = result[0] as Campaign
            val usageRate = result[1] as Double
            val budgetUtilization = result[2] as Double

            CampaignPerformanceMetric(
                campaign = campaign,
                usageRate = usageRate,
                budgetUtilization = budgetUtilization
            )
        }
    }

    /**
     * Find campaigns without coupons
     */
    @Transactional(readOnly = true)
    fun findCampaignsWithoutCoupons(): List<Campaign> {
        return campaignRepository.findCampaignsWithoutCoupons()
    }

    /**
     * Find duplicate campaign names
     */
    @Transactional(readOnly = true)
    fun findDuplicateCampaignNames(): List<Campaign> {
        return campaignRepository.findDuplicateCampaignNames()
    }

    /**
     * Count campaigns by status
     */
    @Transactional(readOnly = true)
    fun countCampaignsByStatus(status: CampaignStatus): Long {
        return campaignRepository.countByStatus(status)
    }

    /**
     * Count campaigns by type
     */
    @Transactional(readOnly = true)
    fun countCampaignsByType(campaignType: CampaignType): Long {
        return campaignRepository.countByCampaignType(campaignType)
    }

    /**
     * Count active campaigns
     */
    @Transactional(readOnly = true)
    fun countActiveCampaigns(): Long {
        return campaignRepository.countActiveCampaigns(LocalDateTime.now())
    }

    /**
     * Check if campaign name exists
     */
    @Transactional(readOnly = true)
    fun campaignNameExists(name: String): Boolean {
        return campaignRepository.existsByNameIgnoreCase(name)
    }

    /**
     * Get campaign budget utilization
     */
    @Transactional(readOnly = true)
    fun getCampaignBudgetUtilization(id: Long): Double? {
        val campaign = getCampaignById(id)
        return campaign.getBudgetUtilizationRate()
    }

    /**
     * Get campaign usage rate
     */
    @Transactional(readOnly = true)
    fun getCampaignUsageRate(id: Long): Double {
        val campaign = getCampaignById(id)
        return campaign.getUsageRate()
    }

    /**
     * Refresh campaign statistics
     */
    fun refreshCampaignStatistics(id: Long): Campaign {
        val campaign = getCampaignById(id)
        val totalCoupons = couponRepository.countByCampaign(campaign).toInt()
        val usedCoupons = couponRepository.countUsedCouponsByCampaign(campaign).toInt()

        campaignRepository.updateCampaignCouponStats(id, totalCoupons, usedCoupons)
        return getCampaignById(id)
    }
}

/**
 * Campaign performance metric data class
 */
data class CampaignPerformanceMetric(
    val campaign: Campaign,
    val usageRate: Double,
    val budgetUtilization: Double
)