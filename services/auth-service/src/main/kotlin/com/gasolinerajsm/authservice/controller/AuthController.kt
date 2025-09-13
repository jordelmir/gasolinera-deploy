package com.gasolinerajsm.authservice.controller

import com.gasolinerajsm.authservice.dto.*
import com.gasolinerajsm.authservice.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Simple REST Controller for authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun registerUser(@RequestBody request: UserRegistrationRequest): ResponseEntity<String> {
        return try {
            authService.registerUser(request)
            ResponseEntity.ok("User registered successfully")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Registration failed: ${e.message}")
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: Map<String, String>): ResponseEntity<String> {
        return try {
            ResponseEntity.ok("Login endpoint - implementation needed")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Login failed: ${e.message}")
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<String> {
        return ResponseEntity.ok("Auth service is running")
    }
}