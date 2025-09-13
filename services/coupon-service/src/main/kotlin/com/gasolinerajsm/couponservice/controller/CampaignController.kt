package com.gasolinerajsm.couponservice.controller

import com.gasolinerajsm.couponservice.dto.*
import com.gasolinerajsm.couponservice.model.CampaignStatus
import com.gasolinerajsm.couponservice.model.CampaignType
import com.gasolinerajsm.couponservice.service.CampaignService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * REST Controller for Campaign management operations
 */
@RestController
@RequestMapping("/api/v1/campaigns")
@CrossOrigin(origins = ["*"])
class CampaignController(
    private val campaignService: CampaignService
) {

    /**
     * Create a new campaign
     */
    @PostMapping
    fun createCampaign(@Valid @RequestBody request: CreateCampaignRequest): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.createCampaign(
            name = request.name,
            description = request.description,
            campaignType = request.campaignType,
            startDate = request.startDate,
            endDate = request.endDate,
            budget = request.budget,
            maxCoupons = request.maxCoupons,
            targetAudience = request.targetAudience,
            applicableStations = request.applicableStations,
            applicableFuelTypes = request.applicableFuelTypes,
            minimumPurchaseAmount = request.minimumPurchaseAmount,
            defaultDiscountAmount = request.defaultDiscountAmount,
            defaultDiscountPercentage = request.defaultDiscountPercentage,
            defaultRaffleTickets = request.defaultRaffleTickets,
            maxUsesPerCoupon = request.maxUsesPerCoupon,
            maxUsesPerUser = request.maxUsesPerUser,
            termsAndConditions = request.termsAndConditions,
            createdBy = request.createdBy
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Get campaign by ID
     */
    @GetMapping("/{id}")
    fun getCampaignById(@PathVariable id: Long): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.getCampaignById(id)
        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Get campaign by name
     */
    @GetMapping("/name/{name}")
    fun getCampaignByName(@PathVariable name: String): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.getCampaignByName(name)
        return if (campaign != null) {
            ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all campaigns with pagination
     */
    @GetMapping
    fun getAllCampaigns(pageable: Pageable): ResponseEntity<Page<CampaignResponse>> {
        val campaigns = campaignService.getAllCampaigns(pageable)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns by status
     */
    @GetMapping("/status/{status}")
    fun getCampaignsByStatus(
        @PathVariable status: CampaignStatus,
        pageable: Pageable
    ): ResponseEntity<Page<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsByStatus(status, pageable)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns by type
     */
    @GetMapping("/type/{type}")
    fun getCampaignsByType(
        @PathVariable type: CampaignType,
        pageable: Pageable
    ): ResponseEntity<Page<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsByType(type, pageable)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get active campaigns
     */
    @GetMapping("/active")
    fun getActiveCampaigns(pageable: Pageable): ResponseEntity<Page<CampaignResponse>> {
        val campaigns = campaignService.getActiveCampaigns(pageable)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns by creator
     */
    @GetMapping("/creator/{createdBy}")
    fun getCampaignsByCreator(
        @PathVariable createdBy: String,
        pageable: Pageable
    ): ResponseEntity<Page<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsByCreator(createdBy, pageable)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Search campaigns
     */
    @GetMapping("/search")
    fun searchCampaigns(
        @RequestParam searchTerm: String,
        pageable: Pageable
    ): ResponseEntity<Page<CampaignResponse>> {
        val campaigns = campaignService.searchCampaigns(searchTerm, pageable)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Update campaign
     */
    @PutMapping("/{id}")
    fun updateCampaign(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCampaignRequest
    ): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.updateCampaign(
            id = id,
            name = request.name,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            budget = request.budget,
            maxCoupons = request.maxCoupons,
            targetAudience = request.targetAudience,
            applicableStations = request.applicableStations,
            applicableFuelTypes = request.applicableFuelTypes,
            minimumPurchaseAmount = request.minimumPurchaseAmount,
            defaultDiscountAmount = request.defaultDiscountAmount,
            defaultDiscountPercentage = request.defaultDiscountPercentage,
            defaultRaffleTickets = request.defaultRaffleTickets,
            maxUsesPerCoupon = request.maxUsesPerCoupon,
            maxUsesPerUser = request.maxUsesPerUser,
            termsAndConditions = request.termsAndConditions,
            updatedBy = request.updatedBy
        )

        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Activate campaign
     */
    @PostMapping("/{id}/activate")
    fun activateCampaign(
        @PathVariable id: Long,
        @RequestParam(required = false) updatedBy: String?
    ): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.activateCampaign(id, updatedBy)
        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Pause campaign
     */
    @PostMapping("/{id}/pause")
    fun pauseCampaign(
        @PathVariable id: Long,
        @RequestParam(required = false) updatedBy: String?
    ): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.pauseCampaign(id, updatedBy)
        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Complete campaign
     */
    @PostMapping("/{id}/complete")
    fun completeCampaign(
        @PathVariable id: Long,
        @RequestParam(required = false) updatedBy: String?
    ): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.completeCampaign(id, updatedBy)
        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Cancel campaign
     */
    @PostMapping("/{id}/cancel")
    fun cancelCampaign(
        @PathVariable id: Long,
        @RequestParam(required = false) updatedBy: String?
    ): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.cancelCampaign(id, updatedBy)
        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Delete campaign
     */
    @DeleteMapping("/{id}")
    fun deleteCampaign(
        @PathVariable id: Long,
        @RequestParam(required = false) updatedBy: String?
    ): ResponseEntity<Void> {
        campaignService.deleteCampaign(id, updatedBy)
        return ResponseEntity.noContent().build()
    }

    /**
     * Update campaign status
     */
    @PatchMapping("/{id}/status")
    fun updateCampaignStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCampaignStatusRequest
    ): ResponseEntity<CampaignResponse> {
        val campaign = when (request.status) {
            CampaignStatus.ACTIVE -> campaignService.activateCampaign(id, request.updatedBy)
            CampaignStatus.PAUSED -> campaignService.pauseCampaign(id, request.updatedBy)
            CampaignStatus.COMPLETED -> campaignService.completeCampaign(id, request.updatedBy)
            CampaignStatus.CANCELLED -> campaignService.cancelCampaign(id, request.updatedBy)
            else -> throw IllegalArgumentException("Cannot set campaign to status: ${request.status}")
        }

        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Update campaign budget
     */
    @PatchMapping("/{id}/budget")
    fun updateCampaignBudget(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCampaignBudgetRequest
    ): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.updateCampaignSpentAmount(id, request.amount)
        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Get expired campaigns
     */
    @GetMapping("/expired")
    fun getExpiredCampaigns(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getExpiredCampaigns()
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns expiring soon
     */
    @GetMapping("/expiring-soon")
    fun getCampaignsExpiringSoon(@RequestParam(defaultValue = "24") hours: Long): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsExpiringSoon(hours)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns starting soon
     */
    @GetMapping("/starting-soon")
    fun getCampaignsStartingSoon(@RequestParam(defaultValue = "24") hours: Long): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsStartingSoon(hours)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns for station
     */
    @GetMapping("/station/{stationId}")
    fun getCampaignsForStation(@PathVariable stationId: Long): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsForStation(stationId)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns with budget remaining
     */
    @GetMapping("/budget/remaining")
    fun getCampaignsWithBudgetRemaining(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsWithBudgetRemaining()
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns over budget
     */
    @GetMapping("/budget/over")
    fun getCampaignsOverBudget(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsOverBudget()
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns with low budget
     */
    @GetMapping("/budget/low")
    fun getCampaignsWithLowBudget(@RequestParam(defaultValue = "80.0") threshold: Double): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsWithLowBudget(threshold)
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns that can generate more coupons
     */
    @GetMapping("/coupons/can-generate")
    fun getCampaignsCanGenerateMoreCoupons(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsCanGenerateMoreCoupons()
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get campaigns at coupon limit
     */
    @GetMapping("/coupons/at-limit")
    fun getCampaignsAtCouponLimit(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getCampaignsAtCouponLimit()
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Update expired campaigns status
     */
    @PostMapping("/maintenance/update-expired")
    fun updateExpiredCampaignsStatus(): ResponseEntity<Map<String, Int>> {
        val updatedCount = campaignService.updateExpiredCampaignsStatus()
        val response = mapOf("updated_count" to updatedCount)
        return ResponseEntity.ok(response)
    }

    /**
     * Get campaign statistics
     */
    @GetMapping("/statistics")
    fun getCampaignStatistics(): ResponseEntity<CampaignStatisticsResponse> {
        val stats = campaignService.getCampaignStatistics()

        val response = CampaignStatisticsResponse(
            totalCampaigns = stats["totalCampaigns"] as? Long ?: 0L,
            activeCampaigns = stats["activeCampaigns"] as? Long ?: 0L,
            draftCampaigns = stats["draftCampaigns"] as? Long ?: 0L,
            completedCampaigns = stats["completedCampaigns"] as? Long ?: 0L,
            cancelledCampaigns = stats["cancelledCampaigns"] as? Long ?: 0L,
            totalBudget = stats["totalBudget"] as? BigDecimal ?: BigDecimal.ZERO,
            totalSpent = stats["totalSpent"] as? BigDecimal ?: BigDecimal.ZERO,
            totalCouponsGenerated = stats["totalCouponsGenerated"] as? Long ?: 0L,
            totalCouponsUsed = stats["totalCouponsUsed"] as? Long ?: 0L,
            averageCouponsPerCampaign = stats["averageCouponsPerCampaign"] as? Double ?: 0.0,
            overallUsageRate = if ((stats["totalCouponsGenerated"] as? Long ?: 0L) > 0) {
                ((stats["totalCouponsUsed"] as? Long ?: 0L).toDouble() / (stats["totalCouponsGenerated"] as? Long ?: 1L).toDouble()) * 100
            } else 0.0,
            overallBudgetUtilization = if ((stats["totalBudget"] as? BigDecimal ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                ((stats["totalSpent"] as? BigDecimal ?: BigDecimal.ZERO).toDouble() / (stats["totalBudget"] as? BigDecimal ?: BigDecimal.ONE).toDouble()) * 100
            } else 0.0
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get campaign performance metrics
     */
    @GetMapping("/performance")
    fun getCampaignPerformanceMetrics(): ResponseEntity<List<CampaignPerformanceResponse>> {
        val metrics = campaignService.getCampaignPerformanceMetrics()

        val responses = metrics.map { metric ->
            val recommendations = mutableListOf<String>()

            // Generate recommendations based on performance
            if (metric.usageRate < 50.0) {
                recommendations.add("Consider increasing marketing efforts to improve coupon usage")
            }
            if (metric.budgetUtilization > 90.0) {
                recommendations.add("Budget is nearly exhausted, consider increasing budget or reducing campaign scope")
            }
            if (metric.usageRate > 80.0 && metric.budgetUtilization < 70.0) {
                recommendations.add("Campaign is performing well, consider expanding budget or duration")
            }

            val performanceScore = (metric.usageRate + (100 - metric.budgetUtilization)) / 2

            CampaignPerformanceResponse(
                campaign = CampaignResponse.fromCampaign(metric.campaign),
                usageRate = metric.usageRate,
                budgetUtilization = metric.budgetUtilization,
                performanceScore = performanceScore,
                recommendations = recommendations
            )
        }

        return ResponseEntity.ok(responses)
    }

    /**
     * Find campaigns without coupons
     */
    @GetMapping("/maintenance/without-coupons")
    fun findCampaignsWithoutCoupons(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.findCampaignsWithoutCoupons()
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Find duplicate campaign names
     */
    @GetMapping("/maintenance/duplicates/names")
    fun findDuplicateCampaignNames(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.findDuplicateCampaignNames()
        val responses = campaigns.map { CampaignResponse.fromCampaign(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Count campaigns by status
     */
    @GetMapping("/count/status/{status}")
    fun countCampaignsByStatus(@PathVariable status: CampaignStatus): ResponseEntity<Map<String, Long>> {
        val count = campaignService.countCampaignsByStatus(status)
        val response = mapOf("count" to count)
        return ResponseEntity.ok(response)
    }

    /**
     * Count campaigns by type
     */
    @GetMapping("/count/type/{type}")
    fun countCampaignsByType(@PathVariable type: CampaignType): ResponseEntity<Map<String, Long>> {
        val count = campaignService.countCampaignsByType(type)
        val response = mapOf("count" to count)
        return ResponseEntity.ok(response)
    }

    /**
     * Count active campaigns
     */
    @GetMapping("/count/active")
    fun countActiveCampaigns(): ResponseEntity<Map<String, Long>> {
        val count = campaignService.countActiveCampaigns()
        val response = mapOf("count" to count)
        return ResponseEntity.ok(response)
    }

    /**
     * Check if campaign name exists
     */
    @GetMapping("/exists/name/{name}")
    fun campaignNameExists(@PathVariable name: String): ResponseEntity<Map<String, Boolean>> {
        val exists = campaignService.campaignNameExists(name)
        val response = mapOf("exists" to exists)
        return ResponseEntity.ok(response)
    }

    /**
     * Get campaign budget utilization
     */
    @GetMapping("/{id}/budget/utilization")
    fun getCampaignBudgetUtilization(@PathVariable id: Long): ResponseEntity<Map<String, Double?>> {
        val utilization = campaignService.getCampaignBudgetUtilization(id)
        val response = mapOf("budget_utilization" to utilization)
        return ResponseEntity.ok(response)
    }

    /**
     * Get campaign usage rate
     */
    @GetMapping("/{id}/usage-rate")
    fun getCampaignUsageRate(@PathVariable id: Long): ResponseEntity<Map<String, Double>> {
        val usageRate = campaignService.getCampaignUsageRate(id)
        val response = mapOf("usage_rate" to usageRate)
        return ResponseEntity.ok(response)
    }

    /**
     * Refresh campaign statistics
     */
    @PostMapping("/{id}/refresh-stats")
    fun refreshCampaignStatistics(@PathVariable id: Long): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.refreshCampaignStatistics(id)
        return ResponseEntity.ok(CampaignResponse.fromCampaign(campaign))
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        val response = mapOf(
            "status" to "UP",
            "service" to "Campaign Service",
            "timestamp" to java.time.Instant.now().toString()
        )
        return ResponseEntity.ok(response)
    }
}