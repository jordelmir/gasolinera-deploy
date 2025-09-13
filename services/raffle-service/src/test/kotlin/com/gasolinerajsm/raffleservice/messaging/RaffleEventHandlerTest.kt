package com.gasolinerajsm.raffleservice.messaging

import com.gasolinerajsm.messaging.events.*
import com.gasolinerajsm.messaging.publisher.EventPublisher
import com.gasolinerajsm.raffleservice.service.RaffleEntryService
import com.gasolinerajsm.raffleservice.service.TicketValidationService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Integration tests for RaffleEventHandler
 */
class RaffleEventHandlerTest {

    private val raffleEntryService = mockk<RaffleEntryService>()
    private val ticketValidationService = mockk<TicketValidationService>()
    private val eventPublisher = mockk<EventPublisher>()

    private lateinit var raffleEventHandler: RaffleEventHandler

    @BeforeEach
    fun setUp() {
        raffleEventHandler = RaffleEventHandler(
            raffleEntryService = raffleEntryService,
            ticketValidationService = ticketValidationService,
            eventPublisher = eventPublisher
        )
        clearAllMocks()
    }

    @Test
    fun `should handle raffle tickets generated event successfully`() {
        // Given
        val event = RaffleTicketsGeneratedEvent(
            redemptionId = 123L,
            adEngagementId = null,
            userId = 456L,
            ticketCount = 5,
            ticketIds = listOf(1L, 2L, 3L, 4L, 5L),
            multiplierValue = 1,
            sourceType = "REDEMPTION",
            generatedAt = LocalDateTime.now()
        )

        val validationResult = TicketValidationService.ValidationResult(
            isValid = true,
            reason = null
        )

        every { ticketValidationService.validateGeneratedTickets(any(), any()) } returns validationResult
        every { raffleEntryService.updateUserTicketBalance(any(), any()) } just Runs
        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleRaffleTicketsGenerated(event)

        // Then
        verify { ticketValidationService.validateGeneratedTickets(event.ticketIds, event.userId) }
        verify { raffleEntryService.updateUserTicketBalance(event.userId, event.ticketCount) }
        verify {
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
        }
    }

    @Test
    fun `should handle raffle tickets generated event with validation failure`() {
        // Given
        val event = RaffleTicketsGeneratedEvent(
            redemptionId = 123L,
            adEngagementId = null,
            userId = 456L,
            ticketCount = 5,
            ticketIds = listOf(1L, 2L, 3L, 4L, 5L),
            multiplierValue = 1,
            sourceType = "REDEMPTION",
            generatedAt = LocalDateTime.now()
        )

        val validationResult = TicketValidationService.ValidationResult(
            isValid = false,
            reason = "Invalid ticket configuration"
        )

        every { ticketValidationService.validateGeneratedTickets(any(), any()) } returns validationResult
        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleRaffleTicketsGenerated(event)

        // Then
        verify { ticketValidationService.validateGeneratedTickets(event.ticketIds, event.userId) }
        verify(exactly = 0) { raffleEntryService.updateUserTicketBalance(any(), any()) }
        verify {
            eventPublisher.publishAuditEvent(
                action = "TICKET_VALIDATION_FAILED",
                resource = "raffle_tickets",
                resourceId = event.ticketIds.joinToString(","),
                auditType = AuditType.SYSTEM_EVENT,
                userId = event.userId,
                success = false,
                errorMessage = validationResult.reason,
                source = "raffle-service"
            )
        }
    }

    @Test
    fun `should handle ad engagement completed event with qualifying completion`() {
        // Given
        val event = AdEngagementCompletedEvent(
            engagementId = 789L,
            advertisementId = 101L,
            userId = 456L,
            engagementType = "COMPLETE",
            duration = 30L,
            completionPercentage = 95,
            campaignId = 202L,
            stationId = null,
            completedAt = LocalDateTime.now()
        )

        every { raffleEntryService.processAdTicketMultiplication(any(), any(), any(), any()) } just Runs
        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleAdEngagementCompleted(event)

        // Then
        verify {
            raffleEntryService.processAdTicketMultiplication(
                userId = event.userId,
                advertisementId = event.advertisementId,
                engagementId = event.engagementId,
                completionPercentage = event.completionPercentage
            )
        }
        verify {
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
        }
    }

