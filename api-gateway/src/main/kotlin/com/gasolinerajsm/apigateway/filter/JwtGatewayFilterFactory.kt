package com.gasolinerajsm.apigateway.filter

import com.gasolinerajsm.apigateway.security.JwtAuthenticationManager
import com.gasolinerajsm.shared.resilience.ResilienceService
import com.gasolinerajsm.shared.resilience.ResilienceConfig
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Factory para crear filtros JWT personalizados en Gateway
 */
@Component
class JwtGatewayFilterFactory(
    private val jwtAuthenticationManager: JwtAuthenticationManager,
    private val resilienceService: ResilienceService
) : AbstractGatewayFilterFactory<JwtGatewayFilterFactory.Config>() {

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val response = exchange.response

            // Extraer token JWT del header
            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.statusCode = HttpStatus.UNAUTHORIZED
                return@GatewayFilter response.setComplete()
            }

            val token = authHeader.substring(7)

            // Validar token con resilience
            val resilienceConfig = ResilienceConfig(
                circuitBreakerName = "jwt-validation",
                retryName = "jwt-validation",
                rateLimiterName = "api-calls"
            )

            Mono.fromCallable {
                resilienceService.executeWithResilience(resilienceConfig, {
                    // Validar token
                    if (jwtAuthenticationManager.isTokenExpired(token)) {
                        throw RuntimeException("Token expired")
                    }

                    val username = jwtAuthenticationManager.getUsernameFromToken(token)
                        ?: throw RuntimeException("Invalid token")

                    val roles = jwtAuthenticationManager.getRolesFromToken(token)

                    // Agregar headers con información del usuario
                    val mutatedRequest = request.mutate()
                        .header("X-User-Id", username)
                        .header("X-User-Roles", roles.joinToString(","))
                        .build()

                    exchange.mutate().request(mutatedRequest).build()
                }, {
                    // Fallback en caso de error
                    throw RuntimeException("JWT validation failed")
                })
            }.flatMap { mutatedExchange ->
                chain.filter(mutatedExchange)
            }.onErrorResume { error ->
                response.statusCode = when {
                    error.message?.contains("expired") == true -> HttpStatus.UNAUTHORIZED
                    error.message?.contains("Invalid") == true -> HttpStatus.UNAUTHORIZED
                    else -> HttpStatus.INTERNAL_SERVER_ERROR
                }
                response.setComplete()
            }
        }
    }

    override fun getConfigClass(): Class<Config> = Config::class.java

    class Config {
        var enabled: Boolean = true
        var validateExpiration: Boolean = true
        var addUserHeaders: Boolean = true
    }
}