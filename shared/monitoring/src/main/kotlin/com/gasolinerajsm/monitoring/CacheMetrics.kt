package com.gasolinerajsm.monitoring

import io.micrometer.core.instrument.*
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * World-Class Cache Performance Metrics
 *
 * Monitors Redis cache performance, hit rates, and memory usage:
 * - Cache hit/miss ratios and performance
 * - Memory usage and eviction patterns
 * - Connection health and latency
 * - Key distribution and expiration patterns
 */
@Component
class CacheMetrics(private val meterRegistry: MeterRegistry) {

    // ==================== CACHE PERFORMANCE METRICS ====================

    private val cacheHits = Counter.builder("cache.hits.total")
        .description("Total number of cache hits")
        .register(meterRegistry)

    private val cacheMisses = Counter.builder("cache.misses.total")
        .description("Total number of cache misses")
        .register(meterRegistry)

    private val cacheHitRatio = Gauge.builder("cache.hit.ratio")
        .description("Cache hit ratio percentage")
        .register(meterRegistry, this) { getCacheHitRatio() }

    private val cacheOperationTime = Timer.builder("cache.operation.time")
        .description("Cache operation execution time")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    private val cacheSize = Gauge.builder("cache.size")
        .description("Number of keys in cache")
        .register(meterRegistry, this) { getCacheSize() }

    // ==================== MEMORY METRICS ====================

    private val memoryUsed = Gauge.builder("cache.memory.used")
        .description("Cache memory used in bytes")
        .baseUnit("bytes")
        .register(meterRegistry, this) { getMemoryUsed() }

    private val memoryMax = Gauge.builder("cache.memory.max")
        .description("Maximum cache memory in bytes")
        .baseUnit("bytes")
        .register(meterRegistry, this) { getMemoryMax() }

    private val memoryUsageRatio = Gauge.builder("cache.memory.usage.ratio")
        .description("Cache memory usage percentage")
        .register(meterRegistry, this) { getMemoryUsageRatio() }

    private val evictions = Counter.builder("cache.evictions.total")
        .description("Total number of cache evictions")
        .register(meterRegistry)

    private val evictionRate = Gauge.builder("cache.eviction.rate")
        .description("Cache eviction rate per second")
        .baseUnit("evictions/sec")
        .register(meterRegistry, this) { getEvictionRate() }

    // ==================== CONNECTION METRICS ====================

    private val connectionPoolActive = Gauge.builder("cache.connection.pool.active")
        .description("Number of active cache connections")
        .register(meterRegistry, this) { getActiveConnections() }

    private val connectionPoolIdle = Gauge.builder("cache.connection.pool.idle")
        .description("Number of idle cache connections")
        .register(meterRegistry, this) { getIdleConnections() }

    private val connectionErrors = Counter.builder("cache.connection.errors.total")
        .description("Total number of connection errors")
        .register(meterRegistry)

    private val connectionLatency = Timer.builder("cache.connection.latency")
        .description("Cache connection latency")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(meterRegistry)

    // ==================== OPERATION METRICS ====================

    private val getOperations = Counter.builder("cache.operations.get.total")
        .description("Total number of GET operations")
        .register(meterRegistry)

    private val setOperations = Counter.builder("cache.operations.set.total")
        .description("Total number of SET operations")
        .register(meterRegistry)

    private val deleteOperations = Counter.builder("cache.operations.delete.total")
        .description("Total number of DELETE operations")
        .register(meterRegistry)

    private val expireOperations = Counter.builder("cache.operations.expire.total")
        .description("Total number of EXPIRE operations")
        .register(meterRegistry)

    private val pipelineOperations = Counter.builder("cache.operations.pipeline.total")
        .description("Total number of pipeline operations")
        .register(meterRegistry)

    // ==================== KEY PATTERN METRICS ====================

    private val keyExpirations = Counter.builder("cache.keys.expirations.total")
        .description("Total number of key expirations")
        .register(meterRegistry)

    private val keyDistribution = Gauge.builder("cache.keys.distribution")
        .description("Key distribution across different patterns")
        .register(meterRegistry, this) { getKeyDistribution() }

    private val largeValues = Counter.builder("cache.values.large.total")
        .description("Total number of large values (>1MB)")
        .register(meterRegistry)

