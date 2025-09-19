package com.gasolinerajsm.raffleservice.domain.valueobject

import java.time.LocalDateTime

/**
 * Value object representing raffle statistics
 */
data class RaffleStatistics(
    val currentParticipants: Int = 0,
    val totalTicketsUsed: Int = 0,
    val totalRevenue: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
) {

    /**
     * Check if raffle can accept more participants
     */
    fun canAcceptMoreParticipants(maxParticipants: Int): Boolean {
        return currentParticipants < maxParticipants
    }

    /**
     * Add participant with ticket count
     */
    fun addParticipant(ticketCount: Int): RaffleStatistics {
        return this.copy(
            currentParticipants = currentParticipants + 1,
            totalTicketsUsed = totalTicketsUsed + ticketCount,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Remove participant with ticket count
     */
    fun removeParticipant(ticketCount: Int): RaffleStatistics {
        return this.copy(
            currentParticipants = maxOf(0, currentParticipants - 1),
            totalTicketsUsed = maxOf(0, totalTicketsUsed - ticketCount),
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Record draw completion
     */
    fun recordDrawCompletion(drawResults: DrawResults): RaffleStatistics {
        return this.copy(
            lastUpdated = LocalDateTime.now()
        )
    }

    companion object {
        /**
         * Create initial statistics
         */
        fun initial(): RaffleStatistics {
            return RaffleStatistics()
        }
    }
}