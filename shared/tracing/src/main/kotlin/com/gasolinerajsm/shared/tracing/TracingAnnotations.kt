package com.gasolinerajsm.shared.tracing

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * Tracing Annotations for automatic span creation
 */

/**
 * Trace a method execution with custom span name and attributes
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Traced(
    val operationName: String = "",
    val spanKind: String = "INTERNAL", // INTERNAL, SERVER, CLIENT, PRODUCER, CONSUMER
    val attributes: Array<String> = []
)

/**
 * Trace a business operation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TraceBusinessOperation(
    val operation: String,
    val entityType: String = "",
    val includeParameters: Boolean = true,
    val includeResult: Boolean = false
)

/**
 * Trace a database operation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TraceDatabaseOperation(
    val operation: String = "", // SELECT, INSERT, UPDATE, DELETE
    val table: String = "",
    val includeQuery: Boolean = false
)

/**
 * Trace a cache operation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TraceCacheOperation(
    val operation: String = "", // GET, SET, DELETE, EVICT
    val keyParameter: String = "",
    val includeValue: Boolean = false
)

/**
 * Trace an external API call
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TraceExternalCall(
    val service: String,
    val operation: String = "",
    val includeRequest: Boolean = false,
    val includeResponse: Boolean = false
)

/**
 * Aspect for processing tracing annotations
 */
