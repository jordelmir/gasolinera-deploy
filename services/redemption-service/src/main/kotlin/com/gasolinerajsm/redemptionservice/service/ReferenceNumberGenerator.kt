package com.gasolinerajsm.redemptionservice.service

import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Service for generating unique reference numbers for redemptions
 */
@Service
class ReferenceNumberGenerator {

    private val secureRandom = SecureRandom()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Generate a unique reference number
     * Format: RDM-YYYYMMDD-XXXXXX
     * Where XXXXXX is a 6-digit random number
     */
    fun generate(): String {
        val dateString = LocalDateTime.now().format(dateFormatter)
        val randomNumber = secureRandom.nextInt(999999).toString().padStart(6, '0')

        return "RDM-$dateString-$randomNumber"
    }

    /**
     * Generate a reference number with custom prefix
     */
    fun generate(prefix: String): String {
        val dateString = LocalDateTime.now().format(dateFormatter)
        val randomNumber = secureRandom.nextInt(999999).toString().padStart(6, '0')

        return "$prefix-$dateString-$randomNumber"
    }

    /**
     * Validate reference number format
     */
    fun isValidFormat(referenceNumber: String): Boolean {
        val pattern = Regex("^[A-Z]{3}-\\d{8}-\\d{6}$")
        return pattern.matches(referenceNumber)
    }

    /**
     * Extract date from reference number
     */
    fun extractDate(referenceNumber: String): LocalDateTime? {
        if (!isValidFormat(referenceNumber)) {
            return null
        }

        return try {
            val datePart = referenceNumber.split("-")[1]
            LocalDateTime.parse("${datePart}T00:00:00", DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:ss"))
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Extract prefix from reference number
     */
    fun extractPrefix(referenceNumber: String): String? {
        if (!isValidFormat(referenceNumber)) {
            return null
        }

        return referenceNumber.split("-")[0]
    }
}