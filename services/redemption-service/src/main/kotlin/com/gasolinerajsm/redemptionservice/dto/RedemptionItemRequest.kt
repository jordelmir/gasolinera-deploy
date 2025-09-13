package com.gasolinerajsm.redemptionservice.dto

import java.math.BigDecimal

/**
 * Simple item request DTO for redemption items
 */
data class RedemptionItemRequest(
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal
)