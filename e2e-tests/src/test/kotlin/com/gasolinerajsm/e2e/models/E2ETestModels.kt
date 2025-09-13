package com.gasolinerajsm.e2e.models

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Data Models for End-to-End Tests
 * Contains all request/response models and test data structures
 */

// Test User Data
data class TestUser(
    val email: String = "test.user.${UUID.randomUUID()}@gasolinera-test.com",
    val phone: String = "555${Random().nextInt(1000000, 9999999)}",
    val firstName: String = "Test",
    val lastName: String = "User",
    val password: String = "TestPassword123!",
    var id: UUID = UUID.randomUUID(),
    var accessToken: String = "",
    var refreshToken: String = "",
    var couponId: UUID = UUID.randomUUID(),
    var qrCode: String = "",
    var redemptionId: UUID = UUID.randomUUID(),
    var ticketsGenerated: Int = 0,
    var raffleTicketIds: List<UUID> = emptyList()
)

// Test Station Data
data class TestStation(
    var id: UUID = UUID.randomUUID(),
    val name: String = "Test Gasolinera Central",
    val address: String = "Av. Test 123, CDMX",
    val latitude: Double = 19.4326,
    val longitude: Double = -99.1332,
    val fuelPrices: Map<String, BigDecimal> = mapOf(
        "REGULAR" to BigDecimal("22.50"),
        "PREMIUM" to BigDecimal("24.80"),
        "DIESEL" to BigDecimal("23.20")
    )
)

// Test Raffle Data
data class TestRaffle(
    var id: UUID = UUID.randomUUID(),
    val name: String = "Test Monthly Raffle",
    val description: String = "Test raffle for E2E testing",
    val prizeDescription: String = "Test Prize - $10,000 Cash",
    val prizeValue: BigDecimal = BigDecimal("10000.00"),
    val startDate: LocalDateTime = LocalDateTime.now().minusDays(1),
    val endDate: LocalDateTime = LocalDateTime.now().plusDays(30),
    val maxParticipants: Int = 10000,
    val status: String = "ACTIVE"
)

// Test Coupon Data
data class TestCoupon(
    val id: UUID = UUID.randomUUID(),
    val qrCode: String = "QR_TEST_${UUID.randomUUID().toString().replace("-", "").uppercase()}",
    val amount: BigDecimal = BigDecimal("500.00"),
    val fuelType: String = "REGULAR",
    val status: String = "ACTIVE",
    val expiresAt: LocalDateTime = LocalDateTime.now().plusDays(30)
)

// Request Models
data class UserRegistrationRequest(
    val email: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val password: String
)

data class UserLoginRequest(
    val identifier: String,
    val password: String
)

data class CouponPurchaseRequest(
    val stationId: UUID,
    val amount: BigDecimal,
    val fuelType: String,
    val paymentMethod: String,
    val paymentToken: String
)

data class CouponRedemptionRequest(
    val qrCode: String,
    val stationId: UUID,
    val fuelAmount: BigDecimal,
    val pricePerLiter: BigDecimal
)

data class RaffleParticipationRequest(
    val raffleId: UUID,
    val ticketsToUse: Int
)

// Response Models
data class UserRegistrationResponse(
    val userId: UUID,
    val email: String,
    val message: String,
    val emailVerificationRequired: Boolean = true
)

data class UserLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val user: UserInfo
)

data class UserInfo(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean
)

data class StationSearchResponse(
    val stations: List<StationInfo>,
    val totalCount: Int,
    val searchRadius: Double,
    val centerLatitude: Double,
    val centerLongitude: Double
)

data class StationInfo(
    val id: UUID,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double,
    val fuelPrices: Map<String, BigDecimal>,
    val isOperational: Boolean,
    val rating: Double,
    val amenities: List<String>
)

data class CouponPurchaseResponse(
    val couponId: UUID,
    val qrCode: String,
    val amount: BigDecimal,
    val fuelType: String,
    val expiresAt: LocalDateTime,
    val paymentId: UUID,
    val transactionId: String
)

