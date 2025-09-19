package com.gasolinerajsm.couponservice.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("Campaign Entity Tests")
class CampaignTest {

    @Nested
    @DisplayName("Campaign Creation Tests")
    inner class CampaignCreationTests {

        @Test
        @DisplayName("Should create campaign with valid data")
        fun shouldCreateCampaignWithValidData() {
            // Given
            val name = "Summer Promotion 2024"
            val description = "Special summer discount campaign"
            val campaignCode = "SUMMER2024"
            val discountValue = BigDecimal("15.00")
            val startDate = LocalDateTime.now().plusDays(1)
            val endDate = LocalDateTime.now().plusDays(30)

            // When
            val campaign = Campaign(
                name = name,
                description = description,
                campaignCode = campaignCode,
                discountValue = discountValue,
                startDate = startDate,
                endDate = endDate,
                budget = BigDecimal("10000.00"),
                maxCoupons = 1000
            )

            // Then
            assertEquals(name, campaign.name)
            assertEquals(description, campaign.description)
            assertEquals(campaignCode, campaign.campaignCode)
            assertEquals(discountValue, campaign.discountValue)
            assertEquals(CampaignStatus.DRAFT, campaign.status)
            assertEquals(CampaignType.PROMOTIONAL, campaign.campaignType)
            assertEquals(DiscountType.PERCENTAGE, campaign.discountType)
            assertEquals(startDate, campaign.startDate)
            assertEquals(endDate, campaign.endDate)
            assertEquals(BigDecimal("10000.00"), campaign.budget)
            assertEquals(BigDecimal.ZERO, campaign.spentAmount)
            assertEquals(1000, campaign.maxCoupons)
            assertEquals(0, campaign.generatedCoupons)
            assertEquals(0, campaign.usedCoupons)
        }

        @Test
        @DisplayName("Should create campaign with minimal required data")
        fun shouldCreateCampaignWithMinimalData() {
            // Given
            val name = "Basic Campaign"
            val campaignCode = "BASIC2024"
            val discountValue = BigDecimal("10.00")
            val startDate = LocalDateTime.now()
            val endDate = LocalDateTime.now().plusDays(7)

            // When
            val campaign = Campaign(
                name = name,
                campaignCode = campaignCode,
                discountValue = discountValue,
                startDate = startDate,
                endDate = endDate
            )

            // Then
            assertEquals(name, campaign.name)
            assertEquals(campaignCode, campaign.campaignCode)
            assertEquals(discountValue, campaign.discountValue)
            assertNotNull(campaign.createdAt)
            assertTrue(campaign.isActive)
        }
    }

    @Nested
    @DisplayName("Campaign Status Tests")
    inner class CampaignStatusTests {

        @Test
        @DisplayName("Should check if campaign is active and scheduled")
        fun shouldCheckIfCampaignIsActiveAndScheduled() {
            // Given
            val now = LocalDateTime.now()
            val activeCampaign = Campaign(
                name = "Active Campaign",
                campaignCode = "ACTIVE2024",
                discountValue = BigDecimal("10.00"),
                startDate = now.minusDays(1),
                endDate = now.plusDays(1),
                isActive = true
            )

            val inactiveCampaign = Campaign(
                name = "Inactive Campaign",
                campaignCode = "INACTIVE2024",
                discountValue = BigDecimal("10.00"),
                startDate = now.minusDays(1),
                endDate = now.plusDays(1),
                isActive = false
            )

            // Then
            assertTrue(activeCampaign.isActiveAndScheduled())
            assertFalse(inactiveCampaign.isActiveAndScheduled())
        }

        @Test
        @DisplayName("Should check campaign usage availability")
        fun shouldCheckCampaignUsageAvailability() {
            // Given
            val campaignWithLimit = Campaign(
                name = "Limited Campaign",
                campaignCode = "LIMITED2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                totalUsageLimit = 100,
                currentUsageCount = 50
            )

            val campaignWithoutLimit = Campaign(
                name = "Unlimited Campaign",
                campaignCode = "UNLIMITED2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7)
            )

            // Then
            assertTrue(campaignWithLimit.hasAvailableUses())
            assertTrue(campaignWithoutLimit.hasAvailableUses())
        }
    }

