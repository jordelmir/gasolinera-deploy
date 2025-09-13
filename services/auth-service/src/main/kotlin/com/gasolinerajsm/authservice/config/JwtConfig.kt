package com.gasolinerajsm.authservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * JWT Configuration properties with Vault integration support
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
data class JwtConfig(

    @field:NotBlank(message = "JWT secret key cannot be blank")
    @field:Size(min = 32, message = "JWT secret key must be at least 32 characters long")
    var secretKey: String = "",

    @field:NotBlank(message = "JWT refresh secret key cannot be blank")
    @field:Size(min = 32, message = "JWT refresh secret key must be at least 32 characters long")
    var refreshSecretKey: String = "",

    @field:Min(value = 300, message = "Access token expiration must be at least 300 seconds (5 minutes)")
    var accessTokenExpiration: Long = 3600, // 1 hour

    @field:Min(value = 3600, message = "Refresh token expiration must be at least 3600 seconds (1 hour)")
    var refreshTokenExpiration: Long = 86400, // 24 hours

    @field:NotBlank(message = "JWT issuer cannot be blank")
    var issuer: String = "gasolinera-jsm-platform",

    @field:NotBlank(message = "JWT audience cannot be blank")
    var audience: String = "gasolinera-jsm-users",

    var algorithm: String = "HS256",

    var clockSkewSeconds: Long = 30,

    var enableBlacklist: Boolean = true,

    var enableSessionTracking: Boolean = true,

    var maxSessionsPerUser: Int = 5,

    var nearExpiryThresholdMinutes: Long = 5
) {

    /**
     * Validates the configuration after properties are bound
     */
    fun validate() {
        require(secretKey.isNotBlank()) { "JWT secret key must not be blank" }
        require(refreshSecretKey.isNotBlank()) { "JWT refresh secret key must not be blank" }
        require(secretKey.length >= 32) { "JWT secret key must be at least 32 characters long" }
        require(refreshSecretKey.length >= 32) { "JWT refresh secret key must be at least 32 characters long" }
        require(secretKey != refreshSecretKey) { "JWT secret key and refresh secret key must be different" }
        require(accessTokenExpiration >= 300) { "Access token expiration must be at least 300 seconds" }
        require(refreshTokenExpiration >= 3600) { "Refresh token expiration must be at least 3600 seconds" }
        require(refreshTokenExpiration > accessTokenExpiration) { "Refresh token expiration must be greater than access token expiration" }
        require(issuer.isNotBlank()) { "JWT issuer must not be blank" }
        require(audience.isNotBlank()) { "JWT audience must not be blank" }
        require(maxSessionsPerUser > 0) { "Max sessions per user must be greater than 0" }
    }

    /**
     * Gets access token expiration in milliseconds
     */
    fun getAccessTokenExpirationMs(): Long = accessTokenExpiration * 1000

    /**
     * Gets refresh token expiration in milliseconds
     */
    fun getRefreshTokenExpirationMs(): Long = refreshTokenExpiration * 1000

    /**
     * Checks if the configuration is for production environment
     */
    fun isProductionConfig(): Boolean {
        return !secretKey.contains("development") &&
               !secretKey.contains("test") &&
               !refreshSecretKey.contains("development") &&
               !refreshSecretKey.contains("test")
    }

    /**
     * Gets security level based on configuration
     */
    fun getSecurityLevel(): SecurityLevel {
        return when {
            !isProductionConfig() -> SecurityLevel.DEVELOPMENT
            secretKey.length >= 64 && refreshSecretKey.length >= 64 -> SecurityLevel.HIGH
            secretKey.length >= 48 && refreshSecretKey.length >= 48 -> SecurityLevel.MEDIUM
            else -> SecurityLevel.LOW
        }
    }
}

/**
 * Security levels for JWT configuration
 */
enum class SecurityLevel {
    DEVELOPMENT,
    LOW,
    MEDIUM,
    HIGH
}