package com.gasolinerajsm.redemptionservice.service

import com.gasolinerajsm.redemptionservice.model.*
import com.gasolinerajsm.redemptionservice.repository.RedemptionRepository
import com.gasolinerajsm.redemptionservice.repository.RedemptionItemRepository
import com.gasolinerajsm.redemptionservice.dto.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing redemption operations and business logic
 */
@Service
@Transactional
class RedemptionService(
    private val redemptionRepository: RedemptionRepository,
    private val redemptionItemRepository: RedemptionItemRepository,
    private val couponValidationService: CouponValidationService,
    private val eventPublishingService: EventPublishingService,
    // private val fraudDetectionService: FraudDetectionService,
    private val referenceNumberGenerator: ReferenceNumberGenerator
) {

    private val logger = LoggerFactory.getLogger(RedemptionService::class.java)

    /**
     * Process a coupon redemption
     */
    fun processRedemption(request: RedemptionRequest): RedemptionResult {
        logger.info("Processing redemption for user ${request.userId}, coupon ${request.couponId}")

        try {
            // Validate coupon
            val couponValidation = couponValidationService.validateCoupon(
                request.couponId,
                request.userId,
                request.stationId
            )

            if (!couponValidation.isValid) {
                logger.warn("Coupon validation failed: ${couponValidation.reason}")
                return RedemptionResult.failure(couponValidation.reason ?: "Invalid coupon")
            }

            // Check for duplicate redemption
            if (redemptionRepository.existsByUserIdAndCouponId(request.userId, request.couponId)) {
                logger.warn("Duplicate redemption attempt for user ${request.userId}, coupon ${request.couponId}")
                return RedemptionResult.failure("Coupon has already been redeemed")
            }

            // Generate reference number
            val referenceNumber = referenceNumberGenerator.generate()

            // TODO: Implement fraud detection
            val fraudScore = BigDecimal.ZERO
            val riskLevel = "LOW"

            // Create redemption entity
            val redemption = createRedemption(request, referenceNumber, fraudScore, riskLevel)

            // Save redemption
            val savedRedemption = redemptionRepository.save(redemption)

            // TODO: Handle redemption items if needed

            // Generate raffle tickets if applicable
            val raffleTickets = couponValidation.couponDetails?.let {
                generateRaffleTickets(savedRedemption, it)
            } ?: emptyList()

            // Publish redemption event
            eventPublishingService.publishRedemptionEvent(savedRedemption, raffleTickets)

            logger.info("Redemption processed successfully: ${savedRedemption.transactionReference}")

            return RedemptionResult.success(
                redemption = savedRedemption,
                raffleTickets = raffleTickets,
                message = "Redemption processed successfully"
            )

        } catch (exception: Exception) {
            logger.error("Error processing redemption", exception)
            return RedemptionResult.failure("Failed to process redemption: ${exception.message}")
        }
    }

    /**
     * Get redemption by reference number
     */
    @Transactional(readOnly = true)
    fun getRedemptionByReference(referenceNumber: String): Redemption? {
        return redemptionRepository.findByReferenceNumber(referenceNumber)
    }

    /**
     * Get redemptions by user ID
     */
    @Transactional(readOnly = true)
    fun getRedemptionsByUser(userId: Long, pageable: Pageable): Page<Redemption> {
        return redemptionRepository.findByUserIdOrderByRedeemedAtDesc(userId, pageable)
    }

    /**
     * Get redemptions by station ID
     */
    @Transactional(readOnly = true)
    fun getRedemptionsByStation(stationId: Long, pageable: Pageable): Page<Redemption> {
        return redemptionRepository.findByStationIdOrderByRedeemedAtDesc(stationId, pageable)
    }

    /**
     * Get redemptions by status
     */
    @Transactional(readOnly = true)
    fun getRedemptionsByStatus(status: RedemptionStatus, pageable: Pageable): Page<Redemption> {
        return redemptionRepository.findByStatusOrderByRedeemedAtDesc(status, pageable)
    }

    /**
     * Get redemptions requiring review
     */
    @Transactional(readOnly = true)
    fun getRedemptionsRequiringReview(pageable: Pageable): Page<Redemption> {
        return redemptionRepository.findByFlaggedForReviewTrueAndReviewedAtIsNullOrderByRedeemedAtDesc(pageable)
    }

    /**
     * Complete a redemption
     */
    fun completeRedemption(redemptionId: Long, employeeId: Long?): RedemptionResult {
        logger.info("Completing redemption $redemptionId")

        val redemption = redemptionRepository.findById(redemptionId).orElse(null)
            ?: return RedemptionResult.failure("Redemption not found")

        if (redemption.status != RedemptionStatus.PENDING) {
            return RedemptionResult.failure("Redemption is not in pending status")
        }

        val completedRedemption = redemption.complete().copy(
            employeeId = employeeId ?: redemption.employeeId,
            updatedAt = LocalDateTime.now()
        )

        val savedRedemption = redemptionRepository.save(completedRedemption)

        // Publish completion event
        eventPublishingService.publishRedemptionCompletedEvent(savedRedemption)

        logger.info("Redemption completed: ${savedRedemption.transactionReference}")

        return RedemptionResult.success(
            redemption = savedRedemption,
            message = "Redemption completed successfully"
        )
    }

    /**
     * Void a redemption
     */
    fun voidRedemption(redemptionId: Long, voidedBy: Long, reason: String): RedemptionResult {
        logger.info("Voiding redemption $redemptionId by user $voidedBy")

        val redemption = redemptionRepository.findById(redemptionId).orElse(null)
            ?: return RedemptionResult.failure("Redemption not found")

        if (redemption.status.isFinalState() && redemption.status != RedemptionStatus.COMPLETED) {
            return RedemptionResult.failure("Redemption cannot be voided in current status")
        }

        val voidedRedemption = redemption.cancel(reason)
        val savedRedemption = redemptionRepository.save(voidedRedemption)

        // Publish void event
        eventPublishingService.publishRedemptionVoidedEvent(savedRedemption)

        logger.info("Redemption voided: ${savedRedemption.transactionReference}")

        return RedemptionResult.success(
            redemption = savedRedemption,
            message = "Redemption voided successfully"
        )
    }

    /**
     * Flag redemption for review
     */
    fun flagForReview(redemptionId: Long, reason: String): RedemptionResult {
        logger.info("Flagging redemption $redemptionId for review")

        val redemption = redemptionRepository.findById(redemptionId).orElse(null)
            ?: return RedemptionResult.failure("Redemption not found")

        val flaggedRedemption = redemption.addNotes("FLAGGED FOR REVIEW: $reason")
        val savedRedemption = redemptionRepository.save(flaggedRedemption)

        logger.info("Redemption flagged for review: ${savedRedemption.transactionReference}")

        return RedemptionResult.success(
            redemption = savedRedemption,
            message = "Redemption flagged for review"
        )
    }

    /**
     * Review a redemption
     */
    fun reviewRedemption(redemptionId: Long, reviewedBy: Long, approved: Boolean, notes: String?): RedemptionResult {
        logger.info("Reviewing redemption $redemptionId by user $reviewedBy")

        val redemption = redemptionRepository.findById(redemptionId).orElse(null)
            ?: return RedemptionResult.failure("Redemption not found")

        val reviewedRedemption = if (approved) {
            redemption.addNotes("REVIEWED BY USER $reviewedBy: APPROVED - $notes").complete()
        } else {
            redemption.addNotes("REVIEWED BY USER $reviewedBy: REJECTED - $notes").fail("Review rejected: $notes")
        }

        val savedRedemption = redemptionRepository.save(reviewedRedemption)

        logger.info("Redemption reviewed: ${savedRedemption.transactionReference}, approved: $approved")

        return RedemptionResult.success(
            redemption = savedRedemption,
            message = if (approved) "Redemption approved" else "Redemption rejected"
        )
    }

    /**
     * Get redemption statistics
     */
    @Transactional(readOnly = true)
    fun getRedemptionStatistics(
        stationId: Long?,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): RedemptionStatistics {

        val totalCount = if (stationId != null) {
            redemptionRepository.countByStationId(stationId)
        } else {
            redemptionRepository.countByRedeemedAtBetween(startDate, endDate)
        }

        val totalAmount = if (stationId != null) {
            redemptionRepository.sumTotalAmountByStationId(stationId)
        } else {
            redemptionRepository.sumTotalAmountByDateRange(startDate, endDate)
        }

        val averageAmount = if (totalCount > 0) {
            totalAmount.divide(BigDecimal(totalCount), 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return RedemptionStatistics(
            totalRedemptions = totalCount,
            totalAmount = totalAmount,
            totalTickets = 0L, // TODO: Calculate from raffle tickets
            averageAmount = averageAmount
        )
    }

    /**
     * Process expired redemptions
     */
    fun processExpiredRedemptions(): Int {
        logger.info("Processing expired redemptions")

        val expiredRedemptions = redemptionRepository.findExpiredRedemptions(LocalDateTime.now())
        var processedCount = 0

        expiredRedemptions.forEach { redemption ->
            try {
                val expiredRedemption = redemption.copy(
                    status = RedemptionStatus.CANCELLED,
                    updatedAt = LocalDateTime.now()
                )
                redemptionRepository.save(expiredRedemption)

                // Publish expiration event
                eventPublishingService.publishRedemptionExpiredEvent(expiredRedemption)

                processedCount++
            } catch (exception: Exception) {
                logger.error("Error processing expired redemption ${redemption.id}", exception)
            }
        }

        logger.info("Processed $processedCount expired redemptions")
        return processedCount
    }

    private fun createRedemption(
        request: RedemptionRequest,
        referenceNumber: String,
        fraudScore: BigDecimal,
        riskLevel: String
    ): Redemption {
        return Redemption(
            userId = request.userId,
            couponId = request.couponId,
            stationId = request.stationId,
            employeeId = request.employeeId ?: 0L,
            campaignId = request.campaignId ?: 0L,
            transactionReference = referenceNumber,
            status = RedemptionStatus.PENDING,
            qrCode = "QR_${request.couponId}",
            couponCode = "COUPON_${request.couponId}",
            purchaseAmount = request.originalValue,
            discountAmount = request.discountAmount ?: BigDecimal.ZERO,
            finalAmount = request.totalAmount,
            raffleTicketsEarned = 0,
            paymentMethod = "COUPON",
            paymentReference = "REF_${System.currentTimeMillis()}",
            validationTimestamp = LocalDateTime.now(),
            notes = "Coupon redemption",
            metadata = request.metadata
        )
    }

    // TODO: Implement createRedemptionItem when needed

    private fun generateRaffleTickets(redemption: Redemption, couponDetails: CouponDetails): List<RaffleTicket> {
        // This will be implemented based on the coupon type and redemption amount
        // For now, return empty list
        return emptyList()
    }
}