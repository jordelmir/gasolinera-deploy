package com.gasolinerajsm.stationservice.repository

import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.model.StationType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("StationRepository Integration Tests")
class StationRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var stationRepository: StationRepository

    private lateinit var testStation1: Station
    private lateinit var testStation2: Station
    private lateinit var testStation3: Station

    @BeforeEach
    fun setUp() {
        // Create test stations
        testStation1 = Station(
            name = "Central Station",
            code = "CNT001",
            address = "123 Central Ave",
            city = "Mexico City",
            state = "CDMX",
            postalCode = "12345",
            latitude = BigDecimal("19.4326"),
            longitude = BigDecimal("-99.1332"),
            status = StationStatus.ACTIVE,
            stationType = StationType.FULL_SERVICE,
            pumpCount = 8,
            is24Hours = true,
            hasConvenienceStore = true,
            servicesOffered = "Fuel, Car Wash, Convenience Store"
        )

        testStation2 = Station(
            name = "North Station",
            code = "NTH002",
            address = "456 North St",
            city = "Guadalajara",
            state = "Jalisco",
            postalCode = "54321",
            latitude = BigDecimal("20.6597"),
            longitude = BigDecimal("-103.3496"),
            status = StationStatus.ACTIVE,
            stationType = StationType.SELF_SERVICE,
            pumpCount = 6,
            is24Hours = false,
            hasConvenienceStore = false,
            servicesOffered = "Fuel"
        )

        testStation3 = Station(
            name = "South Station",
            code = "STH003",
            address = "789 South Blvd",
            city = "Mexico City",
            state = "CDMX",
            postalCode = "67890",
            latitude = BigDecimal("19.3000"),
            longitude = BigDecimal("-99.2000"),
            status = StationStatus.MAINTENANCE,
            stationType = StationType.HYBRID,
            pumpCount = 4,
            is24Hours = false,
            hasConvenienceStore = true,
            servicesOffered = "Fuel, Convenience Store"
        )

        // Persist test data
        entityManager.persistAndFlush(testStation1)
        entityManager.persistAndFlush(testStation2)
        entityManager.persistAndFlush(testStation3)
    }

    @Nested
    @DisplayName("Basic Query Tests")
    inner class BasicQueryTests {

        @Test
        @DisplayName("Should find station by code")
        fun shouldFindStationByCode() {
            // When
            val foundStation = stationRepository.findByCode("CNT001")

            // Then
            assertNotNull(foundStation)
            assertEquals("Central Station", foundStation?.name)
            assertEquals("CNT001", foundStation?.code)
        }

        @Test
        @DisplayName("Should return null for non-existent code")
        fun shouldReturnNullForNonExistentCode() {
            // When
            val foundStation = stationRepository.findByCode("NONEXISTENT")

            // Then
            assertNull(foundStation)
        }

        @Test
        @DisplayName("Should check if station code exists")
        fun shouldCheckIfStationCodeExists() {
            // When & Then
            assertTrue(stationRepository.existsByCode("CNT001"))
            assertFalse(stationRepository.existsByCode("NONEXISTENT"))
        }

        @Test
        @DisplayName("Should find station by code and status")
        fun shouldFindStationByCodeAndStatus() {
            // When
            val activeStation = stationRepository.findByCodeAndStatus("CNT001", StationStatus.ACTIVE)
            val inactiveStation = stationRepository.findByCodeAndStatus("CNT001", StationStatus.INACTIVE)

            // Then
            assertNotNull(activeStation)
            assertEquals("Central Station", activeStation?.name)
            assertNull(inactiveStation)
        }
    }

    @Nested
    @DisplayName("Status-based Query Tests")
    inner class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find stations by status")
        fun shouldFindStationsByStatus() {
            // When
            val activeStations = stationRepository.findByStatus(StationStatus.ACTIVE)
            val maintenanceStations = stationRepository.findByStatus(StationStatus.MAINTENANCE)

            // Then
            assertEquals(2, activeStations.size)
            assertEquals(1, maintenanceStations.size)
            assertEquals("South Station", maintenanceStations[0].name)
        }

        @Test
        @DisplayName("Should find stations by multiple statuses")
        fun shouldFindStationsByMultipleStatuses() {
            // When
            val stations = stationRepository.findByStatusIn(
                listOf(StationStatus.ACTIVE, StationStatus.MAINTENANCE)
            )

            // Then
            assertEquals(3, stations.size)
        }

        @Test
        @DisplayName("Should count stations by status")
        fun shouldCountStationsByStatus() {
            // When & Then
            assertEquals(2L, stationRepository.countByStatus(StationStatus.ACTIVE))
            assertEquals(1L, stationRepository.countByStatus(StationStatus.MAINTENANCE))
            assertEquals(0L, stationRepository.countByStatus(StationStatus.INACTIVE))
        }
    }

    @Nested
    @DisplayName("Location-based Query Tests")
    inner class LocationBasedQueryTests {

        @Test
        @DisplayName("Should find stations by city")
        fun shouldFindStationsByCity() {
            // When
            val mexicoCityStations = stationRepository.findByCity("Mexico City")
            val guadalajaraStations = stationRepository.findByCity("Guadalajara")

            // Then
            assertEquals(2, mexicoCityStations.size)
            assertEquals(1, guadalajaraStations.size)
        }

        @Test
        @DisplayName("Should find stations by city and status")
        fun shouldFindStationsByCityAndStatus() {
            // When
            val activeMexicoCityStations = stationRepository.findByCityAndStatus("Mexico City", StationStatus.ACTIVE)

            // Then
            assertEquals(1, activeMexicoCityStations.size)
            assertEquals("Central Station", activeMexicoCityStations[0].name)
        }

        @Test
        @DisplayName("Should find stations by state")
        fun shouldFindStationsByState() {
            // When
            val cdmxStations = stationRepository.findByState("CDMX")
            val jaliscoStations = stationRepository.findByState("Jalisco")

            // Then
            assertEquals(2, cdmxStations.size)
            assertEquals(1, jaliscoStations.size)
        }

        @Test
        @DisplayName("Should find stations within radius")
        fun shouldFindStationsWithinRadius() {
            // Given - Mexico City center coordinates
            val centerLat = BigDecimal("19.4326")
            val centerLng = BigDecimal("-99.1332")
            val radiusKm = 50.0

            // When
            val nearbyStations = stationRepository.findStationsWithinRadius(
                centerLat, centerLng, radiusKm, StationStatus.ACTIVE
            )

            // Then
            assertTrue(nearbyStations.isNotEmpty())
            // Should include stations in Mexico City area
        }

        @Test
        @DisplayName("Should find nearest stations")
        fun shouldFindNearestStations() {
            // Given
            val lat = BigDecimal("19.4326")
            val lng = BigDecimal("-99.1332")
            val pageable = PageRequest.of(0, 2)

            // When
            val nearestStations = stationRepository.findNearestStations(lat, lng, StationStatus.ACTIVE, pageable)

            // Then
            assertEquals(2, nearestStations.content.size)
            // Results should be ordered by distance
        }
    }

    @Nested
    @DisplayName("Type and Feature-based Query Tests")
    inner class TypeAndFeatureBasedQueryTests {

        @Test
        @DisplayName("Should find stations by type")
        fun shouldFindStationsByType() {
            // When
            val fullServiceStations = stationRepository.findByStationType(StationType.FULL_SERVICE)
            val selfServiceStations = stationRepository.findByStationType(StationType.SELF_SERVICE)

            // Then
            assertEquals(1, fullServiceStations.size)
            assertEquals(1, selfServiceStations.size)
            assertEquals("Central Station", fullServiceStations[0].name)
            assertEquals("North Station", selfServiceStations[0].name)
        }

        @Test
        @DisplayName("Should find 24-hour stations")
        fun shouldFind24HourStations() {
            // When
            val stations24Hours = stationRepository.findByIs24HoursAndStatus(true, StationStatus.ACTIVE)

            // Then
            assertEquals(1, stations24Hours.size)
            assertEquals("Central Station", stations24Hours[0].name)
        }

        @Test
        @DisplayName("Should find stations with convenience store")
        fun shouldFindStationsWithConvenienceStore() {
            // When
            val stationsWithStore = stationRepository.findByHasConvenienceStoreAndStatus(true, StationStatus.ACTIVE)

            // Then
            assertEquals(1, stationsWithStore.size)
            assertEquals("Central Station", stationsWithStore[0].name)
        }

        @Test
        @DisplayName("Should find stations with specific service")
        fun shouldFindStationsWithSpecificService() {
            // When
            val stationsWithCarWash = stationRepository.findStationsWithService("Car Wash", StationStatus.ACTIVE)

            // Then
            assertEquals(1, stationsWithCarWash.size)
            assertEquals("Central Station", stationsWithCarWash[0].name)
        }

        @Test
        @DisplayName("Should find stations by pump count range")
        fun shouldFindStationsByPumpCountRange() {
            // When
            val mediumStations = stationRepository.findByPumpCountRange(4, 6, StationStatus.ACTIVE)
            val largeStations = stationRepository.findByPumpCountRange(7, 10, StationStatus.ACTIVE)

            // Then
            assertEquals(1, mediumStations.size)
            assertEquals("North Station", mediumStations[0].name)
            assertEquals(1, largeStations.size)
            assertEquals("Central Station", largeStations[0].name)
        }
    }

    @Nested
    @DisplayName("Search Tests")
    inner class SearchTests {

        @Test
        @DisplayName("Should search stations by name")
        fun shouldSearchStationsByName() {
            // When
            val centralStations = stationRepository.searchByName("Central")
            val stationStations = stationRepository.searchByName("Station") // Should match all

            // Then
            assertEquals(1, centralStations.size)
            assertEquals("Central Station", centralStations[0].name)
            assertEquals(3, stationStations.size)
        }

        @Test
        @DisplayName("Should search stations by name with pagination")
        fun shouldSearchStationsByNameWithPagination() {
            // Given
            val pageable = PageRequest.of(0, 2)

            // When
            val searchResults = stationRepository.searchByName("Station", pageable)

            // Then
            assertEquals(3, searchResults.totalElements)
            assertEquals(2, searchResults.content.size)
        }

        @Test
        @DisplayName("Should search stations by multiple criteria")
        fun shouldSearchStationsByMultipleCriteria() {
            // Given
            val pageable = PageRequest.of(0, 10)

            // When
            val searchResults = stationRepository.searchStations(
                name = "Station",
                city = "Mexico City",
                state = null,
                status = StationStatus.ACTIVE,
                stationType = null,
                pageable = pageable
            )

            // Then
            assertEquals(1, searchResults.totalElements)
            assertEquals("Central Station", searchResults.content[0].name)
        }
    }

    @Nested
    @DisplayName("Statistics and Aggregation Tests")
    inner class StatisticsAndAggregationTests {

        @Test
        @DisplayName("Should count stations by type")
        fun shouldCountStationsByType() {
            // When & Then
            assertEquals(1L, stationRepository.countByStationType(StationType.FULL_SERVICE))
            assertEquals(1L, stationRepository.countByStationType(StationType.SELF_SERVICE))
            assertEquals(1L, stationRepository.countByStationType(StationType.HYBRID))
        }

        @Test
        @DisplayName("Should count stations by city")
        fun shouldCountStationsByCity() {
            // When & Then
            assertEquals(2L, stationRepository.countByCity("Mexico City"))
            assertEquals(1L, stationRepository.countByCity("Guadalajara"))
        }

        @Test
        @DisplayName("Should get total pump count")
        fun shouldGetTotalPumpCount() {
            // When
            val totalPumps = stationRepository.getTotalPumpCount(StationStatus.ACTIVE)

            // Then
            assertEquals(14L, totalPumps) // 8 + 6 = 14 (only active stations)
        }

        @Test
        @DisplayName("Should get average pump count")
        fun shouldGetAveragePumpCount() {
            // When
            val averagePumps = stationRepository.getAveragePumpCount(StationStatus.ACTIVE)

            // Then
            assertEquals(7.0, averagePumps) // (8 + 6) / 2 = 7.0
        }
    }

    @Nested
    @DisplayName("Update Operation Tests")
    @Transactional
    inner class UpdateOperationTests {

        @Test
        @DisplayName("Should update station status")
        fun shouldUpdateStationStatus() {
            // Given
            val stationId = testStation1.id
            val newStatus = StationStatus.MAINTENANCE
            val updatedAt = LocalDateTime.now()
            val updatedBy = "admin"

            // When
            val updatedRows = stationRepository.updateStationStatus(stationId, newStatus, updatedAt, updatedBy)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedStation = stationRepository.findById(stationId).orElse(null)
            assertNotNull(updatedStation)
            assertEquals(newStatus, updatedStation.status)
            assertEquals(updatedBy, updatedStation.updatedBy)
        }

        @Test
        @DisplayName("Should update station manager")
        fun shouldUpdateStationManager() {
            // Given
            val stationId = testStation1.id
            val managerName = "John Manager"
            val updatedAt = LocalDateTime.now()
            val updatedBy = "admin"

            // When
            val updatedRows = stationRepository.updateStationManager(stationId, managerName, updatedAt, updatedBy)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedStation = stationRepository.findById(stationId).orElse(null)
            assertNotNull(updatedStation)
            assertEquals(managerName, updatedStation.managerName)
        }

        @Test
        @DisplayName("Should update station contact information")
        fun shouldUpdateStationContactInformation() {
            // Given
            val stationId = testStation1.id
            val phoneNumber = "+525512345678"
            val email = "central@gasolinera.com"
            val updatedAt = LocalDateTime.now()
            val updatedBy = "admin"

            // When
            val updatedRows = stationRepository.updateStationContact(stationId, phoneNumber, email, updatedAt, updatedBy)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedStation = stationRepository.findById(stationId).orElse(null)
            assertNotNull(updatedStation)
            assertEquals(phoneNumber, updatedStation.phoneNumber)
            assertEquals(email, updatedStation.email)
        }

        @Test
        @DisplayName("Should bulk update station status")
        fun shouldBulkUpdateStationStatus() {
            // Given
            val currentStatus = StationStatus.ACTIVE
            val newStatus = StationStatus.MAINTENANCE
            val updatedAt = LocalDateTime.now()

            // When
            val updatedRows = stationRepository.bulkUpdateStationStatus(currentStatus, newStatus, updatedAt)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(2, updatedRows) // Should update 2 active stations

            val maintenanceStations = stationRepository.findByStatus(StationStatus.MAINTENANCE)
            assertEquals(3, maintenanceStations.size) // 1 original + 2 updated
        }
    }

    @Nested
    @DisplayName("Date-based Query Tests")
    inner class DateBasedQueryTests {

        @Test
        @DisplayName("Should find stations created within date range")
        fun shouldFindStationsCreatedWithinDateRange() {
            // Given
            val startDate = LocalDateTime.now().minusHours(1)
            val endDate = LocalDateTime.now().plusHours(1)

            // When
            val stationsInRange = stationRepository.findByCreatedAtBetween(startDate, endDate)

            // Then
            assertEquals(3, stationsInRange.size)
        }

        @Test
        @DisplayName("Should find stations needing maintenance")
        fun shouldFindStationsNeedingMaintenance() {
            // Given
            val thresholdDate = LocalDateTime.now().plusDays(1) // Future date for test

            // When
            val stationsNeedingMaintenance = stationRepository.findStationsNeedingMaintenance(thresholdDate)

            // Then
            assertEquals(2, stationsNeedingMaintenance.size) // Active stations that haven't been updated recently
        }
    }
}