data class CouponRedemptionResponse(
    val redemptionId: UUID,
    val couponId: UUID,
    val ticketsGenerated: Int,
    val multiplier: Double,
    val fuelDispensed: BigDecimal,
    val totalCost: BigDecimal,
    val redeemedAt: LocalDateTime
)

data class ActiveRafflesResponse(
    val raffles: List<RaffleInfo>,
    val totalCount: Int
)

data class RaffleInfo(
    val id: UUID,
    val name: String,
    val description: String,
    val prizeDescription: String,
    val prizeValue: BigDecimal,
    val endDate: LocalDateTime,
    val participantCount: Int,
    val maxParticipants: Int,
    val ticketPrice: Int,
    val status: String
)

data class RaffleParticipationResponse(
    val participationId: UUID,
    val raffleId: UUID,
    val ticketIds: List<UUID>,
    val ticketsUsed: Int,
    val participatedAt: LocalDateTime
)

data class UserDashboardResponse(
    val userId: UUID,
    val totalCoupons: Int,
    val activeCoupons: Int,
    val redeemedCoupons: Int,
    val expiredCoupons: Int,
    val totalSpent: BigDecimal,
    val totalSaved: BigDecimal,
    val totalTickets: Int,
    val activeRaffleParticipations: Int,
    val recentTransactions: List<TransactionInfo>,
    val upcomingExpirations: List<CouponInfo>
)

data class TransactionInfo(
    val id: UUID,
    val type: String,
    val amount: BigDecimal,
    val description: String,
    val timestamp: LocalDateTime,
    val status: String
)

data class CouponInfo(
    val id: UUID,
    val amount: BigDecimal,
    val fuelType: String,
    val status: String,
    val expiresAt: LocalDateTime,
    val stationName: String
)

data class UserProfileResponse(
    val id: UUID,
    val email: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean,
    val createdAt: LocalDateTime,
    val lastLoginAt: LocalDateTime?
)

data class CouponDetailsResponse(
    val id: UUID,
    val userId: UUID,
    val stationId: UUID,
    val amount: BigDecimal,
    val fuelType: String,
    val status: String,
    val qrCode: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val redeemedAt: LocalDateTime?,
    val stationInfo: StationInfo?
)

data class RedemptionDetailsResponse(
    val id: UUID,
    val couponId: UUID,
    val userId: UUID,
    val stationId: UUID,
    val amount: BigDecimal,
    val fuelAmount: BigDecimal,
    val pricePerLiter: BigDecimal,
    val ticketsGenerated: Int,
    val multiplier: Double,
    val redeemedAt: LocalDateTime
)

data class UserRaffleTicketsResponse(
    val raffleId: UUID,
    val tickets: List<RaffleTicketInfo>,
    val totalTickets: Int
)

data class RaffleTicketInfo(
    val id: UUID,
    val ticketNumber: String,
    val isWinner: Boolean,
    val createdAt: LocalDateTime
)

data class UserActivityResponse(
    val userId: UUID,
    val totalCouponsRedeemed: Int,
    val totalRaffleParticipations: Int,
    val totalSpent: BigDecimal,
    val averageTransactionValue: BigDecimal,
    val favoriteStation: StationInfo?,
    val preferredFuelType: String,
    val activityScore: Int
)

data class UserAnalyticsSummaryResponse(
    val userId: UUID,
    val totalTransactions: Int,
    val totalSpent: BigDecimal,
    val totalSaved: BigDecimal,
    val averageMonthlySpend: BigDecimal,
    val loyaltyPoints: Int,
    val membershipTier: String,
    val joinDate: LocalDateTime
)

data class SystemHealthResponse(
    val status: String,
    val timestamp: LocalDateTime,
    val services: List<ServiceHealthInfo>,
    val overallHealth: String,
    val version: String
)

data class ServiceHealthInfo(
    val name: String,
    val status: String,
    val responseTime: Long,
    val lastChecked: LocalDateTime,
    val details: Map<String, Any>
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime,
    val path: String,
    val details: Map<String, Any>? = null
)

