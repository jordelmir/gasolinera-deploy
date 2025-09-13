package com.gasolinerajsm.shared.resilience

import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * Servicio de ejemplo que demuestra el uso de anotaciones de resilience
 */
@Service
class ResilienceAnnotationExampleService {

    /**
     * Ejemplo de uso de Circuit Breaker con fallback
     */
    @CircuitBreaker(name = "database", fallbackMethod = "getDatabaseDataFallback")
    fun getDatabaseData(id: String): String {
        // Simular operación que puede fallar
        if (id == "fail") {
            throw RuntimeException("Database connection failed")
        }
        return "Database data for $id"
    }

    /**
     * Método de fallback para getDatabaseData
     */
    fun getDatabaseDataFallback(id: String, ex: Exception): String {
        return "Fallback data for $id (reason: ${ex.message})"
    }

    /**
     * Ejemplo de uso de Retry con fallback
     */
    @Retry(name = "external-service", fallbackMethod = "getExternalDataFallback")
    fun getExternalData(id: String): String {
        // Simular operación que falla las primeras veces
        val random = Math.random()
        if (random < 0.7) { // 70% de probabilidad de fallo
            throw RuntimeException("External service temporarily unavailable")
        }
        return "External data for $id"
    }

    /**
     * Método de fallback para getExternalData
     */
    fun getExternalDataFallback(id: String, ex: Exception): String {
        return "Cached data for $id (service unavailable)"
    }

    /**
     * Ejemplo de uso de Bulkhead
     */
    @Bulkhead(name = "payment-gateway")
    fun processPayment(amount: Double, cardNumber: String): String {
        // Simular procesamiento de pago que toma tiempo
        Thread.sleep(1000)
        return "Payment of $amount processed successfully"
    }

    /**
     * Ejemplo de uso de Rate Limiter con fallback
     */
    @RateLimiter(name = "api-calls", fallbackMethod = "getApiDataFallback")
    fun getApiData(endpoint: String): String {
        return "API data from $endpoint"
    }

    /**
     * Método de fallback para getApiData
     */
    fun getApiDataFallback(endpoint: String, ex: Exception): String {
        return "Rate limited - please try again later"
    }

    /**
     * Ejemplo de uso de Time Limiter para operaciones asíncronas
     */
    @TimeLimiter(name = "business-operation", fallbackMethod = "getLongRunningOperationFallback")
    fun getLongRunningOperationAsync(duration: Long): CompletionStage<String> {
        return CompletableFuture.supplyAsync {
            Thread.sleep(duration)
            "Long running operation completed in ${duration}ms"
        }
    }

    /**
     * Método de fallback para getLongRunningOperationAsync
     */
    fun getLongRunningOperationFallback(duration: Long, ex: Exception): String {
        return "Operation timed out after ${duration}ms"
    }

    /**
     * Ejemplo de uso de múltiples patrones de resilience
     */
    @Resilient(
        circuitBreaker = "database",
        retry = "database",
        bulkhead = "database",
        rateLimiter = "database-calls",
        fallbackMethod = "getCriticalDataFallback"
    )
    fun getCriticalData(id: String): String {
        // Simular operación crítica que necesita múltiples patrones de resilience
        if (id == "critical-fail") {
            throw RuntimeException("Critical system failure")
        }
        return "Critical data for $id"
    }

    /**
     * Método de fallback para getCriticalData
     */
    fun getCriticalDataFallback(id: String, ex: Exception): String {
        return "Emergency fallback data for $id"
    }

    /**
     * Ejemplo de operación sin resilience para comparación
     */
    fun getDataWithoutResilience(id: String): String {
        // Esta operación no tiene protección de resilience
        if (id == "fail") {
            throw RuntimeException("Operation failed without resilience")
        }
        return "Data for $id"
    }

    /**
     * Ejemplo de operación con Circuit Breaker específico para Redis
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "getCacheDataFallback")
    fun getCacheData(key: String): String {
        // Simular operación de cache que puede fallar
        if (key == "redis-fail") {
            throw RuntimeException("Redis connection timeout")
        }
        return "Cached data for $key"
    }

    /**
     * Método de fallback para getCacheData
     */
    fun getCacheDataFallback(key: String, ex: Exception): String {
        return "Data retrieved from secondary cache for $key"
    }

    /**
     * Ejemplo de operación con Retry específico para operaciones de negocio
     */
    @Retry(name = "business-operation", fallbackMethod = "getBusinessDataFallback")
    fun getBusinessData(operation: String): String {
        // Simular operación de negocio que puede fallar temporalmente
        val random = Math.random()
        if (random < 0.5) { // 50% de probabilidad de fallo
            throw RuntimeException("Business operation temporarily failed")
        }
        return "Business result for $operation"
    }

    /**
     * Método de fallback para getBusinessData
     */
    fun getBusinessDataFallback(operation: String, ex: Exception): String {
        return "Default business result for $operation"
    }

    /**
     * Ejemplo de operación con Rate Limiter específico para llamadas a gateway de pago
     */
    @RateLimiter(name = "payment-gateway-calls", fallbackMethod = "getPaymentStatusFallback")
    fun getPaymentStatus(transactionId: String): String {
        return "Payment status for transaction $transactionId: COMPLETED"
    }

    /**
     * Método de fallback para getPaymentStatus
     */
    fun getPaymentStatusFallback(transactionId: String, ex: Exception): String {
        return "Payment status for transaction $transactionId: PENDING (rate limited)"
    }

    /**
     * Ejemplo de método que usa fallback personalizado
     */
    @Fallback("customFallback")
    @CircuitBreaker(name = "external-service")
    fun getDataWithCustomFallback(id: String): String {
        throw RuntimeException("Always fails for demonstration")
    }

    /**
     * Fallback personalizado
     */
    fun customFallback(id: String, ex: Exception): String {
        return "Custom fallback response for $id: ${ex.message}"
    }
}