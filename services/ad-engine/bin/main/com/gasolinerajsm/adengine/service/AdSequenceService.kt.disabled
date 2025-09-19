package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.domain.AdDurationConfig
import com.gasolinerajsm.adengine.domain.AdSequence
import com.gasolinerajsm.adengine.domain.SequenceStatus
import com.gasolinerajsm.adengine.dto.StartAdSequenceRequest
import com.gasolinerajsm.adengine.dto.CompleteAdRequest
import com.gasolinerajsm.adengine.dto.AdSequenceResponse
import com.gasolinerajsm.adengine.repository.AdSequenceRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class AdSequenceService(
    private val adSequenceRepository: AdSequenceRepository,
    private val advertisementService: AdvertisementService,
    private val rabbitTemplate: RabbitTemplate
) {

    fun startAdSequence(request: StartAdSequenceRequest): AdSequenceResponse {
        // Verificar si ya existe una secuencia activa para este cupón
        val existingSequence = adSequenceRepository.findByCouponIdAndStatus(
            request.couponId,
            SequenceStatus.ACTIVE
        )

        if (existingSequence != null) {
            return buildSequenceResponse(existingSequence)
        }

        // Crear nueva secuencia
        val sequence = AdSequence(
            couponId = request.couponId,
            userId = request.userId,
            baseTickets = request.baseTickets,
            currentTickets = request.baseTickets
        )

        val savedSequence = adSequenceRepository.save(sequence)
        return buildSequenceResponse(savedSequence)
    }

    fun completeAdStep(request: CompleteAdRequest): AdSequenceResponse {
        val sequence = adSequenceRepository.findById(request.sequenceId)
            .orElseThrow { IllegalArgumentException("Secuencia no encontrada") }

        if (sequence.userId != request.userId) {
            throw IllegalArgumentException("No tienes permisos para esta secuencia")
        }

        if (sequence.status != SequenceStatus.ACTIVE) {
            throw IllegalStateException("Esta secuencia no está activa")
        }

        // Calcular tickets ganados en este paso
        val ticketsEarned = sequence.baseTickets * AdDurationConfig.getTicketMultiplier(sequence.currentStep)
        val newTotalTickets = sequence.currentTickets + ticketsEarned

        // Actualizar secuencia
        val updatedSequence = if (sequence.currentStep >= sequence.maxSteps) {
            // Secuencia completada
            sequence.copy(
                status = SequenceStatus.COMPLETED,
                currentTickets = newTotalTickets,
                completedAt = LocalDateTime.now()
            )
        } else {
            // Continuar con siguiente paso
            sequence.copy(
                currentStep = sequence.currentStep + 1,
                currentTickets = newTotalTickets
            )
        }

        val savedSequence = adSequenceRepository.save(updatedSequence)

        // Publicar evento de tickets ganados
        rabbitTemplate.convertAndSend(
            "coupon.exchange",
            "ad.completed",
            mapOf(
                "couponId" to savedSequence.couponId,
                "userId" to savedSequence.userId,
                "step" to sequence.currentStep,
                "ticketsEarned" to ticketsEarned,
                "totalTickets" to newTotalTickets,
                "sequenceCompleted" to (savedSequence.status == SequenceStatus.COMPLETED)
            )
        )

        return buildSequenceResponse(savedSequence)
    }

    fun abandonSequence(sequenceId: UUID, userId: UUID): AdSequenceResponse {
        val sequence = adSequenceRepository.findById(sequenceId)
            .orElseThrow { IllegalArgumentException("Secuencia no encontrada") }

        if (sequence.userId != userId) {
            throw IllegalArgumentException("No tienes permisos para esta secuencia")
        }

        val updatedSequence = sequence.copy(
            status = SequenceStatus.ABANDONED,
            completedAt = LocalDateTime.now()
        )

        val savedSequence = adSequenceRepository.save(updatedSequence)

        // Publicar evento de secuencia abandonada
        rabbitTemplate.convertAndSend(
            "coupon.exchange",
            "sequence.abandoned",
            mapOf(
                "couponId" to savedSequence.couponId,
                "userId" to savedSequence.userId,
                "stepsCompleted" to savedSequence.currentStep - 1,
                "finalTickets" to savedSequence.currentTickets
            )
        )

        return buildSequenceResponse(savedSequence)
    }

    fun getActiveSequence(userId: UUID, couponId: UUID): AdSequenceResponse? {
        val sequence = adSequenceRepository.findByCouponIdAndUserIdAndStatus(
            couponId, userId, SequenceStatus.ACTIVE
        ) ?: return null

        return buildSequenceResponse(sequence)
    }

    private fun buildSequenceResponse(sequence: AdSequence): AdSequenceResponse {
        val nextAd = if (sequence.status == SequenceStatus.ACTIVE && sequence.currentStep <= sequence.maxSteps) {
            advertisementService.getAdForStep(sequence.currentStep)
        } else null

        val nextDuration = if (sequence.status == SequenceStatus.ACTIVE && sequence.currentStep <= sequence.maxSteps) {
            AdDurationConfig.getDuration(sequence.currentStep)
        } else 0

        val potentialTickets = if (sequence.status == SequenceStatus.ACTIVE && sequence.currentStep <= sequence.maxSteps) {
            sequence.baseTickets * AdDurationConfig.getTicketMultiplier(sequence.currentStep)
        } else 0

        return AdSequenceResponse(
            sequenceId = sequence.id,
            couponId = sequence.couponId,
            currentStep = sequence.currentStep,
            maxSteps = sequence.maxSteps,
            currentTickets = sequence.currentTickets,
            status = sequence.status,
            nextAd = nextAd,
            nextAdDuration = nextDuration,
            potentialTicketsFromNextAd = potentialTickets,
            canContinue = sequence.status == SequenceStatus.ACTIVE && sequence.currentStep <= sequence.maxSteps,
            isCompleted = sequence.status == SequenceStatus.COMPLETED
        )
    }
}