package com.gasolinerajsm.authservice.domain.valueobject

/**
 * Phone Number Value Object
 * Encapsulates phone number validation and formatting logic
 */
@JvmInline
value class PhoneNumber(val value: String) {

    init {
        require(isValid(value)) { "Invalid phone number format: $value" }
    }

    companion object {
        private val PHONE_REGEX = Regex("^\\+?[1-9]\\d{1,14}$")

        /**
         * Create PhoneNumber from string with validation
         */
        fun from(value: String): PhoneNumber {
            val normalized = normalize(value)
            return PhoneNumber(normalized)
        }

        /**
         * Normalize phone number format
         */
        private fun normalize(phoneNumber: String): String {
            // Remove all non-digit characters except +
            val cleaned = phoneNumber.replace(Regex("[^+\\d]"), "")

            // Add + prefix if not present and starts with digit
            return if (cleaned.startsWith("+")) {
                cleaned
            } else if (cleaned.isNotEmpty() && cleaned[0].isDigit()) {
                "+$cleaned"
            } else {
                cleaned
            }
        }

        /**
         * Check if phone number format is valid
         */
        private fun isValid(phoneNumber: String): Boolean {
            return phoneNumber.isNotBlank() && PHONE_REGEX.matches(phoneNumber)
        }
    }

    /**
     * Get formatted phone number for display
     */
    fun getDisplayFormat(): String {
        return when {
            value.startsWith("+1") && value.length == 12 -> {
                // US format: +1 (XXX) XXX-XXXX
                val digits = value.substring(2)
                "+1 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            value.startsWith("+52") && value.length >= 12 -> {
                // Mexico format: +52 XX XXXX XXXX
                val digits = value.substring(3)
                "+52 ${digits.substring(0, 2)} ${digits.substring(2, 6)} ${digits.substring(6)}"
            }
            else -> value // Default format
        }
    }

    /**
     * Get country code from phone number
     */
    fun getCountryCode(): String? {
        return when {
            value.startsWith("+1") -> "US"
            value.startsWith("+52") -> "MX"
            value.startsWith("+34") -> "ES"
            value.startsWith("+44") -> "GB"
            else -> null
        }
    }

    /**
     * Check if this is a mobile number (basic heuristic)
     */
    fun isMobile(): Boolean {
        return when {
            value.startsWith("+1") -> {
                // US mobile numbers typically don't start with 0 or 1 after area code
                val areaCode = value.substring(2, 5)
                !areaCode.startsWith("0") && !areaCode.startsWith("1")
            }
            value.startsWith("+52") -> {
                // Mexico mobile numbers start with specific prefixes
                val prefix = value.substring(3, 5)
                prefix in listOf("55", "56", "33", "81", "22")
            }
            else -> true // Assume mobile for other countries
        }
    }

    /**
     * Get masked phone number for security (show only last 4 digits)
     */
    fun getMasked(): String {
        return if (value.length > 4) {
            "*".repeat(value.length - 4) + value.takeLast(4)
        } else {
            "*".repeat(value.length)
        }
    }

    override fun toString(): String = value
}