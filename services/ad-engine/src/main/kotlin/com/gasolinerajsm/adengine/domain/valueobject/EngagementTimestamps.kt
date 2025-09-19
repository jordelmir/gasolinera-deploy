package com.gasolinerajsm.adengine.domain.valueobject

import java.time.LocalDateTime

/**
 * Value object representing timestamps for engagement lifecycle
 */
data class EngagementTimestamps(
    val startedAt: LocalDateTime,
    val lastActivityAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val skippedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(startedAt.isBefore(lastActivityAt) || startedAt.isEqual(lastActivityAt)) {
            "Started time must be before or equal to last activity time"
        }
        completedAt?.let {
            require(lastActivityAt.isBefore(it) || lastActivityAt.isEqual(it)) {
                "Last activity time must be before or equal to completion time"
            }
        }
        skippedAt?.let {
            require(lastActivityAt.isBefore(it) || lastActivityAt.isEqual(it)) {
                "Last activity time must be before or equal to skip time"
            }
        }
    }

    companion object {
        fun start(): EngagementTimestamps {
            val now = LocalDateTime.now()
            return EngagementTimestamps(
                startedAt = now,
                lastActivityAt = now
            )
        }
    }

    fun getDurationSeconds(): Long? {
        val endTime = completedAt ?: skippedAt ?: return null
        return java.time.Duration.between(startedAt, endTime).toSeconds()
    }

    fun duration(): Long? {
        return completedAt?.let {
            java.time.Duration.between(startedAt, it).toMillis()
        }
    }

    fun isCompleted(): Boolean = completedAt != null

    fun isSkipped(): Boolean = skippedAt != null

    fun updateLastActivity(): EngagementTimestamps {
        return copy(lastActivityAt = LocalDateTime.now())
    }

    fun markCompleted(): EngagementTimestamps {
        val now = LocalDateTime.now()
        return copy(
            lastActivityAt = now,
            completedAt = now
        )
    }

    fun markSkipped(): EngagementTimestamps {
        val now = LocalDateTime.now()
        return copy(
            lastActivityAt = now,
            skippedAt = now
        )
    }
}