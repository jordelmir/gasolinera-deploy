package com.gasolinerajsm.raffleservice.repository

import com.gasolinerajsm.raffleservice.model.RafflePrize
import com.gasolinerajsm.raffleservice.model.PrizeStatus
import com.gasolinerajsm.raffleservice.model.PrizeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository interface for RafflePrize entity operations
 */
@Repository
interface RafflePrizeRepository : JpaRepository<RafflePrize, Long> {

    /**
     * Find prizes by raffle ID
     */
    fun findByRaffleId(raffleId: Long): List<RafflePrize>

    /**
     * Find prizes by raffle ID with pagination
     */
    fun findByRaffleId(raffleId: Long, pageable: Pageable): Page<RafflePrize>

    /**
     * Find prizes by status
     */
    fun findByStatus(status: PrizeStatus, pageable: Pageable): Page<RafflePrize>

    /**
     * Find prizes by type
     */
    fun findByPrizeType(prizeType: PrizeType, pageable: Pageable): Page<RafflePrize>

    /**
     * Find prizes by raffle and status
     */
    fun findByRaffleIdAndStatus(raffleId: Long, status: PrizeStatus): List<RafflePrize>

    /**
     * Find prizes by raffle and type
     */
    fun findByRaffleIdAndPrizeType(raffleId: Long, prizeType: PrizeType): List<RafflePrize>

    /**
     * Find available prizes for raffle
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.raffle.id = :raffleId
        AND p.status = 'ACTIVE'
        AND p.quantityAwarded < p.quantityAvailable
        AND (p.expiryDate IS NULL OR p.expiryDate > :now)
        ORDER BY p.prizeTier ASC
    """)
    fun findAvailablePrizesByRaffle(
        @Param("raffleId") raffleId: Long,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): List<RafflePrize>

    /**
     * Find prizes by tier
     */
    fun findByPrizeTier(prizeTier: Int): List<RafflePrize>

    /**
     * Find prizes by raffle and tier
     */
    fun findByRaffleIdAndPrizeTier(raffleId: Long, prizeTier: Int): List<RafflePrize>

    /**
     * Find prizes by value range
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.value BETWEEN :minValue AND :maxValue
        ORDER BY p.value DESC
    """)
    fun findByValueBetween(
        @Param("minValue") minValue: BigDecimal,
        @Param("maxValue") maxValue: BigDecimal
    ): List<RafflePrize>

    /**
     * Find prizes above value
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.value >= :minValue
        ORDER BY p.value DESC
    """)
    fun findByValueGreaterThanEqual(@Param("minValue") minValue: BigDecimal): List<RafflePrize>

    /**
     * Find expired prizes
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.expiryDate IS NOT NULL
        AND p.expiryDate <= :now
        AND p.status != 'EXPIRED'
    """)
    fun findExpiredPrizes(@Param("now") now: LocalDateTime = LocalDateTime.now()): List<RafflePrize>

    /**
     * Find prizes expiring soon
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.expiryDate IS NOT NULL
        AND p.expiryDate BETWEEN :now AND :deadline
        AND p.status = 'ACTIVE'
    """)
    fun findPrizesExpiringSoon(
        @Param("now") now: LocalDateTime = LocalDateTime.now(),
        @Param("deadline") deadline: LocalDateTime
    ): List<RafflePrize>

    /**
     * Find fully awarded prizes
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.quantityAwarded >= p.quantityAvailable
        AND p.status != 'EXHAUSTED'
    """)
    fun findFullyAwardedPrizes(): List<RafflePrize>

    /**
     * Find prizes by sponsor
     */
    fun findBySponsorNameContainingIgnoreCase(sponsorName: String): List<RafflePrize>

    /**
     * Find prizes by name containing
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<RafflePrize>

    /**
     * Count prizes by raffle
     */
    fun countByRaffleId(raffleId: Long): Long

    /**
     * Count prizes by raffle and status
     */
    fun countByRaffleIdAndStatus(raffleId: Long, status: PrizeStatus): Long

