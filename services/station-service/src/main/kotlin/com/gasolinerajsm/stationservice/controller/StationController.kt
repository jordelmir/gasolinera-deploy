package com.gasolinerajsm.stationservice.controller

import com.gasolinerajsm.stationservice.dto.*
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.service.StationService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * REST Controller for Station management operations
 */
@RestController
@RequestMapping("/api/v1/stations")
@CrossOrigin(origins = ["*"])
class StationController(
    private val stationService: StationService
) {

    /**
     * Create a new station
     */
    @PostMapping
    fun createStation(@Valid @RequestBody request: CreateStationRequest): ResponseEntity<StationResponse> {
        val station = stationService.createStation(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(station)
    }

    /**
     * Get station by ID
     */
    @GetMapping("/{id}")
    fun getStationById(@PathVariable id: Long): ResponseEntity<StationResponse> {
        val station = stationService.getStationById(id)
        return ResponseEntity.ok(station)
    }

    /**
     * Get all stations with pagination
     */
    @GetMapping
    fun getAllStations(pageable: Pageable): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.getAllStations(pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Update station information
     */
    @PutMapping("/{id}")
    fun updateStation(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStationRequest
    ): ResponseEntity<StationResponse> {
        val updatedStation = stationService.updateStation(id, request)
        return ResponseEntity.ok(updatedStation)
    }

    /**
     * Delete station (soft delete - set to inactive)
     */
    @DeleteMapping("/{id}")
    fun deleteStation(@PathVariable id: Long): ResponseEntity<Void> {
        stationService.deleteStation(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Update station status
     */
    @PatchMapping("/{id}/status")
    fun updateStationStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStationStatusRequest
    ): ResponseEntity<StationResponse> {
        val updatedStation = stationService.updateStationStatus(id, request.status)
        return ResponseEntity.ok(updatedStation)
    }

    /**
     * Activate station
     */
    @PostMapping("/{id}/activate")
    fun activateStation(@PathVariable id: Long): ResponseEntity<StationResponse> {
        val activatedStation = stationService.activateStation(id)
        return ResponseEntity.ok(activatedStation)
    }

    /**
     * Deactivate station
     */
    @PostMapping("/{id}/deactivate")
    fun deactivateStation(@PathVariable id: Long): ResponseEntity<StationResponse> {
        val deactivatedStation = stationService.deactivateStation(id)
        return ResponseEntity.ok(deactivatedStation)
    }

    /**
     * Put station under maintenance
     */
    @PostMapping("/{id}/maintenance")
    fun putStationUnderMaintenance(@PathVariable id: Long): ResponseEntity<StationResponse> {
        val maintenanceStation = stationService.putStationUnderMaintenance(id)
        return ResponseEntity.ok(maintenanceStation)
    }

    /**
     * Update station contact information
     */
    @PatchMapping("/{id}/contact")
    fun updateStationContact(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStationContactRequest
    ): ResponseEntity<StationResponse> {
        val updatedStation = stationService.updateStationContact(id, request)
        return ResponseEntity.ok(updatedStation)
    }

    /**
     * Update station manager
     */
    @PatchMapping("/{id}/manager")
    fun updateStationManager(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStationManagerRequest
    ): ResponseEntity<StationResponse> {
        val updatedStation = stationService.updateStationManager(id, request.managerName)
        return ResponseEntity.ok(updatedStation)
    }

    /**
     * Search stations by criteria
     */
    @PostMapping("/search")
    fun searchStations(
        @Valid @RequestBody request: StationSearchRequest,
        pageable: Pageable
    ): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.searchStations(request, pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Search stations by location
     */
    @PostMapping("/search/location")
    fun searchStationsByLocation(
        @Valid @RequestBody request: LocationSearchRequest
    ): ResponseEntity<List<StationWithDistanceResponse>> {
        val stations = stationService.searchStationsByLocation(request)
        return ResponseEntity.ok(stations)
    }

    /**
     * Find stations by name (partial match)
     */
    @GetMapping("/search/name")
    fun findStationsByName(
        @RequestParam name: String,
        pageable: Pageable
    ): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.findStationsByName(name, pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Find stations by status
     */
    @GetMapping("/status/{status}")
    fun findStationsByStatus(
        @PathVariable status: StationStatus,
        pageable: Pageable
    ): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.findStationsByStatus(status, pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Find 24-hour stations
     */
    @GetMapping("/24-hours")
    fun find24HourStations(pageable: Pageable): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.find24HourStations(pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Find stations with convenience store
     */
    @GetMapping("/convenience-store")
    fun findStationsWithConvenienceStore(pageable: Pageable): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.findStationsWithConvenienceStore(pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Find stations with car wash
     */
    @GetMapping("/car-wash")
    fun findStationsWithCarWash(pageable: Pageable): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.findStationsWithCarWash(pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Find stations by fuel type
     */
    @GetMapping("/fuel-type/{fuelType}")
    fun findStationsByFuelType(
        @PathVariable fuelType: String,
        pageable: Pageable
    ): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.findStationsByFuelType(fuelType, pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Find stations by service
     */
    @GetMapping("/service/{service}")
    fun findStationsByService(
        @PathVariable service: String,
        pageable: Pageable
    ): ResponseEntity<Page<StationResponse>> {
        val stations = stationService.findStationsByService(service, pageable)
        return ResponseEntity.ok(stations)
    }

    /**
     * Calculate distance between two stations
     */
    @GetMapping("/{id1}/distance/{id2}")
    fun calculateDistanceBetweenStations(
        @PathVariable id1: Long,
        @PathVariable id2: Long
    ): ResponseEntity<Map<String, Any>> {
        val distance = stationService.calculateDistanceBetweenStations(id1, id2)
        val response = mapOf(
            "station1_id" to id1,
            "station2_id" to id2,
            "distance_km" to distance
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Calculate distance from coordinates to station
     */
    @GetMapping("/{id}/distance")
    fun calculateDistanceFromCoordinates(
        @PathVariable id: Long,
        @RequestParam latitude: BigDecimal,
        @RequestParam longitude: BigDecimal
    ): ResponseEntity<Map<String, Any>> {
        val distance = stationService.calculateDistanceFromCoordinates(id, latitude, longitude)
        val response = mapOf(
            "station_id" to id,
            "latitude" to latitude,
            "longitude" to longitude,
            "distance_km" to distance
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Get station statistics
     */
    @GetMapping("/statistics")
    fun getStationStatistics(): ResponseEntity<StationStatisticsResponse> {
        val statistics = stationService.getStationStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * Get stations needing attention (inactive/maintenance for too long)
     */
    @GetMapping("/attention-needed")
    fun getStationsNeedingAttention(): ResponseEntity<List<StationResponse>> {
        val stations = stationService.getStationsNeedingAttention()
        return ResponseEntity.ok(stations)
    }

    /**
     * Get stations without manager
     */
    @GetMapping("/no-manager")
    fun getStationsWithoutManager(): ResponseEntity<List<StationResponse>> {
        val stations = stationService.getStationsWithoutManager()
        return ResponseEntity.ok(stations)
    }

    /**
     * Get duplicate stations
     */
    @GetMapping("/duplicates")
    fun getDuplicateStations(): ResponseEntity<List<StationResponse>> {
        val stations = stationService.getDuplicateStations()
        return ResponseEntity.ok(stations)
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        val response = mapOf(
            "status" to "UP",
            "service" to "Station Service",
            "timestamp" to java.time.Instant.now().toString()
        )
        return ResponseEntity.ok(response)
    }
}
