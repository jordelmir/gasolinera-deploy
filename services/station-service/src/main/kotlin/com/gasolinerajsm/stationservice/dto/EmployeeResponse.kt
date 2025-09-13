package com.gasolinerajsm.stationservice.dto

import java.time.Instant

data class EmployeeResponse(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
    val stationId: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)
