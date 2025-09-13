package com.gasolinerajsm.integration.util

import com.gasolinerajsm.integration.model.*
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Utility functions for integration tests
 */
object TestUtils {

    /**
     * Authenticate a user and return the access token
     */
    fun authenticateUser(phoneNumber: String, otpCode: String = "123456", baseUrl: String): String {
        // First request OTP
        val otpResponse = given()
            .contentType(ContentType.JSON)
            .body(OtpRequest(phoneNumber))
            .`when`()
            .post("$baseUrl/api/auth/otp/request")
            .then()
            .statusCode(200)
            .extract()
            .`as`(OtpResponse::class.java)

        // Then login with OTP
        val authResponse = given()
            .contentType(ContentType.JSON)
            .body(LoginRequest(phoneNumber, otpCode))
            .`when`()
            .post("$baseUrl/api/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .`as`(AuthResponse::class.java)

        return authResponse.accessToken
    }

    /**
     * Get user information by phone number
     */
    fun getUserInfo(phoneNumber: String, token: String, baseUrl: String): UserInfo {
        return given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("$baseUrl/api/auth/user/profile")
            .then()
            .statusCode(200)
            .extract()
            .`as`(UserInfo::class.java)
    }

    /**
     * Get station information by ID
     */
    fun getStationInfo(stationId: Long, token: String, baseUrl: String): StationInfo {
        return given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("$baseUrl/api/stations/$stationId")
            .then()
            .statusCode(200)
            .extract()
            .`as`(StationInfo::class.java)
    }

    /**
     * Get campaign information by ID
     */
    fun getCampaignInfo(campaignId: Long, token: String, baseUrl: String): CampaignInfo {
        return given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("$baseUrl/api/campaigns/$campaignId")
            .then()
            .statusCode(200)
            .extract()
            .`as`(CampaignInfo::class.java)
    }

    /**
     * Validate a coupon
     */
    fun validateCoupon(
        couponCode: String,
        stationId: Long,
        userId: Long,
        token: String,
        baseUrl: String
    ): CouponValidationResponse {
        return given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)
            .body(CouponValidationRequest(couponCode, stationId, userId))
            .`when`()
            .post("$baseUrl/api/coupons/validate")
            .then()
            .statusCode(200)
            .extract()
            .`as`(CouponValidationResponse::class.java)
    }

    /**
     * Process a redemption
     */
    fun processRedemption(
        request: RedemptionRequest,
        token: String,
        baseUrl: String
    ): RedemptionResponse {
        return given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("$baseUrl/api/redemptions")
            .then()
            .statusCode(201)
            .extract()
            .`as`(RedemptionResponse::class.java)
    }

    /**
     * Get available raffles
     */
    fun getAvailableRaffles(token: String, baseUrl: String): List<RaffleInfo> {
        return given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("$baseUrl/api/raffles/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", RaffleInfo::class.java)
    }

    /**
     * Enter a raffle
     */
    fun enterRaffle(
        request: RaffleEntryRequest,
        token: String,
        baseUrl: String
    ): RaffleEntryResponse {
        return given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("$baseUrl/api/raffles/entries")
            .then()
            .statusCode(201)
            .extract()
            .`as`(RaffleEntryResponse::class.java)
    }

    /**
     * Get available advertisements
     */
    fun getAvailableAds(userId: Long, stationId: Long, token: String, baseUrl: String): List<AdInfo> {
        return given()
            .header("Authorization", "Bearer $token")
            .queryParam("userId", userId)
            .queryParam("stationId", stationId)
            .`when`()
            .get("$baseUrl/api/ads/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", AdInfo::class.java)
    }

    /**
     * Record ad engagement
     */
    fun recordAdEngagement(
        request: AdEngagementRequest,
        token: String,
        baseUrl: String
    ): AdEngagementResponse {
        return given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("$baseUrl/api/ads/engagements")
            .then()
            .statusCode(201)
            .extract()
            .`as`(AdEngagementResponse::class.java)
    }

    /**
     * Wait for a condition to be true with timeout
     */
    fun waitForCondition(
        timeout: Duration = Duration.ofSeconds(30),
        pollInterval: Duration = Duration.ofSeconds(1),
        condition: () -> Boolean
    ) {
        await.atMost(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .pollInterval(pollInterval.toMillis(), TimeUnit.MILLISECONDS)
            .until { condition() }
    }

    /**
     * Wait for HTTP endpoint to be available
     */
    fun waitForEndpoint(url: String, timeout: Duration = Duration.ofSeconds(60)) {
        await.atMost(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until {
                try {
                    given()
                        .`when`()
                        .get("$url/actuator/health")
                        .then()
                        .statusCode(200)
                    true
                } catch (e: Exception) {
                    false
                }
            }
    }

    /**
     * Generate a unique session ID for testing
     */
    fun generateSessionId(): String {
        return "test-session-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    /**
     * Generate a unique transaction reference for testing
     */
    fun generateTransactionReference(): String {
        return "TXN-TEST-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    /**
     * Verify response contains expected fields
     */
    fun Response.verifySuccessResponse(): Response {
        return this.then()
            .statusCode(200)
            .extract()
            .response()
    }

    /**
     * Verify error response format
     */
    fun Response.verifyErrorResponse(expectedStatus: Int): ErrorResponse {
        return this.then()
            .statusCode(expectedStatus)
            .extract()
            .`as`(ErrorResponse::class.java)
    }
}