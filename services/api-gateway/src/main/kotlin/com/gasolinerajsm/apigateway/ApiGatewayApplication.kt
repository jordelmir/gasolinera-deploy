package com.gasolinerajsm.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean

@SpringBootApplication
class ApiGatewayApplication {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("auth-service") { r ->
                r.path("/api/auth/**")
                    .uri("http://localhost:8081")
            }
            .route("station-service") { r ->
                r.path("/api/stations/**")
                    .uri("http://localhost:8082")
            }
            .route("coupon-service") { r ->
                r.path("/api/coupons/**")
                    .uri("http://localhost:8083")
            }
            .route("raffle-service") { r ->
                r.path("/api/raffles/**")
                    .uri("http://localhost:8084")
            }
            .route("redemption-service") { r ->
                r.path("/api/redemptions/**")
                    .uri("http://localhost:8085")
            }
            .route("ad-engine") { r ->
                r.path("/api/ads/**")
                    .uri("http://localhost:8086")
            }
            .route("message-improver") { r ->
                r.path("/api/messages/**")
                    .uri("http://localhost:8087")
            }
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}