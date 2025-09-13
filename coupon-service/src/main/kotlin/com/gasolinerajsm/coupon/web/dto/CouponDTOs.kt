package com.gasolinerajsm.coupon.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Data Transfer Objects for Coupon API
 * Comprehensive DTOs with OpenAPI documentation and validation
 */

// Request DTOs
@Schema(
    name = "CouponPurchaseRequest",
    description = "Request payload for purchasing a new fuel coupon",
    example = """{
        "stationId": "987fcdeb-51a2-43d7-b456-426614174999",
        "amount": 500.00,
        "fuelType": "REGULAR",
        "paymentMethod": "CREDIT_CARD",
        "paymentToken": "tok_1234567890abcdef",
        "promoCode": "SAVE10",
        "autoRedeem": false,
        "notificationPreferences": {
            "email": true,
            "sms": true,
            "push": false
        }
    }"""
)
data class CouponPurchaseRequest(
    @field:NotNull(message = "Station ID is required")
    @Schema(
        description = "ID of the gas station where coupon will be redeemed",
        example = "987fcdeb-51a2-43d7-b456-426614174999",
        format = "uuid",
        required = true
    )
    val stationId: UUID,

    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "50.0", message = "Minimum coupon amount is $50 MXN")
    @field:DecimalMax(value = "10000.0", message = "Maximum coupon amount is $10,000 MXN")
    @field:Digits(integer = 5, fraction = 2, message = "Amount must have at most 5 digits and 2 decimal places")
    @Schema(
        description = "Coupon amount in Mexican Pesos (MXN)",
        example = "500.00",
        minimum = "50.00",
        maximum = "10000.00",
        multipleOf = 0.01,
        required = true
    )
    val amount: BigDecimal,

    @field:NotBlank(message = "Fuel type is required")
    @field:Pattern(
        regexp = "^(REGULAR|PREMIUM|DIESEL)$",
        message = "Fuel type must be REGULAR, PREMIUM, or DIESEL"
    )
    @Schema(
        description = "Type of fuel for the coupon",
        example = "REGULAR",
        allowableValues = ["REGULAR", "PREMIUM", "DIESEL"],
        required = true
    )
    val fuelType: String,

    @field:NotBlank(message = "Payment method is required")
    @field:Pattern(
        regexp = "^(CREDIT_CARD|DEBIT_CARD|BANK_TRANSFER|DIGITAL_WALLET|LOYALTY_POINTS)$",
        message = "Invalid payment method"
    )
    @Schema(
        description = "Payment method for the purchase",
        example = "CREDIT_CARD",
        allowableValues = ["CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "DIGITAL_WALLET", "LOYALTY_POINTS"],
        required = true
    )
    val paymentMethod: String,

    @field:NotBlank(message = "Payment token is required")
    @field:Size(max = 255, message = "Payment token must not exceed 255 characters")
    @Schema(
        description = "Secure payment token from payment processor",
        example = "tok_1234567890abcdef",
        maxLength = 255,
        required = true
    )
    val paymentToken: String,

    @field:Size(max = 50, message = "Promo code must not exceed 50 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9]*$",
        message = "Promo code can only contain uppercase letters and numbers"
    )
    @Schema(
        description = "Optional promotional code for discounts",
        example = "SAVE10",
        maxLength = 50,
        pattern = "^[A-Z0-9]*$"
    )
    val promoCode: String? = null,

    @Schema(
        description = "Whether to automatically redeem the coupon after purchase",
        example = "false",
        defaultValue = "false"
    )
    val autoRedeem: Boolean = false,

    @Schema(
        description = "User notification preferences for this purchase",
        implementation = NotificationPreferences::class
    )
    val notificationPreferences: NotificationPreferences? = null,

    @Schema(
        description = "Optional metadata for analytics and tracking",
        example = """{"source": "mobile_app", "campaign": "summer_promo"}"""
    )
    val metadata: Map<String, String>? = null
)

