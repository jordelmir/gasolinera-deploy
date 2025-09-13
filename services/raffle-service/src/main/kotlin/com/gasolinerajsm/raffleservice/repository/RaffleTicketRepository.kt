package com.gasolinerajsm.raffleservice.repository

import com.gasolinerajsm.raffleservice.model.RaffleTicket
import com.gasolinerajsm.raffleservice.model.TicketStatus
import com.gasolinerajsm.raffleservice.model.TicketSourceType
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
    fun findByUserId(userId: Long, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by raffle ID
     */
    fun findByRaffleId(raffleId: Long, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by user and raffle
     */
    fun findByUserIdAndRaffleId(userId: Long, raffleId: Long, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by status
     */
    fun findByStatus(status: TicketStatus, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by source type
     */
    fun findBySourceType(sourceType: TicketSourceType, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find winning tickets
     */
    fun findByIsWinnerTrue(pageable: Pageable): Page<RaffleTicket>

    /**
     * Find winning tickets by raffle
     */
    fun findByRaffleIdAndIsWinnerTrue(raffleId: Long): List<RaffleTicket>

    /**
     * Find tickets by user and status
     */
    fun findByUserIdAndStatus(userId: Long, status: TicketStatus, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by raffle and status
     */
    fun findByRaffleIdAndStatus(raffleId: Long, status: TicketStatus, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets eligible for draw
     */
    @Query("""
        SELECT t FROM RaffleTicket t
        WHERE t.raffle.id = :raffleId
        AND t.status = 'ACTIVE'
        AND (t.raffle.requiresVerification = false OR t.isVerified = true)
    """)
    fun findEligibleTicketsForDraw(@Param("raffleId") raffleId: Long): List<RaffleTicket>

    /**
     * Find tickets needing verification
     */
    @Query("""
        SELECT t FROM RaffleTicket t
        WHERE t.raffle.requiresVerification = true
        AND t.isVerified = false
        AND t.status = 'ACTIVE'
        AND t.verificationCode IS NOT NULL
    """)
    fun findTicketsNeedingVerification(): List<RaffleTicket>

    /**
     * Find tickets by coupon ID
     */
    fun findByCouponId(couponId: Long): List<RaffleTicket>

    /**
     * Find tickets by campaign ID
     */
    fun findByCampaignId(campaignId: Long): List<RaffleTicket>

    /**
     * Find tickets by station ID
     */
    fun findByStationId(stationId: Long, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by transaction reference
     */
    fun findByTransactionReference(transactionReference: String): List<RaffleTicket>

    /**
     * Count tickets by user
     */
    fun countByUserId(userId: Long): Long

    /**
     * Count tickets by user and raffle
     */
    fun countByUserIdAndRaffleId(userId: Long, raffleId: Long): Long

    /**
     * Count tickets by user, raffle and status
     */
    fun countByUserIdAndRaffleIdAndStatus(userId: Long, raffleId: Long, status: TicketStatus): Long

    /**
     * Count tickets by user and status
     */
    fun countByUserIdAndStatus(userId: Long, status: TicketStatus): Long

    /**
     * Count tickets by raffle
     */
    fun countByRaffleId(raffleId: Long): Long

    /**
     * Count tickets by raffle and status
     */
    fun countByRaffleIdAndStatus(raffleId: Long, status: TicketStatus): Long

    /**
     * Count winning tickets by raffle
     */
    fun countByRaffleIdAndIsWinnerTrue(raffleId: Long): Long

    /**
     * Find tickets by verification code
     */
    fun findByVerificationCode(verificationCode: String): RaffleTicket?

    /**
     * Find tickets by ticket number and raffle
     */
    fun findByTicketNumberAndRaffleId(ticketNumber: String, raffleId: Long): RaffleTicket?

    /**
     * Check if ticket number exists in raffle
     */
    fun existsByTicketNumberAndRaffleId(ticketNumber: String, raffleId: Long): Boolean

    /**
     * Find tickets created in date range
     */
    fun findByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RaffleTicket>

    /**
     * Find tickets by user in date range
     */
    fun findByUserIdAndCreatedAtBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RaffleTicket>

    /**
     * Get user ticket statistics
     */
    @Query("""
        SELECT
            COUNT(t) as totalTickets,
            COUNT(CASE WHEN t.status = 'ACTIVE' THEN 1 END) as activeTickets,
            COUNT(CASE WHEN t.isWinner = true THEN 1 END) as winningTickets,
            COUNT(CASE WHEN t.prizeClaimed = true THEN 1 END) as claimedPrizes
        FROM RaffleTicket t
        WHERE t.userId = :userId
    """)
    fun getUserTicketStatistics(@Param("userId") userId: Long): Map<String, Any>

    /**
     * Get raffle ticket statistics
     */
    @Query("""
        SELECT
            COUNT(t) as totalTickets,
            COUNT(CASE WHEN t.status = 'ACTIVE' THEN 1 END) as activeTickets,
            COUNT(CASE WHEN t.isWinner = true THEN 1 END) as winningTickets,
            COUNT(DISTINCT t.userId) as uniqueParticipants
        FROM RaffleTicket t
        WHERE t.raffle.id = :raffleId
    """)
    fun getRaffleTicketStatistics(@Param("raffleId") raffleId: Long): Map<String, Any>

    /**
     * Find tickets by source type and date range
     */
    fun findBySourceTypeAndCreatedAtBetween(
        sourceType: TicketSourceType,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RaffleTicket>

    /**
     * Find tickets with unclaimed prizes
     */
    @Query("""
        SELECT t FROM RaffleTicket t
        WHERE t.isWinner = true
        AND t.prizeClaimed = false
        AND t.status = 'ACTIVE'
    """)
    fun findUnclaimedWinningTickets(): List<RaffleTicket>

    /**
     * Find tickets with unclaimed prizes by raffle
     */
    @Query("""
        SELECT t FROM RaffleTicket t
        WHERE t.raffle.id = :raffleId
        AND t.isWinner = true
        AND t.prizeClaimed = false
        AND t.status = 'ACTIVE'
    """)
    fun findUnclaimedWinningTicketsByRaffle(@Param("raffleId") raffleId: Long): List<RaffleTicket>

    /**
     * Find tickets by multiple statuses
     */
    fun findByStatusIn(statuses: List<TicketStatus>, pageable: Pageable): Page<RaffleTicket>

    /**
     * Find tickets by user and multiple statuses
     */
    fun findByUserIdAndStatusIn(userId: Long, statuses: List<TicketStatus>, pageable: Pageable): Page<RaffleTicket>

    /**
     * Update ticket status
     */
    @Query("UPDATE RaffleTicket t SET t.status = :status WHERE t.id = :id")
    fun updateStatus(@Param("id") id: Long, @Param("status") status: TicketStatus): Int

    /**
     * Mark ticket as winner
     */
    @Query("UPDATE RaffleTicket t SET t.isWinner = true WHERE t.id = :id")
    fun markAsWinner(@Param("id") id: Long): Int

    /**
     * Mark prize as claimed
     */
    @Query("""
        UPDATE RaffleTicket t
        SET t.prizeClaimed = true, t.prizeClaimDate = :claimDate
        WHERE t.id = :id
    """)
    fun markPrizeAsClaimed(@Param("id") id: Long, @Param("claimDate") claimDate: LocalDateTime): Int

    /**
     * Verify ticket
     */
    @Query("""
        UPDATE RaffleTicket t
        SET t.isVerified = true, t.verifiedAt = :verifiedAt, t.verifiedBy = :verifiedBy
        WHERE t.id = :id
    """)
    fun verifyTicket(
        @Param("id") id: Long,
        @Param("verifiedAt") verifiedAt: LocalDateTime,
        @Param("verifiedBy") verifiedBy: String?
    ): Int

    /**
     * Find duplicate tickets by user and raffle (for validation)
     */
    @Query("""
        SELECT t FROM RaffleTicket t
        WHERE t.userId = :userId
        AND t.raffle.id = :raffleId
        AND t.sourceType = :sourceType
        AND t.sourceReference = :sourceReference
        AND t.status != 'CANCELLED'
    """)
    fun findDuplicateTickets(
        @Param("userId") userId: Long,
        @Param("raffleId") raffleId: Long,
        @Param("sourceType") sourceType: TicketSourceType,
        @Param("sourceReference") sourceReference: String?
    ): List<RaffleTicket>

    /**
     * Find tickets by user where tickets were awarded (multiplied)
     */
    @Query("""
        SELECT t FROM RaffleTicket t
        WHERE t.userId = :userId
        AND t.ticketsAwarded = true
    """)
    fun findByUserIdAndTicketsAwardedTrue(@Param("userId") userId: Long): List<RaffleTicket>
}