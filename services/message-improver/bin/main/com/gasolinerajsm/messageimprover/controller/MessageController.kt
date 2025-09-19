package com.gasolinerajsm.messageimprover.controller

import com.gasolinerajsm.messageimprover.dto.MessageImprovementRequest
import com.gasolinerajsm.messageimprover.dto.MessageImprovementResponse
import com.gasolinerajsm.messageimprover.service.MessageImprovementService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/messages")
class MessageController(
    private val messageImprovementService: MessageImprovementService
) {
    private val logger = LoggerFactory.getLogger(MessageController::class.java)

    @PostMapping("/improve")
    fun improveMessage(@RequestBody request: MessageImprovementRequest): ResponseEntity<MessageImprovementResponse> {
        logger.info("Received message improvement request for message: ${request.message.take(50)}...")

        return try {
            val response = messageImprovementService.improveMessage(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error processing message improvement request", e)
            ResponseEntity.internalServerError().body(
                MessageImprovementResponse(
                    originalMessage = request.message,
                    improvedMessage = request.message,
                    improvements = listOf("Error interno del servidor"),
                    confidence = 0.0
                )
            )
        }
    }
}