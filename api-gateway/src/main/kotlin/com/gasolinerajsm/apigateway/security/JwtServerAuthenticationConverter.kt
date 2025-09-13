package com.gasolinerajsm.apigateway.security

import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Convertidor de autenticación JWT para extraer el token del header
 */
class JwtServerAuthenticationConverter : ServerAuthenticationConverter {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        return Mono.fromCallable {
            val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                val token = authHeader.substring(BEARER_PREFIX.length)
                UsernamePasswordAuthenticationToken(null, token)
            } else {
                null
            }
        }
    }
}