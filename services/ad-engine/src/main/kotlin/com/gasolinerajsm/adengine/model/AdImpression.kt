package com.gasolinerajsm.adengine.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entity representing an ad impression
 */
@Entity
@Table(name = "ad_impressions")
data class AdImpression(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: String,

    @Column(nullable = false)
    val campaignId: Long,

    @Column(nullable = false)
    val creativeId: String,

    @Column(nullable = false)
    val stationId: String,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val duration: Int = 0,

    @Column(nullable = false)
    val completed: Boolean = false,

    @Column(nullable = false)
    val skipped: Boolean = false,

    @Column(name = "session_id")
    val sessionId: String? = null,

    @Column(name = "sequence_id")
    val sequenceId: String? = null,

    @Column(name = "interaction_data", columnDefinition = "TEXT")
    val interactionData: String? = null
) {
    /**
     * Impression status constants
     */
    companion object {
        const val STATUS_STARTED = "STARTED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_SKIPPED = "SKIPPED"
        const val STATUS_ABANDONED = "ABANDONED"
    }
}