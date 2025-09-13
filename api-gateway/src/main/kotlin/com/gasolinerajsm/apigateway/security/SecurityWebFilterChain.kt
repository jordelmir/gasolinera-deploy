package com.gasolinerajsm.apigateway.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Configuración de seguridad para API Gateway con JWT
 */
@Configuration
@EnableWebFluxSecurity
class SecurityWebFilterChain(
    private val jwtAuthenticationManager: JwtAuthenticationManager,
    private val jwtProperties: JwtProperties
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { exchange, _ ->
                        val response = exchange.response
                        response.statusCode = HttpStatus.UNAUTHORIZED
                        response.setComplete()
                    }
                    .accessDeniedHandler { exchange, _ ->
                        val response = exchange.response
                        response.statusCode = HttpStatus.FORBIDDEN
                        response.setComplete()
                    }
            }
            .authorizeExchange { exchanges ->
                exchanges
                    // Rutas públicas
                    .pathMatchers(*jwtProperties.publicPaths.toTypedArray()).permitAll()
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Rutas de administrador
                    .pathMatchers(*jwtProperties.adminPaths.toTypedArray()).hasRole("ADMIN")

                    // Rutas de moderador
                    .pathMatchers(*jwtProperties.moderatorPaths.toTypedArray()).hasAnyRole("ADMIN", "MODERATOR")

                    // Rutas específicas por servicio
                    .pathMatchers(HttpMethod.POST, "/api/stations/**").hasAnyRole("ADMIN", "MODERATOR")
                    .pathMatchers(HttpMethod.PUT, "/api/stations/**").hasAnyRole("ADMIN", "MODERATOR")
                    .pathMatchers(HttpMethod.DELETE, "/api/stations/**").hasRole("ADMIN")

                    .pathMatchers(HttpMethod.POST, "/api/campaigns/**").hasAnyRole("ADMIN", "MODERATOR")
                    .pathMatchers(HttpMethod.PUT, "/api/campaigns/**").hasAnyRole("ADMIN", "MODERATOR")
                    .pathMatchers(HttpMethod.DELETE, "/api/campaigns/**").hasRole("ADMIN")

                    .pathMatchers(HttpMethod.POST, "/api/raffles/**").hasAnyRole("ADMIN", "MODERATOR")
                    .pathMatchers(HttpMethod.PUT, "/api/raffles/**").hasAnyRole("ADMIN", "MODERATOR")
                    .pathMatchers(HttpMethod.DELETE, "/api/raffles/**").hasRole("ADMIN")

                    // Todas las demás rutas requieren autenticación
                    .anyExchange().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    @Bean
    fun jwtAuthenticationFilter(): AuthenticationWebFilter {
        val authenticationFilter = AuthenticationWebFilter(jwtAuthenticationManager)
        authenticationFilter.setServerAuthenticationConverter(jwtAuthenticationConverter())
        return authenticationFilter
    }

    @Bean
    fun jwtAuthenticationConverter(): ServerAuthenticationConverter {
        return JwtServerAuthenticationConverter()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.exposedHeaders = listOf("Authorization", "X-Total-Count", "X-Correlation-ID")
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}