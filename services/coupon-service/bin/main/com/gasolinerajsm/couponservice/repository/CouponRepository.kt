package com.gasolinerajsm.couponservice.repository

import com.gasolinerajsm.couponservice.model.Campaign
import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CouponStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CouponRepository : JpaRepository<Coupon, Long> {

    fun findByCode(code: String): Coupon?

    fun existsByCode(code: String): Boolean

    fun findByStatus(status: CouponStatus): List<Coupon>

    fun findByNameContainingIgnoreCase(name: String): List<Coupon>

    fun findByQrCode(qrCode: String): Coupon?

    fun findByCouponCode(couponCode: String): Coupon?

    // Campaign-related methods
    fun countByCampaign(campaign: Campaign): Long

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.campaign = :campaign AND c.status = 'USED'")
    fun countUsedCouponsByCampaign(@Param("campaign") campaign: Campaign): Long

    fun findByCampaign(campaign: Campaign): List<Coupon>

    fun findByCampaignAndStatus(campaign: Campaign, status: CouponStatus): List<Coupon>
}