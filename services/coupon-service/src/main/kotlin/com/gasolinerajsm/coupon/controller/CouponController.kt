package com.gasolinerajsm.coupon.controller

import com.gasolinerajsm.coupon.dto.CouponResponse
import com.gasolinerajsm.coupon.entity.Coupon
import com.gasolinerajsm.coupon.service.CouponService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController @RequestMapping("/api/coupons")
class CouponController(private val couponService: CouponService) {

    @GetMapping
    fun getAllCoupons(): ResponseEntity<List<CouponResponse>> {
        return ResponseEntity.ok(couponService.getAllCoupons())
    }

    @GetMapping("/{id}")
    fun getCouponById( @PathVariable id: UUID): ResponseEntity<CouponResponse> {
        val coupon = couponService.getCouponById(id)
        return if (coupon != null) {
            ResponseEntity.ok(coupon)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createCoupon( @RequestBody coupon: Coupon): ResponseEntity<CouponResponse> {
        val createdCoupon = couponService.createCoupon(coupon)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCoupon)
    }

    @PostMapping("/{id}/redeem")
    fun redeemCoupon( @PathVariable id: UUID): ResponseEntity<CouponResponse> {
        return try {
            val redeemedCoupon = couponService.redeemCoupon(id)
            ResponseEntity.ok(redeemedCoupon)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getCouponsByUser( @PathVariable userId: UUID): ResponseEntity<List<CouponResponse>> {
        val coupons = couponService.getCouponsByUser(userId)
        return ResponseEntity.ok(coupons)
    }
}