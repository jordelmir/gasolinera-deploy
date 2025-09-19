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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end integration tests for user registration and authentication flow
 *
 * Tests the complete user journey from registration to authentication across services
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=integration-test"])
@Tag("e2e")
@Tag("integration")
@DisplayName("User Authentication Flow E2E Tests")
class UserAuthenticationFlowTest : BaseIntegrationTest() {

    @Test
    @DisplayName("Complete user registration and authentication flow")
    fun `should complete full user registration and authentication flow`() {
        val testPhoneNumber = "+525555999001"
        val baseUrl = baseUrl()
        println("DEBUG: Starting user registration and authentication flow test")
        println("DEBUG: Base URL = $baseUrl")
        println("DEBUG: Test phone number = $testPhoneNumber")

        // Step 1: Request OTP for new user registration
        val otpResponse = given()
            .contentType(ContentType.JSON)
            .body(OtpRequest(testPhoneNumber, "REGISTRATION"))
            .`when`()
            .post("$baseUrl/api/auth/otp/request")
            .then()
            .statusCode(200)
            .extract()
            .`as`(OtpResponse::class.java)

        assertNotNull(otpResponse.sessionToken)
        assertTrue(otpResponse.message.contains("OTP sent"))

        // Step 2: Complete user registration with OTP
        val registrationRequest = mapOf(
            "phone_number" to testPhoneNumber,
            "otp_code" to "123456",
            "first_name" to "Test",
            "last_name" to "User",
            "session_token" to otpResponse.sessionToken
        )

        val registrationResponse = given()
            .contentType(ContentType.JSON)
            .body(registrationRequest)
            .`when`()
            .post("$baseUrl/api/auth/register")
            .then()
            .statusCode(201)
            .extract()
            .`as`(AuthResponse::class.java)

        assertNotNull(registrationResponse.accessToken)
        assertNotNull(registrationResponse.refreshToken)
        assertEquals("Bearer", registrationResponse.tokenType)
        assertEquals("CUSTOMER", registrationResponse.user.role)
        assertEquals(testPhoneNumber, registrationResponse.user.phoneNumber)
        assertTrue(registrationResponse.user.isActive)
        assertTrue(registrationResponse.user.isPhoneVerified)

        // Step 3: Verify user can access protected endpoints
        val userProfile = given()
            .header("Authorization", "Bearer ${registrationResponse.accessToken}")
            .`when`()
            .get("$baseUrl/api/auth/user/profile")
            .then()
            .statusCode(200)
            .extract()
            .`as`(UserInfo::class.java)

        assertEquals(registrationResponse.user.id, userProfile.id)
        assertEquals(testPhoneNumber, userProfile.phoneNumber)
        assertEquals("Test", userProfile.firstName)
        assertEquals("User", userProfile.lastName)

        // Step 4: Test logout functionality
        given()
            .header("Authorization", "Bearer ${registrationResponse.accessToken}")
            .`when`()
            .post("$baseUrl/api/auth/logout")
            .then()
            .statusCode(200)

        // Step 5: Verify token is invalidated after logout
        given()
            .header("Authorization", "Bearer ${registrationResponse.accessToken}")
            .`when`()
            .get("$baseUrl/api/auth/user/profile")
            .then()
            .statusCode(401)

        // Step 6: Test login with existing user
        val loginOtpResponse = given()
            .contentType(ContentType.JSON)
            .body(OtpRequest(testPhoneNumber, "LOGIN"))
            .`when`()
            .post("$baseUrl/api/auth/otp/request")
            .then()
            .statusCode(200)
            .extract()
            .`as`(OtpResponse::class.java)

        val loginResponse = given()
            .contentType(ContentType.JSON)
            .body(LoginRequest(testPhoneNumber, "123456"))
            .`when`()
            .post("$baseUrl/api/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .`as`(AuthResponse::class.java)

        assertNotNull(loginResponse.accessToken)
        assertEquals(registrationResponse.user.id, loginResponse.user.id)

        // Step 7: Test token refresh
        val refreshResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("refresh_token" to loginResponse.refreshToken))
            .`when`()
            .post("$baseUrl/api/auth/refresh")
            .then()
            .statusCode(200)
            .extract()
            .`as`(AuthResponse::class.java)

        assertNotNull(refreshResponse.accessToken)
        assertNotNull(refreshResponse.refreshToken)

        // Step 8: Verify new token works
        given()
            .header("Authorization", "Bearer ${refreshResponse.accessToken}")
            .`when`()
            .get("$baseUrl/api/auth/user/profile")
            .then()
            .statusCode(200)
    }

