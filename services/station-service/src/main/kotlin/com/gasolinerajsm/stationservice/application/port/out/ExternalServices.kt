package com.gasolinerajsm.stationservice.application.port.out

import com.gasolinerajsm.stationservice.domain.event.DomainEvent
import com.gasolinerajsm.stationservice.domain.model.FuelType
import com.gasolinerajsm.stationservice.domain.valueobject.Location
import com.gasolinerajsm.stationservice.domain.valueobject.StationId
import java.math.BigDecimal

/**
 * Port for publishing domain events
 */
interface EventPublisher {
    suspend fun publish(event: DomainEvent): Result<Unit>
    suspend fun publishAll(events: List<DomainEvent>): Result<Unit>
}

/**
 * Port for notification services
 */
interface NotificationService {
    suspend fun notifyStationStatusChange(
        stationId: StationId,
        stationName: String,
        oldStatus: String,
        newStatus: String,
        managerId: String?
    ): Result<Unit>

    suspend fun notifyFuelPriceChange(
        stationId: StationId,
        stationName: String,
        fuelType: FuelType,
        oldPrice: BigDecimal?,
        newPrice: BigDecimal,
        managerId: String?
    ): Result<Unit>
}

/**
 * Port for geocoding services
 */
interface GeocodingService {
    suspend fun geocodeAddress(address: String): Result<Location>
    suspend fun reverseGeocode(location: Location): Result<String>
    suspend fun validateAddress(address: String): Result<Boolean>
}

/**
 * Port for competitive pricing services
 */
interface CompetitivePricingService {
    suspend fun getNearbyStationPrices(
        location: Location,
        radiusKm: Double,
        fuelType: FuelType
    ): Result<List<BigDecimal>>

    suspend fun getMarketAveragePrice(
        location: Location,
        fuelType: FuelType
    ): Result<BigDecimal>
}