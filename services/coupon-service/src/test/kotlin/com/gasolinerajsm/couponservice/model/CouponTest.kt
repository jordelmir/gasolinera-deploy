package com.gasolinerajsm.couponservice.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("Coupon Entity Tests")
class CouponTest {

    private val testCampaign = Campaign(
        id = 1L,
        name = "Test Campaign",
        startDate = LocalDateTime.now().minusDays(1),
        endDate = LocalDateTime.now().plusDays(30)
    )

    @Nested
    @DisplayName("Coupon Creation Tests")
    inner class CouponCreationTests {

        @Test
        @DisplayName("Should create coupon with valid data")
        fun shouldCreateCouponWithValidData() {
            // Given
            val qrCode = "QR123456789"
            val qrSignature = "signature123"
            val couponCode = "COUP001"
            val validFrom = LocalDateTime.now().minusDays(1)
            val validUntil = LocalDateTime.now().plusDays(30)

            // When
            val coupon = Coupon(
                campaign = testCampaign,
                qrCode = qrCode,
                qrSignature = qrSignature,
                couponCode = couponCode,
                validFrom = validFrom,
                validUntil = validUntil,
                discountAmount = BigDecimal("10.00"),
                raffleTickets = 5
            )

            // Then
            assertEquals(testCampaign, coupon.campaign)
            assertEquals(qrCode, coupon.qrCode)
            assertEquals(qrSignature, coupon.qrSignature)
            assertEquals(couponCode, coupon.couponCode)
            assertEquals(CouponStatus.ACTIVE, coupon.status)
            assertEquals(BigDecimal("10.00"), coupon.discountAmount)
            assertEquals(5, coupon.raffleTickets)
            assertEquals(0, coupon.currentUses)
        }

        @Test
        @DisplayName("Should create coupon with percentage discount")
        fun shouldCreateCouponWithPercentageDiscount() {
            // When
            val coupon = Coupon(
                campaign = testCampaign,
                qrCode = "QR987654321",
                qrSignature = "signature456",
                couponCode = "COUP002",
                validFrom = LocalDateTime.now().minusDays(1),
                validUntil = LocalDateTime.now().plusDays(30),
                discountPercentage = BigDecimal("15.00"),
                raffleTickets = 3
            )

            // Then
            assertNull(coupon.discountAmount)
            assertEquals(BigDecimal("15.00"), coupon.discountPercentage)
            assertEquals(DiscountType.PERCENTAGE, coupon.getDiscountType())
        }
    }

    @Nested
    @DisplayName("Coupon Validation Tests")
    inner class CouponValidationTests {

        private val validCoupon = Coupon(
            id = 1L,
            campaign = testCampaign,
            qrCode = "QR123456789",
            qrSignature = "signature123",
            couponCode = "COUP001",
            status = CouponStatus.ACTIVE,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30),
            maxUses = 5,
            currentUses = 2
        )

        @Test
        @DisplayName("Should validate active coupon within date range")
        fun shouldValidateActiveCouponWithinDateRange() {
            // When & Then
            assertTrue(validCoupon.isValid())
            assertTrue(validCoupon.canBeUsed())
            assertFalse(validCoupon.isExpired())
            assertFalse(validCoupon.isNotYetValid())
            assertFalse(validCoupon.isMaxUsesReached())
        }

        @Test
        @DisplayName("Should detect expired coupon")
        fun shouldDetectExpiredCoupon() {
            // Given
            val expiredCoupon = validCoupon.copy(
                validUntil = LocalDateTime.now().minusDays(1)
            )

            // When & Then
            assertFalse(expiredCoupon.isValid())
            assertFalse(expiredCoupon.canBeUsed())
            assertTrue(expiredCoupon.isExpired())
        }

        @Test
        @DisplayName("Should detect coupon not yet valid")
        fun shouldDetectCouponNotYetValid() {
            // Given
            val futureCoupon = validCoupon.copy(
                validFrom = LocalDateTime.now().plusDays(1)
            )

            // When & Then
            assertFalse(futureCoupon.isValid())
            assertFalse(futureCoupon.canBeUsed())
            assertTrue(futureCoupon.isNotYetValid())
        }

        @Test
        @DisplayName("Should detect max uses reached")
        fun shouldDetectMaxUsesReached() {
            // Given
            val maxUsedCoupon = validCoupon.copy(
                maxUses = 3,
                currentUses = 3
            )

            // When & Then
            assertFalse(maxUsedCoupon.isValid())
            assertFalse(maxUsedCoupon.canBeUsed())
            assertTrue(maxUsedCoupon.isMaxUsesReached())
        }

