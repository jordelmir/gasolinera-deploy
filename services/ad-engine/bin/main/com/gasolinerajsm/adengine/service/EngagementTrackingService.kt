package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.domain.model.AdEngagement
import com.gasolinerajsm.adengine.domain.model.EngagementStatus
import com.gasolinerajsm.adengine.domain.model.EngagementType
import com.gasolinerajsm.adengine.domain.valueobject.*
import com.gasolinerajsm.adengine.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class EngagementTrackingService {

    // In-memory storage for demo purposes - in real implementation, this would be a repository
    private val engagements = mutableMapOf<EngagementId, AdEngagement>()

    fun trackView(
        engagementId: EngagementId,
        viewDurationSeconds: Int?
    ): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.copy(
            interactionData = engagement.interactionData.copy(
                viewDurationSeconds = viewDurationSeconds
            ),
            status = EngagementStatus.VIEWED,
            updatedAt = LocalDateTime.now()
        )
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun trackClick(
        engagementId: EngagementId,
        clickThroughUrl: String?
    ): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.copy(
            interactionData = engagement.interactionData.recordClick(clickThroughUrl),
            status = EngagementStatus.INTERACTED,
            updatedAt = LocalDateTime.now()
        )
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun trackInteraction(
        engagementId: EngagementId,
        interactionType: String
    ): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.copy(
            interactionData = engagement.interactionData.copy(
                interactionsCount = engagement.interactionData.interactionsCount + 1
            ),
            status = EngagementStatus.INTERACTED,
            updatedAt = LocalDateTime.now()
        )
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun trackCompletion(
        engagementId: EngagementId,
        completionPercentage: BigDecimal?,
        viewDurationSeconds: Int?
    ): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.complete(completionPercentage, viewDurationSeconds)
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun updateEngagementProgress(
        engagementId: EngagementId,
        viewDurationSeconds: Int,
        completionPercentage: BigDecimal?,
        interactions: Int,
        pauses: Int,
        replays: Int
    ): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.copy(
            interactionData = engagement.interactionData.copy(
                viewDurationSeconds = viewDurationSeconds,
                completionPercentage = completionPercentage,
                interactionsCount = engagement.interactionData.interactionsCount + interactions,
                pauseCount = engagement.interactionData.pauseCount + pauses,
                replayCount = engagement.interactionData.replayCount + replays
            ),
            updatedAt = LocalDateTime.now()
        )
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun trackSkip(engagementId: EngagementId): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.skip()
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun trackError(
        engagementId: EngagementId,
        errorMessage: String,
        errorCode: String?
    ): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.error(errorMessage, errorCode)
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun awardTickets(
        engagementId: EngagementId,
        baseTickets: Int,
        bonusTickets: Int,
        raffleEntryId: RaffleEntryId?
    ): AdEngagement {
        val engagement = engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
        val updatedEngagement = engagement.awardTickets(baseTickets, bonusTickets, raffleEntryId)
        engagements[engagementId] = updatedEngagement
        return updatedEngagement
    }

    fun getEngagementById(id: Long): AdEngagement {
        val engagementId = EngagementId.fromLong(id)
        return engagements[engagementId] ?: throw NoSuchElementException("Engagement not found")
    }

    fun getUserEngagements(userId: Long, pageable: Pageable): Page<AdEngagement> {
        val userEngagements = engagements.values
            .filter { it.userId.toLong() == userId }
            .sortedByDescending { it.createdAt }

        val start = pageable.pageNumber * pageable.pageSize
        val end = minOf(start + pageable.pageSize, userEngagements.size)
        val pageContent = if (start < userEngagements.size) userEngagements.subList(start, end) else emptyList()

        return PageImpl(pageContent, pageable, userEngagements.size.toLong())
    }

    fun getAdvertisementEngagements(advertisementId: Long, pageable: Pageable): Page<AdEngagement> {
        val adEngagements = engagements.values
            .filter { it.advertisementId.toLong() == advertisementId }
            .sortedByDescending { it.createdAt }

        val start = pageable.pageNumber * pageable.pageSize
        val end = minOf(start + pageable.pageSize, adEngagements.size)
        val pageContent = if (start < adEngagements.size) adEngagements.subList(start, end) else emptyList()

        return PageImpl(pageContent, pageable, adEngagements.size.toLong())
    }

    fun getUserEngagementStatistics(userId: Long): Map<String, Any> {
        val userEngagements = engagements.values.filter { it.userId.toLong() == userId }

        return mapOf(
            "totalEngagements" to userEngagements.size.toLong(),
            "completedEngagements" to userEngagements.count { it.isCompleted() }.toLong(),
            "clickedEngagements" to userEngagements.count { it.interactionData.clicked }.toLong(),
            "totalTicketsEarned" to userEngagements.sumOf { it.rewardData.totalTicketsEarned }.toLong(),
            "uniqueAdsEngaged" to userEngagements.map { it.advertisementId }.distinct().size.toLong(),
            "avgViewDuration" to if (userEngagements.isNotEmpty()) {
                userEngagements.mapNotNull { it.interactionData.viewDurationSeconds }.average()
            } else 0.0
        )
    }

    fun getDailyEngagementStatistics(): Map<String, Any> {
        val today = LocalDateTime.now().toLocalDate()
        val todayEngagements = engagements.values.filter {
            it.createdAt.toLocalDate() == today
        }

        return mapOf(
            "totalEngagements" to todayEngagements.size.toLong(),
            "impressions" to todayEngagements.size.toLong(),
            "clicks" to todayEngagements.count { it.interactionData.clicked }.toLong(),
            "completions" to todayEngagements.count { it.isCompleted() }.toLong(),
            "uniqueUsers" to todayEngagements.map { it.userId }.distinct().size.toLong(),
            "totalTicketsEarned" to todayEngagements.sumOf { it.rewardData.totalTicketsEarned }.toLong(),
            "totalCost" to BigDecimal.ZERO // Simplified
        )
    }

    fun getEngagementFunnel(
        advertisementId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Map<String, Any> {
        val adEngagements = engagements.values.filter {
            it.advertisementId.toLong() == advertisementId &&
            it.createdAt.isAfter(startDate) &&
            it.createdAt.isBefore(endDate)
        }

        val impressions = adEngagements.size.toLong()
        val views = adEngagements.count { it.status != EngagementStatus.STARTED }.toLong()
        val clicks = adEngagements.count { it.interactionData.clicked }.toLong()
        val completions = adEngagements.count { it.isCompleted() }.toLong()

        return mapOf(
            "impressions" to impressions,
            "views" to views,
            "clicks" to clicks,
            "completions" to completions
        )
    }

    // Helper method to create test engagements
    fun createTestEngagement(
        userId: UserId,
        advertisementId: AdvertisementId,
        engagementType: EngagementType = EngagementType.VIEW
    ): AdEngagement {
        val engagement = AdEngagement.start(
            userId = userId,
            advertisementId = advertisementId,
            sessionId = SessionId.generate(),
            engagementType = engagementType
        )
        engagements[engagement.id] = engagement
        return engagement
    }
}