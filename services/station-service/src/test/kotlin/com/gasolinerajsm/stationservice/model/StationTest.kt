package com.gasolinerajsm.stationservice.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("Station Entity Tests")
class StationTest {

    @Nested
    @DisplayName("Station Creation Tests")
    inner class StationCreationTests {

        @Test
        @DisplayName("Should create station with valid data")
        fun shouldCreateStationWithValidData() {
            // Given
            val name = "Shell Station Downtown"
            val address = "123 Main Street, Downtown, City"
            val latitude = BigDecimal("40.7128")
            val longitude = BigDecimal("-74.0060")
            val phoneNumber = "+1234567890"

            // When
            val station = Station(
                name = name,
                address = address,
                latitude = latitude,
                longitude = longitude,
                phoneNumber = phoneNumber
            )

            // Then
            assertEquals(name, station.name)
            assertEquals(address, station.address)
            assertEquals(latitude, station.latitude)
            assertEquals(longitude, station.longitude)
            assertEquals(phoneNumber, station.phoneNumber)
            assertEquals(StationStatus.ACTIVE, station.status)
            assertFalse(station.is24Hours)
            assertFalse(station.hasConvenienceStore)
            assertFalse(station.hasCarWash)
        }

        @Test
        @DisplayName("Should create station with all optional fields")
        fun shouldCreateStationWithAllOptionalFields() {
            // When
            val station = Station(
                name = "Premium Gas Station",
                address = "456 Oak Avenue, Suburb, City",
                latitude = BigDecimal("40.7589"),
                longitude = BigDecimal("-73.9851"),
                phoneNumber = "+1987654321",
                email = "manager@premiumgas.com",
                status = StationStatus.ACTIVE,
                managerName = "John Manager",
                operatingHours = "6:00 AM - 10:00 PM",
                servicesOffered = "Fuel, Car Wash, Convenience Store",
                is24Hours = false,
                hasConvenienceStore = true,
                hasCarWash = true,
                fuelTypes = "Regular, Premium, Diesel"
            )

            // Then
            assertEquals("manager@premiumgas.com", station.email)
            assertEquals("John Manager", station.managerName)
            assertEquals("6:00 AM - 10:00 PM", station.operatingHours)
            assertTrue(station.hasConvenienceStore)
            assertTrue(station.hasCarWash)
            assertEquals("Regular, Premium, Diesel", station.fuelTypes)
        }
    }

    @Nested
    @DisplayName("Station Business Logic Tests")
    inner class StationBusinessLogicTests {

        private val testStation = Station(
            id = 1L,
            name = "Test Station",
            address = "123 Test Street",
            latitude = BigDecimal("40.7128"),
            longitude = BigDecimal("-74.0060"),
            phoneNumber = "+1234567890",
            status = StationStatus.ACTIVE,
            servicesOffered = "Fuel, Car Wash, Convenience Store",
            fuelTypes = "Regular, Premium, Diesel"
        )

        @Test
        @DisplayName("Should detect operational station")
        fun shouldDetectOperationalStation() {
            // When & Then
            assertTrue(testStation.isOperational())
            assertFalse(testStation.isUnderMaintenance())
        }

        @Test
        @DisplayName("Should detect station under maintenance")
        fun shouldDetectStationUnderMaintenance() {
            // Given
            val maintenanceStation = testStation.copy(status = StationStatus.MAINTENANCE)

            // When & Then
            assertFalse(maintenanceStation.isOperational())
            assertTrue(maintenanceStation.isUnderMaintenance())
        }

        @Test
        @DisplayName("Should calculate distance correctly")
        fun shouldCalculateDistanceCorrectly() {
            // Given - Coordinates for Times Square, NYC
            val timesSquareLat = BigDecimal("40.7580")
            val timesSquareLng = BigDecimal("-73.9855")

            // When
            val distance = testStation.getDistanceFrom(timesSquareLat, timesSquareLng)

            // Then
            assertTrue(distance > 0, "Distance should be positive")
            assertTrue(distance < 50, "Distance should be reasonable for NYC area")
        }

        @Test
        @DisplayName("Should return zero distance for same coordinates")
        fun shouldReturnZeroDistanceForSameCoordinates() {
            // When
            val distance = testStation.getDistanceFrom(testStation.latitude, testStation.longitude)

            // Then
            assertTrue(distance < 0.001, "Distance should be essentially zero")
        }

        @Test
        @DisplayName("Should format address correctly")
        fun shouldFormatAddressCorrectly() {
            // Given
            val stationWithSpaces = testStation.copy(address = "  123 Test Street  ")

            // When
            val formattedAddress = stationWithSpaces.getFormattedAddress()

            // Then
            assertEquals("123 Test Street", formattedAddress)
        }

        @Test
        @DisplayName("Should return coordinates as pair")
        fun shouldReturnCoordinatesAsPair() {
            // When
            val coordinates = testStation.getCoordinates()

            // Then
            assertEquals(testStation.latitude, coordinates.first)
            assertEquals(testStation.longitude, coordinates.second)
        }

        @Test
        @DisplayName("Should detect services correctly")
        fun shouldDetectServicesCorrectly() {
            // When & Then
            assertTrue(testStation.hasService("Car Wash"))
            assertTrue(testStation.hasService("car wash")) // Case insensitive
            assertTrue(testStation.hasService("Fuel"))
            assertFalse(testStation.hasService("Oil Change"))
        }

        @Test
        @DisplayName("Should parse fuel types correctly")
        fun shouldParseFuelTypesCorrectly() {
            // When
            val fuelTypesList = testStation.getFuelTypesList()

            // Then
            assertEquals(3, fuelTypesList.size)
            assertTrue(fuelTypesList.contains("Regular"))
            assertTrue(fuelTypesList.contains("Premium"))
            assertTrue(fuelTypesList.contains("Diesel"))
        }

        @Test
        @DisplayName("Should return empty list for null fuel types")
        fun shouldReturnEmptyListForNullFuelTypes() {
            // Given
            val stationWithoutFuelTypes = testStation.copy(fuelTypes = null)

            // When
            val fuelTypesList = stationWithoutFuelTypes.getFuelTypesList()

            // Then
            assertTrue(fuelTypesList.isEmpty())
        }
    }

