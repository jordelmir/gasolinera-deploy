package com.gasolinerajsm.authservice.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.LocalDateTime

@DisplayName("User Entity Tests")
class UserTest {

    @Nested
    @DisplayName("User Creation Tests")
    inner class UserCreationTests {

        @Test
        @DisplayName("Should create user with valid data")
        fun shouldCreateUserWithValidData() {
            // Given
            val phoneNumber = "+1234567890"
            val firstName = "John"
            val lastName = "Doe"
            val role = UserRole.CUSTOMER

            // When
            val user = User(
                phoneNumber = phoneNumber,
                firstName = firstName,
                lastName = lastName,
                role = role
            )

            // Then
            assertEquals(phoneNumber, user.phoneNumber)
            assertEquals(firstName, user.firstName)
            assertEquals(lastName, user.lastName)
            assertEquals(role, user.role)
            assertTrue(user.isActive)
            assertFalse(user.isPhoneVerified)
            assertEquals(0, user.failedLoginAttempts)
            assertNull(user.accountLockedUntil)
            assertNull(user.lastLoginAt)
        }

        @Test
        @DisplayName("Should create user with default values")
        fun shouldCreateUserWithDefaultValues() {
            // When
            val user = User(
                phoneNumber = "+1234567890",
                firstName = "John",
                lastName = "Doe"
            )

            // Then
            assertEquals(UserRole.CUSTOMER, user.role)
            assertTrue(user.isActive)
            assertFalse(user.isPhoneVerified)
            assertEquals(0, user.failedLoginAttempts)
        }
    }

    @Nested
    @DisplayName("User Business Logic Tests")
    inner class UserBusinessLogicTests {

        private val testUser = User(
            id = 1L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.CUSTOMER
        )

        @Test
        @DisplayName("Should return full name correctly")
        fun shouldReturnFullNameCorrectly() {
            // When
            val fullName = testUser.getFullName()

            // Then
            assertEquals("John Doe", fullName)
        }

        @Test
        @DisplayName("Should detect unlocked account")
        fun shouldDetectUnlockedAccount() {
            // When & Then
            assertFalse(testUser.isAccountLocked())
        }

        @Test
        @DisplayName("Should detect locked account")
        fun shouldDetectLockedAccount() {
            // Given
            val lockedUser = testUser.copy(
                accountLockedUntil = LocalDateTime.now().plusMinutes(30)
            )

            // When & Then
            assertTrue(lockedUser.isAccountLocked())
        }

        @Test
        @DisplayName("Should detect expired lock")
        fun shouldDetectExpiredLock() {
            // Given
            val expiredLockUser = testUser.copy(
                accountLockedUntil = LocalDateTime.now().minusMinutes(30)
            )

            // When & Then
            assertFalse(expiredLockUser.isAccountLocked())
        }

        @Test
        @DisplayName("Should allow login for active verified user")
        fun shouldAllowLoginForActiveVerifiedUser() {
            // Given
            val verifiedUser = testUser.copy(
                isActive = true,
                isPhoneVerified = true
            )

            // When & Then
            assertTrue(verifiedUser.canAttemptLogin())
        }

        @Test
        @DisplayName("Should not allow login for inactive user")
        fun shouldNotAllowLoginForInactiveUser() {
            // Given
            val inactiveUser = testUser.copy(
                isActive = false,
                isPhoneVerified = true
            )

            // When & Then
            assertFalse(inactiveUser.canAttemptLogin())
        }

        @Test
        @DisplayName("Should not allow login for unverified user")
        fun shouldNotAllowLoginForUnverifiedUser() {
            // Given
            val unverifiedUser = testUser.copy(
                isActive = true,
                isPhoneVerified = false
            )

            // When & Then
            assertFalse(unverifiedUser.canAttemptLogin())
        }

        @Test
        @DisplayName("Should not allow login for locked user")
        fun shouldNotAllowLoginForLockedUser() {
            // Given
            val lockedUser = testUser.copy(
                isActive = true,
                isPhoneVerified = true,
                accountLockedUntil = LocalDateTime.now().plusMinutes(30)
            )

            // When & Then
            assertFalse(lockedUser.canAttemptLogin())
        }
    }

