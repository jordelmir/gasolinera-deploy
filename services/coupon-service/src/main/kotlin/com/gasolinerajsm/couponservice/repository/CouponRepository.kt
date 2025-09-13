package com.gasolinerajsm.couponservice.repository

import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CouponStatus
import com.gasolinerajsm.couponservice.model.Campaign
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
interface CouponRepository : JpaRepository<Coupon, Long> {

    /**
     * Find coupon by QR code
     */
    fun findByQrCode(qrCode: String): Coupon?

    /**
     * Find coupon by coupon code
     */
    fun findByCouponCode(couponCode: String): Coupon?

    /**
     * Check if QR code exists
     */
    fun existsByQrCode(qrCode: String): Boolean

    /**
     * Check if coupon code exists
     */
    fun existsByCouponCode(couponCode: String): Boolean

    /**
     * Find coupons by campaign
     */
    fun findByCampaign(campaign: Campaign): List<Coupon>

    /**
     * Find coupons by campaign with pagination
     */
    fun findByCampaign(campaign: Campaign, pageable: Pageable): Page<Coupon>

    /**
     * Find coupons by campaign ID
     */
    fun findByCampaignId(campaignId: Long): List<Coupon>

    /**
     * Find coupons by status
     */
    fun findByStatus(status: CouponStatus): List<Coupon>

    /**
     * Find coupons by status with pagination
     */
    fun findByStatus(status: CouponStatus, pageable: Pageable): Page<Coupon>

    /**
     * Find coupons by campaign and status
     */
    fun findByCampaignAndStatus(campaign: Campaign, status: CouponStatus): List<Coupon>

    /**
     * Find valid coupons (active and within date range)
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.status = 'ACTIVE'
        AND c.validFrom <= :now
        AND c.validUntil > :now
        AND (c.maxUses IS NULL OR c.currentUses < c.maxUses)
    """)
    fun findValidCoupons(@Param("now") now: LocalDateTime): List<Coupon>

    /**
     * Find valid coupons by campaign
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.campaign = :campaign
        AND c.status = 'ACTIVE'
        AND c.validFrom <= :now
        AND c.validUntil > :now
        AND (c.maxUses IS NULL OR c.currentUses < c.maxUses)
    """)
    fun findValidCouponsByCampaign(
        @Param("campaign") campaign: Campaign,
        @Param("now") now: LocalDateTime
    ): List<Coupon>

    /**
     * Find expired coupons
     */
    @Query("SELECT c FROM Coupon c WHERE c.validUntil < :now")
    fun findExpiredCoupons(@Param("now") now: LocalDateTime): List<Coupon>

    /**
     * Find coupons expiring soon
     */
    @Query("SELECT c FROM Coupon c WHERE c.validUntil BETWEEN :now AND :expirationThreshold")
    fun findCouponsExpiringSoon(
        @Param("now") now: LocalDateTime,
        @Param("expirationThreshold") expirationThreshold: LocalDateTime
    ): List<Coupon>

