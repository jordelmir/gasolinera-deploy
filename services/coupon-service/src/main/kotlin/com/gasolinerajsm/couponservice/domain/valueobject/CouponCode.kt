package com.gasolinerajsm.couponservice.domain.valueobject

import java.security.SecureRandom
import kotlin.random.Random

/**
 * Coupon Code Value Object
 * Encapsulates coupon code generation and validation logic
 */
@JvmInline
value class CouponCode(val value: String) {

    init {
        require(value.isNotBlank()) { "Coupon code cannot be blank" }
        require(value.length >= MIN_LENGTH) { "Coupon code must be at least $MIN_LENGTH characters" }
        require(value.length <= MAX_LENGTH) { "Coupon code must not exceed $MAX_LENGTH characters" }
        require(value.matches(VALID_PATTERN)) { "Coupon code contains invalid characters" }
    }

    companion object {
        private const val MIN_LENGTH = 6
        private const val MAX_LENGTH = 20
        private val VALID_PATTERN = Regex("^[A-Z0-9-]+$")
        private const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val secureRandom = SecureRandom()

        /**
         * Generate a random coupon code
         */
        fun generate(length: Int = 12, prefix: String? = null): CouponCode {
            require(length >= MIN_LENGTH) { "Length must be at least $MIN_LENGTH" }
            require(length <= MAX_LENGTH) { "Length must not exceed $MAX_LENGTH" }

            val effectiveLength = if (prefix != null) length - prefix.length - 1 else length
            require(effectiveLength > 0) { "Effective length must be positive after prefix" }

            val randomPart = generateRandomString(effectiveLength)
            val code = if (prefix != null) "$prefix-$randomPart" else randomPart

            return CouponCode(code)
        }

        /**
         * Generate a coupon code with campaign prefix
         */
        fun generateWithCampaign(campaignName: String, length: Int = 12): CouponCode {
            val prefix = campaignName.take(4).uppercase().replace(Regex("[^A-Z0-9]"), "")
            return generate(length, prefix.ifEmpty { null })
        }

        /**
         * Create CouponCode from string
         */
        fun from(value: String): CouponCode = CouponCode(value.uppercase())

        private fun generateRandomString(length: Int): String {
            return (1..length)
                .map { CHARACTERS[secureRandom.nextInt(CHARACTERS.length)] }
                .joinToString("")
        }
    }

    /**
     * Check if code has a prefix
     */
    fun hasPrefix(): Boolean = value.contains("-")

    /**
     * Get the prefix part of the code
     */
    fun getPrefix(): String? = if (hasPrefix()) value.substringBefore("-") else null

    /**
     * Get the random part of the code
     */
    fun getRandomPart(): String = if (hasPrefix()) value.substringAfter("-") else value

    /**
     * Check if code matches a pattern
     */
    fun matches(pattern: String): Boolean = value.matches(Regex(pattern))

    /**
     * Get masked version for display (shows only first and last 2 characters)
     */
    fun getMasked(): String {
        return when {
            value.length <= 4 -> "*".repeat(value.length)
            else -> "${value.take(2)}${"*".repeat(value.length - 4)}${value.takeLast(2)}"
        }
    }

    override fun toString(): String = value
}