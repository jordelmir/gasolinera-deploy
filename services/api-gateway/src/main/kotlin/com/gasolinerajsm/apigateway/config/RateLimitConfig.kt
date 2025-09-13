package com.gasolinerajsm.apigateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Mono

/**
 * Rate limiting configuration for API Gateway
 */
@Configuration
class RateLimitConfig {

    /**
     * Redis-based rate limiter with default configuration
     */
    @Bean
    @Primary
    fun redisRateLimiter(): RedisRateLimiter {
        return RedisRateLimiter(
            10, // replenishRate: tokens per second
            20, // burstCapacity: maximum tokens in bucket
            1   // requestedTokens: tokens per request
        )
    }

    /**
     * Strict rate limiter for sensitive operations
     */
    @Bean
    fun strictRateLimiter(): RedisRateLimiter {
        return RedisRateLimiter(
            5,  // replenishRate: 5 tokens per second
            10, // burstCapacity: maximum 10 tokens
            1   // requestedTokens: 1 token per request
        )
    }

    /**
     * Lenient rate limiter for read operations
     */
    @Bean
    fun lenientRateLimiter(): RedisRateLimiter {
        return RedisRateLimiter(
            50,  // replenishRate: 50 tokens per second
            100, // burstCapacity: maximum 100 tokens
            1    // requestedTokens: 1 token per request
        )
    }

    /**
     * Key resolver based on IP address
     */
    @Bean
    fun ipKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val clientIp = exchange.request.remoteAddress?.address?.hostAddress
                ?: exchange.request.headers.getFirst("X-Forwarded-For")
                ?: exchange.request.headers.getFirst("X-Real-IP")
                ?: "unknown"

            Mono.just("ip:$clientIp")
        }
    }

    /**
     * Key resolver based on user ID from JWT token
     */
    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val userId = exchange.request.headers.getFirst("X-User-ID")
                ?: exchange.request.headers.getFirst("Authorization")?.let { auth ->
                    // Extract user ID from JWT token if needed
                    // This is a simplified version - in production, you'd decode the JWT
                    if (auth.startsWith("Bearer ")) {
                        "user:${auth.substring(7).take(10)}" // Use first 10 chars as identifier
                    } else {
                        "anonymous"
                    }
                }
                ?: "anonymous"

            Mono.just("user:$userId")
        }
    }

    /**
     * Key resolver based on API key
     */
    @Bean
    fun apiKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val apiKey = exchange.request.headers.getFirst("X-API-Key")
                ?: exchange.request.queryParams.getFirst("api_key")
                ?: "no-key"

            Mono.just("api:$apiKey")
        }
    }

    /**
     * Key resolver based on service name
     */
    @Bean
    fun serviceKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val serviceName = exchange.request.headers.getFirst("X-Service")
                ?: exchange.request.path.value().split("/").getOrNull(2)
                ?: "unknown-service"

            Mono.just("service:$serviceName")
        }
    }

    /**
     * Composite key resolver that combines user and IP
     */
    @Bean
    fun compositeKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val userId = exchange.request.headers.getFirst("X-User-ID") ?: "anonymous"
            val clientIp = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"

            Mono.just("composite:$userId:$clientIp")
        }
    }
}