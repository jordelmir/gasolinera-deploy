package com.gasolinerajsm.messaging.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

/**
 * Shared RabbitMQ configuration for all Gasolinera JSM services
 */
@Configuration
@ConditionalOnProperty(name = ["app.messaging.enabled"], havingValue = "true", matchIfMissing = true)
class RabbitMQConfig {

    companion object {
        // Exchange Names
        const val GASOLINERA_EVENTS_EXCHANGE = "gasolinera.events"
        const val REDEMPTION_EXCHANGE = "redemption.exchange"
        const val AD_EXCHANGE = "ad.exchange"
        const val RAFFLE_EXCHANGE = "raffle.exchange"
        const val COUPON_EXCHANGE = "coupon.exchange"
        const val AUDIT_EXCHANGE = "audit.exchange"
        const val DEAD_LETTER_EXCHANGE = "gasolinera.dlx"

        // Queue Names
        const val REDEMPTION_CREATED_QUEUE = "redemption.created.queue"
        const val REDEMPTION_COMPLETED_QUEUE = "redemption.completed.queue"
        const val REDEMPTION_VOIDED_QUEUE = "redemption.voided.queue"
        const val RAFFLE_TICKETS_GENERATED_QUEUE = "raffle.tickets.generated.queue"
        const val RAFFLE_ENTRY_CREATED_QUEUE = "raffle.entry.created.queue"
        const val RAFFLE_WINNER_SELECTED_QUEUE = "raffle.winner.selected.queue"
        const val AD_ENGAGEMENT_COMPLETED_QUEUE = "ad.engagement.completed.queue"
        const val AD_TICKETS_MULTIPLIED_QUEUE = "ad.tickets.multiplied.queue"
        const val COUPON_VALIDATED_QUEUE = "coupon.validated.queue"
        const val COUPON_REDEEMED_QUEUE = "coupon.redeemed.queue"
        const val AUDIT_EVENTS_QUEUE = "audit.events.queue"
        const val AUDIT_SECURITY_QUEUE = "audit.security.queue"
        const val DEAD_LETTER_QUEUE = "gasolinera.dlq"

        // Routing Keys
        const val REDEMPTION_CREATED_KEY = "redemption.created"
        const val REDEMPTION_COMPLETED_KEY = "redemption.completed"
        const val REDEMPTION_VOIDED_KEY = "redemption.voided"
        const val REDEMPTION_EXPIRED_KEY = "redemption.expired"
        const val RAFFLE_TICKETS_GENERATED_KEY = "raffle.tickets.generated"
        const val RAFFLE_ENTRY_CREATED_KEY = "raffle.entry.created"
        const val RAFFLE_WINNER_SELECTED_KEY = "raffle.winner.selected"
        const val AD_ENGAGEMENT_COMPLETED_KEY = "ad.engagement.completed"
        const val AD_TICKETS_MULTIPLIED_KEY = "ad.tickets.multiplied"
        const val COUPON_VALIDATED_KEY = "coupon.validated"
        const val COUPON_REDEEMED_KEY = "coupon.redeemed"
        const val AUDIT_USER_ACTION_KEY = "audit.user.action"
        const val AUDIT_SECURITY_VIOLATION_KEY = "audit.security.violation"
        const val AUDIT_SYSTEM_EVENT_KEY = "audit.system.event"
    }

    /**
     * JSON message converter for RabbitMQ
     */
    @Bean
    fun jsonMessageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter {
        val converter = Jackson2JsonMessageConverter(objectMapper)
        converter.setCreateMessageIds(true)
        return converter
    }

