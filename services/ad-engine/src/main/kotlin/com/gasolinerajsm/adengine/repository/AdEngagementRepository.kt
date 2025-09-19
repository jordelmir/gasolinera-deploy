package com.gasolinerajsm.adengine.repository

import com.gasolinerajsm.adengine.model.AdEngagement
import com.gasolinerajsm.adengine.domain.model.EngagementStatus
import com.gasolinerajsm.adengine.domain.model.EngagementType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository interface for AdEngagement entity operations
 */
@Repository
interface AdEngagementRepository : JpaRepository<AdEngagement, Long> {

    /**
     * Find engagements by user ID
     */
    fun findByUserId(userId: Long, pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by advertisement ID
     */
    fun findByAdvertisementId(advertisementId: Long, pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by user and advertisement
     */
    fun findByUserIdAndAdvertisementId(userId: Long, advertisementId: Long, pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by status
     */
    fun findByStatus(status: EngagementStatus, pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by type
     */
    fun findByEngagementType(engagementType: EngagementType, pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by session ID
     */
    fun findBySessionId(sessionId: String): List<AdEngagement>

    /**
     * Find engagements by station ID
     */
    fun findByStationId(stationId: Long, pageable: Pageable): Page<AdEngagement>

    /**
     * Find completed engagements
     */
    fun findByStatusAndCompletedAtIsNotNull(status: EngagementStatus): List<AdEngagement>

    /**
     * Find engagements with tickets awarded
     */
    fun findByTicketsAwardedTrue(pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements pending ticket award
     */
    @Query("""
        SELECT e FROM AdEngagement e
        WHERE e.status = 'COMPLETED'
        AND e.totalTicketsEarned > 0
        AND e.ticketsAwarded = false
    """)
    fun findEngagementsPendingTicketAward(): List<AdEngagement>

    /**
     * Find engagements with errors
     */
    fun findByErrorOccurredTrue(pageable: Pageable): Page<AdEngagement>

    /**
     * Count engagements by user
     */
    fun countByUserId(userId: Long): Long

    /**
     * Count engagements by user and advertisement
     */
    fun countByUserIdAndAdvertisementId(userId: Long, advertisementId: Long): Long

    /**
     * Count engagements by advertisement
     */
    fun countByAdvertisementId(advertisementId: Long): Long

    /**
     * Count engagements by status
     */
    fun countByStatus(status: EngagementStatus): Long

    /**
     * Count completed engagements by user
     */
    fun countByUserIdAndStatus(userId: Long, status: EngagementStatus): Long

    /**
     * Count user impressions for advertisement today
     */
    @Query("""
        SELECT COUNT(e) FROM AdEngagement e
        WHERE e.userId = :userId
        AND e.advertisement.id = :advertisementId
        AND e.engagementType = 'IMPRESSION'
        AND e.createdAt >= :startOfDay
    """)
    fun countUserImpressionsToday(
        @Param("userId") userId: Long,
        @Param("advertisementId") advertisementId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime
    ): Long

    /**
     * Count daily impressions for advertisement
     */
    @Query("""
        SELECT COUNT(e) FROM AdEngagement e
        WHERE e.advertisement.id = :advertisementId
        AND e.engagementType = 'IMPRESSION'
        AND e.createdAt >= :startOfDay
    """)
    fun countDailyImpressions(
        @Param("advertisementId") advertisementId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime
    ): Long

    /**
     * Find engagements in date range
     */
    fun findByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<AdEngagement>

    /**
     * Find engagements by user in date range
     */
    fun findByUserIdAndCreatedAtBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<AdEngagement>

    /**
     * Get engagement statistics by advertisement
     */
    @Query("""
        SELECT
            COUNT(e) as totalEngagements,
            COUNT(CASE WHEN e.engagementType = 'IMPRESSION' THEN 1 END) as impressions,
            COUNT(CASE WHEN e.engagementType = 'CLICK' THEN 1 END) as clicks,
            COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) as completions,
            COUNT(CASE WHEN e.ticketsAwarded = true THEN 1 END) as ticketAwards,
            SUM(e.totalTicketsEarned) as totalTicketsEarned,
            SUM(e.costCharged) as totalCost,
            AVG(e.viewDurationSeconds) as avgViewDuration,
            AVG(e.completionPercentage) as avgCompletionPercentage
        FROM AdEngagement e
        WHERE e.advertisement.id = :advertisementId
    """)
    fun getEngagementStatisticsByAdvertisement(@Param("advertisementId") advertisementId: Long): Map<String, Any>

    /**
     * Get user engagement statistics
     */
    @Query("""
        SELECT
            COUNT(e) as totalEngagements,
            COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) as completedEngagements,
            COUNT(CASE WHEN e.clicked = true THEN 1 END) as clickedEngagements,
            SUM(e.totalTicketsEarned) as totalTicketsEarned,
            COUNT(DISTINCT e.advertisement.id) as uniqueAdsEngaged,
            AVG(e.viewDurationSeconds) as avgViewDuration
        FROM AdEngagement e
        WHERE e.userId = :userId
    """)
    fun getUserEngagementStatistics(@Param("userId") userId: Long): Map<String, Any>

    /**
     * Get daily engagement statistics
     */
    @Query("""
        SELECT
            COUNT(e) as totalEngagements,
            COUNT(CASE WHEN e.engagementType = 'IMPRESSION' THEN 1 END) as impressions,
            COUNT(CASE WHEN e.engagementType = 'CLICK' THEN 1 END) as clicks,
            COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) as completions,
            COUNT(DISTINCT e.userId) as uniqueUsers,
            SUM(e.totalTicketsEarned) as totalTicketsEarned,
            SUM(e.costCharged) as totalCost
        FROM AdEngagement e
        WHERE e.createdAt >= :startOfDay
    """)
    fun getDailyEngagementStatistics(@Param("startOfDay") startOfDay: LocalDateTime): Map<String, Any>

    /**
     * Find recent engagements by user
     */
    @Query("""
        SELECT e FROM AdEngagement e
        WHERE e.userId = :userId
        ORDER BY e.createdAt DESC
    """)
    fun findRecentEngagementsByUser(@Param("userId") userId: Long, pageable: Pageable): Page<AdEngagement>

    /**
     * Find high-performing engagements
     */
    @Query("""
        SELECT e FROM AdEngagement e
        WHERE e.status = 'COMPLETED'
        AND e.completionPercentage >= :minCompletionPercentage
        AND e.viewDurationSeconds >= :minViewDuration
        ORDER BY e.completionPercentage DESC, e.viewDurationSeconds DESC
    """)
    fun findHighPerformingEngagements(
        @Param("minCompletionPercentage") minCompletionPercentage: BigDecimal,
        @Param("minViewDuration") minViewDuration: Int
    ): List<AdEngagement>

    /**
     * Find engagements with location data
     */
    fun findByLocationLatitudeIsNotNullAndLocationLongitudeIsNotNull(pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by device type
     */
    fun findByDeviceType(deviceType: String, pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by multiple statuses
     */
    fun findByStatusIn(statuses: List<EngagementStatus>, pageable: Pageable): Page<AdEngagement>

    /**
     * Update engagement status
     */
    @Query("UPDATE AdEngagement e SET e.status = :status WHERE e.id = :id")
    fun updateStatus(@Param("id") id: Long, @Param("status") status: EngagementStatus): Int

    /**
     * Mark tickets as awarded
     */
    @Query("""
        UPDATE AdEngagement e
        SET e.ticketsAwarded = true, e.ticketsAwardedAt = :awardedAt, e.raffleEntryId = :raffleEntryId
        WHERE e.id = :id
    """)
    fun markTicketsAwarded(
        @Param("id") id: Long,
        @Param("awardedAt") awardedAt: LocalDateTime,
        @Param("raffleEntryId") raffleEntryId: Long?
    ): Int

    /**
     * Update engagement completion
     */
    @Query("""
        UPDATE AdEngagement e
        SET e.status = 'COMPLETED', e.completedAt = :completedAt,
            e.completionPercentage = :completionPercentage, e.viewDurationSeconds = :viewDuration
        WHERE e.id = :id
    """)
    fun markAsCompleted(
        @Param("id") id: Long,
        @Param("completedAt") completedAt: LocalDateTime,
        @Param("completionPercentage") completionPercentage: BigDecimal?,
        @Param("viewDuration") viewDuration: Int?
    ): Int

    /**
     * Update click information
     */
    @Query("""
        UPDATE AdEngagement e
        SET e.clicked = true, e.clickedAt = :clickedAt, e.clickThroughUrl = :clickThroughUrl
        WHERE e.id = :id
    """)
    fun markAsClicked(
        @Param("id") id: Long,
        @Param("clickedAt") clickedAt: LocalDateTime,
        @Param("clickThroughUrl") clickThroughUrl: String?
    ): Int

    /**
     * Find engagements by campaign context
     */
    fun findByCampaignContext(campaignContext: String, pageable: Pageable): Page<AdEngagement>

    /**
     * Find engagements by placement context
     */
    fun findByPlacementContext(placementContext: String, pageable: Pageable): Page<AdEngagement>

    /**
     * Get engagement conversion funnel
     */
    @Query("""
        SELECT
            COUNT(CASE WHEN e.engagementType = 'IMPRESSION' THEN 1 END) as impressions,
            COUNT(CASE WHEN e.engagementType = 'VIEW' THEN 1 END) as views,
            COUNT(CASE WHEN e.clicked = true THEN 1 END) as clicks,
            COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) as completions
        FROM AdEngagement e
        WHERE e.advertisement.id = :advertisementId
        AND e.createdAt BETWEEN :startDate AND :endDate
    """)
    fun getEngagementFunnel(
        @Param("advertisementId") advertisementId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Map<String, Any>

    /**
     * Find abandoned engagements
     */
    @Query("""
        SELECT e FROM AdEngagement e
        WHERE e.status = 'STARTED'
        AND e.createdAt < :cutoffTime
    """)
    fun findAbandonedEngagements(@Param("cutoffTime") cutoffTime: LocalDateTime): List<AdEngagement>

    /**
     * Get hourly engagement distribution
     */
    @Query("""
        SELECT EXTRACT(HOUR FROM e.createdAt) as hour, COUNT(e) as count
        FROM AdEngagement e
        WHERE e.createdAt >= :startDate
        GROUP BY EXTRACT(HOUR FROM e.createdAt)
        ORDER BY hour
    """)
    fun getHourlyEngagementDistribution(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
}