
package com.gasolinerajsm.adengine.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${app.jwt.secret:defaultSecretKeyForDevelopment}")
    private lateinit var secret: String

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun extractAllClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body
    }

    fun getUsername(token: String): String {
        return extractAllClaims(token).subject
    }

    fun getRoles(token: String): List<String> {
        return extractAllClaims(token).get("roles", List::class.java) as List<String>
    }

    fun isTokenValid(token: String): Boolean {
        // Basic validation, a real implementation would check expiration, etc.
        return try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
