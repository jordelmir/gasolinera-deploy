package com.gasolinerajsm.adengine.controller

import com.gasolinerajsm.adengine.service.CampaignPerformanceSummaryDto
import com.gasolinerajsm.adengine.service.CampaignService
import jakarta.validation.Valid
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.time.LocalDate

// DTOs
data class CampaignDto(val id: Long, val name: String, val startDate: LocalDate, val endDate: LocalDate, val budget: Double, val adUrl: String)

data class CreateCampaignDto(
    @field:NotBlank(message = "Campaign name cannot be blank")
    val name: String,

    @field:NotNull(message = "Start date cannot be null")
    @field:FutureOrPresent(message = "Start date must be in the present or future")
    val startDate: LocalDate,

    @field:NotNull(message = "End date cannot be null")
    val endDate: LocalDate,

    @field:Min(value = 0, message = "Budget must be positive")
    val budget: Double,

    @field:NotBlank(message = "Ad URL cannot be blank")
    val adUrl: String
)

@RestController
@RequestMapping("/api/v1/campaigns")
@PreAuthorize("hasRole('ADVERTISER')")
class CampaignController(private val campaignService: CampaignService) {

    @GetMapping
    fun getMyCampaigns(principal: Principal): List<CampaignDto> {
        val advertiserId = principal.name
        return campaignService.getCampaignsForAdvertiser(advertiserId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCampaign(@Valid @RequestBody campaignDto: CreateCampaignDto, principal: Principal): CampaignDto {
        val advertiserId = principal.name
        return campaignService.createCampaign(advertiserId, campaignDto)
    }

    @GetMapping("/summary")
    fun getCampaignPerformanceSummary(principal: Principal): CampaignPerformanceSummaryDto {
        val advertiserId = principal.name
        return campaignService.getPerformanceSummaryForAdvertiser(advertiserId)
    }

    // PUT and DELETE endpoints would follow a similar pattern
}
