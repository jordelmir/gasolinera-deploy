package com.gasolinerajsm.coupon.domain

import com.gasolinerajsm.testing.shared.TestDataFactory
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Unit Tests for Coupon Domain Entity
 * Tests business logic, validation, and domain rules
 */
@DisplayName("Coupon Domain Entity Tests")
class CouponTest {

    @Nested
    @DisplayName("Creation Tests")
    inner class CreationTests {

        @Test
        @DisplayName("Should create valid Coupon with all required fields")
        fun shouldCreateValidCoupon() {
            // Given
            val id = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val stationId = UUID.randomUUID()
            val amount = BigDecimal("500.00")
            val fuelType = FuelType.REGULAR
            val expiresAt = LocalDateTime.now().plusDays(30)

            // When
            val coupon = Coupon.create(
                id = id,
                userId = userId,
                stationId = stationId,
                amount = amount,
                fuelType = fuelType,
                expiresAt = expiresAt
            )

            // Then
            assertThat(coupon.id).isEqualTo(id)
            assertThat(coupon.userId).isEqualTo(userId)
            assertThat(coupon.stationId).isEqualTo(stationId)
            assertThat(coupon.amount).isEqualTo(amount)
            assertThat(coupon.fuelType).isEqualTo(fuelType)
            assertThat(coupon.expiresAt).isEqualTo(expiresAt)
            assertThat(coupon.status).isEqualTo(CouponStatus.ACTIVE)
            assertThat(coupon.qrCode).isNotNull()
            assertThat(coupon.qrCode).isNotEmpty()
            assertThat(coupon.createdAt).isBeforeOrEqualTo(LocalDateTime.now())
            assertThat(coupon.updatedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @ParameterizedTest
        @ValueSource(strings = ["0", "-1", "-100"])
        @DisplayName("Should throw exception when creating Coupon with invalid amount")
        fun shouldThrowExceptionWithInvalidAmount(invalidAmount: String) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Coupon.create(
                    id = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal(invalidAmount),
                    fuelType = FuelType.REGULAR,
                    expiresAt = LocalDateTime.now().plusDays(30)
                )
            }
        }

        @Test
        @DisplayName("Should throw exception when creating Coupon with past expiration date")
        fun shouldThrowExceptionWithPastExpirationDate() {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Coupon.create(
                    id = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("500.00"),
                    fuelType = FuelType.REGULAR,
                    expiresAt = LocalDateTime.now().minusDays(1)
                )
            }
        }

