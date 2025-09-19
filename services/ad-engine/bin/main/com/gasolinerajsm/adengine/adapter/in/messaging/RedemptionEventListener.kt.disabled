package com.gasolinerajsm.adengine.adapter.in.messaging

import com.gasolinerajsm.adengine.dto.RedemptionInitiatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class RedemptionEventListener {

    private val logger = LoggerFactory.getLogger(RedemptionEventListener::class.java)

    @KafkaListener(topics = ["puntog.redemption.events"], groupId = "ad-engine")
    fun listen(event: RedemptionInitiatedEvent) {
        logger.info("Received RedemptionInitiatedEvent: redemptionId={}, userId={}, stationId={}",
            event.redemptionId, event.userId, event.stationId)
        // Future logic could go here, e.g., pre-selecting ads
    }
}
