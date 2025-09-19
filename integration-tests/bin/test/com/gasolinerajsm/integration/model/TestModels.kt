package com.gasolinerajsm.integration.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Test data models for integration tests
 */

// Auth Service Models
data class LoginRequest(
    @JsonProperty("phone_number") val phoneNumber: String,
    @JsonProperty("otp_code") val otpCode: String
)

data class AuthResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Long,
    val user: UserInfo
)

data class UserInfo(
    val id: Long,
    @JsonProperty("phone_number") val phoneNumber: String,
    @JsonProperty("first_name") val firstName: String,
    @JsonProperty("last_name") val lastName: String,
    val role: String,
    @JsonProperty("is_active") val isActive: Boolean,
    @JsonProperty("is_phone_verified") val isPhoneVerified: Boolean
)

data class OtpRequest(
    @JsonProperty("phone_number") val phoneNumber: String,
    val purpose: String = "LOGIN"
)

data class OtpResponse(
    @JsonProperty("session_token") val sessionToken: String,
    @JsonProperty("expires_at") val expiresAt: LocalDateTime,
    val message: String
)

// Station Service Models
data class StationInfo(
    val id: Long,
    val name: String,
    val code: String,
    val address: String,
    val city: String,
    val state: String,
    val status: String,
    @JsonProperty("station_type") val stationType: String
)

// Coupon Service Models
data class CampaignInfo(
    val id: Long,
    val name: String,
    @JsonProperty("campaign_code") val campaignCode: String,
    @JsonProperty("discount_type") val discountType: String,
    @JsonProperty("discount_value") val discountValue: Double,
    @JsonProperty("is_active") val isActive: Boolean
)

data class CouponInfo(
    val id: Long,
    @JsonProperty("coupon_code") val couponCode: String,
    @JsonProperty("campaign_id") val campaignId: Long,
    val status: String,
    @JsonProperty("discount_amount") val discountAmount: Double,
    @JsonProperty("raffle_tickets") val raffleTickets: Int,
    @JsonProperty("valid_from") val validFrom: LocalDateTime,
    @JsonProperty("valid_until") val validUntil: LocalDateTime
)

data class CouponValidationRequest(
    @JsonProperty("coupon_code") val couponCode: String,
    @JsonProperty("station_id") val stationId: Long,
    @JsonProperty("user_id") val userId: Long
)

data class CouponValidationResponse(
    val valid: Boolean,
    val coupon: CouponInfo?,
    val campaign: CampaignInfo?,
    val message: String?
)

// Redemption Service Models
data class RedemptionRequest(
    @JsonProperty("user_id") val userId: Long,
    @JsonProperty("station_id") val stationId: Long,
    @JsonProperty("employee_id") val employeeId: Long,
    @JsonProperty("coupon_code") val couponCode: String,
    @JsonProperty("fuel_type") val fuelType: String,
    @JsonProperty("fuel_quantity") val fuelQuantity: Double,
    @JsonProperty("fuel_price_per_unit") val fuelPricePerUnit: Double,
    @JsonProperty("purchase_amount") val purchaseAmount: Double,
    @JsonProperty("payment_method") val paymentMethod: String
)

data class RedemptionResponse(
    val id: Long,
    @JsonProperty("transaction_reference") val transactionReference: String,
    val status: String,
    @JsonProperty("purchase_amount") val purchaseAmount: Double,
    @JsonProperty("discount_amount") val discountAmount: Double,
    @JsonProperty("final_amount") val finalAmount: Double,
    @JsonProperty("raffle_tickets_earned") val raffleTicketsEarned: Int
)

// Raffle Service Models
data class RaffleInfo(
    val id: Long,
    val name: String,
    val description: String,
    @JsonProperty("raffle_type") val raffleType: String,
    val status: String,
    @JsonProperty("registration_start") val registrationStart: LocalDateTime,
    @JsonProperty("registration_end") val registrationEnd: LocalDateTime,
    @JsonProperty("draw_date") val drawDate: LocalDateTime,
    @JsonProperty("current_participants") val currentParticipants: Int,
    @JsonProperty("max_participants") val maxParticipants: Int?
)

data class RaffleEntryRequest(
    @JsonProperty("raffle_id") val raffleId: Long,
    @JsonProperty("user_id") val userId: Long,
    @JsonProperty("tickets_used") val ticketsUsed: Int,
    @JsonProperty("entry_method") val entryMethod: String = "MOBILE_APP"
)

data class RaffleEntryResponse(
    val id: Long,
    @JsonProperty("raffle_id") val raffleId: Long,
    @JsonProperty("user_id") val userId: Long,
    @JsonProperty("tickets_used") val ticketsUsed: Int,
    @JsonProperty("entry_date") val entryDate: LocalDateTime,
    @JsonProperty("transaction_reference") val transactionReference: String
)

// Ad Engine Models
data class AdInfo(
    val id: Long,
    val title: String,
    val description: String,
    @JsonProperty("ad_type") val adType: String,
    @JsonProperty("content_url") val contentUrl: String,
    @JsonProperty("duration_seconds") val durationSeconds: Int?,
    @JsonProperty("ticket_multiplier") val ticketMultiplier: Int,
    val status: String
)

data class AdEngagementRequest(
    @JsonProperty("advertisement_id") val advertisementId: Long,
    @JsonProperty("user_id") val userId: Long,
    @JsonProperty("session_id") val sessionId: String,
    @JsonProperty("engagement_type") val engagementType: String,
    @JsonProperty("duration_seconds") val durationSeconds: Int?,
    @JsonProperty("completion_percentage") val completionPercentage: Int,
    @JsonProperty("station_id") val stationId: Long?
)

data class AdEngagementResponse(
    val id: Long,
    @JsonProperty("advertisement_id") val advertisementId: Long,
    @JsonProperty("user_id") val userId: Long,
    val status: String,
    @JsonProperty("tickets_multiplied") val ticketsMultiplied: Int,
    @JsonProperty("multiplier_applied") val multiplierApplied: Int
)

// Common Response Models
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val errors: List<String>?
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime,
    val path: String
)