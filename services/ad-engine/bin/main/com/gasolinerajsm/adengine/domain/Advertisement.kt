package com.gasolinerajsm.adengine.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "advertisements")
@EntityListeners(AuditingEntityListener::class)
data class Advertisement(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val videoUrl: String,

    @Column(nullable = false)
    val duration: Int, // Duración en segundos

    @Column(nullable = false)
    val advertiserId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: AdCategory = AdCategory.GENERAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: AdStatus = AdStatus.ACTIVE,

    @Column(nullable = false)
    val priority: Int = 1, // 1 = alta, 5 = baja

    @Column(nullable = false)
    val maxViews: Int = -1, // -1 = ilimitado

    @Column(nullable = false)
    val currentViews: Int = 0,

    @Column
    val startDate: LocalDateTime? = null,

    @Column
    val endDate: LocalDateTime? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class AdCategory {
    AUTOMOTIVE,
    FOOD_BEVERAGE,
    TECHNOLOGY,
    RETAIL,
    SERVICES,
    GENERAL
}

enum class AdStatus {
    ACTIVE,
    PAUSED,
    EXPIRED,
    DRAFT
}