    // ==================== CLUSTER METRICS (if applicable) ====================

    private val clusterNodes = Gauge.builder("cache.cluster.nodes")
        .description("Number of cluster nodes")
        .register(meterRegistry, this) { getClusterNodes() }

    private val clusterSlots = Gauge.builder("cache.cluster.slots")
        .description("Number of cluster slots")
        .register(meterRegistry, this) { getClusterSlots() }

    private val clusterFailovers = Counter.builder("cache.cluster.failovers.total")
        .description("Total number of cluster failovers")
        .register(meterRegistry)

    // ==================== INTERNAL COUNTERS ====================

    private val totalHits = AtomicLong(0)
    private val totalMisses = AtomicLong(0)
    private val cacheKeyCount = AtomicLong(0)
    private val memoryUsedBytes = AtomicLong(0)
    private val memoryMaxBytes = AtomicLong(1024 * 1024 * 1024) // 1GB default
    private val totalEvictions = AtomicLong(0)
    private val activeConnections = AtomicLong(5)
    private val idleConnections = AtomicLong(5)
    private val clusterNodeCount = AtomicLong(3)
    private val clusterSlotCount = AtomicLong(16384)

    // ==================== PUBLIC METHODS ====================

    fun recordCacheHit(key: String, keyPattern: String, valueSize: Long, ttl: Duration?) {
        val tags = Tags.of(
            Tag.of("key_pattern", keyPattern),
            Tag.of("value_size_range", getValueSizeRange(valueSize)),
            Tag.of("has_ttl", (ttl != null).toString())
        )

        cacheHits.increment(tags)
        totalHits.incrementAndGet()

        if (valueSize > 1024 * 1024) { // 1MB
            largeValues.increment(tags)
        }
    }

    fun recordCacheMiss(key: String, keyPattern: String, reason: String) {
        val tags = Tags.of(
            Tag.of("key_pattern", keyPattern),
            Tag.of("reason", reason) // "not_found", "expired", "evicted"
        )

        cacheMisses.increment(tags)
        totalMisses.incrementAndGet()
    }

    fun recordCacheOperation(
        operation: String,
        keyPattern: String,
        duration: Duration,
        success: Boolean,
        valueSize: Long = 0
    ) {
        val tags = Tags.of(
            Tag.of("operation", operation.uppercase()),
            Tag.of("key_pattern", keyPattern),
            Tag.of("success", success.toString()),
            Tag.of("value_size_range", getValueSizeRange(valueSize))
        )

        cacheOperationTime.record(duration, tags)

        // Record specific operation counters
        when (operation.uppercase()) {
            "GET" -> getOperations.increment(tags)
            "SET" -> setOperations.increment(tags)
            "DEL", "DELETE" -> deleteOperations.increment(tags)
            "EXPIRE" -> expireOperations.increment(tags)
            "PIPELINE" -> pipelineOperations.increment(tags)
        }
    }

    fun recordEviction(key: String, keyPattern: String, reason: String, valueSize: Long) {
        val tags = Tags.of(
            Tag.of("key_pattern", keyPattern),
            Tag.of("reason", reason), // "maxmemory", "ttl", "lru", "lfu"
            Tag.of("value_size_range", getValueSizeRange(valueSize))
        )

        evictions.increment(tags)
        totalEvictions.incrementAndGet()
    }

    fun recordKeyExpiration(key: String, keyPattern: String, originalTtl: Duration) {
        val tags = Tags.of(
            Tag.of("key_pattern", keyPattern),
            Tag.of("ttl_range", getTtlRange(originalTtl))
        )

        keyExpirations.increment(tags)
    }

    fun recordConnectionError(errorType: String, operation: String) {
        connectionErrors.increment(Tags.of(
            Tag.of("error_type", errorType),
            Tag.of("operation", operation)
        ))
    }

    fun recordConnectionLatency(duration: Duration, operation: String) {
        connectionLatency.record(duration, Tags.of(
            Tag.of("operation", operation)
        ))
    }

    fun recordClusterFailover(nodeId: String, reason: String, duration: Duration) {
        clusterFailovers.increment(Tags.of(
            Tag.of("node_id", nodeId),
            Tag.of("reason", reason)
        ))
    }

    // ==================== GAUGE METHODS ====================

