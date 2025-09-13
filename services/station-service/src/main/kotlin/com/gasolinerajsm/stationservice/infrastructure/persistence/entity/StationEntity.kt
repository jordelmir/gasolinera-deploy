package com.gasolinerajsm.stationservice.infrastructure.persistence.entity

import com.gasolinerajsm.stationservice.domain.model.*
import com.gasolinerajsm.stationservice.domain.valueobject.*
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

/**
 * JPA Entity for Station persistence
 */
@Entity
@Table(name = "stations")
data class StationEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 200)
    val name: String = "",

    @Column(name = "address", nullable = false, length = 500)
    val address: String = "",

    @Column(name = "latitude", nullable = false)
    val latitude: Double = 0.0,

    @Column(name = "longitude", nullable = false)
    val longitude: Double = 0.0,

    @Column(name = "phone_number", length = 20)
    val phoneNumber: String? = null,

    @Column(name = "email", length = 100)
    val email: String? = null,

    @Column(name = "manager_id", length = 50)
    val managerId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: StationStatus = StationStatus.ACTIVE,

    @Column(name = "is_24_hours", nullable = false)
    val is24Hours: Boolean = false,

    @Column(name = "fuel_pumps", nullable = false)
    val fuelPumps: Int = 0,

    @Column(name = "electric_charging_stations", nullable = false)
    val electricChargingStations: Int = 0,

    @Column(name = "parking_spaces", nullable = false)
    val parkingSpaces: Int = 0,

    @Column(name = "max_vehicles_per_hour", nullable = false)
    val maxVehiclesPerHour: Int = 0,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "station", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val operatingHours: Set<OperatingHoursEntity> = emptySet(),

    @OneToMany(mappedBy = "station", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val fuelPrices: Set<FuelPriceEntity> = emptySet(),

    @OneToMany(mappedBy = "station", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val amenities: Set<StationAmenityEntity> = emptySet()
) {

    /**
     * Convert to domain model
     */
    fun toDomain(): Station {
        val location = Location.of(latitude, longitude)

        val operatingHoursMap = operatingHours.associate { entity ->
            entity.dayOfWeek to DaySchedule(
                openTime = entity.openTime,
                closeTime = entity.closeTime,
                isOpen = entity.isOpen
            )
        }

        val operatingHoursVO = OperatingHours(
            schedule = operatingHoursMap,
            is24Hours = is24Hours
        )

        val capacity = StationCapacity(
            fuelPumps = fuelPumps,
            electricChargingStations = electricChargingStations,
            parkingSpaces = parkingSpaces,
            maxVehiclesPerHour = maxVehiclesPerHour
        )

        val fuelPricesMap = fuelPrices.associate { entity ->
            entity.fuelType to FuelPrice(
                amount = entity.price,
                lastUpdated = entity.lastUpdated
            )
        }

        val amenitiesSet = amenities.map { it.amenity }.toSet()

        return Station(
            id = StationId.from(id),
            name = name,
            address = address,
            location = location,
            phoneNumber = phoneNumber,
            email = email,
            managerId = managerId,
            status = status,
            operatingHours = operatingHoursVO,
            fuelPrices = fuelPricesMap,
            amenities = amenitiesSet,
            capacity = capacity,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomain(station: Station): StationEntity {
            return StationEntity(
                id = station.id.value,
                name = station.name,
                address = station.address,
                latitude = station.location.latitude,
                longitude = station.location.longitude,
                phoneNumber = station.phoneNumber,
                email = station.email,
                managerId = station.managerId,
                status = station.status,
                is24Hours = station.operatingHours.is24Hours,
                fuelPumps = station.capacity.fuelPumps,
                electricChargingStations = station.capacity.electricChargingStations,
                parkingSpaces = station.capacity.parkingSpaces,
                maxVehiclesPerHour = station.capacity.maxVehiclesPerHour,
                isActive = station.isActive,
                createdAt = station.createdAt,
                updatedAt = station.updatedAt
            )
        }
    }
}