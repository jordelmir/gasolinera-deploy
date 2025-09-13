package com.gasolinerajsm.redemptionservice.domain.model

import com.gasolinerajsm.redemptionservice.domain.event.DomainEvent
import com.gasolinerajsm.redemptionservice.domain.event.RaffleTicketGeneratedEvent
import com.gasolinerajsm.redemptionservice.domain.event.RaffleTicketUsedEvent
import com.gasolinerajsm.redemptionservice.domain.event.RaffleTicketWonEvent
import com.gasolinerajsm.redemptionservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Raffle Ticket Domain Entity
 * Represents a ticket earned from redemptions that can be used in raffles
 */
data class RaffleTicket(
    val id: RaffleTicketId,
    val userId: UserId,
    val redemptionId: RedemptionId,
    val ticketNumber: TicketNumber,
    val status: TicketStatus = TicketStatus.ACTIVE,
    val sourceInfo: TicketSourceInfo,
    val raffleInfo: RaffleInfo? = null,
    val prizeInfo: PrizeInfo? = null,
    val transferInfo: TransferInfo = TransferInfo.initial(),
    val validationInfo: ValidationInfo? = null,
    val expiryDate: LocalDateTime? = null,
    val notes: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new raffle ticket
         */
        fun create(
            userId: UserId,
            redemptionId: RedemptionId,
            ticketNumber: TicketNumber,
            sourceInfo: TicketSourceInfo,
            expiryDate: LocalDateTime? = null,
            metadata: Map<String, String> = emptyMap()
        ): RaffleTicket {
            val ticket = RaffleTicket(
                id = RaffleTicketId.generate(),
                userId = userId,
                redemptionId = redemptionId,
                ticketNumber = ticketNumber,
                sourceInfo = sourceInfo,
                expiryDate = expiryDate,
                metadata = metadata
            )

            ticket.addDomainEvent(
                RaffleTicketGeneratedEvent(
                    ticketId = ticket.id,
                    userId = ticket.userId,
                    redemptionId = ticket.redemptionId,
                    ticketNumber = ticket.ticketNumber,
                    sourceType = ticket.sourceInfo.sourceType,
                    campaignId = ticket.sourceInfo.campaignId,
                    stationId = ticket.sourceInfo.stationId,
                    occurredAt = LocalDateTime.now()
                )
            )

            return ticket
        }
    }

    /**
     * Check if ticket is active and can be used
     */
    fun isActive(): Boolean {
        return status == TicketStatus.ACTIVE && !isExpired()
    }

    /**
     * Check if ticket has expired
     */
    fun isExpired(): Boolean {
        return expiryDate?.isBefore(LocalDateTime.now()) ?: false
    }

    /**
     * Check if ticket is eligible for raffle entry
     */
    fun isEligibleForRaffle(): Boolean {
        return isActive() && raffleInfo?.isUsed != true
    }

    /**
     * Check if ticket is a winning ticket
     */
    fun isWinningTicket(): Boolean {
        return prizeInfo?.isWinner == true
    }

    /**
     * Check if prize has been claimed
     */
    fun isPrizeClaimed(): Boolean {
        return prizeInfo?.isClaimed == true
    }

    /**
     * Check if ticket can be transferred
     */
    fun canBeTransferred(): Boolean {
        return isActive() && transferInfo.canTransfer()
    }

    /**
     * Use the ticket in a raffle
     */
    fun useInRaffle(raffleId: RaffleId, raffleName: String): RaffleTicket {
        if (!isEligibleForRaffle()) {
            throw IllegalStateException("Ticket is not eligible for raffle entry")
        }

        val updatedTicket = this.copy(
            raffleInfo = RaffleInfo(
                raffleId = raffleId,
                raffleName = raffleName,
                isUsed = true,
                usedAt = LocalDateTime.now()
            ),
            updatedAt = LocalDateTime.now()
        )

        updatedTicket.addDomainEvent(
            RaffleTicketUsedEvent(
                ticketId = id,
                userId = userId,
                ticketNumber = ticketNumber,
                raffleId = raffleId,
                raffleName = raffleName,
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedTicket
    }

    /**
     * Mark ticket as winner
     */
    fun markAsWinner(prize: String, prizeValue: BigDecimal? = null): RaffleTicket {
        if (raffleInfo?.isUsed != true) {
            throw IllegalStateException("Ticket must be used in a raffle before marking as winner")
        }

        val updatedTicket = this.copy(
            prizeInfo = PrizeInfo(
                isWinner = true,
                prizeDescription = prize,
                prizeValue = prizeValue,
                isClaimed = false,
                wonAt = LocalDateTime.now()
            ),
            updatedAt = LocalDateTime.now()
        )

        updatedTicket.addDomainEvent(
            RaffleTicketWonEvent(
                ticketId = id,
                userId = userId,
                ticketNumber = ticketNumber,
                raffleId = raffleInfo!!.raffleId,
                prizeDescription = prize,
                prizeValue = prizeValue,
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedTicket
    }

    /**
     * Claim the prize
     */
    fun claimPrize(claimedBy: String? = null): RaffleTicket {
        if (!isWinningTicket()) {
            throw IllegalStateException("Cannot claim prize for non-winning ticket")
        }

        if (isPrizeClaimed()) {
            throw IllegalStateException("Prize has already been claimed")
        }

        return this.copy(
            prizeInfo = prizeInfo!!.copy(
                isClaimed = true,
                claimedAt = LocalDateTime.now(),
                claimedBy = claimedBy
            ),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Transfer ticket to another user
     */
    fun transferTo(toUserId: UserId, reason: String? = null): RaffleTicket {
        if (!canBeTransferred()) {
            throw IllegalStateException("Ticket cannot be transferred")
        }

        return this.copy(
            userId = toUserId,
            transferInfo = transferInfo.recordTransfer(toUserId, reason),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Validate the ticket
     */
    fun validate(validationCode: String, validatedBy: String? = null): RaffleTicket {
        return this.copy(
            validationInfo = ValidationInfo(
                isValidated = true,
                validationCode = validationCode,
                validatedAt = LocalDateTime.now(),
                validatedBy = validatedBy
            ),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Expire the ticket
     */
    fun expire(): RaffleTicket {
        return this.copy(
            status = TicketStatus.EXPIRED,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Cancel the ticket
     */
    fun cancel(reason: String? = null): RaffleTicket {
        return this.copy(
            status = TicketStatus.CANCELLED,
            notes = reason?.let { "${notes ?: ""}\nCancelled: $it".trim() } ?: notes,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Suspend the ticket
     */
    fun suspend(reason: String? = null): RaffleTicket {
        return this.copy(
            status = TicketStatus.SUSPENDED,
            notes = reason?.let { "${notes ?: ""}\nSuspended: $it".trim() } ?: notes,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Reactivate suspended ticket
     */
    fun reactivate(): RaffleTicket {
        if (status != TicketStatus.SUSPENDED) {
            throw IllegalStateException("Can only reactivate suspended tickets")
        }

        return this.copy(
            status = TicketStatus.ACTIVE,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Get ticket age in hours
     */
    fun getAgeInHours(): Long {
        return java.time.temporal.ChronoUnit.HOURS.between(createdAt, LocalDateTime.now())
    }

    /**
     * Get ticket age in days
     */
    fun getAgeInDays(): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now())
    }

    /**
     * Get days until expiry
     */
    fun getDaysUntilExpiry(): Long? {
        return expiryDate?.let {
            java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), it)
        }
    }

    /**
     * Get formatted ticket number for display
     */
    fun getFormattedTicketNumber(): String {
        return ticketNumber.getFormatted()
    }

    /**
     * Get ticket status display
     */
    fun getStatusDisplay(): String {
        return when {
            isExpired() -> "Expired"
            raffleInfo?.isUsed == true && !isWinningTicket() -> "Used in Raffle"
            isWinningTicket() && !isPrizeClaimed() -> "Winner - Prize Pending"
            isWinningTicket() && isPrizeClaimed() -> "Winner - Prize Claimed"
            isActive() -> "Active"
            else -> status.displayName
        }
    }

    /**
     * Add notes to the ticket
     */
    fun addNotes(additionalNotes: String): RaffleTicket {
        val updatedNotes = if (notes.isNullOrBlank()) {
            additionalNotes
        } else {
            "$notes\n$additionalNotes"
        }

        return this.copy(
            notes = updatedNotes,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update metadata
     */
    fun updateMetadata(newMetadata: Map<String, String>): RaffleTicket {
        return this.copy(
            metadata = metadata + newMetadata,
            updatedAt = LocalDateTime.now()
        )
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "RaffleTicket(id=$id, ticketNumber=${ticketNumber.value}, userId=$userId, status=$status)"
    }
}