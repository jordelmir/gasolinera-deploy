package com.gasolinerajsm.adengine.domain.valueobject

import com.gasolinerajsm.adengine.domain.model.BillingEvent
import java.time.LocalDateTime

/**
 * Value object representing reward data for engagements
 */
data class RewardData(
    val baseTicketsEarned: Int = 0,
    val bonusTicketsEarned: Int = 0,
    val totalTicketsEarned: Int = 0,
    val ticketsAwarded: Boolean = false,
    val ticketsAwardedAt: LocalDateTime? = null,
    val raffleEntryCreated: Boolean = false,
    val raffleEntryId: RaffleEntryId? = null,
    val costCharged: java.math.BigDecimal? = null,
    val billingEvent: BillingEvent? = null
) {

    companion object {
        fun initial(): RewardData {
            return RewardData()
        }
    }


    fun hasTicketsAwarded(): Boolean = ticketsAwarded

    fun qualifiesForRewards(): Boolean = totalTicketsEarned > 0
}