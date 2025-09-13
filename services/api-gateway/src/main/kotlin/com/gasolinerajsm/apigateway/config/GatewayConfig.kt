package com.gasolinerajsm.apigateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import java.net.URI
import java.time.Duration

/**
 * Spring Cloud Gateway configuration for routing requests to microservices
 */
@Configuration
class GatewayConfig {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            // Auth Service Routes - Public endpoints
            .route("auth-service-public") { r ->
                r.path("/api/auth/register", "/api/auth/login", "/api/auth/verify", "/api/auth/refresh")
                    .and().method(HttpMethod.POST)
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.addRequestHeader("X-Service", "auth-service")
                        f.requestRateLimiter { rl ->
                            rl.rateLimiter = "redisRateLimiter"
                            rl.keyResolver = "ipKeyResolver"
                        }
                    }
                    .uri("http://auth-service:8080")
            }

            // Auth Service Routes - Protected endpoints
            .route("auth-service-protected") { r ->
                r.path("/api/auth/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.addRequestHeader("X-Service", "auth-service")
                    }
                    .uri("http://auth-service:8080")
            }

            // Station Service Routes
            .route("station-service") { r ->
                r.path("/api/stations/**", "/api/employees/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.addRequestHeader("X-Service", "station-service")
                        f.circuitBreaker { cb ->
                            cb.name = "station-service-cb"
                            cb.fallbackUri = "forward:/fallback/stations"
                        }
                    }
                    .uri("http://station-service:8082")
            }

            // Coupon Service Routes
            .route("coupon-service") { r ->
                r.path("/api/coupons/**", "/api/campaigns/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.addRequestHeader("X-Service", "coupon-service")
                        f.circuitBreaker { cb ->
                            cb.name = "coupon-service-cb"
                            cb.fallbackUri = "forward:/fallback/coupons"
                        }
                        f.requestRateLimiter { rl ->
                            rl.rateLimiter = "redisRateLimiter"
                            rl.keyResolver = "userKeyResolver"
                        }
                    }
                    .uri("http://coupon-service:8081")
            }

            // Redemption Service Routes
            .route("redemption-service") { r ->
                r.path("/api/redemptions/**", "/api/tickets/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.addRequestHeader("X-Service", "redemption-service")
                        f.circuitBreaker { cb ->
                            cb.name = "redemption-service-cb"
                            cb.fallbackUri = "forward:/fallback/redemptions"
                        }
                        f.requestRateLimiter { rl ->
                            rl.rateLimiter = "redisRateLimiter"
                            rl.keyResolver = "userKeyResolver"
                        }
                    }
                    .uri("http://redemption-service:8083")
            }

            // Ad Engine Routes
            .route("ad-engine") { r ->
                r.path("/api/ads/**", "/api/engagements/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.addRequestHeader("X-Service", "ad-engine")
                        f.circuitBreaker { cb ->
                            cb.name = "ad-engine-cb"
                            cb.fallbackUri = "forward:/fallback/ads"
                        }
                        f.requestRateLimiter { rl ->
                            rl.rateLimiter = "redisRateLimiter"
                            rl.keyResolver = "userKeyResolver"
                        }
                    }
                    .uri("http://ad-engine:8084")
            }

            // Raffle Service Routes
            .route("raffle-service") { r ->
                r.path("/api/raffles/**", "/api/entries/**", "/api/prizes/**", "/api/winners/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.addRequestHeader("X-Service", "raffle-service")
                        f.circuitBreaker { cb ->
                            cb.name = "raffle-service-cb"
                            cb.fallbackUri = "forward:/fallback/raffles"
                        }
                        f.requestRateLimiter { rl ->
                            rl.rateLimiter = "redisRateLimiter"
                            rl.keyResolver = "userKeyResolver"
                        }
                    }
                    .uri("http://raffle-service:8085")
            }

            // Health Check Routes - Allow direct access without authentication
            .route("health-checks") { r ->
                r.path("/actuator/health", "/actuator/info", "/actuator/metrics")
                    .filters { f ->
                        f.addRequestHeader("X-Health-Check", "gateway")
                    }
                    .uri("http://localhost:8080")
            }

            // Service Health Checks - Route to individual services
            .route("auth-service-health") { r ->
                r.path("/health/auth")
                    .filters { f ->
                        f.rewritePath("/health/auth", "/actuator/health")
                    }
                    .uri("http://auth-service:8080")
            }

            .route("station-service-health") { r ->
                r.path("/health/stations")
                    .filters { f ->
                        f.rewritePath("/health/stations", "/actuator/health")
                    }
                    .uri("http://station-service:8082")
            }

            .route("coupon-service-health") { r ->
                r.path("/health/coupons")
                    .filters { f ->
                        f.rewritePath("/health/coupons", "/actuator/health")
                    }
                    .uri("http://coupon-service:8081")
            }

            .route("redemption-service-health") { r ->
                r.path("/health/redemptions")
                    .filters { f ->
                        f.rewritePath("/health/redemptions", "/actuator/health")
                    }
                    .uri("http://redemption-service:8083")
            }

            .route("ad-engine-health") { r ->
                r.path("/health/ads")
                    .filters { f ->
                        f.rewritePath("/health/ads", "/actuator/health")
                    }
                    .uri("http://ad-engine:8084")
            }

            .route("raffle-service-health") { r ->
                r.path("/health/raffles")
                    .filters { f ->
                        f.rewritePath("/health/raffles", "/actuator/health")
                    }
                    .uri("http://raffle-service:8085")
            }

            // API Documentation Routes
            .route("api-docs") { r ->
                r.path("/v3/api-docs/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                    }
                    .uri("http://localhost:8080")
            }

            .build()
    }
}