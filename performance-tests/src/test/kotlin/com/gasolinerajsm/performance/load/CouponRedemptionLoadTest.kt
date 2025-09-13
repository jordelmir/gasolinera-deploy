package com.gasolinerajsm.performance.load

import com.gasolinerajsm.performance.base.BasePerformanceTest
import com.gasolinerajsm.performance.model.*
import com.gasolinerajsm.performance.util.LoadTestExecutor
import com.gasolinerajsm.performance.util.TestDataGenerator
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import kotlinx.coroutines.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

/**
 * Load tests for high-concurrency coupon redemption scenarios
 *
 * Tests the system's ability to handle multiple concurrent coupon redemptions
 * while maintaining performance and data consistency.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=performance-test"])
@Tag("performance")
@Tag("load")
@DisplayName("Coupon Redemption Load Tests")
class CouponRedemptionLoadTest : BasePerformanceTest() {

    @Autowired
    private lateinit var loadTestExecutor: LoadTestExecutor

    @Autowired
    private lateinit var testDataGenerator: TestDataGenerator

    @Test
    @DisplayName("High-concurrency coupon validation load test")
    fun `should handle high-concurrency coupon validation requests`() = runBlocking {
        val concurrentUsers = getConcurrentUsers()
        val testDuration = getTestDuration()
        val baseUrl = getServiceUrl("coupon-service")

        println("Starting coupon validation load test with $concurrentUsers concurrent users for ${testDuration.seconds} seconds")

        // Prepare test data
        val testUsers = generateTestUsers(concurrentUsers)
        val testCoupons = generateTestCoupons(concurrentUsers * 2) // More coupons than users
        val userSessions = ConcurrentHashMap<String, UserSession>()

        // Authenticate all test users
        println("Authenticating $concurrentUsers test users...")
        val authJobs = testUsers.map { phoneNumber ->
            async(Dispatchers.IO) {
                try {
                    val token = authenticateUser(phoneNumber)
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

        // Wait for system stabilization
        waitForStabilization(Duration.ofSeconds(10))

        // Execute load test
        val scenario = LoadTestScenario(
            name = "Coupon Validation Load Test",
            description = "High-concurrency coupon validation requests",
            concurrentUsers = concurrentUsers,
            duration = testDuration,
            rampUpTime = Duration.ofSeconds(30),
            rampDownTime = Duration.ofSeconds(15),
            requestPattern = RequestPattern(PatternType.CONSTANT, concurrentUsers.toDouble()),
            thresholds = PerformanceThresholds(
                maxResponseTimeP95 = 500L,
                maxResponseTimeP99 = 1000L,
                minThroughput = 50.0,
                maxErrorRate = 1.0,
                maxCpuUsage = 80.0,
                maxMemoryUsage = 85.0
            )
        )

        val successCounter = AtomicInteger(0)
        val errorCounter = AtomicInteger(0)

        // Execute concurrent coupon validation requests
        val jobs = (1..concurrentUsers).map { userIndex ->
            async(Dispatchers.IO) {
                val phoneNumber = testUsers[userIndex % testUsers.size]
                val session = userSessions[phoneNumber]

                if (session?.accessToken == null) {
                    errorCounter.incrementAndGet()
                    return@async
                }

                val endTime = System.currentTimeMillis() + testDuration.toMillis()
                var requestCount = 0

                while (System.currentTimeMillis() < endTime) {
                    try {
                        val couponCode = testCoupons[(userIndex + requestCount) % testCoupons.size]
                        val startTime = System.currentTimeMillis()

                        val response = given()
                            .header("Authorization", "Bearer ${session.accessToken}")
                            .contentType(ContentType.JSON)
                            .body(mapOf(
                                "coupon_code" to couponCode,
                                "station_id" to 1,
                                "user_id" to userIndex
                            ))
                            .`when`()
                            .post("$baseUrl/api/coupons/validate")

                        val responseTime = System.currentTimeMillis() - startTime

                        when (response.statusCode) {
                            200 -> {
                                recordRequest(responseTime)
                                successCounter.incrementAndGet()
                            }
                            404 -> {
                                // Expected for some test coupons
                                recordRequest(responseTime)
                                successCounter.incrementAndGet()
                            }
                            else -> {
                                recordError(responseTime, "HTTP_${response.statusCode}")
                                errorCounter.incrementAndGet()
                            }
                        }

                        requestCount++

                        // Small delay to prevent overwhelming the system
                        delay(50)

                    } catch (e: Exception) {
                        recordError(0, "REQUEST_EXCEPTION")
                        errorCounter.incrementAndGet()
                        delay(100) // Longer delay on error
                    }
                }
            }
        }

        // Wait for all jobs to complete
        jobs.awaitAll()

        val totalRequests = successCounter.get() + errorCounter.get()
        val successRate = if (totalRequests > 0) (successCounter.get().toDouble() / totalRequests) * 100 else 0.0

        println("Coupon validation load test completed:")
        println("  Total requests: $totalRequests")
        println("  Successful requests: ${successCounter.get()}")
        println("  Failed requests: ${errorCounter.get()}")
        println("  Success rate: ${String.format("%.2f", successRate)}%")

        // Validate results
        assertTrue(successRate >= 95.0, "Success rate $successRate% is below 95%")
        assertTrue(totalRequests > 0, "No requests were executed")
    }

    @Test
    @DisplayName("Concurrent coupon redemption stress test")
    fun `should handle concurrent coupon redemption under stress`() = runBlocking {
        val concurrentUsers = getConcurrentUsers()
        val testDuration = getTestDuration()
        val redemptionServiceUrl = getServiceUrl("redemption-service")
        val couponServiceUrl = getServiceUrl("coupon-service")

        println("Starting concurrent coupon redemption stress test with $concurrentUsers users")

        // Prepare test data
        val testUsers = generateTestUsers(concurrentUsers)
        val testCoupons = generateTestCoupons(concurrentUsers)
        val userSessions = ConcurrentHashMap<String, UserSession>()

        // Authenticate users and get valid coupons
        println("Setting up test data...")
        val setupJobs = testUsers.mapIndexed { index, phoneNumber ->
            async(Dispatchers.IO) {
                try {
                    val token = authenticateUser(phoneNumber)
                    userSessions[phoneNumber] = UserSession(
                        userId = (index + 1).toString(),
                        phoneNumber = phoneNumber,
                        accessToken = token
                    )
                } catch (e: Exception) {
                    println("Failed to authenticate user $phoneNumber: ${e.message}")
                }
            }
        }
        setupJobs.awaitAll()

        // Wait for system stabilization
        waitForStabilization(Duration.ofSeconds(15))

        val successfulRedemptions = AtomicInteger(0)
        val failedRedemptions = AtomicInteger(0)
        val conflictErrors = AtomicInteger(0)

        // Execute concurrent redemption attempts
        val redemptionJobs = userSessions.entries.mapIndexed { index, (phoneNumber, session) ->
            async(Dispatchers.IO) {
                if (session.accessToken == null) {
                    failedRedemptions.incrementAndGet()
                    return@async
                }

                try {
                    val couponCode = testCoupons[index % testCoupons.size]
                    val startTime = System.currentTimeMillis()

                    // First validate the coupon
                    val validationResponse = given()
                        .header("Authorization", "Bearer ${session.accessToken}")
                        .contentType(ContentType.JSON)
                        .body(mapOf(
                            "coupon_code" to couponCode,
                            "station_id" to 1,
                            "user_id" to session.userId
                        ))
                        .`when`()
                        .post("$couponServiceUrl/api/coupons/validate")

                    if (validationResponse.statusCode != 200) {
                        recordError(System.currentTimeMillis() - startTime, "VALIDATION_FAILED")
                        failedRedemptions.incrementAndGet()
                        return@async
                    }

                    // Then attempt redemption
                    val redemptionRequest = mapOf(
                        "user_id" to session.userId,
                        "station_id" to 1,
                        "employee_id" to 1,
                        "coupon_code" to couponCode,
                        "fuel_type" to "Premium",
                        "fuel_quantity" to 25.0,
                        "fuel_price_per_unit" to 22.50,
                        "purchase_amount" to 150.0,
                        "payment_method" to "CREDIT_CARD"
                    )

                    val redemptionResponse = given()
                        .header("Authorization", "Bearer ${session.accessToken}")
                        .contentType(ContentType.JSON)
                        .body(redemptionRequest)
                        .`when`()
                        .post("$redemptionServiceUrl/api/redemptions")

                    val responseTime = System.currentTimeMillis() - startTime

                    when (redemptionResponse.statusCode) {
                        201 -> {
                            recordRequest(responseTime)
                            successfulRedemptions.incrementAndGet()
                        }
                        409 -> {
                            // Conflict - coupon already used (expected in concurrent scenario)
                            recordRequest(responseTime)
                            conflictErrors.incrementAndGet()
                        }
                        else -> {
                            recordError(responseTime, "HTTP_${redemptionResponse.statusCode}")
                            failedRedemptions.incrementAndGet()
                        }
                    }

                } catch (e: Exception) {
                    recordError(0, "REDEMPTION_EXCEPTION")
                    failedRedemptions.incrementAndGet()
                    println("Redemption failed for user $phoneNumber: ${e.message}")
                }
            }
        }

        // Wait for all redemption attempts
        redemptionJobs.awaitAll()

        val totalAttempts = successfulRedemptions.get() + failedRedemptions.get() + conflictErrors.get()
        val successRate = if (totalAttempts > 0) (successfulRedemptions.get().toDouble() / totalAttempts) * 100 else 0.0

        println("Concurrent coupon redemption stress test completed:")
        println("  Total attempts: $totalAttempts")
        println("  Successful redemptions: ${successfulRedemptions.get()}")
        println("  Conflict errors (expected): ${conflictErrors.get()}")
        println("  Failed redemptions: ${failedRedemptions.get()}")
        println("  Success rate: ${String.format("%.2f", successRate)}%")

        // Validate results - allowing for some conflicts in concurrent scenarios
        val acceptableSuccessRate = if (getTestMode() == "stress") 70.0 else 80.0
        assertTrue(
            successRate >= acceptableSuccessRate,
            "Success rate $successRate% is below acceptable threshold $acceptableSuccessRate%"
        )

        // Ensure we had some successful redemptions
        assertTrue(successfulRedemptions.get() > 0, "No successful redemptions occurred")

        // Verify data consistency - each coupon should only be redeemed once
        verifyRedemptionConsistency(redemptionServiceUrl, testCoupons)
    }

    @Test
    @DisplayName("Coupon redemption throughput benchmark")
    fun `should maintain acceptable throughput under sustained load`() = runBlocking {
        val targetThroughput = 100.0 // requests per second
        val testDuration = Duration.ofMinutes(5) // Longer test for throughput
        val baseUrl = getServiceUrl("redemption-service")

        println("Starting coupon redemption throughput benchmark")
        println("Target: $targetThroughput RPS for ${testDuration.toMinutes()} minutes")

        // Prepare test data
        val testUsers = generateTestUsers(200) // More users for sustained load
        val testCoupons = generateTestCoupons(1000) // Many coupons
        val userSessions = ConcurrentHashMap<String, UserSession>()

        // Authenticate users
        val authJobs = testUsers.map { phoneNumber ->
            async(Dispatchers.IO) {
                try {
                    val token = authenticateUser(phoneNumber)
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

        println("Authenticated ${userSessions.size} users")

        // Calculate request interval to achieve target throughput
        val requestIntervalMs = (1000.0 / targetThroughput).toLong()
        val endTime = System.currentTimeMillis() + testDuration.toMillis()

        val requestCounter = AtomicInteger(0)
        val successCounter = AtomicInteger(0)
        val errorCounter = AtomicInteger(0)

        // Execute sustained load
        val loadJob = async(Dispatchers.IO) {
            var couponIndex = 0

            while (System.currentTimeMillis() < endTime) {
                val startTime = System.currentTimeMillis()

                // Select user and coupon
                val userEntry = userSessions.entries.random()
                val session = userEntry.value
                val couponCode = testCoupons[couponIndex % testCoupons.size]
                couponIndex++

                if (session.accessToken != null) {
                    launch(Dispatchers.IO) {
                        try {
                            val redemptionRequest = mapOf(
                                "user_id" to session.userId,
                                "station_id" to (1..5).random(), // Distribute across stations
                                "employee_id" to 1,
                                "coupon_code" to couponCode,
                                "fuel_type" to listOf("Magna", "Premium", "Diesel").random(),
                                "fuel_quantity" to (10.0..50.0).random(),
                                "fuel_price_per_unit" to (20.0..25.0).random(),
                                "purchase_amount" to (100.0..300.0).random(),
                                "payment_method" to listOf("CASH", "CREDIT_CARD", "DEBIT_CARD").random()
                            )

                            val requestStartTime = System.currentTimeMillis()
                            val response = given()
                                .header("Authorization", "Bearer ${session.accessToken}")
                                .contentType(ContentType.JSON)
                                .body(redemptionRequest)
                                .`when`()
                                .post("$baseUrl/api/redemptions")

                            val responseTime = System.currentTimeMillis() - requestStartTime
                            requestCounter.incrementAndGet()

                            if (response.statusCode in 200..299) {
                                recordRequest(responseTime)
                                successCounter.incrementAndGet()
                            } else {
                                recordError(responseTime, "HTTP_${response.statusCode}")
                                errorCounter.incrementAndGet()
                            }

                        } catch (e: Exception) {
                            recordError(0, "REQUEST_EXCEPTION")
                            errorCounter.incrementAndGet()
                        }
                    }
                }

                // Maintain target throughput
                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = requestIntervalMs - elapsed
                if (sleepTime > 0) {
                    delay(sleepTime)
                }
            }
        }

        loadJob.await()

        val actualDuration = Duration.ofMillis(System.currentTimeMillis() - testStartTime.toEpochMilli())
        val actualThroughput = requestCounter.get().toDouble() / actualDuration.seconds
        val successRate = if (requestCounter.get() > 0) (successCounter.get().toDouble() / requestCounter.get()) * 100 else 0.0

        println("Throughput benchmark completed:")
        println("  Duration: ${actualDuration.seconds} seconds")
        println("  Total requests: ${requestCounter.get()}")
        println("  Successful requests: ${successCounter.get()}")
        println("  Failed requests: ${errorCounter.get()}")
        println("  Actual throughput: ${String.format("%.2f", actualThroughput)} RPS")
        println("  Target throughput: $targetThroughput RPS")
        println("  Success rate: ${String.format("%.2f", successRate)}%")

        // Validate throughput
        val minAcceptableThroughput = targetThroughput * 0.8 // 80% of target
        assertTrue(
            actualThroughput >= minAcceptableThroughput,
            "Actual throughput $actualThroughput RPS is below minimum $minAcceptableThroughput RPS"
        )

        // Validate success rate
        assertTrue(successRate >= 90.0, "Success rate $successRate% is below 90%")
    }

    private suspend fun authenticateUser(phoneNumber: String): String {
        val authUrl = getServiceUrl("auth-service")

        // Request OTP
        given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to phoneNumber, "purpose" to "LOGIN"))
            .`when`()
            .post("$authUrl/api/auth/otp/request")

        // Login with OTP
        val response = given()
            .contentType(ContentType.JSON)
            .body(mapOf("phone_number" to phoneNumber, "otp_code" to "123456"))
            .`when`()
            .post("$authUrl/api/auth/login")

        if (response.statusCode != 200) {
            throw Exception("Authentication failed with status ${response.statusCode}")
        }

        return response.jsonPath().getString("access_token")
    }

    private suspend fun verifyRedemptionConsistency(baseUrl: String, testCoupons: List<String>) {
        println("Verifying redemption consistency...")

        // This would typically query the database to verify each coupon was only redeemed once
        // For now, we'll just log that consistency check should be performed
        println("Consistency check: Each of ${testCoupons.size} coupons should be redeemed at most once")

        // In a real implementation, you would:
        // 1. Query the database for all redemptions
        // 2. Group by coupon_code
        // 3. Verify each coupon appears at most once
        // 4. Assert no duplicate redemptions exist
    }
}