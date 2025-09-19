package com.gasolinerajsm.redemptionservice.domain.model

import com.gasolinerajsm.redemptionservice.domain.event.DomainEvent
import com.gasolinerajsm.redemptionservice.domain.event.RedemptionCreatedEvent
import com.gasolinerajsm.redemptionservice.domain.event.RedemptionCompletedEvent
import com.gasolinerajsm.redemptionservice.domain.event.RedemptionFailedEvent
import com.gasolinerajsm.redemptionservice.domain.valueobject.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Redemption Domain Entity - Core business logic
 * Represents a coupon redemption transaction with complete lifecycle management
 */
data class Redemption(
    val id: RedemptionId,
    val userId: UserId,
    val stationId: StationId,
    val employeeId: EmployeeId,
    val couponId: CouponId,
    val campaignId: CampaignId,
    val transactionReference: TransactionReference,
    val status: RedemptionStatus = RedemptionStatus.PENDING,
    val couponDetails: CouponDetails,
    val purchaseDetails: PurchaseDetails,
    val discountApplied: DiscountApplied,
    val raffleTicketsEarned: Int = 0,
    val paymentInfo: PaymentInfo? = null,
    val timestamps: RedemptionTimestamps,
    val failureReason: String? = null,
    val notes: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new redemption
         */
        fun create(
            userId: UserId,
            stationId: StationId,
            employeeId: EmployeeId,
            couponId: CouponId,
            campaignId: CampaignId,
            transactionReference: TransactionReference,
            couponDetails: CouponDetails,
            purchaseDetails: PurchaseDetails,
            discountApplied: DiscountApplied,
            raffleTicketsEarned: Int = 0,
            paymentInfo: PaymentInfo? = null,
            metadata: Map<String, String> = emptyMap()
        ): Redemption {
            val redemption = Redemption(
                id = RedemptionId.generate(),
                userId = userId,
                stationId = stationId,
                employeeId = employeeId,
                couponId = couponId,
                campaignId = campaignId,
                transactionReference = transactionReference,
                couponDetails = couponDetails,
                purchaseDetails = purchaseDetails,
                discountApplied = discountApplied,
                raffleTicketsEarned = raffleTicketsEarned,
                paymentInfo = paymentInfo,
                timestamps = RedemptionTimestamps.create(),
                metadata = metadata
            )

            redemption.addDomainEvent(
                RedemptionCreatedEvent(
                    redemptionId = redemption.id,
                    userId = redemption.userId,
                    stationId = redemption.stationId,
                    couponId = redemption.couponId,
                    campaignId = redemption.campaignId,
                    purchaseAmount = redemption.purchaseDetails.totalAmount,
                    discountAmount = redemption.discountApplied.amount,
                    raffleTicketsEarned = redemption.raffleTicketsEarned,
                    occurredAt = LocalDateTime.now()
                )
            )

            return redemption
        }
    }

    /**
     * Check if redemption is successful
     */
    fun isSuccessful(): Boolean = status == RedemptionStatus.COMPLETED

    /**
     * Check if redemption is pending
     */
    fun isPending(): Boolean = status == RedemptionStatus.PENDING

    /**
     * Check if redemption is in progress
     */
    fun isInProgress(): Boolean = status == RedemptionStatus.IN_PROGRESS

    /**
     * Check if redemption has failed
     */
    fun hasFailed(): Boolean = status == RedemptionStatus.FAILED

    /**
     * Check if redemption was cancelled
     */
    fun isCancelled(): Boolean = status == RedemptionStatus.CANCELLED

    /**
     * Check if redemption is in a final state
     */
    fun isInFinalState(): Boolean = status.isFinalState()

    /**
     * Start processing the redemption
     */
    fun startProcessing(): Redemption {
        if (status != RedemptionStatus.PENDING) {
            throw IllegalStateException("Cannot start processing redemption in status: $status")
        }

        return this.copy(
            status = RedemptionStatus.IN_PROGRESS,
            timestamps = timestamps.markRedemptionStarted(),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Complete the redemption successfully
     */
    fun complete(): Redemption {
        if (status != RedemptionStatus.IN_PROGRESS) {
            throw IllegalStateException("Cannot complete redemption in status: $status")
        }

        val completedRedemption = this.copy(
            status = RedemptionStatus.COMPLETED,
            timestamps = timestamps.markCompleted(),
            updatedAt = LocalDateTime.now()
        )

        completedRedemption.addDomainEvent(
            RedemptionCompletedEvent(
                redemptionId = id,
                userId = userId,
                stationId = stationId,
                couponId = couponId,
                campaignId = campaignId,
                finalAmount = purchaseDetails.totalAmount - discountApplied.amount,
                discountAmount = discountApplied.amount,
                raffleTicketsEarned = raffleTicketsEarned,
                processingDuration = timestamps.getProcessingDuration(),
                occurredAt = LocalDateTime.now()
            )
        )

        return completedRedemption
    }

    /**
     * Fail the redemption with a reason
     */
    fun fail(reason: String): Redemption {
        if (isInFinalState()) {
            throw IllegalStateException("Cannot fail redemption in final state: $status")
        }

        val failedRedemption = this.copy(
            status = RedemptionStatus.FAILED,
            failureReason = reason,
            timestamps = timestamps.markCompleted(),
            updatedAt = LocalDateTime.now()
        )

        failedRedemption.addDomainEvent(
            RedemptionFailedEvent(
                redemptionId = id,
                userId = userId,
                stationId = stationId,
                couponId = couponId,
                failureReason = reason,
                attemptedAmount = purchaseDetails.totalAmount,
                occurredAt = LocalDateTime.now()
            )
        )

        return failedRedemption
    }

    /**
     * Cancel the redemption
     */
    fun cancel(reason: String? = null): Redemption {
        if (isInFinalState()) {
            throw IllegalStateException("Cannot cancel redemption in final state: $status")
        }

        return this.copy(
            status = RedemptionStatus.CANCELLED,
            failureReason = reason,
            timestamps = timestamps.markCompleted(),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Add notes to the redemption
     */
    fun addNotes(additionalNotes: String): Redemption {
        val updatedNotes = if (notes.isNullOrBlank()) {
            additionalNotes
        } else {
            "$notes\n$additionalNotes"
        }

        return this.copy(
            notes = updatedNotes,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update metadata
     */
    fun updateMetadata(newMetadata: Map<String, String>): Redemption {
        return this.copy(
            metadata = metadata + newMetadata,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Get final amount after discount
     */
    fun getFinalAmount(): BigDecimal {
        return purchaseDetails.totalAmount - discountApplied.amount
    }

    /**
     * Get discount percentage
     */
    fun getDiscountPercentage(): BigDecimal {
        return if (purchaseDetails.totalAmount > BigDecimal.ZERO) {
            (discountApplied.amount / purchaseDetails.totalAmount) * BigDecimal("100")
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Get processing duration in seconds
     */
    fun getProcessingDurationSeconds(): Long? {
        return timestamps.getProcessingDuration()?.seconds
    }

    /**
     * Check if redemption has fuel transaction
     */
    fun hasFuelTransaction(): Boolean {
        return purchaseDetails.fuelDetails != null
    }

    /**
     * Check if redemption is eligible for raffle tickets
     */
    fun isEligibleForRaffleTickets(): Boolean {
        return isSuccessful() && raffleTicketsEarned > 0
    }

    /**
     * Validate redemption business rules
     */
    fun validateBusinessRules(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate amounts
        if (purchaseDetails.totalAmount <= BigDecimal.ZERO) {
            errors.add("Purchase amount must be positive")
        }

        if (discountApplied.amount < BigDecimal.ZERO) {
            errors.add("Discount amount cannot be negative")
        }

        if (discountApplied.amount > purchaseDetails.totalAmount) {
            errors.add("Discount amount cannot exceed purchase amount")
        }

        // Validate raffle tickets
        if (raffleTicketsEarned < 0) {
            errors.add("Raffle tickets earned cannot be negative")
        }

        // Validate fuel details if present
        purchaseDetails.fuelDetails?.let { fuelDetails ->
            if (fuelDetails.quantity <= BigDecimal.ZERO) {
                errors.add("Fuel quantity must be positive")
            }
            if (fuelDetails.pricePerUnit <= BigDecimal.ZERO) {
                errors.add("Fuel price per unit must be positive")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Redemption is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "Redemption(id=$id, userId=$userId, stationId=$stationId, status=$status, amount=${purchaseDetails.totalAmount})"
    }
}

/**
 * Validation result for domain operations
 */
data class ValidationResult(
    val isSuccess: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = ValidationResult(true, message)
        fun failure(message: String) = ValidationResult(false, message)
    }
}