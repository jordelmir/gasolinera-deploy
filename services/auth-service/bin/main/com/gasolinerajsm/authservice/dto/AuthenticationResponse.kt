package com.gasolinerajsm.authservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTOs for authentication operations
 */

/**
 * Response for successful OTP generation
 */
data class OtpGenerationResponse(
    @JsonProperty("success")
    val success: Boolean = true,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("expires_at")
    val expiresAt: LocalDateTime,

    @JsonProperty("attempts_remaining")
    val attemptsRemaining: Int,

    @JsonProperty("retry_after_seconds")
    val retryAfterSeconds: Long? = null
)

/**
 * Response for authentication operations (login, registration)
 */
data class AuthenticationResponse(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("user")
    val user: UserDetailsResponse? = null,

    @JsonProperty("tokens")
    val tokens: TokenResponse? = null,

    @JsonProperty("requires_verification")
    val requiresVerification: Boolean = false,

    @JsonProperty("verification_method")
    val verificationMethod: String? = null
)

/**
 * Response for logout operations
 */
data class LogoutResponse(
    @JsonProperty("success")
    val success: Boolean = true,

    @JsonProperty("message")
    val message: String = "Logged out successfully",

    @JsonProperty("logged_out_at")
    val loggedOutAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("data")
    val data: T? = null,

    @JsonProperty("error_code")
    val errorCode: String? = null,

    @JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("request_id")
    val requestId: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String = "Operation successful"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message,
                data = data
            )
        }

        fun <T> success(message: String = "Operation successful"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message,
                data = null
            )
        }

        fun <T> error(message: String, errorCode: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                message = message,
                data = null,
                errorCode = errorCode
            )
        }
    }
}

/**
 * Error response for validation failures
 */
data class ValidationErrorResponse(
    @JsonProperty("success")
    val success: Boolean = false,

    @JsonProperty("message")
    val message: String = "Validation failed",

    @JsonProperty("errors")
    val errors: Map<String, List<String>>,

    @JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Response for account status checks
 */
data class AccountStatusResponse(
    @JsonProperty("phone_number")
    val phoneNumber: String,

    @JsonProperty("exists")
    val exists: Boolean,

    @JsonProperty("is_active")
    val isActive: Boolean = false,

    @JsonProperty("is_verified")
    val isVerified: Boolean = false,

    @JsonProperty("is_locked")
    val isLocked: Boolean = false,

    @JsonProperty("can_login")
    val canLogin: Boolean = false,

    @JsonProperty("lockout_expires_at")
    val lockoutExpiresAt: LocalDateTime? = null,

    @JsonProperty("registration_required")
    val registrationRequired: Boolean = false
)