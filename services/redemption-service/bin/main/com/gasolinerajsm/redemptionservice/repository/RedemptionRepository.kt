package com.gasolinerajsm.redemptionservice.repository

import com.gasolinerajsm.redemptionservice.model.Redemption
import com.gasolinerajsm.redemptionservice.model.RedemptionStatus
import com.gasolinerajsm.redemptionservice.model.RedemptionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository interface for Redemption entity operations
 */
@Repository
interface RedemptionRepository : JpaRepository<Redemption, Long> {

    /**
     * Find redemption by reference number
     */
    fun findByReferenceNumber(referenceNumber: String): Redemption?

    /**
     * Find all redemptions by user ID
     */
    fun findByUserIdOrderByRedeemedAtDesc(userId: Long): List<Redemption>

    /**
     * Find redemptions by user ID with pagination
     */
    fun findByUserIdOrderByRedeemedAtDesc(userId: Long, pageable: Pageable): Page<Redemption>

    /**
     * Find redemptions by coupon ID
     */
    fun findByCouponId(couponId: Long): List<Redemption>

    /**
     * Find redemptions by station ID
     */
    fun findByStationIdOrderByRedeemedAtDesc(stationId: Long): List<Redemption>

    /**
     * Find redemptions by station ID with pagination
     */
    fun findByStationIdOrderByRedeemedAtDesc(stationId: Long, pageable: Pageable): Page<Redemption>

    /**
     * Find redemptions by status
     */
    fun findByStatusOrderByRedeemedAtDesc(status: RedemptionStatus): List<Redemption>

    /**
     * Find redemptions by status with pagination
     */
    fun findByStatusOrderByRedeemedAtDesc(status: RedemptionStatus, pageable: Pageable): Page<Redemption>

    /**
     * Find redemptions by redemption type
     */
    fun findByRedemptionTypeOrderByRedeemedAtDesc(redemptionType: RedemptionType): List<Redemption>

    /**
     * Find redemptions by employee ID
     */
    fun findByEmployeeIdOrderByRedeemedAtDesc(employeeId: Long): List<Redemption>

    /**
     * Find redemptions by date range
     */
    fun findByRedeemedAtBetweenOrderByRedeemedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Redemption>

    /**
     * Find redemptions by date range with pagination
     */
    fun findByRedeemedAtBetweenOrderByRedeemedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Redemption>

    /**
     * Find redemptions flagged for review
     */
    fun findByFlaggedForReviewTrueAndReviewedAtIsNullOrderByRedeemedAtDesc(): List<Redemption>

    /**
     * Find redemptions flagged for review with pagination
     */
    fun findByFlaggedForReviewTrueAndReviewedAtIsNullOrderByRedeemedAtDesc(pageable: Pageable): Page<Redemption>

    /**
     * Find high-risk redemptions
     */
    @Query("SELECT r FROM Redemption r WHERE r.fraudScore >= :minScore OR r.riskLevel = 'HIGH' ORDER BY r.redeemedAt DESC")
    fun findHighRiskRedemptions(@Param("minScore") minScore: BigDecimal): List<Redemption>

