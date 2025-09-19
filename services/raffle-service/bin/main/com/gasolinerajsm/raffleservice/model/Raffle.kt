package com.gasolinerajsm.raffleservice.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "raffles")
data class Raffle(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(length = 1000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var raffleType: RaffleType = RaffleType.WEEKLY,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RaffleStatus = RaffleStatus.DRAFT,

    @Column(nullable = false)
    var registrationStart: LocalDateTime,

    @Column(nullable = false)
    var registrationEnd: LocalDateTime,

    @Column(nullable = false)
    var drawDate: LocalDateTime,

    @Column(nullable = false)
    var maxParticipants: Int = 1000,

    @Column(nullable = false)
    var currentParticipants: Int = 0,

    @Column(nullable = false)
    var minTicketsToParticipate: Int = 1,

    @Column(nullable = false)
    var maxTicketsPerUser: Int = 10,

    @Column(nullable = false)
    var totalTicketsIssued: Long = 0,

    @Column(nullable = false)
    var totalTicketsUsed: Long = 0,

    @Column(nullable = false, precision = 10, scale = 2)
    var prizePoolValue: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 10, scale = 2)
    var entryFee: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var winnerSelectionMethod: String = "RANDOM",

    @Column(nullable = false)
    var isPublic: Boolean = true,

    @Column(nullable = false)
    var requiresVerification: Boolean = false,

    @Column(length = 2000)
    var termsAndConditions: String? = null,

    @Column(length = 1000)
    var eligibilityCriteria: String? = null,

    @Column(nullable = false)
    var createdBy: String,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedBy: String? = null,

    var updatedAt: LocalDateTime? = null,

    // Legacy fields for compatibility
    val period: String = "",
    val merkleRoot: String = "",
    var drawAt: LocalDateTime? = null,
    var externalSeed: String? = null,
    var winnerEntryId: String? = null
) {

    // Computed properties
    fun isRegistrationOpen(): Boolean {
        val now = LocalDateTime.now()
        return status == RaffleStatus.ACTIVE &&
               now.isAfter(registrationStart) &&
               now.isBefore(registrationEnd)
    }

    fun isRegistrationClosed(): Boolean = !isRegistrationOpen()

    fun isDrawCompleted(): Boolean = status == RaffleStatus.COMPLETED

    fun canAcceptMoreParticipants(): Boolean = currentParticipants < maxParticipants

    fun canUserParticipate(userTicketCount: Int): Boolean {
        return isRegistrationOpen() &&
               canAcceptMoreParticipants() &&
               userTicketCount >= minTicketsToParticipate
    }

    fun getRemainingParticipantSlots(): Int = maxParticipants - currentParticipants

    fun getParticipationRate(): Double =
        if (maxParticipants > 0) currentParticipants.toDouble() / maxParticipants else 0.0

    fun getTicketUsageRate(): Double =
        if (totalTicketsIssued > 0) totalTicketsUsed.toDouble() / totalTicketsIssued else 0.0

    fun getPrizeCount(): Int = 1 // Simplified for now

    fun getWinnerCount(): Int = if (isDrawCompleted()) 1 else 0

    fun getTotalPrizeValue(): BigDecimal = prizePoolValue

    fun allowsModifications(): Boolean = status == RaffleStatus.DRAFT

    fun allowsRegistration(): Boolean = status == RaffleStatus.ACTIVE

    fun isEligibleForDraw(): Boolean {
        return status == RaffleStatus.ACTIVE &&
               LocalDateTime.now().isAfter(registrationEnd) &&
               currentParticipants > 0
    }

    fun isFinalState(): Boolean = status in listOf(RaffleStatus.COMPLETED, RaffleStatus.CANCELLED)

    fun updateParticipantCount(newCount: Int): Raffle {
        return this.copy(
            currentParticipants = newCount,
            updatedAt = LocalDateTime.now()
        )
    }

    // State transition methods
    fun activate(): Raffle {
        require(status == RaffleStatus.DRAFT) { "Can only activate draft raffles" }
        return copy(status = RaffleStatus.ACTIVE, updatedAt = LocalDateTime.now())
    }

    fun pause(): Raffle {
        require(status == RaffleStatus.ACTIVE) { "Can only pause active raffles" }
        return copy(status = RaffleStatus.PAUSED, updatedAt = LocalDateTime.now())
    }

    fun cancel(): Raffle {
        require(!isFinalState()) { "Cannot cancel raffle in final state" }
        return copy(status = RaffleStatus.CANCELLED, updatedAt = LocalDateTime.now())
    }

    fun complete(): Raffle {
        require(isEligibleForDraw()) { "Raffle is not eligible for completion" }
        return copy(status = RaffleStatus.COMPLETED, updatedAt = LocalDateTime.now())
    }
}
