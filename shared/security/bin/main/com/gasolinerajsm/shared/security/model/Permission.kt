package com.gasolinerajsm.shared.security.model

import java.util.*

data class Permission(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val resource: String? = null,
    val action: String? = null
) {
    companion object {
        // Common permissions
        val READ_USERS = Permission(name = "READ_USERS", description = "Read user information")
        val WRITE_USERS = Permission(name = "WRITE_USERS", description = "Create and update users")
        val DELETE_USERS = Permission(name = "DELETE_USERS", description = "Delete users")

        val READ_ROLES = Permission(name = "READ_ROLES", description = "Read role information")
        val WRITE_ROLES = Permission(name = "WRITE_ROLES", description = "Create and update roles")
        val DELETE_ROLES = Permission(name = "DELETE_ROLES", description = "Delete roles")

        val READ_COUPONS = Permission(name = "READ_COUPONS", description = "Read coupon information")
        val WRITE_COUPONS = Permission(name = "WRITE_COUPONS", description = "Create and update coupons")
        val DELETE_COUPONS = Permission(name = "DELETE_COUPONS", description = "Delete coupons")

        val READ_CAMPAIGNS = Permission(name = "READ_CAMPAIGNS", description = "Read campaign information")
        val WRITE_CAMPAIGNS = Permission(name = "WRITE_CAMPAIGNS", description = "Create and update campaigns")
        val DELETE_CAMPAIGNS = Permission(name = "DELETE_CAMPAIGNS", description = "Delete campaigns")

        val READ_STATIONS = Permission(name = "READ_STATIONS", description = "Read station information")
        val WRITE_STATIONS = Permission(name = "WRITE_STATIONS", description = "Create and update stations")
        val DELETE_STATIONS = Permission(name = "DELETE_STATIONS", description = "Delete stations")

        val ADMIN_ACCESS = Permission(name = "ADMIN_ACCESS", description = "Full administrative access")
    }
}

enum class PermissionScope {
    GLOBAL,
    ORGANIZATION,
    STATION,
    USER
}