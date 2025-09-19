package com.gasolinerajsm.adengine.controller

import com.gasolinerajsm.adengine.dto.*
import com.gasolinerajsm.adengine.model.*
import com.gasolinerajsm.adengine.service.*
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Simplified REST Controller for advertisement serving endpoints for testing
 */
@RestController
@RequestMapping("/api/v1/ads")
@CrossOrigin(origins = ["*"])
class AdController(
    private val adService: AdService,
    private val engagementTrackingService: EngagementTrackingService,
    private val ticketMultiplicationService: TicketMultiplicationService
) {
    private val logger = LoggerFactory.getLogger(AdController::class.java)

    /**
     * Serve an advertisement to a user
     */
    @PostMapping("/serve")
    fun serveAdvertisement(
        @Valid @RequestBody request: AdServingRequest
    ): ResponseEntity<AdServingResponse> {
        logger.info("Serving advertisement to user ${request.userId}")

        return try {
            val advertisement = adService.serveAdvertisement(
                userId = request.userId,
                sessionId = request.sessionId ?: "default-session",
                userAge = request.userAge,
                userGender = request.userGender,
                userLocation = request.userLocation,
                userSegments = request.userSegments,
                stationId = request.stationId,
                adType = request.adType,
                placementContext = request.placementContext
            )

            if (advertisement != null) {
                ResponseEntity.ok(
                    AdServingResponse(
                        success = true,
                        advertisement = advertisement.toDto(),
                        engagementId = 1L,
                        eligibleCount = 1
                    )
                )
            } else {
                ResponseEntity.ok(
                    AdServingResponse(
                        success = false,
                        message = "No eligible advertisements found",
                        eligibleCount = 0
                    )
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to serve advertisement to user ${request.userId}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AdServingResponse(
                    success = false,
                    message = "Failed to serve advertisement: ${e.message}"
                )
            )
        }
    }

    @GetMapping("/eligible")
    fun getEligibleAdvertisements(
        @RequestParam userId: Long,
        @RequestParam(required = false) limit: Int = 10
    ): ResponseEntity<List<AdvertisementDto>> {
        val advertisements = adService.getEligibleAdvertisements(
            userId = userId,
            userAge = null,
            userGender = null,
            userLocation = null,
            userSegments = emptyList(),
            stationId = null,
            adType = null,
            limit = limit
        )
        val advertisementDtos = advertisements.map { advertisement -> advertisement.toDto() }
        return ResponseEntity.ok(advertisementDtos)
    }
}
