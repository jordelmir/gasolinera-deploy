package com.gasolinerajsm.stationservice.infrastructure.persistence.entity

import com.gasolinerajsm.stationservice.domain.model.DayOfWeek
import com.gasolinerajsm.stationservice.domain.model.FuelType
import com.gasolinerajsm.stationservice.domain.model.StationAmenity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

/**
 * JPA Entity for Station Operating Hours
 */
@Entity
@Table(name = "station_operating_hours")
data class OperatingHoursEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    val station: StationEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: DayOfWeek,

    @Column(name = "open_time")
    val openTime: LocalTime,

    @Column(name = "close_time")
    val closeTime: LocalTime,

    @Column(name = "is_open", nullable = false)
    val isOpen: Boolean = true
)

/**
 * JPA Entity for Fuel Prices
 */
@Entity
@Table(name = "station_fuel_prices")
data class FuelPriceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    val station: StationEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    val fuelType: FuelType,

    @Column(name = "price", nullable = false, precision = 10, scale = 3)
    val price: BigDecimal,

    @Column(name = "last_updated", nullable = false)
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

/**
 * JPA Entity for Station Amenities
 */
@Entity
@Table(name = "station_amenities")
data class StationAmenityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    val station: StationEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", nullable = false)
    val amenity: StationAmenity
)