package com.gasolinerajsm.shared.cache

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

/**
 * Controlador REST para gestión y monitoreo de cache
 */
@RestController
@RequestMapping("/api/cache")
class CacheController(
    private val cacheService: CacheService,
    private val cacheInvalidationService: CacheInvalidationService,
    private val cacheMetricsService: CacheMetricsService,
    private val cacheWarmupService: CacheWarmupService
) {

    // Endpoints de gestión de cache

    @GetMapping("/{cacheName}/stats")
    fun getCacheStats(@PathVariable cacheName: String): ResponseEntity<CacheStats> {
        val stats = cacheService.getCacheStats(cacheName)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/system/metrics")
    fun getSystemMetrics(): ResponseEntity<SystemCacheMetrics> {
        val metrics = cacheMetricsService.getSystemMetrics()
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/metrics")
    fun getAllCacheMetrics(): ResponseEntity<Map<String, CacheMetrics>> {
        val metrics = cacheMetricsService.getAllCacheMetrics()
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/{cacheName}/metrics")
    fun getCacheMetrics(@PathVariable cacheName: String): ResponseEntity<CacheMetrics?> {
        val metrics = cacheMetricsService.getCacheMetrics(cacheName)
        return ResponseEntity.ok(metrics)
    }

    // Endpoints de invalidación

    @DeleteMapping("/{cacheName}")
    fun clearCache(@PathVariable cacheName: String): ResponseEntity<Map<String, String>> {
        cacheService.clear(cacheName)
        return ResponseEntity.ok(mapOf("message" to "Cache '$cacheName' cleared successfully"))
    }

    @DeleteMapping("/{cacheName}/keys/{key}")
    fun evictKey(
        @PathVariable cacheName: String,
        @PathVariable key: String
    ): ResponseEntity<Map<String, String>> {
        cacheService.evict(cacheName, key)
        return ResponseEntity.ok(mapOf("message" to "Key '$key' evicted from cache '$cacheName'"))
    }

    @DeleteMapping("/{cacheName}/pattern")
    fun evictByPattern(
        @PathVariable cacheName: String,
        @RequestParam pattern: String
    ): ResponseEntity<Map<String, String>> {
        cacheService.evictByPattern(cacheName, pattern)
        return ResponseEntity.ok(mapOf("message" to "Pattern '$pattern' evicted from cache '$cacheName'"))
    }

    @PostMapping("/invalidate/event")
    fun invalidateByEvent(
        @RequestBody request: InvalidationRequest
    ): ResponseEntity<Map<String, String>> {
        cacheInvalidationService.invalidateByEvent(request.eventType, request.entityId)
        return ResponseEntity.ok(mapOf("message" to "Cache invalidation triggered for event '${request.eventType}'"))
    }

    @PostMapping("/invalidate/pattern")
    fun invalidateByPattern(
        @RequestParam pattern: String
    ): ResponseEntity<Map<String, String>> {
        cacheInvalidationService.invalidateByPattern(pattern)
        return ResponseEntity.ok(mapOf("message" to "Cache invalidation triggered for pattern '$pattern'"))
    }

    @PostMapping("/invalidate/cascade")
    fun cascadeInvalidation(
        @RequestBody request: CascadeInvalidationRequest
    ): ResponseEntity<Map<String, String>> {
        cacheInvalidationService.cascadeInvalidation(request.rootEvent, request.entityId)
        return ResponseEntity.ok(mapOf("message" to "Cascade invalidation triggered for event '${request.rootEvent}'"))
    }

    // Endpoints de warmup

    @PostMapping("/warmup")
    fun warmupAllCaches(): ResponseEntity<Map<String, String>> {
        cacheWarmupService.warmupAllCaches()
        return ResponseEntity.ok(mapOf("message" to "Cache warmup started for all caches"))
    }

    @PostMapping("/{cacheName}/warmup")
    fun warmupCache(
        @PathVariable cacheName: String,
        @RequestParam(defaultValue = "LAZY") strategy: CacheProperties.WarmupStrategy
    ): ResponseEntity<Map<String, String>> {
        cacheWarmupService.warmupCache(cacheName, strategy)
        return ResponseEntity.ok(mapOf("message" to "Cache warmup started for '$cacheName' with strategy '$strategy'"))
    }

    @GetMapping("/warmup/stats")
    fun getWarmupStats(): ResponseEntity<Map<String, WarmupStats>> {
        val stats = cacheWarmupService.getWarmupStats()
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/{cacheName}/warmup/stats")
    fun getCacheWarmupStats(@PathVariable cacheName: String): ResponseEntity<WarmupStats?> {
        val stats = cacheWarmupService.getWarmupStats(cacheName)
        return ResponseEntity.ok(stats)
    }

    // Endpoints de análisis y monitoreo

    @GetMapping("/{cacheName}/analysis")
    fun getPerformanceAnalysis(@PathVariable cacheName: String): ResponseEntity<PerformanceAnalysis> {
        val analysis = cacheMetricsService.getPerformanceAnalysis(cacheName)
        return ResponseEntity.ok(analysis)
    }

    @GetMapping("/{cacheName}/history")
    fun getPerformanceHistory(@PathVariable cacheName: String): ResponseEntity<List<PerformanceSnapshot>> {
        val history = cacheMetricsService.getPerformanceHistory(cacheName)
        return ResponseEntity.ok(history)
    }

    @GetMapping("/anomalies")
    fun detectAnomalies(): ResponseEntity<List<CacheAnomaly>> {
        val anomalies = cacheMetricsService.detectAnomalies()
        return ResponseEntity.ok(anomalies)
    }

    @GetMapping("/health/report")
    fun getHealthReport(): ResponseEntity<CacheHealthReport> {
        val report = cacheMetricsService.generateHealthReport()
        return ResponseEntity.ok(report)
    }

    // Endpoints de utilidades

    @GetMapping("/{cacheName}/exists/{key}")
    fun checkKeyExists(
        @PathVariable cacheName: String,
        @PathVariable key: String
    ): ResponseEntity<Map<String, Boolean>> {
        val exists = cacheService.exists(cacheName, key)
        return ResponseEntity.ok(mapOf("exists" to exists))
    }

    @GetMapping("/{cacheName}/ttl/{key}")
    fun getKeyTtl(
        @PathVariable cacheName: String,
        @PathVariable key: String
    ): ResponseEntity<Map<String, Any?>> {
        val ttl = cacheService.getTtl(cacheName, key)
        return ResponseEntity.ok(mapOf(
            "key" to key,
            "ttl" to ttl?.seconds,
            "ttlFormatted" to ttl?.toString()
        ))
    }

    @PostMapping("/{cacheName}/put")
    fun putValue(
        @PathVariable cacheName: String,
        @RequestBody request: PutValueRequest
    ): ResponseEntity<Map<String, String>> {
        val ttl = request.ttlSeconds?.let { Duration.ofSeconds(it) }
        cacheService.put(cacheName, request.key, request.value, ttl)
        return ResponseEntity.ok(mapOf("message" to "Value stored in cache '$cacheName' with key '${request.key}'"))
    }

    @GetMapping("/{cacheName}/get/{key}")
    fun getValue(
        @PathVariable cacheName: String,
        @PathVariable key: String
    ): ResponseEntity<Map<String, Any?>> {
        val value = cacheService.get<Any>(cacheName, key)
        return ResponseEntity.ok(mapOf(
            "key" to key,
            "value" to value,
            "found" to (value != null)
        ))
    }

    @PostMapping("/{cacheName}/multi-get")
    fun getMultipleValues(
        @PathVariable cacheName: String,
        @RequestBody keys: List<String>
    ): ResponseEntity<Map<String, Any?>> {
        val values = cacheService.multiGet<Any>(cacheName, keys)
        return ResponseEntity.ok(values)
    }

    @PostMapping("/{cacheName}/multi-put")
    fun putMultipleValues(
        @PathVariable cacheName: String,
        @RequestBody request: MultiPutRequest
    ): ResponseEntity<Map<String, String>> {
        val ttl = request.ttlSeconds?.let { Duration.ofSeconds(it) }
        cacheService.multiPut(cacheName, request.values, ttl)
        return ResponseEntity.ok(mapOf("message" to "${request.values.size} values stored in cache '$cacheName'"))
    }
}

// DTOs para requests

data class InvalidationRequest(
    val eventType: String,
    val entityId: String? = null
)

data class CascadeInvalidationRequest(
    val rootEvent: String,
    val entityId: String? = null
)

data class PutValueRequest(
    val key: String,
    val value: Any,
    val ttlSeconds: Long? = null
)

data class MultiPutRequest(
    val values: Map<String, Any>,
    val ttlSeconds: Long? = null
)