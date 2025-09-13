package com.gasolinerajsm.station.web.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Data Transfer Objects for Station Service API
 * Comprehensive DTOs with OpenAPI documentation and validation
 */

// Request DTOs
@Schema(
    name = "StationSearchRequest",
    description = "Station search criteria",
    example = """
    {
      "latitude": 19.4326,
      "longitude": -99.1332,
      "radiusKm": 10.0,
      "fuelType": "REGULAR",
      "sortBy": "DISTANCE",
      "limit": 20
    }
    """
)
data class StationSearchRequest(
    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(
        description = "User's current latitude for distance calculation",
        example = "19.4326",
        minimum = "-90.0",
        maximum = "90.0"
    )
    val latitude: BigDecimal,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(
        description = "User's current longitude for distance calculation",
        example = "-99.1332",
        minimum = "-180.0",
        maximum = "180.0"
    )
    val longitude: BigDecimal,

    @field:DecimalMin(value = "0.1", message = "Radius must be at least 0.1 km")
    @field:DecimalMax(value = "100.0", message = "Radius cannot exceed 100 km")
    @Schema(
        description = "Search radius in kilometers",
        example = "10.0",
        minimum = "0.1",
        maximum = "100.0",
        defaultValue = "5.0"
    )
    val radiusKm: BigDecimal = BigDecimal("5.0"),

    @Schema(
        description = "Filter by fuel type",
        example = "REGULAR",
        allowableValues = ["REGULAR", "PREMIUM", "DIESEL", "ALL"]
    )
    val fuelType: String? = null,

    @Schema(
        description = "Sort results by criteria",
        example = "DISTANCE",
        allowableValues = ["DISTANCE", "PRICE", "NAME", "RATING"],
        defaultValue = "DISTANCE"
    )
    val sortBy: String = "DISTANCE",

    @field:Min(value = 1, message = "Limit must be at least 1")
    @field:Max(value = 100, message = "Limit cannot exceed 100")
    @Schema(
        description = "Maximum number of results to return",
        example = "20",
        minimum = "1",
        maximum = "100",
        defaultValue = "20"
    )
    val limit: Int = 20
)

@Schema(
    name = "StationCreateRequest",
    description = "Station creation data (Admin only)",
    example = """
    {
      "name": "Gasolinera JSM Centro",
      "address": "Av. Reforma 123, Centro, CDMX",
      "latitude": 19.4326,
      "longitude": -99.1332,
      "phone": "5551234567",
      "operatingHours": "24/7",
      "services": ["FULL_SERVICE", "CONVENIENCE_STORE", "CAR_WASH"],
      "fuelPrices": {
        "REGULAR": 22.50,
        "PREMIUM": 24.80,
        "DIESEL": 23.20
      }
    }
    """
)
data class StationCreateRequest(
    @field:NotBlank(message = "Station name is required")
    @field:Size(min = 3, max = 100, message = "Station name must be between 3 and 100 characters")
    @Schema(
        description = "Station name",
        example = "Gasolinera JSM Centro",
        minLength = 3,
        maxLength = 100
    )
    val name: String,

    @field:NotBlank(message = "Address is required")
    @field:Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(
        description = "Station physical address",
        example = "Av. Reforma 123, Centro, CDMX",
        maxLength = 255
    )
    val address: String,

    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(
        description = "Station latitude coordinate",
        example = "19.4326",
        minimum = "-90.0",
        maximum = "90.0"
    )
    val latitude: BigDecimal,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(
        description = "Station longitude coordinate",
        example = "-99.1332",
        minimum = "-180.0",
        maximum = "180.0"
    )
    val longitude: BigDecimal,

    @field:Pattern(
        regexp = "^\\\\d{10,12}$",
        message = "Phone number must be 10-12 digits"
    )
    @Schema(
        description = "Station contact phone number",
        example = "5551234567",
        pattern = "^\\\\d{10,12}$"
    )
    val phone: String?,

    @field:Size(max = 100, message = "Operating hours must not exceed 100 characters")
    @Schema(
        description = "Station operating hours",
        example = "24/7",
        maxLength = 100
    )
    val operatingHours: String?,

    @Schema(
        description = "Available services at the station",
        example = "[\"FULL_SERVICE\", \"CONVENIENCE_STORE\", \"CAR_WASH\"]"
    )
    val services: List<String> = emptyList(),

    @Schema(
        description = "Current fuel prices by type",
        example = """{"REGULAR": 22.50, "PREMIUM": 24.80, "DIESEL": 23.20}"""
    )
    val fuelPrices: Map<String, BigDecimal> = emptyMap()
)

