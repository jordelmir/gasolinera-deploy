package com.gasolinerajsm.shared.cache

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(SpringJUnitExtension::class)
@SpringBootTest
@Testcontainers
class CacheServiceTest {

    @Container
    private val redisContainer = GenericContainer<Nothing>("redis:7-alpine").apply {
        withExposedPorts(6379)
    }

    private lateinit var cacheService: CacheService
    private lateinit var redisTemplate: RedisTemplate<String, Any>
    private lateinit var cacheManager: CacheManager
    private lateinit var properties: CacheProperties
    private lateinit var meterRegistry: SimpleMeterRegistry

    @BeforeEach
    fun setUp() {
        // Mock dependencies
        redisTemplate = mock()
        cacheManager = mock()
        meterRegistry = SimpleMeterRegistry()

        // Configure properties
        properties = CacheProperties(
            enabled = true,
            defaultTtl = Duration.ofMinutes(30),
            keyPrefix = "test"
        )

        // Create service
        cacheService = CacheService(redisTemplate, cacheManager, properties, meterRegistry)
    }

    @Test
    fun `should get value from cache successfully`() {
        // Given
        val cacheName = "testCache"
        val key = "testKey"
        val expectedValue = "testValue"

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)
        whenever(cache.get(key, String::class.java)).thenReturn(expectedValue)

        // When
        val result = cacheService.get<String>(cacheName, key)

        // Then
        assertEquals(expectedValue, result)
        verify(cache).get(key, String::class.java)
    }

    @Test
    fun `should return fallback value when cache miss`() {
        // Given
        val cacheName = "testCache"
        val key = "testKey"
        val fallbackValue = "fallbackValue"

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)
        whenever(cache.get(key, String::class.java)).thenReturn(null)

        // When
        val result = cacheService.get<String>(cacheName, key) { fallbackValue }

        // Then
        assertEquals(fallbackValue, result)
    }

    @Test
    fun `should put value to cache successfully`() {
        // Given
        val cacheName = "testCache"
        val key = "testKey"
        val value = "testValue"

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)

        // When
        cacheService.put(cacheName, key, value)

        // Then
        verify(cache).put(key, value)
    }

    @Test
    fun `should get or compute value successfully`() {
        // Given
        val cacheName = "testCache"
        val key = "testKey"
        val computedValue = "computedValue"

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)
        whenever(cache.get(key, String::class.java)).thenReturn(null)

        // When
        val result = cacheService.getOrCompute<String>(cacheName, key) { computedValue }

        // Then
        assertEquals(computedValue, result)
        verify(cache).put(key, computedValue)
    }

    @Test
    fun `should evict key from cache successfully`() {
        // Given
        val cacheName = "testCache"
        val key = "testKey"

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)

        // When
        cacheService.evict(cacheName, key)

        // Then
        verify(cache).evict(key)
    }

    @Test
    fun `should clear cache successfully`() {
        // Given
        val cacheName = "testCache"

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)

        // When
        cacheService.clear(cacheName)

        // Then
        verify(cache).clear()
    }

    @Test
    fun `should check if key exists in cache`() {
        // Given
        val cacheName = "testCache"
        val key = "testKey"

        val cache = mock<Cache>()
        val cacheValue = mock<Cache.ValueWrapper>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)
        whenever(cache.get(key)).thenReturn(cacheValue)

        // When
        val exists = cacheService.exists(cacheName, key)

        // Then
        assertTrue(exists)
    }

    @Test
    fun `should get cache stats successfully`() {
        // Given
        val cacheName = "testCache"
        val config = CacheProperties.CacheConfig(maxSize = 1000)
        val propertiesWithCache = properties.copy(
            caches = mapOf(cacheName to config)
        )

        val cacheServiceWithConfig = CacheService(redisTemplate, cacheManager, propertiesWithCache, meterRegistry)

        whenever(redisTemplate.keys(any<String>())).thenReturn(setOf("key1", "key2"))

        // When
        val stats = cacheServiceWithConfig.getCacheStats(cacheName)

        // Then
        assertNotNull(stats)
        assertEquals(cacheName, stats.cacheName)
        assertEquals(2L, stats.size)
        assertEquals(1000L, stats.maxSize)
    }

    @Test
    fun `should handle multi-get operations`() {
        // Given
        val cacheName = "testCache"
        val keys = listOf("key1", "key2", "key3")
        val values = mapOf("key1" to "value1", "key2" to null, "key3" to "value3")

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)
        whenever(cache.get("key1", String::class.java)).thenReturn("value1")
        whenever(cache.get("key2", String::class.java)).thenReturn(null)
        whenever(cache.get("key3", String::class.java)).thenReturn("value3")

        // When
        val result = cacheService.multiGet<String>(cacheName, keys)

        // Then
        assertEquals(values, result)
    }

    @Test
    fun `should handle multi-put operations`() {
        // Given
        val cacheName = "testCache"
        val values = mapOf("key1" to "value1", "key2" to "value2")

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)

        // When
        cacheService.multiPut(cacheName, values)

        // Then
        verify(cache).put("key1", "value1")
        verify(cache).put("key2", "value2")
    }

    @Test
    fun `should record metrics for cache operations`() {
        // Given
        val cacheName = "testCache"
        val key = "testKey"
        val value = "testValue"

        val cache = mock<Cache>()
        whenever(cacheManager.getCache(cacheName)).thenReturn(cache)
        whenever(cache.get(key, String::class.java)).thenReturn(value)

        // When
        cacheService.get<String>(cacheName, key)
        cacheService.put(cacheName, key, value)

        // Then
        val getTimer = meterRegistry.find("cache.operation")
            .tag("cache", cacheName)
            .tag("operation", "get")
            .timer()

        val putTimer = meterRegistry.find("cache.operation")
            .tag("cache", cacheName)
            .tag("operation", "put")
            .timer()

        assertNotNull(getTimer)
        assertNotNull(putTimer)
        assertTrue(getTimer.count() > 0)
        assertTrue(putTimer.count() > 0)
    }
}