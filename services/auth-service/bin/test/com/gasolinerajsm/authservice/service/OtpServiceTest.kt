package com.gasolinerajsm.authservice.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@DisplayName("OTP Service Tests")
class OtpServiceTest {

    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var valueOperations: ValueOperations<String, String>
    private lateinit var otpService: OtpService

    private val otpExpirationMinutes = 5L
    private val maxVerificationAttempts = 3
    private val otpLength = 6
    private val rateLimitPerHour = 10
    private val lockoutMinutes = 30L
    private val cleanupIntervalHours = 24L

    @BeforeEach
    fun setUp() {
        redisTemplate = mockk()
        valueOperations = mockk()

        every { redisTemplate.opsForValue() } returns valueOperations
        every { redisTemplate.hasKey(any()) } returns false
        every { redisTemplate.keys(any()) } returns emptySet()
        every { redisTemplate.delete(any<String>()) } returns true
        every { redisTemplate.delete(any<Collection<String>>()) } returns 1L
        every { redisTemplate.getExpire(any(), any()) } returns -1L
        every { redisTemplate.expire(any(), any(), any()) } returns true
        every { valueOperations.set(any(), any(), any(), any<TimeUnit>()) } returns Unit
        every { valueOperations.get(any()) } returns null
        every { valueOperations.increment(any()) } returns 1L

        otpService = OtpService(
            otpExpirationMinutes = otpExpirationMinutes,
            maxVerificationAttempts = maxVerificationAttempts,
            otpLength = otpLength,
            rateLimitPerHour = rateLimitPerHour,
            lockoutMinutes = lockoutMinutes,
            cleanupIntervalHours = cleanupIntervalHours,
            redisTemplate = redisTemplate
        )
    }

    @Nested
    @DisplayName("OTP Generation Tests")
    inner class OtpGenerationTests {

        @Test
        @DisplayName("Should generate OTP successfully")
        fun shouldGenerateOtpSuccessfully() {
            // Given
            val phoneNumber = "+1234567890"
            val purpose = OtpPurpose.LOGIN

            // When
            val result = otpService.generateOtp(phoneNumber, purpose)

            // Then
            assertTrue(result is OtpGenerationResult.Success)
            val success = result as OtpGenerationResult.Success
            assertEquals(otpLength, success.otp.length)
            assertTrue(success.otp.all { it.isDigit() })
            assertEquals(maxVerificationAttempts, success.attemptsRemaining)
            assertTrue(success.expiresAt.isAfter(LocalDateTime.now()))

            // Verify Redis interactions
            verify { valueOperations.set(any(), any(), otpExpirationMinutes, TimeUnit.MINUTES) }
            verify { valueOperations.increment(any()) }
        }

        @Test
        @DisplayName("Should normalize phone number")
        fun shouldNormalizePhoneNumber() {
            // Given
            val phoneNumber = "+1 (234) 567-8900"
            val normalizedPhone = "+12345678900"

            // When
            val result = otpService.generateOtp(phoneNumber, OtpPurpose.LOGIN)

            // Then
            assertTrue(result is OtpGenerationResult.Success)

            // Verify that normalized phone number is used in Redis key
            verify { valueOperations.set(match { it.contains(normalizedPhone) }, any(), any(), any()) }
        }

        @Test
        @DisplayName("Should reject generation for locked phone number")
        fun shouldRejectGenerationForLockedPhoneNumber() {
            // Given
            val phoneNumber = "+1234567890"
            every { redisTemplate.hasKey("otp:lockout:$phoneNumber") } returns true
            every { valueOperations.get("otp:lockout:$phoneNumber") } returns LocalDateTime.now().plusMinutes(30).toString()

            // When
            val result = otpService.generateOtp(phoneNumber, OtpPurpose.LOGIN)

            // Then
            assertTrue(result is OtpGenerationResult.Failure)
            val failure = result as OtpGenerationResult.Failure
            assertEquals(OtpError.PHONE_LOCKED_OUT, failure.error)
        }

        @Test
        @DisplayName("Should reject generation when rate limited")
        fun shouldRejectGenerationWhenRateLimited() {
            // Given
            val phoneNumber = "+1234567890"
            every { valueOperations.get("otp:rate:$phoneNumber") } returns rateLimitPerHour.toString()

            // When
            val result = otpService.generateOtp(phoneNumber, OtpPurpose.LOGIN)

            // Then
            assertTrue(result is OtpGenerationResult.Failure)
            val failure = result as OtpGenerationResult.Failure
            assertEquals(OtpError.RATE_LIMITED, failure.error)
        }
    }

