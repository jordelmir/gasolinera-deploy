package com.gasolinerajsm.shared.cache

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Servicio para locks distribuidos usando Redis
 */
@Service
class DistributedLockService(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(DistributedLockService::class.java)
    private val activeLocks = ConcurrentHashMap<String, LockInfo>()

    // Script Lua para adquisición atómica de lock
    private val acquireLockScript = DefaultRedisScript<Boolean>().apply {
        scriptText = """
            if redis.call('exists', KEYS[1]) == 0 then
                redis.call('setex', KEYS[1], ARGV[2], ARGV[1])
                return true
            else
                return false
            end
        """.trimIndent()
        resultType = Boolean::class.java
    }

    // Script Lua para liberación segura de lock
    private val releaseLockScript = DefaultRedisScript<Boolean>().apply {
        scriptText = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return false
            end
        """.trimIndent()
        resultType = Boolean::class.java
    }

    // Script Lua para renovación de lock
    private val renewLockScript = DefaultRedisScript<Boolean>().apply {
        scriptText = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('expire', KEYS[1], ARGV[2])
                return true
            else
                return false
            end
        """.trimIndent()
        resultType = Boolean::class.java
    }

    /**
     * Intenta adquirir un lock distribuido
     */
    fun tryLock(
        lockKey: String,
        ttl: Duration = Duration.ofMinutes(5),
        waitTime: Duration = Duration.ZERO
    ): DistributedLock? {
        val lockId = generateLockId()
        val fullKey = buildLockKey(lockKey)
        val startTime = Instant.now()

        logger.debug("Attempting to acquire lock: $lockKey with ID: $lockId")

        do {
            val acquired = redisTemplate.execute(
                acquireLockScript,
                listOf(fullKey),
                lockId,
                ttl.seconds.toString()
            ) as Boolean

            if (acquired) {
                val lockInfo = LockInfo(
                    lockKey = lockKey,
                    lockId = lockId,
                    acquiredAt = Instant.now(),
                    ttl = ttl,
                    threadId = Thread.currentThread().id
                )

                activeLocks[lockKey] = lockInfo
                logger.debug("Successfully acquired lock: $lockKey with ID: $lockId")

                return DistributedLock(
                    lockKey = lockKey,
                    lockId = lockId,
                    lockService = this,
                    autoRenew = false
                )
            }

            if (waitTime > Duration.ZERO) {
                val elapsed = Duration.between(startTime, Instant.now())
                if (elapsed >= waitTime) {
                    break
                }

                try {
                    Thread.sleep(100) // Esperar 100ms antes de reintentar
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        } while (waitTime > Duration.ZERO)

        logger.debug("Failed to acquire lock: $lockKey")
        return null
    }

    /**
     * Intenta adquirir un lock con auto-renovación
     */
    fun tryLockWithAutoRenew(
        lockKey: String,
        ttl: Duration = Duration.ofMinutes(5),
        waitTime: Duration = Duration.ZERO,
        renewInterval: Duration = Duration.ofMinutes(2)
    ): DistributedLock? {
        val lock = tryLock(lockKey, ttl, waitTime)
        return lock?.apply {
            enableAutoRenew(renewInterval)
        }
    }

    /**
     * Libera un lock distribuido
     */
    fun releaseLock(lockKey: String, lockId: String): Boolean {
        val fullKey = buildLockKey(lockKey)

        logger.debug("Attempting to release lock: $lockKey with ID: $lockId")

        val released = redisTemplate.execute(
            releaseLockScript,
            listOf(fullKey),
            lockId
        ) as Boolean

        if (released) {
            activeLocks.remove(lockKey)
            logger.debug("Successfully released lock: $lockKey with ID: $lockId")
        } else {
            logger.warn("Failed to release lock: $lockKey with ID: $lockId - lock may have expired or been acquired by another process")
        }

        return released
    }

    /**
     * Renueva un lock distribuido
     */
    fun renewLock(lockKey: String, lockId: String, ttl: Duration): Boolean {
        val fullKey = buildLockKey(lockKey)

        logger.debug("Attempting to renew lock: $lockKey with ID: $lockId")

        val renewed = redisTemplate.execute(
            renewLockScript,
            listOf(fullKey),
            lockId,
            ttl.seconds.toString()
        ) as Boolean

        if (renewed) {
            activeLocks[lockKey]?.let { lockInfo ->
                activeLocks[lockKey] = lockInfo.copy(ttl = ttl)
            }
            logger.debug("Successfully renewed lock: $lockKey with ID: $lockId")
        } else {
            logger.warn("Failed to renew lock: $lockKey with ID: $lockId")
        }

        return renewed
    }

    /**
     * Verifica si un lock está activo
     */
    fun isLocked(lockKey: String): Boolean {
        val fullKey = buildLockKey(lockKey)
        return redisTemplate.hasKey(fullKey)
    }

    /**
     * Obtiene información de un lock activo
     */
    fun getLockInfo(lockKey: String): LockInfo? {
        return activeLocks[lockKey]
    }

    /**
     * Obtiene todos los locks activos del proceso actual
     */
    fun getActiveLocks(): Map<String, LockInfo> {
        return activeLocks.toMap()
    }

    /**
     * Ejecuta una operación con lock distribuido
     */
    fun <T> withLock(
        lockKey: String,
        ttl: Duration = Duration.ofMinutes(5),
        waitTime: Duration = Duration.ofSeconds(10),
        operation: () -> T
    ): T? {
        val lock = tryLock(lockKey, ttl, waitTime)
        return if (lock != null) {
            try {
                operation()
            } finally {
                lock.release()
            }
        } else {
            logger.warn("Could not acquire lock for operation: $lockKey")
            null
        }
    }

    /**
     * Ejecuta una operación con lock distribuido y auto-renovación
     */
    fun <T> withAutoRenewLock(
        lockKey: String,
        ttl: Duration = Duration.ofMinutes(5),
        waitTime: Duration = Duration.ofSeconds(10),
        renewInterval: Duration = Duration.ofMinutes(2),
        operation: () -> T
    ): T? {
        val lock = tryLockWithAutoRenew(lockKey, ttl, waitTime, renewInterval)
        return if (lock != null) {
            try {
                operation()
            } finally {
                lock.release()
            }
        } else {
            logger.warn("Could not acquire auto-renew lock for operation: $lockKey")
            null
        }
    }

    /**
     * Limpia locks expirados del registro local
     */
    fun cleanupExpiredLocks() {
        val now = Instant.now()
        val expiredLocks = activeLocks.filter { (_, lockInfo) ->
            val expirationTime = lockInfo.acquiredAt.plus(lockInfo.ttl)
            now.isAfter(expirationTime)
        }

        expiredLocks.forEach { (lockKey, _) ->
            activeLocks.remove(lockKey)
            logger.debug("Cleaned up expired lock: $lockKey")
        }
    }

    /**
     * Fuerza la liberación de todos los locks del proceso actual
     */
    fun releaseAllLocks() {
        val locksToRelease = activeLocks.toMap()
        locksToRelease.forEach { (lockKey, lockInfo) ->
            releaseLock(lockKey, lockInfo.lockId)
        }
        logger.info("Released ${locksToRelease.size} locks")
    }

    // Métodos auxiliares

    private fun generateLockId(): String {
        return "${UUID.randomUUID()}-${Thread.currentThread().id}-${System.currentTimeMillis()}"
    }

    private fun buildLockKey(lockKey: String): String {
        return "gasolinera:lock:$lockKey"
    }
}

/**
 * Representa un lock distribuido
 */
class DistributedLock(
    val lockKey: String,
    val lockId: String,
    private val lockService: DistributedLockService,
    private var autoRenew: Boolean = false
) {

    private val logger = LoggerFactory.getLogger(DistributedLock::class.java)
    private var renewalTimer: Timer? = null

    /**
     * Habilita auto-renovación del lock
     */
    fun enableAutoRenew(renewInterval: Duration) {
        if (autoRenew) return

        autoRenew = true
        renewalTimer = Timer("LockRenewal-$lockKey", true)

        renewalTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val renewed = lockService.renewLock(lockKey, lockId, Duration.ofMinutes(5))
                if (!renewed) {
                    logger.warn("Failed to renew lock: $lockKey - stopping auto-renewal")
                    cancel()
                }
            }
        }, renewInterval.toMillis(), renewInterval.toMillis())

        logger.debug("Enabled auto-renewal for lock: $lockKey")
    }

    /**
     * Deshabilita auto-renovación del lock
     */
    fun disableAutoRenew() {
        if (!autoRenew) return

        autoRenew = false
        renewalTimer?.cancel()
        renewalTimer = null

        logger.debug("Disabled auto-renewal for lock: $lockKey")
    }

    /**
     * Renueva manualmente el lock
     */
    fun renew(ttl: Duration = Duration.ofMinutes(5)): Boolean {
        return lockService.renewLock(lockKey, lockId, ttl)
    }

    /**
     * Libera el lock
     */
    fun release(): Boolean {
        disableAutoRenew()
        return lockService.releaseLock(lockKey, lockId)
    }

    /**
     * Verifica si el lock sigue siendo válido
     */
    fun isValid(): Boolean {
        return lockService.isLocked(lockKey)
    }
}

/**
 * Información de un lock activo
 */
data class LockInfo(
    val lockKey: String,
    val lockId: String,
    val acquiredAt: Instant,
    val ttl: Duration,
    val threadId: Long
)