package com.gasolinerajsm.apigateway.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * Fallback controller for circuit breaker patterns
 */
@RestController
@RequestMapping("/fallback")
class FallbackController {

    private val logger = LoggerFactory.getLogger(FallbackController::class.java)

    /**
     * Fallback for auth service
     */
    @RequestMapping("/auth/**")
    fun authServiceFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Auth service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Authentication service is temporarily unavailable. Please try again later.",
            service = "auth-service",
            timestamp = LocalDateTime.now(),
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value(),
            fallbackData = mapOf(
                "authenticated" to false,
                "retryAfter" to 60
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Fallback for station service
     */
    @RequestMapping("/stations/**")
    fun stationServiceFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Station service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Station service is temporarily unavailable. Station data cannot be retrieved at this time.",
            service = "station-service",
            timestamp = LocalDateTime.now(),
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value(),
            fallbackData = mapOf(
                "stations" to emptyList<Any>(),
                "employees" to emptyList<Any>(),
                "availableStations" to 0
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Fallback for coupon service
     */
    @RequestMapping("/coupons/**")
    fun couponServiceFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Coupon service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Coupon service is temporarily unavailable. Coupon operations cannot be processed at this time.",
            service = "coupon-service",
            timestamp = LocalDateTime.now(),
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value(),
            fallbackData = mapOf(
                "coupons" to emptyList<Any>(),
                "campaigns" to emptyList<Any>(),
                "activeCoupons" to 0
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Fallback for redemption service
     */
    @RequestMapping("/redemptions/**")
    fun redemptionServiceFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Redemption service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Redemption service is temporarily unavailable. Redemption operations cannot be processed at this time.",
            service = "redemption-service",
            timestamp = LocalDateTime.now(),
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value(),
            fallbackData = mapOf(
                "redemptions" to emptyList<Any>(),
                "tickets" to emptyList<Any>(),
                "userBalance" to 0
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Fallback for ad engine service
     */
    @RequestMapping("/ads/**")
    fun adEngineFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Ad Engine service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Advertisement service is temporarily unavailable. Default content will be shown.",
            service = "ad-engine",
            timestamp = LocalDateTime.now(),
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value(),
            fallbackData = mapOf(
                "ads" to emptyList<Any>(),
                "engagements" to emptyList<Any>(),
                "defaultAd" to mapOf(
                    "title" to "Welcome to Gasolinera JSM",
                    "description" to "Your trusted fuel station",
                    "imageUrl" to "/images/default-ad.jpg"
                )
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Fallback for raffle service
     */
    @RequestMapping("/raffles/**")
    fun raffleServiceFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Raffle service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Raffle service is temporarily unavailable. Raffle operations cannot be processed at this time.",
            service = "raffle-service",
            timestamp = LocalDateTime.now(),
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value(),
            fallbackData = mapOf(
                "raffles" to emptyList<Any>(),
                "entries" to emptyList<Any>(),
                "prizes" to emptyList<Any>(),
                "winners" to emptyList<Any>(),
                "activeRaffles" to 0
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Generic fallback for any service
     */
    @RequestMapping("/generic/**")
    fun genericFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Service is unavailable, returning generic fallback response")

        val response = FallbackResponse(
            message = "Service is temporarily unavailable. Please try again later.",
            service = "unknown",
            timestamp = LocalDateTime.now(),
            statusCode = HttpStatus.SERVICE_UNAVAILABLE.value(),
            fallbackData = mapOf(
                "retryAfter" to 30,
                "supportContact" to "support@gasolinerajsm.com"
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Health check fallback
     */
    @GetMapping("/health")
    fun healthFallback(): ResponseEntity<Map<String, Any>> {
        logger.info("Health check fallback called")

        val response = mapOf(
            "status" to "DEGRADED",
            "timestamp" to LocalDateTime.now(),
            "message" to "Some services are unavailable",
            "gateway" to "UP"
        )

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response)
    }
}

/**
 * Fallback response data class
 */
data class FallbackResponse(
    val message: String,
    val service: String,
    val timestamp: LocalDateTime,
    val statusCode: Int,
    val fallbackData: Map<String, Any>,
    val error: String = "SERVICE_UNAVAILABLE",
    val retryable: Boolean = true
)