package com.gasolinerajsm.shared.security.rbac

import java.time.LocalDateTime

/**
 * Role System for Gasolinera JSM RBAC
 * Defines roles with associated permissions and hierarchies
 */
data class Role(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val permissions: Set<Permission>,
    val isSystemRole: Boolean = false,
    val isActive: Boolean = true,
    val parentRoles: Set<String> = emptySet(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: String? = null,
    val updatedBy: String? = null
) {

    /**
     * Check if role has specific permission
     */
    fun hasPermission(permission: Permission): Boolean {
        return permissions.contains(permission) || hasInheritedPermission(permission)
    }

    /**
     * Check if role has permission through inheritance
     */
    private fun hasInheritedPermission(permission: Permission): Boolean {
        // This would be implemented with role hierarchy resolution
        return false
    }

    /**
     * Get all permission codes for this role
     */
    fun getPermissionCodes(): Set<String> {
        return permissions.map { it.code }.toSet()
    }

    /**
     * Get permissions by category
     */
    fun getPermissionsByCategory(category: PermissionCategory): Set<Permission> {
        return permissions.filter { it.category == category }.toSet()
    }

    /**
     * Check if role is hierarchically above another role
     */
    fun isParentOf(otherRole: Role): Boolean {
        return otherRole.parentRoles.contains(this.id)
    }

    companion object {
        /**
         * Create a new role builder
         */
        fun builder(id: String, name: String): RoleBuilder {
            return RoleBuilder(id, name)
        }
    }
}

/**
 * Builder pattern for Role creation
 */
class RoleBuilder(
    private val id: String,
    private val name: String
) {
    private var displayName: String = name
    private var description: String = ""
    private var permissions: MutableSet<Permission> = mutableSetOf()
    private var isSystemRole: Boolean = false
    private var isActive: Boolean = true
    private var parentRoles: MutableSet<String> = mutableSetOf()
    private var createdBy: String? = null

    fun displayName(displayName: String) = apply { this.displayName = displayName }
    fun description(description: String) = apply { this.description = description }
    fun systemRole(isSystem: Boolean = true) = apply { this.isSystemRole = isSystem }
    fun active(isActive: Boolean = true) = apply { this.isActive = isActive }
    fun createdBy(createdBy: String) = apply { this.createdBy = createdBy }

    fun addPermission(permission: Permission) = apply { this.permissions.add(permission) }
    fun addPermissions(vararg permissions: Permission) = apply { this.permissions.addAll(permissions) }
    fun addPermissions(permissions: Collection<Permission>) = apply { this.permissions.addAll(permissions) }

    fun addParentRole(parentRoleId: String) = apply { this.parentRoles.add(parentRoleId) }
    fun addParentRoles(vararg parentRoleIds: String) = apply { this.parentRoles.addAll(parentRoleIds) }

    fun build(): Role {
        return Role(
            id = id,
            name = name,
            displayName = displayName,
            description = description,
            permissions = permissions.toSet(),
            isSystemRole = isSystemRole,
            isActive = isActive,
            parentRoles = parentRoles.toSet(),
            createdBy = createdBy
        )
    }
}

/**
 * Predefined System Roles for Gasolinera JSM
 */
object SystemRoles {

    /**
     * Super Administrator - Full system access
     */
    val SUPER_ADMIN = Role.builder("super-admin", "SUPER_ADMIN")
        .displayName("Super Administrator")
        .description("Full system access with all permissions")
        .systemRole(true)
        .addPermissions(Permission.values().toList())
        .build()

    /**
     * Administrator - Administrative access
     */
    val ADMIN = Role.builder("admin", "ADMIN")
        .displayName("Administrator")
        .description("Administrative access to most system functions")
        .systemRole(true)
        .addPermissions(
            // User Management
            Permission.USER_CREATE, Permission.USER_READ, Permission.USER_UPDATE,
            Permission.USER_LIST, Permission.USER_SEARCH, Permission.USER_ROLE_ASSIGN,

            // Station Management
            Permission.STATION_CREATE, Permission.STATION_READ, Permission.STATION_UPDATE,
            Permission.STATION_DELETE, Permission.STATION_LIST, Permission.STATION_SEARCH,
            Permission.STATION_FUEL_PRICE_UPDATE, Permission.STATION_INVENTORY_MANAGE,
            Permission.STATION_ANALYTICS_VIEW,

            // Campaign Management
            Permission.CAMPAIGN_CREATE, Permission.CAMPAIGN_READ, Permission.CAMPAIGN_UPDATE,
            Permission.CAMPAIGN_DELETE, Permission.CAMPAIGN_LIST, Permission.CAMPAIGN_ACTIVATE,
            Permission.CAMPAIGN_ANALYTICS_VIEW,

            // Coupon Management
            Permission.COUPON_CREATE, Permission.COUPON_READ, Permission.COUPON_UPDATE,
            Permission.COUPON_DELETE, Permission.COUPON_LIST, Permission.COUPON_GENERATE,
            Permission.COUPON_BULK_OPERATIONS,

            // Raffle Management
            Permission.RAFFLE_CREATE, Permission.RAFFLE_READ, Permission.RAFFLE_UPDATE,
            Permission.RAFFLE_DELETE, Permission.RAFFLE_LIST, Permission.RAFFLE_DRAW,
            Permission.RAFFLE_WINNER_SELECT, Permission.RAFFLE_PRIZE_DISTRIBUTE,

            // Analytics and Reporting
            Permission.ANALYTICS_VIEW, Permission.ANALYTICS_EXPORT, Permission.REPORTS_GENERATE,
            Permission.REPORTS_SCHEDULE, Permission.REPORTS_SHARE, Permission.METRICS_VIEW,

            // System Administration
            Permission.SYSTEM_CONFIG, Permission.SYSTEM_MONITOR, Permission.SYSTEM_LOGS_VIEW,

            // Role Management
            Permission.ROLE_READ, Permission.ROLE_LIST, Permission.PERMISSION_VIEW
        )
        .build()

    /**
     * Manager - Management level access
     */
    val MANAGER = Role.builder("manager", "MANAGER")
        .displayName("Manager")
        .description("Management access to stations and operations")
        .systemRole(true)
        .addPermissions(
            // User Management (limited)
            Permission.USER_READ, Permission.USER_LIST, Permission.USER_SEARCH,

            // Station Management
            Permission.STATION_READ, Permission.STATION_UPDATE, Permission.STATION_LIST,
            Permission.STATION_SEARCH, Permission.STATION_FUEL_PRICE_UPDATE,
            Permission.STATION_INVENTORY_MANAGE, Permission.STATION_ANALYTICS_VIEW,

            // Campaign Management
            Permission.CAMPAIGN_CREATE, Permission.CAMPAIGN_READ, Permission.CAMPAIGN_UPDATE,
            Permission.CAMPAIGN_LIST, Permission.CAMPAIGN_ACTIVATE, Permission.CAMPAIGN_ANALYTICS_VIEW,

            // Coupon Management
            Permission.COUPON_CREATE, Permission.COUPON_READ, Permission.COUPON_UPDATE,
            Permission.COUPON_LIST, Permission.COUPON_USE, Permission.COUPON_VALIDATE,
            Permission.COUPON_GENERATE,

            // Raffle Management
            Permission.RAFFLE_CREATE, Permission.RAFFLE_READ, Permission.RAFFLE_UPDATE,
            Permission.RAFFLE_LIST, Permission.RAFFLE_DRAW, Permission.RAFFLE_WINNER_SELECT,

            // Advertisement Management
            Permission.AD_CREATE, Permission.AD_READ, Permission.AD_UPDATE, Permission.AD_LIST,
            Permission.AD_CAMPAIGN_MANAGE, Permission.AD_TARGETING_CONFIGURE, Permission.AD_ANALYTICS_VIEW,

            // Redemption Management
            Permission.REDEMPTION_READ, Permission.REDEMPTION_LIST, Permission.REDEMPTION_PROCESS,
            Permission.REDEMPTION_VALIDATE,

            // Analytics and Reporting
            Permission.ANALYTICS_VIEW, Permission.REPORTS_GENERATE, Permission.METRICS_VIEW,
            Permission.DASHBOARD_CUSTOMIZE,

            // Financial Operations
            Permission.FINANCIAL_VIEW, Permission.FINANCIAL_REPORTS
        )
        .build()

    /**
     * Employee - Operational level access
     */
    val EMPLOYEE = Role.builder("employee", "EMPLOYEE")
        .displayName("Employee")
        .description("Operational access for station employees")
        .systemRole(true)
        .addPermissions(
            // User Management (self only)
            Permission.USER_READ, Permission.USER_PROFILE_UPDATE,

            // Station Management (limited)
            Permission.STATION_READ, Permission.STATION_LIST, Permission.STATION_SEARCH,
            Permission.STATION_FUEL_PRICE_UPDATE,

            // Campaign Management (read only)
            Permission.CAMPAIGN_READ, Permission.CAMPAIGN_LIST,

            // Coupon Management
            Permission.COUPON_READ, Permission.COUPON_LIST, Permission.COUPON_USE,
            Permission.COUPON_VALIDATE,

            // Raffle Management
            Permission.RAFFLE_READ, Permission.RAFFLE_LIST, Permission.RAFFLE_PARTICIPATE,

            // Advertisement Management (limited)
            Permission.AD_READ, Permission.AD_LIST,

            // Redemption Management
            Permission.REDEMPTION_READ, Permission.REDEMPTION_LIST, Permission.REDEMPTION_PROCESS,

            // Customer Service
            Permission.CUSTOMER_SUPPORT, Permission.CUSTOMER_PROFILE_VIEW,
            Permission.CUSTOMER_ISSUE_RESOLVE, Permission.CUSTOMER_COMMUNICATION
        )
        .build()

    /**
     * Customer - Customer level access
     */
    val CUSTOMER = Role.builder("customer", "CUSTOMER")
        .displayName("Customer")
        .description("Customer access to public features")
        .systemRole(true)
        .addPermissions(
            // User Management (self only)
            Permission.USER_PROFILE_UPDATE, Permission.USER_PASSWORD_CHANGE,

            // Station Management (read only)
            Permission.STATION_READ, Permission.STATION_LIST, Permission.STATION_SEARCH,

            // Campaign Management (read only)
            Permission.CAMPAIGN_READ, Permission.CAMPAIGN_LIST,

            // Coupon Management (limited)
            Permission.COUPON_READ, Permission.COUPON_USE,

            // Raffle Management (participation)
            Permission.RAFFLE_READ, Permission.RAFFLE_LIST, Permission.RAFFLE_PARTICIPATE,

            // Advertisement Management (view only)
            Permission.AD_READ,

            // Redemption Management (own redemptions)
            Permission.REDEMPTION_READ
        )
        .build()

    /**
     * Auditor - Read-only access for auditing
     */
    val AUDITOR = Role.builder("auditor", "AUDITOR")
        .displayName("Auditor")
        .description("Read-only access for auditing and compliance")
        .systemRole(true)
        .addPermissions(
            // Read-only permissions across all categories
            Permission.USER_READ, Permission.USER_LIST, Permission.USER_SEARCH,
            Permission.STATION_READ, Permission.STATION_LIST, Permission.STATION_ANALYTICS_VIEW,
            Permission.CAMPAIGN_READ, Permission.CAMPAIGN_LIST, Permission.CAMPAIGN_ANALYTICS_VIEW,
            Permission.COUPON_READ, Permission.COUPON_LIST,
            Permission.RAFFLE_READ, Permission.RAFFLE_LIST,
            Permission.AD_READ, Permission.AD_LIST, Permission.AD_ANALYTICS_VIEW,
            Permission.REDEMPTION_READ, Permission.REDEMPTION_LIST,
            Permission.ANALYTICS_VIEW, Permission.REPORTS_GENERATE, Permission.METRICS_VIEW,
            Permission.AUDIT_VIEW, Permission.FINANCIAL_VIEW, Permission.FINANCIAL_REPORTS
        )
        .build()

    /**
     * API Service - Service-to-service communication
     */
    val API_SERVICE = Role.builder("api-service", "API_SERVICE")
        .displayName("API Service")
        .description("Service-to-service communication role")
        .systemRole(true)
        .addPermissions(
            // Limited permissions for service integration
            Permission.USER_READ, Permission.STATION_READ, Permission.COUPON_READ,
            Permission.COUPON_USE, Permission.COUPON_VALIDATE, Permission.RAFFLE_READ,
            Permission.REDEMPTION_READ, Permission.REDEMPTION_PROCESS
        )
        .build()

    /**
     * Get all system roles
     */
    fun getAllSystemRoles(): List<Role> {
        return listOf(SUPER_ADMIN, ADMIN, MANAGER, EMPLOYEE, CUSTOMER, AUDITOR, API_SERVICE)
    }

    /**
     * Get role by name
     */
    fun getRoleByName(name: String): Role? {
        return getAllSystemRoles().find { it.name == name }
    }

    /**
     * Get role by id
     */
    fun getRoleById(id: String): Role? {
        return getAllSystemRoles().find { it.id == id }
    }
}

/**
 * Role hierarchy and inheritance utilities
 */
object RoleHierarchy {

    private val hierarchy = mapOf(
        "super-admin" to emptySet<String>(),
        "admin" to setOf("manager"),
        "manager" to setOf("employee"),
        "employee" to setOf("customer"),
        "customer" to emptySet<String>(),
        "auditor" to emptySet<String>(),
        "api-service" to emptySet<String>()
    )

    /**
     * Check if role A is higher than role B in hierarchy
     */
    fun isHigherRole(roleA: String, roleB: String): Boolean {
        return getInheritedRoles(roleA).contains(roleB)
    }

    /**
     * Get all roles that a role inherits from
     */
    fun getInheritedRoles(roleId: String): Set<String> {
        val inherited = mutableSetOf<String>()
        val toProcess = mutableSetOf(roleId)

        while (toProcess.isNotEmpty()) {
            val current = toProcess.first()
            toProcess.remove(current)

            hierarchy[current]?.forEach { parent ->
                if (!inherited.contains(parent)) {
                    inherited.add(parent)
                    toProcess.add(parent)
                }
            }
        }

        return inherited
    }

    /**
     * Get effective permissions for a role including inherited permissions
     */
    fun getEffectivePermissions(role: Role): Set<Permission> {
        val effectivePermissions = mutableSetOf<Permission>()
        effectivePermissions.addAll(role.permissions)

        // Add permissions from inherited roles
        getInheritedRoles(role.id).forEach { inheritedRoleId ->
            SystemRoles.getRoleById(inheritedRoleId)?.let { inheritedRole ->
                effectivePermissions.addAll(inheritedRole.permissions)
            }
        }

        return effectivePermissions
    }

    /**
     * Validate role assignment (ensure no circular dependencies)
     */
    fun validateRoleAssignment(roleId: String, parentRoleId: String): Boolean {
        return !getInheritedRoles(parentRoleId).contains(roleId)
    }
}