package com.gasolinerajsm.couponservice.controller

import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CouponStatus
import com.gasolinerajsm.couponservice.service.CouponService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for Coupon operations
 */
@RestController
@RequestMapping("/api/v1/coupons")
@CrossOrigin(origins = ["*"])
class CouponController(
    private val couponService: CouponService
) {

    /**
     * Create a new coupon
     */
    @PostMapping
    fun createCoupon(@RequestBody coupon: Coupon): ResponseEntity<Coupon> {
        return try {
            val createdCoupon = couponService.createCoupon(coupon)
            ResponseEntity.status(HttpStatus.CREATED).body(createdCoupon)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Get coupon by ID
     */
    @GetMapping("/{couponId}")
    fun getCoupon(@PathVariable couponId: Long): ResponseEntity<Coupon> {
        return try {
            val coupon = couponService.getCouponById(couponId)
            ResponseEntity.ok(coupon)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get coupon by code
     */
    @GetMapping("/code/{code}")
    fun getCouponByCode(@PathVariable code: String): ResponseEntity<Coupon> {
        val coupon = couponService.getCouponByCode(code)
        return if (coupon != null) {
            ResponseEntity.ok(coupon)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all coupons
     */
    @GetMapping
    fun getAllCoupons(): ResponseEntity<List<Coupon>> {
        val coupons = couponService.getAllCoupons()
        return ResponseEntity.ok(coupons)
    }

    /**
     * Get active coupons
     */
    @GetMapping("/active")
    fun getActiveCoupons(): ResponseEntity<List<Coupon>> {
        val coupons = couponService.getActiveCoupons()
        return ResponseEntity.ok(coupons)
    }

    /**
     * Get coupons by status
     */
    @GetMapping("/status/{status}")
    fun getCouponsByStatus(@PathVariable status: CouponStatus): ResponseEntity<List<Coupon>> {
        val coupons = couponService.getCouponsByStatus(status)
        return ResponseEntity.ok(coupons)
    }

    /**
     * Search coupons by name
     */
    @GetMapping("/search")
    fun searchCouponsByName(@RequestParam name: String): ResponseEntity<List<Coupon>> {
        val coupons = couponService.getCouponsByName(name)
        return ResponseEntity.ok(coupons)
    }

    /**
     * Update coupon
     */
    @PutMapping("/{couponId}")
    fun updateCoupon(@PathVariable couponId: Long, @RequestBody coupon: Coupon): ResponseEntity<Coupon> {
        return try {
            val updatedCoupon = couponService.updateCoupon(couponId, coupon)
            ResponseEntity.ok(updatedCoupon)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Use coupon
     */
    @PostMapping("/{couponId}/use")
    fun useCoupon(@PathVariable couponId: Long): ResponseEntity<Coupon> {
        return try {
            val usedCoupon = couponService.useCoupon(couponId)
            ResponseEntity.ok(usedCoupon)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Delete coupon
     */
    @DeleteMapping("/{couponId}")
    fun deleteCoupon(@PathVariable couponId: Long): ResponseEntity<Void> {
        val deleted = couponService.deleteCoupon(couponId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}