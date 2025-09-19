package com.gasolinerajsm.authservice.domain.repository

import com.gasolinerajsm.authservice.domain.model.User
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import com.gasolinerajsm.authservice.domain.valueobject.UserId

/**
 * User Repository Interface (Port)
 * Defines the contract for user persistence without implementation details
 */
interface UserRepository {

    /**
     * Save a user entity
     */
    suspend fun save(user: User): Result<User>

    /**
     * Find user by ID
     */
    suspend fun findById(id: UserId): Result<User?>

    /**
     * Find user by phone number
     */
    suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): Result<User?>

    /**
     * Check if user exists by phone number
     */
    suspend fun existsByPhoneNumber(phoneNumber: PhoneNumber): Result<Boolean>

    /**
     * Find all active users
     */
    suspend fun findAllActive(): Result<List<User>>

    /**
     * Find users by role
     */
    suspend fun findByRole(role: com.gasolinerajsm.authservice.domain.model.UserRole): Result<List<User>>

    /**
     * Delete user by ID
     */
    suspend fun deleteById(id: UserId): Result<Unit>

    /**
     * Count total users
     */
    suspend fun count(): Result<Long>

    /**
     * Count active users
     */
    suspend fun countActive(): Result<Long>
}