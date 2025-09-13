package com.gasolinerajsm.stationservice.service

import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.model.StationType
import com.gasolinerajsm.stationservice.repository.StationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

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
    fun createStation(station: Station, createdBy: String? = null): Station {
        return try {
            // Validate station code uniqueness
            if (stationRepository.existsByCode(station.code)) {
                throw IllegalArgumentException("Station code '${station.code}' already exists")
            }

            // Set creation metadata
            val stationToSave = station.copy(
                createdBy = createdBy,
                updatedBy = createdBy
            )

            val savedStation = stationRepository.save(stationToSave)
            logger.info("Created new station: {} with code: {}", savedStation.name, savedStation.code)
            savedStation
        } catch (e: Exception) {
            logger.error("Error creating station: {}", e.message, e)
            throw StationServiceException("Failed to create station: ${e.message}", e)
        }
    }

    /**
     * Update an existing station
     */
    fun updateStation(stationId: Long, updatedStation: Station, updatedBy: String? = null): Station {
        return try {
            val existingStation = getStationById(stationId)

            // Check if code is being changed and if new code already exists
            if (existingStation.code != updatedStation.code &&
                stationRepository.existsByCode(updatedStation.code)) {
                throw IllegalArgumentException("Station code '${updatedStation.code}' already exists")
            }

            val stationToSave = updatedStation.copy(
                id = stationId,
                createdAt = existingStation.createdAt,
                createdBy = existingStation.createdBy,
                updatedBy = updatedBy,
                updatedAt = LocalDateTime.now()
            )

            val savedStation = stationRepository.save(stationToSave)
            logger.info("Updated station: {} (ID: {})", savedStation.name, savedStation.id)
            savedStation
        } catch (e: Exception) {
            logger.error("Error updating station {}: {}", stationId, e.message, e)
            throw StationServiceException("Failed to update station: ${e.message}", e)
        }
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
     * Get all stations with pagination
     */
    @Transactional(readOnly = true)
    fun getAllStations(pageable: Pageable): Page<Station> {
        return stationRepository.findAll(pageable)
    }

    /**
     * Get stations by status
     */
    @Transactional(readOnly = true)
    fun getStationsByStatus(status: StationStatus, pageable: Pageable? = null): List<Station> {
        return if (pageable != null) {
            stationRepository.findByStatus(status, pageable).content
        } else {
            stationRepository.findByStatus(status)
        }
    }

    /**
     * Get active stations
     */
    @Transactional(readOnly = true)
    fun getActiveStations(): List<Station> {
        return stationRepository.findByStatus(StationStatus.ACTIVE)
    }

    /**
     * Get stations by type
     */
    @Transactional(readOnly = true)
    fun getStationsByType(stationType: StationType): List<Station> {
        return stationRepository.findByStationType(stationType)
    }

    /**
     * Get stations by city
     */
    @Transactional(readOnly = true)
    fun getStationsByCity(city: String): List<Station> {
        return stationRepository.findByCity(city)
    }

    /**
     * Get stations by state
     */
    @Transactional(readOnly = true)
    fun getStationsByState(state: String): List<Station> {
        return stationRepository.findByState(state)
    }

    /**
     * Search stations by name
     */
    @Transactional(readOnly = true)
    fun searchStationsByName(searchTerm: String, pageable: Pageable? = null): List<Station> {
        return if (pageable != null) {
            stationRepository.searchByName(searchTerm, pageable).content
        } else {
            stationRepository.searchByName(searchTerm)
        }
    }

    /**
     * Search stations by multiple criteria
     */
    @Transactional(readOnly = true)
    fun searchStations(
        name: String? = null,
        city: String? = null,
        state: String? = null,
        status: StationStatus? = null,
        stationType: StationType? = null,
        pageable: Pageable
    ): Page<Station> {
        return stationRepository.searchStations(name, city, state, status, stationType, pageable)
    }

    /**
     * Find stations within radius
     */
    @Transactional(readOnly = true)
    fun findStationsWithinRadius(
        latitude: BigDecimal,
        longitude: BigDecimal,
        radiusKm: Double,
        status: StationStatus = StationStatus.ACTIVE
    ): List<Station> {
        return try {
            stationRepository.findStationsWithinRadius(latitude, longitude, radiusKm, status)
        } catch (e: Exception) {
            logger.error("Error finding stations within radius: {}", e.message, e)
            emptyList()
        }
    }

    /**
     * Find nearest stations
     */
    @Transactional(readOnly = true)
    fun findNearestStations(
        latitude: BigDecimal,
        longitude: BigDecimal,
        pageable: Pageable,
        status: StationStatus = StationStatus.ACTIVE
    ): Page<Station> {
        return try {
            stationRepository.findNearestStations(latitude, longitude, status, pageable)
        } catch (e: Exception) {
            logger.error("Error finding nearest stations: {}", e.message, e)
            Page.empty(pageable)
        }
    }

    /**
     * Find stations with specific service
     */
    @Transactional(readOnly = true)
    fun findStationsWithService(
        service: String,
        status: StationStatus = StationStatus.ACTIVE
    ): List<Station> {
        return stationRepository.findStationsWithService(service, status)
    }

    /**
     * Find 24-hour stations
     */
    @Transactional(readOnly = true)
    fun find24HourStations(status: StationStatus = StationStatus.ACTIVE): List<Station> {
        return stationRepository.findByIs24HoursAndStatus(true, status)
    }

    /**
     * Find stations with convenience store
     */
    @Transactional(readOnly = true)
    fun findStationsWithConvenienceStore(status: StationStatus = StationStatus.ACTIVE): List<Station> {
        return stationRepository.findByHasConvenienceStoreAndStatus(true, status)
    }

    /**
     * Activate station
     */
    fun activateStation(stationId: Long, updatedBy: String? = null): Station {
        return updateStationStatus(stationId, StationStatus.ACTIVE, updatedBy)
    }

    /**
     * Deactivate station
     */
    fun deactivateStation(stationId: Long, updatedBy: String? = null): Station {
        return updateStationStatus(stationId, StationStatus.INACTIVE, updatedBy)
    }

    /**
     * Set station to maintenance
     */
    fun setStationMaintenance(stationId: Long, updatedBy: String? = null): Station {
        return updateStationStatus(stationId, StationStatus.MAINTENANCE, updatedBy)
    }

    /**
     * Update station status
     */
    fun updateStationStatus(stationId: Long, status: StationStatus, updatedBy: String? = null): Station {
        return try {
            val station = getStationById(stationId)
            val updatedStation = when (status) {
                StationStatus.ACTIVE -> station.activate(updatedBy)
                StationStatus.INACTIVE -> station.deactivate(updatedBy)
                StationStatus.MAINTENANCE -> station.setMaintenance(updatedBy)
                else -> station.copy(
                    status = status,
                    updatedBy = updatedBy,
                    updatedAt = LocalDateTime.now()
                )
            }

            val savedStation = stationRepository.save(updatedStation)
            logger.info("Updated station {} status to: {}", stationId, status)
            savedStation
        } catch (e: Exception) {
            logger.error("Error updating station {} status: {}", stationId, e.message, e)
            throw StationServiceException("Failed to update station status: ${e.message}", e)
        }
    }

    /**
     * Update station manager
     */
    fun updateStationManager(stationId: Long, managerName: String?, updatedBy: String? = null): Station {
        return try {
            val station = getStationById(stationId)
            val updatedStation = station.copy(
                managerName = managerName,
                updatedBy = updatedBy,
                updatedAt = LocalDateTime.now()
            )

            val savedStation = stationRepository.save(updatedStation)
            logger.info("Updated station {} manager to: {}", stationId, managerName)
            savedStation
        } catch (e: Exception) {
            logger.error("Error updating station {} manager: {}", stationId, e.message, e)
            throw StationServiceException("Failed to update station manager: ${e.message}", e)
        }
    }

    /**
     * Update station contact information
     */
    fun updateStationContact(
        stationId: Long,
        phoneNumber: String? = null,
        email: String? = null,
        updatedBy: String? = null
    ): Station {
        return try {
            val station = getStationById(stationId)
            val updatedStation = station.copy(
                phoneNumber = phoneNumber ?: station.phoneNumber,
                email = email ?: station.email,
                updatedBy = updatedBy,
                updatedAt = LocalDateTime.now()
            )

            val savedStation = stationRepository.save(updatedStation)
            logger.info("Updated station {} contact information", stationId)
            savedStation
        } catch (e: Exception) {
            logger.error("Error updating station {} contact: {}", stationId, e.message, e)
            throw StationServiceException("Failed to update station contact: ${e.message}", e)
        }
    }

    /**
     * Delete station (soft delete by deactivating)
     */
    fun deleteStation(stationId: Long, updatedBy: String? = null): Boolean {
        return try {
            deactivateStation(stationId, updatedBy)
            logger.info("Soft deleted station: {}", stationId)
            true
        } catch (e: Exception) {
            logger.error("Error deleting station {}: {}", stationId, e.message, e)
            false
        }
    }

    /**
     * Get station statistics
     */
    @Transactional(readOnly = true)
    fun getStationStatistics(): StationStatistics {
        return try {
            val stats = stationRepository.getStationStatistics()
            StationStatistics(
                totalStations = (stats["totalStations"] as? Number)?.toLong() ?: 0L,
                activeStations = (stats["activeStations"] as? Number)?.toLong() ?: 0L,
                inactiveStations = (stats["inactiveStations"] as? Number)?.toLong() ?: 0L,
                maintenanceStations = (stats["maintenanceStations"] as? Number)?.toLong() ?: 0L,
                stations24Hours = (stats["stations24Hours"] as? Number)?.toLong() ?: 0L,
                stationsWithStore = (stats["stationsWithStore"] as? Number)?.toLong() ?: 0L,
                averagePumpCount = (stats["averagePumpCount"] as? Number)?.toDouble() ?: 0.0,
                totalPumps = (stats["totalPumps"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            logger.error("Error getting station statistics: {}", e.message, e)
            StationStatistics()
        }
    }

    /**
     * Get stations needing maintenance
     */
    @Transactional(readOnly = true)
    fun getStationsNeedingMaintenance(daysSinceLastUpdate: Long = 90): List<Station> {
        val thresholdDate = LocalDateTime.now().minusDays(daysSinceLastUpdate)
        return stationRepository.findStationsNeedingMaintenance(thresholdDate)
    }

    /**
     * Get understaffed stations
     */
    @Transactional(readOnly = true)
    fun getUnderstaffedStations(minimumStaff: Long = 2): List<Station> {
        return stationRepository.findUnderstaffedStations(minimumStaff)
    }

    /**
     * Bulk update station status
     */
    fun bulkUpdateStationStatus(
        currentStatus: StationStatus,
        newStatus: StationStatus,
        updatedBy: String? = null
    ): Int {
        return try {
            val updatedRows = stationRepository.bulkUpdateStationStatus(
                currentStatus, newStatus, LocalDateTime.now()
            )
            logger.info("Bulk updated {} stations from {} to {}", updatedRows, currentStatus, newStatus)
            updatedRows
        } catch (e: Exception) {
            logger.error("Error bulk updating station status: {}", e.message, e)
            0
        }
    }

    /**
     * Validate station data
     */
    private fun validateStationData(station: Station) {
        require(station.name.isNotBlank()) { "Station name cannot be blank" }
        require(station.code.isNotBlank()) { "Station code cannot be blank" }
        require(station.address.isNotBlank()) { "Station address cannot be blank" }
        require(station.city.isNotBlank()) { "Station city cannot be blank" }
        require(station.state.isNotBlank()) { "Station state cannot be blank" }
        require(station.postalCode.isNotBlank()) { "Station postal code cannot be blank" }
        require(station.pumpCount > 0) { "Station must have at least 1 pump" }
        require(station.latitude >= BigDecimal("-90") && station.latitude <= BigDecimal("90")) {
            "Latitude must be between -90 and 90"
        }
        require(station.longitude >= BigDecimal("-180") && station.longitude <= BigDecimal("180")) {
            "Longitude must be between -180 and 180"
        }
    }
}

/**
 * Data class for station statistics
 */
data class StationStatistics(
    val totalStations: Long = 0L,
    val activeStations: Long = 0L,
    val inactiveStations: Long = 0L,
    val maintenanceStations: Long = 0L,
    val stations24Hours: Long = 0L,
    val stationsWithStore: Long = 0L,
    val averagePumpCount: Double = 0.0,
    val totalPumps: Long = 0L
)

/**
 * Custom exceptions for station service
 */
class StationServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class StationNotFoundException(message: String) : RuntimeException(message)