package com.gasolinerajsm.adengine.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Value object representing advertisement performance statistics
 */
data class AdStatistics(
    val totalImpressions: Long = 0,
    val totalClicks: Long = 0,
    val totalCompletions: Long = 0,
    val totalInteractions: Long = 0,
    val totalSkips: Long = 0,
    val totalErrors: Long = 0,
    val uniqueUsers: Long = 0,
    val uniqueImpressions: Long = 0,
    val totalViewDurationSeconds: Long = 0,
    val totalCost: BigDecimal = BigDecimal.ZERO,
    val dailyStats: Map<String, DailyStats> = emptyMap(),
    val hourlyStats: Map<String, HourlyStats> = emptyMap(),
    val deviceStats: Map<String, Long> = emptyMap(),
    val locationStats: Map<String, Long> = emptyMap(),
    val lastUpdated: LocalDateTime = LocalDateTime.now()
) {

    /**
     * Record an impression
     */
    fun recordImpression(): AdStatistics {
        return this.copy(
            totalImpressions = totalImpressions + 1,
            uniqueImpressions = uniqueImpressions + 1, // Simplified
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Record a click
     */
    fun recordClick(): AdStatistics {
        return this.copy(
            totalClicks = totalClicks + 1,
            totalInteractions = totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Record a completion
     */
    fun recordCompletion(viewDurationSeconds: Long = 0): AdStatistics {
        return this.copy(
            totalCompletions = totalCompletions + 1,
            totalInteractions = totalInteractions + 1,
            totalViewDurationSeconds = totalViewDurationSeconds + viewDurationSeconds,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Record an interaction
     */
    fun recordInteraction(): AdStatistics {
        return this.copy(
            totalInteractions = totalInteractions + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Record a skip
     */
    fun recordSkip(): AdStatistics {
        return this.copy(
            totalSkips = totalSkips + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Record an error
     */
    fun recordError(): AdStatistics {
        return this.copy(
            totalErrors = totalErrors + 1,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Get click-through rate (CTR)
     */
    fun getClickThroughRate(): Double {
        return if (totalImpressions > 0) {
            (totalClicks.toDouble() / totalImpressions.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Get completion rate
     */
    fun getCompletionRate(): Double {
        return if (totalImpressions > 0) {
            (totalCompletions.toDouble() / totalImpressions.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Get interaction rate
     */
    fun getInteractionRate(): Double {
        return if (totalImpressions > 0) {
            (totalInteractions.toDouble() / totalImpressions.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Get skip rate
     */
    fun getSkipRate(): Double {
        return if (totalImpressions > 0) {
            (totalSkips.toDouble() / totalImpressions.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Get error rate
     */
    fun getErrorRate(): Double {
        return if (totalImpressions > 0) {
            (totalErrors.toDouble() / totalImpressions.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Get average view duration
     */
    fun getAverageViewDurationSeconds(): Double {
        return if (totalCompletions > 0) {
            totalViewDurationSeconds.toDouble() / totalCompletions.toDouble()
        } else {
            0.0
        }
    }

    /**
     * Get effective CPM (Cost Per Mille)
     */
    fun getEffectiveCPM(totalSpend: BigDecimal): BigDecimal {
        return if (totalImpressions > 0) {
            totalSpend.divide(
                BigDecimal.valueOf(totalImpressions),
                4,
                RoundingMode.HALF_UP
            ).multiply(BigDecimal.valueOf(1000))
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Get effective CPC (Cost Per Click)
     */
    fun getEffectiveCPC(totalSpend: BigDecimal): BigDecimal {
        return if (totalClicks > 0) {
            totalSpend.divide(
                BigDecimal.valueOf(totalClicks),
                4,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Get effective CPA (Cost Per Action)
     */
    fun getEffectiveCPA(totalSpend: BigDecimal): BigDecimal {
        val actions = totalClicks + totalCompletions + totalInteractions
        return if (actions > 0) {
            totalSpend.divide(
                BigDecimal.valueOf(actions),
                4,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Check if daily impression limit is reached
     */
    fun hasReachedDailyLimit(dailyLimit: Int): Boolean {
        val today = LocalDateTime.now().toLocalDate().toString()
        val todayStats = dailyStats[today]
        return todayStats?.impressions ?: 0 >= dailyLimit
    }

    /**
     * Get performance metrics summary
     */
    fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            impressions = totalImpressions,
            clicks = totalClicks,
            completions = totalCompletions,
            interactions = totalInteractions,
            skips = totalSkips,
            errors = totalErrors,
            uniqueUsers = uniqueUsers,
            clickThroughRate = getClickThroughRate(),
            completionRate = getCompletionRate(),
            interactionRate = getInteractionRate(),
            skipRate = getSkipRate(),
            errorRate = getErrorRate(),
            averageViewDuration = getAverageViewDurationSeconds(),
            totalCost = totalCost
        )
    }

    /**
     * Get statistics summary
     */
    fun getStatisticsSummary(): Map<String, Any> {
        return mapOf(
            "totalImpressions" to totalImpressions,
            "totalClicks" to totalClicks,
            "totalCompletions" to totalCompletions,
            "totalInteractions" to totalInteractions,
            "totalSkips" to totalSkips,
            "totalErrors" to totalErrors,
            "uniqueUsers" to uniqueUsers,
            "clickThroughRate" to getClickThroughRate(),
            "completionRate" to getCompletionRate(),
            "interactionRate" to getInteractionRate(),
            "skipRate" to getSkipRate(),
            "errorRate" to getErrorRate(),
            "averageViewDurationSeconds" to getAverageViewDurationSeconds(),
            "totalCost" to totalCost,
            "lastUpdated" to lastUpdated
        )
    }

    companion object {
        fun initial(): AdStatistics {
            return AdStatistics()
        }

        fun create(
            impressions: Long = 0,
            clicks: Long = 0,
            completions: Long = 0
        ): AdStatistics {
            return AdStatistics(
                totalImpressions = impressions,
                totalClicks = clicks,
                totalCompletions = completions
            )
        }
    }
}

/**
 * Daily statistics
 */
data class DailyStats(
    val date: String,
    val impressions: Long = 0,
    val clicks: Long = 0,
    val completions: Long = 0,
    val interactions: Long = 0,
    val cost: BigDecimal = BigDecimal.ZERO
)

/**
 * Hourly statistics
 */
data class HourlyStats(
    val hour: String, // Format: "yyyy-MM-dd-HH"
    val impressions: Long = 0,
    val clicks: Long = 0,
    val completions: Long = 0,
    val interactions: Long = 0,
    val cost: BigDecimal = BigDecimal.ZERO
)

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val impressions: Long,
    val clicks: Long,
    val completions: Long,
    val interactions: Long,
    val skips: Long,
    val errors: Long,
    val uniqueUsers: Long,
    val clickThroughRate: Double,
    val completionRate: Double,
    val interactionRate: Double,
    val skipRate: Double,
    val errorRate: Double,
    val averageViewDuration: Double,
    val totalCost: BigDecimal
)