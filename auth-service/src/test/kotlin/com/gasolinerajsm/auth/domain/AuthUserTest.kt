package com.gasolinerajsm.auth.domain

import com.gasolinerajsm.testing.shared.TestDataFactory
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.*

/**
 * Unit Tests for AuthUser Domain Entity
 * Tests business logic, validation, and domain rules
 */
@DisplayName("AuthUser Domain Entity Tests")
class AuthUserTest {

    @Nested
    @DisplayName("Creation Tests")
    inner class CreationTests {

        @Test
        @DisplayName("Should create valid AuthUser with all required fields")
        fun shouldCreateValidAuthUser() {
            // Given
            val id = UUID.randomUUID()
            val email = "test@gasolinera.com"
            val phone = "5551234567"
            val firstName = "Juan"
            val lastName = "Pérez"
            val passwordHash = "hashedPassword123"

            // When
            val authUser = AuthUser.create(
                id = id,
                email = email,
                phone = phone,
                firstName = firstName,
                lastName = lastName,
                passwordHash = passwordHash
            )

            // Then
            assertThat(authUser.id).isEqualTo(id)
            assertThat(authUser.email.value).isEqualTo(email)
            assertThat(authUser.phone.value).isEqualTo(phone)
            assertThat(authUser.firstName).isEqualTo(firstName)
            assertThat(authUser.lastName).isEqualTo(lastName)
            assertThat(authUser.passwordHash).isEqualTo(passwordHash)
            assertThat(authUser.isActive).isTrue()
            assertThat(authUser.isEmailVerified).isFalse()
            assertThat(authUser.isPhoneVerified).isFalse()
            assertThat(authUser.createdAt).isBeforeOrEqualTo(LocalDateTime.now())
            assertThat(authUser.updatedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should throw exception when creating AuthUser with invalid email")
        fun shouldThrowExceptionWithInvalidEmail() {
            // Given
            val invalidEmail = "invalid-email"

            // When & Then
            assertThrows<IllegalArgumentException> {
                AuthUser.create(
                    id = UUID.randomUUID(),
                    email = invalidEmail,
                    phone = "5551234567",
                    firstName = "Juan",
                    lastName = "Pérez",
                    passwordHash = "hashedPassword123"
                )
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "   ", "123", "invalid-phone", "555-123-4567"])
        @DisplayName("Should throw exception when creating AuthUser with invalid phone")
        fun shouldThrowExceptionWithInvalidPhone(invalidPhone: String) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                AuthUser.create(
                    id = UUID.randomUUID(),
                    email = "test@gasolinera.com",
                    phone = invalidPhone,
                    firstName = "Juan",
                    lastName = "Pérez",
                    passwordHash = "hashedPassword123"
                )
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        @DisplayName("Should throw exception when creating AuthUser with empty names")
        fun shouldThrowExceptionWithEmptyNames(emptyName: String) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                AuthUser.create(
                    id = UUID.randomUUID(),
                    email = "test@gasolinera.com",
                    phone = "5551234567",
                    firstName = emptyName,
                    lastName = "Pérez",
                    passwordHash = "hashedPassword123"
                )
            }

            assertThrows<IllegalArgumentException> {
                AuthUser.create(
                    id = UUID.randomUUID(),
                    email = "test@gasolinera.com",
                    phone = "5551234567",
                    firstName = "Juan",
                    lastName = emptyName,
                    passwordHash = "hashedPassword123"
                )
            }
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    inner class BusinessLogicTests {

        private fun createValidAuthUser(): AuthUser {
            return AuthUser.create(
                id = UUID.randomUUID(),
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                passwordHash = "hashedPassword123"
            )
        }

        @Test
        @DisplayName("Should verify email successfully")
        fun shouldVerifyEmailSuccessfully() {
            // Given
            val authUser = createValidAuthUser()
            assertThat(authUser.isEmailVerified).isFalse()

            // When
            authUser.verifyEmail()

            // Then
            assertThat(authUser.isEmailVerified).isTrue()
            assertThat(authUser.emailVerifiedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should verify phone successfully")
        fun shouldVerifyPhoneSuccessfully() {
            // Given
            val authUser = createValidAuthUser()
            assertThat(authUser.isPhoneVerified).isFalse()

            // When
            authUser.verifyPhone()

            // Then
            assertThat(authUser.isPhoneVerified).isTrue()
            assertThat(authUser.phoneVerifiedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should deactivate user successfully")
        fun shouldDeactivateUserSuccessfully() {
            // Given
            val authUser = createValidAuthUser()
            assertThat(authUser.isActive).isTrue()

            // When
            authUser.deactivate()

            // Then
            assertThat(authUser.isActive).isFalse()
            assertThat(authUser.deactivatedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should reactivate user successfully")
        fun shouldReactivateUserSuccessfully() {
            // Given
            val authUser = createValidAuthUser()
            authUser.deactivate()
            assertThat(authUser.isActive).isFalse()

            // When
            authUser.reactivate()

            // Then
            assertThat(authUser.isActive).isTrue()
            assertThat(authUser.deactivatedAt).isNull()
        }

        @Test
        @DisplayName("Should update password hash successfully")
        fun shouldUpdatePasswordHashSuccessfully() {
            // Given
            val authUser = createValidAuthUser()
            val oldPasswordHash = authUser.passwordHash
            val newPasswordHash = "newHashedPassword456"

            // When
            authUser.updatePasswordHash(newPasswordHash)

            // Then
            assertThat(authUser.passwordHash).isEqualTo(newPasswordHash)
            assertThat(authUser.passwordHash).isNotEqualTo(oldPasswordHash)
            assertThat(authUser.passwordUpdatedAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should throw exception when updating password with empty hash")
        fun shouldThrowExceptionWithEmptyPasswordHash() {
            // Given
            val authUser = createValidAuthUser()

            // When & Then
            assertThrows<IllegalArgumentException> {
                authUser.updatePasswordHash("")
            }

            assertThrows<IllegalArgumentException> {
                authUser.updatePasswordHash("   ")
            }
        }

        @Test
        @DisplayName("Should update last login timestamp")
        fun shouldUpdateLastLoginTimestamp() {
            // Given
            val authUser = createValidAuthUser()
            assertThat(authUser.lastLoginAt).isNull()

            // When
            authUser.updateLastLogin()

            // Then
            assertThat(authUser.lastLoginAt).isBeforeOrEqualTo(LocalDateTime.now())
        }

        @Test
        @DisplayName("Should check if user is fully verified")
        fun shouldCheckIfUserIsFullyVerified() {
            // Given
            val authUser = createValidAuthUser()

            // When & Then - Initially not verified
            assertThat(authUser.isFullyVerified()).isFalse()

            // When - Verify email only
            authUser.verifyEmail()
            assertThat(authUser.isFullyVerified()).isFalse()

            // When - Verify phone as well
            authUser.verifyPhone()
            assertThat(authUser.isFullyVerified()).isTrue()
        }

        @Test
        @DisplayName("Should get full name correctly")
        fun shouldGetFullNameCorrectly() {
            // Given
            val authUser = createValidAuthUser()

            // When
            val fullName = authUser.getFullName()

            // Then
            assertThat(fullName).isEqualTo("Juan Pérez")
        }
    }

    @Nested
    @DisplayName("Value Objects Tests")
    inner class ValueObjectsTests {

        @Test
        @DisplayName("Should create valid Email value object")
        fun shouldCreateValidEmail() {
            // Given
            val emailString = "test@gasolinera.com"

            // When
            val email = Email(emailString)

            // Then
            assertThat(email.value).isEqualTo(emailString)
            assertThat(email.domain).isEqualTo("gasolinera.com")
            assertThat(email.localPart).isEqualTo("test")
        }

        @ParameterizedTest
        @ValueSource(strings = ["invalid-email", "@gasolinera.com", "test@", "test.gasolinera.com"])
        @DisplayName("Should throw exception with invalid email formats")
        fun shouldThrowExceptionWithInvalidEmailFormats(invalidEmail: String) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Email(invalidEmail)
            }
        }

        @Test
        @DisplayName("Should create valid Phone value object")
        fun shouldCreateValidPhone() {
            // Given
            val phoneString = "5551234567"

            // When
            val phone = Phone(phoneString)

            // Then
            assertThat(phone.value).isEqualTo(phoneString)
            assertThat(phone.formattedValue).isEqualTo("555-123-4567")
        }

        @ParameterizedTest
        @ValueSource(strings = ["123", "555-123-4567", "invalid-phone", ""])
        @DisplayName("Should throw exception with invalid phone formats")
        fun shouldThrowExceptionWithInvalidPhoneFormats(invalidPhone: String) {
            // When & Then
            assertThrows<IllegalArgumentException> {
                Phone(invalidPhone)
            }
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    inner class EqualityTests {

        @Test
        @DisplayName("Should be equal when same ID")
        fun shouldBeEqualWhenSameId() {
            // Given
            val id = UUID.randomUUID()
            val authUser1 = AuthUser.create(
                id = id,
                email = "test1@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                passwordHash = "hash1"
            )
            val authUser2 = AuthUser.create(
                id = id,
                email = "test2@gasolinera.com",
                phone = "5559876543",
                firstName = "María",
                lastName = "García",
                passwordHash = "hash2"
            )

            // When & Then
            assertThat(authUser1).isEqualTo(authUser2)
            assertThat(authUser1.hashCode()).isEqualTo(authUser2.hashCode())
        }

        @Test
        @DisplayName("Should not be equal when different ID")
        fun shouldNotBeEqualWhenDifferentId() {
            // Given
            val authUser1 = AuthUser.create(
                id = UUID.randomUUID(),
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                passwordHash = "hash"
            )
            val authUser2 = AuthUser.create(
                id = UUID.randomUUID(),
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                passwordHash = "hash"
            )

            // When & Then
            assertThat(authUser1).isNotEqualTo(authUser2)
            assertThat(authUser1.hashCode()).isNotEqualTo(authUser2.hashCode())
        }
    }

    @Nested
    @DisplayName("Domain Events Tests")
    inner class DomainEventsTests {

        @Test
        @DisplayName("Should publish UserCreated event when user is created")
        fun shouldPublishUserCreatedEvent() {
            // Given
            val id = UUID.randomUUID()

            // When
            val authUser = AuthUser.create(
                id = id,
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                passwordHash = "hashedPassword123"
            )

            // Then
            val events = authUser.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(UserCreatedEvent::class.java)

            val event = events.first() as UserCreatedEvent
            assertThat(event.userId).isEqualTo(id)
            assertThat(event.email).isEqualTo("test@gasolinera.com")
        }

        @Test
        @DisplayName("Should publish EmailVerified event when email is verified")
        fun shouldPublishEmailVerifiedEvent() {
            // Given
            val authUser = AuthUser.create(
                id = UUID.randomUUID(),
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                passwordHash = "hashedPassword123"
            )
            authUser.clearDomainEvents() // Clear creation event

            // When
            authUser.verifyEmail()

            // Then
            val events = authUser.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(EmailVerifiedEvent::class.java)
        }

        @Test
        @DisplayName("Should publish UserDeactivated event when user is deactivated")
        fun shouldPublishUserDeactivatedEvent() {
            // Given
            val authUser = AuthUser.create(
                id = UUID.randomUUID(),
                email = "test@gasolinera.com",
                phone = "5551234567",
                firstName = "Juan",
                lastName = "Pérez",
                passwordHash = "hashedPassword123"
            )
            authUser.clearDomainEvents() // Clear creation event

            // When
            authUser.deactivate()

            // Then
            val events = authUser.getDomainEvents()
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(UserDeactivatedEvent::class.java)
        }
    }
}