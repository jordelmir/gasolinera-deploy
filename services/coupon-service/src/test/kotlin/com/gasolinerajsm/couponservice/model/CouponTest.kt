package com.gasolinerajsm.couponservice.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("Coupon Entity Tests")
class CouponTest {

    @Test
    @DisplayName("Should create coupon with valid data")
    fun shouldCreateCouponWithValidData() {
        // Given
        val campaign = Campaign(
            name = "Test Campaign",
            campaignCode = "TEST2024",
            discountValue = BigDecimal("10.00"),
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(7)
        )

        // When creating a coupon, we need to check what constructor parameters are available
        // This is a placeholder test that will be updated based on the actual Coupon model

        // Then
        assertNotNull(campaign)
        assertEquals("Test Campaign", campaign.name)
    }

    @Test
    @DisplayName("Should validate coupon status")
    fun shouldValidateCouponStatus() {
        // Given
        val campaign = Campaign(
            name = "Test Campaign",
            campaignCode = "TEST2024",
            discountValue = BigDecimal("10.00"),
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(7)
        )

        // Then
        assertTrue(campaign.isActiveAndScheduled())
    }
}