package com.gasolinerajsm.authservice.unit

import com.gasolinerajsm.authservice.domain.model.User
import com.gasolinerajsm.authservice.domain.model.UserRole
import com.gasolinerajsm.authservice.domain.service.AuthenticationDomainService
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import com.gasolinerajsm.shared.testing.BaseUnitTest
import com.gasolinerajsm.shared.testing.TestDataFactory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

/**
 * Unit tests for User Domain Model and Authentication Domain Service
 * Tests business logic in isolation
 */
class UserDomainUnitTest : BaseUnitTest() {

    private val authenticationDomainService = AuthenticationDomainService()

    init {
        given("User Domain Model") {
            `when`("creating a new user") {
                then("should generate valid user with domain events") {
                    val phoneNumber = PhoneNumber.from(TestDataFactory.randomPhoneNumber())
                    val firstName = TestDataFactory.randomString(10)
                    val lastName = TestDataFactory.randomString(10)

                    val user = User.create(
                        phoneNumber = phoneNumber,
                        firstName = firstName,
                        lastName = lastName,
                        role = UserRole.CUSTOMER
                    )

                    user.id shouldNotBe null
                    user.phoneNumber shouldBe phoneNumber
                    user.firstName shouldBe firstName
                    user.lastName shouldBe lastName
                    user.role shouldBe UserRole.CUSTOMER
                    user.isActive shouldBe true
                    user.isPhoneVerified shouldBe false
                    user.getUncommittedEvents().size shouldBe 1
                }
            }

            `when`("recording successful login") {
                then("should update login timestamp and clear failed attempts") {
                    val user = createTestUser()
                        .copy(failedLoginAttempts = 3)

                    val updatedUser = user.recordSuccessfulLogin()

                    updatedUser.lastLoginAt shouldNotBe null
                    updatedUser.failedLoginAttempts shouldBe 0
                    updatedUser.accountLockedUntil shouldBe null
                    updatedUser.getUncommittedEvents().size shouldBe 1
                }
            }

            `when`("recording failed login attempts") {
                then("should increment failed attempts and lock account after max attempts") {
                    val user = createTestUser()
                        .copy(failedLoginAttempts = 4) // One less than max

                    val updatedUser = user.recordFailedLogin(5, 30)

                    updatedUser.failedLoginAttempts shouldBe 5
                    updatedUser.accountLockedUntil shouldNotBe null
                    updatedUser.isAccountLocked() shouldBe true
                }
            }

            `when`("verifying phone number") {
                then("should mark phone as verified and generate event") {
                    val user = createTestUser()

                    val updatedUser = user.verifyPhone()

                    updatedUser.isPhoneVerified shouldBe true
                    updatedUser.getUncommittedEvents().size shouldBe 1
                }
            }
        }

        given("Authentication Domain Service") {
            `when`("validating user login capability") {
                then("should allow login for active verified user") {
                    val user = createTestUser()
                        .copy(isActive = true, isPhoneVerified = true)

                    val result = authenticationDomainService.canUserLogin(user)

                    result.isSuccess shouldBe true
                }

                then("should reject login for inactive user") {
                    val user = createTestUser()
                        .copy(isActive = false)

                    val result = authenticationDomainService.canUserLogin(user)

                    result.isSuccess shouldBe false
                    result.message shouldBe "Account is deactivated"
                }

                then("should reject login for unverified user") {
                    val user = createTestUser()
                        .copy(isPhoneVerified = false)

                    val result = authenticationDomainService.canUserLogin(user)

                    result.isSuccess shouldBe false
                    result.message shouldBe "Phone number not verified"
                }

                then("should reject login for locked user") {
                    val user = createTestUser()
                        .copy(
                            accountLockedUntil = LocalDateTime.now().plusMinutes(30),
                            isActive = true,
                            isPhoneVerified = true
                        )

                    val result = authenticationDomainService.canUserLogin(user)

                    result.isSuccess shouldBe false
                    result.message shouldBe "Account is temporarily locked"
                }
            }

            `when`("validating phone numbers") {
                then("should validate correct mobile phone numbers") {
                    val phoneNumber = PhoneNumber.from("+1234567890")

                    val result = authenticationDomainService.validatePhoneNumber(phoneNumber)

                    result.isSuccess shouldBe true
                }
            }

            `when`("generating OTP codes") {
                then("should generate 6-digit OTP codes") {
                    val otpCode = authenticationDomainService.generateOtpCode()

                    otpCode.length shouldBe 6
                    otpCode.all { it.isDigit() } shouldBe true
                }
            }

            `when`("validating user registration data") {
                then("should validate correct registration data") {
                    val phoneNumber = PhoneNumber.from(TestDataFactory.randomPhoneNumber())
                    val firstName = "John"
                    val lastName = "Doe"

                    val result = authenticationDomainService.validateUserRegistration(
                        phoneNumber, firstName, lastName
                    )

                    result.isSuccess shouldBe true
                }

                then("should reject invalid names") {
                    val phoneNumber = PhoneNumber.from(TestDataFactory.randomPhoneNumber())
                    val firstName = "A" // Too short
                    val lastName = "B" // Too short

                    val result = authenticationDomainService.validateUserRegistration(
                        phoneNumber, firstName, lastName
                    )

                    result.isSuccess shouldBe false
                }
            }
        }
    }

    private fun createTestUser(): User {
        return User.create(
            phoneNumber = PhoneNumber.from(TestDataFactory.randomPhoneNumber()),
            firstName = TestDataFactory.randomString(10),
            lastName = TestDataFactory.randomString(10),
            role = UserRole.CUSTOMER
        ).copy(isPhoneVerified = true) // Make it verified for most tests
    }
}