    /**
     * Find redemptions by user and date range
     */
    fun findByUserIdAndRedeemedAtBetweenOrderByRedeemedAtDesc(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Redemption>

    /**
     * Find redemptions by station and date range
     */
    fun findByStationIdAndRedeemedAtBetweenOrderByRedeemedAtDesc(
        stationId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Redemption>

    /**
     * Find redemptions by multiple statuses
     */
    fun findByStatusInOrderByRedeemedAtDesc(statuses: List<RedemptionStatus>): List<Redemption>

    /**
     * Find expired redemptions
     */
    @Query("SELECT r FROM Redemption r WHERE r.expiresAt IS NOT NULL AND r.expiresAt < :currentTime AND r.status = 'PENDING'")
    fun findExpiredRedemptions(@Param("currentTime") currentTime: LocalDateTime): List<Redemption>

    /**
     * Count redemptions by user ID
     */
    fun countByUserId(userId: Long): Long

    /**
     * Count redemptions by station ID
     */
    fun countByStationId(stationId: Long): Long

    /**
     * Count redemptions by status
     */
    fun countByStatus(status: RedemptionStatus): Long

    /**
     * Count redemptions by date range
     */
    fun countByRedeemedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    /**
     * Sum total amount by user ID
     */
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Redemption r WHERE r.userId = :userId AND r.status = 'COMPLETED'")
    fun sumTotalAmountByUserId(@Param("userId") userId: Long): BigDecimal

    /**
     * Sum total amount by station ID
     */
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Redemption r WHERE r.stationId = :stationId AND r.status = 'COMPLETED'")
    fun sumTotalAmountByStationId(@Param("stationId") stationId: Long): BigDecimal

    /**
     * Sum total amount by date range
     */
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Redemption r WHERE r.redeemedAt BETWEEN :startDate AND :endDate AND r.status = 'COMPLETED'")
    fun sumTotalAmountByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    /**
     * Find redemptions with location data
     */
    @Query("SELECT r FROM Redemption r WHERE r.latitude IS NOT NULL AND r.longitude IS NOT NULL ORDER BY r.redeemedAt DESC")
    fun findRedemptionsWithLocation(): List<Redemption>

    /**
     * Find redemptions by transaction ID
     */
    fun findByTransactionId(transactionId: String): Redemption?

    /**
     * Find redemptions by validation code
     */
    fun findByValidationCode(validationCode: String): Redemption?

    /**
     * Find redemptions by campaign ID
     */
    fun findByCampaignIdOrderByRedeemedAtDesc(campaignId: Long): List<Redemption>

    /**
     * Find redemptions by promotion code
     */
    fun findByPromotionCodeOrderByRedeemedAtDesc(promotionCode: String): List<Redemption>

    /**
     * Check if user has redeemed coupon
     */
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    /**
     * Find recent redemptions by user (last 30 days)
     */
    @Query("SELECT r FROM Redemption r WHERE r.userId = :userId AND r.redeemedAt >= :thirtyDaysAgo ORDER BY r.redeemedAt DESC")
    fun findRecentRedemptionsByUser(
        @Param("userId") userId: Long,
        @Param("thirtyDaysAgo") thirtyDaysAgo: LocalDateTime
    ): List<Redemption>

    /**
     * Find redemptions requiring review
     */
    @Query("""
        SELECT r FROM Redemption r
        WHERE (r.flaggedForReview = true AND r.reviewedAt IS NULL)
        OR (r.fraudScore >= :highRiskScore)
        OR (r.riskLevel = 'HIGH')
        ORDER BY r.redeemedAt DESC
    """)
    fun findRedemptionsRequiringReview(@Param("highRiskScore") highRiskScore: BigDecimal): List<Redemption>

    /**
     * Find top redemption amounts by date range
     */
    @Query("""
        SELECT r FROM Redemption r
        WHERE r.redeemedAt BETWEEN :startDate AND :endDate
        AND r.status = 'COMPLETED'
        ORDER BY r.totalAmount DESC
    """)
    fun findTopRedemptionsByAmount(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Redemption>

    /**
     * Find redemptions by device ID
     */
    fun findByDeviceIdOrderByRedeemedAtDesc(deviceId: String): List<Redemption>

    /**
     * Find redemptions by IP address
     */
    fun findByIpAddressOrderByRedeemedAtDesc(ipAddress: String): List<Redemption>

    /**
     * Get redemption statistics by station and date range
     */
    @Query("""
        SELECT
            COUNT(r) as totalCount,
            COALESCE(SUM(r.totalAmount), 0) as totalAmount,
            COALESCE(AVG(r.totalAmount), 0) as averageAmount
        FROM Redemption r
        WHERE r.stationId = :stationId
        AND r.redeemedAt BETWEEN :startDate AND :endDate
        AND r.status = 'COMPLETED'
    """)
    fun getRedemptionStatsByStation(
        @Param("stationId") stationId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Map<String, Any>

    /**
     * Find duplicate redemptions by user and time window
     */
    @Query("""
        SELECT r FROM Redemption r
        WHERE r.userId = :userId
        AND r.redeemedAt BETWEEN :startTime AND :endTime
        AND r.id != :excludeId
        ORDER BY r.redeemedAt DESC
    """)
    fun findPotentialDuplicateRedemptions(
        @Param("userId") userId: Long,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        @Param("excludeId") excludeId: Long
    ): List<Redemption>
}