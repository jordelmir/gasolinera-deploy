package com.gasolinerajsm.raffleservice.dto

import com.gasolinerajsm.raffleservice.model.WinnerStatus
import java.time.LocalDateTime

/**
 * DTO for RaffleWinner entity
 */
data class RaffleWinnerDto(
    val id: Long = 0,
    val raffleId: Long,
    val raffleName: String? = null,
    val userId: Long,
    val ticketId: Long,
    val ticketNumber: String? = null,
    val prizeId: Long,
    val prizeName: String? = null,
    val prizeValue: String? = null,
    val prizeTier: Int? = null,
    val status: WinnerStatus,
    val wonAt: LocalDateTime,
    val notifiedAt: LocalDateTime? = null,
    val claimedAt: LocalDateTime? = null,
    val claimDeadline: LocalDateTime? = null,
    val verificationCode: String? = null,
    val isVerified: Boolean = false,
    val verifiedAt: LocalDateTime? = null,
    val verifiedBy: String? = null,
    val deliveryMethod: String? = null,
    val deliveryAddress: String? = null,
    val deliveryStatus: String? = null,
    val deliveredAt: LocalDateTime? = null,
    val trackingNumber: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val identityDocument: String? = null,
    val notes: String? = null,
    val processedBy: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,

    // Additional computed fields
    val isNotified: Boolean = false,
    val isClaimed: Boolean = false,
    val isClaimExpired: Boolean = false,
    val needsVerification: Boolean = false,
    val canClaimPrize: Boolean = false,
    val requiresDelivery: Boolean = false,
    val daysUntilDeadline: Long? = null,
    val hoursSinceWon: Long = 0,
    val daysSinceWon: Long = 0,
    val statusDisplay: String = "",
    val prizeSummary: String = ""
)