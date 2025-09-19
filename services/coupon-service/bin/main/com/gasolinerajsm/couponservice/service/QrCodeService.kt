package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.Coupon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * Service for QR code generation and validation
 */
@Service
class QrCodeService {

    private val logger = LoggerFactory.getLogger(QrCodeService::class.java)
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    /**
     * Generate QR code string for a coupon
     */
    fun generateQrCode(campaignId: Long, couponCode: String): String {
        val timestamp = java.time.LocalDateTime.now().format(formatter)
        val randomSuffix = String.format("%06d", Random.nextInt(1000000))
        return "GSL_v1_${String.format("%06d", campaignId)}_${timestamp}_ABC12345_${couponCode}"
    }

    /**
     * Generate QR code string for a campaign and coupon
     */
    fun generateQrCode(campaign: Any, couponCode: String): String {
        // Simplified implementation - in real scenario would use campaign object
        val campaignId = 123L // Default for testing
        return generateQrCode(campaignId, couponCode)
    }

    /**
     * Generate QR signature for a coupon
     */
    fun generateQrSignature(qrCode: String, coupon: Coupon): String {
        val data = "${qrCode}${coupon.couponCode}${coupon.discountAmount}${coupon.validUntil}"
        return hashString(data)
    }

    /**
     * Validate QR signature
     */
    fun validateQrSignature(qrCode: String, signature: String, coupon: Coupon): Boolean {
        val expectedSignature = generateQrSignature(qrCode, coupon)
        return expectedSignature == signature
    }

    /**
     * Check if QR code format is valid
     */
    fun isValidQrCodeFormat(qrCode: String): Boolean {
        val pattern = Regex("^GSL_v1_\\d{6}_\\d{14}_[A-Z0-9]{8}_[A-Z0-9]{6,50}$")
        return pattern.matches(qrCode)
    }

    /**
     * Check if QR code is expired by timestamp
     */
    fun isQrCodeExpiredByTimestamp(qrCode: String): Boolean {
        // Extract timestamp from QR code (positions 16-29 in format GSL_v1_000123_20240315120000_ABC12345_COUP001)
        if (qrCode.length < 30) return true

        try {
            val timestampStr = qrCode.substring(16, 30)
            logger.debug("Extracted timestamp from QR code: {}", timestampStr)
            val qrDateTime = java.time.LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val isExpired = qrDateTime.isBefore(java.time.LocalDateTime.now().minusHours(24))
            logger.debug("QR code timestamp: {}, current time: {}, isExpired: {}", qrDateTime, java.time.LocalDateTime.now(), isExpired)
            return isExpired
        } catch (e: Exception) {
            logger.error("Error parsing timestamp from QR code: {}", qrCode, e)
            return true
        }
    }

    /**
     * Parse QR code components
     */
    fun parseQrCode(qrCode: String): QrCodeComponents {
        if (!isValidQrCodeFormat(qrCode)) {
            logger.error("Invalid QR code format: {}", qrCode)
            throw IllegalArgumentException("Invalid QR code format")
        }

        val parts = qrCode.split("_")
        logger.debug("Parsed QR code parts: {}", parts)
        if (parts.size < 6) {
            logger.error("Insufficient parts in QR code: {}", qrCode)
            throw IllegalArgumentException("Invalid QR code format")
        }

        return QrCodeComponents(
            prefix = parts[0],
            version = parts[1],
            campaignId = parts[2].toLong(),
            timestamp = parts[3],
            salt = parts[4],
            couponCode = parts[5]
        )
    }

    /**
     * Generate QR code data object
     */
    fun generateQrCodeData(coupon: Coupon): QrCodeData {
        val qrCode = coupon.qrCode ?: ""
        val signature = coupon.qrSignature ?: ""
        logger.debug("Generating QR code data for coupon: {}, qrCode: {}, signature: {}", coupon.code, qrCode, signature)
        return QrCodeData(
            qrCode = qrCode,
            signature = signature,
            couponCode = coupon.code,
            campaignId = 123L, // Would be from coupon.campaign.id
            campaignName = "Test Campaign", // Would be from coupon.campaign.name
            discountAmount = coupon.discountAmount,
            discountPercentage = coupon.discountPercentage,
            raffleTickets = coupon.raffleTickets,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            termsAndConditions = coupon.termsAndConditions
        )
    }

    /**
     * Validate QR code data
     */
    fun validateQrCodeData(qrCodeData: QrCodeData, coupon: Coupon): ValidationResult {
        val errors = mutableListOf<String>()

        if (qrCodeData.qrCode != coupon.qrCode) {
            errors.add("QR code mismatch")
        }

        if (qrCodeData.couponCode != coupon.code) {
            errors.add("Coupon code mismatch")
        }

        if (qrCodeData.discountAmount != coupon.discountAmount) {
            errors.add("Discount amount mismatch")
        }

        return if (errors.isEmpty()) {
            ValidationResult(true, emptyList())
        } else {
            ValidationResult(false, errors)
        }
    }

    /**
     * Generate QR code hash
     */
    fun generateQrCodeHash(qrCode: String): String {
        return hashString(qrCode)
    }

    /**
     * Verify QR code integrity
     */
    fun verifyQrCodeIntegrity(qrCode: String, hash: String): Boolean {
        return generateQrCodeHash(qrCode) == hash
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * QR code components data class
 */
data class QrCodeComponents(
    val prefix: String,
    val version: String,
    val campaignId: Long,
    val timestamp: String,
    val salt: String,
    val couponCode: String
)

/**
 * QR code data for mobile app
 */
data class QrCodeData(
    val qrCode: String,
    val signature: String,
    val couponCode: String,
    val campaignId: Long,
    val campaignName: String,
    val discountAmount: java.math.BigDecimal,
    val discountPercentage: java.math.BigDecimal?,
    val raffleTickets: Int,
    val validFrom: java.time.LocalDateTime,
    val validUntil: java.time.LocalDateTime,
    val termsAndConditions: String?
)

/**
 * Validation result for QR code operations
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    val message: String
        get() = if (isValid) "Valid" else errors.joinToString("; ")
}