@Schema(
    name = "StationUpdateRequest",
    description = "Station update data (Admin only)",
    example = """
    {
      "name": "Gasolinera JSM Centro Renovada",
      "phone": "5559876543",
      "operatingHours": "6:00 AM - 11:00 PM",
      "services": ["FULL_SERVICE", "CONVENIENCE_STORE", "CAR_WASH", "ATM"],
      "fuelPrices": {
        "REGULAR": 22.75,
        "PREMIUM": 25.00,
        "DIESEL": 23.45
      }
    }
    """
)
data class StationUpdateRequest(
    @field:Size(min = 3, max = 100, message = "Station name must be between 3 and 100 characters")
    @Schema(
        description = "Updated station name",
        example = "Gasolinera JSM Centro Renovada",
        minLength = 3,
        maxLength = 100
    )
    val name: String?,

    @field:Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(
        description = "Updated station address",
        example = "Av. Reforma 123, Centro, CDMX",
        maxLength = 255
    )
    val address: String?,

    @field:Pattern(
        regexp = "^\\\\d{10,12}$",
        message = "Phone number must be 10-12 digits"
    )
    @Schema(
        description = "Updated contact phone number",
        example = "5559876543",
        pattern = "^\\\\d{10,12}$"
    )
    val phone: String?,

    @field:Size(max = 100, message = "Operating hours must not exceed 100 characters")
    @Schema(
        description = "Updated operating hours",
        example = "6:00 AM - 11:00 PM",
        maxLength = 100
    )
    val operatingHours: String?,

    @Schema(
        description = "Updated available services",
        example = "[\"FULL_SERVICE\", \"CONVENIENCE_STORE\", \"CAR_WASH\", \"ATM\"]"
    )
    val services: List<String>?,

    @Schema(
        description = "Updated fuel prices by type",
        example = """{"REGULAR": 22.75, "PREMIUM": 25.00, "DIESEL": 23.45}"""
    )
    val fuelPrices: Map<String, BigDecimal>?,

    @Schema(
        description = "Whether station is currently active",
        example = "true"
    )
    val isActive: Boolean?
)

