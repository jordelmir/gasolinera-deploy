package com.gasolinerajsm.authservice.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * SMS Service for sending OTP and other messages.
 *
 * This service provides an abstraction layer for SMS operations and includes
 * a development mode for testing without actual SMS sending.
 */
@Service
class SmsService(
    @Value("\${sms.provider.enabled:false}")
    private val smsEnabled: Boolean,

    @Value("\${sms.provider.api-key:}")
    private val apiKey: String,

    @Value("\${sms.provider.url:}")
    private val providerUrl: String,

    @Value("\${sms.sender-name:Gasolinera JSM}")
    private val senderName: String,

    @Value("\${sms.development-mode:true}")
    private val developmentMode: Boolean,

    @Value("\${sms.rate-limit-per-minute:10}")
    private val rateLimitPerMinute: Int
) {

    private val logger = LoggerFactory.getLogger(SmsService::class.java)

    // In-memory storage for development mode (in production, use Redis or database)
    private val sentMessages = mutableListOf<SmsRecord>()
    private val rateLimitTracker = mutableMapOf<String, MutableList<LocalDateTime>>()

    init {
        logger.info("SMS Service initialized - Enabled: {}, Development Mode: {}", smsEnabled, developmentMode)
        if (!developmentMode && smsEnabled) {
            validateConfiguration()
        }
    }

    /**
     * Sends an OTP SMS to the specified phone number
     */
    fun sendOtpSms(phoneNumber: String, otp: String, purpose: OtpPurpose): SmsResult {
        return try {
            // Check rate limiting
            if (isRateLimited(phoneNumber)) {
                logger.warn("SMS rate limited for phone number: {}", phoneNumber)
                return SmsResult.failure(
                    SmsError.RATE_LIMITED,
                    "Too many SMS requests. Please wait before requesting another SMS."
                )
            }

            val message = buildOtpMessage(otp, purpose)
            val result = sendSms(phoneNumber, message, SmsType.OTP)

            if (result is SmsResult.Success) {
                recordSentMessage(phoneNumber, message, SmsType.OTP)
                updateRateLimit(phoneNumber)
                logger.info("OTP SMS sent successfully to: {}", phoneNumber)
            }

            result
        } catch (e: Exception) {
            logger.error("Failed to send OTP SMS to {}: {}", phoneNumber, e.message, e)
            SmsResult.failure(SmsError.SYSTEM_ERROR, "Failed to send SMS: ${e.message}")
        }
    }

    /**
     * Sends a general notification SMS
     */
    fun sendNotificationSms(phoneNumber: String, message: String): SmsResult {
        return try {
            if (isRateLimited(phoneNumber)) {
                logger.warn("SMS rate limited for phone number: {}", phoneNumber)
                return SmsResult.failure(
                    SmsError.RATE_LIMITED,
                    "Too many SMS requests. Please wait before requesting another SMS."
                )
            }

            val result = sendSms(phoneNumber, message, SmsType.NOTIFICATION)

            if (result is SmsResult.Success) {
                recordSentMessage(phoneNumber, message, SmsType.NOTIFICATION)
                updateRateLimit(phoneNumber)
                logger.info("Notification SMS sent successfully to: {}", phoneNumber)
            }

            result
        } catch (e: Exception) {
            logger.error("Failed to send notification SMS to {}: {}", phoneNumber, e.message, e)
            SmsResult.failure(SmsError.SYSTEM_ERROR, "Failed to send SMS: ${e.message}")
        }
    }

    /**
     * Gets the SMS history for a phone number (development mode only)
     */
    fun getSmsHistory(phoneNumber: String): List<SmsRecord> {
        return if (developmentMode) {
            sentMessages.filter { it.phoneNumber == phoneNumber }
                .sortedByDescending { it.sentAt }
        } else {
            emptyList()
        }
    }

    /**
     * Gets the last sent OTP for a phone number (development mode only)
     */
    fun getLastOtpForDevelopment(phoneNumber: String): String? {
        return if (developmentMode) {
            sentMessages
                .filter { it.phoneNumber == phoneNumber && it.type == SmsType.OTP }
                .maxByOrNull { it.sentAt }
                ?.let { extractOtpFromMessage(it.message) }
        } else {
            null
        }
    }

    /**
     * Clears SMS history (development mode only)
     */
    fun clearSmsHistory() {
        if (developmentMode) {
            sentMessages.clear()
            logger.info("SMS history cleared")
        }
    }

    /**
     * Checks if SMS service is available
     */
    fun isServiceAvailable(): Boolean {
        return if (developmentMode) {
            true
        } else {
            smsEnabled && apiKey.isNotBlank() && providerUrl.isNotBlank()
        }
    }

    // Private helper methods

    private fun sendSms(phoneNumber: String, message: String, type: SmsType): SmsResult {
        return if (developmentMode) {
            // Development mode - simulate SMS sending
            logger.info("📱 [DEVELOPMENT SMS] To: {} | Message: {}", phoneNumber, message)
            SmsResult.success(
                messageId = "dev-${System.currentTimeMillis()}",
                phoneNumber = phoneNumber,
                sentAt = LocalDateTime.now()
            )
        } else if (smsEnabled) {
            // Production mode - integrate with actual SMS provider
            sendSmsViaProvider(phoneNumber, message, type)
        } else {
            logger.warn("SMS service is disabled")
            SmsResult.failure(SmsError.SERVICE_DISABLED, "SMS service is disabled")
        }
    }

    private fun sendSmsViaProvider(phoneNumber: String, message: String, type: SmsType): SmsResult {
        // TODO: Implement actual SMS provider integration
        // This is a placeholder for production SMS provider integration

        return try {
            // Example integration with SMS provider API
            // val response = httpClient.post(providerUrl) {
            //     headers {
            //         append("Authorization", "Bearer $apiKey")
            //         append("Content-Type", "application/json")
            //     }
            //     setBody(SmsRequest(
            //         to = phoneNumber,
            //         message = message,
            //         from = senderName
            //     ))
            // }

            // For now, return a simulated success
            logger.info("SMS sent via provider to: {}", phoneNumber)
            SmsResult.success(
                messageId = "provider-${System.currentTimeMillis()}",
                phoneNumber = phoneNumber,
                sentAt = LocalDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("SMS provider error: {}", e.message, e)
            SmsResult.failure(SmsError.PROVIDER_ERROR, "SMS provider error: ${e.message}")
        }
    }

    private fun buildOtpMessage(otp: String, purpose: OtpPurpose): String {
        return when (purpose) {
            OtpPurpose.LOGIN -> "Tu código de acceso para Gasolinera JSM es: $otp. Válido por 5 minutos. No compartas este código."
            OtpPurpose.REGISTRATION -> "Bienvenido a Gasolinera JSM. Tu código de verificación es: $otp. Válido por 5 minutos."
            OtpPurpose.PASSWORD_RESET -> "Tu código para restablecer contraseña en Gasolinera JSM es: $otp. Válido por 5 minutos."
            OtpPurpose.PHONE_VERIFICATION -> "Código de verificación de teléfono para Gasolinera JSM: $otp. Válido por 5 minutos."
            OtpPurpose.ACCOUNT_RECOVERY -> "Código de recuperación de cuenta para Gasolinera JSM: $otp. Válido por 5 minutos."
        }
    }

    private fun extractOtpFromMessage(message: String): String? {
        // Extract OTP from message using regex
        val otpRegex = Regex("\\b\\d{4,8}\\b")
        return otpRegex.find(message)?.value
    }

    private fun isRateLimited(phoneNumber: String): Boolean {
        val now = LocalDateTime.now()
        val oneMinuteAgo = now.minusMinutes(1)

        val recentMessages = rateLimitTracker[phoneNumber]?.filter { it.isAfter(oneMinuteAgo) } ?: emptyList()
        return recentMessages.size >= rateLimitPerMinute
    }

    private fun updateRateLimit(phoneNumber: String) {
        val now = LocalDateTime.now()
        val oneMinuteAgo = now.minusMinutes(1)

        val phoneHistory = rateLimitTracker.getOrPut(phoneNumber) { mutableListOf() }

        // Remove old entries
        phoneHistory.removeAll { it.isBefore(oneMinuteAgo) }

        // Add current timestamp
        phoneHistory.add(now)
    }

    private fun recordSentMessage(phoneNumber: String, message: String, type: SmsType) {
        if (developmentMode) {
            val record = SmsRecord(
                phoneNumber = phoneNumber,
                message = message,
                type = type,
                sentAt = LocalDateTime.now(),
                messageId = "dev-${System.currentTimeMillis()}"
            )
            sentMessages.add(record)

            // Keep only last 100 messages to prevent memory issues
            if (sentMessages.size > 100) {
                sentMessages.removeAt(0)
            }
        }
    }

    private fun validateConfiguration() {
        require(apiKey.isNotBlank()) { "SMS API key must not be blank when SMS is enabled" }
        require(providerUrl.isNotBlank()) { "SMS provider URL must not be blank when SMS is enabled" }
        require(senderName.isNotBlank()) { "SMS sender name must not be blank" }
    }
}

/**
 * Data class representing an SMS record
 */
data class SmsRecord(
    val phoneNumber: String,
    val message: String,
    val type: SmsType,
    val sentAt: LocalDateTime,
    val messageId: String
)

/**
 * Enum representing SMS types
 */
enum class SmsType {
    OTP,
    NOTIFICATION,
    MARKETING,
    ALERT
}

/**
 * Result class for SMS operations
 */
sealed class SmsResult {
    data class Success(
        val messageId: String,
        val phoneNumber: String,
        val sentAt: LocalDateTime
    ) : SmsResult()

    data class Failure(
        val error: SmsError,
        val message: String
    ) : SmsResult()

    companion object {
        fun success(messageId: String, phoneNumber: String, sentAt: LocalDateTime) =
            Success(messageId, phoneNumber, sentAt)

        fun failure(error: SmsError, message: String) =
            Failure(error, message)
    }
}

/**
 * Enum representing SMS error types
 */
enum class SmsError {
    INVALID_PHONE_NUMBER,
    MESSAGE_TOO_LONG,
    RATE_LIMITED,
    SERVICE_DISABLED,
    PROVIDER_ERROR,
    INSUFFICIENT_CREDITS,
    SYSTEM_ERROR
}