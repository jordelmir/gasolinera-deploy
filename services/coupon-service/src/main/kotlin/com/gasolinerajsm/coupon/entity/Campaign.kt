package com.gasolinerajsm.coupon.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "campaigns")
data class Campaign(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", unique = true, nullable = false)
    val name: String,

    @Column(name = "description")
    val description: String? = null,

    @Column(name = "start_date", nullable = false)
    val startDate: Instant,

    @Column(name = "end_date", nullable = false)
    val endDate: Instant,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "discount_percentage")
    val discountPercentage: Double? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}
