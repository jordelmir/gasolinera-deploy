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
@Table(name = "stations")
data class Station(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false)
    @field:NotBlank(message = "Station name is required")
    val name: String,

    @Column(name = "code", unique = true, nullable = false)
    @field:NotBlank(message = "Station code is required")
    val code: String,

    @Column(name = "address", nullable = false)
    @field:NotBlank(message = "Address is required")
    val address: String,

    @Column(name = "city", nullable = false)
    @field:NotBlank(message = "City is required")
    val city: String,

    @Column(name = "latitude", nullable = false)
    @field:NotNull(message = "Latitude is required")
    val latitude: BigDecimal,

    @Column(name = "longitude", nullable = false)
    @field:NotNull(message = "Longitude is required")
    val longitude: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: StationStatus = StationStatus.ACTIVE,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    override fun toString(): String {
        return "Station(id=$id, name='$name', code='$code', city='$city', status=$status)"
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