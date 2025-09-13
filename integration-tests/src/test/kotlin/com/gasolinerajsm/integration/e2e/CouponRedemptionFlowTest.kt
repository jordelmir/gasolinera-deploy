package com.gasolinerajsm.integration.e2e

import com.gasolinerajsm.integration.base.BaseIntegrationTest
import com.gasolinerajsm.integration.model.*
import com.gasolinerajsm.integration.util.TestUtils
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import kotlin.test.*

/**
 * End-to-end integration tests for coupon redemption workflow
 *
 * Tests the complete coupon redemption journey across multiple services:
 * Auth -> Station -> Coupon -> Redemption -> Raffle
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=integration-test"])
@Tag("e2e")
@Tag("integration")
@DisplayName("Coupon Redemption Flow E2E Tests")
class CouponRedemptionFlowTest : BaseIntegrationTest() {

    @Test
    @DisplayName("Complete coupon redemption workflow")
    fun `should complete full coupon redemption workflow`() {
        val baseUrl = baseUrl()

        // Step 1: Authenticate customer
        val customerToken = TestUtils.authenticateUser("+525555001001", "123456", baseUrl)
        val customerInfo = TestUtils.getUserInfo("+525555001001", customerToken, baseUrl)

        // Step 2: Authenticate employee
        val employeeToken = TestUtils.authenticateUser("+525555000005", "123456", baseUrl)
        val employeeInfo = TestUtils.getUserInfo("+525555000005", employeeToken, baseUrl)

        // Step 3: Get station information
        val stationInfo = TestUtils.getStationInfo(1L, employeeToken, baseUrl)
        assertEquals("JSM001", stationInfo.code)
        assertEquals("ACTIVE", stationInfo.status)

        // Step 4: Get available campaigns
        val campaigns = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$baseUrl/api/campaigns/active")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CampaignInfo::class.java)

        assertTrue(campaigns.isNotEmpty())
        val welcomeCampaign = campaigns.first { it.campaignCode == "WELCOME2024" }
        assertTrue(welcomeCampaign.isActive)

        // Step 5: Get available coupons for the campaign
        val coupons = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("campaignId", welcomeCampaign.id)
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/coupons/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CouponInfo::class.java)

        assertTrue(coupons.isNotEmpty())
        val testCoupon = coupons.first { it.status == "ACTIVE" }

        // Step 6: Validate coupon before redemption
        val validationResponse = TestUtils.validateCoupon(
            testCoupon.couponCode,
            stationInfo.id,
            customerInfo.id,
            employeeToken,
            baseUrl
        )

        assertTrue(validationResponse.valid)
        assertNotNull(validationResponse.coupon)
        assertNotNull(validationResponse.campaign)
        assertEquals(testCoupon.couponCode, validationResponse.coupon!!.couponCode)

        // Step 7: Process redemption
        val redemptionRequest = RedemptionRequest(
            userId = customerInfo.id,
            stationId = stationInfo.id,
            employeeId = employeeInfo.id,
            couponCode = testCoupon.couponCode,
            fuelType = "Premium",
            fuelQuantity = 25.0,
            fuelPricePerUnit = 22.50,
            purchaseAmount = 150.0,
            paymentMethod = "CREDIT_CARD"
        )

        val redemptionResponse = TestUtils.processRedemption(
            redemptionRequest,
            employeeToken,
            baseUrl
        )

        assertNotNull(redemptionResponse.transactionReference)
        assertEquals("COMPLETED", redemptionResponse.status)
        assertEquals(150.0, redemptionResponse.purchaseAmount)
        assertTrue(redemptionResponse.discountAmount > 0)
        assertTrue(redemptionResponse.finalAmount < redemptionResponse.purchaseAmount)
        assertTrue(redemptionResponse.raffleTicketsEarned > 0)

        // Step 8: Verify coupon status changed to USED
        val updatedCoupon = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$baseUrl/api/coupons/${testCoupon.id}")
            .then()
            .statusCode(200)
            .extract()
            .`as`(CouponInfo::class.java)

        assertEquals("USED", updatedCoupon.status)

        // Step 9: Verify redemption appears in user's history
        val redemptionHistory = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/redemptions/history")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", RedemptionResponse::class.java)

        assertTrue(redemptionHistory.any { it.transactionReference == redemptionResponse.transactionReference })

        // Step 10: Verify raffle tickets were awarded
        TestUtils.waitForCondition(Duration.ofSeconds(10)) {
            val raffleTickets = given()
                .header("Authorization", "Bearer $customerToken")
                .queryParam("userId", customerInfo.id)
                .`when`()
                .get("$baseUrl/api/raffles/tickets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("tickets_used", Int::class.java)

            raffleTickets.sum() >= redemptionResponse.raffleTicketsEarned
        }

        // Step 11: Verify audit trail was created
        val auditLogs = given()
            .header("Authorization", "Bearer $employeeToken")
            .queryParam("transactionReference", redemptionResponse.transactionReference)
            .`when`()
            .get("$baseUrl/api/audit/redemptions")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", Map::class.java)

        assertTrue(auditLogs.isNotEmpty())
        assertTrue(auditLogs.any { it["event_type"] == "REDEMPTION_COMPLETED" })
    }

    @Test
    @DisplayName("Coupon redemption with fraud detection")
    fun `should detect and handle fraudulent redemption attempts`() {
        val baseUrl = baseUrl()

        // Authenticate users
        val customerToken = TestUtils.authenticateUser("+525555001002", "123456", baseUrl)
        val customerInfo = TestUtils.getUserInfo("+525555001002", customerToken, baseUrl)
        val employeeToken = TestUtils.authenticateUser("+525555000005", "123456", baseUrl)
        val employeeInfo = TestUtils.getUserInfo("+525555000005", employeeToken, baseUrl)

        // Get a valid coupon
        val coupons = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/coupons/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CouponInfo::class.java)

        val testCoupon = coupons.first { it.status == "ACTIVE" }

        // Attempt 1: Normal redemption
        val redemptionRequest = RedemptionRequest(
            userId = customerInfo.id,
            stationId = 1L,
            employeeId = employeeInfo.id,
            couponCode = testCoupon.couponCode,
            fuelType = "Magna",
            fuelQuantity = 20.0,
            fuelPricePerUnit = 20.80,
            purchaseAmount = 100.0,
            paymentMethod = "CASH"
        )

        val firstRedemption = TestUtils.processRedemption(redemptionRequest, employeeToken, baseUrl)
        assertEquals("COMPLETED", firstRedemption.status)

        // Attempt 2: Try to use the same coupon again (should be detected as fraud)
        given()
            .header("Authorization", "Bearer $employeeToken")
            .contentType(ContentType.JSON)
            .body(redemptionRequest)
            .`when`()
            .post("$baseUrl/api/redemptions")
            .then()
            .statusCode(409) // Conflict - coupon already used

        // Attempt 3: Rapid successive attempts with different coupons (velocity fraud)
        val availableCoupons = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/coupons/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CouponInfo::class.java)
            .filter { it.status == "ACTIVE" }

        // Make rapid redemption attempts
        repeat(5) { index ->
            if (index < availableCoupons.size) {
                val rapidRequest = redemptionRequest.copy(
                    couponCode = availableCoupons[index].couponCode
                )

                val response = given()
                    .header("Authorization", "Bearer $employeeToken")
                    .contentType(ContentType.JSON)
                    .body(rapidRequest)
                    .`when`()
                    .post("$baseUrl/api/redemptions")

                // After a few attempts, should trigger fraud detection
                if (index >= 3) {
                    response.then().statusCode(429) // Too Many Requests
                } else {
                    response.then().statusCode(201)
                }
            }
        }

        // Verify fraud detection logs were created
        TestUtils.waitForCondition(Duration.ofSeconds(5)) {
            val fraudLogs = given()
                .header("Authorization", "Bearer $employeeToken")
                .queryParam("userId", customerInfo.id)
                .`when`()
                .get("$baseUrl/api/fraud/logs")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", Map::class.java)

            fraudLogs.any { it["fraud_type"] == "VELOCITY_FRAUD" }
        }
    }

    @Test
    @DisplayName("Coupon redemption error scenarios")
    fun `should handle coupon redemption error scenarios properly`() {
        val baseUrl = baseUrl()

        val customerToken = TestUtils.authenticateUser("+525555001003", "123456", baseUrl)
        val customerInfo = TestUtils.getUserInfo("+525555001003", customerToken, baseUrl)
        val employeeToken = TestUtils.authenticateUser("+525555000005", "123456", baseUrl)
        val employeeInfo = TestUtils.getUserInfo("+525555000005", employeeToken, baseUrl)

        // Test 1: Invalid coupon code
        val invalidRedemptionRequest = RedemptionRequest(
            userId = customerInfo.id,
            stationId = 1L,
            employeeId = employeeInfo.id,
            couponCode = "INVALID_COUPON",
            fuelType = "Magna",
            fuelQuantity = 20.0,
            fuelPricePerUnit = 20.80,
            purchaseAmount = 100.0,
            paymentMethod = "CASH"
        )

        given()
            .header("Authorization", "Bearer $employeeToken")
            .contentType(ContentType.JSON)
            .body(invalidRedemptionRequest)
            .`when`()
            .post("$baseUrl/api/redemptions")
            .then()
            .statusCode(404) // Coupon not found

        // Test 2: Expired coupon
        val expiredCoupons = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("status", "EXPIRED")
            .`when`()
            .get("$baseUrl/api/coupons")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CouponInfo::class.java)

        if (expiredCoupons.isNotEmpty()) {
            val expiredRequest = invalidRedemptionRequest.copy(
                couponCode = expiredCoupons.first().couponCode
            )

            given()
                .header("Authorization", "Bearer $employeeToken")
                .contentType(ContentType.JSON)
                .body(expiredRequest)
                .`when`()
                .post("$baseUrl/api/redemptions")
                .then()
                .statusCode(410) // Gone - expired
        }

        // Test 3: Insufficient purchase amount
        val activeCoupons = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/coupons/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CouponInfo::class.java)
            .filter { it.status == "ACTIVE" }

        if (activeCoupons.isNotEmpty()) {
            val insufficientRequest = invalidRedemptionRequest.copy(
                couponCode = activeCoupons.first().couponCode,
                purchaseAmount = 10.0 // Too low for most campaigns
            )

            given()
                .header("Authorization", "Bearer $employeeToken")
                .contentType(ContentType.JSON)
                .body(insufficientRequest)
                .`when`()
                .post("$baseUrl/api/redemptions")
                .then()
                .statusCode(400) // Bad Request - insufficient amount
        }

        // Test 4: Invalid station for coupon
        if (activeCoupons.isNotEmpty()) {
            val wrongStationRequest = invalidRedemptionRequest.copy(
                couponCode = activeCoupons.first().couponCode,
                stationId = 999L, // Non-existent station
                purchaseAmount = 150.0
            )

            given()
                .header("Authorization", "Bearer $employeeToken")
                .contentType(ContentType.JSON)
                .body(wrongStationRequest)
                .`when`()
                .post("$baseUrl/api/redemptions")
                .then()
                .statusCode(400) // Bad Request - invalid station
        }
    }

    @Test
    @DisplayName("Concurrent coupon redemption handling")
    fun `should handle concurrent coupon redemption attempts`() {
        val baseUrl = baseUrl()

        val customerToken = TestUtils.authenticateUser("+525555001004", "123456", baseUrl)
        val customerInfo = TestUtils.getUserInfo("+525555001004", customerToken, baseUrl)
        val employeeToken = TestUtils.authenticateUser("+525555000006", "123456", baseUrl)
        val employeeInfo = TestUtils.getUserInfo("+525555000006", employeeToken, baseUrl)

        // Get an active coupon
        val coupons = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/coupons/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CouponInfo::class.java)
            .filter { it.status == "ACTIVE" }

        assertTrue(coupons.isNotEmpty())
        val testCoupon = coupons.first()

        val redemptionRequest = RedemptionRequest(
            userId = customerInfo.id,
            stationId = 1L,
            employeeId = employeeInfo.id,
            couponCode = testCoupon.couponCode,
            fuelType = "Premium",
            fuelQuantity = 25.0,
            fuelPricePerUnit = 22.50,
            purchaseAmount = 150.0,
            paymentMethod = "CREDIT_CARD"
        )

        // Simulate concurrent redemption attempts
        val responses = (1..3).map { attemptNumber ->
            Thread {
                given()
                    .header("Authorization", "Bearer $employeeToken")
                    .contentType(ContentType.JSON)
                    .body(redemptionRequest)
                    .`when`()
                    .post("$baseUrl/api/redemptions")
            }.apply { start() }
        }.map { thread ->
            thread.join()
            // In a real scenario, we'd capture the response from each thread
            // For this test, we'll verify that only one redemption succeeded
        }

        // Verify that the coupon can only be redeemed once
        val finalCouponStatus = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$baseUrl/api/coupons/${testCoupon.id}")
            .then()
            .statusCode(200)
            .extract()
            .`as`(CouponInfo::class.java)

        assertEquals("USED", finalCouponStatus.status)

        // Verify only one redemption record exists for this coupon
        val redemptionHistory = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("couponCode", testCoupon.couponCode)
            .`when`()
            .get("$baseUrl/api/redemptions/by-coupon")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", RedemptionResponse::class.java)

        assertEquals(1, redemptionHistory.size)
        assertEquals("COMPLETED", redemptionHistory.first().status)
    }
}