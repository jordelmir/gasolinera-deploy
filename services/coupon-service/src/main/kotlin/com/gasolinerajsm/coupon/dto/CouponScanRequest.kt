package com.gasolinerajsm.coupon.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CouponScanRequest(
    @field:NotBlank(message = "Coupon code cannot be blank")
    val code: String,

    @field:NotNull(message = "User ID cannot be null")
    val userId: UUID
)
