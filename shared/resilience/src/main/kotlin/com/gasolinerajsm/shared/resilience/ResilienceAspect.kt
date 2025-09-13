package com.gasolinerajsm.shared.resilience

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * Aspecto AOP para aplicar patrones de resilience automáticamente
 */
@Aspect
@Component
class ResilienceAspect(
    private val resilienceService: ResilienceService,
    private val fallbackService: FallbackService
) {

    private val logger = LoggerFactory.getLogger(ResilienceAspect::class.java)

    /**
     * Intercepta métodos anotados con @CircuitBreaker
     */
    @Around("@annotation(circuitBreaker)")
    fun handleCircuitBreaker(joinPoint: ProceedingJoinPoint, circuitBreaker: CircuitBreaker): Any? {
        val fallbackMethod = if (circuitBreaker.fallbackMethod.isNotEmpty()) {
            findFallbackMethod(joinPoint, circuitBreaker.fallbackMethod)
        } else null

        return resilienceService.executeWithCircuitBreaker(
            name = circuitBreaker.name,
            operation = { joinPoint.proceed() },
            fallback = fallbackMethod?.let { method ->
                { invokeFallbackMethod(joinPoint, method) }
            }
        )
    }

    /**
     * Intercepta métodos anotados con @Retry
     */
    @Around("@annotation(retry)")
    fun handleRetry(joinPoint: ProceedingJoinPoint, retry: Retry): Any? {
        val fallbackMethod = if (retry.fallbackMethod.isNotEmpty()) {
            findFallbackMethod(joinPoint, retry.fallbackMethod)
        } else null

        return resilienceService.executeWithRetry(
            name = retry.name,
            operation = { joinPoint.proceed() },
            fallback = fallbackMethod?.let { method ->
                { invokeFallbackMethod(joinPoint, method) }
            }
        )
    }

    /**
     * Intercepta métodos anotados con @Bulkhead
     */
    @Around("@annotation(bulkhead)")
    fun handleBulkhead(joinPoint: ProceedingJoinPoint, bulkhead: Bulkhead): Any? {
        return resilienceService.executeWithBulkhead(
            name = bulkhead.name,
            operation = { joinPoint.proceed() }
        )
    }

    /**
     * Intercepta métodos anotados con @RateLimiter
     */
    @Around("@annotation(rateLimiter)")
    fun handleRateLimiter(joinPoint: ProceedingJoinPoint, rateLimiter: RateLimiter): Any? {
        val fallbackMethod = if (rateLimiter.fallbackMethod.isNotEmpty()) {
            findFallbackMethod(joinPoint, rateLimiter.fallbackMethod)
        } else null

        return resilienceService.executeWithRateLimit(
            name = rateLimiter.name,
            operation = { joinPoint.proceed() },
            fallback = fallbackMethod?.let { method ->
                { invokeFallbackMethod(joinPoint, method) }
            }
        )
    }

    /**
     * Intercepta métodos anotados con @TimeLimiter
     */
    @Around("@annotation(timeLimiter)")
    fun handleTimeLimiter(joinPoint: ProceedingJoinPoint, timeLimiter: TimeLimiter): Any? {
        val fallbackMethod = if (timeLimiter.fallbackMethod.isNotEmpty()) {
            findFallbackMethod(joinPoint, timeLimiter.fallbackMethod)
        } else null

        // Verificar si el método retorna CompletionStage
        val method = (joinPoint.signature as MethodSignature).method
        val isAsync = CompletionStage::class.java.isAssignableFrom(method.returnType)

        return if (isAsync) {
            resilienceService.executeWithTimeLimit(
                name = timeLimiter.name,
                operation = { joinPoint.proceed() as CompletionStage<*> },
                fallback = fallbackMethod?.let { fallback ->
                    { invokeFallbackMethod(joinPoint, fallback) }
                }
            )
        } else {
            // Para métodos síncronos, convertir a async temporalmente
            val future = CompletableFuture.supplyAsync { joinPoint.proceed() }
            resilienceService.executeWithTimeLimit(
                name = timeLimiter.name,
                operation = { future },
                fallback = fallbackMethod?.let { fallback ->
                    { invokeFallbackMethod(joinPoint, fallback) }
                }
            ).get()
        }
    }

    /**
     * Intercepta métodos anotados con @Resilient
     */
    @Around("@annotation(resilient)")
    fun handleResilient(joinPoint: ProceedingJoinPoint, resilient: Resilient): Any? {
        val config = ResilienceConfig(
            circuitBreakerName = resilient.circuitBreaker.takeIf { it.isNotEmpty() },
            retryName = resilient.retry.takeIf { it.isNotEmpty() },
            bulkheadName = resilient.bulkhead.takeIf { it.isNotEmpty() },
            rateLimiterName = resilient.rateLimiter.takeIf { it.isNotEmpty() },
            timeLimiterName = resilient.timeLimiter.takeIf { it.isNotEmpty() }
        )

        val fallbackMethod = if (resilient.fallbackMethod.isNotEmpty()) {
            findFallbackMethod(joinPoint, resilient.fallbackMethod)
        } else null

        // Verificar si es operación asíncrona
        val method = (joinPoint.signature as MethodSignature).method
        val isAsync = CompletionStage::class.java.isAssignableFrom(method.returnType)

        return if (isAsync) {
            resilienceService.executeAsyncWithResilience(
                config = config,
                operation = { joinPoint.proceed() as CompletionStage<*> },
                fallback = fallbackMethod?.let { fallback ->
                    { invokeFallbackMethod(joinPoint, fallback) }
                }
            )
        } else {
            resilienceService.executeWithResilience(
                config = config,
                operation = { joinPoint.proceed() },
                fallback = fallbackMethod?.let { fallback ->
                    { invokeFallbackMethod(joinPoint, fallback) }
                }
            )
        }
    }

    /**
     * Busca un método de fallback en la clase
     */
    private fun findFallbackMethod(joinPoint: ProceedingJoinPoint, fallbackMethodName: String): Method? {
        return try {
            val targetClass = joinPoint.target.javaClass
            val originalMethod = (joinPoint.signature as MethodSignature).method

            // Buscar método con la misma signature
            targetClass.getDeclaredMethod(fallbackMethodName, *originalMethod.parameterTypes)
        } catch (e: NoSuchMethodException) {
            try {
                // Buscar método con signature de fallback (parámetros + Exception)
                val targetClass = joinPoint.target.javaClass
                val originalMethod = (joinPoint.signature as MethodSignature).method
                val parameterTypes = originalMethod.parameterTypes + Exception::class.java

                targetClass.getDeclaredMethod(fallbackMethodName, *parameterTypes)
            } catch (e2: NoSuchMethodException) {
                logger.warn("Fallback method '$fallbackMethodName' not found in class ${joinPoint.target.javaClass.simpleName}")
                null
            }
        }
    }

    /**
     * Invoca un método de fallback
     */
    private fun invokeFallbackMethod(joinPoint: ProceedingJoinPoint, fallbackMethod: Method): Any? {
        return try {
            fallbackMethod.isAccessible = true

            // Determinar argumentos para el método de fallback
            val args = if (fallbackMethod.parameterCount == joinPoint.args.size + 1) {
                // Método de fallback espera Exception como último parámetro
                joinPoint.args + RuntimeException("Circuit breaker or resilience pattern triggered")
            } else {
                // Método de fallback tiene la misma signature que el original
                joinPoint.args
            }

            fallbackMethod.invoke(joinPoint.target, *args)
        } catch (e: Exception) {
            logger.error("Error invoking fallback method ${fallbackMethod.name}", e)

            // Como último recurso, usar fallback service
            val context = FallbackContext(
                operationName = joinPoint.signature.name,
                parameters = joinPoint.args.mapIndexed { index, arg ->
                    "param$index" to arg
                }.toMap(),
                lastException = e
            )

            fallbackService.getFallback<Any>(joinPoint.signature.name, context)
        }
    }
}