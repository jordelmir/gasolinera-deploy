package com.gasolinerajsm.auth.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDateTime
import java.util.*

/**
 * Data Transfer Objects for Authentication API
 * Comprehensive DTOs with OpenAPI documentation and validation
 */

// Request DTOs
@Schema(
    name = "UserRegistrationRequest",
    description = "Request payload for user registration",
    example = """{
        "email": "juan.perez@example.com",
        "phone": "5551234567",
        "firstName": "Juan",
        "lastName": "Pérez",
        "password": "SecurePassword123!",
        "acceptTerms": true,
        "marketingConsent": false
    }"""
)
data class UserRegistrationRequest(
    @field:Email(message = "Please provide a valid email address")
    @field:NotBlank(message = "Email is required")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(
        description = "User's email address (must be unique)",
        example = "juan.perez@example.com",
        format = "email",
        maxLength = 255
    )
    val email: String,

    @field:Pattern(
        regexp = "^[0-9]{10,12}$",
        message = "Phone number must be 10-12 digits without spaces or special characters"
    )
    @field:NotBlank(message = "Phone number is required")
    @Schema(
        description = "User's phone number (10-12 digits, must be unique)",
        example = "5551234567",
        pattern = "^[0-9]{10,12}$"
    )
    val phone: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
        message = "First name can only contain letters, spaces, apostrophes, and hyphens"
    )
    @Schema(
        description = "User's first name",
        example = "Juan",
        minLength = 2,
        maxLength = 50
    )
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
        message = "Last name can only contain letters, spaces, apostrophes, and hyphens"
    )
    @Schema(
        description = "User's last name",
        example = "Pérez",
        minLength = 2,
        maxLength = 50
    )
    val lastName: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(
        description = "User's password (must meet security requirements)",
        example = "SecurePassword123!",
        minLength = 8,
        maxLength = 128,
        format = "password"
    )
    val password: String,

    @field:AssertTrue(message = "You must accept the terms and conditions")
    @Schema(
        description = "User must accept terms and conditions",
        example = "true",
        required = true
    )
    val acceptTerms: Boolean = false,

    @Schema(
        description = "Optional consent for marketing communications",
        example = "false",
        defaultValue = "false"
    )
    val marketingConsent: Boolean = false,

    @Schema(
        description = "Optional referral code from existing user",
        example = "REF123456",
        maxLength = 20
    )
    val referralCode: String? = null
)

@Schema(
    name = "UserLoginRequest",
    description = "Request payload for user authentication",
    example = """{
        "identifier": "juan.perez@example.com",
        "password": "SecurePassword123!",
        "rememberMe": true,
        "deviceInfo": {
            "deviceId": "device-123",
            "deviceName": "iPhone 13",
            "platform": "iOS",
            "version": "15.0"
        }
    }"""
)
data class UserLoginRequest(
    @field:NotBlank(message = "Email or phone number is required")
    @field:Size(max = 255, message = "Identifier must not exceed 255 characters")
    @Schema(
        description = "User's email address or phone number",
        example = "juan.perez@example.com",
        maxLength = 255
    )
    val identifier: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(max = 128, message = "Password must not exceed 128 characters")
    @Schema(
        description = "User's password",
        example = "SecurePassword123!",
        format = "password",
        maxLength = 128
    )
    val password: String,

    @Schema(
        description = "Whether to extend session duration",
        example = "true",
        defaultValue = "false"
    )
    val rememberMe: Boolean = false,

    @Schema(
        description = "Optional device information for session tracking",
        implementation = DeviceInfo::class
    )
    val deviceInfo: DeviceInfo? = null
)

@Schema(
    name = "TokenRefreshRequest",
    description = "Request payload for token refresh",
    example = """{
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }"""
)
data class TokenRefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
    @Schema(
        description = "Valid refresh token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        format = "jwt"
    )
    val refreshToken: String
)

