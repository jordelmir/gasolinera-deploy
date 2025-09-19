package com.gasolinerajsm.raffleservice.repository

import com.gasolinerajsm.raffleservice.model.RaffleWinner
import com.gasolinerajsm.raffleservice.model.WinnerStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository interface for RaffleWinner entity operations
 */
@Repository
interface RaffleWinnerRepository : JpaRepository<RaffleWinner, Long> {

    /**
     * Find winners by raffle ID
     */
    fun findByRaffleId(raffleId: Long): List<RaffleWinner>

    /**
     * Find winners by raffle ID with pagination
     */
    fun findByRaffleId(raffleId: Long, pageable: Pageable): Page<RaffleWinner>

    /**
     * Find winners by user ID
     */
    fun findByUserId(userId: Long, pageable: Pageable): Page<RaffleWinner>

    /**
     * Find winners by status
     */
    fun findByStatus(status: WinnerStatus, pageable: Pageable): Page<RaffleWinner>

    /**
     * Find winners by raffle and status
     */
    fun findByRaffleIdAndStatus(raffleId: Long, status: WinnerStatus): List<RaffleWinner>

    /**
     * Find winners by user and status
     */
    fun findByUserIdAndStatus(userId: Long, status: WinnerStatus, pageable: Pageable): Page<RaffleWinner>

    /**
     * Find winners by ticket ID
     */
    fun findByTicketId(ticketId: Long): RaffleWinner?

    /**
     * Find winners by prize ID
     */
    fun findByPrizeId(prizeId: Long): List<RaffleWinner>

    /**
     * Find winners pending claim
     */
    fun findByStatusAndClaimedAtIsNull(status: WinnerStatus): List<RaffleWinner>

    /**
     * Find winners with expired claims
     */
    @Query("""
        SELECT w FROM RaffleWinner w
        WHERE w.status = 'PENDING_CLAIM'
        AND w.claimDeadline IS NOT NULL
        AND w.claimDeadline <= :now
        AND w.claimedAt IS NULL
    """)
    fun findExpiredClaims(@Param("now") now: LocalDateTime = LocalDateTime.now()): List<RaffleWinner>

    /**
     * Find winners needing verification
     */
    @Query("""
        SELECT w FROM RaffleWinner w
        WHERE w.prize.requiresIdentityVerification = true
        AND w.isVerified = false
        AND w.status = 'PENDING_CLAIM'
    """)
    fun findWinnersNeedingVerification(): List<RaffleWinner>

    /**
     * Find unnotified winners
     */
    fun findByNotifiedAtIsNull(): List<RaffleWinner>

    /**
     * Find winners by verification code
     */
    fun findByVerificationCode(verificationCode: String): RaffleWinner?

    /**
     * Find winners won in date range
     */
    fun findByWonAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RaffleWinner>

    /**
     * Find winners claimed in date range
     */
    fun findByClaimedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RaffleWinner>

    /**
     * Find winners by delivery status
     */
    fun findByDeliveryStatus(deliveryStatus: String): List<RaffleWinner>

    /**
     * Find winners requiring delivery
     */
    @Query("""
        SELECT w FROM RaffleWinner w
        WHERE w.deliveryMethod IS NOT NULL
        AND w.deliveryStatus IN ('PENDING', 'PROCESSING')
        AND w.status = 'CLAIMED'
    """)
    fun findWinnersRequiringDelivery(): List<RaffleWinner>

    /**
     * Find winners by tracking number
     */
    fun findByTrackingNumber(trackingNumber: String): RaffleWinner?

    /**
     * Count winners by raffle
     */
    fun countByRaffleId(raffleId: Long): Long

    /**
     * Count winners by raffle and status
     */
    fun countByRaffleIdAndStatus(raffleId: Long, status: WinnerStatus): Long

    /**
     * Count winners by user
     */
    fun countByUserId(userId: Long): Long

    /**
     * Count winners by user and status
     */
    fun countByUserIdAndStatus(userId: Long, status: WinnerStatus): Long

    /**
     * Count claimed prizes by raffle
     */
    fun countByRaffleIdAndStatusIn(raffleId: Long, statuses: List<WinnerStatus>): Long

    /**
     * Get winner statistics by raffle
     */
    @Query("""
        SELECT
            COUNT(w) as totalWinners,
            COUNT(CASE WHEN w.status = 'PENDING_CLAIM' THEN 1 END) as pendingClaims,
            COUNT(CASE WHEN w.status = 'CLAIMED' THEN 1 END) as claimedPrizes,
            COUNT(CASE WHEN w.status = 'EXPIRED' THEN 1 END) as expiredClaims,
            COUNT(CASE WHEN w.isVerified = true THEN 1 END) as verifiedWinners,
            COUNT(CASE WHEN w.notifiedAt IS NOT NULL THEN 1 END) as notifiedWinners
        FROM RaffleWinner w
        WHERE w.raffle.id = :raffleId
    """)
    fun getWinnerStatisticsByRaffle(@Param("raffleId") raffleId: Long): Map<String, Any>

