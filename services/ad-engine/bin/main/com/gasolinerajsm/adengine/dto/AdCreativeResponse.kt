package com.gasolinerajsm.adengine.dto

import java.time.LocalDateTime

/**
 * Response DTO for ad creative content
 */
data class AdCreativeResponse(
    val adUrl: String,
    val campaignId: Long,
    val creativeId: String,
    val duration: Int = 10,
    val skipAfter: Int = 5,
    val title: String? = null,
    val description: String? = null,
    val callToAction: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Request DTO for starting an ad sequence
 */
data class StartAdSequenceRequest(
    val userId: String,
    val stationId: String,
    val sessionId: String,
    val userPreferences: Map<String, Any> = emptyMap()
)

/**
 * Request DTO for completing an ad
 */
data class CompleteAdRequest(
    val sequenceId: String,
    val stepCompleted: Int,
    val watchedDuration: Int,
    val skipped: Boolean = false,
    val interactionData: Map<String, Any> = emptyMap()
)

/**
 * Response DTO for ad sequence information
 */
data class AdSequenceResponse(
    val sequenceId: String,
    val currentStep: Int,
    val totalSteps: Int,
    val nextAd: AdCreativeResponse?,
    val rewardEarned: Int = 0,
    val isComplete: Boolean = false,
    val estimatedTimeRemaining: Int = 0
)