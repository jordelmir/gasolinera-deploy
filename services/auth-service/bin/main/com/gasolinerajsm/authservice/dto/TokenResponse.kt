package com.gasolinerajsm.authservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for JWT token operations
 */
data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("refresh_token")
    val refreshToken: String,

    @JsonProperty("token_type")
    val tokenType: String = "Bearer",

    @JsonProperty("expires_in")
    val expiresIn: Long,

    @JsonProperty("refresh_expires_in")
    val refreshExpiresIn: Long,

    @JsonProperty("scope")
    val scope: String? = null,

    @JsonProperty("session_id")
    val sessionId: String,

    @JsonProperty("issued_at")
    val issuedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Response DTO for token refresh operations
 */
data class RefreshTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("token_type")
    val tokenType: String = "Bearer",

    @JsonProperty("expires_in")
    val expiresIn: Long,

    @JsonProperty("session_id")
    val sessionId: String,

    @JsonProperty("issued_at")
    val issuedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Response DTO for token validation
 */
data class TokenValidationResponse(
    @JsonProperty("valid")
    val valid: Boolean,

    @JsonProperty("user_id")
    val userId: Long? = null,

    @JsonProperty("roles")
    val roles: List<String> = emptyList(),

    @JsonProperty("permissions")
    val permissions: Set<String> = emptySet(),

    @JsonProperty("expires_at")
    val expiresAt: LocalDateTime? = null,

    @JsonProperty("session_id")
    val sessionId: String? = null,

    @JsonProperty("phone_verified")
    val phoneVerified: Boolean = false,

    @JsonProperty("account_status")
    val accountStatus: String? = null
)

/**
 * Response DTO for token introspection (detailed token information)
 */
data class TokenIntrospectionResponse(
    @JsonProperty("active")
    val active: Boolean,

    @JsonProperty("sub")
    val subject: String? = null,

    @JsonProperty("aud")
    val audience: String? = null,

    @JsonProperty("iss")
    val issuer: String? = null,

    @JsonProperty("exp")
    val expiration: Long? = null,

    @JsonProperty("iat")
    val issuedAt: Long? = null,

    @JsonProperty("nbf")
    val notBefore: Long? = null,

    @JsonProperty("jti")
    val tokenId: String? = null,

    @JsonProperty("token_type")
    val tokenType: String? = null,

    @JsonProperty("scope")
    val scope: String? = null,

    @JsonProperty("client_id")
    val clientId: String? = null,

    @JsonProperty("username")
    val username: String? = null,

    @JsonProperty("roles")
    val roles: List<String> = emptyList(),

    @JsonProperty("permissions")
    val permissions: Set<String> = emptySet()
)
