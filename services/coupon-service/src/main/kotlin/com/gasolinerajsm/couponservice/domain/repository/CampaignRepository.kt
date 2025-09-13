package com.gasolinerajsm.couponservice.domain.repository

import com.gasolinerajsm.couponservice.domain.model.Campaign
import com.gasolinerajsm.couponservice.domain.model.CampaignStatus
import com.gasolinerajsm.couponservice.domain.valueobject.CampaignId
import java.time.LocalDateTime

/**
 * Campaign Repository Interface (Port)
 * Defines the contract for campaign persistence without implementation details
 */
interface CampaignRepository {

    /**
     * Save a campaign entity
     */
    suspend fun save(campaign: Campaign): Result<Campaign>

    /**
     * Find campaign by ID
     */
    suspend fun findById(id: CampaignId): Result<Campaign?>

    /**
     * Find campaign by name
     */
    suspend fun findByName(name: String): Result<Campaign?>

    /**
     * Find all campaigns
     */
    suspend fun findAll(): Result<List<Campaign>>

    /**
     * Find campaigns by status
     */
    suspend fun findByStatus(status: CampaignStatus): Result<List<Campaign>>

    /**
     * Find active campaigns
     */
    suspend fun findActiveCampaigns(): Result<List<Campaign>>

    /**
     * Find campaigns that are currently running (active and within validity period)
     */
    suspend fun findRunningCampaigns(asOf: LocalDateTime = LocalDateTime.now()): Result<List<Campaign>>

    /**
     * Find campaigns expiring soon
     */
    suspend fun findCampaignsExpiringSoon(withinHours: Int = 24): Result<List<Campaign>>

    /**
     * Find campaigns that have expired but are still active
     */
    suspend fun findExpiredActiveCampaigns(asOf: LocalDateTime = LocalDateTime.now()): Result<List<Campaign>>

    /**
     * Find campaigns within date range
     */
    suspend fun findCampaignsInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Result<List<Campaign>>

    /**
     * Check if campaign name exists
     */
    suspend fun existsByName(name: String): Result<Boolean>

    /**
     * Count campaigns by status
     */
    suspend fun countByStatus(status: CampaignStatus): Result<Long>

    /**
     * Count total campaigns
     */
    suspend fun count(): Result<Long>

    /**
     * Delete campaign by ID
     */
    suspend fun deleteById(id: CampaignId): Result<Unit>

    /**
     * Find campaigns for analytics with pagination
     */
    suspend fun findForAnalytics(
        status: CampaignStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Campaign>>
}