package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.*
import com.gasolinerajsm.couponservice.repository.CouponRepository
import com.gasolinerajsm.couponservice.repository.CampaignRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing coupons - generation, validation, and lifecycle management
 */
@Service
@Transactional
class CouponService(
    private val couponRepository: CouponRepository,
    private val campaignRepository: CampaignRepository,
    private val qrCodeService: QrCodeService,
    private val couponValidationService: CouponValidationService
) {

    /**
     * Generate a new coupon for a campaign
     */
    fun generateCoupon(
        campaignId: Long,
        couponCode: String? = null,
        discountAmount: BigDecimal? = null,
        discountPercentage: BigDecimal? = null,
        raffleTickets: Int? = null,
        validFrom: LocalDateTime? = null,
        validUntil: LocalDateTime? = null,
        maxUses: Int? = null,
        minimumPurchaseAmount: BigDecimal? = null,
        applicableFuelTypes: String? = null,
        applicableStations: String? = null,
        termsAndConditions: String? = null
    ): Coupon {
        val campaign = campaignRepository.findById(campaignId)
            .orElseThrow { IllegalArgumentException("Campaign not found with id: $campaignId") }

        // Validate campaign can generate more coupons
        if (!campaign.canGenerateMoreCoupons()) {
            throw IllegalStateException("Campaign has reached maximum coupon limit")
        }

        if (!campaign.status.allowsCouponGeneration()) {
            throw IllegalStateException("Campaign status does not allow coupon generation: ${campaign.status}")
        }

        // Generate unique coupon code if not provided
        val finalCouponCode = couponCode ?: generateUniqueCouponCode(campaign)

        // Use campaign defaults if values not provided
        val finalDiscountAmount = discountAmount ?: campaign.defaultDiscountAmount
        val finalDiscountPercentage = discountPercentage ?: campaign.defaultDiscountPercentage
        val finalRaffleTickets = raffleTickets ?: campaign.defaultRaffleTickets
        val finalValidFrom = validFrom ?: campaign.startDate
        val finalValidUntil = validUntil ?: campaign.endDate
        val finalMaxUses = maxUses ?: campaign.maxUsesPerCoupon
        val finalMinimumPurchase = minimumPurchaseAmount ?: campaign.minimumPurchaseAmount
        val finalApplicableFuelTypes = applicableFuelTypes ?: campaign.applicableFuelTypes
        val finalApplicableStations = applicableStations ?: campaign.applicableStations
        val finalTermsAndConditions = termsAndConditions ?: campaign.termsAndConditions

        // Validate discount configuration
        if (finalDiscountAmount != null && finalDiscountPercentage != null) {
            throw IllegalArgumentException("Cannot have both fixed amount and percentage discount")
        }

        // Generate QR code
        val qrCode = qrCodeService.generateQrCode(campaign, finalCouponCode)

        // Create coupon (without signature first)
        val coupon = Coupon(
            campaign = campaign,
            qrCode = qrCode,
            qrSignature = "", // Will be set after creation
            couponCode = finalCouponCode,
            discountAmount = finalDiscountAmount,
            discountPercentage = finalDiscountPercentage,
            raffleTickets = finalRaffleTickets,
            validFrom = finalValidFrom,
            validUntil = finalValidUntil,
            maxUses = finalMaxUses,
            minimumPurchaseAmount = finalMinimumPurchase,
            applicableFuelTypes = finalApplicableFuelTypes,
            applicableStations = finalApplicableStations,
            termsAndConditions = finalTermsAndConditions
        )

        // Generate signature
        val signature = qrCodeService.generateQrSignature(qrCode, coupon)
        val couponWithSignature = coupon.copy(qrSignature = signature)

        // Save coupon
        val savedCoupon = couponRepository.save(couponWithSignature)

        // Update campaign statistics
        updateCampaignCouponStats(campaign)

        return savedCoupon
    }

    /**
     * Generate multiple coupons for a campaign
     */
    fun generateMultipleCoupons(
        campaignId: Long,
        count: Int,
        baseDiscountAmount: BigDecimal? = null,
        baseDiscountPercentage: BigDecimal? = null,
        baseRaffleTickets: Int? = null
    ): List<Coupon> {
        if (count <= 0) {
            throw IllegalArgumentException("Count must be positive")
        }

        if (count > 1000) {
            throw IllegalArgumentException("Cannot generate more than 1000 coupons at once")
        }

        val campaign = campaignRepository.findById(campaignId)
            .orElseThrow { IllegalArgumentException("Campaign not found with id: $campaignId") }

        // Check if campaign can generate the requested number of coupons
        val remainingSlots = campaign.getRemainingCouponSlots()
        if (remainingSlots != null && count > remainingSlots) {
            throw IllegalStateException("Campaign can only generate $remainingSlots more coupons")
        }

        return (1..count).map { index ->
            generateCoupon(
                campaignId = campaignId,
                discountAmount = baseDiscountAmount,
                discountPercentage = baseDiscountPercentage,
                raffleTickets = baseRaffleTickets
            )
        }
    }

    /**
     * Get coupon by ID
     */
    @Transactional(readOnly = true)
    fun getCouponById(id: Long): Coupon {
        return couponRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Coupon not found with id: $id") }
    }

    /**
     * Get coupon by QR code
     */
    @Transactional(readOnly = true)
    fun getCouponByQrCode(qrCode: String): Coupon? {
        return couponRepository.findByQrCode(qrCode)
    }

    /**
     * Get coupon by coupon code
     */
    @Transactional(readOnly = true)
    fun getCouponByCouponCode(couponCode: String): Coupon? {
        return couponRepository.findByCouponCode(couponCode)
    }

    /**
     * Get all coupons with pagination
     */
    @Transactional(readOnly = true)
    fun getAllCoupons(pageable: Pageable): Page<Coupon> {
        return couponRepository.findAll(pageable)
    }

    /**
     * Get coupons by campaign
     */
    @Transactional(readOnly = true)
    fun getCouponsByCampaign(campaignId: Long, pageable: Pageable): Page<Coupon> {
        val campaign = campaignRepository.findById(campaignId)
            .orElseThrow { IllegalArgumentException("Campaign not found with id: $campaignId") }
        return couponRepository.findByCampaign(campaign, pageable)
    }

    /**
     * Get coupons by status
     */
    @Transactional(readOnly = true)
    fun getCouponsByStatus(status: CouponStatus, pageable: Pageable): Page<Coupon> {
        return couponRepository.findByStatus(status, pageable)
    }

    /**
     * Get valid coupons
     */
    @Transactional(readOnly = true)
    fun getValidCoupons(): List<Coupon> {
        return couponRepository.findValidCoupons(LocalDateTime.now())
    }

    /**
     * Get valid coupons for station
     */
    @Transactional(readOnly = true)
    fun getValidCouponsForStation(stationId: Long): List<Coupon> {
        return couponRepository.findValidCouponsForStation(stationId, LocalDateTime.now())
    }

    /**
     * Get valid coupons for fuel type
     */
    @Transactional(readOnly = true)
    fun getValidCouponsForFuelType(fuelType: String): List<Coupon> {
        return couponRepository.findValidCouponsForFuelType(fuelType, LocalDateTime.now())
    }

    /**
     * Use a coupon (increment usage count)
     */
    fun useCoupon(couponId: Long): Coupon {
        val coupon = getCouponById(couponId)

        if (!coupon.canBeUsed()) {
            throw IllegalStateException("Coupon cannot be used: ${coupon.couponCode}")
        }

        val updatedCoupon = coupon.use()
        val savedCoupon = couponRepository.save(updatedCoupon)

        // Update campaign statistics
        updateCampaignCouponStats(coupon.campaign)

        // Check if coupon should be marked as used up
        if (savedCoupon.isMaxUsesReached()) {
            return activateCouponStatus(savedCoupon.id, CouponStatus.USED_UP)
        }

        return savedCoupon
    }

    /**
     * Activate coupon
     */
    fun activateCoupon(couponId: Long): Coupon {
        return activateCouponStatus(couponId, CouponStatus.ACTIVE)
    }

    /**
     * Deactivate coupon
     */
    fun deactivateCoupon(couponId: Long): Coupon {
        return activateCouponStatus(couponId, CouponStatus.INACTIVE)
    }

    /**
     * Expire coupon
     */
    fun expireCoupon(couponId: Long): Coupon {
        return activateCouponStatus(couponId, CouponStatus.EXPIRED)
    }

    /**
     * Cancel coupon
     */
    fun cancelCoupon(couponId: Long): Coupon {
        return activateCouponStatus(couponId, CouponStatus.CANCELLED)
    }

    /**
     * Update coupon status
     */
    private fun activateCouponStatus(couponId: Long, status: CouponStatus): Coupon {
        val coupon = getCouponById(couponId)

        // Check if status change is allowed
        if (coupon.status.isFinalState() && status != coupon.status) {
            throw IllegalStateException("Cannot change status from ${coupon.status} to $status")
        }

        couponRepository.updateCouponStatus(couponId, status)
        return getCouponById(couponId)
    }

    /**
     * Delete coupon (soft delete by setting to cancelled)
     */
    fun deleteCoupon(couponId: Long) {
        cancelCoupon(couponId)
    }

    /**
     * Get expired coupons
     */
    @Transactional(readOnly = true)
    fun getExpiredCoupons(): List<Coupon> {
        return couponRepository.findExpiredCoupons(LocalDateTime.now())
    }

    /**
     * Get coupons expiring soon
     */
    @Transactional(readOnly = true)
    fun getCouponsExpiringSoon(hours: Long = 24): List<Coupon> {
        val threshold = LocalDateTime.now().plusHours(hours)
        return couponRepository.findCouponsExpiringSoon(LocalDateTime.now(), threshold)
    }

    /**
     * Update expired coupons status
     */
    fun updateExpiredCouponsStatus(): Int {
        return couponRepository.updateExpiredCouponsStatus(LocalDateTime.now())
    }

    /**
     * Update used up coupons status
     */
    fun updateUsedUpCouponsStatus(): Int {
        return couponRepository.updateUsedUpCouponsStatus()
    }

    /**
     * Get coupon statistics by campaign
     */
    @Transactional(readOnly = true)
    fun getCouponStatisticsByCampaign(campaignId: Long): Map<String, Any> {
        val campaign = campaignRepository.findById(campaignId)
            .orElseThrow { IllegalArgumentException("Campaign not found with id: $campaignId") }
        return couponRepository.getCouponStatisticsByCampaign(campaign)
    }

    /**
     * Get overall coupon statistics
     */
    @Transactional(readOnly = true)
    fun getOverallCouponStatistics(): Map<String, Any> {
        return couponRepository.getOverallCouponStatistics()
    }

    /**
     * Find duplicate QR codes
     */
    @Transactional(readOnly = true)
    fun findDuplicateQrCodes(): List<Coupon> {
        return couponRepository.findDuplicateQrCodes()
    }

    /**
     * Find unused coupons
     */
    @Transactional(readOnly = true)
    fun findUnusedCoupons(daysOld: Long = 30): List<Coupon> {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld)
        return couponRepository.findUnusedCoupons(cutoffDate)
    }

    /**
     * Find high usage coupons
     */
    @Transactional(readOnly = true)
    fun findHighUsageCoupons(usageThreshold: Double = 80.0): List<Coupon> {
        return couponRepository.findHighUsageCoupons(usageThreshold)
    }

    /**
     * Validate coupon for redemption
     */
    @Transactional(readOnly = true)
    fun validateCouponForRedemption(
        qrCode: String,
        stationId: Long,
        fuelType: String? = null,
        purchaseAmount: BigDecimal? = null
    ): CouponValidationResult {
        return couponValidationService.validateCouponForRedemption(qrCode, stationId, fuelType, purchaseAmount)
    }

    /**
     * Generate unique coupon code
     */
    private fun generateUniqueCouponCode(campaign: Campaign): String {
        var attempts = 0
        val maxAttempts = 10

        while (attempts < maxAttempts) {
            val code = generateCouponCode(campaign)
            if (!couponRepository.existsByCouponCode(code)) {
                return code
            }
            attempts++
        }

        throw IllegalStateException("Unable to generate unique coupon code after $maxAttempts attempts")
    }

    /**
     * Generate coupon code based on campaign
     */
    private fun generateCouponCode(campaign: Campaign): String {
        val campaignPrefix = campaign.name.take(3).uppercase().replace(Regex("[^A-Z]"), "")
        val timestamp = System.currentTimeMillis().toString().takeLast(6)
        val random = UUID.randomUUID().toString().take(4).uppercase()

        return "${campaignPrefix}${timestamp}${random}"
    }

    /**
     * Update campaign coupon statistics
     */
    private fun updateCampaignCouponStats(campaign: Campaign) {
        val totalCoupons = couponRepository.countByCampaign(campaign).toInt()
        val usedCoupons = couponRepository.countUsedCouponsByCampaign(campaign).toInt()

        campaignRepository.updateCampaignCouponStats(campaign.id, totalCoupons, usedCoupons)
    }

    /**
     * Check if QR code exists
     */
    @Transactional(readOnly = true)
    fun qrCodeExists(qrCode: String): Boolean {
        return couponRepository.existsByQrCode(qrCode)
    }

    /**
     * Check if coupon code exists
     */
    @Transactional(readOnly = true)
    fun couponCodeExists(couponCode: String): Boolean {
        return couponRepository.existsByCouponCode(couponCode)
    }

    /**
     * Get coupon usage statistics
     */
    @Transactional(readOnly = true)
    fun getCouponUsageStats(couponId: Long): CouponUsageStats? {
        return couponValidationService.getCouponUsageStats(couponId)
    }

    /**
     * Validate coupon integrity
     */
    @Transactional(readOnly = true)
    fun validateCouponIntegrity(couponId: Long): CouponIntegrityResult {
        val coupon = getCouponById(couponId)
        return couponValidationService.validateCouponIntegrity(coupon)
    }
}