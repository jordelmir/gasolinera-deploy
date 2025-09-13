package com.gasolinerajsm.authservice.domain.service

import com.gasolinerajsm.authservice.domain.model.User
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import java.time.LocalDateTime

/**
 * Authentication Domain Service
 * Contains complex business logic that doesn't belong to a single entity
 */
class AuthenticationDomainService {

    companion object {
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MINUTES = 30L
        private const val OTP_VALIDITY_MINUTES = 5L
        private const val SESSION_DURATION_HOURS = 24L
    }

    /**
     * Validate if user can attempt login
     */
    fun canUserLogin(user: User): AuthenticationResult {
        return when {
            !user.isActive -> AuthenticationResult.failure("Account is deactivated")
            !user.isPhoneVerified -> AuthenticationResult.failure("Phone number not verified")
            user.isAccountLocked() -> AuthenticationResult.failure("Account is temporarily locked")
            else -> AuthenticationResult.success("User can login")
        }
    }

    /**
     * Process login attempt and update user state
     */
    fun processLoginAttempt(user: User, isSuccessful: Boolean): User {
        return if (isSuccessful) {
            user.recordSuccessfulLogin()
        } else {
            user.recordFailedLogin(MAX_LOGIN_ATTEMPTS, LOCKOUT_DURATION_MINUTES)
        }
    }

    /**
     * Validate phone number format and business rules
     */
    fun validatePhoneNumber(phoneNumber: PhoneNumber): ValidationResult {
        return try {
            when {
                !phoneNumber.isMobile() -> ValidationResult.failure("Only mobile numbers are allowed")
                phoneNumber.getCountryCode() == null -> ValidationResult.warning("Country code not recognized")
                else -> ValidationResult.success("Phone number is valid")
            }
        } catch (e: Exception) {
            ValidationResult.failure("Invalid phone number format: ${e.message}")
        }
    }

    /**
     * Calculate OTP expiration time
     */
    fun calculateOtpExpiration(): LocalDateTime {
        return LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES)
    }

    /**
     * Calculate session expiration time
     */
    fun calculateSessionExpiration(): LocalDateTime {
        return LocalDateTime.now().plusHours(SESSION_DURATION_HOURS)
    }

    /**
     * Check if OTP is still valid
     */
    fun isOtpValid(createdAt: LocalDateTime): Boolean {
        return createdAt.plusMinutes(OTP_VALIDITY_MINUTES).isAfter(LocalDateTime.now())
    }

    /**
     * Generate secure OTP code
     */
    fun generateOtpCode(): String {
        return (100000..999999).random().toString()
    }

    /**
     * Validate user registration data
     */
    fun validateUserRegistration(
        phoneNumber: PhoneNumber,
        firstName: String,
        lastName: String
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate phone number
        val phoneValidation = validatePhoneNumber(phoneNumber)
        if (!phoneValidation.isSuccess) {
            errors.add(phoneValidation.message)
        }

        // Validate first name
        if (firstName.isBlank() || firstName.length < 2) {
            errors.add("First name must be at least 2 characters")
        }
        if (firstName.length > 100) {
            errors.add("First name must not exceed 100 characters")
        }

        // Validate last name
        if (lastName.isBlank() || lastName.length < 2) {
            errors.add("Last name must be at least 2 characters")
        }
        if (lastName.length > 100) {
            errors.add("Last name must not exceed 100 characters")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Registration data is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }
}

/**
 * Result of authentication operations
 */
sealed class AuthenticationResult(val isSuccess: Boolean, val message: String) {
    class Success(message: String) : AuthenticationResult(true, message)
    class Failure(message: String) : AuthenticationResult(false, message)

    companion object {
        fun success(message: String) = Success(message)
        fun failure(message: String) = Failure(message)
    }
}

/**
 * Result of validation operations
 */
sealed class ValidationResult(val isSuccess: Boolean, val message: String) {
    class Success(message: String) : ValidationResult(true, message)
    class Warning(message: String) : ValidationResult(true, message)
    class Failure(message: String) : ValidationResult(false, message)

    companion object {
        fun success(message: String) = Success(message)
        fun warning(message: String) = Warning(message)
        fun failure(message: String) = Failure(message)
    }
}