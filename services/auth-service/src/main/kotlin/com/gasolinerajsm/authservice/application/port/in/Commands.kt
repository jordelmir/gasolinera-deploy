package com.gasolinerajsm.authservice.application.port.`in`

import com.gasolinerajsm.authservice.domain.model.UserRole

/**
 * Command to register a new user
 */
data class RegisterUserCommand(
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole = UserRole.CUSTOMER
)

/**
 * Command to authenticate a user with OTP
 */
data class AuthenticateUserCommand(
    val phoneNumber: String,
    val otpCode: String
)

/**
 * Command to request OTP for login
 */
data class RequestOtpCommand(
    val phoneNumber: String
)

/**
 * Command to verify phone number with OTP
 */
data class VerifyPhoneCommand(
    val phoneNumber: String,
    val otpCode: String
)

/**
 * Command to refresh JWT token
 */
data class RefreshTokenCommand(
    val refreshToken: String
)

/**
 * Command to update user profile
 */
data class UpdateUserProfileCommand(
    val userId: String,
    val firstName: String?,
    val lastName: String?
)

/**
 * Command to deactivate user account
 */
data class DeactivateUserCommand(
    val userId: String,
    val deactivatedBy: String?,
    val reason: String?
)

/**
 * Command to reactivate user account
 */
data class ReactivateUserCommand(
    val userId: String,
    val reactivatedBy: String?
)