        @Test
        @DisplayName("Should generate unique QR codes for different coupons")
        fun shouldGenerateUniqueQRCodes() {
            // Given
            val coupon1 = createValidCoupon()
            val coupon2 = createValidCoupon()

            // When & Then
            assertThat(coupon1.qrCode).isNotEqualTo(coupon2.qrCode)
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    inner class BusinessLogicTests {

        @Test
        @DisplayName("Should redeem coupon successfully")
        fun shouldRedeemCouponSuccessfully() {
            // Given
            val coupon = createValidCoupon()
            assertThat(coupon.status).isEqualTo(CouponStatus.ACTIVE)

            // When
            coupon.redeem()

            // Then
            assertThat(coupon.status).isEqualTo(CouponStatus.REDEEMED)
            assertThat(coupon.redeemedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should throw exception when redeeming already redeemed coupon")
        fun shouldThrowExceptionWhenRedeemingAlreadyRedeemedCoupon() {
            // Given
            val coupon = createValidCoupon()
            coupon.redeem()
            assertThat(coupon.status).isEqualTo(CouponStatus.REDEEMED)

            // When & Then
            assertThrows<IllegalStateException> {
                coupon.redeem()
            }
        }

        @Test
        @DisplayName("Should throw exception when redeeming expired coupon")
        fun shouldThrowExceptionWhenRedeemingExpiredCoupon() {
            // Given
            val coupon = createExpiredCoupon()
            coupon.markAsExpired()
            assertThat(coupon.status).isEqualTo(CouponStatus.EXPIRED)

            // When & Then
            assertThrows<IllegalStateException> {
                coupon.redeem()
            }
        }

        @Test
        @DisplayName("Should cancel coupon successfully")
        fun shouldCancelCouponSuccessfully() {
            // Given
            val coupon = createValidCoupon()
            assertThat(coupon.status).isEqualTo(CouponStatus.ACTIVE)

            // When
            coupon.cancel()

            // Then
            assertThat(coupon.status).isEqualTo(CouponStatus.CANCELLED)
            assertThat(coupon.cancelledAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should throw exception when cancelling already redeemed coupon")
        fun shouldThrowExceptionWhenCancellingAlreadyRedeemedCoupon() {
            // Given
            val coupon = createValidCoupon()
            coupon.redeem()
            assertThat(coupon.status).isEqualTo(CouponStatus.REDEEMED)

            // When & Then
            assertThrows<IllegalStateException> {
                coupon.cancel()
            }
        }

        @Test
        @DisplayName("Should mark coupon as expired successfully")
        fun shouldMarkCouponAsExpiredSuccessfully() {
            // Given
            val coupon = createExpiredCoupon()
            assertThat(coupon.status).isEqualTo(CouponStatus.ACTIVE)

            // When
            coupon.markAsExpired()

            // Then
            assertThat(coupon.status).isEqualTo(CouponStatus.EXPIRED)
        }

        @Test
        @DisplayName("Should check if coupon is redeemable when active and not expired")
        fun shouldCheckIfCouponIsRedeemableWhenActiveAndNotExpired() {
            // Given
            val coupon = createValidCoupon()

            // When & Then
            assertThat(coupon.isRedeemable()).isTrue()
        }

        @Test
        @DisplayName("Should check if coupon is not redeemable when expired")
        fun shouldCheckIfCouponIsNotRedeemableWhenExpired() {
            // Given
            val coupon = createExpiredCoupon()

            // When & Then
            assertThat(coupon.isRedeemable()).isFalse()
        }

        @Test
        @DisplayName("Should check if coupon is not redeemable when already redeemed")
        fun shouldCheckIfCouponIsNotRedeemableWhenAlreadyRedeemed() {
            // Given
            val coupon = createValidCoupon()
            coupon.redeem()

            // When & Then
            assertThat(coupon.isRedeemable()).isFalse()
        }

        @Test
        @DisplayName("Should check if coupon is not redeemable when cancelled")
        fun shouldCheckIfCouponIsNotRedeemableWhenCancelled() {
            // Given
            val coupon = createValidCoupon()
            coupon.cancel()

            // When & Then
            assertThat(coupon.isRedeemable()).isFalse()
        }

        @Test
        @DisplayName("Should check if coupon is expired based on expiration date")
        fun shouldCheckIfCouponIsExpiredBasedOnExpirationDate() {
            // Given
            val expiredCoupon = createExpiredCoupon()
            val validCoupon = createValidCoupon()

            // When & Then
            assertThat(expiredCoupon.isExpired()).isTrue()
            assertThat(validCoupon.isExpired()).isFalse()
        }

        @Test
        @DisplayName("Should calculate days until expiration")
        fun shouldCalculateDaysUntilExpiration() {
            // Given
            val daysUntilExpiration = 15L
            val coupon = Coupon.create(
                id = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("500.00"),
                fuelType = FuelType.REGULAR,
                expiresAt = LocalDateTime.now().plusDays(daysUntilExpiration)
            )

            // When
            val calculatedDays = coupon.daysUntilExpiration()

            // Then
            assertThat(calculatedDays).isEqualTo(daysUntilExpiration)
        }

        @Test
        @DisplayName("Should return negative days for expired coupon")
        fun shouldReturnNegativeDaysForExpiredCoupon() {
            // Given
            val expiredCoupon = createExpiredCoupon()

            // When
            val daysUntilExpiration = expiredCoupon.daysUntilExpiration()

            // Then
            assertThat(daysUntilExpiration).isNegative()
        }
    }

    @Nested
    @DisplayName("QR Code Tests")
    inner class QRCodeTests {

        @Test
        @DisplayName("Should generate QR code with correct format")
        fun shouldGenerateQRCodeWithCorrectFormat() {
            // Given
            val coupon = createValidCoupon()

            // When
            val qrCode = coupon.qrCode

            // Then
            assertThat(qrCode).startsWith("QR_")
            assertThat(qrCode).hasSize(35) // QR_ + 32 character UUID without dashes
            assertThat(qrCode).matches(Regex("^QR_[A-Z0-9]{32}$"))
        }

        @Test
        @DisplayName("Should validate QR code format")
        fun shouldValidateQRCodeFormat() {
            // Given
            val validQRCode = "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6"
            val invalidQRCode1 = "INVALID_QR_CODE"
            val invalidQRCode2 = "QR_SHORT"
            val invalidQRCode3 = "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7" // Too long

            // When & Then
            assertThat(QRCode.isValidFormat(validQRCode)).isTrue()
            assertThat(QRCode.isValidFormat(invalidQRCode1)).isFalse()
            assertThat(QRCode.isValidFormat(invalidQRCode2)).isFalse()
            assertThat(QRCode.isValidFormat(invalidQRCode3)).isFalse()
        }

        @Test
        @DisplayName("Should regenerate QR code")
        fun shouldRegenerateQRCode() {
            // Given
            val coupon = createValidCoupon()
            val originalQRCode = coupon.qrCode

            // When
            coupon.regenerateQRCode()

            // Then
            assertThat(coupon.qrCode).isNotEqualTo(originalQRCode)
            assertThat(coupon.qrCode).startsWith("QR_")
            assertThat(coupon.qrCode).hasSize(35)
        }

        @Test
        @DisplayName("Should throw exception when regenerating QR code for redeemed coupon")
        fun shouldThrowExceptionWhenRegeneratingQRCodeForRedeemedCoupon() {
            // Given
            val coupon = createValidCoupon()
            coupon.redeem()

            // When & Then
            assertThrows<IllegalStateException> {
                coupon.regenerateQRCode()
            }
        }
    }

    @Nested
    @DisplayName("Coupon Status Tests")
    inner class CouponStatusTests {

        @ParameterizedTest
        @EnumSource(CouponStatus::class)
        @DisplayName("Should have all expected coupon statuses")
        fun shouldHaveAllExpectedCouponStatuses(status: CouponStatus) {
            // When & Then
            assertThat(CouponStatus.values()).contains(status)
        }

        @Test
        @DisplayName("Should get status display name")
        fun shouldGetStatusDisplayName() {
            // When & Then
            assertThat(CouponStatus.ACTIVE.displayName).isEqualTo("Activo")
            assertThat(CouponStatus.REDEEMED.displayName).isEqualTo("Canjeado")
            assertThat(CouponStatus.EXPIRED.displayName).isEqualTo("Expirado")
            assertThat(CouponStatus.CANCELLED.displayName).isEqualTo("Cancelado")
        }

        @Test
        @DisplayName("Should check if status is final")
        fun shouldCheckIfStatusIsFinal() {
            // When & Then
            assertThat(CouponStatus.ACTIVE.isFinal()).isFalse()
            assertThat(CouponStatus.REDEEMED.isFinal()).isTrue()
            assertThat(CouponStatus.EXPIRED.isFinal()).isTrue()
            assertThat(CouponStatus.CANCELLED.isFinal()).isTrue()
        }
    }

    @Nested
    @DisplayName("Domain Events Tests")
    inner class DomainEventsTests {

        @Test
        @DisplayName("Should publish CouponCreated event when coupon is created")
        fun shouldPublishCouponCreatedEvent() {
            // Given
            val id = UUID.randomUUID()
            val userId = UUID.randomUUID()

            // When
            val coupon = Coupon.create(
                id = id,
                userId = userId,
                stationId = UUID.randomUUID(),
                amount = BigDecimal("500.00"),
                fuelType = FuelType.REGULAR,
                expiresAt = LocalDateTime.now().plusDays(30)
            )

            // Then
            val events = coupon.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(CouponCreatedEvent::class.java)

            val event = events.first() as CouponCreatedEvent
            assertThat(event.couponId).isEqualTo(id)
            assertThat(event.userId).isEqualTo(userId)
            assertThat(event.amount).isEqualTo(BigDecimal("500.00"))
        }

        @Test
        @DisplayName("Should publish CouponRedeemed event when coupon is redeemed")
        fun shouldPublishCouponRedeemedEvent() {
            // Given
            val coupon = createValidCoupon()
            coupon.clearDomainEvents() // Clear creation event

            // When
            coupon.redeem()

            // Then
            val events = coupon.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(CouponRedeemedEvent::class.java)

            val event = events.first() as CouponRedeemedEvent
            assertThat(event.couponId).isEqualTo(coupon.id)
            assertThat(event.userId).isEqualTo(coupon.userId)
        }

        @Test
        @DisplayName("Should publish CouponExpired event when coupon is marked as expired")
        fun shouldPublishCouponExpiredEvent() {
            // Given
            val coupon = createExpiredCoupon()
            coupon.clearDomainEvents() // Clear creation event

            // When
            coupon.markAsExpired()

            // Then
            val events = coupon.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(CouponExpiredEvent::class.java)
        }

        @Test
        @DisplayName("Should publish CouponCancelled event when coupon is cancelled")
        fun shouldPublishCouponCancelledEvent() {
            // Given
            val coupon = createValidCoupon()
            coupon.clearDomainEvents() // Clear creation event

            // When
            coupon.cancel()

            // Then
            val events = coupon.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(CouponCancelledEvent::class.java)
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    inner class EqualityTests {

        @Test
        @DisplayName("Should be equal when same ID")
        fun shouldBeEqualWhenSameId() {
            // Given
            val id = UUID.randomUUID()
            val coupon1 = Coupon.create(
                id = id,
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("500.00"),
                fuelType = FuelType.REGULAR,
                expiresAt = LocalDateTime.now().plusDays(30)
            )
            val coupon2 = Coupon.create(
                id = id,
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("1000.00"),
                fuelType = FuelType.PREMIUM,
                expiresAt = LocalDateTime.now().plusDays(15)
            )

            // When & Then
            assertThat(coupon1).isEqualTo(coupon2)
            assertThat(coupon1.hashCode()).isEqualTo(coupon2.hashCode())
        }

        @Test
        @DisplayName("Should not be equal when different ID")
        fun shouldNotBeEqualWhenDifferentId() {
            // Given
            val coupon1 = createValidCoupon()
            val coupon2 = createValidCoupon()

            // When & Then
            assertThat(coupon1).isNotEqualTo(coupon2)
            assertThat(coupon1.hashCode()).isNotEqualTo(coupon2.hashCode())
        }
    }

    private fun createValidCoupon(): Coupon {
        return Coupon.create(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            stationId = UUID.randomUUID(),
            amount = BigDecimal("500.00"),
            fuelType = FuelType.REGULAR,
            expiresAt = LocalDateTime.now().plusDays(30)
        )
    }

    private fun createExpiredCoupon(): Coupon {
        return Coupon.create(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            stationId = UUID.randomUUID(),
            amount = BigDecimal("500.00"),
            fuelType = FuelType.REGULAR,
            expiresAt = LocalDateTime.now().minusDays(1)
        )
    }
}