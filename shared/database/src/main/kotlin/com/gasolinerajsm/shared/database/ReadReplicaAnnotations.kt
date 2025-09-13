package com.gasolinerajsm.shared.database

import org.springframework.core.annotation.AliasFor
import org.springframework.transaction.annotation.Transactional
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * Anotación para marcar métodos que deben ejecutarse en read replicas
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Transactional(readOnly = true)
annotation class ReadOnlyQuery(
    /**
     * Estrategia de selección de replica
     */
    val strategy: ReplicaSelectionStrategy = ReplicaSelectionStrategy.ROUND_ROBIN,

    /**
     * Índice específico de replica (solo para estrategia SPECIFIC)
     */
    val replicaIndex: Int = 0,

    /**
     * Timeout específico para la query (en milisegundos)
     */
    val timeout: Long = -1,

    /**
     * Si debe hacer fallback a write datasource en caso de error
     */
    val fallbackToWrite: Boolean = true
)

/**
 * Anotación para marcar métodos que requieren write datasource
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Transactional
annotation class WriteQuery(
    /**
     * Propagación de la transacción
     */
    @get:AliasFor(annotation = Transactional::class, attribute = "propagation")
    val propagation: org.springframework.transaction.annotation.Propagation = org.springframework.transaction.annotation.Propagation.REQUIRED,

    /**
     * Aislamiento de la transacción
     */
    @get:AliasFor(annotation = Transactional::class, attribute = "isolation")
    val isolation: org.springframework.transaction.annotation.Isolation = org.springframework.transaction.annotation.Isolation.DEFAULT,

    /**
     * Timeout de la transacción
     */
    @get:AliasFor(annotation = Transactional::class, attribute = "timeout")
    val timeout: Int = -1
)

/**
 * Anotación para optimización automática de queries
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OptimizedQuery(
    /**
     * Tipo de optimización a aplicar
     */
    val optimization: QueryOptimization = QueryOptimization.AUTO,

    /**
     * Si debe cachear el resultado de la query
     */
    val cacheable: Boolean = false,

    /**
     * Nombre del cache (si cacheable = true)
     */
    val cacheName: String = "",

    /**
     * TTL del cache en segundos
     */
    val cacheTtl: Long = 300,

    /**
     * Si debe usar paginación automática para resultados grandes
     */
    val autoPagination: Boolean = false,

    /**
     * Tamaño máximo de página para auto-paginación
     */
    val maxPageSize: Int = 1000
)

/**
 * Anotación para análisis de performance de queries
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MonitorQuery(
    /**
     * Nombre de la métrica personalizada
     */
    val metricName: String = "",

    /**
     * Umbral de tiempo lento en milisegundos
     */
    val slowQueryThreshold: Long = 1000,

    /**
     * Si debe loggear queries lentas
     */
    val logSlowQueries: Boolean = true,

    /**
     * Si debe generar métricas Prometheus
     */
    val generateMetrics: Boolean = true
)

/**
 * Estrategias de selección de read replica
 */
enum class ReplicaSelectionStrategy {
    /**
     * Round-robin entre todas las replicas disponibles
     */
    ROUND_ROBIN,

    /**
     * Selección aleatoria
     */
    RANDOM,

    /**
     * Replica específica por índice
     */
    SPECIFIC,

    /**
     * Replica con menor latencia
     */
    LOWEST_LATENCY,

    /**
     * Replica con menor carga
     */
    LOWEST_LOAD
}

/**
 * Tipos de optimización de queries
 */
enum class QueryOptimization {
    /**
     * Optimización automática basada en análisis
     */
    AUTO,

    /**
     * Optimización para queries de agregación
     */
    AGGREGATION,

    /**
     * Optimización para queries con JOINs complejos
     */
    COMPLEX_JOINS,

    /**
     * Optimización para queries con paginación
     */
    PAGINATION,

    /**
     * Optimización para queries de búsqueda full-text
     */
    FULL_TEXT_SEARCH,

    /**
     * Sin optimización
     */
    NONE
}