package com.gasolinerajsm.shared.resilience

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FallbackServiceTest {

    private lateinit var fallbackService: FallbackService

    @BeforeEach
    fun setup() {
        fallbackService = FallbackService()
    }

    @Test
    fun `should return coupon fallback response`() {
        // Given
        val context = FallbackContext(
            operationName = "coupon-generation",
            lastException = RuntimeException("Service unavailable")
        )

        // When
        val fallback = fallbackService.getCouponFallback(context)

        // Then
        assertNotNull(fallback)
        assertEquals(false, fallback.success)
        assertTrue(fallback.message.contains("cupones"))
        assertEquals("Service unavailable", fallback.fallbackReason)
        assertEquals(Duration.ofMinutes(5), fallback.retryAfter)
    }

    @Test
    fun `should return auth fallback response`() {
        // Given
        val context = FallbackContext(
            operationName = "user-authentication",
            lastException = RuntimeException("Auth service down")
        )

        // When
        val fallback = fallbackService.getAuthFallback(context)

        // Then
        assertNotNull(fallback)
        assertEquals(false, fallback.success)
        assertTrue(fallback.message.contains("autenticación"))
        assertEquals(true, fallback.allowGuestAccess)
        assertEquals("Auth service down", fallback.fallbackReason)
        assertEquals(Duration.ofMinutes(2), fallback.retryAfter)
    }

    @Test
    fun `should return station fallback response`() {
        // Given
        val context = FallbackContext(
            operationName = "station-lookup"
        )

        // When
        val fallback = fallbackService.getStationFallback(context)

        // Then
        assertNotNull(fallback)
        assertEquals(true, fallback.success)
        assertTrue(fallback.stations.isNotEmpty())
        assertEquals("fallback-station-1", fallback.stations.first().id)
        assertEquals("Estación Principal", fallback.stations.first().name)
        assertEquals(false, fallback.stations.first().available)
    }

    @Test
    fun `should return redemption fallback response`() {
        // Given
        val context = FallbackContext(
            operationName = "coupon-redemption",
            lastException = RuntimeException("Redemption service error")
        )

        // When
        val fallback = fallbackService.getRedemptionFallback(context)

        // Then
        assertNotNull(fallback)
        assertEquals(false, fallback.success)
        assertTrue(fallback.message.contains("redención"))
        assertEquals(true, fallback.allowOfflineRedemption)
        assertTrue(fallback.offlineInstructions.contains("código"))
        assertEquals("Redemption service error", fallback.fallbackReason)
        assertEquals(Duration.ofMinutes(3), fallback.retryAfter)
    }

    @Test
    fun `should return raffle fallback response`() {
        // Given
        val context = FallbackContext(
            operationName = "raffle-participation"
        )

        // When
        val fallback = fallbackService.getRaffleFallback(context)

        // Then
        assertNotNull(fallback)
        assertEquals(false, fallback.success)
        assertTrue(fallback.message.contains("rifas"))
        assertEquals(true, fallback.showCachedRaffles)
        assertEquals(Duration.ofMinutes(10), fallback.retryAfter)
    }

    @Test
    fun `should return ad fallback response`() {
        // Given
        val context = FallbackContext(
            operationName = "ad-serving"
        )

        // When
        val fallback = fallbackService.getAdFallback(context)

        // Then
        assertNotNull(fallback)
        assertEquals(true, fallback.success)
        assertNotNull(fallback.ad)
        assertEquals("default-ad", fallback.ad.id)
        assertEquals("¡Gasolinera JSM!", fallback.ad.title)
        assertTrue(fallback.ad.content.contains("Combustible"))
    }

    @Test
    fun `should return external api fallback response`() {
        // Given
        val context = FallbackContext(
            operationName = "external-api-call",
            lastException = RuntimeException("External service timeout")
        )

        // When
        val fallback = fallbackService.getExternalApiFallback(context)

        // Then
        assertNotNull(fallback)
        assertEquals(false, fallback.success)
        assertTrue(fallback.message.contains("externo"))
        assertTrue(fallback.fallbackData.isEmpty())
        assertEquals("External service timeout", fallback.fallbackReason)
        assertEquals(Duration.ofMinutes(5), fallback.retryAfter)
    }

    @Test
    fun `should register and use custom fallback strategy`() {
        // Given
        val customStrategy = FallbackStrategy(
            cacheable = true,
            cacheTtl = Duration.ofMinutes(1)
        ) { context ->
            "Custom fallback for ${context.operationName}"
        }

        fallbackService.registerFallbackStrategy("custom-operation", customStrategy)

        val context = FallbackContext(operationName = "custom-operation")

        // When
        val result = fallbackService.getFallback<String>("custom-operation", context)

        // Then
        assertEquals("Custom fallback for custom-operation", result)
    }

    @Test
    fun `should cache fallback results when strategy allows`() {
        // Given
        val customStrategy = FallbackStrategy(
            cacheable = true,
            cacheTtl = Duration.ofMinutes(1)
        ) { context ->
            "Cached result ${System.currentTimeMillis()}"
        }

        fallbackService.registerFallbackStrategy("cacheable-operation", customStrategy)

        val context = FallbackContext(operationName = "cacheable-operation")

        // When
        val result1 = fallbackService.getFallback<String>("cacheable-operation", context)
        val result2 = fallbackService.getFallback<String>("cacheable-operation", context)

        // Then
        assertEquals(result1, result2) // Should be the same cached result
    }

    @Test
    fun `should return default value when no strategy found`() {
        // Given
        val defaultValue = "default"
        val context = FallbackContext(operationName = "unknown-operation")

        // When
        val result = fallbackService.getFallback("unknown-operation", context, defaultValue)

        // Then
        assertEquals(defaultValue, result)
    }

    @Test
    fun `should return null when no strategy and no default value`() {
        // Given
        val context = FallbackContext(operationName = "unknown-operation")

        // When
        val result = fallbackService.getFallback<String>("unknown-operation", context)

        // Then
        assertNull(result)
    }

    @Test
    fun `should clear fallback cache`() {
        // Given
        val customStrategy = FallbackStrategy(
            cacheable = true,
            cacheTtl = Duration.ofMinutes(1)
        ) { context ->
            "Cached result"
        }

        fallbackService.registerFallbackStrategy("cacheable-operation", customStrategy)

        val context = FallbackContext(operationName = "cacheable-operation")

        // Cache a result
        fallbackService.getFallback<String>("cacheable-operation", context)

        // When
        fallbackService.clearFallbackCache()

        // Then
        val stats = fallbackService.getFallbackCacheStats()
        assertEquals(0, stats.totalEntries)
    }

    @Test
    fun `should get fallback cache stats`() {
        // Given
        val customStrategy = FallbackStrategy(
            cacheable = true,
            cacheTtl = Duration.ofMinutes(1)
        ) { context ->
            "Cached result"
        }

        fallbackService.registerFallbackStrategy("cacheable-operation", customStrategy)

        val context = FallbackContext(operationName = "cacheable-operation")

        // Cache some results
        fallbackService.getFallback<String>("cacheable-operation", context)

        // When
        val stats = fallbackService.getFallbackCacheStats()

        // Then
        assertTrue(stats.totalEntries >= 0)
        assertTrue(stats.activeEntries >= 0)
        assertTrue(stats.expiredEntries >= 0)
        assertTrue(stats.hitRate >= 0.0)
        assertNotNull(stats.timestamp)
    }

    @Test
    fun `should cleanup expired fallbacks`() {
        // Given
        val shortTtlStrategy = FallbackStrategy(
            cacheable = true,
            cacheTtl = Duration.ofMillis(1) // Very short TTL
        ) { context ->
            "Short-lived result"
        }

        fallbackService.registerFallbackStrategy("short-lived-operation", shortTtlStrategy)

        val context = FallbackContext(operationName = "short-lived-operation")

        // Cache a result
        fallbackService.getFallback<String>("short-lived-operation", context)

        // Wait for expiration
        Thread.sleep(10)

        // When
        fallbackService.cleanupExpiredFallbacks()

        // Then
        val stats = fallbackService.getFallbackCacheStats()
        assertEquals(0, stats.expiredEntries)
    }
}