    /**
     * Count prizes by type
     */
    fun countByPrizeType(prizeType: PrizeType): Long

    /**
     * Get total prize value by raffle
     */
    @Query("""
        SELECT COALESCE(SUM(p.value * p.quantityAvailable), 0)
        FROM RafflePrize p
        WHERE p.raffle.id = :raffleId
        AND p.value IS NOT NULL
    """)
    fun getTotalPrizeValueByRaffle(@Param("raffleId") raffleId: Long): BigDecimal

    /**
     * Get awarded prize value by raffle
     */
    @Query("""
        SELECT COALESCE(SUM(p.value * p.quantityAwarded), 0)
        FROM RafflePrize p
        WHERE p.raffle.id = :raffleId
        AND p.value IS NOT NULL
    """)
    fun getAwardedPrizeValueByRaffle(@Param("raffleId") raffleId: Long): BigDecimal

    /**
     * Get prize statistics by raffle
     */
    @Query("""
        SELECT
            COUNT(p) as totalPrizes,
            COUNT(CASE WHEN p.status = 'ACTIVE' THEN 1 END) as activePrizes,
            SUM(p.quantityAvailable) as totalQuantity,
            SUM(p.quantityAwarded) as awardedQuantity,
            COALESCE(SUM(p.value * p.quantityAvailable), 0) as totalValue
        FROM RafflePrize p
        WHERE p.raffle.id = :raffleId
    """)
    fun getPrizeStatisticsByRaffle(@Param("raffleId") raffleId: Long): Map<String, Any>

    /**
     * Find prizes by winning probability range
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.winningProbability BETWEEN :minProbability AND :maxProbability
        ORDER BY p.winningProbability DESC
    """)
    fun findByWinningProbabilityBetween(
        @Param("minProbability") minProbability: BigDecimal,
        @Param("maxProbability") maxProbability: BigDecimal
    ): List<RafflePrize>

    /**
     * Find prizes requiring identity verification
     */
    fun findByRequiresIdentityVerificationTrue(): List<RafflePrize>

    /**
     * Find transferable prizes
     */
    fun findByIsTransferableTrue(): List<RafflePrize>

    /**
     * Update prize status
     */
    @Query("UPDATE RafflePrize p SET p.status = :status WHERE p.id = :id")
    fun updateStatus(@Param("id") id: Long, @Param("status") status: PrizeStatus): Int

    /**
     * Update awarded quantity
     */
    @Query("UPDATE RafflePrize p SET p.quantityAwarded = :quantity WHERE p.id = :id")
    fun updateAwardedQuantity(@Param("id") id: Long, @Param("quantity") quantity: Int): Int

    /**
     * Increment awarded quantity
     */
    @Query("UPDATE RafflePrize p SET p.quantityAwarded = p.quantityAwarded + 1 WHERE p.id = :id")
    fun incrementAwardedQuantity(@Param("id") id: Long): Int

    /**
     * Find prizes by delivery method
     */
    fun findByDeliveryMethodContainingIgnoreCase(deliveryMethod: String): List<RafflePrize>

    /**
     * Find monetary prizes
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.prizeType IN ('CASH', 'GIFT_CARD', 'CREDIT', 'FUEL_CREDIT')
        AND p.value IS NOT NULL
        ORDER BY p.value DESC
    """)
    fun findMonetaryPrizes(): List<RafflePrize>

    /**
     * Find physical prizes
     */
    @Query("""
        SELECT p FROM RafflePrize p
        WHERE p.prizeType IN ('PHYSICAL_ITEM', 'MERCHANDISE')
    """)
    fun findPhysicalPrizes(): List<RafflePrize>

    /**
     * Check if prize name exists in raffle
     */
    fun existsByRaffleIdAndNameIgnoreCase(raffleId: Long, name: String): Boolean

    /**
     * Find prizes by multiple statuses
     */
    fun findByStatusIn(statuses: List<PrizeStatus>, pageable: Pageable): Page<RafflePrize>

    /**
     * Find prizes created in date range
     */
    fun findByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RafflePrize>
}