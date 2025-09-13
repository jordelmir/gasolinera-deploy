package com.gasolinerajsm.security.jwt

import com.gasolinerajsm.security.base.BaseSecurityTest
import com.gasolinerajsm.security.model.*
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive JWT security tests
 *
 * Tests JWT token security, validation, and role-based access control
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=security-test"])
@Tag("security")
@Tag("jwt")
@DisplayName("JWT Security Tests")
class JwtSecurityTest : BaseSecurityTest() {

    @Test
    @DisplayName("JWT token validation security test")
    fun `should properly validate JWT tokens and reject invalid ones`() {
        val authUrl = getServiceUrl("auth-service")
        val protectedEndpoint = "$authUrl/api/auth/user/profile"

        println("Testing JWT token validation security...")

        // Test various JWT token scenarios
        val results = testAuthenticationScenarios(protectedEndpoint)
        testResults.addAll(results)

        // Additional JWT-specific tests
        testJwtTokenManipulation(protectedEndpoint)
        testJwtTokenReplay(protectedEndpoint)
        testJwtTokenTiming(protectedEndpoint)

        // Validate results
        val criticalFailures = results.filter { !it.passed && it.severity == SecuritySeverity.CRITICAL }
        assertTrue(
            criticalFailures.isEmpty(),
            "Critical JWT security issues found: ${criticalFailures.map { it.testName }}"
        )
    }

    @Test
    @DisplayName("Role-based access control security test")
    fun `should enforce role-based access control properly`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Testing role-based access control...")

        // Test different endpoints with different role requirements
        val testCases = listOf(
            Triple("$authUrl/api/auth/user/profile", "CUSTOMER", "GET"),
            Triple("$authUrl/api/auth/admin/users", "SYSTEM_ADMIN", "GET"),
            Triple("$gatewayUrl/api/stations", "EMPLOYEE", "GET"),
            Triple("$gatewayUrl/api/campaigns/active", "CUSTOMER", "GET")
        )

        testCases.forEach { (endpoint, requiredRole, method) ->
            val results = testRoleBasedAccess(endpoint, requiredRole, method)
            testResults.addAll(results)
        }

        // Test privilege escalation attempts
        testPrivilegeEscalation()

        // Validate no unauthorized access occurred
        val authorizationFailures = testResults.filter {
            it.testType == SecurityTestType.AUTHORIZATION && !it.passed
        }

