package com.gasolinerajsm.shared.database

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Aspecto para manejo automático de read replicas y optimización de queries
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "gasolinera.database.read-replica", name = ["enabled"], havingValue = "true")
class ReadReplicaAspect(
    private val readReplicaManager: ReadReplicaManager,
    private val queryOptimizer: QueryOptimizer,
    private val meterRegistry: MeterRegistry? = null,
    private val cacheManager: CacheManager? = null
) {

    private val logger = LoggerFactory.getLogger(ReadReplicaAspect::class.java)

    /**
     * Intercepta métodos anotados con @ReadOnlyQuery
     */
    @Around("@annotation(readOnlyQuery)")
    fun handleReadOnlyQuery(joinPoint: ProceedingJoinPoint, readOnlyQuery: ReadOnlyQuery): Any? {
        val startTime = Instant.now()
        val methodName = getMethodName(joinPoint)

        logger.debug("Executing read-only query: $methodName with strategy: ${readOnlyQuery.strategy}")

        return try {
            // Seleccionar replica según estrategia
            val replicaType = selectReplica(readOnlyQuery.strategy, readOnlyQuery.replicaIndex)

            // Ejecutar en read replica
            readReplicaManager.executeOnReadReplica {
                ReadReplicaContext.setDataSourceType(replicaType)

                val result = if (readOnlyQuery.timeout > 0) {
                    executeWithTimeout(joinPoint, readOnlyQuery.timeout)
                } else {
                    joinPoint.proceed()
                }

                recordQueryMetrics(methodName, startTime, true)
                result
            }

        } catch (e: Exception) {
            logger.warn("Error executing query on read replica: $methodName", e)

            if (readOnlyQuery.fallbackToWrite) {
                logger.info("Falling back to write datasource for: $methodName")

                readReplicaManager.executeOnWrite {
                    val result = joinPoint.proceed()
                    recordQueryMetrics(methodName, startTime, false, true)
                    result
                }
            } else {
                recordQueryMetrics(methodName, startTime, false)
                throw e
            }
        } finally {
            ReadReplicaContext.clear()
        }
    }

    /**
     * Intercepta métodos anotados con @WriteQuery
     */
    @Around("@annotation(writeQuery)")
    fun handleWriteQuery(joinPoint: ProceedingJoinPoint, writeQuery: WriteQuery): Any? {
        val startTime = Instant.now()
        val methodName = getMethodName(joinPoint)

        logger.debug("Executing write query: $methodName")

        return try {
            readReplicaManager.executeOnWrite {
                ReadReplicaContext.setDataSourceType(DataSourceType.WRITE)

                val result = if (writeQuery.timeout > 0) {
                    executeWithTimeout(joinPoint, writeQuery.timeout.toLong())
                } else {
                    joinPoint.proceed()
                }

                recordQueryMetrics(methodName, startTime, true)
                result
            }
        } catch (e: Exception) {
            recordQueryMetrics(methodName, startTime, false)
            throw e
        } finally {
            ReadReplicaContext.clear()
        }
    }

    /**
     * Intercepta métodos anotados con @OptimizedQuery
     */
    @Around("@annotation(optimizedQuery)")
    fun handleOptimizedQuery(joinPoint: ProceedingJoinPoint, optimizedQuery: OptimizedQuery): Any? {
        val startTime = Instant.now()
        val methodName = getMethodName(joinPoint)

        logger.debug("Executing optimized query: $methodName with optimization: ${optimizedQuery.optimization}")

        return try {
            // Verificar cache si está habilitado
            if (optimizedQuery.cacheable && cacheManager != null) {
                val cacheKey = generateCacheKey(joinPoint)
                val cacheName = optimizedQuery.cacheName.ifEmpty { "optimized-queries" }

                val cache = cacheManager.getCache(cacheName)
                val cachedResult = cache?.get(cacheKey)

                if (cachedResult != null) {
                    logger.debug("Cache hit for query: $methodName")
                    recordCacheMetrics(methodName, true)
                    return cachedResult.get()
                }
            }

            // Aplicar optimizaciones
            val optimizedJoinPoint = applyQueryOptimizations(joinPoint, optimizedQuery)

            val result = optimizedJoinPoint.proceed()

            // Guardar en cache si está habilitado
            if (optimizedQuery.cacheable && cacheManager != null && result != null) {
                val cacheKey = generateCacheKey(joinPoint)
                val cacheName = optimizedQuery.cacheName.ifEmpty { "optimized-queries" }

                cacheManager.getCache(cacheName)?.put(cacheKey, result)
                recordCacheMetrics(methodName, false)
            }

            recordQueryMetrics(methodName, startTime, true)
            result

        } catch (e: Exception) {
            recordQueryMetrics(methodName, startTime, false)
            throw e
        }
    }

    /**
     * Intercepta métodos anotados con @MonitorQuery
     */
    @Around("@annotation(monitorQuery)")
    fun handleMonitorQuery(joinPoint: ProceedingJoinPoint, monitorQuery: MonitorQuery): Any? {
        val startTime = Instant.now()
        val methodName = getMethodName(joinPoint)
        val metricName = monitorQuery.metricName.ifEmpty { methodName }

        return try {
            val result = joinPoint.proceed()

            val duration = Duration.between(startTime, Instant.now())

            // Registrar métricas
            if (monitorQuery.generateMetrics) {
                recordDetailedMetrics(metricName, duration, true)
            }

            // Loggear queries lentas
            if (monitorQuery.logSlowQueries && duration.toMillis() > monitorQuery.slowQueryThreshold) {
                logger.warn("Slow query detected: $methodName took ${duration.toMillis()}ms")
            }

            result

        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())

            if (monitorQuery.generateMetrics) {
                recordDetailedMetrics(metricName, duration, false)
            }

            logger.error("Query failed: $methodName after ${duration.toMillis()}ms", e)
            throw e
        }
    }

    // Métodos auxiliares

    private fun selectReplica(strategy: ReplicaSelectionStrategy, replicaIndex: Int): DataSourceType {
        return when (strategy) {
            ReplicaSelectionStrategy.ROUND_ROBIN -> readReplicaManager.selectNextReadReplica()
            ReplicaSelectionStrategy.RANDOM -> {
                val randomIndex = ThreadLocalRandom.current().nextInt(0, getReplicaCount())
                DataSourceType.read(randomIndex)
            }
            ReplicaSelectionStrategy.SPECIFIC -> DataSourceType.read(replicaIndex)
            ReplicaSelectionStrategy.LOWEST_LATENCY -> selectLowestLatencyReplica()
            ReplicaSelectionStrategy.LOWEST_LOAD -> selectLowestLoadReplica()
        }
    }

    private fun selectLowestLatencyReplica(): DataSourceType {
        // Implementar lógica para seleccionar replica con menor latencia
        // Por ahora usar round-robin como fallback
        return readReplicaManager.selectNextReadReplica()
    }

    private fun selectLowestLoadReplica(): DataSourceType {
        // Implementar lógica para seleccionar replica con menor carga
        // Por ahora usar round-robin como fallback
        return readReplicaManager.selectNextReadReplica()
    }

    private fun getReplicaCount(): Int {
        // Obtener número de replicas configuradas
        return 1 // Placeholder
    }

    private fun executeWithTimeout(joinPoint: ProceedingJoinPoint, timeoutMs: Long): Any? {
        // Implementar ejecución con timeout
        // Por simplicidad, ejecutar normalmente por ahora
        return joinPoint.proceed()
    }

    private fun applyQueryOptimizations(
        joinPoint: ProceedingJoinPoint,
        optimizedQuery: OptimizedQuery
    ): ProceedingJoinPoint {
        // Aplicar optimizaciones según el tipo
        when (optimizedQuery.optimization) {
            QueryOptimization.AUTO -> {
                // Análisis automático y aplicación de optimizaciones
                analyzeAndOptimizeQuery(joinPoint)
            }
            QueryOptimization.PAGINATION -> {
                if (optimizedQuery.autoPagination) {
                    applyAutoPagination(joinPoint, optimizedQuery.maxPageSize)
                }
            }
            else -> {
                // Otras optimizaciones específicas
            }
        }

        return joinPoint
    }

    private fun analyzeAndOptimizeQuery(joinPoint: ProceedingJoinPoint) {
        // Usar QueryOptimizer para análisis automático
        val methodName = getMethodName(joinPoint)
        logger.debug("Applying automatic optimization for: $methodName")
    }

    private fun applyAutoPagination(joinPoint: ProceedingJoinPoint, maxPageSize: Int) {
        // Implementar paginación automática
        logger.debug("Applying auto-pagination with max size: $maxPageSize")
    }

    private fun generateCacheKey(joinPoint: ProceedingJoinPoint): String {
        val methodName = getMethodName(joinPoint)
        val args = joinPoint.args?.contentHashCode() ?: 0
        return "$methodName:$args"
    }

    private fun getMethodName(joinPoint: ProceedingJoinPoint): String {
        val signature = joinPoint.signature as MethodSignature
        return "${signature.declaringType.simpleName}.${signature.name}"
    }

    private fun recordQueryMetrics(
        methodName: String,
        startTime: Instant,
        success: Boolean,
        fallback: Boolean = false
    ) {
        val duration = Duration.between(startTime, Instant.now())

        meterRegistry?.let { registry ->
            val timer = Timer.builder("database.query.duration")
                .tag("method", methodName)
                .tag("success", success.toString())
                .tag("fallback", fallback.toString())
                .tag("datasource", if (ReadReplicaContext.isReadOnly()) "read" else "write")
                .register(registry)

            timer.record(duration)

            registry.counter(
                "database.query.count",
                "method", methodName,
                "success", success.toString(),
                "fallback", fallback.toString(),
                "datasource", if (ReadReplicaContext.isReadOnly()) "read" else "write"
            ).increment()
        }
    }

    private fun recordCacheMetrics(methodName: String, hit: Boolean) {
        meterRegistry?.let { registry ->
            registry.counter(
                "database.query.cache",
                "method", methodName,
                "result", if (hit) "hit" else "miss"
            ).increment()
        }
    }

    private fun recordDetailedMetrics(metricName: String, duration: Duration, success: Boolean) {
        meterRegistry?.let { registry ->
            val timer = Timer.builder("database.query.detailed")
                .tag("query", metricName)
                .tag("success", success.toString())
                .register(registry)

            timer.record(duration)
        }
    }
}