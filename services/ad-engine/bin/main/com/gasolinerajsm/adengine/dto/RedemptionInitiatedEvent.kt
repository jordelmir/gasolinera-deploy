package com.gasolinerajsm.adengine.dto

import java.util.UUID

data class RedemptionInitiatedEvent(
    val redemptionId: UUID,
    val userId: String,
    val stationId: String
)
