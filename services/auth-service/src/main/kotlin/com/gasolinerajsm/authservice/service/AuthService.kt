package com.gasolinerajsm.authservice.service

import com.gasolinerajsm.authservice.dto.TokenResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Service responsible for authentication operations including OTP generation,
 * verification, and token issuance.
 *
 * This service follows hexagonal architecture principles by depending on
 * abstractions (ports) rather than concrete implementations.
 */
@Service
class AuthService(
    private val redisTemplate: StringRedisTemplate,
    private val jwtService: JwtService,
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    companion object {
        private const val OTP_PREFIX = "otp:"
        private const val OTP_EXPIRATION_MINUTES = 5L
        private const val OTP_LENGTH = 6
        private const val MIN_OTP_VALUE = 100000
        private const val MAX_OTP_VALUE = 999999
    }

    /**
     * Generates and stores an OTP for the given phone number.
     * In production, this should trigger SMS sending.
     *
     * @param phone The phone number to send OTP to
     * @throws IllegalArgumentException if phone number is invalid
     */
    fun sendOtp(phone: String) {
        require(phone.isNotBlank()) { "Phone number cannot be blank" }

        val otp = generateSecureOtp()
        val redisKey = "$OTP_PREFIX$phone"

        redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES)

        logger.info("Generated OTP for phone number ending in {}", phone.takeLast(4))

        // TODO: Replace with actual SMS service integration
        // otpSender.send(phone, "Your Gasolinera JSM code is: $otp")
        logger.debug("Mock OTP for $phone: $otp") // Remove in production
    }

    /**
     * Verifies the provided OTP and issues JWT tokens if valid.
     *
     * @param phone The phone number associated with the OTP
     * @param code The OTP code to verify
     * @return TokenResponse containing access and refresh tokens
     * @throws IllegalArgumentException if OTP is invalid or expired
     */
    fun verifyOtpAndIssueTokens(phone: String, code: String): TokenResponse {
        require(phone.isNotBlank()) { "Phone number cannot be blank" }
        require(code.isNotBlank()) { "OTP code cannot be blank" }

        val redisKey = "$OTP_PREFIX$phone"
        val storedOtp = redisTemplate.opsForValue().get(redisKey)

        if (storedOtp == null || storedOtp != code) {
            logger.warn("Invalid OTP attempt for phone number ending in {}", phone.takeLast(4))
            throw IllegalArgumentException("Invalid or expired OTP")
        }

        logger.info("Successfully verified OTP for phone number ending in {}", phone.takeLast(4))

        // Find or create user
        val user = userService.findOrCreateUser(phone)

        // Generate tokens
        val accessToken = jwtService.generateAccessToken(user.id.toString())
        val refreshToken = jwtService.generateRefreshToken(user.id.toString())

        // Clean up OTP
        redisTemplate.delete(redisKey)

        logger.info("Issued access and refresh tokens for userId: {}", user.id)

        return TokenResponse(accessToken, refreshToken)
    }

    /**
     * Generates a cryptographically secure OTP.
     *
     * @return A 6-digit OTP as string
     */
    private fun generateSecureOtp(): String {
        return Random.nextInt(MIN_OTP_VALUE, MAX_OTP_VALUE).toString()
    }
}