    @Nested
    @DisplayName("Station State Management Tests")
    inner class StationStateManagementTests {

        private val testStation = Station(
            id = 1L,
            name = "Test Station",
            address = "123 Test Street",
            latitude = BigDecimal("40.7128"),
            longitude = BigDecimal("-74.0060"),
            phoneNumber = "+1234567890",
            status = StationStatus.ACTIVE
        )

        @Test
        @DisplayName("Should activate station")
        fun shouldActivateStation() {
            // Given
            val inactiveStation = testStation.copy(status = StationStatus.INACTIVE)

            // When
            val activatedStation = inactiveStation.activate()

            // Then
            assertEquals(StationStatus.ACTIVE, activatedStation.status)
            assertTrue(activatedStation.isOperational())
        }

        @Test
        @DisplayName("Should deactivate station")
        fun shouldDeactivateStation() {
            // When
            val deactivatedStation = testStation.deactivate()

            // Then
            assertEquals(StationStatus.INACTIVE, deactivatedStation.status)
            assertFalse(deactivatedStation.isOperational())
        }

        @Test
        @DisplayName("Should put station under maintenance")
        fun shouldPutStationUnderMaintenance() {
            // When
            val maintenanceStation = testStation.putUnderMaintenance()

            // Then
            assertEquals(StationStatus.MAINTENANCE, maintenanceStation.status)
            assertTrue(maintenanceStation.isUnderMaintenance())
            assertFalse(maintenanceStation.isOperational())
        }
    }

    @Nested
    @DisplayName("Station Status Tests")
    inner class StationStatusTests {

        @Test
        @DisplayName("Should validate status operations permissions")
        fun shouldValidateStatusOperationsPermissions() {
            // Test ACTIVE status
            assertTrue(StationStatus.ACTIVE.allowsCustomerOperations())
            assertFalse(StationStatus.ACTIVE.isTemporary())

            // Test INACTIVE status
            assertFalse(StationStatus.INACTIVE.allowsCustomerOperations())
            assertTrue(StationStatus.INACTIVE.isTemporary())

            // Test MAINTENANCE status
            assertFalse(StationStatus.MAINTENANCE.allowsCustomerOperations())
            assertTrue(StationStatus.MAINTENANCE.isTemporary())

            // Test PERMANENTLY_CLOSED status
            assertFalse(StationStatus.PERMANENTLY_CLOSED.allowsCustomerOperations())
            assertFalse(StationStatus.PERMANENTLY_CLOSED.isTemporary())
        }

        @Test
        @DisplayName("Should have correct display names")
        fun shouldHaveCorrectDisplayNames() {
            assertEquals("Active", StationStatus.ACTIVE.displayName)
            assertEquals("Inactive", StationStatus.INACTIVE.displayName)
            assertEquals("Under Maintenance", StationStatus.MAINTENANCE.displayName)
            assertEquals("Permanently Closed", StationStatus.PERMANENTLY_CLOSED.displayName)
        }
    }

    @Nested
    @DisplayName("Station Employee Relationship Tests")
    inner class StationEmployeeRelationshipTests {

        @Test
        @DisplayName("Should get active employee count")
        fun shouldGetActiveEmployeeCount() {
            // Given
            val station = Station(
                name = "Test Station",
                address = "123 Test Street",
                latitude = BigDecimal("40.7128"),
                longitude = BigDecimal("-74.0060"),
                phoneNumber = "+1234567890"
            )

            // When
            val activeCount = station.getActiveEmployeeCount()

            // Then
            assertEquals(0, activeCount) // Empty list by default
        }
    }

    @Nested
    @DisplayName("Station Validation Tests")
    inner class StationValidationTests {

        @Test
        @DisplayName("Should validate toString method")
        fun shouldValidateToStringMethod() {
            // Given
            val station = Station(
                id = 1L,
                name = "Test Station",
                address = "123 Test Street",
                latitude = BigDecimal("40.7128"),
                longitude = BigDecimal("-74.0060"),
                phoneNumber = "+1234567890",
                status = StationStatus.ACTIVE
            )

            // When
            val stringRepresentation = station.toString()

            // Then
            assertTrue(stringRepresentation.contains("Test Station"))
            assertTrue(stringRepresentation.contains("123 Test Street"))
            assertTrue(stringRepresentation.contains("ACTIVE"))
            assertTrue(stringRepresentation.contains("+1234567890"))
        }
    }
}