@Schema(
    name = "UpdateProfileRequest",
    description = "Request payload for profile updates",
    example = """{
        "firstName": "Juan Carlos",
        "lastName": "Pérez García",
        "phone": "5559876543",
        "preferences": {
            "language": "es",
            "timezone": "America/Mexico_City",
            "notifications": {
                "email": true,
                "sms": false,
                "push": true
            }
        }
    }"""
)
data class UpdateProfileRequest(
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
        message = "First name can only contain letters, spaces, apostrophes, and hyphens"
    )
    @Schema(
        description = "Updated first name",
        example = "Juan Carlos",
        minLength = 2,
        maxLength = 50
    )
    val firstName: String? = null,

    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
        message = "Last name can only contain letters, spaces, apostrophes, and hyphens"
    )
    @Schema(
        description = "Updated last name",
        example = "Pérez García",
        minLength = 2,
        maxLength = 50
    )
    val lastName: String? = null,

    @field:Pattern(
        regexp = "^[0-9]{10,12}$",
        message = "Phone number must be 10-12 digits without spaces or special characters"
    )
    @Schema(
        description = "Updated phone number (requires re-verification)",
        example = "5559876543",
        pattern = "^[0-9]{10,12}$"
    )
    val phone: String? = null,

    @Schema(
        description = "User preferences and settings",
        implementation = UserPreferences::class
    )
    val preferences: UserPreferences? = null
)

// Response DTOs
@Schema(
    name = "UserRegistrationResponse",
    description = "Response payload for successful user registration"
)
data class UserRegistrationResponse(
    @Schema(
        description = "Unique identifier for the newly created user",
        example = "123e4567-e89b-12d3-a456-426614174000",
        format = "uuid"
    )
    val userId: UUID,

    @Schema(
        description = "Registered email address",
        example = "juan.perez@example.com",
        format = "email"
    )
    val email: String,

    @Schema(
        description = "Success message with next steps",
        example = "User registered successfully. Please check your email for verification."
    )
    val message: String,

    @Schema(
        description = "Whether email verification is required",
        example = "true"
    )
    val emailVerificationRequired: Boolean,

    @Schema(
        description = "List of recommended next steps",
        example = "[\"Verify your email address\", \"Complete your profile\", \"Start using the platform\"]"
    )
    val nextSteps: List<String> = listOf(
        "Verify your email address",
        "Complete your profile",
        "Start using the platform"
    ),

    @Schema(
        description = "Referral bonus information (if applicable)",
        implementation = ReferralBonus::class
    )
    val referralBonus: ReferralBonus? = null
)

@Schema(
    name = "UserLoginResponse",
    description = "Response payload for successful user authentication"
)
data class UserLoginResponse(
    @Schema(
        description = "JWT access token for API authentication",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        format = "jwt"
    )
    val accessToken: String,

    @Schema(
        description = "JWT refresh token for token renewal",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        format = "jwt"
    )
    val refreshToken: String,

    @Schema(
        description = "Token type (always 'Bearer')",
        example = "Bearer",
        defaultValue = "Bearer"
    )
    val tokenType: String = "Bearer",

    @Schema(
        description = "Access token expiration time in seconds",
        example = "3600"
    )
    val expiresIn: Long,

    @Schema(
        description = "Authenticated user information",
        implementation = UserInfo::class
    )
    val user: UserInfo? = null,

    @Schema(
        description = "Error message (only present if login failed)",
        example = null
    )
    val error: String? = null,

    @Schema(
        description = "Session information",
        implementation = SessionInfo::class
    )
    val session: SessionInfo? = null
)