    /**
     * Get user winner statistics
     */
    @Query("""
        SELECT
            COUNT(w) as totalWins,
            COUNT(CASE WHEN w.status = 'CLAIMED' THEN 1 END) as claimedPrizes,
            COUNT(CASE WHEN w.status = 'PENDING_CLAIM' THEN 1 END) as pendingClaims,
            COUNT(DISTINCT w.raffle.id) as uniqueRaffles
        FROM RaffleWinner w
        WHERE w.userId = :userId
    """)
    fun getUserWinnerStatistics(@Param("userId") userId: Long): Map<String, Any>

    /**
     * Find winners by contact phone
     */
    fun findByContactPhone(contactPhone: String): List<RaffleWinner>

    /**
     * Find winners by contact email
     */
    fun findByContactEmailIgnoreCase(contactEmail: String): List<RaffleWinner>

    /**
     * Find winners by identity document
     */
    fun findByIdentityDocument(identityDocument: String): List<RaffleWinner>

    /**
     * Find winners processed by user
     */
    fun findByProcessedBy(processedBy: String, pageable: Pageable): Page<RaffleWinner>

    /**
     * Find winners verified by user
     */
    fun findByVerifiedBy(verifiedBy: String, pageable: Pageable): Page<RaffleWinner>

    /**
     * Check if user won in raffle
     */
    fun existsByUserIdAndRaffleId(userId: Long, raffleId: Long): Boolean

    /**
     * Check if ticket won
     */
    fun existsByTicketId(ticketId: Long): Boolean

    /**
     * Find winners by multiple statuses
     */
    fun findByStatusIn(statuses: List<WinnerStatus>, pageable: Pageable): Page<RaffleWinner>

    /**
     * Find winners by user and multiple statuses
     */
    fun findByUserIdAndStatusIn(userId: Long, statuses: List<WinnerStatus>, pageable: Pageable): Page<RaffleWinner>

    /**
     * Update winner status
     */
    @Query("UPDATE RaffleWinner w SET w.status = :status WHERE w.id = :id")
    fun updateStatus(@Param("id") id: Long, @Param("status") status: WinnerStatus): Int

    /**
     * Mark as notified
     */
    @Query("UPDATE RaffleWinner w SET w.notifiedAt = :notifiedAt WHERE w.id = :id")
    fun markAsNotified(@Param("id") id: Long, @Param("notifiedAt") notifiedAt: LocalDateTime): Int

    /**
     * Mark as claimed
     */
    @Query("""
        UPDATE RaffleWinner w
        SET w.status = 'CLAIMED', w.claimedAt = :claimedAt, w.processedBy = :processedBy
        WHERE w.id = :id
    """)
    fun markAsClaimed(
        @Param("id") id: Long,
        @Param("claimedAt") claimedAt: LocalDateTime,
        @Param("processedBy") processedBy: String?
    ): Int

    /**
     * Verify winner
     */
    @Query("""
        UPDATE RaffleWinner w
        SET w.isVerified = true, w.verifiedAt = :verifiedAt, w.verifiedBy = :verifiedBy, w.identityDocument = :identityDoc
        WHERE w.id = :id
    """)
    fun verifyWinner(
        @Param("id") id: Long,
        @Param("verifiedAt") verifiedAt: LocalDateTime,
        @Param("verifiedBy") verifiedBy: String?,
        @Param("identityDoc") identityDoc: String?
    ): Int

    /**
     * Update delivery status
     */
    @Query("""
        UPDATE RaffleWinner w
        SET w.deliveryStatus = :status, w.trackingNumber = :trackingNumber,
            w.deliveredAt = CASE WHEN :status = 'DELIVERED' THEN :deliveredAt ELSE w.deliveredAt END
        WHERE w.id = :id
    """)
    fun updateDeliveryStatus(
        @Param("id") id: Long,
        @Param("status") status: String,
        @Param("trackingNumber") trackingNumber: String?,
        @Param("deliveredAt") deliveredAt: LocalDateTime?
    ): Int

    /**
     * Set delivery information
     */
    @Query("""
        UPDATE RaffleWinner w
        SET w.deliveryMethod = :method, w.deliveryAddress = :address,
            w.contactPhone = :phone, w.contactEmail = :email, w.deliveryStatus = 'PENDING'
        WHERE w.id = :id
    """)
    fun setDeliveryInfo(
        @Param("id") id: Long,
        @Param("method") method: String,
        @Param("address") address: String?,
        @Param("phone") phone: String?,
        @Param("email") email: String?
    ): Int

    /**
     * Find recent winners (last N days)
     */
    @Query("""
        SELECT w FROM RaffleWinner w
        WHERE w.wonAt >= :sinceDate
        ORDER BY w.wonAt DESC
    """)
    fun findRecentWinners(@Param("sinceDate") sinceDate: LocalDateTime): List<RaffleWinner>

    /**
     * Find winners with claims expiring soon
     */
    @Query("""
        SELECT w FROM RaffleWinner w
        WHERE w.status = 'PENDING_CLAIM'
        AND w.claimDeadline IS NOT NULL
        AND w.claimDeadline BETWEEN :now AND :deadline
        AND w.claimedAt IS NULL
    """)
    fun findWinnersWithClaimsExpiringSoon(
        @Param("now") now: LocalDateTime = LocalDateTime.now(),
        @Param("deadline") deadline: LocalDateTime
    ): List<RaffleWinner>
}