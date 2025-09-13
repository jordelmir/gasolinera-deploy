package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CouponStatus
import com.gasolinerajsm.couponservice.repository.CouponRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Service for validating coupons and their usage conditions
 */
@Service
class CouponValidationService(
    private val couponRepository: CouponRepository,
    private val qrCodeService: QrCodeService
) {

    /**
     * Validate coupon for redemption
     */
    fun validateCouponForRedemption(
        qrCode: String,
        stationId: Long,
        fuelType: String? = null,
        purchaseAmount: BigDecimal? = null
    ): CouponValidationResult {
        val validationErrors = mutableListOf<String>()

        // Find coupon by QR code
        val coupon = couponRepository.findByQrCode(qrCode)
        if (coupon == null) {
            return CouponValidationResult(
                isValid = false,
                coupon = null,
                errors = listOf("Coupon not found"),
                canBeUsed = false
            )
        }

        // Validate QR code format and signature
        if (!qrCodeService.isValidQrCodeFormat(qrCode)) {
            validationErrors.add("Invalid QR code format")
        }

        if (!qrCodeService.validateQrSignature(qrCode, coupon.qrSignature, coupon)) {
            validationErrors.add("Invalid QR code signature - possible tampering detected")
        }

        // Check coupon status
        if (coupon.status != CouponStatus.ACTIVE) {
            validationErrors.add("Coupon is not active (status: ${coupon.status.displayName})")
        }

        // Check if coupon is valid (date range and usage limits)
        if (!coupon.isValid()) {
            when {
                coupon.isExpired() -> validationErrors.add("Coupon has expired")
                coupon.isNotYetValid() -> validationErrors.add("Coupon is not yet valid")
                coupon.isMaxUsesReached() -> validationErrors.add("Coupon has reached maximum usage limit")
            }
        }

        // Check campaign status
        if (!coupon.campaign.isActive()) {
            validationErrors.add("Campaign is not active")
        }

        // Check station applicability
        if (!coupon.appliesTo(stationId)) {
            validationErrors.add("Coupon is not valid at this station")
        }

        // Check fuel type applicability
        if (fuelType != null && !coupon.appliesTo(fuelType)) {
            validationErrors.add("Coupon is not valid for fuel type: $fuelType")
        }

        // Check minimum purchase amount
        if (purchaseAmount != null && !coupon.meetsMinimumPurchase(purchaseAmount)) {
            validationErrors.add("Purchase amount does not meet minimum requirement of ${coupon.minimumPurchaseAmount}")
        }

        // Check if QR code has expired by timestamp
        if (qrCodeService.isQrCodeExpiredByTimestamp(qrCode)) {
            validationErrors.add("QR code has expired due to age")
        }

        return CouponValidationResult(
            isValid = validationErrors.isEmpty(),
            coupon = coupon,
            errors = validationErrors,
            canBeUsed = validationErrors.isEmpty() && coupon.canBeUsed()
        )
    }

    /**
     * Validate coupon by coupon code
     */
    fun validateCouponByCouponCode(
        couponCode: String,
        stationId: Long,
        fuelType: String? = null,
        purchaseAmount: BigDecimal? = null
    ): CouponValidationResult {
        val coupon = couponRepository.findByCouponCode(couponCode)
        if (coupon == null) {
            return CouponValidationResult(
                isValid = false,
                coupon = null,
                errors = listOf("Coupon not found"),
                canBeUsed = false
            )
        }

        return validateCouponForRedemption(coupon.qrCode, stationId, fuelType, purchaseAmount)
    }

    /**
     * Validate multiple coupons for batch processing
     */
    fun validateMultipleCoupons(
        qrCodes: List<String>,
        stationId: Long,
        fuelType: String? = null,
        purchaseAmount: BigDecimal? = null
    ): List<CouponValidationResult> {
        return qrCodes.map { qrCode ->
            validateCouponForRedemption(qrCode, stationId, fuelType, purchaseAmount)
        }
    }

    /**
     * Check if coupon can be used by specific user
     */
    fun validateCouponForUser(
        qrCode: String,
        userId: Long,
        stationId: Long
    ): CouponValidationResult {
        val baseValidation = validateCouponForRedemption(qrCode, stationId)

        if (!baseValidation.isValid || baseValidation.coupon == null) {
            return baseValidation
        }

        val coupon = baseValidation.coupon
        val additionalErrors = mutableListOf<String>()

        // Check user-specific usage limits (this would require additional tracking)
        // For now, we'll implement basic validation

        // Check if campaign has user usage limits
        if (coupon.campaign.maxUsesPerUser != null) {
            // This would require a separate service to track user usage
            // For now, we'll just validate the coupon itself
        }

        return baseValidation.copy(
            errors = baseValidation.errors + additionalErrors,
            isValid = baseValidation.isValid && additionalErrors.isEmpty()
        )
    }

    /**
     * Pre-validate coupon before showing to user
     */
    fun preValidateCoupon(qrCode: String): CouponPreValidationResult {
        val coupon = couponRepository.findByQrCode(qrCode)
        if (coupon == null) {
            return CouponPreValidationResult(
                exists = false,
                isActive = false,
                isExpired = true,
                campaign = null,
                discountInfo = null
            )
        }

        val discountInfo = when {
            coupon.discountAmount != null -> "Fixed discount: ${coupon.discountAmount}"
            coupon.discountPercentage != null -> "Percentage discount: ${coupon.discountPercentage}%"
            else -> "Raffle tickets only: ${coupon.raffleTickets} tickets"
        }

        return CouponPreValidationResult(
            exists = true,
            isActive = coupon.status == CouponStatus.ACTIVE,
            isExpired = coupon.isExpired(),
            campaign = coupon.campaign.name,
            discountInfo = discountInfo
        )
    }

    /**
     * Get coupon usage statistics
     */
    fun getCouponUsageStats(couponId: Long): CouponUsageStats? {
        val coupon = couponRepository.findById(couponId).orElse(null) ?: return null

        return CouponUsageStats(
            couponId = coupon.id,
            couponCode = coupon.couponCode,
            currentUses = coupon.currentUses,
            maxUses = coupon.maxUses,
            remainingUses = coupon.getRemainingUses(),
            usageRate = if (coupon.maxUses != null) {
                (coupon.currentUses.toDouble() / coupon.maxUses.toDouble()) * 100
            } else null,
            isMaxUsesReached = coupon.isMaxUsesReached()
        )
    }

    /**
     * Validate coupon integrity
     */
    fun validateCouponIntegrity(coupon: Coupon): CouponIntegrityResult {
        val issues = mutableListOf<String>()

        // Check QR code format
        if (!qrCodeService.isValidQrCodeFormat(coupon.qrCode)) {
            issues.add("Invalid QR code format")
        }

        // Check signature
        if (!qrCodeService.validateQrSignature(coupon.qrCode, coupon.qrSignature, coupon)) {
            issues.add("Invalid QR signature")
        }

        // Check date consistency
        if (coupon.validFrom.isAfter(coupon.validUntil)) {
            issues.add("Invalid date range: validFrom is after validUntil")
        }

        // Check usage consistency
        if (coupon.maxUses != null && coupon.currentUses > coupon.maxUses) {
            issues.add("Current uses exceed maximum uses")
        }

        // Check discount consistency
        if (coupon.discountAmount != null && coupon.discountPercentage != null) {
            issues.add("Both fixed amount and percentage discount are set")
        }

        return CouponIntegrityResult(
            isIntact = issues.isEmpty(),
            issues = issues
        )
    }
}

/**
 * Coupon validation result
 */
data class CouponValidationResult(
    val isValid: Boolean,
    val coupon: Coupon?,
    val errors: List<String>,
    val canBeUsed: Boolean
)

/**
 * Coupon pre-validation result for UI display
 */
data class CouponPreValidationResult(
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
    val maxUses: Int?,
    val remainingUses: Int?,
    val usageRate: Double?,
    val isMaxUsesReached: Boolean
)

/**
 * Coupon integrity validation result
 */
data class CouponIntegrityResult(
    val isIntact: Boolean,
    val issues: List<String>
)