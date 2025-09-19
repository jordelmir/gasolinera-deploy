package com.gasolinerajsm.authservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.authservice.service.OtpPurpose
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request DTO for OTP generation
 */
data class OtpRequest(
    @field:NotBlank(message = "Phone number cannot be blank")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format (E.164)"
    )
    @JsonProperty("phone_number")
    val phoneNumber: String,

    @JsonProperty("purpose")
    val purpose: OtpPurpose = OtpPurpose.LOGIN,

    @JsonProperty("language")
    val language: String = "es" // Default to Spanish
)