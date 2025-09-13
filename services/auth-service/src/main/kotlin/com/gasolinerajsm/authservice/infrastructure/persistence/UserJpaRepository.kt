package com.gasolinerajsm.authservice.infrastructure.persistence

import com.gasolinerajsm.authservice.domain.model.User
import com.gasolinerajsm.authservice.domain.model.UserRole
import com.gasolinerajsm.authservice.domain.repository.UserRepository
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import com.gasolinerajsm.authservice.domain.valueobject.UserId
import com.gasolinerajsm.authservice.infrastructure.persistence.entity.UserEntity
import com.gasolinerajsm.authservice.infrastructure.persistence.jpa.UserJpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA implementation of UserRepository (Adapter)
 * Converts between domain objects and JPA entities
 */
@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository
) : UserRepository {

    override suspend fun save(user: User): Result<User> {
        return try {
            val entity = user.toEntity()
            val savedEntity = jpaRepository.save(entity)
            Result.success(savedEntity.toDomain())
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to save user: ${e.message}", e))
        }
    }

    override suspend fun findById(id: UserId): Result<User?> {
        return try {
            val entity = jpaRepository.findById(id.value)
            Result.success(entity.orElse(null)?.toDomain())
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to find user by ID: ${e.message}", e))
        }
    }

    override suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): Result<User?> {
        return try {
            val entity = jpaRepository.findByPhoneNumber(phoneNumber.value)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to find user by phone number: ${e.message}", e))
        }
    }

    override suspend fun existsByPhoneNumber(phoneNumber: PhoneNumber): Result<Boolean> {
        return try {
            val exists = jpaRepository.existsByPhoneNumber(phoneNumber.value)
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to check user existence: ${e.message}", e))
        }
    }

    override suspend fun findAllActive(): Result<List<User>> {
        return try {
            val entities = jpaRepository.findByIsActiveTrue()
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to find active users: ${e.message}", e))
        }
    }

    override suspend fun findByRole(role: UserRole): Result<List<User>> {
        return try {
            val entities = jpaRepository.findByRole(role)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to find users by role: ${e.message}", e))
        }
    }

    override suspend fun deleteById(id: UserId): Result<Unit> {
        return try {
            jpaRepository.deleteById(id.value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to delete user: ${e.message}", e))
        }
    }

    override suspend fun count(): Result<Long> {
        return try {
            val count = jpaRepository.count()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to count users: ${e.message}", e))
        }
    }

    override suspend fun countActive(): Result<Long> {
        return try {
            val count = jpaRepository.countByIsActiveTrue()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to count active users: ${e.message}", e))
        }
    }
}

/**
 * Extension functions for domain-entity conversion
 */
private fun User.toEntity(): UserEntity {
    return UserEntity(
        id = this.id.value,
        phoneNumber = this.phoneNumber.value,
        firstName = this.firstName,
        lastName = this.lastName,
        role = this.role,
        isActive = this.isActive,
        isPhoneVerified = this.isPhoneVerified,
        lastLoginAt = this.lastLoginAt,
        failedLoginAttempts = this.failedLoginAttempts,
        accountLockedUntil = this.accountLockedUntil,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

private fun UserEntity.toDomain(): User {
    return User(
        id = UserId.from(this.id),
        phoneNumber = PhoneNumber.from(this.phoneNumber),
        firstName = this.firstName,
        lastName = this.lastName,
        role = this.role,
        isActive = this.isActive,
        isPhoneVerified = this.isPhoneVerified,
        lastLoginAt = this.lastLoginAt,
        failedLoginAttempts = this.failedLoginAttempts,
        accountLockedUntil = this.accountLockedUntil,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}