package com.gasolinerajsm.adengine.controller

import com.gasolinerajsm.adengine.dto.AdCreativeResponse
import com.gasolinerajsm.adengine.dto.AdSelectionRequest
import com.gasolinerajsm.adengine.dto.ImpressionRequest
import com.gasolinerajsm.adengine.model.AdImpression
import com.gasolinerajsm.adengine.repository.AdImpressionRepository
import com.gasolinerajsm.adengine.service.AdSelectionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page

@RestController
@RequestMapping("/ad")
class AdController(
    private val adSelectionService: AdSelectionService,
    private val adImpressionRepository: AdImpressionRepository
) {

    private val logger = LoggerFactory.getLogger(AdController::class.java)

    @PostMapping("/select")
    fun selectAd(@Valid @RequestBody request: AdSelectionRequest): ResponseEntity<AdCreativeResponse> {
        val adCreative = adSelectionService.selectAd(request)
        return ResponseEntity.ok(adCreative)
    }

    @PostMapping("/impression")
    fun recordImpression(@Valid @RequestBody request: ImpressionRequest): ResponseEntity<Void> {
        val adImpression = AdImpression(
            userId = request.userId,
            campaignId = request.campaignId,
            creativeId = request.creativeId,
            stationId = request.stationId,
            sessionId = request.sessionId,
            sequenceId = request.sequenceId,
            duration = request.duration,
            completed = request.completed,
            skipped = request.skipped
        )
        adImpressionRepository.save(adImpression)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/impressions")
    fun getImpressions(
        @RequestParam(required = false) campaignId: Long?,
        pageable: Pageable
    ): ResponseEntity<Page<AdImpression>> {
        logger.info("Received request to get impressions. CampaignId filter: {}, Pageable: {}", campaignId ?: "none", pageable)
        val impressions = if (campaignId != null) {
            adImpressionRepository.findByCampaignId(campaignId, pageable)
        } else {
            adImpressionRepository.findAll(pageable)
        }
        return ResponseEntity.ok(impressions)
    }

    @GetMapping("/impressions/count")
    fun countImpressions(): ResponseEntity<Long> {
        val count = adImpressionRepository.count()
        logger.info("Total impressions count requested: {}", count)
        return ResponseEntity.ok(count)
    }
}
