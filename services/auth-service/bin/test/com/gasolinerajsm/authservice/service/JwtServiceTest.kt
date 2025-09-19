package com.gasolinerajsm.authservice.service

import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.model.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@DisplayName("JWT Service Tests")
class JwtServiceTest {

    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var valueOperations: ValueOperations<String, String>
    private lateinit var jwtService: JwtService

    private val testSecretKey = "test-secret-key-for-jwt-signing-in-tests-environment-only-must-be-32-chars"
    private val testRefreshSecretKey = "test-refresh-secret-key-for-jwt-refresh-in-tests-environment-only-must-be-32-chars"
    private val accessTokenExpiration = 3600L // 1 hour
    private val refreshTokenExpiration = 86400L // 24 hours
    private val issuer = "gasolinera-jsm-test"
    private val audience = "gasolinera-jsm-test-users"

    @BeforeEach
    fun setUp() {
        redisTemplate = mockk()
        valueOperations = mockk()

        every { redisTemplate.opsForValue() } returns valueOperations
        every { redisTemplate.hasKey(any()) } returns false
        every { redisTemplate.keys(any()) } returns emptySet()
        every { redisTemplate.delete(any<String>()) } returns true
        every { redisTemplate.delete(any<Collection<String>>()) } returns 1L
        every { valueOperations.set(any(), any(), any(), any<TimeUnit>()) } returns Unit

        jwtService = JwtService(
            jwtSecretKey = testSecretKey,
            refreshSecretKey = testRefreshSecretKey,
            accessTokenExpirationSeconds = accessTokenExpiration,
            refreshTokenExpirationSeconds = refreshTokenExpiration,
            issuer = issuer,
            audience = audience,
            redisTemplate = redisTemplate
        )
    }

    @Nested
    @DisplayName("Token Generation Tests")
    inner class TokenGenerationTests {

        private val testUser = User(
            id = 1L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.CUSTOMER,
            isActive = true,
            isPhoneVerified = true
        )

        @Test
        @DisplayName("Should generate valid access token")
        fun shouldGenerateValidAccessToken() {
            // When
            val token = jwtService.generateAccessToken(testUser)

            // Then
            assertNotNull(token)
            assertTrue(token.isNotBlank())
            assertTrue(jwtService.validateAccessToken(token))

            // Verify Redis interaction for session storage
            verify { valueOperations.set(any(), any(), accessTokenExpiration, TimeUnit.SECONDS) }
        }

        @Test
        @DisplayName("Should generate valid refresh token")
        fun shouldGenerateValidRefreshToken() {
            // When
            val token = jwtService.generateRefreshToken(testUser)

            // Then
            assertNotNull(token)
            assertTrue(token.isNotBlank())
            assertTrue(jwtService.validateRefreshToken(token))

            // Verify Redis interaction for refresh token storage
            verify { valueOperations.set(any(), any(), refreshTokenExpiration, TimeUnit.SECONDS) }
        }

        @Test
        @DisplayName("Should generate tokens with custom session ID")
        fun shouldGenerateTokensWithCustomSessionId() {
            // Given
            val customSessionId = "custom-session-123"

            // When
            val token = jwtService.generateAccessToken(testUser, customSessionId)

            // Then
            assertNotNull(token)
            assertEquals(customSessionId, jwtService.getSessionIdFromToken(token))
        }

        @Test
        @DisplayName("Should include user information in access token")
        fun shouldIncludeUserInformationInAccessToken() {
            // When
            val token = jwtService.generateAccessToken(testUser)

            // Then
            assertEquals(testUser.id, jwtService.getUserIdFromToken(token))
            assertEquals(testUser.phoneNumber, jwtService.getPhoneNumberFromToken(token))
            assertEquals(listOf(testUser.role.name), jwtService.getRolesFromToken(token))
            assertEquals(testUser.role.permissions, jwtService.getPermissionsFromToken(token))
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    inner class TokenValidationTests {

        private val testUser = User(
            id = 1L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.CUSTOMER,
            isActive = true,
            isPhoneVerified = true
        )

        @Test
        @DisplayName("Should validate correct access token")
        fun shouldValidateCorrectAccessToken() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When & Then
            assertTrue(jwtService.validateAccessToken(token))
        }

        @Test
        @DisplayName("Should validate correct refresh token")
        fun shouldValidateCorrectRefreshToken() {
            // Given
            val token = jwtService.generateRefreshToken(testUser)

            // When & Then
            assertTrue(jwtService.validateRefreshToken(token))
        }

        @Test
        @DisplayName("Should reject invalid token")
        fun shouldRejectInvalidToken() {
            // Given
            val invalidToken = "invalid.token.here"

            // When & Then
            assertFalse(jwtService.validateAccessToken(invalidToken))
            assertFalse(jwtService.validateRefreshToken(invalidToken))
        }

        @Test
        @DisplayName("Should reject access token as refresh token")
        fun shouldRejectAccessTokenAsRefreshToken() {
            // Given
            val accessToken = jwtService.generateAccessToken(testUser)

            // When & Then
            assertFalse(jwtService.validateRefreshToken(accessToken))
        }

        @Test
        @DisplayName("Should reject refresh token as access token")
        fun shouldRejectRefreshTokenAsAccessToken() {
            // Given
            val refreshToken = jwtService.generateRefreshToken(testUser)

            // When & Then
            assertFalse(jwtService.validateAccessToken(refreshToken))
        }

        @Test
        @DisplayName("Should reject blacklisted token")
        fun shouldRejectBlacklistedToken() {
            // Given
            val token = jwtService.generateAccessToken(testUser)
            every { redisTemplate.hasKey(any()) } returns true

            // When & Then
            assertFalse(jwtService.validateAccessToken(token))
        }

        @Test
        @DisplayName("Should reject token with invalid session")
        fun shouldRejectTokenWithInvalidSession() {
            // Given
            val token = jwtService.generateAccessToken(testUser)
            every { redisTemplate.keys(any()) } returns emptySet() // No valid session

            // When & Then
            assertFalse(jwtService.validateAccessToken(token))
        }
    }

    @Nested
    @DisplayName("Token Claims Extraction Tests")
    inner class TokenClaimsExtractionTests {

        private val testUser = User(
            id = 123L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.STATION_ADMIN,
            isActive = true,
            isPhoneVerified = true
        )

        @Test
        @DisplayName("Should extract user ID from token")
        fun shouldExtractUserIdFromToken() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When
            val userId = jwtService.getUserIdFromToken(token)

            // Then
            assertEquals(testUser.id, userId)
        }

        @Test
        @DisplayName("Should extract roles from token")
        fun shouldExtractRolesFromToken() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When
            val roles = jwtService.getRolesFromToken(token)

            // Then
            assertEquals(listOf(testUser.role.name), roles)
        }

