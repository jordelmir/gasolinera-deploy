package com.gasolinerajsm.station.domain

import com.gasolinerajsm.testing.shared.TestDataFactory
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Unit Tests for Station Domain Entity
 * Tests business logic, validation, and domain rules
 */
@DisplayName("Station Domain Entity Tests")
class StationTest {

    @Nested
    @DisplayName("Creation Tests")
    inner class CreationTests {

        @Test
        @DisplayName("Should create valid Station with all required fields")
        fun shouldCreateValidStation() {
            // Given
            val id = UUID.randomUUID()
            val name = "Gasolinera Central"
            val address = "Av. Reforma 123, CDMX"
            val location = Location(19.4326, -99.1332) // Mexico City coordinates
            val brandId = UUID.randomUUID()

            // When
            val station = Station.create(
                id = id,
                name = name,
                address = address,
                location = location,
                brandId = brandId
            )

            // Then
            assertThat(station.id).isEqualTo(id)
            assertThat(station.name).isEqualTo(name)
            assertThat(station.address).isEqualTo(address)
            assertThat(station.location).isEqualTo(location)
            assertThat(station.brandId).isEqualTo(brandId)
            assertThat(station.isActive).isTrue()
            assertThat(station.isOperational).isTrue()
            assertThat(station.createdAt).isBeforeOrEqualTo(LocalDateTime.now())
            assertThat(station.updatedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        @DisplayName("Should throw exception when creating Station with empty name")
        fun shouldThrowExceptionWithEmptyName(emptyName: String) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Station.create(
                    id = UUID.randomUUID(),
                    name = emptyName,
                    address = "Av. Reforma 123, CDMX",
                    location = Location(19.4326, -99.1332),
                    brandId = UUID.randomUUID()
                )
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        @DisplayName("Should throw exception when creating Station with empty address")
        fun shouldThrowExceptionWithEmptyAddress(emptyAddress: String) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Station.create(
                    id = UUID.randomUUID(),
                    name = "Gasolinera Central",
                    address = emptyAddress,
                    location = Location(19.4326, -99.1332),
                    brandId = UUID.randomUUID()
                )
            }
        }

        @Test
        @DisplayName("Should throw exception when creating Station with invalid location")
        fun shouldThrowExceptionWithInvalidLocation() {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Station.create(
                    id = UUID.randomUUID(),
                    name = "Gasolinera Central",
                    address = "Av. Reforma 123, CDMX",
                    location = Location(200.0, -200.0), // Invalid coordinates
                    brandId = UUID.randomUUID()
                )
            }
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    inner class BusinessLogicTests {

        private fun createValidStation(): Station {
            return Station.create(
                id = UUID.randomUUID(),
                name = "Gasolinera Central",
                address = "Av. Reforma 123, CDMX",
                location = Location(19.4326, -99.1332),
                brandId = UUID.randomUUID()
            )
        }

        @Test
        @DisplayName("Should deactivate station successfully")
        fun shouldDeactivateStationSuccessfully() {
            // Given
            val station = createValidStation()
            assertThat(station.isActive).isTrue()

            // When
            station.deactivate()

            // Then
            assertThat(station.isActive).isFalse()
            assertThat(station.deactivatedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should reactivate station successfully")
        fun shouldReactivateStationSuccessfully() {
            // Given
            val station = createValidStation()
            station.deactivate()
            assertThat(station.isActive).isFalse()

            // When
            station.reactivate()

            // Then
            assertThat(station.isActive).isTrue()
            assertThat(station.deactivatedAt).isNull()
        }

        @Test
        @DisplayName("Should set station as non-operational")
        fun shouldSetStationAsNonOperational() {
            // Given
            val station = createValidStation()
            assertThat(station.isOperational).isTrue()

            // When
            station.setOperational(false)

            // Then
            assertThat(station.isOperational).isFalse()
            assertThat(station.operationalStatusChangedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should update station information successfully")
        fun shouldUpdateStationInformationSuccessfully() {
            // Given
            val station = createValidStation()
            val newName = "Gasolinera Norte"
            val newAddress = "Av. Insurgentes 456, CDMX"
            val newLocation = Location(19.5000, -99.2000)

            // When
            station.updateInformation(newName, newAddress, newLocation)

            // Then
            assertThat(station.name).isEqualTo(newName)
            assertThat(station.address).isEqualTo(newAddress)
            assertThat(station.location).isEqualTo(newLocation)
            assertThat(station.updatedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should add fuel price successfully")
        fun shouldAddFuelPriceSuccessfully() {
            // Given
            val station = createValidStation()
            val fuelType = FuelType.REGULAR
            val price = BigDecimal("22.50")

            // When
            station.updateFuelPrice(fuelType, price)

            // Then
            assertThat(station.getFuelPrice(fuelType)).isEqualTo(price)
            assertThat(station.fuelPrices).containsKey(fuelType)
        }

        @Test
        @DisplayName("Should update existing fuel price successfully")
        fun shouldUpdateExistingFuelPriceSuccessfully() {
            // Given
            val station = createValidStation()
            val fuelType = FuelType.PREMIUM
            val oldPrice = BigDecimal("24.00")
            val newPrice = BigDecimal("24.50")

            station.updateFuelPrice(fuelType, oldPrice)
            assertThat(station.getFuelPrice(fuelType)).isEqualTo(oldPrice)

            // When
            station.updateFuelPrice(fuelType, newPrice)

            // Then
            assertThat(station.getFuelPrice(fuelType)).isEqualTo(newPrice)
        }

        @Test
        @DisplayName("Should throw exception when updating fuel price with negative value")
        fun shouldThrowExceptionWithNegativeFuelPrice() {
            // Given
            val station = createValidStation()
            val fuelType = FuelType.DIESEL
            val negativePrice = BigDecimal("-1.00")

            // When & Then
            assertThrows<IllegalArgumentException> {
                station.updateFuelPrice(fuelType, negativePrice)
            }
        }

        @Test
        @DisplayName("Should calculate distance to another location")
        fun shouldCalculateDistanceToAnotherLocation() {
            // Given
            val station = createValidStation() // Mexico City
            val otherLocation = Location(19.4978, -99.1269) // Zócalo, Mexico City

            // When
            val distance = station.distanceTo(otherLocation)

            // Then
            assertThat(distance).isGreaterThan(0.0)
            assertThat(distance).isLessThan(10.0) // Should be less than 10km
        }

        @Test
        @DisplayName("Should check if station is within radius")
        fun shouldCheckIfStationIsWithinRadius() {
            // Given
            val station = createValidStation() // Mexico City
            val centerLocation = Location(19.4978, -99.1269) // Zócalo, Mexico City
            val radiusKm = 10.0

            // When
            val isWithinRadius = station.isWithinRadius(centerLocation, radiusKm)

            // Then
            assertThat(isWithinRadius).isTrue()
        }

        @Test
        @DisplayName("Should check if station is outside radius")
        fun shouldCheckIfStationIsOutsideRadius() {
            // Given
            val station = createValidStation() // Mexico City
            val farLocation = Location(25.6866, -100.3161) // Monterrey
            val radiusKm = 10.0

            // When
            val isWithinRadius = station.isWithinRadius(farLocation, radiusKm)

            // Then
            assertThat(isWithinRadius).isFalse()
        }
    }

    @Nested
    @DisplayName("Value Objects Tests")
    inner class ValueObjectsTests {

        @Test
        @DisplayName("Should create valid Location value object")
        fun shouldCreateValidLocation() {
            // Given
            val latitude = 19.4326
            val longitude = -99.1332

            // When
            val location = Location(latitude, longitude)

            // Then
            assertThat(location.latitude).isEqualTo(latitude)
            assertThat(location.longitude).isEqualTo(longitude)
        }

        @ParameterizedTest
        @ValueSource(doubles = [-91.0, 91.0, 200.0, -200.0])
        @DisplayName("Should throw exception with invalid latitude")
        fun shouldThrowExceptionWithInvalidLatitude(invalidLatitude: Double) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Location(invalidLatitude, -99.1332)
            }
        }

        @ParameterizedTest
        @ValueSource(doubles = [-181.0, 181.0, 200.0, -200.0])
        @DisplayName("Should throw exception with invalid longitude")
        fun shouldThrowExceptionWithInvalidLongitude(invalidLongitude: Double) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Location(19.4326, invalidLongitude)
            }
        }

        @Test
        @DisplayName("Should calculate distance between two locations")
        fun shouldCalculateDistanceBetweenTwoLocations() {
            // Given
            val location1 = Location(19.4326, -99.1332) // Mexico City
            val location2 = Location(19.4978, -99.1269) // Zócalo, Mexico City

            // When
            val distance = location1.distanceTo(location2)

            // Then
            assertThat(distance).isGreaterThan(0.0)
            assertThat(distance).isLessThan(10.0) // Should be less than 10km
        }

        @Test
        @DisplayName("Should return zero distance for same location")
        fun shouldReturnZeroDistanceForSameLocation() {
            // Given
            val location = Location(19.4326, -99.1332)

            // When
            val distance = location.distanceTo(location)

            // Then
            assertThat(distance).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("Fuel Type Tests")
    inner class FuelTypeTests {

        @Test
        @DisplayName("Should have all expected fuel types")
        fun shouldHaveAllExpectedFuelTypes() {
            // When & Then
            assertThat(FuelType.values()).containsExactlyInAnyOrder(
                FuelType.REGULAR,
                FuelType.PREMIUM,
                FuelType.DIESEL
            )
        }

        @Test
        @DisplayName("Should get fuel type display name")
        fun shouldGetFuelTypeDisplayName() {
            // When & Then
            assertThat(FuelType.REGULAR.displayName).isEqualTo("Regular")
            assertThat(FuelType.PREMIUM.displayName).isEqualTo("Premium")
            assertThat(FuelType.DIESEL.displayName).isEqualTo("Diesel")
        }

        @Test
        @DisplayName("Should get fuel type from string")
        fun shouldGetFuelTypeFromString() {
            // When & Then
            assertThat(FuelType.fromString("REGULAR")).isEqualTo(FuelType.REGULAR)
            assertThat(FuelType.fromString("regular")).isEqualTo(FuelType.REGULAR)
            assertThat(FuelType.fromString("Regular")).isEqualTo(FuelType.REGULAR)
        }

        @Test
        @DisplayName("Should throw exception for invalid fuel type string")
        fun shouldThrowExceptionForInvalidFuelTypeString() {
            // When & Then
            assertThrows<IllegalArgumentException> {
                FuelType.fromString("INVALID")
            }
        }
    }

    @Nested
    @DisplayName("Domain Events Tests")
    inner class DomainEventsTests {

        @Test
        @DisplayName("Should publish StationCreated event when station is created")
        fun shouldPublishStationCreatedEvent() {
            // Given
            val id = UUID.randomUUID()

            // When
            val station = Station.create(
                id = id,
                name = "Gasolinera Central",
                address = "Av. Reforma 123, CDMX",
                location = Location(19.4326, -99.1332),
                brandId = UUID.randomUUID()
            )

            // Then
            val events = station.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(StationCreatedEvent::class.java)

            val event = events.first() as StationCreatedEvent
            assertThat(event.stationId).isEqualTo(id)
            assertThat(event.name).isEqualTo("Gasolinera Central")
        }

        @Test
        @DisplayName("Should publish StationDeactivated event when station is deactivated")
        fun shouldPublishStationDeactivatedEvent() {
            // Given
            val station = Station.create(
                id = UUID.randomUUID(),
                name = "Gasolinera Central",
                address = "Av. Reforma 123, CDMX",
                location = Location(19.4326, -99.1332),
                brandId = UUID.randomUUID()
            )
            station.clearDomainEvents() // Clear creation event

            // When
            station.deactivate()

            // Then
            val events = station.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(StationDeactivatedEvent::class.java)
        }

        @Test
        @DisplayName("Should publish FuelPriceUpdated event when fuel price is updated")
        fun shouldPublishFuelPriceUpdatedEvent() {
            // Given
            val station = Station.create(
                id = UUID.randomUUID(),
                name = "Gasolinera Central",
                address = "Av. Reforma 123, CDMX",
                location = Location(19.4326, -99.1332),
                brandId = UUID.randomUUID()
            )
            station.clearDomainEvents() // Clear creation event

            // When
            station.updateFuelPrice(FuelType.REGULAR, BigDecimal("22.50"))

            // Then
            val events = station.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(FuelPriceUpdatedEvent::class.java)

            val event = events.first() as FuelPriceUpdatedEvent
            assertThat(event.stationId).isEqualTo(station.id)
            assertThat(event.fuelType).isEqualTo(FuelType.REGULAR)
            assertThat(event.newPrice).isEqualTo(BigDecimal("22.50"))
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    inner class EqualityTests {

        @Test
        @DisplayName("Should be equal when same ID")
        fun shouldBeEqualWhenSameId() {
            // Given
            val id = UUID.randomUUID()
            val station1 = Station.create(
                id = id,
                name = "Station 1",
                address = "Address 1",
                location = Location(19.4326, -99.1332),
                brandId = UUID.randomUUID()
            )
            val station2 = Station.create(
                id = id,
                name = "Station 2",
                address = "Address 2",
                location = Location(20.0000, -100.0000),
                brandId = UUID.randomUUID()
            )

            // When & Then
            assertThat(station1).isEqualTo(station2)
            assertThat(station1.hashCode()).isEqualTo(station2.hashCode())
        }

        @Test
        @DisplayName("Should not be equal when different ID")
        fun shouldNotBeEqualWhenDifferentId() {
            // Given
            val station1 = Station.create(
                id = UUID.randomUUID(),
                name = "Station",
                address = "Address",
                location = Location(19.4326, -99.1332),
                brandId = UUID.randomUUID()
            )
            val station2 = Station.create(
                id = UUID.randomUUID(),
                name = "Station",
                address = "Address",
                location = Location(19.4326, -99.1332),
                brandId = UUID.randomUUID()
            )

            // When & Then
            assertThat(station1).isNotEqualTo(station2)
            assertThat(station1.hashCode()).isNotEqualTo(station2.hashCode())
        }
    }
}