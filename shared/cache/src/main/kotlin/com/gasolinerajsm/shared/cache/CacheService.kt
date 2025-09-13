package com.gasolinerajsm.shared.cache

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Servicio principal de caching con funcionalidades avanzadas
 */
@Service
class CacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val cacheManager: CacheManager,
    private val properties: CacheProperties,
    private val meterRegistry: MeterRegistry? = null
) {

    private val logger = LoggerFactory.getLogger(CacheService::class.java)

    /**
     * Obtiene un valor del cache con fallback
     */
    inline fun <reified T> get(
        cacheName: String,
        key: String,
        fallback: () -> T? = { null }
    ): T? {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "get")
        return timer?.recordCallable(Callable {
            getInternal<T>(cacheName, key, fallback)
        }) ?: getInternal(cacheName, key, fallback)
    }

    /**
     * Obtiene un valor del cache de forma asíncrona
     */
    inline fun <reified T> getAsync(
        cacheName: String,
        key: String,
        noinline fallback: () -> T? = { null }
    ): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync {
            get<T>(cacheName, key, fallback)
        }
    }

    /**
     * Almacena un valor en el cache
     */
    fun <T> put(cacheName: String, key: String, value: T, ttl: Duration? = null) {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "put")
        timer?.recordCallable(Callable {
            putInternal(cacheName, key, value, ttl)
        }) ?: putInternal(cacheName, key, value, ttl)
    }

    /**
     * Almacena un valor en el cache de forma asíncrona
     */
    fun <T> putAsync(cacheName: String, key: String, value: T, ttl: Duration? = null): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            put(cacheName, key, value, ttl)
        }
    }

    /**
     * Obtiene o calcula un valor (cache-aside pattern)
     */
    inline fun <reified T> getOrCompute(
        cacheName: String,
        key: String,
        ttl: Duration? = null,
        noinline computation: () -> T
    ): T {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "get_or_compute")
        return timer?.recordCallable(Callable {
            getOrComputeInternal<T>(cacheName, key, ttl, computation)
        }) ?: getOrComputeInternal(cacheName, key, ttl, computation)
    }

    /**
     * Obtiene o calcula un valor de forma asíncrona
     */
    inline fun <reified T> getOrComputeAsync(
        cacheName: String,
        key: String,
        ttl: Duration? = null,
        noinline computation: () -> T
    ): CompletableFuture<T> {
        return CompletableFuture.supplyAsync {
            getOrCompute(cacheName, key, ttl, computation)
        }
    }

    /**
     * Elimina una entrada del cache
     */
    fun evict(cacheName: String, key: String) {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "evict")
        timer?.recordCallable(Callable {
            evictInternal(cacheName, key)
        }) ?: evictInternal(cacheName, key)
    }

    /**
     * Elimina múltiples entradas del cache por patrón
     */
    fun evictByPattern(cacheName: String, pattern: String) {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "evict_pattern")
        timer?.recordCallable(Callable {
            evictByPatternInternal(cacheName, pattern)
        }) ?: evictByPatternInternal(cacheName, pattern)
    }

    /**
     * Limpia completamente un cache
     */
    fun clear(cacheName: String) {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "clear")
        timer?.recordCallable(Callable {
            clearInternal(cacheName)
        }) ?: clearInternal(cacheName)
    }

    /**
     * Verifica si existe una clave en el cache
     */
    fun exists(cacheName: String, key: String): Boolean {
        return try {
            val cache = getCache(cacheName)
            cache?.get(key) != null
        } catch (e: Exception) {
            logger.warn("Error checking cache existence for key: $key in cache: $cacheName", e)
            false
        }
    }

    /**
     * Obtiene el TTL restante de una clave
     */
    fun getTtl(cacheName: String, key: String): Duration? {
        return try {
            val fullKey = buildKey(cacheName, key)
            val ttlSeconds = redisTemplate.getExpire(fullKey, TimeUnit.SECONDS)
            if (ttlSeconds > 0) Duration.ofSeconds(ttlSeconds) else null
        } catch (e: Exception) {
            logger.warn("Error getting TTL for key: $key in cache: $cacheName", e)
            null
        }
    }

    /**
     * Obtiene estadísticas del cache
     */
    fun getCacheStats(cacheName: String): CacheStats {
        return try {
            val cache = getCache(cacheName)
            val config = properties.caches[cacheName]

            // Obtener métricas básicas
            val pattern = buildKey(cacheName, "*")
            val keys = redisTemplate.keys(pattern)
            val size = keys?.size ?: 0

            CacheStats(
                cacheName = cacheName,
                size = size.toLong(),
                maxSize = config?.maxSize ?: 0L,
                hitRate = getHitRate(cacheName),
                missRate = getMissRate(cacheName),
                evictionCount = getEvictionCount(cacheName),
                averageLoadTime = getAverageLoadTime(cacheName)
            )
        } catch (e: Exception) {
            logger.warn("Error getting cache stats for cache: $cacheName", e)
            CacheStats(cacheName, 0, 0, 0.0, 0.0, 0, Duration.ZERO)
        }
    }

    /**
     * Obtiene múltiples valores del cache
     */
    inline fun <reified T> multiGet(cacheName: String, keys: List<String>): Map<String, T?> {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "multi_get")
        return timer?.recordCallable(Callable {
            multiGetInternal<T>(cacheName, keys)
        }) ?: multiGetInternal(cacheName, keys)
    }

    /**
     * Almacena múltiples valores en el cache
     */
    fun <T> multiPut(cacheName: String, values: Map<String, T>, ttl: Duration? = null) {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "multi_put")
        timer?.recordCallable(Callable {
            multiPutInternal(cacheName, values, ttl)
        }) ?: multiPutInternal(cacheName, values, ttl)
    }

    // Métodos internos de implementación

    @PublishedApi
    internal inline fun <reified T> getInternal(
        cacheName: String,
        key: String,
        fallback: () -> T?
    ): T? {
        return try {
            val cache = getCache(cacheName)
            val cachedValue = cache?.get(key, T::class.java)

            if (cachedValue != null) {
                recordCacheHit(cacheName)
                cachedValue
            } else {
                recordCacheMiss(cacheName)
                fallback()
            }
        } catch (e: Exception) {
            logger.warn("Error getting value from cache: $cacheName, key: $key", e)
            recordCacheError(cacheName)
            fallback()
        }
    }

    @PublishedApi
    internal fun <T> putInternal(cacheName: String, key: String, value: T, ttl: Duration?) {
        try {
            val cache = getCache(cacheName)
            cache?.put(key, value)

            // Si se especifica TTL personalizado, configurarlo en Redis
            if (ttl != null) {
                val fullKey = buildKey(cacheName, key)
                redisTemplate.expire(fullKey, ttl)
            }

            recordCachePut(cacheName)
        } catch (e: Exception) {
            logger.error("Error putting value to cache: $cacheName, key: $key", e)
            recordCacheError(cacheName)
        }
    }

    @PublishedApi
    internal inline fun <reified T> getOrComputeInternal(
        cacheName: String,
        key: String,
        ttl: Duration?,
        computation: () -> T
    ): T {
        return try {
            val cache = getCache(cacheName)
            val cachedValue = cache?.get(key, T::class.java)

            if (cachedValue != null) {
                recordCacheHit(cacheName)
                cachedValue
            } else {
                recordCacheMiss(cacheName)
                val computedValue = computation()
                put(cacheName, key, computedValue, ttl)
                computedValue
            }
        } catch (e: Exception) {
            logger.error("Error in getOrCompute for cache: $cacheName, key: $key", e)
            recordCacheError(cacheName)
            computation()
        }
    }

    @PublishedApi
    internal fun evictInternal(cacheName: String, key: String) {
        try {
            val cache = getCache(cacheName)
            cache?.evict(key)
            recordCacheEviction(cacheName)
        } catch (e: Exception) {
            logger.error("Error evicting key: $key from cache: $cacheName", e)
            recordCacheError(cacheName)
        }
    }

    @PublishedApi
    internal fun evictByPatternInternal(cacheName: String, pattern: String) {
        try {
            val fullPattern = buildKey(cacheName, pattern)
            val keys = redisTemplate.keys(fullPattern)

            keys?.forEach { fullKey ->
                val key = extractKey(cacheName, fullKey)
                evict(cacheName, key)
            }
        } catch (e: Exception) {
            logger.error("Error evicting by pattern: $pattern from cache: $cacheName", e)
            recordCacheError(cacheName)
        }
    }

    @PublishedApi
    internal fun clearInternal(cacheName: String) {
        try {
            val cache = getCache(cacheName)
            cache?.clear()
            recordCacheClear(cacheName)
        } catch (e: Exception) {
            logger.error("Error clearing cache: $cacheName", e)
            recordCacheError(cacheName)
        }
    }

    @PublishedApi
    internal inline fun <reified T> multiGetInternal(cacheName: String, keys: List<String>): Map<String, T?> {
        return keys.associateWith { key ->
            get<T>(cacheName, key)
        }
    }

    @PublishedApi
    internal fun <T> multiPutInternal(cacheName: String, values: Map<String, T>, ttl: Duration?) {
        values.forEach { (key, value) ->
            put(cacheName, key, value, ttl)
        }
    }

    // Métodos auxiliares

    private fun getCache(cacheName: String): Cache? {
        return cacheManager.getCache(cacheName)
    }

    private fun buildKey(cacheName: String, key: String): String {
        val config = properties.caches[cacheName]
        val prefix = config?.keyPrefix?.takeIf { it.isNotEmpty() } ?: cacheName
        return "${properties.keyPrefix}:$prefix:$key"
    }

    private fun extractKey(cacheName: String, fullKey: String): String {
        val config = properties.caches[cacheName]
        val prefix = config?.keyPrefix?.takeIf { it.isNotEmpty() } ?: cacheName
        val expectedPrefix = "${properties.keyPrefix}:$prefix:"
        return fullKey.removePrefix(expectedPrefix)
    }

    // Métodos de métricas

    private fun recordCacheHit(cacheName: String) {
        meterRegistry?.counter("cache.requests", "cache", cacheName, "result", "hit")?.increment()
    }

    private fun recordCacheMiss(cacheName: String) {
        meterRegistry?.counter("cache.requests", "cache", cacheName, "result", "miss")?.increment()
    }

    private fun recordCachePut(cacheName: String) {
        meterRegistry?.counter("cache.puts", "cache", cacheName)?.increment()
    }

    private fun recordCacheEviction(cacheName: String) {
        meterRegistry?.counter("cache.evictions", "cache", cacheName)?.increment()
    }

    private fun recordCacheClear(cacheName: String) {
        meterRegistry?.counter("cache.clears", "cache", cacheName)?.increment()
    }

    private fun recordCacheError(cacheName: String) {
        meterRegistry?.counter("cache.errors", "cache", cacheName)?.increment()
    }

    private fun getHitRate(cacheName: String): Double {
        val hits = meterRegistry?.counter("cache.requests", "cache", cacheName, "result", "hit")?.count() ?: 0.0
        val misses = meterRegistry?.counter("cache.requests", "cache", cacheName, "result", "miss")?.count() ?: 0.0
        val total = hits + misses
        return if (total > 0) hits / total else 0.0
    }

    private fun getMissRate(cacheName: String): Double {
        return 1.0 - getHitRate(cacheName)
    }

    private fun getEvictionCount(cacheName: String): Long {
        return meterRegistry?.counter("cache.evictions", "cache", cacheName)?.count()?.toLong() ?: 0L
    }

    private fun getAverageLoadTime(cacheName: String): Duration {
        val timer = meterRegistry?.timer("cache.operation", "cache", cacheName, "operation", "get_or_compute")
        return if (timer != null && timer.count() > 0) {
            Duration.ofNanos(timer.mean(TimeUnit.NANOSECONDS).toLong())
        } else {
            Duration.ZERO
        }
    }
}

/**
 * Clase de datos para estadísticas de cache
 */
data class CacheStats(
    val cacheName: String,
    val size: Long,
    val maxSize: Long,
    val hitRate: Double,
    val missRate: Double,
    val evictionCount: Long,
    val averageLoadTime: Duration
)