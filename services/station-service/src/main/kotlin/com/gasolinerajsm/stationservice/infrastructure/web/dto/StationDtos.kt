package com.gasolinerajsm.stationservice.infrastructure.web.dto

import com.gasolinerajsm.stationservice.domain.model.*
import com.gasolinerajsm.stationservice.domain.valueobject.*
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * DTO for creating a new station
 */
data class CreateStationRequest(
    @field:NotBlank(message = "Station name is required")
    @field:Size(min = 3, max = 200, message = "Station name must be between 3 and 200 characters")
    val name: String,

    @field:NotBlank(message = "Address is required")
    @field:Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    val address: String,

    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val longitude: Double,

    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    val phoneNumber: String?,

    @field:Email(message = "Invalid email format")
    val email: String?,

    val managerId: String?,

    @field:Valid
    val operatingHours: OperatingHoursDto,

    @field:Valid
    val capacity: StationCapacityDto,

    val amenities: Set<StationAmenity> = emptySet(),

    val initialFuelPrices: Map<FuelType, BigDecimal> = emptyMap()
)

/**
 * DTO for updating station information
 */
data class UpdateStationRequest(
    @field:Size(min = 3, max = 200, message = "Station name must be between 3 and 200 characters")
    val name: String?,

    @field:Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    val address: String?,

    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val latitude: Double?,

    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val longitude: Double?,

    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    val phoneNumber: String?,

    @field:Email(message = "Invalid email format")
    val email: String?,

    val managerId: String?
)

/**
 * DTO for updating fuel price
 */
data class UpdateFuelPriceRequest(
    @field:NotNull(message = "Fuel type is required")
    val fuelType: FuelType,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @field:DecimalMax(value = "999.99", message = "Price cannot exceed 999.99")
    val price: BigDecimal
)

/**
 * DTO for station response
 */
data class StationResponse(
    val id: String,
    val name: String,
    val address: String,
    val location: LocationDto,
    val phoneNumber: String?,
    val email: String?,
    val managerId: String?,
    val status: StationStatus,
    val operatingHours: OperatingHoursDto,
    val fuelPrices: Map<FuelType, FuelPriceDto>,
    val amenities: Set<StationAmenity>,
    val capacity: StationCapacityDto,
    val isActive: Boolean,
    val isCurrentlyOpen: Boolean,
    val canProcessTransactions: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromDomain(station: Station): StationResponse {
            return StationResponse(
                id = station.id.toString(),
                name = station.name,
                address = station.address,
                location = LocationDto.fromDomain(station.location),
                phoneNumber = station.phoneNumber,
                email = station.email,
                managerId = station.managerId,
                status = station.status,
                operatingHours = OperatingHoursDto.fromDomain(station.operatingHours),
                fuelPrices = station.fuelPrices.mapValues { (_, price) ->
                    FuelPriceDto.fromDomain(price)
                },
                amenities = station.amenities,
                capacity = StationCapacityDto.fromDomain(station.capacity),
                isActive = station.isActive,
                isCurrentlyOpen = station.isCurrentlyOpen(),
                canProcessTransactions = station.canProcessTransactions(),
                createdAt = station.createdAt,
                updatedAt = station.updatedAt
            )
        }
    }
}

/**
 * DTO for location
 */
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val displayString: String
) {
    companion object {
        fun fromDomain(location: Location): LocationDto {
            return LocationDto(
                latitude = location.latitude,
                longitude = location.longitude,
                displayString = location.toDisplayString()
            )
        }
    }
}

/**
 * DTO for operating hours
 */
data class OperatingHoursDto(
    val is24Hours: Boolean,
    val schedule: Map<DayOfWeek, DayScheduleDto>
) {
    companion object {
        fun fromDomain(operatingHours: OperatingHours): OperatingHoursDto {
            return OperatingHoursDto(
                is24Hours = operatingHours.is24Hours,
                schedule = operatingHours.schedule.mapValues { (_, schedule) ->
                    DayScheduleDto.fromDomain(schedule)
                }
            )
        }
    }

    fun toDomain(): OperatingHours {
        return OperatingHours(
            is24Hours = is24Hours,
            schedule = schedule.mapValues { (_, dto) -> dto.toDomain() }
        )
    }
}

/**
 * DTO for day schedule
 */
data class DayScheduleDto(
    val openTime: LocalTime,
    val closeTime: LocalTime,
    val isOpen: Boolean,
    val operatingHours: Double
) {
    companion object {
        fun fromDomain(daySchedule: DaySchedule): DayScheduleDto {
            return DayScheduleDto(
                openTime = daySchedule.openTime,
                closeTime = daySchedule.closeTime,
                isOpen = daySchedule.isOpen,
                operatingHours = daySchedule.getOperatingHours()
            )
        }
    }

    fun toDomain(): DaySchedule {
        return DaySchedule(
            openTime = openTime,
            closeTime = closeTime,
            isOpen = isOpen
        )
    }
}

/**
 * DTO for fuel price
 */
data class FuelPriceDto(
    val amount: BigDecimal,
    val formattedPrice: String,
    val lastUpdated: LocalDateTime,
    val isRecent: Boolean
) {
    companion object {
        fun fromDomain(fuelPrice: FuelPrice): FuelPriceDto {
            return FuelPriceDto(
                amount = fuelPrice.amount,
                formattedPrice = fuelPrice.getFormattedPrice(),
                lastUpdated = fuelPrice.lastUpdated,
                isRecent = fuelPrice.isRecent()
            )
        }
    }
}

/**
 * DTO for station capacity
 */
data class StationCapacityDto(
    val fuelPumps: Int,
    val electricChargingStations: Int,
    val parkingSpaces: Int,
    val maxVehiclesPerHour: Int,
    val totalServicePoints: Int,
    val hasElectricCharging: Boolean
) {
    companion object {
        fun fromDomain(capacity: StationCapacity): StationCapacityDto {
            return StationCapacityDto(
                fuelPumps = capacity.fuelPumps,
                electricChargingStations = capacity.electricChargingStations,
                parkingSpaces = capacity.parkingSpaces,
                maxVehiclesPerHour = capacity.maxVehiclesPerHour,
                totalServicePoints = capacity.getTotalServicePoints(),
                hasElectricCharging = capacity.hasElectricCharging()
            )
        }
    }

    fun toDomain(): StationCapacity {
        return StationCapacity(
            fuelPumps = fuelPumps,
            electricChargingStations = electricChargingStations,
            parkingSpaces = parkingSpaces,
            maxVehiclesPerHour = maxVehiclesPerHour
        )
    }
}