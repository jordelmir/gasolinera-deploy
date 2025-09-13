package com.gasolinerajsm.station.web

import com.gasolinerajsm.station.application.*
import com.gasolinerajsm.station.web.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*

/**
 * Gas Station Controller with comprehensive OpenAPI documentation
 * Handles station discovery, fuel pricing, and location-based services
 */
@RestController
@RequestMapping("/api/v1/stations")
@Tag(
    name = "Gas Stations",
    description = "⛽ Gas station management system with location services, fuel pricing, real-time availability, and station operations"
)
class StationController(
    private val stationUseCase: StationUseCase
) {

    @Operation(
        summary = "Find nearby gas stations",
        description = """
            Discovers gas stations within a specified radius of a given location using advanced geospatial algorithms.

            ## Location Services:
            - 🗺️ **GPS Integration** - Precise location-based search
            - 📍 **Radius Search** - Customizable search radius (1-50 km)
            - 🧭 **Distance Calculation** - Accurate distance using Haversine formula
            - 🚗 **Route Optimization** - Stations sorted by driving distance
            - 📱 **Real-Time Data** - Live fuel prices and availability

            ## Search Features:
            - ⛽ **Fuel Type Filter** - Find stations with specific fuel types
            - 💰 **Price Sorting** - Sort by fuel prices (lowest first)
            - ⭐ **Rating Filter** - Filter by customer ratings
            - 🕒 **Operating Hours** - Filter by current availability
            - 🏪 **Amenities** - Filter by services (car wash, convenience store)

            ## Response Data:
            - 📊 **Station Details** - Name, address, contact information
            - 💰 **Current Prices** - Real-time fuel prices by type
            - 📍 **Location Data** - GPS coordinates and directions
            - ⭐ **Ratings & Reviews** - Customer feedback and ratings
            - 🕒 **Operating Status** - Current operational status
            - 🛠️ **Services** - Available amenities and services

            ## Performance:
            - ⚡ **Fast Response** - Optimized geospatial queries
            - 💾 **Caching** - Intelligent caching for frequently searched areas
            - 📱 **Mobile Optimized** - Lightweight responses for mobile apps
            - 🔄 **Real-Time Updates** - Live price and availability updates
        """,
        tags = ["Gas Stations"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "✅ Nearby stations found successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = NearbyStationsResponse::class),
                    examples = [ExampleObject(
                        name = "Nearby Stations",
                        value = """{
                            "stations": [
                                {
                                    "id": "987fcdeb-51a2-43d7-b456-426614174999",
                                    "name": "Gasolinera Central CDMX",
                                    "brand": "Pemex",
                                    "address": "Av. Reforma 123, Cuauhtémoc, CDMX",
                                    "location": {
                                        "latitude": 19.4326,
                                        "longitude": -99.1332
                                    },
                                    "distance": 0.8,
                                    "estimatedTravelTime": 3,
                                    "fuelPrices": {
                                        "REGULAR": 22.50,
                                        "PREMIUM": 24.80,
                                        "DIESEL": 23.20
                                    },
                                    "isOperational": true,
                                    "operatingHours": {
                                        "monday": "06:00-22:00",
                                        "tuesday": "06:00-22:00",
                                        "wednesday": "06:00-22:00",
                                        "thursday": "06:00-22:00",
                                        "friday": "06:00-22:00",
                                        "saturday": "07:00-21:00",
                                        "sunday": "08:00-20:00"
                                    },
                                    "rating": 4.5,
                                    "reviewCount": 1247,
                                    "amenities": [
                                        "CAR_WASH",
                                        "CONVENIENCE_STORE",
                                        "ATM",
                                        "RESTROOMS",
                                        "AIR_PUMP"
                                    ],
                                    "paymentMethods": [
                                        "CASH",
                                        "CREDIT_CARD",
                                        "DEBIT_CARD",
                                        "DIGITAL_WALLET"
                                    ],
                                    "lastPriceUpdate": "2024-01-15T08:00:00Z",
                                    "promotions": [
                                        {
                                            "title": "Premium Fuel Discount",
                                            "description": "10% off premium fuel with coupon",
                                            "validUntil": "2024-01-31T23:59:59Z"
                                        }
                                    ]
                                }
                            ],
                            "searchCriteria": {
                                "centerLatitude": 19.4326,
                                "centerLongitude": -99.1332,
                                "radiusKm": 10.0,
                                "fuelTypeFilter": null,
                                "maxPriceFilter": null
                            },
                            "totalFound": 15,
                            "searchRadius": 10.0,
                            "searchTime": 0.045,
                            "recommendations": [
                                {
                                    "type": "BEST_PRICE",
                                    "stationId": "987fcdeb-51a2-43d7-b456-426614174999",
                                    "message": "Best price for Regular fuel in your area"
                                },
                                {
                                    "type": "CLOSEST",
                                    "stationId": "123e4567-e89b-12d3-a456-426614174000",
                                    "message": "Closest station to your location"
                                }
                            ]
                        }"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "❌ Invalid location parameters",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Invalid Coordinates",
                        value = """{
                            "error": "INVALID_COORDINATES",
                            "message": "Invalid latitude or longitude values provided",
                            "details": {
                                "latitude": 200.0,
                                "longitude": -200.0,
                                "validLatitudeRange": "-90.0 to 90.0",
                                "validLongitudeRange": "-180.0 to 180.0"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }"""
                    )]
                )]
            )
        ]
    )
    @GetMapping("/nearby")
    fun findNearbyStations(
        @Parameter(
            description = "Latitude coordinate for search center",
            required = true,
            example = "19.4326",
            schema = Schema(minimum = "-90.0", maximum = "90.0")
        )
        @RequestParam
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        latitude: Double,

        @Parameter(
            description = "Longitude coordinate for search center",
            required = true,
            example = "-99.1332",
            schema = Schema(minimum = "-180.0", maximum = "180.0")
        )
        @RequestParam
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        longitude: Double,

        @Parameter(
            description = "Search radius in kilometers",
            example = "10.0",
            schema = Schema(minimum = "0.1", maximum = "50.0", defaultValue = "10.0")
        )
        @RequestParam(defaultValue = "10.0")
        @DecimalMin(value = "0.1", message = "Minimum radius is 0.1 km")
        @DecimalMax(value = "50.0", message = "Maximum radius is 50 km")
        radius: Double,

        @Parameter(
            description = "Filter by fuel type availability",
            schema = Schema(allowableValues = ["REGULAR", "PREMIUM", "DIESEL"])
        )
        @RequestParam(required = false)
        fuelType: String?,

        @Parameter(
            description = "Maximum fuel price filter",
            example = "25.0"
        )
        @RequestParam(required = false)
        maxPrice: BigDecimal?,

        @Parameter(
            description = "Minimum station rating filter",
            example = "4.0",
            schema = Schema(minimum = "1.0", maximum = "5.0")
        )
        @RequestParam(required = false)
        @DecimalMin(value = "1.0")
        @DecimalMax(value = "5.0")
        minRating: Double?,

        @Parameter(
            description = "Filter by amenities",
            example = "CAR_WASH,CONVENIENCE_STORE"
        )
        @RequestParam(required = false)
        amenities: List<String>?,

        @Parameter(
            description = "Sort results by criteria",
            schema = Schema(
                allowableValues = ["DISTANCE", "PRICE", "RATING", "NAME"],
                defaultValue = "DISTANCE"
            )
        )
        @RequestParam(defaultValue = "DISTANCE")
        sortBy: String,

        @Parameter(description = "Maximum number of results to return")
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 100, message = "Limit must not exceed 100")
        limit: Int
    ): ResponseEntity<NearbyStationsResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get detailed station information",
        description = """
            Retrieves comprehensive information about a specific gas station.

            ## Station Information:
            - 🏪 **Basic Details** - Name, brand, address, contact info
            - 📍 **Location Data** - GPS coordinates, directions, landmarks
            - ⛽ **Fuel Services** - Available fuel types and current prices
            - 🕒 **Operating Hours** - Daily schedules and holiday hours
            - 🛠️ **Amenities** - Services like car wash, convenience store, ATM
            - ⭐ **Reviews & Ratings** - Customer feedback and ratings
            - 📊 **Statistics** - Usage stats, popularity metrics

            ## Real-Time Data:
            - 💰 **Live Pricing** - Current fuel prices updated hourly
            - 🚦 **Operational Status** - Real-time availability
            - 🚗 **Queue Information** - Current wait times
            - ⛽ **Fuel Availability** - Stock levels by fuel type
            - 🎯 **Promotions** - Active deals and discounts

            ## Additional Features:
            - 📱 **QR Code** - Station-specific QR for quick access
            - 🗺️ **Directions** - Integration with maps applications
            - 📞 **Contact Options** - Phone, website, social media
            - 📸 **Photos** - Station images and facility photos
            - 🎫 **Coupon Compatibility** - Accepted coupon types
        """,
        tags = ["Gas Stations"]
    )
    @GetMapping("/{stationId}")
    fun getStationDetails(
        @Parameter(
            description = "Unique station identifier",
            required = true,
            example = "987fcdeb-51a2-43d7-b456-426614174999"
        )
        @PathVariable stationId: UUID,

        @Parameter(
            description = "Include real-time data (prices, availability)",
            example = "true"
        )
        @RequestParam(defaultValue = "true") includeRealTimeData: Boolean,

        @Parameter(
            description = "Include customer reviews and ratings",
            example = "false"
        )
        @RequestParam(defaultValue = "false") includeReviews: Boolean
    ): ResponseEntity<StationDetailsResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get current fuel prices for all stations",
        description = """
            Retrieves current fuel prices across all gas stations in the network.

            ## Price Information:
            - 💰 **Real-Time Prices** - Updated every 15 minutes
            - ⛽ **All Fuel Types** - Regular, Premium, Diesel prices
            - 📊 **Price Comparison** - Easy comparison across stations
            - 📈 **Price Trends** - Historical price movements
            - 🎯 **Best Deals** - Highlighted lowest prices

            ## Filtering & Sorting:
            - 🏪 **By Station** - Specific station price lookup
            - ⛽ **By Fuel Type** - Filter by specific fuel type
            - 📍 **By Location** - Prices in specific geographic area
            - 💰 **By Price Range** - Filter by price thresholds
            - ⏰ **By Update Time** - Recently updated prices first

            ## Business Features:
            - 🎫 **Coupon Integration** - Shows coupon-eligible stations
            - 🏆 **Loyalty Discounts** - Member pricing where applicable
            - 📢 **Promotions** - Active price promotions
            - 📊 **Analytics** - Price volatility and market insights
        """,
        tags = ["Gas Stations"]
    )
    @GetMapping("/prices")
    fun getFuelPrices(
        @Parameter(description = "Filter by specific station")
        @RequestParam(required = false) stationId: UUID?,

        @Parameter(
            description = "Filter by fuel type",
            schema = Schema(allowableValues = ["REGULAR", "PREMIUM", "DIESEL"])
        )
        @RequestParam(required = false) fuelType: String?,

        @Parameter(description = "Filter by geographic region")
        @RequestParam(required = false) region: String?,

        @Parameter(
            description = "Sort prices by criteria",
            schema = Schema(
                allowableValues = ["PRICE_ASC", "PRICE_DESC", "STATION_NAME", "DISTANCE", "LAST_UPDATED"],
                defaultValue = "PRICE_ASC"
            )
        )
        @RequestParam(defaultValue = "PRICE_ASC") sortBy: String,

        @Parameter(description = "Include price history")
        @RequestParam(defaultValue = "false") includePriceHistory: Boolean,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 50, sort = ["name"])
        pageable: Pageable
    ): ResponseEntity<FuelPricesResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Search stations by name or location",
        description = """
            Advanced search functionality for finding gas stations by various criteria.

            ## Search Capabilities:
            - 🔍 **Text Search** - Search by station name, brand, or address
            - 📍 **Location Search** - Search by city, neighborhood, or landmark
            - 🏷️ **Tag Search** - Search by amenities or services
            - 🔤 **Fuzzy Matching** - Handles typos and partial matches
            - 🌐 **Multi-Language** - Supports Spanish and English search terms

            ## Advanced Filters:
            - ⛽ **Fuel Availability** - Stations with specific fuel types
            - 💰 **Price Range** - Stations within price thresholds
            - ⭐ **Rating Threshold** - Minimum customer rating
            - 🕒 **Operating Hours** - Open now or specific time ranges
            - 🛠️ **Service Types** - Specific amenities required
            - 🏪 **Brand Filter** - Filter by gas station brand

            ## Smart Features:
            - 🧠 **AI-Powered** - Machine learning enhanced search
            - 📊 **Popularity Ranking** - Results ranked by user preferences
            - 🎯 **Personalization** - Results tailored to user history
            - 📱 **Auto-Complete** - Search suggestions as you type
        """,
        tags = ["Gas Stations"]
    )
    @GetMapping("/search")
    fun searchStations(
        @Parameter(
            description = "Search query (station name, address, or keyword)",
            required = true,
            example = "Pemex Reforma"
        )
        @RequestParam
        @NotBlank(message = "Search query is required")
        @Size(min = 2, max = 100, message = "Search query must be between 2 and 100 characters")
        query: String,

        @Parameter(description = "Search center latitude for distance calculation")
        @RequestParam(required = false) latitude: Double?,

        @Parameter(description = "Search center longitude for distance calculation")
        @RequestParam(required = false) longitude: Double?,

        @Parameter(description = "Maximum search radius in kilometers")
        @RequestParam(defaultValue = "25.0") radius: Double,

        @Parameter(description = "Filter by fuel type availability")
        @RequestParam(required = false) fuelType: String?,

        @Parameter(description = "Minimum station rating")
        @RequestParam(required = false) minRating: Double?,

        @Parameter(description = "Filter by brand")
        @RequestParam(required = false) brand: String?,

        @Parameter(description = "Required amenities (comma-separated)")
        @RequestParam(required = false) amenities: List<String>?,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 20, sort = ["name"])
        pageable: Pageable
    ): ResponseEntity<StationSearchResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get station operating hours and availability",
        description = """
            Retrieves detailed operating hours and real-time availability for gas stations.

            ## Operating Information:
            - 🕒 **Daily Hours** - Operating hours for each day of the week
            - 🎄 **Holiday Hours** - Special hours for holidays and events
            - 🚦 **Current Status** - Open/Closed status right now
            - ⏰ **Next Opening** - When closed stations will reopen
            - 🔄 **Schedule Changes** - Temporary schedule modifications

            ## Availability Data:
            - ⛽ **Fuel Availability** - Stock levels by fuel type
            - 🚗 **Queue Status** - Current wait times and queue length
            - 🛠️ **Service Status** - Operational status of pumps and services
            - 👥 **Staff Availability** - Attendant availability for full service
            - 🚧 **Maintenance Alerts** - Planned maintenance or service interruptions

            ## Smart Features:
            - 📱 **Real-Time Updates** - Live status updates every 5 minutes
            - 🔔 **Notifications** - Alerts for status changes
            - 📊 **Predictive Analytics** - Estimated busy times
            - 🎯 **Recommendations** - Best times to visit
        """,
        tags = ["Gas Stations"]
    )
    @GetMapping("/availability")
    fun getStationAvailability(
        @Parameter(description = "Filter by specific station")
        @RequestParam(required = false) stationId: UUID?,

        @Parameter(description = "Filter by geographic region")
        @RequestParam(required = false) region: String?,

        @Parameter(description = "Only show currently open stations")
        @RequestParam(defaultValue = "false") openOnly: Boolean,

        @Parameter(description = "Include queue and wait time information")
        @RequestParam(defaultValue = "true") includeQueueInfo: Boolean
    ): ResponseEntity<StationAvailabilityResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Update station fuel prices (Station Operator only)",
        description = """
            Updates fuel prices for a gas station (restricted to station operators and admins).

            ## Authorization:
            - 🔒 **Station Operators** - Can update their assigned stations only
            - 👑 **Admins** - Can update any station prices
            - 📝 **Audit Logging** - All price changes logged with user info

            ## Price Update Features:
            - ⛽ **Multi-Fuel Support** - Update multiple fuel types simultaneously
            - 📊 **Price Validation** - Market-based price validation
            - ⏰ **Effective Dating** - Schedule price changes for future
            - 📈 **Price History** - Maintains complete price change history
            - 🔔 **Notifications** - Alerts customers of price changes

            ## Business Rules:
            - 💰 **Price Limits** - Prices must be within market range
            - ⏰ **Update Frequency** - Maximum 4 updates per day
            - 📊 **Variance Limits** - Price changes limited to ±10% per update
            - 🎫 **Coupon Impact** - Existing coupons remain valid at old prices
        """,
        tags = ["Gas Stations"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PutMapping("/{stationId}/prices")
    @PreAuthorize("hasRole('STATION_OPERATOR') or hasRole('ADMIN')")
    fun updateStationPrices(
        @Parameter(
            description = "Station ID to update prices for",
            required = true
        )
        @PathVariable stationId: UUID,

        @Parameter(
            description = "New fuel prices",
            required = true,
            schema = Schema(implementation = UpdatePricesRequest::class)
        )
        @Valid @RequestBody request: UpdatePricesRequest
    ): ResponseEntity<UpdatePricesResponse> {
        TODO("Implementation needed")
    }

    @Operation(
        summary = "Get station performance analytics (Admin only)",
        description = """
            Provides comprehensive performance analytics for gas stations.

            ## Performance Metrics:
            - 📊 **Sales Analytics** - Revenue, transaction volume, average sale
            - ⛽ **Fuel Performance** - Sales by fuel type, inventory turnover
            - 👥 **Customer Analytics** - Unique customers, repeat visits, satisfaction
            - ⏰ **Operational Metrics** - Uptime, service quality, efficiency
            - 🎫 **Coupon Performance** - Coupon redemption rates and impact

            ## Comparative Analysis:
            - 🏆 **Ranking** - Station performance ranking
            - 📈 **Benchmarking** - Comparison with network averages
            - 🎯 **Goal Tracking** - Progress against targets
            - 📊 **Trend Analysis** - Performance trends over time

            ## Actionable Insights:
            - 🔍 **Improvement Areas** - Identified optimization opportunities
            - 💡 **Recommendations** - AI-powered suggestions
            - 🚨 **Alerts** - Performance issues and anomalies
            - 📈 **Growth Opportunities** - Market expansion insights
        """,
        tags = ["Gas Stations"],
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @GetMapping("/{stationId}/analytics")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STATION_OPERATOR') and @stationSecurityService.canAccessStation(authentication.name, #stationId))")
    fun getStationAnalytics(
        @Parameter(description = "Station ID for analytics")
        @PathVariable stationId: UUID,

        @Parameter(description = "Start date for analytics period")
        @RequestParam(required = false) startDate: String?,

        @Parameter(description = "End date for analytics period")
        @RequestParam(required = false) endDate: String?,

        @Parameter(description = "Analytics granularity")
        @RequestParam(defaultValue = "day") granularity: String,

        @Parameter(description = "Include comparative data")
        @RequestParam(defaultValue = "true") includeComparison: Boolean
    ): ResponseEntity<StationAnalyticsResponse> {
        TODO("Implementation needed")
    }
}