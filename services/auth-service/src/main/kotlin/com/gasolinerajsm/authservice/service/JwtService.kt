package com.gasolinerajsm.authservice.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey
import org.slf4j.LoggerFactory

/**
 * Service responsible for JWT token operations including generation,
 * validation, and claims extraction.
 *
 * This service handles both access tokens (short-lived) and refresh tokens (long-lived)
 * following OAuth 2.0 best practices.
 */
@Service
class JwtService(
    @Value("\${app.jwt.secret}")
    private val jwtSecret: String
) {

    companion object {
        private const val ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L // 15 minutes
        private const val REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
        private const val ROLES_CLAIM = "roles"
        private const val TOKEN_TYPE_CLAIM = "type"
        private const val ACCESS_TOKEN_TYPE = "access"
        private const val REFRESH_TOKEN_TYPE = "refresh"
    }

    private val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    private val logger = LoggerFactory.getLogger(JwtService::class.java)

    /**
     * Generates an access token for the given subject.
     *
     * @param subject The subject (usually user ID) for the token
     * @return JWT access token as string
     */
    fun generateAccessToken(subject: String): String {
        val token = Jwts.builder()
            .setSubject(subject)
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        logger.info("Generated access token for subject: {}", subject)
        return token
    }

    /**
     * Generates an access token with roles for the given subject.
     *
     * @param subject The subject (usually user ID) for the token
     * @param roles List of roles to include in the token
     * @return JWT access token as string
     */
    fun generateAccessToken(subject: String, roles: List<String>): String {
        val token = Jwts.builder()
            .setSubject(subject)
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .claim(ROLES_CLAIM, roles)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        logger.info("Generated access token with roles {} for subject: {}", roles, subject)
        return token
    }

    /**
     * Generates a refresh token for the given subject.
     *
     * @param subject The subject (usually user ID) for the token
     * @return JWT refresh token as string
     */
    fun generateRefreshToken(subject: String): String {
        val token = Jwts.builder()
            .setSubject(subject)
            .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_MS))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        logger.info("Generated refresh token for subject: {}", subject)
        return token
    }

    /**
     * Validates a JWT token.
     *
     * @param token The JWT token to validate
     * @return true if token is valid, false otherwise
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            // Check if token is not expired
            val expiration = claims.expiration
            val now = Date()

            if (expiration.before(now)) {
                logger.warn("Token has expired")
                return false
            }

            logger.debug("Token validated successfully")
            true
        } catch (e: Exception) {
            logger.warn("Token validation failed: {}", e.message)
            false
        }
    }

    /**
     * Extracts the subject from a JWT token.
     *
     * @param token The JWT token
     * @return The subject claim from the token
     * @throws IllegalArgumentException if token is invalid
     */
    fun getSubjectFromToken(token: String): String {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
                .subject
        } catch (e: Exception) {
            logger.error("Failed to extract subject from token: {}", e.message)
            throw IllegalArgumentException("Invalid token", e)
        }
    }

    /**
     * Extracts roles from a JWT token.
     *
     * @param token The JWT token
     * @return List of roles, empty if no roles claim exists
     */
    @Suppress("UNCHECKED_CAST")
    fun getRolesFromToken(token: String): List<String> {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            claims.get(ROLES_CLAIM, List::class.java) as? List<String> ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to extract roles from token: {}", e.message)
            emptyList()
        }
    }

    /**
     * Checks if a token is of the specified type.
     *
     * @param token The JWT token
     * @param expectedType The expected token type (access or refresh)
     * @return true if token is of expected type, false otherwise
     */
    fun isTokenOfType(token: String, expectedType: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            val tokenType = claims.get(TOKEN_TYPE_CLAIM, String::class.java)
            tokenType == expectedType
        } catch (e: Exception) {
            logger.error("Failed to check token type: {}", e.message)
            false
        }
    }
}
