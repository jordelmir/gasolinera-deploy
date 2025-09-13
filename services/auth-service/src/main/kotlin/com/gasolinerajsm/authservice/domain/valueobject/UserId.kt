package com.gasolinerajsm.authservice.domain.valueobject

import java.util.*

/**
 * User ID Value Object
 * Encapsulates user identification logic
 */
@JvmInline
value class UserId(val value: UUID) {

    companion object {
        /**
         * Generate a new unique user ID
         */
        fun generate(): UserId = UserId(UUID.randomUUID())

        /**
         * Create UserId from string representation
         */
        fun from(value: String): UserId {
            return try {
                UserId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid UserId format: $value", e)
            }
        }

        /**
         * Create UserId from UUID
         */
        fun from(uuid: UUID): UserId = UserId(uuid)
    }

    /**
     * Convert to string representation
     */
    override fun toString(): String = value.toString()

    /**
     * Check if this is a valid user ID
     */
    fun isValid(): Boolean = value.toString().isNotBlank()
}