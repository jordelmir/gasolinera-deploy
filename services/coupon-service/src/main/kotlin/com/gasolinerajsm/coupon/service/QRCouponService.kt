package com.gasolinerajsm.coupon.service

import com.gasolinerajsm.coupon.domain.CouponStatus
import com.gasolinerajsm.coupon.domain.QRCoupon
import com.gasolinerajsm.coupon.dto.GenerateQRRequest
import com.gasolinerajsm.coupon.dto.ScanQRRequest
import com.gasolinerajsm.coupon.dto.ActivateCouponRequest
import com.gasolinerajsm.coupon.repository.QRCouponRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@Service
@Transactional
class QRCouponService(
    private val couponRepository: QRCouponRepository,
    private val qrCodeGenerator: QRCodeGenerator,
    private val tokenGenerator: TokenGenerator,
    private val rabbitTemplate: RabbitTemplate,
    private val redisTemplate: RedisTemplate<String, Any>
) {

    fun generateQRCoupon(request: GenerateQRRequest): QRCoupon {
        val token = tokenGenerator.generateUniqueToken()
        val qrCode = qrCodeGenerator.generateQRCode(token)
        val baseTickets = request.amount // 1 ticket por cada múltiplo de 5000

        val coupon = QRCoupon(
            qrCode = qrCode,
            token = token,
            stationId = request.stationId,
            employeeId = request.employeeId,
            amount = request.amount,
            baseTickets = baseTickets,
            totalTickets = baseTickets,
            expiresAt = LocalDateTime.now().plusHours(24) // Expira en 24 horas
        )

        val savedCoupon = couponRepository.save(coupon)

        // Cache QR code for quick lookup
        redisTemplate.opsForValue().set(
            "qr:${qrCode}",
            savedCoupon.id.toString(),
            24,
            TimeUnit.HOURS
        )

        return savedCoupon
    }

    fun scanQRCoupon(request: ScanQRRequest): QRCoupon {
        val coupon = couponRepository.findByQrCode(request.qrCode)
            ?: throw IllegalArgumentException("QR Code no válido")

        if (coupon.status != CouponStatus.GENERATED) {
            throw IllegalStateException("Este QR ya fue utilizado")
        }

        if (coupon.expiresAt.isBefore(LocalDateTime.now())) {
            throw IllegalStateException("Este QR ha expirado")
        }

        val updatedCoupon = coupon.copy(
            status = CouponStatus.SCANNED,
            scannedBy = request.userId,
            scannedAt = LocalDateTime.now()
        )

        return couponRepository.save(updatedCoupon)
    }

    fun activateCoupon(request: ActivateCouponRequest): QRCoupon {
        val coupon = couponRepository.findById(request.couponId)
            .orElseThrow { IllegalArgumentException("Cupón no encontrado") }

        if (coupon.scannedBy != request.userId) {
            throw IllegalArgumentException("No tienes permisos para activar este cupón")
        }

        if (coupon.status != CouponStatus.SCANNED) {
            throw IllegalStateException("Este cupón no puede ser activado")
        }

        val updatedCoupon = coupon.copy(
            status = CouponStatus.ACTIVATED,
            activatedAt = LocalDateTime.now()
        )

        val savedCoupon = couponRepository.save(updatedCoupon)

        // Publicar evento para iniciar secuencia de anuncios
        rabbitTemplate.convertAndSend(
            "coupon.exchange",
            "coupon.activated",
            mapOf(
                "couponId" to savedCoupon.id,
                "userId" to request.userId,
                "baseTickets" to savedCoupon.baseTickets
            )
        )

        return savedCoupon
    }

    fun getUserCoupons(userId: UUID): List<QRCoupon> {
        return couponRepository.findByScannedBy(userId)
    }

    fun getUserActiveTickets(userId: UUID): Int {
        val activeCoupons = couponRepository.findByUserIdAndStatuses(
            userId,
            listOf(CouponStatus.ACTIVATED, CouponStatus.COMPLETED)
        )
        return activeCoupons.sumOf { it.totalTickets }
    }

    fun getStationStats(stationId: UUID, days: Int = 30): Map<String, Any> {
        val coupons = couponRepository.findByStationId(stationId)

        return mapOf(
            "totalCoupons" to coupons.size,
            "totalTickets" to coupons.sumOf { it.totalTickets },
            "activeCoupons" to coupons.count { it.status in listOf(CouponStatus.ACTIVATED, CouponStatus.COMPLETED) },
            "expiredCoupons" to coupons.count { it.status == CouponStatus.EXPIRED }
        )
    }

    fun getEmployeeStats(employeeId: UUID, days: Int = 30): Map<String, Any> {
        val coupons = couponRepository.findByEmployeeId(employeeId)

        return mapOf(
            "totalCoupons" to coupons.size,
            "totalTickets" to coupons.sumOf { it.totalTickets },
            "scannedCoupons" to coupons.count { it.scannedBy != null },
            "conversionRate" to if (coupons.isNotEmpty()) {
                (coupons.count { it.scannedBy != null }.toDouble() / coupons.size * 100)
            } else 0.0
        )
    }
}