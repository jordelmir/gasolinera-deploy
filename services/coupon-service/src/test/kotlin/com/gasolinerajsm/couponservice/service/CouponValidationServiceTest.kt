package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.*
import com.gasolinerajsm.couponservice.repository.CouponRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@DisplayName("Coupon Validation Service Tests")
class CouponValidationServiceTest {

    private lateinit var couponValidationService: CouponValidationService
    private lateinit var couponRepository: CouponRepository
    private lateinit var qrCodeService: QrCodeService

    private lateinit var testCampaign: Campaign
    private lateinit var testCoupon: Coupon

    @BeforeEach
    fun setUp() {
        couponRepository = mock()
        qrCodeService = mock()
        couponValidationService = CouponValidationService(couponRepository, qrCodeService)

        testCampaign = Campaign(
            id = 1L,
            name = "Test Campaign",
            status = CampaignStatus.ACTIVE,
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now().plusDays(30)
        )

        testCoupon = Coupon(
            id = 1L,
            campaign = testCampaign,
            qrCode = "GSL_v1_000001_20240315120000_ABC12345_COUP001",
            qrSignature = "valid-signature",
            couponCode = "COUP001",
            status = CouponStatus.ACTIVE,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30),
            discountAmount = BigDecimal("10.00"),
            raffleTickets = 5,
            maxUses = 10,
            currentUses = 2,
            minimumPurchaseAmount = BigDecimal("50.00"),
            applicableFuelTypes = "Regular, Premium",
            applicableStations = "1, 2, 3"
        )
    }

    @Nested
    @DisplayName("Coupon Validation Tests")
    inner class CouponValidationTests {

        @Test
        @DisplayName("Should validate valid coupon successfully")
        fun shouldValidateValidCouponSuccessfully() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L,
                fuelType = "Regular",
                purchaseAmount = BigDecimal("75.00")
            )

            // Then
            assertTrue(result.isValid)
            assertTrue(result.canBeUsed)
            assertEquals(testCoupon, result.coupon)
            assertTrue(result.errors.isEmpty())
        }

        @Test
        @DisplayName("Should reject non-existent coupon")
        fun shouldRejectNonExistentCoupon() {
            // Given
            whenever(couponRepository.findByQrCode("INVALID_QR")).thenReturn(null)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = "INVALID_QR",
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertFalse(result.canBeUsed)
            assertNull(result.coupon)
            assertTrue(result.errors.contains("Coupon not found"))
        }

        @Test
        @DisplayName("Should reject coupon with invalid QR format")
        fun shouldRejectCouponWithInvalidQrFormat() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Invalid QR code format"))
        }

        @Test
        @DisplayName("Should reject coupon with invalid signature")
        fun shouldRejectCouponWithInvalidSignature() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Invalid QR code signature - possible tampering detected"))
        }

        @Test
        @DisplayName("Should reject inactive coupon")
        fun shouldRejectInactiveCoupon() {
            // Given
            val inactiveCoupon = testCoupon.copy(status = CouponStatus.INACTIVE)
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(inactiveCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, inactiveCoupon)).thenReturn(true)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Coupon is not active (status: Inactive)"))
        }

        @Test
        @DisplayName("Should reject expired coupon")
        fun shouldRejectExpiredCoupon() {
            // Given
            val expiredCoupon = testCoupon.copy(validUntil = LocalDateTime.now().minusDays(1))
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(expiredCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, expiredCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Coupon has expired"))
        }

        @Test
        @DisplayName("Should reject coupon not yet valid")
        fun shouldRejectCouponNotYetValid() {
            // Given
            val futureCoupon = testCoupon.copy(validFrom = LocalDateTime.now().plusDays(1))
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(futureCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, futureCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Coupon is not yet valid"))
        }

        @Test
        @DisplayName("Should reject coupon with max uses reached")
        fun shouldRejectCouponWithMaxUsesReached() {
            // Given
            val maxUsedCoupon = testCoupon.copy(maxUses = 5, currentUses = 5)
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(maxUsedCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, maxUsedCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Coupon has reached maximum usage limit"))
        }

        @Test
        @DisplayName("Should reject coupon for inactive campaign")
        fun shouldRejectCouponForInactiveCampaign() {
            // Given
            val inactiveCampaign = testCampaign.copy(status = CampaignStatus.PAUSED)
            val couponWithInactiveCampaign = testCoupon.copy(campaign = inactiveCampaign)
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(couponWithInactiveCampaign)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, couponWithInactiveCampaign)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Campaign is not active"))
        }

        @Test
        @DisplayName("Should reject coupon for wrong station")
        fun shouldRejectCouponForWrongStation() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 999L // Not in applicable stations (1, 2, 3)
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Coupon is not valid at this station"))
        }

        @Test
        @DisplayName("Should reject coupon for wrong fuel type")
        fun shouldRejectCouponForWrongFuelType() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L,
                fuelType = "Diesel" // Not in applicable fuel types (Regular, Premium)
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("Coupon is not valid for fuel type: Diesel"))
        }

        @Test
        @DisplayName("Should reject coupon for insufficient purchase amount")
        fun shouldRejectCouponForInsufficientPurchaseAmount() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L,
                purchaseAmount = BigDecimal("25.00") // Less than minimum 50.00
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Purchase amount does not meet minimum requirement") })
        }

        @Test
        @DisplayName("Should reject QR code expired by timestamp")
        fun shouldRejectQrCodeExpiredByTimestamp() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(true)

            // When
            val result = couponValidationService.validateCouponForRedemption(
                qrCode = testCoupon.qrCode,
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertTrue(result.errors.contains("QR code has expired due to age"))
        }
    }

    @Nested
    @DisplayName("Coupon Code Validation Tests")
    inner class CouponCodeValidationTests {

        @Test
        @DisplayName("Should validate coupon by coupon code")
        fun shouldValidateCouponByCouponCode() {
            // Given
            whenever(couponRepository.findByCouponCode("COUP001")).thenReturn(testCoupon)
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponByCouponCode(
                couponCode = "COUP001",
                stationId = 1L
            )

            // Then
            assertTrue(result.isValid)
            assertEquals(testCoupon, result.coupon)
        }

        @Test
        @DisplayName("Should reject non-existent coupon code")
        fun shouldRejectNonExistentCouponCode() {
            // Given
            whenever(couponRepository.findByCouponCode("INVALID")).thenReturn(null)

            // When
            val result = couponValidationService.validateCouponByCouponCode(
                couponCode = "INVALID",
                stationId = 1L
            )

            // Then
            assertFalse(result.isValid)
            assertNull(result.coupon)
            assertTrue(result.errors.contains("Coupon not found"))
        }
    }

    @Nested
    @DisplayName("Multiple Coupon Validation Tests")
    inner class MultipleCouponValidationTests {

        @Test
        @DisplayName("Should validate multiple coupons")
        fun shouldValidateMultipleCoupons() {
            // Given
            val qrCodes = listOf(testCoupon.qrCode, "INVALID_QR")
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)
            whenever(couponRepository.findByQrCode("INVALID_QR")).thenReturn(null)
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)
            whenever(qrCodeService.isQrCodeExpiredByTimestamp(testCoupon.qrCode)).thenReturn(false)

            // When
            val results = couponValidationService.validateMultipleCoupons(
                qrCodes = qrCodes,
                stationId = 1L
            )

            // Then
            assertEquals(2, results.size)
            assertTrue(results[0].isValid) // Valid coupon
            assertFalse(results[1].isValid) // Invalid coupon
        }
    }

    @Nested
    @DisplayName("Pre-validation Tests")
    inner class PreValidationTests {

        @Test
        @DisplayName("Should pre-validate existing coupon")
        fun shouldPreValidateExistingCoupon() {
            // Given
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(testCoupon)

            // When
            val result = couponValidationService.preValidateCoupon(testCoupon.qrCode)

            // Then
            assertTrue(result.exists)
            assertTrue(result.isActive)
            assertFalse(result.isExpired)
            assertEquals(testCampaign.name, result.campaign)
            assertEquals("Fixed discount: 10.00", result.discountInfo)
        }

        @Test
        @DisplayName("Should pre-validate non-existent coupon")
        fun shouldPreValidateNonExistentCoupon() {
            // Given
            whenever(couponRepository.findByQrCode("INVALID")).thenReturn(null)

            // When
            val result = couponValidationService.preValidateCoupon("INVALID")

            // Then
            assertFalse(result.exists)
            assertFalse(result.isActive)
            assertTrue(result.isExpired)
            assertNull(result.campaign)
            assertNull(result.discountInfo)
        }

        @Test
        @DisplayName("Should pre-validate percentage discount coupon")
        fun shouldPreValidatePercentageDiscountCoupon() {
            // Given
            val percentageCoupon = testCoupon.copy(
                discountAmount = null,
                discountPercentage = BigDecimal("15.00")
            )
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(percentageCoupon)

            // When
            val result = couponValidationService.preValidateCoupon(testCoupon.qrCode)

            // Then
            assertEquals("Percentage discount: 15.00%", result.discountInfo)
        }

        @Test
        @DisplayName("Should pre-validate raffle-only coupon")
        fun shouldPreValidateRaffleOnlyCoupon() {
            // Given
            val raffleCoupon = testCoupon.copy(
                discountAmount = null,
                discountPercentage = null,
                raffleTickets = 10
            )
            whenever(couponRepository.findByQrCode(testCoupon.qrCode)).thenReturn(raffleCoupon)

            // When
            val result = couponValidationService.preValidateCoupon(testCoupon.qrCode)

            // Then
            assertEquals("Raffle tickets only: 10 tickets", result.discountInfo)
        }
    }

    @Nested
    @DisplayName("Usage Statistics Tests")
    inner class UsageStatisticsTests {

        @Test
        @DisplayName("Should get coupon usage statistics")
        fun shouldGetCouponUsageStatistics() {
            // Given
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon))

            // When
            val stats = couponValidationService.getCouponUsageStats(1L)

            // Then
            assertNotNull(stats)
            assertEquals(1L, stats!!.couponId)
            assertEquals("COUP001", stats.couponCode)
            assertEquals(2, stats.currentUses)
            assertEquals(10, stats.maxUses)
            assertEquals(8, stats.remainingUses)
            assertEquals(20.0, stats.usageRate)
            assertFalse(stats.isMaxUsesReached)
        }

        @Test
        @DisplayName("Should return null for non-existent coupon")
        fun shouldReturnNullForNonExistentCoupon() {
            // Given
            whenever(couponRepository.findById(999L)).thenReturn(Optional.empty())

            // When
            val stats = couponValidationService.getCouponUsageStats(999L)

            // Then
            assertNull(stats)
        }
    }

    @Nested
    @DisplayName("Integrity Validation Tests")
    inner class IntegrityValidationTests {

        @Test
        @DisplayName("Should validate coupon integrity successfully")
        fun shouldValidateCouponIntegritySuccessfully() {
            // Given
            whenever(qrCodeService.isValidQrCodeFormat(testCoupon.qrCode)).thenReturn(true)
            whenever(qrCodeService.validateQrSignature(testCoupon.qrCode, testCoupon.qrSignature, testCoupon)).thenReturn(true)

            // When
            val result = couponValidationService.validateCouponIntegrity(testCoupon)

            // Then
            assertTrue(result.isIntact)
            assertTrue(result.issues.isEmpty())
        }

        @Test
        @DisplayName("Should detect integrity issues")
        fun shouldDetectIntegrityIssues() {
            // Given
            val corruptedCoupon = testCoupon.copy(
                validFrom = LocalDateTime.now().plusDays(1),
                validUntil = LocalDateTime.now().minusDays(1), // Invalid date range
                maxUses = 5,
                currentUses = 10, // Usage exceeds max
                discountAmount = BigDecimal("10.00"),
                discountPercentage = BigDecimal("15.00") // Both discounts set
            )
            whenever(qrCodeService.isValidQrCodeFormat(corruptedCoupon.qrCode)).thenReturn(false)
            whenever(qrCodeService.validateQrSignature(corruptedCoupon.qrCode, corruptedCoupon.qrSignature, corruptedCoupon)).thenReturn(false)

            // When
            val result = couponValidationService.validateCouponIntegrity(corruptedCoupon)

            // Then
            assertFalse(result.isIntact)
            assertTrue(result.issues.contains("Invalid QR code format"))
            assertTrue(result.issues.contains("Invalid QR signature"))
            assertTrue(result.issues.contains("Invalid date range: validFrom is after validUntil"))
            assertTrue(result.issues.contains("Current uses exceed maximum uses"))
            assertTrue(result.issues.contains("Both fixed amount and percentage discount are set"))
        }
    }
}