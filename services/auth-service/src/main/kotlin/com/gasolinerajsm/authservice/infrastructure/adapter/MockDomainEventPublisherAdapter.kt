package com.gasolinerajsm.authservice.infrastructure.adapter

import com.gasolinerajsm.authservice.application.port.out.DomainEventPublisher
import com.gasolinerajsm.authservice.domain.event.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Mock Domain Event Publisher implementation for development and testing
 */
@Service
class MockDomainEventPublisherAdapter : DomainEventPublisher {

    private val logger = LoggerFactory.getLogger(MockDomainEventPublisherAdapter::class.java)

    override suspend fun publish(event: DomainEvent): Result<Unit> {
        return try {
            logger.info("Publishing domain event: {} - {}", event::class.simpleName, event)
            // In a real implementation, this would publish to a message broker
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to publish domain event: {}", event, e)
            Result.failure(e)
        }
    }

    override suspend fun publishAll(events: List<DomainEvent>): Result<Unit> {
        return try {
            events.forEach { event ->
                logger.info("Publishing domain event: {} - {}", event::class.simpleName, event)
            }
            logger.info("Published {} domain events", events.size)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to publish domain events: {}", events, e)
            Result.failure(e)
        }
    }
}