package com.gasolinerajsm.coupon.dto

import com.gasolinerajsm.coupon.entity.CouponStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

// Este es el DTO que faltaba. Contiene todos los campos que se usan
// en el servicio y el controlador para representar la respuesta de un cupón.
data class CouponResponse(
    val id: UUID,
    val code: String,
    val value: BigDecimal,
    val status: CouponStatus,
    val generatedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val redeemedAt: LocalDateTime?,
    val userId: UUID?,
    val stationId: Long?,
    val employeeId: Long?,
    val qrCode: String?,
    val token: String?,
    val amount: BigDecimal?,
    val baseTickets: Int?,
    val totalTickets: Int?
)