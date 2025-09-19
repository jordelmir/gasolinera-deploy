package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.domain.AdSequence
import com.gasolinerajsm.adengine.repository.AdSequenceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Simplified Ad Sequence Service for basic functionality
 */
@Service
class AdSequenceServiceSimple(
    private val adSequenceRepository: AdSequenceRepository
) {

    private val logger = LoggerFactory.getLogger(AdSequenceServiceSimple::class.java)

    /**
     * Create a new ad sequence for a user
     */
    fun createSequence(userId: String, stationId: String): AdSequence {
        val sequence = AdSequence(
            id = UUID.randomUUID().toString(),
            userId = userId,
            stationId = stationId,
            currentStep = 1,
            totalSteps = 5,
            status = "ACTIVE",
            createdAt = LocalDateTime.now()
        )

        val savedSequence = adSequenceRepository.save(sequence)
        logger.info("Created new ad sequence {} for user {} at station {}",
            savedSequence.id, userId, stationId)

        return savedSequence
    }

    /**
     * Get active sequences for a user
     */
    fun getActiveSequences(userId: String): List<AdSequence> {
        return adSequenceRepository.findByUserIdAndStatus(userId, "ACTIVE")
    }

    /**
     * Complete a step in the sequence
     */
    fun completeStep(sequenceId: String): AdSequence? {
        val sequence = adSequenceRepository.findById(sequenceId).orElse(null)

        return if (sequence != null && sequence.status == "ACTIVE") {
            val updatedSequence = sequence.copy(
                currentStep = sequence.currentStep + 1,
                status = if (sequence.currentStep + 1 >= sequence.totalSteps) "COMPLETED" else "ACTIVE"
            )

            val savedSequence = adSequenceRepository.save(updatedSequence)
            logger.info("Completed step {} of sequence {}", savedSequence.currentStep, sequenceId)

            savedSequence
        } else {
            logger.warn("Sequence {} not found or not active", sequenceId)
            null
        }
    }

    /**
     * Get sequence statistics
     */
    fun getSequenceStats(): Map<String, Long> {
        return mapOf(
            "total" to adSequenceRepository.count(),
            "active" to adSequenceRepository.countByStatus("ACTIVE"),
            "completed" to adSequenceRepository.countByStatus("COMPLETED")
        )
    }
}