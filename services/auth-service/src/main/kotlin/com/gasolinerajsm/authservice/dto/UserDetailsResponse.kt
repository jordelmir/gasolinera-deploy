package com.gasolinerajsm.authservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.model.UserRole
import java.time.LocalDateTime

/**
 * Response DTO for user details
 */
data class UserDetailsResponse(
    @JsonProperty("user_id")
    val userId: Long,

    @JsonProperty("phone_number")
    val phoneNumber: String,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("full_name")
    val fullName: String,

    @JsonProperty("role")
    val role: UserRole,

    @JsonProperty("permissions")
    val permissions: Set<String>,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("is_phone_verified")
    val isPhoneVerified: Boolean,

    @JsonProperty("last_login_at")
    val lastLoginAt: LocalDateTime?,

    @JsonProperty("account_status")
    val accountStatus: String,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        /**
         * Creates a UserDetailsResponse from a User entity
         */
        fun fromUser(user: User): UserDetailsResponse {
            return UserDetailsResponse(
                userId = user.id,
                phoneNumber = user.phoneNumber,
                firstName = user.firstName,
                lastName = user.lastName,
                fullName = user.getFullName(),
                role = user.role,
                permissions = user.role.permissions,
                isActive = user.isActive,
                isPhoneVerified = user.isPhoneVerified,
                lastLoginAt = user.lastLoginAt,
                accountStatus = when {
                    !user.isActive -> "inactive"
                    !user.isPhoneVerified -> "unverified"
                    user.isAccountLocked() -> "locked"
                    else -> "active"
                },
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
