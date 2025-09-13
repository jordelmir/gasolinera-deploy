package com.gasolinerajsm.coupon.web

import com.gasolinerajsm.coupon.application.*
import com.gasolinerajsm.coupon.web.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Coupon Controller with comprehensive OpenAPI documentation
 * Handles coupon creation, management, redemption, and analytics
 */
@RestController
@RequestMapping("/api/v1/coupons")
@Tag(
    name = "Coupons",
    description = "🎫 Digital coupon system for fuel purchases, QR code generation, redemption tracking, and loyalty rewards"
)
class CouponController(
    private val couponUseCase: CouponUseCase
) {

    @Operation(
        summary = "Purchase a new fuel coupon",
        description = """
            Creates a new digital fuel coupon with QR code for redemption at gas stations.

            ## Purchase Process:
            1. 💳 **Payment Processing** - Secure payment via integrated gateway
            2. 🎫 **Coupon Generation** - Digital coupon with unique QR code
            3. 📱 **QR Code Creation** - Scannable code for station redemption
            4. 📧 **Confirmation** - Email/SMS confirmation with coupon details
            5. 💾 **Blockchain Record** - Immutable transaction record

            ## Coupon Features:
            - ✅ **Secure QR Codes** - Unique, tamper-proof identification
            - ⏰ **Flexible Expiry** - 30-day default validity period
            - ⛽ **Fuel Type Specific** - Regular, Premium, or Diesel
            - 🏪 **Station Specific** - Valid at selected gas station
            - 🎰 **Raffle Integration** - Automatic raffle ticket generation

            ## Payment Methods:
            - 💳 Credit/Debit Cards (Visa, MasterCard, AMEX)
            - 🏦 Bank Transfers (SPEI, Wire Transfer)
            - 📱 Digital Wallets (PayPal, Apple Pay, Google Pay)
            - 💰 Loyalty Points Redemption

            ## Business Rules:
            - Minimum purchase: $50 MXN
            - Maximum purchase: $10,000 MXN per transaction
            - Daily limit: $50,000 MXN per user
            - Station must be active and operational
            - User must have verified payment method
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "✅ Coupon purchased successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CouponPurchaseResponse::class),
                    examples = [ExampleObject(
                        name = "Successful Purchase",
                        value = """{
                            "couponId": "123e4567-e89b-12d3-a456-426614174000",
                            "qrCode": "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
                            "amount": 500.00,
                            "fuelType": "REGULAR",
                            "stationId": "987fcdeb-51a2-43d7-b456-426614174999",
                            "stationName": "Gasolinera Central CDMX",
                            "expiresAt": "2024-02-15T23:59:59Z",
                            "paymentId": "pay_1234567890",
                            "transactionId": "TXN_ABC123DEF456",
                            "estimatedLiters": 22.22,
                            "pricePerLiter": 22.50,
                            "loyaltyPointsEarned": 50,
                            "raffleTicketsEarned": 5,
                            "nextSteps": [
                                "Present QR code at selected gas station",
                                "Scan code at fuel dispenser",
                                "Enjoy your fuel and earn raffle tickets!"
                            ]
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "❌ Invalid purchase request",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Invalid Amount",
                        value = """{
                            "error": "INVALID_AMOUNT",
                            "message": "Purchase amount must be between $50 and $10,000 MXN",
                            "details": {
                                "minAmount": 50.00,
                                "maxAmount": 10000.00,
                                "providedAmount": 25.00,
                                "currency": "MXN"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "402",
                description = "💳 Payment failed",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Payment Declined",
                        value = """{
                            "error": "PAYMENT_DECLINED",
                            "message": "Payment was declined by your bank",
                            "details": {
                                "paymentMethod": "CREDIT_CARD",
                                "declineReason": "INSUFFICIENT_FUNDS",
                                "suggestion": "Please try a different payment method or contact your bank"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "429",
                description = "⏰ Rate limit exceeded",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Daily Limit Exceeded",
                        value = """{
                            "error": "DAILY_LIMIT_EXCEEDED",
                            "message": "You have exceeded your daily purchase limit",
                            "details": {
                                "dailyLimit": 50000.00,
                                "currentSpent": 48500.00,
                                "attemptedAmount": 2000.00,
                                "resetTime": "2024-01-16T00:00:00Z"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/purchase")
    @PreAuthorize("hasRole('USER')")
    fun purchaseCoupon(
        @Parameter(
            description = "Coupon purchase details including amount, fuel type, station, and payment information",
            required = true,
            schema = Schema(implementation = CouponPurchaseRequest::class)
        )
        @Valid @RequestBody request: CouponPurchaseRequest
    ): ResponseEntity<CouponPurchaseResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get user's coupons with filtering and pagination",
        description = """
            Retrieves a paginated list of coupons belonging to the authenticated user.

            ## Filtering Options:
            - 🎫 **Status**: ACTIVE, REDEEMED, EXPIRED, CANCELLED
            - ⛽ **Fuel Type**: REGULAR, PREMIUM, DIESEL
            - 🏪 **Station**: Filter by specific gas station
            - 📅 **Date Range**: Created between specific dates
            - 💰 **Amount Range**: Filter by coupon value

            ## Sorting Options:
            - 📅 **Creation Date** (newest/oldest first)
            - 💰 **Amount** (highest/lowest first)
            - ⏰ **Expiration Date** (expiring soon first)
            - 🎫 **Status** (active first)

            ## Response Features:
            - 📊 **Pagination** - Efficient data loading
            - 🔍 **Search** - Find specific coupons
            - 📈 **Statistics** - Summary of coupon usage
            - 🎯 **Recommendations** - Suggested actions
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "✅ Coupons retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CouponListResponse::class),
                    examples = [ExampleObject(
                        name = "Coupon List",
                        value = """{
                            "content": [
                                {
                                    "id": "123e4567-e89b-12d3-a456-426614174000",
                                    "amount": 500.00,
                                    "fuelType": "REGULAR",
                                    "status": "ACTIVE",
                                    "qrCode": "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
                                    "stationName": "Gasolinera Central",
                                    "createdAt": "2024-01-15T10:00:00Z",
                                    "expiresAt": "2024-02-15T23:59:59Z",
                                    "daysUntilExpiry": 31,
                                    "estimatedLiters": 22.22,
                                    "canRedeem": true
                                }
                            ],
                            "totalElements": 25,
                            "totalPages": 3,
                            "currentPage": 0,
                            "pageSize": 10,
                            "hasNext": true,
                            "hasPrevious": false,
                            "summary": {
                                "totalValue": 12500.00,
                                "activeCoupons": 15,
                                "expiringSoon": 3,
                                "totalSavings": 2500.00
                            }
                        }"""
                    )]
                )]
            )
        ]
    )
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    fun getUserCoupons(
        @Parameter(description = "Filter by coupon status")
        @RequestParam(required = false) status: String?,

        @Parameter(description = "Filter by fuel type")
        @RequestParam(required = false) fuelType: String?,

        @Parameter(description = "Filter by station ID")
        @RequestParam(required = false) stationId: UUID?,

        @Parameter(description = "Search in coupon details")
        @RequestParam(required = false) search: String?,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 10, sort = ["createdAt"], direction = org.springframework.data.domain.Sort.Direction.DESC)
        pageable: Pageable
    ): ResponseEntity<CouponListResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get detailed coupon information",
        description = """
            Retrieves comprehensive details for a specific coupon.

            ## Returned Information:
            - 🎫 **Basic Details** - Amount, fuel type, status, dates
            - 📱 **QR Code** - High-resolution QR code for redemption
            - 🏪 **Station Info** - Gas station details and location
            - 💳 **Payment Info** - Transaction details and receipt
            - 🎰 **Raffle Info** - Associated raffle tickets
            - 📊 **Usage Stats** - Redemption history and analytics

            ## Security:
            - ✅ **Ownership Validation** - Users can only view their own coupons
            - ✅ **Admin Override** - Admins can view any coupon
            - ✅ **Audit Logging** - All access attempts logged

            ## QR Code Features:
            - 🔒 **Tamper-Proof** - Cryptographically signed
            - ⏰ **Time-Sensitive** - Includes expiration validation
            - 🏪 **Station-Specific** - Tied to specific gas station
            - 📱 **Mobile Optimized** - Perfect for smartphone scanning
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @GetMapping("/{couponId}")
    @PreAuthorize("hasRole('USER')")
    fun getCouponDetails(
        @Parameter(
            description = "Unique coupon identifier",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @PathVariable couponId: UUID
    ): ResponseEntity<CouponDetailsResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Redeem coupon at gas station",
        description = """
            Redeems a digital coupon at a gas station using QR code scanning.

            ## Redemption Process:
            1. 📱 **QR Code Scan** - Station scans customer's QR code
            2. ✅ **Validation** - System validates coupon authenticity
            3. ⛽ **Fuel Dispensing** - Authorized fuel amount dispensed
            4. 🎫 **Ticket Generation** - Raffle tickets automatically generated
            5. 📧 **Confirmation** - Receipt and confirmation sent
            6. 🏆 **Rewards** - Loyalty points and bonuses applied

            ## Validation Checks:
            - ✅ **QR Code Authenticity** - Cryptographic signature validation
            - ✅ **Expiration Status** - Not expired or used
            - ✅ **Station Match** - Redeemed at correct station
            - ✅ **Amount Validation** - Sufficient coupon balance
            - ✅ **User Verification** - Valid user account

            ## Raffle Integration:
            - 🎰 **Automatic Tickets** - Based on fuel amount
            - 🎯 **Multipliers** - Bonus tickets for premium fuel
            - 📢 **Ad Engagement** - Extra tickets for viewing ads
            - 🏆 **Loyalty Bonuses** - VIP member multipliers

            ## Security Features:
            - 🔒 **One-Time Use** - Prevents double redemption
            - 📍 **Geolocation** - Station location verification
            - ⏰ **Time Windows** - Prevents replay attacks
            - 🔐 **Encryption** - End-to-end encrypted transactions
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "✅ Coupon redeemed successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CouponRedemptionResponse::class),
                    examples = [ExampleObject(
                        name = "Successful Redemption",
                        value = """{
                            "redemptionId": "red_123e4567-e89b-12d3-a456-426614174000",
                            "couponId": "123e4567-e89b-12d3-a456-426614174000",
                            "stationId": "987fcdeb-51a2-43d7-b456-426614174999",
                            "fuelDispensed": 22.22,
                            "fuelType": "REGULAR",
                            "pricePerLiter": 22.50,
                            "totalCost": 499.95,
                            "remainingBalance": 0.05,
                            "ticketsGenerated": 5,
                            "ticketMultiplier": 1.0,
                            "loyaltyPointsEarned": 50,
                            "redeemedAt": "2024-01-15T14:30:00Z",
                            "receipt": {
                                "receiptNumber": "RCP_20240115_143000_001",
                                "stationName": "Gasolinera Central CDMX",
                                "attendant": "Juan Pérez",
                                "pumpNumber": 3
                            },
                            "raffleTickets": [
                                {
                                    "ticketId": "TKT_001",
                                    "raffleId": "raffle_monthly_jan2024",
                                    "ticketNumber": "T20240115001",
                                    "drawDate": "2024-01-31T20:00:00Z"
                                }
                            ]
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "❌ Invalid QR code or redemption data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Invalid QR Code",
                        value = """{
                            "error": "INVALID_QR_CODE",
                            "message": "The provided QR code is invalid or corrupted",
                            "details": {
                                "qrCode": "QR_INVALID123",
                                "validationErrors": [
                                    "Invalid format",
                                    "Checksum mismatch"
                                ],
                                "suggestion": "Please ensure the QR code is clearly visible and not damaged"
                            },
                            "timestamp": "2024-01-15T14:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409",
                description = "⚠️ Coupon already redeemed",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Already Redeemed",
                        value = """{
                            "error": "COUPON_ALREADY_REDEEMED",
                            "message": "This coupon has already been redeemed",
                            "details": {
                                "originalRedemptionDate": "2024-01-10T12:00:00Z",
                                "originalStation": "Gasolinera Norte",
                                "redemptionId": "red_previous_123"
                            },
                            "timestamp": "2024-01-15T14:30:00Z"
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "410",
                description = "⏰ Coupon expired",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Expired Coupon",
                        value = """{
                            "error": "COUPON_EXPIRED",
                            "message": "This coupon has expired and cannot be redeemed",
                            "details": {
                                "expirationDate": "2024-01-10T23:59:59Z",
                                "daysExpired": 5,
                                "suggestion": "Purchase a new coupon to continue enjoying our services"
                            },
                            "timestamp": "2024-01-15T14:30:00Z"
                        }"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/redeem")
    @PreAuthorize("hasRole('USER') or hasRole('STATION_OPERATOR')")
    fun redeemCoupon(
        @Parameter(
            description = "Coupon redemption details including QR code and station information",
            required = true,
            schema = Schema(implementation = CouponRedemptionRequest::class)
        )
        @Valid @RequestBody request: CouponRedemptionRequest
    ): ResponseEntity<CouponRedemptionResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Cancel an active coupon",
        description = """
            Cancels an active coupon and processes refund if applicable.

            ## Cancellation Rules:
            - ✅ **Active Coupons Only** - Cannot cancel redeemed/expired coupons
            - ⏰ **Time Limits** - Must cancel within 24 hours of purchase
            - 💰 **Refund Policy** - Full refund for cancellations within 2 hours
            - 🎫 **Partial Refund** - 90% refund for cancellations within 24 hours
            - ❌ **No Refund** - After 24 hours, no refund available

            ## Refund Process:
            1. 🔍 **Eligibility Check** - Validate cancellation eligibility
            2. 💳 **Payment Reversal** - Process refund to original payment method
            3. 🎫 **Coupon Invalidation** - Mark coupon as cancelled
            4. 📧 **Confirmation** - Send cancellation and refund confirmation
            5. 📊 **Analytics Update** - Update user and system statistics

            ## Business Impact:
            - 🏆 **Loyalty Points** - Deducted if previously awarded
            - 🎰 **Raffle Tickets** - Removed if not yet used in draws
            - 📈 **Statistics** - Cancellation tracked for analytics
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PostMapping("/{couponId}/cancel")
    @PreAuthorize("hasRole('USER')")
    fun cancelCoupon(
        @Parameter(
            description = "Unique coupon identifier to cancel",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @PathVariable couponId: UUID,

        @Parameter(
            description = "Cancellation reason and details",
            required = true,
            schema = Schema(implementation = CouponCancellationRequest::class)
        )
        @Valid @RequestBody request: CouponCancellationRequest
    ): ResponseEntity<CouponCancellationResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get user coupon statistics and analytics",
        description = """
            Provides comprehensive analytics and statistics for the user's coupon usage.

            ## Statistics Included:
            - 📊 **Usage Summary** - Total coupons, redemptions, savings
            - 💰 **Financial Overview** - Total spent, average purchase, savings
            - ⛽ **Fuel Preferences** - Most used fuel types and stations
            - 📅 **Time Patterns** - Usage patterns by day/month
            - 🏆 **Achievements** - Milestones and loyalty progress
            - 🎰 **Raffle Performance** - Tickets earned, wins, participation

            ## Insights & Recommendations:
            - 🎯 **Personalized Offers** - Tailored coupon recommendations
            - 📈 **Savings Opportunities** - Best deals and promotions
            - ⛽ **Station Suggestions** - Nearby stations with best prices
            - 🎫 **Loyalty Benefits** - Available rewards and upgrades

            ## Data Privacy:
            - 🔒 **User-Specific** - Only shows authenticated user's data
            - 📊 **Aggregated Insights** - No personal information exposed
            - ⏰ **Time-Bounded** - Configurable date ranges
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    fun getUserCouponStatistics(
        @Parameter(description = "Start date for statistics (ISO 8601 format)")
        @RequestParam(required = false) startDate: String?,

        @Parameter(description = "End date for statistics (ISO 8601 format)")
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<CouponStatisticsResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Regenerate QR code for active coupon",
        description = """
            Generates a new QR code for an active coupon (security feature).

            ## Use Cases:
            - 🔒 **Security Concern** - If QR code may be compromised
            - 📱 **Technical Issues** - If QR code is unreadable
            - 🔄 **Refresh Request** - User wants new code

            ## Security Features:
            - ✅ **Active Coupons Only** - Cannot regenerate for redeemed coupons
            - 🔒 **Old Code Invalidation** - Previous QR code becomes invalid
            - 📝 **Audit Trail** - All regenerations logged
            - ⏰ **Rate Limiting** - Maximum 3 regenerations per coupon

            ## Process:
            1. 🔍 **Validation** - Ensure coupon is active and owned by user
            2. 🔐 **New Code Generation** - Create cryptographically secure QR code
            3. ❌ **Old Code Invalidation** - Mark previous code as invalid
            4. 📧 **Notification** - Send new QR code to user
            5. 📊 **Logging** - Record regeneration for security audit
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PostMapping("/{couponId}/regenerate-qr")
    @PreAuthorize("hasRole('USER')")
    fun regenerateQRCode(
        @Parameter(
            description = "Unique coupon identifier",
            required = true,
            example = "123e4567-e89b-12d3-a456-426614174000"
        )
        @PathVariable couponId: UUID
    ): ResponseEntity<QRCodeRegenerationResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get system-wide coupon statistics (Admin only)",
        description = """
            Provides comprehensive system-wide coupon analytics for administrators.

            ## Admin Analytics:
            - 📊 **Platform Overview** - Total coupons, users, revenue
            - 💰 **Financial Metrics** - Revenue, refunds, average transaction
            - 🏪 **Station Performance** - Top performing stations
            - ⛽ **Fuel Type Analysis** - Popular fuel types and trends
            - 📅 **Time Series Data** - Usage patterns over time
            - 🎰 **Raffle Integration** - Ticket generation and redemption rates

            ## Business Intelligence:
            - 📈 **Growth Metrics** - User acquisition and retention
            - 🎯 **Conversion Rates** - Purchase to redemption ratios
            - 🏆 **Top Users** - Most active customers
            - 📍 **Geographic Analysis** - Regional usage patterns

            ## Export Options:
            - 📊 **CSV Export** - Raw data for further analysis
            - 📈 **Chart Data** - Ready for visualization
            - 📧 **Scheduled Reports** - Automated report delivery
        """,
        tags = ["Coupons"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @GetMapping("/statistics/system")
    @PreAuthorize("hasRole('ADMIN')")
    fun getSystemCouponStatistics(
        @Parameter(description = "Start date for statistics")
        @RequestParam(required = false) startDate: String?,

        @Parameter(description = "End date for statistics")
        @RequestParam(required = false) endDate: String?,

        @Parameter(description = "Group by dimension (day, week, month)")
        @RequestParam(defaultValue = "day") groupBy: String,

        @Parameter(description = "Include detailed breakdowns")
        @RequestParam(defaultValue = "false") includeDetails: Boolean
    ): ResponseEntity<SystemCouponStatisticsResponse> {
        TODO("Implementation needed")
    }
}