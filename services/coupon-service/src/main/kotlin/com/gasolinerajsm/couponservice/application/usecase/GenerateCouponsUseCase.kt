package com.gasolinerajsm.couponservice.application.usecase

import com.gasolinerajsm.couponservice.application.port.`in`.GenerateCouponsCommand
import com.gasolinerajsm.couponservice.application.port.out.EventPublisher
import com.gasolinerajsm.couponservice.application.port.out.QRCodeService
import com.gasolinerajsm.couponservice.application.port.out.SecurityService
import com.gasolinerajsm.couponservice.domain.model.Coupon
import com.gasolinerajsm.couponservice.domain.repository.CampaignRepository
import com.gasolinerajsm.couponservice.domain.repository.CouponRepository
import com.gasolinerajsm.couponservice.domain.service.CouponDomainService
import com.gasolinerajsm.couponservice.domain.valueobject.CouponCode
import com.gasolinerajsm.couponservice.domain.valueobject.QRCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Use case for generating coupons for a campaign
 */
class GenerateCouponsUseCase(
    private val campaignRepository: CampaignRepository,
    private val couponRepository: CouponRepository,
    private val couponDomainService: CouponDomainService,
    private val qrCodeService: QRCodeService,
    private val securityService: SecurityService,
    private val eventPublisher: EventPublisher
) {

    suspend fun execute(command: GenerateCouponsCommand): Result<List<Coupon>> {
        return try {
            // Find and validate campaign
            val campaignResult = campaignRepository.findById(command.campaignId)
            if (campaignResult.isFailure) {
                return Result.failure(
                    campaignResult.exceptionOrNull()
                        ?: Exception("Failed to find campaign")
                )
            }

            val campaign = campaignResult.getOrNull()
                ?: return Result.failure(
                    NoSuchElementException("Campaign not found: ${command.campaignId}")
                )

            // Check if campaign can generate coupons
            if (!campaign.canGenerateCoupons()) {
                return Result.failure(
                    IllegalStateException("Campaign cannot generate coupons: ${campaign.status}")
                )
            }

            // Check if quantity exceeds remaining capacity
            val remainingCapacity = campaign.getRemainingCapacity()
            if (remainingCapacity != null && command.quantity > remainingCapacity) {
                return Result.failure(
                    IllegalArgumentException("Requested quantity exceeds remaining capacity: $remainingCapacity")
                )
            }

            // Generate coupons in parallel
            val coupons = generateCouponsInParallel(command, campaign)

            // Save all coupons
            val savedCoupons = mutableListOf<Coupon>()
            val allEvents = mutableListOf<com.gasolinerajsm.couponservice.domain.event.DomainEvent>()

            for (coupon in coupons) {
                val savedCouponResult = couponRepository.save(coupon)
                if (savedCouponResult.isSuccess) {
                    val savedCoupon = savedCouponResult.getOrThrow()
                    savedCoupons.add(savedCoupon)
                    allEvents.addAll(savedCoupon.getUncommittedEvents())
                    savedCoupon.markEventsAsCommitted()
                }
            }

            // Update campaign counters
            val updatedCampaign = campaign.incrementGeneratedCount(savedCoupons.size)
            campaignRepository.save(updatedCampaign)

            // Publish all domain events
            if (allEvents.isNotEmpty()) {
                eventPublisher.publishAll(allEvents)
            }

            Result.success(savedCoupons)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateCouponsInParallel(
        command: GenerateCouponsCommand,
        campaign: com.gasolinerajsm.couponservice.domain.model.Campaign
    ): List<Coupon> = coroutineScope {
        (1..command.quantity).map { index ->
            async {
                generateSingleCoupon(command, campaign, index)
            }
        }.awaitAll()
    }

    private suspend fun generateSingleCoupon(
        command: GenerateCouponsCommand,
        campaign: com.gasolinerajsm.couponservice.domain.model.Campaign,
        index: Int
    ): Coupon {
        // Generate unique coupon code
        var couponCode: CouponCode
        do {
            couponCode = CouponCode.generateWithCampaign(campaign.name)
        } while (couponRepository.existsByCouponCode(couponCode).getOrDefault(false))

        // Create QR code data
        val qrCodeData = createQRCodeData(couponCode, campaign)

        // Generate secure signature for QR code
        val signatureResult = securityService.generateSecureSignature(qrCodeData, "coupon-secret-key")
        val signature = signatureResult.getOrDefault("fallback-signature")

        val qrCode = QRCode(qrCodeData, signature)

        // Use custom values or campaign defaults
        val discountValue = command.customDiscountValue ?: campaign.defaultDiscountValue
        val validityPeriod = command.customValidityPeriod ?: campaign.validityPeriod
        val raffleTickets = command.customRaffleTickets ?: campaign.defaultRaffleTickets

        // Create coupon
        return Coupon.create(
            campaignId = campaign.id,
            couponCode = couponCode,
            qrCode = qrCode,
            discountValue = discountValue,
            raffleTickets = raffleTickets,
            validityPeriod = validityPeriod,
            usageRules = campaign.usageRules,
            applicabilityRules = campaign.applicabilityRules,
            metadata = mapOf(
                "batchId" to (command.batchId ?: "default"),
                "generationIndex" to index.toString(),
                "generatedAt" to java.time.LocalDateTime.now().toString()
            )
        )
    }

    private fun createQRCodeData(
        couponCode: CouponCode,
        campaign: com.gasolinerajsm.couponservice.domain.model.Campaign
    ): String {
        return buildString {
            append("COUPON:")
            append(couponCode.value)
            append("|CAMPAIGN:")
            append(campaign.id)
            append("|DISCOUNT:")
            append(campaign.defaultDiscountValue.getDisplayString())
            append("|VALID_UNTIL:")
            append(campaign.validityPeriod.validUntil)
        }
    }
}