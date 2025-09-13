package com.gasolinerajsm.redemptionservice.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Service for validating coupons with the Coupon Service
 */
@Service
class CouponValidationService(
    private val restTemplate: RestTemplate
) {

    private val logger = LoggerFactory.getLogger(CouponValidationService::class.java)
    private val couponServiceUrl = "http://coupon-service:8080"

    /**
     * Validate a coupon for redemption
     */
    fun validateCoupon(couponId: Long, userId: Long, stationId: Long): CouponValidationResult {
        logger.info("Validating coupon $couponId for user $userId at station $stationId")

        try {
            val validationRequest = CouponValidationRequest(
                couponId = couponId,
                userId = userId,
                stationId = stationId,
                validationTime = LocalDateTime.now()
            )

            val response = restTemplate.postForObject(
                "$couponServiceUrl/api/coupons/validate",
                validationRequest,
                CouponValidationResponse::class.java
            )

            return if (response?.isValid == true) {
                logger.info("Coupon $couponId validation successful")
                CouponValidationResult(
                    isValid = true,
                    couponDetails = response.couponDetails,
                    reason = null
                )
            } else {
                logger.warn("Coupon $couponId validation failed: ${response?.reason}")
                CouponValidationResult(
                    isValid = false,
                    couponDetails = null,
                    reason = response?.reason ?: "Unknown validation error"
                )
            }

        } catch (exception: Exception) {
            logger.error("Error validating coupon $couponId", exception)
            return CouponValidationResult(
                isValid = false,
                couponDetails = null,
                reason = "Service unavailable: ${exception.message}"
            )
        }
    }

    /**
     * Mark coupon as redeemed
     */
    fun markCouponAsRedeemed(couponId: Long, redemptionId: Long): Boolean {
        logger.info("Marking coupon $couponId as redeemed for redemption $redemptionId")

        try {
            val redemptionRequest = CouponRedemptionRequest(
                couponId = couponId,
                redemptionId = redemptionId,
                redeemedAt = LocalDateTime.now()
            )

            val response = restTemplate.postForObject(
                "$couponServiceUrl/api/coupons/redeem",
                redemptionRequest,
                CouponRedemptionResponse::class.java
            )

            return response?.success == true

        } catch (exception: Exception) {
            logger.error("Error marking coupon $couponId as redeemed", exception)
            return false
        }
    }
}

/**
 * Coupon validation result
 */
data class CouponValidationResult(
    val isValid: Boolean,
    val couponDetails: CouponDetails?,
    val reason: String?
)

/**
 * Coupon validation request
 */
data class CouponValidationRequest(
    val couponId: Long,
    val userId: Long,
    val stationId: Long,
    val validationTime: LocalDateTime
)

/**
 * Coupon validation response
 */
data class CouponValidationResponse(
    val isValid: Boolean,
    val couponDetails: CouponDetails?,
    val reason: String?
)

/**
 * Coupon redemption request
 */
data class CouponRedemptionRequest(
    val couponId: Long,
    val redemptionId: Long,
    val redeemedAt: LocalDateTime
)

/**
 * Coupon redemption response
 */
data class CouponRedemptionResponse(
    val success: Boolean,
    val message: String?
)

/**
 * Coupon details
 */
data class CouponDetails(
    val id: Long,
    val code: String,
    val campaignId: Long,
    val discountType: String,
    val discountValue: BigDecimal,
    val minimumPurchase: BigDecimal?,
    val maximumDiscount: BigDecimal?,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val usageLimit: Int?,
    val usageCount: Int,
    val applicableStations: List<Long>?,
    val applicableProducts: List<String>?,
    val ticketMultiplier: Int?
)