@Schema(
    name = "UserProfileResponse",
    description = "Response payload containing user profile information"
)
data class UserProfileResponse(
    @Schema(
        description = "User's unique identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        format = "uuid"
    )
    val id: UUID,

    @Schema(
        description = "User's email address",
        example = "juan.perez@example.com",
        format = "email"
    )
    val email: String,

    @Schema(
        description = "User's phone number",
        example = "5551234567"
    )
    val phone: String,

    @Schema(
        description = "User's first name",
        example = "Juan"
    )
    val firstName: String,

    @Schema(
        description = "User's last name",
        example = "Pérez"
    )
    val lastName: String,

    @Schema(
        description = "Whether email is verified",
        example = "true"
    )
    val isEmailVerified: Boolean,

    @Schema(
        description = "Whether phone is verified",
        example = "true"
    )
    val isPhoneVerified: Boolean,

    @Schema(
        description = "User's roles in the system",
        example = "[\"USER\"]"
    )
    val roles: List<String>,

    @Schema(
        description = "User's permissions",
        example = "[\"READ_COUPONS\", \"REDEEM_COUPONS\", \"PARTICIPATE_RAFFLES\"]"
    )
    val permissions: List<String>,

    @Schema(
        description = "Account creation timestamp",
        example = "2024-01-01T00:00:00Z",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val createdAt: LocalDateTime,

    @Schema(
        description = "Last login timestamp",
        example = "2024-01-15T10:30:00Z",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val lastLoginAt: LocalDateTime? = null,

    @Schema(
        description = "User preferences and settings",
        implementation = UserPreferences::class
    )
    val preferences: UserPreferences? = null,

    @Schema(
        description = "User statistics and activity summary",
        implementation = UserStatistics::class
    )
    val statistics: UserStatistics? = null
)

// Supporting DTOs
@Schema(
    name = "UserInfo",
    description = "Basic user information included in authentication responses"
)
data class UserInfo(
    @Schema(descripti"User's unique identifier", format = "uuid")
    val id: UUID,

    @Schema(description = "User's email address", format = "email")
    val email: String,

    @Schema(description = "User's first name")
    val firstName: String,

    @Schema(description = "User's last name")
    val lastName: String,

    @Schema(description = "Whether email is verified")
    val isEmailVerified: Boolean,

    @Schema(description = "Whether phone is verified")
    val isPhoneVerified: Boolean,

    @Schema(description = "User's roles")
    val roles: List<String> = emptyList(),

    @Schema(description = "User's permissions")
    val permissions: List<String> = emptyList()
)

@Schema(
    name = "DeviceInfo",
    description = "Device information for session tracking"
)
data class DeviceInfo(
    @Schema(description = "Unique device identifier", example = "device-123")
    val deviceId: String,

    @Schema(description = "Human-readable device name", example = "iPhone 13")
    val deviceName: String,

    @Schema(description = "Device platform", example = "iOS")
    val platform: String,

    @Schema(description = "Platform version", example = "15.0")
    val version: String,

    @Schema(description = "User agent string", example = "Mozilla/5.0...")
    val userAgent: String? = null
)

@Schema(
    name = "UserPreferences",
    description = "User preferences and settings"
)
data class UserPreferences(
    @Schema(description = "Preferred language code", example = "es")
    val language: String = "es",

    @Schema(description = "User's timezone", example = "America/Mexico_City")
    val timezone: String = "America/Mexico_City",

    @Schema(description = "Notification preferences")
    val notifications: NotificationPreferences = NotificationPreferences(),

    @Schema(description = "Privacy settings")
    val privacy: PrivacySettings = PrivacySettings()
)

@Schema(
    name = "NotificationPreferences",
    description = "User notification preferences"
)
data class NotificationPreferences(
    @Schema(description = "Email notifications enabled")
    val email: Boolean = true,

    @Schema(description = "SMS notifications enabled")
    val sms: Boolean = false,

    @Schema(description = "Push notifications enabled")
    val push: Boolean = true,

    @Schema(description = "Marketing communications enabled")
    val marketing: Boolean = false
)

@Schema(
    name = "PrivacySettings",
    description = "User privacy settings"
)
data class PrivacySettings(
    @Schema(description = "Profile visibility to other users")
    val profileVisibility: String = "PRIVATE",

    @Schema(description = "Allow data sharing for analytics")
    val dataSharing: Boolean = false,

    @Schema(description = "Allow location tracking")
    val locationTracking: Boolean = true
)

@Schema(
    name = "UserStatistics",
    description = "User activity statistics"
)
data class UserStatistics(
    @Schema(description = "Total number of coupons purchased")
    val totalCoupons: Int = 0,

    @Schema(description = "Total number of redemptions")
    val totalRedemptions: Int = 0,

    @Schema(description = "Total raffle participations")
    val totalRaffleParticipations: Int = 0,

    @Schema(description = "Current membership tier")
    val membershipTier: String = "BRONZE",

    @Schema(description = "Loyalty points balance")
    val loyaltyPoints: Int = 0
)

@Schema(
    name = "SessionInfo",
    description = "Session information"
)
data class SessionInfo(
    @Schema(description = "Session identifier")
    val sessionId: String,

    @Schema(description = "Session creation time", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val createdAt: LocalDateTime,

    @Schema(description = "Session expiration time", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val expiresAt: LocalDateTime,

    @Schema(description = "Device information")
    val device: DeviceInfo? = null
)

@Schema(
    name = "ReferralBonus",
    description = "Referral bonus information"
)
data class ReferralBonus(
    @Schema(description = "Bonus amount awarded")
    val amount: Int,

    @Schema(description = "Bonus type")
    val type: String,

    @Schema(description = "Bonus description")
    val description: String
)

// Additional request/response DTOs for other endpoints
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val error: String? = null
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128)
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    val newPassword: String
)

data class ChangePasswordResponse(
    val success: Boolean,
    val message: String
)

data class ForgotPasswordRequest(
    @field:Email
    @field:NotBlank
    val email: String
)

data class ForgotPasswordResponse(
    val message: String,
    val resetTokenSent: Boolean
)

data class ResetPasswordRequest(
    @field:NotBlank
    val resetToken: String,

    @field:NotBlank
    @field:Size(min = 8, max = 128)
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
    )
    val newPassword: String
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)

data class LogoutRequest(
    val allDevices: Boolean = false
)

data class LogoutResponse(
    val success: Boolean,
    val message: String,
    val sessionsTerminated: Int
)

@Schema(
    name = "ErrorResponse",
    description = "Standard error response format"
)
data class ErrorResponse(
    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    val error: String,

    @Schema(description = "Human-readable error message")
    val message: String,

    @Schema(description = "Additional error details")
    val details: Map<String, Any>? = null,

    @Schema(description = "Error timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Request path where error occurred")
    val path: String? = null,

    @Schema(description = "Trace ID for debugging")
    val traceId: String? = null
)
       "New password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(
        description = "User's new password (min 8 chars, must include uppercase, lowercase, number, and special character)",
        example = "NewSecurePassword456!",
        format = "password",
        minLength = 8,
        maxLength = 128
    )
    val newPassword: String
)

