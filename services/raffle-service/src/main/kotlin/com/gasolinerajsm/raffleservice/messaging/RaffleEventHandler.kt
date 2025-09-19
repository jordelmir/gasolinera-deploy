package com.gasolinerajsm.raffleservice.messaging

import com.gasolinerajsm.messaging.events.*
import com.gasolinerajsm.messaging.publisher.EventPublisher
import com.gasolinerajsm.raffleservice.service.RaffleEntryService
import com.gasolinerajsm.raffleservice.service.TicketValidationService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * Event handler for raffle-related events
 */
@Component
class RaffleEventHandler(
    private val raffleEntryService: RaffleEntryService,
    private val ticketValidationService: TicketValidationService,
    private val eventPublisher: EventPublisher
) {

    private val logger = LoggerFactory.getLogger(RaffleEventHandler::class.java)

    /**
     * Handle raffle tickets generated from redemptions
     */
    @RabbitListener(queues = ["raffle.tickets.generated.queue"])
    fun handleRaffleTicketsGenerated(event: RaffleTicketsGeneratedEvent) {
        logger.info(
            "Processing raffle tickets generated event: {} tickets for user {}",
            event.ticketCount,
            event.userId
        )

        try {
            // Validate the tickets exist and are properly configured
            val validationResult = ticketValidationService.validateGeneratedTickets(
                event.ticketIds,
                event.userId
            )

            if (!validationResult.isValid) {
                logger.error("Ticket validation failed: {}", validationResult.errorMessage)

                // Publish audit event for failed validation
                eventPublisher.publishAuditEvent(
                    action = "TICKET_VALIDATION_FAILED",
                    resource = "raffle_tickets",
                    resourceId = event.ticketIds.joinToString(","),
                    auditType = AuditType.SYSTEM_EVENT,
                    userId = event.userId,
                    success = false,
                    errorMessage = validationResult.errorMessage,
                    source = "raffle-service"
                )
                return
            }

            // Update ticket balances and user statistics
            raffleEntryService.updateUserTicketBalance(event.userId, event.ticketCount)

            // Log successful processing
            logger.info(
                "Successfully processed {} raffle tickets for user {}",
                event.ticketCount,
                event.userId
            )

            // Publish audit event for successful processing
            eventPublisher.publishAuditEvent(
                action = "RAFFLE_TICKETS_PROCESSED",
                resource = "raffle_tickets",
                resourceId = event.ticketIds.joinToString(","),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = true,
                details = mapOf(
                    "ticketCount" to event.ticketCount,
                    "sourceType" to event.sourceType,
                    "multiplierValue" to event.multiplierValue
                ),
                source = "raffle-service"
            )

        } catch (exception: Exception) {
            logger.error("Error processing raffle tickets generated event", exception)

            // Publish audit event for processing failure
            eventPublisher.publishAuditEvent(
                action = "RAFFLE_TICKETS_PROCESSING_FAILED",
                resource = "raffle_tickets",
                resourceId = event.ticketIds.joinToString(","),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = "raffle-service"
            )

            throw exception
        }
    }

    /**
     * Handle ad engagement completed events for ticket multiplication
     */
    @RabbitListener(queues = ["ad.engagement.completed.queue"])
    fun handleAdEngagementCompleted(event: AdEngagementCompletedEvent) {
        logger.info(
            "Processing ad engagement completed event: engagement {} for user {}",
            event.engagementId,
            event.userId
        )

        try {
            // Check if this engagement qualifies for ticket multiplication
            if (event.completionPercentage >= 80) { // 80% completion required

                logger.info(
                    "Ad engagement {} qualifies for ticket multiplication ({}% completion)",
                    event.engagementId,
                    event.completionPercentage
                )

                // Process ticket multiplication based on ad engagement
                raffleEntryService.processAdTicketMultiplication(
                    userId = event.userId,
                    advertisementId = event.advertisementId,
                    engagementId = event.engagementId,
                    completionPercentage = event.completionPercentage
                )

                // Publish audit event
                eventPublisher.publishAuditEvent(
                    action = "AD_ENGAGEMENT_QUALIFIED",
                    resource = "ad_engagement",
                    resourceId = event.engagementId.toString(),
                    auditType = AuditType.USER_ACTION,
                    userId = event.userId,
                    success = true,
                    details = mapOf(
                        "advertisementId" to event.advertisementId,
                        "completionPercentage" to event.completionPercentage,
                        "duration" to (event.duration ?: 0)
                    ),
                    source = "raffle-service"
                )
            } else {
                logger.info(
                    "Ad engagement {} does not qualify for ticket multiplication ({}% completion < 80%)",
                    event.engagementId,
                    event.completionPercentage
                )

                // Publish audit event for non-qualifying engagement
                eventPublisher.publishAuditEvent(
                    action = "AD_ENGAGEMENT_NOT_QUALIFIED",
                    resource = "ad_engagement",
                    resourceId = event.engagementId.toString(),
                    auditType = AuditType.USER_ACTION,
                    userId = event.userId,
                    success = true,
                    details = mapOf(
                        "advertisementId" to event.advertisementId,
                        "completionPercentage" to event.completionPercentage,
                        "reason" to "Completion percentage below threshold"
                    ),
                    source = "raffle-service"
                )
            }

        } catch (exception: Exception) {
            logger.error("Error processing ad engagement completed event", exception)

            eventPublisher.publishAuditEvent(
                action = "AD_ENGAGEMENT_PROCESSING_FAILED",
                resource = "ad_engagement",
                resourceId = event.engagementId.toString(),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = "raffle-service"
            )
        }
    }

    /**
     * Handle ad tickets multiplied events
     */
    @RabbitListener(queues = ["ad.tickets.multiplied.queue"])
    fun handleAdTicketsMultiplied(event: AdTicketsMultipliedEvent) {
        logger.info(
            "Processing ad tickets multiplied event: {} bonus tickets for user {}",
            event.bonusTicketCount,
            event.userId
        )

        try {
            // Update user's ticket balance with the multiplied tickets
            raffleEntryService.updateUserTicketBalance(event.userId, event.bonusTicketCount)

            // Update user statistics for ad-based ticket generation
            raffleEntryService.updateUserAdEngagementStats(
                userId = event.userId,
                advertisementId = event.advertisementId,
                bonusTickets = event.bonusTicketCount,
                multiplier = event.multiplier
            )

            logger.info(
                "Successfully processed {} bonus tickets from ad engagement for user {}",
                event.bonusTicketCount,
                event.userId
            )

            // Publish audit event for successful processing
            eventPublisher.publishAuditEvent(
                action = "AD_TICKETS_MULTIPLIED_PROCESSED",
                resource = "raffle_tickets",
                resourceId = event.ticketIds.joinToString(","),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = true,
                details = mapOf(
                    "advertisementId" to event.advertisementId,
                    "originalTicketCount" to event.originalTicketCount,
                    "multiplier" to event.multiplier,
                    "bonusTicketCount" to event.bonusTicketCount,
                    "totalTicketCount" to event.totalTicketCount
                ),
                source = "raffle-service"
            )

        } catch (exception: Exception) {
            logger.error("Error processing ad tickets multiplied event", exception)

            eventPublisher.publishAuditEvent(
                action = "AD_TICKETS_MULTIPLIED_PROCESSING_FAILED",
                resource = "raffle_tickets",
                resourceId = event.ticketIds.joinToString(","),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = "raffle-service"
            )

            throw exception
        }
    }

    /**
     * Handle redemption completed events for additional processing
     */
    @RabbitListener(queues = ["redemption.completed.queue"])
    fun handleRedemptionCompleted(event: RedemptionCompletedEvent) {
        logger.info(
            "Processing redemption completed event: {} for user {}",
            event.referenceNumber,
            event.userId
        )

        try {
            // Update user statistics or trigger additional business logic
            // This could include loyalty point calculations, user tier updates, etc.

            logger.info(
                "Successfully processed redemption completed event for redemption {}",
                event.referenceNumber
            )

            // Publish audit event
            eventPublisher.publishAuditEvent(
                action = "REDEMPTION_COMPLETED_PROCESSED",
                resource = "redemption",
                resourceId = event.redemptionId.toString(),
                auditType = AuditType.TRANSACTION,
                userId = event.userId,
                stationId = event.stationId,
                success = true,
                details = mapOf(
                    "referenceNumber" to event.referenceNumber,
                    "totalAmount" to event.totalAmount,
                    "employeeId" to (event.employeeId ?: "system")
                ),
                source = "raffle-service"
            )

        } catch (exception: Exception) {
            logger.error("Error processing redemption completed event", exception)

            eventPublisher.publishAuditEvent(
                action = "REDEMPTION_COMPLETED_PROCESSING_FAILED",
                resource = "redemption",
                resourceId = event.redemptionId.toString(),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = "raffle-service"
            )
        }
    }

    /**
     * Handle redemption voided events
     */
    @RabbitListener(queues = ["redemption.voided.queue"])
    fun handleRedemptionVoided(event: RedemptionVoidedEvent) {
        logger.info(
            "Processing redemption voided event: {} for user {}",
            event.referenceNumber,
            event.userId
        )

        try {
            // Handle ticket reversal or other cleanup operations
            // This might involve marking related tickets as invalid

            logger.info(
                "Successfully processed redemption voided event for redemption {}",
                event.referenceNumber
            )

            // Publish audit event
            eventPublisher.publishAuditEvent(
                action = "REDEMPTION_VOIDED_PROCESSED",
                resource = "redemption",
                resourceId = event.redemptionId.toString(),
                auditType = AuditType.TRANSACTION,
                userId = event.userId,
                stationId = event.stationId,
                success = true,
                details = mapOf(
                    "referenceNumber" to event.referenceNumber,
                    "voidedBy" to event.voidedBy,
                    "voidReason" to event.voidReason
                ),
                source = "raffle-service"
            )

        } catch (exception: Exception) {
            logger.error("Error processing redemption voided event", exception)

            eventPublisher.publishAuditEvent(
                action = "REDEMPTION_VOIDED_PROCESSING_FAILED",
                resource = "redemption",
                resourceId = event.redemptionId.toString(),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = "raffle-service"
            )
        }
    }

    /**
     * Handle coupon validated events
     */
    @RabbitListener(queues = ["coupon.validated.queue"])
    fun handleCouponValidated(event: CouponValidatedEvent) {
        logger.info(
            "Processing coupon validated event: coupon {} for user {}",
            event.couponCode,
            event.userId
        )

        try {
            // Process coupon validation for potential raffle entry
            if (event.validationResult == "VALID") {
                logger.info(
                    "Valid coupon {} processed for user {} at station {}",
                    event.couponCode,
                    event.userId,
                    event.stationId
                )

                // Publish audit event for valid coupon
                eventPublisher.publishAuditEvent(
                    action = "COUPON_VALIDATED_PROCESSED",
                    resource = "coupon",
                    resourceId = event.couponId.toString(),
                    auditType = AuditType.TRANSACTION,
                    userId = event.userId,
                    stationId = event.stationId,
                    success = true,
                    details = mapOf(
                        "couponCode" to event.couponCode,
                        "campaignId" to event.campaignId,
                        "discountValue" to (event.discountValue ?: 0)
                    ),
                    source = "raffle-service"
                )
            } else {
                logger.warn(
                    "Invalid coupon {} for user {}: {}",
                    event.couponCode,
                    event.userId,
                    event.validationReason ?: "Unknown reason"
                )

                // Publish audit event for invalid coupon
                eventPublisher.publishAuditEvent(
                    action = "INVALID_COUPON_PROCESSED",
                    resource = "coupon",
                    resourceId = event.couponId.toString(),
                    auditType = AuditType.USER_ACTION,
                    userId = event.userId,
                    stationId = event.stationId,
                    success = false,
                    errorMessage = event.validationReason,
                    details = mapOf(
                        "couponCode" to event.couponCode,
                        "validationResult" to event.validationResult
                    ),
                    source = "raffle-service"
                )
            }

        } catch (exception: Exception) {
            logger.error("Error processing coupon validated event", exception)

            eventPublisher.publishAuditEvent(
                action = "COUPON_VALIDATED_PROCESSING_FAILED",
                resource = "coupon",
                resourceId = event.couponId.toString(),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = "raffle-service"
            )
        }
    }

    /**
     * Handle coupon redeemed events
     */
    @RabbitListener(queues = ["coupon.redeemed.queue"])
    fun handleCouponRedeemed(event: CouponRedeemedEvent) {
        logger.info(
            "Processing coupon redeemed event: coupon {} for user {} with discount {}",
            event.couponCode,
            event.userId,
            event.discountAmount
        )

        try {
            // Process coupon redemption for raffle ticket generation
            logger.info(
                "Coupon {} redeemed successfully for user {} - Final amount: {}",
                event.couponCode,
                event.userId,
                event.finalAmount
            )

            // Publish audit event for successful coupon redemption
            eventPublisher.publishAuditEvent(
                action = "COUPON_REDEEMED_PROCESSED",
                resource = "coupon",
                resourceId = event.couponId.toString(),
                auditType = AuditType.TRANSACTION,
                userId = event.userId,
                stationId = event.stationId,
                success = true,
                details = mapOf(
                    "couponCode" to event.couponCode,
                    "campaignId" to event.campaignId,
                    "redemptionId" to event.redemptionId,
                    "discountType" to event.discountType,
                    "discountValue" to event.discountValue,
                    "originalAmount" to event.originalAmount,
                    "discountAmount" to event.discountAmount,
                    "finalAmount" to event.finalAmount,
                    "currency" to event.currency
                ),
                source = "raffle-service"
            )

        } catch (exception: Exception) {
            logger.error("Error processing coupon redeemed event", exception)

            eventPublisher.publishAuditEvent(
                action = "COUPON_REDEEMED_PROCESSING_FAILED",
                resource = "coupon",
                resourceId = event.couponId.toString(),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = exception.message,
                source = "raffle-service"
            )
        }
    }
}