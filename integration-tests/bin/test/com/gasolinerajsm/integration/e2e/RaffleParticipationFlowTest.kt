package com.gasolinerajsm.integration.e2e

import com.gasolinerajsm.integration.base.BaseIntegrationTest
import com.gasolinerajsm.integration.model.*
import com.gasolinerajsm.integration.util.TestUtils
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import kotlin.test.*

/**
 * End-to-end integration tests for raffle participation and winner selection
 *
 * Tests the complete raffle journey:
 * Coupon Redemption -> Raffle Tickets -> Ad Engagement -> Ticket Multiplication -> Raffle Entry -> Winner Selection
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.profiles.active=integration-test"])
@Tag("e2e")
@Tag("integration")
@DisplayName("Raffle Participation Flow E2E Tests")
class RaffleParticipationFlowTest : BaseIntegrationTest() {

    @Test
    @DisplayName("Complete raffle participation workflow from coupon redemption to entry")
    fun `should complete full raffle participation workflow`() {
        val baseUrl = baseUrl()

        // Step 1: Authenticate customer and employee
        val customerToken = TestUtils.authenticateUser("+525555001005", "123456", baseUrl)
        val customerInfo = TestUtils.getUserInfo("+525555001005", customerToken, baseUrl)
        val employeeToken = TestUtils.authenticateUser("+525555000007", "123456", baseUrl)
        val employeeInfo = TestUtils.getUserInfo("+525555000007", employeeToken, baseUrl)

        // Step 2: Get available raffles
        val availableRaffles = TestUtils.getAvailableRaffles(customerToken, baseUrl)
        assertTrue(availableRaffles.isNotEmpty())

        val openRaffle = availableRaffles.first { it.status == "OPEN" }
        assertEquals("OPEN", openRaffle.status)
        assertTrue(openRaffle.currentParticipants < (openRaffle.maxParticipants ?: Int.MAX_VALUE))

        // Step 3: Redeem coupon to earn raffle tickets
        val coupons = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/coupons/available")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", CouponInfo::class.java)
            .filter { it.status == "ACTIVE" }

        assertTrue(coupons.isNotEmpty())
        val testCoupon = coupons.first()

        val redemptionRequest = RedemptionRequest(
            userId = customerInfo.id,
            stationId = 1L,
            employeeId = employeeInfo.id,
            couponCode = testCoupon.couponCode,
            fuelType = "Premium",
            fuelQuantity = 30.0,
            fuelPricePerUnit = 22.50,
            purchaseAmount = 200.0,
            paymentMethod = "CREDIT_CARD"
        )

        val redemptionResponse = TestUtils.processRedemption(redemptionRequest, employeeToken, baseUrl)
        assertTrue(redemptionResponse.raffleTicketsEarned > 0)

        // Step 4: Wait for raffle tickets to be processed
        TestUtils.waitForCondition(Duration.ofSeconds(15)) {
            val tickets = given()
                .header("Authorization", "Bearer $customerToken")
                .queryParam("userId", customerInfo.id)
                .`when`()
                .get("$baseUrl/api/raffles/tickets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", Map::class.java)

            tickets.any {
                it["source_type"] == "COUPON_REDEMPTION" &&
                it["source_id"] == redemptionResponse.id.toString()
            }
        }

        // Step 5: Get available advertisements to earn more tickets
        val availableAds = TestUtils.getAvailableAds(customerInfo.id, 1L, customerToken, baseUrl)
        assertTrue(availableAds.isNotEmpty())

        val testAd = availableAds.first { it.status == "ACTIVE" }
        assertTrue(testAd.ticketMultiplier > 1)

        // Step 6: Engage with advertisement to multiply tickets
        val adEngagementRequest = AdEngagementRequest(
            advertisementId = testAd.id,
            userId = customerInfo.id,
            sessionId = TestUtils.generateSessionId(),
            engagementType = "COMPLETION",
            durationSeconds = testAd.durationSeconds,
            completionPercentage = 100,
            stationId = 1L
        )

        val adEngagementResponse = TestUtils.recordAdEngagement(adEngagementRequest, customerToken, baseUrl)
        assertEquals("COMPLETED", adEngagementResponse.status)
        assertTrue(adEngagementResponse.ticketsMultiplied > 0)
        assertEquals(testAd.ticketMultiplier, adEngagementResponse.multiplierApplied)

        // Step 7: Wait for ad engagement tickets to be processed
        TestUtils.waitForCondition(Duration.ofSeconds(15)) {
            val tickets = given()
                .header("Authorization", "Bearer $customerToken")
                .queryParam("userId", customerInfo.id)
                .`when`()
                .get("$baseUrl/api/raffles/tickets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", Map::class.java)

            tickets.any {
                it["source_type"] == "AD_ENGAGEMENT" &&
                it["source_id"] == adEngagementResponse.id.toString()
            }
        }

        // Step 8: Get total available tickets for user
        val userTickets = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/raffles/tickets/summary")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getInt("total_available")

        assertTrue(userTickets >= redemptionResponse.raffleTicketsEarned + adEngagementResponse.ticketsMultiplied)

        // Step 9: Enter the raffle
        val ticketsToUse = minOf(userTickets, 5) // Use up to 5 tickets
        val raffleEntryRequest = RaffleEntryRequest(
            raffleId = openRaffle.id,
            userId = customerInfo.id,
            ticketsUsed = ticketsToUse,
            entryMethod = "MOBILE_APP"
        )

        val raffleEntryResponse = TestUtils.enterRaffle(raffleEntryRequest, customerToken, baseUrl)
        assertEquals(openRaffle.id, raffleEntryResponse.raffleId)
        assertEquals(customerInfo.id, raffleEntryResponse.userId)
        assertEquals(ticketsToUse, raffleEntryResponse.ticketsUsed)
        assertNotNull(raffleEntryResponse.transactionReference)

        // Step 10: Verify raffle entry appears in user's entries
        val userEntries = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/raffles/entries")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", RaffleEntryResponse::class.java)

        assertTrue(userEntries.any { it.transactionReference == raffleEntryResponse.transactionReference })

        // Step 11: Verify raffle participant count increased
        val updatedRaffle = given()
            .header("Authorization", "Bearer $customerToken")
            .`when`()
            .get("$baseUrl/api/raffles/${openRaffle.id}")
            .then()
            .statusCode(200)
            .extract()
            .`as`(RaffleInfo::class.java)

        assertTrue(updatedRaffle.currentParticipants > openRaffle.currentParticipants)

        // Step 12: Verify user's available tickets decreased
        val updatedTicketSummary = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/raffles/tickets/summary")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getInt("total_available")

        assertEquals(userTickets - ticketsToUse, updatedTicketSummary)

        // Step 13: Verify audit trail
        val auditLogs = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("raffleId", openRaffle.id)
            .queryParam("userId", customerInfo.id)
            .`when`()
            .get("$baseUrl/api/audit/raffle-entries")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", Map::class.java)

        assertTrue(auditLogs.any { it["event_type"] == "ENTRY_SUBMITTED" })
    }

    @Test
    @DisplayName("Raffle winner selection process")
    fun `should handle raffle winner selection process`() {
        val baseUrl = baseUrl()

        // This test requires admin privileges to trigger winner selection
        val adminToken = TestUtils.authenticateUser("+525555000001", "123456", baseUrl)

        // Step 1: Get a raffle that's ready for drawing
        val raffles = given()
            .header("Authorization", "Bearer $adminToken")
            .queryParam("status", "CLOSED")
            .`when`()
            .get("$baseUrl/api/admin/raffles")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", RaffleInfo::class.java)

        if (raffles.isEmpty()) {
            // Create a test raffle for winner selection
            val testRaffleRequest = mapOf(
                "name" to "Test Winner Selection Raffle",
                "description" to "Test raffle for winner selection",
                "raffle_type" to "STANDARD",
                "registration_start" to "2024-01-01T00:00:00",
                "registration_end" to "2024-01-02T00:00:00",
                "draw_date" to "2024-01-03T00:00:00",
                "min_tickets_to_participate" to 1,
                "max_tickets_per_user" to 10,
                "max_participants" to 100,
                "prize_pool_total" to 5000.0,
                "is_public" to true
            )

            val createdRaffle = given()
                .header("Authorization", "Bearer $adminToken")
                .contentType(ContentType.JSON)
                .body(testRaffleRequest)
                .`when`()
                .post("$baseUrl/api/admin/raffles")
                .then()
                .statusCode(201)
                .extract()
                .`as`(RaffleInfo::class.java)

            // Add some test entries (this would normally be done by users)
            val testUsers = listOf("+525555001001", "+525555001002", "+525555001003")
            testUsers.forEach { phoneNumber ->
                val userToken = TestUtils.authenticateUser(phoneNumber, "123456", baseUrl)
                val userInfo = TestUtils.getUserInfo(phoneNumber, userToken, baseUrl)

                // Simulate user having tickets and entering raffle
                val entryRequest = RaffleEntryRequest(
                    raffleId = createdRaffle.id,
                    userId = userInfo.id,
                    ticketsUsed = 3,
                    entryMethod = "WEB_PORTAL"
                )

                TestUtils.enterRaffle(entryRequest, userToken, baseUrl)
            }

            // Close the raffle for drawing
            given()
                .header("Authorization", "Bearer $adminToken")
                .`when`()
                .put("$baseUrl/api/admin/raffles/${createdRaffle.id}/close")
                .then()
                .statusCode(200)
        }

        // Step 2: Get a closed raffle ready for winner selection
        val closedRaffles = given()
            .header("Authorization", "Bearer $adminToken")
            .queryParam("status", "CLOSED")
            .`when`()
            .get("$baseUrl/api/admin/raffles")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", RaffleInfo::class.java)

        assertTrue(closedRaffles.isNotEmpty())
        val testRaffle = closedRaffles.first()

        // Step 3: Get raffle prizes
        val prizes = given()
            .header("Authorization", "Bearer $adminToken")
            .`when`()
            .get("$baseUrl/api/admin/raffles/${testRaffle.id}/prizes")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", Map::class.java)

        assertTrue(prizes.isNotEmpty())

        // Step 4: Trigger winner selection
        val winnerSelectionRequest = mapOf(
            "raffle_id" to testRaffle.id,
            "draw_algorithm" to "RANDOM",
            "seed" to System.currentTimeMillis()
        )

        val winnerSelectionResponse = given()
            .header("Authorization", "Bearer $adminToken")
            .contentType(ContentType.JSON)
            .body(winnerSelectionRequest)
            .`when`()
            .post("$baseUrl/api/admin/raffles/${testRaffle.id}/select-winners")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()

        val selectedWinners = winnerSelectionResponse.getList("winners", Map::class.java)
        assertTrue(selectedWinners.isNotEmpty())
        assertEquals(prizes.size, selectedWinners.size)

        // Step 5: Verify raffle status changed to COMPLETED
        val completedRaffle = given()
            .header("Authorization", "Bearer $adminToken")
            .`when`()
            .get("$baseUrl/api/admin/raffles/${testRaffle.id}")
            .then()
            .statusCode(200)
            .extract()
            .`as`(RaffleInfo::class.java)

        assertEquals("COMPLETED", completedRaffle.status)

        // Step 6: Verify winners were recorded
        val winners = given()
            .header("Authorization", "Bearer $adminToken")
            .`when`()
            .get("$baseUrl/api/admin/raffles/${testRaffle.id}/winners")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", Map::class.java)

        assertEquals(selectedWinners.size, winners.size)
        winners.forEach { winner ->
            assertNotNull(winner["user_id"])
            assertNotNull(winner["prize_id"])
            assertNotNull(winner["ticket_number"])
            assertEquals(false, winner["prize_claimed"]) // Initially unclaimed
            assertTrue(winner["notification_sent"] as Boolean)
        }

        // Step 7: Verify winner notifications were sent
        TestUtils.waitForCondition(Duration.ofSeconds(10)) {
            val notifications = given()
                .header("Authorization", "Bearer $adminToken")
                .queryParam("raffleId", testRaffle.id)
                .`when`()
                .get("$baseUrl/api/admin/notifications/raffle-winners")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", Map::class.java)

            notifications.size == winners.size
        }

        // Step 8: Test winner prize claiming
        val firstWinner = winners.first()
        val winnerUserId = firstWinner["user_id"] as Int
        val winnerId = firstWinner["id"] as Int

        // Simulate winner claiming prize
        val claimRequest = mapOf(
            "winner_id" to winnerId,
            "verification_code" to "CLAIM123"
        )

        given()
            .header("Authorization", "Bearer $adminToken")
            .contentType(ContentType.JSON)
            .body(claimRequest)
            .`when`()
            .post("$baseUrl/api/admin/raffles/winners/$winnerId/claim")
            .then()
            .statusCode(200)

        // Step 9: Verify prize claim was recorded
        val claimedWinner = given()
            .header("Authorization", "Bearer $adminToken")
            .`when`()
            .get("$baseUrl/api/admin/raffles/winners/$winnerId")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()

        assertTrue(claimedWinner.getBoolean("prize_claimed"))
        assertNotNull(claimedWinner.getString("claim_date"))
    }

    @Test
    @DisplayName("Raffle participation edge cases and error handling")
    fun `should handle raffle participation edge cases properly`() {
        val baseUrl = baseUrl()

        val customerToken = TestUtils.authenticateUser("+525555001006", "123456", baseUrl)
        val customerInfo = TestUtils.getUserInfo("+525555001006", customerToken, baseUrl)

        // Test 1: Try to enter raffle without enough tickets
        val availableRaffles = TestUtils.getAvailableRaffles(customerToken, baseUrl)
        if (availableRaffles.isNotEmpty()) {
            val openRaffle = availableRaffles.first { it.status == "OPEN" }

            val insufficientTicketsRequest = RaffleEntryRequest(
                raffleId = openRaffle.id,
                userId = customerInfo.id,
                ticketsUsed = 1000, // More tickets than user has
                entryMethod = "MOBILE_APP"
            )

            given()
                .header("Authorization", "Bearer $customerToken")
                .contentType(ContentType.JSON)
                .body(insufficientTicketsRequest)
                .`when`()
                .post("$baseUrl/api/raffles/entries")
                .then()
                .statusCode(400) // Bad Request - insufficient tickets
        }

        // Test 2: Try to enter closed raffle
        val closedRaffles = given()
            .header("Authorization", "Bearer $customerToken")
            .queryParam("status", "CLOSED")
            .`when`()
            .get("$baseUrl/api/raffles")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList(".", RaffleInfo::class.java)

        if (closedRaffles.isNotEmpty()) {
            val closedRaffle = closedRaffles.first()

            val closedRaffleRequest = RaffleEntryRequest(
                raffleId = closedRaffle.id,
                userId = customerInfo.id,
                ticketsUsed = 1,
                entryMethod = "MOBILE_APP"
            )

            given()
                .header("Authorization", "Bearer $customerToken")
                .contentType(ContentType.JSON)
                .body(closedRaffleRequest)
                .`when`()
                .post("$baseUrl/api/raffles/entries")
                .then()
                .statusCode(409) // Conflict - raffle closed
        }

        // Test 3: Try to enter non-existent raffle
        val nonExistentRaffleRequest = RaffleEntryRequest(
            raffleId = 99999L,
            userId = customerInfo.id,
            ticketsUsed = 1,
            entryMethod = "MOBILE_APP"
        )

        given()
            .header("Authorization", "Bearer $customerToken")
            .contentType(ContentType.JSON)
            .body(nonExistentRaffleRequest)
            .`when`()
            .post("$baseUrl/api/raffles/entries")
            .then()
            .statusCode(404) // Not Found

        // Test 4: Try to use zero or negative tickets
        if (availableRaffles.isNotEmpty()) {
            val openRaffle = availableRaffles.first { it.status == "OPEN" }

            val invalidTicketsRequest = RaffleEntryRequest(
                raffleId = openRaffle.id,
                userId = customerInfo.id,
                ticketsUsed = 0,
                entryMethod = "MOBILE_APP"
            )

            given()
                .header("Authorization", "Bearer $customerToken")
                .contentType(ContentType.JSON)
                .body(invalidTicketsRequest)
                .`when`()
                .post("$baseUrl/api/raffles/entries")
                .then()
                .statusCode(400) // Bad Request - invalid ticket count
        }
    }
}