    @Nested
    @DisplayName("OTP Verification Tests")
    inner class OtpVerificationTests {

        @Test
        @DisplayName("Should verify correct OTP successfully")
        fun shouldVerifyCorrectOtpSuccessfully() {
            // Given
            val phoneNumber = "+1234567890"
            val otp = "123456"
            val purpose = OtpPurpose.LOGIN
            val otpData = createOtpData(phoneNumber, otp, purpose)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(otpData)

            // When
            val result = otpService.verifyOtp(phoneNumber, otp, purpose)

            // Then
            assertTrue(result is OtpVerificationResult.Success)
            val success = result as OtpVerificationResult.Success
            assertEquals(phoneNumber, success.phoneNumber)
            assertEquals(purpose, success.purpose)
            assertTrue(success.verifiedAt.isAfter(LocalDateTime.now().minusMinutes(1)))

            // Verify cleanup
            verify { redisTemplate.delete("otp:$phoneNumber") }
            verify { redisTemplate.delete("otp:rate:$phoneNumber") }
        }

        @Test
        @DisplayName("Should reject invalid OTP format")
        fun shouldRejectInvalidOtpFormat() {
            // Given
            val phoneNumber = "+1234567890"
            val invalidOtps = listOf("12345", "1234567", "abcdef", "12a456", "")

            invalidOtps.forEach { invalidOtp ->
                // When
                val result = otpService.verifyOtp(phoneNumber, invalidOtp, OtpPurpose.LOGIN)

                // Then
                assertTrue(result is OtpVerificationResult.Failure, "Should reject OTP: $invalidOtp")
                val failure = result as OtpVerificationResult.Failure
                assertEquals(OtpError.INVALID_FORMAT, failure.error)
            }
        }

        @Test
        @DisplayName("Should reject verification for non-existent OTP")
        fun shouldRejectVerificationForNonExistentOtp() {
            // Given
            val phoneNumber = "+1234567890"
            val otp = "123456"
            every { valueOperations.get("otp:$phoneNumber") } returns null

            // When
            val result = otpService.verifyOtp(phoneNumber, otp, OtpPurpose.LOGIN)

            // Then
            assertTrue(result is OtpVerificationResult.Failure)
            val failure = result as OtpVerificationResult.Failure
            assertEquals(OtpError.OTP_NOT_FOUND, failure.error)
        }

        @Test
        @DisplayName("Should reject expired OTP")
        fun shouldRejectExpiredOtp() {
            // Given
            val phoneNumber = "+1234567890"
            val otp = "123456"
            val purpose = OtpPurpose.LOGIN
            val expiredOtpData = createOtpData(phoneNumber, otp, purpose, expired = true)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(expiredOtpData)

            // When
            val result = otpService.verifyOtp(phoneNumber, otp, purpose)

            // Then
            assertTrue(result is OtpVerificationResult.Failure)
            val failure = result as OtpVerificationResult.Failure
            assertEquals(OtpError.OTP_EXPIRED, failure.error)

            // Verify cleanup
            verify { redisTemplate.delete("otp:$phoneNumber") }
        }

        @Test
        @DisplayName("Should reject OTP with wrong purpose")
        fun shouldRejectOtpWithWrongPurpose() {
            // Given
            val phoneNumber = "+1234567890"
            val otp = "123456"
            val storedPurpose = OtpPurpose.LOGIN
            val verificationPurpose = OtpPurpose.REGISTRATION
            val otpData = createOtpData(phoneNumber, otp, storedPurpose)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(otpData)

            // When
            val result = otpService.verifyOtp(phoneNumber, otp, verificationPurpose)

            // Then
            assertTrue(result is OtpVerificationResult.Failure)
            val failure = result as OtpVerificationResult.Failure
            assertEquals(OtpError.PURPOSE_MISMATCH, failure.error)
        }

        @Test
        @DisplayName("Should handle incorrect OTP with attempts tracking")
        fun shouldHandleIncorrectOtpWithAttemptsTracking() {
            // Given
            val phoneNumber = "+1234567890"
            val correctOtp = "123456"
            val incorrectOtp = "654321"
            val purpose = OtpPurpose.LOGIN
            val otpData = createOtpData(phoneNumber, correctOtp, purpose, attempts = 1)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(otpData)

            // When
            val result = otpService.verifyOtp(phoneNumber, incorrectOtp, purpose)

            // Then
            assertTrue(result is OtpVerificationResult.Failure)
            val failure = result as OtpVerificationResult.Failure
            assertEquals(OtpError.INVALID_OTP, failure.error)
            assertEquals(1, failure.attemptsRemaining) // 3 max - 2 current = 1 remaining

            // Verify attempts are incremented
            verify { valueOperations.set(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("Should lock phone number after max attempts")
        fun shouldLockPhoneNumberAfterMaxAttempts() {
            // Given
            val phoneNumber = "+1234567890"
            val correctOtp = "123456"
            val incorrectOtp = "654321"
            val purpose = OtpPurpose.LOGIN
            val otpData = createOtpData(phoneNumber, correctOtp, purpose, attempts = maxVerificationAttempts - 1)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(otpData)

            // When
            val result = otpService.verifyOtp(phoneNumber, incorrectOtp, purpose)

            // Then
            assertTrue(result is OtpVerificationResult.Failure)
            val failure = result as OtpVerificationResult.Failure
            assertEquals(OtpError.MAX_ATTEMPTS_EXCEEDED, failure.error)

            // Verify lockout is set
            verify { valueOperations.set("otp:lockout:$phoneNumber", any(), lockoutMinutes, TimeUnit.MINUTES) }
            verify { redisTemplate.delete("otp:$phoneNumber") }
        }
    }

    @Nested
    @DisplayName("OTP Status Check Tests")
    inner class OtpStatusCheckTests {

        @Test
        @DisplayName("Should check if valid OTP exists")
        fun shouldCheckIfValidOtpExists() {
            // Given
            val phoneNumber = "+1234567890"
            val otpData = createOtpData(phoneNumber, "123456", OtpPurpose.LOGIN)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(otpData)

            // When
            val hasValidOtp = otpService.hasValidOtp(phoneNumber)

            // Then
            assertTrue(hasValidOtp)
        }

        @Test
        @DisplayName("Should return false for expired OTP")
        fun shouldReturnFalseForExpiredOtp() {
            // Given
            val phoneNumber = "+1234567890"
            val expiredOtpData = createOtpData(phoneNumber, "123456", OtpPurpose.LOGIN, expired = true)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(expiredOtpData)

            // When
            val hasValidOtp = otpService.hasValidOtp(phoneNumber)

            // Then
            assertFalse(hasValidOtp)
        }

        @Test
        @DisplayName("Should get remaining time for OTP")
        fun shouldGetRemainingTimeForOtp() {
            // Given
            val phoneNumber = "+1234567890"
            val otpData = createOtpData(phoneNumber, "123456", OtpPurpose.LOGIN)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(otpData)

            // When
            val remainingTime = otpService.getOtpRemainingTime(phoneNumber)

            // Then
            assertNotNull(remainingTime)
            assertTrue(remainingTime!! > 0)
            assertTrue(remainingTime <= otpExpirationMinutes)
        }

        @Test
        @DisplayName("Should get remaining attempts")
        fun shouldGetRemainingAttempts() {
            // Given
            val phoneNumber = "+1234567890"
            val attempts = 1
            val otpData = createOtpData(phoneNumber, "123456", OtpPurpose.LOGIN, attempts = attempts)

            every { valueOperations.get("otp:$phoneNumber") } returns serializeOtpData(otpData)

            // When
            val remainingAttempts = otpService.getRemainingAttempts(phoneNumber)

            // Then
            assertNotNull(remainingAttempts)
            assertEquals(maxVerificationAttempts - attempts, remainingAttempts)
        }
    }

    @Nested
    @DisplayName("OTP Management Tests")
    inner class OtpManagementTests {

        @Test
        @DisplayName("Should cancel existing OTP")
        fun shouldCancelExistingOtp() {
            // Given
            val phoneNumber = "+1234567890"

            // When
            val result = otpService.cancelOtp(phoneNumber)

            // Then
            assertTrue(result)
            verify { redisTemplate.delete("otp:$phoneNumber") }
        }

        @Test
        @DisplayName("Should cleanup expired data")
        fun shouldCleanupExpiredData() {
            // Given
            val expiredKeys = setOf("otp:expired1", "otp:rate:expired2", "otp:lockout:expired3")
            every { redisTemplate.keys("otp:*") } returns expiredKeys
            every { redisTemplate.keys("otp:rate:*") } returns expiredKeys
            every { redisTemplate.keys("otp:lockout:*") } returns expiredKeys
            every { redisTemplate.getExpire(any(), TimeUnit.SECONDS) } returns 0L // Expired

            // When
            otpService.cleanupExpiredData()

            // Then
            verify { redisTemplate.delete(any<Set<String>>()) }
        }
    }

    // Helper methods

    private fun createOtpData(
        phoneNumber: String,
        otp: String,
        purpose: OtpPurpose,
        attempts: Int = 0,
        expired: Boolean = false
    ): OtpData {
        val now = LocalDateTime.now()
        val expiresAt = if (expired) {
            now.minusMinutes(1)
        } else {
            now.plusMinutes(otpExpirationMinutes)
        }

        return OtpData(
            otp = otp,
            phoneNumber = phoneNumber,
            purpose = purpose,
            createdAt = now.minusMinutes(1),
            expiresAt = expiresAt,
            attempts = attempts,
            maxAttempts = maxVerificationAttempts
        )
    }

    private fun serializeOtpData(otpData: OtpData): String {
        return "${otpData.otp}|${otpData.phoneNumber}|${otpData.purpose}|${otpData.createdAt}|${otpData.expiresAt}|${otpData.attempts}|${otpData.maxAttempts}"
    }
}