    /**
     * RabbitMQ template with JSON converter and retry configuration
     */
    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: Jackson2JsonMessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        template.setRetryTemplate(retryTemplate())
        template.setConfirmCallback { correlationData, ack, cause ->
            if (!ack) {
                println("Message not delivered: $cause")
            }
        }
        template.setReturnsCallback { returned ->
            println("Message returned: ${returned.message}")
        }
        template.setMandatory(true)
        return template
    }

    /**
     * Retry template for failed message publishing
     */
    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()

        // Retry policy: retry up to 3 times
        val retryPolicy = SimpleRetryPolicy()
        retryPolicy.maxAttempts = 3
        retryTemplate.setRetryPolicy(retryPolicy)

        // Backoff policy: exponential backoff starting at 1 second
        val backOffPolicy = ExponentialBackOffPolicy()
        backOffPolicy.initialInterval = 1000
        backOffPolicy.multiplier = 2.0
        backOffPolicy.maxInterval = 10000
        retryTemplate.setBackOffPolicy(backOffPolicy)

        return retryTemplate
    }

    /**
     * Rabbit listener container factory with error handling
     */
    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        messageConverter: Jackson2JsonMessageConverter
    ): RabbitListenerContainerFactory<SimpleMessageListenerContainer> {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(connectionFactory)
        factory.setMessageConverter(messageConverter)
        factory.setConcurrentConsumers(3)
        factory.setMaxConcurrentConsumers(10)
        factory.setPrefetchCount(10)
        factory.setDefaultRequeueRejected(false) // Send to DLQ instead of requeue
        factory.setRetryTemplate(retryTemplate())
        return factory
    }

    // Exchange Declarations
    @Bean
    fun gasolineraEventsExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(GASOLINERA_EVENTS_EXCHANGE)
            .durable(true)
            .withArgument("alternate-exchange", DEAD_LETTER_EXCHANGE)
            .build()
    }

    @Bean
    fun redemptionExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(REDEMPTION_EXCHANGE)
            .durable(true)
            .withArgument("alternate-exchange", DEAD_LETTER_EXCHANGE)
            .build()
    }

    @Bean
    fun adExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(AD_EXCHANGE)
            .durable(true)
            .withArgument("alternate-exchange", DEAD_LETTER_EXCHANGE)
            .build()
    }

    @Bean
    fun raffleExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(RAFFLE_EXCHANGE)
            .durable(true)
            .withArgument("alternate-exchange", DEAD_LETTER_EXCHANGE)
            .build()
    }

    @Bean
    fun couponExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(COUPON_EXCHANGE)
            .durable(true)
            .withArgument("alternate-exchange", DEAD_LETTER_EXCHANGE)
            .build()
    }

    @Bean
    fun auditExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(AUDIT_EXCHANGE)
            .durable(true)
            .build()
    }

    @Bean
    fun deadLetterExchange(): DirectExchange {
        return ExchangeBuilder
            .directExchange(DEAD_LETTER_EXCHANGE)
            .durable(true)
            .build()
    }

    // Queue Declarations with Dead Letter Configuration
    @Bean
    fun redemptionCreatedQueue(): Queue {
        return QueueBuilder
            .durable(REDEMPTION_CREATED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "redemption.created.failed")
            .withArgument("x-message-ttl", 3600000) // 1 hour
            .build()
    }

    @Bean
    fun redemptionCompletedQueue(): Queue {
        return QueueBuilder
            .durable(REDEMPTION_COMPLETED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "redemption.completed.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun redemptionVoidedQueue(): Queue {
        return QueueBuilder
            .durable(REDEMPTION_VOIDED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "redemption.voided.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun raffleTicketsGeneratedQueue(): Queue {
        return QueueBuilder
            .durable(RAFFLE_TICKETS_GENERATED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "raffle.tickets.generated.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun raffleEntryCreatedQueue(): Queue {
        return QueueBuilder
            .durable(RAFFLE_ENTRY_CREATED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "raffle.entry.created.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun raffleWinnerSelectedQueue(): Queue {
        return QueueBuilder
            .durable(RAFFLE_WINNER_SELECTED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "raffle.winner.selected.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun adEngagementCompletedQueue(): Queue {
        return QueueBuilder
            .durable(AD_ENGAGEMENT_COMPLETED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "ad.engagement.completed.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun adTicketsMultipliedQueue(): Queue {
        return QueueBuilder
            .durable(AD_TICKETS_MULTIPLIED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "ad.tickets.multiplied.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun couponValidatedQueue(): Queue {
        return QueueBuilder
            .durable(COUPON_VALIDATED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "coupon.validated.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun couponRedeemedQueue(): Queue {
        return QueueBuilder
            .durable(COUPON_REDEEMED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "coupon.redeemed.failed")
            .withArgument("x-message-ttl", 3600000)
            .build()
    }

    @Bean
    fun auditEventsQueue(): Queue {
        return QueueBuilder
            .durable(AUDIT_EVENTS_QUEUE)
            .withArgument("x-message-ttl", 604800000) // 7 days
            .withArgument("x-max-length", 100000)
            .build()
    }

    @Bean
    fun auditSecurityQueue(): Queue {
        return QueueBuilder
            .durable(AUDIT_SECURITY_QUEUE)
            .withArgument("x-message-ttl", 2592000000L) // 30 days
            .withArgument("x-max-length", 50000)
            .build()
    }

    @Bean
    fun deadLetterQueue(): Queue {
        return QueueBuilder
            .durable(DEAD_LETTER_QUEUE)
            .withArgument("x-message-ttl", 86400000) // 24 hours
            .withArgument("x-max-length", 10000)
            .build()
    }

    // Bindings
    @Bean
    fun redemptionCreatedBinding(): Binding {
        return BindingBuilder
            .bind(redemptionCreatedQueue())
            .to(redemptionExchange())
            .with(REDEMPTION_CREATED_KEY)
    }

    @Bean
    fun redemptionCompletedBinding(): Binding {
        return BindingBuilder
            .bind(redemptionCompletedQueue())
            .to(redemptionExchange())
            .with(REDEMPTION_COMPLETED_KEY)
    }

    @Bean
    fun redemptionVoidedBinding(): Binding {
        return BindingBuilder
            .bind(redemptionVoidedQueue())
            .to(redemptionExchange())
            .with(REDEMPTION_VOIDED_KEY)
    }

    @Bean
    fun raffleTicketsGeneratedFromRedemptionBinding(): Binding {
        return BindingBuilder
            .bind(raffleTicketsGeneratedQueue())
            .to(redemptionExchange())
            .with(RAFFLE_TICKETS_GENERATED_KEY)
    }

    @Bean
    fun raffleTicketsGeneratedFromAdBinding(): Binding {
        return BindingBuilder
            .bind(raffleTicketsGeneratedQueue())
            .to(adExchange())
            .with(RAFFLE_TICKETS_GENERATED_KEY)
    }

    @Bean
    fun adEngagementCompletedBinding(): Binding {
        return BindingBuilder
            .bind(adEngagementCompletedQueue())
            .to(adExchange())
            .with(AD_ENGAGEMENT_COMPLETED_KEY)
    }

    @Bean
    fun adTicketsMultipliedBinding(): Binding {
        return BindingBuilder
            .bind(adTicketsMultipliedQueue())
            .to(adExchange())
            .with(AD_TICKETS_MULTIPLIED_KEY)
    }

    @Bean
    fun raffleEntryCreatedBinding(): Binding {
        return BindingBuilder
            .bind(raffleEntryCreatedQueue())
            .to(raffleExchange())
            .with(RAFFLE_ENTRY_CREATED_KEY)
    }

    @Bean
    fun raffleWinnerSelectedBinding(): Binding {
        return BindingBuilder
            .bind(raffleWinnerSelectedQueue())
            .to(raffleExchange())
            .with(RAFFLE_WINNER_SELECTED_KEY)
    }

    @Bean
    fun couponValidatedBinding(): Binding {
        return BindingBuilder
            .bind(couponValidatedQueue())
            .to(couponExchange())
            .with(COUPON_VALIDATED_KEY)
    }

    @Bean
    fun couponRedeemedBinding(): Binding {
        return BindingBuilder
            .bind(couponRedeemedQueue())
            .to(couponExchange())
            .with(COUPON_REDEEMED_KEY)
    }

    @Bean
    fun auditEventsBinding(): Binding {
        return BindingBuilder
            .bind(auditEventsQueue())
            .to(auditExchange())
            .with("audit.#")
    }

    @Bean
    fun auditSecurityBinding(): Binding {
        return BindingBuilder
            .bind(auditSecurityQueue())
            .to(auditExchange())
            .with("audit.security.#")
    }

    @Bean
    fun deadLetterBinding(): Binding {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("#")
    }
}