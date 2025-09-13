package com.gasolinerajsm.shared.security.rbac

import com.gasolinerajsm.shared.security.model.SecurityContextHolder
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.time.LocalDateTime

/**
 * Authorization Interceptor
 * Intercepts requests to perform authorization checks based on annotations
 */
@Component
class AuthorizationInterceptor(
    private val authorizationService: AuthorizationService,
    private val auditService: AuditService,
    private val rateLimitService: RateLimitService
) : HandlerInterceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthorizationInterceptor::class.java)
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (handler !is HandlerMethod) {
            return true
        }

        val currentUser = SecurityContextHolder.getCurrentUser()
        val startTime = System.currentTimeMillis()

        try {
            // Check if endpoint is public
            if (isPublicEndpoint(handler)) {
                return true
            }

            // Require authentication for non-public endpoints
            if (currentUser == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")
                return false
            }

            // Check rate limiting
            if (!checkRateLimit(handler, request, currentUser.id)) {
                response.sendError(HttpServletResponse.SC_TOO_MANY_REQUESTS, "Rate limit exceeded")
                return false
            }

            // Check business hours restriction
            if (!checkBusinessHours(handler, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied outside business hours")
                return false
            }

            // Check authorization
            val authResult = checkAuthorization(handler, request, currentUser.id)
            if (!authResult.allowed) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, authResult.reason ?: "Access denied")
                return false
            }

            // Store authorization context for auditing
            request.setAttribute("authorizationResult", authResult)
            request.setAttribute("authStartTime", startTime)

            return true

        } catch (ex: Exception) {
            logger.error("Authorization check failed", ex)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authorization check failed")
            return false
        }
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        if (handler !is HandlerMethod) {
            return
        }

        val currentUser = SecurityContextHolder.getCurrentUser()
        val authResult = request.getAttribute("authorizationResult") as? AuthorizationResult
        val startTime = request.getAttribute("authStartTime") as? Long

        if (currentUser != null && shouldAudit(handler)) {
            val duration = if (startTime != null) System.currentTimeMillis() - startTime else 0

            auditService.logAccessAttempt(
                userId = currentUser.id,
                action = getActionFromMethod(handler),
                resourceType = getResourceTypeFromPath(request.requestURI),
                resourceId = extractResourceId(request),
                result = authResult ?: AuthorizationResult.allowed(),
                timestamp = LocalDateTime.now()
            )

            // Log performance metrics
            if (duration > 100) { // Log slow authorization checks
                logger.warn("Slow authorization check: {}ms for user {} on {}",
                    duration, currentUser.id, request.requestURI)
            }
        }
    }

    /**
     * Check if endpoint is public
     */
    private fun isPublicEndpoint(handler: HandlerMethod): Boolean {
        return handler.hasMethodAnnotation(PublicEndpoint::class.java) ||
               handler.beanType.isAnnotationPresent(PublicEndpoint::class.java)
    }

    /**
     * Check rate limiting
     */
    private fun checkRateLimit(
        handler: HandlerMethod,
        request: HttpServletRequest,
        userId: String
    ): Boolean {
        val rateLimitAnnotation = handler.getMethodAnnotation(RateLimit::class.java)
            ?: handler.beanType.getAnnotation(RateLimit::class.java)
            ?: return true

        val key = when (rateLimitAnnotation.scope) {
            RateLimitScope.USER -> "user:$userId"
            RateLimitScope.IP -> "ip:${request.remoteAddr}"
            RateLimitScope.GLOBAL -> "global"
        }

        return rateLimitService.isAllowed(
            key = key,
            limit = rateLimitAnnotation.requests,
            window = rateLimitAnnotation.window
        )
    }

    /**
     * Check business hours restriction
     */
    private fun checkBusinessHours(handler: HandlerMethod, currentUser: com.gasolinerajsm.shared.security.model.UserPrincipal): Boolean {
        val requireBusinessHours = handler.hasMethodAnnotation(RequireBusinessHours::class.java) ||
                                  handler.beanType.isAnnotationPresent(RequireBusinessHours::class.java)

        if (!requireBusinessHours) {
            return true
        }

        // Admins can access anytime
        if (currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return true
        }

        val currentHour = LocalDateTime.now().hour
        return currentHour in 8..18 // Business hours: 8 AM to 6 PM
    }

    /**
     * Check authorization based on method annotations
     */
    private fun checkAuthorization(
        handler: HandlerMethod,
        request: HttpServletRequest,
        userId: String
    ): AuthorizationResult {
        val currentUser = SecurityContextHolder.getCurrentUser()!!

        // Check permission-based annotations
        handler.getMethodAnnotation(RequirePermission::class.java)?.let { annotation ->
            val permission = Permission.fromCode(annotation.permission)
                ?: return AuthorizationResult.denied("Invalid permission: ${annotation.permission}")

            if (!authorizationService.hasPermission(currentUser, permission)) {
                return AuthorizationResult.denied("Missing permission: ${annotation.permission}")
            }
        }

        // Check any permission annotation
        handler.getMethodAnnotation(RequireAnyPermission::class.java)?.let { annotation ->
            val permissions = annotation.permissions.mapNotNull { Permission.fromCode(it) }.toSet()
            if (!authorizationService.hasAnyPermission(currentUser, permissions)) {
                return AuthorizationResult.denied("Missing any of permissions: ${annotation.permissions.joinToString()}")
            }
        }

        // Check all permissions annotation
        handler.getMethodAnnotation(RequireAllPermissions::class.java)?.let { annotation ->
            val permissions = annotation.permissions.mapNotNull { Permission.fromCode(it) }.toSet()
            if (!authorizationService.hasAllPermissions(currentUser, permissions)) {
                return AuthorizationResult.denied("Missing all permissions: ${annotation.permissions.joinToString()}")
            }
        }

        // Check resource access annotation
        handler.getMethodAnnotation(RequireResourceAccess::class.java)?.let { annotation ->
            val resourceId = resolveResourceId(annotation.resourceId, request)
            val permission = Permission.fromCode(annotation.permission)
                ?: return AuthorizationResult.denied("Invalid permission: ${annotation.permission}")

            if (!authorizationService.hasResourceAccess(currentUser, annotation.resourceType, resourceId, permission)) {
                return AuthorizationResult.denied("No access to resource: ${annotation.resourceType}:$resourceId")
            }
        }

        // Check ownership annotation
        handler.getMethodAnnotation(RequireOwnership::class.java)?.let { annotation ->
            val resourceId = resolveResourceId(annotation.resourceId, request)
            if (!isOwner(currentUser, resourceId) && !currentUser.hasRole("ADMIN")) {
                return AuthorizationResult.denied("Not owner of resource: $resourceId")
            }
        }

        // Check station access annotation
        handler.getMethodAnnotation(RequireStationAccess::class.java)?.let { annotation ->
            val stationId = resolveResourceId(annotation.stationId, request)
            if (!hasStationAccess(currentUser, stationId)) {
                return AuthorizationResult.denied("No access to station: $stationId")
            }
        }

        return AuthorizationResult.allowed("Authorization successful")
    }

    /**
     * Resolve resource ID from annotation parameter
     */
    private fun resolveResourceId(resourceIdExpression: String, request: HttpServletRequest): String {
        return when {
            resourceIdExpression.startsWith("#") -> {
                // Extract from path variable
                val paramName = resourceIdExpression.substring(1)
                extractPathVariable(request, paramName) ?: resourceIdExpression
            }
            resourceIdExpression.startsWith("@") -> {
                // Extract from request parameter
                val paramName = resourceIdExpression.substring(1)
                request.getParameter(paramName) ?: resourceIdExpression
            }
            else -> resourceIdExpression
        }
    }

    /**
     * Extract path variable from request
     */
    private fun extractPathVariable(request: HttpServletRequest, paramName: String): String? {
        // This would typically use Spring's path variable resolution
        // For now, extract from URI pattern
        val uri = request.requestURI
        val segments = uri.split("/")

        return when (paramName) {
            "userId" -> segments.find { it.matches(Regex("[a-f0-9-]{36}")) } // UUID pattern
            "stationId" -> segments.find { it.startsWith("station-") }
            "roleId" -> segments.find { it.matches(Regex("[a-z-]+")) }
            else -> null
        }
    }

    /**
     * Check if user is owner of resource
     */
    private fun isOwner(currentUser: com.gasolinerajsm.shared.security.model.UserPrincipal, resourceId: String): Boolean {
        // For user resources, check if it's the same user
        return currentUser.id == resourceId
    }

    /**
     * Check if user has access to station
     */
    private fun hasStationAccess(currentUser: com.gasolinerajsm.shared.security.model.UserPrincipal, stationId: String): Boolean {
        return when {
            currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN", "MANAGER") -> true
            currentUser.hasRole("EMPLOYEE") -> currentUser.stationId == stationId
            else -> false
        }
    }

    /**
     * Check if method should be audited
     */
    private fun shouldAudit(handler: HandlerMethod): Boolean {
        val auditAnnotation = handler.getMethodAnnotation(Audited::class.java)
            ?: handler.beanType.getAnnotation(Audited::class.java)

        val securedController = handler.beanType.getAnnotation(SecuredController::class.java)

        return auditAnnotation != null || securedController?.auditAll == true
    }

    /**
     * Get action from method
     */
    private fun getActionFromMethod(handler: HandlerMethod): String {
        val auditAnnotation = handler.getMethodAnnotation(Audited::class.java)
        if (auditAnnotation != null && auditAnnotation.action.isNotBlank()) {
            return auditAnnotation.action
        }

        val methodName = handler.method.name
        return when {
            methodName.startsWith("create") -> "CREATE"
            methodName.startsWith("update") -> "UPDATE"
            methodName.startsWith("delete") -> "DELETE"
            methodName.startsWith("get") || methodName.startsWith("find") -> "READ"
            else -> methodName.uppercase()
        }
    }

    /**
     * Get resource type from request path
     */
    private fun getResourceTypeFromPath(path: String): String {
        val segments = path.split("/").filter { it.isNotBlank() }
        return when {
            segments.contains("users") -> "USER"
            segments.contains("roles") -> "ROLE"
            segments.contains("stations") -> "STATION"
            segments.contains("campaigns") -> "CAMPAIGN"
            segments.contains("coupons") -> "COUPON"
            segments.contains("raffles") -> "RAFFLE"
            segments.contains("ads") -> "ADVERTISEMENT"
            segments.contains("redemptions") -> "REDEMPTION"
            else -> "UNKNOWN"
        }
    }

    /**
     * Extract resource ID from request
     */
    private fun extractResourceId(request: HttpServletRequest): String? {
        val uri = request.requestURI
        val segments = uri.split("/")

        // Look for UUID pattern or specific ID patterns
        return segments.find {
            it.matches(Regex("[a-f0-9-]{36}")) || // UUID
            it.matches(Regex("\\d+")) || // Numeric ID
            it.matches(Regex("[a-z-]+")) // Kebab-case ID
        }
    }
}