    @Test
    fun `should handle ad engagement completed event with non-qualifying completion`() {
        // Given
        val event = AdEngagementCompletedEvent(
            engagementId = 789L,
            advertisementId = 101L,
            userId = 456L,
            engagementType = "PARTIAL",
            duration = 15L,
            completionPercentage = 70,
            campaignId = 202L,
            stationId = null,
            completedAt = LocalDateTime.now()
        )

        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleAdEngagementCompleted(event)

        // Then
        verify(exactly = 0) { raffleEntryService.processAdTicketMultiplication(any(), any(), any(), any()) }
        verify {
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
    }

    @Test
    fun `should handle ad tickets multiplied event successfully`() {
        // Given
        val event = AdTicketsMultipliedEvent(
            engagementId = 789L,
            advertisementId = 101L,
            userId = 456L,
            originalTicketCount = 10,
            multiplier = 2,
            bonusTicketCount = 20,
            totalTicketCount = 30,
            ticketIds = listOf(11L, 12L, 13L),
            campaignId = 202L,
            multipliedAt = LocalDateTime.now()
        )

        every { raffleEntryService.updateUserTicketBalance(any(), any()) } just Runs
        every { raffleEntryService.updateUserAdEngagementStats(any(), any(), any(), any()) } just Runs
        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleAdTicketsMultiplied(event)

        // Then
        verify { raffleEntryService.updateUserTicketBalance(event.userId, event.bonusTicketCount) }
        verify {
            raffleEntryService.updateUserAdEngagementStats(
                userId = event.userId,
                advertisementId = event.advertisementId,
                bonusTickets = event.bonusTicketCount,
                multiplier = event.multiplier
            )
        }
        verify {
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
        }
    }

    @Test
    fun `should handle coupon validated event with valid coupon`() {
        // Given
        val event = CouponValidatedEvent(
            couponId = 123L,
            couponCode = "SAVE20",
            userId = 456L,
            stationId = 789L,
            campaignId = 101L,
            validationResult = "VALID",
            validationReason = null,
            discountType = "PERCENTAGE",
            discountValue = BigDecimal("20.00"),
            validatedAt = LocalDateTime.now()
        )

        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleCouponValidated(event)

        // Then
        verify {
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
        }
    }

    @Test
    fun `should handle coupon validated event with invalid coupon`() {
        // Given
        val event = CouponValidatedEvent(
            couponId = 123L,
            couponCode = "EXPIRED20",
            userId = 456L,
            stationId = 789L,
            campaignId = 101L,
            validationResult = "EXPIRED",
            validationReason = "Coupon has expired",
            discountType = null,
            discountValue = null,
            validatedAt = LocalDateTime.now()
        )

        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleCouponValidated(event)

        // Then
        verify {
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
    }

    @Test
    fun `should handle coupon redeemed event successfully`() {
        // Given
        val event = CouponRedeemedEvent(
            couponId = 123L,
            couponCode = "SAVE20",
            userId = 456L,
            stationId = 789L,
            campaignId = 101L,
            redemptionId = 555L,
            discountType = "PERCENTAGE",
            discountValue = BigDecimal("20.00"),
            originalAmount = BigDecimal("100.00"),
            discountAmount = BigDecimal("20.00"),
            finalAmount = BigDecimal("80.00"),
            currency = "USD",
            redeemedAt = LocalDateTime.now()
        )

        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When
        raffleEventHandler.handleCouponRedeemed(event)

        // Then
        verify {
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
        }
    }

    @Test
    fun `should handle exception in raffle tickets generated event processing`() {
        // Given
        val event = RaffleTicketsGeneratedEvent(
            redemptionId = 123L,
            adEngagementId = null,
            userId = 456L,
            ticketCount = 5,
            ticketIds = listOf(1L, 2L, 3L, 4L, 5L),
            multiplierValue = 1,
            sourceType = "REDEMPTION",
            generatedAt = LocalDateTime.now()
        )

        val exception = RuntimeException("Database connection failed")

        every { ticketValidationService.validateGeneratedTickets(any(), any()) } throws exception
        every { eventPublisher.publishAuditEvent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        // When & Then
        assertThrows<RuntimeException> {
            raffleEventHandler.handleRaffleTicketsGenerated(event)
        }

        verify {
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
        }
    }
}