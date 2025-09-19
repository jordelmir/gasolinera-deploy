package com.gasolinerajsm.raffleservice.dto

import com.gasolinerajsm.raffleservice.model.TicketStatus
import com.gasolinerajsm.raffleservice.model.TicketSourceType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for RaffleTicket entity
 */
data class RaffleTicketDto(
    val id: Long = 0,
    val userId: Long,
    val raffleId: Long,
    val raffleName: String? = null,
    val ticketNumber: String,
    val status: TicketStatus,
    val sourceType: TicketSourceType,
    val sourceReference: String? = null,
    val couponId: Long? = null,
    val campaignId: Long? = null,
    val stationId: Long? = null,
    val transactionReference: String? = null,
    val purchaseAmount: BigDecimal? = null,
    val isWinner: Boolean = false,
    val prizeClaimed: Boolean = false,
    val prizeClaimDate: LocalDateTime? = null,
    val verificationCode: String? = null,
    val isVerified: Boolean = false,
    val verifiedAt: LocalDateTime? = null,
    val verifiedBy: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,

    // Additional computed fields
    val isEligibleForDraw: Boolean = false,
    val isWinningTicket: Boolean = false,
    val isPrizeClaimed: Boolean = false,
    val needsVerification: Boolean = false,
    val canBeVerified: Boolean = false,
    val ageInHours: Long = 0,
    val ageInDays: Long = 0,
    val sourceDescription: String = "",
    val formattedTicketNumber: String = ""
)

/**
 * DTO for entering a raffle with coupon
 */
data class EnterRaffleWithCouponRequest(
    @field:NotNull(message = "Raffle ID is required")
    val raffleId: Long,

    @field:NotNull(message = "Coupon ID is required")
    val couponId: Long,

    @field:Min(value = 1, message = "Ticket count must be at least 1")
    @field:Max(value = 100, message = "Ticket count cannot exceed 100")
    val ticketCount: Int,

    val stationId: Long? = null,
    val transactionReference: String? = null
)

/**
 * DTO for entering a raffle with direct purchase
 */
data class EnterRaffleWithPurchaseRequest(
    @field:NotNull(message = "Raffle ID is required")
    val raffleId: Long,

    @field:Min(value = 1, message = "Ticket count must be at least 1")
    @field:Max(value = 100, message = "Ticket count cannot exceed 100")
    val ticketCount: Int,

    @field:NotNull(message = "Purchase amount is required")
    @field:DecimalMin(value = "0.01", message = "Purchase amount must be positive")
    val purchaseAmount: BigDecimal,

    val stationId: Long? = null,
    val transactionReference: String? = null
)

/**
 * DTO for entering a raffle with promotional tickets
 */
data class EnterRaffleWithPromotionalTicketsRequest(
    @field:NotNull(message = "Raffle ID is required")
    val raffleId: Long,

    @field:Min(value = 1, message = "Ticket count must be at least 1")
    @field:Max(value = 100, message = "Ticket count cannot exceed 100")
    val ticketCount: Int,

    val campaignId: Long? = null,
    val sourceReference: String? = null
)

/**
 * DTO for ticket verification
 */
data class VerifyTicketRequest(
    @field:NotBlank(message = "Verification code is required")
    @field:Size(min = 6, max = 6, message = "Verification code must be 6 characters")
    val verificationCode: String,

    val verifiedBy: String? = null
)

/**
 * DTO for ticket cancellation
 */
data class CancelTicketRequest(
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    val reason: String? = null
)

/**
 * DTO for raffle entry response
 */
data class RaffleEntryResponse(
    val success: Boolean,
    val message: String,
    val tickets: List<RaffleTicketDto> = emptyList(),
    val totalTicketsCreated: Int = 0,
    val userTotalTickets: Long = 0,
    val raffleInfo: RaffleEntryInfo? = null
)

/**
 * DTO for raffle entry information
 */
data class RaffleEntryInfo(
    val raffleId: Long,
    val raffleName: String,
    val registrationEndsAt: LocalDateTime,
    val drawDate: LocalDateTime,
    val currentParticipants: Int,
    val maxParticipants: Int?,
    val remainingSlots: Int?
)

/**
 * DTO for user ticket statistics
 */
data class UserTicketStatisticsDto(
    val totalTickets: Long,
    val activeTickets: Long,
    val winningTickets: Long,
    val claimedPrizes: Long,
    val ticketsBySource: Map<TicketSourceType, Long> = emptyMap(),
    val ticketsByStatus: Map<TicketStatus, Long> = emptyMap(),
    val recentTickets: List<RaffleTicketDto> = emptyList()
)

/**
 * DTO for ticket validation summary
 */
data class TicketValidationSummaryDto(
    val canEnter: Boolean,
    val registrationOpen: Boolean,
    val hasCapacity: Boolean,
    val userTicketCount: Int,
    val minTicketsRequired: Int,
    val maxTicketsAllowed: Int?,
    val remainingSlots: Int?,
    val registrationEndsAt: LocalDateTime,
    val drawDate: LocalDateTime,
    val validationMessages: List<String> = emptyList()
)