package com.gasolinerajsm.stationservice.infrastructure.persistence.jpa

import com.gasolinerajsm.stationservice.domain.model.StationStatus
import com.gasolinerajsm.stationservice.infrastructure.persistence.entity.StationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * JPA Repository for Station entities
 */
@Repository
interface StationJpaRepository : JpaRepository<StationEntity, UUID> {

    /**
     * Find all active stations
     */
    fun findByIsActiveTrue(): List<StationEntity>

    /**
     * Find stations by status
     */
    fun findByStatus(status: StationStatus): List<StationEntity>

    /**
     * Find stations by manager ID
     */
    fun findByManagerId(managerId: String): List<StationEntity>

    /**
     * Find stations within radius using Haversine formula
     */
    @Query("""
        SELECT s FROM StationEntity s
        WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) *
               cos(radians(s.longitude) - radians(:longitude)) +
               sin(radians(:latitude)) * sin(radians(s.latitude)))) <= :radiusKm
        ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) *
                 cos(radians(s.longitude) - radians(:longitude)) +
                 sin(radians(:latitude)) * sin(radians(s.latitude))))
    """)
    fun findWithinRadius(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusKm") radiusKm: Double
    ): List<StationEntity>

    /**
     * Find nearest stations to a location
     */
    @Query("""
        SELECT s FROM StationEntity s
        WHERE s.isActive = true
        ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) *
                 cos(radians(s.longitude) - radians(:longitude)) +
                 sin(radians(:latitude)) * sin(radians(s.latitude))))
    """)
    fun findNearestStations(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double
    ): List<StationEntity>

    /**
     * Search stations by name or address
     */
    @Query("""
        SELECT s FROM StationEntity s
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(s.address) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    fun searchByNameOrAddress(@Param("query") query: String): List<StationEntity>

    /**
     * Check if station exists by name and location within radius
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM StationEntity s
        WHERE LOWER(s.name) = LOWER(:name)
          AND (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) *
               cos(radians(s.longitude) - radians(:longitude)) +
               sin(radians(:latitude)) * sin(radians(s.latitude)))) <= :radiusKm
    """)
    fun existsByNameAndLocation(
        @Param("name") name: String,
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radiusKm") radiusKm: Double
    ): Boolean

    /**
     * Count active stations
     */
    fun countByIsActiveTrue(): Long

    /**
     * Count stations by status
     */
    fun countByStatus(status: StationStatus): Long
}