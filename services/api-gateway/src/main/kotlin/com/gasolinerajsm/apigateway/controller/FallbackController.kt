package com.gasolinerajsm.apigateway.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Fallback controller for circuit breaker patterns
 */
@RestController
@RequestMapping("/fallback")
class FallbackController {

    private val logger = LoggerFactory.getLogger(FallbackController::class.java)

    /**
     * Fallback for ad engine service
     */
    @GetMapping("/ads")
    fun adEngineFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Ad Engine service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Ad Engine service is temporarily unavailable",
            service = "ad-engine",
            timestamp = LocalDateTime.now(),
            fallbackData = mapOf(
                "ads" to emptyList<Any>(),
                "campaigns" to emptyList<Any>()
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Fallback for raffle service
     */
    @GetMapping("/raffles")
    fun raffleServiceFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Raffle service is unavailable, returning fallback response")

        val response = FallbackResponse(
            message = "Raffle service is temporarily unavailable",
            service = "raffle-service",
            timestamp = LocalDateTime.now(),
            fallbackData = mapOf(
                "raffles" to emptyList<Any>(),
                "activeRaffles" to 0
            )
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    /**
     * Generic fallback for any service
     */
    @GetMapping("/generic")
    fun genericFallback(): ResponseEntity<FallbackResponse> {
        logger.warn("Service is unavailable, returning generic fallback response")

        val response = FallbackResponse(
            message = "Service is temporarily unavailable",
            service = "unknown",
            timestamp = LocalDateTime.now(),
            fallbackData = emptyMap()
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }
}

/**
 * Fallback response data class
 */
data class FallbackResponse(
    val message: String,
    val service: String,
    val timestamp: LocalDateTime,
    val fallbackData: Map<String, Any>
)