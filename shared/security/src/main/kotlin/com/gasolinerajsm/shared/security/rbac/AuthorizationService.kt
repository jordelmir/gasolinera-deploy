package com.gasolinerajsm.shared.security.rbac

import com.gasolinerajsm.shared.security.model.UserPrincipal
import com.gasolinerajsm.shared.security.model.SecurityContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Authorization Service for RBAC
 * Handles permission checking and access control decisions
 */
@Service
class AuthorizationService(
    private val roleService: RoleService,
    private val auditService: AuditService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthorizationService::class.java)
    }

    /**
     * Check if current user has specific permission
     */
    fun hasPermission(permission: Permission): Boolean {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return false

        return hasPermission(currentUser, permission)
    }

    /**
     * Check if user has specific permission
     */
    fun hasPermission(user: UserPrincipal, permission: Permission): Boolean {
        val userPermissions = getUserEffectivePermissions(user)
        val hasAccess = PermissionUtils.hasPermission(userPermissions, permission)

        // Audit permission check
        auditService.logPermissionCheck(
            userId = user.id,
            permission = permission.code,
            granted = hasAccess,
            timestamp = LocalDateTime.now()
        )

        return hasAccess
    }

    /**
     * Check if current user has any of the specified permissions
     */
    fun hasAnyPermission(vararg permissions: Permission): Boolean {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return false

        return hasAnyPermission(currentUser, permissions.toSet())
    }

    /**
     * Check if user has any of the specified permissions
     */
    fun hasAnyPermission(user: UserPrincipal, permissions: Set<Permission>): Boolean {
        val userPermissions = getUserEffectivePermissions(user)
        return PermissionUtils.hasAnyPermission(userPermissions, permissions)
    }

    /**
     * Check if current user has all specified permissions
     */
    fun hasAllPermissions(vararg permissions: Permission): Boolean {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return false

        return hasAllPermissions(currentUser, permissions.toSet())
    }

    /**
     * Check if user has all specified permissions
     */
    fun hasAllPermissions(user: UserPrincipal, permissions: Set<Permission>): Boolean {
        val userPermissions = getUserEffectivePermissions(user)
        return PermissionUtils.hasAllPermissions(userPermissions, permissions)
    }

    /**
     * Check if current user has access to specific resource
     */
    fun hasResourceAccess(resourceType: String, resourceId: String, permission: Permission): Boolean {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return false

        return hasResourceAccess(currentUser, resourceType, resourceId, permission)
    }

    /**
     * Check if user has access to specific resource
     */
    fun hasResourceAccess(
        user: UserPrincipal,
        resourceType: String,
        resourceId: String,
        permission: Permission
    ): Boolean {
        // First check if user has the basic permission
        if (!hasPermission(user, permission)) {
            return false
        }

        // Apply resource-specific access control
        return when (resourceType.lowercase()) {
            "station" -> hasStationAccess(user, resourceId, permission)
            "user" -> hasUserAccess(user, resourceId, permission)
            "campaign" -> hasCampaignAccess(user, resourceId, permission)
            "coupon" -> hasCouponAccess(user, resourceId, permission)
            "raffle" -> hasRaffleAccess(user, resourceId, permission)
            else -> true // Default allow if no specific rules
        }
    }

    /**
     * Check station-specific access
     */
    private fun hasStationAccess(user: UserPrincipal, stationId: String, permission: Permission): Boolean {
        // Station employees can only access their assigned station
        if (user.hasRole("EMPLOYEE") && user.stationId != null) {
            return user.stationId == stationId
        }

        // Managers and above can access all stations
        return user.hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")
    }

    /**
     * Check user-specific access
     */
    private fun hasUserAccess(user: UserPrincipal, targetUserId: String, permission: Permission): Boolean {
        // Users can always access their own profile
        if (user.id == targetUserId && permission.level == PermissionLevel.SELF) {
            return true
        }

        // Admin level permissions required for other users
        return permission.level != PermissionLevel.SELF || user.hasAnyRole("ADMIN", "SUPER_ADMIN")
    }

    /**
     * Check campaign-specific access
     */
    private fun hasCampaignAccess(user: UserPrincipal, campaignId: String, permission: Permission): Boolean {
        // Station-specific campaigns
        if (user.hasRole("EMPLOYEE") && user.stationId != null) {
            // Would need to check if campaign is associated with user's station
            return true // Simplified for now
        }

        return true
    }

    /**
     * Check coupon-specific access
     */
    private fun hasCouponAccess(user: UserPrincipal, couponId: String, permission: Permission): Boolean {
        // Customers can only use coupons, not manage them
        if (user.hasRole("CUSTOMER") && permission == Permission.COUPON_USE) {
            return true
        }

        return !user.hasRole("CUSTOMER")
    }

    /**
     * Check raffle-specific access
     */
    private fun hasRaffleAccess(user: UserPrincipal, raffleId: String, permission: Permission): Boolean {
        // Anyone can participate in raffles
        if (permission == Permission.RAFFLE_PARTICIPATE) {
            return true
        }

        // Only managers and above can manage raffles
        return user.hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")
    }

    /**
     * Get all effective permissions for a user
     */
    fun getUserEffectivePermissions(user: UserPrincipal): Set<String> {
        val allPermissions = mutableSetOf<String>()

        // Get permissions from all user roles
        user.roles.forEach { roleName ->
            val role = SystemRoles.getRoleByName(roleName)
            if (role != null) {
                val effectivePermissions = RoleHierarchy.getEffectivePermissions(role)
                allPermissions.addAll(effectivePermissions.map { it.code })
            }
        }

        // Add implied permissions
        val impliedPermissions = PermissionUtils.getImpliedPermissions(allPermissions)
        allPermissions.addAll(impliedPermissions)

        return allPermissions
    }

    /**
     * Check if user can perform action on resource
     */
    fun canPerformAction(
        action: String,
        resourceType: String,
        resourceId: String? = null,
        context: Map<String, Any> = emptyMap()
    ): AuthorizationResult {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return AuthorizationResult.denied("No authenticated user")

        return canPerformAction(currentUser, action, resourceType, resourceId, context)
    }

    /**
     * Check if user can perform action on resource with detailed result
     */
    fun canPerformAction(
        user: UserPrincipal,
        action: String,
        resourceType: String,
        resourceId: String? = null,
        context: Map<String, Any> = emptyMap()
    ): AuthorizationResult {
        try {
            // Find required permission for action
            val permission = findPermissionForAction(action, resourceType)
                ?: return AuthorizationResult.denied("Unknown action: $action on $resourceType")

            // Check basic permission
            if (!hasPermission(user, permission)) {
                return AuthorizationResult.denied("Insufficient permissions: ${permission.code}")
            }

            // Check resource-specific access if resource ID provided
            if (resourceId != null && !hasResourceAccess(user, resourceType, resourceId, permission)) {
                return AuthorizationResult.denied("No access to resource: $resourceType:$resourceId")
            }

            // Apply context-specific rules
            val contextResult = applyContextualRules(user, action, resourceType, resourceId, context)
            if (!contextResult.allowed) {
                return contextResult
            }

            return AuthorizationResult.allowed("Access granted")

        } catch (ex: Exception) {
            logger.error("Error during authorization check", ex)
            return AuthorizationResult.denied("Authorization check failed: ${ex.message}")
        }
    }

    /**
     * Find permission required for specific action
     */
    private fun findPermissionForAction(action: String, resourceType: String): Permission? {
        val permissionCode = "$resourceType:$action"
        return Permission.fromCode(permissionCode)
    }

    /**
     * Apply contextual authorization rules
     */
    private fun applyContextualRules(
        user: UserPrincipal,
        action: String,
        resourceType: String,
        resourceId: String?,
        context: Map<String, Any>
    ): AuthorizationResult {
        // Time-based restrictions
        val currentHour = LocalDateTime.now().hour
        if (context["requireBusinessHours"] == true && (currentHour < 8 || currentHour > 18)) {
            if (!user.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
                return AuthorizationResult.denied("Action not allowed outside business hours")
            }
        }

        // IP-based restrictions
        val clientIp = context["clientIp"] as? String
        if (clientIp != null && isRestrictedIp(clientIp)) {
            return AuthorizationResult.denied("Access denied from IP: $clientIp")
        }

        // Rate limiting context
        val rateLimitExceeded = context["rateLimitExceeded"] as? Boolean ?: false
        if (rateLimitExceeded) {
            return AuthorizationResult.denied("Rate limit exceeded")
        }

        return AuthorizationResult.allowed()
    }

    /**
     * Check if IP is in restricted list
     */
    private fun isRestrictedIp(ip: String): Boolean {
        // Implement IP restriction logic
        return false
    }

    /**
     * Get user's accessible resources of a specific type
     */
    fun getAccessibleResources(user: UserPrincipal, resourceType: String): Set<String> {
        return when (resourceType.lowercase()) {
            "station" -> getAccessibleStations(user)
            "campaign" -> getAccessibleCampaigns(user)
            else -> emptySet()
        }
    }

    /**
     * Get stations accessible to user
     */
    private fun getAccessibleStations(user: UserPrincipal): Set<String> {
        return when {
            user.hasAnyRole("ADMIN", "SUPER_ADMIN") -> setOf("*") // All stations
            user.hasRole("MANAGER") -> setOf("*") // All stations for managers
            user.hasRole("EMPLOYEE") && user.stationId != null -> setOf(user.stationId!!)
            else -> emptySet()
        }
    }

    /**
     * Get campaigns accessible to user
     */
    private fun getAccessibleCampaigns(user: UserPrincipal): Set<String> {
        return when {
            user.hasAnyRole("ADMIN", "SUPER_ADMIN", "MANAGER") -> setOf("*")
            else -> emptySet()
        }
    }
}