        @Test
        @DisplayName("Should extract permissions from token")
        fun shouldExtractPermissionsFromToken() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When
            val permissions = jwtService.getPermissionsFromToken(token)

            // Then
            assertEquals(testUser.role.permissions, permissions)
        }

        @Test
        @DisplayName("Should extract phone number from token")
        fun shouldExtractPhoneNumberFromToken() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When
            val phoneNumber = jwtService.getPhoneNumberFromToken(token)

            // Then
            assertEquals(testUser.phoneNumber, phoneNumber)
        }

        @Test
        @DisplayName("Should extract session ID from token")
        fun shouldExtractSessionIdFromToken() {
            // Given
            val sessionId = "test-session-123"
            val token = jwtService.generateAccessToken(testUser, sessionId)

            // When
            val extractedSessionId = jwtService.getSessionIdFromToken(token)

            // Then
            assertEquals(sessionId, extractedSessionId)
        }

        @Test
        @DisplayName("Should return null for invalid token claims")
        fun shouldReturnNullForInvalidTokenClaims() {
            // Given
            val invalidToken = "invalid.token.here"

            // When & Then
            assertNull(jwtService.getUserIdFromToken(invalidToken))
            assertNull(jwtService.getPhoneNumberFromToken(invalidToken))
            assertNull(jwtService.getSessionIdFromToken(invalidToken))
            assertTrue(jwtService.getRolesFromToken(invalidToken).isEmpty())
            assertTrue(jwtService.getPermissionsFromToken(invalidToken).isEmpty())
        }
    }

    @Nested
    @DisplayName("Permission and Role Checking Tests")
    inner class PermissionAndRoleCheckingTests {

        private val testUser = User(
            id = 1L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.EMPLOYEE,
            isActive = true,
            isPhoneVerified = true
        )

        @Test
        @DisplayName("Should check user permissions correctly")
        fun shouldCheckUserPermissionsCorrectly() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When & Then
            assertTrue(jwtService.hasPermission(token, "coupon:validate"))
            assertTrue(jwtService.hasPermission(token, "redemption:process"))
            assertFalse(jwtService.hasPermission(token, "station:manage"))
        }

        @Test
        @DisplayName("Should check user roles correctly")
        fun shouldCheckUserRolesCorrectly() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When & Then
            assertTrue(jwtService.hasRole(token, UserRole.EMPLOYEE))
            assertFalse(jwtService.hasRole(token, UserRole.CUSTOMER))
            assertFalse(jwtService.hasRole(token, UserRole.SYSTEM_ADMIN))
        }
    }

    @Nested
    @DisplayName("Token Blacklisting Tests")
    inner class TokenBlacklistingTests {

        private val testUser = User(
            id = 1L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.CUSTOMER,
            isActive = true,
            isPhoneVerified = true
        )

        @Test
        @DisplayName("Should blacklist token successfully")
        fun shouldBlacklistTokenSuccessfully() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When
            val result = jwtService.blacklistToken(token)

            // Then
            assertTrue(result)
            verify { valueOperations.set(any(), eq("blacklisted"), any(), eq(TimeUnit.SECONDS)) }
        }

        @Test
        @DisplayName("Should invalidate refresh token successfully")
        fun shouldInvalidateRefreshTokenSuccessfully() {
            // Given
            val token = jwtService.generateRefreshToken(testUser)

            // When
            val result = jwtService.invalidateRefreshToken(token)

            // Then
            assertTrue(result)
            verify { redisTemplate.delete(any<String>()) }
        }

        @Test
        @DisplayName("Should invalidate all user tokens")
        fun shouldInvalidateAllUserTokens() {
            // Given
            val userId = testUser.id
            every { redisTemplate.keys("jwt:session:*:$userId") } returns setOf("session1", "session2")
            every { redisTemplate.keys("jwt:refresh:*:$userId") } returns setOf("refresh1", "refresh2")

            // When
            jwtService.invalidateAllUserTokens(userId)

            // Then
            verify { redisTemplate.delete(setOf("session1", "session2")) }
            verify { redisTemplate.delete(setOf("refresh1", "refresh2")) }
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    inner class TokenExpirationTests {

        private val testUser = User(
            id = 1L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.CUSTOMER,
            isActive = true,
            isPhoneVerified = true
        )

        @Test
        @DisplayName("Should get token expiration time")
        fun shouldGetTokenExpirationTime() {
            // Given
            val token = jwtService.generateAccessToken(testUser)

            // When
            val expiration = jwtService.getTokenExpiration(token)

            // Then
            assertNotNull(expiration)
            assertTrue(expiration!!.isAfter(LocalDateTime.now()))
        }

        @Test
        @DisplayName("Should detect token near expiry")
        fun shouldDetectTokenNearExpiry() {
            // Given - Create service with very short expiration for testing
            val shortExpirationService = JwtService(
                jwtSecretKey = testSecretKey,
                refreshSecretKey = testRefreshSecretKey,
                accessTokenExpirationSeconds = 300, // 5 minutes
                refreshTokenExpirationSeconds = refreshTokenExpiration,
                issuer = issuer,
                audience = audience,
                redisTemplate = redisTemplate
            )
            val token = shortExpirationService.generateAccessToken(testUser)

            // When & Then
            assertTrue(shortExpirationService.isTokenNearExpiry(token, 10)) // 10 minutes threshold
            assertFalse(shortExpirationService.isTokenNearExpiry(token, 1)) // 1 minute threshold
        }

        @Test
        @DisplayName("Should return null expiration for invalid token")
        fun shouldReturnNullExpirationForInvalidToken() {
            // Given
            val invalidToken = "invalid.token.here"

            // When
            val expiration = jwtService.getTokenExpiration(invalidToken)

            // Then
            assertNull(expiration)
        }
    }

    @Nested
    @DisplayName("Security Tests")
    inner class SecurityTests {

        @Test
        @DisplayName("Should reject token with insufficient secret key length")
        fun shouldRejectTokenWithInsufficientSecretKeyLength() {
            // Given
            val shortSecretKey = "short"

            // When & Then
            assertThrows<IllegalArgumentException> {
                JwtService(
                    jwtSecretKey = shortSecretKey,
                    refreshSecretKey = testRefreshSecretKey,
                    accessTokenExpirationSeconds = accessTokenExpiration,
                    refreshTokenExpirationSeconds = refreshTokenExpiration,
                    issuer = issuer,
                    audience = audience,
                    redisTemplate = redisTemplate
                )
            }
        }

        @Test
        @DisplayName("Should reject token signed with different key")
        fun shouldRejectTokenSignedWithDifferentKey() {
            // Given
            val differentKeyService = JwtService(
                jwtSecretKey = "different-secret-key-for-jwt-signing-in-tests-environment-only-must-be-32-chars",
                refreshSecretKey = testRefreshSecretKey,
                accessTokenExpirationSeconds = accessTokenExpiration,
                refreshTokenExpirationSeconds = refreshTokenExpiration,
                issuer = issuer,
                audience = audience,
                redisTemplate = redisTemplate
            )

            val testUser = User(
                id = 1L,
                phoneNumber = "+1234567890",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.CUSTOMER,
                isActive = true,
                isPhoneVerified = true
            )

            val tokenFromDifferentService = differentKeyService.generateAccessToken(testUser)

            // When & Then
            assertFalse(jwtService.validateAccessToken(tokenFromDifferentService))
        }

        @Test
        @DisplayName("Should handle malformed tokens gracefully")
        fun shouldHandleMalformedTokensGracefully() {
            // Given
            val malformedTokens = listOf(
                "not.a.jwt",
                "header.payload", // Missing signature
                "header.payload.signature.extra", // Too many parts
                "", // Empty string
                "   ", // Whitespace only
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature" // Invalid payload
            )

            // When & Then
            malformedTokens.forEach { token ->
                assertFalse(jwtService.validateAccessToken(token), "Should reject malformed token: $token")
                assertFalse(jwtService.validateRefreshToken(token), "Should reject malformed token: $token")
                assertNull(jwtService.getUserIdFromToken(token), "Should return null for malformed token: $token")
            }
        }
    }
}