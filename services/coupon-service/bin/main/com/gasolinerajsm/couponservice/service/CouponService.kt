package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CouponStatus
import com.gasolinerajsm.couponservice.repository.CouponRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing coupons
 */
@Service
@Transactional
class CouponService(
    private val couponRepository: CouponRepository
) {

    private val logger = LoggerFactory.getLogger(CouponService::class.java)

    /**
     * Create a new coupon
     */
    fun createCoupon(coupon: Coupon): Coupon {
        // Validate coupon code uniqueness
        if (couponRepository.existsByCode(coupon.code)) {
            throw IllegalArgumentException("Coupon code '${coupon.code}' already exists")
        }

        val savedCoupon = couponRepository.save(coupon)
        logger.info("Created new coupon: {} with code: {}", savedCoupon.name, savedCoupon.code)
        return savedCoupon
    }

    /**
     * Get coupon by ID
     */
    @Transactional(readOnly = true)
    fun getCouponById(couponId: Long): Coupon {
        return couponRepository.findById(couponId).orElseThrow {
            CouponNotFoundException("Coupon not found with ID: $couponId")
        }
    }

    /**
     * Get coupon by code
     */
    @Transactional(readOnly = true)
    fun getCouponByCode(code: String): Coupon? {
        return couponRepository.findByCode(code)
    }

    /**
     * Get all coupons
     */
    @Transactional(readOnly = true)
    fun getAllCoupons(): List<Coupon> {
        return couponRepository.findAll()
    }

    /**
     * Get coupons by status
     */
    @Transactional(readOnly = true)
    fun getCouponsByStatus(status: CouponStatus): List<Coupon> {
        return couponRepository.findByStatus(status)
    }

    /**
     * Get active coupons
     */
    @Transactional(readOnly = true)
    fun getActiveCoupons(): List<Coupon> {
        return couponRepository.findByStatus(CouponStatus.ACTIVE)
    }

    /**
     * Get coupons by name (search)
     */
    @Transactional(readOnly = true)
    fun getCouponsByName(name: String): List<Coupon> {
        return couponRepository.findByNameContainingIgnoreCase(name)
    }

    /**
     * Update coupon
     */
    fun updateCoupon(couponId: Long, updatedCoupon: Coupon): Coupon {
        val existingCoupon = getCouponById(couponId)

        // Check if code is being changed and if new code already exists
        if (existingCoupon.code != updatedCoupon.code &&
            couponRepository.existsByCode(updatedCoupon.code)) {
            throw IllegalArgumentException("Coupon code '${updatedCoupon.code}' already exists")
        }

        val couponToSave = updatedCoupon.copy(id = couponId)
        val savedCoupon = couponRepository.save(couponToSave)
        logger.info("Updated coupon: {} (ID: {})", savedCoupon.name, savedCoupon.id)
        return savedCoupon
    }

    /**
     * Delete coupon
     */
    fun deleteCoupon(couponId: Long): Boolean {
        return try {
            couponRepository.deleteById(couponId)
            logger.info("Deleted coupon: {}", couponId)
            true
        } catch (e: Exception) {
            logger.error("Error deleting coupon {}: {}", couponId, e.message, e)
            false
        }
    }

    /**
     * Use coupon (increment usage count)
     */
    fun useCoupon(couponId: Long): Coupon {
        val coupon = getCouponById(couponId)

        if (!coupon.canBeUsed()) {
            throw IllegalStateException("Coupon cannot be used: ${coupon.code}")
        }

        val newUses = coupon.currentUses + 1
        val newStatus = if (newUses >= coupon.maxUses) CouponStatus.USED_UP else coupon.status

        val updatedCoupon = coupon.copy(
            currentUses = newUses,
            status = newStatus
        )

        val savedCoupon = couponRepository.save(updatedCoupon)
        logger.info("Used coupon: {} (uses: {}/{})", savedCoupon.code, savedCoupon.currentUses, savedCoupon.maxUses)
        return savedCoupon
    }
}

/**
 * Custom exceptions for coupon service
 */
class CouponServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class CouponNotFoundException(message: String) : RuntimeException(message)