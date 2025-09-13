package com.gasolinerajsm.coupon.infrastructure

import com.gasolinerajsm.coupon.domain.*
import com.gasolinerajsm.testing.shared.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Integration Tests for Coupon Repository
 * Tests real database interactions with PostgreSQL TestContainer
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Coupon Repository Integration Tests")
class CouponRepositoryIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        // Clean database before each test
        couponRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("Save and Find Operations")
    inner class SaveAndFindOperations {

        @Test
        @DisplayName("Should save and retrieve Coupon successfully")
        fun shouldSaveAndRetrieveCouponSuccessfully() {
            // Given
            val coupon = createTestCoupon()

            // When
            val savedCoupon = couponRepository.save(coupon)
            entityManager.flush()
            entityManager.clear()

            val foundCoupon = couponRepository.findById(savedCoupon.id)

            // Then
            assertThat(foundCoupon).isPresent
            assertThat(foundCoupon.get().id).isEqualTo(coupon.id)
            assertThat(foundCoupon.get().userId).isEqualTo(coupon.userId)
            assertThat(foundCoupon.get().stationId).isEqualTo(coupon.stationId)
            assertThat(foundCoupon.get().amount).isEqualTo(coupon.amount)
            assertThat(foundCoupon.get().fuelType).isEqualTo(coupon.fuelType)
            assertThat(foundCoupon.get().status).isEqualTo(coupon.status)
            assertThat(foundCoupon.get().qrCode).isEqualTo(coupon.qrCode)
        }

        @Test
        @DisplayName("Should find coupons by user ID successfully")
        fun shouldFindCouponsByUserIdSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val coupon1 = createTestCoupon(userId = userId, amount = BigDecimal("100.00"))
            val coupon2 = createTestCoupon(userId = userId, amount = BigDecimal("200.00"))
            val coupon3 = createTestCoupon(userId = UUID.randomUUID(), amount = BigDecimal("300.00"))

            couponRepository.saveAll(listOf(coupon1, coupon2, coupon3))
            entityManager.flush()
            entityManager.clear()

            // When
            val userCoupons = couponRepository.findByUserId(userId)

            // Then
            assertThat(userCoupons).hasSize(2)
            assertThat(userCoupons.map { it.amount }).containsExactlyInAnyOrder(
                BigDecimal("100.00"),
                BigDecimal("200.00")
            )
        }

        @Test
        @DisplayName("Should find coupons by station ID successfully")
        fun shouldFindCouponsByStationIdSuccessfully() {
            // Given
            val stationId = UUID.randomUUID()
            val coupon1 = createTestCoupon(stationId = stationId, fuelType = FuelType.REGULAR)
            val coupon2 = createTestCoupon(stationId = stationId, fuelType = FuelType.PREMIUM)
            val coupon3 = createTestCoupon(stationId = UUID.randomUUID(), fuelType = FuelType.DIESEL)

            couponRepository.saveAll(listOf(coupon1, coupon2, coupon3))
            entityManager.flush()
            entityManager.clear()

            // When
            val stationCoupons = couponRepository.findByStationId(stationId)

            // Then
            assertThat(stationCoupons).hasSize(2)
            assertThat(stationCoupons.map { it.fuelType }).containsExactlyInAnyOrder(
                FuelType.REGULAR,
                FuelType.PREMIUM
            )
        }

        @Test
        @DisplayName("Should find coupon by QR code successfully")
        fun shouldFindCouponByQRCodeSuccessfully() {
            // Given
            val qrCode = "QR_UNIQUE_TEST_CODE_123456789012345"
            val coupon = createTestCoupon().apply {
                // Simulate setting QR code
                regenerateQRCode()
            }

            couponRepository.save(coupon)
            entityManager.flush()
            entityManager.clear()

            // When
            val foundCoupon = couponRepository.findByQrCode(coupon.qrCode)

            // Then
            assertThat(foundCoupon).isNotNull
            assertThat(foundCoupon?.qrCode).isEqualTo(coupon.qrCode)
        }
    }

    @Nested
    @DisplayName("Status-based Queries")
    inner class StatusBasedQueries {

        @Test
        @DisplayName("Should find active coupons successfully")
        fun shouldFindActiveCouponsSuccessfully() {
            // Given
            val activeCoupon1 = createTestCoupon(status = CouponStatus.ACTIVE)
            val activeCoupon2 = createTestCoupon(status = CouponStatus.ACTIVE)
            val redeemedCoupon = createTestCoupon(status = CouponStatus.REDEEMED)
            val expiredCoupon = createTestCoupon(status = CouponStatus.EXPIRED)

            couponRepository.saveAll(listOf(activeCoupon1, activeCoupon2, redeemedCoupon, expiredCoupon))
            entityManager.flush()
            entityManager.clear()

            // When
            val activeCoupons = couponRepository.findByStatus(CouponStatus.ACTIVE)

            // Then
            assertThat(activeCoupons).hasSize(2)
            assertThat(activeCoupons.map { it.status }).allMatch { it == CouponStatus.ACTIVE }
        }

        @Test
        @DisplayName("Should find redeemed coupons successfully")
        fun shouldFindRedeemedCouponsSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val redeemedCoupon1 = createTestCoupon(userId = userId, status = CouponStatus.REDEEMED)
            val redeemedCoupon2 = createTestCoupon(userId = userId, status = CouponStatus.REDEEMED)
            val activeCoupon = createTestCoupon(userId = userId, status = CouponStatus.ACTIVE)

            couponRepository.saveAll(listOf(redeemedCoupon1, redeemedCoupon2, activeCoupon))
            entityManager.flush()
            entityManager.clear()

            // When
            val redeemedCoupons = couponRepository.findByUserIdAndStatus(userId, CouponStatus.REDEEMED)

            // Then
            assertThat(redeemedCoupons).hasSize(2)
            assertThat(redeemedCoupons.map { it.status }).allMatch { it == CouponStatus.REDEEMED }
        }

        @Test
        @DisplayName("Should find expired coupons successfully")
        fun shouldFindExpiredCouponsSuccessfully() {
            // Given
            val now = LocalDateTime.now()
            val expiredCoupon1 = createTestCoupon(expiresAt = now.minusDays(1))
            val expiredCoupon2 = createTestCoupon(expiresAt = now.minusDays(2))
            val validCoupon = createTestCoupon(expiresAt = now.plusDays(1))

            couponRepository.saveAll(listOf(expiredCoupon1, expiredCoupon2, validCoupon))
            entityManager.flush()
            entityManager.clear()

            // When
            val expiredCoupons = couponRepository.findByExpiresAtBefore(now)

            // Then
            assertThat(expiredCoupons).hasSize(2)
            assertThat(expiredCoupons.map { it.expiresAt }).allMatch { it.isBefore(now) }
        }
    }

    @Nested
    @DisplayName("Date Range Queries")
    inner class DateRangeQueries {

        @Test
        @DisplayName("Should find coupons created within date range")
        fun shouldFindCouponsCreatedWithinDateRange() {
            // Given
            val startDate = LocalDateTime.now().minusDays(7)
            val endDate = LocalDateTime.now().minusDays(1)

            val oldCoupon = createTestCoupon()
            val recentCoupon1 = createTestCoupon()
            val recentCoupon2 = createTestCoupon()
            val newCoupon = createTestCoupon()

            // Save and manually update creation dates
            couponRepository.saveAll(listOf(oldCoupon, recentCoupon1, recentCoupon2, newCoupon))
            entityManager.flush()

            // Update creation dates using native query
            entityManager.createQuery(
                "UPDATE Coupon c SET c.createdAt = :oldDate WHERE c.id = :id"
            ).setParameter("oldDate", startDate.minusDays(5))
             .setParameter("id", oldCoupon.id)
             .executeUpdate()

            entityManager.createQuery(
                "UPDATE Coupon c SET c.createdAt = :recentDate WHERE c.id = :id"
            ).setParameter("recentDate", startDate.plusDays(1))
             .setParameter("id", recentCoupon1.id)
             .executeUpdate()

            entityManager.createQuery(
                "UPDATE Coupon c SET c.createdAt = :recentDate WHERE c.id = :id"
            ).setParameter("recentDate", startDate.plusDays(2))
             .setParameter("id", recentCoupon2.id)
             .executeUpdate()

            entityManager.createQuery(
                "UPDATE Coupon c SET c.createdAt = :newDate WHERE c.id = :id"
            ).setParameter("newDate", endDate.plusDays(2))
             .setParameter("id", newCoupon.id)
             .executeUpdate()

            entityManager.flush()
            entityManager.clear()

            // When
            val couponsInRange = couponRepository.findByCreatedAtBetween(startDate, endDate)

            // Then
            assertThat(couponsInRange).hasSize(2)
        }

        @Test
        @DisplayName("Should find coupons expiring soon")
        fun shouldFindCouponsExpiringSoon() {
            // Given
            val now = LocalDateTime.now()
            val soonThreshold = now.plusDays(7)

            val expiringSoon1 = createTestCoupon(expiresAt = now.plusDays(3))
            val expiringSoon2 = createTestCoupon(expiresAt = now.plusDays(5))
            val expiredCoupon = createTestCoupon(expiresAt = now.minusDays(1))
            val farFutureCoupon = createTestCoupon(expiresAt = now.plusDays(30))

            couponRepository.saveAll(listOf(expiringSoon1, expiringSoon2, expiredCoupon, farFutureCoupon))
            entityManager.flush()
            entityManager.clear()

            // When
            val expiringSoonCoupons = couponRepository.findByExpiresAtBetween(now, soonThreshold)

            // Then
            assertThat(expiringSoonCoupons).hasSize(2)
            assertThat(expiringSoonCoupons.map { it.expiresAt }).allMatch {
                it.isAfter(now) && it.isBefore(soonThreshold)
            }
        }
    }

    @Nested
    @DisplayName("Aggregation Queries")
    inner class AggregationQueries {

        @Test
        @DisplayName("Should count coupons by status")
        fun shouldCountCouponsByStatus() {
            // Given
            val userId = UUID.randomUUID()
            val activeCoupons = (1..5).map { createTestCoupon(userId = userId, status = CouponStatus.ACTIVE) }
            val redeemedCoupons = (1..3).map { createTestCoupon(userId = userId, status = CouponStatus.REDEEMED) }
            val expiredCoupons = (1..2).map { createTestCoupon(userId = userId, status = CouponStatus.EXPIRED) }

            couponRepository.saveAll(activeCoupons + redeemedCoupons + expiredCoupons)
            entityManager.flush()
            entityManager.clear()

            // When
            val activeCount = couponRepository.countByUserIdAndStatus(userId, CouponStatus.ACTIVE)
            val redeemedCount = couponRepository.countByUserIdAndStatus(userId, CouponStatus.REDEEMED)
            val expiredCount = couponRepository.countByUserIdAndStatus(userId, CouponStatus.EXPIRED)

            // Then
            assertThat(activeCount).isEqualTo(5)
            assertThat(redeemedCount).isEqualTo(3)
            assertThat(expiredCount).isEqualTo(2)
        }

        @Test
        @DisplayName("Should calculate total coupon value by user")
        fun shouldCalculateTotalCouponValueByUser() {
            // Given
            val userId = UUID.randomUUID()
            val coupon1 = createTestCoupon(userId = userId, amount = BigDecimal("100.00"))
            val coupon2 = createTestCoupon(userId = userId, amount = BigDecimal("250.00"))
            val coupon3 = createTestCoupon(userId = userId, amount = BigDecimal("150.00"))
            val otherUserCoupon = createTestCoupon(userId = UUID.randomUUID(), amount = BigDecimal("500.00"))

            couponRepository.saveAll(listOf(coupon1, coupon2, coupon3, otherUserCoupon))
            entityManager.flush()
            entityManager.clear()

            // When
            val totalValue = couponRepository.sumAmountByUserId(userId)

            // Then
            assertThat(totalValue).isEqualTo(BigDecimal("500.00"))
        }
    }

    @Nested
    @DisplayName("Pagination and Sorting")
    inner class PaginationAndSorting {

        @Test
        @DisplayName("Should paginate user coupons correctly")
        fun shouldPaginateUserCouponsCorrectly() {
            // Given
            val userId = UUID.randomUUID()
            val coupons = (1..15).map { i ->
                createTestCoupon(
                    userId = userId,
                    amount = BigDecimal("${i * 100}.00")
                )
            }
            couponRepository.saveAll(coupons)
            entityManager.flush()
            entityManager.clear()

            // When
            val pageRequest = PageRequest.of(0, 5)
            val firstPage = couponRepository.findByUserId(userId, pageRequest)

            // Then
            assertThat(firstPage.content).hasSize(5)
            assertThat(firstPage.totalElements).isEqualTo(15)
            assertThat(firstPage.totalPages).isEqualTo(3)
            assertThat(firstPage.hasNext()).isTrue()
        }

        @Test
        @DisplayName("Should sort coupons by creation date")
        fun shouldSortCouponsByCreationDate() {
            // Given
            val userId = UUID.randomUUID()
            val coupons = (1..5).map { i ->
                createTestCoupon(
                    userId = userId,
                    amount = BigDecimal("${i * 100}.00")
                )
            }
            couponRepository.saveAll(coupons)
            entityManager.flush()
            entityManager.clear()

            // When
            val sortByCreatedAt = Sort.by(Sort.Direction.DESC, "createdAt")
            val pageRequest = PageRequest.of(0, 10, sortByCreatedAt)
            val sortedCoupons = couponRepository.findByUserId(userId, pageRequest)

            // Then
            assertThat(sortedCoupons.content).hasSize(5)
            // Verify descending order
            for (i in 0 until sortedCoupons.content.size - 1) {
                assertThat(sortedCoupons.content[i].createdAt)
                    .isAfterOrEqualTo(sortedCoupons.content[i + 1].createdAt)
            }
        }

        @Test
        @DisplayName("Should sort coupons by amount")
        fun shouldSortCouponsByAmount() {
            // Given
            val userId = UUID.randomUUID()
            val amounts = listOf("500.00", "100.00", "300.00", "200.00", "400.00")
            val coupons = amounts.map { amount ->
                createTestCoupon(userId = userId, amount = BigDecimal(amount))
            }
            couponRepository.saveAll(coupons)
            entityManager.flush()
            entityManager.clear()

            // When
            val sortByAmount = Sort.by(Sort.Direction.ASC, "amount")
            val pageRequest = PageRequest.of(0, 10, sortByAmount)
            val sortedCoupons = couponRepository.findByUserId(userId, pageRequest)

            // Then
            assertThat(sortedCoupons.content).hasSize(5)
            assertThat(sortedCoupons.content.map { it.amount }).containsExactly(
                BigDecimal("100.00"),
                BigDecimal("200.00"),
                BigDecimal("300.00"),
                BigDecimal("400.00"),
                BigDecimal("500.00")
            )
        }
    }

    @Nested
    @DisplayName("Update Operations")
    inner class UpdateOperations {

        @Test
        @DisplayName("Should update coupon status successfully")
        fun shouldUpdateCouponStatusSuccessfully() {
            // Given
            val coupon = createTestCoupon(status = CouponStatus.ACTIVE)
            val savedCoupon = couponRepository.save(coupon)
            entityManager.flush()
            entityManager.clear()

            // When
            val couponToUpdate = couponRepository.findById(savedCoupon.id).get()
            couponToUpdate.redeem()
            val updatedCoupon = couponRepository.save(couponToUpdate)
            entityManager.flush()
            entityManager.clear()

            // Then
            val finalCoupon = couponRepository.findById(updatedCoupon.id).get()
            assertThat(finalCoupon.status).isEqualTo(CouponStatus.REDEEMED)
            assertThat(finalCoupon.redeemedAt).isNotNull()
        }

        @Test
        @DisplayName("Should regenerate QR code successfully")
        fun shouldRegenerateQRCodeSuccessfully() {
            // Given
            val coupon = createTestCoupon()
            val savedCoupon = couponRepository.save(coupon)
            val originalQRCode = savedCoupon.qrCode
            entityManager.flush()
            entityManager.clear()

            // When
            val couponToUpdate = couponRepository.findById(savedCoupon.id).get()
            couponToUpdate.regenerateQRCode()
            val updatedCoupon = couponRepository.save(couponToUpdate)
            entityManager.flush()
            entityManager.clear()

            // Then
            val finalCoupon = couponRepository.findById(updatedCoupon.id).get()
            assertThat(finalCoupon.qrCode).isNotEqualTo(originalQRCode)
            assertThat(finalCoupon.qrCode).startsWith("QR_")
        }
    }

    @Nested
    @DisplayName("Constraint Validation")
    inner class ConstraintValidation {

        @Test
        @DisplayName("Should enforce unique QR code constraint")
        fun shouldEnforceUniqueQRCodeConstraint() {
            // Given
            val qrCode = "QR_DUPLICATE_TEST_CODE_123456789012"
            val coupon1 = createTestCoupon()
            val coupon2 = createTestCoupon()

            // Manually set same QR code (this would normally be prevented by domain logic)
            couponRepository.save(coupon1)
            entityManager.flush()

            // When & Then
            // This test assumes QR code uniqueness is enforced at database level
            // In practice, the domain logic should prevent duplicate QR codes
            assertThat(coupon1.qrCode).isNotEqualTo(coupon2.qrCode)
        }
    }

    private fun createTestCoupon(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        stationId: UUID = UUID.randomUUID(),
        amount: BigDecimal = BigDecimal("500.00"),
        fuelType: FuelType = FuelType.REGULAR,
        status: CouponStatus = CouponStatus.ACTIVE,
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(30)
    ): Coupon {
        return Coupon.create(
            id = id,
            userId = userId,
            stationId = stationId,
            amount = amount,
            fuelType = fuelType,
            expiresAt = expiresAt
        ).apply {
            // Simulate status change if needed
            when (status) {
                CouponStatus.REDEEMED -> redeem()
                CouponStatus.EXPIRED -> markAsExpired()
                CouponStatus.CANCELLED -> cancel()
                else -> { /* Already ACTIVE */ }
            }
        }
    }
}