package com.gasolinerajsm.authservice.domain.model

import com.gasolinerajsm.authservice.domain.event.DomainEvent
import com.gasolinerajsm.authservice.domain.event.UserCreatedEvent
import com.gasolinerajsm.authservice.domain.event.UserLoggedInEvent
import com.gasolinerajsm.authservice.domain.event.UserPhoneVerifiedEvent
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import com.gasolinerajsm.authservice.domain.valueobject.UserId
import java.time.LocalDateTime

/**
 * User Domain Entity - Core business logic
 * This is the heart of the hexagonal architecture
 */
data class User(
    val id: UserId,
    val phoneNumber: PhoneNumber,
    val firstName: String,
    val lastName: String,
    val role: UserRole = UserRole.CUSTOMER,
    val isActive: Boolean = true,
    val isPhoneVerified: Boolean = false,
    val lastLoginAt: LocalDateTime? = null,
    val failedLoginAttempts: Int = 0,
    val accountLockedUntil: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new user
         */
        fun create(
            phoneNumber: PhoneNumber,
            firstName: String,
            lastName: String,
            role: UserRole = UserRole.CUSTOMER
        ): User {
            val user = User(
                id = UserId.generate(),
                phoneNumber = phoneNumber,
                firstName = firstName,
                lastName = lastName,
                role = role
            )

            user.addDomainEvent(
                UserCreatedEvent(
                    userId = user.id,
                    phoneNumber = user.phoneNumber,
                    role = user.role,
                    occurredAt = LocalDateTime.now()
                )
            )

            return user
        }
    }

    /**
     * Get the user's full name
     */
    fun getFullName(): String = "$firstName $lastName"

    /**
     * Check if the account is currently locked
     */
    fun isAccountLocked(): Boolean {
        return accountLockedUntil?.isAfter(LocalDateTime.now()) == true
    }

    /**
     * Check if the user can attempt login
     */
    fun canAttemptLogin(): Boolean {
        return isActive && isPhoneVerified && !isAccountLocked()
    }

    /**
     * Record a successful login
     */
    fun recordSuccessfulLogin(): User {
        val updatedUser = this.copy(
            lastLoginAt = LocalDateTime.now(),
            failedLoginAttempts = 0,
            accountLockedUntil = null,
            updatedAt = LocalDateTime.now()
        )

        updatedUser.addDomainEvent(
            UserLoggedInEvent(
                userId = id,
                loginAt = LocalDateTime.now()
            )
        )

        return updatedUser
    }

    /**
     * Record a failed login attempt
     */
    fun recordFailedLogin(maxAttempts: Int, lockoutMinutes: Long): User {
        val newFailedAttempts = failedLoginAttempts + 1
        val lockUntil = if (newFailedAttempts >= maxAttempts) {
            LocalDateTime.now().plusMinutes(lockoutMinutes)
        } else {
            accountLockedUntil
        }

        return this.copy(
            failedLoginAttempts = newFailedAttempts,
            accountLockedUntil = lockUntil,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Verify the user's phone number
     */
    fun verifyPhone(): User {
        if (isPhoneVerified) {
            throw IllegalStateException("Phone number is already verified")
        }

        val updatedUser = this.copy(
            isPhoneVerified = true,
            updatedAt = LocalDateTime.now()
        )

        updatedUser.addDomainEvent(
            UserPhoneVerifiedEvent(
                userId = id,
                phoneNumber = phoneNumber,
                verifiedAt = LocalDateTime.now()
            )
        )

        return updatedUser
    }

    /**
     * Deactivate the user account
     */
    fun deactivate(): User {
        return this.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Activate the user account
     */
    fun activate(): User {
        return this.copy(
            isActive = true,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update user profile
     */
    fun updateProfile(firstName: String, lastName: String): User {
        return this.copy(
            firstName = firstName,
            lastName = lastName,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Check if user has specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return role.hasPermission(permission)
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "User(id=$id, phoneNumber=$phoneNumber, firstName='$firstName', lastName='$lastName', role=$role, isActive=$isActive, isPhoneVerified=$isPhoneVerified)"
    }
}