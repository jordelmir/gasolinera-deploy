package com.gasolinerajsm.coupon.service

import com.gasolinerajsm.coupon.domain.QRCoupon
import com.gasolinerajsm.coupon.dto.GenerateQRRequest
import com.gasolinerajsm.coupon.repository.QRCouponRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class QRCouponServiceTest {

    private lateinit var couponRepository: QRCouponRepository
    private lateinit var qrCodeGenerator: QRCodeGenerator
    private lateinit var tokenGenerator: TokenGenerator
    private lateinit var rabbitTemplate: RabbitTemplate
    private lateinit var redisTemplate: RedisTemplate<String, Any>
    private lateinit var valueOperations: ValueOperations<String, Any>

    private lateinit var qrCouponService: QRCouponService

    @BeforeEach
    fun setUp() {
        couponRepository = mockk()
        qrCodeGenerator = mockk()
        tokenGenerator = mockk()
        rabbitTemplate = mockk(relaxed = true) // relaxed = true to avoid stubbing all calls
        redisTemplate = mockk()
        valueOperations = mockk()

        every { redisTemplate.opsForValue() } returns valueOperations
        every { valueOperations.set(any(), any(), any<Long>(), any()) } returns Unit

        qrCouponService = QRCouponService(
            couponRepository,
            qrCodeGenerator,
            tokenGenerator,
            rabbitTemplate,
            redisTemplate
        )
    }

    @Test
    fun `generateQRCoupon should create and save a new QR coupon`() {
        // Given
        val request = GenerateQRRequest(
            stationId = UUID.randomUUID(),
            employeeId = UUID.randomUUID(),
            amount = 10000
        )
        val generatedToken = "test-token-123"
        val generatedQrCode = "test-qr-code-abc"
        val savedCoupon = QRCoupon(
            id = UUID.randomUUID(),
            qrCode = generatedQrCode,
            token = generatedToken,
            stationId = request.stationId,
            employeeId = request.employeeId,
            amount = request.amount,
            baseTickets = request.amount / 5000, // Assuming this logic
            totalTickets = request.amount / 5000,
            expiresAt = LocalDateTime.now().plusHours(24)
        )

        every { tokenGenerator.generateUniqueToken() } returns generatedToken
        every { qrCodeGenerator.generateQRCode(generatedToken) } returns generatedQrCode
        every { couponRepository.save(any<QRCoupon>()) } returns savedCoupon

        // When
        val result = qrCouponService.generateQRCoupon(request)

        // Then
        verify(exactly = 1) { tokenGenerator.generateUniqueToken() }
        verify(exactly = 1) { qrCodeGenerator.generateQRCode(generatedToken) }
        verify(exactly = 1) { couponRepository.save(any<QRCoupon>()) }
        verify(exactly = 1) { 
            valueOperations.set(
                "qr:${generatedQrCode}",
                savedCoupon.id.toString(),
                24,
                TimeUnit.HOURS
            )
        }

        // Assertions on the returned coupon
        assert(result.qrCode == generatedQrCode)
        assert(result.token == generatedToken)
        assert(result.stationId == request.stationId)
        assert(result.employeeId == request.employeeId)
        assert(result.amount == request.amount)
        assert(result.baseTickets == request.amount / 5000)
        assert(result.totalTickets == request.amount / 5000)
        assert(result.expiresAt.isAfter(LocalDateTime.now()))
    }
}
