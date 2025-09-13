package com.gasolinerajsm.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Role-based authorization filter for API Gateway
 * Validates user roles against endpoint requirements
 */
@Component
class RoleAuthorizationFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(RoleAuthorizationFilter::class.java)

    companion object {
        // Define role-based access rules
        private val ROLE_BASED_RULES = mapOf(
            // Admin-only endpoints
            "/api/stations" to mapOf("POST" to listOf("ADMIN", "MANAGER"), "PUT" to listOf("ADMIN", "MANAGER"), "DELETE" to listOf("ADMIN")),
            "/api/campaigns" to mapOf("POST" to listOf("ADMIN", "MANAGER"), "PUT" to listOf("ADMIN", "MANAGER"), "DELETE" to listOf("ADMIN")),
            "/api/coupons/admin" to mapOf("GET" to listOf("ADMIN", "MANAGER"), "POST" to listOf("ADMIN", "MANAGER")),
            "/api/redemptions/admin" to mapOf("GET" to listOf("ADMIN", "MANAGER"), "POST" to listOf("ADMIN", "MANAGER")),
            "/api/raffles/admin" to mapOf("GET" to listOf("ADMIN", "MANAGER"), "POST" to listOf("ADMIN", "MANAGER")),
            "/api/ads/admin" to mapOf("GET" to listOf("ADMIN", "MANAGER"), "POST" to listOf("ADMIN", "MANAGER")),

            // Manager and above endpoints
            "/api/employees" to mapOf("GET" to listOf("ADMIN", "MANAGER", "EMPLOYEE"), "POST" to listOf("ADMIN", "MANAGER")),
            "/api/redemptions/review" to mapOf("GET" to listOf("ADMIN", "MANAGER"), "POST" to listOf("ADMIN", "MANAGER")),
            "/api/prizes/admin" to mapOf("GET" to listOf("ADMIN", "MANAGER"), "POST" to listOf("ADMIN", "MANAGER")),

            // Employee and above endpoints
            "/api/redemptions/complete" to mapOf("POST" to listOf("ADMIN", "MANAGER", "EMPLOYEE")),
            "/api/coupons/validate" to mapOf("POST" to listOf("ADMIN", "MANAGER", "EMPLOYEE")),

            // User endpoints (authenticated users)
            "/api/redemptions/user" to mapOf("GET" to listOf("USER", "EMPLOYEE", "MANAGER", "ADMIN")),
            "/api/tickets/user" to mapOf("GET" to listOf("USER", "EMPLOYEE", "MANAGER", "ADMIN")),
            "/api/raffles/entries" to mapOf("POST" to listOf("USER", "EMPLOYEE", "MANAGER", "ADMIN")),
            "/api/ads" to mapOf("GET" to listOf("USER", "EMPLOYEE", "MANAGER", "ADMIN")),
            "/api/engagements" to mapOf("POST" to listOf("USER", "EMPLOYEE", "MANAGER", "ADMIN"))
        )

        private val PUBLIC_PATHS = setOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify",
            "/actuator/health",
            "/actuator/info",
            "/fallback",
            "/health",
            "/v3/api-docs",
            "/swagger-ui"
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()
        val method = request.method.name()

        // Skip authorization for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange)
        }

        // Get user roles from headers (set by JWT filter)
        val userRoles = request.headers.getFirst("X-User-Roles")?.split(",")?.map { it.trim() } ?: emptyList()
        val userId = request.headers.getFirst("X-User-ID") ?: "anonymous"
        val isAuthenticated = request.headers.getFirst("X-Authenticated") == "true"

        // If not authenticated, let security config handle it
        if (!isAuthenticated) {
            return chain.filter(exchange)
        }

        // Check role-based authorization
        val requiredRoles = getRequiredRoles(path, method)

        if (requiredRoles.isNotEmpty()) {
            val hasRequiredRole = userRoles.any { userRole ->
                requiredRoles.contains(userRole.uppercase())
            }

            if (!hasRequiredRole) {
                logger.warn(
                    "Access denied - UserId: {}, Path: {}, Method: {}, UserRoles: {}, RequiredRoles: {}",
                    userId, path, method, userRoles, requiredRoles
                )
                return handleForbidden(exchange, path, method, userRoles, requiredRoles)
            }

            logger.debug(
                "Access granted - UserId: {}, Path: {}, Method: {}, UserRoles: {}",
                userId, path, method, userRoles
            )
        }

        return chain.filter(exchange)
    }

    /**
     * Get required roles for a specific path and method
     */
    private fun getRequiredRoles(path: String, method: String): List<String> {
        // Find the most specific matching rule
        val matchingRule = ROLE_BASED_RULES.entries
            .filter { (rulePath, _) -> path.startsWith(rulePath) }
            .maxByOrNull { (rulePath, _) -> rulePath.length }

        return matchingRule?.value?.get(method) ?: emptyList()
    }

    /**
     * Check if the path is public and doesn't require authorization
     */
    private fun isPublicPath(path: String): Boolean {
        return PUBLIC_PATHS.any { publicPath ->
            path.startsWith(publicPath) || path.contains(publicPath)
        }
    }

    /**
     * Handle forbidden access
     */
    private fun handleForbidden(
        exchange: ServerWebExchange,
        path: String,
        method: String,
        userRoles: List<String>,
        requiredRoles: List<String>
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.FORBIDDEN
        response.headers.add(HttpHeaders.CONTENT_TYPE, "application/json")

        val errorBody = """
            {
                "error": "FORBIDDEN",
                "message": "Insufficient privileges to access this resource",
                "timestamp": "${java.time.LocalDateTime.now()}",
                "path": "$path",
                "method": "$method",
                "userRoles": ${userRoles.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"")},
                "requiredRoles": ${requiredRoles.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"")}
            }
        """.trimIndent()

        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getOrder(): Int {
        return -50 // Execute after JWT filter but before other filters
    }
}