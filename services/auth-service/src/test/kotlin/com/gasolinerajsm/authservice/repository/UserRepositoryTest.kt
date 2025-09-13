package com.gasolinerajsm.authservice.repository

import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.model.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testCustomer: User
    private lateinit var testEmployee: User
    private lateinit var testStationAdmin: User

    @BeforeEach
    fun setUp() {
        // Create test users
        testCustomer = User(
            phoneNumber = "+1234567890",
            firstName = "John",
            lastName = "Customer",
            role = UserRole.CUSTOMER,
            isActive = true,
            isPhoneVerified = true
        )

        testEmployee = User(
            phoneNumber = "+1234567891",
            firstName = "Jane",
            lastName = "Employee",
            role = UserRole.EMPLOYEE,
            isActive = true,
            isPhoneVerified = true
        )

        testStationAdmin = User(
            phoneNumber = "+1234567892",
            firstName = "Bob",
            lastName = "Admin",
            role = UserRole.STATION_ADMIN,
            isActive = false,
            isPhoneVerified = false
        )

        // Persist test data
        entityManager.persistAndFlush(testCustomer)
        entityManager.persistAndFlush(testEmployee)
        entityManager.persistAndFlush(testStationAdmin)
    }

    @Nested
    @DisplayName("Basic Query Tests")
    inner class BasicQueryTests {

        @Test
        @DisplayName("Should find user by phone number")
        fun shouldFindUserByPhoneNumber() {
            // When
            val foundUser = userRepository.findByPhoneNumber("+1234567890")

            // Then
            assertNotNull(foundUser)
            assertEquals("John", foundUser?.firstName)
            assertEquals("Customer", foundUser?.lastName)
            assertEquals(UserRole.CUSTOMER, foundUser?.role)
        }

        @Test
        @DisplayName("Should return null for non-existent phone number")
        fun shouldReturnNullForNonExistentPhoneNumber() {
            // When
            val foundUser = userRepository.findByPhoneNumber("+9999999999")

            // Then
            assertNull(foundUser)
        }

        @Test
        @DisplayName("Should check if phone number exists")
        fun shouldCheckIfPhoneNumberExists() {
            // When & Then
            assertTrue(userRepository.existsByPhoneNumber("+1234567890"))
            assertFalse(userRepository.existsByPhoneNumber("+9999999999"))
        }

        @Test
        @DisplayName("Should find user by phone number and active status")
        fun shouldFindUserByPhoneNumberAndActiveStatus() {
            // When
            val activeUser = userRepository.findByPhoneNumberAndIsActive("+1234567890", true)
            val inactiveUser = userRepository.findByPhoneNumberAndIsActive("+1234567892", false)
            val nonExistentActive = userRepository.findByPhoneNumberAndIsActive("+1234567892", true)

            // Then
            assertNotNull(activeUser)
            assertEquals("John", activeUser?.firstName)

            assertNotNull(inactiveUser)
            assertEquals("Bob", inactiveUser?.firstName)

            assertNull(nonExistentActive)
        }
    }

    @Nested
    @DisplayName("Role-based Query Tests")
    inner class RoleBasedQueryTests {

        @Test
        @DisplayName("Should find users by role")
        fun shouldFindUsersByRole() {
            // When
            val customers = userRepository.findByRole(UserRole.CUSTOMER)
            val employees = userRepository.findByRole(UserRole.EMPLOYEE)
            val systemAdmins = userRepository.findByRole(UserRole.SYSTEM_ADMIN)

            // Then
            assertEquals(1, customers.size)
            assertEquals("John", customers[0].firstName)

            assertEquals(1, employees.size)
            assertEquals("Jane", employees[0].firstName)

            assertEquals(0, systemAdmins.size)
        }

        @Test
        @DisplayName("Should find users by role with pagination")
        fun shouldFindUsersByRoleWithPagination() {
            // Given
            val pageable = PageRequest.of(0, 10)

            // When
            val customerPage = userRepository.findByRole(UserRole.CUSTOMER, pageable)

            // Then
            assertEquals(1, customerPage.totalElements)
            assertEquals(1, customerPage.content.size)
            assertEquals("John", customerPage.content[0].firstName)
        }

        @Test
        @DisplayName("Should find active users by role")
        fun shouldFindActiveUsersByRole() {
            // When
            val activeCustomers = userRepository.findByRoleAndIsActive(UserRole.CUSTOMER, true)
            val activeStationAdmins = userRepository.findByRoleAndIsActive(UserRole.STATION_ADMIN, true)
            val inactiveStationAdmins = userRepository.findByRoleAndIsActive(UserRole.STATION_ADMIN, false)

            // Then
            assertEquals(1, activeCustomers.size)
            assertEquals(0, activeStationAdmins.size)
            assertEquals(1, inactiveStationAdmins.size)
        }

        @Test
        @DisplayName("Should count users by role")
        fun shouldCountUsersByRole() {
            // When & Then
            assertEquals(1L, userRepository.countByRole(UserRole.CUSTOMER))
            assertEquals(1L, userRepository.countByRole(UserRole.EMPLOYEE))
            assertEquals(1L, userRepository.countByRole(UserRole.STATION_ADMIN))
            assertEquals(0L, userRepository.countByRole(UserRole.SYSTEM_ADMIN))
        }
    }

    @Nested
    @DisplayName("Status-based Query Tests")
    inner class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find users by active status")
        fun shouldFindUsersByActiveStatus() {
            // When
            val activeUsers = userRepository.findByIsActive(true)
            val inactiveUsers = userRepository.findByIsActive(false)

            // Then
            assertEquals(2, activeUsers.size)
            assertEquals(1, inactiveUsers.size)
            assertEquals("Bob", inactiveUsers[0].firstName)
        }

        @Test
        @DisplayName("Should find users by phone verification status")
        fun shouldFindUsersByPhoneVerificationStatus() {
            // When
            val verifiedUsers = userRepository.findByIsPhoneVerified(true)
            val unverifiedUsers = userRepository.findByIsPhoneVerified(false)

            // Then
            assertEquals(2, verifiedUsers.size)
            assertEquals(1, unverifiedUsers.size)
            assertEquals("Bob", unverifiedUsers[0].firstName)
        }

        @Test
        @DisplayName("Should count users by status")
        fun shouldCountUsersByStatus() {
            // When & Then
            assertEquals(2L, userRepository.countByIsActive(true))
            assertEquals(1L, userRepository.countByIsActive(false))
            assertEquals(2L, userRepository.countByIsPhoneVerified(true))
            assertEquals(1L, userRepository.countByIsPhoneVerified(false))
        }
    }

    @Nested
    @DisplayName("Date-based Query Tests")
    inner class DateBasedQueryTests {

        @Test
        @DisplayName("Should find users created within date range")
        fun shouldFindUsersCreatedWithinDateRange() {
            // Given
            val startDate = LocalDateTime.now().minusHours(1)
            val endDate = LocalDateTime.now().plusHours(1)

            // When
            val usersInRange = userRepository.findByCreatedAtBetween(startDate, endDate)

            // Then
            assertEquals(3, usersInRange.size)
        }

        @Test
        @DisplayName("Should find inactive users")
        fun shouldFindInactiveUsers() {
            // Given
            val cutoffDate = LocalDateTime.now().minusDays(30)

            // When
            val inactiveUsers = userRepository.findInactiveUsers(cutoffDate)

            // Then
            assertEquals(3, inactiveUsers.size) // All users have null lastLoginAt
        }
    }

    @Nested
    @DisplayName("Search Tests")
    inner class SearchTests {

        @Test
        @DisplayName("Should search users by first name")
        fun shouldSearchUsersByFirstName() {
            // When
            val johnUsers = userRepository.searchByName("John")
            val janeUsers = userRepository.searchByName("jane") // case insensitive

            // Then
            assertEquals(1, johnUsers.size)
            assertEquals("John", johnUsers[0].firstName)

            assertEquals(1, janeUsers.size)
            assertEquals("Jane", janeUsers[0].firstName)
        }

        @Test
        @DisplayName("Should search users by last name")
        fun shouldSearchUsersByLastName() {
            // When
            val customerUsers = userRepository.searchByName("Customer")
            val employeeUsers = userRepository.searchByName("employee") // case insensitive

            // Then
            assertEquals(1, customerUsers.size)
            assertEquals("Customer", customerUsers[0].lastName)

            assertEquals(1, employeeUsers.size)
            assertEquals("Employee", employeeUsers[0].lastName)
        }

        @Test
        @DisplayName("Should search users with pagination")
        fun shouldSearchUsersWithPagination() {
            // Given
            val pageable = PageRequest.of(0, 10)

            // When
            val searchResults = userRepository.searchByName("o", pageable) // Should match John and Bob

            // Then
            assertEquals(2, searchResults.totalElements)
            assertEquals(2, searchResults.content.size)
        }
    }

    @Nested
    @DisplayName("Security-related Query Tests")
    inner class SecurityRelatedQueryTests {

        @Test
        @DisplayName("Should find users with failed attempts")
        fun shouldFindUsersWithFailedAttempts() {
            // Given - Create user with failed attempts
            val userWithFailedAttempts = User(
                phoneNumber = "+1234567893",
                firstName = "Failed",
                lastName = "User",
                role = UserRole.CUSTOMER,
                failedLoginAttempts = 3
            )
            entityManager.persistAndFlush(userWithFailedAttempts)

            // When
            val usersWithFailedAttempts = userRepository.findUsersWithFailedAttempts(2)

            // Then
            assertEquals(1, usersWithFailedAttempts.size)
            assertEquals("Failed", usersWithFailedAttempts[0].firstName)
        }

        @Test
        @DisplayName("Should find locked accounts")
        fun shouldFindLockedAccounts() {
            // Given - Create locked user
            val lockedUser = User(
                phoneNumber = "+1234567894",
                firstName = "Locked",
                lastName = "User",
                role = UserRole.CUSTOMER,
                accountLockedUntil = LocalDateTime.now().plusMinutes(30)
            )
            entityManager.persistAndFlush(lockedUser)

            // When
            val lockedAccounts = userRepository.findLockedAccounts(LocalDateTime.now())

            // Then
            assertEquals(1, lockedAccounts.size)
            assertEquals("Locked", lockedAccounts[0].firstName)
        }
    }

    @Nested
    @DisplayName("Update Operation Tests")
    @Transactional
    inner class UpdateOperationTests {

        @Test
        @DisplayName("Should update last login")
        fun shouldUpdateLastLogin() {
            // Given
            val loginTime = LocalDateTime.now()
            val userId = testCustomer.id

            // When
            val updatedRows = userRepository.updateLastLogin(userId, loginTime)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedUser = userRepository.findById(userId).orElse(null)
            assertNotNull(updatedUser)
            assertNotNull(updatedUser.lastLoginAt)
            assertEquals(0, updatedUser.failedLoginAttempts)
            assertNull(updatedUser.accountLockedUntil)
        }

        @Test
        @DisplayName("Should increment failed login attempts")
        fun shouldIncrementFailedLoginAttempts() {
            // Given
            val userId = testCustomer.id

            // When
            val updatedRows = userRepository.incrementFailedLoginAttempts(userId)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedUser = userRepository.findById(userId).orElse(null)
            assertNotNull(updatedUser)
            assertEquals(1, updatedUser.failedLoginAttempts)
        }

        @Test
        @DisplayName("Should lock account")
        fun shouldLockAccount() {
            // Given
            val userId = testCustomer.id
            val lockUntil = LocalDateTime.now().plusMinutes(30)

            // When
            val updatedRows = userRepository.lockAccount(userId, lockUntil)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedUser = userRepository.findById(userId).orElse(null)
            assertNotNull(updatedUser)
            assertNotNull(updatedUser.accountLockedUntil)
            assertTrue(updatedUser.accountLockedUntil!!.isAfter(LocalDateTime.now()))
        }

        @Test
        @DisplayName("Should unlock account")
        fun shouldUnlockAccount() {
            // Given - First lock the account
            val userId = testCustomer.id
            userRepository.lockAccount(userId, LocalDateTime.now().plusMinutes(30))
            entityManager.flush()

            // When
            val updatedRows = userRepository.unlockAccount(userId)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedUser = userRepository.findById(userId).orElse(null)
            assertNotNull(updatedUser)
            assertNull(updatedUser.accountLockedUntil)
            assertEquals(0, updatedUser.failedLoginAttempts)
        }

        @Test
        @DisplayName("Should verify phone number")
        fun shouldVerifyPhoneNumber() {
            // Given
            val userId = testStationAdmin.id // This user is not verified

            // When
            val updatedRows = userRepository.verifyPhoneNumber(userId)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedUser = userRepository.findById(userId).orElse(null)
            assertNotNull(updatedUser)
            assertTrue(updatedUser.isPhoneVerified)
        }

        @Test
        @DisplayName("Should deactivate user")
        fun shouldDeactivateUser() {
            // Given
            val userId = testCustomer.id // This user is active

            // When
            val updatedRows = userRepository.deactivateUser(userId)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedUser = userRepository.findById(userId).orElse(null)
            assertNotNull(updatedUser)
            assertFalse(updatedUser.isActive)
        }

        @Test
        @DisplayName("Should activate user")
        fun shouldActivateUser() {
            // Given
            val userId = testStationAdmin.id // This user is inactive

            // When
            val updatedRows = userRepository.activateUser(userId)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, updatedRows)

            val updatedUser = userRepository.findById(userId).orElse(null)
            assertNotNull(updatedUser)
            assertTrue(updatedUser.isActive)
        }

        @Test
        @DisplayName("Should clean up expired locks")
        fun shouldCleanUpExpiredLocks() {
            // Given - Create user with expired lock
            val expiredLockUser = User(
                phoneNumber = "+1234567895",
                firstName = "Expired",
                lastName = "Lock",
                role = UserRole.CUSTOMER,
                accountLockedUntil = LocalDateTime.now().minusMinutes(30)
            )
            entityManager.persistAndFlush(expiredLockUser)

            // When
            val cleanedRows = userRepository.cleanupExpiredLocks(LocalDateTime.now())
            entityManager.flush()
            entityManager.clear()

            // Then
            assertEquals(1, cleanedRows)

            val updatedUser = userRepository.findById(expiredLockUser.id).orElse(null)
            assertNotNull(updatedUser)
            assertNull(updatedUser.accountLockedUntil)
        }
    }
}