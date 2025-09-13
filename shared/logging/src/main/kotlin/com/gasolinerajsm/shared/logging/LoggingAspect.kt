package com.gasolinerajsm.shared.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Aspecto AOP para logging automático basado en anotaciones
 */
@Aspect
@Component
class LoggingAspect(
    private val structuredLogger: StructuredLogger
) {

    /**
     * Intercepta métodos anotados con @LogBusinessOperation
     */
    @Around("@annotation(logBusinessOperation)")
    fun logBusinessOperation(
        joinPoint: ProceedingJoinPoint,
        logBusinessOperation: LogBusinessOperation
    ): Any? {
        val logger = LoggerFactory.getLogger(joinPoint.target.javaClass)
        val methodName = joinPoint.signature.name
        val args = joinPoint.args

        val startTime = System.currentTimeMillis()

        // Contexto inicial
        val context = mutableMapOf<String, Any>(
            "method" to methodName,
            "class" to joinPoint.target.javaClass.simpleName
        )

        // Agregar parámetros si está habilitado
        if (logBusinessOperation.includeParameters && args.isNotEmpty()) {
            context["parameters"] = args.mapIndexed { index, arg ->
                "param$index" to (arg?.toString()?.take(100) ?: "null")
            }.toMap()
        }

        // Log de inicio
        structuredLogger.business(
            logger,
            logBusinessOperation.operation,
            "Starting business operation: $methodName",
            MdcUtils.getCurrentUserId(),
            null,
            context
        )

        return try {
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - startTime

            // Contexto de éxito
            val successContext = context.toMutableMap().apply {
                put("success", true)
                put("durationMs", duration)
            }

            // Agregar resultado si está habilitado
            if (logBusinessOperation.includeResult && result != null) {
                successContext["result"] = result.toString().take(200)
            }

            // Log de éxito
            structuredLogger.business(
                logger,
                logBusinessOperation.operation,
                "Business operation completed successfully: $methodName",
                MdcUtils.getCurrentUserId(),
                extractEntityId(result),
                successContext
            )

            result
        } catch (exception: Exception) {
            val duration = System.currentTimeMillis() - startTime

            // Contexto de error
            val errorContext = context.toMutableMap().apply {
                put("success", false)
                put("durationMs", duration)
                put("error", exception.message ?: "Unknown error")
                put("exceptionType", exception.javaClass.simpleName)
            }

            // Log de error
            when (logBusinessOperation.logLevel) {
                LogLevel.ERROR -> structuredLogger.error(
                    logger,
                    "Business operation failed: $methodName",
                    LogEvent(
                        type = "BUSINESS_OPERATION_ERROR",
                        category = "BUSINESS",
                        action = logBusinessOperation.operation.name,
                        resource = methodName
                    ),
                    errorContext,
                    exception
                )
                LogLevel.WARN -> structuredLogger.warn(
                    logger,
                    "Business operation failed: $methodName",
                    LogEvent(
                        type = "BUSINESS_OPERATION_WARNING",
                        category = "BUSINESS",
                        action = logBusinessOperation.operation.name,
                        resource = methodName
                    ),
                    errorContext,
                    exception
                )
                else -> structuredLogger.info(
                    logger,
                    "Business operation completed with issues: $methodName",
                    LogEvent(
                        type = "BUSINESS_OPERATION_INFO",
                        category = "BUSINESS",
                        action = logBusinessOperation.operation.name,
                        resource = methodName
                    ),
                    errorContext
                )
            }

            throw exception
        }
    }

    /**
     * Intercepta métodos anotados con @LogPerformance
     */
    @Around("@annotation(logPerformance)")
    fun logPerformance(
        joinPoint: ProceedingJoinPoint,
        logPerformance: LogPerformance
    ): Any? {
        val logger = LoggerFactory.getLogger(joinPoint.target.javaClass)
        val methodName = joinPoint.signature.name
        val operationName = logPerformance.operation.ifEmpty { methodName }
        val args = joinPoint.args

        val startTime = System.currentTimeMillis()

        return try {
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - startTime

            // Contexto de performance
            val context = mutableMapOf<String, Any>(
                "method" to methodName,
                "class" to joinPoint.target.javaClass.simpleName,
                "threshold" to logPerformance.threshold
            )

            // Agregar parámetros si está habilitado
            if (logPerformance.includeParameters && args.isNotEmpty()) {
                context["parameters"] = args.mapIndexed { index, arg ->
                    "param$index" to (arg?.toString()?.take(50) ?: "null")
                }.toMap()
            }

            // Log solo si excede el threshold o si hay error
            if (duration > logPerformance.threshold) {
                structuredLogger.performance(
                    logger,
                    operationName,
                    duration,
                    true,
                    context.apply { put("exceededThreshold", true) }
                )
            } else {
                // Log de debug para operaciones rápidas
                structuredLogger.debug(
                    logger,
                    "Performance: $operationName completed in ${duration}ms",
                    LogEvent(
                        type = "PERFORMANCE_METRIC",
                        category = "PERFORMANCE",
                        action = operationName,
                        resource = methodName
                    ),
                    context
                )
            }

            result
        } catch (exception: Exception) {
            val duration = System.currentTimeMillis() - startTime

            structuredLogger.performance(
                logger,
                operationName,
                duration,
                false,
                mapOf(
                    "method" to methodName,
                    "class" to joinPoint.target.javaClass.simpleName,
                    "error" to (exception.message ?: "Unknown error"),
                    "exceptionType" to exception.javaClass.simpleName
                )
            )

            throw exception
        }
    }

    /**
     * Extrae el ID de entidad del resultado si es posible
     */
    private fun extractEntityId(result: Any?): String? {
        if (result == null) return null

        return try {
            // Intentar obtener ID usando reflexión
            val idField = result.javaClass.getDeclaredField("id")
            idField.isAccessible = true
            idField.get(result)?.toString()
        } catch (e: Exception) {
            try {
                // Intentar método getId()
                val getIdMethod = result.javaClass.getMethod("getId")
                getIdMethod.invoke(result)?.toString()
            } catch (e: Exception) {
                null
            }
        }
    }
}