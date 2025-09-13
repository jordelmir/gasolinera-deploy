package com.gasolinerajsm.coupon.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tickets")
data class Ticket(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "coupon_id", nullable = false)
    val couponId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "base_tickets", nullable = false)
    val baseTickets: Int,

    @Column(name = "ad_multiplier", nullable = false)
    var adMultiplier: Int = 1,

    @Column(name = "total_tickets", nullable = false)
    var totalTickets: Int = baseTickets * adMultiplier,

    @Column(name = "generated_at", nullable = false)
    val generatedAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TicketStatus = TicketStatus.ACTIVE
) {
    @PreUpdate
    fun preUpdate() {
        totalTickets = baseTickets * adMultiplier
    }
}
