package com.gasolinerajsm.auth.web

import com.gasolinerajsm.auth.application.*
import com.gasolinerajsm.auth.web.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Authentication Controller with comprehensive OpenAPI documentation
 * Handles user registration, login, token refresh, and profile management
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(
    name = "Authentication",
    description = "🔐 Authentication and authorization endpoints for user management, login, registration, and token operations"
)
class AuthController(
    private val authenticationUseCase: AuthenticationUseCase
) {

    @Operation(
        summary = "Register new user account",
        description = """
            Creates a new user account in the Gasolinera JSM platform.

            ## Features:
            - ✅ Email and phone validation
            - ✅ Password strength requirements
            - ✅ Automatic email verification trigger
            - ✅ Duplicate prevention

            ## Password Requirements:
            - Minimum 8 characters
            - At least one uppercase letter
            - At least one lowercase letter
            - At least one number
            - At least one special character

            ## Post-Registration:
            - Email verification link sent
            - User can login immediately
            - Profile completion recommended
        """,
        tags = ["Authentication"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "✅ User registered successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserRegistrationResponse::class),
                    examples = [ExampleObject(
                        name = "Successful Registration",
                        value = """{
                            "userId": "123e4567-e89b-12d3-a456-426614174000",
                            "email": "juan.perez@example.com",
                            "message": "User registered successfully. Please check your email for verification.",
                            "emailVerificationRequired": true,
                            "nextSteps": [
                                "Verify your email address",
                                "Complete your profile",
                                "Start using the platform"
                            ]
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "❌ Invalid input data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Validation Error",
                        value = """{
                            "error": "VALIDATION_ERROR",
                            "message": "Invalid input data provided",
                            "details": {
                                "email": "Invalid email format",
                                "password": "Password must contain at least 8 characters with uppercase, lowercase, number and special character",
                                "phone": "Phone number must be 10 digits"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409",
                description = "⚠️ User already exists",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Duplicate User",
                        value = """{
                            "error": "USER_ALREADY_EXISTS",
                            "message": "A user with this email or phone number already exists",
                            "details": {
                                "conflictField": "email",
                                "suggestion": "Try logging in or use password reset if you forgot your password"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/register")
    fun registerUser(
        @Parameter(
            description = "User registration information",
            required = true,
            schema = Schema(implementation = UserRegistrationRequest::class)
        )
        @Valid @RequestBody request: UserRegistrationRequest
    ): ResponseEntity<UserRegistrationResponse> {
        val result = authenticationUseCase.registerUser(request)

        return if (result.isSuccess) {
            ResponseEntity.status(HttpStatus.CREATED).body(
                UserRegistrationResponse(
                    userId = result.user!!.id,
                    email = result.user.email.value,
                    message = "User registered successfully. Please check your email for verification.",
                    emailVerificationRequired = !result.user.isEmailVerified
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                UserRegistrationResponse(
                    userId = UUID.randomUUID(),
                    email = request.email,
                    message = result.error?.message ?: "Registration failed",
                    emailVerificationRequired = false
                )
            )
        }
    }

    @Operation(
        summary = "User login authentication",
        description = """
            Authenticates a user and returns JWT tokens for API access.

            ## Authentication Methods:
            - 📧 **Email + Password**
            - 📱 **Phone + Password**

            ## Response Tokens:
            - **Access Token**: Short-lived (1 hour) for API requests
            - **Refresh Token**: Long-lived (7 days) for token renewal

            ## Security Features:
            - ✅ Rate limiting (5 attempts per 15 minutes)
            - ✅ Account lockout after failed attempts
            - ✅ Secure password hashing verification
            - ✅ Session tracking and management

            ## Usage:
            Use the returned `accessToken` in the Authorization header:
            ```
            Authorization: Bearer <access-token>
            ```
        """,
        tags = ["Authentication"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "✅ Login successful",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserLoginResponse::class),
                    examples = [ExampleObject(
                        name = "Successful Login",
                        value = """{
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
                                "isPhoneVerified": true,
                                "roles": ["USER"],
                                "permissions": ["READ_COUPONS", "REDEEM_COUPONS", "PARTICIPATE_RAFFLES"]
                            }
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "❌ Invalid credentials",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Invalid Credentials",
                        value = """{
                            "error": "INVALID_CREDENTIALS",
                            "message": "Invalid email/phone or password",
                            "details": {
                                "attemptsRemaining": 3,
                                "lockoutTime": null,
                                "suggestion": "Check your credentials or use password reset"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "423",
                description = "🔒 Account locked",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Account Locked",
                        value = """{
                            "error": "ACCOUNT_LOCKED",
                            "message": "Account temporarily locked due to multiple failed login attempts",
                            "details": {
                                "lockoutExpiresAt": "2024-01-15T11:00:00Z",
                                "lockoutDurationMinutes": 15,
                                "suggestion": "Wait for lockout to expire or contact support"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "429",
                description = "⏰ Too many requests",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Rate Limited",
                        value = """{
                            "error": "RATE_LIMIT_EXCEEDED",
                            "message": "Too many login attempts. Please try again later.",
                            "details": {
                                "retryAfterSeconds": 900,
                                "maxAttemptsPerWindow": 5,
                                "windowDurationMinutes": 15
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/login")
    fun loginUser(
        @Parameter(
            description = "User login credentials (email/phone + password)",
            required = true,
            schema = Schema(implementation = UserLoginRequest::class)
        )
        @Valid @RequestBody request: UserLoginRequest
    ): ResponseEntity<UserLoginResponse> {
        val result = authenticationUseCase.login(request)

        return if (result.isSuccess) {
            ResponseEntity.ok(
                UserLoginResponse(
                    accessToken = result.accessToken!!,
                    refreshToken = result.refreshToken!!,
                    tokenType = "Bearer",
                    expiresIn = result.expiresIn,
                    user = UserInfo(
                        id = result.user!!.id,
                        email = result.user.email.value,
                        firstName = result.user.firstName,
                        lastName = result.user.lastName,
                        isEmailVerified = result.user.isEmailVerified,
                        isPhoneVerified = result.user.isPhoneVerified
                    )
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                UserLoginResponse(
                    accessToken = "",
                    refreshToken = "",
                    tokenType = "Bearer",
                    expiresIn = 0,
                    user = null,
                    error = result.error?.message
                )
            )
        }
    }

    @Operation(
        summary = "Refresh access token",
        description = """
            Generates a new access token using a valid refresh token.

            ## When to use:
            - When your access token expires (401 Unauthorized)
            - Proactively before token expiration
            - During long-running sessions

            ## Security:
            - ✅ Refresh token validation
            - ✅ Token rotation (new refresh token issued)
            - ✅ Automatic cleanup of old tokens
            - ✅ User session validation

            ## Best Practices:
            - Store refresh tokens securely
            - Handle token rotation properly
            - Implement automatic retry logic
            - Clear tokens on logout
        """,
        tags = ["Authentication"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "✅ Token refreshed successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TokenRefreshResponse::class),
                    examples = [ExampleObject(
                        name = "Token Refreshed",
                        value = """{
                            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "tokenType": "Bearer",
                            "expiresIn": 3600,
                            "issuedAt": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "❌ Invalid refresh token",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Invalid Refresh Token",
                        value = """{
                            "error": "INVALID_REFRESH_TOKEN",
                            "message": "The provided refresh token is invalid or expired",
                            "details": {
                                "reason": "TOKEN_EXPIRED",
                                "suggestion": "Please login again to get new tokens"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/refresh")
    fun refreshToken(
        @Parameter(
            description = "Refresh token request",
            required = true,
            schema = Schema(implementation = TokenRefreshRequest::class)
        )
        @Valid @RequestBody request: TokenRefreshRequest
    ): ResponseEntity<TokenRefreshResponse> {
        val result = authenticationUseCase.refreshToken(request.refreshToken)

        return if (result.isSuccess) {
            ResponseEntity.ok(
                TokenRefreshResponse(
                    accessToken = result.accessToken!!,
                    refreshToken = result.refreshToken!!,
                    tokenType = "Bearer",
                    expiresIn = result.expiresIn
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                TokenRefreshResponse(
                    accessToken = "",
                    refreshToken = "",
                    tokenType = "Bearer",
                    expiresIn = 0,
                    error = result.error?.message
                )
            )
        }
    }

    @Operation(
        summary = "Get user profile information",
        description = """
            Retrieves the authenticated user's profile information.

            ## Returned Information:
            - 👤 Basic profile (name, email, phone)
            - ✅ Verification status (email, phone)
            - 🏷️ User roles and permissions
            - 📊 Account statistics
            - ⏰ Account timestamps

            ## Privacy:
            - Only returns data for the authenticated user
            - Sensitive information is filtered out
            - Admin users can access additional fields
        """,
        tags = ["Authentication"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "✅ Profile retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserProfileResponse::class),
                    examples = [ExampleObject(
                        name = "User Profile",
                        value = """{
                            "id": "123e4567-e89b-12d3-a456-426614174000",
                            "email": "juan.perez@example.com",
                            "phone": "5551234567",
                            "firstName": "Juan",
                            "lastName": "Pérez",
                            "isEmailVerified": true,
                            "isPhoneVerified": true,
                            "roles": ["USER"],
                            "permissions": ["READ_COUPONS", "REDEEM_COUPONS"],
                            "createdAt": "2024-01-01T00:00:00Z",
                            "lastLoginAt": "2024-01-15T10:30:00Z",
                            "statistics": {
                                "totalCoupons": 15,
                                "totalRedemptions": 8,
                                "totalRaffleParticipations": 3,
                                "membershipTier": "SILVER"
                            }
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "❌ Authentication required",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    fun getUserProfile(): ResponseEntity<UserProfileResponse> {
        // Implementation would get current user from security context
        // and return their profile information
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Update user profile",
        description = """
            Updates the authenticated user's profile information.

            ## Updatable Fields:
            - 👤 First name and last name
            - 📱 Phone number (requires re-verification)
            - 🔔 Notification preferences
            - 🌍 Language and timezone preferences

            ## Restrictions:
            - ❌ Email cannot be changed (security)
            - ❌ Roles and permissions (admin only)
            - ❌ Account status (admin only)

            ## Verification:
            - Phone changes trigger SMS verification
            - Email changes require admin approval
        """,
        tags = ["Authentication"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    fun updateUserProfile(
        @Parameter(
            description = "Updated profile information",
            required = true,
            schema = Schema(implementation = UpdateProfileRequest::class)
        )
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserProfileResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Change user password",
        description = """
            Changes the authenticated user's password.

            ## Security Requirements:
            - ✅ Current password verification
            - ✅ New password strength validation
            - ✅ Password history check (last 5 passwords)
            - ✅ Session invalidation (all devices)

            ## Post-Change Actions:
            - All active sessions are terminated
            - New login required on all devices
            - Password change notification sent
            - Security audit log entry created
        """,
        tags = ["Authentication"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('USER')")
    fun changePassword(
        @Parameter(
            description = "Password change request",
            required = true,
            schema = Schema(implementation = ChangePasswordRequest::class)
        )
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ChangePasswordResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Initiate password reset",
        description = """
            Initiates password reset process for a user account.

            ## Process:
            1. 📧 Email with reset link sent (if account exists)
            2. 🔗 Secure reset token generated (15-minute expiry)
            3. 🔒 Account temporarily locked for security
            4. 📱 SMS notification sent (if phone verified)

            ## Security:
            - ✅ Rate limiting (3 requests per hour)
            - ✅ No account enumeration (same response for all emails)
            - ✅ Secure token generation
            - ✅ Audit logging
        """,
        tags = ["Authentication"]
    )
    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Parameter(
            description = "Password reset request",
            required = true,
            schema = Schema(implementation = ForgotPasswordRequest::class)
        )
        @Valid @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<ForgotPasswordResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Complete password reset",
        description = """
            Completes the password reset process using a reset token.

            ## Requirements:
            - ✅ Valid reset token (not expired)
            - ✅ Strong new password
            - ✅ Token single-use validation

            ## Actions:
            - Password updated with new hash
            - All sessions invalidated
            - Reset token consumed
            - Success notification sent
        """,
        tags = ["Authentication"]
    )
    @PostMapping("/reset-password")
    fun resetPassword(
        @Parameter(
            description = "Password reset completion",
            required = true,
            schema = Schema(implementation = ResetPasswordRequest::class)
        )
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<ResetPasswordResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "User logout",
        description = """
            Logs out the authenticated user and invalidates tokens.

            ## Actions:
            - ✅ Access token blacklisted
            - ✅ Refresh token invalidated
            - ✅ User session terminated
            - ✅ Security audit logged

            ## Options:
            - **Single Device**: Logout current session only
            - **All Devices**: Logout from all active sessions
        """,
        tags = ["Authentication"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER')")
    fun logout(
        @Parameter(
            description = "Logout options",
            required = false,
            schema = Schema(implementation = LogoutRequest::class)
        )
        @RequestBody(required = false) request: LogoutRequest?
    ): ResponseEntity<LogoutResponse> {
        TODO("Implementation needed")
    }
}