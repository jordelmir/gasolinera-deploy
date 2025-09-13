package com.gasolinerajsm.authservice.application.usecase

import com.gasolinerajsm.authservice.application.port.`in`.AuthenticateUserCommand
import com.gasolinerajsm.authservice.application.port.out.DomainEventPublisher
import com.gasolinerajsm.authservice.application.port.out.JwtTokenService
import com.gasolinerajsm.authservice.application.port.out.OtpService
import com.gasolinerajsm.authservice.domain.repository.UserRepository
import com.gasolinerajsm.authservice.domain.service.AuthenticationDomainService
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Authenticate User Use Case
 * Handles the complete user authentication flow with OTP
 */
@Service
@Transactional
class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
    private val authenticationDomainService: AuthenticationDomainService,
    private val otpService: OtpService,
    private val jwtTokenService: JwtTokenService,
    private val eventPublisher: DomainEventPublisher
) {

    suspend fun execute(command: AuthenticateUserCommand): Result<AuthenticationResult> {
        return try {
            val phoneNumber = PhoneNumber.from(command.phoneNumber)

            // Find user by phone number
            val user = userRepository.findByPhoneNumber(phoneNumber)
                .getOrNull()
                ?: return Result.failure(IllegalArgumentException("User not found"))

            // Check if user can login
            val canLoginResult = authenticationDomainService.canUserLogin(user)
            if (!canLoginResult.isSuccess) {
                return Result.failure(IllegalStateException(canLoginResult.message))
            }

            // Verify OTP
            val otpVerificationResult = otpService.verifyOtp(phoneNumber, command.otpCode)
                .getOrElse {
                    // Record failed login attempt
                    val updatedUser = authenticationDomainService.processLoginAttempt(user, false)
                    userRepository.save(updatedUser)

                    return Result.failure(IllegalArgumentException("Invalid or expired OTP"))
                }

            if (!otpVerificationResult) {
                // Record failed login attempt
                val updatedUser = authenticationDomainService.processLoginAttempt(user, false)
                userRepository.save(updatedUser)

                return Result.failure(IllegalArgumentException("Invalid OTP"))
            }

            // Record successful login
            val authenticatedUser = authenticationDomainService.processLoginAttempt(user, true)
            val savedUser = userRepository.save(authenticatedUser)
                .getOrElse {
                    return Result.failure(RuntimeException("Failed to update user login status"))
                }

            // Generate JWT tokens
            val accessToken = jwtTokenService.generateAccessToken(savedUser)
            val refreshToken = jwtTokenService.generateRefreshToken(savedUser)

            // Publish domain events
            savedUser.getUncommittedEvents().forEach { event ->
                eventPublisher.publish(event)
            }
            savedUser.markEventsAsCommitted()

            Result.success(
                AuthenticationResult(
                    userId = savedUser.id.toString(),
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = 3600, // 1 hour
                    tokenType = "Bearer",
                    user = UserInfo(
                        id = savedUser.id.toString(),
                        phoneNumber = savedUser.phoneNumber.toString(),
                        firstName = savedUser.firstName,
                        lastName = savedUser.lastName,
                        role = savedUser.role.name,
                        permissions = savedUser.role.getAllPermissions()
                    )
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Result of user authentication
 */
data class AuthenticationResult(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String,
    val user: UserInfo
)

/**
 * User information included in authentication result
 */
data class UserInfo(
    val id: String,
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val permissions: List<String>
)