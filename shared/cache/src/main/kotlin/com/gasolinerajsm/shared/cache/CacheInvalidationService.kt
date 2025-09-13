package com.gasolinerajsm.shared.cache

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Servicio para invalidación inteligente de cache basada en eventos
 */
@Service
class CacheInvalidationService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val properties: CacheProperties
) {

    private val logger = LoggerFactory.getLogger(CacheInvalidationService::class.java)
    private val invalidationListeners = ConcurrentHashMap<String, MutableList<(String) -> Unit>>()

    /**
     * Invalida cache basado en un evento
     */
    @Async
    fun invalidateByEvent(eventType: String, entityId: String? = null) {
        if (!properties.invalidation.enabled) {
            return
        }

        logger.debug("Processing cache invalidation for event: $eventType, entityId: $entityId")

        val patterns = properties.invalidation.patterns[eventType]
        if (patterns.isNullOrEmpty()) {
            logger.debug("No invalidation patterns found for event: $eventType")
            return
        }

        try {
            if (properties.invalidation.asyncInvalidation) {
                invalidateAsync(patterns, entityId)
            } else {
                invalidateSync(patterns, entityId)
            }

            // Notificar listeners personalizados
            notifyCustomListeners(eventType, entityId)

        } catch (e: Exception) {
            logger.error("Error during cache invalidation for event: $eventType", e)
        }
    }

    /**
     * Invalida cache por patrón específico
     */
    fun invalidateByPattern(pattern: String) {
        try {
            logger.debug("Invalidating cache by pattern: $pattern")

            val fullPattern = "${properties.keyPrefix}:$pattern"
            val keys = redisTemplate.keys(fullPattern)

            if (keys.isNullOrEmpty()) {
                logger.debug("No keys found for pattern: $pattern")
                return
            }

            logger.info("Invalidating ${keys.size} cache entries for pattern: $pattern")

            if (properties.invalidation.batchSize > 0) {
                keys.chunked(properties.invalidation.batchSize).forEach { batch ->
                    redisTemplate.delete(batch)
                }
            } else {
                redisTemplate.delete(keys)
            }

        } catch (e: Exception) {
            logger.error("Error invalidating cache by pattern: $pattern", e)
        }
    }

    /**
     * Invalida cache por múltiples patrones
     */
    fun invalidateByPatterns(patterns: List<String>, entityId: String? = null) {
        patterns.forEach { pattern ->
            val resolvedPattern = resolvePattern(pattern, entityId)
            invalidateByPattern(resolvedPattern)
        }
    }

    /**
     * Invalida cache en cascada
     */
    fun cascadeInvalidation(rootEvent: String, entityId: String? = null) {
        if (!properties.invalidation.cascadeInvalidation) {
            return
        }

        logger.debug("Starting cascade invalidation for event: $rootEvent, entityId: $entityId")

        val processedEvents = mutableSetOf<String>()
        val eventsToProcess = mutableListOf(rootEvent)

        while (eventsToProcess.isNotEmpty()) {
            val currentEvent = eventsToProcess.removeAt(0)

            if (processedEvents.contains(currentEvent)) {
                continue
            }

            processedEvents.add(currentEvent)
            invalidateByEvent(currentEvent, entityId)

            // Buscar eventos relacionados para invalidación en cascada
            val relatedEvents = findRelatedEvents(currentEvent)
            eventsToProcess.addAll(relatedEvents.filter { !processedEvents.contains(it) })
        }

        logger.debug("Cascade invalidation completed for event: $rootEvent")
    }

    /**
     * Registra un listener personalizado para invalidación
     */
    fun registerInvalidationListener(eventType: String, listener: (String) -> Unit) {
        invalidationListeners.computeIfAbsent(eventType) { mutableListOf() }.add(listener)
        logger.debug("Registered custom invalidation listener for event: $eventType")
    }

    /**
     * Invalida todo el cache de una aplicación específica
     */
    fun invalidateAll() {
        try {
            logger.warn("Invalidating ALL cache entries")

            val pattern = "${properties.keyPrefix}:*"
            val keys = redisTemplate.keys(pattern)

            if (!keys.isNullOrEmpty()) {
                logger.info("Invalidating ${keys.size} total cache entries")

                if (properties.invalidation.batchSize > 0) {
                    keys.chunked(properties.invalidation.batchSize).forEach { batch ->
                        redisTemplate.delete(batch)
                    }
                } else {
                    redisTemplate.delete(keys)
                }
            }

        } catch (e: Exception) {
            logger.error("Error invalidating all cache entries", e)
        }
    }

    /**
     * Invalida cache por tags
     */
    fun invalidateByTags(tags: List<String>) {
        tags.forEach { tag ->
            val pattern = "tag:$tag:*"
            invalidateByPattern(pattern)
        }
    }

    /**
     * Programa invalidación diferida
     */
    fun scheduleInvalidation(eventType: String, entityId: String?, delayMillis: Long) {
        CompletableFuture.runAsync({
            Thread.sleep(delayMillis)
            invalidateByEvent(eventType, entityId)
        })
    }

    /**
     * Obtiene estadísticas de invalidación
     */
    fun getInvalidationStats(): InvalidationStats {
        // Implementar métricas de invalidación si es necesario
        return InvalidationStats(
            totalInvalidations = 0,
            invalidationsByEvent = emptyMap(),
            averageInvalidationTime = 0L,
            failedInvalidations = 0
        )
    }

    // Métodos privados

    private fun invalidateAsync(patterns: List<String>, entityId: String?) {
        CompletableFuture.runAsync {
            invalidateSync(patterns, entityId)
        }
    }

    private fun invalidateSync(patterns: List<String>, entityId: String?) {
        patterns.forEach { pattern ->
            val resolvedPattern = resolvePattern(pattern, entityId)
            invalidateByPattern(resolvedPattern)
        }
    }

    private fun resolvePattern(pattern: String, entityId: String?): String {
        return if (entityId != null && pattern.contains("{id}")) {
            pattern.replace("{id}", entityId)
        } else {
            pattern
        }
    }

    private fun notifyCustomListeners(eventType: String, entityId: String?) {
        invalidationListeners[eventType]?.forEach { listener ->
            try {
                listener(entityId ?: "")
            } catch (e: Exception) {
                logger.error("Error in custom invalidation listener for event: $eventType", e)
            }
        }
    }

    private fun findRelatedEvents(eventType: String): List<String> {
        // Lógica para encontrar eventos relacionados basada en el dominio
        return when {
            eventType.startsWith("user.") -> listOf("session.invalidate", "coupon.user.invalidate")
            eventType.startsWith("coupon.") -> listOf("campaign.stats.invalidate", "user.coupons.invalidate")
            eventType.startsWith("campaign.") -> listOf("coupon.campaign.invalidate")
            eventType.startsWith("station.") -> listOf("config.stations.invalidate")
            else -> emptyList()
        }
    }

    // Event Listeners para eventos de dominio

    @EventListener
    fun handleUserEvent(event: UserCacheEvent) {
        invalidateByEvent(event.eventType, event.userId)
    }

    @EventListener
    fun handleCouponEvent(event: CouponCacheEvent) {
        invalidateByEvent(event.eventType, event.couponId)
    }

    @EventListener
    fun handleCampaignEvent(event: CampaignCacheEvent) {
        invalidateByEvent(event.eventType, event.campaignId)
    }

    @EventListener
    fun handleStationEvent(event: StationCacheEvent) {
        invalidateByEvent(event.eventType, event.stationId)
    }
}

/**
 * Eventos de cache para diferentes dominios
 */
data class UserCacheEvent(val eventType: String, val userId: String)
data class CouponCacheEvent(val eventType: String, val couponId: String)
data class CampaignCacheEvent(val eventType: String, val campaignId: String)
data class StationCacheEvent(val eventType: String, val stationId: String)

/**
 * Estadísticas de invalidación
 */
data class InvalidationStats(
    val totalInvalidations: Long,
    val invalidationsByEvent: Map<String, Long>,
    val averageInvalidationTime: Long,
    val failedInvalidations: Long
)