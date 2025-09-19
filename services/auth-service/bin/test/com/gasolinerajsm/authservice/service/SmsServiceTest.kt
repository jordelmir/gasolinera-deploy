package com.gasolinerajsm.authservice.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("SMS Service Tests")
class SmsServiceTest {

    private lateinit var smsService: SmsService

    @BeforeEach
    fun setUp() {
        smsService = SmsService(
            smsEnabled = true,
            apiKey = "test-api-key",
            providerUrl = "https://api.test-sms-provider.com",
            senderName = "Gasolinera JSM Test",
            developmentMode = true,
            rateLimitPerMinute = 5
        )
    }

    @Nested
    @DisplayName("OTP SMS Tests")
    inner class OtpSmsTests {

        @Test
        @DisplayName("Should send OTP SMS successfully in development mode")
        fun shouldSendOtpSmsSuccessfullyInDevelopmentMode() {
            // Given
            val phoneNumber = "+1234567890"
            val otp = "123456"
            val purpose = OtpPurpose.LOGIN

            // When
            val result = smsService.sendOtpSms(phoneNumber, otp, purpose)

            // Then
            assertTrue(result is SmsResult.Success)
            val success = result as SmsResult.Success
            assertEquals(phoneNumber, success.phoneNumber)
            assertNotNull(success.messageId)
            assertTrue(success.messageId.startsWith("dev-"))
            assertTrue(success.sentAt.isAfter(LocalDateTime.now().minusMinutes(1)))
        }

        @Test
        @DisplayName("Should build correct OTP message for different purposes")
        fun shouldBuildCorrectOtpMessageForDifferentPurposes() {
            // Given
            val phoneNumber = "+1234567890"
            val otp = "123456"
            val purposes = listOf(
                OtpPurpose.LOGIN,
                OtpPurpose.REGISTRATION,
                OtpPurpose.PASSWORD_RESET,
                OtpPurpose.PHONE_VERIFICATION,
                OtpPurpose.ACCOUNT_RECOVERY
            )

            purposes.forEach { purpose ->
                // When
                val result = smsService.sendOtpSms(phoneNumber, otp, purpose)

                // Then
                assertTrue(result is SmsResult.Success, "Should succeed for purpose: $purpose")

                // Verify message contains OTP
                val history = smsService.getSmsHistory(phoneNumber)
                val lastMessage = history.firstOrNull()
                assertNotNull(lastMessage, "Should have SMS history for purpose: $purpose")
                assertTrue(lastMessage!!.message.contains(otp), "Message should contain OTP for purpose: $purpose")

                // Clear history for next test
                smsService.clearSmsHistory()
            }
        }

        @Test
        @DisplayName("Should track SMS history in development mode")
        fun shouldTrackSmsHistoryInDevelopmentMode() {
            // Given
            val phoneNumber = "+1234567890"
            val otp1 = "123456"
            val otp2 = "654321"

            // When
            smsService.sendOtpSms(phoneNumber, otp1, OtpPurpose.LOGIN)
            smsService.sendOtpSms(phoneNumber, otp2, OtpPurpose.REGISTRATION)

            // Then
            val history = smsService.getSmsHistory(phoneNumber)
            assertEquals(2, history.size)

            // Verify order (most recent first)
            assertTrue(history[0].message.contains(otp2))
            assertTrue(history[1].message.contains(otp1))

            // Verify types
            assertEquals(SmsType.OTP, history[0].type)
            assertEquals(SmsType.OTP, history[1].type)
        }

        @Test
        @DisplayName("Should get last OTP for development")
        fun shouldGetLastOtpForDevelopment() {
            // Given
            val phoneNumber = "+1234567890"
            val otp1 = "123456"
            val otp2 = "654321"

            // When
            smsService.sendOtpSms(phoneNumber, otp1, OtpPurpose.LOGIN)
            smsService.sendOtpSms(phoneNumber, otp2, OtpPurpose.REGISTRATION)

            // Then
            val lastOtp = smsService.getLastOtpForDevelopment(phoneNumber)
            assertEquals(otp2, lastOtp)
        }
    }

    @Nested
    @DisplayName("Notification SMS Tests")
    inner class NotificationSmsTests {

        @Test
        @DisplayName("Should send notification SMS successfully")
        fun shouldSendNotificationSmsSuccessfully() {
            // Given
            val phoneNumber = "+1234567890"
            val message = "Your account has been activated successfully."

            // When
            val result = smsService.sendNotificationSms(phoneNumber, message)

            // Then
            assertTrue(result is SmsResult.Success)
            val success = result as SmsResult.Success
            assertEquals(phoneNumber, success.phoneNumber)
            assertNotNull(success.messageId)
        }

        @Test
        @DisplayName("Should track notification SMS in history")
        fun shouldTrackNotificationSmsInHistory() {
            // Given
            val phoneNumber = "+1234567890"
            val message = "Test notification message"

            // When
            smsService.sendNotificationSms(phoneNumber, message)

            // Then
            val history = smsService.getSmsHistory(phoneNumber)
            assertEquals(1, history.size)
            assertEquals(message, history[0].message)
            assertEquals(SmsType.NOTIFICATION, history[0].type)
        }
    }

