package com.gasolinerajsm.stationservice.domain.model

import com.gasolinerajsm.stationservice.domain.event.DomainEvent
import com.gasolinerajsm.stationservice.domain.event.StationActivatedEvent
import com.gasolinerajsm.stationservice.domain.event.StationCreatedEvent
import com.gasolinerajsm.stationservice.domain.event.StationDeactivatedEvent
import com.gasolinerajsm.stationservice.domain.valueobject.*
import java.time.LocalDateTime

/**
 * Station Domain Entity - Core business logic for gas stations
 */
data class Station(
    val id: StationId,
    val name: String,
    val code: StationCode,
    val address: Address,
    val location: GeoLocation,
    val contactInfo: ContactInfo,
    val status: StationStatus = StationStatus.ACTIVE,
    val stationType: StationType = StationType.FULL_SERVICE,
    val operatingHours: String? = null,
    val services: StationServices = StationServices(),
    val facilities: StationFacilities = StationFacilities(),
    val operationalInfo: OperationalInfo = OperationalInfo(),
    val employees: List<Employee> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: String? = null,
    val updatedBy: String? = null,
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {

    companion object {
        /**
         * Factory method to create a new station
         */
        fun create(
            name: String,
            code: StationCode,
            address: Address,
            location: GeoLocation,
            contactInfo: ContactInfo,
            stationType: StationType = StationType.FULL_SERVICE,
            createdBy: String? = null
        ): Station {
            val station = Station(
                id = StationId.generate(),
                name = name,
                code = code,
                address = address,
                location = location,
                contactInfo = contactInfo,
                stationType = stationType,
                createdBy = createdBy
            )

            station.addDomainEvent(
                StationCreatedEvent(
                    stationId = station.id,
                    stationCode = station.code,
                    name = station.name,
                    location = station.location,
                    createdBy = createdBy,
                    occurredAt = LocalDateTime.now()
                )
            )

            return station
        }
    }

    /**
     * Check if the station is currently operational
     */
    fun isOperational(): Boolean {
        return status.isOperational()
    }

    /**
     * Check if the station is available for new customers
     */
    fun isAvailableForService(): Boolean {
        return status.allowsNewCustomers() && hasActiveEmployees()
    }

    /**
     * Get the number of active employees
     */
    fun getActiveEmployeeCount(): Int {
        return employees.count { it.isCurrentlyEmployed() }
    }

    /**
     * Check if station has active employees
     */
    fun hasActiveEmployees(): Boolean {
        return getActiveEmployeeCount() > 0
    }

    /**
     * Get employees by role
     */
    fun getEmployeesByRole(role: EmployeeRole): List<Employee> {
        return employees.filter { it.role == role && it.isCurrentlyEmployed() }
    }

    /**
     * Check if station has manager
     */
    fun hasManager(): Boolean {
        return employees.any { it.role == EmployeeRole.MANAGER && it.isCurrentlyEmployed() }
    }

    /**
     * Get distance from a point
     */
    fun getDistanceFrom(otherLocation: GeoLocation): Distance {
        return location.distanceTo(otherLocation)
    }

    /**
     * Check if location is within service radius
     */
    fun isWithinServiceRadius(otherLocation: GeoLocation, radiusKm: Double): Boolean {
        return getDistanceFrom(otherLocation).kilometers <= radiusKm
    }

    /**
     * Check if the station has a specific service
     */
    fun hasService(service: String): Boolean {
        return services.hasService(service)
    }

    /**
     * Check if the station accepts a specific payment method
     */
    fun acceptsPaymentMethod(method: String): Boolean {
        return services.acceptsPaymentMethod(method)
    }

    /**
     * Check if station can handle expected capacity
     */
    fun canHandleCapacity(expectedVehicles: Int): Boolean {
        return operationalInfo.capacityVehiclesPerHour?.let { capacity ->
            expectedVehicles <= capacity
        } ?: true // If capacity not set, assume it can handle
    }

    /**
     * Activate the station
     */
    fun activate(updatedBy: String? = null): Station {
        if (status == StationStatus.ACTIVE) {
            return this
        }

        val updatedStation = this.copy(
            status = StationStatus.ACTIVE,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )

        updatedStation.addDomainEvent(
            StationActivatedEvent(
                stationId = id,
                activatedBy = updatedBy,
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedStation
    }

    /**
     * Deactivate the station
     */
    fun deactivate(reason: String? = null, updatedBy: String? = null): Station {
        if (status == StationStatus.INACTIVE) {
            return this
        }

        val updatedStation = this.copy(
            status = StationStatus.INACTIVE,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )

        updatedStation.addDomainEvent(
            StationDeactivatedEvent(
                stationId = id,
                reason = reason,
                deactivatedBy = updatedBy,
                occurredAt = LocalDateTime.now()
            )
        )

        return updatedStation
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

    /**
     * Update station information
     */
    fun updateInfo(
        name: String? = null,
        contactInfo: ContactInfo? = null,
        operatingHours: String? = null,
        updatedBy: String? = null
    ): Station {
        return this.copy(
            name = name ?: this.name,
            contactInfo = contactInfo ?: this.contactInfo,
            operatingHours = operatingHours ?: this.operatingHours,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Add employee to station
     */
    fun addEmployee(employee: Employee): Station {
        require(employee.stationId == this.id) { "Employee must belong to this station" }

        return this.copy(
            employees = employees + employee,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Remove employee from station
     */
    fun removeEmployee(employeeId: EmployeeId): Station {
        return this.copy(
            employees = employees.filterNot { it.id == employeeId },
            updatedAt = LocalDateTime.now()
        )
    }

    // Domain Events Management
    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()

    fun markEventsAsCommitted() = domainEvents.clear()

    override fun toString(): String {
        return "Station(id=$id, name='$name', code=$code, city='${address.city}', status=$status, employeeCount=${employees.size})"
    }
}