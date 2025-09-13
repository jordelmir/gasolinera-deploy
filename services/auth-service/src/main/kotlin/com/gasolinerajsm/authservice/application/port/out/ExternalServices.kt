package com.gasolinerajsm.authservice.application.port.out

import com.gasolinerajsm.authservice.domain.event.DomainEvent
import com.gasolinerajsm.authservice.domain.model.User
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import java.time.LocalDateTime

/**
 * Port for OTP (One-Time Password) service
 */
interface OtpService {

    /**
     * Send OTP to phone number
     */
    suspend fun sendOtp(
        phoneNumber: PhoneNumber,
        otpCode: String,
        expiresAt: LocalDateTime
    ): Result<Unit>

    /**
     * Verify OTP code
     */
    suspend fun verifyOtp(
        phoneNumber: PhoneNumber,
        otpCode: String
    ): Result<Boolean>

    /**
     * Check if OTP exists and is valid
     */
    suspend fun isOtpValid(
        phoneNumber: PhoneNumber,
        otpCode: String
    ): Result<Boolean>

    /**
     * Invalidate OTP (mark as used)
     */
    suspend fun invalidateOtp(
        phoneNumber: PhoneNumber,
        otpCode: String
    ): Result<Unit>
}

/**
 * Port for JWT token service
 */
interface JwtTokenService {

    /**
     * Generate access token for user
     */
    fun generateAccessToken(user: User): String

    /**
     * Generate refresh token for user
     */
    fun generateRefreshToken(user: User): String

    /**
     * Validate access token
     */
    fun validateAccessToken(token: String): Result<TokenClaims>

    /**
     * Validate refresh token
     */
    fun validateRefreshToken(token: String): Result<TokenClaims>

    /**
     * Extract user ID from token
     */
    fun extractUserId(token: String): Result<String>

    /**
     * Check if token is expired
     */
    fun isTokenExpired(token: String): Boolean
}

/**
 * Port for SMS service
 */
interface SmsService {

    /**
     * Send SMS message
     */
    suspend fun sendSms(
        phoneNumber: PhoneNumber,
        message: String
    ): Result<Unit>

    /**
     * Send OTP SMS
     */
    suspend fun sendOtpSms(
        phoneNumber: PhoneNumber,
        otpCode: String
    ): Result<Unit>

    /**
     * Check SMS service health
     */
    suspend fun isHealthy(): Boolean
}

/**
 * Port for domain event publishing
 */
interface DomainEventPublisher {

    /**
     * Publish a single domain event
     */
    suspend fun publish(event: DomainEvent): Result<Unit>

    /**
     * Publish multiple domain events
     */
    suspend fun publishAll(events: List<DomainEvent>): Result<Unit>
}

/**
 * Port for caching service
 */
interface CacheService {

    /**
     * Store value in cache
     */
    suspend fun set(key: String, value: String, ttlSeconds: Long): Result<Unit>

    /**
     * Get value from cache
     */
    suspend fun get(key: String): Result<String?>

    /**
     * Delete value from cache
     */
    suspend fun delete(key: String): Result<Unit>

    /**
     * Check if key exists in cache
     */
    suspend fun exists(key: String): Result<Boolean>
}

/**
 * JWT token claims
 */
data class TokenClaims(
    val userId: String,
    val phoneNumber: String,
    val role: String,
    val permissions: List<String>,
    val issuedAt: Long,
    val expiresAt: Long
)