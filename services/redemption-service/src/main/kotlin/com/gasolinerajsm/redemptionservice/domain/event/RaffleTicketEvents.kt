package com.gasolinerajsm.redemptionservice.domain.event

import com.gasolinerajsm.redemptionservice.domain.model.TicketSourceType
import com.gasolinerajsm.redemptionservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event fired when a raffle ticket is generated
 */
data class RaffleTicketGeneratedEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val redemptionId: RedemptionId,
    val ticketNumber: TicketNumber,
    val sourceType: TicketSourceType,
    val campaignId: CampaignId?,
    val stationId: StationId?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketGenerated",
    occurredAt = occurredAt
)

/**
 * Event fired when a raffle ticket is used in a raffle
 */
data class RaffleTicketUsedEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val ticketNumber: TicketNumber,
    val raffleId: RaffleId,
    val raffleName: String,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketUsed",
    occurredAt = occurredAt
)

/**
 * Event fired when a raffle ticket wins a prize
 */
data class RaffleTicketWonEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val ticketNumber: TicketNumber,
    val raffleId: RaffleId,
    val prizeDescription: String,
    val prizeValue: BigDecimal?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketWon",
    occurredAt = occurredAt
)

/**
 * Event fired when a prize is claimed
 */
data class PrizeClaimedEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val ticketNumber: TicketNumber,
    val raffleId: RaffleId,
    val prizeDescription: String,
    val prizeValue: BigDecimal?,
    val claimedBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "PrizeClaimed",
    occurredAt = occurredAt
)

/**
 * Event fired when a raffle ticket is transferred
 */
data class RaffleTicketTransferredEvent(
    val ticketId: RaffleTicketId,
    val fromUserId: UserId,
    val toUserId: UserId,
    val ticketNumber: TicketNumber,
    val transferReason: String?,
    val transferCount: Int,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketTransferred",
    occurredAt = occurredAt
)

/**
 * Event fired when a raffle ticket is validated
 */
data class RaffleTicketValidatedEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val ticketNumber: TicketNumber,
    val validationCode: String,
    val validatedBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketValidated",
    occurredAt = occurredAt
)

/**
 * Event fired when a raffle ticket expires
 */
data class RaffleTicketExpiredEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val ticketNumber: TicketNumber,
    val expiryDate: LocalDateTime,
    val wasUsed: Boolean,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketExpired",
    occurredAt = occurredAt
)

/**
 * Event fired when a raffle ticket is cancelled
 */
data class RaffleTicketCancelledEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val ticketNumber: TicketNumber,
    val cancellationReason: String?,
    val cancelledBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketCancelled",
    occurredAt = occurredAt
)

/**
 * Event fired when a raffle ticket is suspended
 */
data class RaffleTicketSuspendedEvent(
    val ticketId: RaffleTicketId,
    val userId: UserId,
    val ticketNumber: TicketNumber,
    val suspensionReason: String?,
    val suspendedBy: String?,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "RaffleTicketSuspended",
    occurredAt = occurredAt
)