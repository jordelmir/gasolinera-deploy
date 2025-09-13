package com.gasolinerajsm.stationservice.domain.model

/**
 * Station operational status
 */
enum class StationStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Station is operational and accepting customers"),
    INACTIVE("Inactive", "Station is temporarily closed"),
    MAINTENANCE("Under Maintenance", "Station is under maintenance"),
    OUT_OF_FUEL("Out of Fuel", "Station has no fuel available"),
    EMERGENCY_CLOSED("Emergency Closed", "Station closed due to emergency");

    /**
     * Check if station can serve customers
     */
    fun canServeCustomers(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status is temporary
     */
    fun isTemporary(): Boolean {
        return this in listOf(MAINTENANCE, OUT_OF_FUEL, EMERGENCY_CLOSED)
    }
}

/**
 * Types of fuel available at stations
 */
enum class FuelType(val displayName: String, val octaneRating: Int?) {
    REGULAR("Regular", 87),
    PREMIUM("Premium", 91),
    SUPER_PREMIUM("Super Premium", 93),
    DIESEL("Diesel", null),
    E85("E85 Ethanol", null),
    ELECTRIC("Electric Charging", null);

    /**
     * Check if fuel type is gasoline
     */
    fun isGasoline(): Boolean {
        return this in listOf(REGULAR, PREMIUM, SUPER_PREMIUM, E85)
    }

    /**
     * Check if fuel type requires special handling
     */
    fun requiresSpecialHandling(): Boolean {
        return this in listOf(DIESEL, E85, ELECTRIC)
    }
}

/**
 * Station amenities and services
 */
enum class StationAmenity(val displayName: String, val category: AmenityCategory) {
    // Convenience Store
    CONVENIENCE_STORE("Convenience Store", AmenityCategory.RETAIL),
    ATM("ATM", AmenityCategory.FINANCIAL),

    // Food Services
    RESTAURANT("Restaurant", AmenityCategory.FOOD),
    FAST_FOOD("Fast Food", AmenityCategory.FOOD),
    COFFEE_SHOP("Coffee Shop", AmenityCategory.FOOD),

    // Vehicle Services
    CAR_WASH("Car Wash", AmenityCategory.VEHICLE_SERVICE),
    AUTO_REPAIR("Auto Repair", AmenityCategory.VEHICLE_SERVICE),
    TIRE_SERVICE("Tire Service", AmenityCategory.VEHICLE_SERVICE),
    OIL_CHANGE("Oil Change", AmenityCategory.VEHICLE_SERVICE),

    // Facilities
    RESTROOMS("Restrooms", AmenityCategory.FACILITY),
    PARKING("Parking", AmenityCategory.FACILITY),
    TRUCK_PARKING("Truck Parking", AmenityCategory.FACILITY),
    ELECTRIC_CHARGING("Electric Vehicle Charging", AmenityCategory.FACILITY),

    // Services
    WIFI("Free WiFi", AmenityCategory.SERVICE),
    LOYALTY_PROGRAM("Loyalty Program", AmenityCategory.SERVICE),
    MOBILE_PAYMENT("Mobile Payment", AmenityCategory.SERVICE),
    PROPANE("Propane Refill", AmenityCategory.SERVICE),
    DIESEL("Diesel Fuel", AmenityCategory.SERVICE),
    FLEET_SERVICES("Fleet Services", AmenityCategory.SERVICE);

    /**
     * Check if amenity is essential
     */
    fun isEssential(): Boolean {
        return this in listOf(RESTROOMS, PARKING, MOBILE_PAYMENT)
    }
}

/**
 * Categories for station amenities
 */
enum class AmenityCategory(val displayName: String) {
    RETAIL("Retail"),
    FINANCIAL("Financial Services"),
    FOOD("Food & Beverage"),
    VEHICLE_SERVICE("Vehicle Services"),
    FACILITY("Facilities"),
    SERVICE("Services")
}

/**
 * Days of the week for operating hours
 */
enum class DayOfWeek(val displayName: String) {
    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday");

    /**
     * Check if day is weekend
     */
    fun isWeekend(): Boolean {
        return this in listOf(SATURDAY, SUNDAY)
    }

    /**
     * Check if day is weekday
     */
    fun isWeekday(): Boolean {
        return !isWeekend()
    }
}