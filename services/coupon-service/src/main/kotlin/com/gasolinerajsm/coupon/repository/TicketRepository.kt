package com.gasolinerajsm.coupon.repository

import com.gasolinerajsm.coupon.domain.Ticket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TicketRepository : JpaRepository<Ticket, UUID> {
    fun findByUserId(userId: UUID): List<Ticket>
    fun findByCouponId(couponId: UUID): List<Ticket>
}
