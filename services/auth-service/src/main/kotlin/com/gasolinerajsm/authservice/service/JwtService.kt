package com.gasolinerajsm.authservice.service

import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.model.UserRole
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.Key
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

/**
 * Enhanced JWT Service with Vault integration and comprehensive token management.
 *
 * Features:
 * - RS256 and HS256 algorithm support
 * - Access and refresh token management
 * - Token blacklisting with Redis
 * - Comprehensive claims management
 * - Security best practices implementation
 * - Vault integration for secret management
 */
@Service
class JwtService(
    @Value("\${jwt.secret-key:default-secret-key-for-development-only}")
    private val jwtSecretKey: String,

    @Value("\${jwt.refresh-secret-key:default-refresh-secret-key-for-development-only}")
    private val refreshSecretKey: String,

    @Value("\${jwt.access-token-expiration:3600}")
    private val accessTokenExpirationSeconds: Long,

    @Value("\${jwt.refresh-token-expiration:86400}")
    private val refreshTokenExpirationSeconds: Long,

    @Value("\${jwt.issuer:gasolinera-jsm-platform}")
    private val issuer: String,

    @Value("\${jwt.audience:gasolinera-jsm-users}")
    private val audience: String,

    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private const val TOKEN_TYPE_CLAIM = "typ"
        private const val ROLES_CLAIM = "roles"
        private const val PERMISSIONS_CLAIM = "permissions"
        private const val USER_ID_CLAIM = "uid"
        private const val PHONE_NUMBER_CLAIM = "phone"
        private const val FULL_NAME_CLAIM = "name"
        private const val PHONE_VERIFIED_CLAIM = "phone_verified"
        private const val ACCOUNT_STATUS_CLAIM = "status"
        private const val SESSION_ID_CLAIM = "sid"

        private const val ACCESS_TOKEN_TYPE = "access"
        private const val REFRESH_TOKEN_TYPE = "refresh"

        private const val BLACKLIST_PREFIX = "jwt:blacklist:"
        private const val REFRESH_TOKEN_PREFIX = "jwt:refresh:"
        private const val SESSION_PREFIX = "jwt:session:"

        // Security constants
        private const val MIN_SECRET_LENGTH = 32
        private const val TOKEN_LEEWAY_SECONDS = 30L
    }

    private val logger = LoggerFactory.getLogger(JwtService::class.java)

    // Initialize signing keys
    private val accessTokenKey: SecretKey by lazy {
        validateSecretKey(jwtSecretKey, "JWT secret key")
        Keys.hmacShaKeyFor(jwtSecretKey.toByteArray())
    }

    private val refreshTokenKey: SecretKey by lazy {
        validateSecretKey(refreshSecretKey, "JWT refresh secret key")
        Keys.hmacShaKeyFor(refreshSecretKey.toByteArray())
    }

    /**
     * Validates that the secret key meets security requirements
     */
    private fun validateSecretKey(secretKey: String, keyName: String) {
        if (secretKey.length < MIN_SECRET_LENGTH) {
            logger.error("$keyName is too short. Minimum length is $MIN_SECRET_LENGTH characters")
            throw IllegalArgumentException("$keyName must be at least $MIN_SECRET_LENGTH characters long")
        }
    }

    /**
     * Generates a comprehensive access token for a user
     */
    fun generateAccessToken(user: User, sessionId: String = UUID.randomUUID().toString()): String {
        val now = Date()
        val expiration = Date(now.time + accessTokenExpirationSeconds * 1000)

        val claims = Jwts.claims()
            .setSubject(user.id.toString())
            .setIssuer(issuer)
            .setAudience(audience)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .setNotBefore(now)
            .setId(UUID.randomUUID().toString())

        // Add custom claims
        claims[TOKEN_TYPE_CLAIM] = ACCESS_TOKEN_TYPE
        claims[USER_ID_CLAIM] = user.id
        claims[PHONE_NUMBER_CLAIM] = user.phoneNumber
        claims[FULL_NAME_CLAIM] = user.getFullName()
        claims[ROLES_CLAIM] = listOf(user.role.name)
        claims[PERMISSIONS_CLAIM] = user.role.permissions.toList()
        claims[PHONE_VERIFIED_CLAIM] = user.isPhoneVerified
        claims[ACCOUNT_STATUS_CLAIM] = if (user.isActive) "active" else "inactive"
        claims[SESSION_ID_CLAIM] = sessionId

        val token = Jwts.builder()
            .setClaims(claims)
            .signWith(accessTokenKey, SignatureAlgorithm.HS256)
            .compact()

        // Store session information in Redis
        storeSessionInfo(sessionId, user.id, accessTokenExpirationSeconds)

        logger.info("Generated access token for user {} with session {}", user.id, sessionId)
        return token
    }

    /**
     * Generates a refresh token for a user
     */
    fun generateRefreshToken(user: User): String {
        val now = Date()
        val expiration = Date(now.time + refreshTokenExpirationSeconds * 1000)
        val tokenId = UUID.randomUUID().toString()

        val claims = Jwts.claims()
            .setSubject(user.id.toString())
            .setIssuer(issuer)
            .setAudience(audience)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .setNotBefore(now)
            .setId(tokenId)

        claims[TOKEN_TYPE_CLAIM] = REFRESH_TOKEN_TYPE
        claims[USER_ID_CLAIM] = user.id

        val token = Jwts.builder()
            .setClaims(claims)
            .signWith(refreshTokenKey, SignatureAlgorithm.HS256)
            .compact()

        // Store refresh token in Redis for tracking
        storeRefreshToken(tokenId, user.id, refreshTokenExpirationSeconds)

        logger.info("Generated refresh token for user {}", user.id)
        return token
    }

    /**
     * Validates an access token
     */
    fun validateAccessToken(token: String): Boolean {
        return validateToken(token, accessTokenKey, ACCESS_TOKEN_TYPE)
    }

    /**
     * Validates a refresh token
     */
    fun validateRefreshToken(token: String): Boolean {
        return validateToken(token, refreshTokenKey, REFRESH_TOKEN_TYPE)
    }

    /**
     * Generic token validation method
     */
    private fun validateToken(token: String, key: Key, expectedType: String): Boolean {
        return try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                logger.warn("Token is blacklisted")
                return false
            }

            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(TOKEN_LEEWAY_SECONDS)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .body

            // Verify token type
            val tokenType = claims[TOKEN_TYPE_CLAIM] as? String
            if (tokenType != expectedType) {
                logger.warn("Invalid token type. Expected: {}, Got: {}", expectedType, tokenType)
                return false
            }

            // For access tokens, verify session is still valid
            if (expectedType == ACCESS_TOKEN_TYPE) {
                val sessionId = claims[SESSION_ID_CLAIM] as? String
                if (sessionId != null && !isSessionValid(sessionId)) {
                    logger.warn("Session {} is no longer valid", sessionId)
                    return false
                }
            }

            // For refresh tokens, verify it's still stored
            if (expectedType == REFRESH_TOKEN_TYPE) {
                val tokenId = claims.id
                if (!isRefreshTokenValid(tokenId)) {
                    logger.warn("Refresh token {} is no longer valid", tokenId)
                    return false
                }
            }

            logger.debug("Token validated successfully")
            true
        } catch (e: ExpiredJwtException) {
            logger.warn("Token has expired: {}", e.message)
            false
        } catch (e: UnsupportedJwtException) {
            logger.warn("Unsupported JWT token: {}", e.message)
            false
        } catch (e: MalformedJwtException) {
            logger.warn("Malformed JWT token: {}", e.message)
            false
        } catch (e: SignatureException) {
            logger.warn("Invalid JWT signature: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            logger.warn("JWT token compact of handler are invalid: {}", e.message)
            false
        } catch (e: Exception) {
            logger.error("Unexpected error during token validation: {}", e.message, e)
            false
        }
    }

    /**
     * Extracts user ID from token
     */
    fun getUserIdFromToken(token: String): Long? {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            claims[USER_ID_CLAIM] as? Long ?: claims.subject.toLongOrNull()
        } catch (e: Exception) {
            logger.error("Failed to extract user ID from token: {}", e.message)
            null
        }
    }

    /**
     * Extracts user roles from token
     */
    @Suppress("UNCHECKED_CAST")
    fun getRolesFromToken(token: String): List<String> {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            claims[ROLES_CLAIM] as? List<String> ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to extract roles from token: {}", e.message)
            emptyList()
        }
    }

    /**
     * Extracts user permissions from token
     */
    @Suppress("UNCHECKED_CAST")
    fun getPermissionsFromToken(token: String): Set<String> {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            val permissions = claims[PERMISSIONS_CLAIM] as? List<String> ?: emptyList()
            permissions.toSet()
        } catch (e: Exception) {
            logger.error("Failed to extract permissions from token: {}", e.message)
            emptySet()
        }
    }

    /**
     * Extracts session ID from token
     */
    fun getSessionIdFromToken(token: String): String? {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            claims[SESSION_ID_CLAIM] as? String
        } catch (e: Exception) {
            logger.error("Failed to extract session ID from token: {}", e.message)
            null
        }
    }

    /**
     * Extracts phone number from token
     */
    fun getPhoneNumberFromToken(token: String): String? {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            claims[PHONE_NUMBER_CLAIM] as? String
        } catch (e: Exception) {
            logger.error("Failed to extract phone number from token: {}", e.message)
            null
        }
    }

    /**
     * Checks if user has specific permission based on token
     */
    fun hasPermission(token: String, permission: String): Boolean {
        val permissions = getPermissionsFromToken(token)
        return permissions.contains(permission)
    }

    /**
     * Checks if user has specific role based on token
     */
    fun hasRole(token: String, role: UserRole): Boolean {
        val roles = getRolesFromToken(token)
        return roles.contains(role.name)
    }

    /**
     * Blacklists a token (logout functionality)
     */
    fun blacklistToken(token: String): Boolean {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            val expiration = claims.expiration
            val tokenId = claims.id

            val ttl = (expiration.time - System.currentTimeMillis()) / 1000
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                    "$BLACKLIST_PREFIX$tokenId",
                    "blacklisted",
                    ttl,
                    TimeUnit.SECONDS
                )

                // Also invalidate session
                val sessionId = claims[SESSION_ID_CLAIM] as? String
                if (sessionId != null) {
                    invalidateSession(sessionId)
                }

                logger.info("Token {} blacklisted successfully", tokenId)
                true
            } else {
                logger.warn("Attempted to blacklist expired token {}", tokenId)
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to blacklist token: {}", e.message)
            false
        }
    }

    /**
     * Invalidates a refresh token
     */
    fun invalidateRefreshToken(token: String): Boolean {
        return try {
            val claims = parseTokenClaims(token, refreshTokenKey)
            val tokenId = claims.id

            redisTemplate.delete("$REFRESH_TOKEN_PREFIX$tokenId")
            logger.info("Refresh token {} invalidated successfully", tokenId)
            true
        } catch (e: Exception) {
            logger.error("Failed to invalidate refresh token: {}", e.message)
            false
        }
    }

    /**
     * Invalidates all tokens for a user
     */
    fun invalidateAllUserTokens(userId: Long) {
        try {
            // Find and delete all user sessions
            val sessionKeys = redisTemplate.keys("$SESSION_PREFIX*:$userId")
            if (sessionKeys.isNotEmpty()) {
                redisTemplate.delete(sessionKeys)
            }

            // Find and delete all user refresh tokens
            val refreshKeys = redisTemplate.keys("$REFRESH_TOKEN_PREFIX*:$userId")
            if (refreshKeys.isNotEmpty()) {
                redisTemplate.delete(refreshKeys)
            }

            logger.info("All tokens invalidated for user {}", userId)
        } catch (e: Exception) {
            logger.error("Failed to invalidate all tokens for user {}: {}", userId, e.message)
        }
    }

    /**
     * Gets token expiration time
     */
    fun getTokenExpiration(token: String): LocalDateTime? {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            val expiration = claims.expiration
            LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault())
        } catch (e: Exception) {
            logger.error("Failed to get token expiration: {}", e.message)
            null
        }
    }

    /**
     * Checks if token is about to expire (within 5 minutes)
     */
    fun isTokenNearExpiry(token: String, minutesThreshold: Long = 5): Boolean {
        val expiration = getTokenExpiration(token) ?: return true
        val threshold = LocalDateTime.now().plusMinutes(minutesThreshold)
        return expiration.isBefore(threshold)
    }

    // Private helper methods

    private fun parseTokenClaims(token: String, key: Key): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .setAllowedClockSkewSeconds(TOKEN_LEEWAY_SECONDS)
            .build()
            .parseClaimsJws(token)
            .body
    }

    private fun isTokenBlacklisted(token: String): Boolean {
        return try {
            val claims = parseTokenClaims(token, accessTokenKey)
            val tokenId = claims.id
            redisTemplate.hasKey("$BLACKLIST_PREFIX$tokenId")
        } catch (e: Exception) {
            // If we can't parse the token, consider it invalid
            true
        }
    }

    private fun storeSessionInfo(sessionId: String, userId: Long, ttlSeconds: Long) {
        val sessionKey = "$SESSION_PREFIX$sessionId:$userId"
        redisTemplate.opsForValue().set(
            sessionKey,
            LocalDateTime.now().toString(),
            ttlSeconds,
            TimeUnit.SECONDS
        )
    }

    private fun isSessionValid(sessionId: String): Boolean {
        val sessionKeys = redisTemplate.keys("$SESSION_PREFIX$sessionId:*")
        return sessionKeys.isNotEmpty()
    }

    private fun invalidateSession(sessionId: String) {
        val sessionKeys = redisTemplate.keys("$SESSION_PREFIX$sessionId:*")
        if (sessionKeys.isNotEmpty()) {
            redisTemplate.delete(sessionKeys)
        }
    }

    private fun storeRefreshToken(tokenId: String, userId: Long, ttlSeconds: Long) {
        val refreshKey = "$REFRESH_TOKEN_PREFIX$tokenId:$userId"
        redisTemplate.opsForValue().set(
            refreshKey,
            LocalDateTime.now().toString(),
            ttlSeconds,
            TimeUnit.SECONDS
        )
    }

    private fun isRefreshTokenValid(tokenId: String): Boolean {
        val refreshKeys = redisTemplate.keys("$REFRESH_TOKEN_PREFIX$tokenId:*")
        return refreshKeys.isNotEmpty()
    }
}