@Aspect
@Component
class TracingAspect(
    private val businessTracingService: BusinessTracingService
) {

    /**
     * Process @Traced annotation
     */
    @Around("@annotation(traced)")
    fun traceMethod(joinPoint: ProceedingJoinPoint, traced: Traced): Any? {
        val operationName = if (traced.operationName.isNotEmpty()) {
            traced.operationName
        } else {
            "${joinPoint.signature.declaringType.simpleName}.${joinPoint.signature.name}"
        }

        val attributes = mutableMapOf<String, Any>()

        // Add method information
        attributes["method.class"] = joinPoint.signature.declaringType.simpleName
        attributes["method.name"] = joinPoint.signature.name

        // Add custom attributes from annotation
        traced.attributes.forEach { attr ->
            val parts = attr.split("=", limit = 2)
            if (parts.size == 2) {
                attributes[parts[0]] = parts[1]
            }
        }

        // Add parameter information
        addParameterAttributes(joinPoint, attributes)

        return businessTracingService.traceOperation(
            operationType = operationName,
            attributes = attributes
        ) {
            joinPoint.proceed()
        }
    }

    /**
     * Process @TraceBusinessOperation annotation
     */
    @Around("@annotation(traceBusinessOperation)")
    fun traceBusinessOperation(
        joinPoint: ProceedingJoinPoint,
        traceBusinessOperation: TraceBusinessOperation
    ): Any? {
        val attributes = mutableMapOf<String, Any>()

        if (traceBusinessOperation.entityType.isNotEmpty()) {
            attributes["entity.type"] = traceBusinessOperation.entityType
        }

        if (traceBusinessOperation.includeParameters) {
            addParameterAttributes(joinPoint, attributes)
        }

        return businessTracingService.traceOperation(
            operationType = traceBusinessOperation.operation,
            attributes = attributes
        ) {
            val result = joinPoint.proceed()

            if (traceBusinessOperation.includeResult && result != null) {
                businessTracingService.addSpanAttributes(mapOf(
                    "result.type" to result.javaClass.simpleName,
                    "result.value" to result.toString()
                ))
            }

            result
        }
    }

    /**
     * Process @TraceDatabaseOperation annotation
     */
    @Around("@annotation(traceDatabaseOperation)")
    fun traceDatabaseOperation(
        joinPoint: ProceedingJoinPoint,
        traceDatabaseOperation: TraceDatabaseOperation
    ): Any? {
        val operation = if (traceDatabaseOperation.operation.isNotEmpty()) {
            traceDatabaseOperation.operation
        } else {
            extractOperationFromMethodName(joinPoint.signature.name)
        }

        val table = if (traceDatabaseOperation.table.isNotEmpty()) {
            traceDatabaseOperation.table
        } else {
            extractTableFromClass(joinPoint.signature.declaringType.simpleName)
        }

        val query = if (traceDatabaseOperation.includeQuery) {
            extractQueryFromParameters(joinPoint.args)
        } else null

        return businessTracingService.traceDatabaseOperation(
            operation = operation,
            table = table,
            query = query
        ) {
            joinPoint.proceed()
        }
    }

    /**
     * Process @TraceCacheOperation annotation
     */
    @Around("@annotation(traceCacheOperation)")
    fun traceCacheOperation(
        joinPoint: ProceedingJoinPoint,
        traceCacheOperation: TraceCacheOperation
    ): Any? {
        val operation = if (traceCacheOperation.operation.isNotEmpty()) {
            traceCacheOperation.operation
        } else {
            extractOperationFromMethodName(joinPoint.signature.name)
        }

        val key = if (traceCacheOperation.keyParameter.isNotEmpty()) {
            extractParameterValue(joinPoint, traceCacheOperation.keyParameter)?.toString()
        } else {
            extractKeyFromParameters(joinPoint.args)
        }

        return businessTracingService.traceRedisOperation(
            operation = operation,
            key = key
        ) {
            val result = joinPoint.proceed()

            if (traceCacheOperation.includeValue && result != null) {
                businessTracingService.addSpanAttributes(mapOf(
                    "cache.value.type" to result.javaClass.simpleName,
                    "cache.value.size" to result.toString().length
                ))
            }

            result
        }
    }

    /**
     * Process @TraceExternalCall annotation
     */
    @Around("@annotation(traceExternalCall)")
    fun traceExternalCall(
        joinPoint: ProceedingJoinPoint,
        traceExternalCall: TraceExternalCall
    ): Any? {
        val operation = if (traceExternalCall.operation.isNotEmpty()) {
            traceExternalCall.operation
        } else {
            joinPoint.signature.name
        }

        val attributes = mutableMapOf<String, Any>()

        if (traceExternalCall.includeRequest) {
            addParameterAttributes(joinPoint, attributes, "request")
        }

        return businessTracingService.traceExternalApiCall(
            service = traceExternalCall.service,
            operation = operation
        ) {
            val result = joinPoint.proceed()

            if (traceExternalCall.includeResponse && result != null) {
                businessTracingService.addSpanAttributes(mapOf(
                    "response.type" to result.javaClass.simpleName,
                    "response.size" to result.toString().length
                ))
            }

            result
        }
    }

    /**
     * Add parameter attributes to span
     */
    private fun addParameterAttributes(
        joinPoint: ProceedingJoinPoint,
        attributes: MutableMap<String, Any>,
        prefix: String = "param"
    ) {
        val parameterNames = getParameterNames(joinPoint)
        joinPoint.args.forEachIndexed { index, arg ->
            if (arg != null && !isComplexObject(arg)) {
                val paramName = parameterNames.getOrNull(index) ?: "arg$index"
                attributes["$prefix.$paramName"] = arg.toString()
            }
        }
    }

    /**
     * Extract parameter value by name
     */
    private fun extractParameterValue(joinPoint: ProceedingJoinPoint, parameterName: String): Any? {
        val parameterNames = getParameterNames(joinPoint)
        val index = parameterNames.indexOf(parameterName)
        return if (index >= 0 && index < joinPoint.args.size) {
            joinPoint.args[index]
        } else null
    }

    /**
     * Get parameter names from method signature
     */
    private fun getParameterNames(joinPoint: ProceedingJoinPoint): List<String> {
        // This is a simplified implementation
        // In a real scenario, you might use reflection or compile-time parameter name retention
        return (0 until joinPoint.args.size).map { "param$it" }
    }

    /**
     * Check if object is complex (should not be included in traces)
     */
    private fun isComplexObject(obj: Any): Boolean {
        return when (obj) {
            is String, is Number, is Boolean, is Enum<*> -> false
            is Collection<*> -> obj.size > 10 // Don't include large collections
            else -> obj.javaClass.packageName.startsWith("com.gasolinerajsm") // Don't include domain objects
        }
    }

    /**
     * Extract operation from method name
     */
    private fun extractOperationFromMethodName(methodName: String): String {
        return when {
            methodName.startsWith("find") || methodName.startsWith("get") -> "SELECT"
            methodName.startsWith("save") || methodName.startsWith("create") -> "INSERT"
            methodName.startsWith("update") -> "UPDATE"
            methodName.startsWith("delete") || methodName.startsWith("remove") -> "DELETE"
            methodName.startsWith("set") || methodName.startsWith("put") -> "SET"
            else -> methodName.uppercase()
        }
    }

    /**
     * Extract table name from class name
     */
    private fun extractTableFromClass(className: String): String {
        return className
            .replace("Repository", "")
            .replace("Service", "")
            .replace("Impl", "")
            .lowercase()
    }

    /**
     * Extract query from method parameters
     */
    private fun extractQueryFromParameters(args: Array<Any?>): String? {
        return args.find { it is String && it.contains("SELECT", ignoreCase = true) } as? String
    }

    /**
     * Extract cache key from parameters
     */
    private fun extractKeyFromParameters(args: Array<Any?>): String? {
        return args.firstOrNull { it is String }?.toString()
    }
}