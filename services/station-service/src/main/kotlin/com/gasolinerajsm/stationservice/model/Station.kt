package com.gasolinerajsm.stationservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Station entity representing a gas station in the system
 */
@Entity
@Table(
    name = "stations",
    schema = "station_schema",
    indexes = [
        Index(name = "idx_stations_status", columnList = "status"),
        Index(name = "idx_stations_location", columnList = "latitude, longitude"),
        Index(name = "idx_stations_name", columnList = "name"),
        Index(name = "idx_stations_created_at", columnList = "created_at")
    ]
)
data class Station(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "name", nullable = false, length = 200)
    @field:NotBlank(message = "Station name is required")
    @field:Size(min = 2, max = 200, message = "Station name must be between 2 and 200 characters")
    val name: String,

    @Column(name = "code", unique = true, nullable = false, length = 20)
    @field:NotBlank(message = "Station code is required")
    @field:Pattern(
        regexp = "^[A-Z0-9]{3,20}$",
        message = "Station code must be 3-20 characters, uppercase letters and numbers only"
    )
    val code: String,

    @Column(name = "address", nullable = false, length = 500)
    @field:NotBlank(message = "Address is required")
    @field:Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    val address: String,

    @Column(name = "city", nullable = false, length = 100)
    @field:NotBlank(message = "City is required")
    @field:Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    val city: String,

    @Column(name = "state", nullable = false, length = 100)
    @field:NotBlank(message = "State is required")
    @field:Size(min = 2, max = 100, message = "State must be between 2 and 100 characters")
    val state: String,

    @Column(name = "postal_code", nullable = false, length = 20)
    @field:NotBlank(message = "Postal code is required")
    @field:Pattern(
        regexp = "^[0-9]{5}(-[0-9]{4})?$",
        message = "Postal code must be in format 12345 or 12345-6789"
    )
    val postalCode: String,

    @Column(name = "country", nullable = false, length = 100)
    @field:NotBlank(message = "Country is required")
    val country: String = "Mexico",

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val latitude: BigDecimal,

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val longitude: BigDecimal,

    @Column(name = "phone_number", length = 20)
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format"
    )
    val phoneNumber: String? = null,

    @Column(name = "email", length = 100)
    @field:Email(message = "Email must be valid")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    val email: String? = null,

    @Column(name = "manager_name", length = 200)
    @field:Size(max = 200, message = "Manager name must not exceed 200 characters")
    val managerName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: StationStatus = StationStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "station_type", nullable = false, length = 30)
    val stationType: StationType = StationType.FULL_SERVICE,

    @Column(name = "operating_hours", length = 100)
    val operatingHours: String? = null,

    @Column(name = "services_offered", length = 1000)
    val servicesOffered: String? = null,

    @Column(name = "fuel_types", length = 500)
    val fuelTypes: String? = null,

    @Column(name = "payment_methods", length = 500)
    val paymentMethods: String? = null,

    @Column(name = "is_24_hours", nullable = false)
    val is24Hours: Boolean = false,

    @Column(name = "has_convenience_store", nullable = false)
    val hasConvenienceStore: Boolean = false,

    @Column(name = "has_car_wash", nullable = false)
    val hasCarWash: Boolean = false,

    @Column(name = "has_atm", nullable = false)
    val hasAtm: Boolean = false,

    @Column(name = "has_restrooms", nullable = false)
    val hasRestrooms: Boolean = true,

    @Column(name = "pump_count", nullable = false)
    @field:Min(value = 1, message = "Station must have at least 1 pump")
    @field:Max(value = 50, message = "Station cannot have more than 50 pumps")
    val pumpCount: Int = 1,

    @Column(name = "capacity_vehicles_per_hour")
    @field:Min(value = 0, message = "Capacity cannot be negative")
    val capacityVehiclesPerHour: Int? = null,

    @Column(name = "average_service_time_minutes")
    @field:Min(value = 1, message = "Service time must be at least 1 minute")
    val averageServiceTimeMinutes: Int? = null,

    @OneToMany(mappedBy = "station", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val employees: List<Employee> = emptyList(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    val createdBy: String? = null,

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null
) {

    /**
     * Get the full address as a single string
     */
    fun getFullAddress(): String {
        return "$address, $city, $state $postalCode, $country"
    }

    /**
     * Check if the station is currently operational
     */
    fun isOperational(): Boolean {
        return status == StationStatus.ACTIVE
    }

    /**
     * Check if the station is available for new customers
     */
    fun isAvailableForService(): Boolean {
        return status == StationStatus.ACTIVE && status != StationStatus.MAINTENANCE
    }

    /**
     * Get the number of active employees
     */
    fun getActiveEmployeeCount(): Int {
        return employees.count { it.isActive }
    }

    /**
     * Check if the station has a specific service
     */
    fun hasService(service: String): Boolean {
        return servicesOffered?.contains(service, ignoreCase = true) == true
    }

    /**
     * Check if the station accepts a specific payment method
     */
    fun acceptsPaymentMethod(method: String): Boolean {
        return paymentMethods?.contains(method, ignoreCase = true) == true
    }

    /**
     * Get distance from a point (simplified calculation)
     */
    fun getDistanceFrom(lat: BigDecimal, lng: BigDecimal): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians((lat - latitude).toDouble())
        val dLng = Math.toRadians((lng - longitude).toDouble())

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude.toDouble())) * Math.cos(Math.toRadians(lat.toDouble())) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Activate the station
     */
    fun activate(updatedBy: String? = null): Station {
        return this.copy(
            status = StationStatus.ACTIVE,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Deactivate the station
     */
    fun deactivate(updatedBy: String? = null): Station {
        return this.copy(
            status = StationStatus.INACTIVE,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Put station in maintenance mode
     */
    fun setMaintenance(updatedBy: String? = null): Station {
        return this.copy(
            status = StationStatus.MAINTENANCE,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    override fun toString(): String {
        return "Station(id=$id, name='$name', code='$code', city='$city', status=$status, pumpCount=$pumpCount)"
    }
}

/**
 * Station status enumeration
 */
enum class StationStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Station is operational and accepting customers"),
    INACTIVE("Inactive", "Station is temporarily closed"),
    MAINTENANCE("Under Maintenance", "Station is under maintenance"),
    CONSTRUCTION("Under Construction", "Station is being built or renovated"),
    PERMANENTLY_CLOSED("Permanently Closed", "Station is permanently closed");

    fun isOperational(): Boolean {
        return this == ACTIVE
    }

    fun allowsNewCustomers(): Boolean {
        return this == ACTIVE
    }
}

/**
 * Station type enumeration
 */
enum class StationType(val displayName: String, val description: String) {
    FULL_SERVICE("Full Service", "Full service gas station with attendants"),
    SELF_SERVICE("Self Service", "Self-service gas station"),
    HYBRID("Hybrid", "Both full service and self-service options"),
    TRUCK_STOP("Truck Stop", "Gas station designed for trucks and large vehicles"),
    CONVENIENCE("Convenience", "Gas station with convenience store focus"),
    PREMIUM("Premium", "Premium gas station with luxury services");

    fun requiresAttendant(): Boolean {
        return this == FULL_SERVICE || this == HYBRID
    }

    fun allowsSelfService(): Boolean {
        return this == SELF_SERVICE || this == HYBRID
    }
}