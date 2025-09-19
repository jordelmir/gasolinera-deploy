package com.gasolinerajsm.shared.security.model

import java.time.LocalDateTime
import java.util.*

data class Role(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val permissions: Set<Permission> = emptySet(),
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: UUID? = null,
    val updatedBy: UUID? = null
) {
    companion object {
        // Predefined roles
        val ADMIN = Role(
            name = "ADMIN",
            description = "System Administrator",
            permissions = setOf(
                Permission.ADMIN_ACCESS,
                Permission.READ_USERS,
                Permission.WRITE_USERS,
                Permission.DELETE_USERS,
                Permission.READ_ROLES,
                Permission.WRITE_ROLES,
                Permission.DELETE_ROLES,
                Permission.READ_COUPONS,
                Permission.WRITE_COUPONS,
                Permission.DELETE_COUPONS,
                Permission.READ_CAMPAIGNS,
                Permission.WRITE_CAMPAIGNS,
                Permission.DELETE_CAMPAIGNS,
                Permission.READ_STATIONS,
                Permission.WRITE_STATIONS,
                Permission.DELETE_STATIONS
            )
        )

        val MANAGER = Role(
            name = "MANAGER",
            description = "Station Manager",
            permissions = setOf(
                Permission.READ_COUPONS,
                Permission.WRITE_COUPONS,
                Permission.READ_CAMPAIGNS,
                Permission.WRITE_CAMPAIGNS,
                Permission.READ_STATIONS,
                Permission.WRITE_STATIONS
            )
        )

        val EMPLOYEE = Role(
            name = "EMPLOYEE",
            description = "Station Employee",
            permissions = setOf(
                Permission.READ_COUPONS,
                Permission.READ_STATIONS
            )
        )

        val USER = Role(
            name = "USER",
            description = "Regular User",
            permissions = setOf(
                Permission.READ_COUPONS
            )
        )
    }

    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)
    fun hasPermission(permissionName: String): Boolean = permissions.any { it.name == permissionName }
}