package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.dto.AdCreativeResponse
import com.gasolinerajsm.adengine.dto.AdSelectionRequest
import com.gasolinerajsm.adengine.repository.CampaignRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class AdSelectionService(
    private val campaignRepository: CampaignRepository,
    @Value("\${ad.fallback.url}")
    private val fallbackAdUrl: String
) {

    private val logger = LoggerFactory.getLogger(AdSelectionService::class.java)

    fun selectAd(request: AdSelectionRequest): AdCreativeResponse {
        val activeCampaigns = campaignRepository.findActiveCampaignsForStation(
            request.stationId
        )

        return if (activeCampaigns.isNotEmpty()) {
            val selectedCampaign = activeCampaigns.first() // Simple selection logic
            logger.info("Selected campaign {} for station {} and user {}", selectedCampaign.id, request.stationId, request.userId)
            AdCreativeResponse(
                adUrl = selectedCampaign.adUrl,
                campaignId = selectedCampaign.id,
                creativeId = "creative-${selectedCampaign.id}"
            )
        } else {
            logger.warn("No active campaigns found for station {}. Serving fallback ad.", request.stationId)
            AdCreativeResponse(
                adUrl = fallbackAdUrl,
                campaignId = -1L,
                creativeId = "fallback-creative"
            )
        }
    }
}