package com.gasolinerajsm.adengine.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Simplified Ad Sequence entity
 */
@Entity
@Table(name = "ad_sequences")
data class AdSequence(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val userId: String = "",

    @Column(nullable = false)
    val stationId: String = "",

    @Column(nullable = false)
    val currentStep: Int = 1,

    @Column(nullable = false)
    val totalSteps: Int = 5,

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val completedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val rewardEarned: Int = 0,

    @Column(nullable = false)
    val rewardClaimed: Boolean = false,

    @Column
    val sessionId: String? = null
)

enum class SequenceStatus {
    ACTIVE,     // Secuencia activa
    COMPLETED,  // Usuario completó todos los anuncios
    ABANDONED,  // Usuario abandonó la secuencia
    EXPIRED     // Secuencia expiró
}

// Configuración de duración de anuncios por paso
object AdDurationConfig {
    val DURATIONS = mapOf(
        1 to 10,   // 10 segundos
        2 to 15,   // 15 segundos
        3 to 30,   // 30 segundos
        4 to 60,   // 1 minuto
        5 to 120,  // 2 minutos
        6 to 180,  // 3 minutos
        7 to 240,  // 4 minutos
        8 to 300,  // 5 minutos
        9 to 420,  // 7 minutos
        10 to 600  // 10 minutos
    )

    fun getDuration(step: Int): Int = DURATIONS[step] ?: 600

    fun getTicketMultiplier(step: Int): Int = step // Cada paso duplica los tickets base
}