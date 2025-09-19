package com.gasolinerajsm.adengine.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class AdSelectionRequest(
    @field:NotNull(message = "User ID cannot be null")
    @field:Positive(message = "User ID must be positive")
    val userId: Long,

    @field:NotNull(message = "Station ID cannot be null")
    @field:Positive(message = "Station ID must be positive")
    val stationId: Long
)