package com.gasolinerajsm.adengine.domain.valueobject

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Value object representing interaction data for engagements
 */
data class InteractionData(
    val viewDurationSeconds: Int? = null,
    val completionPercentage: BigDecimal? = null,
    val clicked: Boolean = false,
    val clickedAt: LocalDateTime? = null,
    val clickThroughUrl: String? = null,
    val interactionsCount: Int = 0,
    val pauseCount: Int = 0,
    val replayCount: Int = 0,
    val skipAttempted: Boolean = false,
    val skipAllowed: Boolean = false,
    val skippedAt: LocalDateTime? = null,
    val errorOccurred: Boolean = false,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val referrerUrl: String? = null,
    val campaignContext: String? = null,
    val placementContext: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {

    companion object {
        fun initial(): InteractionData {
            return InteractionData()
        }
    }

    fun recordClick(clickThroughUrl: String? = null): InteractionData {
        return this.copy(
            clicked = true,
            clickedAt = LocalDateTime.now(),
            clickThroughUrl = clickThroughUrl,
            interactionsCount = interactionsCount + 1
        )
    }

    fun updateInteractions(interactions: Int = 0, pauses: Int = 0, replays: Int = 0): InteractionData {
        return this.copy(
            interactionsCount = interactionsCount + interactions,
            pauseCount = pauseCount + pauses,
            replayCount = replayCount + replays
        )
    }

    fun recordSkip(): InteractionData {
        return this.copy(
            skipAttempted = true,
            skippedAt = LocalDateTime.now()
        )
    }

    fun recordError(errorMessage: String, errorCode: String? = null): InteractionData {
        return this.copy(
            errorOccurred = true,
            errorMessage = errorMessage,
            errorCode = errorCode
        )
    }

    fun hasMetadata(key: String): Boolean = metadata.containsKey(key)

    fun getMetadata(key: String): Any? = metadata[key]

    fun getMetadataAsString(key: String): String? = getMetadata(key)?.toString()
}