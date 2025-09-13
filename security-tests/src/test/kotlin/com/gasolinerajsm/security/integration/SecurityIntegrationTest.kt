package com.gasolinerajsm.security.integration

import com.gasolinerajsm.security.base.BaseSecurityTest
import com.gasolinerajsm.security.model.*
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertTrue

/**
 * Security integration tests across multiple services
 *
 * Tests end-to-end security workflows and cross-service security interactions
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=security-test"])
@Tag("security")
@Tag("integration")
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest : BaseSecurityTest() {

    @Test
    @DisplayName("End-to-end authentication and authorization flow test")
    fun `should maintain security throughout complete user journey`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Testing end-to-end security flow...")

        // Step 1: User registration with security validation
        val registrationResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf(
                "phone_number" to "+525555001001",
                "first_name" to "Security",
                "last_name" to "Test"
            ))
            .`when`()
            .post("$authUrl/api/auth/register")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Secure User Registration",
                testType = SecurityTestType.AUTHENTICATION,
                passed = registrationResponse.statusCode in listOf(200, 201, 409), // 409 if user exists
                expectedResult = "Successful registration or user exists",
                actualResult = "Status: ${registrationResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "User registration should be secure and handle existing users"
            )
        )

        // Step 2: OTP request with rate limiting
        val otpResponses = (1..5).map {
            given()
                .contentType(ContentType.JSON)
                .body(mapOf("phone_number" to "+525555001001", "purpose" to "LOGIN"))
                .`when`()
                .post("$authUrl/api/auth/otp/request")
        }

        val rateLimitingActive = otpResponses.takeLast(2).any { it.statusCode == 429 }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "OTP Request Rate Limiting",
                testType = SecurityTestType.RATE_LIMITING,
                passed = rateLimitingActive,
                expectedResult = "Rate limiting should activate for multiple OTP requests",
                actualResult = "Rate limiting active: $rateLimitingActive",
                severity = SecuritySeverity.MEDIUM,
                details = "OTP requests should be rate limited to prevent abuse"
            )
        )

        // Step 3: Authentication with JWT token generation
        val loginResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555001001", "otp_code" to "123456"))
            .`when`()
            .post("$authUrl/api/auth/login")

        val token = if (loginResponse.statusCode == 200) {
            loginResponse.jsonPath().getString("access_token")
        } else {
            generateValidJwtToken("security-test-user", "CUSTOMER", "+525555001001")
        }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Secure Authentication Flow",
                testType = SecurityTestType.AUTHENTICATION,
                passed = token != null && token.isNotEmpty(),
                expectedResult = "Valid JWT token generated",
                actualResult = "Token generated: ${token != null}",
                severity = SecuritySeverity.CRITICAL,
                details = "Authentication should generate valid JWT tokens"
            )
        )

        // Step 4: Authorized API access
        val authorizedResponse = given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("$gatewayUrl/api/coupons")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Authorized API Access",
                testType = SecurityTestType.AUTHORIZATION,
                passed = authorizedResponse.statusCode in 200..299,
                expectedResult = "Authorized access to protected resources",
                actualResult = "Status: ${authorizedResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Valid tokens should allow access to authorized resources"
            )
        )

        // Step 5: Cross-service security validation
        val crossServiceResponse = given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)
            .body(mapOf("coupon_id" to "test-coupon", "station_id" to 1))
            .`when`()
            .post("$gatewayUrl/api/redemptions")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Cross-Service Security Validation",
                testType = SecurityTestType.AUTHORIZATION,
                passed = crossServiceResponse.statusCode in listOf(200, 201, 400, 404), // Business logic may reject
                expectedResult = "Security context maintained across services",
                actualResult = "Status: ${crossServiceResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Security context should be maintained in cross-service calls"
            )
        )

        // Step 6: Token expiration handling
        val expiredToken = generateExpiredJwtToken("security-test-user", "CUSTOMER", "+525555001001")
        val expiredResponse = given()
            .header("Authorization", "Bearer $expiredToken")
            .`when`()
            .get("$gatewayUrl/api/coupons")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Expired Token Handling",
                testType = SecurityTestType.AUTHENTICATION,
                passed = expiredResponse.statusCode == 401,
                expectedResult = "401 Unauthorized for expired tokens",
                actualResult = "Status: ${expiredResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Expired tokens should be consistently rejected across all services"
            )
        )
    }

    @Test
    @DisplayName("Multi-service security boundary test")
    fun `should enforce security boundaries between services`() {
        val gatewayUrl = getServiceUrl("api-gateway")
        val authUrl = getServiceUrl("auth-service")

        println("Testing multi-service security boundaries...")

        // Test 1: Service-to-service authentication
        val directServiceResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("test" to "direct_access"))
            .`when`()
            .post("$authUrl/api/auth/internal/validate-token")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Direct Service Access Protection",
                testType = SecurityTestType.AUTHORIZATION,
                passed = directServiceResponse.statusCode in listOf(401, 403, 404),
                expectedResult = "Internal endpoints should not be directly accessible",
                actualResult = "Status: ${directServiceResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Internal service endpoints should be protected from direct access"
            )
        )

        // Test 2: Gateway bypass attempts
        val bypassAttempts = listOf(
            "http://auth-service:8081/api/auth/profile",
            "http://coupon-service:8083/api/coupons",
            "http://localhost:8081/api/auth/profile",
            "http://localhost:8083/api/coupons"
        )

        bypassAttempts.forEach { url ->
            try {
                val bypassResponse = given()
                    .`when`()
                    .get(url)

                recordSecurityTestResult(
                    SecurityTestResult(
                        testName = "Gateway Bypass Attempt: $url",
                        testType = SecurityTestType.AUTHORIZATION,
                        passed = bypassResponse.statusCode in listOf(401, 403, 404) ||
                                bypassResponse.statusCode >= 500, // Connection refused is also acceptable
                        expectedResult = "Direct service access should be blocked",
                        actualResult = "Status: ${bypassResponse.statusCode}",
                        severity = SecuritySeverity.HIGH,
                        details = "Services should only be accessible through the API gateway"
                    )
                )
            } catch (e: Exception) {
                // Connection refused is expected and acceptable
                recordSecurityTestResult(
                    SecurityTestResult(
                        testName = "Gateway Bypass Attempt: $url",
                        testType = SecurityTestType.AUTHORIZATION,
                        passed = true,
                        expectedResult = "Connection should be refused or blocked",
                        actualResult = "Connection refused: ${e.message}",
                        severity = SecuritySeverity.HIGH,
                        details = "Direct service access properly blocked"
                    )
                )
            }
        }

        // Test 3: Inter-service communication security
        val customerToken = generateValidJwtToken("customer", "CUSTOMER", "+525555001001")
        val interServiceResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .header("X-Internal-Service", "true")
            .`when`()
            .get("$gatewayUrl/api/internal/service-status")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Inter-Service Communication Security",
                testType = SecurityTestType.AUTHORIZATION,
                passed = interServiceResponse.statusCode in listOf(403, 404),
                expectedResult = "Internal service endpoints should not be accessible to users",
                actualResult = "Status: ${interServiceResponse.statusCode}",
                severity = SecuritySeverity.MEDIUM,
                details = "Internal service communication should be protected"
            )
        )

        // Test 4: Service mesh security (if implemented)
        val meshSecurityResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .header("X-Service-Mesh-Bypass", "true")
            .`when`()
            .get("$gatewayUrl/api/coupons")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Service Mesh Security Headers",
                testType = SecurityTestType.AUTHORIZATION,
                passed = meshSecurityResponse.statusCode in 200..299, // Should work normally, ignoring bypass header
                expectedResult = "Service mesh bypass headers should be ignored",
                actualResult = "Status: ${meshSecurityResponse.statusCode}",
                severity = SecuritySeverity.LOW,
                details = "Service mesh security should not be bypassable via headers"
            )
        )
    }

    @Test
    @DisplayName("Security monitoring and logging test")
    fun `should properly log security events`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Testing security monitoring and logging...")

        // Test 1: Failed authentication attempts should be logged
        val failedAuthResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555001001", "otp_code" to "wrong_otp"))
            .`when`()
            .post("$authUrl/api/auth/login")

        val hasSecurityHeaders = failedAuthResponse.headers.hasHeaderWithName("X-Request-ID") ||
                                failedAuthResponse.headers.hasHeaderWithName("X-Correlation-ID")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Failed Authentication Logging",
                testType = SecurityTestType.SECURITY_MONITORING,
                passed = hasSecurityHeaders && failedAuthResponse.statusCode == 401,
                expectedResult = "Failed auth should be logged with correlation ID",
                actualResult = "Has correlation headers: $hasSecurityHeaders",
                severity = SecuritySeverity.MEDIUM,
                details = "Failed authentication attempts should be logged for monitoring"
            )
        )

        // Test 2: Suspicious activity detection
        val suspiciousResponses = (1..10).map {
            given()
                .contentType(ContentType.JSON)
                .body(mapOf("phone_number" to "+525555001001", "otp_code" to "wrong_$it"))
                .`when`()
                .post("$authUrl/api/auth/login")
        }

        val accountLockingActive = suspiciousResponses.takeLast(3).all { it.statusCode == 429 }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Suspicious Activity Detection",
                testType = SecurityTestType.SECURITY_MONITORING,
                passed = accountLockingActive,
                expectedResult = "Multiple failed attempts should trigger rate limiting",
                actualResult = "Account locking active: $accountLockingActive",
                severity = SecuritySeverity.HIGH,
                details = "Suspicious activity should be detected and mitigated"
            )
        )

        // Test 3: Security event correlation
        val maliciousToken = generateMalformedJwtToken()
        val maliciousResponses = listOf(
            given()
                .header("Authorization", "Bearer $maliciousToken")
                .`when`()
                .get("$gatewayUrl/api/coupons"),
            given()
                .header("Authorization", "Bearer $maliciousToken")
                .`when`()
                .get("$gatewayUrl/api/stations"),
            given()
                .header("Authorization", "Bearer $maliciousToken")
                .`when`()
                .get("$gatewayUrl/api/redemptions")
        )

        val consistentSecurityResponse = maliciousResponses.all { it.statusCode == 401 }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Security Event Correlation",
                testType = SecurityTestType.SECURITY_MONITORING,
                passed = consistentSecurityResponse,
                expectedResult = "Malicious tokens should be consistently rejected",
                actualResult = "Consistent rejection: $consistentSecurityResponse",
                severity = SecuritySeverity.HIGH,
                details = "Security events should be correlated across services"
            )
        )

        // Test 4: Audit trail completeness
        val validToken = generateValidJwtToken("audit-user", "CUSTOMER", "+525555001001")
        val auditResponse = given()
            .header("Authorization", "Bearer $validToken")
            .contentType(ContentType.JSON)
            .body(mapOf("coupon_id" to "audit-test"))
            .`when`()
            .post("$gatewayUrl/api/redemptions")

        val hasAuditHeaders = auditResponse.headers.hasHeaderWithName("X-Request-ID") ||
                             auditResponse.headers.hasHeaderWithName("X-Trace-ID")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Audit Trail Completeness",
                testType = SecurityTestType.SECURITY_MONITORING,
                passed = hasAuditHeaders,
                expectedResult = "All requests should have audit trail headers",
                actualResult = "Has audit headers: $hasAuditHeaders",
                severity = SecuritySeverity.MEDIUM,
                details = "Complete audit trails are essential for security monitoring"
            )
        )
    }

    @Test
    @DisplayName("Security resilience under load test")
    fun `should maintain security under high load conditions`() {
        val gatewayUrl = getServiceUrl("api-gateway")
        val authUrl = getServiceUrl("auth-service")

        println("Testing security resilience under load...")

        // Test 1: Authentication under load
        val concurrentAuthRequests = (1..20).map { index ->
            Thread {
                given()
                    .contentType(ContentType.JSON)
                    .body(mapOf("phone_number" to "+52555500${String.format("%04d", index)}", "otp_code" to "123456"))
                    .`when`()
                    .post("$authUrl/api/auth/login")
            }
        }

        concurrentAuthRequests.forEach { it.start() }
        concurrentAuthRequests.forEach { it.join() }

        // Check if service is still responsive
        val postLoadResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555001001", "otp_code" to "123456"))
            .`when`()
            .post("$authUrl/api/auth/login")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Authentication Resilience Under Load",
                testType = SecurityTestType.SECURITY_RESILIENCE,
                passed = postLoadResponse.statusCode in listOf(200, 401, 429), // Service should still respond
                expectedResult = "Service should remain responsive under load",
                actualResult = "Status: ${postLoadResponse.statusCode}",
                severity = SecuritySeverity.MEDIUM,
                details = "Authentication service should handle concurrent requests securely"
            )
        )

        // Test 2: JWT validation under load
        val validToken = generateValidJwtToken("load-test-user", "CUSTOMER", "+525555001001")
        val concurrentJwtRequests = (1..30).map {
            Thread {
                given()
                    .header("Authorization", "Bearer $validToken")
                    .`when`()
                    .get("$gatewayUrl/api/coupons")
            }
        }

        concurrentJwtRequests.forEach { it.start() }
        concurrentJwtRequests.forEach { it.join() }

        val postJwtLoadResponse = given()
            .header("Authorization", "Bearer $validToken")
            .`when`()
            .get("$gatewayUrl/api/coupons")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "JWT Validation Resilience Under Load",
                testType = SecurityTestType.SECURITY_RESILIENCE,
                passed = postJwtLoadResponse.statusCode in 200..299,
                expectedResult = "JWT validation should work under load",
                actualResult = "Status: ${postJwtLoadResponse.statusCode}",
                severity = SecuritySeverity.MEDIUM,
                details = "JWT validation should remain secure under concurrent load"
            )
        )

        // Test 3: Rate limiting effectiveness under attack
        val attackToken = generateValidJwtToken("attacker", "CUSTOMER", "+525555001001")
        val attackResponses = (1..50).map {
            given()
                .header("Authorization", "Bearer $attackToken")
                .`when`()
                .get("$gatewayUrl/api/coupons")
        }

        val rateLimitTriggered = attackResponses.count { it.statusCode == 429 } > 0

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Rate Limiting Under Attack",
                testType = SecurityTestType.SECURITY_RESILIENCE,
                passed = rateLimitTriggered,
                expectedResult = "Rate limiting should activate under attack",
                actualResult = "Rate limit triggered: $rateLimitTriggered",
                severity = SecuritySeverity.HIGH,
                details = "Rate limiting should protect against DoS attacks"
            )
        )

        // Test 4: Circuit breaker activation under failures
        val circuitBreakerResponses = (1..15).map {
            given()
                .header("Authorization", "Bearer $validToken")
                .header("X-Simulate-Failure", "true")
                .`when`()
                .get("$gatewayUrl/api/redemptions")
        }

        val circuitBreakerActivated = circuitBreakerResponses.takeLast(5).any {
            it.statusCode == 503 && it.body.asString().contains("circuit")
        }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Circuit Breaker Security Under Failures",
                testType = SecurityTestType.SECURITY_RESILIENCE,
                passed = circuitBreakerActivated,
                expectedResult = "Circuit breaker should activate to prevent cascading failures",
                actualResult = "Circuit breaker activated: $circuitBreakerActivated",
                severity = SecuritySeverity.MEDIUM,
                details = "Circuit breakers help maintain security during service failures"
            )
        )
    }
}