package com.gasolinerajsm.shared.security.rbac

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.context.annotation.Import
import org.slf4j.LoggerFactory

/**
 * RBAC Configuration
 * Configures Role-Based Access Control system
 */
@Configuration
@EnableConfigurationProperties(RbacProperties::class)
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableScheduling
@Import(SecurityMethodConfiguration::class)
class RbacConfiguration(
    private val rbacProperties: RbacProperties,
    private val authorizationInterceptor: AuthorizationInterceptor,
    private val rateLimitService: RateLimitService
) : WebMvcConfigurer {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacConfiguration::class.java)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        if (rbacProperties.interceptor.enabled) {
            registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns(rbacProperties.interceptor.includePatterns)
                .excludePathPatterns(rbacProperties.interceptor.excludePatterns)
                .order(rbacProperties.interceptor.order)

            logger.info("Authorization interceptor registered with patterns: include={}, exclude={}",
                rbacProperties.interceptor.includePatterns, rbacProperties.interceptor.excludePatterns)
        }
    }

    @Bean
    fun roleRepository(): RoleRepository {
        return InMemoryRoleRepository()
    }

    /**
     * Scheduled cleanup for rate limiting cache
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    fun cleanupRateLimitCache() {
        if (rbacProperties.rateLimit.enabled) {
            rateLimitService.cleanup()
        }
    }

    /**
     * Scheduled role cache refresh
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    fun refreshRoleCache() {
        if (rbacProperties.cache.enabled) {
            try {
                // This would be injected in a real implementation
                // roleService.refreshCache()
                logger.debug("Role cache refresh completed")
            } catch (ex: Exception) {
                logger.warn("Failed to refresh role cache", ex)
            }
        }
    }
}

/**
 * RBAC Properties Configuration
 */
@ConfigurationProperties(prefix = "rbac")
data class RbacProperties(
    val enabled: Boolean = true,
    val interceptor: InterceptorProperties = InterceptorProperties(),
    val rateLimit: RateLimitProperties = RateLimitProperties(),
    val cache: CacheProperties = CacheProperties(),
    val audit: AuditProperties = AuditProperties(),
    val businessHours: BusinessHoursProperties = BusinessHoursProperties()
) {

    data class InterceptorProperties(
        val enabled: Boolean = true,
        val order: Int = 100,
        val includePatterns: List<String> = listOf("/api/**"),
        val excludePatterns: List<String> = listOf(
            "/api/v1/auth/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
        )
    )

    data class RateLimitProperties(
        val enabled: Boolean = true,
        val defaultLimit: Int = 100,
        val defaultWindow: String = "1m",
        val cleanupInterval: Long = 300000 // 5 minutes
    )

    data class CacheProperties(
        val enabled: Boolean = true,
        val ttl: Long = 1800000, // 30 minutes
        val maxSize: Int = 1000
    )

    data class AuditProperties(
        val enabled: Boolean = true,
        val logLevel: String = "INFO",
        val includeSuccessful: Boolean = false,
        val includeFailed: Boolean = true,
        val sensitiveOperations: List<String> = listOf(
            "CREATE_ROLE", "UPDATE_ROLE", "DELETE_ROLE",
            "ASSIGN_ROLE", "REMOVE_ROLE",
            "CREATE_USER", "DELETE_USER"
        )
    )

    data class BusinessHoursProperties(
        val enabled: Boolean = false,
        val startHour: Int = 8,
        val endHour: Int = 18,
        val timezone: String = "UTC",
        val weekendsAllowed: Boolean = false
    )
}

/**
 * Security Method Configuration
 * Provides method-level security beans
 */
@Configuration
class SecurityMethodConfiguration {

    @Bean
    fun methodSecurityExpressionHandler(): org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler {
        val handler = org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler()
        handler.setPermissionEvaluator(CustomPermissionEvaluator())
        return handler
    }
}

/**
 * Custom Permission Evaluator for SpEL expressions
 */
class CustomPermissionEvaluator : org.springframework.security.access.PermissionEvaluator {

    override fun hasPermission(
        authentication: org.springframework.security.core.Authentication?,
        targetDomainObject: Any?,
        permission: Any?
    ): Boolean {
        if (authentication == null || !authentication.isAuthenticated) {
            return false
        }

        val principal = authentication.principal
        if (principal !is com.gasolinerajsm.shared.security.model.UserPrincipal) {
            return false
        }

        return when (permission) {
            is String -> {
                val perm = Permission.fromCode(permission)
                perm != null && hasPermissionInternal(principal, perm)
            }
            is Permission -> hasPermissionInternal(principal, permission)
            else -> false
        }
    }

