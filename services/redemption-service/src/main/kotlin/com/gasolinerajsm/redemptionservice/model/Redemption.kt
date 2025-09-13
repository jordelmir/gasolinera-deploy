package com.gasolinerajsm.redemptionservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Redemption entity representing a coupon redemption transaction
 */
@Entity
@Table(
    name = "redemptions",
    schema = "redemption_schema",
    indexes = [
        Index(name = "idx_redemptions_user_id", columnList = "user_id"),
        Index(name = "idx_redemptions_station_id", columnList = "station_id"),
        Index(name = "idx_redemptions_employee_id", columnList = "employee_id"),
        Index(name = "idx_redemptions_coupon_id", columnList = "coupon_id"),
        Index(name = "idx_redemptions_status", columnList = "status"),
        Index(name = "idx_redemptions_transaction_ref", columnList = "transaction_reference"),
        Index(name = "idx_redemptions_created_at", columnList = "created_at")
    ]
)
data class Redemption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @Column(name = "station_id", nullable = false)
    @field:NotNull(message = "Station ID is required")
    val stationId: Long,

    @Column(name = "employee_id", nullable = false)
    @field:NotNull(message = "Employee ID is required")
    val employeeId: Long,

    @Column(name = "coupon_id", nullable = false)
    @field:NotNull(message = "Coupon ID is required")
    val couponId: Long,

    @Column(name = "campaign_id", nullable = false)
    @field:NotNull(message = "Campaign ID is required")
    val campaignId: Long,

    @Column(name = "transaction_reference", unique = true, nullable = false, length = 100)
    @field:NotBlank(message = "Transaction reference is required")
    @field:Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    val transactionReference: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: RedemptionStatus = RedemptionStatus.PENDING,

    @Column(name = "qr_code", nullable = false, length = 500)
    @field:NotBlank(message = "QR code is required")
    @field:Size(max = 500, message = "QR code must not exceed 500 characters")
    val qrCode: String,

    @Column(name = "coupon_code", nullable = false, length = 50)
    @field:NotBlank(message = "Coupon code is required")
    @field:Size(max = 50, message = "Coupon code must not exceed 50 characters")
    val couponCode: String,

    @Column(name = "fuel_type", length = 50)
    @field:Size(max = 50, message = "Fuel type must not exceed 50 characters")
    val fuelType: String? = null,

    @Column(name = "fuel_quantity", precision = 8, scale = 3)
    @field:DecimalMin(value = "0.0", message = "Fuel quantity must be positive")
    val fuelQuantity: BigDecimal? = null,

    @Column(name = "fuel_price_per_unit", precision = 8, scale = 3)
    @field:DecimalMin(value = "0.0", message = "Fuel price per unit must be positive")
    val fuelPricePerUnit: BigDecimal? = null,

    @Column(name = "purchase_amount", precision = 10, scale = 2, nullable = false)
    @field:NotNull(message = "Purchase amount is required")
    @field:DecimalMin(value = "0.0", message = "Purchase amount must be positive")
    val purchaseAmount: BigDecimal,

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    @field:DecimalMin(value = "0.0", message = "Discount amount must be positive")
    val discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "final_amount", precision = 10, scale = 2, nullable = false)
    @field:DecimalMin(value = "0.0", message = "Final amount must be positive")
    val finalAmount: BigDecimal,

    @Column(name = "raffle_tickets_earned", nullable = false)
    @field:Min(value = 0, message = "Raffle tickets earned must be non-negative")
    val raffleTicketsEarned: Int = 0,

    @Column(name = "payment_method", length = 50)
    @field:Size(max = 50, message = "Payment method must not exceed 50 characters")
    val paymentMethod: String? = null,

    @Column(name = "payment_reference", length = 100)
    @field:Size(max = 100, message = "Payment reference must not exceed 100 characters")
    val paymentReference: String? = null,

    @Column(name = "validation_timestamp", nullable = false)
    @field:NotNull(message = "Validation timestamp is required")
    val validationTimestamp: LocalDateTime,

    @Column(name = "redemption_timestamp")
    val redemptionTimestamp: LocalDateTime? = null,

    @Column(name = "completion_timestamp")
    val completionTimestamp: LocalDateTime? = null,

    @Column(name = "failure_reason", length = 500)
    @field:Size(max = 500, message = "Failure reason must not exceed 500 characters")
    val failureReason: String? = null,

    @Column(name = "notes", length = 1000)
    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    @Column(name = "metadata", length = 2000)
    val metadata: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * Check if redemption is successful
     */
    fun isSuccessful(): Boolean {
        return status == RedemptionStatus.COMPLETED
    }

    /**
     * Check if redemption is pending
     */
    fun isPending(): Boolean {
        return status == RedemptionStatus.PENDING
    }

    /**
     * Check if redemption is in progress
     */
    fun isInProgress(): Boolean {
        return status == RedemptionStatus.IN_PROGRESS
    }

    /**
     * Check if redemption has failed
     */
    fun hasFailed(): Boolean {
        return status == RedemptionStatus.FAILED
    }

    /**
     * Check if redemption was cancelled
     */
    fun isCancelled(): Boolean {
        return status == RedemptionStatus.CANCELLED
    }

    /**
     * Check if redemption is in a final state
     */
    fun isInFinalState(): Boolean {
        return status.isFinalState()
    }

    /**
     * Get processing duration in seconds
     */
    fun getProcessingDurationSeconds(): Long? {
        return if (validationTimestamp != null && completionTimestamp != null) {
            java.time.Duration.between(validationTimestamp, completionTimestamp).seconds
        } else null
    }

    /**
     * Get discount percentage applied
     */
    fun getDiscountPercentage(): BigDecimal {
        return if (purchaseAmount > BigDecimal.ZERO) {
            (discountAmount / purchaseAmount) * BigDecimal("100")
        } else BigDecimal.ZERO
    }

    /**
     * Get savings amount
     */
    fun getSavingsAmount(): BigDecimal {
        return discountAmount
    }

    /**
     * Check if redemption has fuel transaction
     */
    fun hasFuelTransaction(): Boolean {
        return fuelType != null && fuelQuantity != null && fuelPricePerUnit != null
    }

    /**
     * Calculate fuel total amount
     */
    fun calculateFuelTotalAmount(): BigDecimal? {
        return if (fuelQuantity != null && fuelPricePerUnit != null) {
            fuelQuantity * fuelPricePerUnit
        } else null
    }

    /**
     * Start processing redemption
     */
    fun startProcessing(): Redemption {
        return this.copy(
            status = RedemptionStatus.IN_PROGRESS,
            redemptionTimestamp = LocalDateTime.now()
        )
    }

    /**
     * Complete redemption successfully
     */
    fun complete(): Redemption {
        return this.copy(
            status = RedemptionStatus.COMPLETED,
            completionTimestamp = LocalDateTime.now()
        )
    }

    /**
     * Fail redemption with reason
     */
    fun fail(reason: String): Redemption {
        return this.copy(
            status = RedemptionStatus.FAILED,
            failureReason = reason,
            completionTimestamp = LocalDateTime.now()
        )
    }

    /**
     * Cancel redemption
     */
    fun cancel(reason: String? = null): Redemption {
        return this.copy(
            status = RedemptionStatus.CANCELLED,
            failureReason = reason,
            completionTimestamp = LocalDateTime.now()
        )
    }

    /**
     * Add notes to redemption
     */
    fun addNotes(additionalNotes: String): Redemption {
        val updatedNotes = if (notes.isNullOrBlank()) {
            additionalNotes
        } else {
            "$notes\n$additionalNotes"
        }
        return this.copy(notes = updatedNotes)
    }

    override fun toString(): String {
        return "Redemption(id=$id, userId=$userId, stationId=$stationId, couponCode='$couponCode', status=$status, purchaseAmount=$purchaseAmount, discountAmount=$discountAmount)"
    }
}

/**
 * Redemption status enumeration
 */
enum class RedemptionStatus(val displayName: String, val description: String) {
    PENDING("Pending", "Redemption is waiting to be processed"),
    IN_PROGRESS("In Progress", "Redemption is currently being processed"),
    COMPLETED("Completed", "Redemption has been successfully completed"),
    FAILED("Failed", "Redemption has failed due to an error"),
    CANCELLED("Cancelled", "Redemption has been cancelled");

    /**
     * Check if the status is a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == FAILED || this == CANCELLED
    }

    /**
     * Check if the status allows modifications
     */
    fun allowsModifications(): Boolean {
        return this == PENDING
    }

    /**
     * Check if the status indicates success
     */
    fun isSuccessful(): Boolean {
        return this == COMPLETED
    }
}