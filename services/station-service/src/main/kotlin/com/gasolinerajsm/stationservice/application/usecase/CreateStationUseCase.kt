package com.gasolinerajsm.stationservice.application.usecase

import com.gasolinerajsm.stationservice.application.port.`in`.CreateStationCommand
import com.gasolinerajsm.stationservice.application.port.out.EventPublisher
import com.gasolinerajsm.stationservice.application.port.out.GeocodingService
import com.gasolinerajsm.stationservice.domain.model.Station
import com.gasolinerajsm.stationservice.domain.repository.StationRepository
import com.gasolinerajsm.stationservice.domain.service.StationDomainService
import com.gasolinerajsm.stationservice.domain.valueobject.FuelPrice
import java.time.LocalDateTime

/**
 * Use case for creating a new station
 */
class CreateStationUseCase(
    private val stationRepository: StationRepository,
    private val stationDomainService: StationDomainService,
    private val geocodingService: GeocodingService,
    private val eventPublisher: EventPublisher
) {

    suspend fun execute(command: CreateStationCommand): Result<Station> {
        return try {
            // Validate command data
            val validationResult = stationDomainService.validateStationCreation(
                name = command.name,
                address = command.address,
                location = command.getLocation(),
                operatingHours = command.operatingHours,
                capacity = command.capacity
            )

            if (!validationResult.isSuccess) {
                return Result.failure(IllegalArgumentException(validationResult.message))
            }

            // Validate address with geocoding service
            val addressValidation = geocodingService.validateAddress(command.address)
            if (addressValidation.isFailure) {
                return Result.failure(
                    IllegalArgumentException("Invalid address: ${command.address}")
                )
            }

            // Check if station already exists at this location
            val existingStation = stationRepository.existsByNameAndLocation(
                name = command.name,
                location = command.getLocation(),
                radiusKm = 0.1
            )

            if (existingStation.isSuccess && existingStation.getOrNull() == true) {
                return Result.failure(
                    IllegalStateException("Station already exists at this location")
                )
            }

            // Create station entity
            val station = Station.create(
                name = command.name,
                address = command.address,
                location = command.getLocation(),
                phoneNumber = command.phoneNumber,
                email = command.email,
                managerId = command.managerId,
                operatingHours = command.operatingHours,
                capacity = command.capacity
            )

            // Add amenities if provided
            val stationWithAmenities = command.amenities.fold(station) { acc, amenity ->
                acc.addAmenity(amenity)
            }

            // Add initial fuel prices if provided
            val stationWithPrices = command.initialFuelPrices.entries.fold(stationWithAmenities) { acc, (fuelType, price) ->
                acc.updateFuelPrice(fuelType, price)
            }

            // Save station
            val savedStationResult = stationRepository.save(stationWithPrices)
            if (savedStationResult.isFailure) {
                return Result.failure(
                    savedStationResult.exceptionOrNull()
                        ?: Exception("Failed to save station")
                )
            }

            val savedStation = savedStationResult.getOrThrow()

            // Publish domain events
            val events = savedStation.getUncommittedEvents()
            if (events.isNotEmpty()) {
                eventPublisher.publishAll(events)
                savedStation.markEventsAsCommitted()
            }

            Result.success(savedStation)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}