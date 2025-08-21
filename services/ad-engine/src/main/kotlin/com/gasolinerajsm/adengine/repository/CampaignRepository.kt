package com.gasolinerajsm.adengine.repository

import com.gasolinerajsm.adengine.model.AdCampaign
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository interface for AdCampaign entity operations
 */
@Repository
interface CampaignRepository : JpaRepository<AdCampaign, Long> {

    /**
     * Find active campaigns for a specific station
     */
    @Query("""
        SELECT c FROM AdCampaign c
        WHERE c.status = 'ACTIVE'
        AND c.startDate <= :currentDate
        AND c.endDate >= :currentDate
        AND (c.targetStations IS NULL OR :stationId MEMBER OF c.targetStations)
    """)
    fun findActiveCampaignsForStation(
        @Param("stationId") stationId: String,
        @Param("currentDate") currentDate: LocalDateTime = LocalDateTime.now()
    ): List<AdCampaign>

    /**
     * Find campaigns by status and date range
     */
    fun findByStatusAndStartDateBeforeAndEndDateAfter(
        status: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<AdCampaign>

    /**
     * Find active campaigns
     */
    @Query("""
        SELECT c FROM AdCampaign c
        WHERE c.status = 'ACTIVE'
        AND c.startDate <= :currentDate
        AND c.endDate >= :currentDate
    """)
    fun findActiveCampaigns(
        @Param("currentDate") currentDate: LocalDateTime = LocalDateTime.now()
    ): List<AdCampaign>

    /**
     * Find campaigns by advertiser
     */
    fun findByAdvertiserId(advertiserId: String): List<AdCampaign>

    /**
     * Find campaigns by status
     */
    fun findByStatus(status: String): List<AdCampaign>

    /**
     * Count active campaigns
     */
    @Query("""
        SELECT COUNT(c) FROM AdCampaign c
        WHERE c.status = 'ACTIVE'
        AND c.startDate <= :currentDate
        AND c.endDate >= :currentDate
    """)
    fun countActiveCampaigns(
        @Param("currentDate") currentDate: LocalDateTime = LocalDateTime.now()
    ): Long

    /**
     * Find campaigns with budget remaining
     */
    @Query("""
        SELECT c FROM AdCampaign c
        WHERE c.status = 'ACTIVE'
        AND c.budgetSpent < c.budgetTotal
        AND c.startDate <= :currentDate
        AND c.endDate >= :currentDate
    """)
    fun findCampaignsWithBudget(
        @Param("currentDate") currentDate: LocalDateTime = LocalDateTime.now()
    ): List<AdCampaign>
}