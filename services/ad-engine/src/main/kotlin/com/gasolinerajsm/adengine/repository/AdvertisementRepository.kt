package com.gasolinerajsm.adengine.repository

import com.gasolinerajsm.adengine.model.Advertisement
import com.gasolinerajsm.adengine.model.AdStatus
import com.gasolinerajsm.adengine.model.AdType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository interface for Advertisement entity operations
 */
@Repository
interface AdvertisementRepository : JpaRepository<Advertisement, Long> {

    /**
     * Find advertisements by status
     */
    fun findByStatus(status: AdStatus, pageable: Pageable): Page<Advertisement>

    /**
     * Find advertisements by campaign ID
     */
    fun findByCampaignId(campaignId: Long, pageable: Pageable): Page<Advertisement>

    /**
     * Find advertisements by type
     */
    fun findByAdType(adType: AdType, pageable: Pageable): Page<Advertisement>

    /**
     * Find advertisements by status and type
     */
    fun findByStatusAndAdType(status: AdStatus, adType: AdType, pageable: Pageable): Page<Advertisement>

    /**
     * Find active advertisements within date range
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND a.startDate <= :now
        AND a.endDate > :now
        ORDER BY a.priority DESC, a.createdAt ASC
    """)
    fun findActiveAdvertisements(@Param("now") now: LocalDateTime = LocalDateTime.now()): List<Advertisement>

    /**
     * Find advertisements eligible for serving to a user
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND a.startDate <= :now
        AND a.endDate > :now
        AND (a.totalBudget IS NULL OR a.totalSpend < a.totalBudget)
        AND (a.maxDailyImpressions IS NULL OR a.totalImpressions < a.maxDailyImpressions)
        ORDER BY a.priority DESC, a.createdAt ASC
    """)
    fun findEligibleAdvertisements(@Param("now") now: LocalDateTime = LocalDateTime.now()): List<Advertisement>

    /**
     * Find advertisements by priority range
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.priority BETWEEN :minPriority AND :maxPriority
        AND a.status = :status
        ORDER BY a.priority DESC
    """)
    fun findByPriorityRange(
        @Param("minPriority") minPriority: Int,
        @Param("maxPriority") maxPriority: Int,
        @Param("status") status: AdStatus = AdStatus.ACTIVE
    ): List<Advertisement>

    /**
     * Find advertisements with ticket bonuses
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND (a.ticketMultiplier IS NOT NULL OR a.bonusTicketsOnCompletion > 0)
        ORDER BY a.priority DESC
    """)
    fun findAdvertisementsWithTicketBonuses(): List<Advertisement>

    /**
     * Find advertisements by advertiser
     */
    fun findByAdvertiserNameContainingIgnoreCase(advertiserName: String, pageable: Pageable): Page<Advertisement>

    /**
     * Find advertisements by title containing
     */
    fun findByTitleContainingIgnoreCase(title: String, pageable: Pageable): Page<Advertisement>

    /**
     * Find advertisements expiring soon
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND a.endDate BETWEEN :now AND :deadline
        ORDER BY a.endDate ASC
    """)
    fun findAdvertisementsExpiringSoon(
        @Param("now") now: LocalDateTime = LocalDateTime.now(),
        @Param("deadline") deadline: LocalDateTime
    ): List<Advertisement>

    /**
     * Find advertisements with low budget remaining
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND a.totalBudget IS NOT NULL
        AND (a.totalBudget - a.totalSpend) < :threshold
        ORDER BY (a.totalBudget - a.totalSpend) ASC
    """)
    fun findAdvertisementsWithLowBudget(@Param("threshold") threshold: BigDecimal): List<Advertisement>

    /**
     * Find advertisements by location targeting
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND (a.targetLocations IS NULL OR a.targetLocations LIKE %:location%)
        ORDER BY a.priority DESC
    """)
    fun findByLocationTargeting(@Param("location") location: String): List<Advertisement>

    /**
     * Find advertisements by station targeting
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND (a.targetStations IS NULL OR a.targetStations LIKE %:stationId%)
        ORDER BY a.priority DESC
    """)
    fun findByStationTargeting(@Param("stationId") stationId: String): List<Advertisement>

    /**
     * Count advertisements by status
     */
    fun countByStatus(status: AdStatus): Long

    /**
     * Count advertisements by campaign
     */
    fun countByCampaignId(campaignId: Long): Long

    /**
     * Count active advertisements
     */
    @Query("""
        SELECT COUNT(a) FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND a.startDate <= :now
        AND a.endDate > :now
    """)
    fun countActiveAdvertisements(@Param("now") now: LocalDateTime = LocalDateTime.now()): Long

