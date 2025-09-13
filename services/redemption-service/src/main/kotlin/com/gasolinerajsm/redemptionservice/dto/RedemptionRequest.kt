package com.gasolinerajsm.redemptionservice.dto

import com.gasolinerajsm.redemptionservice.model.RedemptionType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Simple request DTO for creating a redemption
 */
data class RedemptionRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @field:NotNull(message = "Coupon ID is required")
    val couponId: Long,

    @field:NotNull(message = "Station ID is required")
    val stationId: Long,

    val employeeId: Long? = null,

    @field:NotNull(message = "Original value is required")
    @field:DecimalMin(value = "0.0", message = "Original value must be positive")
    val originalValue: BigDecimal,

    @field:NotNull(message = "Total amount is required")
    @field:DecimalMin(value = "0.0", message = "Total amount must be positive")
    val totalAmount: BigDecimal,

    val discountAmount: BigDecimal? = null,
    val campaignId: Long? = null,
    val metadata: String? = null
)