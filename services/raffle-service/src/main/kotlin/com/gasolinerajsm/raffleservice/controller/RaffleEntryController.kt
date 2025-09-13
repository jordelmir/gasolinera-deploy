package com.gasolinerajsm.raffleservice.controller

import com.gasolinerajsm.raffleservice.dto.*
import com.gasolinerajsm.raffleservice.model.*
import com.gasolinerajsm.raffleservice.service.*
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for raffle entry and ticket management operations
 */
@RestController
@RequestMapping("/api/v1/raffle-entries")
@CrossOrigin(origins = ["*"])
class RaffleEntryController(
    private val raffleEntryService: RaffleEntryService,
    private val ticketValidationService: TicketValidationService,
    private val raffleService: RaffleService
) {
    private val logger = LoggerFactory.getLogger(RaffleEntryController::class.java)

    /**
     * Enter a raffle using coupon redemption
     */
    @PostMapping("/coupon")
    fun enterRaffleWithCoupon(
        @Valid @RequestBody request: EnterRaffleWithCouponRequest,
        @RequestHeader("X-User-ID") userId: Long
    ): ResponseEntity<RaffleEntryResponse> {
        logger.info("User $userId entering raffle ${request.raffleId} with coupon ${request.couponId}")

        return try {
            val tickets = raffleEntryService.enterRaffleWithCoupon(
                userId = userId,
                raffleId = request.raffleId,
                couponId = request.couponId,
                ticketCount = request.ticketCount,
                stationId = request.stationId,
                transactionReference = request.transactionReference
            )

            val userTotalTickets = raffleEntryService.getUserActiveTicketCount(userId, request.raffleId)
            val raffle = raffleService.getRaffleById(request.raffleId)

            val response = RaffleEntryResponse(
                success = true,
                message = "Successfully entered raffle with ${tickets.size} tickets",
                tickets = tickets.map { it.toDto() },
                totalTicketsCreated = tickets.size,
                userTotalTickets = userTotalTickets,
                raffleInfo = raffle.toEntryInfo()
            )

            ResponseEntity.status(HttpStatus.CREATED).body(response)

        } catch (e: NoSuchElementException) {
            logger.warn("Raffle not found: ${request.raffleId}")
            ResponseEntity.badRequest().body(
                RaffleEntryResponse(false, "Raffle not found")
            )
        } catch (e: IllegalStateException) {
            logger.warn("Invalid raffle entry: ${e.message}")
            ResponseEntity.badRequest().body(
                RaffleEntryResponse(false, e.message ?: "Invalid entry")
            )
        } catch (e: Exception) {
            logger.error("Failed to enter raffle with coupon", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                RaffleEntryResponse(false, "Failed to enter raffle")
            )
        }
    }

    /**
     * Enter a raffle with direct purchase
     */
    @PostMapping("/purchase")
    fun enterRaffleWithPurchase(
        @Valid @RequestBody request: EnterRaffleWithPurchaseRequest,
        @RequestHeader("X-User-ID") userId: Long
    ): ResponseEntity<RaffleEntryResponse> {
        logger.info("User $userId entering raffle ${request.raffleId} with purchase ${request.purchaseAmount}")

        return try {
            val tickets = raffleEntryService.enterRaffleWithPurchase(
                userId = userId,
                raffleId = request.raffleId,
                ticketCount = request.ticketCount,
                purchaseAmount = request.purchaseAmount,
                stationId = request.stationId,
                transactionReference = request.transactionReference
            )

            val userTotalTickets = raffleEntryService.getUserActiveTicketCount(userId, request.raffleId)
            val raffle = raffleService.getRaffleById(request.raffleId)

            val response = RaffleEntryResponse(
                success = true,
                message = "Successfully purchased ${tickets.size} raffle tickets",
                tickets = tickets.map { it.toDto() },
                totalTicketsCreated = tickets.size,
                userTotalTickets = userTotalTickets,
                raffleInfo = raffle.toEntryInfo()
            )

            ResponseEntity.status(HttpStatus.CREATED).body(response)

        } catch (e: NoSuchElementException) {
            logger.warn("Raffle not found: ${request.raffleId}")
            ResponseEntity.badRequest().body(
                RaffleEntryResponse(false, "Raffle not found")
            )
        } catch (e: IllegalStateException) {
            logger.warn("Invalid raffle purchase: ${e.message}")
            ResponseEntity.badRequest().body(
                RaffleEntryResponse(false, e.message ?: "Invalid purchase")
            )
        } catch (e: Exception) {
            logger.error("Failed to enter raffle with purchase", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                RaffleEntryResponse(false, "Failed to enter raffle")
            )
        }
    }

    /**
     * Enter a raffle with promotional tickets
     */
    @PostMapping("/promotional")
    fun enterRaffleWithPromotionalTickets(
        @Valid @RequestBody request: EnterRaffleWithPromotionalTicketsRequest,
        @RequestHeader("X-User-ID") userId: Long,
        @RequestHeader("X-Issued-By", required = false) issuedBy: String?
    ): ResponseEntity<RaffleEntryResponse> {
        logger.info("User $userId entering raffle ${request.raffleId} with ${request.ticketCount} promotional tickets")

        return try {
            val tickets = raffleEntryService.enterRaffleWithPromotionalTickets(
                userId = userId,
                raffleId = request.raffleId,
                ticketCount = request.ticketCount,
                campaignId = request.campaignId,
                sourceReference = request.sourceReference,
                issuedBy = issuedBy
            )

            val userTotalTickets = raffleEntryService.getUserActiveTicketCount(userId, request.raffleId)
            val raffle = raffleService.getRaffleById(request.raffleId)

            val response = RaffleEntryResponse(
                success = true,
                message = "Successfully received ${tickets.size} promotional tickets",
                tickets = tickets.map { it.toDto() },
                totalTicketsCreated = tickets.size,
                userTotalTickets = userTotalTickets,
                raffleInfo = raffle.toEntryInfo()
            )

            ResponseEntity.status(HttpStatus.CREATED).body(response)

        } catch (e: NoSuchElementException) {
            logger.warn("Raffle not found: ${request.raffleId}")
            ResponseEntity.badRequest().body(
                RaffleEntryResponse(false, "Raffle not found")
            )
        } catch (e: IllegalStateException) {
            logger.warn("Invalid promotional ticket entry: ${e.message}")
            ResponseEntity.badRequest().body(
                RaffleEntryResponse(false, e.message ?: "Invalid entry")
            )
        } catch (e: Exception) {
            logger.error("Failed to enter raffle with promotional tickets", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                RaffleEntryResponse(false, "Failed to enter raffle")
            )
        }
    }

    /**
     * Get user's tickets for a specific raffle
     */
    @GetMapping("/raffles/{raffleId}/tickets")
    fun getUserRaffleTickets(
        @PathVariable raffleId: Long,
        @RequestHeader("X-User-ID") userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<Page<RaffleTicketDto>> {
        logger.debug("Getting tickets for user $userId in raffle $raffleId")

        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }

        val pageable = PageRequest.of(page, size, sort)
        val tickets = raffleEntryService.getUserRaffleTickets(userId, raffleId, pageable)
        val ticketDtos = tickets.map { it.toDto() }

        return ResponseEntity.ok(ticketDtos)
    }

    /**
     * Get user's ticket statistics
     */
    @GetMapping("/users/{userId}/statistics")
    fun getUserTicketStatistics(
        @PathVariable userId: Long,
        @RequestHeader("X-User-ID") requestingUserId: Long
    ): ResponseEntity<UserTicketStatisticsDto> {
        // Verify user can access these statistics (same user or admin)
        if (userId != requestingUserId) {
            logger.warn("User $requestingUserId attempted to access statistics for user $userId")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.debug("Getting ticket statistics for user $userId")

        val stats = raffleEntryService.getUserTicketStatistics(userId)
        val statsDto = stats.toUserTicketStatisticsDto()

        return ResponseEntity.ok(statsDto)
    }

    /**
     * Verify a ticket using verification code
     */
    @PostMapping("/tickets/verify")
    fun verifyTicket(
        @Valid @RequestBody request: VerifyTicketRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<RaffleTicketDto> {
        logger.info("Verifying ticket with code: ${request.verificationCode}")

        return try {
            val ticket = raffleEntryService.verifyTicket(request.verificationCode, request.verifiedBy)
            ResponseEntity.ok(ticket.toDto())
        } catch (e: NoSuchElementException) {
            logger.warn("Invalid verification code: ${request.verificationCode}")
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot verify ticket: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Failed to verify ticket", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Cancel a ticket
     */
    @PostMapping("/tickets/{ticketId}/cancel")
    fun cancelTicket(
        @PathVariable ticketId: Long,
        @Valid @RequestBody request: CancelTicketRequest,
        @RequestHeader("X-User-ID") userId: Long
    ): ResponseEntity<RaffleTicketDto> {
        logger.info("User $userId cancelling ticket $ticketId")

        return try {
            val ticket = raffleEntryService.cancelTicket(ticketId, userId, request.reason)
            ResponseEntity.ok(ticket.toDto())
        } catch (e: NoSuchElementException) {
            logger.warn("Ticket not found: $ticketId")
            ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            logger.warn("User $userId does not own ticket $ticketId")
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot cancel ticket $ticketId: ${e.message}")
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Failed to cancel ticket $ticketId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get ticket by ID
     */
    @GetMapping("/tickets/{ticketId}")
    fun getTicketById(
        @PathVariable ticketId: Long,
        @RequestHeader("X-User-ID") userId: Long
    ): ResponseEntity<RaffleTicketDto> {
        logger.debug("Getting ticket $ticketId for user $userId")

        return try {
            val ticket = raffleEntryService.getTicketById(ticketId, userId)
            ResponseEntity.ok(ticket.toDto())
        } catch (e: NoSuchElementException) {
            logger.warn("Ticket not found: $ticketId")
            ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            logger.warn("User $userId does not own ticket $ticketId")
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    /**
     * Validate raffle entry conditions
     */
    @GetMapping("/raffles/{raffleId}/validate")
    fun validateRaffleEntry(
        @PathVariable raffleId: Long,
        @RequestHeader("X-User-ID") userId: Long,
        @RequestParam(defaultValue = "1") ticketCount: Int,
        @RequestParam sourceType: TicketSourceType,
        @RequestParam(required = false) sourceReference: String?
    ): ResponseEntity<Map<String, Any>> {
        logger.debug("Validating raffle entry for user $userId, raffle $raffleId")

        val validation = ticketValidationService.validateRaffleEntry(
            userId = userId,
            raffleId = raffleId,
            ticketCount = ticketCount,
            sourceType = sourceType,
            sourceReference = sourceReference
        )

        val response = mapOf(
            "valid" to validation.isValid,
            "message" to (validation.errorMessage ?: "Entry is valid"),
            "canEnter" to raffleEntryService.canUserEnterRaffle(userId, raffleId, ticketCount)
        )

        return ResponseEntity.ok(response)
    }
}

// Extension functions for DTO conversion
private fun RaffleTicket.toDto(): RaffleTicketDto {
    return RaffleTicketDto(
        id = this.id,
        userId = this.userId,
        raffleId = this.raffle.id,
        raffleName = this.raffle.name,
        ticketNumber = this.ticketNumber,
        status = this.status,
        sourceType = this.sourceType,
        sourceReference = this.sourceReference,
        couponId = this.couponId,
        campaignId = this.campaignId,
        stationId = this.stationId,
        transactionReference = this.transactionReference,
        purchaseAmount = this.purchaseAmount,
        isWinner = this.isWinner,
        prizeClaimed = this.prizeClaimed,
        prizeClaimDate = this.prizeClaimDate,
        verificationCode = this.verificationCode,
        isVerified = this.isVerified,
        verifiedAt = this.verifiedAt,
        verifiedBy = this.verifiedBy,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isEligibleForDraw = this.isEligibleForDraw(),
        isWinningTicket = this.isWinningTicket(),
        isPrizeClaimed = this.isPrizeClaimed(),
        needsVerification = this.needsVerification(),
        canBeVerified = this.canBeVerified(),
        ageInHours = this.getAgeInHours(),
        ageInDays = this.getAgeInDays(),
        sourceDescription = this.getSourceDescription(),
        formattedTicketNumber = this.getFormattedTicketNumber()
    )
}

private fun Raffle.toEntryInfo(): RaffleEntryInfo {
    return RaffleEntryInfo(
        raffleId = this.id,
        raffleName = this.name,
        registrationEndsAt = this.registrationEnd,
        drawDate = this.drawDate,
        currentParticipants = this.currentParticipants,
        maxParticipants = this.maxParticipants,
        remainingSlots = this.getRemainingParticipantSlots()
    )
}

private fun Map<String, Any>.toUserTicketStatisticsDto(): UserTicketStatisticsDto {
    return UserTicketStatisticsDto(
        totalTickets = this["totalTickets"] as Long,
        activeTickets = this["activeTickets"] as Long,
        winningTickets = this["winningTickets"] as Long,
        claimedPrizes = this["claimedPrizes"] as Long
    )
}

private fun RaffleWinner.toDto(): RaffleWinnerDto {
    return RaffleWinnerDto(
        id = this.id,
        raffleId = this.raffle.id,
        raffleName = this.raffle.name,
        userId = this.userId,
        ticketId = this.ticket.id,
        ticketNumber = this.ticket.ticketNumber,
        prizeId = this.prize.id,
        prizeName = this.prize.name,
        prizeValue = this.prize.getFormattedValue(),
        prizeTier = this.prize.prizeTier,
        status = this.status,
        wonAt = this.wonAt,
        notifiedAt = this.notifiedAt,
        claimedAt = this.claimedAt,
        claimDeadline = this.claimDeadline,
        verificationCode = this.verificationCode,
        isVerified = this.isVerified,
        verifiedAt = this.verifiedAt,
        verifiedBy = this.verifiedBy,
        deliveryMethod = this.deliveryMethod,
        deliveryAddress = this.deliveryAddress,
        deliveryStatus = this.deliveryStatus,
        deliveredAt = this.deliveredAt,
        trackingNumber = this.trackingNumber,
        contactPhone = this.contactPhone,
        contactEmail = this.contactEmail,
        identityDocument = this.identityDocument,
        notes = this.notes,
        processedBy = this.processedBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isNotified = this.isNotified(),
        isClaimed = this.isClaimed(),
        isClaimExpired = this.isClaimExpired(),
        needsVerification = this.needsVerification(),
        canClaimPrize = this.canClaimPrize(),
        requiresDelivery = this.requiresDelivery(),
        daysUntilDeadline = this.getDaysUntilDeadline(),
        hoursSinceWon = this.getHoursSinceWon(),
        daysSinceWon = this.getDaysSinceWon(),
        statusDisplay = this.getStatusDisplay(),
        prizeSummary = this.getPrizeSummary()
    )
}