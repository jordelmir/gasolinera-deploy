package com.gasolinerajsm.apigateway.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Circuit breaker configuration for API Gateway
 */
@Configuration
class CircuitBreakerConfig {

    /**
     * Default circuit breaker configuration
     */
    @Bean
    fun defaultCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configureDefault { id ->
                Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(10)
                            .minimumNumberOfCalls(5)
                            .failureRateThreshold(50.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(50.0f)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(10))
                            .build()
                    )
                    .build()
            }
        }
    }

    /**
     * Auth service specific circuit breaker
     */
    @Bean
    fun authServiceCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configure({ builder ->
                builder
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(20)
                            .minimumNumberOfCalls(10)
                            .failureRateThreshold(60.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(60))
                            .slowCallRateThreshold(60.0f)
                            .slowCallDurationThreshold(Duration.ofSeconds(3))
                            .permittedNumberOfCallsInHalfOpenState(5)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(15))
                            .build()
                    )
            }, "auth-service-cb")
        }
    }

    /**
     * Station service specific circuit breaker
     */
    @Bean
    fun stationServiceCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configure({ builder ->
                builder
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .minimumNumberOfCalls(8)
                            .failureRateThreshold(50.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(45))
                            .slowCallRateThreshold(50.0f)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(4)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(8))
                            .build()
                    )
            }, "station-service-cb")
        }
    }

    /**
     * Coupon service specific circuit breaker
     */
    @Bean
    fun couponServiceCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configure({ builder ->
                builder
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(12)
                            .minimumNumberOfCalls(6)
                            .failureRateThreshold(45.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallRateThreshold(45.0f)
                            .slowCallDurationThreshold(Duration.ofSeconds(1.5))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(6))
                            .build()
                    )
            }, "coupon-service-cb")
        }
    }

    /**
     * Redemption service specific circuit breaker
     */
    @Bean
    fun redemptionServiceCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configure({ builder ->
                builder
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(15)
                            .minimumNumberOfCalls(8)
                            .failureRateThreshold(40.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(45))
                            .slowCallRateThreshold(40.0f)
                            .slowCallDurationThreshold(Duration.ofSeconds(2))
                            .permittedNumberOfCallsInHalfOpenState(4)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(12))
                            .build()
                    )
            }, "redemption-service-cb")
        }
    }

    /**
     * Ad engine specific circuit breaker (more lenient for non-critical service)
     */
    @Bean
    fun adEngineCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configure({ builder ->
                builder
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(10)
                            .minimumNumberOfCalls(5)
                            .failureRateThreshold(70.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(20))
                            .slowCallRateThreshold(70.0f)
                            .slowCallDurationThreshold(Duration.ofSeconds(3))
                            .permittedNumberOfCallsInHalfOpenState(2)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()
                    )
            }, "ad-engine-cb")
        }
    }

    /**
     * Raffle service specific circuit breaker
     */
    @Bean
    fun raffleServiceCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configure({ builder ->
                builder
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(12)
                            .minimumNumberOfCalls(6)
                            .failureRateThreshold(50.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(40))
                            .slowCallRateThreshold(50.0f)
                            .slowCallDurationThreshold(Duration.ofSeconds(2.5))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(10))
                            .build()
                    )
            }, "raffle-service-cb")
        }
    }
}