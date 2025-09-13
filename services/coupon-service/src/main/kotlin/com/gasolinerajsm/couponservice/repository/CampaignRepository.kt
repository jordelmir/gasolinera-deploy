package com.gasolinerajsm.couponservice.repository

import com.gasolinerajsm.couponservice.model.Campaign
import com.gasolinerajsm.couponservice.model.CampaignStatus
import com.gasolinerajsm.couponservice.model.CampaignType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface CampaignRepository : JpaRepository<Campaign, Long> {

    /**
     * Find campaign by name
     */
    fun findByName(name: String): Campaign?

    /**
     * Find campaign by name (case insensitive)
     */
    fun findByNameIgnoreCase(name: String): Campaign?

    /**
     * Check if campaign name exists
     */
    fun existsByName(name: String): Boolean

    /**
     * Check if campaign name exists (case insensitive)
     */
    fun existsByNameIgnoreCase(name: String): Boolean

    /**
     * Find campaigns by status
     */
    fun findByStatus(status: CampaignStatus): List<Campaign>

    /**
     * Find campaigns by status with pagination
     */
    fun findByStatus(status: CampaignStatus, pageable: Pageable): Page<Campaign>

    /**
     * Find campaigns by type
     */
    fun findByCampaignType(campaignType: CampaignType): List<Campaign>

    /**
     * Find campaigns by type with pagination
     */
    fun findByCampaignType(campaignType: CampaignType, pageable: Pageable): Page<Campaign>

    /**
     * Find campaigns by status and type
     */
    fun findByStatusAndCampaignType(status: CampaignStatus, campaignType: CampaignType): List<Campaign>

    /**
     * Find active campaigns
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.status = 'ACTIVE'
        AND c.startDate <= :now
        AND c.endDate > :now
    """)
    fun findActiveCampaigns(@Param("now") now: LocalDateTime): List<Campaign>

    /**
     * Find active campaigns with pagination
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.status = 'ACTIVE'
        AND c.startDate <= :now
        AND c.endDate > :now
    """)
    fun findActiveCampaigns(@Param("now") now: LocalDateTime, pageable: Pageable): Page<Campaign>

    /**
     * Find expired campaigns
     */
    @Query("SELECT c FROM Campaign c WHERE c.endDate < :now")
    fun findExpiredCampaigns(@Param("now") now: LocalDateTime): List<Campaign>

    /**
     * Find campaigns expiring soon
     */
    @Query("SELECT c FROM Campaign c WHERE c.endDate BETWEEN :now AND :expirationThreshold")
    fun findCampaignsExpiringSoon(
        @Param("now") now: LocalDateTime,
        @Param("expirationThreshold") expirationThreshold: LocalDateTime
    ): List<Campaign>

    /**
     * Find campaigns starting soon
     */
    @Query("SELECT c FROM Campaign c WHERE c.startDate BETWEEN :now AND :startThreshold")
    fun findCampaignsStartingSoon(
        @Param("now") now: LocalDateTime,
        @Param("startThreshold") startThreshold: LocalDateTime
    ): List<Campaign>

    /**
     * Find campaigns by date range
     */
    @Query("SELECT c FROM Campaign c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Campaign>

    /**
     * Find campaigns by creator
     */
    fun findByCreatedBy(createdBy: String): List<Campaign>

    /**
     * Find campaigns by creator with pagination
     */
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<Campaign>

    /**
     * Find campaigns applicable to station
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.status = 'ACTIVE'
        AND c.startDate <= :now
        AND c.endDate > :now
        AND (c.applicableStations IS NULL OR c.applicableStations LIKE CONCAT('%', :stationId, '%'))
    """)
    fun findCampaignsForStation(
        @Param("stationId") stationId: Long,
        @Param("now") now: LocalDateTime
    ): List<Campaign>

    /**
     * Find campaigns with budget remaining
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.budget IS NOT NULL
        AND c.spentAmount < c.budget
    """)
    fun findCampaignsWithBudgetRemaining(): List<Campaign>

    /**
     * Find campaigns over budget
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.budget IS NOT NULL
        AND c.spentAmount >= c.budget
    """)
    fun findCampaignsOverBudget(): List<Campaign>

    /**
     * Find campaigns with low budget remaining
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.budget IS NOT NULL
        AND c.spentAmount >= (c.budget * :threshold / 100)
    """)
    fun findCampaignsWithLowBudget(@Param("threshold") threshold: Double): List<Campaign>

    /**
     * Find campaigns that can generate more coupons
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.maxCoupons IS NULL
        OR c.generatedCoupons < c.maxCoupons
    """)
    fun findCampaignsCanGenerateMoreCoupons(): List<Campaign>

    /**
     * Find campaigns at coupon limit
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.maxCoupons IS NOT NULL
        AND c.generatedCoupons >= c.maxCoupons
    """)
    fun findCampaignsAtCouponLimit(): List<Campaign>

    /**
     * Search campaigns by name or description
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun searchByNameOrDescription(@Param("searchTerm") searchTerm: String): List<Campaign>

    /**
     * Search campaigns by name or description with pagination
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun searchByNameOrDescription(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Campaign>

    /**
     * Count campaigns by status
     */
    fun countByStatus(status: CampaignStatus): Long

    /**
     * Count campaigns by type
     */
    fun countByCampaignType(campaignType: CampaignType): Long

    /**
     * Count active campaigns
     */
    @Query("""
        SELECT COUNT(c) FROM Campaign c
        WHERE c.status = 'ACTIVE'
        AND c.startDate <= :now
        AND c.endDate > :now
    """)
    fun countActiveCampaigns(@Param("now") now: LocalDateTime): Long

    /**
     * Update campaign status
     */
    @Modifying
    @Query("UPDATE Campaign c SET c.status = :status, c.updatedBy = :updatedBy WHERE c.id = :campaignId")
    fun updateCampaignStatus(
        @Param("campaignId") campaignId: Long,
        @Param("status") status: CampaignStatus,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update campaign coupon statistics
     */
    @Modifying
    @Query("""
        UPDATE Campaign c SET
        c.generatedCoupons = :generatedCount,
        c.usedCoupons = :usedCount
        WHERE c.id = :campaignId
    """)
    fun updateCampaignCouponStats(
        @Param("campaignId") campaignId: Long,
        @Param("generatedCount") generatedCount: Int,
        @Param("usedCount") usedCount: Int
    ): Int

    /**
     * Update campaign spent amount
     */
    @Modifying
    @Query("UPDATE Campaign c SET c.spentAmount = c.spentAmount + :amount WHERE c.id = :campaignId")
    fun updateCampaignSpentAmount(
        @Param("campaignId") campaignId: Long,
        @Param("amount") amount: BigDecimal
    ): Int

    /**
     * Update expired campaigns status
     */
    @Modifying
    @Query("UPDATE Campaign c SET c.status = 'EXPIRED' WHERE c.endDate < :now AND c.status IN ('ACTIVE', 'PAUSED')")
    fun updateExpiredCampaignsStatus(@Param("now") now: LocalDateTime): Int

    /**
     * Get campaign statistics
     */
    @Query("""
        SELECT
            COUNT(c) as totalCampaigns,
            COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as activeCampaigns,
            COUNT(CASE WHEN c.status = 'DRAFT' THEN 1 END) as draftCampaigns,
            COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedCampaigns,
            COUNT(CASE WHEN c.status = 'CANCELLED' THEN 1 END) as cancelledCampaigns,
            SUM(c.budget) as totalBudget,
            SUM(c.spentAmount) as totalSpent,
            SUM(c.generatedCoupons) as totalCouponsGenerated,
            SUM(c.usedCoupons) as totalCouponsUsed,
            AVG(c.generatedCoupons) as averageCouponsPerCampaign
        FROM Campaign c
    """)
    fun getCampaignStatistics(): Map<String, Any>

    /**
     * Get campaign performance metrics
     */
    @Query("""
        SELECT c,
            (CASE WHEN c.generatedCoupons > 0 THEN (c.usedCoupons * 100.0 / c.generatedCoupons) ELSE 0 END) as usageRate,
            (CASE WHEN c.budget IS NOT NULL AND c.budget > 0 THEN (c.spentAmount * 100.0 / c.budget) ELSE 0 END) as budgetUtilization
        FROM Campaign c
        WHERE c.status IN ('ACTIVE', 'COMPLETED')
    """)
    fun getCampaignPerformanceMetrics(): List<Array<Any>>

    /**
     * Find campaigns without coupons
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE NOT EXISTS (
            SELECT 1 FROM Coupon cp WHERE cp.campaign = c
        )
    """)
    fun findCampaignsWithoutCoupons(): List<Campaign>

    /**
     * Find duplicate campaign names
     */
    @Query("""
        SELECT c FROM Campaign c
        WHERE EXISTS (
            SELECT c2 FROM Campaign c2
            WHERE c2.id != c.id
            AND LOWER(c2.name) = LOWER(c.name)
        )
    """)
    fun findDuplicateCampaignNames(): List<Campaign>
}