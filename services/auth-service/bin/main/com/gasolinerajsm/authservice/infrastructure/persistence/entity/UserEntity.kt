package com.gasolinerajsm.authservice.infrastructure.persistence.entity

import com.gasolinerajsm.authservice.domain.model.UserRole
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.*

/**
 * JPA Entity for User persistence
 * This is the infrastructure layer representation
 */
@Entity
@Table(
    name = "users",
    schema = "auth_service",
    indexes = [
        Index(name = "idx_users_phone_number", columnList = "phone_number"),
        Index(name = "idx_users_role", columnList = "role"),
        Index(name = "idx_users_active", columnList = "is_active"),
        Index(name = "idx_users_created_at", columnList = "created_at")
    ]
)
data class UserEntity(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID,

    @Column(name = "phone_number", unique = true, nullable = false, length = 20)
    val phoneNumber: String,

    @Column(name = "first_name", nullable = false, length = 100)
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    val lastName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: UserRole = UserRole.CUSTOMER,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "is_phone_verified", nullable = false)
    val isPhoneVerified: Boolean = false,

    @Column(name = "last_login_at")
    val lastLoginAt: LocalDateTime? = null,

    @Column(name = "failed_login_attempts", nullable = false)
    val failedLoginAttempts: Int = 0,

    @Column(name = "account_locked_until")
    val accountLockedUntil: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // JPA requires no-arg constructor
    constructor() : this(
        id = UUID.randomUUID(),
        phoneNumber = "",
        firstName = "",
        lastName = "",
        role = UserRole.CUSTOMER,
        isActive = true,
        isPhoneVerified = false,
        lastLoginAt = null,
        failedLoginAttempts = 0,
        accountLockedUntil = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}