// Result Models for Test Flow
data class RegistrationResult(
    val success: Boolean,
    val userId: UUID = UUID.randomUUID(),
    val message: String = "",
    val error: String? = null
)

data class LoginResult(
    val success: Boolean,
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 3600,
    val error: String? = null
)

data class StationResult(
    val success: Boolean,
    val stationId: UUID = UUID.randomUUID(),
    val stationName: String = "",
    val fuelPrices: Map<String, BigDecimal> = emptyMap(),
    val error: String? = null
)

data class CouponResult(
    val success: Boolean,
    val couponId: UUID = UUID.randomUUID(),
    val qrCode: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val expiresAt: LocalDateTime = LocalDateTime.now(),
    val error: String? = null
)

data class RedemptionResult(
    val success: Boolean,
    val redemptionId: UUID = UUID.randomUUID(),
    val ticketsGenerated: Int = 0,
    val multiplier: Double = 1.0,
    val error: String? = null
)

data class RaffleResult(
    val success: Boolean,
    val raffleId: UUID = UUID.randomUUID(),
    val ticketIds: List<UUID> = emptyList(),
    val participationId: UUID = UUID.randomUUID(),
    val error: String? = null
)

data class DashboardResult(
    val success: Boolean,
    val totalCoupons: Int = 0,
    val totalTickets: Int = 0,
    val totalSpent: BigDecimal = BigDecimal.ZERO,
    val error: String? = null
)

data class UserFlowResult(
    val success: Boolean,
    val userId: UUID = UUID.randomUUID(),
    val couponId: UUID = UUID.randomUUID(),
    val redemptionId: UUID = UUID.randomUUID(),
    val ticketIds: List<UUID> = emptyList(),
    val totalDuration: Long = 0,
    val error: String? = null
)

data class PerformanceResult(
    val operationIndex: Int,
    val duration: Long,
    val success: Boolean,
    val error: String? = null
)

// Test Configuration Models
data class E2ETestConfig(
    val baseUrl: String,
    val timeoutSeconds: Int = 30,
    val retryAttempts: Int = 3,
    val concurrentUsers: Int = 5,
    val performanceTestOperations: Int = 50,
    val enablePerformanceTests: Boolean = true,
    val enableSecurityTests: Boolean = true,
    val enableLoadTests: Boolean = true
)

// Test Scenario Models
data class TestScenario(
    val name: String,
    val description: String,
    val steps: List<TestStep>,
    val expectedDuration: Long,
    val criticalPath: Boolean = false
)

data class TestStep(
    val name: String,
    val description: String,
    val endpoint: String,
    val method: String,
    val expectedStatus: Int,
    val timeout: Long = 30000,
    val retryable: Boolean = false
)

// Performance Test Models
data class PerformanceTestResult(
    val scenarioName: String,
    val totalOperations: Int,
    val successfulOperations: Int,
    val failedOperations: Int,
    val averageResponseTime: Double,
    val p95ResponseTime: Long,
    val p99ResponseTime: Long,
    val throughput: Double,
    val errorRate: Double,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)

// Load Test Models
data class LoadTestConfig(
    val concurrentUsers: Int,
    val rampUpTime: Long,
    val testDuration: Long,
    val targetThroughput: Double,
    val maxResponseTime: Long
)

data class LoadTestResult(
    val config: LoadTestConfig,
    val actualThroughput: Double,
    val averageResponseTime: Double,
    val errorRate: Double,
    val resourceUtilization: ResourceUtilization,
    val passed: Boolean
)

data class ResourceUtilization(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskUsage: Double,
    val networkUsage: Double
)

// Security Test Models
data class SecurityTestResult(
    val testName: String,
    val passed: Boolean,
    val vulnerabilities: List<SecurityVulnerability>,
    val riskLevel: String
)

data class SecurityVulnerability(
    val type: String,
    val severity: String,
    val description: String,
    val endpoint: String,
    val recommendation: String
)