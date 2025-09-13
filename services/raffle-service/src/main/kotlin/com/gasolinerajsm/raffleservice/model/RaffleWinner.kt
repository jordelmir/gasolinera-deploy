package com.gasolinerajsm.raffleservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * RaffleWinner entity representing winners of raffle prizes
 */
@Entity
@Table(
    name = "raffle_winners",
    schema = "raffle_schema",
    indexes = [
        Index(name = "idx_raffle_winners_raffle_id", columnList = "raffle_id"),
        Index(name = "idx_raffle_winners_user_id", columnList = "user_id"),
        Index(name = "idx_raffle_winners_ticket_id", columnList = "ticket_id"),
        Index(name = "idx_raffle_winners_prize_id", columnList = "prize_id"),
        Index(name = "idx_raffle_winners_status", columnList = "status"),
        Index(name = "idx_raffle_winners_won_at", columnList = "won_at"),
        Index(name = "idx_raffle_winners_claimed_at", columnList = "claimed_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_raffle_winners_ticket_prize", columnNames = ["ticket_id", "prize_id"])
    ]
)
data class RaffleWinner(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raffle_id", nullable = false)
    @field:NotNull(message = "Raffle is required")
    val raffle: Raffle,

    @Column(name = "user_id", nullable = false)
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @field:NotNull(message = "Ticket is required")
    val ticket: RaffleTicket,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_id", nullable = false)
    @field:NotNull(message = "Prize is required")
    val prize: RafflePrize,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: WinnerStatus = WinnerStatus.PENDING_CLAIM,

    @Column(name = "won_at", nullable = false)
    @field:NotNull(message = "Won date is required")
    val wonAt: LocalDateTime,

    @Column(name = "notified_at")
    val notifiedAt: LocalDateTime? = null,

    @Column(name = "claimed_at")
    val claimedAt: LocalDateTime? = null,

    @Column(name = "claim_deadline")
    val claimDeadline: LocalDateTime? = null,

    @Column(name = "verification_code", length = 20)
    val verificationCode: String? = null,

    @Column(name = "is_verified", nullable = false)
    val isVerified: Boolean = false,

    @Column(name = "verified_at")
    val verifiedAt: LocalDateTime? = null,

    @Column(name = "verified_by", length = 100)
    val verifiedBy: String? = null,

    @Column(name = "delivery_method", length = 50)
    val deliveryMethod: String? = null,

    @Column(name = "delivery_address", length = 500)
    val deliveryAddress: String? = null,

    @Column(name = "delivery_status", length = 30)
    val deliveryStatus: String? = null,

    @Column(name = "delivered_at")
    val deliveredAt: LocalDateTime? = null,

    @Column(name = "tracking_number", length = 100)
    val trackingNumber: String? = null,

    @Column(name = "contact_phone", length = 20)
    val contactPhone: String? = null,

    @Column(name = "contact_email", length = 100)
    val contactEmail: String? = null,

    @Column(name = "identity_document", length = 50)
    val identityDocument: String? = null,

    @Column(name = "notes", length = 1000)
    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    @Column(name = "processed_by", length = 100)
    val processedBy: String? = null,

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
     * Check if winner has been notified
     */
    fun isNotified(): Boolean {
        return notifiedAt != null
    }

    /**
     * Check if prize has been claimed
     */
    fun isClaimed(): Boolean {
        return status == WinnerStatus.CLAIMED && claimedAt != null
    }

    /**
     * Check if claim has expired
     */
    fun isClaimExpired(): Boolean {
        return claimDeadline?.isBefore(LocalDateTime.now()) == true && !isClaimed()
    }

    /**
     * Check if winner needs verification
     */
    fun needsVerification(): Boolean {
        return prize.requiresIdentityVerification && !isVerified
    }

    /**
     * Check if winner can claim prize
     */
    fun canClaimPrize(): Boolean {
        return status == WinnerStatus.PENDING_CLAIM &&
               !isClaimExpired() &&
               (!prize.requiresIdentityVerification || isVerified)
    }

    /**
     * Check if prize requires delivery
     */
    fun requiresDelivery(): Boolean {
        return prize.isPhysical() && deliveryMethod != null
    }

    /**
     * Get days until claim deadline
     */
    fun getDaysUntilDeadline(): Long? {
        return claimDeadline?.let {
            java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), it)
        }
    }

    /**
     * Get hours since won
     */
    fun getHoursSinceWon(): Long {
        return java.time.temporal.ChronoUnit.HOURS.between(wonAt, LocalDateTime.now())
    }

    /**
     * Get days since won
     */
    fun getDaysSinceWon(): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(wonAt, LocalDateTime.now())
    }

    /**
     * Mark as notified
     */
    fun markAsNotified(): RaffleWinner {
        return this.copy(notifiedAt = LocalDateTime.now())
    }

    /**
     * Verify the winner
     */
    fun verify(verifiedBy: String? = null, identityDoc: String? = null): RaffleWinner {
        return this.copy(
            isVerified = true,
            verifiedAt = LocalDateTime.now(),
            verifiedBy = verifiedBy,
            identityDocument = identityDoc
        )
    }

    /**
     * Claim the prize
     */
    fun claimPrize(processedBy: String? = null): RaffleWinner {
        if (!canClaimPrize()) {
            throw IllegalStateException("Prize cannot be claimed at this time")
        }
        return this.copy(
            status = WinnerStatus.CLAIMED,
            claimedAt = LocalDateTime.now(),
            processedBy = processedBy
        )
    }

    /**
     * Set delivery information
     */
    fun setDeliveryInfo(
        method: String,
        address: String? = null,
        phone: String? = null,
        email: String? = null
    ): RaffleWinner {
        return this.copy(
            deliveryMethod = method,
            deliveryAddress = address,
            contactPhone = phone,
            contactEmail = email,
            deliveryStatus = "PENDING"
        )
    }

    /**
     * Update delivery status
     */
    fun updateDeliveryStatus(status: String, trackingNumber: String? = null): RaffleWinner {
        return this.copy(
            deliveryStatus = status,
            trackingNumber = trackingNumber,
            deliveredAt = if (status == "DELIVERED") LocalDateTime.now() else deliveredAt
        )
    }

    /**
     * Expire the claim
     */
    fun expireClaim(): RaffleWinner {
        return this.copy(status = WinnerStatus.EXPIRED)
    }

    /**
     * Forfeit the prize
     */
    fun forfeit(): RaffleWinner {
        return this.copy(status = WinnerStatus.FORFEITED)
    }

    /**
     * Disqualify the winner
     */
    fun disqualify(reason: String? = null): RaffleWinner {
        return this.copy(
            status = WinnerStatus.DISQUALIFIED,
            notes = reason?.let { "${notes ?: ""}\nDisqualified: $it".trim() } ?: notes
        )
    }

    /**
     * Get status display with additional info
     */
    fun getStatusDisplay(): String {
        return when (status) {
            WinnerStatus.PENDING_CLAIM -> {
                if (isClaimExpired()) "Claim Expired"
                else if (needsVerification()) "Pending Verification"
                else "Pending Claim"
            }
            WinnerStatus.CLAIMED -> {
                if (requiresDelivery()) "Claimed - ${deliveryStatus ?: "Pending Delivery"}"
                else "Claimed"
            }
            else -> status.displayName
        }
    }

    /**
     * Get prize summary
     */
    fun getPrizeSummary(): String {
        return "${prize.name} (${prize.getTierDisplay()})"
    }

    override fun toString(): String {
        return "RaffleWinner(id=$id, userId=$userId, prize='${prize.name}', status=$status, wonAt=$wonAt)"
    }
}

/**
 * Winner status enumeration
 */
enum class WinnerStatus(val displayName: String, val description: String) {
    PENDING_CLAIM("Pending Claim", "Winner has been selected but hasn't claimed prize"),
    CLAIMED("Claimed", "Prize has been claimed by winner"),
    EXPIRED("Expired", "Claim period has expired"),
    FORFEITED("Forfeited", "Winner has forfeited the prize"),
    DISQUALIFIED("Disqualified", "Winner has been disqualified"),
    PROCESSING("Processing", "Prize claim is being processed"),
    DELIVERED("Delivered", "Prize has been delivered to winner");

    /**
     * Check if status allows claiming
     */
    fun allowsClaiming(): Boolean {
        return this == PENDING_CLAIM || this == PROCESSING
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this == CLAIMED || this == EXPIRED || this == FORFEITED ||
               this == DISQUALIFIED || this == DELIVERED
    }

    /**
     * Check if status indicates success
     */
    fun isSuccessful(): Boolean {
        return this == CLAIMED || this == DELIVERED
    }
}