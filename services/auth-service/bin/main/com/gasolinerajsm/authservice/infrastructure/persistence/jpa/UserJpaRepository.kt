package com.gasolinerajsm.authservice.infrastructure.persistence.jpa

import com.gasolinerajsm.authservice.domain.model.UserRole
import com.gasolinerajsm.authservice.infrastructure.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Spring Data JPA Repository for UserEntity
 */
@Repository
interface UserJpaRepository : JpaRepository<UserEntity, UUID> {

    /**
     * Find user by phone number
     */
    fun findByPhoneNumber(phoneNumber: String): UserEntity?

    /**
     * Check if user exists by phone number
     */
    fun existsByPhoneNumber(phoneNumber: String): Boolean

    /**
     * Find all active users
     */
    fun findByIsActiveTrue(): List<UserEntity>

    /**
     * Find users by role
     */
    fun findByRole(role: UserRole): List<UserEntity>

    /**
     * Count active users
     */
    fun countByIsActiveTrue(): Long

    /**
     * Find users by role and active status
     */
    fun findByRoleAndIsActive(role: UserRole, isActive: Boolean): List<UserEntity>

    /**
     * Find users created after a specific date
     */
    @Query("SELECT u FROM UserEntity u WHERE u.createdAt >= :fromDate ORDER BY u.createdAt DESC")
    fun findUsersCreatedAfter(@Param("fromDate") fromDate: java.time.LocalDateTime): List<UserEntity>

    /**
     * Find users with failed login attempts above threshold
     */
    @Query("SELECT u FROM UserEntity u WHERE u.failedLoginAttempts >= :threshold AND u.isActive = true")
    fun findUsersWithFailedAttempts(@Param("threshold") threshold: Int): List<UserEntity>

    /**
     * Find locked users
     */
    @Query("SELECT u FROM UserEntity u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil > CURRENT_TIMESTAMP")
    fun findLockedUsers(): List<UserEntity>

    /**
     * Find users by phone number pattern (for admin search)
     */
    @Query("SELECT u FROM UserEntity u WHERE u.phoneNumber LIKE %:pattern%")
    fun findByPhoneNumberContaining(@Param("pattern") pattern: String): List<UserEntity>

    /**
     * Find users by name pattern (for admin search)
     */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByNameContaining(@Param("name") name: String): List<UserEntity>
}