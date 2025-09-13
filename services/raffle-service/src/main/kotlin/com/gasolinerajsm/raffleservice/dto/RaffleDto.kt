package com.gasolinerajsm.raffleservice.dto

import com.gasolinerajsm.raffleservice.model.RaffleStatus
import com.gasolinerajsm.raffleservice.model.RaffleType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for Raffle entity
 */
data class RaffleDto(
    val id: Long = 0,

    @field:NotBlank(message = "Raffle name is required")
    @field:Size(min = 2, max = 200, message = "Raffle name must be between 2 and 200 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    val status: RaffleStatus = RaffleStatus.DRAFT,
    val raffleType: RaffleType = RaffleType.REGULAR,

    @field:NotNull(message = "Registration start date is required")
    val registrationStart: LocalDateTime,

    @field:NotNull(message = "Registration end date is required")
    val registrationEnd: LocalDateTime,

    @field:NotNull(message = "Draw date is required")
    val drawDate: LocalDateTime,

    @field:Min(value = 1, message = "Max participants must be at least 1")
    val maxParticipants: Int? = null,

    val currentParticipants: Int = 0,

    @field:Min(value = 1, message = "Minimum tickets to participate must be at least 1")
    val minTicketsToParticipate: Int = 1,

    @field:Min(value = 1, message = "Max tickets per user must be at least 1")
    val maxTicketsPerUser: Int? = null,

    val totalTicketsIssued: Long = 0,
    val totalTicketsUsed: Long = 0,

    @field:DecimalMin(value = "0.0", message = "Prize pool value must be positive")
    val prizePoolValue: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Entry fee must be positive")
    val entryFee: BigDecimal? = null,

    val winnerSelectionMethod: String = "RANDOM",
    val isPublic: Boolean = true,
    val requiresVerification: Boolean = false,
    val termsAndConditions: String? = null,
    val eligibilityCriteria: String? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,

    // Additional computed fields
    val isRegistrationOpen: Boolean = false,
    val isRegistrationClosed: Boolean = false,
    val isDrawCompleted: Boolean = false,
    val canAcceptMoreParticipants: Boolean = true,
    val remainingParticipantSlots: Int? = null,
    val participationRate: Double? = null,
    val ticketUsageRate: Double = 0.0,
    val prizeCount: Int = 0,
    val winnerCount: Int = 0,
    val totalPrizeValue: BigDecimal = BigDecimal.ZERO
)

/**
 * DTO for creating a new raffle
 */
data class CreateRaffleRequest(
    @field:NotBlank(message = "Raffle name is required")
    @field:Size(min = 2, max = 200, message = "Raffle name must be between 2 and 200 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    val raffleType: RaffleType = RaffleType.REGULAR,

    @field:NotNull(message = "Registration start date is required")
    val registrationStart: LocalDateTime,

    @field:NotNull(message = "Registration end date is required")
    val registrationEnd: LocalDateTime,

    @field:NotNull(message = "Draw date is required")
    val drawDate: LocalDateTime,

    @field:Min(value = 1, message = "Max participants must be at least 1")
    val maxParticipants: Int? = null,

    @field:Min(value = 1, message = "Minimum tickets to participate must be at least 1")
    val minTicketsToParticipate: Int = 1,

    @field:Min(value = 1, message = "Max tickets per user must be at least 1")
    val maxTicketsPerUser: Int? = null,

    @field:DecimalMin(value = "0.0", message = "Prize pool value must be positive")
    val prizePoolValue: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Entry fee must be positive")
    val entryFee: BigDecimal? = null,

    val winnerSelectionMethod: String = "RANDOM",
    val isPublic: Boolean = true,
    val requiresVerification: Boolean = false,
    val termsAndConditions: String? = null,
    val eligibilityCriteria: String? = null
)

/**
 * DTO for updating a raffle
 */
data class UpdateRaffleRequest(
    @field:NotBlank(message = "Raffle name is required")
    @field:Size(min = 2, max = 200, message = "Raffle name must be between 2 and 200 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @field:NotNull(message = "Registration start date is required")
    val registrationStart: LocalDateTime,

    @field:NotNull(message = "Registration end date is required")
    val registrationEnd: LocalDateTime,

    @field:NotNull(message = "Draw date is required")
    val drawDate: LocalDateTime,

    @field:Min(value = 1, message = "Max participants must be at least 1")
    val maxParticipants: Int? = null,

    @field:Min(value = 1, message = "Minimum tickets to participate must be at least 1")
    val minTicketsToParticipate: Int = 1,

    @field:Min(value = 1, message = "Max tickets per user must be at least 1")
    val maxTicketsPerUser: Int? = null,

    @field:DecimalMin(value = "0.0", message = "Prize pool value must be positive")
    val prizePoolValue: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Entry fee must be positive")
    val entryFee: BigDecimal? = null,

    val winnerSelectionMethod: String = "RANDOM",
    val isPublic: Boolean = true,
    val requiresVerification: Boolean = false,
    val termsAndConditions: String? = null,
    val eligibilityCriteria: String? = null
)

/**
 * DTO for raffle status update
 */
data class RaffleStatusUpdateRequest(
    val status: RaffleStatus,
    val updatedBy: String? = null
)

/**
 * DTO for raffle statistics
 */
data class RaffleStatisticsDto(
    val raffle: RaffleBasicInfo,
    val tickets: TicketStatistics,
    val prizes: PrizeStatistics,
    val winners: WinnerStatistics
)

data class RaffleBasicInfo(
    val id: Long,
    val name: String,
    val status: RaffleStatus,
    val type: RaffleType,
    val currentParticipants: Int,
    val maxParticipants: Int?,
    val registrationStart: LocalDateTime,
    val registrationEnd: LocalDateTime,
    val drawDate: LocalDateTime,
    val isRegistrationOpen: Boolean,
    val participationRate: Double?
)

data class TicketStatistics(
    val totalTickets: Long,
    val activeTickets: Long,
    val winningTickets: Long,
    val uniqueParticipants: Long
)

data class PrizeStatistics(
    val totalPrizes: Long,
    val activePrizes: Long,
    val totalQuantity: Long,
    val awardedQuantity: Long,
    val totalValue: BigDecimal
)

data class WinnerStatistics(
    val totalWinners: Long,
    val pendingClaims: Long,
    val claimedPrizes: Long,
    val expiredClaims: Long,
    val verifiedWinners: Long,
    val notifiedWinners: Long
)