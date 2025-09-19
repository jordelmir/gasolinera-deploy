package com.gasolinerajsm.raffleservice.domain.valueobject

import java.time.LocalDateTime

/**
 * Value object representing user profile information for eligibility checks
 */
data class UserProfile(
    val userId: UserId,
    val userType: UserType,
    val age: Int? = null,
    val location: UserLocation? = null,
    val hasActiveSubscription: Boolean = false,
    val createdAt: LocalDateTime,
    val lastActivity: LocalDateTime? = null
)

/**
 * User location information
 */
data class UserLocation(
    val country: String,
    val region: String? = null,
    val city: String? = null
)

/**
 * Entry details for raffle participation
 */
data class EntryDetails(
    val userProfile: UserProfile,
    val entryMethod: EntryMethod = EntryMethod.DIRECT,
    val sourceReference: String? = null
)

/**
 * Entry method enumeration
 */
enum class EntryMethod {
    DIRECT,
    COUPON_REDEMPTION,
    PROMOTIONAL,
    REFERRAL
}

/**
 * Draw results value object
 */
data class DrawResults(
    val winners: List<WinnerInfo>,
    val drawTimestamp: LocalDateTime = LocalDateTime.now(),
    val seedUsed: String,
    val totalParticipants: Int
)

/**
 * Winner information
 */
data class WinnerInfo(
    val userId: UserId,
    val prizeId: PrizeId,
    val ticketId: TicketId,
    val position: Int
)