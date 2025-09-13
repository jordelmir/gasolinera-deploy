package com.gasolinerajsm.shared.database

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Detector de problemas N+1 en queries
 */
@Service
@ConditionalOnProperty(prefix = "gasolinera.database.optimization", name = ["n-plus-one-detection"], havingValue = "true", matchIfMissing = true)
class NPlusOneDetector(
    private val meterRegistry: MeterRegistry? = null
) {

    private val logger = LoggerFactory.getLogger(NPlusOneDetector::class.java)

    // Tracking de queries por request/transacción
    private val queryTracker = ThreadLocal<QueryTrackingContext>()

    // Estadísticas globales
    private val nPlusOneStats = ConcurrentHashMap<String, NPlusOneStats>()

    /**
     * Inicia el tracking de queries para un request/transacción
     */
    fun startTracking(requestId: String) {
        val context = QueryTrackingContext(
            requestId = requestId,
            startTime = Instant.now(),
            queries = mutableListOf()
        )
        queryTracker.set(context)

        logger.debug("Started N+1 tracking for request: $requestId")
    }

    /**
     * Registra una query ejecutada
     */
    fun recordQuery(
        sql: String,
        parameters: List<Any?> = emptyList(),
        executionTime: Duration,
        stackTrace: List<StackTraceElement> = Thread.currentThread().stackTrace.toList()
    ) {
        val context = queryTracker.get() ?: return

        val queryInfo = QueryInfo(
            sql = normalizeSql(sql),
            parameters = parameters,
            executionTime = executionTime,
            timestamp = Instant.now(),
            stackTrace = filterStackTrace(stackTrace)
        )

        context.queries.add(queryInfo)

        // Análisis en tiempo real para detección temprana
        if (context.queries.size > 10) { // Umbral configurable
            analyzeForNPlusOne(context)
        }
    }

    /**
     * Finaliza el tracking y analiza los resultados
     */
    fun finishTracking(): NPlusOneAnalysisResult? {
        val context = queryTracker.get() ?: return null

        try {
            val result = analyzeForNPlusOne(context)

            if (result.hasNPlusOneIssues) {
                logger.warn("N+1 query problem detected in request ${context.requestId}: ${result.issues.size} issues found")

                // Registrar métricas
                recordNPlusOneMetrics(result)

                // Actualizar estadísticas
                updateGlobalStats(result)
            }

            return result

        } finally {
            queryTracker.remove()
        }
    }

    /**
     * Analiza las queries para detectar patrones N+1
     */
    private fun analyzeForNPlusOne(context: QueryTrackingContext): NPlusOneAnalysisResult {
        val issues = mutableListOf<NPlusOneIssue>()
        val queryGroups = groupSimilarQueries(context.queries)

        queryGroups.forEach { (normalizedSql, queries) ->
            if (queries.size > 3) { // Umbral mínimo para considerar N+1
                val issue = analyzeQueryGroup(normalizedSql, queries)
                if (issue != null) {
                    issues.add(issue)
                }
            }
        }

        return NPlusOneAnalysisResult(
            requestId = context.requestId,
            totalQueries = context.queries.size,
            totalExecutionTime = context.queries.sumOf { it.executionTime.toMillis() },
            issues = issues,
            suggestions = generateSuggestions(issues)
        )
    }

    /**
     * Agrupa queries similares (mismo patrón SQL)
     */
    private fun groupSimilarQueries(queries: List<QueryInfo>): Map<String, List<QueryInfo>> {
        return queries.groupBy { it.sql }
    }

    /**
     * Analiza un grupo de queries similares para detectar N+1
     */
    private fun analyzeQueryGroup(normalizedSql: String, queries: List<QueryInfo>): NPlusOneIssue? {
        if (queries.size < 3) return null

        // Detectar patrones N+1 comunes
        val isSelectById = normalizedSql.contains("WHERE") &&
                          normalizedSql.contains("id = ?") &&
                          !normalizedSql.contains("IN (")

        val isSequentialExecution = areQueriesSequential(queries)
        val hasSimilarParameters = haveSimilarParameterPatterns(queries)

        if (isSelectById && isSequentialExecution && hasSimilarParameters) {
            return NPlusOneIssue(
                type = NPlusOneType.SELECT_BY_ID,
                sql = normalizedSql,
                queryCount = queries.size,
                totalExecutionTime = queries.sumOf { it.executionTime.toMillis() },
                averageExecutionTime = queries.map { it.executionTime.toMillis() }.average(),
                stackTrace = queries.first().stackTrace,
                suggestion = generateSelectByIdSuggestion(normalizedSql, queries)
            )
        }

        // Detectar otros patrones N+1
        if (queries.size > 5 && isSequentialExecution) {
            return NPlusOneIssue(
                type = NPlusOneType.REPEATED_QUERY,
                sql = normalizedSql,
                queryCount = queries.size,
                totalExecutionTime = queries.sumOf { it.executionTime.toMillis() },
                averageExecutionTime = queries.map { it.executionTime.toMillis() }.average(),
                stackTrace = queries.first().stackTrace,
                suggestion = generateRepeatedQuerySuggestion(normalizedSql, queries)
            )
        }

        return null
    }

    /**
     * Verifica si las queries se ejecutaron secuencialmente
     */
    private fun areQueriesSequential(queries: List<QueryInfo>): Boolean {
        if (queries.size < 2) return false

        val timeGaps = queries.zipWithNext { current, next ->
            Duration.between(current.timestamp, next.timestamp).toMillis()
        }

        // Si la mayoría de gaps son pequeños, probablemente son secuenciales
        val smallGaps = timeGaps.count { it < 100 } // 100ms threshold
        return smallGaps.toDouble() / timeGaps.size > 0.7
    }

    /**
     * Verifica si las queries tienen patrones de parámetros similares
     */
    private fun haveSimilarParameterPatterns(queries: List<QueryInfo>): Boolean {
        if (queries.size < 2) return false

        val parameterCounts = queries.map { it.parameters.size }.distinct()
        return parameterCounts.size == 1 // Todas tienen el mismo número de parámetros
    }

    /**
     * Genera sugerencia para problema SELECT BY ID
     */
    private fun generateSelectByIdSuggestion(sql: String, queries: List<QueryInfo>): String {
        val ids = queries.mapNotNull { it.parameters.firstOrNull() }.distinct()

        return """
            N+1 Query Problem Detected:
            - Current: ${queries.size} individual SELECT queries
            - Suggestion: Use IN clause with batch loading
            - Example: ${sql.replace("id = ?", "id IN (${ids.joinToString(", ") { "?" }})")}
            - Potential time savings: ${(queries.sumOf { it.executionTime.toMillis() } * 0.8).toLong()}ms
        """.trimIndent()
    }

    /**
     * Genera sugerencia para queries repetidas
     */
    private fun generateRepeatedQuerySuggestion(sql: String, queries: List<QueryInfo>): String {
        return """
            Repeated Query Problem Detected:
            - Query executed ${queries.size} times
            - Suggestion: Consider caching or batch processing
            - Current total time: ${queries.sumOf { it.executionTime.toMillis() }}ms
            - Consider using @Cacheable or batch operations
        """.trimIndent()
    }

    /**
     * Genera sugerencias generales basadas en los issues encontrados
     */
    private fun generateSuggestions(issues: List<NPlusOneIssue>): List<String> {
        val suggestions = mutableListOf<String>()

        if (issues.any { it.type == NPlusOneType.SELECT_BY_ID }) {
            suggestions.add("Consider using JPA @BatchSize annotation or custom batch loading")
            suggestions.add("Use JOIN FETCH for eager loading of associations")
            suggestions.add("Implement custom repository methods with IN clauses")
        }

        if (issues.any { it.type == NPlusOneType.REPEATED_QUERY }) {
            suggestions.add("Implement caching for frequently accessed data")
            suggestions.add("Use @EntityGraph for optimized loading")
            suggestions.add("Consider using projection queries for read-only operations")
        }

        if (issues.size > 3) {
            suggestions.add("Review overall data access patterns")
            suggestions.add("Consider implementing a data loader pattern")
            suggestions.add("Use database views for complex queries")
        }

        return suggestions
    }

    /**
     * Normaliza SQL para agrupación
     */
    private fun normalizeSql(sql: String): String {
        return sql
            .replace(Regex("\\s+"), " ") // Normalizar espacios
            .replace(Regex("\\b\\d+\\b"), "?") // Reemplazar números literales
            .replace(Regex("'[^']*'"), "?") // Reemplazar strings literales
            .trim()
            .uppercase()
    }

    /**
     * Filtra stack trace para mostrar solo código relevante
     */
    private fun filterStackTrace(stackTrace: List<StackTraceElement>): List<StackTraceElement> {
        return stackTrace
            .filter { element ->
                element.className.startsWith("com.gasolinerajsm") &&
                !element.className.contains("$Proxy") &&
                !element.className.contains("CGLIB")
            }
            .take(5) // Limitar a 5 elementos más relevantes
    }

    /**
     * Registra métricas de N+1
     */
    private fun recordNPlusOneMetrics(result: NPlusOneAnalysisResult) {
        meterRegistry?.let { registry ->
            registry.counter(
                "database.nplus1.detected",
                "request_id", result.requestId
            ).increment()

            result.issues.forEach { issue ->
                registry.counter(
                    "database.nplus1.issues",
                    "type", issue.type.name,
                    "query_count", issue.queryCount.toString()
                ).increment()

                registry.timer(
                    "database.nplus1.wasted_time",
                    "type", issue.type.name
                ).record(Duration.ofMillis(issue.totalExecutionTime))
            }
        }
    }

    /**
     * Actualiza estadísticas globales
     */
    private fun updateGlobalStats(result: NPlusOneAnalysisResult) {
        result.issues.forEach { issue ->
            val key = "${issue.type.name}:${issue.sql.hashCode()}"
            nPlusOneStats.compute(key) { _, existing ->
                if (existing == null) {
                    NPlusOneStats(
                        sql = issue.sql,
                        type = issue.type,
                        occurrences = AtomicInteger(1),
                        totalWastedTime = AtomicInteger(issue.totalExecutionTime.toInt()),
                        firstDetected = Instant.now(),
                        lastDetected = Instant.now()
                    )
                } else {
                    existing.occurrences.incrementAndGet()
                    existing.totalWastedTime.addAndGet(issue.totalExecutionTime.toInt())
                    existing.lastDetected = Instant.now()
                    existing
                }
            }
        }
    }

    /**
     * Obtiene estadísticas globales de N+1
     */
    fun getGlobalStats(): Map<String, NPlusOneStats> {
        return nPlusOneStats.toMap()
    }

    /**
     * Limpia estadísticas antiguas
     */
    fun cleanupOldStats(olderThan: Duration) {
        val cutoff = Instant.now().minus(olderThan)
        nPlusOneStats.entries.removeIf { (_, stats) ->
            stats.lastDetected.isBefore(cutoff)
        }
    }
}

