package com.gasolinerajsm.authservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.authservice.model.UserRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request DTO for user registration
 */
data class UserRegistrationRequest(
    @field:NotBlank(message = "Phone number cannot be blank")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format (E.164)"
    )
    @JsonProperty("phone_number")
    val phoneNumber: String,

    @field:NotBlank(message = "First name cannot be blank")
    @field:Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @field:Pattern(
        regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$",
        message = "First name can only contain letters and spaces"
    )
    @JsonProperty("first_name")
    val firstName: String,

    @field:NotBlank(message = "Last name cannot be blank")
    @field:Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @field:Pattern(
        regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$",
        message = "Last name can only contain letters and spaces"
    )
    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("role")
    val role: UserRole = UserRole.CUSTOMER,

    @JsonProperty("accept_terms")
    val acceptTerms: Boolean = false,

    @JsonProperty("marketing_consent")
    val marketingConsent: Boolean = false
) {

    /**
     * Validates the registration request
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (!acceptTerms) {
            errors.add("Terms and conditions must be accepted")
        }

        // Additional business validation can be added here

        return errors
    }

    /**
     * Normalizes the phone number
     */
    fun normalizedPhoneNumber(): String {
        return phoneNumber.replace(Regex("[^+\\d]"), "")
    }

    /**
     * Capitalizes names properly
     */
    fun capitalizedFirstName(): String {
        return firstName.trim().split(" ").joinToString(" ") {
            it.lowercase().replaceFirstChar { char -> char.uppercase() }
        }
    }

    /**
     * Capitalizes names properly
     */
    fun capitalizedLastName(): String {
        return lastName.trim().split(" ").joinToString(" ") {
            it.lowercase().replaceFirstChar { char -> char.uppercase() }
        }
    }
}