    @Nested
    @DisplayName("User State Management Tests")
    inner class UserStateManagementTests {

        private val testUser = User(
            id = 1L,
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.CUSTOMER,
            failedLoginAttempts = 2
        )

        @Test
        @DisplayName("Should record successful login")
        fun shouldRecordSuccessfulLogin() {
            // When
            val updatedUser = testUser.recordSuccessfulLogin()

            // Then
            assertNotNull(updatedUser.lastLoginAt)
            assertEquals(0, updatedUser.failedLoginAttempts)
            assertNull(updatedUser.accountLockedUntil)
        }

        @Test
        @DisplayName("Should record failed login without locking")
        fun shouldRecordFailedLoginWithoutLocking() {
            // Given
            val maxAttempts = 5
            val lockoutMinutes = 30L

            // When
            val updatedUser = testUser.recordFailedLogin(maxAttempts, lockoutMinutes)

            // Then
            assertEquals(3, updatedUser.failedLoginAttempts)
            assertNull(updatedUser.accountLockedUntil)
        }

        @Test
        @DisplayName("Should lock account after max failed attempts")
        fun shouldLockAccountAfterMaxFailedAttempts() {
            // Given
            val userWithMaxAttempts = testUser.copy(failedLoginAttempts = 4)
            val maxAttempts = 5
            val lockoutMinutes = 30L

            // When
            val updatedUser = userWithMaxAttempts.recordFailedLogin(maxAttempts, lockoutMinutes)

            // Then
            assertEquals(5, updatedUser.failedLoginAttempts)
            assertNotNull(updatedUser.accountLockedUntil)
            assertTrue(updatedUser.accountLockedUntil!!.isAfter(LocalDateTime.now()))
        }

        @Test
        @DisplayName("Should verify phone number")
        fun shouldVerifyPhoneNumber() {
            // Given
            val unverifiedUser = testUser.copy(isPhoneVerified = false)

            // When
            val verifiedUser = unverifiedUser.verifyPhone()

            // Then
            assertTrue(verifiedUser.isPhoneVerified)
        }

        @Test
        @DisplayName("Should deactivate user")
        fun shouldDeactivateUser() {
            // Given
            val activeUser = testUser.copy(isActive = true)

            // When
            val deactivatedUser = activeUser.deactivate()

            // Then
            assertFalse(deactivatedUser.isActive)
        }

        @Test
        @DisplayName("Should activate user")
        fun shouldActivateUser() {
            // Given
            val inactiveUser = testUser.copy(isActive = false)

            // When
            val activatedUser = inactiveUser.activate()

            // Then
            assertTrue(activatedUser.isActive)
        }
    }

    @Nested
    @DisplayName("UserRole Tests")
    inner class UserRoleTests {

        @Test
        @DisplayName("Customer should have correct permissions")
        fun customerShouldHaveCorrectPermissions() {
            // Given
            val role = UserRole.CUSTOMER

            // Then
            assertTrue(role.hasPermission("coupon:redeem"))
            assertTrue(role.hasPermission("raffle:participate"))
            assertTrue(role.hasPermission("ad:view"))
            assertFalse(role.hasPermission("station:manage"))
            assertFalse(role.isAdmin())
            assertFalse(role.canManageStations())
            assertFalse(role.canProcessRedemptions())
        }

        @Test
        @DisplayName("Employee should have correct permissions")
        fun employeeShouldHaveCorrectPermissions() {
            // Given
            val role = UserRole.EMPLOYEE

            // Then
            assertTrue(role.hasPermission("coupon:validate"))
            assertTrue(role.hasPermission("redemption:process"))
            assertFalse(role.hasPermission("station:manage"))
            assertFalse(role.isAdmin())
            assertFalse(role.canManageStations())
            assertTrue(role.canProcessRedemptions())
        }

        @Test
        @DisplayName("Station Admin should have correct permissions")
        fun stationAdminShouldHaveCorrectPermissions() {
            // Given
            val role = UserRole.STATION_ADMIN

            // Then
            assertTrue(role.hasPermission("station:update"))
            assertTrue(role.hasPermission("employee:manage"))
            assertTrue(role.hasPermission("analytics:station"))
            assertFalse(role.hasPermission("system:configure"))
            assertTrue(role.isAdmin())
            assertFalse(role.canManageStations())
            assertTrue(role.canProcessRedemptions())
        }

        @Test
        @DisplayName("System Admin should have correct permissions")
        fun systemAdminShouldHaveCorrectPermissions() {
            // Given
            val role = UserRole.SYSTEM_ADMIN

            // Then
            assertTrue(role.hasPermission("user:manage"))
            assertTrue(role.hasPermission("station:manage"))
            assertTrue(role.hasPermission("system:configure"))
            assertTrue(role.isAdmin())
            assertTrue(role.canManageStations())
            assertFalse(role.canProcessRedemptions())
        }

        @Test
        @DisplayName("Should have correct display names")
        fun shouldHaveCorrectDisplayNames() {
            assertEquals("Customer", UserRole.CUSTOMER.displayName)
            assertEquals("Employee", UserRole.EMPLOYEE.displayName)
            assertEquals("Station Administrator", UserRole.STATION_ADMIN.displayName)
            assertEquals("System Administrator", UserRole.SYSTEM_ADMIN.displayName)
        }
    }

    @Nested
    @DisplayName("User Validation Tests")
    inner class UserValidationTests {

        @Test
        @DisplayName("Should have proper toString implementation")
        fun shouldHaveProperToStringImplementation() {
            // Given
            val user = User(
                id = 1L,
                phoneNumber = "+1234567890",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.CUSTOMER
            )

            // When
            val toString = user.toString()

            // Then
            assertTrue(toString.contains("id=1"))
            assertTrue(toString.contains("phoneNumber='+1234567890'"))
            assertTrue(toString.contains("firstName='John'"))
            assertTrue(toString.contains("lastName='Doe'"))
            assertTrue(toString.contains("role=CUSTOMER"))
            assertFalse(toString.contains("password"))
        }
    }
}