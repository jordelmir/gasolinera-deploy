package com.gasolinerajsm.authservice.service

import com.gasolinerajsm.authservice.dto.*
import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.model.UserRole
import com.gasolinerajsm.authservice.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Comprehensive Authentication Service following hexagonal architecture principles.
 *
 * This service orchestrates all authentication operations including:
 * - User registration and management
 * - OTP-based authentication
 * - JWT token management
 * - Account status management
 */
@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val otpService: OtpService,
    private val smsService: SmsService
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Registers a new user in the system
     */
    fun registerUser(request: UserRegistrationRequest): AuthenticationResponse {
        return try {
            // Validate request
            val validationErrors = request.validate()
            if (validationErrors.isNotEmpty()) {
                return AuthenticationResponse(
                    success = false,
                    message = "Validation failed: ${validationErrors.joinToString(", ")}"
                )
            }

            val normalizedPhone = request.normalizedPhoneNumber()

            // Check if user already exists
            if (userRepository.existsByPhoneNumber(normalizedPhone)) {
                return AuthenticationResponse(
                    success = false,
                    message = "User with this phone number already exists"
                )
            }

            // Create new user
            val user = User(
                phoneNumber = normalizedPhone,
                firstName = request.capitalizedFirstName(),
                lastName = request.capitalizedLastName(),
                role = request.role,
                isActive = true,
                isPhoneVerified = false
            )

            val savedUser = userRepository.save(user)
            logger.info("New user registered with ID: {}", savedUser.id)

            // Generate OTP for phone verification
            val otpResult = otpService.generateOtp(normalizedPhone, OtpPurpose.PHONE_VERIFICATION)

            when (otpResult) {
                is OtpGenerationResult.Success -> {
                    // Send OTP via SMS
                    smsService.sendOtpSms(normalizedPhone, otpResult.otp, OtpPurpose.PHONE_VERIFICATION)

                    AuthenticationResponse(
                        success = true,
                        message = "User registered successfully. Please verify your phone number.",
                        user = UserDetailsResponse.fromUser(savedUser),
                        requiresVerification = true,
                        verificationMethod = "sms"
                    )
                }
                is OtpGenerationResult.Failure -> {
                    AuthenticationResponse(
                        success = false,
                        message = "User registered but failed to send verification SMS: ${otpResult.message}"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error during user registration: {}", e.message, e)
            AuthenticationResponse(
                success = false,
                message = "Registration failed due to system error"
            )
        }
    }

    /**
     * Initiates login process by sending OTP
     */
    fun initiateLogin(request: OtpRequest): OtpGenerationResponse {
        return try {
            val normalizedPhone = request.phoneNumber.replace(Regex("[^+\\d]"), "")

            // Check if user exists and can login
            val user = userRepository.findByPhoneNumber(normalizedPhone)
            if (user == null) {
                return OtpGenerationResponse(
                    success = false,
                    message = "No account found with this phone number",
                    expiresAt = LocalDateTime.now(),
                    attemptsRemaining = 0
                )
            }

            if (!user.canAttemptLogin()) {
                val message = when {
                    !user.isActive -> "Account is deactivated"
                    !user.isPhoneVerified -> "Phone number not verified"
                    user.isAccountLocked() -> "Account is temporarily locked"
                    else -> "Cannot login at this time"
                }
                return OtpGenerationResponse(
                    success = false,
                    message = message,
                    expiresAt = LocalDateTime.now(),
                    attemptsRemaining = 0
                )
            }

            // Generate and send OTP
            val otpResult = otpService.generateOtp(normalizedPhone, request.purpose)

            when (otpResult) {
                is OtpGenerationResult.Success -> {
                    // Send OTP via SMS
                    val smsResult = smsService.sendOtpSms(normalizedPhone, otpResult.otp, request.purpose)

                    val message = if (smsResult is SmsResult.Success) {
                        "OTP sent successfully to your phone"
                    } else {
                        "OTP generated but SMS delivery may be delayed"
                    }

                    OtpGenerationResponse(
                        success = true,
                        message = message,
                        expiresAt = otpResult.expiresAt,
                        attemptsRemaining = otpResult.attemptsRemaining
                    )
                }
                is OtpGenerationResult.Failure -> {
                    OtpGenerationResponse(
                        success = false,
                        message = otpResult.message,
                        expiresAt = LocalDateTime.now(),
                        attemptsRemaining = 0
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error during login initiation: {}", e.message, e)
            OtpGenerationResponse(
                success = false,
                message = "Failed to send OTP due to system error",
                expiresAt = LocalDateTime.now(),
                attemptsRemaining = 0
            )
        }
    }

    /**
     * Completes login process by verifying OTP and issuing tokens
     */
    fun completeLogin(request: OtpVerifyRequest): AuthenticationResponse {
        return try {
            val normalizedPhone = request.phoneNumber.replace(Regex("[^+\\d]"), "")

            // Verify OTP
            val otpResult = otpService.verifyOtp(normalizedPhone, request.otpCode, request.purpose)

            when (otpResult) {
                is OtpVerificationResult.Success -> {
                    // Get user
                    val user = userRepository.findByPhoneNumber(normalizedPhone)
                        ?: return AuthenticationResponse(
                            success = false,
                            message = "User not found"
                        )

                    // Update user login information
                    val updatedUser = user.recordSuccessfulLogin()
                    userRepository.save(updatedUser)

                    // If this was phone verification, mark as verified
                    if (request.purpose == OtpPurpose.PHONE_VERIFICATION && !user.isPhoneVerified) {
                        val verifiedUser = updatedUser.verifyPhone()
                        userRepository.save(verifiedUser)
                    }

                    // Generate tokens
                    val sessionId = UUID.randomUUID().toString()
                    val accessToken = jwtService.generateAccessToken(updatedUser, sessionId)
                    val refreshToken = jwtService.generateRefreshToken(updatedUser)

                    val tokenResponse = TokenResponse(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresIn = 3600, // 1 hour
                        refreshExpiresIn = 86400, // 24 hours
                        sessionId = sessionId
                    )

                    logger.info("User {} logged in successfully", updatedUser.id)

                    AuthenticationResponse(
                        success = true,
                        message = "Login successful",
                        user = UserDetailsResponse.fromUser(updatedUser),
                        tokens = tokenResponse
                    )
                }
                is OtpVerificationResult.Failure -> {
                    // Handle failed verification
                    if (otpResult.error == OtpError.INVALID_OTP) {
                        // Record failed login attempt
                        val user = userRepository.findByPhoneNumber(normalizedPhone)
                        if (user != null) {
                            val updatedUser = user.recordFailedLogin(5, 30) // 5 attempts, 30 min lockout
                            userRepository.save(updatedUser)
                        }
                    }

                    AuthenticationResponse(
                        success = false,
                        message = otpResult.message
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error during login completion: {}", e.message, e)
            AuthenticationResponse(
                success = false,
                message = "Login failed due to system error"
            )
        }
    }

    /**
     * Refreshes access token using refresh token
     */
    fun refreshToken(request: RefreshTokenRequest): AuthenticationResponse {
        return try {
            // Validate refresh token
            if (!jwtService.validateRefreshToken(request.refreshToken)) {
                return AuthenticationResponse(
                    success = false,
                    message = "Invalid or expired refresh token"
                )
            }

            // Get user ID from token
            val userId = jwtService.getUserIdFromToken(request.refreshToken)
                ?: return AuthenticationResponse(
                    success = false,
                    message = "Invalid token format"
                )

            // Get user
            val user = userRepository.findById(userId).orElse(null)
                ?: return AuthenticationResponse(
                    success = false,
                    message = "User not found"
                )

            // Check if user can still login
            if (!user.canAttemptLogin()) {
                return AuthenticationResponse(
                    success = false,
                    message = "Account access restricted"
                )
            }

            // Generate new access token
            val sessionId = UUID.randomUUID().toString()
            val newAccessToken = jwtService.generateAccessToken(user, sessionId)

            val tokenResponse = RefreshTokenResponse(
                accessToken = newAccessToken,
                expiresIn = 3600,
                sessionId = sessionId
            )

            logger.info("Token refreshed for user {}", user.id)

            AuthenticationResponse(
                success = true,
                message = "Token refreshed successfully",
                tokens = TokenResponse(
                    accessToken = newAccessToken,
                    refreshToken = request.refreshToken,
                    expiresIn = 3600,
                    refreshExpiresIn = 86400,
                    sessionId = sessionId
                )
            )
        } catch (e: Exception) {
            logger.error("Error during token refresh: {}", e.message, e)
            AuthenticationResponse(
                success = false,
                message = "Token refresh failed"
            )
        }
    }

    /**
     * Logs out user by invalidating tokens
     */
    fun logout(accessToken: String): LogoutResponse {
        return try {
            // Blacklist the access token
            val blacklisted = jwtService.blacklistToken(accessToken)

            if (blacklisted) {
                logger.info("User logged out successfully")
                LogoutResponse(
                    success = true,
                    message = "Logged out successfully"
                )
            } else {
                LogoutResponse(
                    success = false,
                    message = "Failed to logout"
                )
            }
        } catch (e: Exception) {
            logger.error("Error during logout: {}", e.message, e)
            LogoutResponse(
                success = false,
                message = "Logout failed"
            )
        }
    }

    /**
     * Gets user profile information
     */
    fun getUserProfile(userId: Long): UserDetailsResponse? {
        return try {
            val user = userRepository.findById(userId).orElse(null)
            user?.let { UserDetailsResponse.fromUser(it) }
        } catch (e: Exception) {
            logger.error("Error getting user profile: {}", e.message, e)
            null
        }
    }

    /**
     * Checks account status for a phone number
     */
    fun checkAccountStatus(phoneNumber: String): AccountStatusResponse {
        return try {
            val normalizedPhone = phoneNumber.replace(Regex("[^+\\d]"), "")
            val user = userRepository.findByPhoneNumber(normalizedPhone)

            if (user == null) {
                AccountStatusResponse(
                    phoneNumber = normalizedPhone,
                    exists = false,
                    registrationRequired = true
                )
            } else {
                AccountStatusResponse(
                    phoneNumber = normalizedPhone,
                    exists = true,
                    isActive = user.isActive,
                    isVerified = user.isPhoneVerified,
                    isLocked = user.isAccountLocked(),
                    canLogin = user.canAttemptLogin(),
                    lockoutExpiresAt = user.accountLockedUntil
                )
            }
        } catch (e: Exception) {
            logger.error("Error checking account status: {}", e.message, e)
            AccountStatusResponse(
                phoneNumber = phoneNumber,
                exists = false,
                registrationRequired = true
            )
        }
    }

    /**
     * Validates a JWT token and returns token information
     */
    fun validateToken(token: String): TokenValidationResponse {
        return try {
            if (jwtService.validateAccessToken(token)) {
                val userId = jwtService.getUserIdFromToken(token)
                val roles = jwtService.getRolesFromToken(token)
                val permissions = jwtService.getPermissionsFromToken(token)
                val sessionId = jwtService.getSessionIdFromToken(token)
                val expiresAt = jwtService.getTokenExpiration(token)
                val phoneNumber = jwtService.getPhoneNumberFromToken(token)

                TokenValidationResponse(
                    valid = true,
                    userId = userId,
                    roles = roles,
                    permissions = permissions,
                    expiresAt = expiresAt,
                    sessionId = sessionId,
                    phoneVerified = true, // If token is valid, phone is verified
                    accountStatus = "active"
                )
            } else {
                TokenValidationResponse(valid = false)
            }
        } catch (e: Exception) {
            logger.error("Error validating token: {}", e.message, e)
            TokenValidationResponse(valid = false)
        }
    }

    /**
     * Invalidates all tokens for a user (useful for security incidents)
     */
    fun invalidateAllUserTokens(userId: Long): Boolean {
        return try {
            jwtService.invalidateAllUserTokens(userId)
            logger.info("All tokens invalidated for user {}", userId)
            true
        } catch (e: Exception) {
            logger.error("Error invalidating user tokens: {}", e.message, e)
            false
        }
    }
}