package com.gasolinerajsm.authservice.domain.model

/**
 * User roles in the system - Domain Value Object
 */
enum class UserRole(val displayName: String, val permissions: Set<String>) {
    CUSTOMER(
        "Customer",
        setOf("coupon:redeem", "raffle:participate", "ad:view", "profile:view", "profile:update")
    ),
    EMPLOYEE(
        "Employee",
        setOf("coupon:validate", "redemption:process", "station:view", "profile:view", "profile:update")
    ),
    STATION_ADMIN(
        "Station Administrator",
        setOf(
            "coupon:validate", "redemption:process", "redemption:view",
            "station:view", "station:update", "employee:manage",
            "profile:view", "profile:update", "analytics:station"
        )
    ),
    SYSTEM_ADMIN(
        "System Administrator",
        setOf(
            "user:manage", "station:manage", "campaign:manage", "coupon:manage",
            "raffle:manage", "ad:manage", "analytics:system", "system:configure"
        )
    );

    /**
     * Check if this role has a specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Check if this role can access admin features
     */
    fun isAdmin(): Boolean {
        return this == STATION_ADMIN || this == SYSTEM_ADMIN
    }

    /**
     * Check if this role can manage stations
     */
    fun canManageStations(): Boolean {
        return this == SYSTEM_ADMIN
    }

    /**
     * Check if this role can process redemptions
     */
    fun canProcessRedemptions(): Boolean {
        return this == EMPLOYEE || this == STATION_ADMIN
    }

    /**
     * Get all permissions as a list
     */
    fun getAllPermissions(): List<String> = permissions.toList()

    /**
     * Check if this role has any of the specified permissions
     */
    fun hasAnyPermission(vararg permissions: String): Boolean {
        return permissions.any { hasPermission(it) }
    }

    /**
     * Check if this role has all of the specified permissions
     */
    fun hasAllPermissions(vararg permissions: String): Boolean {
        return permissions.all { hasPermission(it) }
    }
}