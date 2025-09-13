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
            // Auth Service Routes
            .route("auth-service") { r ->
                r.path("/api/auth/**")
                    .uri("http://auth-service:8081")
            }
            // Station Service Routes
            .route("station-service") { r ->
                r.path("/api/stations/**")
                    .uri("http://station-service:8082")
            }
            // Coupon Service Routes
            .route("coupon-service") { r ->
                r.path("/api/coupons/**", "/api/campaigns/**")
                    .uri("http://coupon-service:8083")
            }
            // Redemption Service Routes
            .route("redemption-service") { r ->
                r.path("/api/redemptions/**", "/api/raffle-tickets/**")
                    .uri("http://redemption-service:8084")
            }
            // Ad Engine Routes
            .route("ad-engine") { r ->
                r.path("/api/ads/**", "/api/engagements/**")
                    .uri("http://ad-engine:8085")
            }
            // Raffle Service Routes
            .route("raffle-service") { r ->
                r.path("/api/raffles/**")
                    .uri("http://raffle-service:8086")
            }
            // Health and Monitoring Routes
            .route("health-aggregation") { r ->
                r.path("/api/health/**")
                    .uri("http://health-service:8090")
            }
            .route("metrics-aggregation") { r ->
                r.path("/api/metrics/**")
                    .uri("http://monitoring-service:8091")
            }
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}