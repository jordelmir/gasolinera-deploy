package com.gasolinerajsm.adengine.repository

import com.gasolinerajsm.adengine.domain.AdSequence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository interface for AdSequence entity operations
 */
@Repository
interface AdSequenceRepository : JpaRepository<AdSequence, String> {

    /**
     * Find sequences by user and station
     */
    fun findByUserIdAndStationId(userId: String, stationId: String): List<AdSequence>

    /**
     * Find active sequences
     */
    @Query("""
        SELECT s FROM AdSequence s
        WHERE s.status = 'ACTIVE'
        AND s.createdAt >= :since
    """)
    fun findActiveSequences(
        @Param("since") since: LocalDateTime = LocalDateTime.now().minusHours(24)
    ): List<AdSequence>

    /**
     * Find sequences by user
     */
    fun findByUserId(userId: String): List<AdSequence>

    /**
     * Find sequences by status
     */
    fun findByStatus(status: String): List<AdSequence>

    /**
     * Find incomplete sequences for user
     */
    @Query("""
        SELECT s FROM AdSequence s
        WHERE s.userId = :userId
        AND s.status IN ('ACTIVE', 'PAUSED')
        AND s.currentStep < s.totalSteps
    """)
    fun findIncompleteSequencesForUser(@Param("userId") userId: String): List<AdSequence>

    /**
     * Find sequences by session ID
     */
    fun findBySessionId(sessionId: String): List<AdSequence>

    /**
     * Count completed sequences for user
     */
    @Query("""
        SELECT COUNT(s) FROM AdSequence s
        WHERE s.userId = :userId
        AND s.status = 'COMPLETED'
        AND s.createdAt >= :since
    """)
    fun countCompletedSequencesForUser(
        @Param("userId") userId: String,
        @Param("since") since: LocalDateTime
    ): Long

    /**
     * Find sequences with rewards pending
     */
    @Query("""
        SELECT s FROM AdSequence s
        WHERE s.status = 'COMPLETED'
        AND s.rewardEarned > 0
        AND s.rewardClaimed = false
    """)
    fun findSequencesWithPendingRewards(): List<AdSequence>

    /**
     * Count sequences by status
     */
    fun countByStatus(status: String): Long

    /**
     * Find sequences by user and status
     */
    fun findByUserIdAndStatus(userId: String, status: String): List<AdSequence>
}