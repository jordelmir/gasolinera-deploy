package com.gasolinerajsm.redemptionservice.domain.valueobject

import com.gasolinerajsm.redemptionservice.domain.model.*
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

/**
 * Transaction Reference Value Object
 */
@JvmInline
value class TransactionReference(val value: String) {
    init {
        require(value.isNotBlank()) { "Transaction reference cannot be blank" }
        require(value.length <= 100) { "Transaction reference cannot exceed 100 characters" }
        require(value.matches(Regex("^[A-Z0-9-_]+$"))) { "Transaction reference contains invalid characters" }
    }

    companion object {
        fun generate(prefix: String = "TXN"): TransactionReference {
            val timestamp = System.currentTimeMillis()
            val random = SecureRandom().nextInt(10000).toString().padStart(4, '0')
            return TransactionReference("$prefix-$timestamp-$random")
        }

        fun from(value: String): TransactionReference = TransactionReference(value.uppercase())
    }

    override fun toString(): String = value
}

/**
 * Ticket Number Value Object
 */
@JvmInline
value class TicketNumber(val value: String) {
    init {
        require(value.isNotBlank()) { "Ticket number cannot be blank" }
        require(value.length <= 50) { "Ticket number cannot exceed 50 characters" }
        require(value.matches(Regex("^[A-Z0-9-]+$"))) { "Ticket number contains invalid characters" }
    }

    companion object {
        private val secureRandom = SecureRandom()

        fun generate(prefix: String = "TKT"): TicketNumber {
            val timestamp = System.currentTimeMillis().toString().takeLast(8)
            val random = (1..6).map {
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"[secureRandom.nextInt(36)]
            }.joinToString("")
            return TicketNumber("$prefix-$timestamp-$random")
        }

        fun from(value: String): TicketNumber = TicketNumber(value.uppercase())
    }

    fun getFormatted(): String = "#$value"

    override fun toString(): String = value
}

/**
 * Coupon Details Value Object
 */
data class CouponDetails(
    val qrCode: String,
    val couponCode: String,
    val discountType: DiscountType,
    val originalDiscountAmount: BigDecimal
) {
    init {
        require(qrCode.isNotBlank()) { "QR code cannot be blank" }
        require(couponCode.isNotBlank()) { "Coupon code cannot be blank" }
        require(originalDiscountAmount >= BigDecimal.ZERO) { "Original discount amount cannot be negative" }
    }

    fun getMaskedCouponCode(): String {
        return when {
            couponCode.length <= 4 -> "*".repeat(couponCode.length)
            else -> "${couponCode.take(2)}${"*".repeat(couponCode.length - 4)}${couponCode.takeLast(2)}"
        }
    }
}

/**
 * Purchase Details Value Object
 */
data class PurchaseDetails(
    val totalAmount: BigDecimal,
    val fuelDetails: FuelDetails? = null,
    val items: List<PurchaseItem> = emptyList()
) {
    init {
        require(totalAmount > BigDecimal.ZERO) { "Total amount must be positive" }

        // Validate that fuel details amount matches if present
        fuelDetails?.let { fuel ->
            val fuelTotal = fuel.quantity * fuel.pricePerUnit
            if (items.isEmpty() && fuelTotal != totalAmount) {
                throw IllegalArgumentException("Fuel total does not match purchase total")
            }
        }
    }

    fun hasFuelPurchase(): Boolean = fuelDetails != null

    fun hasItems(): Boolean = items.isNotEmpty()

    fun getItemsTotal(): BigDecimal = items.sumOf { it.totalPrice }

    fun getFuelTotal(): BigDecimal = fuelDetails?.let { it.quantity * it.pricePerUnit } ?: BigDecimal.ZERO
}

/**
 * Fuel Details Value Object
 */
data class FuelDetails(
    val fuelType: FuelType,
    val quantity: BigDecimal,
    val pricePerUnit: BigDecimal
) {
    init {
        require(quantity > BigDecimal.ZERO) { "Fuel quantity must be positive" }
        require(pricePerUnit > BigDecimal.ZERO) { "Fuel price per unit must be positive" }
    }

    fun getTotalAmount(): BigDecimal = quantity * pricePerUnit

    fun getFormattedQuantity(): String = "${quantity.setScale(3)} L"

    fun getFormattedPrice(): String = "$${pricePerUnit.setScale(3)}/L"
}

/**
 * Purchase Item Value Object
 */
data class PurchaseItem(
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal
) {
    init {
        require(name.isNotBlank()) { "Item name cannot be blank" }
        require(quantity > 0) { "Item quantity must be positive" }
        require(unitPrice >= BigDecimal.ZERO) { "Unit price cannot be negative" }
        require(totalPrice >= BigDecimal.ZERO) { "Total price cannot be negative" }
        require(totalPrice == unitPrice * BigDecimal(quantity)) { "Total price must equal unit price * quantity" }
    }
}

/**
 * Discount Applied Value Object
 */
