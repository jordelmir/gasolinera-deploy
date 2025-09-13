package com.gasolinerajsm.couponservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Campaign entity representing a coupon campaign
 */
@Entity
@Table(
    name = "campaigns",
    schema = "coupon_schema",
    indexes = [
        Index(name = "idx_campaigns_name", columnList = "name"),
        Index(name = "idx_campaigns_active", columnList = "is_active"),
        Index(name = "idx_campaigns_start_date", columnList = "start_date"),
        Index(name = "idx_campaigns_end_date", columnList = "end_date")
    ]
)
data class Campaign(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "name", nullable = false, length = 200)
    @field:NotBlank(message = "Campaign name is required")
    @field:Size(max = 200, message = "Campaign name must not exceed 200 characters")
    val name: String,

    @Column(name = "description", length = 1000)
    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @field:NotNull(message = "Discount amount is required")
    @field:DecimalMin(value = "0.0", message = "Discount amount must be positive")
    val discountAmount: BigDecimal,

    @Column(name = "raffle_tickets_per_coupon", nullable = false)
    @field:NotNull(message = "Raffle tickets per coupon is required")
    @field:Min(value = 0, message = "Raffle tickets per coupon must be non-negative")
    val raffleTicketsPerCoupon: Int,

    @Column(name = "start_date", nullable = false)
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDateTime,

    @Column(name = "end_date", nullable = false)
    @field:NotNull(message = "End date is required")
    val endDate: LocalDateTime,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if the campaign is currently active
     */
    fun isCurrentlyActive(): Boolean {
        val now = LocalDateTime.now()
        return isActive &&
               now.isAfter(startDate) &&
               now.isBefore(endDate)
    }

    /**
     * Check if the campaign is expired
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(endDate)
    }

    /**
     * Check if the campaign has started
     */
    fun hasStarted(): Boolean {
        return LocalDateTime.now().isAfter(startDate)
    }

    /**
     * Activate the campaign
     */
    fun activate(): Campaign {
        return this.copy(isActive = true)
    }

    /**
     * Deactivate the campaign
     */
    fun deactivate(): Campaign {
        return this.copy(isActive = false)
    }
}