        @Test
        @DisplayName("Should detect inactive coupon")
        fun shouldDetectInactiveCoupon() {
            // Given
            val inactiveCoupon = validCoupon.copy(status = CouponStatus.INACTIVE)

            // When & Then
            assertFalse(inactiveCoupon.isValid())
            assertFalse(inactiveCoupon.canBeUsed())
        }
    }

    @Nested
    @DisplayName("Coupon Business Logic Tests")
    inner class CouponBusinessLogicTests {

        private val testCoupon = Coupon(
            id = 1L,
            campaign = testCampaign,
            qrCode = "QR123456789",
            qrSignature = "signature123",
            couponCode = "COUP001",
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30),
            discountAmount = BigDecimal("15.00"),
            minimumPurchaseAmount = BigDecimal("50.00"),
            applicableFuelTypes = "Regular, Premium",
            applicableStations = "1, 2, 3",
            maxUses = 10,
            currentUses = 3
        )

        @Test
        @DisplayName("Should calculate remaining uses correctly")
        fun shouldCalculateRemainingUsesCorrectly() {
            // When
            val remainingUses = testCoupon.getRemainingUses()

            // Then
            assertEquals(7, remainingUses)
        }

        @Test
        @DisplayName("Should return null for unlimited uses")
        fun shouldReturnNullForUnlimitedUses() {
            // Given
            val unlimitedCoupon = testCoupon.copy(maxUses = null)

            // When
            val remainingUses = unlimitedCoupon.getRemainingUses()

            // Then
            assertNull(remainingUses)
        }

        @Test
        @DisplayName("Should check fuel type applicability")
        fun shouldCheckFuelTypeApplicability() {
            // When & Then
            assertTrue(testCoupon.appliesTo("Regular"))
            assertTrue(testCoupon.appliesTo("Premium"))
            assertFalse(testCoupon.appliesTo("Diesel"))
        }

        @Test
        @DisplayName("Should check station applicability")
        fun shouldCheckStationApplicability() {
            // When & Then
            assertTrue(testCoupon.appliesTo(1L))
            assertTrue(testCoupon.appliesTo(2L))
            assertTrue(testCoupon.appliesTo(3L))
            assertFalse(testCoupon.appliesTo(4L))
        }

        @Test
        @DisplayName("Should check minimum purchase requirement")
        fun shouldCheckMinimumPurchaseRequirement() {
            // When & Then
            assertTrue(testCoupon.meetsMinimumPurchase(BigDecimal("50.00")))
            assertTrue(testCoupon.meetsMinimumPurchase(BigDecimal("75.00")))
            assertFalse(testCoupon.meetsMinimumPurchase(BigDecimal("25.00")))
        }

        @Test
        @DisplayName("Should calculate fixed amount discount")
        fun shouldCalculateFixedAmountDiscount() {
            // When
            val discount = testCoupon.calculateDiscount(BigDecimal("100.00"))

            // Then
            assertEquals(BigDecimal("15.00"), discount)
            assertEquals(DiscountType.FIXED_AMOUNT, testCoupon.getDiscountType())
        }

        @Test
        @DisplayName("Should calculate percentage discount")
        fun shouldCalculatePercentageDiscount() {
            // Given
            val percentageCoupon = testCoupon.copy(
                discountAmount = null,
                discountPercentage = BigDecimal("20.00")
            )

            // When
            val discount = percentageCoupon.calculateDiscount(BigDecimal("100.00"))

            // Then
            assertEquals(BigDecimal("20.00"), discount)
            assertEquals(DiscountType.PERCENTAGE, percentageCoupon.getDiscountType())
        }

        @Test
        @DisplayName("Should return zero discount for no discount coupon")
        fun shouldReturnZeroDiscountForNoDiscountCoupon() {
            // Given
            val noDiscountCoupon = testCoupon.copy(
                discountAmount = null,
                discountPercentage = null
            )

            // When
            val discount = noDiscountCoupon.calculateDiscount(BigDecimal("100.00"))

            // Then
            assertEquals(BigDecimal.ZERO, discount)
            assertEquals(DiscountType.NONE, noDiscountCoupon.getDiscountType())
        }

        @Test
        @DisplayName("Should parse applicable fuel types list")
        fun shouldParseApplicableFuelTypesList() {
            // When
            val fuelTypes = testCoupon.getApplicableFuelTypesList()

            // Then
            assertEquals(2, fuelTypes.size)
            assertTrue(fuelTypes.contains("Regular"))
            assertTrue(fuelTypes.contains("Premium"))
        }

        @Test
        @DisplayName("Should parse applicable stations list")
        fun shouldParseApplicableStationsList() {
            // When
            val stations = testCoupon.getApplicableStationsList()

            // Then
            assertEquals(3, stations.size)
            assertTrue(stations.contains(1L))
            assertTrue(stations.contains(2L))
            assertTrue(stations.contains(3L))
        }
    }

    @Nested
    @DisplayName("Coupon State Management Tests")
    inner class CouponStateManagementTests {

        private val testCoupon = Coupon(
            id = 1L,
            campaign = testCampaign,
            qrCode = "QR123456789",
            qrSignature = "signature123",
            couponCode = "COUP001",
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30),
            status = CouponStatus.ACTIVE,
            currentUses = 2
        )

        @Test
        @DisplayName("Should use coupon and increment usage")
        fun shouldUseCouponAndIncrementUsage() {
            // When
            val usedCoupon = testCoupon.use()

            // Then
            assertEquals(3, usedCoupon.currentUses)
        }

        @Test
        @DisplayName("Should activate coupon")
        fun shouldActivateCoupon() {
            // Given
            val inactiveCoupon = testCoupon.copy(status = CouponStatus.INACTIVE)

            // When
            val activatedCoupon = inactiveCoupon.activate()

            // Then
            assertEquals(CouponStatus.ACTIVE, activatedCoupon.status)
        }

        @Test
        @DisplayName("Should deactivate coupon")
        fun shouldDeactivateCoupon() {
            // When
            val deactivatedCoupon = testCoupon.deactivate()

            // Then
            assertEquals(CouponStatus.INACTIVE, deactivatedCoupon.status)
        }

        @Test
        @DisplayName("Should expire coupon")
        fun shouldExpireCoupon() {
            // When
            val expiredCoupon = testCoupon.expire()

            // Then
            assertEquals(CouponStatus.EXPIRED, expiredCoupon.status)
        }
    }

    @Nested
    @DisplayName("Coupon Status Tests")
    inner class CouponStatusTests {

        @Test
        @DisplayName("Should validate status usage permissions")
        fun shouldValidateStatusUsagePermissions() {
            // When & Then
            assertTrue(CouponStatus.ACTIVE.allowsUsage())
            assertFalse(CouponStatus.INACTIVE.allowsUsage())
            assertFalse(CouponStatus.EXPIRED.allowsUsage())
            assertFalse(CouponStatus.USED_UP.allowsUsage())
            assertFalse(CouponStatus.CANCELLED.allowsUsage())
        }

        @Test
        @DisplayName("Should identify final states")
        fun shouldIdentifyFinalStates() {
            // When & Then
            assertFalse(CouponStatus.ACTIVE.isFinalState())
            assertFalse(CouponStatus.INACTIVE.isFinalState())
            assertTrue(CouponStatus.EXPIRED.isFinalState())
            assertTrue(CouponStatus.USED_UP.isFinalState())
            assertTrue(CouponStatus.CANCELLED.isFinalState())
        }

        @Test
        @DisplayName("Should have correct display names")
        fun shouldHaveCorrectDisplayNames() {
            assertEquals("Active", CouponStatus.ACTIVE.displayName)
            assertEquals("Inactive", CouponStatus.INACTIVE.displayName)
            assertEquals("Expired", CouponStatus.EXPIRED.displayName)
            assertEquals("Used Up", CouponStatus.USED_UP.displayName)
            assertEquals("Cancelled", CouponStatus.CANCELLED.displayName)
        }
    }

    @Nested
    @DisplayName("Discount Type Tests")
    inner class DiscountTypeTests {

        @Test
        @DisplayName("Should identify discount providing types")
        fun shouldIdentifyDiscountProvidingTypes() {
            // When & Then
            assertTrue(DiscountType.FIXED_AMOUNT.providesDiscount())
            assertTrue(DiscountType.PERCENTAGE.providesDiscount())
            assertFalse(DiscountType.NONE.providesDiscount())
        }

        @Test
        @DisplayName("Should have correct display names")
        fun shouldHaveCorrectDisplayNames() {
            assertEquals("Fixed Amount", DiscountType.FIXED_AMOUNT.displayName)
            assertEquals("Percentage", DiscountType.PERCENTAGE.displayName)
            assertEquals("No Discount", DiscountType.NONE.displayName)
        }
    }

    @Nested
    @DisplayName("Coupon Validation Tests")
    inner class CouponValidationTests {

        @Test
        @DisplayName("Should validate toString method")
        fun shouldValidateToStringMethod() {
            // Given
            val coupon = Coupon(
                id = 1L,
                campaign = testCampaign,
                qrCode = "QR123456789",
                qrSignature = "signature123",
                couponCode = "COUP001",
                validFrom = LocalDateTime.now().minusDays(1),
                validUntil = LocalDateTime.now().plusDays(30),
                status = CouponStatus.ACTIVE
            )

            // When
            val stringRepresentation = coupon.toString()

            // Then
            assertTrue(stringRepresentation.contains("id=1"))
            assertTrue(stringRepresentation.contains("couponCode='COUP001'"))
            assertTrue(stringRepresentation.contains("status=ACTIVE"))
            assertTrue(stringRepresentation.contains("campaign=Test Campaign"))
        }
    }
}