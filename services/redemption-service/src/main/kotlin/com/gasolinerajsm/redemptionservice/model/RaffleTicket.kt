package com.gasolinerajsm.redemptionservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * RaffleTicket entity with user association and status tracking for redemption service
 */
@Entity
@Table(
    name = "raffle_tickets",
    schema = "redemption_schema",
    indexes = [
        Index(name = "idx_raffle_tickets_user_id", columnList = "user_id"),
        Index(name = "idx_raffle_tickets_redemption_id", columnList = "redemption_id"),
        Index(name = "idx_raffle_tickets_ticket_number", columnList = "ticket_number"),
        Index(name = "idx_raffle_tickets_status", columnList = "status"),
        Index(name = "idx_raffle_tickets_raffle_id", columnList = "raffle_id"),
        Index(name = "idx_raffle_tickets_created_at", columnList = "created_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_raffle_tickets_number", columnNames = ["ticket_number"])
    ]
)
data class RaffleTicket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id", nullable = false)
    @field:NotNull(message = "Redemption is required")
    val redemption: Redemption,

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    @field:NotBlank(message = "Ticket number is required")
    @field:Size(max = 50, message = "Ticket number must not exceed 50 characters")
    val ticketNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: TicketStatus = TicketStatus.ACTIVE,

    @Column(name = "raffle_id")
    val raffleId: Long? = null,

    @Column(name = "raffle_name", length = 200)
    val raffleName: String? = null,

    @Column(name = "source_type", nullable = false, length = 30)
    val sourceType: String = "COUPON_REDEMPTION",

    @Column(name = "source_reference", length = 100)
    val sourceReference: String? = null,

    @Column(name = "campaign_id")
    val campaignId: Long? = null,

    @Column(name = "station_id")
    val stationId: Long? = null,

    @Column(name = "is_used", nullable = false)
    val isUsed: Boolean = false,

    @Column(name = "used_at")
    val usedAt: LocalDateTime? = null,

    @Column(name = "used_in_raffle_id")
    val usedInRaffleId: Long? = null,

    @Column(name = "is_winner", nullable = false)
    val isWinner: Boolean = false,

    @Column(name = "prize_won", length = 200)
    val prizeWon: String? = null,

    @Column(name = "prize_value", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Prize value must be positive")
    val prizeValue: java.math.BigDecimal? = null,

    @Column(name = "prize_claimed", nullable = false)
    val prizeClaimed: Boolean = false,

    @Column(name = "prize_claim_date")
    val prizeClaimDate: LocalDateTime? = null,

    @Column(name = "expiry_date")
    val expiryDate: LocalDateTime? = null,

    @Column(name = "transfer_count", nullable = false)
    val transferCount: Int = 0,

    @Column(name = "last_transferred_to")
    val lastTransferredTo: Long? = null,

    @Column(name = "last_transfer_date")
    val lastTransferDate: LocalDateTime? = null,

    @Column(name = "validation_code", length = 20)
    val validationCode: String? = null,

    @Column(name = "is_validated", nullable = false)
    val isValidated: Boolean = false,

    @Column(name = "validated_at")
    val validatedAt: LocalDateTime? = null,

    @Column(name = "validated_by", length = 100)
    val validatedBy: String? = null,

    @Column(name = "notes", length = 500)
    @field:Size(max = 500, message = "Notes must not exceed 500 characters")
    val notes: String? = null,

    @Column(name = "metadata", length = 1000)
    val metadata: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if ticket is active and can be used
     */
    fun isActive(): Boolean {
        return status == TicketStatus.ACTIVE && !isUsed && !isExpired()
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
        return isActive() && (!isValidated || validationCode == null)
    }

    /**
     * Check if ticket is a winning ticket
     */
    fun isWinningTicket(): Boolean {
        return isWinner && prizeWon != null
    }

    /**
     * Check if prize has been claimed
     */
    fun isPrizeClaimed(): Boolean {
        return prizeClaimed && prizeClaimDate != null
    }

    /**
     * Check if ticket can be transferred
     */
    fun canBeTransferred(): Boolean {
        return isActive() && transferCount < 3 // Max 3 transfers
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
     * Use the ticket in a raffle
     */
    fun use(raffleId: Long): RaffleTicket {
        if (!isActive()) {
            throw IllegalStateException("Ticket is not active and cannot be used")
        }
        return this.copy(
            isUsed = true,
            usedAt = LocalDateTime.now(),
            usedInRaffleId = raffleId,
            status = TicketStatus.USED
        )
    }

    /**
     * Mark ticket as winner
     */
    fun markAsWinner(prize: String, value: java.math.BigDecimal? = null): RaffleTicket {
        return this.copy(
            isWinner = true,
            prizeWon = prize,
            prizeValue = value
        )
    }

    /**
     * Claim the prize
     */
    fun claimPrize(): RaffleTicket {
        if (!isWinningTicket()) {
            throw IllegalStateException("Cannot claim prize for non-winning ticket")
        }
        return this.copy(
            prizeClaimed = true,
            prizeClaimDate = LocalDateTime.now()
        )
    }

    /**
     * Transfer ticket to another user
     */
    fun transfer(toUserId: Long): RaffleTicket {
        if (!canBeTransferred()) {
            throw IllegalStateException("Ticket cannot be transferred")
        }
        return this.copy(
            userId = toUserId,
            transferCount = transferCount + 1,
            lastTransferredTo = toUserId,
            lastTransferDate = LocalDateTime.now()
        )
    }

    /**
     * Validate the ticket
     */
    fun validate(validatedBy: String? = null): RaffleTicket {
        return this.copy(
            isValidated = true,
            validatedAt = LocalDateTime.now(),
            validatedBy = validatedBy
        )
    }

    /**
     * Expire the ticket
     */
    fun expire(): RaffleTicket {
        return this.copy(status = TicketStatus.EXPIRED)
    }

    /**
     * Cancel the ticket
     */
    fun cancel(reason: String? = null): RaffleTicket {
        return this.copy(
            status = TicketStatus.CANCELLED,
            notes = reason?.let { "${notes ?: ""}\nCancelled: $it".trim() } ?: notes
        )
    }

    /**
     * Get formatted ticket number for display
     */
    fun getFormattedTicketNumber(): String {
        return "#$ticketNumber"
    }

    /**
     * Get ticket status display
     */
    fun getStatusDisplay(): String {
        return when {
            isExpired() -> "Expired"
            isUsed -> "Used in Raffle"
            isWinningTicket() && !isPrizeClaimed() -> "Winner - Prize Pending"
            isWinningTicket() && isPrizeClaimed() -> "Winner - Prize Claimed"
            isActive() -> "Active"
            else -> status.displayName
        }
    }

    override fun toString(): String {
        return "RaffleTicket(id=$id, ticketNumber='$ticketNumber', userId=$userId, status=$status, isWinner=$isWinner)"
    }
}

/**
 * Ticket status enumeration
 */
enum class TicketStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Ticket is active and can be used"),
    USED("Used", "Ticket has been used in a raffle"),
    EXPIRED("Expired", "Ticket has expired"),
    CANCELLED("Cancelled", "Ticket has been cancelled"),
    TRANSFERRED("Transferred", "Ticket has been transferred to another user"),
    SUSPENDED("Suspended", "Ticket is temporarily suspended");

    /**
     * Check if status allows ticket usage
     */
    fun allowsUsage(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status is final (cannot be changed)
     */
    fun isFinalStatus(): Boolean {
        return this == USED || this == EXPIRED || this == CANCELLED
    }

    /**
     * Check if status allows transfer
     */
    fun allowsTransfer(): Boolean {
        return this == ACTIVE
    }
}