    override fun hasPermission(
        authentication: org.springframework.security.core.Authentication?,
        targetId: java.io.Serializable?,
        targetType: String?,
        permission: Any?
    ): Boolean {
        if (authentication == null || !authentication.isAuthenticated) {
            return false
        }

        val principal = authentication.principal
        if (principal !is com.gasolinerajsm.shared.security.model.UserPrincipal) {
            return false
        }

        val perm = when (permission) {
            is String -> Permission.fromCode(permission)
            is Permission -> permission
            else -> return false
        } ?: return false

        // Basic permission check
        if (!hasPermissionInternal(principal, perm)) {
            return false
        }

        // Resource-specific checks
        if (targetType != null && targetId != null) {
            return hasResourceAccess(principal, targetType, targetId.toString(), perm)
        }

        return true
    }

    private fun hasPermissionInternal(principal: com.gasolinerajsm.shared.security.model.UserPrincipal, permission: Permission): Boolean {
        val userPermissions = getUserEffectivePermissions(principal)
        return PermissionUtils.hasPermission(userPermissions, permission)
    }

    private fun getUserEffectivePermissions(principal: com.gasolinerajsm.shared.security.model.UserPrincipal): Set<String> {
        val allPermissions = mutableSetOf<String>()

        principal.roles.forEach { roleName ->
            val role = SystemRoles.getRoleByName(roleName)
            if (role != null) {
                val effectivePermissions = RoleHierarchy.getEffectivePermissions(role)
                allPermissions.addAll(effectivePermissions.map { it.code })
            }
        }

        return allPermissions
    }

    private fun hasResourceAccess(
        principal: com.gasolinerajsm.shared.security.model.UserPrincipal,
        resourceType: String,
        resourceId: String,
        permission: Permission
    ): Boolean {
        return when (resourceType.lowercase()) {
            "station" -> hasStationAccess(principal, resourceId)
            "user" -> hasUserAccess(principal, resourceId, permission)
            else -> true
        }
    }

    private fun hasStationAccess(principal: com.gasolinerajsm.shared.security.model.UserPrincipal, stationId: String): Boolean {
        return when {
            principal.hasAnyRole("ADMIN", "SUPER_ADMIN", "MANAGER") -> true
            principal.hasRole("EMPLOYEE") -> principal.stationId == stationId
            else -> false
        }
    }

    private fun hasUserAccess(
        principal: com.gasolinerajsm.shared.security.model.UserPrincipal,
        userId: String,
        permission: Permission
    ): Boolean {
        // Users can access their own data
        if (principal.id == userId && permission.level == PermissionLevel.SELF) {
            return true
        }

        // Admin level required for other users
        return principal.hasAnyRole("ADMIN", "SUPER_ADMIN")
    }
}

/**
 * In-Memory Role Repository Implementation
 * For demonstration purposes - replace with actual database implementation
 */
class InMemoryRoleRepository : RoleRepository {

    private val roles = mutableMapOf<String, Role>()
    private val userRoles = mutableMapOf<String, MutableSet<String>>()

    init {
        // Initialize with system roles
        SystemRoles.getAllSystemRoles().forEach { role ->
            roles[role.id] = role
        }
    }

    override fun save(role: Role): Role {
        roles[role.id] = role
        return role
    }

    override fun findById(id: String): Role? {
        return roles[id]
    }

    override fun findByName(name: String): Role? {
        return roles.values.find { it.name == name }
    }

    override fun findAll(): List<Role> {
        return roles.values.toList()
    }

    override fun findByIsActive(isActive: Boolean): List<Role> {
        return roles.values.filter { it.isActive == isActive }
    }

    override fun findByIsSystemRole(isSystemRole: Boolean): List<Role> {
        return roles.values.filter { it.isSystemRole == isSystemRole }
    }

    override fun deleteById(id: String) {
        roles.remove(id)
    }

    override fun existsById(id: String): Boolean {
        return roles.containsKey(id)
    }

    override fun existsByName(name: String): Boolean {
        return roles.values.any { it.name == name }
    }

    override fun searchByNameOrDescription(query: String): List<Role> {
        val lowerQuery = query.lowercase()
        return roles.values.filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery)
        }
    }

    override fun isRoleAssigned(roleId: String): Boolean {
        return userRoles.values.any { it.contains(roleId) }
    }

    override fun assignRoleToUser(userId: String, roleId: String) {
        userRoles.computeIfAbsent(userId) { mutableSetOf() }.add(roleId)
    }

    override fun removeRoleFromUser(userId: String, roleId: String) {
        userRoles[userId]?.remove(roleId)
    }

    override fun getUserRoleIds(userId: String): List<String> {
        return userRoles[userId]?.toList() ?: emptyList()
    }

    override fun findMostUsedRole(): String? {
        return userRoles.values
            .flatMap { it }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    override fun findLeastUsedRole(): String? {
        return userRoles.values
            .flatMap { it }
            .groupingBy { it }
            .eachCount()
            .minByOrNull { it.value }
            ?.key
    }
}