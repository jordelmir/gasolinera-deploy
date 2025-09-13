package com.gasolinerajsm.security.base

import com.fasterxml.jackson.databind.ObjectMapper
import com.gasolinerajsm.security.config.SecurityTestConfiguration
import com.gasolinerajsm.security.model.*
import com.gasolinerajsm.security.util.SecurityTestUtils
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Base class for security tests with common utilities and setup
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.profiles.active=security-test"
    ]
)
@ActiveProfiles("security-test")
@Testcontainers
@Import(SecurityTestConfiguration::class)
abstract class BaseSecurityTest {

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var securityTestUtils: SecurityTestUtils

    protected val testResults = mutableListOf<SecurityTestResult>()

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("gasolinera_security_db")
            withUsername("security_user")
            withPassword("security_password")
            withReuse(true)
        }

        @Container
        @JvmStatic
        val redisContainer = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
            withCommand("redis-server", "--requirepass", "security_password")
            withReuse(true)
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.redis.host", redisContainer::getHost)
            registry.add("spring.redis.port", redisContainer::getFirstMappedPort)
        }
    }

    @BeforeEach
    fun setUpSecurityTest() {
        testResults.clear()
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @AfterEach
    fun tearDownSecurityTest() {
        generateSecurityTestReport()
    }

    /**
     * Get service URLs for testing
     */
    protected fun getServiceUrl(serviceName: String): String {
        return when (serviceName) {
            "auth-service" -> "http://localhost:8081"
            "coupon-service" -> "http://localhost:8083"
            "redemption-service" -> "http://localhost:8084"
            "api-gateway" -> "http://localhost:8080"
            else -> throw IllegalArgumentException("Unknown service: $serviceName")
        }
    }

    /**
     * Generate a valid JWT token for testing
     */
    protected fun generateValidJwtToken(
        userId: String,
        role: String,
        phoneNumber: String,
        expirationMinutes: Long = 60
    ): String {
        val secret = "security_test_jwt_secret_key_for_testing_only_do_not_use_in_production"
        val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

        val now = Instant.now()
        val expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES)

        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .claim("phone_number", phoneNumber)
            .claim("user_id", userId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(key)
            .compact()
    }

    /**
     * Generate an expired JWT token for testing
     */
    protected fun generateExpiredJwtToken(
        userId: String,
        role: String,
        phoneNumber: String
    ): String {
        val secret = "security_test_jwt_secret_key_for_testing_only_do_not_use_in_production"
        val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

        val now = Instant.now()
        val expiration = now.minus(1, ChronoUnit.HOURS) // Expired 1 hour ago

        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .claim("phone_number", phoneNumber)
            .claim("user_id", userId)
            .issuedAt(Date.from(now.minus(2, ChronoUnit.HOURS)))
            .expiration(Date.from(expiration))
            .signWith(key)
            .compact()
    }

    /**
     * Generate a malformed JWT token for testing
     */
    protected fun generateMalformedJwtToken(): String {
        return "malformed.jwt.token.for.testing"
    }

    /**
     * Generate a JWT token with invalid signature
     */
    protected fun generateInvalidSignatureJwtToken(
        userId: String,
        role: String,
        phoneNumber: String
    ): String {
        val invalidSecret = "invalid_secret_for_negative_testing"
        val key: SecretKey = Keys.hmacShaKeyFor(invalidSecret.toByteArray())

        val now = Instant.now()
        val expiration = now.plus(1, ChronoUnit.HOURS)

        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .claim("phone_number", phoneNumber)
            .claim("user_id", userId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(key)
            .compact()
    }

    /**
     * Authenticate a test user and return JWT token
     */
    protected fun authenticateTestUser(phoneNumber: String, role: String = "CUSTOMER"): String {
        val authUrl = getServiceUrl("auth-service")

        // Request OTP
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to phoneNumber, "purpose" to "LOGIN"))
            .`when`()
            .post("$authUrl/api/auth/otp/request")

        // Login with OTP
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to phoneNumber, "otp_code" to "123456"))
            .`when`()
            .post("$authUrl/api/auth/login")

        if (response.statusCode == 200) {
            return response.jsonPath().getString("access_token")
        } else {
            // Fallback to generated token for testing
            return generateValidJwtToken("test-user", role, phoneNumber)
        }
    }

    /**
     * Test authentication with various token scenarios
     */
    protected fun testAuthenticationScenarios(
        endpoint: String,
        method: String = "GET",
        body: Any? = null
    ): List<SecurityTestResult> {
        val results = mutableListOf<SecurityTestResult>()

        // Test 1: Valid token
        val validToken = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")
        val validResponse = makeRequest(endpoint, method, body, validToken)
        results.add(
            SecurityTestResult(
                testName = "Valid JWT Token",
                testType = SecurityTestType.AUTHENTICATION,
                passed = validResponse.statusCode in 200..299,
                expectedResult = "Access granted with valid token",
                actualResult = "Status: ${validResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Valid JWT token should allow access to protected resources"
            )
        )

        // Test 2: Expired token
        val expiredToken = generateExpiredJwtToken("test-user", "CUSTOMER", "+525555001001")
        val expiredResponse = makeRequest(endpoint, method, body, expiredToken)
        results.add(
            SecurityTestResult(
                testName = "Expired JWT Token",
                testType = SecurityTestType.AUTHENTICATION,
                passed = expiredResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${expiredResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Expired JWT tokens should be rejected"
            )
        )

        // Test 3: Malformed token
        val malformedToken = generateMalformedJwtToken()
        val malformedResponse = makeRequest(endpoint, method, body, malformedToken)
        results.add(
            SecurityTestResult(
                testName = "Malformed JWT Token",
                testType = SecurityTestType.AUTHENTICATION,
                passed = malformedResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${malformedResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Malformed JWT tokens should be rejected"
            )
        )

        // Test 4: Invalid signature
        val invalidSignatureToken = generateInvalidSignatureJwtToken("test-user", "CUSTOMER", "+525555001001")
        val invalidSignatureResponse = makeRequest(endpoint, method, body, invalidSignatureToken)
        results.add(
            SecurityTestResult(
                testName = "Invalid Signature JWT Token",
                testType = SecurityTestType.AUTHENTICATION,
                passed = invalidSignatureResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${invalidSignatureResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "JWT tokens with invalid signatures should be rejected"
            )
        )

        // Test 5: Missing token
        val missingTokenResponse = makeRequest(endpoint, method, body, null)
        results.add(
            SecurityTestResult(
                testName = "Missing JWT Token",
                testType = SecurityTestType.AUTHENTICATION,
                passed = missingTokenResponse.statusCode == 401,
                expectedResult = "401 Unauthorized",
                actualResult = "Status: ${missingTokenResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Requests without JWT tokens should be rejected for protected endpoints"
            )
        )

        return results
    }

    /**
     * Test role-based access control
     */
    protected fun testRoleBasedAccess(
        endpoint: String,
        requiredRole: String,
        method: String = "GET",
        body: Any? = null
    ): List<SecurityTestResult> {
        val results = mutableListOf<SecurityTestResult>()
        val roles = listOf("CUSTOMER", "EMPLOYEE", "STATION_ADMIN", "SYSTEM_ADMIN")

        roles.forEach { role ->
            val token = generateValidJwtToken("test-user-$role", role, "+525555001001")
            val response = makeRequest(endpoint, method, body, token)

            val shouldHaveAccess = hasRoleAccess(role, requiredRole)
            val hasAccess = response.statusCode in 200..299

            results.add(
                SecurityTestResult(
                    testName = "Role-based Access: $role",
                    testType = SecurityTestType.AUTHORIZATION,
                    passed = shouldHaveAccess == hasAccess,
                    expectedResult = if (shouldHaveAccess) "Access granted" else "403 Forbidden",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.HIGH,
                    details = "Role $role ${if (shouldHaveAccess) "should have" else "should not have"} access to $endpoint"
                )
            )
        }

        return results
    }

    /**
     * Test input validation against common attacks
     */
    protected fun testInputValidation(
        endpoint: String,
        parameterName: String,
        method: String = "POST"
    ): List<SecurityTestResult> {
        val results = mutableListOf<SecurityTestResult>()
        val token = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")

        // SQL Injection payloads
        val sqlInjectionPayloads = listOf(
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "' UNION SELECT * FROM users --",
            "1' OR '1'='1' --",
            "admin'--",
            "' OR 1=1#"
        )

        sqlInjectionPayloads.forEach { payload ->
            val body = mapOf(parameterName to payload)
            val response = makeRequest(endpoint, method, body, token)

            results.add(
                SecurityTestResult(
                    testName = "SQL Injection: $payload",
                    testType = SecurityTestType.INPUT_VALIDATION,
                    passed = response.statusCode in listOf(400, 422) || !response.body.asString().contains("SQL"),
                    expectedResult = "400 Bad Request or sanitized input",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.CRITICAL,
                    details = "SQL injection attempts should be blocked or sanitized"
                )
            )
        }

        // XSS payloads
        val xssPayloads = listOf(
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "javascript:alert('XSS')",
            "<svg onload=alert('XSS')>",
            "'\"><script>alert('XSS')</script>"
        )

        xssPayloads.forEach { payload ->
            val body = mapOf(parameterName to payload)
            val response = makeRequest(endpoint, method, body, token)

            results.add(
                SecurityTestResult(
                    testName = "XSS: $payload",
                    testType = SecurityTestType.INPUT_VALIDATION,
                    passed = response.statusCode in listOf(400, 422) || !response.body.asString().contains("<script>"),
                    expectedResult = "400 Bad Request or sanitized input",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.HIGH,
                    details = "XSS attempts should be blocked or sanitized"
                )
            )
        }

        return results
    }

    /**
     * Test security headers
     */
    protected fun testSecurityHeaders(endpoint: String): List<SecurityTestResult> {
        val results = mutableListOf<SecurityTestResult>()
        val response = makeRequest(endpoint, "GET", null, null)

        val requiredHeaders = mapOf(
            "X-Content-Type-Options" to "nosniff",
            "X-Frame-Options" to "DENY",
            "X-XSS-Protection" to "1; mode=block"
        )

        requiredHeaders.forEach { (headerName, expectedValue) ->
            val actualValue = response.header(headerName)
            results.add(
                SecurityTestResult(
                    testName = "Security Header: $headerName",
                    testType = SecurityTestType.SECURITY_HEADERS,
                    passed = actualValue != null && actualValue.contains(expectedValue),
                    expectedResult = "$headerName: $expectedValue",
                    actualResult = "$headerName: ${actualValue ?: "missing"}",
                    severity = SecuritySeverity.MEDIUM,
                    details = "Security headers should be present to prevent common attacks"
                )
            )
        }

        return results
    }

    /**
     * Make HTTP request with optional authentication
     */
    private fun makeRequest(
        endpoint: String,
        method: String,
        body: Any?,
        token: String?
    ): Response {
        val request = RestAssured.given()
            .contentType(ContentType.JSON)

        if (token != null) {
            request.header("Authorization", "Bearer $token")
        }

        if (body != null) {
            request.body(body)
        }

        return when (method.uppercase()) {
            "GET" -> request.`when`().get(endpoint)
            "POST" -> request.`when`().post(endpoint)
            "PUT" -> request.`when`().put(endpoint)
            "DELETE" -> request.`when`().delete(endpoint)
            "PATCH" -> request.`when`().patch(endpoint)
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }
    }

    /**
     * Check if a role has access to a required role
     */
    private fun hasRoleAccess(userRole: String, requiredRole: String): Boolean {
        val roleHierarchy = mapOf(
            "CUSTOMER" to 1,
            "EMPLOYEE" to 2,
            "STATION_ADMIN" to 3,
            "SYSTEM_ADMIN" to 4
        )

        val userLevel = roleHierarchy[userRole] ?: 0
        val requiredLevel = roleHierarchy[requiredRole] ?: 0

        return userLevel >= requiredLevel
    }

    /**
     * Record a security test result
     */
    protected fun recordSecurityTestResult(result: SecurityTestResult) {
        testResults.add(result)
    }

    /**
     * Generate security test report
     */
    private fun generateSecurityTestReport() {
        if (testResults.isEmpty()) return

        val passedTests = testResults.count { it.passed }
        val totalTests = testResults.size
        val passRate = (passedTests.toDouble() / totalTests) * 100

        println("\n=== Security Test Report ===")
        println("Total Tests: $totalTests")
        println("Passed: $passedTests")
        println("Failed: ${totalTests - passedTests}")
        println("Pass Rate: ${String.format("%.2f", passRate)}%")

        // Group by severity
        val criticalIssues = testResults.filter { !it.passed && it.severity == SecuritySeverity.CRITICAL }
        val highIssues = testResults.filter { !it.passed && it.severity == SecuritySeverity.HIGH }
        val mediumIssues = testResults.filter { !it.passed && it.severity == SecuritySeverity.MEDIUM }

        if (criticalIssues.isNotEmpty()) {
            println("\nCRITICAL Issues (${criticalIssues.size}):")
            criticalIssues.forEach { println("  - ${it.testName}: ${it.actualResult}") }
        }

        if (highIssues.isNotEmpty()) {
            println("\nHIGH Issues (${highIssues.size}):")
            highIssues.forEach { println("  - ${it.testName}: ${it.actualResult}") }
        }

        if (mediumIssues.isNotEmpty()) {
            println("\nMEDIUM Issues (${mediumIssues.size}):")
            mediumIssues.forEach { println("  - ${it.testName}: ${it.actualResult}") }
        }

        // Validate overall security posture
        assertTrue(
            criticalIssues.isEmpty(),
            "Critical security issues found: ${criticalIssues.map { it.testName }}"
        )

        assertTrue(
            passRate >= 80.0,
            "Security test pass rate $passRate% is below 80%"
        )
    }
}