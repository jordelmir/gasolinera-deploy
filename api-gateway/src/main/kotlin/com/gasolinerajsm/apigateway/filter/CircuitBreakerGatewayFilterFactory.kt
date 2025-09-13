package com.gasolinerajsm.apigateway.filter

import com.gasolinerajsm.shared.resilience.ResilienceService
import com.gasolinerajsm.shared.resilience.ResilienceConfig
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Factory para aplicar circuit breakers en rutas del Gateway
 */
@Component
class CircuitBreakerGatewayFilterFactory(
    private val resilienceService: ResilienceService
) : AbstractGatewayFilterFactory<CircuitBreakerGatewayFilterFactory.Config>() {

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val resilienceConfig = ResilienceConfig(
                circuitBreakerName = config.circuitBreakerName,
                retryName = config.retryName,
                rateLimiterName = config.rateLimiterName,
                timeLimiterName = config.timeLimiterName
            )

            Mono.fromCallable {
                resilienceService.executeAsyncWithResilience(resilienceConfig, {
                    chain.filter(exchange).toFuture()
                }, {
                    // Fallback response
                    val response = exchange.response
                    response.statusCode = HttpStatus.SERVICE_UNAVAILABLE
                    response.headers.add("X-Fallback-Reason", "Service temporarily unavailable")
                    response.setComplete().toFuture().get()
                })
            }.flatMap { future ->
                Mono.fromFuture(future)
            }.onErrorResume { error ->
                val response = exchange.response
                response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                response.headers.add("X-Error-Reason", error.message ?: "Unknown error")
                response.setComplete()
            }
        }
    }

    override fun getConfigClass(): Class<Config> = Config::class.java

    class Config {
        var circuitBreakerName: String = "default"
        var retryName: String? = null
        var rateLimiterName: String? = null
        var timeLimiterName: String? = null
        var fallbackUri: String? = null
        var timeout: Duration = Duration.ofSeconds(10)
    }
}