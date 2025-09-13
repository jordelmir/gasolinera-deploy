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
            val startDate = LocalDateTime.now().plusDays(1)
            val endDate = LocalDateTime.now().plusDays(30)

            // When
            val campaign = Campaign(
                name = name,
                description = description,
                startDate = startDate,
                endDate = endDate,
                budget = BigDecimal("10000.00"),
                maxCoupons = 1000
            )

            // Then
            assertEquals(name, campaign.name)
            assertEquals(description, campaign.description)
            assertEquals(CampaignStatus.DRAFT, campaign.status)
            assertEquals(CampaignType.DISCOUNT, campaign.campaignType)
            assertEquals(startDate, campaign.startDate)
            assertEquals(endDate, campaign.endDate)
            assertEquals(BigDecimal("10000.00"), campaign.budget)
            assertEquals(BigDecimal.ZERO, campaign.spentAmount)
            assertEquals(1000, campaign.maxCoupons)
            assertEquals(0, campaign.generatedCoupons)
            assertEquals(0, campaign.usedCoupons)
        }

        @Test
        @DisplayName("Should create campaign with all optional fields")
        fun shouldCreateCampaignWithAllOptionalFields() {
            // When
            val campaign = Campaign(
                name = "Premium Campaign",
                description = "Exclusive premium customer campaign",
                status = CampaignStatus.ACTIVE,
                campaignType = CampaignType.LOYALTY,
                startDate = LocalDateTime.now().minusDays(1),
                endDate = LocalDateTime.now().plusDays(60),
                budget = BigDecimal("25000.00"),
                maxCoupons = 2500,
                targetAudience = "Premium customers",
                applicableStations = "1, 2, 3, 4",
                applicableFuelTypes = "Premium, Super Premium",
                minimumPurchaseAmount = BigDecimal("100.00"),
                defaultDiscountPercentage = BigDecimal("15.00"),
                defaultRaffleTickets = 10,
                maxUsesPerCoupon = 3,
                maxUsesPerUser = 5,
                createdBy = "admin@gasolinera.com"
            )

            // Then
            assertEquals(CampaignStatus.ACTIVE, campaign.status)
            assertEquals(CampaignType.LOYALTY, campaign.campaignType)
            assertEquals("Premium customers", campaign.targetAudience)
            assertEquals("1, 2, 3, 4", campaign.applicableStations)
            assertEquals("Premium, Super Premium", campaign.applicableFuelTypes)
            assertEquals(BigDecimal("100.00"), campaign.minimumPurchaseAmount)
            assertEquals(BigDecimal("15.00"), campaign.defaultDiscountPercentage)
            assertEquals(10, campaign.defaultRaffleTickets)
            assertEquals(3, campaign.maxUsesPerCoupon)
            assertEquals(5, campaign.maxUsesPerUser)
            assertEquals("admin@gasolinera.com", campaign.createdBy)
        }
    }

    @Nested
    @DisplayName("Campaign Status Tests")
    inner class CampaignStatusTests {

        private val activeCampaign = Campaign(
            id = 1L,
            name = "Active Campaign",
            status = CampaignStatus.ACTIVE,
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now().plusDays(30)
        )

        @Test
        @DisplayName("Should detect active campaign")
        fun shouldDetectActiveCampaign() {
            // When & Then
            assertTrue(activeCampaign.isActive())
            assertFalse(activeCampaign.isExpired())
            assertFalse(activeCampaign.isNotYetStarted())
        }

        @Test
        @DisplayName("Should detect expired campaign")
        fun shouldDetectExpiredCampaign() {
            // Given
            val expiredCampaign = activeCampaign.copy(
                endDate = LocalDateTime.now().minusDays(1)
            )

            // When & Then
            assertFalse(expiredCampaign.isActive())
            assertTrue(expiredCampaign.isExpired())
            assertFalse(expiredCampaign.isNotYetStarted())
        }

        @Test
        @DisplayName("Should detect campaign not yet started")
        fun shouldDetectCampaignNotYetStarted() {
            // Given
            val futureCampaign = activeCampaign.copy(
                startDate = LocalDateTime.now().plusDays(1)
            )

            // When & Then
            assertFalse(futureCampaign.isActive())
            assertFalse(futureCampaign.isExpired())
            assertTrue(futureCampaign.isNotYetStarted())
        }

        @Test
        @DisplayName("Should detect inactive campaign status")
        fun shouldDetectInactiveCampaignStatus() {
            // Given
            val draftCampaign = activeCampaign.copy(status = CampaignStatus.DRAFT)

            // When & Then
            assertFalse(draftCampaign.isActive())
        }
    }

    @Nested
    @DisplayName("Campaign Budget Management Tests")
    inner class CampaignBudgetManagementTests {

        private val budgetCampaign = Campaign(
            id = 1L,
            name = "Budget Campaign",
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now().plusDays(30),
            budget = BigDecimal("5000.00"),
            spentAmount = BigDecimal("2000.00")
        )

        @Test
        @DisplayName("Should check budget remaining")
        fun shouldCheckBudgetRemaining() {
            // When & Then
            assertTrue(budgetCampaign.hasBudgetRemaining())
            assertEquals(BigDecimal("3000.00"), budgetCampaign.getRemainingBudget())
        }

        @Test
        @DisplayName("Should detect over budget campaign")
        fun shouldDetectOverBudgetCampaign() {
            // Given
            val overBudgetCampaign = budgetCampaign.copy(
                spentAmount = BigDecimal("6000.00")
            )

            // When & Then
            assertFalse(overBudgetCampaign.hasBudgetRemaining())
            assertEquals(BigDecimal("-1000.00"), overBudgetCampaign.getRemainingBudget())
        }

        @Test
        @DisplayName("Should calculate budget utilization rate")
        fun shouldCalculateBudgetUtilizationRate() {
            // When
            val utilizationRate = budgetCampaign.getBudgetUtilizationRate()

            // Then
            assertEquals(40.0, utilizationRate, 0.01)
        }

        @Test
        @DisplayName("Should handle unlimited budget")
        fun shouldHandleUnlimitedBudget() {
            // Given
            val unlimitedBudgetCampaign = budgetCampaign.copy(budget = null)

            // When & Then
            assertTrue(unlimitedBudgetCampaign.hasBudgetRemaining())
            assertNull(unlimitedBudgetCampaign.getRemainingBudget())
            assertNull(unlimitedBudgetCampaign.getBudgetUtilizationRate())
        }

        @Test
        @DisplayName("Should update spent amount")
        fun shouldUpdateSpentAmount() {
            // When
            val updatedCampaign = budgetCampaign.updateSpentAmount(BigDecimal("500.00"))

            // Then
            assertEquals(BigDecimal("2500.00"), updatedCampaign.spentAmount)
        }
    }

    @Nested
    @DisplayName("Campaign Coupon Management Tests")
    inner class CampaignCouponManagementTests {

        private val couponCampaign = Campaign(
            id = 1L,
            name = "Coupon Campaign",
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now().plusDays(30),
            maxCoupons = 100,
            generatedCoupons = 60,
            usedCoupons = 25
        )

        @Test
        @DisplayName("Should check if can generate more coupons")
        fun shouldCheckIfCanGenerateMoreCoupons() {
            // When & Then
            assertTrue(couponCampaign.canGenerateMoreCoupons())
            assertEquals(40, couponCampaign.getRemainingCouponSlots())
        }

        @Test
        @DisplayName("Should detect coupon limit reached")
        fun shouldDetectCouponLimitReached() {
            // Given
            val limitReachedCampaign = couponCampaign.copy(generatedCoupons = 100)

            // When & Then
            assertFalse(limitReachedCampaign.canGenerateMoreCoupons())
            assertEquals(0, limitReachedCampaign.getRemainingCouponSlots())
        }

        @Test
        @DisplayName("Should handle unlimited coupons")
        fun shouldHandleUnlimitedCoupons() {
            // Given
            val unlimitedCouponCampaign = couponCampaign.copy(maxCoupons = null)

            // When & Then
            assertTrue(unlimitedCouponCampaign.canGenerateMoreCoupons())
            assertNull(unlimitedCouponCampaign.getRemainingCouponSlots())
        }

        @Test
        @DisplayName("Should calculate usage rate")
        fun shouldCalculateUsageRate() {
            // When
            val usageRate = couponCampaign.getUsageRate()

            // Then
            assertEquals(41.67, usageRate, 0.01)
        }

        @Test
        @DisplayName("Should handle zero generated coupons")
        fun shouldHandleZeroGeneratedCoupons() {
            // Given
            val zeroCouponCampaign = couponCampaign.copy(generatedCoupons = 0, usedCoupons = 0)

            // When
            val usageRate = zeroCouponCampaign.getUsageRate()

            // Then
            assertEquals(0.0, usageRate)
        }

        @Test
        @DisplayName("Should update coupon statistics")
        fun shouldUpdateCouponStatistics() {
            // When
            val updatedCampaign = couponCampaign.updateCouponStats(80, 35)

            // Then
            assertEquals(80, updatedCampaign.generatedCoupons)
            assertEquals(35, updatedCampaign.usedCoupons)
        }
    }

    @Nested
    @DisplayName("Campaign Applicability Tests")
    inner class CampaignApplicabilityTests {

        private val applicabilityCampaign = Campaign(
            id = 1L,
            name = "Applicability Campaign",
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now().plusDays(30),
            applicableStations = "1, 2, 3",
            applicableFuelTypes = "Regular, Premium"
        )

        @Test
        @DisplayName("Should check station applicability")
        fun shouldCheckStationApplicability() {
            // When & Then
            assertTrue(applicabilityCampaign.appliesTo(1L))
            assertTrue(applicabilityCampaign.appliesTo(2L))
            assertTrue(applicabilityCampaign.appliesTo(3L))
            assertFalse(applicabilityCampaign.appliesTo(4L))
        }

        @Test
        @DisplayName("Should check fuel type applicability")
        fun shouldCheckFuelTypeApplicability() {
            // When & Then
            assertTrue(applicabilityCampaign.appliesTo("Regular"))
            assertTrue(applicabilityCampaign.appliesTo("Premium"))
            assertFalse(applicabilityCampaign.appliesTo("Diesel"))
        }

        @Test
        @DisplayName("Should handle universal applicability")
        fun shouldHandleUniversalApplicability() {
            // Given
            val universalCampaign = applicabilityCampaign.copy(
                applicableStations = null,
                applicableFuelTypes = null
            )

            // When & Then
            assertTrue(universalCampaign.appliesTo(999L))
            assertTrue(universalCampaign.appliesTo("Diesel"))
        }

        @Test
        @DisplayName("Should parse applicable stations list")
        fun shouldParseApplicableStationsList() {
            // When
            val stations = applicabilityCampaign.getApplicableStationsList()

            // Then
            assertEquals(3, stations.size)
            assertTrue(stations.contains(1L))
            assertTrue(stations.contains(2L))
            assertTrue(stations.contains(3L))
        }

        @Test
        @DisplayName("Should parse applicable fuel types list")
        fun shouldParseApplicableFuelTypesList() {
            // When
            val fuelTypes = applicabilityCampaign.getApplicableFuelTypesList()

            // Then
            assertEquals(2, fuelTypes.size)
            assertTrue(fuelTypes.contains("Regular"))
            assertTrue(fuelTypes.contains("Premium"))
        }
    }

    @Nested
    @DisplayName("Campaign Discount Tests")
    inner class CampaignDiscountTests {

        @Test
        @DisplayName("Should identify fixed amount discount type")
        fun shouldIdentifyFixedAmountDiscountType() {
            // Given
            val fixedAmountCampaign = Campaign(
                name = "Fixed Amount Campaign",
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(30),
                defaultDiscountAmount = BigDecimal("10.00")
            )

            // When & Then
            assertEquals(DiscountType.FIXED_AMOUNT, fixedAmountCampaign.getDefaultDiscountType())
        }

        @Test
        @DisplayName("Should identify percentage discount type")
        fun shouldIdentifyPercentageDiscountType() {
            // Given
            val percentageCampaign = Campaign(
                name = "Percentage Campaign",
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(30),
                defaultDiscountPercentage = BigDecimal("15.00")
            )

            // When & Then
            assertEquals(DiscountType.PERCENTAGE, percentageCampaign.getDefaultDiscountType())
        }

        @Test
        @DisplayName("Should identify no discount type")
        fun shouldIdentifyNoDiscountType() {
            // Given
            val noDiscountCampaign = Campaign(
                name = "No Discount Campaign",
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(30)
            )

            // When & Then
            assertEquals(DiscountType.NONE, noDiscountCampaign.getDefaultDiscountType())
        }

        @Test
        @DisplayName("Should calculate default fixed amount discount")
        fun shouldCalculateDefaultFixedAmountDiscount() {
            // Given
            val fixedAmountCampaign = Campaign(
                name = "Fixed Amount Campaign",
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(30),
                defaultDiscountAmount = BigDecimal("20.00")
            )

            // When
            val discount = fixedAmountCampaign.calculateDefaultDiscount(BigDecimal("100.00"))

            // Then
            assertEquals(BigDecimal("20.00"), discount)
        }

        @Test
        @DisplayName("Should calculate default percentage discount")
        fun shouldCalculateDefaultPercentageDiscount() {
            // Given
            val percentageCampaign = Campaign(
                name = "Percentage Campaign",
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(30),
                defaultDiscountPercentage = BigDecimal("25.00")
            )

            // When
            val discount = percentageCampaign.calculateDefaultDiscount(BigDecimal("100.00"))

            // Then
            assertEquals(BigDecimal("25.00"), discount)
        }
    }

    @Nested
    @DisplayName("Campaign State Management Tests")
    inner class CampaignStateManagementTests {

        private val testCampaign = Campaign(
            id = 1L,
            name = "Test Campaign",
            status = CampaignStatus.DRAFT,
            startDate = LocalDateTime.now().plusDays(1),
            endDate = LocalDateTime.now().plusDays(30)
        )

        @Test
        @DisplayName("Should activate campaign")
        fun shouldActivateCampaign() {
            // When
            val activatedCampaign = testCampaign.activate("admin@test.com")

            // Then
            assertEquals(CampaignStatus.ACTIVE, activatedCampaign.status)
            assertEquals("admin@test.com", activatedCampaign.updatedBy)
        }

        @Test
        @DisplayName("Should pause campaign")
        fun shouldPauseCampaign() {
            // When
            val pausedCampaign = testCampaign.pause("admin@test.com")

            // Then
            assertEquals(CampaignStatus.PAUSED, pausedCampaign.status)
            assertEquals("admin@test.com", pausedCampaign.updatedBy)
        }

        @Test
        @DisplayName("Should complete campaign")
        fun shouldCompleteCampaign() {
            // When
            val completedCampaign = testCampaign.complete("admin@test.com")

            // Then
            assertEquals(CampaignStatus.COMPLETED, completedCampaign.status)
            assertEquals("admin@test.com", completedCampaign.updatedBy)
        }

        @Test
        @DisplayName("Should cancel campaign")
        fun shouldCancelCampaign() {
            // When
            val cancelledCampaign = testCampaign.cancel("admin@test.com")

            // Then
            assertEquals(CampaignStatus.CANCELLED, cancelledCampaign.status)
            assertEquals("admin@test.com", cancelledCampaign.updatedBy)
        }
    }

    @Nested
    @DisplayName("Campaign Status Enum Tests")
    inner class CampaignStatusEnumTests {

        @Test
        @DisplayName("Should validate coupon generation permissions")
        fun shouldValidateCouponGenerationPermissions() {
            // When & Then
            assertTrue(CampaignStatus.DRAFT.allowsCouponGeneration())
            assertTrue(CampaignStatus.ACTIVE.allowsCouponGeneration())
            assertFalse(CampaignStatus.PAUSED.allowsCouponGeneration())
            assertFalse(CampaignStatus.COMPLETED.allowsCouponGeneration())
            assertFalse(CampaignStatus.CANCELLED.allowsCouponGeneration())
            assertFalse(CampaignStatus.EXPIRED.allowsCouponGeneration())
        }

        @Test
        @DisplayName("Should validate coupon usage permissions")
        fun shouldValidateCouponUsagePermissions() {
            // When & Then
            assertFalse(CampaignStatus.DRAFT.allowsCouponUsage())
            assertTrue(CampaignStatus.ACTIVE.allowsCouponUsage())
            assertFalse(CampaignStatus.PAUSED.allowsCouponUsage())
            assertFalse(CampaignStatus.COMPLETED.allowsCouponUsage())
            assertFalse(CampaignStatus.CANCELLED.allowsCouponUsage())
            assertFalse(CampaignStatus.EXPIRED.allowsCouponUsage())
        }

        @Test
        @DisplayName("Should identify final states")
        fun shouldIdentifyFinalStates() {
            // When & Then
            assertFalse(CampaignStatus.DRAFT.isFinalState())
            assertFalse(CampaignStatus.ACTIVE.isFinalState())
            assertFalse(CampaignStatus.PAUSED.isFinalState())
            assertTrue(CampaignStatus.COMPLETED.isFinalState())
            assertTrue(CampaignStatus.CANCELLED.isFinalState())
            assertTrue(CampaignStatus.EXPIRED.isFinalState())
        }

        @Test
        @DisplayName("Should validate modification permissions")
        fun shouldValidateModificationPermissions() {
            // When & Then
            assertTrue(CampaignStatus.DRAFT.allowsModifications())
            assertFalse(CampaignStatus.ACTIVE.allowsModifications())
            assertTrue(CampaignStatus.PAUSED.allowsModifications())
            assertFalse(CampaignStatus.COMPLETED.allowsModifications())
            assertFalse(CampaignStatus.CANCELLED.allowsModifications())
            assertFalse(CampaignStatus.EXPIRED.allowsModifications())
        }
    }

    @Nested
    @DisplayName("Campaign Type Enum Tests")
    inner class CampaignTypeEnumTests {

        @Test
        @DisplayName("Should identify discount providing types")
        fun shouldIdentifyDiscountProvidingTypes() {
            // When & Then
            assertTrue(CampaignType.DISCOUNT.providesDiscounts())
            assertFalse(CampaignType.RAFFLE_ONLY.providesDiscounts())
            assertTrue(CampaignType.PROMOTIONAL.providesDiscounts())
            assertTrue(CampaignType.SEASONAL.providesDiscounts())
            assertTrue(CampaignType.LOYALTY.providesDiscounts())
            assertTrue(CampaignType.GRAND_OPENING.providesDiscounts())
        }

        @Test
        @DisplayName("Should validate raffle ticket provision")
        fun shouldValidateRaffleTicketProvision() {
            // When & Then - All campaign types provide raffle tickets
            CampaignType.values().forEach { type ->
                assertTrue(type.providesRaffleTickets())
            }
        }
    }

    @Nested
    @DisplayName("Campaign Validation Tests")
    inner class CampaignValidationTests {

        @Test
        @DisplayName("Should validate toString method")
        fun shouldValidateToStringMethod() {
            // Given
            val campaign = Campaign(
                id = 1L,
                name = "Test Campaign",
                status = CampaignStatus.ACTIVE,
                campaignType = CampaignType.DISCOUNT,
                startDate = LocalDateTime.now().minusDays(1),
                endDate = LocalDateTime.now().plusDays(30)
            )

            // When
            val stringRepresentation = campaign.toString()

            // Then
            assertTrue(stringRepresentation.contains("id=1"))
            assertTrue(stringRepresentation.contains("name='Test Campaign'"))
            assertTrue(stringRepresentation.contains("status=ACTIVE"))
            assertTrue(stringRepresentation.contains("type=DISCOUNT"))
        }
    }
}