package com.gasolinerajsm.coupon.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class CouponGenerateRequest(
    @field:NotNull(message = "Value cannot be null")
    @field:Positive(message = "Value must be positive")
    val value: Double,

    @field:NotNull(message = "Station ID cannot be null")
    @field:Positive(message = "Station ID must be positive")
    val stationId: Long,

    @field:NotNull(message = "Employee ID cannot be null")
    @field:Positive(message = "Employee ID must be positive")
    val employeeId: Long
)
