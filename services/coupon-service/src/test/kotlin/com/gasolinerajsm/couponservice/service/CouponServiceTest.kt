package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.*
import com.gasolinerajsm.couponservice.repository.CouponRepository
import com.gasolinerajsm.couponservice.repository.CampaignRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@DisplayName("Coupon Service Tests")
class CouponServiceTest {

    private lateinit var couponService: CouponService
    private lateinit var couponRepository: CouponRepository
    private lateinit var campaignRepository: CampaignRepository
    private lateinit var qrCodeService: QrCodeService
    private lateinit var couponValidationService: CouponValidationService

    private lateinit var testCampaign: Campaign
    private lateinit var testCoupon: Coupon

    @BeforeEach
    fun setUp() {
        couponRepository = mock()
        campaignRepository = mock()
        qrCodeService = mock()
        couponValidationService = mock()

        couponService = CouponService(
            couponRepository,
            campaignRepository,
            qrCodeService,
            couponValidationService
        )

        testCampaign = Campaign(
            id = 1L,
            name = "Test Campaign",
            status = CampaignStatus.ACTIVE,
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now().plusDays(30),
            defaultDiscountAmount = BigDecimal("10.00"),
            defaultRaffleTickets = 5,
            maxCoupons = 1000,
            generatedCoupons = 100
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
            currentUses = 2
        )
    }    @Nes
ted
    @DisplayName("Coupon Generation Tests")
    inner class CouponGenerationTests {

        @Test
        @DisplayName("Should generate coupon successfully")
        fun shouldGenerateCouponSuccessfully() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            whenever(qrCodeService.generateQrCode(any(), any())).thenReturn("generated-qr-code")
            whenever(qrCodeService.generateQrSignature(any(), any())).thenReturn("generated-signature")
            whenever(couponRepository.existsByCouponCode(any())).thenReturn(false)
            whenever(couponRepository.save(any<Coupon>())).thenReturn(testCoupon)
            whenever(couponRepository.countByCampaign(testCampaign)).thenReturn(101L)
            whenever(couponRepository.countUsedCouponsByCampaign(testCampaign)).thenReturn(25L)

            // When
            val result = couponService.generateCoupon(
                campaignId = 1L,
                discountAmount = BigDecimal("15.00"),
                raffleTickets = 3
            )

            // Then
            assertNotNull(result)
            verify(couponRepository).save(any<Coupon>())
            verify(campaignRepository).updateCampaignCouponStats(1L, 101, 25)
        }

        @Test
        @DisplayName("Should throw exception for non-existent campaign")
        fun shouldThrowExceptionForNonExistentCampaign() {
            // Given
            whenever(campaignRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                couponService.generateCoupon(campaignId = 999L)
            }
        }