    @Nested
    @DisplayName("Campaign Business Logic Tests")
    inner class CampaignBusinessLogicTests {

        @Test
        @DisplayName("Should calculate remaining coupon slots")
        fun shouldCalculateRemainingCouponSlots() {
            // Given
            val campaign = Campaign(
                name = "Test Campaign",
                campaignCode = "TEST2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                maxCoupons = 100,
                generatedCoupons = 30
            )

            // When
            val remainingSlots = campaign.getRemainingCouponSlots()

            // Then
            assertEquals(70, remainingSlots)
        }

        @Test
        @DisplayName("Should calculate usage rate")
        fun shouldCalculateUsageRate() {
            // Given
            val campaign = Campaign(
                name = "Test Campaign",
                campaignCode = "TEST2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                generatedCoupons = 100,
                usedCoupons = 25
            )

            // When
            val usageRate = campaign.getUsageRate()

            // Then
            assertEquals(25.0, usageRate, 0.01)
        }

        @Test
        @DisplayName("Should update coupon statistics")
        fun shouldUpdateCouponStatistics() {
            // Given
            val campaign = Campaign(
                name = "Test Campaign",
                campaignCode = "TEST2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7)
            )

            // When
            val updatedCampaign = campaign.updateCouponStats(50, 10)

            // Then
            assertEquals(50, updatedCampaign.generatedCoupons)
            assertEquals(10, updatedCampaign.usedCoupons)
        }
    }

    @Nested
    @DisplayName("Campaign Applicability Tests")
    inner class CampaignApplicabilityTests {

        @Test
        @DisplayName("Should check if campaign applies to station")
        fun shouldCheckIfCampaignAppliesToStation() {
            // Given
            val universalCampaign = Campaign(
                name = "Universal Campaign",
                campaignCode = "UNIVERSAL2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                applicableStations = "ALL"
            )

            val specificCampaign = Campaign(
                name = "Specific Campaign",
                campaignCode = "SPECIFIC2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                applicableStations = "1,2,3"
            )

            // Then
            assertTrue(universalCampaign.appliesTo(1L))
            assertTrue(universalCampaign.appliesTo(999L))
            assertTrue(specificCampaign.appliesTo(1L))
            assertTrue(specificCampaign.appliesTo(2L))
            assertFalse(specificCampaign.appliesTo(999L))
        }
    }

    @Nested
    @DisplayName("Campaign State Management Tests")
    inner class CampaignStateManagementTests {

        @Test
        @DisplayName("Should activate campaign")
        fun shouldActivateCampaign() {
            // Given
            val campaign = Campaign(
                name = "Test Campaign",
                campaignCode = "TEST2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                status = CampaignStatus.DRAFT
            )

            // When
            val activatedCampaign = campaign.activate("admin")

            // Then
            assertEquals(CampaignStatus.ACTIVE, activatedCampaign.status)
            assertEquals("admin", activatedCampaign.updatedBy)
        }

        @Test
        @DisplayName("Should pause campaign")
        fun shouldPauseCampaign() {
            // Given
            val campaign = Campaign(
                name = "Test Campaign",
                campaignCode = "TEST2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                status = CampaignStatus.ACTIVE
            )

            // When
            val pausedCampaign = campaign.pause("admin")

            // Then
            assertEquals(CampaignStatus.PAUSED, pausedCampaign.status)
            assertEquals("admin", pausedCampaign.updatedBy)
        }

        @Test
        @DisplayName("Should complete campaign")
        fun shouldCompleteCampaign() {
            // Given
            val campaign = Campaign(
                name = "Test Campaign",
                campaignCode = "TEST2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                status = CampaignStatus.ACTIVE
            )

            // When
            val completedCampaign = campaign.complete("admin")

            // Then
            assertEquals(CampaignStatus.COMPLETED, completedCampaign.status)
            assertEquals("admin", completedCampaign.updatedBy)
        }

        @Test
        @DisplayName("Should cancel campaign")
        fun shouldCancelCampaign() {
            // Given
            val campaign = Campaign(
                name = "Test Campaign",
                campaignCode = "TEST2024",
                discountValue = BigDecimal("10.00"),
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7),
                status = CampaignStatus.DRAFT
            )

            // When
            val cancelledCampaign = campaign.cancel("admin")

            // Then
            assertEquals(CampaignStatus.CANCELLED, cancelledCampaign.status)
            assertEquals("admin", cancelledCampaign.updatedBy)
        }
    }
}