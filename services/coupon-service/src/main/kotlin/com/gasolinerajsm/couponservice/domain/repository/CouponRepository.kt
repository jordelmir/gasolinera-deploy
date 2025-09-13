package com.gasolinerajsm.couponservice.domain.repository

import com.gasolinerajsm.couponservice.domain.model.Coupon
import com.gasolinerajsm.couponservice.domain.model.CouponStatus
import com.gasolinerajsm.couponservice.domain.valueobject.CampaignId
import com.gasolinerajsm.couponservice.domain.valueobject.CouponCode
import com.gasolinerajsm.couponservice.domain.valueobject.CouponId
import java.time.LocalDateTime

/**
 * Coupon Repository Interface (Port)
 * Defines the contract for coupon persistence without implementation details
 */
interface CouponRepository {

    /**
     * Save a coupon entity
     */
    suspend fun save(coupon: Coupon): Result<Coupon>

    /**
     * Find coupon by ID
     */
    suspend fun findById(id: CouponId): Result<Coupon?>

    /**
     * Find coupon by coupon code
     */
    suspend fun findByCouponCode(couponCode: CouponCode): Result<Coupon?>

    /**
     * Find coupon by QR code data
     */
    suspend fun findByQRCode(qrCodeData: String): Result<Coupon?>

    /**
     * Find all coupons for a campaign
     */
    suspend fun findByCampaignId(campaignId: CampaignId): Result<List<Coupon>>

    /**
     * Find coupons by status
     */
    suspend fun findByStatus(status: CouponStatus): Result<List<Coupon>>

    /**
     * Find active coupons for a campaign
     */
    suspend fun findActiveByCampaignId(campaignId: CampaignId): Result<List<Coupon>>

    /**
     * Find expired coupons that need to be processed
     */
    suspend fun findExpiredCoupons(asOf: LocalDateTime = LocalDateTime.now()): Result<List<Coupon>>

    /**
     * Find coupons expiring soon (within specified hours)
     */
    suspend fun findCouponsExpiringSoon(withinHours: Int = 24): Result<List<Coupon>>

    /**
     * Find used coupons within date range
     */
    suspend fun findUsedCouponsInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Result<List<Coupon>>

    /**
     * Find coupons by user (if tracking user usage)
     */
    suspend fun findByUserId(userId: String): Result<List<Coupon>>

    /**
     * Check if coupon code exists
     */
    suspend fun existsByCouponCode(couponCode: CouponCode): Result<Boolean>

    /**
     * Check if QR code exists
     */
    suspend fun existsByQRCode(qrCodeData: String): Result<Boolean>

    /**
     * Count coupons by campaign and status
     */
    suspend fun countByCampaignIdAndStatus(
        campaignId: CampaignId,
        status: CouponStatus
    ): Result<Long>

    /**
     * Count total coupons by campaign
     */
    suspend fun countByCampaignId(campaignId: CampaignId): Result<Long>

    /**
     * Count used coupons by campaign
     */
    suspend fun countUsedByCampaignId(campaignId: CampaignId): Result<Long>

    /**
     * Delete coupon by ID
     */
    suspend fun deleteById(id: CouponId): Result<Unit>

    /**
     * Delete expired coupons older than specified date
     */
    suspend fun deleteExpiredCouponsOlderThan(date: LocalDateTime): Result<Int>

    /**
     * Find coupons for analytics (with pagination)
     */
    suspend fun findForAnalytics(
        campaignId: CampaignId? = null,
        status: CouponStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Coupon>>
}