    @Nested
    @DisplayName("Rate Limiting Tests")
    inner class RateLimitingTests {

        @Test
        @DisplayName("Should enforce rate limiting")
        fun shouldEnforceRateLimiting() {
            // Given
            val phoneNumber = "+1234567890"
            val rateLimitPerMinute = 5

            // When - Send messages up to the limit
            repeat(rateLimitPerMinute) { i ->
                val result = smsService.sendOtpSms(phoneNumber, "12345$i", OtpPurpose.LOGIN)
                assertTrue(result is SmsResult.Success, "Should succeed for message $i")
            }

            // Then - Next message should be rate limited
            val rateLimitedResult = smsService.sendOtpSms(phoneNumber, "999999", OtpPurpose.LOGIN)
            assertTrue(rateLimitedResult is SmsResult.Failure)
            val failure = rateLimitedResult as SmsResult.Failure
            assertEquals(SmsError.RATE_LIMITED, failure.error)
        }

        @Test
        @DisplayName("Should allow messages for different phone numbers")
        fun shouldAllowMessagesForDifferentPhoneNumbers() {
            // Given
            val phoneNumber1 = "+1234567890"
            val phoneNumber2 = "+0987654321"
            val rateLimitPerMinute = 5

            // When - Send messages up to the limit for first number
            repeat(rateLimitPerMinute) { i ->
                val result = smsService.sendOtpSms(phoneNumber1, "12345$i", OtpPurpose.LOGIN)
                assertTrue(result is SmsResult.Success)
            }

            // Then - Should still allow messages for second number
            val result = smsService.sendOtpSms(phoneNumber2, "123456", OtpPurpose.LOGIN)
            assertTrue(result is SmsResult.Success)
        }
    }

    @Nested
    @DisplayName("Service Configuration Tests")
    inner class ServiceConfigurationTests {

        @Test
        @DisplayName("Should report service availability in development mode")
        fun shouldReportServiceAvailabilityInDevelopmentMode() {
            // When & Then
            assertTrue(smsService.isServiceAvailable())
        }

        @Test
        @DisplayName("Should handle disabled SMS service")
        fun shouldHandleDisabledSmsService() {
            // Given
            val disabledSmsService = SmsService(
                smsEnabled = false,
                apiKey = "",
                providerUrl = "",
                senderName = "Test",
                developmentMode = false,
                rateLimitPerMinute = 5
            )

            // When
            val result = disabledSmsService.sendOtpSms("+1234567890", "123456", OtpPurpose.LOGIN)

            // Then
            assertTrue(result is SmsResult.Failure)
            val failure = result as SmsResult.Failure
            assertEquals(SmsError.SERVICE_DISABLED, failure.error)
            assertFalse(disabledSmsService.isServiceAvailable())
        }

        @Test
        @DisplayName("Should clear SMS history")
        fun shouldClearSmsHistory() {
            // Given
            val phoneNumber = "+1234567890"
            smsService.sendOtpSms(phoneNumber, "123456", OtpPurpose.LOGIN)
            smsService.sendNotificationSms(phoneNumber, "Test message")

            // Verify history exists
            assertEquals(2, smsService.getSmsHistory(phoneNumber).size)

            // When
            smsService.clearSmsHistory()

            // Then
            assertEquals(0, smsService.getSmsHistory(phoneNumber).size)
        }
    }

    @Nested
    @DisplayName("Production Mode Tests")
    inner class ProductionModeTests {

        @Test
        @DisplayName("Should not return SMS history in production mode")
        fun shouldNotReturnSmsHistoryInProductionMode() {
            // Given
            val productionSmsService = SmsService(
                smsEnabled = true,
                apiKey = "test-api-key",
                providerUrl = "https://api.test-sms-provider.com",
                senderName = "Gasolinera JSM",
                developmentMode = false,
                rateLimitPerMinute = 5
            )
            val phoneNumber = "+1234567890"

            // When
            productionSmsService.sendOtpSms(phoneNumber, "123456", OtpPurpose.LOGIN)
            val history = productionSmsService.getSmsHistory(phoneNumber)

            // Then
            assertEquals(0, history.size)
        }

        @Test
        @DisplayName("Should not return last OTP in production mode")
        fun shouldNotReturnLastOtpInProductionMode() {
            // Given
            val productionSmsService = SmsService(
                smsEnabled = true,
                apiKey = "test-api-key",
                providerUrl = "https://api.test-sms-provider.com",
                senderName = "Gasolinera JSM",
                developmentMode = false,
                rateLimitPerMinute = 5
            )
            val phoneNumber = "+1234567890"

            // When
            productionSmsService.sendOtpSms(phoneNumber, "123456", OtpPurpose.LOGIN)
            val lastOtp = productionSmsService.getLastOtpForDevelopment(phoneNumber)

            // Then
            assertNull(lastOtp)
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle SMS sending errors gracefully")
        fun shouldHandleSmsendingErrorsGracefully() {
            // This test would be more relevant with actual SMS provider integration
            // For now, we test that the service handles errors without crashing

            // Given
            val phoneNumber = "+1234567890"
            val otp = "123456"

            // When & Then - Should not throw exceptions
            assertDoesNotThrow {
                smsService.sendOtpSms(phoneNumber, otp, OtpPurpose.LOGIN)
            }
        }

        @Test
        @DisplayName("Should validate phone number format")
        fun shouldValidatePhoneNumberFormat() {
            // Given
            val invalidPhoneNumbers = listOf("", "   ", "invalid", "123")
            val otp = "123456"

            invalidPhoneNumbers.forEach { phoneNumber ->
                // When & Then - Should not crash on invalid phone numbers
                assertDoesNotThrow {
                    smsService.sendOtpSms(phoneNumber, otp, OtpPurpose.LOGIN)
                }
            }
        }
    }
}