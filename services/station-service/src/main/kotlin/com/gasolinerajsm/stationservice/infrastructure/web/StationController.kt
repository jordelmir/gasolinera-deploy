package com.gasolinerajsm.stationservice.infrastructure.web

import com.gasolinerajsm.stationservice.application.port.`in`.*
import com.gasolinerajsm.stationservice.application.usecase.*
import com.gasolinerajsm.stationservice.domain.model.FuelType
import com.gasolinerajsm.stationservice.domain.model.StationAmenity
import com.gasolinerajsm.stationservice.domain.valueobject.StationId
import com.gasolinerajsm.stationservice.infrastructure.web.dto.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for Station operations
 */
@RestController
@RequestMapping("/api/v1/stations")
@CrossOrigin(origins = ["*"])
class StationController(
    private val createStationUseCase: CreateStationUseCase,
    private val updateFuelPriceUseCase: UpdateFuelPriceUseCase,
    private val findNearbyStationsUseCase: FindNearbyStationsUseCase
) {

    /**
     * Create a new station
     */
    @PostMapping
    suspend fun createStation(
        @Valid @RequestBody request: CreateStationRequest
    ): ResponseEntity<StationResponse> {
        val command = CreateStationCommand(
            name = request.name,
            address = request.address,
            latitude = request.latitude,
            longitude = request.longitude,
            phoneNumber = request.phoneNumber,
            email = request.email,
            managerId = request.managerId,
            operatingHours = request.operatingHours.toDomain(),
            capacity = request.capacity.toDomain(),
            amenities = request.amenities,
            initialFuelPrices = request.initialFuelPrices
        )

        return createStationUseCase.execute(command)
            .fold(
                onSuccess = { station ->
                    ResponseEntity.status(HttpStatus.CREATED)
                        .body(StationResponse.fromDomain(station))
                },
                onFailure = { error ->
                    when (error) {
                        is IllegalArgumentException ->
                            ResponseEntity.badRequest().build()
                        is IllegalStateException ->
                            ResponseEntity.status(HttpStatus.CONFLICT).build()
                        else ->
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            )
    }

    /**
     * Update fuel price for a station
     */
    @PutMapping("/{stationId}/fuel-prices")
    suspend fun updateFuelPrice(
        @PathVariable stationId: String,
        @Valid @RequestBody request: UpdateFuelPriceRequest
    ): ResponseEntity<StationResponse> {
        val command = UpdateFuelPriceCommand(
            stationId = StationId.from(stationId),
            fuelType = request.fuelType,
            newPrice = request.price,
            updatedBy = null // TODO: Get from security context
        )

        return updateFuelPriceUseCase.execute(command)
            .fold(
                onSuccess = { station ->
                    ResponseEntity.ok(StationResponse.fromDomain(station))
                },
                onFailure = { error ->
                    when (error) {
                        is NoSuchElementException ->
                            ResponseEntity.notFound().build()
                        is IllegalArgumentException ->
                            ResponseEntity.badRequest().build()
                        else ->
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            )
    }

    /**
     * Find stations near a location
     */
    @GetMapping("/nearby")
    suspend fun findNearbyStations(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "10.0") radiusKm: Double,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
        @RequestParam(required = false) amenities: Set<StationAmenity>?,
        @RequestParam(required = false) fuelTypes: Set<FuelType>?
    ): ResponseEntity<List<StationResponse>> {
        val query = FindStationsNearLocationQuery(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            limit = limit,
            includeInactive = includeInactive,
            requiredAmenities = amenities ?: emptySet(),
            fuelTypes = fuelTypes ?: emptySet()
        )

        return findNearbyStationsUseCase.execute(query)
            .fold(
                onSuccess = { stations ->
                    val response = stations.map { StationResponse.fromDomain(it) }
                    ResponseEntity.ok(response)
                },
                onFailure = { error ->
                    when (error) {
                        is IllegalArgumentException ->
                            ResponseEntity.badRequest().build()
                        else ->
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            )
    }

    /**
     * Get station by ID
     */
    @GetMapping("/{stationId}")
    suspend fun getStation(
        @PathVariable stationId: String
    ): ResponseEntity<StationResponse> {
        // TODO: Implement GetStationUseCase
        return ResponseEntity.notFound().build()
    }

    /**
     * Search stations by name or address
     */
    @GetMapping("/search")
    suspend fun searchStations(
        @RequestParam query: String,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "false") includeInactive: Boolean
    ): ResponseEntity<List<StationResponse>> {
        // TODO: Implement SearchStationsUseCase
        return ResponseEntity.ok(emptyList())
    }

    /**
     * Get stations by manager
     */
    @GetMapping("/manager/{managerId}")
    suspend fun getStationsByManager(
        @PathVariable managerId: String,
        @RequestParam(defaultValue = "false") includeInactive: Boolean
    ): ResponseEntity<List<StationResponse>> {
        // TODO: Implement GetStationsByManagerUseCase
        return ResponseEntity.ok(emptyList())
    }
}