package com.gasolinerajsm.redemptionservice.repository

import com.gasolinerajsm.redemptionservice.model.RaffleTicket
import com.gasolinerajsm.redemptionservice.model.TicketStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository interface for RaffleTicket entity operations
 */
@Repository
interface RaffleTicketRepository : JpaRepository<RaffleTicket, Long> {

    /**
     * Find tickets by user ID
     */
    fun findByUserIdOrderByGeneratedAtDesc(userId: Long): List<RaffleTicket>

    /**
     * Find tickets by user ID with pagination
     */
    fun findByUserIdOrderByGeneratedAtDesc(userId: Long, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by redemption ID
     */
    fun findByRedemptionIdOrderByGeneratedAtDesc(redemptionId: Long): List<RaffleTicket>

    /**
     * Find tickets by ticket number
     */
    fun findByTicketNumber(ticketNumber: String): RaffleTicket?

    /**
     * Find tickets by status
     */
    fun findByStatusOrderByGeneratedAtDesc(status: TicketStatus): List<RaffleTicket>

    /**
     * Find tickets by status with pagination
     */
    fun findByStatusOrderByGeneratedAtDesc(status: TicketStatus, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find active tickets by user ID
     */
    fun findByUserIdAndStatusOrderByGeneratedAtDesc(userId: Long, status: TicketStatus): List<RaffleTicket>

    /**
     * Find tickets by user and status with pagination
     */
    fun findByUserIdAndStatusOrderByGeneratedAtDesc(
        userId: Long,
        status: TicketStatus,
        pageable: Pageable
    ): Page<RaffleTicket>

    /**
     * Find tickets by date range
     */
    fun findByGeneratedAtBetweenOrderByGeneratedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<RaffleTicket>

    /**
     * Find tickets by date range with pagination
     */
    fun findByGeneratedAtBetweenOrderByGeneratedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RaffleTicket>

    /**
     * Find tickets by user and date range
     */
    fun findByUserIdAndGeneratedAtBetweenOrderByGeneratedAtDesc(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<RaffleTicket>

    /**
     * Find tickets by campaign ID
     */
    fun findByCampaignIdOrderByGeneratedAtDesc(campaignId: Long): List<RaffleTicket>

    /**
     * Find tickets by source type
     */
    fun findBySourceTypeOrderByGeneratedAtDesc(sourceType: String): List<RaffleTicket>

    /**
     * Find tickets by source ID
     */
    fun findBySourceIdOrderByGeneratedAtDesc(sourceId: Long): List<RaffleTicket>

    /**
     * Find expired tickets
     */
    @Query("SELECT rt FROM RaffleTicket rt WHERE rt.expiresAt IS NOT NULL AND rt.expiresAt < :currentTime AND rt.status = 'ACTIVE'")
    fun findExpiredTickets(@Param("currentTime") currentTime: LocalDateTime): List<RaffleTicket>

    /**
     * Find tickets expiring soon
     */
    @Query("SELECT rt FROM RaffleTicket rt WHERE rt.expiresAt IS NOT NULL AND rt.expiresAt BETWEEN :currentTime AND :futureTime AND rt.status = 'ACTIVE'")
    fun findTicketsExpiringSoon(
        @Param("currentTime") currentTime: LocalDateTime,
        @Param("futureTime") futureTime: LocalDateTime
    ): List<RaffleTicket>

    /**
     * Count tickets by user ID
     */
    fun countByUserId(userId: Long): Long

    /**
     * Count active tickets by user ID
     */
    fun countByUserIdAndStatus(userId: Long, status: TicketStatus): Long

    /**
     * Count tickets by redemption ID
     */
    fun countByRedemptionId(redemptionId: Long): Long

    /**
     * Count tickets by status
     */
    fun countByStatus(status: TicketStatus): Long

    /**
     * Count tickets by date range
     */
    fun countByGeneratedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    /**
     * Count tickets by user and date range
     */
    fun countByUserIdAndGeneratedAtBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long

    /**
     * Find tickets by multiple statuses
     */
    fun findByStatusInOrderByGeneratedAtDesc(statuses: List<TicketStatus>): List<RaffleTicket>

    /**
     * Find tickets by user and multiple statuses
     */
    fun findByUserIdAndStatusInOrderByGeneratedAtDesc(
        userId: Long,
        statuses: List<TicketStatus>
    ): List<RaffleTicket>

    /**
     * Find tickets by raffle ID (when consumed)
     */
    fun findByRaffleIdOrderByConsumedAtDesc(raffleId: Long): List<RaffleTicket>

    /**
     * Find consumed tickets by user
     */
    @Query("SELECT rt FROM RaffleTicket rt WHERE rt.userId = :userId AND rt.status = 'CONSUMED' ORDER BY rt.consumedAt DESC")
    fun findConsumedTicketsByUser(@Param("userId") userId: Long): List<RaffleTicket>

    /**
     * Find won tickets by user
     */
    @Query("SELECT rt FROM RaffleTicket rt WHERE rt.userId = :userId AND rt.status = 'WON' ORDER BY rt.generatedAt DESC")
    fun findWonTicketsByUser(@Param("userId") userId: Long): List<RaffleTicket>

    /**
     * Get user ticket balance (active tickets)
     */
    @Query("SELECT COUNT(rt) FROM RaffleTicket rt WHERE rt.userId = :userId AND rt.status = 'ACTIVE'")
    fun getUserTicketBalance(@Param("userId") userId: Long): Long

    /**
     * Get user total tickets generated
     */
    @Query("SELECT COUNT(rt) FROM RaffleTicket rt WHERE rt.userId = :userId")
    fun getUserTotalTicketsGenerated(@Param("userId") userId: Long): Long

    /**
     * Get user tickets consumed
     */
    @Query("SELECT COUNT(rt) FROM RaffleTicket rt WHERE rt.userId = :userId AND rt.status = 'CONSUMED'")
    fun getUserTicketsConsumed(@Param("userId") userId: Long): Long

    /**
     * Get user tickets won
     */
    @Query("SELECT COUNT(rt) FROM RaffleTicket rt WHERE rt.userId = :userId AND rt.status = 'WON'")
    fun getUserTicketsWon(@Param("userId") userId: Long): Long

    /**
     * Find tickets by batch ID
     */
    fun findByBatchIdOrderByGeneratedAtDesc(batchId: String): List<RaffleTicket>

    /**
     * Find tickets by multiplier value
     */
    fun findByMultiplierValueOrderByGeneratedAtDesc(multiplierValue: Int): List<RaffleTicket>

    /**
     * Find tickets with notes
     */
    @Query("SELECT rt FROM RaffleTicket rt WHERE rt.notes IS NOT NULL AND rt.notes != '' ORDER BY rt.generatedAt DESC")
    fun findTicketsWithNotes(): List<RaffleTicket>

    /**
     * Get ticket statistics by date range
     */
    @Query("""
        SELECT
            COUNT(rt) as totalCount,
            COUNT(CASE WHEN rt.status = 'ACTIVE' THEN 1 END) as activeCount,
            COUNT(CASE WHEN rt.status = 'CONSUMED' THEN 1 END) as consumedCount,
            COUNT(CASE WHEN rt.status = 'WON' THEN 1 END) as wonCount,
            COUNT(CASE WHEN rt.status = 'EXPIRED' THEN 1 END) as expiredCount
        FROM RaffleTicket rt
        WHERE rt.generatedAt BETWEEN :startDate AND :endDate
    """)
    fun getTicketStatistics(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Map<String, Long>

    /**
     * Get user ticket statistics
     */
    @Query("""
        SELECT
            COUNT(rt) as totalCount,
            COUNT(CASE WHEN rt.status = 'ACTIVE' THEN 1 END) as activeCount,
            COUNT(CASE WHEN rt.status = 'CONSUMED' THEN 1 END) as consumedCount,
            COUNT(CASE WHEN rt.status = 'WON' THEN 1 END) as wonCount,
            COUNT(CASE WHEN rt.status = 'EXPIRED' THEN 1 END) as expiredCount
        FROM RaffleTicket rt
        WHERE rt.userId = :userId
    """)
    fun getUserTicketStatistics(@Param("userId") userId: Long): Map<String, Long>

    /**
     * Find top users by ticket count
     */
    @Query("""
        SELECT rt.userId, COUNT(rt) as ticketCount
        FROM RaffleTicket rt
        WHERE rt.generatedAt BETWEEN :startDate AND :endDate
        GROUP BY rt.userId
        ORDER BY ticketCount DESC
    """)
    fun findTopUsersByTicketCount(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Array<Any>>

    /**
     * Find tickets by metadata
     */
    @Query("SELECT rt FROM RaffleTicket rt WHERE rt.metadata LIKE %:searchTerm% ORDER BY rt.generatedAt DESC")
    fun findTicketsByMetadata(@Param("searchTerm") searchTerm: String): List<RaffleTicket>

    /**
     * Check if ticket number exists
     */
    fun existsByTicketNumber(ticketNumber: String): Boolean

    /**
     * Find duplicate ticket numbers (should not happen, but for validation)
     */
    @Query("SELECT rt.ticketNumber, COUNT(rt) FROM RaffleTicket rt GROUP BY rt.ticketNumber HAVING COUNT(rt) > 1")
    fun findDuplicateTicketNumbers(): List<Array<Any>>

    /**
     * Find tickets ready for raffle (active and not expired)
     */
    @Query("""
        SELECT rt FROM RaffleTicket rt
        WHERE rt.status = 'ACTIVE'
        AND (rt.expiresAt IS NULL OR rt.expiresAt > :currentTime)
        ORDER BY rt.generatedAt ASC
    """)
    fun findTicketsReadyForRaffle(@Param("currentTime") currentTime: LocalDateTime): List<RaffleTicket>

    /**
     * Find tickets by source type and date range
     */
    fun findBySourceTypeAndGeneratedAtBetweenOrderByGeneratedAtDesc(
        sourceType: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<RaffleTicket>

    /**
     * Get campaign ticket statistics
     */
    @Query("""
        SELECT
            rt.campaignId,
            COUNT(rt) as totalCount,
            COUNT(CASE WHEN rt.status = 'ACTIVE' THEN 1 END) as activeCount,
            COUNT(CASE WHEN rt.status = 'CONSUMED' THEN 1 END) as consumedCount
        FROM RaffleTicket rt
        WHERE rt.campaignId IS NOT NULL
        AND rt.generatedAt BETWEEN :startDate AND :endDate
        GROUP BY rt.campaignId
        ORDER BY totalCount DESC
    """)
    fun getCampaignTicketStatistics(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>
}