package com.gasolinerajsm.coupon.repository

import com.gasolinerajsm.coupon.domain.QRCoupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface QRCouponRepository : JpaRepository<QRCoupon, UUID> {
    fun findByCode(code: String): QRCoupon?
}
