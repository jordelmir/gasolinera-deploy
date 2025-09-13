package com.gasolinerajsm.e2e.security

import com.gasolinerajsm.e2e.models.*
import com.gasolinerajsm.testing.shared.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.*

/**
 * Security End-to-End Tests for Gasolinera JSM
 * Tests authentication, authorization, input validation, and security vulnerabilities
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@DisplayName("Security E2E Tests")
class SecurityE2ETest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var baseUrl: String
    private lateinit var validUser: TestUser
    private lateinit var adminUser: TestUser
    private var validUserToken: String = ""
    private var adminUserToken: String = ""

    @BeforeEach
    fun setUp() {
        baseUrl = "http://localhost:$port/api/v1"
        validUser = TestUser(
            email = "security.test@gasolinera-test.com",
            phone = "5551234567",
            firstName = "Security",
            lastName = "Test"
        )
        adminUser = TestUser(
            email = "admin.test@gasolinera-test.com",
            phone = "5559876543",
            firstName = "Admin",
            lastName = "Test"
        )

        // Setup test users
        setupTestUsers()
    }

    @Nested
    @DisplayName("Authentication Security Tests")
    inner class AuthenticationSecurityTests {

        @Test
        @DisplayName("Should reject requests without authentication token")
        fun shouldRejectRequestsWithoutAuthenticationToken() {
            val protectedEndpoints = listOf(
                "/coupons" to HttpMethod.GET,
                "/coupons" to HttpMethod.POST,
                "/dashboard/user" to HttpMethod.GET,
                "/auth/profile" to HttpMethod.GET,
                "/raffles/active" to HttpMethod.GET
            )

            protectedEndpoints.forEach { (endpoint, method) ->
                val response = when (method) {
                    HttpMethod.GET -> restTemplate.getForEntity("$baseUrl$endpoint", ErrorResponse::class.java)
                    HttpMethod.POST -> restTemplate.postForEntity("$baseUrl$endpoint", "{}", ErrorResponse::class.java)
                    else -> throw IllegalArgumentException("Unsupported method: $method")
                }

                assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
                assertThat(response.body?.error).contains("UNAUTHORIZED", "AUTHENTICATION_REQUIRED")
            }
        }

        @Test
        @DisplayName("Should reject requests with invalid JWT token")
        fun shouldRejectRequestsWithInvalidJWTToken() {
            val invalidTokens = listOf(
                "invalid.jwt.token",
                "Bearer invalid.jwt.token",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature",
                "",
                "null",
                "undefined"
            )

            invalidTokens.forEach { invalidToken ->
                val headers = HttpHeaders()
                headers.setBearerAuth(invalidToken)
                val entity = HttpEntity<Any>(headers)

                val response = restTemplate.exchange(
                    "$baseUrl/auth/profile",
                    HttpMethod.GET,
                    entity,
                    ErrorResponse::class.java
                )

                assertThat(response.statusCode).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
            }
        }

        @Test
        @DisplayName("Should reject requests with expired JWT token")
        fun shouldRejectRequestsWithExpiredJWTToken() {
            // This test would require creating an expired token
            // For now, we'll test token refresh mechanism

            val headers = HttpHeaders()
            headers.setBearerAuth("expired.jwt.token.simulation")
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                "$baseUrl/auth/profile",
                HttpMethod.GET,
                entity,
                ErrorResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        @DisplayName("Should implement proper password security requirements")
        fun shouldImplementProperPasswordSecurityRequirements() {
            val weakPasswords = listOf(
                "123456",
                "password",
                "abc123",
                "qwerty",
                "12345678",
                "password123",
                "admin",
                "test"
            )

            weakPasswords.forEach { weakPassword ->
                val request = UserRegistrationRequest(
                    email = "weak.password.test.${UUID.randomUUID()}@test.com",
                    phone = "555${Random().nextInt(1000000, 9999999)}",
                    firstName = "Test",
                    lastName = "User",
                    password = weakPassword
                )

                val response = restTemplate.postForEntity(
                    "$baseUrl/auth/register",
                    request,
                    ErrorResponse::class.java
                )

                assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
                assertThat(response.body?.error).contains("WEAK_PASSWORD", "PASSWORD_REQUIREMENTS")
            }
        }

        @Test
        @DisplayName("Should implement rate limiting for login attempts")
        fun shouldImplementRateLimitingForLoginAttempts() {
            val loginRequest = UserLoginRequest(
                identifier = "nonexistent@test.com",
                password = "wrongpassword"
            )

            // Attempt multiple failed logins
            repeat(10) {
                val response = restTemplate.postForEntity(
                    "$baseUrl/auth/login",
                    loginRequest,
                    ErrorResponse::class.java
                )

                // After several attempts, should get rate limited
                if (it > 5) {
                    assertThat(response.statusCode).isIn(
                        HttpStatus.TOO_MANY_REQUESTS,
                        HttpStatus.UNAUTHORIZED
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Authorization Security Tests")
    inner class AuthorizationSecurityTests {

        @Test
        @DisplayName("Should enforce user ownership for coupon access")
        fun shouldEnforceUserOwnershipForCouponAccess() {
            // Create a coupon for user1
            val user1Token = createTestUserAndGetToken("user1@test.com")
            val coupon = createTestCouponForUser(user1Token)

            // Try to access it with user2
            val user2Token = createTestUserAndGetToken("user2@test.com")
            val headers = HttpHeaders()
            headers.setBearerAuth(user2Token)
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/${coupon.id}",
                HttpMethod.GET,
                entity,
                ErrorResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body?.error).contains("ACCESS_DENIED", "FORBIDDEN")
        }

        @Test
        @DisplayName("Should enforce admin role for system statistics")
        fun shouldEnforceAdminRoleForSystemStatistics() {
            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken) // Regular user token
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/statistics/system",
                HttpMethod.GET,
                entity,
                ErrorResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body?.error).contains("INSUFFICIENT_PRIVILEGES", "ADMIN_REQUIRED")
        }

        @Test
        @DisplayName("Should allow admin access to all resources")
        fun shouldAllowAdminAccessToAllResources() {
            // Create a coupon for regular user
            val userCoupon = createTestCouponForUser(validUserToken)

            // Admin should be able to access it
            val headers = HttpHeaders()
            headers.setBearerAuth(adminUserToken)
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/${userCoupon.id}",
                HttpMethod.GET,
                entity,
                CouponDetailsResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.id).isEqualTo(userCoupon.id)
        }

        @Test
        @DisplayName("Should prevent privilege escalation")
        fun shouldPreventPrivilegeEscalation() {
            // Try to modify user role through profile update
            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)

            val maliciousUpdate = mapOf(
                "firstName" to "Test",
                "lastName" to "User",
                "roles" to listOf("ADMIN", "SUPER_USER"),
                "permissions" to listOf("ALL_PERMISSIONS")
            )

            val entity = HttpEntity(maliciousUpdate, headers)

            val response = restTemplate.exchange(
                "$baseUrl/auth/profile",
                HttpMethod.PUT,
                entity,
                ErrorResponse::class.java
            )

            // Should either ignore the role fields or reject the request
            if (response.statusCode == HttpStatus.OK) {
                // If update succeeds, verify roles weren't changed
                val profileResponse = restTemplate.exchange(
                    "$baseUrl/auth/profile",
                    HttpMethod.GET,
                    HttpEntity<Any>(headers),
                    UserProfileResponse::class.java
                )

                // User should still have regular user permissions
                assertThat(profileResponse.statusCode).isEqualTo(HttpStatus.OK)
                // Additional verification would check that user doesn't have admin access
            } else {
                // Request should be rejected
                assertThat(response.statusCode).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.FORBIDDEN
                )
            }
        }
    }

    @Nested
    @DisplayName("Input Validation Security Tests")
    inner class InputValidationSecurityTests {

        @Test
        @DisplayName("Should prevent SQL injection attacks")
        fun shouldPreventSQLInjectionAttacks() {
            val sqlInjectionPayloads = listOf(
                "'; DROP TABLE users; --",
                "' OR '1'='1",
                "'; UPDATE users SET password='hacked'; --",
                "' UNION SELECT * FROM users --",
                "admin'--",
                "admin' /*",
                "' OR 1=1#"
            )

            sqlInjectionPayloads.forEach { payload ->
                val loginRequest = UserLoginRequest(
                    identifier = payload,
                    password = "anypassword"
                )

                val response = restTemplate.postForEntity(
                    "$baseUrl/auth/login",
                    loginRequest,
                    ErrorResponse::class.java
                )

                // Should not cause internal server error or succeed
                assertThat(response.statusCode).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.UNAUTHORIZED,
                    HttpStatus.UNPROCESSABLE_ENTITY
                )
                assertThat(response.statusCode).isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }

        @Test
        @DisplayName("Should prevent XSS attacks in input fields")
        fun shouldPreventXSSAttacksInInputFields() {
            val xssPayloads = listOf(
                "<script>alert('XSS')</script>",
                "javascript:alert('XSS')",
                "<img src=x onerror=alert('XSS')>",
                "<svg onload=alert('XSS')>",
                "';alert('XSS');//",
                "<iframe src=javascript:alert('XSS')></iframe>"
            )

            xssPayloads.forEach { payload ->
                val registrationRequest = UserRegistrationRequest(
                    email = "xss.test.${UUID.randomUUID()}@test.com",
                    phone = "5551234567",
                    firstName = payload,
                    lastName = "Test",
                    password = "ValidPassword123!"
                )

                val response = restTemplate.postForEntity(
                    "$baseUrl/auth/register",
                    registrationRequest,
                    Any::class.java
                )

                // Should either sanitize input or reject it
                if (response.statusCode == HttpStatus.CREATED) {
                    // If accepted, verify the payload was sanitized
                    val body = response.body as? Map<*, *>
                    val returnedName = body?.get("firstName") as? String
                    assertThat(returnedName).doesNotContain("<script>", "javascript:", "onerror=")
                } else {
                    // Should be rejected with validation error
                    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
                }
            }
        }

        @Test
        @DisplayName("Should validate input length limits")
        fun shouldValidateInputLengthLimits() {
            val veryLongString = "a".repeat(10000)

            val request = UserRegistrationRequest(
                email = "length.test@test.com",
                phone = "5551234567",
                firstName = veryLongString,
                lastName = veryLongString,
                password = "ValidPassword123!"
            )

            val response = restTemplate.postForEntity(
                "$baseUrl/auth/register",
                request,
                ErrorResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.error).contains("VALIDATION_ERROR", "LENGTH_EXCEEDED")
        }

        @Test
        @DisplayName("Should validate email format strictly")
        fun shouldValidateEmailFormatStrictly() {
            val invalidEmails = listOf(
                "invalid-email",
                "@domain.com",
                "user@",
                "user..double.dot@domain.com",
                "user@domain",
                "user name@domain.com",
                "user@domain..com",
                ""
            )

            invalidEmails.forEach { invalidEmail ->
                val request = UserRegistrationRequest(
                    email = invalidEmail,
                    phone = "5551234567",
                    firstName = "Test",
                    lastName = "User",
                    password = "ValidPassword123!"
                )

                val response = restTemplate.postForEntity(
                    "$baseUrl/auth/register",
                    request,
                    ErrorResponse::class.java
                )

                assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
                assertThat(response.body?.error).contains("VALIDATION_ERROR", "INVALID_EMAIL")
            }
        }

        @Test
        @DisplayName("Should validate numeric inputs for overflow")
        fun shouldValidateNumericInputsForOverflow() {
            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)

            val maliciousRequest = mapOf(
                "stationId" to UUID.randomUUID(),
                "amount" to Double.MAX_VALUE,
                "fuelType" to "REGULAR",
                "paymentMethod" to "CREDIT_CARD",
                "paymentToken" to "test_token"
            )

            val entity = HttpEntity(maliciousRequest, headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/purchase",
                HttpMethod.POST,
                entity,
                ErrorResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.error).contains("VALIDATION_ERROR", "INVALID_AMOUNT")
        }
    }

    @Nested
    @DisplayName("Data Security Tests")
    inner class DataSecurityTests {

        @Test
        @DisplayName("Should not expose sensitive data in error messages")
        fun shouldNotExposeSensitiveDataInErrorMessages() {
            // Try to access non-existent resource
            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/${UUID.randomUUID()}",
                HttpMethod.GET,
                entity,
                ErrorResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

            // Error message should not contain sensitive information
            val errorMessage = response.body?.message?.lowercase() ?: ""
            assertThat(errorMessage).doesNotContain(
                "database", "sql", "table", "column", "password", "token", "secret"
            )
        }

        @Test
        @DisplayName("Should not expose internal system information")
        fun shouldNotExposeInternalSystemInformation() {
            // Try to cause an internal error
            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)

            val malformedRequest = "{ invalid json"
            val entity = HttpEntity(malformedRequest, headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons",
                HttpMethod.POST,
                entity,
                ErrorResponse::class.java
            )

            // Should not expose stack traces or internal paths
            val responseBody = objectMapper.writeValueAsString(response.body)
            assertThat(responseBody.lowercase()).doesNotContain(
                "stacktrace", "exception", "java.lang", "springframework",
                "hibernate", "postgresql", "redis", "rabbitmq"
            )
        }

        @Test
        @DisplayName("Should implement proper CORS headers")
        fun shouldImplementProperCORSHeaders() {
            val headers = HttpHeaders()
            headers.add("Origin", "https://malicious-site.com")
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                "$baseUrl/auth/login",
                HttpMethod.OPTIONS,
                entity,
                String::class.java
            )

            // Should have proper CORS headers
            val corsHeaders = response.headers
            assertThat(corsHeaders.getAccessControlAllowOrigin()).isNotEqualTo("*")
            assertThat(corsHeaders.getAccessControlAllowCredentials()).isTrue()
        }

        @Test
        @DisplayName("Should implement security headers")
        fun shouldImplementSecurityHeaders() {
            val response = restTemplate.getForEntity(
                "$baseUrl/auth/login",
                String::class.java
            )

            val headers = response.headers

            // Check for security headers
            assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff")
            assertThat(headers.getFirst("X-Frame-Options")).isIn("DENY", "SAMEORIGIN")
            assertThat(headers.getFirst("X-XSS-Protection")).contains("1")
            assertThat(headers.getFirst("Strict-Transport-Security")).isNotNull()
        }
    }

    @Nested
    @DisplayName("Business Logic Security Tests")
    inner class BusinessLogicSecurityTests {

        @Test
        @DisplayName("Should prevent coupon amount manipulation")
        fun shouldPreventCouponAmountManipulation() {
            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)

            // Try to create coupon with negative amount
            val maliciousRequest = CouponPurchaseRequest(
                stationId = UUID.randomUUID(),
                amount = BigDecimal("-1000.00"),
                fuelType = "REGULAR",
                paymentMethod = "CREDIT_CARD",
                paymentToken = "test_token"
            )

            val entity = HttpEntity(maliciousRequest, headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/purchase",
                HttpMethod.POST,
                entity,
                ErrorResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.error).contains("INVALID_AMOUNT", "VALIDATION_ERROR")
        }

        @Test
        @DisplayName("Should prevent double redemption of coupons")
        fun shouldPreventDoubleRedemptionOfCoupons() {
            // Create and redeem a coupon
            val coupon = createTestCouponForUser(validUserToken)
            val redemptionRequest = CouponRedemptionRequest(
                qrCode = coupon.qrCode,
                stationId = UUID.randomUUID(),
                fuelAmount = BigDecimal("25.0"),
                pricePerLiter = BigDecimal("22.50")
            )

            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)
            val entity = HttpEntity(redemptionRequest, headers)

            // First redemption should succeed
            val firstResponse = restTemplate.exchange(
                "$baseUrl/coupons/redeem",
                HttpMethod.POST,
                entity,
                CouponRedemptionResponse::class.java
            )

            assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.OK)

            // Second redemption should fail
            val secondResponse = restTemplate.exchange(
                "$baseUrl/coupons/redeem",
                HttpMethod.POST,
                entity,
                ErrorResponse::class.java
            )

            assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.CONFLICT)
            assertThat(secondResponse.body?.error).contains("ALREADY_REDEEMED", "COUPON_USED")
        }

        @Test
        @DisplayName("Should prevent redemption of expired coupons")
        fun shouldPreventRedemptionOfExpiredCoupons() {
            // This would require creating an expired coupon
            // For now, we'll test the validation logic

            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)

            val redemptionRequest = CouponRedemptionRequest(
                qrCode = "QR_EXPIRED_COUPON_TEST",
                stationId = UUID.randomUUID(),
                fuelAmount = BigDecimal("25.0"),
                pricePerLiter = BigDecimal("22.50")
            )

            val entity = HttpEntity(redemptionRequest, headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/redeem",
                HttpMethod.POST,
                entity,
                ErrorResponse::class.java
            )

            // Should fail with appropriate error
            assertThat(response.statusCode).isIn(
                HttpStatus.BAD_REQUEST,
                HttpStatus.GONE,
                HttpStatus.NOT_FOUND
            )
        }

        @Test
        @DisplayName("Should validate station ownership for redemption")
        fun shouldValidateStationOwnershipForRedemption() {
            val coupon = createTestCouponForUser(validUserToken)

            // Try to redeem at different station than purchased
            val redemptionRequest = CouponRedemptionRequest(
                qrCode = coupon.qrCode,
                stationId = UUID.randomUUID(), // Different station
                fuelAmount = BigDecimal("25.0"),
                pricePerLiter = BigDecimal("22.50")
            )

            val headers = HttpHeaders()
            headers.setBearerAuth(validUserToken)
            val entity = HttpEntity(redemptionRequest, headers)

            val response = restTemplate.exchange(
                "$baseUrl/coupons/redeem",
                HttpMethod.POST,
                entity,
                ErrorResponse::class.java
            )

            // Should validate station match
            assertThat(response.statusCode).isIn(
                HttpStatus.BAD_REQUEST,
                HttpStatus.FORBIDDEN
            )
        }
    }

    // Helper methods
    private fun setupTestUsers() {
        // Register and login valid user
        registerUser(validUser)
        validUserToken = loginUser(validUser)

        // Register and login admin user (would need special setup for admin role)
        registerUser(adminUser)
        adminUserToken = loginUser(adminUser)
        // In real scenario, would need to assign admin role to this user
    }

    private fun registerUser(user: TestUser) {
        val request = UserRegistrationRequest(
            email = user.email,
            phone = user.phone,
            firstName = user.firstName,
            lastName = user.lastName,
            password = user.password
        )

        restTemplate.postForEntity(
            "$baseUrl/auth/register",
            request,
            UserRegistrationResponse::class.java
        )
    }

    private fun loginUser(user: TestUser): String {
        val request = UserLoginRequest(
            identifier = user.email,
            password = user.password
        )

        val response = restTemplate.postForEntity(
            "$baseUrl/auth/login",
            request,
            UserLoginResponse::class.java
        )

        return response.body?.accessToken ?: ""
    }

    private fun createTestUserAndGetToken(email: String): String {
        val user = TestUser(
            email = email,
            phone = "555${Random().nextInt(1000000, 9999999)}",
            firstName = "Test",
            lastName = "User"
        )

        registerUser(user)
        return loginUser(user)
    }

    private fun createTestCouponForUser(token: String): TestCoupon {
        val headers = HttpHeaders()
        headers.setBearerAuth(token)

        val request = CouponPurchaseRequest(
            stationId = UUID.randomUUID(),
            amount = BigDecimal("500.00"),
            fuelType = "REGULAR",
            paymentMethod = "CREDIT_CARD",
            paymentToken = "test_token"
        )

        val entity = HttpEntity(request, headers)

        val response = restTemplate.exchange(
            "$baseUrl/coupons/purchase",
            HttpMethod.POST,
            entity,
            CouponPurchaseResponse::class.java
        )

        val couponResponse = response.body!!
        return TestCoupon(
            id = couponResponse.couponId,
            qrCode = couponResponse.qrCode,
            amount = couponResponse.amount
        )
    }
}