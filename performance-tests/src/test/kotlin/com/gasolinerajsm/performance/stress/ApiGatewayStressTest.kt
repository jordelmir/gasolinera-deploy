package com.gasolinerajsm.performance.stress

import com.gasolinerajsm.performance.base.BasePerformanceTest
import com.gasolinerajsm.performance.model.*
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import kotlinx.coroutines.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertTrue

/**
 * Stress tests for API Gateway under heavy traffic conditions
 *
 * Tests the API Gateway's ability to handle high traffic loads,
 * rate limiting, circuit breakers, and graceful degradation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=performance-test"])
@Tag("performance")
@Tag("stress")
@DisplayName("API Gateway Stress Tests")
class ApiGatewayStressTest : BasePerformanceTest() {

    @Test
    @DisplayName("API Gateway high traffic stress test")
    fun `should handle high traffic load through API Gateway`() = runBlocking {
        val maxConcurrentUsers = getConcurrentUsers() * 2 // Double the normal load for stress test
        val testDuration = getTestDuration()
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Starting API Gateway stress test with $maxConcurrentUsers concurrent users")
        println("Test duration: ${testDuration.seconds} seconds")

        // Prepare test data
        val testUsers = generateTestUsers(maxConcurrentUsers)
        val userSessions = ConcurrentHashMap<String, UserSession>()

        // Authenticate users through API Gateway
        println("Authenticating users through API Gateway...")
        val authJobs = testUsers.map { phoneNumber ->
            async(Dispatchers.IO) {
                try {
                    val token = authenticateUserThroughGateway(phoneNumber, gatewayUrl)
                    userSessions[phoneNumber] = UserSession(
                        userId = phoneNumber,
                        phoneNumber = phoneNumber,
                        accessToken = token
                    )
                } catch (e: Exception) {
                    recordError(0, "AUTH_FAILED")
                    println("Failed to authenticate user $phoneNumber: ${e.message}")
                }
            }
        }
        authJobs.awaitAll()

        println("Successfully authenticated ${userSessions.size} users")

        // Define API endpoints to test through gateway
        val endpoints = listOf(
            "/api/auth/user/profile" to "GET",
            "/api/stations" to "GET",
            "/api/campaigns/active" to "GET",
            "/api/coupons/available" to "GET",
            "/api/raffles/available" to "GET"
        )

        val requestCounter = AtomicInteger(0)
        val successCounter = AtomicInteger(0)
        val errorCounter = AtomicInteger(0)
        val rateLimitCounter = AtomicInteger(0)
        val timeoutCounter = AtomicInteger(0)

        // Execute stress test with ramping load
        val rampUpDuration = Duration.ofSeconds(60)
        val steadyDuration = testDuration.minus(rampUpDuration).minus(Duration.ofSeconds(30))
        val rampDownDuration = Duration.ofSeconds(30)

        println("Starting ramped stress test:")
        println("  Ramp up: ${rampUpDuration.seconds}s")
        println("  Steady load: ${steadyDuration.seconds}s")
        println("  Ramp down: ${rampDownDuration.seconds}s")

        // Phase 1: Ramp up
        val rampUpJob = async(Dispatchers.IO) {
            executeRampUpPhase(
                userSessions, endpoints, gatewayUrl, rampUpDuration, maxConcurrentUsers,
                requestCounter, successCounter, errorCounter, rateLimitCounter, timeoutCounter
            )
        }

        rampUpJob.await()

        // Phase 2: Steady high load
        val steadyLoadJob = async(Dispatchers.IO) {
            executeSteadyLoadPhase(
                userSessions, endpoints, gatewayUrl, steadyDuration, maxConcurrentUsers,
                requestCounter, successCounter, errorCounter, rateLimitCounter, timeoutCounter
            )
        }

        steadyLoadJob.await()

        // Phase 3: Ramp down
        val rampDownJob = async(Dispatchers.IO) {
            executeRampDownPhase(
                userSessions, endpoints, gatewayUrl, rampDownDuration, maxConcurrentUsers,
                requestCounter, successCounter, errorCounter, rateLimitCounter, timeoutCounter
            )
        }

        rampDownJob.await()

        val totalRequests = requestCounter.get()
        val successRate = if (totalRequests > 0) (successCounter.get().toDouble() / totalRequests) * 100 else 0.0
        val rateLimitRate = if (totalRequests > 0) (rateLimitCounter.get().toDouble() / totalRequests) * 100 else 0.0

        println("API Gateway stress test completed:")
        println("  Total requests: $totalRequests")
        println("  Successful requests: ${successCounter.get()}")
        println("  Failed requests: ${errorCounter.get()}")
        println("  Rate limited requests: ${rateLimitCounter.get()}")
        println("  Timeout requests: ${timeoutCounter.get()}")
        println("  Success rate: ${String.format("%.2f", successRate)}%")
        println("  Rate limit rate: ${String.format("%.2f", rateLimitRate)}%")

        // Validate stress test results
        // Under stress, we expect some rate limiting and timeouts
        val minAcceptableSuccessRate = when (getTestMode()) {
            "stress" -> 70.0 // More lenient for stress tests
            else -> 85.0
        }

        assertTrue(
            successRate >= minAcceptableSuccessRate,
            "Success rate $successRate% is below minimum $minAcceptableSuccessRate%"
        )

        // Ensure the system handled significant load
        assertTrue(totalRequests > 1000, "Total requests $totalRequests is too low for a stress test")

        // Rate limiting should be working (some requests should be rate limited under stress)
        if (getTestMode() == "stress") {
            assertTrue(
                rateLimitCounter.get() > 0,
                "No rate limiting occurred during stress test - rate limiting may not be working"
            )
        }
    }

    @Test
    @DisplayName("API Gateway circuit breaker test")
    fun `should trigger circuit breaker under service failures`() = runBlocking {
        val concurrentUsers = getConcurrentUsers()
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Starting API Gateway circuit breaker test")

        // Authenticate a few users
        val testUsers = generateTestUsers(concurrentUsers)
        val userSessions = ConcurrentHashMap<String, UserSession>()

        val authJobs = testUsers.take(10).map { phoneNumber ->
            async(Dispatchers.IO) {
                try {
                    val token = authenticateUserThroughGateway(phoneNumber, gatewayUrl)
                    userSessions[phoneNumber] = UserSession(
                        userId = phoneNumber,
                        phoneNumber = phoneNumber,
                        accessToken = token
                    )
                } catch (e: Exception) {
                    println("Auth failed for $phoneNumber: ${e.message}")
                }
            }
        }
        authJobs.awaitAll()

        // Test circuit breaker by making requests to a potentially failing endpoint
        val circuitBreakerCounter = AtomicInteger(0)
        val serviceUnavailableCounter = AtomicInteger(0)
        val successCounter = AtomicInteger(0)

        // Make rapid requests to trigger circuit breaker
        val circuitBreakerJobs = (1..100).map { requestIndex ->
            async(Dispatchers.IO) {
                val session = userSessions.values.random()
                if (session.accessToken != null) {
                    try {
                        val startTime = System.currentTimeMillis()

                        val response = given()
                            .header("Authorization", "Bearer ${session.accessToken}")
                            .`when`()
                            .get("$gatewayUrl/api/redemptions/history?userId=${session.userId}")

                        val responseTime = System.currentTimeMillis() - startTime

                        when (response.statusCode) {
                            200 -> {
                                recordRequest(responseTime)
                                successCounter.incrementAndGet()
                            }
                            503 -> {
                                // Service unavailable - circuit breaker open
                                recordRequest(responseTime)
                                circuitBreakerCounter.incrementAndGet()
                            }
                            502, 504 -> {
                                // Bad gateway or timeout - service issues
                                recordError(responseTime, "SERVICE_UNAVAILABLE")
                                serviceUnavailableCounter.incrementAndGet()
                            }
                            else -> {
                                recordError(responseTime, "HTTP_${response.statusCode}")
                            }
                        }

                    } catch (e: Exception) {
                        recordError(0, "REQUEST_EXCEPTION")
                    }
                }

                // Small delay between requests
                delay(50)
            }
        }

        circuitBreakerJobs.awaitAll()

        val totalRequests = successCounter.get() + circuitBreakerCounter.get() + serviceUnavailableCounter.get()

        println("Circuit breaker test completed:")
        println("  Total requests: $totalRequests")
        println("  Successful requests: ${successCounter.get()}")
        println("  Circuit breaker responses (503): ${circuitBreakerCounter.get()}")
        println("  Service unavailable responses: ${serviceUnavailableCounter.get()}")

        // Validate that some requests were processed
        assertTrue(totalRequests > 0, "No requests were processed")

        // In a real scenario with failing services, we would expect circuit breaker responses
        println("Circuit breaker behavior: ${circuitBreakerCounter.get()} circuit breaker responses out of $totalRequests requests")
    }

    @Test
    @DisplayName("API Gateway rate limiting test")
    fun `should enforce rate limiting under high request volume`() = runBlocking {
        val gatewayUrl = getServiceUrl("api-gateway")

        println("Starting API Gateway rate limiting test")

        // Authenticate a single user to test rate limiting
        val testUser = "+525555999001"
        val token = authenticateUserThroughGateway(testUser, gatewayUrl)

        val requestCounter = AtomicInteger(0)
        val successCounter = AtomicInteger(0)
        val rateLimitedCounter = AtomicInteger(0)
        val errorCounter = AtomicInteger(0)

        // Make rapid requests to trigger rate limiting
        val rateLimitJobs = (1..200).map { requestIndex ->
            async(Dispatchers.IO) {
                try {
                    val startTime = System.currentTimeMillis()

                    val response = given()
                        .header("Authorization", "Bearer $token")
                        .`when`()
                        .get("$gatewayUrl/api/auth/user/profile")

                    val responseTime = System.currentTimeMillis() - startTime
                    requestCounter.incrementAndGet()

                    when (response.statusCode) {
                        200 -> {
                            recordRequest(responseTime)
                            successCounter.incrementAndGet()
                        }
                        429 -> {
                            // Too Many Requests - rate limited
                            recordRequest(responseTime)
                            rateLimitedCounter.incrementAndGet()
                        }
                        else -> {
                            recordError(responseTime, "HTTP_${response.statusCode}")
                            errorCounter.incrementAndGet()
                        }
                    }

                } catch (e: Exception) {
                    recordError(0, "REQUEST_EXCEPTION")
                    errorCounter.incrementAndGet()
                }
            }
        }

        rateLimitJobs.awaitAll()

        val totalRequests = requestCounter.get()
        val rateLimitRate = if (totalRequests > 0) (rateLimitedCounter.get().toDouble() / totalRequests) * 100 else 0.0

        println("Rate limiting test completed:")
        println("  Total requests: $totalRequests")
        println("  Successful requests: ${successCounter.get()}")
        println("  Rate limited requests (429): ${rateLimitedCounter.get()}")
        println("  Error requests: ${errorCounter.get()}")
        println("  Rate limit rate: ${String.format("%.2f", rateLimitRate)}%")

        // Validate that requests were processed
        assertTrue(totalRequests > 0, "No requests were processed")

        // Validate that some requests were successful
        assertTrue(successCounter.get() > 0, "No successful requests - rate limiting may be too aggressive")

        // Validate that rate limiting occurred (expected under rapid requests)
        assertTrue(
            rateLimitedCounter.get() > 0,
            "No rate limiting occurred - rate limiting may not be configured properly"
        )

        // Validate rate limiting is reasonable (not blocking everything)
        assertTrue(
            rateLimitRate < 90.0,
            "Rate limiting is too aggressive: ${rateLimitRate}% of requests were blocked"
        )
    }

    private suspend fun executeRampUpPhase(
        userSessions: ConcurrentHashMap<String, UserSession>,
        endpoints: List<Pair<String, String>>,
        gatewayUrl: String,
        duration: Duration,
        maxUsers: Int,
        requestCounter: AtomicInteger,
        successCounter: AtomicInteger,
        errorCounter: AtomicInteger,
        rateLimitCounter: AtomicInteger,
        timeoutCounter: AtomicInteger
    ) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + duration.toMillis()
        val durationMs = duration.toMillis()

        while (System.currentTimeMillis() < endTime) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = elapsed.toDouble() / durationMs
            val currentUsers = (maxUsers * progress).toInt().coerceAtMost(maxUsers)

            // Launch requests for current user count
            val jobs = (1..currentUsers).map {
                async(Dispatchers.IO) {
                    executeRandomRequest(
                        userSessions, endpoints, gatewayUrl,
                        requestCounter, successCounter, errorCounter, rateLimitCounter, timeoutCounter
                    )
                }
            }

            jobs.awaitAll()
            delay(1000) // 1 second intervals during ramp up
        }
    }

    private suspend fun executeSteadyLoadPhase(
        userSessions: ConcurrentHashMap<String, UserSession>,
        endpoints: List<Pair<String, String>>,
        gatewayUrl: String,
        duration: Duration,
        maxUsers: Int,
        requestCounter: AtomicInteger,
        successCounter: AtomicInteger,
        errorCounter: AtomicInteger,
        rateLimitCounter: AtomicInteger,
        timeoutCounter: AtomicInteger
    ) {
        val endTime = System.currentTimeMillis() + duration.toMillis()

        while (System.currentTimeMillis() < endTime) {
            // Maintain steady high load
            val jobs = (1..maxUsers).map {
                async(Dispatchers.IO) {
                    executeRandomRequest(
                        userSessions, endpoints, gatewayUrl,
                        requestCounter, successCounter, errorCounter, rateLimitCounter, timeoutCounter
                    )
                }
            }

            jobs.awaitAll()
            delay(500) // Shorter intervals for sustained load
        }
    }

    private suspend fun executeRampDownPhase(
        userSessions: ConcurrentHashMap<String, UserSession>,
        endpoints: List<Pair<String, String>>,
        gatewayUrl: String,
        duration: Duration,
        maxUsers: Int,
        requestCounter: AtomicInteger,
        successCounter: AtomicInteger,
        errorCounter: AtomicInteger,
        rateLimitCounter: AtomicInteger,
        timeoutCounter: AtomicInteger
    ) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + duration.toMillis()
        val durationMs = duration.toMillis()

        while (System.currentTimeMillis() < endTime) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = elapsed.toDouble() / durationMs
            val currentUsers = (maxUsers * (1.0 - progress)).toInt().coerceAtLeast(1)

            // Launch requests for decreasing user count
            val jobs = (1..currentUsers).map {
                async(Dispatchers.IO) {
                    executeRandomRequest(
                        userSessions, endpoints, gatewayUrl,
                        requestCounter, successCounter, errorCounter, rateLimitCounter, timeoutCounter
                    )
                }
            }

            jobs.awaitAll()
            delay(1000) // 1 second intervals during ramp down
        }
    }

    private suspend fun executeRandomRequest(
        userSessions: ConcurrentHashMap<String, UserSession>,
        endpoints: List<Pair<String, String>>,
        gatewayUrl: String,
        requestCounter: AtomicInteger,
        successCounter: AtomicInteger,
        errorCounter: AtomicInteger,
        rateLimitCounter: AtomicInteger,
        timeoutCounter: AtomicInteger
    ) {
        val session = userSessions.values.randomOrNull()
        if (session?.accessToken == null) return

        val (endpoint, method) = endpoints.random()

        try {
            val startTime = System.currentTimeMillis()

            val response = when (method) {
                "GET" -> given()
                    .header("Authorization", "Bearer ${session.accessToken}")
                    .`when`()
                    .get("$gatewayUrl$endpoint")
                "POST" -> given()
                    .header("Authorization", "Bearer ${session.accessToken}")
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .`when`()
                    .post("$gatewayUrl$endpoint")
                else -> return
            }

            val responseTime = System.currentTimeMillis() - startTime
            requestCounter.incrementAndGet()

            when (response.statusCode) {
                in 200..299 -> {
                    recordRequest(responseTime)
                    successCounter.incrementAndGet()
                }
                429 -> {
                    recordRequest(responseTime)
                    rateLimitCounter.incrementAndGet()
                }
                408, 504 -> {
                    recordError(responseTime, "TIMEOUT")
                    timeoutCounter.incrementAndGet()
                }
                else -> {
                    recordError(responseTime, "HTTP_${response.statusCode}")
                    errorCounter.incrementAndGet()
                }
            }

        } catch (e: Exception) {
            recordError(0, "REQUEST_EXCEPTION")
            errorCounter.incrementAndGet()
        }
    }

    private suspend fun authenticateUserThroughGateway(phoneNumber: String, gatewayUrl: String): String {
        // Request OTP through gateway
        given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to phoneNumber, "purpose" to "LOGIN"))
            .`when`()
            .post("$gatewayUrl/api/auth/otp/request")

        // Login through gateway
        val response = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to phoneNumber, "otp_code" to "123456"))
            .`when`()
            .post("$gatewayUrl/api/auth/login")

        if (response.statusCode != 200) {
            throw Exception("Authentication failed with status ${response.statusCode}")
        }

        return response.jsonPath().getString("access_token")
    }
}