package com.gasolinerajsm.authservice.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * OTP (One-Time Password) Service for phone number verification and authentication.
 *
 * Features:
 * - Secure OTP generation using cryptographically strong random numbers
 * - Redis-based storage with automatic expiration
 * - Rate limiting to prevent abuse
 * - Attempt tracking and lockout mechanism
 * - Comprehensive logging and monitoring
 */
@Service
class OtpService(
    @Value("\${otp.expiration-minutes:5}")
    private val otpExpirationMinutes: Long,

    @Value("\${otp.max-attempts:3}")
    private val maxVerificationAttempts: Int,

    @Value("\${otp.length:6}")
    private val otpLength: Int,

    @Value("\${otp.rate-limit-per-hour:10}")
    private val rateLimitPerHour: Int,

    @Value("\${otp.lockout-minutes:30}")
    private val lockoutMinutes: Long,

    @Value("\${otp.cleanup-interval-hours:24}")
    private val cleanupIntervalHours: Long,

    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private const val OTP_PREFIX = "otp:"
        private const val OTP_ATTEMPTS_PREFIX = "otp:attempts:"
        private const val OTP_RATE_LIMIT_PREFIX = "otp:rate:"
        private const val OTP_LOCKOUT_PREFIX = "otp:lockout:"
        private const val OTP_METADATA_PREFIX = "otp:meta:"

        private const val MIN_OTP_LENGTH = 4
        private const val MAX_OTP_LENGTH = 8
        private const val MIN_EXPIRATION_MINUTES = 1L
        private const val MAX_EXPIRATION_MINUTES = 60L
    }

    private val logger = LoggerFactory.getLogger(OtpService::class.java)
    private val secureRandom = SecureRandom()

    init {
        validateConfiguration()
        logger.info("OTP Service initialized with expiration: {}min, max attempts: {}, length: {}",
                   otpExpirationMinutes, maxVerificationAttempts, otpLength)
    }

    /**
     * Validates the OTP service configuration
     */
    private fun validateConfiguration() {
        require(otpLength in MIN_OTP_LENGTH..MAX_OTP_LENGTH) {
            "OTP length must be between $MIN_OTP_LENGTH and $MAX_OTP_LENGTH"
        }
        require(otpExpirationMinutes in MIN_EXPIRATION_MINUTES..MAX_EXPIRATION_MINUTES) {
            "OTP expiration must be between $MIN_EXPIRATION_MINUTES and $MAX_EXPIRATION_MINUTES minutes"
        }
        require(maxVerificationAttempts > 0) {
            "Max verification attempts must be greater than 0"
        }
        require(rateLimitPerHour > 0) {
            "Rate limit per hour must be greater than 0"
        }
    }

    /**
     * Generates and stores an OTP for the given phone number
     *
     * @param phoneNumber The phone number to generate OTP for
     * @param purpose The purpose of the OTP (login, registration, etc.)
     * @return OtpGenerationResult containing the OTP and metadata
     */
    fun generateOtp(phoneNumber: String, purpose: OtpPurpose = OtpPurpose.LOGIN): OtpGenerationResult {
        val normalizedPhone = normalizePhoneNumber(phoneNumber)

        // Check if phone number is locked out
        if (isPhoneNumberLockedOut(normalizedPhone)) {
            val lockoutExpiry = getLockoutExpiry(normalizedPhone)
            logger.warn("OTP generation blocked for locked phone number: {}", normalizedPhone)
            return OtpGenerationResult.failure(
                OtpError.PHONE_LOCKED_OUT,
                "Phone number is temporarily locked. Try again after $lockoutExpiry"
            )
        }

        // Check rate limiting
        if (isRateLimited(normalizedPhone)) {
            logger.warn("OTP generation rate limited for phone number: {}", normalizedPhone)
            return OtpGenerationResult.failure(
                OtpError.RATE_LIMITED,
                "Too many OTP requests. Please wait before requesting another OTP."
            )
        }

        // Generate OTP
        val otp = generateSecureOtp()
        val expirationTime = LocalDateTime.now().plusMinutes(otpExpirationMinutes)

        // Store OTP and metadata
        val otpData = OtpData(
            otp = otp,
            phoneNumber = normalizedPhone,
            purpose = purpose,
            createdAt = LocalDateTime.now(),
            expiresAt = expirationTime,
            attempts = 0,
            maxAttempts = maxVerificationAttempts
        )

        storeOtp(normalizedPhone, otpData)
        incrementRateLimit(normalizedPhone)

        logger.info("OTP generated for phone number: {} with purpose: {}", normalizedPhone, purpose)

        return OtpGenerationResult.success(
            otp = otp,
            expiresAt = expirationTime,
            attemptsRemaining = maxVerificationAttempts
        )
    }

    /**
     * Verifies an OTP for the given phone number
     *
     * @param phoneNumber The phone number to verify OTP for
     * @param otp The OTP to verify
     * @param purpose The expected purpose of the OTP
     * @return OtpVerificationResult containing the verification status
     */
    fun verifyOtp(phoneNumber: String, otp: String, purpose: OtpPurpose = OtpPurpose.LOGIN): OtpVerificationResult {
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val normalizedOtp = otp.trim()

        // Validate input
        if (normalizedOtp.length != otpLength || !normalizedOtp.all { it.isDigit() }) {
            logger.warn("Invalid OTP format for phone number: {}", normalizedPhone)
            return OtpVerificationResult.failure(
                OtpError.INVALID_FORMAT,
                "Invalid OTP format"
            )
        }

        // Check if phone number is locked out
        if (isPhoneNumberLockedOut(normalizedPhone)) {
            val lockoutExpiry = getLockoutExpiry(normalizedPhone)
            logger.warn("OTP verification blocked for locked phone number: {}", normalizedPhone)
            return OtpVerificationResult.failure(
                OtpError.PHONE_LOCKED_OUT,
                "Phone number is temporarily locked. Try again after $lockoutExpiry"
            )
        }

        // Retrieve stored OTP data
        val otpData = getStoredOtp(normalizedPhone)
        if (otpData == null) {
            logger.warn("No OTP found for phone number: {}", normalizedPhone)
            return OtpVerificationResult.failure(
                OtpError.OTP_NOT_FOUND,
                "No OTP found for this phone number"
            )
        }

        // Check if OTP has expired
        if (otpData.expiresAt.isBefore(LocalDateTime.now())) {
            logger.warn("Expired OTP verification attempt for phone number: {}", normalizedPhone)
            deleteOtp(normalizedPhone)
            return OtpVerificationResult.failure(
                OtpError.OTP_EXPIRED,
                "OTP has expired"
            )
        }

        // Check if maximum attempts exceeded
        if (otpData.attempts >= otpData.maxAttempts) {
            logger.warn("Maximum OTP attempts exceeded for phone number: {}", normalizedPhone)
            deleteOtp(normalizedPhone)
            lockPhoneNumber(normalizedPhone)
            return OtpVerificationResult.failure(
                OtpError.MAX_ATTEMPTS_EXCEEDED,
                "Maximum verification attempts exceeded. Phone number temporarily locked."
            )
        }

        // Check purpose match
        if (otpData.purpose != purpose) {
            logger.warn("OTP purpose mismatch for phone number: {}. Expected: {}, Got: {}",
                      normalizedPhone, otpData.purpose, purpose)
            return OtpVerificationResult.failure(
                OtpError.PURPOSE_MISMATCH,
                "OTP purpose mismatch"
            )
        }

        // Verify OTP
        if (otpData.otp == normalizedOtp) {
            // Success - clean up OTP data
            deleteOtp(normalizedPhone)
            clearRateLimit(normalizedPhone)

            logger.info("OTP verified successfully for phone number: {}", normalizedPhone)
            return OtpVerificationResult.success(
                phoneNumber = normalizedPhone,
                purpose = purpose,
                verifiedAt = LocalDateTime.now()
            )
        } else {
            // Failed verification - increment attempts
            val updatedOtpData = otpData.copy(attempts = otpData.attempts + 1)
            storeOtp(normalizedPhone, updatedOtpData)

            val attemptsRemaining = otpData.maxAttempts - updatedOtpData.attempts

            logger.warn("Invalid OTP verification attempt for phone number: {}. Attempts remaining: {}",
                       normalizedPhone, attemptsRemaining)

            if (attemptsRemaining <= 0) {
                deleteOtp(normalizedPhone)
                lockPhoneNumber(normalizedPhone)
                return OtpVerificationResult.failure(
                    OtpError.MAX_ATTEMPTS_EXCEEDED,
                    "Maximum verification attempts exceeded. Phone number temporarily locked."
                )
            }

            return OtpVerificationResult.failure(
                OtpError.INVALID_OTP,
                "Invalid OTP. $attemptsRemaining attempts remaining.",
                attemptsRemaining
            )
        }
    }

    /**
     * Checks if an OTP exists for the given phone number
     */
    fun hasValidOtp(phoneNumber: String): Boolean {
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val otpData = getStoredOtp(normalizedPhone)
        return otpData != null && otpData.expiresAt.isAfter(LocalDateTime.now())
    }

    /**
     * Gets the remaining time for an OTP
     */
    fun getOtpRemainingTime(phoneNumber: String): Long? {
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val otpData = getStoredOtp(normalizedPhone) ?: return null

        val now = LocalDateTime.now()
        return if (otpData.expiresAt.isAfter(now)) {
            java.time.Duration.between(now, otpData.expiresAt).toMinutes()
        } else {
            null
        }
    }

    /**
     * Gets the number of remaining verification attempts
     */
    fun getRemainingAttempts(phoneNumber: String): Int? {
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val otpData = getStoredOtp(normalizedPhone) ?: return null
        return maxOf(0, otpData.maxAttempts - otpData.attempts)
    }

    /**
     * Cancels an existing OTP for the given phone number
     */
    fun cancelOtp(phoneNumber: String): Boolean {
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val deleted = deleteOtp(normalizedPhone)
        if (deleted) {
            logger.info("OTP cancelled for phone number: {}", normalizedPhone)
        }
        return deleted
    }

    /**
     * Cleans up expired OTPs and rate limit data
     */
    fun cleanupExpiredData() {
        try {
            val otpKeys = redisTemplate.keys("$OTP_PREFIX*")
            val rateLimitKeys = redisTemplate.keys("$OTP_RATE_LIMIT_PREFIX*")
            val lockoutKeys = redisTemplate.keys("$OTP_LOCKOUT_PREFIX*")

            val expiredKeys = mutableSetOf<String>()

            // Check OTP keys for expiration
            otpKeys.forEach { key ->
                val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
                if (ttl <= 0) {
                    expiredKeys.add(key)
                }
            }

            // Check rate limit keys for expiration
            rateLimitKeys.forEach { key ->
                val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
                if (ttl <= 0) {
                    expiredKeys.add(key)
                }
            }

            // Check lockout keys for expiration
            lockoutKeys.forEach { key ->
                val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
                if (ttl <= 0) {
                    expiredKeys.add(key)
                }
            }

            if (expiredKeys.isNotEmpty()) {
                redisTemplate.delete(expiredKeys)
                logger.info("Cleaned up {} expired OTP-related keys", expiredKeys.size)
            }
        } catch (e: Exception) {
            logger.error("Error during OTP cleanup: {}", e.message, e)
        }
    }

    // Private helper methods

    private fun generateSecureOtp(): String {
        val otp = StringBuilder()
        repeat(otpLength) {
            otp.append(secureRandom.nextInt(10))
        }
        return otp.toString()
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^+\\d]"), "")
    }

    private fun storeOtp(phoneNumber: String, otpData: OtpData) {
        val key = "$OTP_PREFIX$phoneNumber"
        val value = serializeOtpData(otpData)
        redisTemplate.opsForValue().set(key, value, otpExpirationMinutes, TimeUnit.MINUTES)
    }

    private fun getStoredOtp(phoneNumber: String): OtpData? {
        val key = "$OTP_PREFIX$phoneNumber"
        val value = redisTemplate.opsForValue().get(key) ?: return null
        return deserializeOtpData(value)
    }

    private fun deleteOtp(phoneNumber: String): Boolean {
        val key = "$OTP_PREFIX$phoneNumber"
        return redisTemplate.delete(key)
    }

    private fun isRateLimited(phoneNumber: String): Boolean {
        val key = "$OTP_RATE_LIMIT_PREFIX$phoneNumber"
        val count = redisTemplate.opsForValue().get(key)?.toIntOrNull() ?: 0
        return count >= rateLimitPerHour
    }

    private fun incrementRateLimit(phoneNumber: String) {
        val key = "$OTP_RATE_LIMIT_PREFIX$phoneNumber"
        val count = redisTemplate.opsForValue().increment(key) ?: 1
        if (count == 1L) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS)
        }
    }

    private fun clearRateLimit(phoneNumber: String) {
        val key = "$OTP_RATE_LIMIT_PREFIX$phoneNumber"
        redisTemplate.delete(key)
    }

    private fun isPhoneNumberLockedOut(phoneNumber: String): Boolean {
        val key = "$OTP_LOCKOUT_PREFIX$phoneNumber"
        return redisTemplate.hasKey(key)
    }

    private fun lockPhoneNumber(phoneNumber: String) {
        val key = "$OTP_LOCKOUT_PREFIX$phoneNumber"
        val lockoutExpiry = LocalDateTime.now().plusMinutes(lockoutMinutes)
        redisTemplate.opsForValue().set(key, lockoutExpiry.toString(), lockoutMinutes, TimeUnit.MINUTES)
        logger.warn("Phone number locked out until {}: {}", lockoutExpiry, phoneNumber)
    }

    private fun getLockoutExpiry(phoneNumber: String): String? {
        val key = "$OTP_LOCKOUT_PREFIX$phoneNumber"
        return redisTemplate.opsForValue().get(key)
    }

    private fun serializeOtpData(otpData: OtpData): String {
        return "${otpData.otp}|${otpData.phoneNumber}|${otpData.purpose}|${otpData.createdAt}|${otpData.expiresAt}|${otpData.attempts}|${otpData.maxAttempts}"
    }

    private fun deserializeOtpData(value: String): OtpData? {
        return try {
            val parts = value.split("|")
            if (parts.size != 7) return null

            OtpData(
                otp = parts[0],
                phoneNumber = parts[1],
                purpose = OtpPurpose.valueOf(parts[2]),
                createdAt = LocalDateTime.parse(parts[3]),
                expiresAt = LocalDateTime.parse(parts[4]),
                attempts = parts[5].toInt(),
                maxAttempts = parts[6].toInt()
            )
        } catch (e: Exception) {
            logger.error("Failed to deserialize OTP data: {}", e.message)
            null
        }
    }
}

