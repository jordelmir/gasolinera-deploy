package com.gasolinerajsm.raffleservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * RafflePrize entity representing prizes available in a raffle
 */
@Entity
@Table(
    name = "raffle_prizes",
    schema = "raffle_schema",
    indexes = [
        Index(name = "idx_raffle_prizes_raffle_id", columnList = "raffle_id"),
        Index(name = "idx_raffle_prizes_prize_tier", columnList = "prize_tier"),
        Index(name = "idx_raffle_prizes_status", columnList = "status"),
        Index(name = "idx_raffle_prizes_created_at", columnList = "created_at")
    ]
)
data class RafflePrize(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raffle_id", nullable = false)
    @field:NotNull(message = "Raffle is required")
    val raffle: Raffle,

    @Column(name = "name", nullable = false, length = 200)
    @field:NotBlank(message = "Prize name is required")
    @field:Size(min = 2, max = 200, message = "Prize name must be between 2 and 200 characters")
    val name: String,

    @Column(name = "description", length = 1000)
    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "prize_type", nullable = false, length = 30)
    val prizeType: PrizeType,

    @Column(name = "value", precision = 12, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Prize value must be positive")
    val value: BigDecimal? = null,

    @Column(name = "currency", length = 3)
    val currency: String = "USD",

    @Column(name = "prize_tier", nullable = false)
    @field:Min(value = 1, message = "Prize tier must be at least 1")
    val prizeTier: Int = 1,

    @Column(name = "quantity_available", nullable = false)
    @field:Min(value = 1, message = "Quantity available must be at least 1")
    val quantityAvailable: Int = 1,

    @Column(name = "quantity_awarded", nullable = false)
    val quantityAwarded: Int = 0,

    @Column(name = "winning_probability", precision = 8, scale = 6)
    @field:DecimalMin(value = "0.0", message = "Winning probability must be positive")
    @field:DecimalMax(value = "1.0", message = "Winning probability cannot exceed 1.0")
    val winningProbability: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: PrizeStatus = PrizeStatus.ACTIVE,

    @Column(name = "image_url", length = 500)
    val imageUrl: String? = null,

    @Column(name = "terms_and_conditions", length = 2000)
    val termsAndConditions: String? = null,

    @Column(name = "expiry_date")
    val expiryDate: LocalDateTime? = null,

    @Column(name = "redemption_instructions", length = 1000)
    val redemptionInstructions: String? = null,

    @Column(name = "sponsor_name", length = 200)
    val sponsorName: String? = null,

    @Column(name = "sponsor_contact", length = 200)
    val sponsorContact: String? = null,

    @Column(name = "delivery_method", length = 50)
    val deliveryMethod: String? = null,

    @Column(name = "is_transferable", nullable = false)
    val isTransferable: Boolean = false,

    @Column(name = "requires_identity_verification", nullable = false)
    val requiresIdentityVerification: Boolean = false,

    @Column(name = "metadata", length = 1000)
    val metadata: String? = null,

    @OneToMany(mappedBy = "prize", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val winners: List<RaffleWinner> = emptyList(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if prize is available for awarding
     */
    fun isAvailable(): Boolean {
        return status == PrizeStatus.ACTIVE &&
               quantityAwarded < quantityAvailable &&
               (expiryDate?.isAfter(LocalDateTime.now()) ?: true)
    }

    /**
     * Check if prize is fully awarded
     */
    fun isFullyAwarded(): Boolean {
        return quantityAwarded >= quantityAvailable
    }

    /**
     * Get remaining quantity
     */
    fun getRemainingQuantity(): Int {
        return maxOf(0, quantityAvailable - quantityAwarded)
    }

    /**
     * Get award percentage
     */
    fun getAwardPercentage(): Double {
        return if (quantityAvailable > 0) {
            (quantityAwarded.toDouble() / quantityAvailable.toDouble()) * 100
        } else 0.0
    }

    /**
     * Check if prize has expired
     */
    fun isExpired(): Boolean {
        return expiryDate?.isBefore(LocalDateTime.now()) ?: false
    }

    /**
     * Check if prize is monetary
     */
    fun isMonetary(): Boolean {
        return prizeType == PrizeType.CASH || prizeType == PrizeType.GIFT_CARD || prizeType == PrizeType.CREDIT
    }

    /**
     * Check if prize is physical
     */
    fun isPhysical(): Boolean {
        return prizeType == PrizeType.PHYSICAL_ITEM || prizeType == PrizeType.MERCHANDISE
    }

    /**
     * Check if prize requires shipping
     */
    fun requiresShipping(): Boolean {
        return isPhysical() && deliveryMethod?.contains("shipping", ignoreCase = true) == true
    }

    /**
     * Award a prize (increment awarded quantity)
     */
    fun award(): RafflePrize {
        if (!isAvailable()) {
            throw IllegalStateException("Prize is not available for awarding")
        }
        return this.copy(quantityAwarded = quantityAwarded + 1)
    }

    /**
     * Deactivate the prize
     */
    fun deactivate(): RafflePrize {
        return this.copy(status = PrizeStatus.INACTIVE)
    }

    /**
     * Expire the prize
     */
    fun expire(): RafflePrize {
        return this.copy(status = PrizeStatus.EXPIRED)
    }

    /**
     * Get formatted value display
     */
    fun getFormattedValue(): String? {
        return value?.let { "$currency $it" }
    }

    /**
     * Get prize tier display
     */
    fun getTierDisplay(): String {
        return when (prizeTier) {
            1 -> "1st Prize"
            2 -> "2nd Prize"
            3 -> "3rd Prize"
            else -> "${prizeTier}th Prize"
        }
    }

    override fun toString(): String {
        return "RafflePrize(id=$id, name='$name', type=$prizeType, tier=$prizeTier, value=$value, available=${getRemainingQuantity()})"
    }
}

/**
 * Prize type enumeration
 */
enum class PrizeType(val displayName: String, val description: String) {
    CASH("Cash Prize", "Monetary cash prize"),
    GIFT_CARD("Gift Card", "Gift card or voucher"),
    CREDIT("Account Credit", "Credit to user account"),
    PHYSICAL_ITEM("Physical Item", "Physical product or item"),
    MERCHANDISE("Merchandise", "Branded merchandise"),
    SERVICE("Service", "Service or experience"),
    DISCOUNT("Discount", "Discount coupon or offer"),
    POINTS("Points", "Loyalty or reward points"),
    FUEL_CREDIT("Fuel Credit", "Free fuel or fuel discount"),
    OTHER("Other", "Other type of prize");

    /**
     * Check if prize type is digital
     */
    fun isDigital(): Boolean {
        return this == GIFT_CARD || this == CREDIT || this == DISCOUNT || this == POINTS || this == FUEL_CREDIT
    }

    /**
     * Check if prize type requires physical delivery
     */
    fun requiresPhysicalDelivery(): Boolean {
        return this == PHYSICAL_ITEM || this == MERCHANDISE
    }

    /**
     * Check if prize type has monetary value
     */
    fun hasMonetaryValue(): Boolean {
        return this == CASH || this == GIFT_CARD || this == CREDIT || this == FUEL_CREDIT
    }
}

/**
 * Prize status enumeration
 */
enum class PrizeStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Prize is active and available"),
    INACTIVE("Inactive", "Prize is temporarily inactive"),
    EXPIRED("Expired", "Prize has expired"),
    EXHAUSTED("Exhausted", "All prize quantities have been awarded"),
    CANCELLED("Cancelled", "Prize has been cancelled");

    /**
     * Check if status allows awarding
     */
    fun allowsAwarding(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this == EXPIRED || this == EXHAUSTED || this == CANCELLED
    }
}