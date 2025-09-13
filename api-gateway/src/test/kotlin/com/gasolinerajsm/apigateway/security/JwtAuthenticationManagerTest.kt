package com.gasolinerajsm.apigateway.security

import com.gasolinerajsm.shared.vault.VaultClient
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.test.StepVerifier
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtAuthenticationManagerTest {

    private lateinit var vaultClient: VaultClient
    private lateinit var jwtProperties: JwtProperties
    private lateinit var jwtAuthenticationManager: JwtAuthenticationManager

    private val secretKey = "test-secret-key-for-jwt-authentication-manager-testing"

    @BeforeEach
    fun setup() {
        vaultClient = mock()
        jwtProperties = JwtProperties(secretKey = secretKey)
        jwtAuthenticationManager = JwtAuthenticationManager(vaultClient, jwtProperties)
    }

    @Test
    fun `should authenticate valid JWT token`() {
        // Given
        val token = createValidToken("testuser", listOf("USER"))
        val authentication = UsernamePasswordAuthenticationToken(null, token)

        // When & Then
        StepVerifier.create(jwtAuthenticationManager.authenticate(authentication))
            .assertNext { auth ->
                assertEquals("testuser", auth.name)
                assertTrue(auth.authorities.any { it.authority == "ROLE_USER" })
            }
            .verifyComplete()
    }

    @Test
    fun `should reject expired JWT token`() {
        // Given
        val expiredToken = createExpiredToken("testuser", listOf("USER"))
        val authentication = UsernamePasswordAuthenticationToken(null, expiredToken)

        // When & Then
        StepVerifier.create(jwtAuthenticationManager.authenticate(authentication))
            .expectError()
            .verify()
    }

    @Test
    fun `should reject invalid JWT token`() {
        // Given
        val invalidToken = "invalid.jwt.token"
        val authentication = UsernamePasswordAuthenticationToken(null, invalidToken)

        // When & Then
        StepVerifier.create(jwtAuthenticationManager.authenticate(authentication))
            .expectError()
            .verify()
    }

    @Test
    fun `should extract username from valid token`() {
        // Given
        val token = createValidToken("testuser", listOf("USER"))

        // When
        val username = jwtAuthenticationManager.getUsernameFromToken(token)

        // Then
        assertEquals("testuser", username)
    }

    @Test
    fun `should extract roles from valid token`() {
        // Given
        val roles = listOf("USER", "ADMIN")
        val token = createValidToken("testuser", roles)

        // When
        val extractedRoles = jwtAuthenticationManager.getRolesFromToken(token)

        // Then
        assertEquals(roles, extractedRoles)
    }

    @Test
    fun `should detect expired token`() {
        // Given
        val expiredToken = createExpiredToken("testuser", listOf("USER"))

        // When
        val isExpired = jwtAuthenticationManager.isTokenExpired(expiredToken)

        // Then
        assertTrue(isExpired)
    }

    @Test
    fun `should detect valid token as not expired`() {
        // Given
        val validToken = createValidToken("testuser", listOf("USER"))

        // When
        val isExpired = jwtAuthenticationManager.isTokenExpired(validToken)

        // Then
        assertFalse(isExpired)
    }

    @Test
    fun `should use vault secret when available`() {
        // Given
        val vaultSecret = "vault-secret-key"
        whenever(vaultClient.getSecret("secret/jwt")).thenReturn(mapOf("secret-key" to vaultSecret))

        val token = createTokenWithSecret("testuser", listOf("USER"), vaultSecret)
        val authentication = UsernamePasswordAuthenticationToken(null, token)

        // When & Then
        StepVerifier.create(jwtAuthenticationManager.authenticate(authentication))
            .assertNext { auth ->
                assertEquals("testuser", auth.name)
            }
            .verifyComplete()
    }

    private fun createValidToken(username: String, roles: List<String>): String {
        return createTokenWithSecret(username, roles, secretKey)
    }

    private fun createTokenWithSecret(username: String, roles: List<String>, secret: String): String {
        val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
        val now = Instant.now()

        return Jwts.builder()
            .setSubject(username)
            .claim("roles", roles)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
            .signWith(key)
            .compact()
    }

    private fun createExpiredToken(username: String, roles: List<String>): String {
        val key = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
        val now = Instant.now()

        return Jwts.builder()
            .setSubject(username)
            .claim("roles", roles)
            .setIssuedAt(Date.from(now.minus(2, ChronoUnit.HOURS)))
            .setExpiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
            .signWith(key)
            .compact()
    }
}