/**
 * Data class representing stored OTP information
 */
data class OtpData(
    val otp: String,
    val phoneNumber: String,
    val purpose: OtpPurpose,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val attempts: Int,
    val maxAttempts: Int
)

/**
 * Enum representing different OTP purposes
 */
enum class OtpPurpose {
    LOGIN,
    REGISTRATION,
    PASSWORD_RESET,
    PHONE_VERIFICATION,
    ACCOUNT_RECOVERY
}

/**
 * Result class for OTP generation operations
 */
sealed class OtpGenerationResult {
    data class Success(
        val otp: String,
        val expiresAt: LocalDateTime,
        val attemptsRemaining: Int
    ) : OtpGenerationResult()

    data class Failure(
        val error: OtpError,
        val message: String
    ) : OtpGenerationResult()

    companion object {
        fun success(otp: String, expiresAt: LocalDateTime, attemptsRemaining: Int) =
            Success(otp, expiresAt, attemptsRemaining)

        fun failure(error: OtpError, message: String) =
            Failure(error, message)
    }
}

/**
 * Result class for OTP verification operations
 */
sealed class OtpVerificationResult {
    data class Success(
        val phoneNumber: String,
        val purpose: OtpPurpose,
        val verifiedAt: LocalDateTime
    ) : OtpVerificationResult()

    data class Failure(
        val error: OtpError,
        val message: String,
        val attemptsRemaining: Int? = null
    ) : OtpVerificationResult()

    companion object {
        fun success(phoneNumber: String, purpose: OtpPurpose, verifiedAt: LocalDateTime) =
            Success(phoneNumber, purpose, verifiedAt)

        fun failure(error: OtpError, message: String, attemptsRemaining: Int? = null) =
            Failure(error, message, attemptsRemaining)
    }
}

/**
 * Enum representing different OTP error types
 */
enum class OtpError {
    INVALID_FORMAT,
    OTP_NOT_FOUND,
    OTP_EXPIRED,
    INVALID_OTP,
    MAX_ATTEMPTS_EXCEEDED,
    RATE_LIMITED,
    PHONE_LOCKED_OUT,
    PURPOSE_MISMATCH,
    SYSTEM_ERROR
}