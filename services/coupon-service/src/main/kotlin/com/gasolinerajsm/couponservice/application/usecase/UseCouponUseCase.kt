package com.gasolinerajsm.couponservice.application.usecase

import com.gasolinerajsm.couponservice.application.port.`in`.UseCouponCommand
import com.gasolinerajsm.couponservice.application.port.out.*
import com.gasolinerajsm.couponservice.domain.model.Coupon
import com.gasolinerajsm.couponservice.domain.repository.CouponRepository
import com.gasolinerajsm.couponservice.domain.service.CouponDomainService
import com.gasolinerajsm.couponservice.domain.valueobject.CouponCode
import java.math.BigDecimal

/**
 * Use case for using/redeeming a coupon
 */
class UseCouponUseCase(
    private val couponRepository: CouponRepository,
    private val couponDomainService: CouponDomainService,
    private val validationService: ValidationService,
    private val analyticsService: AnalyticsService,
    private val notificationService: NotificationService,
    private val eventPublisher: EventPublisher
) {

    suspend fun execute(command: UseCouponCommand): Result<CouponUsageResult> {
        return try {
            // Find coupon by code
            val couponCode = CouponCode.from(command.couponCode)
            val couponResult = couponRepository.findByCouponCode(couponCode)

            if (couponResult.isFailure) {
                return Result.failure(
                    couponResult.exceptionOrNull()
                        ?: Exception("Failed to find coupon")
                )
            }

            val coupon = couponResult.getOrNull()
                ?: return Result.failure(
                    NoSuchElementException("Coupon not found: ${command.couponCode}")
                )

            // Validate external dependencies
            val externalValidation = validateExternalDependencies(command)
            if (externalValidation.isFailure) {
                return externalValidation
            }

            // Get user's previous usage count for this coupon
            val userPreviousUsages = getUserPreviousUsages(command.userId, coupon)

            // Validate coupon usage context
            val contextValidation = couponDomainService.validateUsageContext(
                coupon = coupon,
                purchaseAmount = command.purchaseAmount,
                fuelType = command.fuelType,
                stationId = command.stationId,
                userId = command.userId,
                userPreviousUsages = userPreviousUsages
            )

            if (!contextValidation.isSuccess) {
                // Record failed usage attempt
                recordFailedUsage(coupon, command, contextValidation.message)
                return Result.failure(IllegalStateException(contextValidation.message))
            }

            // Use the coupon
            val usedCoupon = coupon.use(
                purchaseAmount = command.purchaseAmount,
                fuelType = command.fuelType,
                stationId = command.stationId,
                usedBy = command.userId
            )

            // Calculate discount and raffle tickets
            val discountApplied = coupon.calculateDiscount(command.purchaseAmount)
            val raffleTickets = couponDomainService.calculateRaffleTickets(
                purchaseAmount = command.purchaseAmount,
                discountApplied = discountApplied,
                baseTickets = coupon.raffleTickets
            )

            // Save updated coupon
            val savedCouponResult = couponRepository.save(usedCoupon)
            if (savedCouponResult.isFailure) {
                return Result.failure(
                    savedCouponResult.exceptionOrNull()
                        ?: Exception("Failed to save used coupon")
                )
            }

            val savedCoupon = savedCouponResult.getOrThrow()

            // Publish domain events
            val events = savedCoupon.getUncommittedEvents()
            if (events.isNotEmpty()) {
                eventPublisher.publishAll(events)
                savedCoupon.markEventsAsCommitted()
            }

            // Record analytics
            analyticsService.recordCouponUsage(
                couponId = coupon.id,
                campaignId = coupon.campaignId,
                purchaseAmount = command.purchaseAmount,
                discountApplied = discountApplied,
                stationId = command.stationId,
                fuelType = command.fuelType,
                timestamp = java.time.LocalDateTime.now()
            )

            // Send notification
            notificationService.notifyCouponUsed(
                couponId = coupon.id,
                couponCode = coupon.couponCode.value,
                discountApplied = discountApplied,
                stationId = command.stationId,
                userId = command.userId
            )

            val result = CouponUsageResult(
                couponId = coupon.id.toString(),
                couponCode = coupon.couponCode.value,
                discountApplied = discountApplied,
                finalAmount = command.purchaseAmount - discountApplied,
                raffleTicketsEarned = raffleTickets,
                remainingUses = savedCoupon.getRemainingUses(),
                usageTimestamp = java.time.LocalDateTime.now(),
                transactionId = command.transactionId
            )

            Result.success(result)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun validateExternalDependencies(command: UseCouponCommand): Result<CouponUsageResult> {
        // Validate transaction if provided
        command.transactionId?.let { transactionId ->
            val transactionValidation = validationService.validatePurchaseTransaction(
                transactionId = transactionId,
                purchaseAmount = command.purchaseAmount,
                stationId = command.stationId
            )

            if (transactionValidation.isFailure || transactionValidation.getOrDefault(false) == false) {
                return Result.failure(
                    IllegalArgumentException("Invalid purchase transaction: $transactionId")
                )
            }
        }

        // Validate user eligibility if provided
        command.userId?.let { userId ->
            val userValidation = validationService.validateUserEligibility(
                userId = userId,
                couponId = com.gasolinerajsm.couponservice.domain.valueobject.CouponId.generate() // This would be the actual coupon ID
            )

            if (userValidation.isFailure || userValidation.getOrDefault(false) == false) {
                return Result.failure(
                    IllegalArgumentException("User not eligible to use coupon: $userId")
                )
            }
        }

        // Validate station operational status if provided
        command.stationId?.let { stationId ->
            val stationValidation = validationService.validateStationOperational(stationId)

            if (stationValidation.isFailure || stationValidation.getOrDefault(false) == false) {
                return Result.failure(
                    IllegalArgumentException("Station not operational: $stationId")
                )
            }
        }

        return Result.success(CouponUsageResult("", "", BigDecimal.ZERO, BigDecimal.ZERO, 0, null, java.time.LocalDateTime.now(), null))
    }

    private suspend fun getUserPreviousUsages(userId: String?, coupon: Coupon): Int {
        // This would typically query a usage tracking system
        // For now, return 0 as a placeholder
        return 0
    }

    private suspend fun recordFailedUsage(coupon: Coupon, command: UseCouponCommand, reason: String) {
        // Record failed usage attempt for analytics and fraud detection
        val failedEvent = com.gasolinerajsm.couponservice.domain.event.CouponUsageFailedEvent(
            couponId = coupon.id,
            couponCode = coupon.couponCode,
            failureReason = reason,
            purchaseAmount = command.purchaseAmount,
            stationId = command.stationId,
            attemptedBy = command.userId
        )

        eventPublisher.publish(failedEvent)
    }
}

/**
 * Result of coupon usage operation
 */
data class CouponUsageResult(
    val couponId: String,
    val couponCode: String,
    val discountApplied: BigDecimal,
    val finalAmount: BigDecimal,
    val raffleTicketsEarned: Int,
    val remainingUses: Int?,
    val usageTimestamp: java.time.LocalDateTime,
    val transactionId: String?
)