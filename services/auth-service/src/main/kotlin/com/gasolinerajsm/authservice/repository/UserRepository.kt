package com.gasolinerajsm.authservice.repository

import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.model.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {

    /**
     * Find user by phone number
     */
    fun findByPhoneNumber(phoneNumber: String): User?

    /**
     * Find user by phone number and active status
     */
    fun findByPhoneNumberAndIsActive(phoneNumber: String, isActive: Boolean): User?

    /**
     * Check if phone number exists
     */
    fun existsByPhoneNumber(phoneNumber: String): Boolean

    /**
     * Find users by role
     */
    fun findByRole(role: UserRole): List<User>

    /**
     * Find users by role with pagination
     */
    fun findByRole(role: UserRole, pageable: Pageable): Page<User>

    /**
     * Find active users by role
     */
    fun findByRoleAndIsActive(role: UserRole, isActive: Boolean): List<User>

    /**
     * Find users by active status
     */
    fun findByIsActive(isActive: Boolean): List<User>

    /**
     * Find users by phone verification status
     */
    fun findByIsPhoneVerified(isPhoneVerified: Boolean): List<User>

    /**
     * Find users created within a date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<User>

    /**
     * Find users with failed login attempts above threshold
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
    fun findUsersWithFailedAttempts(@Param("threshold") threshold: Int): List<User>

    /**
     * Find currently locked accounts
     */
    @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil > :currentTime")
    fun findLockedAccounts(@Param("currentTime") currentTime: LocalDateTime): List<User>

    /**
     * Find users who haven't logged in recently
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate")
    fun findInactiveUsers(@Param("cutoffDate") cutoffDate: LocalDateTime): List<User>

    /**
     * Count users by role
     */
    fun countByRole(role: UserRole): Long

    /**
     * Count active users
     */
    fun countByIsActive(isActive: Boolean): Long

    /**
     * Count verified users
     */
    fun countByIsPhoneVerified(isPhoneVerified: Boolean): Long

    /**
     * Search users by name (first name or last name)
     */
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun searchByName(@Param("searchTerm") searchTerm: String): List<User>

    /**
     * Search users by name with pagination
     */
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun searchByName(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<User>

    /**
     * Update user's last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.failedLoginAttempts = 0, u.accountLockedUntil = NULL WHERE u.id = :userId")
    fun updateLastLogin(@Param("userId") userId: Long, @Param("loginTime") loginTime: LocalDateTime): Int

    /**
     * Increment failed login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    fun incrementFailedLoginAttempts(@Param("userId") userId: Long): Int

    /**
     * Lock user account until specified time
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLockedUntil = :lockUntil WHERE u.id = :userId")
    fun lockAccount(@Param("userId") userId: Long, @Param("lockUntil") lockUntil: LocalDateTime): Int

    /**
     * Unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLockedUntil = NULL, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    fun unlockAccount(@Param("userId") userId: Long): Int

    /**
     * Verify user's phone number
     */
    @Modifying
    @Query("UPDATE User u SET u.isPhoneVerified = true WHERE u.id = :userId")
    fun verifyPhoneNumber(@Param("userId") userId: Long): Int

    /**
     * Deactivate user account
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.id = :userId")
    fun deactivateUser(@Param("userId") userId: Long): Int

    /**
     * Activate user account
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = true WHERE u.id = :userId")
    fun activateUser(@Param("userId") userId: Long): Int

    /**
     * Clean up expired account locks
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLockedUntil = NULL WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil <= :currentTime")
    fun cleanupExpiredLocks(@Param("currentTime") currentTime: LocalDateTime): Int

    /**
     * Get user statistics
     */
    @Query("""
        SELECT
            COUNT(u) as totalUsers,
            COUNT(CASE WHEN u.isActive = true THEN 1 END) as activeUsers,
            COUNT(CASE WHEN u.isPhoneVerified = true THEN 1 END) as verifiedUsers,
            COUNT(CASE WHEN u.role = 'CUSTOMER' THEN 1 END) as customers,
            COUNT(CASE WHEN u.role = 'EMPLOYEE' THEN 1 END) as employees,
            COUNT(CASE WHEN u.role = 'STATION_ADMIN' THEN 1 END) as stationAdmins,
            COUNT(CASE WHEN u.role = 'SYSTEM_ADMIN' THEN 1 END) as systemAdmins
        FROM User u
    """)
    fun getUserStatistics(): Map<String, Long>
}
