package com.gasolinerajsm.apigateway.security

import com.gasolinerajsm.shared.vault.VaultClient
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Gestor de autenticación JWT para API Gateway
 */
@Component
class JwtAuthenticationManager(
    private val vaultClient: VaultClient,
    private val jwtProperties: JwtProperties
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val authToken = authentication.credentials.toString()

        return Mono.fromCallable {
            try {
                val claims = validateToken(authToken)
                val username = claims.subject
                val roles = extractRoles(claims)
                val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }

                UsernamePasswordAuthenticationToken(username, null, authorities)
            } catch (e: Exception) {
                throw org.springframework.security.authentication.BadCredentialsException("Invalid JWT token", e)
            }
        }
    }

    private fun validateToken(token: String): Claims {
        val secretKey = getJwtSecret()
        val key = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))

        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    private fun extractRoles(claims: Claims): List<String> {
        @Suppress("UNCHECKED_CAST")
        return claims["roles"] as? List<String> ?: emptyList()
    }

    private fun getJwtSecret(): String {
        return try {
            vaultClient.getSecret("secret/jwt")["secret-key"] as? String
                ?: jwtProperties.secretKey
        } catch (e: Exception) {
            jwtProperties.secretKey
        }
    }

    /**
     * Valida si el token está expirado
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val claims = validateToken(token)
            claims.expiration.before(Date())
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Extrae el username del token
     */
    fun getUsernameFromToken(token: String): String? {
        return try {
            val claims = validateToken(token)
            claims.subject
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extrae los roles del token
     */
    fun getRolesFromToken(token: String): List<String> {
        return try {
            val claims = validateToken(token)
            extractRoles(claims)
        } catch (e: Exception) {
            emptyList()
        }
    }
}