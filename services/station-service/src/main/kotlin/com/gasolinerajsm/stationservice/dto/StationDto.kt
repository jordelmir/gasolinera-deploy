package com.gasolinerajsm.stationservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Request DTO for creating a new station
 */
data class CreateStationRequest(
    @field:NotBlank(message = "Station name is required")
    @field:Size(min = 2, max = 200, message = "Station name must be between 2 and 200 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotBlank(message = "Address is required")
    @field:Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    @JsonProperty("address")
    val address: String,

    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @JsonProperty("latitude")
    val latitude: BigDecimal,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @JsonProperty("longitude")
    val longitude: BigDecimal,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    @JsonProperty("phone_number")
    val phoneNumber: String,

    @field:Email(message = "Email must be valid")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    @JsonProperty("email")
    val email: String? = null,

    @field:Size(max = 200, message = "Manager name must not exceed 200 characters")
    @JsonProperty("manager_name")
    val managerName: String? = null,

    @JsonProperty("operating_hours")
    val operatingHours: String? = null,

    @JsonProperty("services_offered")
    val servicesOffered: String? = null,

    @JsonProperty("fuel_types")
    val fuelTypes: String? = null,

    @JsonProperty("is_24_hours")
    val is24Hours: Boolean = false,

    @JsonProperty("has_convenience_store")
    val hasConvenienceStore: Boolean = false,

    @JsonProperty("has_car_wash")
    val hasCarWash: Boolean = false
) {
    fun toStation(): Station {
        return Station(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            phoneNumber = phoneNumber,
            email = email,
            managerName = managerName,
            operatingHours = operatingHours,
            servicesOffered = servicesOffered,
            fuelTypes = fuelTypes,
            is24Hours = is24Hours,
            hasConvenienceStore = hasConvenienceStore,
            hasCarWash = hasCarWash
        )
    }
}

/**
 * Request DTO for updating a station
 */
data class UpdateStationRequest(
    @field:NotBlank(message = "Station name is required")
    @field:Size(min = 2, max = 200, message = "Station name must be between 2 and 200 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotBlank(message = "Address is required")
    @field:Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    @JsonProperty("address")
    val address: String,

    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @JsonProperty("latitude")
    val latitude: BigDecimal,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @JsonProperty("longitude")
    val longitude: BigDecimal,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    @JsonProperty("phone_number")
    val phoneNumber: String,

    @field:Email(message = "Email must be valid")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    @JsonProperty("email")
    val email: String? = null,

    @field:Size(max = 200, message = "Manager name must not exceed 200 characters")
    @JsonProperty("manager_name")
    val managerName: String? = null,

    @JsonProperty("operating_hours")
    val operatingHours: String? = null,

    @JsonProperty("services_offered")
    val servicesOffered: String? = null,

    @JsonProperty("fuel_types")
    val fuelTypes: String? = null,

    @JsonProperty("is_24_hours")
    val is24Hours: Boolean,

    @JsonProperty("has_convenience_store")
    val hasConvenienceStore: Boolean,

    @JsonProperty("has_car_wash")
    val hasCarWash: Boolean
)

/**
 * Response DTO for station information
 */
data class StationResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("address")
    val address: String,

    @JsonProperty("formatted_address")
    val formattedAddress: String,

    @JsonProperty("latitude")
    val latitude: BigDecimal,

    @JsonProperty("longitude")
    val longitude: BigDecimal,

    @JsonProperty("coordinates")
    val coordinates: Pair<BigDecimal, BigDecimal>,

    @JsonProperty("phone_number")
    val phoneNumber: String,

    @JsonProperty("email")
    val email: String?,

    @JsonProperty("manager_name")
    val managerName: String?,

    @JsonProperty("status")
    val status: StationStatus,

    @JsonProperty("operating_hours")
    val operatingHours: String?,

    @JsonProperty("services_offered")
    val servicesOffered: String?,

    @JsonProperty("fuel_types")
    val fuelTypes: String?,

    @JsonProperty("fuel_types_list")
    val fuelTypesList: List<String>,

    @JsonProperty("is_24_hours")
    val is24Hours: Boolean,

    @JsonProperty("has_convenience_store")
    val hasConvenienceStore: Boolean,

    @JsonProperty("has_car_wash")
    val hasCarWash: Boolean,

    @JsonProperty("active_employee_count")
    val activeEmployeeCount: Int,

    @JsonProperty("is_operational")
    val isOperational: Boolean,

    @JsonProperty("is_under_maintenance")
    val isUnderMaintenance: Boolean,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromStation(station: Station): StationResponse {
            return StationResponse(
                id = station.id,
                name = station.name,
                address = station.address,
                formattedAddress = station.getFormattedAddress(),
                latitude = station.latitude,
                longitude = station.longitude,
                coordinates = station.getCoordinates(),
                phoneNumber = station.phoneNumber,
                email = station.email,
                managerName = station.managerName,
                status = station.status,
                operatingHours = station.operatingHours,
                servicesOffered = station.servicesOffered,
                fuelTypes = station.fuelTypes,
                fuelTypesList = station.getFuelTypesList(),
                is24Hours = station.is24Hours,
                hasConvenienceStore = station.hasConvenienceStore,
                hasCarWash = station.hasCarWash,
                activeEmployeeCount = station.getActiveEmployeeCount(),
                isOperational = station.isOperational(),
                isUnderMaintenance = station.isUnderMaintenance(),
                createdAt = station.createdAt,
                updatedAt = station.updatedAt
            )
        }
    }
}

