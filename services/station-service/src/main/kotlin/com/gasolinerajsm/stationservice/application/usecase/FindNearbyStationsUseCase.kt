package com.gasolinerajsm.stationservice.application.usecase

import com.gasolinerajsm.stationservice.application.port.`in`.FindStationsNearLocationQuery
import com.gasolinerajsm.stationservice.domain.model.Station
import com.gasolinerajsm.stationservice.domain.repository.StationRepository

/**
 * Use case for finding stations near a location
 */
class FindNearbyStationsUseCase(
    private val stationRepository: StationRepository
) {

    suspend fun execute(query: FindStationsNearLocationQuery): Result<List<Station>> {
        return try {
            // Find stations within radius
            val stationsResult = stationRepository.findWithinRadius(
                location = query.getLocation(),
                radiusKm = query.radiusKm
            )

            if (stationsResult.isFailure) {
                return Result.failure(
                    stationsResult.exceptionOrNull()
                        ?: Exception("Failed to find stations")
                )
            }

            var stations = stationsResult.getOrThrow()

            // Filter by active status if required
            if (!query.includeInactive) {
                stations = stations.filter { it.isActive && it.canProcessTransactions() }
            }

            // Filter by required amenities
            if (query.requiredAmenities.isNotEmpty()) {
                stations = stations.filter { station ->
                    query.requiredAmenities.all { amenity ->
                        station.hasAmenity(amenity)
                    }
                }
            }

            // Filter by fuel types
            if (query.fuelTypes.isNotEmpty()) {
                stations = stations.filter { station ->
                    query.fuelTypes.any { fuelType ->
                        station.getFuelPrice(fuelType) != null
                    }
                }
            }

            // Sort by distance and limit results
            val sortedStations = stations
                .sortedBy { it.distanceTo(query.getLocation()) }
                .take(query.limit)

            Result.success(sortedStations)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}