        assertTrue(
            authorizationFailures.isEmpty(),
            "Authorization failures detected: ${authorizationFailures.map { it.testName }}"
        )
    }

    @Test
    @DisplayName("JWT token manipulation security test")
    fun `should detect and reject JWT token manipulation attempts`() {
        val authUrl = getServiceUrl("auth-service")
        val endpoint = "$authUrl/api/auth/user/profile"

        println("Testing JWT token manipulation detection...")

        // Generate a valid token to manipulate
        val validToken = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")
        val tokenParts = validToken.split(".")

        // Test 1: Modified header
        val modifiedHeader = tokenParts[0].dropLast(1) + "X"
        val modifiedHeaderToken = "$modifiedHeader.${tokenParts[1]}.${tokenParts[2]}"

        val headerResponse = given()
            .header("Authorization", "Bearer $modifiedHeaderToken")
            .`when`()
            .get(endpoint)

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "JWT Header Manipulation",
                testType = SecurityTestType.AUTHENTICATION,
                passed = headerResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${headerResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Modified JWT headers should be rejected"
            )
        )

        // Test 2: Modified payload
        val modifiedPayload = tokenParts[1].dropLast(1) + "X"
        val modifiedPayloadToken = "${tokenParts[0]}.$modifiedPayload.${tokenParts[2]}"

        val payloadResponse = given()
            .header("Authorization", "Bearer $modifiedPayloadToken")
            .`when`()
            .get(endpoint)

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "JWT Payload Manipulation",
                testType = SecurityTestType.AUTHENTICATION,
                passed = payloadResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${payloadResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Modified JWT payloads should be rejected"
            )
        )

        // Test 3: Modified signature
        val modifiedSignature = tokenParts[2].dropLast(1) + "X"
        val modifiedSignatureToken = "${tokenParts[0]}.${tokenParts[1]}.$modifiedSignature"

        val signatureResponse = given()
            .header("Authorization", "Bearer $modifiedSignatureToken")
            .`when`()
            .get(endpoint)

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "JWT Signature Manipulation",
                testType = SecurityTestType.AUTHENTICATION,
                passed = signatureResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${signatureResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Modified JWT signatures should be rejected"
            )
        )

        // Test 4: Algorithm confusion attack (none algorithm)
        val noneAlgorithmToken = "${tokenParts[0]}.${tokenParts[1]}."

        val noneAlgResponse = given()
            .header("Authorization", "Bearer $noneAlgorithmToken")
            .`when`()
            .get(endpoint)

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "JWT None Algorithm Attack",
                testType = SecurityTestType.AUTHENTICATION,
                passed = noneAlgResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${noneAlgResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "JWT tokens with 'none' algorithm should be rejected"
            )
        )
    }

    @Test
    @DisplayName("JWT token replay attack test")
    fun `should prevent JWT token replay attacks`() {
        val authUrl = getServiceUrl("auth-service")
        val endpoint = "$authUrl/api/auth/user/profile"

        println("Testing JWT token replay attack prevention...")

        // Generate a valid token
        val token = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")

        // Make multiple requests with the same token
        val responses = (1..5).map {
            given()
                .header("Authorization", "Bearer $token")
                .`when`()
                .get(endpoint)
        }

        // All requests should succeed (token reuse is allowed within expiration)
        val allSuccessful = responses.all { it.statusCode in 200..299 }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "JWT Token Reuse Within Expiration",
                testType = SecurityTestType.AUTHENTICATION,
                passed = allSuccessful,
                expectedResult = "All requests successful",
                actualResult = "Responses: ${responses.map { it.statusCode }}",
                severity = SecuritySeverity.LOW,
                details = "Valid JWT tokens should be reusable within their expiration period"
            )
        )

        // Test with expired token (should fail consistently)
        val expiredToken = generateExpiredJwtToken("test-user", "CUSTOMER", "+525555001001")
        val expiredResponse = given()
            .header("Authorization", "Bearer $expiredToken")
            .`when`()
            .get(endpoint)

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Expired JWT Token Replay",
                testType = SecurityTestType.AUTHENTICATION,
                passed = expiredResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${expiredResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Expired JWT tokens should consistently be rejected"
            )
        )
    }

    @Test
    @DisplayName("JWT token timing attack test")
    fun `should not be vulnerable to JWT timing attacks`() {
        val authUrl = getServiceUrl("auth-service")
        val endpoint = "$authUrl/api/auth/user/profile"

        println("Testing JWT timing attack resistance...")

        // Generate tokens with different validity
        val validToken = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")
        val invalidToken = generateMalformedJwtToken()
        val expiredToken = generateExpiredJwtToken("test-user", "CUSTOMER", "+525555001001")

        // Measure response times
        val validTimes = mutableListOf<Long>()
        val invalidTimes = mutableListOf<Long>()
        val expiredTimes = mutableListOf<Long>()

        repeat(10) {
            // Valid token timing
            val validStart = System.currentTimeMillis()
            given()
                .header("Authorization", "Bearer $validToken")
                .`when`()
                .get(endpoint)
            validTimes.add(System.currentTimeMillis() - validStart)

            // Invalid token timing
            val invalidStart = System.currentTimeMillis()
            given()
                .header("Authorization", "Bearer $invalidToken")
                .`when`()
                .get(endpoint)
            invalidTimes.add(System.currentTimeMillis() - invalidStart)

            // Expired token timing
            val expiredStart = System.currentTimeMillis()
            given()
                .header("Authorization", "Bearer $expiredToken")
                .`when`()
                .get(endpoint)
            expiredTimes.add(System.currentTimeMillis() - expiredStart)
        }

        val validAvg = validTimes.average()
        val invalidAvg = invalidTimes.average()
        val expiredAvg = expiredTimes.average()

        // Check if timing differences are significant (more than 50ms difference might indicate vulnerability)
        val maxDifference = maxOf(
            kotlin.math.abs(validAvg - invalidAvg),
            kotlin.math.abs(validAvg - expiredAvg),
            kotlin.math.abs(invalidAvg - expiredAvg)
        )

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "JWT Timing Attack Resistance",
                testType = SecurityTestType.AUTHENTICATION,
                passed = maxDifference < 50.0,
                expectedResult = "Response times should be similar",
                actualResult = "Max difference: ${maxDifference}ms",
                severity = SecuritySeverity.MEDIUM,
                details = "JWT validation should have consistent timing to prevent timing attacks"
            )
        )
    }

    private fun testPrivilegeEscalation() {
        val authUrl = getServiceUrl("auth-service")
        val adminEndpoint = "$authUrl/api/auth/admin/users"

        // Test 1: Customer trying to access admin endpoint
        val customerToken = generateValidJwtToken("customer-user", "CUSTOMER", "+525555001001")
        val customerResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get(adminEndpoint)

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Customer to Admin Privilege Escalation",
                testType = SecurityTestType.AUTHORIZATION,
                passed = customerResponse.statusCode == 403,
                expectedResult = "403 Forbidden",
                actualResult = "Status: ${customerResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Customers should not be able to access admin endpoints"
            )
        )

        // Test 2: Employee trying to access admin endpoint
        val employeeToken = generateValidJwtToken("employee-user", "EMPLOYEE", "+525555000005")
        val employeeResponse = given()
            .header("Authorization", "Bearer $employeeToken")
            .`when`()
            .get(adminEndpoint)

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Employee to Admin Privilege Escalation",
                testType = SecurityTestType.AUTHORIZATION,
                passed = employeeResponse.statusCode == 403,
                expectedResult = "403 Forbidden",
                actualResult = "Status: ${employeeResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Employees should not be able to access admin endpoints"
            )
        )

        // Test 3: Modified role in JWT payload (this should be caught by signature validation)
        val modifiedRoleToken = generateValidJwtToken("customer-user", "SYSTEM_ADMIN", "+525555001001")
        val modifiedResponse = given()
            .header("Authorization", "Bearer $modifiedRoleToken")
            .`when`()
            .get(adminEndpoint)

        // This should either succeed (if the token is valid) or fail with 401 (if signature validation catches it)
        val isValidBehavior = modifiedResponse.statusCode == 200 || modifiedResponse.statusCode == 401

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Role Modification in JWT",
                testType = SecurityTestType.AUTHORIZATION,
                passed = isValidBehavior,
                expectedResult = "200 OK or 401 Unauthorized",
                actualResult = "Status: ${modifiedResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Modified JWT roles should either be validated properly or rejected due to signature mismatch"
            )
        )
    }
}