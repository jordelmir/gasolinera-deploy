package com.gasolinerajsm.stationservice.service

import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.repository.StationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing gas stations
 */
@Service
@Transactional
class StationService(
    private val stationRepository: StationRepository
) {

    private val logger = LoggerFactory.getLogger(StationService::class.java)

    /**
     * Create a new station
     */
    fun createStation(station: Station): Station {
        // Validate station code uniqueness
        if (stationRepository.existsByCode(station.code)) {
            throw IllegalArgumentException("Station code '${station.code}' already exists")
        }

        val savedStation = stationRepository.save(station)
        logger.info("Created new station: {} with code: {}", savedStation.name, savedStation.code)
        return savedStation
    }

    /**
     * Get station by ID
     */
    @Transactional(readOnly = true)
    fun getStationById(stationId: Long): Station {
        return stationRepository.findById(stationId).orElseThrow {
            StationNotFoundException("Station not found with ID: $stationId")
        }
    }

    /**
     * Get station by code
     */
    @Transactional(readOnly = true)
    fun getStationByCode(code: String): Station? {
        return stationRepository.findByCode(code)
    }

    /**
     * Get all stations
     */
    @Transactional(readOnly = true)
    fun getAllStations(): List<Station> {
        return stationRepository.findAll()
    }

    /**
     * Get stations by status
     */
    @Transactional(readOnly = true)
    fun getStationsByStatus(status: StationStatus): List<Station> {
        return stationRepository.findByStatus(status)
    }

    /**
     * Get active stations
     */
    @Transactional(readOnly = true)
    fun getActiveStations(): List<Station> {
        return stationRepository.findByStatus(StationStatus.ACTIVE)
    }

    /**
     * Get stations by city
     */
    @Transactional(readOnly = true)
    fun getStationsByCity(city: String): List<Station> {
        return stationRepository.findByCity(city)
    }

    /**
     * Update station
     */
    fun updateStation(stationId: Long, updatedStation: Station): Station {
        val existingStation = getStationById(stationId)

        // Check if code is being changed and if new code already exists
        if (existingStation.code != updatedStation.code &&
            stationRepository.existsByCode(updatedStation.code)) {
            throw IllegalArgumentException("Station code '${updatedStation.code}' already exists")
        }

        val stationToSave = updatedStation.copy(id = stationId)
        val savedStation = stationRepository.save(stationToSave)
        logger.info("Updated station: {} (ID: {})", savedStation.name, savedStation.id)
        return savedStation
    }

    /**
     * Delete station
     */
    fun deleteStation(stationId: Long): Boolean {
        return try {
            stationRepository.deleteById(stationId)
            logger.info("Deleted station: {}", stationId)
            true
        } catch (e: Exception) {
            logger.error("Error deleting station {}: {}", stationId, e.message, e)
            false
        }
    }
}

/**
 * Custom exceptions for station service
 */
class StationServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class StationNotFoundException(message: String) : RuntimeException(message)