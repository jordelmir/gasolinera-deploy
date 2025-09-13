package com.gasolinerajsm.stationservice.application.port.`in`

import com.gasolinerajsm.stationservice.domain.model.FuelType
import com.gasolinerajsm.stationservice.domain.model.StationAmenity
import com.gasolinerajsm.stationservice.domain.model.StationStatus
import com.gasolinerajsm.stationservice.domain.valueobject.Location
import com.gasolinerajsm.stationservice.domain.valueobject.OperatingHours
import com.gasolinerajsm.stationservice.domain.valueobject.StationCapacity
import com.gasolinerajsm.stationservice.domain.valueobject.StationId
import java.math.BigDecimal

/**
 * Command to create a new station
 */
data class CreateStationCommand(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phoneNumber: String?,
    val email: String?,
    val managerId: String?,
    val operatingHours: OperatingHours,
    val capacity: StationCapacity,
    val amenities: Set<StationAmenity> = emptySet(),
    val initialFuelPrices: Map<FuelType, BigDecimal> = emptyMap()
) {
    fun getLocation(): Location = Location.of(latitude, longitude)
}

/**
 * Command to update station information
 */
data class UpdateStationCommand(
    val stationId: StationId,
    val name: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val phoneNumber: String?,
    val email: String?,
    val managerId: String?,
    val updatedBy: String?
) {
    fun getLocation(): Location? {
        return if (latitude != null && longitude != null) {
            Location.of(latitude, longitude)
        } else null
    }
}

/**
 * Command to update station status
 */
data class UpdateStationStatusCommand(
    val stationId: StationId,
    val newStatus: StationStatus,
    val reason: String?,
    val changedBy: String?
)

/**
 * Command to update fuel price
 */
data class UpdateFuelPriceCommand(
    val stationId: StationId,
    val fuelType: FuelType,
    val newPrice: BigDecimal,
    val updatedBy: String?
)

/**
 * Command to update multiple fuel prices
 */
data class UpdateMultipleFuelPricesCommand(
    val stationId: StationId,
    val fuelPrices: Map<FuelType, BigDecimal>,
    val updatedBy: String?
)

/**
 * Command to update station operating hours
 */
data class UpdateOperatingHoursCommand(
    val stationId: StationId,
    val operatingHours: OperatingHours,
    val effectiveDate: java.time.LocalDateTime?,
    val updatedBy: String?
)

/**
 * Command to update station amenities
 */
data class UpdateStationAmenitiesCommand(
    val stationId: StationId,
    val amenitiesToAdd: Set<StationAmenity> = emptySet(),
    val amenitiesToRemove: Set<StationAmenity> = emptySet(),
    val updatedBy: String?
)

/**
 * Command to update station capacity
 */
data class UpdateStationCapacityCommand(
    val stationId: StationId,
    val capacity: StationCapacity,
    val updatedBy: String?
)

/**
 * Command to assign manager to station
 */
data class AssignStationManagerCommand(
    val stationId: StationId,
    val managerId: String,
    val assignedBy: String?
)

/**
 * Command to deactivate station
 */
data class DeactivateStationCommand(
    val stationId: StationId,
    val reason: String?,
    val deactivatedBy: String?
)

/**
 * Command to reactivate station
 */
data class ReactivateStationCommand(
    val stationId: StationId,
    val reactivatedBy: String?
)

/**
 * Query to find stations near location
 */
data class FindStationsNearLocationQuery(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 10.0,
    val limit: Int = 20,
    val includeInactive: Boolean = false,
    val requiredAmenities: Set<StationAmenity> = emptySet(),
    val fuelTypes: Set<FuelType> = emptySet()
) {
    fun getLocation(): Location = Location.of(latitude, longitude)
}

/**
 * Query to search stations
 */
data class SearchStationsQuery(
    val query: String,
    val limit: Int = 50,
    val includeInactive: Boolean = false
)

/**
 * Query to get stations by manager
 */
data class GetStationsByManagerQuery(
    val managerId: String,
    val includeInactive: Boolean = false
)

/**
 * Query to get stations by status
 */
data class GetStationsByStatusQuery(
    val status: StationStatus,
    val limit: Int = 100
)

/**
 * Query to get station details
 */
data class GetStationDetailsQuery(
    val stationId: StationId
)