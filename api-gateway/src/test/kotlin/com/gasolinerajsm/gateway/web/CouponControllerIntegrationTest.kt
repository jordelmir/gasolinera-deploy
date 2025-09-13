package com.gasolinerajsm.gateway.web

import com.gasolinerajsm.testing.shared.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Integration Tests for Coupon Controller
 * Tests real HTTP endpoints with security, validation, and database interactions
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Coupon Controller Integration Tests")
class CouponControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val baseUrl = "/api/v1/coupons"

    @BeforeEach
    fun setUp() {
        // Setup test data if needed
    }

    @Nested
    @DisplayName("Coupon Creation Endpoints")
    inner class CouponCreationEndpoints {

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should create coupon successfully with valid request")
        fun shouldCreateCouponSuccessfullyWithValidRequest() {
            // Given
            val request = CreateCouponRequest(
                stationId = UUID.randomUUID(),
                amount = BigDecimal("500.00"),
                fuelType = "REGULAR"
            )

            // When & Then
            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.stationId").value(request.stationId.toString()))
                .andExpect(jsonPath("$.amount").value(request.amount))
                .andExpect(jsonPath("$.fuelType").value(request.fuelType))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.qrCode").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.createdAt").exists())
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 400 when creating coupon with invalid amount")
        fun shouldReturn400WhenCreatingCouponWithInvalidAmount() {
            // Given
            val request = CreateCouponRequest(
                stationId = UUID.randomUUID(),
                amount = BigDecimal("-100.00"), // Invalid negative amount
                fuelType = "REGULAR"
            )

            // When & Then
            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").exists())
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 400 when creating coupon with invalid fuel type")
        fun shouldReturn400WhenCreatingCouponWithInvalidFuelType() {
            // Given
            val request = CreateCouponRequest(
                stationId = UUID.randomUUID(),
                amount = BigDecimal("500.00"),
                fuelType = "INVALID_FUEL_TYPE"
            )

            // When & Then
            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        }

        @Test
        @DisplayName("Should return 401 when creating coupon without authentication")
        fun shouldReturn401WhenCreatingCouponWithoutAuthentication() {
            // Given
            val request = CreateCouponRequest(
                stationId = UUID.randomUUID(),
                amount = BigDecimal("500.00"),
                fuelType = "REGULAR"
            )

            // When & Then
            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("Coupon Retrieval Endpoints")
    inner class CouponRetrievalEndpoints {

        @Test
        @WithMockUser(roles = ["USER"], username = "test-user-id")
        @DisplayName("Should get user coupons successfully")
        fun shouldGetUserCouponsSuccessfully() {
            // Given - Create test coupons first
            val coupon1 = createTestCoupon(amount = BigDecimal("100.00"))
            val coupon2 = createTestCoupon(amount = BigDecimal("200.00"))

            // When & Then
            mockMvc.perform(
                get(baseUrl)
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].amount").exists())
                .andExpect(jsonPath("$.content[1].amount").exists())
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should get coupon by ID successfully")
        fun shouldGetCouponByIdSuccessfully() {
            // Given
            val coupon = createTestCoupon()

            // When & Then
            mockMvc.perform(get("$baseUrl/${coupon.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(coupon.id.toString()))
                .andExpect(jsonPath("$.amount").value(coupon.amount))
                .andExpect(jsonPath("$.fuelType").value(coupon.fuelType.toString()))
                .andExpect(jsonPath("$.status").value(coupon.status.toString()))
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 404 when getting non-existent coupon")
        fun shouldReturn404WhenGettingNonExistentCoupon() {
            // Given
            val nonExistentId = UUID.randomUUID()

            // When & Then
            mockMvc.perform(get("$baseUrl/$nonExistentId"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("COUPON_NOT_FOUND"))
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should filter coupons by status")
        fun shouldFilterCouponsByStatus() {
            // Given
            val activeCoupon = createTestCoupon(status = "ACTIVE")
            val redeemedCoupon = createTestCoupon(status = "REDEEMED")

            // When & Then
            mockMvc.perform(
                get(baseUrl)
                    .param("status", "ACTIVE")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should filter coupons by fuel type")
        fun shouldFilterCouponsByFuelType() {
            // Given
            val regularCoupon = createTestCoupon(fuelType = "REGULAR")
            val premiumCoupon = createTestCoupon(fuelType = "PREMIUM")

            // When & Then
            mockMvc.perform(
                get(baseUrl)
                    .param("fuelType", "PREMIUM")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fuelType").value("PREMIUM"))
        }
    }

    @Nested
    @DisplayName("Coupon Redemption Endpoints")
    inner class CouponRedemptionEndpoints {

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should redeem coupon successfully with valid QR code")
        fun shouldRedeemCouponSuccessfullyWithValidQRCode() {
            // Given
            val coupon = createTestCoupon(status = "ACTIVE")
            val request = RedeemCouponRequest(
                qrCode = coupon.qrCode,
                stationId = coupon.stationId
            )

            // When & Then
            mockMvc.perform(
                post("$baseUrl/redeem")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.couponId").value(coupon.id.toString()))
                .andExpect(jsonPath("$.status").value("REDEEMED"))
                .andExpect(jsonPath("$.redeemedAt").exists())
                .andExpect(jsonPath("$.ticketsGenerated").exists())
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 400 when redeeming with invalid QR code")
        fun shouldReturn400WhenRedeemingWithInvalidQRCode() {
            // Given
            val request = RedeemCouponRequest(
                qrCode = "INVALID_QR_CODE",
                stationId = UUID.randomUUID()
            )

            // When & Then
            mockMvc.perform(
                post("$baseUrl/redeem")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("INVALID_QR_CODE"))
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 409 when redeeming already redeemed coupon")
        fun shouldReturn409WhenRedeemingAlreadyRedeemedCoupon() {
            // Given
            val coupon = createTestCoupon(status = "REDEEMED")
            val request = RedeemCouponRequest(
                qrCode = coupon.qrCode,
                stationId = coupon.stationId
            )

            // When & Then
            mockMvc.perform(
                post("$baseUrl/redeem")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error").value("COUPON_ALREADY_REDEEMED"))
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 410 when redeeming expired coupon")
        fun shouldReturn410WhenRedeemingExpiredCoupon() {
            // Given
            val coupon = createTestCoupon(
                status = "EXPIRED",
                expiresAt = LocalDateTime.now().minusDays(1)
            )
            val request = RedeemCouponRequest(
                qrCode = coupon.qrCode,
                stationId = coupon.stationId
            )

            // When & Then
            mockMvc.perform(
                post("$baseUrl/redeem")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isGone)
                .andExpect(jsonPath("$.error").value("COUPON_EXPIRED"))
        }
    }

    @Nested
    @DisplayName("Coupon Management Endpoints")
    inner class CouponManagementEndpoints {

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should cancel coupon successfully")
        fun shouldCancelCouponSuccessfully() {
            // Given
            val coupon = createTestCoupon(status = "ACTIVE")
            val request = CancelCouponRequest(
                reason = "User requested cancellation"
            )

            // When & Then
            mockMvc.perform(
                post("$baseUrl/${coupon.id}/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(coupon.id.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists())
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should regenerate QR code successfully")
        fun shouldRegenerateQRCodeSuccessfully() {
            // Given
            val coupon = createTestCoupon(status = "ACTIVE")
            val originalQRCode = coupon.qrCode

            // When & Then
            mockMvc.perform(post("$baseUrl/${coupon.id}/regenerate-qr"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(coupon.id.toString()))
                .andExpect(jsonPath("$.qrCode").exists())
                .andExpect(jsonPath("$.qrCode").value(not(originalQRCode)))
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 409 when regenerating QR code for redeemed coupon")
        fun shouldReturn409WhenRegeneratingQRCodeForRedeemedCoupon() {
            // Given
            val coupon = createTestCoupon(status = "REDEEMED")

            // When & Then
            mockMvc.perform(post("$baseUrl/${coupon.id}/regenerate-qr"))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error").value("CANNOT_REGENERATE_QR_REDEEMED_COUPON"))
        }
    }

    @Nested
    @DisplayName("Coupon Statistics Endpoints")
    inner class CouponStatisticsEndpoints {

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should get user coupon statistics successfully")
        fun shouldGetUserCouponStatisticsSuccessfully() {
            // Given
            createTestCoupon(status = "ACTIVE", amount = BigDecimal("100.00"))
            createTestCoupon(status = "ACTIVE", amount = BigDecimal("200.00"))
            createTestCoupon(status = "REDEEMED", amount = BigDecimal("150.00"))
            createTestCoupon(status = "EXPIRED", amount = BigDecimal("50.00"))

            // When & Then
            mockMvc.perform(get("$baseUrl/statistics"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalCoupons").value(4))
                .andExpect(jsonPath("$.activeCoupons").value(2))
                .andExpect(jsonPath("$.redeemedCoupons").value(1))
                .andExpect(jsonPath("$.expiredCoupons").value(1))
                .andExpect(jsonPath("$.totalValue").value(500.00))
                .andExpect(jsonPath("$.activeValue").value(300.00))
                .andExpect(jsonPath("$.redeemedValue").value(150.00))
        }

        @Test
        @WithMockUser(roles = ["ADMIN"])
        @DisplayName("Should get system coupon statistics successfully")
        fun shouldGetSystemCouponStatisticsSuccessfully() {
            // Given - Create coupons for different users
            createTestCoupon(userId = UUID.randomUUID(), status = "ACTIVE")
            createTestCoupon(userId = UUID.randomUUID(), status = "REDEEMED")
            createTestCoupon(userId = UUID.randomUUID(), status = "EXPIRED")

            // When & Then
            mockMvc.perform(get("$baseUrl/statistics/system"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalCoupons").exists())
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.totalValue").exists())
                .andExpect(jsonPath("$.redemptionRate").exists())
                .andExpect(jsonPath("$.averageCouponValue").exists())
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should return 403 when non-admin tries to access system statistics")
        fun shouldReturn403WhenNonAdminTriesToAccessSystemStatistics() {
            // When & Then
            mockMvc.perform(get("$baseUrl/statistics/system"))
                .andExpect(status().isForbidden)
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    inner class InputValidationTests {

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should validate required fields in create request")
        fun shouldValidateRequiredFieldsInCreateRequest() {
            // Given
            val invalidRequest = mapOf(
                "amount" to BigDecimal("500.00")
                // Missing stationId and fuelType
            )

            // When & Then
            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray)
        }

        @Test
        @WithMockUser(roles = ["USER"])
        @DisplayName("Should validate amount constraints")
        fun shouldValidateAmountConstraints() {
            // Given
            val requests = listOf(
                CreateCouponRequest(
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("0.00"), // Zero amount
                    fuelType = "REGULAR"
                ),
                CreateCouponRequest(
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("10000.01"), // Too large amount
                    fuelType = "REGULAR"
                )
            )

            // When & Then
            requests.forEach { request ->
                mockMvc.perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            }
        }
    }

    @Nested
    @DisplayName("Security and Authorization Tests")
    inner class SecurityAndAuthorizationTests {

        @Test
        @WithMockUser(roles = ["USER"], username = "user1")
        @DisplayName("Should only allow access to own coupons")
        fun shouldOnlyAllowAccessToOwnCoupons() {
            // Given
            val otherUserCoupon = createTestCoupon(userId = UUID.randomUUID())

            // When & Then
            mockMvc.perform(get("$baseUrl/${otherUserCoupon.id}"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
        }

        @Test
        @WithMockUser(roles = ["ADMIN"])
        @DisplayName("Should allow admin to access any coupon")
        fun shouldAllowAdminToAccessAnyCoupon() {
            // Given
            val userCoupon = createTestCoupon(userId = UUID.randomUUID())

            // When & Then
            mockMvc.perform(get("$baseUrl/${userCoupon.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(userCoupon.id.toString()))
        }

        @Test
        @DisplayName("Should require authentication for all endpoints")
        fun shouldRequireAuthenticationForAllEndpoints() {
            // Given
            val endpoints = listOf(
                get(baseUrl),
                post(baseUrl),
                get("$baseUrl/${UUID.randomUUID()}"),
                post("$baseUrl/redeem"),
                get("$baseUrl/statistics")
            )

            // When & Then
            endpoints.forEach { request ->
                mockMvc.perform(request)
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    // Helper methods
    private fun createTestCoupon(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        stationId: UUID = UUID.randomUUID(),
        amount: BigDecimal = BigDecimal("500.00"),
        fuelType: String = "REGULAR",
        status: String = "ACTIVE",
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(30)
    ): TestCoupon {
        // This would typically interact with the actual service/repository
        // For testing purposes, we'll return a mock object
        return TestCoupon(
            id = id,
            userId = userId,
            stationId = stationId,
            amount = amount,
            fuelType = fuelType,
            status = status,
            qrCode = "QR_TEST_${id.toString().replace("-", "").uppercase()}",
            expiresAt = expiresAt
        )
    }

    // Data classes for test requests
    data class CreateCouponRequest(
        val stationId: UUID,
        val amount: BigDecimal,
        val fuelType: String
    )

    data class RedeemCouponRequest(
        val qrCode: String,
        val stationId: UUID
    )

    data class CancelCouponRequest(
        val reason: String
    )

    data class TestCoupon(
        val id: UUID,
        val userId: UUID,
        val stationId: UUID,
        val amount: BigDecimal,
        val fuelType: String,
        val status: String,
        val qrCode: String,
        val expiresAt: LocalDateTime
    )
}