@Schema(
    name = "ForgotPasswordRequest",
    description = "Password reset request",
    example = """
    {
      "email": "juan.perez@example.com"
    }
    """
)
data class ForgotPasswordRequest(
    @field:Email(message = "Email format is invalid")
    @field:NotBlank(message = "Email is required")
    @Schema(
        description = "User's email address for password reset",
        example = "juan.perez@example.com",
        format = "email"
    )
    val email: String
)

@Schema(
    name = "ResetPasswordRequest",
    description = "Password reset completion data",
    example = """
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "newPassword": "NewSecurePassword456!"
    }
    """
)
data class ResetPasswordRequest(
    @field:NotBlank(message = "Reset token is required")
    @Schema(
        description = "Password reset token from email",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d)(?=.*[@$!%*?&])[A-Za-z\\\\d@$!%*?&]{8,}$",
        message = "New password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(
        description = "User's new password",
        example = "NewSecurePassword456!",
        format = "password",
        minLength = 8,
        maxLength = 128
    )
    val newPassword: String
)

@Schema(
    name = "EmailVerificationRequest",
    description = "Email verification token",
    example = """
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
    """
)
data class EmailVerificationRequest(
    @field:NotBlank(message = "Verification token is required")
    @Schema(
        description = "Email verification token from registration email",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val token: String
)

// Response DTOs
@Schema(
    name = "UserRegistrationResponse",
    description = "User registration response",
    example = """
    {
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "email": "juan.perez@example.com",
      "message": "User registered successfully. Please check your email for verification.",
      "emailVerificationRequired": true
    }
    """
)
data class UserRegistrationResponse(
    @Schema(
        description = "Unique user identifier",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    val userId: UUID,

    @Schema(
        description = "User's email address",
        example = "juan.perez@example.com"
    )
    val email: String,

    @Schema(
        description = "Registration status message",
        example = "User registered successfully. Please check your email for verification."
    )
    val message: String,

    @Schema(
        description = "Whether email verification is required",
        example = "true"
    )
    val emailVerificationRequired: Boolean,

    @Schema(
        description = "Error message if registration failed",
        example = null
    )
    val error: String? = null
)

@Schema(
    name = "LoginResponse",
    description = "Login response with tokens and user info",
    example = """
    {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600,
      "user": {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "email": "juan.perez@example.com",
        "firstName": "Juan",
        "lastName": "Pérez",
        "isEmailVerified": true,
        "isPhoneVerified": false
      }
    }
    """
)
data class LoginResponse(
    @Schema(
        description = "JWT access token for API authentication",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val accessToken: String,

    @Schema(
        description = "JWT refresh token for token renewal",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val refreshToken: String,

    @Schema(
        description = "Token type (always Bearer)",
        example = "Bearer"
    )
    val tokenType: String,

    @Schema(
        description = "Access token expiration time in seconds",
        example = "3600"
    )
    val expiresIn: Int,

    @Schema(
        description = "User information"
    )
    val user: UserInfo?,

    @Schema(
        description = "Error message if login failed",
        example = null
    )
    val error: String? = null
)

@Schema(
    name = "UserInfo",
    description = "Basic user information",
    example = """
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "email": "juan.perez@example.com",
      "firstName": "Juan",
      "lastName": "Pérez",
      "isEmailVerified": true,
      "isPhoneVerified": false
    }
    """
)
data class UserInfo(
    @Schema(
        description = "Unique user identifier",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    val id: UUID,

    @Schema(
        description = "User's email address",
        example = "juan.perez@example.com"
    )
    val email: String,

    @Schema(
        description = "User's first name",
        example = "Juan"
    )
    val firstName: String,

    @Schema(
        description = "User's last name",
        example = "Pérez"
    )
    val lastName: String,

    @Schema(
        description = "Whether user's email is verified",
        example = "true"
    )
    val isEmailVerified: Boolean,

    @Schema(
        description = "Whether user's phone is verified",
        example = "false"
    )
    val isPhoneVerified: Boolean
)

@Schema(
    name = "TokenRefreshResponse",
    description = "Token refresh response",
    example = """
    {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }
    """
)
data class TokenRefreshResponse(
    @Schema(
        description = "New JWT access token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val accessToken: String,

    @Schema(
        description = "New JWT refresh token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    val refreshToken: String,

    @Schema(
        description = "Token type (always Bearer)",
        example = "Bearer"
    )
    val tokenType: String,

    @Schema(
        description = "Access token expiration time in seconds",
        example = "3600"
    )
    val expiresIn: Int,

    @Schema(
        description = "Error message if refresh failed",
        example = null
    )
    val error: String? = null
)

@Schema(
    name = "LogoutResponse",
    description = "Logout response",
    example = """
    {
      "message": "Logout successful",
      "timestamp": "2024-01-15T10:30:00"
    }
    """
)
data class LogoutResponse(
    @Schema(
        description = "Logout status message",
        example = "Logout successful"
    )
    val message: String,

    @Schema(
        description = "Logout timestamp",
        example = "2024-01-15T10:30:00"
    )
    val timestamp: LocalDateTime
)

@Schema(
    name = "UserProfileResponse",
    description = "User profile information",
    example = """
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "email": "juan.perez@example.com",
      "phone": "5551234567",
      "firstName": "Juan",
      "lastName": "Pérez",
      "isEmailVerified": true,
      "isPhoneVerified": false,
      "createdAt": "2024-01-01T10:00:00",
      "lastLoginAt": "2024-01-15T09:30:00"
    }
    """
)
data class UserProfileResponse(
    @Schema(
        description = "Unique user identifier",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    val id: UUID,

    @Schema(
        description = "User's email address",
        example = "juan.perez@example.com"
    )
    val email: String,

    @Schema(
        description = "User's phone number",
        example = "5551234567"
    )
    val phone: String,

    @Schema(
        description = "User's first name",
        example = "Juan"
    )
    val firstName: String,

    @Schema(
        description = "User's last name",
        example = "Pérez"
    )
    val lastName: String,

    @Schema(
        description = "Whether user's email is verified",
        example = "true"
    )
    val isEmailVerified: Boolean,

    @Schema(
        description = "Whether user's phone is verified",
        example = "false"
    )
    val isPhoneVerified: Boolean,

    @Schema(
        description = "Account creation timestamp",
        example = "2024-01-01T10:00:00"
    )
    val createdAt: LocalDateTime,

    @Schema(
        description = "Last login timestamp",
        example = "2024-01-15T09:30:00"
    )
    val lastLoginAt: LocalDateTime?
)

@Schema(
    name = "PasswordChangeResponse",
    description = "Password change response",
    example = """
    {
      "message": "Password changed successfully. Please login again.",
      "timestamp": "2024-01-15T10:30:00",
      "requiresReauth": true
    }
    """
)
data class PasswordChangeResponse(
    @Schema(
        description = "Password change status message",
        example = "Password changed successfully. Please login again."
    )
    val message: String,

    @Schema(
        description = "Change timestamp",
        example = "2024-01-15T10:30:00"
    )
    val timestamp: LocalDateTime,

    @Schema(
        description = "Whether user needs to re-authenticate",
        example = "true"
    )
    val requiresReauth: Boolean,

    @Schema(
        description = "Error code if change failed",
        example = null
    )
    val error: String? = null
)

@Schema(
    name = "ForgotPasswordResponse",
    description = "Password reset initiation response",
    example = """
    {
      "message": "If the email exists in our system, a password reset link has been sent.",
      "timestamp": "2024-01-15T10:30:00"
    }
    """
)
data class ForgotPasswordResponse(
    @Schema(
        description = "Password reset status message",
        example = "If the email exists in our system, a password reset link has been sent."
    )
    val message: String,

    @Schema(
        description = "Request timestamp",
        example = "2024-01-15T10:30:00"
    )
    val timestamp: LocalDateTime
)

@Schema(
    name = "ResetPasswordResponse",
    description = "Password reset completion response",
    example = """
    {
      "message": "Password reset successful. You can now login with your new password.",
      "timestamp": "2024-01-15T10:30:00"
    }
    """
)
data class ResetPasswordResponse(
    @Schema(
        description = "Password reset status message",
        example = "Password reset successful. You can now login with your new password."
    )
    val message: String,

    @Schema(
        description = "Reset timestamp",
        example = "2024-01-15T10:30:00"
    )
    val timestamp: LocalDateTime,

    @Schema(
        description = "Error code if reset failed",
        example = null
    )
    val error: String? = null
)

@Schema(
    name = "EmailVerificationResponse",
    description = "Email verification response",
    example = """
    {
      "message": "Email verified successfully. Your account is now fully active.",
      "timestamp": "2024-01-15T10:30:00",
      "isVerified": true
    }
    """
)
data class EmailVerificationResponse(
    @Schema(
        description = "Email verification status message",
        example = "Email verified successfully. Your account is now fully active."
    )
    val message: String,

    @Schema(
        description = "Verification timestamp",
        example = "2024-01-15T10:30:00"
    )
    val timestamp: LocalDateTime,

    @Schema(
        description = "Whether email is now verified",
        example = "true"
    )
    val isVerified: Boolean,

    @Schema(
        description = "Error code if verification failed",
        example = null
    )
    val error: String? = null
)

// Error Response DTO
@Schema(
    name = "ErrorResponse",
    description = "Standard error response format",
    example = """
    {
      "error": "VALIDATION_ERROR",
      "message": "Invalid input data",
      "timestamp": "2024-01-15T10:30:00Z",
      "path": "/api/v1/auth/register",
      "details": {
        "email": "Email format is invalid",
        "password": "Password must contain at least 8 characters"
      }
    }
    """
)
data class ErrorResponse(
    @Schema(
        description = "Error code identifier",
        example = "VALIDATION_ERROR"
    )
    val error: String,

    @Schema(
        description = "Human-readable error message",
        example = "Invalid input data"
    )
    val message: String,

    @Schema(
        description = "Error timestamp",
        example = "2024-01-15T10:30:00Z"
    )
    val timestamp: String,

    @Schema(
        description = "API path where error occurred",
        example = "/api/v1/auth/register"
    )
    val path: String,

    @Schema(
        description = "Additional error details (field-specific validation errors)",
        example = """{"email": "Email format is invalid", "password": "Password must contain at least 8 characters"}"""
    )
    val details: Map<String, String>? = null
)

// Principal class for authentication
data class UserPrincipal(
    val userId: UUID,
    val email: String,
    val roles: Set<String> = emptySet()
)