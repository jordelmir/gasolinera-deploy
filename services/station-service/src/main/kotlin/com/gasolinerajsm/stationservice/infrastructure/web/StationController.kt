package com.gasolinerajsm.stationservice.infrastructure.web

import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.service.StationService
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
    private val stationService: StationService
) {

    /**
     * Create a new station
     */
    @PostMapping
    fun createStation(@RequestBody station: Station): ResponseEntity<Station> {
        return try {
            val createdStation = stationService.createStation(station)
            ResponseEntity.status(HttpStatus.CREATED).body(createdStation)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get station by ID
     */
    @GetMapping("/{stationId}")
    fun getStation(@PathVariable stationId: Long): ResponseEntity<Station> {
        return try {
            val station = stationService.getStationById(stationId)
            ResponseEntity.ok(station)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get station by code
     */
    @GetMapping("/code/{code}")
    fun getStationByCode(@PathVariable code: String): ResponseEntity<Station> {
        val station = stationService.getStationByCode(code)
        return if (station != null) {
            ResponseEntity.ok(station)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all stations
     */
    @GetMapping
    fun getAllStations(): ResponseEntity<List<Station>> {
        val stations = stationService.getAllStations()
        return ResponseEntity.ok(stations)
    }

    /**
     * Get active stations
     */
    @GetMapping("/active")
    fun getActiveStations(): ResponseEntity<List<Station>> {
        val stations = stationService.getActiveStations()
        return ResponseEntity.ok(stations)
    }

    /**
     * Get stations by city
     */
    @GetMapping("/city/{city}")
    fun getStationsByCity(@PathVariable city: String): ResponseEntity<List<Station>> {
        val stations = stationService.getStationsByCity(city)
        return ResponseEntity.ok(stations)
    }

    /**
     * Update station
     */
    @PutMapping("/{stationId}")
    fun updateStation(@PathVariable stationId: Long, @RequestBody station: Station): ResponseEntity<Station> {
        return try {
            val updatedStation = stationService.updateStation(stationId, station)
            ResponseEntity.ok(updatedStation)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Delete station
     */
    @DeleteMapping("/{stationId}")
    fun deleteStation(@PathVariable stationId: Long): ResponseEntity<Void> {
        val deleted = stationService.deleteStation(stationId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}