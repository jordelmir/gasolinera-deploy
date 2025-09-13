package com.gasolinerajsm.coupon.service

import com.gasolinerajsm.coupon.entity.Campaign
import com.gasolinerajsm.coupon.dto.CampaignRequest
import com.gasolinerajsm.coupon.dto.CampaignResponse
import com.gasolinerajsm.coupon.repository.CampaignRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CampaignService(private val campaignRepository: CampaignRepository) {

    @Transactional
    fun createCampaign(request: CampaignRequest): CampaignResponse {
        val campaign = Campaign(
            name = request.name,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            isActive = request.isActive ?: true,
            discountPercentage = request.discountPercentage
        )
        return campaignRepository.save(campaign).toResponse()
    }

    fun getCampaignById(id: Long): CampaignResponse? {
        return campaignRepository.findByIdOrNull(id)?.toResponse()
    }

    fun getAllCampaigns(): List<CampaignResponse> {
        return campaignRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun updateCampaign(id: Long, request: CampaignRequest): CampaignResponse? {
        return campaignRepository.findByIdOrNull(id)?.let { existingCampaign ->
            val updatedCampaign = existingCampaign.copy(
                name = request.name,
                description = request.description ?: existingCampaign.description,
                startDate = request.startDate,
                endDate = request.endDate,
                isActive = request.isActive ?: existingCampaign.isActive,
                discountPercentage = request.discountPercentage ?: existingCampaign.discountPercentage
            )
            campaignRepository.save(updatedCampaign).toResponse()
        }
    }

    @Transactional
    fun deleteCampaign(id: Long) {
        campaignRepository.deleteById(id)
    }

    // Extension function to convert Campaign entity to CampaignResponse DTO
    fun Campaign.toResponse(): CampaignResponse {
        return CampaignResponse(
            id = this.id,
            name = this.name,
            description = this.description,
            startDate = this.startDate,
            endDate = this.endDate,
            isActive = this.isActive,
            discountPercentage = this.discountPercentage,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
