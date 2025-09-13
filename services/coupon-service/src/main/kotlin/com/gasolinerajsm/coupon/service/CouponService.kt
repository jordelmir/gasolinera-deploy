package com.gasolinerajsm.coupon.service

import com.gasolinerajsm.coupon.dto.CouponResponse
import com.gasolinerajsm.coupon.entity.Coupon
import com.gasolinerajsm.coupon.entity.CouponStatus
import com.gasolinerajsm.coupon.repository.CouponRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class CouponService(private val couponRepository: CouponRepository) {

    fun getAllCoupons(): List<CouponResponse> {
        return couponRepository.findAll().map { it.toResponse() }
    }

    fun getCouponById(id: Long): CouponResponse? {
        return couponRepository.findById(id).orElse(null)?.toResponse()
    }

    fun createCoupon(couponRequest: Coupon): CouponResponse {
        val newCoupon = couponRepository.save(couponRequest)
        return newCoupon.toResponse()
    }

    fun redeemCoupon(id: Long): CouponResponse {
        val coupon = couponRepository.findById(id).orElseThrow { RuntimeException("Coupon not found") }
        if (coupon.status == CouponStatus.REDEEMED || coupon.isExpired()) {
            throw IllegalStateException("Coupon is already redeemed or has expired.")
        }
        val updatedCoupon = couponRepository.save(coupon.redeem())
        return updatedCoupon.toResponse()
    }

    fun getCouponsByUser(userId: Long): List<CouponResponse> {
        return couponRepository.findByUserIdAndStatusIn(userId, listOf(CouponStatus.ACTIVE, CouponStatus.REDEEMED))
            .map { it.toResponse() }
    }

    // Método de extensión para convertir una entidad Coupon a un DTO CouponResponse
    private fun Coupon.toResponse(): CouponResponse {
        return CouponResponse(
            id = this.id,
            status = this.status,
            generatedAt = this.createdAt,
            expiresAt = this.validUntil,
            redeemedAt = null,
            qrCode = this.qrCode
        )
    }
}