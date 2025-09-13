package com.gasolinerajsm.stationservice.dto

import java.time.Instant

data class StationResponse(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
