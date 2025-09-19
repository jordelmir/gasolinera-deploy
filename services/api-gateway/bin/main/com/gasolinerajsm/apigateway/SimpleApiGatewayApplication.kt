package com.gasolinerajsm.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
class SimpleApiGatewayApplication {

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf("status" to "UP", "service" to "api-gateway")
    }

    @GetMapping("/actuator/health")
    fun actuatorHealth(): Map<String, String> {
        return mapOf("status" to "UP")
    }

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("auth-service") { r ->
                r.path("/api/auth/**")
                    .uri("http://localhost:8091")
            }
            .route("station-service") { r ->
                r.path("/api/stations/**")
                    .uri("http://localhost:8092")
            }
            .route("coupon-service") { r ->
                r.path("/api/coupons/**")
                    .uri("http://localhost:8093")
            }
            .route("raffle-service") { r ->
                r.path("/api/raffles/**")
                    .uri("http://localhost:8094")
            }
            .route("redemption-service") { r ->
                r.path("/api/redemptions/**")
                    .uri("http://localhost:8095")
            }
            .route("ad-engine") { r ->
                r.path("/api/ads/**")
                    .uri("http://localhost:8096")
            }
            .route("message-improver") { r ->
                r.path("/api/messages/**")
                    .uri("http://localhost:8097")
            }
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<SimpleApiGatewayApplication>(*args)
}