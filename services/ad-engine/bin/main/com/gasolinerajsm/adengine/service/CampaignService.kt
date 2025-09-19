
package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.controller.CampaignDto
import com.gasolinerajsm.adengine.controller.CreateCampaignDto
import com.gasolinerajsm.adengine.repository.AdCampaign
import com.gasolinerajsm.adengine.repository.AdCampaignRepository
import com.gasolinerajsm.adengine.repository.AdImpressionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CampaignPerformanceSummaryDto(
    val totalImpressions: Long,
    val totalBudgetSpent: Double
)

@Service
class CampaignService(
    private val adCampaignRepository: AdCampaignRepository,
    private val adImpressionRepository: AdImpressionRepository
) {

    fun getCampaignsForAdvertiser(advertiserId: String): List<CampaignDto> {
        return adCampaignRepository.findByAdvertiserId(advertiserId).map { it.toDto() }
    }

    @Transactional
    fun createCampaign(advertiserId: String, dto: CreateCampaignDto): CampaignDto {
        val campaign = AdCampaign(
            name = dto.name,
            advertiserId = advertiserId,
            startDate = dto.startDate,
            endDate = dto.endDate,
            budget = dto.budget,
            adUrl = dto.adUrl
        )
        val savedCampaign = adCampaignRepository.save(campaign)
        return savedCampaign.toDto()
    }

    fun getPerformanceSummaryForAdvertiser(advertiserId: String): CampaignPerformanceSummaryDto {
        // TODO: Implement real aggregation logic
        // For now, return mock data
        val campaigns = adCampaignRepository.findByAdvertiserId(advertiserId)
        val totalBudgetSpent = campaigns.sumOf { it.budget }
        val totalImpressions = adImpressionRepository.countByAdvertiserId(advertiserId) // Assuming this method exists

        return CampaignPerformanceSummaryDto(
            totalImpressions = totalImpressions,
            totalBudgetSpent = totalBudgetSpent
        )
    }
}

fun AdCampaign.toDto(): CampaignDto = CampaignDto(
    id = this.id!!,
    name = this.name,
    startDate = this.startDate,
    endDate = this.endDate,
    budget = this.budget,
    adUrl = this.adUrl
)
