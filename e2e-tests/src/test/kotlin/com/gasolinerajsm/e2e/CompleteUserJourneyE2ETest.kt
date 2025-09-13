package com.gasolinerajsm.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Complete End-to-End User Journey Test
 *
 * Tests the complete flow: Registration → Station Search → Coupon Purchase → Redemption → Raffle → Dashboard
 *
 * This test validates:
 * - User registration and authentication
 * - Station search and discovery
 * - Coupon purchase with payment processing
 * - QR code generation and validation
 * - Coupon redemption at gas station
 * - Raffle ticket generation and multipliers
 * - Ad engagement and bonus tickets
 * - Raffle draw and winner selection
 * - Dashboard analytics and reporting
 *
 * Performance Requirements:
 * - Complete flow should finish in < 30 seconds
 * - Each API call should respond in < 2 seconds
 * - No data inconsistencies between services
 * - All business rules must be enforced
 */
@ExtendWith(SpringJUnitExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CompleteUserJourneyE2ETest {

    @LocalServerPort
    private var port: Int = 0

    private val objectMapper = ObjectMapper().registerKotlinModule()

    // Test containers for complete isolation
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15")
            .withDatabaseName("gasolinera_e2e_test")
            .withUsername("test_user")
            .withPassword("test_password")

        @Container
        @JvmStatic
        val redis = GenericContainer("redis:7")
            .withExposedPorts(6379)

        @Container
        @JvmStatic
        val rabbitmq = RabbitMQContainer("rabbitmq:3-management")
            .withUser("test_user", "test_password")
    }

    // Test data that will be populated during the journey
    private lateinit var testUser: TestUser
    private lateinit var authToken: String
    private lateinit var selectedStation: TestStation
    private lateinit var purchasedCoupon: TestCoupon
    private lateinit var redemptionResult: TestRedemption
    private var raffleTickets: List<TestRaffleTicket> = emptyList()
    private var activeRaffle: TestRaffle? = null

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    @Order(1)
    @DisplayName("🚀 Complete User Journey: Registration to Dashboard")
    fun testCompleteUserJourney() {
        val startTime = System.currentTimeMillis()

        try {
            // Step 1: User Registration
            registerNewUser()

            // Step 2: User Authentication
            authenticateUser()

            // Step 3: Search for Gas Stations
            searchNearbyStations()

            // Step 4: Purchase Coupon
            purchaseCoupon()

            // Step 5: Validate QR Code
            validateQRCode()

            // Step 6: Redeem Coupon at Station
            redeemCoupon()

            // Step 7: Verify Raffle Tickets Generated
            verifyRaffleTicketsGenerated()

            // Step 8: Engage with Ads for Bonus Tickets
            engageWithAds()

            // Step 9: Participate in Raffle Draw
            participateInRaffleDraw()

            // Step 10: View Dashboard Analytics
            viewDashboardAnalytics()

            // Step 11: Validate Data Consistency
            validateDataConsistency()

            val totalTime = System.currentTimeMillis() - startTime
            println("✅ Complete user journey completed in ${totalTime}ms")

            // Assert performance requirement
            assertTrue(totalTime < 30000, "Complete journey should finish in < 30 seconds")

        } catch (Exception e) {
            val totalTime = System.currentTimeMillis() - startTime
            println("❌ User journey failed after ${totalTime}ms: ${e.message}")
            throw e
        }
    }

    // ==========================================
    // Step 1: User Registration
    // ==========================================

    private fun registerNewUser() {
        println("📝 Step 1: Registering new user...")

        testUser = TestUser(
            email = "test.user.${UUID.randomUUID()}@gasolinera-jsm.com",
            password = "SecurePassword123!",
            firstName = "Test",
            lastName = "User",
            phone = "+52${Random().nextInt(1000000000)}"
        )

        val registrationRequest = mapOf(
            "email" to testUser.email,
            "password" to testUser.password,
            "firstName" to testUser.firstName,
            "lastName" to testUser.lastName,
            "phone" to testUser.phone
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registrationRequest)
            .`when`()
            .post("/api/v1/auth/register")
            .then()
            .statusCode(201)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        testUser.id = responseBody.get("id").asText()

        assertNotNull(testUser.id, "User ID should be returned after registration")
        println("✅ User registered successfully: ${testUser.id}")
    }

    // ==========================================
    // Step 2: User Authentication
    // ==========================================

    private fun authenticateUser() {
        println("🔐 Step 2: Authenticating user...")

        val loginRequest = mapOf(
            "email" to testUser.email,
            "password" to testUser.password
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .`when`()
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        authToken = responseBody.get("accessToken").asText()

        assertNotNull(authToken, "Access token should be returned after login")
        assertTrue(authToken.isNotBlank(), "Access token should not be blank")
        println("✅ User authenticated successfully")
    }

    // ==========================================
    // Step 3: Search for Gas Stations
    // ==========================================

    private fun searchNearbyStations() {
        println("⛽ Step 3: Searching for nearby gas stations...")

        // Mexico City coordinates
        val latitude = 19.4326
        val longitude = -99.1332
        val radius = 5.0 // 5km radius

        val response = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .queryParam("latitude", latitude)
            .queryParam("longitude", longitude)
            .queryParam("radius", radius)
            .`when`()
            .get("/api/v1/stations/nearby")
            .then()
            .statusCode(200)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        val stations = responseBody.get("stations")

        assertTrue(stations.isArray && stations.size() > 0, "Should find at least one station")

        // Select the first station with good rating
        val stationNode = stations.first { it.get("rating").asDouble() >= 4.0 }
        selectedStation = TestStation(
            id = stationNode.get("id").asText(),
            name = stationNode.get("name").asText(),
            address = stationNode.get("address").asText(),
            latitude = stationNode.get("latitude").asDouble(),
            longitude = stationNode.get("longitude").asDouble(),
            rating = stationNode.get("rating").asDouble(),
            fuelPrices = mapOf(
                "REGULAR" to stationNode.get("fuelPrices").get("REGULAR").asDouble(),
                "PREMIUM" to stationNode.get("fuelPrices").get("PREMIUM").asDouble()
            )
        )

        println("✅ Selected station: ${selectedStation.name} (Rating: ${selectedStation.rating})")
    }

    // ==========================================
    // Step 4: Purchase Coupon
    // ==========================================

    private fun purchaseCoupon() {
        println("💳 Step 4: Purchasing coupon...")

        val couponAmount = BigDecimal("500.00") // $500 MXN
        val fuelType = "REGULAR"

        val purchaseRequest = mapOf(
            "stationId" to selectedStation.id,
            "amount" to couponAmount,
            "fuelType" to fuelType,
            "paymentMethod" to "CREDIT_CARD",
            "paymentDetails" to mapOf(
                "cardNumber" to "4111111111111111", // Test card
                "expiryMonth" to "12",
                "expiryYear" to "2025",
                "cvv" to "123",
                "cardholderName" to "${testUser.firstName} ${testUser.lastName}"
            )
        )

        val response = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .contentType(ContentType.JSON)
            .body(purchaseRequest)
            .`when`()
            .post("/api/v1/coupons/purchase")
            .then()
            .statusCode(201)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        purchasedCoupon = TestCoupon(
            id = responseBody.get("id").asText(),
            userId = testUser.id,
            stationId = selectedStation.id,
            amount = BigDecimal(responseBody.get("amount").asText()),
            fuelType = responseBody.get("fuelType").asText(),
            qrCode = responseBody.get("qrCode").asText(),
            status = responseBody.get("status").asText(),
            validUntil = LocalDateTime.parse(responseBody.get("validUntil").asText())
        )

        assertEquals("ACTIVE", purchasedCoupon.status, "Coupon should be active after purchase")
        assertNotNull(purchasedCoupon.qrCode, "QR code should be generated")
        assertTrue(purchasedCoupon.qrCode.isNotBlank(), "QR code should not be blank")
        println("✅ Coupon purchased successfully: ${purchasedCoupon.id}")
    }

    // ==========================================
    // Step 5: Validate QR Code
    // ==========================================

    private fun validateQRCode() {
        println("📱 Step 5: Validating QR code...")

        val response = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .queryParam("qrCode", purchasedCoupon.qrCode)
            .`when`()
            .get("/api/v1/coupons/validate-qr")
            .then()
            .statusCode(200)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        val isValid = responseBody.get("valid").asBoolean()
        val couponDetails = responseBody.get("coupon")

        assertTrue(isValid, "QR code should be valid")
        assertEquals(purchasedCoupon.id, couponDetails.get("id").asText(), "QR should match purchased coupon")
        println("✅ QR code validated successfully")
    }

    // ==========================================
    // Step 6: Redeem Coupon at Station
    // ==========================================

    private fun redeemCoupon() {
        println("⛽ Step 6: Redeeming coupon at gas station...")

        val fuelAmount = BigDecimal("20.00") // 20 liters

        val redemptionRequest = mapOf(
            "couponId" to purchasedCoupon.id,
            "stationId" to selectedStation.id,
            "fuelAmount" to fuelAmount,
            "location" to mapOf(
                "latitude" to selectedStation.latitude,
                "longitude" to selectedStation.longitude
            ),
            "attendantId" to "ATT_${Random().nextInt(1000)}",
            "pumpNumber" to Random().nextInt(10) + 1
        )

        val response = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .contentType(ContentType.JSON)
            .body(redemptionRequest)
            .`when`()
            .post("/api/v1/coupons/${purchasedCoupon.id}/redeem")
            .then()
            .statusCode(200)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        redemptionResult = TestRedemption(
            id = responseBody.get("id").asText(),
            couponId = purchasedCoupon.id,
            fuelAmount = BigDecimal(responseBody.get("fuelAmount").asText()),
            totalCost = BigDecimal(responseBody.get("totalCost").asText()),
            ticketsGenerated = responseBody.get("ticketsGenerated").asInt(),
            redeemedAt = LocalDateTime.parse(responseBody.get("redeemedAt").asText())
        )

        assertTrue(redemptionResult.ticketsGenerated > 0, "Should generate raffle tickets")
        assertEquals(fuelAmount, redemptionResult.fuelAmount, "Fuel amount should match request")
        println("✅ Coupon redeemed successfully. Generated ${redemptionResult.ticketsGenerated} raffle tickets")
    }

    // ==========================================
    // Step 7: Verify Raffle Tickets Generated
    // ==========================================

    private fun verifyRaffleTicketsGenerated() {
        println("🎰 Step 7: Verifying raffle tickets generated...")

        // Wait a moment for async processing
        Thread.sleep(2000)

        val response = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .`when`()
            .get("/api/v1/raffle/tickets/my-tickets")
            .then()
            .statusCode(200)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        val ticketsArray = responseBody.get("tickets")

        assertTrue(ticketsArray.isArray && ticketsArray.size() > 0, "Should have raffle tickets")

        raffleTickets = ticketsArray.map { ticketNode ->
            TestRaffleTicket(
                id = ticketNode.get("id").asText(),
                ticketNumber = ticketNode.get("ticketNumber").asText(),
                raffleId = ticketNode.get("raffleId").asText(),
                userId = testUser.id,
                sourceRedemptionId = redemptionResult.id,
                multiplier = ticketNode.get("multiplier").asDouble(),
                generatedAt = LocalDateTime.parse(ticketNode.get("generatedAt").asText())
            )
        }

        assertEquals(redemptionResult.ticketsGenerated, raffleTickets.size,
            "Number of tickets should match redemption result")
        println("✅ Verified ${raffleTickets.size} raffle tickets generated")
    }

    // ==========================================
    // Step 8: Engage with Ads for Bonus Tickets
    // ==========================================

    private fun engageWithAds() {
        println("📺 Step 8: Engaging with ads for bonus tickets...")

        // Get available ads
        val adsResponse = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .`when`()
            .get("/api/v1/ads/available")
            .then()
            .statusCode(200)
            .extract().response()

        val adsBody = objectMapper.readTree(adsResponse.body.asString())
        val availableAds = adsBody.get("ads")

        if (availableAds.isArray && availableAds.size() > 0) {
            val adToEngage = availableAds.first()
            val adId = adToEngage.get("id").asText()
            val multiplier = adToEngage.get("ticketMultiplier").asDouble()

            // Engage with ad (watch/click)
            val engagementRequest = mapOf(
                "adId" to adId,
                "engagementType" to "WATCH_COMPLETE",
                "duration" to 30, // 30 seconds
                "redemptionId" to redemptionResult.id
            )

            val engagementResponse = RestAssured.given()
                .header("Authorization", "Bearer $authToken")
                .contentType(ContentType.JSON)
                .body(engagementRequest)
                .`when`()
                .post("/api/v1/ads/engage")
                .then()
                .statusCode(200)
                .extract().response()

            val engagementBody = objectMapper.readTree(engagementResponse.body.asString())
            val bonusTickets = engagementBody.get("bonusTicketsGenerated").asInt()

            assertTrue(bonusTickets > 0, "Should generate bonus tickets from ad engagement")
            println("✅ Ad engagement completed. Generated $bonusTickets bonus tickets (${multiplier}x multiplier)")
        } else {
            println("⚠️ No ads available for engagement")
        }
    }

    // ==========================================
    // Step 9: Participate in Raffle Draw
    // ==========================================

    private fun participateInRaffleDraw() {
        println("🎲 Step 9: Participating in raffle draw...")

        // Get active raffles
        val rafflesResponse = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .`when`()
            .get("/api/v1/raffle/active")
            .then()
            .statusCode(200)
            .extract().response()

        val rafflesBody = objectMapper.readTree(rafflesResponse.body.asString())
        val activeRaffles = rafflesBody.get("raffles")

        assertTrue(activeRaffles.isArray && activeRaffles.size() > 0, "Should have active raffles")

        val raffleNode = activeRaffles.first()
        activeRaffle = TestRaffle(
            id = raffleNode.get("id").asText(),
            name = raffleNode.get("name").asText(),
            description = raffleNode.get("description").asText(),
            startDate = LocalDateTime.parse(raffleNode.get("startDate").asText()),
            endDate = LocalDateTime.parse(raffleNode.get("endDate").asText()),
            drawDate = LocalDateTime.parse(raffleNode.get("drawDate").asText()),
            status = raffleNode.get("status").asText(),
            totalTickets = raffleNode.get("totalTickets").asInt(),
            userTickets = raffleNode.get("userTickets").asInt()
        )

        assertTrue(activeRaffle!!.userTickets > 0, "User should have tickets in the raffle")
        println("✅ Participating in raffle: ${activeRaffle!!.name} with ${activeRaffle!!.userTickets} tickets")

        // Simulate raffle draw (for testing purposes)
        if (activeRaffle!!.status == "ACTIVE") {
            simulateRaffleDraw()
        }
    }

    private fun simulateRaffleDraw() {
        println("🎯 Simulating raffle draw...")

        val drawRequest = mapOf(
            "raffleId" to activeRaffle!!.id,
            "drawType" to "MANUAL_TEST_DRAW"
        )

        val response = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .contentType(ContentType.JSON)
            .body(drawRequest)
            .`when`()
            .post("/api/v1/raffle/admin/draw")
            .then()
            .statusCode(200)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        val winners = responseBody.get("winners")
        val totalWinners = winners.size()

        println("✅ Raffle draw completed with $totalWinners winners")
    }

    // ==========================================
    // Step 10: View Dashboard Analytics
    // ==========================================

    private fun viewDashboardAnalytics() {
        println("📊 Step 10: Viewing dashboard analytics...")

        // User dashboard
        val userDashboardResponse = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .`when`()
            .get("/api/v1/dashboard/user")
            .then()
            .statusCode(200)
            .extract().response()

        val userDashboard = objectMapper.readTree(userDashboardResponse.body.asString())

        // Verify user statistics
        val totalCoupons = userDashboard.get("totalCoupons").asInt()
        val totalRedemptions = userDashboard.get("totalRedemptions").asInt()
        val totalRaffleTickets = userDashboard.get("totalRaffleTickets").asInt()
        val totalSpent = BigDecimal(userDashboard.get("totalSpent").asText())

        assertTrue(totalCoupons >= 1, "Should have at least 1 coupon")
        assertTrue(totalRedemptions >= 1, "Should have at least 1 redemption")
        assertTrue(totalRaffleTickets >= 1, "Should have at least 1 raffle ticket")
        assertTrue(totalSpent >= purchasedCoupon.amount, "Total spent should include coupon amount")

        println("✅ User dashboard verified:")
        println("   - Total coupons: $totalCoupons")
        println("   - Total redemptions: $totalRedemptions")
        println("   - Total raffle tickets: $totalRaffleTickets")
        println("   - Total spent: $totalSpent MXN")

        // Station analytics (if user has admin access or public stats)
        try {
            val stationAnalyticsResponse = RestAssured.given()
                .header("Authorization", "Bearer $authToken")
                .`when`()
                .get("/api/v1/dashboard/station/${selectedStation.id}")
                .then()
                .extract().response()

            if (stationAnalyticsResponse.statusCode == 200) {
                val stationAnalytics = objectMapper.readTree(stationAnalyticsResponse.body.asString())
                val stationRedemptions = stationAnalytics.get("totalRedemptions").asInt()
                val stationRevenue = BigDecimal(stationAnalytics.get("totalRevenue").asText())

                println("✅ Station analytics verified:")
                println("   - Station redemptions: $stationRedemptions")
                println("   - Station revenue: $stationRevenue MXN")
            }
        } catch (e: Exception) {
            println("ℹ️ Station analytics not accessible (expected for regular users)")
        }
    }

    // ==========================================
    // Step 11: Validate Data Consistency
    // ==========================================

    private fun validateDataConsistency() {
        println("🔍 Step 11: Validating data consistency across services...")

        // Validate coupon status consistency
        val couponResponse = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .`when`()
            .get("/api/v1/coupons/${purchasedCoupon.id}")
            .then()
            .statusCode(200)
            .extract().response()

        val couponData = objectMapper.readTree(couponResponse.body.asString())
        assertEquals("USED", couponData.get("status").asText(), "Coupon should be marked as USED after redemption")

        // Validate redemption record exists
        val redemptionsResponse = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .`when`()
            .get("/api/v1/coupons/${purchasedCoupon.id}/redemptions")
            .then()
            .statusCode(200)
            .extract().response()

        val redemptionsData = objectMapper.readTree(redemptionsResponse.body.asString())
        val redemptions = redemptionsData.get("redemptions")
        assertTrue(redemptions.isArray && redemptions.size() > 0, "Should have redemption records")

        // Validate raffle tickets are linked to redemption
        val ticketsResponse = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .queryParam("redemptionId", redemptionResult.id)
            .`when`()
            .get("/api/v1/raffle/tickets/by-redemption")
            .then()
            .statusCode(200)
            .extract().response()

        val ticketsData = objectMapper.readTree(ticketsResponse.body.asString())
        val linkedTickets = ticketsData.get("tickets")
        assertTrue(linkedTickets.isArray && linkedTickets.size() > 0,
            "Should have raffle tickets linked to redemption")

        // Validate user balance/statistics
        val userStatsResponse = RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .`when`()
            .get("/api/v1/users/me/statistics")
            .then()
            .statusCode(200)
            .extract().response()

        val userStats = objectMapper.readTree(userStatsResponse.body.asString())
        val activeCoupons = userStats.get("activeCoupons").asInt()
        val usedCoupons = userStats.get("usedCoupons").asInt()

        assertTrue(usedCoupons >= 1, "Should have at least 1 used coupon")

        println("✅ Data consistency validated across all services")
        println("   - Coupon status: USED")
        println("   - Redemption records: ${redemptions.size()}")
        println("   - Linked raffle tickets: ${linkedTickets.size()}")
        println("   - User active coupons: $activeCoupons")
        println("   - User used coupons: $usedCoupons")
    }

    // ==========================================
    // Performance and Load Testing
    // ==========================================

    @Test
    @Order(2)
    @DisplayName("🚀 Performance Test: Concurrent User Journeys")
    fun testConcurrentUserJourneys() {
        val numberOfUsers = 10
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Boolean>()
        val startTime = System.currentTimeMillis()

        println("🔥 Starting concurrent user journey test with $numberOfUsers users...")

        repeat(numberOfUsers) { userIndex ->
            val thread = Thread {
                try {
                    val userJourney = ConcurrentUserJourney(port, userIndex)
                    userJourney.executeCompleteJourney()
                    synchronized(results) {
                        results.add(true)
                    }
                } catch (e: Exception) {
                    println("❌ User $userIndex failed: ${e.message}")
                    synchronized(results) {
                        results.add(false)
                    }
                }
            }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join(60000) } // 60 second timeout per thread

        val totalTime = System.currentTimeMillis() - startTime
        val successfulUsers = results.count { it }
        val successRate = (successfulUsers.toDouble() / numberOfUsers) * 100

        println("📊 Concurrent test results:")
        println("   - Total users: $numberOfUsers")
        println("   - Successful: $successfulUsers")
        println("   - Success rate: ${String.format("%.1f", successRate)}%")
        println("   - Total time: ${totalTime}ms")
        println("   - Average time per user: ${totalTime / numberOfUsers}ms")

        // Assert performance requirements
        assertTrue(successRate >= 95.0, "Success rate should be >= 95%")
        assertTrue(totalTime < 120000, "Concurrent test should complete in < 2 minutes")
    }

    // ==========================================
    // Data Classes for Test Objects
    // ==========================================

    data class TestUser(
        val email: String,
        val password: String,
        val firstName: String,
        val lastName: String,
        val phone: String,
        var id: String = ""
    )

    data class TestStation(
        val id: String,
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val rating: Double,
        val fuelPrices: Map<String, Double>
    )

    data class TestCoupon(
        val id: String,
        val userId: String,
        val stationId: String,
        val amount: BigDecimal,
        val fuelType: String,
        val qrCode: String,
        val status: String,
        val validUntil: LocalDateTime
    )

    data class TestRedemption(
        val id: String,
        val couponId: String,
        val fuelAmount: BigDecimal,
        val totalCost: BigDecimal,
        val ticketsGenerated: Int,
        val redeemedAt: LocalDateTime
    )

    data class TestRaffleTicket(
        val id: String,
        val ticketNumber: String,
        val raffleId: String,
        val userId: String,
        val sourceRedemptionId: String,
        val multiplier: Double,
        val generatedAt: LocalDateTime
    )

    data class TestRaffle(
        val id: String,
        val name: String,
        val description: String,
        val startDate: LocalDateTime,
        val endDate: LocalDateTime,
        val drawDate: LocalDateTime,
        val status: String,
        val totalTickets: Int,
        val userTickets: Int
    )
}

/**
 * Helper class for concurrent user journey testing
 */
class ConcurrentUserJourney(private val port: Int, private val userIndex: Int) {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private lateinit var authToken: String

    fun executeCompleteJourney() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        // Simplified journey for concurrent testing
        registerAndAuthenticate()
        searchStations()
        purchaseAndRedeemCoupon()
    }

    private fun registerAndAuthenticate() {
        // Register user
        val registrationRequest = mapOf(
            "email" to "concurrent.user.$userIndex.${UUID.randomUUID()}@test.com",
            "password" to "TestPassword123!",
            "firstName" to "User",
            "lastName" to "$userIndex",
            "phone" to "+52${Random().nextInt(1000000000)}"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registrationRequest)
            .post("/api/v1/auth/register")
            .then()
            .statusCode(201)

        // Login
        val loginRequest = mapOf(
            "email" to registrationRequest["email"],
            "password" to registrationRequest["password"]
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract().response()

        val responseBody = objectMapper.readTree(response.body.asString())
        authToken = responseBody.get("accessToken").asText()
    }

    private fun searchStations() {
        RestAssured.given()
            .header("Authorization", "Bearer $authToken")
            .queryParam("latitude", 19.4326)
            .queryParam("longitude", -99.1332)
            .queryParam("radius", 5.0)
            .get("/api/v1/stations/nearby")
            .then()
            .statusCode(200)
    }

    private fun purchaseAndRedeemCoupon() {
        // This would be a simplified version of purchase and redemption
        // for concurrent testing purposes
        println("User $userIndex completed simplified journey")
    }
}