@Schema(
    name = "CouponRedemptionRequest",
    description = "Request payload for redeeming a coupon at a gas station",
    example = """{
        "qrCode": "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
        "stationId": "987fcdeb-51a2-43d7-b456-426614174999",
        "pumpNumber": 3,
        "attendantId": "emp_juan_perez_001",
        "fuelAmount": 22.22,
        "pricePerLiter": 22.50,
        "location": {
            "latitude": 19.4326,
            "longitude": -99.1332
        }
    }"""
)
data class CouponRedemptionRequest(
    @field:NotBlank(message = "QR code is required")
    @field:Pattern(
        regexp = "^QR_[A-Z0-9]{32}$",
        message = "Invalid QR code format"
    )
    @Schema(
        description = "Unique QR code from the coupon",
        example = "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
        pattern = "^QR_[A-Z0-9]{32}$",
        required = true
    )
    val qrCode: String,

    @field:NotNull(message = "Station ID is required")
    @Schema(
        description = "ID of the gas station where redemption is taking place",
        example = "987fcdeb-51a2-43d7-b456-426614174999",
        format = "uuid",
        required = true
    )
    val stationId: UUID,

    @field:Min(value = 1, message = "Pump number must be at least 1")
    @field:Max(value = 50, message = "Pump number must not exceed 50")
    @Schema(
        description = "Fuel pump number where redemption occurs",
        example = "3",
        minimum = "1",
        maximum = "50"
    )
    val pumpNumber: Int? = null,

    @field:Size(max = 100, message = "Attendant ID must not exceed 100 characters")
    @Schema(
        description = "ID of the gas station attendant processing the redemption",
        example = "emp_juan_perez_001",
        maxLength = 100
    )
    val attendantId: String? = null,

    @field:NotNull(message = "Fuel amount is required")
    @field:DecimalMin(value = "0.1", message = "Minimum fuel amount is 0.1 liters")
    @field:DecimalMax(value = "1000.0", message = "Maximum fuel amount is 1000 liters")
    @field:Digits(integer = 4, fraction = 2, message = "Fuel amount must have at most 4 digits and 2 decimal places")
    @Schema(
        description = "Amount of fuel dispensed in liters",
        example = "22.22",
        minimum = "0.1",
        maximum = "1000.0",
        required = true
    )
    val fuelAmount: BigDecimal,

    @field:NotNull(message = "Price per liter is required")
    @field:DecimalMin(value = "1.0", message = "Minimum price per liter is $1.00")
    @field:DecimalMax(value = "100.0", message = "Maximum price per liter is $100.00")
    @field:Digits(integer = 3, fraction = 2, message = "Price must have at most 3 digits and 2 decimal places")
    @Schema(
        description = "Current price per liter at the station",
        example = "22.50",
        minimum = "1.0",
        maximum = "100.0",
        required = true
    )
    val pricePerLiter: BigDecimal,

    @Schema(
        description = "GPS location of the redemption for verification",
        implementation = LocationData::class
    )
    val location: LocationData? = null,

    @Schema(
        description = "Additional redemption metadata",
        example = """{"temperature": "25C", "weather": "sunny"}"""
    )
    val metadata: Map<String, String>? = null
)

@Schema(
    name = "CouponCancellationRequest",
    description = "Request payload for cancelling an active coupon",
    example = """{
        "reason": "CHANGED_PLANS",
        "description": "Travel plans changed, no longer need fuel",
        "requestRefund": true
    }"""
)
data class CouponCancellationRequest(
    @field:NotBlank(message = "Cancellation reason is required")
    @field:Pattern(
        regexp = "^(CHANGED_PLANS|DUPLICATE_PURCHASE|TECHNICAL_ISSUE|PRICE_CHANGE|OTHER)$",
        message = "Invalid cancellation reason"
    )
    @Schema(
        description = "Reason for cancelling the coupon",
        example = "CHANGED_PLANS",
        allowableValues = ["CHANGED_PLANS", "DUPLICATE_PURCHASE", "TECHNICAL_ISSUE", "PRICE_CHANGE", "OTHER"],
        required = true
    )
    val reason: String,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(
        description = "Optional detailed description of the cancellation reason",
        example = "Travel plans changed, no longer need fuel",
        maxLength = 500
    )
    val description: String? = null,

    @Schema(
        description = "Whether to request a refund (subject to refund policy)",
        example = "true",
        defaultValue = "true"
    )
    val requestRefund: Boolean = true
)

