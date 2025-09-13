package com.gasolinerajsm.apigateway.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Propiedades de configuración JWT
 */
@ConfigurationProperties(prefix = "gasolinera.jwt")
data class JwtProperties(
    val secretKey: String = "gasolinera-jsm-default-secret-key-change-in-production",
    val expiration: Duration = Duration.ofHours(24),
    val refreshExpiration: Duration = Duration.ofDays(7),
    val issuer: String = "gasolinera-jsm",
    val audience: String = "gasolinera-jsm-users",
    val clockSkew: Duration = Duration.ofMinutes(5),
    val publicPaths: List<String> = listOf(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/health/**",
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**"
    ),
    val adminPaths: List<String> = listOf(
        "/api/admin/**",
        "/api/management/**",
        "/api/metrics/**"
    ),
    val moderatorPaths: List<String> = listOf(
        "/api/stations/*/manage",
        "/api/campaigns/*/manage",
        "/api/raffles/*/manage"
    )
)