package com.gasolinerajsm.couponservice.controller

import com.gasolinerajsm.couponservice.dto.*
import com.gasolinerajsm.couponservice.model.CouponStatus
import com.gasolinerajsm.couponservice.service.CouponService
import com.gasolinerajsm.couponservice.service.QrCodeService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for Coupon management operations
 */
@RestController
@RequestMapping("/api/v1/coupons")
@CrossOrigin(origins = ["*"])
class CouponController(
    private val couponService: CouponService,
    private val qrCodeService: QrCodeService
) {

    /**
     * Generate a new coupon
     */
    @PostMapping("/generate")
    fun generateCoupon(@Valid @RequestBody request: GenerateCouponRequest): ResponseEntity<CouponResponse> {
        val coupon = couponService.generateCoupon(
            campaignId = request.campaignId,
            couponCode = request.couponCode,
            discountAmount = request.discountAmount,
            discountPercentage = request.discountPercentage,
            raffleTickets = request.raffleTickets,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            maxUses = request.maxUses,
            minimumPurchaseAmount = request.minimumPurchaseAmount,
            applicableFuelTypes = request.applicableFuelTypes,
            applicableStations = request.applicableStations,
            termsAndConditions = request.termsAndConditions
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.fromCoupon(coupon))
    }

    /**
     * Generate multiple coupons
     */
    @PostMapping("/generate/batch")
    fun generateMultipleCoupons(@Valid @RequestBody request: GenerateMultipleCouponsRequest): ResponseEntity<List<CouponResponse>> {
        val coupons = couponService.generateMultipleCoupons(
            campaignId = request.campaignId,
            count = request.count,
            baseDiscountAmount = request.baseDiscountAmount,
            baseDiscountPercentage = request.baseDiscountPercentage,
            baseRaffleTickets = request.baseRaffleTickets
        )

        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.status(HttpStatus.CREATED).body(responses)
    }

    /**
     * Get coupon by ID
     */
    @GetMapping("/{id}")
    fun getCouponById(@PathVariable id: Long): ResponseEntity<CouponResponse> {
        val coupon = couponService.getCouponById(id)
        return ResponseEntity.ok(CouponResponse.fromCoupon(coupon))
    }

    /**
     * Get coupon by QR code
     */
    @GetMapping("/qr/{qrCode}")
    fun getCouponByQrCode(@PathVariable qrCode: String): ResponseEntity<CouponResponse> {
        val coupon = couponService.getCouponByQrCode(qrCode)
        return if (coupon != null) {
            ResponseEntity.ok(CouponResponse.fromCoupon(coupon))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get coupon by coupon code
     */
    @GetMapping("/code/{couponCode}")
    fun getCouponByCouponCode(@PathVariable couponCode: String): ResponseEntity<CouponResponse> {
        val coupon = couponService.getCouponByCouponCode(couponCode)
        return if (coupon != null) {
            ResponseEntity.ok(CouponResponse.fromCoupon(coupon))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all coupons with pagination
     */
    @GetMapping
    fun getAllCoupons(pageable: Pageable): ResponseEntity<Page<CouponResponse>> {
        val coupons = couponService.getAllCoupons(pageable)
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get coupons by campaign
     */
    @GetMapping("/campaign/{campaignId}")
    fun getCouponsByCampaign(
        @PathVariable campaignId: Long,
        pageable: Pageable
    ): ResponseEntity<Page<CouponResponse>> {
        val coupons = couponService.getCouponsByCampaign(campaignId, pageable)
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get coupons by status
     */
    @GetMapping("/status/{status}")
    fun getCouponsByStatus(
        @PathVariable status: CouponStatus,
        pageable: Pageable
    ): ResponseEntity<Page<CouponResponse>> {
        val coupons = couponService.getCouponsByStatus(status, pageable)
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get valid coupons
     */
    @GetMapping("/valid")
    fun getValidCoupons(): ResponseEntity<List<CouponResponse>> {
        val coupons = couponService.getValidCoupons()
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get valid coupons for station
     */
    @GetMapping("/valid/station/{stationId}")
    fun getValidCouponsForStation(@PathVariable stationId: Long): ResponseEntity<List<CouponResponse>> {
        val coupons = couponService.getValidCouponsForStation(stationId)
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get valid coupons for fuel type
     */
    @GetMapping("/valid/fuel/{fuelType}")
    fun getValidCouponsForFuelType(@PathVariable fuelType: String): ResponseEntity<List<CouponResponse>> {
        val coupons = couponService.getValidCouponsForFuelType(fuelType)
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Validate coupon for redemption
     */
    @PostMapping("/validate")
    fun validateCoupon(@Valid @RequestBody request: CouponValidationRequest): ResponseEntity<CouponValidationResponse> {
        val validationResult = couponService.validateCouponForRedemption(
            qrCode = request.qrCode,
            stationId = request.stationId,
            fuelType = request.fuelType,
            purchaseAmount = request.purchaseAmount
        )

        val response = CouponValidationResponse(
            isValid = validationResult.isValid,
            canBeUsed = validationResult.canBeUsed,
            coupon = validationResult.coupon?.let { CouponResponse.fromCoupon(it) },
            errors = validationResult.errors,
            discountAmount = validationResult.coupon?.calculateDiscount(request.purchaseAmount ?: java.math.BigDecimal.ZERO),
            raffleTickets = validationResult.coupon?.raffleTickets
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Use a coupon
     */
    @PostMapping("/use")
    fun useCoupon(@Valid @RequestBody request: UseCouponRequest): ResponseEntity<UseCouponResponse> {
        // First validate the coupon
        val validationResult = couponService.validateCouponForRedemption(
            qrCode = couponService.getCouponById(request.couponId).qrCode,
            stationId = request.stationId,
            fuelType = request.fuelType,
            purchaseAmount = request.purchaseAmount
        )

        if (!validationResult.isValid || !validationResult.canBeUsed) {
            throw IllegalStateException("Coupon cannot be used: ${validationResult.errors.joinToString(", ")}")
        }

        val usedCoupon = couponService.useCoupon(request.couponId)
        val discountApplied = usedCoupon.calculateDiscount(request.purchaseAmount ?: java.math.BigDecimal.ZERO)

        val response = UseCouponResponse(
            success = true,
            coupon = CouponResponse.fromCoupon(usedCoupon),
            discountApplied = discountApplied,
            raffleTicketsEarned = usedCoupon.raffleTickets,
            remainingUses = usedCoupon.getRemainingUses(),
            message = "Coupon used successfully"
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Activate coupon
     */
    @PostMapping("/{id}/activate")
    fun activateCoupon(@PathVariable id: Long): ResponseEntity<CouponResponse> {
        val coupon = couponService.activateCoupon(id)
        return ResponseEntity.ok(CouponResponse.fromCoupon(coupon))
    }

    /**
     * Deactivate coupon
     */
    @PostMapping("/{id}/deactivate")
    fun deactivateCoupon(@PathVariable id: Long): ResponseEntity<CouponResponse> {
        val coupon = couponService.deactivateCoupon(id)
        return ResponseEntity.ok(CouponResponse.fromCoupon(coupon))
    }

    /**
     * Expire coupon
     */
    @PostMapping("/{id}/expire")
    fun expireCoupon(@PathVariable id: Long): ResponseEntity<CouponResponse> {
        val coupon = couponService.expireCoupon(id)
        return ResponseEntity.ok(CouponResponse.fromCoupon(coupon))
    }

    /**
     * Cancel coupon
     */
    @PostMapping("/{id}/cancel")
    fun cancelCoupon(@PathVariable id: Long): ResponseEntity<CouponResponse> {
        val coupon = couponService.cancelCoupon(id)
        return ResponseEntity.ok(CouponResponse.fromCoupon(coupon))
    }

    /**
     * Delete coupon
     */
    @DeleteMapping("/{id}")
    fun deleteCoupon(@PathVariable id: Long): ResponseEntity<Void> {
        couponService.deleteCoupon(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Get QR code data for coupon
     */
    @GetMapping("/{id}/qr-data")
    fun getQrCodeData(@PathVariable id: Long): ResponseEntity<QrCodeDataResponse> {
        val coupon = couponService.getCouponById(id)
        val qrCodeData = qrCodeService.generateQrCodeData(coupon)

        val response = QrCodeDataResponse(
            qrCode = qrCodeData.qrCode,
            signature = qrCodeData.signature,
            couponCode = qrCodeData.couponCode,
            campaignId = qrCodeData.campaignId,
            campaignName = qrCodeData.campaignName,
            discountAmount = qrCodeData.discountAmount,
            discountPercentage = qrCodeData.discountPercentage,
            raffleTickets = qrCodeData.raffleTickets,
            validFrom = qrCodeData.validFrom,
            validUntil = qrCodeData.validUntil,
            termsAndConditions = qrCodeData.termsAndConditions
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get expired coupons
     */
    @GetMapping("/expired")
    fun getExpiredCoupons(): ResponseEntity<List<CouponResponse>> {
        val coupons = couponService.getExpiredCoupons()
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Get coupons expiring soon
     */
    @GetMapping("/expiring-soon")
    fun getCouponsExpiringSoon(@RequestParam(defaultValue = "24") hours: Long): ResponseEntity<List<CouponResponse>> {
        val coupons = couponService.getCouponsExpiringSoon(hours)
        val responses = coupons.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Update expired coupons status
     */
    @PostMapping("/maintenance/update-expired")
    fun updateExpiredCouponsStatus(): ResponseEntity<Map<String, Int>> {
        val updatedCount = couponService.updateExpiredCouponsStatus()
        val response = mapOf("updated_count" to updatedCount)
        return ResponseEntity.ok(response)
    }

    /**
     * Update used up coupons status
     */
    @PostMapping("/maintenance/update-used-up")
    fun updateUsedUpCouponsStatus(): ResponseEntity<Map<String, Int>> {
        val updatedCount = couponService.updateUsedUpCouponsStatus()
        val response = mapOf("updated_count" to updatedCount)
        return ResponseEntity.ok(response)
    }

    /**
     * Get coupon statistics by campaign
     */
    @GetMapping("/statistics/campaign/{campaignId}")
    fun getCouponStatisticsByCampaign(@PathVariable campaignId: Long): ResponseEntity<Map<String, Any>> {
        val statistics = couponService.getCouponStatisticsByCampaign(campaignId)
        return ResponseEntity.ok(statistics)
    }

    /**
     * Get overall coupon statistics
     */
    @GetMapping("/statistics")
    fun getOverallCouponStatistics(): ResponseEntity<CouponStatisticsResponse> {
        val stats = couponService.getOverallCouponStatistics()

        val response = CouponStatisticsResponse(
            totalCoupons = stats["totalCoupons"] as? Long ?: 0L,
            activeCoupons = stats["activeCoupons"] as? Long ?: 0L,
            expiredCoupons = stats["expiredCoupons"] as? Long ?: 0L,
            usedUpCoupons = stats["usedUpCoupons"] as? Long ?: 0L,
            usedCoupons = stats["usedCoupons"] as? Long ?: 0L,
            totalUses = stats["totalUses"] as? Long ?: 0L,
            averageUses = stats["averageUses"] as? Double ?: 0.0,
            totalCampaigns = stats["totalCampaigns"] as? Long ?: 0L,
            usageRate = if ((stats["totalCoupons"] as? Long ?: 0L) > 0) {
                ((stats["usedCoupons"] as? Long ?: 0L).toDouble() / (stats["totalCoupons"] as? Long ?: 1L).toDouble()) * 100
            } else 0.0
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Find duplicate QR codes
     */
    @GetMapping("/maintenance/duplicates/qr-codes")
    fun findDuplicateQrCodes(): ResponseEntity<List<CouponResponse>> {
        val duplicates = couponService.findDuplicateQrCodes()
        val responses = duplicates.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Find unused coupons
     */
    @GetMapping("/maintenance/unused")
    fun findUnusedCoupons(@RequestParam(defaultValue = "30") daysOld: Long): ResponseEntity<List<CouponResponse>> {
        val unused = couponService.findUnusedCoupons(daysOld)
        val responses = unused.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Find high usage coupons
     */
    @GetMapping("/maintenance/high-usage")
    fun findHighUsageCoupons(@RequestParam(defaultValue = "80.0") threshold: Double): ResponseEntity<List<CouponResponse>> {
        val highUsage = couponService.findHighUsageCoupons(threshold)
        val responses = highUsage.map { CouponResponse.fromCoupon(it) }
        return ResponseEntity.ok(responses)
    }

    /**
     * Check if QR code exists
     */
    @GetMapping("/exists/qr/{qrCode}")
    fun qrCodeExists(@PathVariable qrCode: String): ResponseEntity<Map<String, Boolean>> {
        val exists = couponService.qrCodeExists(qrCode)
        val response = mapOf("exists" to exists)
        return ResponseEntity.ok(response)
    }

    /**
     * Check if coupon code exists
     */
    @GetMapping("/exists/code/{couponCode}")
    fun couponCodeExists(@PathVariable couponCode: String): ResponseEntity<Map<String, Boolean>> {
        val exists = couponService.couponCodeExists(couponCode)
        val response = mapOf("exists" to exists)
        return ResponseEntity.ok(response)
    }

    /**
     * Get coupon usage statistics
     */
    @GetMapping("/{id}/usage-stats")
    fun getCouponUsageStats(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val stats = couponService.getCouponUsageStats(id)
        return if (stats != null) {
            val response = mapOf(
                "coupon_id" to stats.couponId,
                "coupon_code" to stats.couponCode,
                "current_uses" to stats.currentUses,
                "max_uses" to stats.maxUses,
                "remaining_uses" to stats.remainingUses,
                "usage_rate" to stats.usageRate,
                "is_max_uses_reached" to stats.isMaxUsesReached
            )
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Validate coupon integrity
     */
    @GetMapping("/{id}/integrity")
    fun validateCouponIntegrity(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val integrityResult = couponService.validateCouponIntegrity(id)
        val response = mapOf(
            "is_intact" to integrityResult.isIntact,
            "issues" to integrityResult.issues
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        val response = mapOf(
            "status" to "UP",
            "service" to "Coupon Service",
            "timestamp" to java.time.Instant.now().toString()
        )
        return ResponseEntity.ok(response)
    }
}