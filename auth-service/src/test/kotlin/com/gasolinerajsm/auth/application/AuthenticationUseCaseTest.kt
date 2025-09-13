package com.gasolinerajsm.auth.application

import com.gasolinerajsm.auth.domain.*
import com.gasolinerajsm.auth.infrastructure.security.JwtTokenProvider
import com.gasolinerajsm.testing.shared.TestDataFactory
import com.gasolinerajsm.testing.shared.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*

/**
 * Unit Tests for Authentication Use Case
 * Tests authentication business logic and security flows
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("Authentication Use Case Tests")
class AuthenticationUseCaseTest {

    @Mock
    private lateinit var authUserRepository: AuthUserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var domainEventPublisher: DomainEventPublisher

    private lateinit var authenticationUseCase: AuthenticationUseCase

    @BeforeEach
    fun setUp() {
        authenticationUseCase = AuthenticationUseCase(
            authUserRepository = authUserRepository,
            passwordEncoder = passwordEncoder,
            jwtTokenProvider = jwtTokenProvider,
            domainEventPublisher = domainEventPublisher
        )
    }

    @Nested
    @DisplayName("User Registration Tests")
    inner class UserRegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        fun shouldRegisterNewUserSuccessfully() {
            // Given
            val request = RegisterUserRequest(
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                password = "SecurePassword123!"
            )
            val hashedPassword = "hashedPassword123"
            val savedUser = createMockAuthUser()

            whenever(authUserRepository.findByEmail(any())).thenReturn(null)
            whenever(authUserRepository.findByPhone(any())).thenReturn(null)
            whenever(passwordEncoder.encode(request.password)).thenReturn(hashedPassword)
            whenever(authUserRepository.save(any<AuthUser>())).thenReturn(savedUser)

            // When
            val result = authenticationUseCase.registerUser(request)

            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(result.user).isNotNull
            assertThat(result.user?.email?.value).isEqualTo(request.email)

            verify(authUserRepository).findByEmail(Email(request.email))
            verify(authUserRepository).findByPhone(Phone(request.phone))
            verify(passwordEncoder).encode(request.password)
            verify(authUserRepository).save(any<AuthUser>())
            verify(domainEventPublisher).publishAll(any())
        }

        @Test
        @DisplayName("Should fail registration when email already exists")
        fun shouldFailRegistrationWhenEmailAlreadyExists() {
            // Given
            val request = RegisterUserRequest(
                email = "existing@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                password = "SecurePassword123!"
            )
            val existingUser = createMockAuthUser()

            whenever(authUserRepository.findByEmail(any())).thenReturn(existingUser)

            // When
            val result = authenticationUseCase.registerUser(request)

            // Then
            assertThat(result.isSuccess).isFalse()
            assertThat(result.error).isEqualTo(AuthenticationError.EMAIL_ALREADY_EXISTS)

            verify(authUserRepository).findByEmail(Email(request.email))
            verifyNever(authUserRepository).save(any<AuthUser>())
        }

        @Test
        @DisplayName("Should fail registration when phone already exists")
        fun shouldFailRegistrationWhenPhoneAlreadyExists() {
            // Given
            val request = RegisterUserRequest(
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                password = "SecurePassword123!"
            )
            val existingUser = createMockAuthUser()

            whenever(authUserRepository.findByEmail(any())).thenReturn(null)
            whenever(authUserRepository.findByPhone(any())).thenReturn(existingUser)

            // When
            val result = authenticationUseCase.registerUser(request)

            // Then
            assertThat(result.isSuccess).isFalse()
            assertThat(result.error).isEqualTo(AuthenticationError.PHONE_ALREADY_EXISTS)

            verify(authUserRepository).findByPhone(Phone(request.phone))
            verifyNever(authUserRepository).save(any<AuthUser>())
        }

        @Test
        @DisplayName("Should fail registration with invalid password")
        fun shouldFailRegistrationWithInvalidPassword() {
            // Given
            val request = RegisterUserRequest(
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                password = "weak"
            )

            // When & Then
            assertThrows<IllegalArgumentException> {
                authenticationUseCase.registerUser(request)
            }
        }
    }

    @Nested
    @DisplayName("User Login Tests")
    inner class UserLoginTests {

        @Test
        @DisplayName("Should login user successfully with email")
        fun shouldLoginUserSuccessfullyWithEmail() {
            // Given
            val request = LoginRequest(
                identifier = "test@gasolinera.com",
                password = "SecurePassword123!"
            )
            val user = createMockAuthUser()
            val accessToken = "access.jwt.token"
            val refreshToken = "refresh.jwt.token"

            whenever(authUserRepository.findByEmail(any())).thenReturn(user)
            whenever(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
            whenever(jwtTokenProvider.generateAccessToken(any())).thenReturn(accessToken)
            whenever(jwtTokenProvider.generateRefreshToken(any())).thenReturn(refreshToken)
            whenever(authUserRepository.save(any<AuthUser>())).thenReturn(user)

            // When
            val result = authenticationUseCase.login(request)

            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(result.accessToken).isEqualTo(accessToken)
            assertThat(result.refreshToken).isEqualTo(refreshToken)
            assertThat(result.user).isEqualTo(user)

            verify(authUserRepository).findByEmail(Email(request.identifier))
            verify(passwordEncoder).matches(request.password, user.passwordHash)
            verify(jwtTokenProvider).generateAccessToken(user)
            verify(jwtTokenProvider).generateRefreshToken(user)
            verify(authUserRepository).save(user) // For updating last login
        }

        @Test
        @DisplayName("Should login user successfully with phone")
        fun shouldLoginUserSuccessfullyWithPhone() {
            // Given
            val request = LoginRequest(
                identifier = "5551234567",
                password = "SecurePassword123!"
            )
            val user = createMockAuthUser()
            val accessToken = "access.jwt.token"
            val refreshToken = "refresh.jwt.token"

            whenever(authUserRepository.findByEmail(any())).thenReturn(null)
            whenever(authUserRepository.findByPhone(any())).thenReturn(user)
            whenever(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
            whenever(jwtTokenProvider.generateAccessToken(any())).thenReturn(accessToken)
            whenever(jwtTokenProvider.generateRefreshToken(any())).thenReturn(refreshToken)
            whenever(authUserRepository.save(any<AuthUser>())).thenReturn(user)

            // When
            val result = authenticationUseCase.login(request)

            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(result.accessToken).isEqualTo(accessToken)
            assertThat(result.refreshToken).isEqualTo(refreshToken)

            verify(authUserRepository).findByPhone(Phone(request.identifier))
        }

        @Test
        @DisplayName("Should fail login when user not found")
        fun shouldFailLoginWhenUserNotFound() {
            // Given
            val request = LoginRequest(
                identifier = "nonexistent@gasolinera.com",
                password = "SecurePassword123!"
            )

            whenever(authUserRepository.findByEmail(any())).thenReturn(null)
            whenever(authUserRepository.findByPhone(any())).thenReturn(null)

            // When
            val result = authenticationUseCase.login(request)

            // Then
            assertThat(result.isSuccess).isFalse()
            assertThat(result.error).isEqualTo(AuthenticationError.INVALID_CREDENTIALS)

            verifyNever(passwordEncoder).matches(any(), any())
            verifyNever(jwtTokenProvider).generateAccessToken(any())
        }

        @Test
        @DisplayName("Should fail login when password is incorrect")
        fun shouldFailLoginWhenPasswordIsIncorrect() {
            // Given
            val request = LoginRequest(
                identifier = "test@gasolinera.com",
                password = "WrongPassword123!"
            )
            val user = createMockAuthUser()

            whenever(authUserRepository.findByEmail(any())).thenReturn(user)
            whenever(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(false)

            // When
            val result = authenticationUseCase.login(request)

            // Then
            assertThat(result.isSuccess).isFalse()
            assertThat(result.error).isEqualTo(AuthenticationError.INVALID_CREDENTIALS)

            verify(passwordEncoder).matches(request.password, user.passwordHash)
            verifyNever(jwtTokenProvider).generateAccessToken(any())
        }

        @Test
        @DisplayName("Should fail login when user is deactivated")
        fun shouldFailLoginWhenUserIsDeactivated() {
            // Given
            val request = LoginRequest(
                identifier = "test@gasolinera.com",
                password = "SecurePassword123!"
            )
            val user = createMockAuthUser(isActive = false)

            whenever(authUserRepository.findByEmail(any())).thenReturn(user)
            whenever(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)

            // When
            val result = authenticationUseCase.login(request)

            // Then
            assertThat(result.isSuccess).isFalse()
            assertThat(result.error).isEqualTo(AuthenticationError.USER_DEACTIVATED)

            verifyNever(jwtTokenProvider).generateAccessToken(any())
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    inner class TokenRefreshTests {

        @Test
        @DisplayName("Should refresh token successfully")
        fun shouldRefreshTokenSuccessfully() {
            // Given
            val refreshToken = "valid.refresh.token"
            val userId = UUID.randomUUID()
            val user = createMockAuthUser(id = userId)
            val newAccessToken = "new.access.token"
            val newRefreshToken = "new.refresh.token"

            whenever(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true)
            whenever(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId)
            whenever(authUserRepository.findById(userId)).thenReturn(user)
            whenever(jwtTokenProvider.generateAccessToken(user)).thenReturn(newAccessToken)
            whenever(jwtTokenProvider.generateRefreshToken(user)).thenReturn(newRefreshToken)

            // When
            val result = authenticationUseCase.refreshToken(refreshToken)

            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(result.accessToken).isEqualTo(newAccessToken)
            assertThat(result.refreshToken).isEqualTo(newRefreshToken)

            verify(jwtTokenProvider).validateRefreshToken(refreshToken)
            verify(jwtTokenProvider).getUserIdFromToken(refreshToken)
            verify(authUserRepository).findById(userId)
            verify(jwtTokenProvider).generateAccessToken(user)
            verify(jwtTokenProvider).generateRefreshToken(user)
        }

        @Test
        @DisplayName("Should fail refresh when token is invalid")
        fun shouldFailRefreshWhenTokenIsInvalid() {
            // Given
            val invalidRefreshToken = "invalid.refresh.token"

            whenever(jwtTokenProvider.validateRefreshToken(invalidRefreshToken)).thenReturn(false)

            // When
            val result = authenticationUseCase.refreshToken(invalidRefreshToken)

            // Then
            assertThat(result.isSuccess).isFalse()
            assertThat(result.error).isEqualTo(AuthenticationError.INVALID_REFRESH_TOKEN)

            verify(jwtTokenProvider).validateRefreshToken(invalidRefreshToken)
            verifyNever(authUserRepository).findById(any())
        }

        @Test
        @DisplayName("Should fail refresh when user not found")
        fun shouldFailRefreshWhenUserNotFound() {
            // Given
            val refreshToken = "valid.refresh.token"
            val userId = UUID.randomUUID()

            whenever(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true)
            whenever(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId)
            whenever(authUserRepository.findById(userId)).thenReturn(null)

            // When
            val result = authenticationUseCase.refreshToken(refreshToken)

            // Then
            assertThat(result.isSuccess).isFalse()
            assertThat(result.error).isEqualTo(AuthenticationError.USER_NOT_FOUND)

            verify(authUserRepository).findById(userId)
            verifyNever(jwtTokenProvider).generateAccessToken(any())
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    inner class PasswordResetTests {

        @Test
        @DisplayName("Should initiate password reset successfully")
        fun shouldInitiatePasswordResetSuccessfully() {
            // Given
            val email = "test@gasolinera.com"
            val user = createMockAuthUser()
            val resetToken = "reset.token.123"

            whenever(authUserRepository.findByEmail(any())).thenReturn(user)
            whenever(jwtTokenProvider.generatePasswordResetToken(user)).thenReturn(resetToken)
            whenever(authUserRepository.save(any<AuthUser>())).thenReturn(user)

            // When
            val result = authenticationUseCase.initiatePasswordReset(email)

            // Then
            assertThat(result.isSuccess).isTrue()
            assertThat(result.resetToken).isEqualTo(resetToken)

            verify(authUserRepository).findByEmail(Email(email))
            verify(jwtTokenProvider).generatePasswordResetToken(user)
            verify(authUserRepository).save(user)
            verify(domainEventPublisher).publishAll(any())
        }

        @Test
        @DisplayName("Should complete password reset successfully")
        fun shouldCompletePasswordResetSuccessfully() {
            // Given
            val resetToken = "valid.reset.token"
            val newPassword = "NewSecurePassword123!"
            val userId = UUID.randomUUID()
            val user = createMockAuthUser(id = userId)
            val hashedPassword = "newHashedPassword"

            whenever(jwtTokenProvider.validatePasswordResetToken(resetToken)).thenReturn(true)
            whenever(jwtTokenProvider.getUserIdFromToken(resetToken)).thenReturn(userId)
            whenever(authUserRepository.findById(userId)).thenReturn(user)
            whenever(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword)
            whenever(authUserRepository.save(any<AuthUser>())).thenReturn(user)

            // When
            val result = authenticationUseCase.resetPassword(resetToken, newPassword)

            // Then
            assertThat(result.isSuccess).isTrue()

            verify(jwtTokenProvider).validatePasswordResetToken(resetToken)
            verify(jwtTokenProvider).getUserIdFromToken(resetToken)
            verify(authUserRepository).findById(userId)
            verify(passwordEncoder).encode(newPassword)
            verify(authUserRepository).save(user)
            verify(domainEventPublisher).publishAll(any())
        }
    }

    private fun createMockAuthUser(
        id: UUID = UUID.randomUUID(),
        email: String = "test@gasolinera.com",
        phone: String = "5551234567",
        isActive: Boolean = true
    ): AuthUser {
        return AuthUser.create(
            id = id,
            email = email,
            phone = phone,
            firstName = "Juan",
            lastName = "Pérez",
            passwordHash = "hashedPassword123"
        ).apply {
            if (!isActive) {
                deactivate()
            }
        }
    }
}