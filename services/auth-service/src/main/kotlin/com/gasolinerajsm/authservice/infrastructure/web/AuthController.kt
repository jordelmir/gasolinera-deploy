package com.gasolinerajsm.authservice.infrastructure.web

import com.gasolinerajsm.authservice.application.port.`in`.AuthenticateUserCommand
import com.gasolinerajsm.authservice.application.port.`in`.RegisterUserCommand
import com.gasolinerajsm.authservice.application.port.`in`.RequestOtpCommand
import com.gasolinerajsm.authservice.application.usecase.AuthenticateUserUseCase
import com.gasolinerajsm.authservice.application.usecase.RegisterUserUseCase
import com.gasolinerajsm.authservice.infrastructure.web.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val authenticateUserUseCase: AuthenticateUserUseCase
) {

    @Operation(summary = "Register a new user", description = "Register a new user with phone number and send OTP for verification")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "User registered successfully"),
            ApiResponse(responseCode = "400", description = "Invalid input data"),
            ApiResponse(responseCode = "409", description = "User already exists")
        ]
    )
    @PostMapping("/register")
    suspend fun register(@Valid @RequestBody request: RegisterUserRequest): ResponseEntity<ApiResponse<RegisterUserResponse>> {
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
                            ApiResponse.success(
                                data = RegisterUserResponse(
                                    userId = result.userId,
                                    phoneNumber = result.phoneNumber,
                                    otpSent = result.otpSent,
                                    message = result.message
                                ),
                                message = "User registered successfully"
                            )
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is IllegalArgumentException -> ResponseEntity.badRequest().body(
                                ApiResponse.error<RegisterUserResponse>(error.message ?: "Invalid input")
                            )
                            is IllegalStateException -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                                ApiResponse.error<RegisterUserResponse>(error.message ?: "User already exists")
                            )
                            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiResponse.error<RegisterUserResponse>("Registration failed")
                            )
                        }
                    }
                )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error<RegisterUserResponse>("Unexpected error occurred")
            )
        }
    }

    @Operation(summary = "Authenticate user with OTP", description = "Authenticate user using phone number and OTP code")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Authentication successful"),
            ApiResponse(responseCode = "400", description = "Invalid credentials"),
            ApiResponse(responseCode = "401", description = "Authentication failed"),
            ApiResponse(responseCode = "423", description = "Account locked")
        ]
    )
    @PostMapping("/login")
    suspend fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthenticationResponse>> {
        return try {
            val command = AuthenticateUserCommand(
                phoneNumber = request.phoneNumber,
                otpCode = request.otpCode
            )

            authenticateUserUseCase.execute(command)
                .fold(
                    onSuccess = { result ->
                        ResponseEntity.ok(
                            ApiResponse.success(
                                data = AuthenticationResponse(
                                    accessToken = result.accessToken,
                                    refreshToken = result.refreshToken,
                                    tokenType = result.tokenType,
                                    expiresIn = result.expiresIn,
                                    user = UserDetailsResponse(
                                        id = result.user.id,
                                        phoneNumber = result.user.phoneNumber,
                                        firstName = result.user.firstName,
                                        lastName = result.user.lastName,
                                        role = result.user.role,
                                        permissions = result.user.permissions
                                    )
                                ),
                                message = "Authentication successful"
                            )
                        )
                    },
                    onFailure = { error ->
                        when (error) {
                            is IllegalArgumentException -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                ApiResponse.error<AuthenticationResponse>(error.message ?: "Invalid credentials")
                            )
                            is IllegalStateException -> {
                                val status = if (error.message?.contains("locked") == true) {
                                    HttpStatus.LOCKED
                                } else {
                                    HttpStatus.UNAUTHORIZED
                                }
                                ResponseEntity.status(status).body(
                                    ApiResponse.error<AuthenticationResponse>(error.message ?: "Authentication failed")
                                )
                            }
                            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiResponse.error<AuthenticationResponse>("Authentication failed")
                            )
                        }
                    }
                )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error<AuthenticationResponse>("Unexpected error occurred")
            )
        }
    }

    @Operation(summary = "Request OTP for login", description = "Request OTP code to be sent to phone number")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            ApiResponse(responseCode = "400", description = "Invalid phone number"),
            ApiResponse(responseCode = "404", description = "User not found")
        ]
    )
    @PostMapping("/request-otp")
    suspend fun requestOtp(@Valid @RequestBody request: RequestOtpRequest): ResponseEntity<ApiResponse<OtpResponse>> {
        return try {
            // TODO: Implement RequestOtpUseCase
            ResponseEntity.ok(
                ApiResponse.success(
                    data = OtpResponse(
                        phoneNumber = request.phoneNumber,
                        otpSent = true,
                        expiresIn = 300, // 5 minutes
                        message = "OTP sent successfully"
                    ),
                    message = "OTP sent to your phone number"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error<OtpResponse>("Failed to send OTP")
            )
        }
    }

    @Operation(summary = "Health check", description = "Check if the authentication service is healthy")
    @GetMapping("/health")
    fun health(): ResponseEntity<ApiResponse<Map<String, String>>> {
        return ResponseEntity.ok(
            ApiResponse.success(
                data = mapOf(
                    "status" to "UP",
                    "service" to "auth-service",
                    "timestamp" to java.time.LocalDateTime.now().toString()
                ),
                message = "Service is healthy"
            )
        )
    }
}