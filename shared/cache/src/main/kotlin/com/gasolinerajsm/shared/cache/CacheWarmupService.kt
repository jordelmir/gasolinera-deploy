package com.gasolinerajsm.shared.cache

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Servicio para precalentamiento inteligente de cache
 */
@Service
class CacheWarmupService(
    private val cacheService: CacheService,
    private val properties: CacheProperties
) {

    private val logger = LoggerFactory.getLogger(CacheWarmupService::class.java)
    private val warmupStrategies = ConcurrentHashMap<String, WarmupStrategy>()
    private val warmupStats = ConcurrentHashMap<String, WarmupStats>()

    init {
        registerDefaultWarmupStrategies()
    }

    /**
     * Ejecuta warmup al iniciar la aplicación
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (properties.warmup.enabled && properties.warmup.onStartup) {
            logger.info("Starting cache warmup on application startup")
            warmupAllCaches()
        }
    }

    /**
     * Ejecuta warmup programado
     */
    @Scheduled(cron = "#{@cacheProperties.warmup.scheduleCron}")
    fun scheduledWarmup() {
        if (properties.warmup.enabled && properties.warmup.scheduled) {
            logger.info("Starting scheduled cache warmup")
            warmupAllCaches()
        }
    }

    /**
     * Ejecuta warmup para todos los caches configurados
     */
    @Async
    fun warmupAllCaches() {
        val startTime = Instant.now()
        logger.info("Starting warmup for all caches")

        val futures = properties.caches
            .filter { (_, config) -> config.warmupEnabled }
            .map { (cacheName, config) ->
                CompletableFuture.runAsync {
                    warmupCache(cacheName, config.warmupStrategy)
                }
            }

        CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
            val duration = Duration.between(startTime, Instant.now())
            logger.info("Completed warmup for all caches in ${duration.toMillis()}ms")
        }
    }

    /**
     * Ejecuta warmup para un cache específico
     */
    fun warmupCache(cacheName: String, strategy: CacheProperties.WarmupStrategy = CacheProperties.WarmupStrategy.LAZY) {
        val startTime = Instant.now()
        val stats = WarmupStats(cacheName, startTime)
        warmupStats[cacheName] = stats

        try {
            logger.info("Starting warmup for cache: $cacheName with strategy: $strategy")

            val warmupStrategy = warmupStrategies[cacheName]
            if (warmupStrategy == null) {
                logger.warn("No warmup strategy found for cache: $cacheName")
                return
            }

            when (strategy) {
                CacheProperties.WarmupStrategy.EAGER -> eagerWarmup(cacheName, warmupStrategy, stats)
                CacheProperties.WarmupStrategy.LAZY -> lazyWarmup(cacheName, warmupStrategy, stats)
                CacheProperties.WarmupStrategy.SCHEDULED -> scheduledWarmup(cacheName, warmupStrategy, stats)
                CacheProperties.WarmupStrategy.ON_DEMAND -> onDemandWarmup(cacheName, warmupStrategy, stats)
            }

            val duration = Duration.between(startTime, Instant.now())
            stats.completedAt = Instant.now()
            stats.duration = duration
            stats.success = true

            logger.info("Completed warmup for cache: $cacheName in ${duration.toMillis()}ms, loaded ${stats.itemsLoaded} items")

        } catch (e: Exception) {
            logger.error("Error during warmup for cache: $cacheName", e)
            stats.success = false
            stats.error = e.message
        }
    }

    /**
     * Registra una estrategia de warmup personalizada
     */
    fun registerWarmupStrategy(cacheName: String, strategy: WarmupStrategy) {
        warmupStrategies[cacheName] = strategy
        logger.debug("Registered warmup strategy for cache: $cacheName")
    }

    /**
     * Obtiene estadísticas de warmup
     */
    fun getWarmupStats(): Map<String, WarmupStats> {
        return warmupStats.toMap()
    }

    /**
     * Obtiene estadísticas de warmup para un cache específico
     */
    fun getWarmupStats(cacheName: String): WarmupStats? {
        return warmupStats[cacheName]
    }

    /**
     * Limpia estadísticas de warmup
     */
    fun clearWarmupStats() {
        warmupStats.clear()
    }

    // Estrategias de warmup

    private fun eagerWarmup(cacheName: String, strategy: WarmupStrategy, stats: WarmupStats) {
        logger.debug("Executing eager warmup for cache: $cacheName")

        val keys = strategy.getKeysToWarmup()
        val batchSize = properties.warmup.batchSize

        keys.chunked(batchSize).forEach { batch ->
            val futures = batch.map { key ->
                CompletableFuture.supplyAsync {
                    try {
                        val value = strategy.loadValue(key)
                        if (value != null) {
                            cacheService.put(cacheName, key, value)
                            stats.itemsLoaded.incrementAndGet()
                        }
                        true
                    } catch (e: Exception) {
                        logger.warn("Error loading value for key: $key in cache: $cacheName", e)
                        stats.itemsFailed.incrementAndGet()
                        false
                    }
                }
            }

            CompletableFuture.allOf(*futures.toTypedArray()).join()
        }
    }

    private fun lazyWarmup(cacheName: String, strategy: WarmupStrategy, stats: WarmupStats) {
        logger.debug("Executing lazy warmup for cache: $cacheName")

        val priorityKeys = strategy.getPriorityKeys()

        priorityKeys.forEach { key ->
            try {
                val value = strategy.loadValue(key)
                if (value != null) {
                    cacheService.put(cacheName, key, value)
                    stats.itemsLoaded.incrementAndGet()
                }
            } catch (e: Exception) {
                logger.warn("Error loading priority key: $key in cache: $cacheName", e)
                stats.itemsFailed.incrementAndGet()
            }
        }
    }

    private fun scheduledWarmup(cacheName: String, strategy: WarmupStrategy, stats: WarmupStats) {
        logger.debug("Executing scheduled warmup for cache: $cacheName")

        // Similar al eager pero con control de tiempo
        val keys = strategy.getKeysToWarmup()
        val batchSize = properties.warmup.batchSize
        val timeout = properties.warmup.timeout
        val startTime = Instant.now()

        for (batch in keys.chunked(batchSize)) {
            if (Duration.between(startTime, Instant.now()) > timeout) {
                logger.warn("Warmup timeout reached for cache: $cacheName")
                break
            }

            batch.forEach { key ->
                try {
                    val value = strategy.loadValue(key)
                    if (value != null) {
                        cacheService.put(cacheName, key, value)
                        stats.itemsLoaded.incrementAndGet()
                    }
                } catch (e: Exception) {
                    logger.warn("Error loading key: $key in cache: $cacheName", e)
                    stats.itemsFailed.incrementAndGet()
                }
            }
        }
    }

    private fun onDemandWarmup(cacheName: String, strategy: WarmupStrategy, stats: WarmupStats) {
        logger.debug("Executing on-demand warmup for cache: $cacheName")

        // Warmup basado en patrones de uso recientes
        val recentKeys = strategy.getRecentlyUsedKeys()

        recentKeys.forEach { key ->
            try {
                if (!cacheService.exists(cacheName, key)) {
                    val value = strategy.loadValue(key)
                    if (value != null) {
                        cacheService.put(cacheName, key, value)
                        stats.itemsLoaded.incrementAndGet()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error loading recent key: $key in cache: $cacheName", e)
                stats.itemsFailed.incrementAndGet()
            }
        }
    }

    // Estrategias por defecto

    private fun registerDefaultWarmupStrategies() {
        // Estrategia para cache de usuarios
        registerWarmupStrategy("users", object : WarmupStrategy {
            override fun getKeysToWarmup(): List<String> {
                // Cargar usuarios activos recientes
                return emptyList() // Implementar según lógica de negocio
            }

            override fun getPriorityKeys(): List<String> {
                // Usuarios VIP o administradores
                return emptyList() // Implementar según lógica de negocio
            }

            override fun getRecentlyUsedKeys(): List<String> {
                // Usuarios que se han logueado recientemente
                return emptyList() // Implementar según lógica de negocio
            }

            override fun loadValue(key: String): Any? {
                // Cargar usuario desde base de datos
                return null // Implementar según lógica de negocio
            }
        })

        // Estrategia para cache de estaciones
        registerWarmupStrategy("stations", object : WarmupStrategy {
            override fun getKeysToWarmup(): List<String> {
                // Cargar todas las estaciones activas
                return emptyList() // Implementar según lógica de negocio
            }

            override fun getPriorityKeys(): List<String> {
                // Estaciones principales o más utilizadas
                return emptyList() // Implementar según lógica de negocio
            }

            override fun getRecentlyUsedKeys(): List<String> {
                // Estaciones consultadas recientemente
                return emptyList() // Implementar según lógica de negocio
            }

            override fun loadValue(key: String): Any? {
                // Cargar estación desde base de datos
                return null // Implementar según lógica de negocio
            }
        })

        // Estrategia para cache de cupones
        registerWarmupStrategy("coupons", object : WarmupStrategy {
            override fun getKeysToWarmup(): List<String> {
                // Cargar cupones activos
                return emptyList() // Implementar según lógica de negocio
            }

            override fun getPriorityKeys(): List<String> {
                // Cupones de campañas activas
                return emptyList() // Implementar según lógica de negocio
            }

            override fun getRecentlyUsedKeys(): List<String> {
                // Cupones consultados recientemente
                return emptyList() // Implementar según lógica de negocio
            }

            override fun loadValue(key: String): Any? {
                // Cargar cupón desde base de datos
                return null // Implementar según lógica de negocio
            }
        })
    }
}

/**
 * Interfaz para estrategias de warmup personalizadas
 */
interface WarmupStrategy {
    fun getKeysToWarmup(): List<String>
    fun getPriorityKeys(): List<String>
    fun getRecentlyUsedKeys(): List<String>
    fun loadValue(key: String): Any?
}

/**
 * Estadísticas de warmup
 */
data class WarmupStats(
    val cacheName: String,
    val startedAt: Instant,
    var completedAt: Instant? = null,
    var duration: Duration = Duration.ZERO,
    var success: Boolean = false,
    var error: String? = null,
    val itemsLoaded: AtomicInteger = AtomicInteger(0),
    val itemsFailed: AtomicInteger = AtomicInteger(0)
)