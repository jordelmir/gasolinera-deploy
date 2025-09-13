package com.gasolinerajsm.coupon.repository

import com.gasolinerajsm.coupon.entity.Coupon
import com.gasolinerajsm.coupon.entity.CouponStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface CouponRepository : JpaRepository<Coupon, UUID> {
    // Métodos que faltaban y causaban errores de compilación
    fun findByCode(code: String): Coupon?
    fun findByQrCode(qrCode: String): Coupon?
    fun findByScannedBy(scannedBy: Long): List<Coupon>
    fun findByUserIdAndStatusIn(userId: UUID, statuses: List<CouponStatus>): List<Coupon>
    fun findByStationId(stationId: Long): List<Coupon>
    fun findByEmployeeId(employeeId: Long): List<Coupon>
}