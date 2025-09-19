package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CouponStatus
import com.gasolinerajsm.couponservice.repository.CouponRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Service for validating coupons
 */
@Service
@Transactional(readOnly = true)
class CouponValidationService(
    private val couponRepository: CouponRepository,
    private val qrCodeService: QrCodeService
) {

    private val logger = LoggerFactory.getLogger(CouponValidationService::class.java)

    /**
     * Validate coupon for redemption
     */
    fun validateCouponForRedemption(
        qrCode: String,
        stationId: Long? = null,
        fuelType: String? = null,
        purchaseAmount: BigDecimal? = null
    ): CouponValidationResult {
        logger.info("Validating coupon for redemption: qrCode={}, stationId={}, fuelType={}, purchaseAmount={}",
            qrCode, stationId, fuelType, purchaseAmount)

        try {
            // Find coupon by QR code
            val coupon = couponRepository.findByQrCode(qrCode)
                ?: return CouponValidationResult(
                    isValid = false,
                    canBeUsed = false,
                    coupon = null,
                    errors = listOf("Coupon not found")
                )

            val errors = mutableListOf<String>()

            // Validate QR code format
            if (!qrCodeService.isValidQrCodeFormat(qrCode)) {
                errors.add("Invalid QR code format")
            }

            // Validate QR signature
            if (coupon.qrSignature != null && !qrCodeService.validateQrSignature(qrCode, coupon.qrSignature, coupon)) {
                errors.add("Invalid QR code signature - possible tampering detected")
            }

            // Check if coupon is expired by timestamp
            if (qrCodeService.isQrCodeExpiredByTimestamp(qrCode)) {
                errors.add("QR code has expired due to age")
            }

            // Check coupon status
            if (coupon.status != CouponStatus.ACTIVE) {
                errors.add("Coupon is not active (status: ${coupon.status})")
            }

            // Check date validity
            val now = LocalDateTime.now()
            if (now.isBefore(coupon.validFrom)) {
                errors.add("Coupon is not yet valid")
            }
            if (now.isAfter(coupon.validUntil)) {
                errors.add("Coupon has expired")
            }

            // Check usage limits
            if (coupon.currentUses >= coupon.maxUses) {
                errors.add("Coupon has reached maximum usage limit")
            }

            // Validate station
            if (stationId != null && coupon.campaign.applicableStations != null && coupon.campaign.applicableStations != "ALL") {
                val applicableStations = coupon.campaign.applicableStations.split(",").map { it.trim().toLong() }
                if (stationId !in applicableStations) {
                    errors.add("Coupon is not valid at this station")
                }
            }

            // Validate fuel type - Note: Coupon model doesn't have applicableFuelTypes, using campaign's if available
            if (fuelType != null) {
                // For now, skip fuel type validation as it's not in the model
                // errors.add("Fuel type validation not implemented")
            }

            // Validate minimum purchase amount
            if (purchaseAmount != null && coupon.campaign.minimumPurchase != null) {
                if (purchaseAmount < coupon.campaign.minimumPurchase) {
                    errors.add("Purchase amount does not meet minimum requirement of ${coupon.campaign.minimumPurchase}")
                }
            }

            val isValid = errors.isEmpty()
            val canBeUsed = isValid && coupon.canBeUsed()

            return CouponValidationResult(
                isValid = isValid,
                canBeUsed = canBeUsed,
                coupon = coupon,
                errors = errors
            )

        } catch (e: Exception) {
            logger.error("Error validating coupon for redemption: {}", e.message, e)
            return CouponValidationResult(
                isValid = false,
                canBeUsed = false,
                coupon = null,
                errors = listOf("Validation failed: ${e.message}")
            )
        }
    }

    /**
     * Validate coupon by coupon code
     */
    fun validateCouponByCouponCode(
        couponCode: String,
        stationId: Long? = null
    ): CouponValidationResult {
        logger.info("Validating coupon by code: couponCode={}, stationId={}", couponCode, stationId)

        try {
            val coupon = couponRepository.findByCouponCode(couponCode)
                ?: return CouponValidationResult(
                    isValid = false,
                    canBeUsed = false,
                    coupon = null,
                    errors = listOf("Coupon not found")
                )

            // Use the main validation method
            return if (coupon.qrCode != null) {
                validateCouponForRedemption(
                    qrCode = coupon.qrCode,
                    stationId = stationId
                )
            } else {
                CouponValidationResult(
                    isValid = false,
                    canBeUsed = false,
                    coupon = null,
                    errors = listOf("Coupon has no QR code")
                )
            }

        } catch (e: Exception) {
            logger.error("Error validating coupon by code: {}", e.message, e)
            return CouponValidationResult(
                isValid = false,
                canBeUsed = false,
                coupon = null,
                errors = listOf("Validation failed: ${e.message}")
            )
        }
    }

    /**
     * Validate multiple coupons
     */
    fun validateMultipleCoupons(
        qrCodes: List<String>,
        stationId: Long? = null
    ): List<CouponValidationResult> {
        logger.info("Validating multiple coupons: count={}, stationId={}", qrCodes.size, stationId)

        return qrCodes.map { qrCode ->
            validateCouponForRedemption(qrCode = qrCode, stationId = stationId)
        }
    }

    /**
     * Pre-validate coupon (quick check without full validation)
     */
    fun preValidateCoupon(qrCode: String): PreValidationResult {
        logger.info("Pre-validating coupon: qrCode={}", qrCode)

        try {
            val coupon = couponRepository.findByQrCode(qrCode)
                ?: return PreValidationResult(
                    exists = false,
                    isActive = false,
                    isExpired = true,
                    campaign = null,
                    discountInfo = null
                )

            val now = LocalDateTime.now()
            val isExpired = now.isAfter(coupon.validUntil)
            val isActive = coupon.status == CouponStatus.ACTIVE && !isExpired

            val discountInfo = when {
                coupon.discountAmount != null -> "Fixed discount: ${coupon.discountAmount}"
                coupon.discountPercentage != null -> "Percentage discount: ${coupon.discountPercentage}%"
                coupon.raffleTickets > 0 -> "Raffle tickets only: ${coupon.raffleTickets} tickets"
                else -> "No discount information available"
            }

            return PreValidationResult(
                exists = true,
                isActive = isActive,
                isExpired = isExpired,
                campaign = coupon.campaign?.name,
                discountInfo = discountInfo
            )

        } catch (e: Exception) {
            logger.error("Error pre-validating coupon: {}", e.message, e)
            return PreValidationResult(
                exists = false,
                isActive = false,
                isExpired = true,
                campaign = null,
                discountInfo = null
            )
        }
    }

    /**
     * Get coupon usage statistics
     */
    fun getCouponUsageStats(couponId: Long): CouponUsageStats? {
        logger.info("Getting usage stats for coupon: couponId={}", couponId)

        try {
            val coupon = couponRepository.findById(couponId).orElse(null)
                ?: return null

            val remainingUses = maxOf(0, coupon.maxUses - coupon.currentUses)
            val usageRate = if (coupon.maxUses > 0) {
                (coupon.currentUses.toDouble() / coupon.maxUses.toDouble()) * 100.0
            } else 0.0

            return CouponUsageStats(
                couponId = coupon.id,
                couponCode = coupon.code,
                currentUses = coupon.currentUses,
                maxUses = coupon.maxUses,
                remainingUses = remainingUses,
                usageRate = usageRate,
                isMaxUsesReached = coupon.currentUses >= coupon.maxUses
            )

        } catch (e: Exception) {
            logger.error("Error getting coupon usage stats: {}", e.message, e)
            return null
        }
    }

    /**
     * Validate coupon integrity
     */
    fun validateCouponIntegrity(coupon: Coupon): IntegrityValidationResult {
        logger.info("Validating coupon integrity: couponId={}", coupon.id)

        val issues = mutableListOf<String>()

        try {
            // Check QR code format
            if (coupon.qrCode == null || !qrCodeService.isValidQrCodeFormat(coupon.qrCode)) {
                issues.add("Invalid QR code format")
            }

            // Check QR signature
            if (coupon.qrCode == null || coupon.qrSignature == null ||
                !qrCodeService.validateQrSignature(coupon.qrCode, coupon.qrSignature, coupon)) {
                issues.add("Invalid QR signature")
            }

            // Check date range validity
            if (coupon.validFrom.isAfter(coupon.validUntil)) {
                issues.add("Invalid date range: validFrom is after validUntil")
            }

            // Check usage counts
            if (coupon.currentUses > coupon.maxUses) {
                issues.add("Current uses exceed maximum uses")
            }

            // Check discount configuration
            val hasFixedDiscount = coupon.discountAmount != null
            val hasPercentageDiscount = coupon.discountPercentage != null
            if (hasFixedDiscount && hasPercentageDiscount) {
                issues.add("Both fixed amount and percentage discount are set")
            }

            val isIntact = issues.isEmpty()

            return IntegrityValidationResult(
                isIntact = isIntact,
                issues = issues
            )

        } catch (e: Exception) {
            logger.error("Error validating coupon integrity: {}", e.message, e)
            return IntegrityValidationResult(
                isIntact = false,
                issues = listOf("Integrity validation failed: ${e.message ?: "Unknown error"}")
            )
        }
    }
}

/**
 * Result of coupon validation
 */
data class CouponValidationResult(
    val isValid: Boolean,
    val canBeUsed: Boolean,
    val coupon: Coupon?,
    val errors: List<String> = emptyList()
)

/**
 * Result of pre-validation
 */
data class PreValidationResult(
    val exists: Boolean,
    val isActive: Boolean,
    val isExpired: Boolean,
    val campaign: String?,
    val discountInfo: String?
)

/**
 * Coupon usage statistics
 */
data class CouponUsageStats(
    val couponId: Long,
    val couponCode: String,
    val currentUses: Int,
    val maxUses: Int,
    val remainingUses: Int,
    val usageRate: Double,
    val isMaxUsesReached: Boolean
)

/**
 * Result of integrity validation
 */
data class IntegrityValidationResult(
    val isIntact: Boolean,
    val issues: List<String> = emptyList()
)