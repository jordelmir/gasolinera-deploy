package com.gasolinerajsm.auth.infrastructure

import com.gasolinerajsm.testing.shared.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Integration Tests for Redis Token Cache
 * Tests real Redis interactions with Redis TestContainer
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis Token Cache Integration Tests")
class RedisTokenCacheIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    private lateinit var tokenCache: RedisTokenCache

    @BeforeEach
    fun setUp() {
        tokenCache = RedisTokenCache(redisTemplate)
        // Clean Redis before each test
        redisTemplate.connectionFactory?.connection?.flushAll()
    }

    @Nested
    @DisplayName("Token Storage Operations")
    inner class TokenStorageOperations {

        @Test
        @DisplayName("Should store and retrieve access token successfully")
        fun shouldStoreAndRetrieveAccessTokenSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val accessToken = "access.jwt.token.123"
            val ttl = Duration.ofHours(1)

            // When
            tokenCache.storeAccessToken(userId, accessToken, ttl)
            val retrievedToken = tokenCache.getAccessToken(userId)

            // Then
            assertThat(retrievedToken).isEqualTo(accessToken)
        }

        @Test
        @DisplayName("Should store and retrieve refresh token successfully")
        fun shouldStoreAndRetrieveRefreshTokenSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val refreshToken = "refresh.jwt.token.456"
            val ttl = Duration.ofDays(7)

            // When
            tokenCache.storeRefreshToken(userId, refreshToken, ttl)
            val retrievedToken = tokenCache.getRefreshToken(userId)

            // Then
            assertThat(retrievedToken).isEqualTo(refreshToken)
        }

        @Test
        @DisplayName("Should return null when token not found")
        fun shouldReturnNullWhenTokenNotFound() {
            // Given
            val nonExistentUserId = UUID.randomUUID()

            // When
            val accessToken = tokenCache.getAccessToken(nonExistentUserId)
            val refreshToken = tokenCache.getRefreshToken(nonExistentUserId)

            // Then
            assertThat(accessToken).isNull()
            assertThat(refreshToken).isNull()
        }

        @Test
        @DisplayName("Should handle token expiration correctly")
        fun shouldHandleTokenExpirationCorrectly() {
            // Given
            val userId = UUID.randomUUID()
            val accessToken = "short.lived.token"
            val shortTtl = Duration.ofMillis(100)

            // When
            tokenCache.storeAccessToken(userId, accessToken, shortTtl)

            // Verify token exists initially
            assertThat(tokenCache.getAccessToken(userId)).isEqualTo(accessToken)

            // Wait for expiration
            Thread.sleep(150)

            // Then
            assertThat(tokenCache.getAccessToken(userId)).isNull()
        }
    }

    @Nested
    @DisplayName("Token Invalidation Operations")
    inner class TokenInvalidationOperations {

        @Test
        @DisplayName("Should invalidate access token successfully")
        fun shouldInvalidateAccessTokenSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val accessToken = "access.token.to.invalidate"
            tokenCache.storeAccessToken(userId, accessToken, Duration.ofHours(1))

            // Verify token exists
            assertThat(tokenCache.getAccessToken(userId)).isEqualTo(accessToken)

            // When
            tokenCache.invalidateAccessToken(userId)

            // Then
            assertThat(tokenCache.getAccessToken(userId)).isNull()
        }

        @Test
        @DisplayName("Should invalidate refresh token successfully")
        fun shouldInvalidateRefreshTokenSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val refreshToken = "refresh.token.to.invalidate"
            tokenCache.storeRefreshToken(userId, refreshToken, Duration.ofDays(7))

            // Verify token exists
            assertThat(tokenCache.getRefreshToken(userId)).isEqualTo(refreshToken)

            // When
            tokenCache.invalidateRefreshToken(userId)

            // Then
            assertThat(tokenCache.getRefreshToken(userId)).isNull()
        }

        @Test
        @DisplayName("Should invalidate all user tokens successfully")
        fun shouldInvalidateAllUserTokensSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val accessToken = "access.token"
            val refreshToken = "refresh.token"

            tokenCache.storeAccessToken(userId, accessToken, Duration.ofHours(1))
            tokenCache.storeRefreshToken(userId, refreshToken, Duration.ofDays(7))

            // Verify tokens exist
            assertThat(tokenCache.getAccessToken(userId)).isEqualTo(accessToken)
            assertThat(tokenCache.getRefreshToken(userId)).isEqualTo(refreshToken)

            // When
            tokenCache.invalidateAllUserTokens(userId)

            // Then
            assertThat(tokenCache.getAccessToken(userId)).isNull()
            assertThat(tokenCache.getRefreshToken(userId)).isNull()
        }
    }

    @Nested
    @DisplayName("Blacklist Operations")
    inner class BlacklistOperations {

        @Test
        @DisplayName("Should blacklist token successfully")
        fun shouldBlacklistTokenSuccessfully() {
            // Given
            val tokenId = "token.id.123"
            val expirationTime = System.currentTimeMillis() + 3600000 // 1 hour

            // When
            tokenCache.blacklistToken(tokenId, expirationTime)
            val isBlacklisted = tokenCache.isTokenBlacklisted(tokenId)

            // Then
            assertThat(isBlacklisted).isTrue()
        }

        @Test
        @DisplayName("Should return false for non-blacklisted token")
        fun shouldReturnFalseForNonBlacklistedToken() {
            // Given
            val tokenId = "non.blacklisted.token"

            // When
            val isBlacklisted = tokenCache.isTokenBlacklisted(tokenId)

            // Then
            assertThat(isBlacklisted).isFalse()
        }

        @Test
        @DisplayName("Should handle blacklist expiration correctly")
        fun shouldHandleBlacklistExpirationCorrectly() {
            // Given
            val tokenId = "expiring.blacklist.token"
            val shortExpirationTime = System.currentTimeMillis() + 100 // 100ms

            // When
            tokenCache.blacklistToken(tokenId, shortExpirationTime)

            // Verify token is blacklisted initially
            assertThat(tokenCache.isTokenBlacklisted(tokenId)).isTrue()

            // Wait for expiration
            Thread.sleep(150)

            // Then
            assertThat(tokenCache.isTokenBlacklisted(tokenId)).isFalse()
        }
    }

    @Nested
    @DisplayName("Session Management Operations")
    inner class SessionManagementOperations {

        @Test
        @DisplayName("Should store and retrieve user session successfully")
        fun shouldStoreAndRetrieveUserSessionSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val sessionData = UserSession(
                userId = userId,
                deviceId = "device-123",
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0...",
                loginTime = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis()
            )
            val ttl = Duration.ofHours(24)

            // When
            tokenCache.storeUserSession(userId, sessionData, ttl)
            val retrievedSession = tokenCache.getUserSession(userId)

            // Then
            assertThat(retrievedSession).isNotNull
            assertThat(retrievedSession?.userId).isEqualTo(userId)
            assertThat(retrievedSession?.deviceId).isEqualTo("device-123")
            assertThat(retrievedSession?.ipAddress).isEqualTo("192.168.1.1")
        }

        @Test
        @DisplayName("Should update user session activity successfully")
        fun shouldUpdateUserSessionActivitySuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val sessionData = UserSession(
                userId = userId,
                deviceId = "device-123",
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0...",
                loginTime = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis()
            )

            tokenCache.storeUserSession(userId, sessionData, Duration.ofHours(24))
            val originalActivity = sessionData.lastActivity

            // Wait a bit to ensure different timestamp
            Thread.sleep(10)

            // When
            tokenCache.updateSessionActivity(userId)
            val updatedSession = tokenCache.getUserSession(userId)

            // Then
            assertThat(updatedSession).isNotNull
            assertThat(updatedSession?.lastActivity).isGreaterThan(originalActivity)
        }

        @Test
        @DisplayName("Should invalidate user session successfully")
        fun shouldInvalidateUserSessionSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val sessionData = UserSession(
                userId = userId,
                deviceId = "device-123",
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0...",
                loginTime = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis()
            )

            tokenCache.storeUserSession(userId, sessionData, Duration.ofHours(24))

            // Verify session exists
            assertThat(tokenCache.getUserSession(userId)).isNotNull

            // When
            tokenCache.invalidateUserSession(userId)

            // Then
            assertThat(tokenCache.getUserSession(userId)).isNull()
        }
    }

    @Nested
    @DisplayName("Rate Limiting Operations")
    inner class RateLimitingOperations {

        @Test
        @DisplayName("Should track login attempts successfully")
        fun shouldTrackLoginAttemptsSuccessfully() {
            // Given
            val identifier = "test@gasolinera.com"
            val windowDuration = Duration.ofMinutes(15)

            // When
            tokenCache.incrementLoginAttempts(identifier, windowDuration)
            tokenCache.incrementLoginAttempts(identifier, windowDuration)
            tokenCache.incrementLoginAttempts(identifier, windowDuration)

            val attempts = tokenCache.getLoginAttempts(identifier)

            // Then
            assertThat(attempts).isEqualTo(3)
        }

        @Test
        @DisplayName("Should reset login attempts successfully")
        fun shouldResetLoginAttemptsSuccessfully() {
            // Given
            val identifier = "test@gasolinera.com"
            val windowDuration = Duration.ofMinutes(15)

            tokenCache.incrementLoginAttempts(identifier, windowDuration)
            tokenCache.incrementLoginAttempts(identifier, windowDuration)

            // Verify attempts exist
            assertThat(tokenCache.getLoginAttempts(identifier)).isEqualTo(2)

            // When
            tokenCache.resetLoginAttempts(identifier)

            // Then
            assertThat(tokenCache.getLoginAttempts(identifier)).isEqualTo(0)
        }

        @Test
        @DisplayName("Should handle login attempts expiration correctly")
        fun shouldHandleLoginAttemptsExpirationCorrectly() {
            // Given
            val identifier = "test@gasolinera.com"
            val shortWindowDuration = Duration.ofMillis(100)

            // When
            tokenCache.incrementLoginAttempts(identifier, shortWindowDuration)
            tokenCache.incrementLoginAttempts(identifier, shortWindowDuration)

            // Verify attempts exist initially
            assertThat(tokenCache.getLoginAttempts(identifier)).isEqualTo(2)

            // Wait for expiration
            Thread.sleep(150)

            // Then
            assertThat(tokenCache.getLoginAttempts(identifier)).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Performance and Concurrency Tests")
    inner class PerformanceAndConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent token operations successfully")
        fun shouldHandleConcurrentTokenOperationsSuccessfully() {
            // Given
            val userId = UUID.randomUUID()
            val numberOfThreads = 10
            val operationsPerThread = 100

            // When
            val threads = (1..numberOfThreads).map { threadId ->
                Thread {
                    repeat(operationsPerThread) { operationId ->
                        val token = "token-$threadId-$operationId"
                        tokenCache.storeAccessToken(userId, token, Duration.ofMinutes(1))
                        tokenCache.getAccessToken(userId)
                        tokenCache.invalidateAccessToken(userId)
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            // Then
            // No exceptions should be thrown and operations should complete
            assertThat(tokenCache.getAccessToken(userId)).isNull()
        }

        @Test
        @DisplayName("Should handle large token storage efficiently")
        fun shouldHandleLargeTokenStorageEfficiently() {
            // Given
            val numberOfTokens = 1000
            val tokens = mutableMapOf<UUID, String>()

            // When
            val startTime = System.currentTimeMillis()

            repeat(numberOfTokens) { i ->
                val userId = UUID.randomUUID()
                val token = "large-token-$i-${UUID.randomUUID()}"
                tokens[userId] = token
                tokenCache.storeAccessToken(userId, token, Duration.ofHours(1))
            }

            val storageTime = System.currentTimeMillis() - startTime

            // Verify retrieval
            val retrievalStartTime = System.currentTimeMillis()
            tokens.forEach { (userId, expectedToken) ->
                val retrievedToken = tokenCache.getAccessToken(userId)
                assertThat(retrievedToken).isEqualTo(expectedToken)
            }
            val retrievalTime = System.currentTimeMillis() - retrievalStartTime

            // Then
            // Operations should complete in reasonable time (adjust thresholds as needed)
            assertThat(storageTime).isLessThan(5000) // 5 seconds
            assertThat(retrievalTime).isLessThan(5000) // 5 seconds
        }
    }

    // Data class for user session
    data class UserSession(
        val userId: UUID,
        val deviceId: String,
        val ipAddress: String,
        val userAgent: String,
        val loginTime: Long,
        val lastActivity: Long
    )

    // Mock implementation of RedisTokenCache for testing
    class RedisTokenCache(private val redisTemplate: RedisTemplate<String, Any>) {

        fun storeAccessToken(userId: UUID, token: String, ttl: Duration) {
            val key = "access_token:$userId"
            redisTemplate.opsForValue().set(key, token, ttl.toMillis(), TimeUnit.MILLISECONDS)
        }

        fun getAccessToken(userId: UUID): String? {
            val key = "access_token:$userId"
            return redisTemplate.opsForValue().get(key) as String?
        }

        fun storeRefreshToken(userId: UUID, token: String, ttl: Duration) {
            val key = "refresh_token:$userId"
            redisTemplate.opsForValue().set(key, token, ttl.toMillis(), TimeUnit.MILLISECONDS)
        }

        fun getRefreshToken(userId: UUID): String? {
            val key = "refresh_token:$userId"
            return redisTemplate.opsForValue().get(key) as String?
        }

        fun invalidateAccessToken(userId: UUID) {
            val key = "access_token:$userId"
            redisTemplate.delete(key)
        }

        fun invalidateRefreshToken(userId: UUID) {
            val key = "refresh_token:$userId"
            redisTemplate.delete(key)
        }

        fun invalidateAllUserTokens(userId: UUID) {
            invalidateAccessToken(userId)
            invalidateRefreshToken(userId)
            invalidateUserSession(userId)
        }

        fun blacklistToken(tokenId: String, expirationTime: Long) {
            val key = "blacklist:$tokenId"
            val ttl = expirationTime - System.currentTimeMillis()
            if (ttl > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS)
            }
        }

        fun isTokenBlacklisted(tokenId: String): Boolean {
            val key = "blacklist:$tokenId"
            return redisTemplate.hasKey(key)
        }

        fun storeUserSession(userId: UUID, session: UserSession, ttl: Duration) {
            val key = "session:$userId"
            redisTemplate.opsForValue().set(key, session, ttl.toMillis(), TimeUnit.MILLISECONDS)
        }

        fun getUserSession(userId: UUID): UserSession? {
            val key = "session:$userId"
            return redisTemplate.opsForValue().get(key) as UserSession?
        }

        fun updateSessionActivity(userId: UUID) {
            val session = getUserSession(userId)
            if (session != null) {
                val updatedSession = session.copy(lastActivity = System.currentTimeMillis())
                storeUserSession(userId, updatedSession, Duration.ofHours(24))
            }
        }

        fun invalidateUserSession(userId: UUID) {
            val key = "session:$userId"
            redisTemplate.delete(key)
        }

        fun incrementLoginAttempts(identifier: String, windowDuration: Duration) {
            val key = "login_attempts:$identifier"
            redisTemplate.opsForValue().increment(key)
            redisTemplate.expire(key, windowDuration.toMillis(), TimeUnit.MILLISECONDS)
        }

        fun getLoginAttempts(identifier: String): Int {
            val key = "login_attempts:$identifier"
            return (redisTemplate.opsForValue().get(key) as? Int) ?: 0
        }

        fun resetLoginAttempts(identifier: String) {
            val key = "login_attempts:$identifier"
            redisTemplate.delete(key)
        }
    }
}