package com.gasolinerajsm.security.model

import java.time.Instant

/**
 * Data classes for security testing
 */

/**
 * Represents the result of a security test
 */
data class SecurityTestResult(
    val testName: String,
    val testType: SecurityTestType,
    val passed: Boolean,
    val expectedResult: String,
    val actualResult: String,
    val severity: SecuritySeverity,
    val details: String,
    val timestamp: Instant = Instant.now(),
    val vulnerabilityId: String? = null,
    val remediation: String? = null
)

/**
 * Types of security tests
 */
enum class SecurityTestType {
    AUTHENTICATION,
    AUTHORIZATION,
    INPUT_VALIDATION,
    SESSION_MANAGEMENT,
    SECURITY_HEADERS,
    RATE_LIMITING,
    ERROR_HANDLING,
    PENETRATION,
    VULNERABILITY_SCAN
}

/**
 * Security issue severity levels
 */
enum class SecuritySeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

/**
 * Test user for security testing
 */
data class SecurityTestUser(
    val userId: String,
    val phoneNumber: String,
    val role: String,
    val isActive: Boolean = true,
    val isVerified: Boolean = true,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

/**
 * Security test scenario configuration
 */
data class SecurityTestScenario(
    val name: String,
    val description: String,
    val testType: SecurityTestType,
    val severity: SecuritySeverity,
    val enabled: Boolean = true,
    val expectedOutcome: String,
    val testSteps: List<SecurityTestStep>
)

/**
 * Individual test step in a security scenario
 */
data class SecurityTestStep(
    val stepName: String,
    val action: String,
    val endpoint: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: Any? = null,
    val expectedStatusCode: Int,
    val expectedResponse: String? = null,
    val validationRules: List<String> = emptyList()
)

/**
 * Attack payload for penetration testing
 */
data class AttackPayload(
    val type: String,
    val payload: String,
    val description: String,
    val severity: SecuritySeverity,
    val expectedBlocked: Boolean = true
)

/**
 * Security configuration for testing
 */
data class SecurityTestConfig(
    val jwtSecret: String,
    val jwtExpiration: Long,
    val maxLoginAttempts: Int = 5,
    val lockoutDuration: Long = 300000, // 5 minutes
    val sessionTimeout: Long = 3600000, // 1 hour
    val enableRateLimiting: Boolean = true,
    val enableSecurityHeaders: Boolean = true,
    val enableInputValidation: Boolean = true
)

/**
 * Rate limiting test configuration
 */
data class RateLimitTestConfig(
    val endpoint: String,
    val method: String = "POST",
    val maxRequests: Int,
    val timeWindow: Long, // in milliseconds
    val expectedStatusCode: Int = 429
)

/**
 * Brute force attack configuration
 */
data class BruteForceConfig(
    val targetEndpoint: String,
    val targetParameter: String,
    val payloads: List<String>,
    val maxAttempts: Int = 100,
    val delayBetweenAttempts: Long = 100, // milliseconds
    val expectedLockout: Boolean = true
)

/**
 * Session management test data
 */
data class SessionTestData(
    val sessionId: String,
    val userId: String,
    val createdAt: Instant,
    val lastAccessedAt: Instant,
    val expiresAt: Instant,
    val isValid: Boolean = true,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * Error handling test case
 */
data class ErrorHandlingTestCase(
    val testName: String,
    val description: String,
    val triggerCondition: String,
    val expectedErrorCode: Int,
    val expectedErrorMessage: String? = null,
    val shouldExposeInternalDetails: Boolean = false,
    val severity: SecuritySeverity = SecuritySeverity.MEDIUM
)

/**
 * Security vulnerability report
 */
data class SecurityVulnerabilityReport(
    val vulnerabilityId: String,
    val title: String,
    val description: String,
    val severity: SecuritySeverity,
    val affectedEndpoints: List<String>,
    val reproductionSteps: List<String>,
    val impact: String,
    val remediation: String,
    val references: List<String> = emptyList(),
    val discoveredAt: Instant = Instant.now()
)

/**
 * Comprehensive security test report
 */
data class SecurityTestReport(
    val testSuiteName: String,
    val executionTime: Instant,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val passRate: Double,
    val testResults: List<SecurityTestResult>,
    val vulnerabilities: List<SecurityVulnerabilityReport>,
    val recommendations: List<String>,
    val summary: SecurityTestSummary
)

/**
 * Security test summary
 */
data class SecurityTestSummary(
    val criticalIssues: Int,
    val highIssues: Int,
    val mediumIssues: Int,
    val lowIssues: Int,
    val overallRisk: SecurityRiskLevel,
    val complianceStatus: String,
    val nextSteps: List<String>
)

/**
 * Overall security risk assessment
 */
enum class SecurityRiskLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    MINIMAL
}

/**
 * JWT token test data
 */
data class JwtTestToken(
    val token: String,
    val isValid: Boolean,
    val isExpired: Boolean,
    val hasValidSignature: Boolean,
    val claims: Map<String, Any>,
    val testPurpose: String
)

/**
 * Authentication test scenario
 */
data class AuthenticationTestScenario(
    val scenarioName: String,
    val username: String,
    val password: String,
    val expectedResult: AuthenticationResult,
    val additionalChecks: List<String> = emptyList()
)

/**
 * Authentication test result
 */
enum class AuthenticationResult {
    SUCCESS,
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,
    TOKEN_EXPIRED,
    TOKEN_INVALID,
    RATE_LIMITED
}

/**
 * Authorization test case
 */
data class AuthorizationTestCase(
    val testName: String,
    val userRole: String,
    val targetResource: String,
    val action: String,
    val expectedAccess: Boolean,
    val reasoning: String
)

/**
 * Input validation test case
 */
data class InputValidationTestCase(
    val testName: String,
    val inputField: String,
    val maliciousInput: String,
    val attackType: String,
    val expectedBehavior: String,
    val severity: SecuritySeverity
)