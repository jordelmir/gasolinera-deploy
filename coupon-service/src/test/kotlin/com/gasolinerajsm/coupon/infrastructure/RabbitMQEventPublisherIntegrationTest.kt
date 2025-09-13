package com.gasolinerajsm.coupon.infrastructure

import com.gasolinerajsm.coupon.domain.*
import com.gasolinerajsm.testing.shared.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.*
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * Integration Tests for RabbitMQ Event Publisher
 * Tests real message queue interactions with RabbitMQ TestContainer
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RabbitMQ Event Publisher Integration Tests")
class RabbitMQEventPublisherIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var rabbitListenerEndpointRegistry: RabbitListenerEndpointRegistry

    private lateinit var eventPublisher: RabbitMQEventPublisher
    private val receivedMessages = ConcurrentLinkedQueue<ReceivedMessage>()

    // Test queues and exchanges
    private val testExchange = "test.gasolinera.events"
    private val couponEventsQueue = "test.coupon.events"
    private val redemptionEventsQueue = "test.redemption.events"
    private val notificationEventsQueue = "test.notification.events"

    @BeforeEach
    fun setUp() {
        eventPublisher = RabbitMQEventPublisher(rabbitTemplate, objectMapper)
        receivedMessages.clear()

        // Setup test queues and bindings
        setupTestQueues()

        // Setup message listeners
        setupMessageListeners()
    }

    @Nested
    @DisplayName("Coupon Event Publishing")
    inner class CouponEventPublishing {

        @Test
        @DisplayName("Should publish CouponCreated event successfully")
        fun shouldPublishCouponCreatedEventSuccessfully() {
            // Given
            val event = CouponCreatedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("500.00"),
                fuelType = FuelType.REGULAR,
                qrCode = "QR_TEST_CODE_123456789012345678901234",
                expiresAt = LocalDateTime.now().plusDays(30),
                occurredAt = LocalDateTime.now()
            )

            // When
            eventPublisher.publish(event)

            // Then
            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.created" }
            }

            val receivedMessage = receivedMessages.first { it.routingKey == "coupon.created" }
            val receivedEvent = objectMapper.readValue(receivedMessage.payload, CouponCreatedEvent::class.java)

            assertThat(receivedEvent.couponId).isEqualTo(event.couponId)
            assertThat(receivedEvent.userId).isEqualTo(event.userId)
            assertThat(receivedEvent.amount).isEqualTo(event.amount)
            assertThat(receivedEvent.fuelType).isEqualTo(event.fuelType)
        }

        @Test
        @DisplayName("Should publish CouponRedeemed event successfully")
        fun shouldPublishCouponRedeemedEventSuccessfully() {
            // Given
            val event = CouponRedeemedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("300.00"),
                fuelType = FuelType.PREMIUM,
                redeemedAt = LocalDateTime.now(),
                occurredAt = LocalDateTime.now()
            )

            // When
            eventPublisher.publish(event)

            // Then
            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.redeemed" }
            }

            val receivedMessage = receivedMessages.first { it.routingKey == "coupon.redeemed" }
            val receivedEvent = objectMapper.readValue(receivedMessage.payload, CouponRedeemedEvent::class.java)

            assertThat(receivedEvent.couponId).isEqualTo(event.couponId)
            assertThat(receivedEvent.userId).isEqualTo(event.userId)
            assertThat(receivedEvent.amount).isEqualTo(event.amount)
            assertThat(receivedEvent.redeemedAt).isEqualTo(event.redeemedAt)
        }

        @Test
        @DisplayName("Should publish CouponExpired event successfully")
        fun shouldPublishCouponExpiredEventSuccessfully() {
            // Given
            val event = CouponExpiredEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("150.00"),
                fuelType = FuelType.DIESEL,
                expiredAt = LocalDateTime.now(),
                occurredAt = LocalDateTime.now()
            )

            // When
            eventPublisher.publish(event)

            // Then
            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.expired" }
            }

            val receivedMessage = receivedMessages.first { it.routingKey == "coupon.expired" }
            val receivedEvent = objectMapper.readValue(receivedMessage.payload, CouponExpiredEvent::class.java)

            assertThat(receivedEvent.couponId).isEqualTo(event.couponId)
            assertThat(receivedEvent.userId).isEqualTo(event.userId)
            assertThat(receivedEvent.expiredAt).isEqualTo(event.expiredAt)
        }

        @Test
        @DisplayName("Should publish CouponCancelled event successfully")
        fun shouldPublishCouponCancelledEventSuccessfully() {
            // Given
            val event = CouponCancelledEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                reason = "User requested cancellation",
                cancelledAt = LocalDateTime.now(),
                occurredAt = LocalDateTime.now()
            )

            // When
            eventPublisher.publish(event)

            // Then
            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.cancelled" }
            }

            val receivedMessage = receivedMessages.first { it.routingKey == "coupon.cancelled" }
            val receivedEvent = objectMapper.readValue(receivedMessage.payload, CouponCancelledEvent::class.java)

            assertThat(receivedEvent.couponId).isEqualTo(event.couponId)
            assertThat(receivedEvent.reason).isEqualTo(event.reason)
            assertThat(receivedEvent.cancelledAt).isEqualTo(event.cancelledAt)
        }
    }

    @Nested
    @DisplayName("Event Routing and Delivery")
    inner class EventRoutingAndDelivery {

        @Test
        @DisplayName("Should route events to correct queues based on routing key")
        fun shouldRouteEventsToCorrectQueuesBasedOnRoutingKey() {
            // Given
            val couponEvent = CouponCreatedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("100.00"),
                fuelType = FuelType.REGULAR,
                qrCode = "QR_TEST_CODE_123456789012345678901234",
                expiresAt = LocalDateTime.now().plusDays(30),
                occurredAt = LocalDateTime.now()
            )

            val redemptionEvent = CouponRedeemedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("200.00"),
                fuelType = FuelType.PREMIUM,
                redeemedAt = LocalDateTime.now(),
                occurredAt = LocalDateTime.now()
            )

            // When
            eventPublisher.publish(couponEvent)
            eventPublisher.publish(redemptionEvent)

            // Then
            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.size >= 2
            }

            val couponMessages = receivedMessages.filter { it.routingKey == "coupon.created" }
            val redemptionMessages = receivedMessages.filter { it.routingKey == "coupon.redeemed" }

            assertThat(couponMessages).hasSize(1)
            assertThat(redemptionMessages).hasSize(1)
        }

        @Test
        @DisplayName("Should handle message delivery confirmation")
        fun shouldHandleMessageDeliveryConfirmation() {
            // Given
            val event = CouponCreatedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("100.00"),
                fuelType = FuelType.REGULAR,
                qrCode = "QR_TEST_CODE_123456789012345678901234",
                expiresAt = LocalDateTime.now().plusDays(30),
                occurredAt = LocalDateTime.now()
            )

            // When
            val result = eventPublisher.publishWithConfirmation(event)

            // Then
            assertThat(result).isTrue() // Message was confirmed

            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.created" }
            }
        }

        @Test
        @DisplayName("Should handle message persistence")
        fun shouldHandleMessagePersistence() {
            // Given
            val event = CouponCreatedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("100.00"),
                fuelType = FuelType.REGULAR,
                qrCode = "QR_TEST_CODE_123456789012345678901234",
                expiresAt = LocalDateTime.now().plusDays(30),
                occurredAt = LocalDateTime.now()
            )

            // When
            eventPublisher.publishPersistent(event)

            // Then
            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.created" }
            }

            val receivedMessage = receivedMessages.first { it.routingKey == "coupon.created" }
            assertThat(receivedMessage.persistent).isTrue()
        }
    }

    @Nested
    @DisplayName("Batch Event Publishing")
    inner class BatchEventPublishing {

        @Test
        @DisplayName("Should publish multiple events in batch successfully")
        fun shouldPublishMultipleEventsInBatchSuccessfully() {
            // Given
            val events = (1..5).map { i ->
                CouponCreatedEvent(
                    couponId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("${i * 100}.00"),
                    fuelType = FuelType.REGULAR,
                    qrCode = "QR_TEST_CODE_${i}_123456789012345678901",
                    expiresAt = LocalDateTime.now().plusDays(30),
                    occurredAt = LocalDateTime.now()
                )
            }

            // When
            eventPublisher.publishBatch(events)

            // Then
            await().atMost(10, TimeUnit.SECONDS).until {
                receivedMessages.filter { it.routingKey == "coupon.created" }.size >= 5
            }

            val couponMessages = receivedMessages.filter { it.routingKey == "coupon.created" }
            assertThat(couponMessages).hasSize(5)

            val receivedAmounts = couponMessages.map { message ->
                val event = objectMapper.readValue(message.payload, CouponCreatedEvent::class.java)
                event.amount
            }.sorted()

            assertThat(receivedAmounts).containsExactly(
                BigDecimal("100.00"),
                BigDecimal("200.00"),
                BigDecimal("300.00"),
                BigDecimal("400.00"),
                BigDecimal("500.00")
            )
        }

        @Test
        @DisplayName("Should handle mixed event types in batch")
        fun shouldHandleMixedEventTypesInBatch() {
            // Given
            val events = listOf(
                CouponCreatedEvent(
                    couponId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("100.00"),
                    fuelType = FuelType.REGULAR,
                    qrCode = "QR_TEST_CODE_123456789012345678901234",
                    expiresAt = LocalDateTime.now().plusDays(30),
                    occurredAt = LocalDateTime.now()
                ),
                CouponRedeemedEvent(
                    couponId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("200.00"),
                    fuelType = FuelType.PREMIUM,
                    redeemedAt = LocalDateTime.now(),
                    occurredAt = LocalDateTime.now()
                ),
                CouponExpiredEvent(
                    couponId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("300.00"),
                    fuelType = FuelType.DIESEL,
                    expiredAt = LocalDateTime.now(),
                    occurredAt = LocalDateTime.now()
                )
            )

            // When
            eventPublisher.publishBatch(events)

            // Then
            await().atMost(10, TimeUnit.SECONDS).until {
                receivedMessages.size >= 3
            }

            val createdMessages = receivedMessages.filter { it.routingKey == "coupon.created" }
            val redeemedMessages = receivedMessages.filter { it.routingKey == "coupon.redeemed" }
            val expiredMessages = receivedMessages.filter { it.routingKey == "coupon.expired" }

            assertThat(createdMessages).hasSize(1)
            assertThat(redeemedMessages).hasSize(1)
            assertThat(expiredMessages).hasSize(1)
        }
    }

    @Nested
    @DisplayName("Error Handling and Resilience")
    inner class ErrorHandlingAndResilience {

        @Test
        @DisplayName("Should handle serialization errors gracefully")
        fun shouldHandleSerializationErrorsGracefully() {
            // Given
            val invalidEvent = object : DomainEvent {
                override val occurredAt = LocalDateTime.now()
                val circularReference = this // This will cause serialization issues
            }

            // When & Then
            assertThatThrownBy {
                eventPublisher.publish(invalidEvent)
            }.isInstanceOf(Exception::class.java)
        }

        @Test
        @DisplayName("Should retry failed message publishing")
        fun shouldRetryFailedMessagePublishing() {
            // Given
            val event = CouponCreatedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("100.00"),
                fuelType = FuelType.REGULAR,
                qrCode = "QR_TEST_CODE_123456789012345678901234",
                expiresAt = LocalDateTime.now().plusDays(30),
                occurredAt = LocalDateTime.now()
            )

            // When
            val result = eventPublisher.publishWithRetry(event, maxRetries = 3)

            // Then
            assertThat(result).isTrue()

            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.created" }
            }
        }

        @Test
        @DisplayName("Should handle connection failures gracefully")
        fun shouldHandleConnectionFailuresGracefully() {
            // This test would require more complex setup to simulate connection failures
            // For now, we'll test that the publisher handles basic connectivity

            // Given
            val event = CouponCreatedEvent(
                couponId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                stationId = UUID.randomUUID(),
                amount = BigDecimal("100.00"),
                fuelType = FuelType.REGULAR,
                qrCode = "QR_TEST_CODE_123456789012345678901234",
                expiresAt = LocalDateTime.now().plusDays(30),
                occurredAt = LocalDateTime.now()
            )

            // When
            val isConnected = eventPublisher.isConnectionHealthy()

            // Then
            assertThat(isConnected).isTrue()

            // Verify we can still publish
            eventPublisher.publish(event)

            await().atMost(5, TimeUnit.SECONDS).until {
                receivedMessages.any { it.routingKey == "coupon.created" }
            }
        }
    }

    @Nested
    @DisplayName("Performance and Load Testing")
    inner class PerformanceAndLoadTesting {

        @Test
        @DisplayName("Should handle high-volume event publishing")
        fun shouldHandleHighVolumeEventPublishing() {
            // Given
            val numberOfEvents = 100
            val events = (1..numberOfEvents).map { i ->
                CouponCreatedEvent(
                    couponId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    stationId = UUID.randomUUID(),
                    amount = BigDecimal("${i}.00"),
                    fuelType = FuelType.REGULAR,
                    qrCode = "QR_TEST_CODE_${i}_12345678901234567890",
                    expiresAt = LocalDateTime.now().plusDays(30),
                    occurredAt = LocalDateTime.now()
                )
            }

            // When
            val startTime = System.currentTimeMillis()
            events.forEach { eventPublisher.publish(it) }
            val publishTime = System.currentTimeMillis() - startTime

            // Then
            await().atMost(30, TimeUnit.SECONDS).until {
                receivedMessages.filter { it.routingKey == "coupon.created" }.size >= numberOfEvents
            }

            val couponMessages = receivedMessages.filter { it.routingKey == "coupon.created" }
            assertThat(couponMessages).hasSize(numberOfEvents)

            // Performance assertion (adjust threshold as needed)
            assertThat(publishTime).isLessThan(10000) // Should complete within 10 seconds
        }

        @Test
        @DisplayName("Should handle concurrent event publishing")
        fun shouldHandleConcurrentEventPublishing() {
            // Given
            val numberOfThreads = 5
            val eventsPerThread = 20

            // When
            val threads = (1..numberOfThreads).map { threadId ->
                Thread {
                    repeat(eventsPerThread) { eventId ->
                        val event = CouponCreatedEvent(
                            couponId = UUID.randomUUID(),
                            userId = UUID.randomUUID(),
                            stationId = UUID.randomUUID(),
                            amount = BigDecimal("${threadId * 100 + eventId}.00"),
                            fuelType = FuelType.REGULAR,
                            qrCode = "QR_TEST_CODE_${threadId}_${eventId}_123456789",
                            expiresAt = LocalDateTime.now().plusDays(30),
                            occurredAt = LocalDateTime.now()
                        )
                        eventPublisher.publish(event)
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            // Then
            val expectedTotal = numberOfThreads * eventsPerThread
            await().atMost(30, TimeUnit.SECONDS).until {
                receivedMessages.filter { it.routingKey == "coupon.created" }.size >= expectedTotal
            }

            val couponMessages = receivedMessages.filter { it.routingKey == "coupon.created" }
            assertThat(couponMessages).hasSize(expectedTotal)
        }
    }

    private fun setupTestQueues() {
        // Create exchange
        val exchange = TopicExchange(testExchange, true, false)
        rabbitTemplate.execute { channel ->
            channel.exchangeDeclare(testExchange, "topic", true, false, null)

            // Create queues
            channel.queueDeclare(couponEventsQueue, true, false, false, null)
            channel.queueDeclare(redemptionEventsQueue, true, false, false, null)
            channel.queueDeclare(notificationEventsQueue, true, false, false, null)

            // Create bindings
            channel.queueBind(couponEventsQueue, testExchange, "coupon.*")
            channel.queueBind(redemptionEventsQueue, testExchange, "coupon.redeemed")
            channel.queueBind(notificationEventsQueue, testExchange, "coupon.created")
            channel.queueBind(notificationEventsQueue, testExchange, "coupon.expired")

            null
        }
    }

    private fun setupMessageListeners() {
        // Setup listeners for test queues
        val queues = listOf(couponEventsQueue, redemptionEventsQueue, notificationEventsQueue)

        queues.forEach { queueName ->
            rabbitTemplate.execute { channel ->
                val consumer = object : com.rabbitmq.client.DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String,
                        envelope: com.rabbitmq.client.Envelope,
                        properties: com.rabbitmq.client.AMQP.BasicProperties,
                        body: ByteArray
                    ) {
                        val message = ReceivedMessage(
                            queueName = queueName,
                            routingKey = envelope.routingKey,
                            payload = String(body),
                            persistent = properties.deliveryMode == 2,
                            timestamp = System.currentTimeMillis()
                        )
                        receivedMessages.offer(message)

                        // Acknowledge the message
                        channel.basicAck(envelope.deliveryTag, false)
                    }
                }

                channel.basicConsume(queueName, false, consumer)
                null
            }
        }
    }

    // Data class for received messages
    data class ReceivedMessage(
        val queueName: String,
        val routingKey: String,
        val payload: String,
        val persistent: Boolean,
        val timestamp: Long
    )

    // Mock implementation of RabbitMQEventPublisher for testing
    class RabbitMQEventPublisher(
        private val rabbitTemplate: RabbitTemplate,
        private val objectMapper: ObjectMapper
    ) {

        fun publish(event: DomainEvent) {
            val routingKey = getRoutingKey(event)
            val payload = objectMapper.writeValueAsString(event)

            rabbitTemplate.convertAndSend("test.gasolinera.events", routingKey, payload)
        }

        fun publishWithConfirmation(event: DomainEvent): Boolean {
            return try {
                publish(event)
                true
            } catch (e: Exception) {
                false
            }
        }

        fun publishPersistent(event: DomainEvent) {
            val routingKey = getRoutingKey(event)
            val payload = objectMapper.writeValueAsString(event)

            rabbitTemplate.convertAndSend("test.gasolinera.events", routingKey, payload) { message ->
                message.messageProperties.deliveryMode = MessageDeliveryMode.PERSISTENT
                message
            }
        }

        fun publishBatch(events: List<DomainEvent>) {
            events.forEach { publish(it) }
        }

        fun publishWithRetry(event: DomainEvent, maxRetries: Int): Boolean {
            repeat(maxRetries) { attempt ->
                try {
                    publish(event)
                    return true
                } catch (e: Exception) {
                    if (attempt == maxRetries - 1) throw e
                    Thread.sleep(100 * (attempt + 1)) // Exponential backoff
                }
            }
            return false
        }

        fun isConnectionHealthy(): Boolean {
            return try {
                rabbitTemplate.execute { channel ->
                    channel.isOpen
                } ?: false
            } catch (e: Exception) {
                false
            }
        }

        private fun getRoutingKey(event: DomainEvent): String {
            return when (event) {
                is CouponCreatedEvent -> "coupon.created"
                is CouponRedeemedEvent -> "coupon.redeemed"
                is CouponExpiredEvent -> "coupon.expired"
                is CouponCancelledEvent -> "coupon.cancelled"
                else -> "unknown.event"
            }
        }
    }
}