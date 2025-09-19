package com.gasolinerajsm.redemptionservice.dto

import com.gasolinerajsm.redemptionservice.model.Redemption
import com.gasolinerajsm.redemptionservice.model.RaffleTicket

/**
 * Simple result DTO for redemption operations
 */
data class RedemptionResult(
    val success: Boolean,
    val message: String,
    val redemption: Redemption? = null,
    val raffleTickets: List<RaffleTicket> = emptyList(),
    val errorCode: String? = null
) {
    companion object {
        fun success(
            redemption: Redemption,
            raffleTickets: List<RaffleTicket> = emptyList(),
            message: String = "Operation completed successfully"
        ): RedemptionResult {
            return RedemptionResult(
                success = true,
                message = message,
                redemption = redemption,
                raffleTickets = raffleTickets
            )
        }

        fun failure(
            message: String,
            errorCode: String? = null
        ): RedemptionResult {
            return RedemptionResult(
                success = false,
                message = message,
                errorCode = errorCode
            )
        }
    }
}