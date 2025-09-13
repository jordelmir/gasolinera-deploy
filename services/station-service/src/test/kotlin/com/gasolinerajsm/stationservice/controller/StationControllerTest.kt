package com.gasolinerajsm.stationservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gasolinerajsm.stationservice.dto.*
import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import com.gasolinerajsm.stationservice.service.StationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(StationController::class)
@DisplayName("Station Controller Tests")
class StationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var stationService: StationService

    private lateinit var testStation: Station
    private lateinit var testStationResponse: StationResponse
    private lateinit var createStationRequest: CreateStationRequest

    @BeforeEach
    fun setUp() {
        testStation = Station(
            id = 1L,
            name = "Test Station",
            address = "123 Test Street",
            latitude = BigDecimal("40.7128"),
            longitude = BigDecimal("-74.0060"),
            phoneNumber = "+1234567890",
            email = "test@station.com",
            managerName = "John Manager",
            status = StationStatus.ACTIVE
        )

        testStationResponse = StationResponse.fromStation(testStation)

        createStationRequest = CreateStationRequest(
            name = "New Station",
            address = "456 New Street",
            latitude = BigDecimal("40.7589"),
            longitude = BigDecimal("-73.9851"),
            phoneNumber = "+1987654321",
            email = "new@station.com",
            managerName = "Jane Manager"
        )
    }

    @Nested
    @DisplayName("Station CRUD Operations")
    inner class StationCrudOperations {

        @Test
        @DisplayName("Should create station successfully")
        fun shouldCreateStationSuccessfully() {
            // Given
            whenever(stationService.createStation(any<CreateStationRequest>())).thenReturn(testStationResponse)

            // When & Then
            mockMvc.perform(
                post("/api/v1/stations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createStationRequest))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value(testStationResponse.name))
                .andExpect(jsonPath("$.address").value(testStationResponse.address))
                .andExpect(jsonPath("$.phone_number").value(testStationResponse.phoneNumber))

            verify(stationService).createStation(any<CreateStationRequest>())
        }

        @Test
        @DisplayName("Should return validation error for invalid station data")
        fun shouldReturnValidationErrorForInvalidStationData() {
            // Given
            val invalidRequest = createStationRequest.copy(
                name = "", // Invalid: empty name
                phoneNumber = "invalid-phone" // Invalid: bad format
            )

            // When & Then
            mockMvc.perform(
                post("/api/v1/stations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
                .andExpect(status().isBadRequest)

            verify(stationService, never()).createStation(any())
        }

        @Test
        @DisplayName("Should get station by ID successfully")
        fun shouldGetStationByIdSuccessfully() {
            // Given
            whenever(stationService.getStationById(1L)).thenReturn(testStationResponse)

            // When & Then
            mockMvc.perform(get("/api/v1/stations/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(testStationResponse.name))

            verify(stationService).getStationById(1L)
        }

        @Test
        @DisplayName("Should get all stations with pagination")
        fun shouldGetAllStationsWithPagination() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val stationsPage = PageImpl(listOf(testStationResponse), pageable, 1)
            whenever(stationService.getAllStations(any())).thenReturn(stationsPage)

            // When & Then
            mockMvc.perform(
                get("/api/v1/stations")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].name").value(testStationResponse.name))
                .andExpect(jsonPath("$.totalElements").value(1))

            verify(stationService).getAllStations(any())
        }

        @Test
        @DisplayName("Should update station successfully")
        fun shouldUpdateStationSuccessfully() {
            // Given
            val updateRequest = UpdateStationRequest(
                name = "Updated Station",
                address = "789 Updated Street",
                latitude = BigDecimal("40.7000"),
                longitude = BigDecimal("-74.0000"),
                phoneNumber = "+1111111111",
                is24Hours = true,
                hasConvenienceStore = true,
                hasCarWash = false
            )
            val updatedResponse = testStationResponse.copy(name = "Updated Station")
            whenever(stationService.updateStation(eq(1L), any<UpdateStationRequest>())).thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                put("/api/v1/stations/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("Updated Station"))

            verify(stationService).updateStation(eq(1L), any<UpdateStationRequest>())
        }

        @Test
        @DisplayName("Should delete station successfully")
        fun shouldDeleteStationSuccessfully() {
            // Given
            doNothing().whenever(stationService).deleteStation(1L)

            // When & Then
            mockMvc.perform(delete("/api/v1/stations/1"))
                .andExpect(status().isNoContent)

            verify(stationService).deleteStation(1L)
        }
    }

    @Nested
    @DisplayName("Station Status Management")
    inner class StationStatusManagement {

        @Test
        @DisplayName("Should update station status successfully")
        fun shouldUpdateStationStatusSuccessfully() {
            // Given
            val statusRequest = UpdateStationStatusRequest(StationStatus.MAINTENANCE)
            val updatedResponse = testStationResponse.copy(status = StationStatus.MAINTENANCE)
            whenever(stationService.updateStationStatus(1L, StationStatus.MAINTENANCE)).thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                patch("/api/v1/stations/1/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("MAINTENANCE"))

            verify(stationService).updateStationStatus(1L, StationStatus.MAINTENANCE)
        }

        @Test
        @DisplayName("Should activate station successfully")
        fun shouldActivateStationSuccessfully() {
            // Given
            whenever(stationService.activateStation(1L)).thenReturn(testStationResponse)

            // When & Then
            mockMvc.perform(post("/api/v1/stations/1/activate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ACTIVE"))

            verify(stationService).activateStation(1L)
        }

        @Test
        @DisplayName("Should deactivate station successfully")
        fun shouldDeactivateStationSuccessfully() {
            // Given
            val deactivatedResponse = testStationResponse.copy(status = StationStatus.INACTIVE)
            whenever(stationService.deactivateStation(1L)).thenReturn(deactivatedResponse)

            // When & Then
            mockMvc.perform(post("/api/v1/stations/1/deactivate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("INACTIVE"))

            verify(stationService).deactivateStation(1L)
        }

        @Test
        @DisplayName("Should put station under maintenance successfully")
        fun shouldPutStationUnderMaintenanceSuccessfully() {
            // Given
            val maintenanceResponse = testStationResponse.copy(status = StationStatus.MAINTENANCE)
            whenever(stationService.putStationUnderMaintenance(1L)).thenReturn(maintenanceResponse)

            // When & Then
            mockMvc.perform(post("/api/v1/stations/1/maintenance"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("MAINTENANCE"))

            verify(stationService).putStationUnderMaintenance(1L)
        }
    }

    @Nested
    @DisplayName("Station Information Updates")
    inner class StationInformationUpdates {

        @Test
        @DisplayName("Should update station contact successfully")
        fun shouldUpdateStationContactSuccessfully() {
            // Given
            val contactRequest = UpdateStationContactRequest(
                phoneNumber = "+1999999999",
                email = "updated@station.com"
            )
            val updatedResponse = testStationResponse.copy(
                phoneNumber = "+1999999999",
                email = "updated@station.com"
            )
            whenever(stationService.updateStationContact(1L, contactRequest)).thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                patch("/api/v1/stations/1/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(contactRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.phone_number").value("+1999999999"))
                .andExpect(jsonPath("$.email").value("updated@station.com"))

            verify(stationService).updateStationContact(1L, contactRequest)
        }

        @Test
        @DisplayName("Should update station manager successfully")
        fun shouldUpdateStationManagerSuccessfully() {
            // Given
            val managerRequest = UpdateStationManagerRequest("New Manager")
            val updatedResponse = testStationResponse.copy(managerName = "New Manager")
            whenever(stationService.updateStationManager(1L, "New Manager")).thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                patch("/api/v1/stations/1/manager")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(managerRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.manager_name").value("New Manager"))

            verify(stationService).updateStationManager(1L, "New Manager")
        }
    }

    @Nested
    @DisplayName("Station Search Operations")
    inner class StationSearchOperations {

        @Test
        @DisplayName("Should search stations by criteria successfully")
        fun shouldSearchStationsByCriteriaSuccessfully() {
            // Given
            val searchRequest = StationSearchRequest(
                name = "Test",
                status = StationStatus.ACTIVE,
                is24Hours = false
            )
            val pageable = PageRequest.of(0, 10)
            val searchResults = PageImpl(listOf(testStationResponse), pageable, 1)
            whenever(stationService.searchStations(any<StationSearchRequest>(), any())).thenReturn(searchResults)

            // When & Then
            mockMvc.perform(
                post("/api/v1/stations/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest))
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].name").value(testStationResponse.name))

            verify(stationService).searchStations(any<StationSearchRequest>(), any())
        }

        @Test
        @DisplayName("Should search stations by location successfully")
        fun shouldSearchStationsByLocationSuccessfully() {
            // Given
            val locationRequest = LocationSearchRequest(
                latitude = BigDecimal("40.7128"),
                longitude = BigDecimal("-74.0060"),
                radiusKm = 5.0,
                status = StationStatus.ACTIVE
            )
            val stationWithDistance = StationWithDistanceResponse.fromStationAndDistance(testStation, 2.5)
            whenever(stationService.searchStationsByLocation(any<LocationSearchRequest>()))
                .thenReturn(listOf(stationWithDistance))

            // When & Then
            mockMvc.perform(
                post("/api/v1/stations/search/location")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(locationRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$[0].station.name").value(testStationResponse.name))
                .andExpect(jsonPath("$[0].distance_km").value(2.5))

            verify(stationService).searchStationsByLocation(any<LocationSearchRequest>())
        }

        @Test
        @DisplayName("Should find stations by name successfully")
        fun shouldFindStationsByNameSuccessfully() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val searchResults = PageImpl(listOf(testStationResponse), pageable, 1)
            whenever(stationService.findStationsByName(eq("Test"), any())).thenReturn(searchResults)

            // When & Then
            mockMvc.perform(
                get("/api/v1/stations/search/name")
                    .param("name", "Test")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].name").value(testStationResponse.name))

            verify(stationService).findStationsByName(eq("Test"), any())
        }

        @Test
        @DisplayName("Should find stations by status successfully")
        fun shouldFindStationsByStatusSuccessfully() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val searchResults = PageImpl(listOf(testStationResponse), pageable, 1)
            whenever(stationService.findStationsByStatus(eq(StationStatus.ACTIVE), any())).thenReturn(searchResults)

            // When & Then
            mockMvc.perform(
                get("/api/v1/stations/status/ACTIVE")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))

            verify(stationService).findStationsByStatus(eq(StationStatus.ACTIVE), any())
        }
    }

    @Nested
    @DisplayName("Station Utility Operations")
    inner class StationUtilityOperations {

        @Test
        @DisplayName("Should calculate distance between stations successfully")
        fun shouldCalculateDistanceBetweenStationsSuccessfully() {
            // Given
            whenever(stationService.calculateDistanceBetweenStations(1L, 2L)).thenReturn(5.5)

            // When & Then
            mockMvc.perform(get("/api/v1/stations/1/distance/2"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.station1_id").value(1))
                .andExpect(jsonPath("$.station2_id").value(2))
                .andExpect(jsonPath("$.distance_km").value(5.5))

            verify(stationService).calculateDistanceBetweenStations(1L, 2L)
        }

        @Test
        @DisplayName("Should calculate distance from coordinates successfully")
        fun shouldCalculateDistanceFromCoordinatesSuccessfully() {
            // Given
            val lat = BigDecimal("40.7580")
            val lng = BigDecimal("-73.9855")
            whenever(stationService.calculateDistanceFromCoordinates(1L, lat, lng)).thenReturn(3.2)

            // When & Then
            mockMvc.perform(
                get("/api/v1/stations/1/distance")
                    .param("latitude", lat.toString())
                    .param("longitude", lng.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.station_id").value(1))
                .andExpect(jsonPath("$.distance_km").value(3.2))

            verify(stationService).calculateDistanceFromCoordinates(1L, lat, lng)
        }

        @Test
        @DisplayName("Should get station statistics successfully")
        fun shouldGetStationStatisticsSuccessfully() {
            // Given
            val statistics = StationStatisticsResponse(
                totalStations = 100,
                activeStations = 85,
                inactiveStations = 10,
                maintenanceStations = 5,
                stations24Hours = 20,
                stationsWithStore = 30,
                stationsWithCarWash = 25
            )
            whenever(stationService.getStationStatistics()).thenReturn(statistics)

            // When & Then
            mockMvc.perform(get("/api/v1/stations/statistics"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.total_stations").value(100))
                .andExpect(jsonPath("$.active_stations").value(85))

            verify(stationService).getStationStatistics()
        }

        @Test
        @DisplayName("Should get health check successfully")
        fun shouldGetHealthCheckSuccessfully() {
            // When & Then
            mockMvc.perform(get("/api/v1/stations/health"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Station Service"))
        }
    }
}