// Clases de datos

data class QueryTrackingContext(
    val requestId: String,
    val startTime: Instant,
    val queries: MutableList<QueryInfo>
)

data class QueryInfo(
    val sql: String,
    val parameters: List<Any?>,
    val executionTime: Duration,
    val timestamp: Instant,
    val stackTrace: List<StackTraceElement>
)

data class NPlusOneAnalysisResult(
    val requestId: String,
    val totalQueries: Int,
    val totalExecutionTime: Long,
    val issues: List<NPlusOneIssue>,
    val suggestions: List<String>
) {
    val hasNPlusOneIssues: Boolean
        get() = issues.isNotEmpty()
}

data class NPlusOneIssue(
    val type: NPlusOneType,
    val sql: String,
    val queryCount: Int,
    val totalExecutionTime: Long,
    val averageExecutionTime: Double,
    val stackTrace: List<StackTraceElement>,
    val suggestion: String
)

data class NPlusOneStats(
    val sql: String,
    val type: NPlusOneType,
    val occurrences: AtomicInteger,
    val totalWastedTime: AtomicInteger,
    val firstDetected: Instant,
    var lastDetected: Instant
)

enum class NPlusOneType {
    SELECT_BY_ID,
    REPEATED_QUERY,
    LAZY_LOADING,
    MISSING_JOIN_FETCH,
    INEFFICIENT_PAGINATION
}