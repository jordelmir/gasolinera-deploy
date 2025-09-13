package com.gasolinerajsm.authservice.infrastructure.web.dto

import com.gasolinerajsm.authservice.domain.model.UserRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request to register a new user
 */
data class RegisterUserRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    val phoneNumber: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    val lastName: String,

    val role: UserRole = UserRole.CUSTOMER
)

/**
 * Response for user registration
 */
data class RegisterUserResponse(
    val userId: String,
    val phoneNumber: String,
    val otpSent: Boolean,
    val message: String
)

/**
 * Request to login with OTP
 */
data class LoginRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    val phoneNumber: String,

    @field:NotBlank(message = "OTP code is required")
    @field:Pattern(
        regexp = "^\\d{6}$",
        message = "OTP code must be 6 digits"
    )
    val otpCode: String
)

/**
 * Request to get OTP
 */
data class RequestOtpRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    val phoneNumber: String
)

/**
 * Response for OTP request
 */
data class OtpResponse(
    val phoneNumber: String,
    val otpSent: Boolean,
    val expiresIn: Long, // seconds
    val message: String
)

/**
 * Authentication response with tokens and user info
 */
data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserDetailsResponse
)

/**
 * User details in authentication response
 */
data class UserDetailsResponse(
    val id: String,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val permissions: List<String>
)

/**
 * Request to refresh token
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

/**
 * Response for token refresh
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val error: String? = null,
    val timestamp: String = java.time.LocalDateTime.now().toString()
) {
    companion object {
        fun <T> success(data: T, message: String = "Success"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message,
                data = data
            )
        }

        fun <T> error(error: String, message: String = "Error"): ApiResponse<T> {
            return ApiResponse(
                success = false,
                message = message,
                error = error
            )
        }
    }
}