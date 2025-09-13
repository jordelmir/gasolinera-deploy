package com.gasolinerajsm.couponservice.application.usecase

import com.gasolinerajsm.couponservice.application.port.`in`.CreateCampaignCommand
import com.gasolinerajsm.couponservice.application.port.out.EventPublisher
import com.gasolinerajsm.couponservice.domain.model.Campaign
import com.gasolinerajsm.couponservice.domain.repository.CampaignRepository
import com.gasolinerajsm.couponservice.domain.service.CouponDomainService

/**
 * Use case for creating a new campaign
 */
class CreateCampaignUseCase(
    private val campaignRepository: CampaignRepository,
    private val couponDomainService: CouponDomainService,
    private val eventPublisher: EventPublisher
) {

    suspend fun execute(command: CreateCampaignCommand): Result<Campaign> {
        return try {
            // Validate campaign name uniqueness
            val existingCampaign = campaignRepository.findByName(command.name)
            if (existingCampaign.isSuccess && existingCampaign.getOrNull() != null) {
                return Result.failure(
                    IllegalArgumentException("Campaign with name '${command.name}' already exists")
                )
            }

            // Validate campaign creation data
            val validationResult = couponDomainService.validateCouponCreation(
                campaignId = com.gasolinerajsm.couponservice.domain.valueobject.CampaignId.generate(), // Temporary for validation
                discountValue = command.getDiscountValue(),
                validityPeriod = command.getValidityPeriod(),
                usageRules = command.getUsageRules(),
                applicabilityRules = command.getApplicabilityRules()
            )

            if (!validationResult.isSuccess) {
                return Result.failure(IllegalArgumentException(validationResult.message))
            }

            // Create campaign entity
            val campaign = Campaign.create(
                name = command.name,
                description = command.description,
                validityPeriod = command.getValidityPeriod(),
                defaultDiscountValue = command.getDiscountValue(),
                defaultRaffleTickets = command.defaultRaffleTickets,
                generationStrategy = command.generationStrategy,
                targetCouponCount = command.targetCouponCount,
                applicabilityRules = command.getApplicabilityRules(),
                usageRules = command.getUsageRules(),
                metadata = command.metadata
            )

            // Save campaign
            val savedCampaignResult = campaignRepository.save(campaign)
            if (savedCampaignResult.isFailure) {
                return Result.failure(
                    savedCampaignResult.exceptionOrNull()
                        ?: Exception("Failed to save campaign")
                )
            }

            val savedCampaign = savedCampaignResult.getOrThrow()

            // Publish domain events
            val events = savedCampaign.getUncommittedEvents()
            if (events.isNotEmpty()) {
                eventPublisher.publishAll(events)
                savedCampaign.markEventsAsCommitted()
            }

            Result.success(savedCampaign)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}