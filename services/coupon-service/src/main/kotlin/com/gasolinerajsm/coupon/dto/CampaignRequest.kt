package com.gasolinerajsm.coupon.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CampaignRequest(
    @field:NotBlank(message = "Name cannot be blank")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Start date cannot be null")
    val startDate: Instant,

    @field:NotNull(message = "End date cannot be null")
    val endDate: Instant,

    val isActive: Boolean? = true,

    val discountPercentage: Double? = null
)