    /**
     * Get advertisement statistics
     */
    @Query("""
        SELECT
            COUNT(a) as totalAds,
            COUNT(CASE WHEN a.status = 'ACTIVE' THEN 1 END) as activeAds,
            COUNT(CASE WHEN a.status = 'PAUSED' THEN 1 END) as pausedAds,
            COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) as completedAds,
            SUM(a.totalImpressions) as totalImpressions,
            SUM(a.totalClicks) as totalClicks,
            SUM(a.totalCompletions) as totalCompletions,
            SUM(a.totalSpend) as totalSpend
        FROM Advertisement a
    """)
    fun getAdvertisementStatistics(): Map<String, Any>

    /**
     * Get campaign statistics
     */
    @Query("""
        SELECT
            COUNT(a) as totalAds,
            SUM(a.totalImpressions) as totalImpressions,
            SUM(a.totalClicks) as totalClicks,
            SUM(a.totalCompletions) as totalCompletions,
            SUM(a.totalSpend) as totalSpend,
            AVG(a.priority) as avgPriority
        FROM Advertisement a
        WHERE a.campaignId = :campaignId
    """)
    fun getCampaignStatistics(@Param("campaignId") campaignId: Long): Map<String, Any>

    /**
     * Find advertisements created in date range
     */
    fun findByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Advertisement>

    /**
     * Find advertisements by multiple statuses
     */
    fun findByStatusIn(statuses: List<AdStatus>, pageable: Pageable): Page<Advertisement>

    /**
     * Find advertisements by tags
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.tags IS NOT NULL
        AND a.tags LIKE %:tag%
    """)
    fun findByTag(@Param("tag") tag: String): List<Advertisement>

    /**
     * Update advertisement status
     */
    @Query("UPDATE Advertisement a SET a.status = :status, a.updatedBy = :updatedBy WHERE a.id = :id")
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: AdStatus,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update advertisement statistics
     */
    @Query("""
        UPDATE Advertisement a
        SET a.totalImpressions = a.totalImpressions + :impressions,
            a.totalClicks = a.totalClicks + :clicks,
            a.totalCompletions = a.totalCompletions + :completions,
            a.totalSpend = a.totalSpend + :spend
        WHERE a.id = :id
    """)
    fun updateStatistics(
        @Param("id") id: Long,
        @Param("impressions") impressions: Long,
        @Param("clicks") clicks: Long,
        @Param("completions") completions: Long,
        @Param("spend") spend: BigDecimal
    ): Int

    /**
     * Check if advertisement title exists
     */
    fun existsByTitleIgnoreCase(title: String): Boolean

    /**
     * Find advertisements with budget remaining
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND (a.totalBudget IS NULL OR a.totalSpend < a.totalBudget)
        ORDER BY a.priority DESC
    """)
    fun findAdvertisementsWithBudgetRemaining(): List<Advertisement>

    /**
     * Find advertisements by user segment targeting
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND (a.targetUserSegments IS NULL OR a.targetUserSegments LIKE %:segment%)
        AND (a.excludeUserSegments IS NULL OR a.excludeUserSegments NOT LIKE %:segment%)
        ORDER BY a.priority DESC
    """)
    fun findByUserSegmentTargeting(@Param("segment") segment: String): List<Advertisement>

    /**
     * Find advertisements by age targeting
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND (a.targetAgeMin IS NULL OR a.targetAgeMin <= :age)
        AND (a.targetAgeMax IS NULL OR a.targetAgeMax >= :age)
        ORDER BY a.priority DESC
    """)
    fun findByAgeTargeting(@Param("age") age: Int): List<Advertisement>

    /**
     * Find advertisements by gender targeting
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND (a.targetGenders IS NULL OR a.targetGenders LIKE %:gender%)
        ORDER BY a.priority DESC
    """)
    fun findByGenderTargeting(@Param("gender") gender: String): List<Advertisement>

    /**
     * Find advertisements scheduled for current time
     */
    @Query("""
        SELECT a FROM Advertisement a
        WHERE a.status = 'ACTIVE'
        AND a.startDate <= :now
        AND a.endDate > :now
        AND (a.allowedDaysOfWeek IS NULL OR a.allowedDaysOfWeek LIKE %:dayOfWeek%)
        AND (a.allowedHoursStart IS NULL OR a.allowedHoursEnd IS NULL
             OR (:hour BETWEEN a.allowedHoursStart AND a.allowedHoursEnd)
             OR (a.allowedHoursStart > a.allowedHoursEnd
                 AND (:hour >= a.allowedHoursStart OR :hour <= a.allowedHoursEnd)))
        ORDER BY a.priority DESC
    """)
    fun findScheduledAdvertisements(
        @Param("now") now: LocalDateTime = LocalDateTime.now(),
        @Param("dayOfWeek") dayOfWeek: String,
        @Param("hour") hour: Int
    ): List<Advertisement>
}