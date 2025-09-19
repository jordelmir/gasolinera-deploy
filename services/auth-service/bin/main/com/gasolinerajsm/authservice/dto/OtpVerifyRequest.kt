package com.gasolinerajsm.authservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.authservice.service.OtpPurpose
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request DTO for OTP verification
 */
data class OtpVerifyRequest(
    @field:NotBlank(message = "Phone number cannot be blank")
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in valid international format (E.164)"
    )
    @JsonProperty("phone_number")
    val phoneNumber: String,

    @field:NotBlank(message = "OTP code cannot be blank")
    @field:Pattern(
        regexp = "^\\d{4,8}$",
        message = "OTP code must contain only digits and be 4-8 characters long"
    )
    @JsonProperty("otp_code")
    val otpCode: String,

    @JsonProperty("purpose")
    val purpose: OtpPurpose = OtpPurpose.LOGIN,

    @JsonProperty("remember_device")
    val rememberDevice: Boolean = false
)