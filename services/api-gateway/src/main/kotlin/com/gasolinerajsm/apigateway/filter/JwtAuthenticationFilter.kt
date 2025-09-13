package com.gasolinerajsm.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * JWT Authentication Filter for API Gateway
 * Validates JWT tokens and extracts user information
 */
@Component
class JwtAuthenticationFilter(
    private val jwtDecoder: ReactiveJwtDecoder
) : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    @Value("\${app.security.jwt.header:Authorization}")
    private lateinit var authHeader: String

    @Value("\${app.security.jwt.prefix:Bearer }")
    private lateinit var tokenPrefix: String

    companion object {
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

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            logger.debug("Skipping JWT validation for public path: $path")
            return chain.filter(exchange)
        }

        // Extract JWT token from request
        val token = extractToken(request)

        if (token == null) {
            logger.warn("No JWT token found in request to protected path: $path")
            return handleUnauthorized(exchange)
        }

        // Validate and decode JWT token
        return jwtDecoder.decode(token)
            .flatMap { jwt ->
                logger.debug("JWT token validated successfully for user: ${jwt.subject}")

                // Extract user information from JWT
                val userId = jwt.subject
                val username = jwt.getClaimAsString("preferred_username") ?: jwt.subject
                val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
                val email = jwt.getClaimAsString("email")
                val stationId = jwt.getClaimAsString("station_id")

                // Add user information to request headers for downstream services
                val modifiedRequest = request.mutate()
                    .header("X-User-ID", userId)
                    .header("X-Username", username)
                    .header("X-User-Email", email ?: "")
                    .header("X-User-Roles", roles.joinToString(","))
                    .header("X-Station-ID", stationId ?: "")
                    .header("X-Authenticated", "true")
                    .build()

                val modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build()

                chain.filter(modifiedExchange)
            }
            .onErrorResume { error ->
                logger.warn("JWT token validation failed for path: $path", error)
                handleUnauthorized(exchange)
            }
    }

    /**
     * Extract JWT token from Authorization header
     */
    private fun extractToken(request: ServerHttpRequest): String? {
        val authHeaderValue = request.headers.getFirst(authHeader)

        return if (authHeaderValue != null && authHeaderValue.startsWith(tokenPrefix)) {
            authHeaderValue.substring(tokenPrefix.length)
        } else {
            // Try to get token from query parameter as fallback
            request.queryParams.getFirst("token")
        }
    }

    /**
     * Check if the path is public and doesn't require authentication
     */
    private fun isPublicPath(path: String): Boolean {
        return PUBLIC_PATHS.any { publicPath ->
            path.startsWith(publicPath) || path.contains(publicPath)
        }
    }

    /**
     * Handle unauthorized requests
     */
    private fun handleUnauthorized(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add(HttpHeaders.CONTENT_TYPE, "application/json")

        val errorBody = """
            {
                "error": "UNAUTHORIZED",
                "message": "Authentication required",
                "timestamp": "${java.time.LocalDateTime.now()}",
                "path": "${exchange.request.path.value()}"
            }
        """.trimIndent()

        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getOrder(): Int {
        return -100 // Execute before other filters
    }
}