data class DiscountApplied(
    val type: DiscountType,
    val amount: BigDecimal,
    val percentage: BigDecimal? = null,
    val description: String? = null
) {
    init {
        require(amount >= BigDecimal.ZERO) { "Discount amount cannot be negative" }

        when (type) {
            DiscountType.PERCENTAGE -> {
                require(percentage != null) { "Percentage must be provided for percentage discount" }
                require(percentage >= BigDecimal.ZERO && percentage <= BigDecimal(100)) {
                    "Percentage must be between 0 and 100"
                }
            }
            DiscountType.FIXED_AMOUNT, DiscountType.FUEL_DISCOUNT, DiscountType.CASHBACK -> {
                require(amount > BigDecimal.ZERO) { "Amount must be positive for ${type.displayName}" }
            }
            DiscountType.NONE -> {
                require(amount == BigDecimal.ZERO) { "Amount must be zero for no discount" }
            }
        }
    }

    fun getDisplayString(): String {
        return when (type) {
            DiscountType.FIXED_AMOUNT, DiscountType.FUEL_DISCOUNT, DiscountType.CASHBACK ->
                "$${amount.setScale(2)}"
            DiscountType.PERCENTAGE ->
                "${percentage?.setScale(1)}% (${amount.setScale(2)})"
            DiscountType.NONE ->
                "No discount"
        }
    }
}

/**
 * Payment Info Value Object
 */
data class PaymentInfo(
    val method: PaymentMethod,
    val reference: String? = null,
    val lastFourDigits: String? = null,
    val authorizationCode: String? = null
) {
    init {
        lastFourDigits?.let { digits ->
            require(digits.matches(Regex("^\\d{4}$"))) { "Last four digits must be exactly 4 digits" }
        }
    }

    fun getMaskedReference(): String? {
        return reference?.let { ref ->
            when {
                ref.length <= 4 -> "*".repeat(ref.length)
                else -> "${ref.take(2)}${"*".repeat(ref.length - 4)}${ref.takeLast(2)}"
            }
        }
    }
}

/**
 * Redemption Timestamps Value Object
 */
data class RedemptionTimestamps(
    val validationTimestamp: LocalDateTime,
    val redemptionStartedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null
) {

    companion object {
        fun create(): RedemptionTimestamps {
            return RedemptionTimestamps(validationTimestamp = LocalDateTime.now())
        }
    }

    fun markRedemptionStarted(): RedemptionTimestamps {
        return this.copy(redemptionStartedAt = LocalDateTime.now())
    }

    fun markCompleted(): RedemptionTimestamps {
        return this.copy(completedAt = LocalDateTime.now())
    }

    fun getProcessingDuration(): Duration? {
        return if (redemptionStartedAt != null && completedAt != null) {
            Duration.between(redemptionStartedAt, completedAt)
        } else null
    }

    fun getTotalDuration(): Duration? {
        return if (completedAt != null) {
            Duration.between(validationTimestamp, completedAt)
        } else null
    }
}

/**
 * Ticket Source Info Value Object
 */
data class TicketSourceInfo(
    val sourceType: TicketSourceType,
    val sourceReference: String? = null,
    val campaignId: CampaignId? = null,
    val stationId: StationId? = null
) {

    fun getDisplayString(): String {
        return buildString {
            append(sourceType.displayName)
            sourceReference?.let { append(" ($it)") }
        }
    }
}

/**
 * Raffle Info Value Object
 */
data class RaffleInfo(
    val raffleId: RaffleId,
    val raffleName: String,
    val isUsed: Boolean = false,
    val usedAt: LocalDateTime? = null
) {
    init {
        if (isUsed) {
            require(usedAt != null) { "Used at timestamp is required when ticket is used" }
        }
    }
}

/**
 * Prize Info Value Object
 */
data class PrizeInfo(
    val isWinner: Boolean = false,
    val prizeDescription: String? = null,
    val prizeValue: BigDecimal? = null,
    val isClaimed: Boolean = false,
    val wonAt: LocalDateTime? = null,
    val claimedAt: LocalDateTime? = null,
    val claimedBy: String? = null
) {
    init {
        if (isWinner) {
            require(prizeDescription != null) { "Prize description is required for winning tickets" }
            require(wonAt != null) { "Won at timestamp is required for winning tickets" }
        }

        if (isClaimed) {
            require(isWinner) { "Only winning tickets can have claimed prizes" }
            require(claimedAt != null) { "Claimed at timestamp is required when prize is claimed" }
        }
    }

    fun getFormattedPrizeValue(): String? {
        return prizeValue?.let { "$${it.setScale(2)}" }
    }
}

/**
 * Transfer Info Value Object
 */
data class TransferInfo(
    val transferCount: Int = 0,
    val lastTransferredTo: UserId? = null,
    val lastTransferDate: LocalDateTime? = null,
    val transferHistory: List<TransferRecord> = emptyList()
) {
    companion object {
        private const val MAX_TRANSFERS = 3

        fun initial(): TransferInfo = TransferInfo()
    }

    fun canTransfer(): Boolean = transferCount < MAX_TRANSFERS

    fun recordTransfer(toUserId: UserId, reason: String? = null): TransferInfo {
        if (!canTransfer()) {
            throw IllegalStateException("Maximum number of transfers exceeded")
        }

        val transferRecord = TransferRecord(
            toUserId = toUserId,
            transferredAt = LocalDateTime.now(),
            reason = reason
        )

        return this.copy(
            transferCount = transferCount + 1,
            lastTransferredTo = toUserId,
            lastTransferDate = LocalDateTime.now(),
            transferHistory = transferHistory + transferRecord
        )
    }
}

/**
 * Transfer Record Value Object
 */
data class TransferRecord(
    val toUserId: UserId,
    val transferredAt: LocalDateTime,
    val reason: String? = null
)

/**
 * Validation Info Value Object
 */
data class ValidationInfo(
    val isValidated: Boolean = false,
    val validationCode: String? = null,
    val validatedAt: LocalDateTime? = null,
    val validatedBy: String? = null
) {
    init {
        if (isValidated) {
            require(validationCode != null) { "Validation code is required when validated" }
            require(validatedAt != null) { "Validated at timestamp is required when validated" }
        }
    }
}