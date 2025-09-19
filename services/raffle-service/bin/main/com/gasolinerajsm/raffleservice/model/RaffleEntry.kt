package com.gasolinerajsm.raffleservice.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "raffle_entries")
data class RaffleEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val raffleId: Long,
    val pointId: String, // ID del punto G
    val createdAt: LocalDateTime = LocalDateTime.now()
)
