package com.gasolinerajsm.authservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gasolinerajsm.authservice.dto.*
import com.gasolinerajsm.authservice.infrastructure.web.AuthController
import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.model.UserRole
import com.gasolinerajsm.authservice.service.AuthService
import com.gasolinerajsm.authservice.service.JwtService
import com.gasolinerajsm.authservice.service.OtpPurpose
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(AuthController::class)
@ActiveProfiles("test")
@DisplayName("Auth Controller Integration Tests")
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var jwtService: JwtService

    private val testUser = User(
        id = 1L,
        phoneNumber = "+1234567890",
        firstName = "John",
        lastName = "Doe",
        role = UserRole.CUSTOMER,
        isActive = true,
        isPhoneVerified = true
    )

    @Nested
    @DisplayName("User Registration Tests")
    inner class UserRegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        fun shouldRegisterUserSuccessfully() {
            // Given
            val request = UserRegistrationRequest(
                phoneNumber = "+1234567890",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.CUSTOMER,
                acceptTerms = true
            )

            val response = AuthenticationResponse(
                success = true,
                message = "User registered successfully",
                user = UserDetailsResponse.fromUser(testUser),
                requiresVerification = true,
                verificationMethod = "sms"
            )

            every { authService.registerUser(any()) } returns response

            // When & Then
            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.user.user_id").value(1))
                .andExpect(jsonPath("$.data.requires_verification").value(true))

            verify { authService.registerUser(any()) }
        }

        @Test
        @DisplayName("Should reject registration with invalid phone number")
        fun shouldRejectRegistrationWithInvalidPhoneNumber() {
            // Given
            val request = UserRegistrationRequest(
                phoneNumber = "invalid-phone",
                firstName = "John",
                lastName = "Doe",
                acceptTerms = true
            )

            // When & Then
            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
        }

        @Test
        @DisplayName("Should reject registration with missing required fields")
        fun shouldRejectRegistrationWithMissingRequiredFields() {
            // Given
            val request = mapOf(
                "phone_number" to "+1234567890"
                // Missing firstName, lastName, acceptTerms
            )

            // When & Then
            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
        }
    }

    @Nested
    @DisplayName("Account Status Tests")
    inner class AccountStatusTests {

        @Test
        @DisplayName("Should return account status for existing user")
        fun shouldReturnAccountStatusForExistingUser() {
            // Given
            val phoneNumber = "+1234567890"
            val accountStatus = AccountStatusResponse(
                phoneNumber = phoneNumber,
                exists = true,
                isActive = true,
                isVerified = true,
                canLogin = true
            )

            every { authService.checkAccountStatus(phoneNumber) } returns accountStatus

            // When & Then
            mockMvc.perform(
                get("/api/auth/account-status")
                    .param("phone_number", phoneNumber)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exists").value(true))
                .andExpect(jsonPath("$.data.is_active").value(true))
                .andExpect(jsonPath("$.data.can_login").value(true))

            verify { authService.checkAccountStatus(phoneNumber) }
        }

        @Test
        @DisplayName("Should return account status for non-existing user")
        fun shouldReturnAccountStatusForNonExistingUser() {
            // Given
            val phoneNumber = "+1234567890"
            val accountStatus = AccountStatusResponse(
                phoneNumber = phoneNumber,
                exists = false,
                registrationRequired = true
            )

            every { authService.checkAccountStatus(phoneNumber) } returns accountStatus

            // When & Then
            mockMvc.perform(
                get("/api/auth/account-status")
                    .param("phone_number", phoneNumber)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.data.registration_required").value(true))
        }

        @Test
        @DisplayName("Should reject empty phone number")
        fun shouldRejectEmptyPhoneNumber() {
            // When & Then
            mockMvc.perform(
                get("/api/auth/account-status")
                    .param("phone_number", "")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("INVALID_PHONE"))
        }
    }

    @Nested
    @DisplayName("OTP Request Tests")
    inner class OtpRequestTests {

        @Test
        @DisplayName("Should request OTP successfully")
        fun shouldRequestOtpSuccessfully() {
            // Given
            val request = OtpRequest(
                phoneNumber = "+1234567890",
                purpose = OtpPurpose.LOGIN
            )

            val response = OtpGenerationResponse(
                success = true,
                message = "OTP sent successfully",
                expiresAt = LocalDateTime.now().plusMinutes(5),
                attemptsRemaining = 3
            )

            every { authService.initiateLogin(any()) } returns response

            // When & Then
            mockMvc.perform(
                post("/api/auth/login/request-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.attempts_remaining").value(3))

            verify { authService.initiateLogin(any()) }
        }

        @Test
        @DisplayName("Should handle rate limited OTP request")
        fun shouldHandleRateLimitedOtpRequest() {
            // Given
            val request = OtpRequest(
                phoneNumber = "+1234567890",
                purpose = OtpPurpose.LOGIN
            )

            val response = OtpGenerationResponse(
                success = false,
                message = "Too many OTP requests. Please wait.",
                expiresAt = LocalDateTime.now(),
                attemptsRemaining = 0
            )

            every { authService.initiateLogin(any()) } returns response

            // When & Then
            mockMvc.perform(
                post("/api/auth/login/request-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isTooManyRequests)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("OTP_REQUEST_FAILED"))
        }

        @Test
        @DisplayName("Should reject invalid phone number format")
        fun shouldRejectInvalidPhoneNumberFormat() {
            // Given
            val request = OtpRequest(
                phoneNumber = "invalid-phone",
                purpose = OtpPurpose.LOGIN
            )

            // When & Then
            mockMvc.perform(
                post("/api/auth/login/request-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
        }
    }

    @Nested
    @DisplayName("OTP Verification Tests")
    inner class OtpVerificationTests {

        @Test
        @DisplayName("Should verify OTP and login successfully")
        fun shouldVerifyOtpAndLoginSuccessfully() {
            // Given
            val request = OtpVerifyRequest(
                phoneNumber = "+1234567890",
                otpCode = "123456",
                purpose = OtpPurpose.LOGIN
            )

            val tokenResponse = TokenResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresIn = 3600,
                refreshExpiresIn = 86400,
                sessionId = "session-123"
            )

            val response = AuthenticationResponse(
                success = true,
                message = "Login successful",
                user = UserDetailsResponse.fromUser(testUser),
                tokens = tokenResponse
            )

            every { authService.completeLogin(any()) } returns response

            // When & Then
            mockMvc.perform(
                post("/api/auth/login/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.tokens.access_token").value("access-token"))
                .andExpect(jsonPath("$.data.user.user_id").value(1))

            verify { authService.completeLogin(any()) }
        }

        @Test
        @DisplayName("Should reject invalid OTP")
        fun shouldRejectInvalidOtp() {
            // Given
            val request = OtpVerifyRequest(
                phoneNumber = "+1234567890",
                otpCode = "123456",
                purpose = OtpPurpose.LOGIN
            )

            val response = AuthenticationResponse(
                success = false,
                message = "Invalid OTP code"
            )

            every { authService.completeLogin(any()) } returns response

            // When & Then
            mockMvc.perform(
                post("/api/auth/login/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("LOGIN_FAILED"))
        }

        @Test
        @DisplayName("Should reject invalid OTP format")
        fun shouldRejectInvalidOtpFormat() {
            // Given
            val request = OtpVerifyRequest(
                phoneNumber = "+1234567890",
                otpCode = "abc123", // Invalid format
                purpose = OtpPurpose.LOGIN
            )

            // When & Then
            mockMvc.perform(
                post("/api/auth/login/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    inner class TokenManagementTests {

        @Test
        @DisplayName("Should refresh token successfully")
        fun shouldRefreshTokenSuccessfully() {
            // Given
            val request = RefreshTokenRequest(refreshToken = "valid-refresh-token")

            val tokenResponse = TokenResponse(
                accessToken = "new-access-token",
                refreshToken = "valid-refresh-token",
                expiresIn = 3600,
                refreshExpiresIn = 86400,
                sessionId = "session-123"
            )

            val response = AuthenticationResponse(
                success = true,
                message = "Token refreshed successfully",
                tokens = tokenResponse
            )

            every { authService.refreshToken(any()) } returns response

            // When & Then
            mockMvc.perform(
                post("/api/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokens.access_token").value("new-access-token"))

            verify { authService.refreshToken(any()) }
        }

        @Test
        @DisplayName("Should reject invalid refresh token")
        fun shouldRejectInvalidRefreshToken() {
            // Given
            val request = RefreshTokenRequest(refreshToken = "invalid-refresh-token")

            val response = AuthenticationResponse(
                success = false,
                message = "Invalid refresh token"
            )

            every { authService.refreshToken(any()) } returns response

            // When & Then
            mockMvc.perform(
                post("/api/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("TOKEN_REFRESH_FAILED"))
        }
    }

    @Nested
    @DisplayName("User Profile Tests")
    inner class UserProfileTests {

        @Test
        @DisplayName("Should get user profile successfully")
        fun shouldGetUserProfileSuccessfully() {
            // Given
            val token = "valid-access-token"
            val userId = 1L

            every { jwtService.validateAccessToken(token) } returns true
            every { jwtService.getUserIdFromToken(token) } returns userId
            every { authService.getUserProfile(userId) } returns UserDetailsResponse.fromUser(testUser)

            // When & Then
            mockMvc.perform(
                get("/api/auth/profile")
                    .header("Authorization", "Bearer $token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(1))
                .andExpect(jsonPath("$.data.phone_number").value("+1234567890"))

            verify { jwtService.validateAccessToken(token) }
            verify { jwtService.getUserIdFromToken(token) }
            verify { authService.getUserProfile(userId) }
        }

        @Test
        @DisplayName("Should reject invalid authorization header")
        fun shouldRejectInvalidAuthorizationHeader() {
            // When & Then
            mockMvc.perform(
                get("/api/auth/profile")
                    .header("Authorization", "Invalid header")
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN"))
        }

        @Test
        @DisplayName("Should reject expired token")
        fun shouldRejectExpiredToken() {
            // Given
            val token = "expired-token"

            every { jwtService.validateAccessToken(token) } returns false

            // When & Then
            mockMvc.perform(
                get("/api/auth/profile")
                    .header("Authorization", "Bearer $token")
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN"))

            verify { jwtService.validateAccessToken(token) }
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    inner class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        fun shouldLogoutSuccessfully() {
            // Given
            val token = "valid-access-token"
            val logoutResponse = LogoutResponse(
                success = true,
                message = "Logged out successfully"
            )

            every { authService.logout(token) } returns logoutResponse

            // When & Then
            mockMvc.perform(
                post("/api/auth/logout")
                    .header("Authorization", "Bearer $token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))

            verify { authService.logout(token) }
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    inner class HealthCheckTests {

        @Test
        @DisplayName("Should return health status")
        fun shouldReturnHealthStatus() {
            // When & Then
            mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("auth-service"))
        }
    }
}