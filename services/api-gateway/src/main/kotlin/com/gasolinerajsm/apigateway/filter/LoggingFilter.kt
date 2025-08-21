package com.gasolinerajsm.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * Global filter for logging requests and responses
 */
@Component
class LoggingFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val correlationId = UUID.randomUUID().toString()

        // Add correlation ID to request headers
        val mutatedRequest = request.mutate()
            .header("X-Correlation-ID", correlationId)
            .build()

        val mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build()

        val startTime = System.currentTimeMillis()

        logger.info(
            "Gateway Request - ID: {}, Method: {}, URI: {}, Headers: {}, Timestamp: {}",
            correlationId,
            request.method,
            request.uri,
            request.headers.toSingleValueMap(),
            LocalDateTime.now()
        )

        return chain.filter(mutatedExchange)
            .doOnSuccess {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                logger.info(
                    "Gateway Response - ID: {}, Status: {}, Duration: {}ms, Timestamp: {}",
                    correlationId,
                    mutatedExchange.response.statusCode,
                    duration,
                    LocalDateTime.now()
                )
            }
            .doOnError { error ->
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                logger.error(
                    "Gateway Error - ID: {}, Error: {}, Duration: {}ms, Timestamp: {}",
                    correlationId,
                    error.message,
                    duration,
                    LocalDateTime.now(),
                    error
                )
            }
    }

    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE
    }
}