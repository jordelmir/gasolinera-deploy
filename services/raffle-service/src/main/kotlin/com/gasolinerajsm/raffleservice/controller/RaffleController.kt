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
 * REST Controller for raffle management operations
 */
@RestController
@RequestMapping("/api/v1/raffles")
@CrossOrigin(origins = ["*"])
class RaffleController(
    private val raffleService: RaffleService,
    private val raffleEntryService: RaffleEntryService,
    private val ticketValidationService: TicketValidationService
) {
    private val logger = LoggerFactory.getLogger(RaffleController::class.java)

    /**
     * Create a new raffle
     */
    @PostMapping
    fun createRaffle(
        @Valid @RequestBody request: CreateRaffleRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<RaffleDto> {
        logger.info("Creating new raffle: ${request.name}")

        try {
            val raffle = request.toEntity(createdBy = userId)
            val savedRaffle = raffleService.createRaffle(raffle)

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedRaffle.toDto())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid raffle creation request: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Failed to create raffle", e)
            throw e
        }
    }

    /**
     * Update an existing raffle
     */
    @PutMapping("/{id}")
    fun updateRaffle(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRaffleRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<RaffleDto> {
        logger.info("Updating raffle with ID: $id")

        try {
            val raffle = request.toEntity(updatedBy = userId)
            val updatedRaffle = raffleService.updateRaffle(id, raffle)

            return ResponseEntity.ok(updatedRaffle.toDto())
        } catch (e: NoSuchElementException) {
            logger.warn("Raffle not found for update: $id")
            return ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Invalid raffle update: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Failed to update raffle $id", e)
            throw e
        }
    }

    /**
     * Get raffle by ID
     */
    @GetMapping("/{id}")
    fun getRaffleById(@PathVariable id: Long): ResponseEntity<RaffleDto> {
        logger.debug("Getting raffle with ID: $id")

        return try {
            val raffle = raffleService.getRaffleById(id)
            ResponseEntity.ok(raffle.toDto())
        } catch (e: NoSuchElementException) {
            logger.warn("Raffle not found: $id")
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all raffles with pagination and filtering
     */
    @GetMapping
    fun getAllRaffles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
        @RequestParam(required = false) status: RaffleStatus?,
        @RequestParam(required = false) type: RaffleType?,
        @RequestParam(required = false) publicOnly: Boolean?
    ): ResponseEntity<Page<RaffleDto>> {
        logger.debug("Getting raffles - page: $page, size: $size, status: $status, type: $type")

        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }

        val pageable = PageRequest.of(page, size, sort)

        val raffles = when {
            status != null && publicOnly == true ->
                raffleService.getRafflesByStatus(status, pageable)
            publicOnly == true ->
                raffleService.getActivePublicRaffles(pageable)
            status != null ->
                raffleService.getRafflesByStatus(status, pageable)
            else ->
                raffleService.getAllRaffles(pageable)
        }

        val raffleDtos = raffles.map { it.toDto() }
        return ResponseEntity.ok(raffleDtos)
    }

    /**
     * Get raffles with open registration
     */
    @GetMapping("/open")
    fun getOpenRaffles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<RaffleDto>> {
        logger.debug("Getting raffles with open registration")

        val pageable = PageRequest.of(page, size, Sort.by("drawDate").ascending())
        val raffles = raffleService.getRafflesWithOpenRegistration(pageable)
        val raffleDtos = raffles.map { it.toDto() }

        return ResponseEntity.ok(raffleDtos)
    }

    /**
     * Search raffles by name
     */
    @GetMapping("/search")
    fun searchRaffles(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<RaffleDto>> {
        logger.debug("Searching raffles with query: $query")

        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val raffles = raffleService.searchRafflesByName(query, pageable)
        val raffleDtos = raffles.map { it.toDto() }

        return ResponseEntity.ok(raffleDtos)
    }

    /**
     * Activate a raffle
     */
    @PostMapping("/{id}/activate")
    fun activateRaffle(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<RaffleDto> {
        logger.info("Activating raffle with ID: $id")

        return try {
            val raffle = raffleService.activateRaffle(id, userId)
            ResponseEntity.ok(raffle.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot activate raffle $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Pause a raffle
     */
    @PostMapping("/{id}/pause")
    fun pauseRaffle(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<RaffleDto> {
        logger.info("Pausing raffle with ID: $id")

        return try {
            val raffle = raffleService.pauseRaffle(id, userId)
            ResponseEntity.ok(raffle.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot pause raffle $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Cancel a raffle
     */
    @PostMapping("/{id}/cancel")
    fun cancelRaffle(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<RaffleDto> {
        logger.info("Cancelling raffle with ID: $id")

        return try {
            val raffle = raffleService.cancelRaffle(id, userId)
            ResponseEntity.ok(raffle.toDto())
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot cancel raffle $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Execute raffle draw
     */
    @PostMapping("/{id}/draw")
    fun executeRaffleDraw(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<List<RaffleWinnerDto>> {
        logger.info("Executing draw for raffle with ID: $id")

        return try {
            val winners = raffleService.executeRaffleDraw(id, userId)
            val winnerDtos = winners.map { it.toDto() }
            ResponseEntity.ok(winnerDtos)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot execute draw for raffle $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Get raffle statistics
     */
    @GetMapping("/{id}/statistics")
    fun getRaffleStatistics(@PathVariable id: Long): ResponseEntity<RaffleStatisticsDto> {
        logger.debug("Getting statistics for raffle with ID: $id")

        return try {
            val stats = raffleService.getRaffleStatistics(id)
            val statsDto = stats.toStatisticsDto()
            ResponseEntity.ok(statsDto)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Delete a raffle
     */
    @DeleteMapping("/{id}")
    fun deleteRaffle(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("Deleting raffle with ID: $id")

        return try {
            raffleService.deleteRaffle(id)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            logger.warn("Cannot delete raffle $id: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Check if user can enter raffle
     */
    @GetMapping("/{id}/can-enter")
    fun canUserEnterRaffle(
        @PathVariable id: Long,
        @RequestHeader("X-User-ID") userId: Long,
        @RequestParam(defaultValue = "1") additionalTickets: Int
    ): ResponseEntity<TicketValidationSummaryDto> {
        logger.debug("Checking if user $userId can enter raffle $id")

        val canEnter = raffleEntryService.canUserEnterRaffle(userId, id, additionalTickets)
        val summary = ticketValidationService.getValidationSummary(userId, id)

        val summaryDto = TicketValidationSummaryDto(
            canEnter = canEnter,
            registrationOpen = summary.registrationOpen,
            hasCapacity = summary.hasCapacity,
            userTicketCount = summary.userTicketCount,
            minTicketsRequired = summary.minTicketsRequired,
            maxTicketsAllowed = summary.maxTicketsAllowed,
            remainingSlots = summary.remainingSlots,
            registrationEndsAt = summary.registrationEndsAt,
            drawDate = summary.drawDate
        )

        return ResponseEntity.ok(summaryDto)
    }
}

// Extension functions for DTO conversion
private fun CreateRaffleRequest.toEntity(createdBy: String? = null): Raffle {
    return Raffle(
        name = this.name,
        description = this.description,
        raffleType = this.raffleType,
        registrationStart = this.registrationStart,
        registrationEnd = this.registrationEnd,
        drawDate = this.drawDate,
        maxParticipants = this.maxParticipants,
        minTicketsToParticipate = this.minTicketsToParticipate,
        maxTicketsPerUser = this.maxTicketsPerUser,
        prizePoolValue = this.prizePoolValue,
        entryFee = this.entryFee,
        winnerSelectionMethod = this.winnerSelectionMethod,
        isPublic = this.isPublic,
        requiresVerification = this.requiresVerification,
        termsAndConditions = this.termsAndConditions,
        eligibilityCriteria = this.eligibilityCriteria,
        createdBy = createdBy
    )
}

private fun UpdateRaffleRequest.toEntity(updatedBy: String? = null): Raffle {
    return Raffle(
        name = this.name,
        description = this.description,
        registrationStart = this.registrationStart,
        registrationEnd = this.registrationEnd,
        drawDate = this.drawDate,
        maxParticipants = this.maxParticipants,
        minTicketsToParticipate = this.minTicketsToParticipate,
        maxTicketsPerUser = this.maxTicketsPerUser,
        prizePoolValue = this.prizePoolValue,
        entryFee = this.entryFee,
        winnerSelectionMethod = this.winnerSelectionMethod,
        isPublic = this.isPublic,
        requiresVerification = this.requiresVerification,
        termsAndConditions = this.termsAndConditions,
        eligibilityCriteria = this.eligibilityCriteria,
        updatedBy = updatedBy
    )
}

private fun Raffle.toDto(): RaffleDto {
    return RaffleDto(
        id = this.id,
        name = this.name,
        description = this.description,
        status = this.status,
        raffleType = this.raffleType,
        registrationStart = this.registrationStart,
        registrationEnd = this.registrationEnd,
        drawDate = this.drawDate,
        maxParticipants = this.maxParticipants,
        currentParticipants = this.currentParticipants,
        minTicketsToParticipate = this.minTicketsToParticipate,
        maxTicketsPerUser = this.maxTicketsPerUser,
        totalTicketsIssued = this.totalTicketsIssued,
        totalTicketsUsed = this.totalTicketsUsed,
        prizePoolValue = this.prizePoolValue,
        entryFee = this.entryFee,
        winnerSelectionMethod = this.winnerSelectionMethod,
        isPublic = this.isPublic,
        requiresVerification = this.requiresVerification,
        termsAndConditions = this.termsAndConditions,
        eligibilityCriteria = this.eligibilityCriteria,
        createdBy = this.createdBy,
        updatedBy = this.updatedBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isRegistrationOpen = this.isRegistrationOpen(),
        isRegistrationClosed = this.isRegistrationClosed(),
        isDrawCompleted = this.isDrawCompleted(),
        canAcceptMoreParticipants = this.canAcceptMoreParticipants(),
        remainingParticipantSlots = this.getRemainingParticipantSlots(),
        participationRate = this.getParticipationRate(),
        ticketUsageRate = this.getTicketUsageRate(),
        prizeCount = this.getPrizeCount(),
        winnerCount = this.getWinnerCount(),
        totalPrizeValue = this.getTotalPrizeValue()
    )
}

private fun Map<String, Any>.toStatisticsDto(): RaffleStatisticsDto {
    val raffleInfo = this["raffle"] as Map<String, Any>
    val ticketStats = this["tickets"] as Map<String, Any>
    val prizeStats = this["prizes"] as Map<String, Any>
    val winnerStats = this["winners"] as Map<String, Any>

    return RaffleStatisticsDto(
        raffle = RaffleBasicInfo(
            id = raffleInfo["id"] as Long,
            name = raffleInfo["name"] as String,
            status = raffleInfo["status"] as RaffleStatus,
            type = raffleInfo["type"] as RaffleType,
            currentParticipants = raffleInfo["currentParticipants"] as Int,
            maxParticipants = raffleInfo["maxParticipants"] as Int?,
            registrationStart = raffleInfo["registrationStart"] as LocalDateTime,
            registrationEnd = raffleInfo["registrationEnd"] as LocalDateTime,
            drawDate = raffleInfo["drawDate"] as LocalDateTime,
            isRegistrationOpen = raffleInfo["isRegistrationOpen"] as Boolean,
            participationRate = raffleInfo["participationRate"] as Double?
        ),
        tickets = TicketStatistics(
            totalTickets = ticketStats["totalTickets"] as Long,
            activeTickets = ticketStats["activeTickets"] as Long,
            winningTickets = ticketStats["winningTickets"] as Long,
            uniqueParticipants = ticketStats["uniqueParticipants"] as Long
        ),
        prizes = PrizeStatistics(
            totalPrizes = prizeStats["totalPrizes"] as Long,
            activePrizes = prizeStats["activePrizes"] as Long,
            totalQuantity = prizeStats["totalQuantity"] as Long,
            awardedQuantity = prizeStats["awardedQuantity"] as Long,
            totalValue = prizeStats["totalValue"] as java.math.BigDecimal
        ),
        winners = WinnerStatistics(
            totalWinners = winnerStats["totalWinners"] as Long,
            pendingClaims = winnerStats["pendingClaims"] as Long,
            claimedPrizes = winnerStats["claimedPrizes"] as Long,
            expiredClaims = winnerStats["expiredClaims"] as Long,
            verifiedWinners = winnerStats["verifiedWinners"] as Long,
            notifiedWinners = winnerStats["notifiedWinners"] as Long
        )
    )
}