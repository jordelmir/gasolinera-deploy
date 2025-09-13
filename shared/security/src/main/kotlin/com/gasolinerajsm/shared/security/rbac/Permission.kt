package com.gasolinerajsm.shared.security.rbac

/**
 * Comprehensive Permission System for Gasolinera JSM
 * Defines granular permissions for all system operations
 */
enum class Permission(
    val code: String,
    val description: String,
    val category: PermissionCategory,
    val level: PermissionLevel = PermissionLevel.STANDARD
) {
    // User Management Permissions
    USER_CREATE("user:create", "Create new users", PermissionCategory.USER_MANAGEMENT, PermissionLevel.ADMIN),
    USER_READ("user:read", "View user information", PermissionCategory.USER_MANAGEMENT),
    USER_UPDATE("user:update", "Update user information", PermissionCategory.USER_MANAGEMENT),
    USER_DELETE("user:delete", "Delete users", PermissionCategory.USER_MANAGEMENT, PermissionLevel.ADMIN),
    USER_LIST("user:list", "List all users", PermissionCategory.USER_MANAGEMENT),
    USER_SEARCH("user:search", "Search users", PermissionCategory.USER_MANAGEMENT),
    USER_PROFILE_UPDATE("user:profile:update", "Update own profile", PermissionCategory.USER_MANAGEMENT, PermissionLevel.SELF),
    USER_PASSWORD_CHANGE("user:password:change", "Change user password", PermissionCategory.USER_MANAGEMENT),
    USER_ROLE_ASSIGN("user:role:assign", "Assign roles to users", PermissionCategory.USER_MANAGEMENT, PermissionLevel.ADMIN),

    // Station Management Permissions
    STATION_CREATE("station:create", "Create new stations", PermissionCategory.STATION_MANAGEMENT),
    STATION_READ("station:read", "View station information", PermissionCategory.STATION_MANAGEMENT),
    STATION_UPDATE("station:update", "Update station information", PermissionCategory.STATION_MANAGEMENT),
    STATION_DELETE("station:delete", "Delete stations", PermissionCategory.STATION_MANAGEMENT, PermissionLevel.ADMIN),
    STATION_LIST("station:list", "List all stations", PermissionCategory.STATION_MANAGEMENT),
    STATION_SEARCH("station:search", "Search stations", PermissionCategory.STATION_MANAGEMENT),
    STATION_FUEL_PRICE_UPDATE("station:fuel:price:update", "Update fuel prices", PermissionCategory.STATION_MANAGEMENT),
    STATION_INVENTORY_MANAGE("station:inventory:manage", "Manage station inventory", PermissionCategory.STATION_MANAGEMENT),
    STATION_ANALYTICS_VIEW("station:analytics:view", "View station analytics", PermissionCategory.STATION_MANAGEMENT),

    // Campaign Management Permissions
    CAMPAIGN_CREATE("campaign:create", "Create new campaigns", PermissionCategory.CAMPAIGN_MANAGEMENT),
    CAMPAIGN_READ("campaign:read", "View campaign information", PermissionCategory.CAMPAIGN_MANAGEMENT),
    CAMPAIGN_UPDATE("campaign:update", "Update campaign information", PermissionCategory.CAMPAIGN_MANAGEMENT),
    CAMPAIGN_DELETE("campaign:delete", "Delete campaigns", PermissionCategory.CAMPAIGN_MANAGEMENT, PermissionLevel.ADMIN),
    CAMPAIGN_LIST("campaign:list", "List all campaigns", PermissionCategory.CAMPAIGN_MANAGEMENT),
    CAMPAIGN_ACTIVATE("campaign:activate", "Activate/deactivate campaigns", PermissionCategory.CAMPAIGN_MANAGEMENT),
    CAMPAIGN_ANALYTICS_VIEW("campaign:analytics:view", "View campaign analytics", PermissionCategory.CAMPAIGN_MANAGEMENT),

    // Coupon Management Permissions
    COUPON_CREATE("coupon:create", "Create new coupons", PermissionCategory.COUPON_MANAGEMENT),
    COUPON_READ("coupon:read", "View coupon information", PermissionCategory.COUPON_MANAGEMENT),
    COUPON_UPDATE("coupon:update", "Update coupon information", PermissionCategory.COUPON_MANAGEMENT),
    COUPON_DELETE("coupon:delete", "Delete coupons", PermissionCategory.COUPON_MANAGEMENT, PermissionLevel.ADMIN),
    COUPON_LIST("coupon:list", "List all coupons", PermissionCategory.COUPON_MANAGEMENT),
    COUPON_USE("coupon:use", "Use/redeem coupons", PermissionCategory.COUPON_MANAGEMENT),
    COUPON_VALIDATE("coupon:validate", "Validate coupon codes", PermissionCategory.COUPON_MANAGEMENT),
    COUPON_GENERATE("coupon:generate", "Generate coupon codes", PermissionCategory.COUPON_MANAGEMENT),
    COUPON_BULK_OPERATIONS("coupon:bulk", "Perform bulk coupon operations", PermissionCategory.COUPON_MANAGEMENT),

    // Raffle Management Permissions
    RAFFLE_CREATE("raffle:create", "Create new raffles", PermissionCategory.RAFFLE_MANAGEMENT),
    RAFFLE_READ("raffle:read", "View raffle information", PermissionCategory.RAFFLE_MANAGEMENT),
    RAFFLE_UPDATE("raffle:update", "Update raffle information", PermissionCategory.RAFFLE_MANAGEMENT),
    RAFFLE_DELETE("raffle:delete", "Delete raffles", PermissionCategory.RAFFLE_MANAGEMENT, PermissionLevel.ADMIN),
    RAFFLE_LIST("raffle:list", "List all raffles", PermissionCategory.RAFFLE_MANAGEMENT),
    RAFFLE_PARTICIPATE("raffle:participate", "Participate in raffles", PermissionCategory.RAFFLE_MANAGEMENT, PermissionLevel.SELF),
    RAFFLE_DRAW("raffle:draw", "Conduct raffle draws", PermissionCategory.RAFFLE_MANAGEMENT),
    RAFFLE_WINNER_SELECT("raffle:winner:select", "Select raffle winners", PermissionCategory.RAFFLE_MANAGEMENT),
    RAFFLE_PRIZE_DISTRIBUTE("raffle:prize:distribute", "Distribute raffle prizes", PermissionCategory.RAFFLE_MANAGEMENT),

    // Advertisement Management Permissions
    AD_CREATE("ad:create", "Create new advertisements", PermissionCategory.ADVERTISEMENT_MANAGEMENT),
    AD_READ("ad:read", "View advertisement information", PermissionCategory.ADVERTISEMENT_MANAGEMENT),
    AD_UPDATE("ad:update", "Update advertisement information", PermissionCategory.ADVERTISEMENT_MANAGEMENT),
    AD_DELETE("ad:delete", "Delete advertisements", PermissionCategory.ADVERTISEMENT_MANAGEMENT, PermissionLevel.ADMIN),
    AD_LIST("ad:list", "List all advertisements", PermissionCategory.ADVERTISEMENT_MANAGEMENT),
    AD_CAMPAIGN_MANAGE("ad:campaign:manage", "Manage ad campaigns", PermissionCategory.ADVERTISEMENT_MANAGEMENT),
    AD_TARGETING_CONFIGURE("ad:targeting:configure", "Configure ad targeting", PermissionCategory.ADVERTISEMENT_MANAGEMENT),
    AD_ANALYTICS_VIEW("ad:analytics:view", "View ad analytics", PermissionCategory.ADVERTISEMENT_MANAGEMENT),
    AD_ENGAGEMENT_TRACK("ad:engagement:track", "Track ad engagement", PermissionCategory.ADVERTISEMENT_MANAGEMENT),

    // Redemption Management Permissions
    REDEMPTION_CREATE("redemption:create", "Create new redemptions", PermissionCategory.REDEMPTION_MANAGEMENT),
    REDEMPTION_READ("redemption:read", "View redemption information", PermissionCategory.REDEMPTION_MANAGEMENT),
    REDEMPTION_UPDATE("redemption:update", "Update redemption information", PermissionCategory.REDEMPTION_MANAGEMENT),
    REDEMPTION_DELETE("redemption:delete", "Delete redemptions", PermissionCategory.REDEMPTION_MANAGEMENT, PermissionLevel.ADMIN),
    REDEMPTION_LIST("redemption:list", "List all redemptions", PermissionCategory.REDEMPTION_MANAGEMENT),
    REDEMPTION_PROCESS("redemption:process", "Process redemptions", PermissionCategory.REDEMPTION_MANAGEMENT),
    REDEMPTION_VALIDATE("redemption:validate", "Validate redemptions", PermissionCategory.REDEMPTION_MANAGEMENT),
    REDEMPTION_CANCEL("redemption:cancel", "Cancel redemptions", PermissionCategory.REDEMPTION_MANAGEMENT),

    // Analytics and Reporting Permissions
    ANALYTICS_VIEW("analytics:view", "View analytics dashboards", PermissionCategory.ANALYTICS_REPORTING),
    ANALYTICS_EXPORT("analytics:export", "Export analytics data", PermissionCategory.ANALYTICS_REPORTING),
    REPORTS_GENERATE("reports:generate", "Generate reports", PermissionCategory.ANALYTICS_REPORTING),
    REPORTS_SCHEDULE("reports:schedule", "Schedule automated reports", PermissionCategory.ANALYTICS_REPORTING),
    REPORTS_SHARE("reports:share", "Share reports with others", PermissionCategory.ANALYTICS_REPORTING),
    METRICS_VIEW("metrics:view", "View system metrics", PermissionCategory.ANALYTICS_REPORTING),
    DASHBOARD_CUSTOMIZE("dashboard:customize", "Customize dashboards", PermissionCategory.ANALYTICS_REPORTING),

    // System Administration Permissions
    SYSTEM_CONFIG("system:config", "Configure system settings", PermissionCategory.SYSTEM_ADMINISTRATION, PermissionLevel.ADMIN),
    SYSTEM_MONITOR("system:monitor", "Monitor system health", PermissionCategory.SYSTEM_ADMINISTRATION),
    SYSTEM_BACKUP("system:backup", "Perform system backups", PermissionCategory.SYSTEM_ADMINISTRATION, PermissionLevel.ADMIN),
    SYSTEM_RESTORE("system:restore", "Restore system from backup", PermissionCategory.SYSTEM_ADMINISTRATION, PermissionLevel.ADMIN),
    SYSTEM_LOGS_VIEW("system:logs:view", "View system logs", PermissionCategory.SYSTEM_ADMINISTRATION),
    SYSTEM_MAINTENANCE("system:maintenance", "Perform system maintenance", PermissionCategory.SYSTEM_ADMINISTRATION, PermissionLevel.ADMIN),

    // Role and Permission Management
    ROLE_CREATE("role:create", "Create new roles", PermissionCategory.ROLE_PERMISSION_MANAGEMENT, PermissionLevel.ADMIN),
    ROLE_READ("role:read", "View role information", PermissionCategory.ROLE_PERMISSION_MANAGEMENT),
    ROLE_UPDATE("role:update", "Update role information", PermissionCategory.ROLE_PERMISSION_MANAGEMENT, PermissionLevel.ADMIN),
    ROLE_DELETE("role:delete", "Delete roles", PermissionCategory.ROLE_PERMISSION_MANAGEMENT, PermissionLevel.ADMIN),
    ROLE_LIST("role:list", "List all roles", PermissionCategory.ROLE_PERMISSION_MANAGEMENT),
    PERMISSION_VIEW("permission:view", "View permission information", PermissionCategory.ROLE_PERMISSION_MANAGEMENT),
    PERMISSION_ASSIGN("permission:assign", "Assign permissions to roles", PermissionCategory.ROLE_PERMISSION_MANAGEMENT, PermissionLevel.ADMIN),

    // Audit and Security Permissions
    AUDIT_VIEW("audit:view", "View audit logs", PermissionCategory.AUDIT_SECURITY, PermissionLevel.ADMIN),
    AUDIT_EXPORT("audit:export", "Export audit logs", PermissionCategory.AUDIT_SECURITY, PermissionLevel.ADMIN),
    SECURITY_SETTINGS("security:settings", "Configure security settings", PermissionCategory.AUDIT_SECURITY, PermissionLevel.ADMIN),
    SESSION_MANAGE("session:manage", "Manage user sessions", PermissionCategory.AUDIT_SECURITY, PermissionLevel.ADMIN),
    TOKEN_MANAGE("token:manage", "Manage authentication tokens", PermissionCategory.AUDIT_SECURITY, PermissionLevel.ADMIN),

    // Financial Operations Permissions
    FINANCIAL_VIEW("financial:view", "View financial information", PermissionCategory.FINANCIAL_OPERATIONS),
    FINANCIAL_TRANSACTIONS("financial:transactions", "Manage financial transactions", PermissionCategory.FINANCIAL_OPERATIONS),
    FINANCIAL_REPORTS("financial:reports", "Generate financial reports", PermissionCategory.FINANCIAL_OPERATIONS),
    FINANCIAL_RECONCILIATION("financial:reconciliation", "Perform financial reconciliation", PermissionCategory.FINANCIAL_OPERATIONS),

    // Customer Service Permissions
    CUSTOMER_SUPPORT("customer:support", "Provide customer support", PermissionCategory.CUSTOMER_SERVICE),
    CUSTOMER_PROFILE_VIEW("customer:profile:view", "View customer profiles", PermissionCategory.CUSTOMER_SERVICE),
    CUSTOMER_ISSUE_RESOLVE("customer:issue:resolve", "Resolve customer issues", PermissionCategory.CUSTOMER_SERVICE),
    CUSTOMER_COMMUNICATION("customer:communication", "Communicate with customers", PermissionCategory.CUSTOMER_SERVICE);

    companion object {
        /**
         * Get permission by code
         */
        fun fromCode(code: String): Permission? {
            return values().find { it.code == code }
        }

        /**
         * Get permissions by category
         */
        fun byCategory(category: PermissionCategory): List<Permission> {
            return values().filter { it.category == category }
        }

        /**
         * Get permissions by level
         */
        fun byLevel(level: PermissionLevel): List<Permission> {
            return values().filter { it.level == level }
        }

        /**
         * Get all permission codes
         */
        fun getAllCodes(): List<String> {
            return values().map { it.code }
        }
    }
}

