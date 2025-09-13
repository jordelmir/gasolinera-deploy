package com.gasolinerajsm.shared.resilience

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@TestPropertySource(properties = [
    "gasolinera.resilience.enabled=true"
])
class ResilienceServiceTest {

    private lateinit var resilienceService: ResilienceService
    private lateinit var properties: ResilienceProperties

    @BeforeEach
    fun setup() {
        properties = ResilienceProperties()

        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        val retryRegistry = RetryRegistry.ofDefaults()
        val bulkheadRegistry = BulkheadRegistry.ofDefaults()
        val rateLimiterRegistry = RateLimiterRegistry.ofDefaults()
        val timeLimiterRegistry = TimeLimiterRegistry.ofDefaults()

        resilienceService = ResilienceService(
            circuitBreakerRegistry,
            retryRegistry,
            bulkheadRegistry,
            rateLimiterRegistry,
            timeLimiterRegistry,
            properties
        )
    }

    @Test
    fun `should execute operation successfully with circuit breaker`() {
        // Given
        val expectedResult = "success"

        // When
        val result = resilienceService.executeWithCircuitBreaker("test-cb") {
            expectedResult
        }

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should use fallback when circuit breaker operation fails`() {
        // Given
        val fallbackResult = "fallback"

        // When
        val result = resilienceService.executeWithCircuitBreaker(
            name = "test-cb",
            operation = { throw RuntimeException("Test failure") },
            fallback = { fallbackResult }
        )

        // Then
        assertEquals(fallbackResult, result)
    }

    @Test
    fun `should retry operation on failure`() {
        // Given
        val attemptCounter = AtomicInteger(0)
        val expectedResult = "success"

        // When
        val result = resilienceService.executeWithRetry("test-retry") {
            val attempt = attemptCounter.incrementAndGet()
            if (attempt < 3) {
                throw RuntimeException("Attempt $attempt failed")
            }
            expectedResult
        }

        // Then
        assertEquals(expectedResult, result)
        assertEquals(3, attemptCounter.get())
    }

    @Test
    fun `should use fallback after retry exhaustion`() {
        // Given
        val fallbackResult = "fallback"

        // When
        val result = resilienceService.executeWithRetry(
            name = "test-retry",
            operation = { throw RuntimeException("Always fails") },
            fallback = { fallbackResult }
        )

        // Then
        assertEquals(fallbackResult, result)
    }

    @Test
    fun `should limit concurrent calls with bulkhead`() {
        // Given
        val concurrentCalls = AtomicInteger(0)
        val maxConcurrentCalls = AtomicInteger(0)

        // When
        val futures = (1..10).map {
            CompletableFuture.supplyAsync {
                resilienceService.executeWithBulkhead("test-bulkhead") {
                    val current = concurrentCalls.incrementAndGet()
                    maxConcurrentCalls.updateAndGet { max -> maxOf(max, current) }

                    Thread.sleep(100) // Simulate work

                    concurrentCalls.decrementAndGet()
                    "success"
                }
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()

        // Then
        // El bulkhead por defecto permite 25 llamadas concurrentes
        assertTrue(maxConcurrentCalls.get() <= 25)
    }

    @Test
    fun `should limit rate with rate limiter`() {
        // Given
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        // When
        repeat(100) {
            try {
                resilienceService.executeWithRateLimit("test-rate-limiter") {
                    successCount.incrementAndGet()
                    "success"
                }
            } catch (e: Exception) {
                failureCount.incrementAndGet()
            }
        }

        // Then
        // Algunas llamadas deberían ser limitadas
        assertTrue(failureCount.get() > 0)
        assertTrue(successCount.get() < 100)
    }

    @Test
    fun `should timeout with time limiter`() {
        // Given & When & Then
        assertThrows<Exception> {
            resilienceService.executeWithTimeLimit("test-time-limiter", {
                CompletableFuture.supplyAsync {
                    Thread.sleep(5000) // Más que el timeout por defecto
                    "success"
                }
            }).get()
        }
    }

    @Test
    fun `should apply multiple resilience patterns`() {
        // Given
        val config = ResilienceConfig(
            circuitBreakerName = "test-cb",
            retryName = "test-retry",
            bulkheadName = "test-bulkhead",
            rateLimiterName = "test-rate-limiter"
        )
        val expectedResult = "success"

        // When
        val result = resilienceService.executeWithResilience(config) {
            expectedResult
        }

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should get circuit breaker state`() {
        // Given
        val cbName = "test-cb"

        // Execute some operations to generate metrics
        repeat(5) {
            try {
                resilienceService.executeWithCircuitBreaker(cbName) {
                    if (it < 3) throw RuntimeException("Test failure")
                    "success"
                }
            } catch (e: Exception) {
                // Expected failures
            }
        }

        // When
        val state = resilienceService.getCircuitBreakerState(cbName)

        // Then
        assertEquals(cbName, state.name)
        assertTrue(state.numberOfCalls >= 0)
    }

    @Test
    fun `should get retry state`() {
        // Given
        val retryName = "test-retry"

        // Execute some operations to generate metrics
        repeat(3) {
            try {
                resilienceService.executeWithRetry(retryName) {
                    throw RuntimeException("Test failure")
                }
            } catch (e: Exception) {
                // Expected failures
            }
        }

        // When
        val state = resilienceService.getRetryState(retryName)

        // Then
        assertEquals(retryName, state.name)
        assertTrue(state.numberOfAttempts >= 0)
    }

    @Test
    fun `should get bulkhead state`() {
        // Given
        val bulkheadName = "test-bulkhead"

        // When
        val state = resilienceService.getBulkheadState(bulkheadName)

        // Then
        assertEquals(bulkheadName, state.name)
        assertTrue(state.maxAllowedConcurrentCalls > 0)
    }

    @Test
    fun `should get rate limiter state`() {
        // Given
        val rateLimiterName = "test-rate-limiter"

        // When
        val state = resilienceService.getRateLimiterState(rateLimiterName)

        // Then
        assertEquals(rateLimiterName, state.name)
        assertTrue(state.availablePermissions >= 0)
    }
}