    /**
     * Find coupons by date range
     */
    @Query("SELECT c FROM Coupon c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Coupon>

    /**
     * Find coupons valid for specific station
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.status = 'ACTIVE'
        AND c.validFrom <= :now
        AND c.validUntil > :now
        AND (c.applicableStations IS NULL OR c.applicableStations LIKE CONCAT('%', :stationId, '%'))
    """)
    fun findValidCouponsForStation(
        @Param("stationId") stationId: Long,
        @Param("now") now: LocalDateTime
    ): List<Coupon>

    /**
     * Find coupons valid for specific fuel type
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.status = 'ACTIVE'
        AND c.validFrom <= :now
        AND c.validUntil > :now
        AND (c.applicableFuelTypes IS NULL OR c.applicableFuelTypes LIKE CONCAT('%', :fuelType, '%'))
    """)
    fun findValidCouponsForFuelType(
        @Param("fuelType") fuelType: String,
        @Param("now") now: LocalDateTime
    ): List<Coupon>

    /**
     * Find coupons with minimum purchase requirement
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.minimumPurchaseAmount IS NOT NULL
        AND c.minimumPurchaseAmount <= :purchaseAmount
    """)
    fun findCouponsWithMinimumPurchase(@Param("purchaseAmount") purchaseAmount: BigDecimal): List<Coupon>

    /**
     * Count coupons by campaign
     */
    fun countByCampaign(campaign: Campaign): Long

    /**
     * Count coupons by campaign and status
     */
    fun countByCampaignAndStatus(campaign: Campaign, status: CouponStatus): Long

    /**
     * Count used coupons by campaign
     */
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.campaign = :campaign AND c.currentUses > 0")
    fun countUsedCouponsByCampaign(@Param("campaign") campaign: Campaign): Long

    /**
     * Update coupon status
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.status = :status WHERE c.id = :couponId")
    fun updateCouponStatus(@Param("couponId") couponId: Long, @Param("status") status: CouponStatus): Int

    /**
     * Increment coupon usage
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.currentUses = c.currentUses + 1 WHERE c.id = :couponId")
    fun incrementCouponUsage(@Param("couponId") couponId: Long): Int

    /**
     * Update expired coupons status
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.status = 'EXPIRED' WHERE c.validUntil < :now AND c.status = 'ACTIVE'")
    fun updateExpiredCouponsStatus(@Param("now") now: LocalDateTime): Int

    /**
     * Update used up coupons status
     */
    @Modifying
    @Query("""
        UPDATE Coupon c SET c.status = 'USED_UP'
        WHERE c.maxUses IS NOT NULL
        AND c.currentUses >= c.maxUses
        AND c.status = 'ACTIVE'
    """)
    fun updateUsedUpCouponsStatus(): Int

    /**
     * Get coupon statistics by campaign
     */
    @Query("""
        SELECT
            COUNT(c) as totalCoupons,
            COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as activeCoupons,
            COUNT(CASE WHEN c.status = 'EXPIRED' THEN 1 END) as expiredCoupons,
            COUNT(CASE WHEN c.status = 'USED_UP' THEN 1 END) as usedUpCoupons,
            COUNT(CASE WHEN c.currentUses > 0 THEN 1 END) as usedCoupons,
            SUM(c.currentUses) as totalUses,
            AVG(c.currentUses) as averageUses
        FROM Coupon c
        WHERE c.campaign = :campaign
    """)
    fun getCouponStatisticsByCampaign(@Param("campaign") campaign: Campaign): Map<String, Any>

    /**
     * Get overall coupon statistics
     */
    @Query("""
        SELECT
            COUNT(c) as totalCoupons,
            COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as activeCoupons,
            COUNT(CASE WHEN c.status = 'EXPIRED' THEN 1 END) as expiredCoupons,
            COUNT(CASE WHEN c.status = 'USED_UP' THEN 1 END) as usedUpCoupons,
            COUNT(CASE WHEN c.currentUses > 0 THEN 1 END) as usedCoupons,
            SUM(c.currentUses) as totalUses,
            COUNT(DISTINCT c.campaign.id) as totalCampaigns
        FROM Coupon c
    """)
    fun getOverallCouponStatistics(): Map<String, Any>

    /**
     * Find duplicate QR codes
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE EXISTS (
            SELECT c2 FROM Coupon c2
            WHERE c2.id != c.id
            AND c2.qrCode = c.qrCode
        )
    """)
    fun findDuplicateQrCodes(): List<Coupon>

    /**
     * Find coupons without usage
     */
    @Query("SELECT c FROM Coupon c WHERE c.currentUses = 0 AND c.createdAt < :cutoffDate")
    fun findUnusedCoupons(@Param("cutoffDate") cutoffDate: LocalDateTime): List<Coupon>

    /**
     * Find high-usage coupons
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.maxUses IS NOT NULL
        AND c.currentUses >= (c.maxUses * :usageThreshold / 100)
    """)
    fun findHighUsageCoupons(@Param("usageThreshold") usageThreshold: Double): List<Coupon>

    /**
     * Find coupons by station ID
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.applicableStations IS NULL
        OR c.applicableStations LIKE CONCAT('%', :stationId, '%')
    """)
    fun findByStationId(@Param("stationId") stationId: Long): List<Coupon>

    /**
     * Find coupons by employee ID (coupons created or managed by employee)
     */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.createdBy = :employeeId
        OR c.updatedBy = :employeeId
    """)
    fun findByEmployeeId(@Param("employeeId") employeeId: String): List<Coupon>
}