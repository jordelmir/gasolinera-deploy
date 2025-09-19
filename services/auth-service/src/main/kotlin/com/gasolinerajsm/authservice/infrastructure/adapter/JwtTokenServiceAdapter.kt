package com.gasolinerajsm.authservice.infrastructure.adapter

import com.gasolinerajsm.authservice.application.port.out.JwtTokenService
import com.gasolinerajsm.authservice.application.port.out.TokenClaims
import com.gasolinerajsm.authservice.config.JwtConfig
import com.gasolinerajsm.authservice.domain.model.User
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.Key
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * JWT Token Service implementation using JJWT library
 */
@Service
class JwtTokenServiceAdapter(
    private val jwtConfig: JwtConfig
) : JwtTokenService {

    private val logger = LoggerFactory.getLogger(JwtTokenServiceAdapter::class.java)

    private val accessTokenKey: Key = Keys.hmacShaKeyFor(jwtConfig.secretKey.toByteArray())
    private val refreshTokenKey: Key = Keys.hmacShaKeyFor(jwtConfig.refreshSecretKey.toByteArray())

    override fun generateAccessToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtConfig.getAccessTokenExpirationMs())

        return Jwts.builder()
            .setSubject(user.id.toString())
            .setIssuer(jwtConfig.issuer)
            .setAudience(jwtConfig.audience)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("phoneNumber", user.phoneNumber.toString())
            .claim("role", user.role.name)
            .claim("permissions", user.role.permissions.toList())
            .claim("firstName", user.firstName)
            .claim("lastName", user.lastName)
            .claim("isActive", user.isActive)
            .claim("isPhoneVerified", user.isPhoneVerified)
            .signWith(accessTokenKey, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun generateRefreshToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtConfig.getRefreshTokenExpirationMs())

        return Jwts.builder()
            .setSubject(user.id.toString())
            .setIssuer(jwtConfig.issuer)
            .setAudience(jwtConfig.audience)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "refresh")
            .claim("phoneNumber", user.phoneNumber.toString())
            .signWith(refreshTokenKey, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun validateAccessToken(token: String): Result<TokenClaims> {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(accessTokenKey)
                .setAllowedClockSkewSeconds(jwtConfig.clockSkewSeconds)
                .build()
                .parseClaimsJws(token)
                .body

            val tokenClaims = TokenClaims(
                userId = claims.subject,
                phoneNumber = claims.get("phoneNumber", String::class.java),
                role = claims.get("role", String::class.java),
                permissions = claims.get("permissions", List::class.java) as List<String>,
                issuedAt = claims.issuedAt.time,
                expiresAt = claims.expiration.time
            )

            Result.success(tokenClaims)
        } catch (e: ExpiredJwtException) {
            logger.warn("Access token expired: {}", e.message)
            Result.failure(IllegalArgumentException("Token expired"))
        } catch (e: UnsupportedJwtException) {
            logger.warn("Unsupported JWT token: {}", e.message)
            Result.failure(IllegalArgumentException("Unsupported token"))
        } catch (e: MalformedJwtException) {
            logger.warn("Malformed JWT token: {}", e.message)
            Result.failure(IllegalArgumentException("Malformed token"))
        } catch (e: SignatureException) {
            logger.warn("Invalid JWT signature: {}", e.message)
            Result.failure(IllegalArgumentException("Invalid signature"))
        } catch (e: IllegalArgumentException) {
            logger.warn("JWT claims string is empty: {}", e.message)
            Result.failure(IllegalArgumentException("Empty claims"))
        } catch (e: Exception) {
            logger.error("Error validating access token", e)
            Result.failure(e)
        }
    }

    override fun validateRefreshToken(token: String): Result<TokenClaims> {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(refreshTokenKey)
                .setAllowedClockSkewSeconds(jwtConfig.clockSkewSeconds)
                .build()
                .parseClaimsJws(token)
                .body

            // Verify it's a refresh token
            val tokenType = claims.get("type", String::class.java)
            if (tokenType != "refresh") {
                return Result.failure(IllegalArgumentException("Not a refresh token"))
            }

            val tokenClaims = TokenClaims(
                userId = claims.subject,
                phoneNumber = claims.get("phoneNumber", String::class.java),
                role = "", // Refresh tokens don't contain role info
                permissions = emptyList(),
                issuedAt = claims.issuedAt.time,
                expiresAt = claims.expiration.time
            )

            Result.success(tokenClaims)
        } catch (e: ExpiredJwtException) {
            logger.warn("Refresh token expired: {}", e.message)
            Result.failure(IllegalArgumentException("Token expired"))
        } catch (e: Exception) {
            logger.error("Error validating refresh token", e)
            Result.failure(e)
        }
    }

    override fun extractUserId(token: String): Result<String> {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(accessTokenKey)
                .build()
                .parseClaimsJws(token)
                .body

            Result.success(claims.subject)
        } catch (e: Exception) {
            logger.error("Error extracting user ID from token", e)
            Result.failure(e)
        }
    }

    override fun isTokenExpired(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(accessTokenKey)
                .build()
                .parseClaimsJws(token)
                .body

            claims.expiration.before(Date())
        } catch (e: ExpiredJwtException) {
            true
        } catch (e: Exception) {
            logger.error("Error checking token expiration", e)
            true
        }
    }
}