    private fun getCacheHitRatio(): Double {
        val hits = totalHits.get()
        val misses = totalMisses.get()
        val total = hits + misses
        return if (total > 0) (hits.toDouble() / total.toDouble()) * 100.0 else 0.0
    }

    private fun getCacheSize(): Double = cacheKeyCount.get().toDouble()

    private fun getMemoryUsed(): Double = memoryUsedBytes.get().toDouble()

    private fun getMemoryMax(): Double = memoryMaxBytes.get().toDouble()

    private fun getMemoryUsageRatio(): Double {
        val used = memoryUsedBytes.get().toDouble()
        val max = memoryMaxBytes.get().toDouble()
        return if (max > 0) (used / max) * 100.0 else 0.0
    }

    private fun getEvictionRate(): Double {
        // Calculate evictions per second over last minute
        return totalEvictions.get() / 60.0 // Simplified calculation
    }

    private fun getActiveConnections(): Double = activeConnections.get().toDouble()

    private fun getIdleConnections(): Double = idleConnections.get().toDouble()

    private fun getKeyDistribution(): Double {
        // Return distribution metric (simplified)
        return 1.0
    }

    private fun getClusterNodes(): Double = clusterNodeCount.get().toDouble()

    private fun getClusterSlots(): Double = clusterSlotCount.get().toDouble()

    // ==================== HELPER METHODS ====================

    private fun getValueSizeRange(size: Long): String = when {
        size == 0L -> "empty"
        size <= 1024 -> "small" // <= 1KB
        size <= 64 * 1024 -> "medium" // <= 64KB
        size <= 1024 * 1024 -> "large" // <= 1MB
        else -> "xlarge" // > 1MB
    }

    private fun getTtlRange(ttl: Duration): String = when {
        ttl.toSeconds() <= 60 -> "short" // <= 1 minute
        ttl.toSeconds() <= 3600 -> "medium" // <= 1 hour
        ttl.toSeconds() <= 86400 -> "long" // <= 1 day
        else -> "very_long" // > 1 day
    }

    // ==================== ADMIN METHODS ====================

    fun updateCacheStats(
        keyCount: Long,
        memoryUsed: Long,
        memoryMax: Long,
        evictionCount: Long
    ) {
        cacheKeyCount.set(keyCount)
        memoryUsedBytes.set(memoryUsed)
        memoryMaxBytes.set(memoryMax)
        totalEvictions.set(evictionCount)
    }

    fun updateConnectionPoolStats(active: Long, idle: Long) {
        activeConnections.set(active)
        idleConnections.set(idle)
    }

    fun updateClusterStats(nodes: Long, slots: Long) {
        clusterNodeCount.set(nodes)
        clusterSlotCount.set(slots)
    }

    // ==================== HEALTH CHECK METHODS ====================

    fun isCacheHealthy(): Boolean {
        val hitRatio = getCacheHitRatio()
        val memoryUsage = getMemoryUsageRatio()
        val evictionRate = getEvictionRate()

        return hitRatio > 70.0 && // Hit ratio should be > 70%
               memoryUsage < 90.0 && // Memory usage should be < 90%
               evictionRate < 100.0 // Eviction rate should be < 100/sec
    }

    fun isConnectionPoolHealthy(): Boolean {
        val active = activeConnections.get()
        val idle = idleConnections.get()
        val total = active + idle

        return total > 0 && active < total * 0.9 // Active connections < 90% of total
    }

    fun isClusterHealthy(): Boolean {
        val nodes = clusterNodeCount.get()
        val slots = clusterSlotCount.get()

        return nodes >= 3 && slots == 16384L // Minimum 3 nodes, all slots covered
    }

    // ==================== CACHE WARMING METHODS ====================

    fun recordCacheWarmup(keyPattern: String, keyCount: Long, duration: Duration, success: Boolean) {
        Timer.builder("cache.warmup.duration")
            .description("Cache warmup duration")
            .register(meterRegistry)
            .record(duration, Tags.of(
                Tag.of("key_pattern", keyPattern),
                Tag.of("success", success.toString()),
                Tag.of("key_count_range", getKeyCountRange(keyCount))
            ))
    }

    private fun getKeyCountRange(count: Long): String = when {
        count <= 100 -> "small"
        count <= 1000 -> "medium"
        count <= 10000 -> "large"
        else -> "xlarge"
    }
}