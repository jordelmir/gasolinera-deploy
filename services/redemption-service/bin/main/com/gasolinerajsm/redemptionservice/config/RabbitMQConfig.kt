package com.gasolinerajsm.redemptionservice.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * RabbitMQ configuration for redemption service
 */
@Configuration
class RabbitMQConfig {

    companion object {
        const val REDEMPTION_EXCHANGE = "redemption.exchange"
        const val REDEMPTION_CREATED_ROUTING_KEY = "redemption.created"
        const val REDEMPTION_COMPLETED_ROUTING_KEY = "redemption.completed"
        const val REDEMPTION_VOIDED_ROUTING_KEY = "redemption.voided"
        const val REDEMPTION_EXPIRED_ROUTING_KEY = "redemption.expired"
        const val RAFFLE_TICKETS_GENERATED_ROUTING_KEY = "raffle.tickets.generated"
    }

    /**
     * Redemption exchange
     */
    @Bean
    fun redemptionExchange(): TopicExchange {
        return ExchangeBuilder
            .topicExchange(REDEMPTION_EXCHANGE)
            .durable(true)
            .build()
    }

    /**
     * JSON message converter
     */
    @Bean
    fun jsonMessageConverter(): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter()
    }

    /**
     * RabbitMQ template with JSON converter
     */
    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = jsonMessageConverter()
        // Publisher confirms and returns can be configured if needed
        return template
    }

    /**
     * Rabbit listener container factory
     */
    @Bean
    fun rabbitListenerContainerFactory(connectionFactory: ConnectionFactory): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(connectionFactory)
        factory.setMessageConverter(jsonMessageConverter())
        factory.setConcurrentConsumers(3)
        factory.setMaxConcurrentConsumers(10)
        factory.setPrefetchCount(10)
        return factory
    }
}