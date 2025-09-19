package com.gasolinerajsm.authservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_phone_number", columnList = "phone_number"),
        Index(name = "idx_users_role", columnList = "role"),
        Index(name = "idx_users_active", columnList = "is_active"),
        Index(name = "idx_users_created_at", columnList = "created_at")
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "phone_number", unique = true, nullable = false, length = 20)
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    val phoneNumber: String,

    @Column(name = "first_name", nullable = false, length = 100)
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    val lastName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: UserRole = UserRole.CUSTOMER,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "is_phone_verified", nullable = false)
    val isPhoneVerified: Boolean = false,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,

    @Column(name = "account_locked_until")
    var accountLockedUntil: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

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
        return this.copy(
            lastLoginAt = LocalDateTime.now(),
            failedLoginAttempts = 0,
            accountLockedUntil = null
        )
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
            accountLockedUntil = lockUntil
        )
    }

    /**
     * Verify the user's phone number
     */
    fun verifyPhone(): User {
        return this.copy(isPhoneVerified = true)
    }

    /**
     * Deactivate the user account
     */
    fun deactivate(): User {
        return this.copy(isActive = false)
    }

    /**
     * Activate the user account
     */
    fun activate(): User {
        return this.copy(isActive = true)
    }

    override fun toString(): String {
        return "User(id=$id, phoneNumber='$phoneNumber', firstName='$firstName', lastName='$lastName', role=$role, isActive=$isActive, isPhoneVerified=$isPhoneVerified)"
    }
}

/**
 * User roles in the system
 */
enum class UserRole(val displayName: String, val permissions: Set<String>) {
    CUSTOMER(
        "Customer",
        setOf("coupon:redeem", "raffle:participate", "ad:view", "profile:view", "profile:update")
    ),
    EMPLOYEE(
        "Employee",
        setOf("coupon:validate", "redemption:process", "station:view", "profile:view", "profile:update")
    ),
    STATION_ADMIN(
        "Station Administrator",
        setOf(
            "coupon:validate", "redemption:process", "redemption:view",
            "station:view", "station:update", "employee:manage",
            "profile:view", "profile:update", "analytics:station"
        )
    ),
    SYSTEM_ADMIN(
        "System Administrator",
        setOf(
            "user:manage", "station:manage", "campaign:manage", "coupon:manage",
            "raffle:manage", "ad:manage", "analytics:system", "system:configure"
        )
    );

    /**
     * Check if this role has a specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Check if this role can access admin features
     */
    fun isAdmin(): Boolean {
        return this == STATION_ADMIN || this == SYSTEM_ADMIN
    }

    /**
     * Check if this role can manage stations
     */
    fun canManageStations(): Boolean {
        return this == SYSTEM_ADMIN
    }

    /**
     * Check if this role can process redemptions
     */
    fun canProcessRedemptions(): Boolean {
        return this == EMPLOYEE || this == STATION_ADMIN
    }
}