        @Test
        @DisplayName("Should throw exception when campaign cannot generate more coupons")
        fun shouldThrowExceptionWhenCampaignCannotGenerateMoreCoupons() {
            // Given
            val fullCampaign = testCampaign.copy(maxCoupons = 100, generatedCoupons = 100)
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(fullCampaign))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                couponService.generateCoupon(campaignId = 1L)
            }
        }

        @Test
        @DisplayName("Should throw exception for campaign with invalid status")
        fun shouldThrowExceptionForCampaignWithInvalidStatus() {
            // Given
            val completedCampaign = testCampaign.copy(status = CampaignStatus.COMPLETED)
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(completedCampaign))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                couponService.generateCoupon(campaignId = 1L)
            }
        }

        @Test
        @DisplayName("Should generate multiple coupons successfully")
        fun shouldGenerateMultipleCouponsSuccessfully() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            whenever(qrCodeService.generateQrCode(any(), any())).thenReturn("generated-qr-code")
            whenever(qrCodeService.generateQrSignature(any(), any())).thenReturn("generated-signature")
            whenever(couponRepository.existsByCouponCode(any())).thenReturn(false)
            whenever(couponRepository.save(any<Coupon>())).thenReturn(testCoupon)
            whenever(couponRepository.countByCampaign(testCampaign)).thenReturn(103L)
            whenever(couponRepository.countUsedCouponsByCampaign(testCampaign)).thenReturn(25L)

            // When
            val result = couponService.generateMultipleCoupons(
                campaignId = 1L,
                count = 3
            )

            // Then
            assertEquals(3, result.size)
            verify(couponRepository, times(3)).save(any<Coupon>())
        }

        @Test
        @DisplayName("Should throw exception for invalid count")
        fun shouldThrowExceptionForInvalidCount() {
            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                couponService.generateMultipleCoupons(campaignId = 1L, count = 0)
            }

            assertThrows(IllegalArgumentException::class.java) {
                couponService.generateMultipleCoupons(campaignId = 1L, count = 1001)
            }
        }
    }

    @Nested
    @DisplayName("Coupon Retrieval Tests")
    inner class CouponRetrievalTests {

        @Test
        @DisplayName("Should get coupon by ID")
        fun shouldGetCouponById() {
            // Given
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon))

            // When
            val result = couponService.getCouponById(1L)

            // Then
            assertEquals(testCoupon, result)
        }

        @Test
        @DisplayName("Should throw exception for non-existent coupon ID")
        fun shouldThrowExceptionForNonExistentCouponId() {
            // Given
            whenever(couponRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                couponService.getCouponById(999L)
            }
        }

        @Test
        @DisplayName("Should get coupon by QR code")
        fun shouldGetCouponByQrCode() {
            // Given
            whenever(couponRepository.findByQrCode("test-qr")).thenReturn(testCoupon)

            // When
            val result = couponService.getCouponByQrCode("test-qr")

            // Then
            assertEquals(testCoupon, result)
        }

        @Test
        @DisplayName("Should get coupon by coupon code")
        fun shouldGetCouponByCouponCode() {
            // Given
            whenever(couponRepository.findByCouponCode("COUP001")).thenReturn(testCoupon)

            // When
            val result = couponService.getCouponByCouponCode("COUP001")

            // Then
            assertEquals(testCoupon, result)
        }
    }

    @Nested
    @DisplayName("Coupon Usage Tests")
    inner class CouponUsageTests {

        @Test
        @DisplayName("Should use coupon successfully")
        fun shouldUseCouponSuccessfully() {
            // Given
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon))
            val usedCoupon = testCoupon.copy(currentUses = 3)
            whenever(couponRepository.save(any<Coupon>())).thenReturn(usedCoupon)
            whenever(couponRepository.countByCampaign(testCampaign)).thenReturn(100L)
            whenever(couponRepository.countUsedCouponsByCampaign(testCampaign)).thenReturn(26L)

            // When
            val result = couponService.useCoupon(1L)

            // Then
            assertEquals(3, result.currentUses)
            verify(couponRepository).save(any<Coupon>())
            verify(campaignRepository).updateCampaignCouponStats(1L, 100, 26)
        }

        @Test
        @DisplayName("Should throw exception when coupon cannot be used")
        fun shouldThrowExceptionWhenCouponCannotBeUsed() {
            // Given
            val expiredCoupon = testCoupon.copy(validUntil = LocalDateTime.now().minusDays(1))
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(expiredCoupon))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                couponService.useCoupon(1L)
            }
        }

        @Test
        @DisplayName("Should mark coupon as used up when max uses reached")
        fun shouldMarkCouponAsUsedUpWhenMaxUsesReached() {
            // Given
            val nearMaxCoupon = testCoupon.copy(maxUses = 3, currentUses = 2)
            val maxUsedCoupon = nearMaxCoupon.copy(currentUses = 3)
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(nearMaxCoupon))
            whenever(couponRepository.save(any<Coupon>())).thenReturn(maxUsedCoupon)
            whenever(couponRepository.countByCampaign(testCampaign)).thenReturn(100L)
            whenever(couponRepository.countUsedCouponsByCampaign(testCampaign)).thenReturn(26L)
            whenever(couponRepository.updateCouponStatus(1L, CouponStatus.USED_UP)).thenReturn(1)

            // When
            val result = couponService.useCoupon(1L)

            // Then
            verify(couponRepository).updateCouponStatus(1L, CouponStatus.USED_UP)
        }
    }

    @Nested
    @DisplayName("Coupon Status Management Tests")
    inner class CouponStatusManagementTests {

        @Test
        @DisplayName("Should activate coupon")
        fun shouldActivateCoupon() {
            // Given
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon))
            whenever(couponRepository.updateCouponStatus(1L, CouponStatus.ACTIVE)).thenReturn(1)

            // When
            val result = couponService.activateCoupon(1L)

            // Then
            verify(couponRepository).updateCouponStatus(1L, CouponStatus.ACTIVE)
        }

        @Test
        @DisplayName("Should deactivate coupon")
        fun shouldDeactivateCoupon() {
            // Given
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon))
            whenever(couponRepository.updateCouponStatus(1L, CouponStatus.INACTIVE)).thenReturn(1)

            // When
            val result = couponService.deactivateCoupon(1L)

            // Then
            verify(couponRepository).updateCouponStatus(1L, CouponStatus.INACTIVE)
        }

        @Test
        @DisplayName("Should not change status from final state")
        fun shouldNotChangeStatusFromFinalState() {
            // Given
            val cancelledCoupon = testCoupon.copy(status = CouponStatus.CANCELLED)
            whenever(couponRepository.findById(1L)).thenReturn(Optional.of(cancelledCoupon))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                couponService.activateCoupon(1L)
            }
        }
    }

    @Nested
    @DisplayName("Coupon Validation Tests")
    inner class CouponValidationTests {

        @Test
        @DisplayName("Should validate coupon for redemption")
        fun shouldValidateCouponForRedemption() {
            // Given
            val validationResult = CouponValidationResult(
                isValid = true,
                coupon = testCoupon,
                errors = emptyList(),
                canBeUsed = true
            )
            whenever(couponValidationService.validateCouponForRedemption(any(), any(), any(), any()))
                .thenReturn(validationResult)

            // When
            val result = couponService.validateCouponForRedemption(
                qrCode = "test-qr",
                stationId = 1L,
                fuelType = "Regular",
                purchaseAmount = BigDecimal("75.00")
            )

            // Then
            assertTrue(result.isValid)
            assertTrue(result.canBeUsed)
            assertEquals(testCoupon, result.coupon)
        }
    }

    @Nested
    @DisplayName("Coupon Statistics Tests")
    inner class CouponStatisticsTests {

        @Test
        @DisplayName("Should get coupon statistics by campaign")
        fun shouldGetCouponStatisticsByCampaign() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            val stats = mapOf("totalCoupons" to 100L, "usedCoupons" to 25L)
            whenever(couponRepository.getCouponStatisticsByCampaign(testCampaign)).thenReturn(stats)

            // When
            val result = couponService.getCouponStatisticsByCampaign(1L)

            // Then
            assertEquals(stats, result)
        }

        @Test
        @DisplayName("Should get overall coupon statistics")
        fun shouldGetOverallCouponStatistics() {
            // Given
            val stats = mapOf("totalCoupons" to 1000L, "totalCampaigns" to 10L)
            whenever(couponRepository.getOverallCouponStatistics()).thenReturn(stats)

            // When
            val result = couponService.getOverallCouponStatistics()

            // Then
            assertEquals(stats, result)
        }
    }

    @Nested
    @DisplayName("Coupon Maintenance Tests")
    inner class CouponMaintenanceTests {

        @Test
        @DisplayName("Should update expired coupons status")
        fun shouldUpdateExpiredCouponsStatus() {
            // Given
            whenever(couponRepository.updateExpiredCouponsStatus(any())).thenReturn(5)

            // When
            val result = couponService.updateExpiredCouponsStatus()

            // Then
            assertEquals(5, result)
        }

        @Test
        @DisplayName("Should update used up coupons status")
        fun shouldUpdateUsedUpCouponsStatus() {
            // Given
            whenever(couponRepository.updateUsedUpCouponsStatus()).thenReturn(3)

            // When
            val result = couponService.updateUsedUpCouponsStatus()

            // Then
            assertEquals(3, result)
        }

        @Test
        @DisplayName("Should find duplicate QR codes")
        fun shouldFindDuplicateQrCodes() {
            // Given
            val duplicates = listOf(testCoupon)
            whenever(couponRepository.findDuplicateQrCodes()).thenReturn(duplicates)

            // When
            val result = couponService.findDuplicateQrCodes()

            // Then
            assertEquals(duplicates, result)
        }
    }
}