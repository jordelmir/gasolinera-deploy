package com.gasolinerajsm.couponservice.domain.valueobject

import java.util.*

/**
 * Coupon ID Value Object
 * Encapsulates coupon identification logic
 */
@JvmInline
value class CouponId(val value: UUID) {

    companion object {
        /**
         * Generate a new unique coupon ID
         */
        fun generate(): CouponId = CouponId(UUID.randomUUID())

        /**
         * Create CouponId from string representation
         */
        fun from(value: String): CouponId {
            return try {
                CouponId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid CouponId format: $value", e)
            }
        }

        /**
         * Create CouponId from UUID
         */
        fun from(uuid: UUID): CouponId = CouponId(uuid)
    }

    /**
     * Convert to string representation
     */
    override fun toString(): String = value.toString()

    /**
     * Check if this is a valid coupon ID
     */
    fun isValid(): Boolean = value.toString().isNotBlank()
}

/**
 * Campaign ID Value Object
 * Encapsulates campaign identification logic
 */
@JvmInline
value class CampaignId(val value: UUID) {

    companion object {
        /**
         * Generate a new unique campaign ID
         */
        fun generate(): CampaignId = CampaignId(UUID.randomUUID())

        /**
         * Create CampaignId from string representation
         */
        fun from(value: String): CampaignId {
            return try {
                CampaignId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid CampaignId format: $value", e)
            }
        }

        /**
         * Create CampaignId from UUID
         */
        fun from(uuid: UUID): CampaignId = CampaignId(uuid)
    }

    /**
     * Convert to string representation
     */
    override fun toString(): String = value.toString()

    /**
     * Check if this is a valid campaign ID
     */
    fun isValid(): Boolean = value.toString().isNotBlank()
}