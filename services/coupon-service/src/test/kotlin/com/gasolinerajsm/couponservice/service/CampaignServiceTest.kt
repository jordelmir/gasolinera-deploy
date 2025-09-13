package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.*
import com.gasolinerajsm.couponservice.repository.CampaignRepository
import com.gasolinerajsm.couponservice.repository.CouponRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@DisplayName("Campaign Service Tests")
class CampaignServiceTest {

    private lateinit var campaignService: CampaignService
    private lateinit var campaignRepository: CampaignRepository
    private lateinit var couponRepository: CouponRepository

    private lateinit var testCampaign: Campaign

    @BeforeEach
    fun setUp() {
        campaignRepository = mock()
        couponRepository = mock()

        campaignService = CampaignService(campaignRepository, couponRepository)

        testCampaign = Campaign(
            id = 1L,
            name = "Test Campaign",
            description = "Test campaign description",
            status = CampaignStatus.DRAFT,
            campaignType = CampaignType.DISCOUNT,
            startDate = LocalDateTime.now().plusDays(1),
            endDate = LocalDateTime.now().plusDays(30),
            budget = BigDecimal("10000.00"),
            maxCoupons = 1000,
            defaultDiscountAmount = BigDecimal("10.00"),
            defaultRaffleTickets = 5,
            createdBy = "admin@test.com"
        )
    }
  @Nested
    @DisplayName("Campaign Creation Tests")
    inner class CampaignCreationTests {

        @Test
        @DisplayName("Should create campaign successfully")
        fun shouldCreateCampaignSuccessfully() {
            // Given
            whenever(campaignRepository.existsByNameIgnoreCase("New Campaign")).thenReturn(false)
            whenever(campaignRepository.save(any<Campaign>())).thenReturn(testCampaign)

            // When
            val result = campaignService.createCampaign(
                name = "New Campaign",
                description = "New campaign description",
                startDate = LocalDateTime.now().plusDays(1),
                endDate = LocalDateTime.now().plusDays(30),
                budget = BigDecimal("5000.00"),
                createdBy = "admin@test.com"
            )

            // Then
            assertNotNull(result)
            verify(campaignRepository).save(any<Campaign>())
        }

        @Test
        @DisplayName("Should throw exception for duplicate campaign name")
        fun shouldThrowExceptionForDuplicateCampaignName() {
            // Given
            whenever(campaignRepository.existsByNameIgnoreCase("Existing Campaign")).thenReturn(true)

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                campaignService.createCampaign(
                    name = "Existing Campaign",
                    startDate = LocalDateTime.now().plusDays(1),
                    endDate = LocalDateTime.now().plusDays(30)
                )
            }
        }

