package com.gasolinerajsm.authservice.application.usecase

import com.gasolinerajsm.authservice.application.port.`in`.RegisterUserCommand
import com.gasolinerajsm.authservice.application.port.out.DomainEventPublisher
import com.gasolinerajsm.authservice.application.port.out.OtpService
import com.gasolinerajsm.authservice.domain.model.User
import com.gasolinerajsm.authservice.domain.repository.UserRepository
import com.gasolinerajsm.authservice.domain.service.AuthenticationDomainService
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Register User Use Case
 * Handles the complete user registration flow
 */
@Service
@Transactional
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val authenticationDomainService: AuthenticationDomainService,
    private val otpService: OtpService,
    private val eventPublisher: DomainEventPublisher
) {

    suspend fun execute(command: RegisterUserCommand): Result<RegisterUserResult> {
        return try {
            // Validate input data
            val phoneNumber = PhoneNumber.from(command.phoneNumber)
            val validationResult = authenticationDomainService.validateUserRegistration(
                phoneNumber = phoneNumber,
                firstName = command.firstName,
                lastName = command.lastName
            )

            if (!validationResult.isSuccess) {
                return Result.failure(IllegalArgumentException(validationResult.message))
            }

            // Check if user already exists
            val existingUser = userRepository.findByPhoneNumber(phoneNumber)
                .getOrNull()

            if (existingUser != null) {
                return Result.failure(IllegalStateException("User with this phone number already exists"))
            }

            // Create new user
            val newUser = User.create(
                phoneNumber = phoneNumber,
                firstName = command.firstName,
                lastName = command.lastName,
                role = command.role
            )

            // Save user
            val savedUser = userRepository.save(newUser)
                .getOrElse {
                    return Result.failure(RuntimeException("Failed to save user: ${it.message}"))
                }

            // Generate and send OTP
            val otpCode = authenticationDomainService.generateOtpCode()
            val otpExpiration = authenticationDomainService.calculateOtpExpiration()

            otpService.sendOtp(phoneNumber, otpCode, otpExpiration)
                .onFailure {
                    // User was created but OTP failed - this is a partial success
                    // We should log this and potentially retry
                    return Result.success(
                        RegisterUserResult(
                            userId = savedUser.id.toString(),
                            phoneNumber = savedUser.phoneNumber.toString(),
                            otpSent = false,
                            message = "User created but OTP sending failed. Please request OTP manually."
                        )
                    )
                }

            // Publish domain events
            savedUser.getUncommittedEvents().forEach { event ->
                eventPublisher.publish(event)
            }
            savedUser.markEventsAsCommitted()

            Result.success(
                RegisterUserResult(
                    userId = savedUser.id.toString(),
                    phoneNumber = savedUser.phoneNumber.toString(),
                    otpSent = true,
                    message = "User registered successfully. Please verify your phone number with the OTP sent."
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Result of user registration
 */
data class RegisterUserResult(
    val userId: String,
    val phoneNumber: String,
    val otpSent: Boolean,
    val message: String
)