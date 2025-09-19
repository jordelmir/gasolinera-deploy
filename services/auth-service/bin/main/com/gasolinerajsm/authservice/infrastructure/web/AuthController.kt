package com.gasolinerajsm.authservice.infrastructure.web

import com.gasolinerajsm.authservice.application.port.`in`.AuthenticateUserCommand
import com.gasolinerajsm.authservice.application.port.`in`.RegisterUserCommand
import com.gasolinerajsm.authservice.application.port.`in`.RequestOtpCommand
import com.gasolinerajsm.authservice.application.usecase.AuthenticateUserUseCase
import com.gasolinerajsm.authservice.application.usecase.RegisterUserUseCase
import com.gasolinerajsm.authservice.infrastructure.web.dto.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Authentication REST Controller (Adapter)
 * Handles HTTP requests and delegates to use cases
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val authenticateUserUseCase: AuthenticateUserUseCase
) {

    @PostMapping("/register")
    suspend fun register(@Valid @RequestBody request: RegisterUserRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val command = RegisterUserCommand(
                phoneNumber = request.phoneNumber,
                firstName = request.firstName,
                lastName = request.lastName,
                role = request.role
            )

            registerUserUseCase.execute(command)
                .fold(
                    onSuccess = { result ->
                        ResponseEntity.status(HttpStatus.CREATED).body(
                            mapOf(
                                "success" to true,
                                "data" to mapOf(
                                    "userId" to result.userId,
                                    "phoneNumber" to result.phoneNumber,
                                    "otpSent" to result.otpSent,
                                    "message" to result.message
                                ),
                                "message" to "User registered successfully"
                            )
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is IllegalArgumentException -> ResponseEntity.badRequest().body(
                                mapOf("success" to false, "message" to (error.message ?: "Invalid input"))
                            )
                            is IllegalStateException -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                                mapOf("success" to false, "message" to (error.message ?: "User already exists"))
                            )
                            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                mapOf("success" to false, "message" to "Registration failed")
                            )
                        }
                    }
                )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "message" to "Unexpected error occurred")
            )
        }
    }

    @PostMapping("/login")
    suspend fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val command = AuthenticateUserCommand(
                phoneNumber = request.phoneNumber,
                otpCode = request.otpCode
            )

            authenticateUserUseCase.execute(command)
                .fold(
                    onSuccess = { result ->
                        ResponseEntity.ok(
                            mapOf(
                                "success" to true,
                                "data" to mapOf(
                                    "accessToken" to result.accessToken,
                                    "refreshToken" to result.refreshToken,
                                    "tokenType" to result.tokenType,
                                    "expiresIn" to result.expiresIn,
                                    "user" to mapOf(
                                        "id" to result.user.id,
                                        "phoneNumber" to result.user.phoneNumber,
                                        "firstName" to result.user.firstName,
                                        "lastName" to result.user.lastName,
                                        "role" to result.user.role,
                                        "permissions" to result.user.permissions
                                    )
                                ),
                                "message" to "Authentication successful"
                            )
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is IllegalArgumentException -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                mapOf("success" to false, "message" to (error.message ?: "Invalid credentials"))
                            )
                            is IllegalStateException -> {
                                val status = if (error.message?.contains("locked") == true) {
                                    HttpStatus.LOCKED
                                } else {
                                    HttpStatus.UNAUTHORIZED
                                }
                                ResponseEntity.status(status).body(
                                    mapOf("success" to false, "message" to (error.message ?: "Authentication failed"))
                                )
                            }
                            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                mapOf("success" to false, "message" to "Authentication failed")
                            )
                        }
                    }
                )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "message" to "Unexpected error occurred")
            )
        }
    }

    @PostMapping("/request-otp")
    suspend fun requestOtp(@Valid @RequestBody request: RequestOtpRequest): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Implement RequestOtpUseCase
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to mapOf(
                        "phoneNumber" to request.phoneNumber,
                        "otpSent" to true,
                        "expiresIn" to 300,
                        "message" to "OTP sent successfully"
                    ),
                    "message" to "OTP sent to your phone number"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "message" to "Failed to send OTP")
            )
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "data" to mapOf(
                    "status" to "UP",
                    "service" to "auth-service",
                    "timestamp" to java.time.LocalDateTime.now().toString()
                ),
                "message" to "Service is healthy"
            )
        )
    }
}