// Response DTOs
@Schema(
    name = "CouponPurchaseResponse",
    description = "Response payload for successful coupon purchase"
)
data class CouponPurchaseResponse(
    @Schema(
        description = "Unique identifier for the purchased coupon",
        example = "123e4567-e89b-12d3-a456-426614174000",
        format = "uuid"
    )
    val couponId: UUID,

    @Schema(
        description = "Unique QR code for coupon redemption",
        example = "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6"
    )
    val qrCode: String,

    @Schema(
        description = "Coupon amount in MXN",
        example = "500.00"
    )
    val amount: BigDecimal,

    @Schema(
        description = "Type of fuel",
        example = "REGULAR"
    )
    val fuelType: String,

    @Schema(
        description = "Gas station ID where coupon can be redeemed",
        format = "uuid"
    )
    val stationId: UUID,

    @Schema(
        description = "Gas station name",
        example = "Gasolinera Central CDMX"
    )
    val stationName: String,

    @Schema(
        description = "Coupon expiration date and time",
        example = "2024-02-15T23:59:59Z",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val expiresAt: LocalDateTime,

    @Schema(
        description = "Payment transaction ID",
        example = "pay_1234567890"
    )
    val paymentId: String,

    @Schema(
        description = "System transaction ID",
        example = "TXN_ABC123DEF456"
    )
    val transactionId: String,

    @Schema(
        description = "Estimated liters based on current fuel price",
        example = "22.22"
    )
    val estimatedLiters: BigDecimal,

    @Schema(
        description = "Current price per liter at the station",
        example = "22.50"
    )
    val pricePerLiter: BigDecimal,

    @Schema(
        description = "Loyalty points earned from this purchase",
        example = "50"
    )
    val loyaltyPointsEarned: Int,

    @Schema(
        description = "Raffle tickets earned from this purchase",
        example = "5"
    )
    val raffleTicketsEarned: Int,

    @Schema(
        description = "Applied discount information",
        implementation = DiscountInfo::class
    )
    val discount: DiscountInfo? = null,

    @Schema(
        description = "Next steps for the user",
        example = "[\"Present QR code at selected gas station\", \"Scan code at fuel dispenser\"]"
    )
    val nextSteps: List<String> = emptyList(),

    @Schema(
        description = "Purchase timestamp",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val purchasedAt: LocalDateTime = LocalDateTime.now()
)

@Schema(
    name = "CouponRedemptionResponse",
    description = "Response payload for successful coupon redemption"
)
data class CouponRedemptionResponse(
    @Schema(
        description = "Unique redemption transaction ID",
        example = "red_123e4567-e89b-12d3-a456-426614174000",
        format = "uuid"
    )
    val redemptionId: UUID,

    @Schema(
        description = "Original coupon ID",
        format = "uuid"
    )
    val couponId: UUID,

    @Schema(
        description = "Station where redemption occurred",
        format = "uuid"
    )
    val stationId: UUID,

    @Schema(
        description = "Amount of fuel dispensed in liters",
        example = "22.22"
    )
    val fuelDispensed: BigDecimal,

    @Schema(
        description = "Type of fuel dispensed",
        example = "REGULAR"
    )
    val fuelType: String,

    @Schema(
        description = "Price per liter at redemption",
        example = "22.50"
    )
    val pricePerLiter: BigDecimal,

    @Schema(
        description = "Total cost of fuel dispensed",
        example = "499.95"
    )
    val totalCost: BigDecimal,

    @Schema(
        description = "Remaining coupon balance after redemption",
        example = "0.05"
    )
    val remainingBalance: BigDecimal,

    @Schema(
        description = "Number of raffle tickets generated",
        example = "5"
    )
    val ticketsGenerated: Int,

    @Schema(
        description = "Multiplier applied to ticket generation",
        example = "1.0"
    )
    val ticketMultiplier: Double,

    @Schema(
        description = "Loyalty points earned from redemption",
        example = "50"
    )
    val loyaltyPointsEarned: Int,

    @Schema(
        description = "Redemption timestamp",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val redeemedAt: LocalDateTime,

    @Schema(
        description = "Digital receipt information",
        implementation = DigitalReceipt::class
    )
    val receipt: DigitalReceipt,

    @Schema(
        description = "Generated raffle tickets",
        type = "array"
    )
    val raffleTickets: List<RaffleTicketInfo> = emptyList()
)

@Schema(
    name = "CouponDetailsResponse",
    description = "Detailed information about a specific coupon"
)
data class CouponDetailsResponse(
    @Schema(description = "Coupon unique identifier", format = "uuid")
    val id: UUID,

    @Schema(description = "User who owns the coupon", format = "uuid")
    val userId: UUID,

    @Schema(description = "Station where coupon can be redeemed", format = "uuid")
    val stationId: UUID,

    @Schema(description = "Coupon amount in MXN")
    val amount: BigDecimal,

    @Schema(description = "Fuel type")
    val fuelType: String,

    @Schema(description = "Current coupon status")
    val status: String,

    @Schema(description = "QR code for redemption")
    val qrCode: String,

    @Schema(description = "Creation timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val createdAt: LocalDateTime,

    @Schema(description = "Expiration timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val expiresAt: LocalDateTime,

    @Schema(description = "Redemption timestamp (if redeemed)", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val redeemedAt: LocalDateTime? = null,

    @Schema(description = "Days until expiration")
    val daysUntilExpiry: Long,

    @Schema(description = "Whether coupon can be redeemed")
    val canRedeem: Boolean,

    @Schema(description = "Station information")
    val stationInfo: StationInfo? = null,

    @Schema(description = "Payment information")
    val paymentInfo: PaymentInfo? = null,

    @Schema(description = "Redemption history")
    val redemptionHistory: List<RedemptionInfo> = emptyList()
)

@Schema(
    name = "CouponListResponse",
    description = "Paginated list of user coupons with summary statistics"
)
data class CouponListResponse(
    @Schema(description = "List of coupons for current page")
    val content: List<CouponSummary>,

    @Schema(description = "Total number of coupons")
    val totalElements: Long,

    @Schema(description = "Total number of pages")
    val totalPages: Int,

    @Schema(description = "Current page number (0-based)")
    val currentPage: Int,

    @Schema(description = "Number of items per page")
    val pageSize: Int,

    @Schema(description = "Whether there are more pages")
    val hasNext: Boolean,

    @Schema(description = "Whether there are previous pages")
    val hasPrevious: Boolean,

    @Schema(description = "Summary statistics")
    val summary: CouponSummaryStats
)

// Supporting DTOs
@Schema(name = "NotificationPreferences")
data class NotificationPreferences(
    @Schema(description = "Send email notifications")
    val email: Boolean = true,

    @Schema(description = "Send SMS notifications")
    val sms: Boolean = false,

    @Schema(description = "Send push notifications")
    val push: Boolean = true
)

@Schema(name = "LocationData")
data class LocationData(
    @Schema(description = "Latitude coordinate", example = "19.4326")
    val latitude: Double,

    @Schema(description = "Longitude coordinate", example = "-99.1332")
    val longitude: Double,

    @Schema(description = "Location accuracy in meters", example = "5.0")
    val accuracy: Double? = null
)

@Schema(name = "DiscountInfo")
data class DiscountInfo(
    @Schema(description = "Discount code applied")
    val code: String,

    @Schema(description = "Discount amount")
    val amount: BigDecimal,

    @Schema(description = "Discount type")
    val type: String,

    @Schema(description = "Discount description")
    val description: String
)

@Schema(name = "DigitalReceipt")
data class DigitalReceipt(
    @Schema(description = "Receipt number")
    val receiptNumber: String,

    @Schema(description = "Station name")
    val stationName: String,

    @Schema(description = "Attendant name")
    val attendant: String? = null,

    @Schema(description = "Pump number")
    val pumpNumber: Int? = null,

    @Schema(description = "Receipt timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val timestamp: LocalDateTime = LocalDateTime.now()
)

@Schema(name = "RaffleTicketInfo")
data class RaffleTicketInfo(
    @Schema(description = "Ticket ID")
    val ticketId: String,

    @Schema(description = "Raffle ID")
    val raffleId: String,

    @Schema(description = "Ticket number")
    val ticketNumber: String,

    @Schema(description = "Draw date", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val drawDate: LocalDateTime
)

@Schema(name = "StationInfo")
data class StationInfo(
    @Schema(description = "Station ID", format = "uuid")
    val id: UUID,

    @Schema(description = "Station name")
    val name: String,

    @Schema(description = "Station address")
    val address: String,

    @Schema(description = "Current fuel prices")
    val fuelPrices: Map<String, BigDecimal>,

    @Schema(description = "Station operating hours")
    val operatingHours: String? = null,

    @Schema(description = "Station amenities")
    val amenities: List<String> = emptyList()
)

@Schema(name = "PaymentInfo")
data class PaymentInfo(
    @Schema(description = "Payment ID")
    val paymentId: String,

    @Schema(description = "Payment method")
    val method: String,

    @Schema(description = "Payment status")
    val status: String,

    @Schema(description = "Payment timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val processedAt: LocalDateTime
)

@Schema(name = "RedemptionInfo")
data class RedemptionInfo(
    @Schema(description = "Redemption ID", format = "uuid")
    val id: UUID,

    @Schema(description = "Fuel amount dispensed")
    val fuelAmount: BigDecimal,

    @Schema(description = "Redemption timestamp", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val redeemedAt: LocalDateTime,

    @Schema(description = "Station where redeemed")
    val stationName: String
)

@Schema(name = "CouponSummary")
data class CouponSummary(
    @Schema(description = "Coupon ID", format = "uuid")
    val id: UUID,

    @Schema(description = "Coupon amount")
    val amount: BigDecimal,

    @Schema(description = "Fuel type")
    val fuelType: String,

    @Schema(description = "Coupon status")
    val status: String,

    @Schema(description = "QR code")
    val qrCode: String,

    @Schema(description = "Station name")
    val stationName: String,

    @Schema(description = "Creation date", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val createdAt: LocalDateTime,

    @Schema(description = "Expiration date", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val expiresAt: LocalDateTime,

    @Schema(description = "Days until expiry")
    val daysUntilExpiry: Long,

    @Schema(description = "Estimated liters")
    val estimatedLiters: BigDecimal,

    @Schema(description = "Can be redeemed")
    val canRedeem: Boolean
)

@Schema(name = "CouponSummaryStats")
data class CouponSummaryStats(
    @Schema(description = "Total value of all coupons")
    val totalValue: BigDecimal,

    @Schema(description = "Number of active coupons")
    val activeCoupons: Int,

    @Schema(description = "Number of coupons expiring soon")
    val expiringSoon: Int,

    @Schema(description = "Total savings achieved")
    val totalSavings: BigDecimal,

    @Schema(description = "Most used fuel type")
    val preferredFuelType: String? = null,

    @Schema(description = "Most used station")
    val preferredStation: String? = null
)

// Additional response DTOs
data class CouponCancellationResponse(
    val success: Boolean,
    val message: String,
    val refundInfo: RefundInfo? = null
)

data class RefundInfo(
    val refundId: String,
    val refundAmount: BigDecimal,
    val refundMethod: String,
    val estimatedProcessingTime: String,
    val refundStatus: String
)

data class QRCodeRegenerationResponse(
    val couponId: UUID,
    val newQrCode: String,
    val previousQrCode: String,
    val regeneratedAt: LocalDateTime,
    val regenerationCount: Int,
    val maxRegenerations: Int
)

data class CouponStatisticsResponse(
    val userId: UUID,
    val totalCoupons: Int,
    val totalSpent: BigDecimal,
    val totalSaved: BigDecimal,
    val redemptionRate: Double,
    val averageCouponValue: BigDecimal,
    val fuelTypeBreakdown: Map<String, Int>,
    val monthlyTrends: List<MonthlyTrend>,
    val topStations: List<StationUsage>
)

data class MonthlyTrend(
    val month: String,
    val couponsCount: Int,
    val totalSpent: BigDecimal,
    val redemptionRate: Double
)

data class StationUsage(
    val stationId: UUID,
    val stationName: String,
    val couponsCount: Int,
    val totalSpent: BigDecimal
)

data class SystemCouponStatisticsResponse(
    val totalCoupons: Long,
    val totalRevenue: BigDecimal,
    val totalUsers: Long,
    val averageTransactionValue: BigDecimal,
    val redemptionRate: Double,
    val topFuelTypes: List<FuelTypeStats>,
    val topStations: List<StationStats>,
    val dailyTrends: List<DailyTrend>,
    val geographicDistribution: List<RegionStats>
)

data class FuelTypeStats(
    val fuelType: String,
    val count: Long,
    val revenue: BigDecimal,
    val percentage: Double
)

data class StationStats(
    val stationId: UUID,
    val stationName: String,
    val couponsCount: Long,
    val revenue: BigDecimal,
    val averageTransactionValue: BigDecimal
)

data class DailyTrend(
    val date: String,
    val couponsCount: Long,
    val revenue: BigDecimal,
    val uniqueUsers: Long
)

data class RegionStats(
    val region: String,
    val couponsCount: Long,
    val revenue: BigDecimal,
    val topStations: List<String>
)