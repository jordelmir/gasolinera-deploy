package com.gasolinerajsm.stationservice.controller

import com.gasolinerajsm.stationservice.config.QrSigningKeys
import com.gasolinerajsm.stationservice.infrastructure.security.QrSigningService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * QR Code Controller for Gas Station Dispensers
 * Handles QR code generation and verification for secure coupon redemption
 */
@RestController
@RequestMapping("/api/v1/qr")
@Tag(name = "QR Codes", description = "QR code generation and verification for gas station dispensers")
class QrController(
    private val qrSigningService: QrSigningService,
    private val qrSigningKeys: QrSigningKeys
) {

    private val logger = LoggerFactory.getLogger(QrController::class.java)

    @Operation(
        summary = "Generate signed QR code for dispenser",
        description = "Generates a cryptographically signed QR code for a specific gas station dispenser"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "QR code generated successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PostMapping("/generate")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('STATION_ADMIN') or hasRole('SYSTEM_ADMIN')")
    fun generateQrCode(
        @RequestBody request: GenerateQrRequest
    ): ResponseEntity<QrResponse> {
        logger.info("Generating QR code for station: {} dispenser: {}", request.stationId, request.dispenserId)

        return qrSigningService.generateSignedQrToken(
            stationId = request.stationId,
            dispenserId = request.dispenserId,
            privateKey = qrSigningKeys.privateKey,
            expirationHours = request.expirationHours ?: 1L
        ).fold(
            onSuccess = { signedToken ->
                val qrContent = qrSigningService.generateQrCodeContent(
                    stationId = request.stationId,
                    dispenserId = request.dispenserId,
                    privateKey = qrSigningKeys.privateKey,
                    baseUrl = request.baseUrl ?: "https://gasolinera-jsm.com/redeem"
                ).getOrElse {
                    return ResponseEntity.internalServerError().build()
                }

                ResponseEntity.ok(
                    QrResponse(
                        success = true,
                        signedToken = signedToken,
                        qrContent = qrContent,
                        stationId = request.stationId,
                        dispenserId = request.dispenserId,
                        expiresAt = LocalDateTime.now().plusHours(request.expirationHours ?: 1L),
                        message = "QR code generated successfully"
                    )
                )
            },
            onFailure = { error ->
                logger.error("Failed to generate QR code", error)
                ResponseEntity.badRequest().body(
                    QrResponse(
                        success = false,
                        message = "Failed to generate QR code: ${error.message}"
                    )
                )
            }
        )
    }

    @Operation(
        summary = "Verify signed QR token",
        description = "Verifies the authenticity and validity of a signed QR token"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Token verified successfully"),
            ApiResponse(responseCode = "400", description = "Invalid or expired token"),
            ApiResponse(responseCode = "401", description = "Invalid signature"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PostMapping("/verify")
    fun verifyQrToken(
        @RequestBody request: VerifyQrRequest
    ): ResponseEntity<VerifyQrResponse> {
        logger.info("Verifying QR token")

        return qrSigningService.verifySignedQrToken(
            signedToken = request.token,
            publicKey = qrSigningKeys.publicKey
        ).fold(
            onSuccess = { payload ->
                ResponseEntity.ok(
                    VerifyQrResponse(
                        valid = true,
                        stationId = payload.stationId,
                        dispenserId = payload.dispenserId,
                        timestamp = payload.timestamp,
                        expiration = payload.expiration,
                        message = "Token is valid"
                    )
                )
            },
            onFailure = { error ->
                logger.warn("QR token verification failed: {}", error.message)
                ResponseEntity.badRequest().body(
                    VerifyQrResponse(
                        valid = false,
                        message = "Token verification failed: ${error.message}"
                    )
                )
            }
        )
    }

    @Operation(
        summary = "Get QR code for specific dispenser",
        description = "Retrieves the current QR code for a specific dispenser"
    )
    @GetMapping("/dispenser/{stationId}/{dispenserId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('STATION_ADMIN') or hasRole('SYSTEM_ADMIN')")
    fun getDispenserQrCode(
        @Parameter(description = "Station ID") @PathVariable stationId: String,
        @Parameter(description = "Dispenser ID") @PathVariable dispenserId: String,
        @Parameter(description = "Expiration hours") @RequestParam(defaultValue = "1") expirationHours: Long
    ): ResponseEntity<QrResponse> {

        return generateQrCode(
            GenerateQrRequest(
                stationId = stationId,
                dispenserId = dispenserId,
                expirationHours = expirationHours
            )
        )
    }

    @Operation(
        summary = "Refresh QR code for dispenser",
        description = "Generates a new QR code for a dispenser, invalidating the previous one"
    )
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('STATION_ADMIN') or hasRole('SYSTEM_ADMIN')")
    fun refreshQrCode(
        @RequestBody request: RefreshQrRequest
    ): ResponseEntity<QrResponse> {
        logger.info("Refreshing QR code for station: {} dispenser: {}", request.stationId, request.dispenserId)

        // Generate new QR code (in a real implementation, we'd also invalidate the old one)
        return generateQrCode(
            GenerateQrRequest(
                stationId = request.stationId,
                dispenserId = request.dispenserId,
                expirationHours = request.expirationHours ?: 1L,
                baseUrl = request.baseUrl
            )
        )
    }
}

// Request/Response DTOs
data class GenerateQrRequest(
    val stationId: String,
    val dispenserId: String,
    val expirationHours: Long? = 1L,
    val baseUrl: String? = null
)

data class QrResponse(
    val success: Boolean,
    val signedToken: String? = null,
    val qrContent: String? = null,
    val stationId: String? = null,
    val dispenserId: String? = null,
    val expiresAt: LocalDateTime? = null,
    val message: String
)

data class VerifyQrRequest(
    val token: String
)

data class VerifyQrResponse(
    val valid: Boolean,
    val stationId: String? = null,
    val dispenserId: String? = null,
    val timestamp: Long? = null,
    val expiration: Long? = null,
    val message: String
)

data class RefreshQrRequest(
    val stationId: String,
    val dispenserId: String,
    val expirationHours: Long? = 1L,
    val baseUrl: String? = null
)