/**
 * Permission Categories for organization
 */
enum class PermissionCategory(val displayName: String, val description: String) {
    USER_MANAGEMENT("User Management", "Permissions related to user account management"),
    STATION_MANAGEMENT("Station Management", "Permissions related to gas station operations"),
    CAMPAIGN_MANAGEMENT("Campaign Management", "Permissions related to marketing campaigns"),
    COUPON_MANAGEMENT("Coupon Management", "Permissions related to coupon operations"),
    RAFFLE_MANAGEMENT("Raffle Management", "Permissions related to raffle operations"),
    ADVERTISEMENT_MANAGEMENT("Advertisement Management", "Permissions related to advertisement operations"),
    REDEMPTION_MANAGEMENT("Redemption Management", "Permissions related to redemption operations"),
    ANALYTICS_REPORTING("Analytics & Reporting", "Permissions related to analytics and reporting"),
    SYSTEM_ADMINISTRATION("System Administration", "Permissions related to system administration"),
    ROLE_PERMISSION_MANAGEMENT("Role & Permission Management", "Permissions related to role and permission management"),
    AUDIT_SECURITY("Audit & Security", "Permissions related to audit and security"),
    FINANCIAL_OPERATIONS("Financial Operations", "Permissions related to financial operations"),
    CUSTOMER_SERVICE("Customer Service", "Permissions related to customer service operations")
}

