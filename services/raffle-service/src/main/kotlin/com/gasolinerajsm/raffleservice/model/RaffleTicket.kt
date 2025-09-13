package com.gasolinerajsm.raffleservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * RaffleTicket entity representing a user's participation ticket in a raffle
 */
@Entity
@Table(
    name = "raffle_tickets",
    schema = "raffle_schema",
    indexes = [
        Index(name = "idx_raffle_tickets_user_id", columnList = "user_id"),
        Index(name = "idx_raffle_tickets_raffle_id", columnList = "raffle_id"),
        Index(name = "idx_raffle_tickets_ticket_number", columnList = "ticket_number"),
        Index(name = "idx_raffle_tickets_status", columnList = "status"),
        Index(name = "idx_raffle_tickets_source", columnList = "source_type"),
        Index(name = "idx_raffle_tickets_created_at", columnList = "created_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_raffle_tickets_number_raffle", columnNames = ["ticket_number", "raffle_id"])
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
    @JoinColumn(name = "raffle_id", nullable = false)
    @field:NotNull(message = "Raffle is required")
    val raffle: Raffle,

    @Column(name = "ticket_number", nullable = false, length = 50)
    @field:NotBlank(message = "Ticket number is required")
    @field:Size(max = 50, message = "Ticket number must not exceed 50 characters")
    val ticketNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: TicketStatus = TicketStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    val sourceType: TicketSourceType,

    @Column(name = "source_reference", length = 100)
    val sourceReference: String? = null,

    @Column(name = "coupon_id")
    val couponId: Long? = null,

    @Column(name = "campaign_id")
    val campaignId: Long? = null,

    @Column(name = "station_id")
    val stationId: Long? = null,

    @Column(name = "transaction_reference", length = 100)
    val transactionReference: String? = null,

    @Column(name = "purchase_amount", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Purchase amount must be positive")
    val purchaseAmount: java.math.BigDecimal? = null,

    @Column(name = "is_winner", nullable = false)
    val isWinner: Boolean = false,

    @Column(name = "prize_claimed", nullable = false)
    val prizeClaimed: Boolean = false,

    @Column(name = "prize_claim_date")
    val prizeClaimDate: LocalDateTime? = null,

    @Column(name = "verification_code", length = 20)
    val verificationCode: String? = null,

    @Column(name = "is_verified", nullable = false)
    val isVerified: Boolean = false,

    @Column(name = "verified_at")
    val verifiedAt: LocalDateTime? = null,

    @Column(name = "verified_by", length = 100)
    val verifiedBy: String? = null,

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
     * Check if the ticket is eligible for the draw
     */
    fun isEligibleForDraw(): Boolean {
        return status == TicketStatus.ACTIVE &&
               (!raffle.requiresVerification || isVerified)
    }

    /**
     * Check if the ticket is a winning ticket
     */
    fun isWinningTicket(): Boolean {
        return isWinner && status == TicketStatus.ACTIVE
    }

    /**
     * Check if the prize has been claimed
     */
    fun isPrizeClaimed(): Boolean {
        return prizeClaimed && prizeClaimDate != null
    }

    /**
     * Check if the ticket needs verification
     */
    fun needsVerification(): Boolean {
        return raffle.requiresVerification && !isVerified
    }

    /**
     * Check if the ticket can be verified
     */
    fun canBeVerified(): Boolean {
        return raffle.requiresVerification && !isVerified && verificationCode != null
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
     * Check if ticket was obtained from a coupon
     */
    fun isFromCoupon(): Boolean {
        return sourceType == TicketSourceType.COUPON_REDEMPTION && couponId != null
    }

    /**
     * Check if ticket was purchased
     */
    fun isPurchased(): Boolean {
        return sourceType == TicketSourceType.DIRECT_PURCHASE && purchaseAmount != null
    }

    /**
     * Check if ticket was earned through promotion
     */
    fun isPromotional(): Boolean {
        return sourceType == TicketSourceType.PROMOTIONAL || sourceType == TicketSourceType.BONUS
    }

    /**
     * Mark ticket as winner
     */
    fun markAsWinner(): RaffleTicket {
        return this.copy(isWinner = true)
    }

    /**
     * Verify the ticket
     */
    fun verify(verifiedBy: String? = null): RaffleTicket {
        return this.copy(
            isVerified = true,
            verifiedAt = LocalDateTime.now(),
            verifiedBy = verifiedBy
        )
    }

    /**
     * Claim the prize
     */
    fun claimPrize(): RaffleTicket {
        if (!isWinner) {
            throw IllegalStateException("Cannot claim prize for non-winning ticket")
        }
        return this.copy(
            prizeClaimed = true,
            prizeClaimDate = LocalDateTime.now()
        )
    }

    /**
     * Deactivate the ticket
     */
    fun deactivate(): RaffleTicket {
        return this.copy(status = TicketStatus.INACTIVE)
    }

    /**
     * Cancel the ticket
     */
    fun cancel(): RaffleTicket {
        return this.copy(status = TicketStatus.CANCELLED)
    }

    /**
     * Expire the ticket
     */
    fun expire(): RaffleTicket {
        return this.copy(status = TicketStatus.EXPIRED)
    }

    /**
     * Get formatted ticket display
     */
    fun getFormattedTicketNumber(): String {
        return "#${ticketNumber}"
    }

    /**
     * Get source description
     */
    fun getSourceDescription(): String {
        return when (sourceType) {
            TicketSourceType.COUPON_REDEMPTION -> "Coupon Redemption${couponId?.let { " (Coupon #$it)" } ?: ""}"
            TicketSourceType.DIRECT_PURCHASE -> "Direct Purchase${purchaseAmount?.let { " ($it)" } ?: ""}"
            TicketSourceType.PROMOTIONAL -> "Promotional Ticket"
            TicketSourceType.BONUS -> "Bonus Ticket"
            TicketSourceType.LOYALTY_REWARD -> "Loyalty Reward"
            TicketSourceType.REFERRAL -> "Referral Bonus"
            TicketSourceType.ADMIN_ISSUED -> "Admin Issued"
        }
    }

    override fun toString(): String {
        return "RaffleTicket(id=$id, ticketNumber='$ticketNumber', userId=$userId, raffleId=${raffle.id}, status=$status, isWinner=$isWinner)"
    }
}

/**
 * Ticket status enumeration
 */
enum class TicketStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Ticket is active and eligible for draw"),
    INACTIVE("Inactive", "Ticket is temporarily inactive"),
    CANCELLED("Cancelled", "Ticket has been cancelled"),
    EXPIRED("Expired", "Ticket has expired"),
    USED("Used", "Ticket has been used in a draw");

    /**
     * Check if the status allows participation in draw
     */
    fun allowsParticipation(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if the status is a final state
     */
    fun isFinalState(): Boolean {
        return this == CANCELLED || this == EXPIRED || this == USED
    }
}

/**
 * Ticket source type enumeration
 */
enum class TicketSourceType(val displayName: String, val description: String) {
    COUPON_REDEMPTION("Coupon Redemption", "Ticket obtained by redeeming a coupon"),
    DIRECT_PURCHASE("Direct Purchase", "Ticket purchased directly"),
    PROMOTIONAL("Promotional", "Ticket given as part of a promotion"),
    BONUS("Bonus", "Bonus ticket for loyal customers"),
    LOYALTY_REWARD("Loyalty Reward", "Ticket earned through loyalty program"),
    REFERRAL("Referral", "Ticket earned through referral program"),
    ADMIN_ISSUED("Admin Issued", "Ticket issued by administrator");

    /**
     * Check if source type requires payment
     */
    fun requiresPayment(): Boolean {
        return this == DIRECT_PURCHASE
    }

    /**
     * Check if source type is earned through activity
     */
    fun isEarned(): Boolean {
        return this == COUPON_REDEMPTION || this == LOYALTY_REWARD || this == REFERRAL
    }

    /**
     * Check if source type is promotional
     */
    fun isPromotional(): Boolean {
        return this == PROMOTIONAL || this == BONUS
    }

    /**
     * Check if source type requires verification
     */
    fun requiresVerification(): Boolean {
        return this == COUPON_REDEMPTION || this == DIRECT_PURCHASE
    }
}