/**
 * Request DTO for updating station status
 */
data class UpdateStationStatusRequest(
    @JsonProperty("status")
    val status: StationStatus
)

/**
 * Request DTO for updating station contact information
 */
data class UpdateStationContactRequest(
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    @JsonProperty("phone_number")
    val phoneNumber: String? = null,

    @field:Email(message = "Email must be valid")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    @JsonProperty("email")
    val email: String? = null
)

/**
 * Request DTO for updating station manager
 */
data class UpdateStationManagerRequest(
    @field:Size(max = 200, message = "Manager name must not exceed 200 characters")
    @JsonProperty("manager_name")
    val managerName: String?
)

/**
 * Request DTO for station search
 */
data class StationSearchRequest(
    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("address")
    val address: String? = null,

    @JsonProperty("status")
    val status: StationStatus? = null,

    @JsonProperty("is_24_hours")
    val is24Hours: Boolean? = null,

    @JsonProperty("has_convenience_store")
    val hasConvenienceStore: Boolean? = null,

    @JsonProperty("has_car_wash")
    val hasCarWash: Boolean? = null,

    @JsonProperty("service")
    val service: String? = null,

    @JsonProperty("fuel_type")
    val fuelType: String? = null,

    @JsonProperty("manager_name")
    val managerName: String? = null
)

/**
 * Request DTO for location-based station search
 */
data class LocationSearchRequest(
    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @JsonProperty("latitude")
    val latitude: BigDecimal,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @JsonProperty("longitude")
    val longitude: BigDecimal,

    @field:DecimalMin(value = "0.1", message = "Radius must be at least 0.1 km")
    @field:DecimalMax(value = "500.0", message = "Radius cannot exceed 500 km")
    @JsonProperty("radius_km")
    val radiusKm: Double = 10.0,

    @JsonProperty("status")
    val status: StationStatus = StationStatus.ACTIVE
)

/**
 * Response DTO for location-based search with distance
 */
data class StationWithDistanceResponse(
    @JsonProperty("station")
    val station: StationResponse,

    @JsonProperty("distance_km")
    val distanceKm: Double
) {
    companion object {
        fun fromStationAndDistance(station: Station, distanceKm: Double): StationWithDistanceResponse {
            return StationWithDistanceResponse(
                station = StationResponse.fromStation(station),
                distanceKm = distanceKm
            )
        }
    }
}

/**
 * Response DTO for station statistics
 */
data class StationStatisticsResponse(
    @JsonProperty("total_stations")
    val totalStations: Long,

    @JsonProperty("active_stations")
    val activeStations: Long,

    @JsonProperty("inactive_stations")
    val inactiveStations: Long,

    @JsonProperty("maintenance_stations")
    val maintenanceStations: Long,

    @JsonProperty("stations_24_hours")
    val stations24Hours: Long,

    @JsonProperty("stations_with_store")
    val stationsWithStore: Long,

    @JsonProperty("stations_with_car_wash")
    val stationsWithCarWash: Long
)