        @Test
        @DisplayName("Should throw exception for invalid date range")
        fun shouldThrowExceptionForInvalidDateRange() {
            // Given
            whenever(campaignRepository.existsByNameIgnoreCase("Test Campaign")).thenReturn(false)

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                campaignService.createCampaign(
                    name = "Test Campaign",
                    startDate = LocalDateTime.now().plusDays(30),
                    endDate = LocalDateTime.now().plusDays(1) // End before start
                )
            }
        }

        @Test
        @DisplayName("Should throw exception for both discount types")
        fun shouldThrowExceptionForBothDiscountTypes() {
            // Given
            whenever(campaignRepository.existsByNameIgnoreCase("Test Campaign")).thenReturn(false)

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                campaignService.createCampaign(
                    name = "Test Campaign",
                    startDate = LocalDateTime.now().plusDays(1),
                    endDate = LocalDateTime.now().plusDays(30),
                    defaultDiscountAmount = BigDecimal("10.00"),
                    defaultDiscountPercentage = BigDecimal("15.00")
                )
            }
        }

        @Test
        @DisplayName("Should throw exception for invalid budget")
        fun shouldThrowExceptionForInvalidBudget() {
            // Given
            whenever(campaignRepository.existsByNameIgnoreCase("Test Campaign")).thenReturn(false)

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                campaignService.createCampaign(
                    name = "Test Campaign",
                    startDate = LocalDateTime.now().plusDays(1),
                    endDate = LocalDateTime.now().plusDays(30),
                    budget = BigDecimal("-1000.00")
                )
            }
        }
    }

    @Nested
    @DisplayName("Campaign Retrieval Tests")
    inner class CampaignRetrievalTests {

        @Test
        @DisplayName("Should get campaign by ID")
        fun shouldGetCampaignById() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))

            // When
            val result = campaignService.getCampaignById(1L)

            // Then
            assertEquals(testCampaign, result)
        }

        @Test
        @DisplayName("Should throw exception for non-existent campaign ID")
        fun shouldThrowExceptionForNonExistentCampaignId() {
            // Given
            whenever(campaignRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                campaignService.getCampaignById(999L)
            }
        }

        @Test
        @DisplayName("Should get campaign by name")
        fun shouldGetCampaignByName() {
            // Given
            whenever(campaignRepository.findByNameIgnoreCase("Test Campaign")).thenReturn(testCampaign)

            // When
            val result = campaignService.getCampaignByName("Test Campaign")

            // Then
            assertEquals(testCampaign, result)
        }

        @Test
        @DisplayName("Should get campaigns by status")
        fun shouldGetCampaignsByStatus() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val campaignsPage = PageImpl(listOf(testCampaign), pageable, 1)
            whenever(campaignRepository.findByStatus(CampaignStatus.DRAFT, pageable)).thenReturn(campaignsPage)

            // When
            val result = campaignService.getCampaignsByStatus(CampaignStatus.DRAFT, pageable)

            // Then
            assertEquals(1, result.content.size)
            assertEquals(testCampaign, result.content[0])
        }

        @Test
        @DisplayName("Should search campaigns")
        fun shouldSearchCampaigns() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val campaignsPage = PageImpl(listOf(testCampaign), pageable, 1)
            whenever(campaignRepository.searchByNameOrDescription("Test", pageable)).thenReturn(campaignsPage)

            // When
            val result = campaignService.searchCampaigns("Test", pageable)

            // Then
            assertEquals(1, result.content.size)
            assertEquals(testCampaign, result.content[0])
        }
    }

    @Nested
    @DisplayName("Campaign Update Tests")
    inner class CampaignUpdateTests {

        @Test
        @DisplayName("Should update campaign successfully")
        fun shouldUpdateCampaignSuccessfully() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            whenever(campaignRepository.save(any<Campaign>())).thenReturn(testCampaign)

            // When
            val result = campaignService.updateCampaign(
                id = 1L,
                description = "Updated description",
                budget = BigDecimal("15000.00"),
                updatedBy = "admin@test.com"
            )

            // Then
            verify(campaignRepository).save(any<Campaign>())
        }

        @Test
        @DisplayName("Should throw exception when updating non-modifiable campaign")
        fun shouldThrowExceptionWhenUpdatingNonModifiableCampaign() {
            // Given
            val activeCampaign = testCampaign.copy(status = CampaignStatus.ACTIVE)
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(activeCampaign))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                campaignService.updateCampaign(id = 1L, description = "New description")
            }
        }

        @Test
        @DisplayName("Should throw exception for duplicate name in update")
        fun shouldThrowExceptionForDuplicateNameInUpdate() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            whenever(campaignRepository.existsByNameIgnoreCase("Existing Campaign")).thenReturn(true)

            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                campaignService.updateCampaign(id = 1L, name = "Existing Campaign")
            }
        }
    }

    @Nested
    @DisplayName("Campaign Status Management Tests")
    inner class CampaignStatusManagementTests {

        @Test
        @DisplayName("Should activate campaign successfully")
        fun shouldActivateCampaignSuccessfully() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            whenever(campaignRepository.save(any<Campaign>())).thenReturn(testCampaign)

            // When
            val result = campaignService.activateCampaign(1L, "admin@test.com")

            // Then
            verify(campaignRepository).save(any<Campaign>())
        }

        @Test
        @DisplayName("Should throw exception when activating final state campaign")
        fun shouldThrowExceptionWhenActivatingFinalStateCampaign() {
            // Given
            val completedCampaign = testCampaign.copy(status = CampaignStatus.COMPLETED)
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(completedCampaign))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                campaignService.activateCampaign(1L)
            }
        }

        @Test
        @DisplayName("Should throw exception when activating expired campaign")
        fun shouldThrowExceptionWhenActivatingExpiredCampaign() {
            // Given
            val expiredCampaign = testCampaign.copy(endDate = LocalDateTime.now().minusDays(1))
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(expiredCampaign))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                campaignService.activateCampaign(1L)
            }
        }

        @Test
        @DisplayName("Should pause campaign successfully")
        fun shouldPauseCampaignSuccessfully() {
            // Given
            val activeCampaign = testCampaign.copy(status = CampaignStatus.ACTIVE)
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(activeCampaign))
            whenever(campaignRepository.save(any<Campaign>())).thenReturn(activeCampaign)

            // When
            val result = campaignService.pauseCampaign(1L, "admin@test.com")

            // Then
            verify(campaignRepository).save(any<Campaign>())
        }

        @Test
        @DisplayName("Should throw exception when pausing non-active campaign")
        fun shouldThrowExceptionWhenPausingNonActiveCampaign() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                campaignService.pauseCampaign(1L)
            }
        }

        @Test
        @DisplayName("Should complete campaign successfully")
        fun shouldCompleteCampaignSuccessfully() {
            // Given
            val activeCampaign = testCampaign.copy(status = CampaignStatus.ACTIVE)
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(activeCampaign))
            whenever(campaignRepository.save(any<Campaign>())).thenReturn(activeCampaign)

            // When
            val result = campaignService.completeCampaign(1L, "admin@test.com")

            // Then
            verify(campaignRepository).save(any<Campaign>())
        }

        @Test
        @DisplayName("Should cancel campaign successfully")
        fun shouldCancelCampaignSuccessfully() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            whenever(campaignRepository.save(any<Campaign>())).thenReturn(testCampaign)

            // When
            val result = campaignService.cancelCampaign(1L, "admin@test.com")

            // Then
            verify(campaignRepository).save(any<Campaign>())
        }
    }

    @Nested
    @DisplayName("Campaign Analytics Tests")
    inner class CampaignAnalyticsTests {

        @Test
        @DisplayName("Should get campaign statistics")
        fun shouldGetCampaignStatistics() {
            // Given
            val stats = mapOf("totalCampaigns" to 10L, "activeCampaigns" to 5L)
            whenever(campaignRepository.getCampaignStatistics()).thenReturn(stats)

            // When
            val result = campaignService.getCampaignStatistics()

            // Then
            assertEquals(stats, result)
        }

        @Test
        @DisplayName("Should get campaign performance metrics")
        fun shouldGetCampaignPerformanceMetrics() {
            // Given
            val metricsData = listOf(arrayOf(testCampaign, 75.0, 60.0))
            whenever(campaignRepository.getCampaignPerformanceMetrics()).thenReturn(metricsData)

            // When
            val result = campaignService.getCampaignPerformanceMetrics()

            // Then
            assertEquals(1, result.size)
            assertEquals(testCampaign, result[0].campaign)
            assertEquals(75.0, result[0].usageRate)
            assertEquals(60.0, result[0].budgetUtilization)
        }

        @Test
        @DisplayName("Should get campaign budget utilization")
        fun shouldGetCampaignBudgetUtilization() {
            // Given
            val campaignWithSpending = testCampaign.copy(
                budget = BigDecimal("1000.00"),
                spentAmount = BigDecimal("600.00")
            )
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(campaignWithSpending))

            // When
            val result = campaignService.getCampaignBudgetUtilization(1L)

            // Then
            assertEquals(60.0, result)
        }

        @Test
        @DisplayName("Should get campaign usage rate")
        fun shouldGetCampaignUsageRate() {
            // Given
            val campaignWithUsage = testCampaign.copy(
                generatedCoupons = 100,
                usedCoupons = 75
            )
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(campaignWithUsage))

            // When
            val result = campaignService.getCampaignUsageRate(1L)

            // Then
            assertEquals(75.0, result)
        }

        @Test
        @DisplayName("Should refresh campaign statistics")
        fun shouldRefreshCampaignStatistics() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))
            whenever(couponRepository.countByCampaign(testCampaign)).thenReturn(150L)
            whenever(couponRepository.countUsedCouponsByCampaign(testCampaign)).thenReturn(75L)

            // When
            val result = campaignService.refreshCampaignStatistics(1L)

            // Then
            verify(campaignRepository).updateCampaignCouponStats(1L, 150, 75)
        }
    }

    @Nested
    @DisplayName("Campaign Maintenance Tests")
    inner class CampaignMaintenanceTests {

        @Test
        @DisplayName("Should update expired campaigns status")
        fun shouldUpdateExpiredCampaignsStatus() {
            // Given
            whenever(campaignRepository.updateExpiredCampaignsStatus(any())).thenReturn(3)

            // When
            val result = campaignService.updateExpiredCampaignsStatus()

            // Then
            assertEquals(3, result)
        }

        @Test
        @DisplayName("Should find campaigns without coupons")
        fun shouldFindCampaignsWithoutCoupons() {
            // Given
            val emptyCampaigns = listOf(testCampaign)
            whenever(campaignRepository.findCampaignsWithoutCoupons()).thenReturn(emptyCampaigns)

            // When
            val result = campaignService.findCampaignsWithoutCoupons()

            // Then
            assertEquals(emptyCampaigns, result)
        }

        @Test
        @DisplayName("Should check if campaign name exists")
        fun shouldCheckIfCampaignNameExists() {
            // Given
            whenever(campaignRepository.existsByNameIgnoreCase("Existing Campaign")).thenReturn(true)
            whenever(campaignRepository.existsByNameIgnoreCase("New Campaign")).thenReturn(false)

            // When & Then
            assertTrue(campaignService.campaignNameExists("Existing Campaign"))
            assertFalse(campaignService.campaignNameExists("New Campaign"))
        }

        @Test
        @DisplayName("Should count campaigns by status")
        fun shouldCountCampaignsByStatus() {
            // Given
            whenever(campaignRepository.countByStatus(CampaignStatus.ACTIVE)).thenReturn(5L)

            // When
            val result = campaignService.countCampaignsByStatus(CampaignStatus.ACTIVE)

            // Then
            assertEquals(5L, result)
        }
    }

    @Nested
    @DisplayName("Campaign Budget Management Tests")
    inner class CampaignBudgetManagementTests {

        @Test
        @DisplayName("Should update campaign spent amount")
        fun shouldUpdateCampaignSpentAmount() {
            // Given
            whenever(campaignRepository.findById(1L)).thenReturn(Optional.of(testCampaign))

            // When
            val result = campaignService.updateCampaignSpentAmount(1L, BigDecimal("500.00"))

            // Then
            verify(campaignRepository).updateCampaignSpentAmount(1L, BigDecimal("500.00"))
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        fun shouldThrowExceptionForNegativeAmount() {
            // When & Then
            assertThrows(IllegalArgumentException::class.java) {
                campaignService.updateCampaignSpentAmount(1L, BigDecimal("-100.00"))
            }
        }

        @Test
        @DisplayName("Should get campaigns with budget remaining")
        fun shouldGetCampaignsWithBudgetRemaining() {
            // Given
            val campaigns = listOf(testCampaign)
            whenever(campaignRepository.findCampaignsWithBudgetRemaining()).thenReturn(campaigns)

            // When
            val result = campaignService.getCampaignsWithBudgetRemaining()

            // Then
            assertEquals(campaigns, result)
        }

        @Test
        @DisplayName("Should get campaigns over budget")
        fun shouldGetCampaignsOverBudget() {
            // Given
            val campaigns = listOf(testCampaign)
            whenever(campaignRepository.findCampaignsOverBudget()).thenReturn(campaigns)

            // When
            val result = campaignService.getCampaignsOverBudget()

            // Then
            assertEquals(campaigns, result)
        }
    }
}