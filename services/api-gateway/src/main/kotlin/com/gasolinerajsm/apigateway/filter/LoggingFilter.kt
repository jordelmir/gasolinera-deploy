package com.gasolinerajsm.apigateway.filter

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * Global filter for logging requests and responses with audit trail
 */
@Component
class LoggingFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(LoggingFilter::class.java)
    private val auditLogger = LoggerFactory.getLogger("AUDIT")

    companion object {
        private val SENSITIVE_HEADERS = setOf(
            "authorization", "x-api-key", "cookie", "set-cookie"
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val correlationId = UUID.randomUUID().toString()
        val requestId = "REQ-${System.currentTimeMillis()}-${correlationId.substring(0, 8)}"

        // Add correlation and request IDs to headers
        val mutatedRequest = request.mutate()
            .header("X-Correlation-ID", correlationId)
            .header("X-Request-ID", requestId)
            .header("X-Gateway-Timestamp", LocalDateTime.now().toString())
            .build()

        val mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build()

        val startTime = System.currentTimeMillis()
        val clientIp = getClientIp(request)
        val userAgent = request.headers.getFirst("User-Agent") ?: "Unknown"
        val userId = request.headers.getFirst("X-User-ID") ?: "anonymous"

        // Set MDC for structured logging
        MDC.put("correlationId", correlationId)
        MDC.put("requestId", requestId)
        MDC.put("userId", userId)
        MDC.put("clientIp", clientIp)

        // Log request details
        logger.info(
            "Gateway Request - ID: {}, Method: {}, URI: {}, ClientIP: {}, UserAgent: {}, UserId: {}, Timestamp: {}",
            requestId,
            request.method,
            request.uri,
            clientIp,
            userAgent,
            userId,
            LocalDateTime.now()
        )

        // Log sensitive operations for audit
        if (isSensitiveOperation(request.path.value(), request.method.name())) {
            auditLogger.info(
                "SENSITIVE_OPERATION - RequestID: {}, UserId: {}, Method: {}, Path: {}, ClientIP: {}, Timestamp: {}",
                requestId,
                userId,
                request.method,
                request.path.value(),
                clientIp,
                LocalDateTime.now()
            )
        }

        return chain.filter(mutatedExchange)
            .doOnSuccess {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                val statusCode = mutatedExchange.response.statusCode

                logger.info(
                    "Gateway Response - ID: {}, Status: {}, Duration: {}ms, UserId: {}, Timestamp: {}",
                    requestId,
                    statusCode,
                    duration,
                    userId,
                    LocalDateTime.now()
                )

                // Log audit trail for completed requests
                auditLogger.info(
                    "REQUEST_COMPLETED - RequestID: {}, UserId: {}, Method: {}, Path: {}, Status: {}, Duration: {}ms, ClientIP: {}",
                    requestId,
                    userId,
                    request.method,
                    request.path.value(),
                    statusCode,
                    duration,
                    clientIp
                )
            }
            .doOnError { error ->
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                logger.error(
                    "Gateway Error - ID: {}, Error: {}, Duration: {}ms, UserId: {}, Timestamp: {}",
                    requestId,
                    error.message,
                    duration,
                    userId,
                    LocalDateTime.now(),
                    error
                )

                // Log audit trail for failed requests
                auditLogger.error(
                    "REQUEST_FAILED - RequestID: {}, UserId: {}, Method: {}, Path: {}, Error: {}, Duration: {}ms, ClientIP: {}",
                    requestId,
                    userId,
                    request.method,
                    request.path.value(),
                    error.message,
                    duration,
                    clientIp
                )
            }
            .doFinally {
                // Clear MDC
                MDC.clear()
            }
    }

    /**
     * Extract client IP address from request
     */
    private fun getClientIp(request: org.springframework.http.server.reactive.ServerHttpRequest): String {
        return request.headers.getFirst("X-Forwarded-For")?.split(",")?.first()?.trim()
            ?: request.headers.getFirst("X-Real-IP")
            ?: request.headers.getFirst("X-Client-IP")
            ?: request.remoteAddress?.address?.hostAddress
            ?: "unknown"
    }

    /**
     * Filter sensitive headers for logging
     */
    private fun filterSensitiveHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            if (SENSITIVE_HEADERS.contains(key.lowercase())) {
                "***REDACTED***"
            } else {
                value
            }
        }
    }

    /**
     * Check if the operation is sensitive and requires audit logging
     */
    private fun isSensitiveOperation(path: String, method: String): Boolean {
        val sensitivePatterns = listOf(
            "/api/auth/login",
            "/api/auth/register",
            "/api/redemptions",
            "/api/coupons/redeem",
            "/api/raffles/admin",
            "/api/stations/admin",
            "/api/campaigns"
        )

        val sensitiveMethods = listOf("POST", "PUT", "DELETE")

        return sensitivePatterns.any { pattern -> path.contains(pattern) } ||
               (sensitiveMethods.contains(method) && !path.contains("/health"))
    }

    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE
    }
}