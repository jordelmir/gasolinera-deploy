package com.gasolinerajsm.redemptionservice.dto

import java.math.BigDecimal

/**
 * Simple statistics DTO for redemptions
 */
data class RedemptionStatistics(
    val totalRedemptions: Long,
    val totalAmount: BigDecimal,
    val totalTickets: Long,
    val averageAmount: BigDecimal
)