// Response DTOs
@Schema(
    name = "StationResponse",
    description = "Station information",
    example = """
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Gasolinera JSM Centro",
      "address": "Av. Reforma 123, Centro, CDMX",
      "latitude": 19.4326,
      "longitude": -99.1332,
      "phone": "5551234567",
      "operatingHours": "24/7",
      "services": ["FULL_SERVICE", "CONVENIENCE_STORE", "CAR_WASH"],
      "fuelPrices": {
        "REGULAR": 22.50,
        "PREMIUM": 24.80,
        "DIESEL": 23.20
      },
      "rating": 4.5,
      "reviewCount": 128,
      "isActive": true,
      "distance": 2.3,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-15T09:30:00"
    }
    """
)
data class StationResponse(
    @Schema(
        description = "Unique station identifier",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    val id: UUID,

    @Schema(
        description = "Station name",
        example = "Gasolinera JSM Centro"
    )
    val name: String,

    @Schema(
        description = "Station physical address",
        example = "Av. Reforma 123, Centro, CDMX"
    )
    val address: String,

    @Schema(
        description = "Station latitude coordinate",
        example = "19.4326"
    )
    val latitude: BigDecimal,

    @Schema(
        description = "Station longitude coordinate",
        example = "-99.1332"
    )
    val longitude: BigDecimal,

    @Schema(
        description = "Station contact phone number",
        example = "5551234567"
    )
    val phone: String?,

    @Schema(
        description = "Station operating hours",
        example = "24/7"
    )
    val operatingHours: String?,

    @Schema(
        description = "Available services at the station",
        example = "[\"FULL_SERVICE\", \"CONVENIENCE_STORE\", \"CAR_WASH\"]"
    )
    val services: List<String>,

    @Schema(
        description = "Current fuel prices by type",
        example = """{"REGULAR": 22.50, "PREMIUM": 24.80, "DIESEL": 23.20}"""
    )
    val fuelPrices: Map<String, BigDecimal>,

    @Schema(
        description = "Average user rating (1-5 stars)",
        example = "4.5"
    )
    val rating: BigDecimal?,

    @Schema(
        description = "Total number of reviews",
        example = "128"
    )
    val reviewCount: Int,

    @Schema(
        description = "Whether station is currently active",
        example = "true"
    )
    val isActive: Boolean,

    @Schema(
        description = "Distance from search point in kilometers (only in search results)",
        example = "2.3"
    )
    val distance: BigDecimal?,

    @Schema(
        description = "Station creation timestamp",
        example = "2024-01-01T10:00:00"
    )
    val createdAt: LocalDateTime,

    @Schema(
        description = "Last update timestamp",
        example = "2024-01-15T09:30:00"
    )
    val updatedAt: LocalDateTime
)

@Schema(
    name = "StationSearchResponse",
    description = "Station search results",
    example = """
    {
      "stations": [
        {
          "id": "123e4567-e89b-12d3-a456-426614174000",
          "name": "Gasolinera JSM Centro",
          "address": "Av. Reforma 123, Centro, CDMX",
          "latitude": 19.4326,
          "longitude": -99.1332,
          "distance": 2.3,
          "fuelPrices": {
            "REGULAR": 22.50,
            "PREMIUM": 24.80
          },
          "rating": 4.5,
          "isActive": true
        }
      ],
      "totalCount": 15,
      "searchCriteria": {
        "latitude": 19.4326,
        "longitude": -99.1332,
        "radiusKm": 10.0,
        "fuelType": "REGULAR"
      },
      "executionTimeMs": 45
    }
    """
)
data class StationSearchResponse(
    @Schema(
        description = "List of stations matching search criteria"
    )
    val stations: List<StationResponse>,

    @Schema(
        description = "Total number of stations found",
        example = "15"
    )
    val totalCount: Int,

    @Schema(
        description = "Search criteria used"
    )
    val searchCriteria: StationSearchCriteria,

    @Schema(
        description = "Query execution time in milliseconds",
        example = "45"
    )
    val executionTimeMs: Long
)

@Schema(
    name = "StationSearchCriteria",
    description = "Applied search criteria",
    example = """
    {
      "latitude": 19.4326,
      "longitude": -99.1332,
      "radiusKm": 10.0,
      "fuelType": "REGULAR",
      "sortBy": "DISTANCE",
      "limit": 20
    }
    """
)
data class StationSearchCriteria(
    @Schema(
        description = "Search center latitude",
        example = "19.4326"
    )
    val latitude: BigDecimal,

    @Schema(
        description = "Search center longitude",
        example = "-99.1332"
    )
    val longitude: BigDecimal,

    @Schema(
        description = "Search radius in kilometers",
        example = "10.0"
    )
    val radiusKm: BigDecimal,

    @Schema(
        description = "Fuel type filter applied",
        example = "REGULAR"
    )
    val fuelType: String?,

    @Schema(
        description = "Sort criteria applied",
        example = "DISTANCE"
    )
    val sortBy: String,

    @Schema(
        description = "Result limit applied",
        example = "20"
    )
    val limit: Int
)

@Schema(
    name = "StationStatsResponse",
    description = "Station statistics and analytics",
    example = """
    {
      "stationId": "123e4567-e89b-12d3-a456-426614174000",
      "totalCouponsRedeemed": 1250,
      "totalRevenue": 125000.50,
      "averageTransactionValue": 100.00,
      "peakHours": ["08:00-09:00", "17:00-19:00"],
      "topFuelType": "REGULAR",
      "monthlyStats": {
        "January": {
          "couponsRedeemed": 95,
          "revenue": 9500.00
        }
      },
      "lastUpdated": "2024-01-15T10:30:00"
    }
    """
)
data class StationStatsResponse(
    @Schema(
        description = "Station identifier",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    val stationId: UUID,

    @Schema(
        description = "Total coupons redeemed at this station",
        example = "1250"
    )
    val totalCouponsRedeemed: Int,

    @Schema(
        description = "Total revenue generated",
        example = "125000.50"
    )
    val totalRevenue: BigDecimal,

    @Schema(
        description = "Average transaction value",
        example = "100.00"
    )
    val averageTransactionValue: BigDecimal,

    @Schema(
        description = "Peak operating hours",
        example = "[\"08:00-09:00\", \"17:00-19:00\"]"
    )
    val peakHours: List<String>,

    @Schema(
        description = "Most popular fuel type",
        example = "REGULAR"
    )
    val topFuelType: String,

    @Schema(
        description = "Monthly statistics breakdown"
    )
    val monthlyStats: Map<String, MonthlyStationStats>,

    @Schema(
        description = "Last statistics update timestamp",
        example = "2024-01-15T10:30:00"
    )
    val lastUpdated: LocalDateTime
)

@Schema(
    name = "MonthlyStationStats",
    description = "Monthly station statistics",
    example = """
    {
      "couponsRedeemed": 95,
      "revenue": 9500.00,
      "averageDaily": 3.2,
      "topDay": "2024-01-15"
    }
    """
)
data class MonthlyStationStats(
    @Schema(
        description = "Coupons redeemed in the month",
        example = "95"
    )
    val couponsRedeemed: Int,

    @Schema(
        description = "Revenue generated in the month",
        example = "9500.00"
    )
    val revenue: BigDecimal,

    @Schema(
        description = "Average daily redemptions",
        example = "3.2"
    )
    val averageDaily: BigDecimal,

    @Schema(
        description = "Best performing day",
        example = "2024-01-15"
    )
    val topDay: String?
)

// Enums for documentation
enum class FuelType {
    REGULAR, PREMIUM, DIESEL
}

enum class StationService {
    FULL_SERVICE,
    SELF_SERVICE,
    CONVENIENCE_STORE,
    CAR_WASH,
    ATM,
    RESTROOMS,
    FOOD_COURT,
    TIRE_SERVICE,
    OIL_CHANGE
}

enum class SortCriteria {
    DISTANCE, PRICE, NAME, RATING
}
// Add
itional Response DTOs for StationController
@Schema(
    name = "NearbyStationsResponse",
    description = "Response for nearby stations search",
    example = """
    {
      "stations": [...],
      "searchCriteria": {...},
      "totalFound": 15,
      "searchRadius": 10.0,
      "searchTime": 0.045,
      "recommendations": [...]
    }
    """
)
data class NearbyStationsResponse(
    @Schema(description = "List of nearby stations")
    val stations: List<StationResponse>,

    @Schema(description = "Applied search criteria")
    val searchCriteria: StationSearchCriteria,

    @Schema(description = "Total stations found", example = "15")
    val totalFound: Int,

    @Schema(description = "Search radius used", example = "10.0")
    val searchRadius: BigDecimal,

    @Schema(description = "Search execution time in seconds", example = "0.045")
    val searchTime: BigDecimal,

    @Schema(description = "AI-powered recommendations")
    val recommendations: List<StationRecommendation>
)

@Schema(
    name = "StationDetailsResponse",
    description = "Detailed station information",
    example = """
    {
      "station": {...},
      "realTimeData": {...},
      "reviews": [...],
      "promotions": [...],
      "qrCode": "data:image/png;base64,..."
    }
    """
)
data class StationDetailsResponse(
    @Schema(description = "Complete station information")
    val station: StationResponse,

    @Schema(description = "Real-time operational data")
    val realTimeData: StationRealTimeData?,

    @Schema(description = "Customer reviews and ratings")
    val reviews: List<StationReview>?,

    @Schema(description = "Active promotions")
    val promotions: List<StationPromotion>,

    @Schema(description = "Station QR code for quick access")
    val qrCode: String?
)

@Schema(
    name = "FuelPricesResponse",
    description = "Fuel prices across stations",
    example = """
    {
      "prices": [...],
      "priceStatistics": {...},
      "lastUpdated": "2024-01-15T10:30:00",
      "totalStations": 45
    }
    """
)
data class FuelPricesResponse(
    @Schema(description = "Fuel prices by station")
    val prices: List<StationFuelPrice>,

    @Schema(description = "Price statistics and analytics")
    val priceStatistics: FuelPriceStatistics,

    @Schema(description = "Last price update timestamp")
    val lastUpdated: LocalDateTime,

    @Schema(description = "Total stations included")
    val totalStations: Int
)

@Schema(
    name = "StationAvailabilityResponse",
    description = "Station availability and operating status",
    example = """
    {
      "stations": [...],
      "currentTime": "2024-01-15T10:30:00",
      "openStationsCount": 12,
      "totalStationsCount": 15
    }
    """
)
data class StationAvailabilityResponse(
    @Schema(description = "Station availability information")
    val stations: List<StationAvailability>,

    @Schema(description = "Current timestamp")
    val currentTime: LocalDateTime,

    @Schema(description = "Number of currently open stations")
    val openStationsCount: Int,

    @Schema(description = "Total stations checked")
    val totalStationsCount: Int
)

@Schema(
    name = "UpdatePricesRequest",
    description = "Request to update station fuel prices",
    example = """
    {
      "fuelPrices": {
        "REGULAR": 22.75,
        "PREMIUM": 25.00,
        "DIESEL": 23.45
      },
      "effectiveDate": "2024-01-15T12:00:00",
      "reason": "Market price adjustment"
    }
    """
)
data class UpdatePricesRequest(
    @Schema(description = "New fuel prices by type", required = true)
    val fuelPrices: Map<String, BigDecimal>,

    @Schema(description = "When prices become effective")
    val effectiveDate: LocalDateTime?,

    @Schema(description = "Reason for price change")
    val reason: String?
)

@Schema(
    name = "UpdatePricesResponse",
    description = "Response after updating fuel prices",
    example = """
    {
      "success": true,
      "updatedPrices": {...},
      "effectiveDate": "2024-01-15T12:00:00",
      "priceChangeId": "abc123"
    }
    """
)
data class UpdatePricesResponse(
    @Schema(description = "Whether update was successful")
    val success: Boolean,

    @Schema(description = "Updated fuel prices")
    val updatedPrices: Map<String, BigDecimal>,

    @Schema(description = "When prices became effective")
    val effectiveDate: LocalDateTime,

    @Schema(description = "Unique identifier for this price change")
    val priceChangeId: String,

    @Schema(description = "Error message if update failed")
    val error: String? = null
)

@Schema(
    name = "StationAnalyticsResponse",
    description = "Station performance analytics",
    example = """
    {
      "stationId": "123e4567-e89b-12d3-a456-426614174000",
      "period": {...},
      "salesMetrics": {...},
      "operationalMetrics": {...},
      "customerMetrics": {...},
      "recommendations": [...]
    }
    """
)
data class StationAnalyticsResponse(
    @Schema(description = "Station identifier")
    val stationId: UUID,

    @Schema(description = "Analytics period")
    val period: AnalyticsPeriod,

    @Schema(description = "Sales performance metrics")
    val salesMetrics: SalesMetrics,

    @Schema(description = "Operational performance metrics")
    val operationalMetrics: OperationalMetrics,

    @Schema(description = "Customer behavior metrics")
    val customerMetrics: CustomerMetrics,

    @Schema(description = "Performance improvement recommendations")
    val recommendations: List<PerformanceRecommendation>
)

// Supporting DTOs
@Schema(name = "StationRecommendation")
data class StationRecommendation(
    @Schema(description = "Recommendation type")
    val type: String,

    @Schema(description = "Recommended station ID")
    val stationId: UUID,

    @Schema(description = "Recommendation message")
    val message: String,

    @Schema(description = "Confidence score")
    val confidence: BigDecimal
)

@Schema(name = "StationRealTimeData")
data class StationRealTimeData(
    @Schema(description = "Current operational status")
    val isOperational: Boolean,

    @Schema(description = "Current queue length")
    val queueLength: Int,

    @Schema(description = "Estimated wait time in minutes")
    val estimatedWaitTime: Int,

    @Schema(description = "Fuel availability by type")
    val fuelAvailability: Map<String, Boolean>,

    @Schema(description = "Last status update")
    val lastUpdated: LocalDateTime
)

@Schema(name = "StationReview")
data class StationReview(
    @Schema(description = "Review ID")
    val id: UUID,

    @Schema(description = "Customer rating (1-5)")
    val rating: Int,

    @Schema(description = "Review comment")
    val comment: String?,

    @Schema(description = "Review date")
    val reviewDate: LocalDateTime,

    @Schema(description = "Reviewer name (anonymized)")
    val reviewerName: String
)

@Schema(name = "StationPromotion")
data class StationPromotion(
    @Schema(description = "Promotion title")
    val title: String,

    @Schema(description = "Promotion description")
    val description: String,

    @Schema(description = "Valid until date")
    val validUntil: LocalDateTime,

    @Schema(description = "Discount percentage")
    val discountPercentage: BigDecimal?,

    @Schema(description = "Applicable fuel types")
    val applicableFuelTypes: List<String>
)

@Schema(name = "StationFuelPrice")
data class StationFuelPrice(
    @Schema(description = "Station information")
    val station: StationResponse,

    @Schema(description = "Fuel prices by type")
    val prices: Map<String, BigDecimal>,

    @Schema(description = "Last price update")
    val lastUpdated: LocalDateTime,

    @Schema(description = "Price change from previous update")
    val priceChange: Map<String, BigDecimal>?
)

@Schema(name = "FuelPriceStatistics")
data class FuelPriceStatistics(
    @Schema(description = "Average prices by fuel type")
    val averagePrices: Map<String, BigDecimal>,

    @Schema(description = "Lowest prices by fuel type")
    val lowestPrices: Map<String, BigDecimal>,

    @Schema(description = "Highest prices by fuel type")
    val highestPrices: Map<String, BigDecimal>,

    @Schema(description = "Price volatility indicators")
    val volatility: Map<String, BigDecimal>
)

@Schema(name = "StationAvailability")
data class StationAvailability(
    @Schema(description = "Station information")
    val station: StationResponse,

    @Schema(description = "Current operating status")
    val isOpen: Boolean,

    @Schema(description = "Operating hours for today")
    val todayHours: String?,

    @Schema(description = "Next opening time if closed")
    val nextOpening: LocalDateTime?,

    @Schema(description = "Queue and wait information")
    val queueInfo: QueueInfo?
)

@Schema(name = "QueueInfo")
data class QueueInfo(
    @Schema(description = "Current queue length")
    val queueLength: Int,

    @Schema(description = "Estimated wait time in minutes")
    val estimatedWaitTime: Int,

    @Schema(description = "Queue status")
    val status: String
)

@Schema(name = "AnalyticsPeriod")
data class AnalyticsPeriod(
    @Schema(description = "Period start date")
    val startDate: LocalDateTime,

    @Schema(description = "Period end date")
    val endDate: LocalDateTime,

    @Schema(description = "Period granularity")
    val granularity: String
)

@Schema(name = "SalesMetrics")
data class SalesMetrics(
    @Schema(description = "Total revenue")
    val totalRevenue: BigDecimal,

    @Schema(description = "Total transactions")
    val totalTransactions: Int,

    @Schema(description = "Average transaction value")
    val averageTransactionValue: BigDecimal,

    @Schema(description = "Revenue by fuel type")
    val revenueByFuelType: Map<String, BigDecimal>
)

@Schema(name = "OperationalMetrics")
data class OperationalMetrics(
    @Schema(description = "Uptime percentage")
    val uptimePercentage: BigDecimal,

    @Schema(description = "Average service time")
    val averageServiceTime: BigDecimal,

    @Schema(description = "Pump utilization rate")
    val pumpUtilization: BigDecimal,

    @Schema(description = "Maintenance incidents")
    val maintenanceIncidents: Int
)

@Schema(name = "CustomerMetrics")
data class CustomerMetrics(
    @Schema(description = "Unique customers served")
    val uniqueCustomers: Int,

    @Schema(description = "Customer retention rate")
    val retentionRate: BigDecimal,

    @Schema(description = "Average customer satisfaction")
    val averageSatisfaction: BigDecimal,

    @Schema(description = "Net Promoter Score")
    val netPromoterScore: BigDecimal
)

@Schema(name = "PerformanceRecommendation")
data class PerformanceRecommendation(
    @Schema(description = "Recommendation category")
    val category: String,

    @Schema(description = "Recommendation title")
    val title: String,

    @Schema(description = "Detailed description")
    val description: String,

    @Schema(description = "Expected impact")
    val expectedImpact: String,

    @Schema(description = "Priority level")
    val priority: String
)