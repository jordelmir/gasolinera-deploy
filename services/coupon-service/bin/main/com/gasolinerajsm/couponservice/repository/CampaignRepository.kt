package com.gasolinerajsm.couponservice.repository

import com.gasolinerajsm.couponservice.model.Campaign
import com.gasolinerajsm.couponservice.model.CampaignStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Repository interface for Campaign entity operations
 */
@Repository
interface CampaignRepository : JpaRepository<Campaign, Long> {

    /**
     * Find campaign by name (case insensitive)
     */
    fun findByNameIgnoreCase(name: String): Campaign?

    /**
     * Find campaigns by status
     */
    fun findByStatus(status: CampaignStatus, pageable: Pageable): Page<Campaign>

    /**
     * Find campaigns by status in list
     */
    fun findByStatusIn(statuses: List<CampaignStatus>, pageable: Pageable): Page<Campaign>

    /**
     * Check if campaign exists by name (case insensitive)
     */
    fun existsByNameIgnoreCase(name: String): Boolean

    /**
     * Search campaigns by name or description
     */
    @Query("SELECT c FROM Campaign c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    fun searchByNameOrDescription(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Campaign>

    /**
     * Find active campaigns within date range
     */
    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.startDate <= :currentDate AND c.endDate >= :currentDate")
    fun findActiveCampaignsInDateRange(@Param("currentDate") currentDate: LocalDateTime, pageable: Pageable): Page<Campaign>

    /**
     * Find campaigns with budget remaining
     */
    @Query("SELECT c FROM Campaign c WHERE c.budget IS NOT NULL AND (c.spentAmount IS NULL OR c.spentAmount < c.budget)")
    fun findCampaignsWithBudgetRemaining(): List<Campaign>

    /**
     * Find campaigns over budget
     */
    @Query("SELECT c FROM Campaign c WHERE c.budget IS NOT NULL AND c.spentAmount > c.budget")
    fun findCampaignsOverBudget(): List<Campaign>

    /**
     * Find campaigns without coupons
     */
    @Query("SELECT c FROM Campaign c WHERE c.generatedCoupons = 0")
    fun findCampaignsWithoutCoupons(): List<Campaign>

    /**
     * Count campaigns by status
     */
    fun countByStatus(status: CampaignStatus): Long

    /**
     * Update campaign spent amount
     */
    @Query("UPDATE Campaign c SET c.spentAmount = :amount WHERE c.id = :id")
    fun updateCampaignSpentAmount(@Param("id") id: Long, @Param("amount") amount: BigDecimal): Int

    /**
     * Update campaign coupon statistics
     */
    @Query("UPDATE Campaign c SET c.generatedCoupons = :generated, c.usedCoupons = :used WHERE c.id = :id")
    fun updateCampaignCouponStats(@Param("id") id: Long, @Param("generated") generated: Int, @Param("used") used: Int): Int

    /**
     * Update expired campaigns status
     */
    @Query("UPDATE Campaign c SET c.status = 'EXPIRED' WHERE c.status = 'ACTIVE' AND c.endDate < :currentDate")
    fun updateExpiredCampaignsStatus(@Param("currentDate") currentDate: LocalDateTime): Int

    /**
     * Get campaign statistics
     */
    @Query("""
        SELECT
            COUNT(c) as totalCampaigns,
            COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as activeCampaigns,
            SUM(c.generatedCoupons) as totalCouponsGenerated,
            SUM(c.usedCoupons) as totalCouponsUsed,
            AVG(c.usedCoupons * 100.0 / NULLIF(c.generatedCoupons, 0)) as averageUsageRate
        FROM Campaign c
    """)
    fun getCampaignStatistics(): Map<String, Any>

    /**
     * Get campaign performance metrics
     */
    @Query("""
        SELECT
            c as campaign,
            CASE WHEN c.generatedCoupons > 0 THEN (c.usedCoupons * 100.0 / c.generatedCoupons) ELSE 0 END as usageRate,
            CASE WHEN c.budget > 0 THEN (c.spentAmount * 100.0 / c.budget) ELSE 0 END as budgetUtilization
        FROM Campaign c
        ORDER BY usageRate DESC
    """)
    fun getCampaignPerformanceMetrics(): List<Array<Any>>

    /**
     * Find campaigns by date range
     */
    fun findByStartDateBetween(startDate: LocalDateTime, endDate: LocalDateTime, pageable: Pageable): Page<Campaign>

    /**
     * Find campaigns created by user
     */
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<Campaign>
}