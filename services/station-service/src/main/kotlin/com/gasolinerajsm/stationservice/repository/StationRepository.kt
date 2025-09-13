package com.gasolinerajsm.stationservice.repository

import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.model.StationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface StationRepository : JpaRepository<Station, Long> {

    /**
     * Find station by code
     */
    fun findByCode(code: String): Station?

    /**
     * Find station by code and status
     */
    fun findByCodeAndStatus(code: String, status: StationStatus): Station?

    /**
     * Check if station code exists
     */
    fun existsByCode(code: String): Boolean

    /**
     * Find stations by status
     */
    fun findByStatus(status: StationStatus): List<Station>

    /**
     * Find stations by status with pagination
     */
    fun findByStatus(status: StationStatus, pageable: Pageable): Page<Station>

    /**
     * Find active stations
     */
    fun findByStatusIn(statuses: List<StationStatus>): List<Station>

    /**
     * Find stations by type
     */
    fun findByStationType(stationType: StationType): List<Station>

    /**
     * Find stations by city
     */
    fun findByCity(city: String): List<Station>

    /**
     * Find stations by city and status
     */
    fun findByCityAndStatus(city: String, status: StationStatus): List<Station>

    /**
     * Find stations by state
     */
    fun findByState(state: String): List<Station>

    /**
     * Find stations by postal code
     */
    fun findByPostalCode(postalCode: String): List<Station>

    /**
     * Search stations by name (case insensitive)
     */
    @Query("SELECT s FROM Station s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    fun searchByName(@Param("searchTerm") searchTerm: String): List<Station>

    /**
     * Search stations by name with pagination
     */
    @Query("SELECT s FROM Station s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    fun searchByName(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Station>

    /**
     * Search stations by multiple criteria
     */
    @Query("""
        SELECT s FROM Station s
        WHERE (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:city IS NULL OR LOWER(s.city) = LOWER(:city))
        AND (:state IS NULL OR LOWER(s.state) = LOWER(:state))
        AND (:status IS NULL OR s.status = :status)
        AND (:stationType IS NULL OR s.stationType = :stationType)
    """)
    fun searchStations(
        @Param("name") name: String?,
        @Param("city") city: String?,
        @Param("state") state: String?,
        @Param("status") status: StationStatus?,
        @Param("stationType") stationType: StationType?,
        pageable: Pageable
    ): Page<Station>

    /**
     * Find stations within a geographic radius
     */
    @Query("""
        SELECT s FROM Station s
        WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) *
               cos(radians(s.longitude) - radians(:longitude)) +
               sin(radians(:latitude)) * sin(radians(s.latitude)))) <= :radiusKm
        AND s.status = :status
        ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) *
                 cos(radians(s.longitude) - radians(:longitude)) +
                 sin(radians(:latitude)) * sin(radians(s.latitude))))
    """)
    fun findStationsWithinRadius(
        @Param("latitude") latitude: BigDecimal,
        @Param("longitude") longitude: BigDecimal,
        @Param("radiusKm") radiusKm: Double,
        @Param("status") status: StationStatus = StationStatus.ACTIVE
    ): List<Station>

    /**
     * Find nearest stations to a location
     */
    @Query("""
        SELECT s FROM Station s
        WHERE s.status = :status
        ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) *
                 cos(radians(s.longitude) - radians(:longitude)) +
                 sin(radians(:latitude)) * sin(radians(s.latitude))))
    """)
    fun findNearestStations(
        @Param("latitude") latitude: BigDecimal,
        @Param("longitude") longitude: BigDecimal,
        @Param("status") status: StationStatus = StationStatus.ACTIVE,
        pageable: Pageable
    ): Page<Station>

    /**
     * Find stations with specific services
     */
    @Query("SELECT s FROM Station s WHERE s.servicesOffered LIKE %:service% AND s.status = :status")
    fun findStationsWithService(
        @Param("service") service: String,
        @Param("status") status: StationStatus = StationStatus.ACTIVE
    ): List<Station>

    /**
     * Find 24-hour stations
     */
    fun findByIs24HoursAndStatus(is24Hours: Boolean, status: StationStatus): List<Station>

    /**
     * Find stations with convenience store
     */
    fun findByHasConvenienceStoreAndStatus(hasConvenienceStore: Boolean, status: StationStatus): List<Station>

    /**
     * Find stations by pump count range
     */
    @Query("SELECT s FROM Station s WHERE s.pumpCount BETWEEN :minPumps AND :maxPumps AND s.status = :status")
    fun findByPumpCountRange(
        @Param("minPumps") minPumps: Int,
        @Param("maxPumps") maxPumps: Int,
        @Param("status") status: StationStatus = StationStatus.ACTIVE
    ): List<Station>

    /**
     * Find stations created within date range
     */
    @Query("SELECT s FROM Station s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Station>

    /**
     * Count stations by status
     */
    fun countByStatus(status: StationStatus): Long

    /**
     * Count stations by type
     */
    fun countByStationType(stationType: StationType): Long

    /**
     * Count stations by city
     */
    fun countByCity(city: String): Long

    /**
     * Count stations by state
     */
    fun countByState(state: String): Long

    /**
     * Get total pump count across all active stations
     */
    @Query("SELECT SUM(s.pumpCount) FROM Station s WHERE s.status = :status")
    fun getTotalPumpCount(@Param("status") status: StationStatus = StationStatus.ACTIVE): Long?

    /**
     * Get average pump count per station
     */
    @Query("SELECT AVG(s.pumpCount) FROM Station s WHERE s.status = :status")
    fun getAveragePumpCount(@Param("status") status: StationStatus = StationStatus.ACTIVE): Double?

    /**
     * Update station status
     */
    @Modifying
    @Query("UPDATE Station s SET s.status = :status, s.updatedAt = :updatedAt, s.updatedBy = :updatedBy WHERE s.id = :stationId")
    fun updateStationStatus(
        @Param("stationId") stationId: Long,
        @Param("status") status: StationStatus,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update station manager
     */
    @Modifying
    @Query("UPDATE Station s SET s.managerName = :managerName, s.updatedAt = :updatedAt, s.updatedBy = :updatedBy WHERE s.id = :stationId")
    fun updateStationManager(
        @Param("stationId") stationId: Long,
        @Param("managerName") managerName: String?,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update station contact information
     */
    @Modifying
    @Query("""
        UPDATE Station s
        SET s.phoneNumber = :phoneNumber, s.email = :email, s.updatedAt = :updatedAt, s.updatedBy = :updatedBy
        WHERE s.id = :stationId
    """)
    fun updateStationContact(
        @Param("stationId") stationId: Long,
        @Param("phoneNumber") phoneNumber: String?,
        @Param("email") email: String?,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Bulk update station status
     */
    @Modifying
    @Query("UPDATE Station s SET s.status = :newStatus, s.updatedAt = :updatedAt WHERE s.status = :currentStatus")
    fun bulkUpdateStationStatus(
        @Param("currentStatus") currentStatus: StationStatus,
        @Param("newStatus") newStatus: StationStatus,
        @Param("updatedAt") updatedAt: LocalDateTime
    ): Int

    /**
     * Get station statistics
     */
    @Query("""
        SELECT
            COUNT(s) as totalStations,
            COUNT(CASE WHEN s.status = 'ACTIVE' THEN 1 END) as activeStations,
            COUNT(CASE WHEN s.status = 'INACTIVE' THEN 1 END) as inactiveStations,
            COUNT(CASE WHEN s.status = 'MAINTENANCE' THEN 1 END) as maintenanceStations,
            COUNT(CASE WHEN s.is24Hours = true THEN 1 END) as stations24Hours,
            COUNT(CASE WHEN s.hasConvenienceStore = true THEN 1 END) as stationsWithStore,
            AVG(s.pumpCount) as averagePumpCount,
            SUM(s.pumpCount) as totalPumps
        FROM Station s
    """)
    fun getStationStatistics(): Map<String, Any>

    /**
     * Find stations needing maintenance (example criteria)
     */
    @Query("""
        SELECT s FROM Station s
        WHERE s.status = 'ACTIVE'
        AND s.updatedAt < :thresholdDate
        ORDER BY s.updatedAt ASC
    """)
    fun findStationsNeedingMaintenance(@Param("thresholdDate") thresholdDate: LocalDateTime): List<Station>

    /**
     * Find stations by multiple statuses
     */
    @Query("SELECT s FROM Station s WHERE s.status IN :statuses")
    fun findByStatusIn(@Param("statuses") statuses: List<StationStatus>): List<Station>

    /**
     * Find stations with employees count
     */
    @Query("""
        SELECT s, COUNT(e) as employeeCount
        FROM Station s
        LEFT JOIN s.employees e
        WHERE e.isActive = true OR e.isActive IS NULL
        GROUP BY s
        HAVING COUNT(e) >= :minEmployees
    """)
    fun findStationsWithMinimumEmployees(@Param("minEmployees") minEmployees: Long): List<Any>

    /**
     * Find understaffed stations
     */
    @Query("""
        SELECT s
        FROM Station s
        LEFT JOIN s.employees e ON e.isActive = true
        WHERE s.status = 'ACTIVE'
        GROUP BY s
        HAVING COUNT(e) < :minimumStaff
    """)
    fun findUnderstaffedStations(@Param("minimumStaff") minimumStaff: Long): List<Station>
}