package com.gasolinerajsm.stationservice.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class StationRequest(
    @field:NotBlank(message = "Name cannot be blank")
    val name: String,

    @field:NotBlank(message = "Address cannot be blank")
    val address: String,

    val latitude: Double? = null,
    val longitude: Double? = null,

    val isActive: Boolean? = true
)
