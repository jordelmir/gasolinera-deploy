package com.gasolinerajsm.raffleservice.repository

import com.gasolinerajsm.raffleservice.model.Raffle
import com.gasolinerajsm.raffleservice.model.RaffleStatus
import com.gasolinerajsm.raffleservice.model.RaffleType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository interface for Raffle entity operations
 */
@Repository
interface RaffleRepository : JpaRepository<Raffle, Long> {

    /**
     * Find raffles by status
     */
    fun findByStatus(status: RaffleStatus, pageable: Pageable): Page<Raffle>

    /**
     * Find raffles by type
     */
    fun findByRaffleType(raffleType: RaffleType, pageable: Pageable): Page<Raffle>

    /**
     * Find raffles by status and type
     */
    fun findByStatusAndRaffleType(
        status: RaffleStatus,
        raffleType: RaffleType,
        pageable: Pageable
    ): Page<Raffle>

    /**
     * Find public raffles
     */
    fun findByIsPublicTrue(pageable: Pageable): Page<Raffle>

    /**
     * Find active public raffles
     */
    fun findByStatusAndIsPublicTrue(status: RaffleStatus, pageable: Pageable): Page<Raffle>

    /**
     * Find raffles with registration currently open
     */
    @Query("""
        SELECT r FROM Raffle r
        WHERE r.status = :status
        AND r.registrationStart <= :now
        AND r.registrationEnd > :now
        AND (:isPublic IS NULL OR r.isPublic = :isPublic)
    """)
    fun findRafflesWithOpenRegistration(
        @Param("status") status: RaffleStatus = RaffleStatus.ACTIVE,
        @Param("now") now: LocalDateTime = LocalDateTime.now(),
        @Param("isPublic") isPublic: Boolean? = null,
        pageable: Pageable
    ): Page<Raffle>

    /**
     * Find raffles ready for draw
     */
    @Query("""
        SELECT r FROM Raffle r
        WHERE r.status = :status
        AND r.registrationEnd <= :now
        AND r.drawDate <= :now
        AND r.currentParticipants >= r.minTicketsToParticipate
    """)
    fun findRafflesReadyForDraw(
        @Param("status") status: RaffleStatus = RaffleStatus.ACTIVE,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): List<Raffle>

    /**
     * Find raffles by draw date range
     */
    @Query("""
        SELECT r FROM Raffle r
        WHERE r.drawDate BETWEEN :startDate AND :endDate
        ORDER BY r.drawDate ASC
    """)
    fun findByDrawDateBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Raffle>

    /**
     * Find raffles by registration period
     */
    @Query("""
        SELECT r FROM Raffle r
        WHERE (r.registrationStart BETWEEN :startDate AND :endDate)
        OR (r.registrationEnd BETWEEN :startDate AND :endDate)
        OR (r.registrationStart <= :startDate AND r.registrationEnd >= :endDate)
        ORDER BY r.registrationStart ASC
    """)
    fun findByRegistrationPeriodOverlapping(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Raffle>

    /**
     * Find raffles created by user
     */
    fun findByCreatedBy(createdBy: String, pageable: Pageable): Page<Raffle>

    /**
     * Find raffles by name containing (case insensitive)
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Raffle>

    /**
     * Count raffles by status
     */
    fun countByStatus(status: RaffleStatus): Long

    /**
     * Count raffles by type
     */
    fun countByRaffleType(raffleType: RaffleType): Long

    /**
     * Count active public raffles
     */
    fun countByStatusAndIsPublicTrue(status: RaffleStatus): Long

    /**
     * Find raffles with low participation
     */
    @Query("""
        SELECT r FROM Raffle r
        WHERE r.status = :status
        AND r.maxParticipants IS NOT NULL
        AND (CAST(r.currentParticipants AS double) / CAST(r.maxParticipants AS double)) < :threshold
    """)
    fun findRafflesWithLowParticipation(
        @Param("status") status: RaffleStatus = RaffleStatus.ACTIVE,
        @Param("threshold") threshold: Double = 0.5
    ): List<Raffle>

    /**
     * Find raffles expiring soon
     */
    @Query("""
        SELECT r FROM Raffle r
        WHERE r.status = :status
        AND r.registrationEnd BETWEEN :now AND :deadline
    """)
    fun findRafflesExpiringSoon(
        @Param("status") status: RaffleStatus = RaffleStatus.ACTIVE,
        @Param("now") now: LocalDateTime = LocalDateTime.now(),
        @Param("deadline") deadline: LocalDateTime
    ): List<Raffle>

    /**
     * Get raffle statistics
     */
    @Query("""
        SELECT
            COUNT(r) as totalRaffles,
            COUNT(CASE WHEN r.status = 'ACTIVE' THEN 1 END) as activeRaffles,
            COUNT(CASE WHEN r.status = 'COMPLETED' THEN 1 END) as completedRaffles,
            SUM(r.currentParticipants) as totalParticipants,
            AVG(r.currentParticipants) as avgParticipants
        FROM Raffle r
    """)
    fun getRaffleStatistics(): Map<String, Any>

    /**
     * Find raffles with prizes above value
     */
    @Query("""
        SELECT DISTINCT r FROM Raffle r
        JOIN r.prizes p
        WHERE p.value >= :minValue
        AND r.status = :status
    """)
    fun findRafflesWithPrizesAboveValue(
        @Param("minValue") minValue: java.math.BigDecimal,
        @Param("status") status: RaffleStatus = RaffleStatus.ACTIVE
    ): List<Raffle>

    /**
     * Check if raffle name exists (case insensitive)
     */
    fun existsByNameIgnoreCase(name: String): Boolean

    /**
     * Find raffles by multiple statuses
     */
    fun findByStatusIn(statuses: List<RaffleStatus>, pageable: Pageable): Page<Raffle>

    /**
     * Find raffles created in date range
     */
    fun findByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Raffle>

    /**
     * Update raffle status
     */
    @Query("UPDATE Raffle r SET r.status = :status, r.updatedBy = :updatedBy WHERE r.id = :id")
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: RaffleStatus,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update participant count
     */
    @Query("UPDATE Raffle r SET r.currentParticipants = :count WHERE r.id = :id")
    fun updateParticipantCount(@Param("id") id: Long, @Param("count") count: Int): Int

    /**
     * Update ticket statistics
     */
    @Query("""
        UPDATE Raffle r
        SET r.totalTicketsIssued = :issued, r.totalTicketsUsed = :used
        WHERE r.id = :id
    """)
    fun updateTicketStatistics(
        @Param("id") id: Long,
        @Param("issued") issued: Long,
        @Param("used") used: Long
    ): Int
}