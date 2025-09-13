package com.gasolinerajsm.stationservice.service

import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.model.StationType
import com.gasolinerajsm.stationservice.repository.StationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.*

@DisplayName("Station Service Tests")
class StationServiceTest {

    private lateinit var stationRepository: StationRepository
    private lateinit var stationService: StationService

    private val testStation = Station(
        id = 1L,
        name = "Test Station",
        code = "TST001",
        address = "123 Test Street",
        city = "Test City",
        state = "Test State",
        postalCode = "12345",
        latitude = BigDecimal("19.4326"),
        longitude = BigDecimal("-99.1332"),
        status = StationStatus.ACTIVE,
        stationType = StationType.FULL_SERVICE,
        pumpCount = 8
    )

    @BeforeEach
    fun setUp() {
        stationRepository = mockk()
        stationService = StationService(stationRepository)
    }

    @Nested
    @DisplayName("Station Creation Tests")
    inner class StationCreationTests {

        @Test
        @DisplayName("Should create station successfully")
        fun shouldCreateStationSuccessfully() {
            // Given
            val newStation = testStation.copy(id = 0L)
            val createdBy = "admin"

            every { stationRepository.existsByCode(newStation.code) } returns false
            every { stationRepository.save(any()) } returns testStation

            // When
            val result = stationService.createStation(newStation, createdBy)

            // Then
            assertEquals(testStation.id, result.id)
            assertEquals(testStation.name, result.name)
            assertEquals(testStation.code, result.code)

            verify { stationRepository.existsByCode(newStation.code) }
            verify { stationRepository.save(any()) }
        }

        @Test
        @DisplayName("Should throw exception when station code already exists")
        fun shouldThrowExceptionWhenStationCodeAlreadyExists() {
            // Given
            val newStation = testStation.copy(id = 0L)
            every { stationRepository.existsByCode(newStation.code) } returns true

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                stationService.createStation(newStation)
            }
            assertEquals("Station code 'TST001' already exists", exception.message)
        }
    }

    @Nested
    @DisplayName("Station Retrieval Tests")
    inner class StationRetrievalTests {

        @Test
        @DisplayName("Should get station by ID successfully")
        fun shouldGetStationByIdSuccessfully() {
            // Given
            val stationId = 1L
            every { stationRepository.findById(stationId) } returns Optional.of(testStation)

            // When
            val result = stationService.getStationById(stationId)

            // Then
            assertEquals(testStation, result)
            verify { stationRepository.findById(stationId) }
        }

        @Test
        @DisplayName("Should throw exception when station not found by ID")
        fun shouldThrowExceptionWhenStationNotFoundById() {
            // Given
            val stationId = 999L
            every { stationRepository.findById(stationId) } returns Optional.empty()

            // When & Then
            val exception = assertThrows<StationNotFoundException> {
                stationService.getStationById(stationId)
            }
            assertEquals("Station not found with ID: 999", exception.message)
        }

        @Test
        @DisplayName("Should get station by code successfully")
        fun shouldGetStationByCodeSuccessfully() {
            // Given
            val code = "TST001"
            every { stationRepository.findByCode(code) } returns testStation

            // When
            val result = stationService.getStationByCode(code)

            // Then
            assertEquals(testStation, result)
            verify { stationRepository.findByCode(code) }
        }

        @Test
        @DisplayName("Should return null when station not found by code")
        fun shouldReturnNullWhenStationNotFoundByCode() {
            // Given
            val code = "NONEXISTENT"
            every { stationRepository.findByCode(code) } returns null

            // When
            val result = stationService.getStationByCode(code)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Station Query Tests")
    inner class StationQueryTests {

        @Test
        @DisplayName("Should get all stations with pagination")
        fun shouldGetAllStationsWithPagination() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val stationPage = PageImpl(listOf(testStation), pageable, 1)
            every { stationRepository.findAll(pageable) } returns stationPage

            // When
            val result = stationService.getAllStations(pageable)

            // Then
            assertEquals(1, result.totalElements)
            assertEquals(testStation, result.content[0])
            verify { stationRepository.findAll(pageable) }
        }

        @Test
        @DisplayName("Should get stations by status")
        fun shouldGetStationsByStatus() {
            // Given
            val status = StationStatus.ACTIVE
            val stations = listOf(testStation)
            every { stationRepository.findByStatus(status) } returns stations

            // When
            val result = stationService.getStationsByStatus(status)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findByStatus(status) }
        }

        @Test
        @DisplayName("Should get active stations")
        fun shouldGetActiveStations() {
            // Given
            val stations = listOf(testStation)
            every { stationRepository.findByStatus(StationStatus.ACTIVE) } returns stations

            // When
            val result = stationService.getActiveStations()

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findByStatus(StationStatus.ACTIVE) }
        }

        @Test
        @DisplayName("Should get stations by type")
        fun shouldGetStationsByType() {
            // Given
            val stationType = StationType.FULL_SERVICE
            val stations = listOf(testStation)
            every { stationRepository.findByStationType(stationType) } returns stations

            // When
            val result = stationService.getStationsByType(stationType)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findByStationType(stationType) }
        }

        @Test
        @DisplayName("Should get stations by city")
        fun shouldGetStationsByCity() {
            // Given
            val city = "Test City"
            val stations = listOf(testStation)
            every { stationRepository.findByCity(city) } returns stations

            // When
            val result = stationService.getStationsByCity(city)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findByCity(city) }
        }

        @Test
        @DisplayName("Should search stations by name")
        fun shouldSearchStationsByName() {
            // Given
            val searchTerm = "Test"
            val stations = listOf(testStation)
            every { stationRepository.searchByName(searchTerm) } returns stations

            // When
            val result = stationService.searchStationsByName(searchTerm)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.searchByName(searchTerm) }
        }
    }

    @Nested
    @DisplayName("Location-based Query Tests")
    inner class LocationBasedQueryTests {

        @Test
        @DisplayName("Should find stations within radius")
        fun shouldFindStationsWithinRadius() {
            // Given
            val latitude = BigDecimal("19.4326")
            val longitude = BigDecimal("-99.1332")
            val radiusKm = 10.0
            val stations = listOf(testStation)

            every {
                stationRepository.findStationsWithinRadius(latitude, longitude, radiusKm, StationStatus.ACTIVE)
            } returns stations

            // When
            val result = stationService.findStationsWithinRadius(latitude, longitude, radiusKm)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify {
                stationRepository.findStationsWithinRadius(latitude, longitude, radiusKm, StationStatus.ACTIVE)
            }
        }

        @Test
        @DisplayName("Should find nearest stations")
        fun shouldFindNearestStations() {
            // Given
            val latitude = BigDecimal("19.4326")
            val longitude = BigDecimal("-99.1332")
            val pageable = PageRequest.of(0, 5)
            val stationPage = PageImpl(listOf(testStation), pageable, 1)

            every {
                stationRepository.findNearestStations(latitude, longitude, StationStatus.ACTIVE, pageable)
            } returns stationPage

            // When
            val result = stationService.findNearestStations(latitude, longitude, pageable)

            // Then
            assertEquals(1, result.totalElements)
            assertEquals(testStation, result.content[0])
            verify {
                stationRepository.findNearestStations(latitude, longitude, StationStatus.ACTIVE, pageable)
            }
        }

        @Test
        @DisplayName("Should handle errors in location queries gracefully")
        fun shouldHandleErrorsInLocationQueriesGracefully() {
            // Given
            val latitude = BigDecimal("19.4326")
            val longitude = BigDecimal("-99.1332")
            val radiusKm = 10.0

            every {
                stationRepository.findStationsWithinRadius(latitude, longitude, radiusKm, StationStatus.ACTIVE)
            } throws RuntimeException("Database error")

            // When
            val result = stationService.findStationsWithinRadius(latitude, longitude, radiusKm)

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Station Update Tests")
    inner class StationUpdateTests {

        @Test
        @DisplayName("Should update station successfully")
        fun shouldUpdateStationSuccessfully() {
            // Given
            val stationId = 1L
            val updatedStation = testStation.copy(name = "Updated Station")
            val updatedBy = "admin"

            every { stationRepository.findById(stationId) } returns Optional.of(testStation)
            every { stationRepository.save(any()) } returns updatedStation

            // When
            val result = stationService.updateStation(stationId, updatedStation, updatedBy)

            // Then
            assertEquals("Updated Station", result.name)
            verify { stationRepository.findById(stationId) }
            verify { stationRepository.save(any()) }
        }

        @Test
        @DisplayName("Should activate station")
        fun shouldActivateStation() {
            // Given
            val stationId = 1L
            val inactiveStation = testStation.copy(status = StationStatus.INACTIVE)
            val activeStation = testStation.copy(status = StationStatus.ACTIVE)
            val updatedBy = "admin"

            every { stationRepository.findById(stationId) } returns Optional.of(inactiveStation)
            every { stationRepository.save(any()) } returns activeStation

            // When
            val result = stationService.activateStation(stationId, updatedBy)

            // Then
            assertEquals(StationStatus.ACTIVE, result.status)
            verify { stationRepository.findById(stationId) }
            verify { stationRepository.save(any()) }
        }

        @Test
        @DisplayName("Should deactivate station")
        fun shouldDeactivateStation() {
            // Given
            val stationId = 1L
            val inactiveStation = testStation.copy(status = StationStatus.INACTIVE)
            val updatedBy = "admin"

            every { stationRepository.findById(stationId) } returns Optional.of(testStation)
            every { stationRepository.save(any()) } returns inactiveStation

            // When
            val result = stationService.deactivateStation(stationId, updatedBy)

            // Then
            assertEquals(StationStatus.INACTIVE, result.status)
            verify { stationRepository.findById(stationId) }
            verify { stationRepository.save(any()) }
        }

        @Test
        @DisplayName("Should set station to maintenance")
        fun shouldSetStationToMaintenance() {
            // Given
            val stationId = 1L
            val maintenanceStation = testStation.copy(status = StationStatus.MAINTENANCE)
            val updatedBy = "admin"

            every { stationRepository.findById(stationId) } returns Optional.of(testStation)
            every { stationRepository.save(any()) } returns maintenanceStation

            // When
            val result = stationService.setStationMaintenance(stationId, updatedBy)

            // Then
            assertEquals(StationStatus.MAINTENANCE, result.status)
            verify { stationRepository.findById(stationId) }
            verify { stationRepository.save(any()) }
        }

        @Test
        @DisplayName("Should update station manager")
        fun shouldUpdateStationManager() {
            // Given
            val stationId = 1L
            val managerName = "John Manager"
            val updatedStation = testStation.copy(managerName = managerName)
            val updatedBy = "admin"

            every { stationRepository.findById(stationId) } returns Optional.of(testStation)
            every { stationRepository.save(any()) } returns updatedStation

            // When
            val result = stationService.updateStationManager(stationId, managerName, updatedBy)

            // Then
            assertEquals(managerName, result.managerName)
            verify { stationRepository.findById(stationId) }
            verify { stationRepository.save(any()) }
        }

        @Test
        @DisplayName("Should update station contact information")
        fun shouldUpdateStationContactInformation() {
            // Given
            val stationId = 1L
            val phoneNumber = "+525512345678"
            val email = "test@station.com"
            val updatedStation = testStation.copy(phoneNumber = phoneNumber, email = email)
            val updatedBy = "admin"

            every { stationRepository.findById(stationId) } returns Optional.of(testStation)
            every { stationRepository.save(any()) } returns updatedStation

            // When
            val result = stationService.updateStationContact(stationId, phoneNumber, email, updatedBy)

            // Then
            assertEquals(phoneNumber, result.phoneNumber)
            assertEquals(email, result.email)
            verify { stationRepository.findById(stationId) }
            verify { stationRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("Station Feature Query Tests")
    inner class StationFeatureQueryTests {

        @Test
        @DisplayName("Should find stations with specific service")
        fun shouldFindStationsWithSpecificService() {
            // Given
            val service = "Car Wash"
            val stations = listOf(testStation)
            every { stationRepository.findStationsWithService(service, StationStatus.ACTIVE) } returns stations

            // When
            val result = stationService.findStationsWithService(service)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findStationsWithService(service, StationStatus.ACTIVE) }
        }

        @Test
        @DisplayName("Should find 24-hour stations")
        fun shouldFind24HourStations() {
            // Given
            val stations = listOf(testStation)
            every { stationRepository.findByIs24HoursAndStatus(true, StationStatus.ACTIVE) } returns stations

            // When
            val result = stationService.find24HourStations()

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findByIs24HoursAndStatus(true, StationStatus.ACTIVE) }
        }

        @Test
        @DisplayName("Should find stations with convenience store")
        fun shouldFindStationsWithConvenienceStore() {
            // Given
            val stations = listOf(testStation)
            every { stationRepository.findByHasConvenienceStoreAndStatus(true, StationStatus.ACTIVE) } returns stations

            // When
            val result = stationService.findStationsWithConvenienceStore()

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findByHasConvenienceStoreAndStatus(true, StationStatus.ACTIVE) }
        }
    }

    @Nested
    @DisplayName("Station Statistics Tests")
    inner class StationStatisticsTests {

        @Test
        @DisplayName("Should get station statistics")
        fun shouldGetStationStatistics() {
            // Given
            val statsMap = mapOf(
                "totalStations" to 10L,
                "activeStations" to 8L,
                "inactiveStations" to 1L,
                "maintenanceStations" to 1L,
                "stations24Hours" to 3L,
                "stationsWithStore" to 5L,
                "averagePumpCount" to 6.5,
                "totalPumps" to 65L
            )
            every { stationRepository.getStationStatistics() } returns statsMap

            // When
            val result = stationService.getStationStatistics()

            // Then
            assertEquals(10L, result.totalStations)
            assertEquals(8L, result.activeStations)
            assertEquals(1L, result.inactiveStations)
            assertEquals(1L, result.maintenanceStations)
            assertEquals(3L, result.stations24Hours)
            assertEquals(5L, result.stationsWithStore)
            assertEquals(6.5, result.averagePumpCount)
            assertEquals(65L, result.totalPumps)
            verify { stationRepository.getStationStatistics() }
        }

        @Test
        @DisplayName("Should handle statistics errors gracefully")
        fun shouldHandleStatisticsErrorsGracefully() {
            // Given
            every { stationRepository.getStationStatistics() } throws RuntimeException("Database error")

            // When
            val result = stationService.getStationStatistics()

            // Then
            assertEquals(0L, result.totalStations)
            assertEquals(0L, result.activeStations)
            assertEquals(0.0, result.averagePumpCount)
        }
    }

    @Nested
    @DisplayName("Station Maintenance Tests")
    inner class StationMaintenanceTests {

        @Test
        @DisplayName("Should get stations needing maintenance")
        fun shouldGetStationsNeedingMaintenance() {
            // Given
            val stations = listOf(testStation)
            every { stationRepository.findStationsNeedingMaintenance(any()) } returns stations

            // When
            val result = stationService.getStationsNeedingMaintenance(90)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findStationsNeedingMaintenance(any()) }
        }

        @Test
        @DisplayName("Should get understaffed stations")
        fun shouldGetUnderstaffedStations() {
            // Given
            val stations = listOf(testStation)
            every { stationRepository.findUnderstaffedStations(2L) } returns stations

            // When
            val result = stationService.getUnderstaffedStations(2L)

            // Then
            assertEquals(1, result.size)
            assertEquals(testStation, result[0])
            verify { stationRepository.findUnderstaffedStations(2L) }
        }

        @Test
        @DisplayName("Should bulk update station status")
        fun shouldBulkUpdateStationStatus() {
            // Given
            val currentStatus = StationStatus.ACTIVE
            val newStatus = StationStatus.MAINTENANCE
            val updatedBy = "admin"

            every { stationRepository.bulkUpdateStationStatus(currentStatus, newStatus, any()) } returns 5

            // When
            val result = stationService.bulkUpdateStationStatus(currentStatus, newStatus, updatedBy)

            // Then
            assertEquals(5, result)
            verify { stationRepository.bulkUpdateStationStatus(currentStatus, newStatus, any()) }
        }
    }

    @Nested
    @DisplayName("Station Deletion Tests")
    inner class StationDeletionTests {

        @Test
        @DisplayName("Should delete station successfully (soft delete)")
        fun shouldDeleteStationSuccessfully() {
            // Given
            val stationId = 1L
            val updatedBy = "admin"
            val inactiveStation = testStation.copy(status = StationStatus.INACTIVE)

            every { stationRepository.findById(stationId) } returns Optional.of(testStation)
            every { stationRepository.save(any()) } returns inactiveStation

            // When
            val result = stationService.deleteStation(stationId, updatedBy)

            // Then
            assertTrue(result)
            verify { stationRepository.findById(stationId) }
            verify { stationRepository.save(any()) }
        }

        @Test
        @DisplayName("Should handle deletion errors gracefully")
        fun shouldHandleDeletionErrorsGracefully() {
            // Given
            val stationId = 999L
            val updatedBy = "admin"

            every { stationRepository.findById(stationId) } returns Optional.empty()

            // When
            val result = stationService.deleteStation(stationId, updatedBy)

            // Then
            assertFalse(result)
        }
    }
}