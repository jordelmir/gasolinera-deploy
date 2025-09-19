package com.gasolinerajsm.authservice.infrastructure.adapter

import com.gasolinerajsm.authservice.application.port.out.OtpService
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Mock OTP Service implementation for development and testing
 */
@Service
@ConditionalOnProperty(name = ["app.otp.provider"], havingValue = "mock", matchIfMissing = true)
class MockOtpServiceAdapter : OtpService {

    private val logger = LoggerFactory.getLogger(MockOtpServiceAdapter::class.java)

    // In-memory storage for development
    private val otpStorage = ConcurrentHashMap<String, OtpData>()

    override suspend fun sendOtp(
        phoneNumber: PhoneNumber,
        otpCode: String,
        expiresAt: LocalDateTime
    ): Result<Unit> {
        return try {
            val key = phoneNumber.toString()
            otpStorage[key] = OtpData(otpCode, expiresAt, false)

            logger.info("Mock OTP sent to {}: {}", phoneNumber, otpCode)
            logger.info("OTP expires at: {}", expiresAt)

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to send OTP to {}", phoneNumber, e)
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(
        phoneNumber: PhoneNumber,
        otpCode: String
    ): Result<Boolean> {
        return try {
            val key = phoneNumber.toString()
            val storedOtp = otpStorage[key]

            if (storedOtp == null) {
                logger.warn("No OTP found for phone number: {}", phoneNumber)
                return Result.success(false)
            }

            if (storedOtp.used) {
                logger.warn("OTP already used for phone number: {}", phoneNumber)
                return Result.success(false)
            }

            if (LocalDateTime.now().isAfter(storedOtp.expiresAt)) {
                logger.warn("OTP expired for phone number: {}", phoneNumber)
                return Result.success(false)
            }

            val isValid = storedOtp.code == otpCode
            if (isValid) {
                // Mark as used
                otpStorage[key] = storedOtp.copy(used = true)
                logger.info("OTP verified successfully for phone number: {}", phoneNumber)
            } else {
                logger.warn("Invalid OTP provided for phone number: {}", phoneNumber)
            }

            Result.success(isValid)
        } catch (e: Exception) {
            logger.error("Failed to verify OTP for {}", phoneNumber, e)
            Result.failure(e)
        }
    }

    override suspend fun isOtpValid(
        phoneNumber: PhoneNumber,
        otpCode: String
    ): Result<Boolean> {
        return try {
            val key = phoneNumber.toString()
            val storedOtp = otpStorage[key]

            val isValid = storedOtp != null &&
                    !storedOtp.used &&
                    LocalDateTime.now().isBefore(storedOtp.expiresAt) &&
                    storedOtp.code == otpCode

            Result.success(isValid)
        } catch (e: Exception) {
            logger.error("Failed to check OTP validity for {}", phoneNumber, e)
            Result.failure(e)
        }
    }

    override suspend fun invalidateOtp(
        phoneNumber: PhoneNumber,
        otpCode: String
    ): Result<Unit> {
        return try {
            val key = phoneNumber.toString()
            val storedOtp = otpStorage[key]

            if (storedOtp != null && storedOtp.code == otpCode) {
                otpStorage[key] = storedOtp.copy(used = true)
                logger.info("OTP invalidated for phone number: {}", phoneNumber)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to invalidate OTP for {}", phoneNumber, e)
            Result.failure(e)
        }
    }

    /**
     * Data class to store OTP information
     */
    private data class OtpData(
        val code: String,
        val expiresAt: LocalDateTime,
        val used: Boolean
    )
}