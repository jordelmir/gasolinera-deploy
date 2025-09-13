package com.gasolinerajsm.shared.security.rbac

import org.springframework.security.access.prepost.PreAuthorize
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * Security Annotations for RBAC
 * Provides declarative security for methods and classes
 */

/**
 * Requires specific permission
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authorizationService.hasPermission(T(com.gasolinerajsm.shared.security.rbac.Permission).valueOf(#permission))")
annotation class RequirePermission(val permission: String)

/**
 * Requires any of the specified permissions
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authorizationService.hasAnyPermission(#permissions)")
annotation class RequireAnyPermission(vararg val permissions: String)

/**
 * Requires all specified permissions
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authorizationService.hasAllPermissions(#permissions)")
annotation class RequireAllPermissions(vararg val permissions: String)

/**
 * Requires specific role
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole(#role)")
annotation class RequireRole(val role: String)

/**
 * Requires any of the specified roles
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole(#roles)")
annotation class RequireAnyRole(vararg val roles: String)

/**
 * Requires resource access with specific permission
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authorizationService.hasResourceAccess(#resourceType, #resourceId, T(com.gasolinerajsm.shared.security.rbac.Permission).valueOf(#permission))")
annotation class RequireResourceAccess(
    val resourceType: String,
    val resourceId: String,
    val permission: String
)

/**
 * Allows access only to own resources
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authorizationService.isOwner(#resourceId) or hasRole('ADMIN')")
annotation class RequireOwnership(val resourceId: String)

/**
 * Station-specific access control
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authorizationService.hasStationAccess(#stationId)")
annotation class RequireStationAccess(val stationId: String)

/**
 * Business hours restriction
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@authorizationService.isBusinessHours() or hasRole('ADMIN')")
annotation class RequireBusinessHours

/**
 * Rate limiting annotation
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    val requests: Int = 100,
    val window: String = "1m", // 1 minute
    val scope: RateLimitScope = RateLimitScope.USER
)

enum class RateLimitScope {
    USER, IP, GLOBAL
}

/**
 * Audit annotation for tracking sensitive operations
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Audited(
    val action: String = "",
    val resourceType: String = "",
    val sensitive: Boolean = false
)

/**
 * Predefined security annotations for common operations
 */

// User Management
@RequirePermission("USER_CREATE")
annotation class CanCreateUsers

@RequirePermission("USER_READ")
annotation class CanReadUsers

@RequirePermission("USER_UPDATE")
annotation class CanUpdateUsers

@RequirePermission("USER_DELETE")
annotation class CanDeleteUsers

// Station Management
@RequirePermission("STATION_CREATE")
annotation class CanCreateStations

@RequirePermission("STATION_READ")
annotation class CanReadStations

@RequirePermission("STATION_UPDATE")
annotation class CanUpdateStations

@RequirePermission("STATION_DELETE")
annotation class CanDeleteStations

// Campaign Management
@RequirePermission("CAMPAIGN_CREATE")
annotation class CanCreateCampaigns

@RequirePermission("CAMPAIGN_READ")
annotation class CanReadCampaigns

@RequirePermission("CAMPAIGN_UPDATE")
annotation class CanUpdateCampaigns

@RequirePermission("CAMPAIGN_DELETE")
annotation class CanDeleteCampaigns

// Coupon Management
@RequirePermission("COUPON_CREATE")
annotation class CanCreateCoupons

@RequirePermission("COUPON_READ")
annotation class CanReadCoupons

@RequirePermission("COUPON_UPDATE")
annotation class CanUpdateCoupons

@RequirePermission("COUPON_DELETE")
annotation class CanDeleteCoupons

@RequirePermission("COUPON_USE")
annotation class CanUseCoupons

// Raffle Management
@RequirePermission("RAFFLE_CREATE")
annotation class CanCreateRaffles

@RequirePermission("RAFFLE_READ")
annotation class CanReadRaffles

@RequirePermission("RAFFLE_UPDATE")
annotation class CanUpdateRaffles

@RequirePermission("RAFFLE_DELETE")
annotation class CanDeleteRaffles

@RequirePermission("RAFFLE_DRAW")
annotation class CanDrawRaffles

// Analytics and Reporting
@RequirePermission("ANALYTICS_VIEW")
annotation class CanViewAnalytics

@RequirePermission("REPORTS_GENERATE")
annotation class CanGenerateReports

// System Administration
@RequirePermission("SYSTEM_CONFIG")
annotation class CanConfigureSystem

@RequirePermission("SYSTEM_MONITOR")
annotation class CanMonitorSystem

// Role Management
@RequirePermission("ROLE_CREATE")
annotation class CanCreateRoles

@RequirePermission("ROLE_UPDATE")
annotation class CanUpdateRoles

@RequirePermission("ROLE_DELETE")
annotation class CanDeleteRoles

// Combined annotations for common scenarios
@RequireAnyRole("ADMIN", "MANAGER")
annotation class RequireManagementRole

@RequireAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
annotation class RequireStaffRole

@RequireAnyPermission("USER_READ", "USER_LIST")
annotation class CanAccessUsers

@RequireAnyPermission("STATION_READ", "STATION_LIST")
annotation class CanAccessStations

/**
 * Security configuration for method-level security
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SecuredController(
    val defaultRole: String = "",
    val auditAll: Boolean = false
)

/**
 * Public endpoint (no authentication required)
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEndpoint

/**
 * Internal service endpoint (service-to-service only)
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RequireRole("API_SERVICE")
annotation class InternalEndpoint