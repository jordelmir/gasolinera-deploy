package com.gasolinerajsm.apigateway.config

import com.gasolinerajsm.apigateway.filter.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFilterChain
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Security configuration for API Gateway with JWT authentication
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints - No authentication required
                    .pathMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/verify").permitAll()
                    .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                    .pathMatchers("/fallback/**").permitAll()
                    .pathMatchers("/health/**").permitAll()
                    .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                    // Auth service endpoints
                    .pathMatchers(HttpMethod.POST, "/api/auth/refresh").authenticated()
                    .pathMatchers("/api/auth/**").authenticated()

                    // Station service endpoints
                    .pathMatchers(HttpMethod.GET, "/api/stations/**").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/stations/**").hasAnyRole("ADMIN", "MANAGER")
                    .pathMatchers(HttpMethod.PUT, "/api/stations/**").hasAnyRole("ADMIN", "MANAGER")
                    .pathMatchers(HttpMethod.DELETE, "/api/stations/**").hasRole("ADMIN")
                    .pathMatchers("/api/employees/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")

                    // Coupon service endpoints
                    .pathMatchers(HttpMethod.GET, "/api/coupons/validate/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/coupons/user/**").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/coupons/redeem").authenticated()
                    .pathMatchers("/api/coupons/**").hasAnyRole("ADMIN", "MANAGER")
                    .pathMatchers("/api/campaigns/**").hasAnyRole("ADMIN", "MANAGER")

                    // Redemption service endpoints
                    .pathMatchers(HttpMethod.POST, "/api/redemptions").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/redemptions/user/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/tickets/user/**").authenticated()
                    .pathMatchers("/api/redemptions/admin/**").hasAnyRole("ADMIN", "MANAGER")
                    .pathMatchers("/api/redemptions/review/**").hasAnyRole("ADMIN", "MANAGER")
                    .pathMatchers("/api/redemptions/**").authenticated()
                    .pathMatchers("/api/tickets/**").authenticated()

                    // Ad engine endpoints
                    .pathMatchers(HttpMethod.GET, "/api/ads/**").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/engagements/**").authenticated()
                    .pathMatchers("/api/ads/admin/**").hasAnyRole("ADMIN", "MANAGER")

                    // Raffle service endpoints
                    .pathMatchers(HttpMethod.GET, "/api/raffles/**").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/entries/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/prizes/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/winners/**").authenticated()
                    .pathMatchers("/api/raffles/admin/**").hasAnyRole("ADMIN", "MANAGER")
                    .pathMatchers("/api/prizes/admin/**").hasAnyRole("ADMIN", "MANAGER")

                    // Default: require authentication
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtDecoder(reactiveJwtDecoder())
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.server.authentication.AuthenticationWebFilter::class.java)
            .build()
    }

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Allow specific origins in production
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:*",
            "https://localhost:*",
            "https://*.gasolinerajsm.com",
            "https://gasolinerajsm.com"
        )

        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        )

        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-User-ID",
            "X-API-Key",
            "X-Gateway"
        )

        configuration.exposedHeaders = listOf(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "X-Total-Count",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset"
        )

        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}