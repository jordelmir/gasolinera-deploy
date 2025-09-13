package com.gasolinerajsm.coupon.dto

import java.time.Instant

data class CampaignResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val startDate: Instant,
    val endDate: Instant,
    val isActive: Boolean,
    val discountPercentage: Double?,
    val createdAt: Instant,
    val updatedAt: Instant
)