/**
 * Permission Levels for access control
 */
enum class PermissionLevel(val displayName: String, val description: String) {
    SELF("Self", "Permission applies only to own resources"),
    STANDARD("Standard", "Standard permission level"),
    ADMIN("Admin", "Administrative permission level requiring elevated access")
}

/**
 * Permission validation and utility functions
 */
object PermissionUtils {

    /**
     * Check if a set of permissions includes a specific permission
     */
    fun hasPermission(userPermissions: Set<String>, requiredPermission: Permission): Boolean {
        return userPermissions.contains(requiredPermission.code)
    }

    /**
     * Check if a set of permissions includes any of the required permissions
     */
    fun hasAnyPermission(userPermissions: Set<String>, requiredPermissions: Set<Permission>): Boolean {
        return requiredPermissions.any { userPermissions.contains(it.code) }
    }

    /**
     * Check if a set of permissions includes all required permissions
     */
    fun hasAllPermissions(userPermissions: Set<String>, requiredPermissions: Set<Permission>): Boolean {
        return requiredPermissions.all { userPermissions.contains(it.code) }
    }

    /**
     * Get permissions by resource type
     */
    fun getResourcePermissions(resource: String): List<Permission> {
        return Permission.values().filter { it.code.startsWith("$resource:") }
    }

    /**
     * Validate permission hierarchy
     */
    fun validatePermissionHierarchy(permissions: Set<String>): Boolean {
        // Add validation logic for permission dependencies
        return true
    }

    /**
     * Get implied permissions (permissions that are automatically granted with others)
     */
    fun getImpliedPermissions(permissions: Set<String>): Set<String> {
        val implied = mutableSetOf<String>()

        // If user has delete permission, they also have read and update
        permissions.forEach { permission ->
            when {
                permission.endsWith(":delete") -> {
                    val base = permission.substringBeforeLast(":")
                    implied.add("$base:read")
                    implied.add("$base:update")
                }
                permission.endsWith(":update") -> {
                    val base = permission.substringBeforeLast(":")
                    implied.add("$base:read")
                }
            }
        }

        return implied
    }
}