package com.gasolinerajsm.authservice.controller

import com.gasolinerajsm.authservice.dto.AdminLoginRequest
import com.gasolinerajsm.authservice.dto.OtpRequest
import com.gasolinerajsm.authservice.dto.OtpVerifyRequest
import com.gasolinerajsm.authservice.dto.TokenResponse
import com.gasolinerajsm.authservice.service.JwtService
import com.gasolinerajsm.authservice.service.UserService
import jakarta.validation.Valid
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

@RestController
@RequestMapping("/auth")
class AuthController(
    private val jwtService: JwtService,
    private val redisTemplate: StringRedisTemplate,
    private val userService: UserService,
    private val env: Environment
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/otp/request")
    fun requestOtp(@Valid @RequestBody request: OtpRequest): ResponseEntity<Void> {
        val otpCode = (100000..999999).random().toString() // Generate 6-digit code
        redisTemplate.opsForValue().set(request.phone, otpCode, 5, TimeUnit.MINUTES) // Store for 5 mins
        logger.info("OTP requested for phone {}") // For manual testing
        return ResponseEntity.ok().build()
    }

    @PostMapping("/otp/verify")
    fun verifyOtp(@Valid @RequestBody request: OtpVerifyRequest): ResponseEntity<Any> {
        val storedOtp = redisTemplate.opsForValue().get(request.phone)

        if (storedOtp == null || storedOtp != request.code) {
            logger.warn("OTP verification failed for phone {}: Invalid or expired OTP", request.phone)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "Invalid OTP"))
        }

        redisTemplate.delete(request.phone)
        logger.info("OTP verified successfully for phone {}", request.phone)

        val user = userService.findOrCreateUser(request.phone)
        val accessToken = jwtService.generateAccessToken(user.id.toString())
        val refreshToken = jwtService.generateRefreshToken(user.id.toString())

        logger.info("Tokens generated for user ID {}", user.id)
        return ResponseEntity.ok(TokenResponse(accessToken, refreshToken))
    }

    @PostMapping("/login/admin")
    fun adminLogin(@Valid @RequestBody request: AdminLoginRequest): ResponseEntity<Any> {
        // HARDCODED credentials for now as per requirements
        val adminEmail = env.getProperty("app.auth.admin-email", "admin@puntog.com")
        val adminPass = env.getProperty("app.auth.admin-password", "admin123")

        if (request.email != adminEmail || request.pass != adminPass) {
            logger.warn("Admin login failed for email: {}", request.email)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "Invalid credentials"))
        }

        val adminId = "admin-system-01"
        val adminRoles = listOf("ADMIN", "USER")
        val accessToken = jwtService.generateAccessToken(adminId, adminRoles)
        val refreshToken = jwtService.generateRefreshToken(adminId)

        logger.info("Admin tokens generated for admin ID {}", adminId)
        return ResponseEntity.ok(TokenResponse(accessToken, refreshToken))
    }

    @PostMapping("/login/advertiser")
    fun advertiserLogin(@Valid @RequestBody request: AdminLoginRequest): ResponseEntity<Any> {
        // In a real system, this would check the advertiser table
        val advertiserEmail = env.getProperty("app.auth.advertiser-email", "anunciante@tosty.com")
        val advertiserPass = env.getProperty("app.auth.advertiser-password", "tosty123")

        if (request.email != advertiserEmail || request.pass != advertiserPass) {
            logger.warn("Advertiser login failed for email: {}", request.email)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "Invalid credentials"))
        }

        val advertiserId = "adv-tosty-01" // This would come from the database
        val advertiserRoles = listOf("ADVERTISER")
        val accessToken = jwtService.generateAccessToken(advertiserId, advertiserRoles)
        val refreshToken = jwtService.generateRefreshToken(advertiserId)

        logger.info("Advertiser tokens generated for advertiser ID {}", advertiserId)
        return ResponseEntity.ok(TokenResponse(accessToken, refreshToken))
    }
}