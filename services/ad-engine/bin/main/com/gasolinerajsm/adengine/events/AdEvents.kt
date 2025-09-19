package com.gasolinerajsm.adengine.events

import java.time.LocalDateTime

/**
 * Event fired when an ad is completed
 */
data class AdCompletionEvent(
    val userId: Long,
    val engagementId: Long,
    val advertisementId: Long,
    val campaignId: Long,
    val baseTickets: Int,
    val bonusTickets: Int,
    val totalTickets: Int,
    val raffleId: Long?,
    val completedAt: LocalDateTime,
    val stationId: Long?
)