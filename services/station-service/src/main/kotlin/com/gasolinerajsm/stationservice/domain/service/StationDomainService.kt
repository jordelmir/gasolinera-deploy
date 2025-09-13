package com.gasolinerajsm.stationservice.domain.service

import com.gasolinerajsm.stationservice.domain.model.*
import com.gasolinerajsm.stationservice.domain.valueobject.Location
import com.gasolinerajsm.stationservice.domain.valueobject.OperatingHours
import com.gasolinerajsm.stationservice.domain.valueobject.StationCapacity
import java.math.BigDecimal

/**
 * Station Domain Service
 * Contains complex business logic that doesn't belong to a single entity
 */
class StationDomainService {

    companion object {
        private const val MIN_DISTANCE_BETWEEN_STATIONS_KM = 0.5
        private const val MAX_FUEL_PRICE_CHANGE_PERCENT = 20.0
        private const val MIN_FUEL_PUMPS = 2
        private const val MAX_FUEL_PUMPS = 50
    }

    /**
     * Validate station creation data
     */
    fun validateStationCreation(
        name: String,
        address: String,
        location: Location,
        operatingHours: OperatingHours,
        capacity: StationCapacity
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate name
        if (name.isBlank() || name.length < 3) {
            errors.add("Station name must be at least 3 characters")
        }
        if (name.length > 200) {
            errors.add("Station name must not exceed 200 characters")
        }

        // Validate address
        if (address.isBlank() || address.length < 10) {
            errors.add("Station address must be at least 10 characters")
        }
        if (address.length > 500) {
            errors.add("Station address must not exceed 500 characters")
        }

        // Validate location
        val locationValidation = validateLocation(location)
        if (!locationValidation.isSuccess) {
            errors.add(locationValidation.message)
        }

        // Validate capacity
        val capacityValidation = validateStationCapacity(capacity)
        if (!capacityValidation.isSuccess) {
            errors.add(capacityValidation.message)
        }

        // Validate operating hours
        val hoursValidation = validateOperatingHours(operatingHours)
        if (!hoursValidation.isSuccess) {
            errors.add(hoursValidation.message)
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Station data is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Validate location for station placement
     */
    fun validateLocation(location: Location): ValidationResult {
        return try {
            // Check if location is in supported regions
            val supportedRegions = listOf(
                GeographicRegion.MEXICO,
                GeographicRegion.USA,
                GeographicRegion.NORTH_AMERICA
            )

            val isInSupportedRegion = supportedRegions.any { location.isInRegion(it) }

            if (!isInSupportedRegion) {
                ValidationResult.warning("Location may be outside supported service areas")
            } else {
                ValidationResult.success("Location is valid")
            }
        } catch (e: Exception) {
            ValidationResult.failure("Invalid location coordinates: ${e.message}")
        }
    }

    /**
     * Check if stations are too close to each other
     */
    fun areStationsTooClose(location1: Location, location2: Location): Boolean {
        return location1.distanceTo(location2) < MIN_DISTANCE_BETWEEN_STATIONS_KM
    }

    /**
     * Validate fuel price update
     */
    fun validateFuelPriceUpdate(
        currentPrice: BigDecimal?,
        newPrice: BigDecimal,
        fuelType: FuelType
    ): ValidationResult {
        // Basic price validation
        if (newPrice <= BigDecimal.ZERO) {
            return ValidationResult.failure("Fuel price must be greater than zero")
        }

        if (newPrice > BigDecimal("999.99")) {
            return ValidationResult.failure("Fuel price cannot exceed 999.99")
        }

        // Check for reasonable price changes
        currentPrice?.let { current ->
            val changePercent = ((newPrice - current) / current * BigDecimal("100")).abs()
            if (changePercent > BigDecimal(MAX_FUEL_PRICE_CHANGE_PERCENT)) {
                return ValidationResult.warning(
                    "Price change of ${changePercent.setScale(1)}% is unusually large"
                )
            }
        }

        // Validate price ranges by fuel type
        val priceRange = when (fuelType) {
            FuelType.REGULAR -> BigDecimal("0.50") to BigDecimal("10.00")
            FuelType.PREMIUM -> BigDecimal("0.60") to BigDecimal("12.00")
            FuelType.SUPER_PREMIUM -> BigDecimal("0.70") to BigDecimal("15.00")
            FuelType.DIESEL -> BigDecimal("0.55") to BigDecimal("12.00")
            FuelType.E85 -> BigDecimal("0.40") to BigDecimal("8.00")
            FuelType.ELECTRIC -> BigDecimal("0.10") to BigDecimal("1.00")
        }

        if (newPrice < priceRange.first || newPrice > priceRange.second) {
            return ValidationResult.warning(
                "Price ${newPrice} is outside typical range for ${fuelType.displayName}"
            )
        }

        return ValidationResult.success("Fuel price is valid")
    }

    /**
     * Validate station capacity
     */
    fun validateStationCapacity(capacity: StationCapacity): ValidationResult {
        val errors = mutableListOf<String>()

        if (capacity.fuelPumps < MIN_FUEL_PUMPS) {
            errors.add("Station must have at least $MIN_FUEL_PUMPS fuel pumps")
        }

        if (capacity.fuelPumps > MAX_FUEL_PUMPS) {
            errors.add("Station cannot have more than $MAX_FUEL_PUMPS fuel pumps")
        }

        if (capacity.electricChargingStations < 0) {
            errors.add("Electric charging stations count cannot be negative")
        }

        if (capacity.parkingSpaces < capacity.getTotalServicePoints()) {
            errors.add("Parking spaces should be at least equal to total service points")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Station capacity is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Validate operating hours
     */
    fun validateOperatingHours(operatingHours: OperatingHours): ValidationResult {
        if (operatingHours.is24Hours) {
            return ValidationResult.success("24-hour operation is valid")
        }

        val errors = mutableListOf<String>()

        // Check if at least one day is open
        val hasOpenDays = operatingHours.schedule.values.any { it.isOpen }
        if (!hasOpenDays) {
            errors.add("Station must be open at least one day per week")
        }

        // Validate each day's schedule
        operatingHours.schedule.forEach { (day, schedule) ->
            if (schedule.isOpen) {
                val operatingHours = schedule.getOperatingHours()
                if (operatingHours < 1.0) {
                    errors.add("$day: Operating hours must be at least 1 hour")
                }
                if (operatingHours > 24.0) {
                    errors.add("$day: Operating hours cannot exceed 24 hours")
                }
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Operating hours are valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Calculate competitive fuel price
     */
    fun calculateCompetitivePrice(
        currentPrice: BigDecimal,
        competitorPrices: List<BigDecimal>,
        strategy: PricingStrategy = PricingStrategy.MATCH_LOWEST
    ): BigDecimal {
        if (competitorPrices.isEmpty()) {
            return currentPrice
        }

        return when (strategy) {
            PricingStrategy.MATCH_LOWEST -> {
                val lowestPrice = competitorPrices.minOrNull() ?: currentPrice
                lowestPrice.coerceAtLeast(BigDecimal("0.50"))
            }
            PricingStrategy.UNDERCUT_BY_PERCENT -> {
                val lowestPrice = competitorPrices.minOrNull() ?: currentPrice
                val undercutPrice = lowestPrice * BigDecimal("0.98") // 2% undercut
                undercutPrice.coerceAtLeast(BigDecimal("0.50"))
            }
            PricingStrategy.MATCH_AVERAGE -> {
                val averagePrice = competitorPrices.fold(BigDecimal.ZERO) { acc, price ->
                    acc + price
                } / BigDecimal(competitorPrices.size)
                averagePrice.setScale(2, java.math.RoundingMode.HALF_UP)
            }
            PricingStrategy.PREMIUM_POSITIONING -> {
                val averagePrice = competitorPrices.fold(BigDecimal.ZERO) { acc, price ->
                    acc + price
                } / BigDecimal(competitorPrices.size)
                (averagePrice * BigDecimal("1.05")).setScale(2, java.math.RoundingMode.HALF_UP)
            }
        }
    }

    /**
     * Determine optimal station amenities based on location and competition
     */
    fun recommendAmenities(
        location: Location,
        nearbyStations: List<Station>,
        targetCustomerSegment: CustomerSegment
    ): Set<StationAmenity> {
        val recommendedAmenities = mutableSetOf<StationAmenity>()

        // Essential amenities for all stations
        recommendedAmenities.addAll(
            setOf(
                StationAmenity.RESTROOMS,
                StationAmenity.PARKING,
                StationAmenity.MOBILE_PAYMENT
            )
        )

        // Amenities based on customer segment
        when (targetCustomerSegment) {
            CustomerSegment.COMMUTERS -> {
                recommendedAmenities.addAll(
                    setOf(
                        StationAmenity.COFFEE_SHOP,
                        StationAmenity.CONVENIENCE_STORE,
                        StationAmenity.ATM,
                        StationAmenity.WIFI
                    )
                )
            }
            CustomerSegment.TRAVELERS -> {
                recommendedAmenities.addAll(
                    setOf(
                        StationAmenity.RESTAURANT,
                        StationAmenity.CONVENIENCE_STORE,
                        StationAmenity.ATM,
                        StationAmenity.WIFI,
                        StationAmenity.TRUCK_PARKING
                    )
                )
            }
            CustomerSegment.LOCAL_RESIDENTS -> {
                recommendedAmenities.addAll(
                    setOf(
                        StationAmenity.CONVENIENCE_STORE,
                        StationAmenity.CAR_WASH,
                        StationAmenity.LOYALTY_PROGRAM
                    )
                )
            }
            CustomerSegment.COMMERCIAL_FLEET -> {
                recommendedAmenities.addAll(
                    setOf(
                        StationAmenity.TRUCK_PARKING,
                        StationAmenity.DIESEL,
                        StationAmenity.FLEET_SERVICES
                    )
                )
            }
        }

        // Add competitive amenities if nearby stations have them
        val nearbyAmenities = nearbyStations.flatMap { it.amenities }.toSet()
        if (nearbyAmenities.contains(StationAmenity.ELECTRIC_CHARGING)) {
            recommendedAmenities.add(StationAmenity.ELECTRIC_CHARGING)
        }

        return recommendedAmenities
    }
}

/**
 * Validation result for domain operations
 */
data class ValidationResult(
    val isSuccess: Boolean,
    val isWarning: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = ValidationResult(true, false, message)
        fun warning(message: String) = ValidationResult(true, true, message)
        fun failure(message: String) = ValidationResult(false, false, message)
    }
}

/**
 * Pricing strategies for fuel price calculation
 */
enum class PricingStrategy {
    MATCH_LOWEST,
    UNDERCUT_BY_PERCENT,
    MATCH_AVERAGE,
    PREMIUM_POSITIONING
}

/**
 * Customer segments for amenity recommendations
 */
enum class CustomerSegment {
    COMMUTERS,
    TRAVELERS,
    LOCAL_RESIDENTS,
    COMMERCIAL_FLEET
}