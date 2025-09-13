package com.gasolinerajsm.stationservice.application.usecase

import com.gasolinerajsm.stationservice.application.port.`in`.UpdateFuelPriceCommand
import com.gasolinerajsm.stationservice.application.port.out.EventPublisher
import com.gasolinerajsm.stationservice.application.port.out.NotificationService
import com.gasolinerajsm.stationservice.domain.model.Station
import com.gasolinerajsm.stationservice.domain.repository.StationRepository
import com.gasolinerajsm.stationservice.domain.service.StationDomainService

/**
 * Use case for updating fuel price at a station
 */
class UpdateFuelPriceUseCase(
    private val stationRepository: StationRepository,
    private val stationDomainService: StationDomainService,
    private val eventPublisher: EventPublisher,
    private val notificationService: NotificationService
) {

    suspend fun execute(command: UpdateFuelPriceCommand): Result<Station> {
        return try {
            // Find station
            val stationResult = stationRepository.findById(command.stationId)
            if (stationResult.isFailure) {
                return Result.failure(
                    stationResult.exceptionOrNull()
                        ?: Exception("Failed to find station")
                )
            }

            val station = stationResult.getOrNull()
                ?: return Result.failure(
                    NoSuchElementException("Station not found: ${command.stationId}")
                )

            // Validate price update
            val currentPrice = station.getFuelPrice(command.fuelType)?.amount
            val validationResult = stationDomainService.validateFuelPriceUpdate(
                currentPrice = currentPrice,
                newPrice = command.newPrice,
                fuelType = command.fuelType
            )

            if (!validationResult.isSuccess) {
                return Result.failure(IllegalArgumentException(validationResult.message))
            }

            // Update fuel price
            val updatedStation = station.updateFuelPrice(command.fuelType, command.newPrice)

            // Save updated station
            val savedStationResult = stationRepository.save(updatedStation)
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

            // Send notification if significant price change
            if (validationResult.isWarning) {
                notificationService.notifyFuelPriceChange(
                    stationId = command.stationId,
                    stationName = station.name,
                    fuelType = command.fuelType,
                    oldPrice = currentPrice,
                    newPrice = command.newPrice,
                    managerId = station.managerId
                )
            }

            Result.success(savedStation)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}