/**
 * Rate Limiting Service
 */
@Component
class RateLimitService {

    private val rateLimitCache = mutableMapOf<String, RateLimitEntry>()

    data class RateLimitEntry(
        val count: Int,
        val windowStart: Long,
        val windowDuration: Long
    )

    /**
     * Check if request is allowed based on rate limit
     */
    fun isAllowed(key: String, limit: Int, window: String): Boolean {
        val windowDurationMs = parseWindowDuration(window)
        val now = System.currentTimeMillis()

        synchronized(rateLimitCache) {
            val entry = rateLimitCache[key]

            if (entry == null || now - entry.windowStart > entry.windowDuration) {
                // New window
                rateLimitCache[key] = RateLimitEntry(1, now, windowDurationMs)
                return true
            }

            if (entry.count >= limit) {
                return false
            }

            // Increment count
            rateLimitCache[key] = entry.copy(count = entry.count + 1)
            return true
        }
    }

    /**
     * Parse window duration string (e.g., "1m", "30s", "1h")
     */
    private fun parseWindowDuration(window: String): Long {
        val number = window.dropLast(1).toIntOrNull() ?: 1
        val unit = window.last()

        return when (unit) {
            's' -> number * 1000L
            'm' -> number * 60 * 1000L
            'h' -> number * 60 * 60 * 1000L
            'd' -> number * 24 * 60 * 60 * 1000L
            else -> 60 * 1000L // Default to 1 minute
        }
    }

    /**
     * Clear expired entries
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        synchronized(rateLimitCache) {
            rateLimitCache.entries.removeIf { (_, entry) ->
                now - entry.windowStart > entry.windowDuration
            }
        }
    }
}