    @Test
    @DisplayName("Authentication flow with different user roles")
    fun `should handle authentication for different user roles`() {
        val baseUrl = baseUrl()

        // Test customer authentication
        val customerToken = TestUtils.authenticateUser("+525555001001", "123456", baseUrl)
        val customerInfo = TestUtils.getUserInfo("+525555001001", customerToken, baseUrl)
        assertEquals("CUSTOMER", customerInfo.role)

        // Test employee authentication
        val employeeToken = TestUtils.authenticateUser("+525555000005", "123456", baseUrl)
        val employeeInfo = TestUtils.getUserInfo("+525555000005", employeeToken, baseUrl)
        assertEquals("EMPLOYEE", employeeInfo.role)

        // Test station admin authentication
        val adminToken = TestUtils.authenticateUser("+525555000002", "123456", baseUrl)
        val adminInfo = TestUtils.getUserInfo("+525555000002", adminToken, baseUrl)
        assertEquals("STATION_ADMIN", adminInfo.role)

        // Verify role-based access control
        // Customer should not access admin endpoints
        given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$baseUrl/api/admin/users")
            .then()
            .statusCode(403)

        // Admin should access admin endpoints
        given()
            .header("Authorization", "Bearer $adminToken")
            .`when`()
            .get("$baseUrl/api/admin/stations")
            .then()
            .statusCode(200)
    }

    @Test
    @DisplayName("Authentication error handling scenarios")
    fun `should handle authentication errors properly`() {
        val baseUrl = baseUrl()

        // Test invalid phone number format
        given()
            .contentType(ContentType.JSON)
            .body(OtpRequest("invalid-phone", "LOGIN"))
            .`when`()
            .post("$baseUrl/api/auth/otp/request")
            .then()
            .statusCode(400)

        // Test invalid OTP
        given()
            .contentType(ContentType.JSON)
            .body(LoginRequest("+525555001001", "000000"))
            .`when`()
            .post("$baseUrl/api/auth/login")
            .then()
            .statusCode(401)

        // Test expired token
        val expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

        given()
            .header("Authorization", "Bearer $expiredToken")
            .`when`()
            .get("$baseUrl/api/auth/user/profile")
            .then()
            .statusCode(401)

        // Test malformed token
        given()
            .header("Authorization", "Bearer invalid-token")
            .`when`()
            .get("$baseUrl/api/auth/user/profile")
            .then()
            .statusCode(401)

        // Test missing authorization header
        given()
            .`when`()
            .get("$baseUrl/api/auth/user/profile")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("Account security features")
    fun `should enforce account security features`() {
        val baseUrl = baseUrl()
        val testPhone = "+525555999002"

        // Test account lockout after multiple failed attempts
        repeat(6) {
            given()
                .contentType(ContentType.JSON)
                .body(LoginRequest(testPhone, "wrong-otp"))
                .`when`()
                .post("$baseUrl/api/auth/login")
                .then()
                .statusCode(401)
        }

        // Account should be locked now
        given()
            .contentType(ContentType.JSON)
            .body(LoginRequest(testPhone, "123456"))
            .`when`()
            .post("$baseUrl/api/auth/login")
            .then()
            .statusCode(423) // Locked

        // Test OTP rate limiting
        repeat(6) {
            given()
                .contentType(ContentType.JSON)
                .body(OtpRequest("+525555999003", "LOGIN"))
                .`when`()
                .post("$baseUrl/api/auth/otp/request")
        }

        // Should be rate limited
        given()
            .contentType(ContentType.JSON)
            .body(OtpRequest("+525555999003", "LOGIN"))
            .`when`()
            .post("$baseUrl/api/auth/otp/request")
            .then()
            .statusCode(429) // Too Many Requests
    }
}
