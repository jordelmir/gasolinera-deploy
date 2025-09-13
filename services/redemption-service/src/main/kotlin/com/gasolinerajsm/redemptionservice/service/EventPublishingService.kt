package com.gasolinerajsm.redemptionservice.service

import com.gasolinerajsm.redemptionservice.model.Redemption
import com.gasolinerajsm.redemptionservice.model.RaffleTicket
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for publishing redemption events to RabbitMQ
 */
@Service
class EventPublishingService(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(EventPublishingService::class.java)

    companion object {
        const val REDEMPTION_EXCHANGE = "redemption.exchange"
        const val REDEMPTION_CREATED_ROUTING_KEY = "redemption.created"
        const val REDEMPTION_COMPLETED_ROUTING_KEY = "redemption.completed"
        const val REDEMPTION_VOIDED_ROUTING_KEY = "redemption.voided"
        const val REDEMPTION_EXPIRED_ROUTING_KEY = "redemption.expired"
        const val RAFFLE_TICKETS_GENERATED_ROUTING_KEY = "raffle.tickets.generated"
    }

    /**
     * Publish redemption created event
     */
    fun publishRedemptionEvent(redemption: Redemption, raffleTickets: List<RaffleTicket>) {
        logger.info("Publishing redemption created event for redemption ${redemption.id}")

        try {
            val event = RedemptionCreatedEvent(
                redemptionId = redemption.id,
                referenceNumber = redemption.transactionReference,
                userId = redemption.userId,
                couponId = redemption.couponId,
                stationId = redemption.stationId,
                totalAmount = redemption.finalAmount,
                redemptionType = "COUPON_REDEMPTION",
                redeemedAt = redemption.validationTimestamp,
                raffleTicketCount = raffleTickets.size,
                eventTime = LocalDateTime.now()
            )

            rabbitTemplate.convertAndSend(
                REDEMPTION_EXCHANGE,
                REDEMPTION_CREATED_ROUTING_KEY,
                event
            )

            // Publish raffle tickets generated event if applicable
            if (raffleTickets.isNotEmpty()) {
                publishRaffleTicketsGeneratedEvent(redemption, raffleTickets)
            }

            logger.info("Redemption created event published successfully")

        } catch (exception: Exception) {
            logger.error("Error publishing redemption created event", exception)
        }
    }

    /**
     * Publish redemption completed event
     */
    fun publishRedemptionCompletedEvent(redemption: Redemption) {
        logger.info("Publishing redemption completed event for redemption ${redemption.id}")

        try {
            val event = RedemptionCompletedEvent(
                redemptionId = redemption.id,
                referenceNumber = redemption.transactionReference,
                userId = redemption.userId,
                stationId = redemption.stationId,
                employeeId = redemption.employeeId,
                totalAmount = redemption.finalAmount,
                completedAt = LocalDateTime.now(),
                eventTime = LocalDateTime.now()
            )

            rabbitTemplate.convertAndSend(
                REDEMPTION_EXCHANGE,
                REDEMPTION_COMPLETED_ROUTING_KEY,
                event
            )

            logger.info("Redemption completed event published successfully")

        } catch (exception: Exception) {
            logger.error("Error publishing redemption completed event", exception)
        }
    }

    /**
     * Publish redemption voided event
     */
    fun publishRedemptionVoidedEvent(redemption: Redemption) {
        logger.info("Publishing redemption voided event for redemption ${redemption.id}")

        try {
            val event = RedemptionVoidedEvent(
                redemptionId = redemption.id,
                referenceNumber = redemption.transactionReference,
                userId = redemption.userId,
                stationId = redemption.stationId,
                voidedBy = 0L, // TODO: Add voidedBy field to model
                voidReason = redemption.failureReason ?: "Voided",
                voidedAt = LocalDateTime.now(),
                eventTime = LocalDateTime.now()
            )

            rabbitTemplate.convertAndSend(
                REDEMPTION_EXCHANGE,
                REDEMPTION_VOIDED_ROUTING_KEY,
                event
            )

            logger.info("Redemption voided event published successfully")

        } catch (exception: Exception) {
            logger.error("Error publishing redemption voided event", exception)
        }
    }

    /**
     * Publish redemption expired event
     */
    fun publishRedemptionExpiredEvent(redemption: Redemption) {
        logger.info("Publishing redemption expired event for redemption ${redemption.id}")

        try {
            val event = RedemptionExpiredEvent(
                redemptionId = redemption.id,
                referenceNumber = redemption.transactionReference,
                userId = redemption.userId,
                stationId = redemption.stationId,
                expiredAt = LocalDateTime.now(),
                eventTime = LocalDateTime.now()
            )

            rabbitTemplate.convertAndSend(
                REDEMPTION_EXCHANGE,
                REDEMPTION_EXPIRED_ROUTING_KEY,
                event
            )

            logger.info("Redemption expired event published successfully")

        } catch (exception: Exception) {
            logger.error("Error publishing redemption expired event", exception)
        }
    }

    /**
     * Publish raffle tickets generated event
     */
    fun publishRaffleTicketsGeneratedEvent(redemption: Redemption, raffleTickets: List<RaffleTicket>) {
        logger.info("Publishing raffle tickets generated event for redemption ${redemption.id}")

        try {
            val event = RaffleTicketsGeneratedEvent(
                redemptionId = redemption.id,
                userId = redemption.userId,
                ticketCount = raffleTickets.size,
                ticketIds = raffleTickets.map { it.id },
                generatedAt = LocalDateTime.now(),
                eventTime = LocalDateTime.now()
            )

            rabbitTemplate.convertAndSend(
                REDEMPTION_EXCHANGE,
                RAFFLE_TICKETS_GENERATED_ROUTING_KEY,
                event
            )

            logger.info("Raffle tickets generated event published successfully")

        } catch (exception: Exception) {
            logger.error("Error publishing raffle tickets generated event", exception)
        }
    }
}

/**
 * Redemption created event
 */
data class RedemptionCreatedEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    val userId: Long,
    val couponId: Long,
    val stationId: Long,
    val totalAmount: java.math.BigDecimal,
    val redemptionType: String,
    val redeemedAt: LocalDateTime,
    val raffleTicketCount: Int,
    val eventTime: LocalDateTime
)

/**
 * Redemption completed event
 */
data class RedemptionCompletedEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    val userId: Long,
    val stationId: Long,
    val employeeId: Long?,
    val totalAmount: java.math.BigDecimal,
    val completedAt: LocalDateTime,
    val eventTime: LocalDateTime
)

/**
 * Redemption voided event
 */
data class RedemptionVoidedEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    val userId: Long,
    val stationId: Long,
    val voidedBy: Long,
    val voidReason: String,
    val voidedAt: LocalDateTime,
    val eventTime: LocalDateTime
)

/**
 * Redemption expired event
 */
data class RedemptionExpiredEvent(
    val redemptionId: Long,
    val referenceNumber: String,
    val userId: Long,
    val stationId: Long,
    val expiredAt: LocalDateTime,
    val eventTime: LocalDateTime
)

/**
 * Raffle tickets generated event
 */
data class RaffleTicketsGeneratedEvent(
    val redemptionId: Long,
    val userId: Long,
    val ticketCount: Int,
    val ticketIds: List<Long>,
    val generatedAt: LocalDateTime,
    val eventTime: LocalDateTime
)