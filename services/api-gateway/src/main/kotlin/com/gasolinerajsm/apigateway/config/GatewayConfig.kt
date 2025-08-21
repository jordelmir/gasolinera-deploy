package com.gasolinerajsm.apigateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import java.net.URI

/**
 * Spring Cloud Gateway configuration for routing requests to microservices
 */
@Configuration
class GatewayConfig {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            // Auth Service Routes
            .route("auth-service") { r ->
                r.path("/auth/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                    }
                    .uri(URI.create("http://auth-service:8081"))
            }

            // Coupon Service Routes
            .route("coupon-service") { r ->
                r.path("/coupons/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                    }
                    .uri(URI.create("http://coupon-service:8084"))
            }

            // Station Service Routes
            .route("station-service") { r ->
                r.path("/api/v1/stations/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                    }
                    .uri(URI.create("http://station-service:8083"))
            }

            // Ad Engine Routes (when available)
            .route("ad-engine") { r ->
                r.path("/ads/**", "/campaigns/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                        f.circuitBreaker { cb ->
                            cb.name = "ad-engine-cb"
                            cb.fallbackUri = URI.create("forward:/fallback/ads")
                        }
                    }
                    .uri(URI.create("http://ad-engine:8082"))
            }

            // Raffle Service Routes (when available)
            .route("raffle-service") { r ->
                r.path("/raffles/**")
                    .filters { f ->
                        f.addRequestHeader("X-Gateway", "api-gateway")
                    }
                    .uri(URI.create("http://raffle-service:8085"))
            }

            // Health Check Routes (allow direct access)
            .route("health-checks") { r ->
                r.path("/actuator/health")
                    .filters { f ->
                        f.addRequestHeader("X-Health-Check", "gateway")
                    }
                    .uri(URI.create("http://localhost:8080"))
            }

            .build()
    }
}