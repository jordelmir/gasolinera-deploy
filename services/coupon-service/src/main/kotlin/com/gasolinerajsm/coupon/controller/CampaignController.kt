package com.gasolinerajsm.coupon.controller

import com.gasolinerajsm.coupon.dto.CampaignRequest
import com.gasolinerajsm.coupon.dto.CampaignResponse
import com.gasolinerajsm.coupon.service.CampaignService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/campaigns")
class CampaignController(private val campaignService: CampaignService) {

    @PostMapping
    fun createCampaign(@Valid @RequestBody request: CampaignRequest): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.createCampaign(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(campaign)
    }

    @GetMapping("/{id}")
    fun getCampaignById(@PathVariable id: Long): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.getCampaignById(id)
        return if (campaign != null) {
            ResponseEntity.ok(campaign)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun getAllCampaigns(): ResponseEntity<List<CampaignResponse>> {
        val campaigns = campaignService.getAllCampaigns()
        return ResponseEntity.ok(campaigns)
    }

    @PutMapping("/{id}")
    fun updateCampaign(@PathVariable id: Long, @Valid @RequestBody request: CampaignRequest): ResponseEntity<CampaignResponse> {
        val updatedCampaign = campaignService.updateCampaign(id, request)
        return if (updatedCampaign != null) {
            ResponseEntity.ok(updatedCampaign)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCampaign(@PathVariable id: Long): ResponseEntity<Void> {
        campaignService.deleteCampaign(id)
        return ResponseEntity.noContent().build()
    }
}