/**
 * Authorization result with detailed information
 */
data class AuthorizationResult(
    val allowed: Boolean,
    val reason: String? = null,
    val requiredPermissions: Set<String> = emptySet(),
    val context: Map<String, Any> = emptyMap()
) {
    companion object {
        fun allowed(reason: String? = null) = AuthorizationResult(true, reason)
        fun denied(reason: String) = AuthorizationResult(false, reason)
    }
}

/**
 * Audit service for tracking authorization events
 */
@Service
class AuditService {

    companion object {
        private val logger = LoggerFactory.getLogger(AuditService::class.java)
    }

    fun logPermissionCheck(
        userId: String,
        permission: String,
        granted: Boolean,
        timestamp: LocalDateTime,
        resourceType: String? = null,
        resourceId: String? = null
    ) {
        // Log to audit system
        logger.info(
            "Permission check: user={}, permission={}, granted={}, resource={}:{}, timestamp={}",
            userId, permission, granted, resourceType, resourceId, timestamp
        )

        // In a real implementation, this would write to an audit database
    }

    fun logAccessAttempt(
        userId: String,
        action: String,
        resourceType: String,
        resourceId: String?,
        result: AuthorizationResult,
        timestamp: LocalDateTime
    ) {
        logger.info(
            "Access attempt: user={}, action={}, resource={}:{}, allowed={}, reason={}, timestamp={}",
            userId, action, resourceType, resourceId, result.allowed, result.reason, timestamp
        )
    }
}