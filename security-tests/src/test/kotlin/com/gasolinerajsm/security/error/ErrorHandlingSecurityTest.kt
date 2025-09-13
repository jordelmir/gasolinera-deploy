package com.gasolinerajsm.security.error

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
 * Comprehensive error handling security tests
 *
 * Tests error handling security, information disclosure prevention,
 * and graceful degradation under failure conditions
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=security-test"])
@Tag("security")
@Tag("error-handling")
@DisplayName("Error Handling Security Tests")
class ErrorHandlingSecurityTest : BaseSecurityTest() {

    @Test
    @DisplayName("Error message information disclosure test")
    fun `should not expose sensitive information in error messages`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Testing error message information disclosure...")

        // Test 1: Database connection errors should not expose database details
        val dbErrorResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555001001", "otp_code" to "invalid"))
            .`when`()
            .post("$authUrl/api/auth/login")

        val dbErrorBody = dbErrorResponse.body.asString().lowercase()
        val containsSensitiveDbInfo = dbErrorBody.contains("postgresql") ||
                                     dbErrorBody.contains("connection") ||
                                     dbErrorBody.contains("database") ||
                                     dbErrorBody.contains("sql")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Database Error Information Disclosure",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = !containsSensitiveDbInfo,
                expectedResult = "Generic error message without database details",
                actualResult = "Response contains DB info: $containsSensitiveDbInfo",
                severity = SecuritySeverity.HIGH,
                details = "Database errors should not expose connection strings or SQL details"
            )
        )

        // Test 2: Stack traces should not be exposed in production
        val stackTraceResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("invalid_field" to "trigger_error"))
            .`when`()
            .post("$authUrl/api/auth/register")

        val stackTraceBody = stackTraceResponse.body.asString()
        val containsStackTrace = stackTraceBody.contains("at com.gasolinerajsm") ||
                                stackTraceBody.contains("Exception in thread") ||
                                stackTraceBody.contains("Caused by:")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Stack Trace Information Disclosure",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = !containsStackTrace,
                expectedResult = "No stack traces in error responses",
                actualResult = "Contains stack trace: $containsStackTrace",
                severity = SecuritySeverity.MEDIUM,
                details = "Stack traces should not be exposed to prevent information leakage"
            )
        )

        // Test 3: Internal server paths should not be exposed
        val pathDisclosureResponse = given()
            .contentType(ContentType.JSON)
            .body("invalid json")
            .`when`()
            .post("$gatewayUrl/api/coupons/validate")

        val pathBody = pathDisclosureResponse.body.asString()
        val containsInternalPaths = pathBody.contains("/app/") ||
                                   pathBody.contains("/home/") ||
                                   pathBody.contains("C:\\") ||
                                   pathBody.contains("/usr/")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Internal Path Information Disclosure",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = !containsInternalPaths,
                expectedResult = "No internal file paths in error responses",
                actualResult = "Contains internal paths: $containsInternalPaths",
                severity = SecuritySeverity.MEDIUM,
                details = "Internal server paths should not be exposed in error messages"
            )
        )

        // Test 4: Version information should not be exposed
        val versionResponse = given()
            .header("User-Agent", "SecurityTest/1.0")
            .`when`()
            .get("$gatewayUrl/api/nonexistent")

        val versionBody = versionResponse.body.asString() + versionResponse.headers.toString()
        val containsVersionInfo = versionBody.contains("Spring Boot") ||
                                 versionBody.contains("Tomcat") ||
                                 versionBody.contains("version") ||
                                 versionBody.contains("Server:")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Version Information Disclosure",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = !containsVersionInfo,
                expectedResult = "No version information in error responses",
                actualResult = "Contains version info: $containsVersionInfo",
                severity = SecuritySeverity.LOW,
                details = "Framework and server version information should not be exposed"
            )
        )
    }

    @Test
    @DisplayName("Graceful degradation under failure conditions test")
    fun `should handle service failures gracefully`() {
        val gatewayUrl = getServiceUrl("api-gateway")
        val token = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")

        println("Testing graceful degradation under failure conditions...")

        // Test 1: Database connection failure simulation
        // This would typically require test configuration to simulate DB failures
        val dbFailureResponse = given()
            .header("Authorization", "Bearer $token")
            .header("X-Simulate-DB-Failure", "true")
            .`when`()
            .get("$gatewayUrl/api/coupons")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Database Failure Graceful Degradation",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = dbFailureResponse.statusCode in listOf(503, 500) &&
                        dbFailureResponse.body.asString().contains("temporarily unavailable"),
                expectedResult = "503 Service Unavailable with user-friendly message",
                actualResult = "Status: ${dbFailureResponse.statusCode}",
                severity = SecuritySeverity.MEDIUM,
                details = "Database failures should result in graceful error responses"
            )
        )

        // Test 2: Circuit breaker behavior
        // Simulate multiple failed requests to trigger circuit breaker
        val circuitBreakerResponses = (1..10).map {
            given()
                .header("Authorization", "Bearer $token")
                .header("X-Simulate-Service-Failure", "true")
                .`when`()
                .get("$gatewayUrl/api/redemptions")
        }

        val circuitBreakerTriggered = circuitBreakerResponses.takeLast(3).all {
            it.statusCode == 503 && it.body.asString().contains("circuit")
        }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Circuit Breaker Activation",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = circuitBreakerTriggered,
                expectedResult = "Circuit breaker should activate after repeated failures",
                actualResult = "Circuit breaker triggered: $circuitBreakerTriggered",
                severity = SecuritySeverity.MEDIUM,
                details = "Circuit breaker should prevent cascading failures"
            )
        )

        // Test 3: Rate limiting graceful handling
        val rateLimitResponses = (1..20).map {
            given()
                .header("Authorization", "Bearer $token")
                .`when`()
                .get("$gatewayUrl/api/stations")
        }

        val rateLimitingActive = rateLimitResponses.any { it.statusCode == 429 }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Rate Limiting Graceful Handling",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = rateLimitingActive,
                expectedResult = "Rate limiting should activate with 429 status",
                actualResult = "Rate limiting active: $rateLimitingActive",
                severity = SecuritySeverity.LOW,
                details = "Rate limiting should provide clear feedback to clients"
            )
        )

        // Test 4: Timeout handling
        val timeoutResponse = given()
            .header("Authorization", "Bearer $token")
            .header("X-Simulate-Timeout", "true")
            .`when`()
            .get("$gatewayUrl/api/raffles")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Timeout Graceful Handling",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = timeoutResponse.statusCode in listOf(408, 504),
                expectedResult = "408 Request Timeout or 504 Gateway Timeout",
                actualResult = "Status: ${timeoutResponse.statusCode}",
                severity = SecuritySeverity.LOW,
                details = "Timeouts should be handled with appropriate HTTP status codes"
            )
        )
    }

    @Test
    @DisplayName("Error response consistency test")
    fun `should provide consistent error response format`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Testing error response consistency...")

        // Test different types of errors and ensure consistent format
        val errorScenarios = listOf(
            Triple("$authUrl/api/auth/login", mapOf("phone_number" to "invalid"), "Validation Error"),
            Triple("$gatewayUrl/api/coupons/nonexistent", null, "Not Found Error"),
            Triple("$authUrl/api/auth/register", mapOf("phone_number" to ""), "Empty Field Error"),
            Triple("$gatewayUrl/api/stations", null, "Unauthorized Error") // No token
        )

        val errorFormats = mutableListOf<Map<String, Any?>>()

        errorScenarios.forEach { (endpoint, body, description) ->
            val response = if (body != null) {
                given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .`when`()
                    .post(endpoint)
            } else {
                given()
                    .`when`()
                    .get(endpoint)
            }

            if (response.statusCode >= 400) {
                try {
                    val errorBody = response.jsonPath().getMap<String, Any?>("")
                    errorFormats.add(errorBody)

                    // Check for required error fields
                    val hasRequiredFields = errorBody.containsKey("message") ||
                                          errorBody.containsKey("error") ||
                                          errorBody.containsKey("code")

                    recordSecurityTestResult(
                        SecurityTestResult(
                            testName = "Error Format Consistency: $description",
                            testType = SecurityTestType.ERROR_HANDLING,
                            passed = hasRequiredFields,
                            expectedResult = "Consistent error format with required fields",
                            actualResult = "Has required fields: $hasRequiredFields",
                            severity = SecuritySeverity.LOW,
                            details = "Error responses should follow consistent format"
                        )
                    )
                } catch (e: Exception) {
                    recordSecurityTestResult(
                        SecurityTestResult(
                            testName = "Error Format Parsing: $description",
                            testType = SecurityTestType.ERROR_HANDLING,
                            passed = false,
                            expectedResult = "Valid JSON error response",
                            actualResult = "Invalid JSON or parsing error",
                            severity = SecuritySeverity.MEDIUM,
                            details = "Error responses should be valid JSON"
                        )
                    )
                }
            }
        }

        // Check if all error formats are similar
        val formatConsistency = errorFormats.isNotEmpty() &&
                               errorFormats.all { it.keys.intersect(errorFormats.first().keys).isNotEmpty() }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Overall Error Format Consistency",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = formatConsistency,
                expectedResult = "All error responses follow similar format",
                actualResult = "Format consistency: $formatConsistency",
                severity = SecuritySeverity.LOW,
                details = "All services should use consistent error response format"
            )
        )
    }

    @Test
    @DisplayName("Security error handling test")
    fun `should handle security errors appropriately`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Testing security error handling...")

        // Test 1: Authentication failure should not reveal user existence
        val nonExistentUserResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555999999", "otp_code" to "123456"))
            .`when`()
            .post("$authUrl/api/auth/login")

        val existentUserResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555001001", "otp_code" to "wrong_otp"))
            .`when`()
            .post("$authUrl/api/auth/login")

        val sameErrorMessage = nonExistentUserResponse.body.asString() == existentUserResponse.body.asString()

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "User Enumeration Prevention",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = sameErrorMessage && nonExistentUserResponse.statusCode == existentUserResponse.statusCode,
                expectedResult = "Same error message for non-existent and wrong credentials",
                actualResult = "Same message: $sameErrorMessage",
                severity = SecuritySeverity.HIGH,
                details = "Authentication errors should not reveal user existence"
            )
        )

        // Test 2: Authorization errors should be generic
        val customerToken = generateValidJwtToken("customer", "CUSTOMER", "+525555001001")
        val adminEndpointResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$authUrl/api/auth/admin/users")

        val authErrorBody = adminEndpointResponse.body.asString().lowercase()
        val revealsSpecificPermission = authErrorBody.contains("admin") ||
                                       authErrorBody.contains("permission") ||
                                       authErrorBody.contains("role")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Authorization Error Information Disclosure",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = !revealsSpecificPermission,
                expectedResult = "Generic authorization error message",
                actualResult = "Reveals specific permission: $revealsSpecificPermission",
                severity = SecuritySeverity.MEDIUM,
                details = "Authorization errors should not reveal specific permission requirements"
            )
        )

        // Test 3: Input validation errors should be safe
        val maliciousInput = "<script>alert('xss')</script>"
        val validationResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to maliciousInput))
            .`when`()
            .post("$authUrl/api/auth/register")

        val validationBody = validationResponse.body.asString()
        val echoesMaliciousInput = validationBody.contains("<script>") ||
                                  validationBody.contains("alert")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Validation Error Input Echoing",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = !echoesMaliciousInput,
                expectedResult = "Validation errors should not echo malicious input",
                actualResult = "Echoes malicious input: $echoesMaliciousInput",
                severity = SecuritySeverity.HIGH,
                details = "Validation error messages should sanitize user input"
            )
        )

        // Test 4: Rate limiting errors should not reveal system details
        val rateLimitToken = generateValidJwtToken("rate-limit-user", "CUSTOMER", "+525555001002")
        val rateLimitResponses = (1..15).map {
            given()
                .header("Authorization", "Bearer $rateLimitToken")
                .`when`()
                .get("$gatewayUrl/api/coupons")
        }

        val rateLimitResponse = rateLimitResponses.find { it.statusCode == 429 }
        if (rateLimitResponse != null) {
            val rateLimitBody = rateLimitResponse.body.asString().lowercase()
            val revealsSystemDetails = rateLimitBody.contains("redis") ||
                                      rateLimitBody.contains("cache") ||
                                      rateLimitBody.contains("bucket")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Rate Limit Error Information Disclosure",
                    testType = SecurityTestType.ERROR_HANDLING,
                    passed = !revealsSystemDetails,
                    expectedResult = "Generic rate limit error message",
                    actualResult = "Reveals system details: $revealsSystemDetails",
                    severity = SecuritySeverity.LOW,
                    details = "Rate limit errors should not reveal implementation details"
                )
            )
        }
    }

    @Test
    @DisplayName("Error logging security test")
    fun `should log errors securely without exposing sensitive data`() {
        val authUrl = getServiceUrl("auth-service")

        println("Testing secure error logging...")

        // Test 1: Sensitive data should not appear in error responses
        val sensitiveData = mapOf(
            "phone_number" to "+525555001001",
            "password" to "sensitive_password_123",
            "credit_card" to "4111111111111111",
            "ssn" to "123-45-6789"
        )

        val sensitiveResponse = given()
            .contentType(ContentType.JSON)
            .body(sensitiveData)
            .`when`()
            .post("$authUrl/api/auth/register")

        val responseBody = sensitiveResponse.body.asString()
        val containsSensitiveData = responseBody.contains("sensitive_password_123") ||
                                   responseBody.contains("4111111111111111") ||
                                   responseBody.contains("123-45-6789")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Sensitive Data in Error Response",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = !containsSensitiveData,
                expectedResult = "No sensitive data in error responses",
                actualResult = "Contains sensitive data: $containsSensitiveData",
                severity = SecuritySeverity.CRITICAL,
                details = "Error responses should never contain sensitive user data"
            )
        )

        // Test 2: Error correlation IDs should be present for tracking
        val correlationResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("invalid" to "data"))
            .`when`()
            .post("$authUrl/api/auth/login")

        val hasCorrelationId = correlationResponse.headers.hasHeaderWithName("X-Correlation-ID") ||
                              correlationResponse.headers.hasHeaderWithName("X-Request-ID") ||
                              correlationResponse.body.asString().contains("correlationId") ||
                              correlationResponse.body.asString().contains("requestId")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Error Correlation ID Presence",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = hasCorrelationId,
                expectedResult = "Error responses should include correlation ID for tracking",
                actualResult = "Has correlation ID: $hasCorrelationId",
                severity = SecuritySeverity.LOW,
                details = "Correlation IDs help with secure error tracking and debugging"
            )
        )

        // Test 3: Error responses should have appropriate security headers
        val securityHeadersResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("trigger" to "error"))
            .`when`()
            .post("$authUrl/api/auth/nonexistent")

        val hasSecurityHeaders = securityHeadersResponse.headers.hasHeaderWithName("X-Content-Type-Options") &&
                                securityHeadersResponse.headers.hasHeaderWithName("X-Frame-Options")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Security Headers in Error Responses",
                testType = SecurityTestType.ERROR_HANDLING,
                passed = hasSecurityHeaders,
                expectedResult = "Error responses should include security headers",
                actualResult = "Has security headers: $hasSecurityHeaders",
                severity = SecuritySeverity.LOW,
                details = "Security headers should be present even in error responses"
            )
        )
    }
}