package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.Campaign
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service for generating and validating QR codes with cryptographic signatures
 */
@Service
class QrCodeService {

    companion object {
        private const val QR_CODE_LENGTH = 32
        private const val SIGNATURE_ALGORITHM = "HmacSHA256"
        private const val SECRET_KEY = "gasolinera-jsm-qr-secret-key-2024" // In production, this should come from Vault
        private const val QR_CODE_PREFIX = "GSL"
        private const val QR_CODE_VERSION = "v1"
    }

    private val secureRandom = SecureRandom()
    private val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), SIGNATURE_ALGORITHM)

    /**
     * Generate a unique QR code for a coupon
     */
    fun generateQrCode(campaign: Campaign, couponCode: String): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val randomComponent = generateRandomString(8)
        val campaignId = campaign.id.toString().padStart(6, '0')

        return "${QR_CODE_PREFIX}_${QR_CODE_VERSION}_${campaignId}_${timestamp}_${randomComponent}_${couponCode}"
    }

    /**
     * Generate cryptographic signature for QR code
     */
    fun generateQrSignature(qrCode: String, coupon: Coupon): String {
        val dataToSign = buildString {
            append(qrCode)
            append("|")
            append(coupon.couponCode)
            append("|")
            append(coupon.campaign.id)
            append("|")
            append(coupon.validFrom.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            append("|")
            append(coupon.validUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            append("|")
            append(coupon.raffleTickets)
            append("|")
            append(coupon.discountAmount ?: "0")
            append("|")
            append(coupon.discountPercentage ?: "0")
        }

        return generateHmacSignature(dataToSign)
    }

    /**
     * Validate QR code signature
     */
    fun validateQrSignature(qrCode: String, signature: String, coupon: Coupon): Boolean {
        val expectedSignature = generateQrSignature(qrCode, coupon)
        return constantTimeEquals(signature, expectedSignature)
    }

    /**
     * Parse QR code components
     */
    fun parseQrCode(qrCode: String): QrCodeComponents? {
        return try {
            val parts = qrCode.split("_")
            if (parts.size != 6) return null

            val prefix = parts[0]
            val version = parts[1]
            val campaignId = parts[2].toLongOrNull()
            val timestamp = parts[3]
            val randomComponent = parts[4]
            val couponCode = parts[5]

            if (prefix != QR_CODE_PREFIX || version != QR_CODE_VERSION || campaignId == null) {
                return null
            }

            QrCodeComponents(
                prefix = prefix,
                version = version,
                campaignId = campaignId,
                timestamp = timestamp,
                randomComponent = randomComponent,
                couponCode = couponCode
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validate QR code format
     */
    fun isValidQrCodeFormat(qrCode: String): Boolean {
        return parseQrCode(qrCode) != null
    }

    /**
     * Check if QR code has expired based on timestamp
     */
    fun isQrCodeExpiredByTimestamp(qrCode: String, maxAgeHours: Long = 24): Boolean {
        val components = parseQrCode(qrCode) ?: return true

        return try {
            val qrTimestamp = LocalDateTime.parse(components.timestamp, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val expirationTime = qrTimestamp.plusHours(maxAgeHours)
            LocalDateTime.now().isAfter(expirationTime)
        } catch (e: Exception) {
            true // If we can't parse the timestamp, consider it expired
        }
    }

    /**
     * Generate QR code data for mobile app scanning
     */
    fun generateQrCodeData(coupon: Coupon): QrCodeData {
        return QrCodeData(
            qrCode = coupon.qrCode,
            signature = coupon.qrSignature,
            couponCode = coupon.couponCode,
            campaignId = coupon.campaign.id,
            campaignName = coupon.campaign.name,
            discountAmount = coupon.discountAmount,
            discountPercentage = coupon.discountPercentage,
            raffleTickets = coupon.raffleTickets,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            termsAndConditions = coupon.termsAndConditions
        )
    }

    /**
     * Validate complete QR code data
     */
    fun validateQrCodeData(qrCodeData: QrCodeData, coupon: Coupon): QrValidationResult {
        val validationErrors = mutableListOf<String>()

        // Check QR code format
        if (!isValidQrCodeFormat(qrCodeData.qrCode)) {
            validationErrors.add("Invalid QR code format")
        }

        // Check signature
        if (!validateQrSignature(qrCodeData.qrCode, qrCodeData.signature, coupon)) {
            validationErrors.add("Invalid QR code signature")
        }

        // Check coupon code match
        if (qrCodeData.couponCode != coupon.couponCode) {
            validationErrors.add("Coupon code mismatch")
        }

        // Check campaign ID match
        if (qrCodeData.campaignId != coupon.campaign.id) {
            validationErrors.add("Campaign ID mismatch")
        }

        // Check if QR code is expired by timestamp
        if (isQrCodeExpiredByTimestamp(qrCodeData.qrCode)) {
            validationErrors.add("QR code has expired")
        }

        return QrValidationResult(
            isValid = validationErrors.isEmpty(),
            errors = validationErrors
        )
    }

    /**
     * Generate secure random string
     */
    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Generate HMAC signature
     */
    private fun generateHmacSignature(data: String): String {
        val mac = Mac.getInstance(SIGNATURE_ALGORITHM)
        mac.init(secretKeySpec)
        val signature = mac.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(signature)
    }

    /**
     * Constant time string comparison to prevent timing attacks
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Generate hash for QR code deduplication
     */
    fun generateQrCodeHash(qrCode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(qrCode.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Verify QR code integrity
     */
    fun verifyQrCodeIntegrity(qrCode: String, expectedHash: String): Boolean {
        val actualHash = generateQrCodeHash(qrCode)
        return constantTimeEquals(actualHash, expectedHash)
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
    val randomComponent: String,
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
    val discountAmount: java.math.BigDecimal?,
    val discountPercentage: java.math.BigDecimal?,
    val raffleTickets: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val termsAndConditions: String?
)

/**
 * QR validation result
 */
data class QrValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)