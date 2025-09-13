package com.gasolinerajsm.auth.infrastructure

import com.gasolinerajsm.auth.domain.*
import com.gasolinerajsm.testing.shared.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*

/**
 * Integration Tests for AuthUser Repository
 * Tests real database interactions with PostgreSQL TestContainer
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AuthUser Repository Integration Tests")
class AuthUserRepositoryIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var authUserRepository: AuthUserRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        // Clean database before each test
        authUserRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("Save and Find Operations")
    inner class SaveAndFindOperations {

        @Test
        @DisplayName("Should save and retrieve AuthUser successfully")
        fun shouldSaveAndRetrieveAuthUserSuccessfully() {
            // Given
            val authUser = createTestAuthUser()

            // When
            val savedUser = authUserRepository.save(authUser)
            entityManager.flush()
            entityManager.clear()

            val foundUser = authUserRepository.findById(savedUser.id)

            // Then
            assertThat(foundUser).isPresent
            assertThat(foundUser.get().id).isEqualTo(authUser.id)
            assertThat(foundUser.get().email.value).isEqualTo(authUser.email.value)
            assertThat(foundUser.get().phone.value).isEqualTo(authUser.phone.value)
            assertThat(foundUser.get().firstName).isEqualTo(authUser.firstName)
            assertThat(foundUser.get().lastName).isEqualTo(authUser.lastName)
            assertThat(foundUser.get().isActive).isEqualTo(authUser.isActive)
        }

        @Test
        @DisplayName("Should find user by email successfully")
        fun shouldFindUserByEmailSuccessfully() {
            // Given
            val email = "test@gasolinera.com"
            val authUser = createTestAuthUser(email = email)
            authUserRepository.save(authUser)
            entityManager.flush()
            entityManager.clear()

            // When
            val foundUser = authUserRepository.findByEmail(Email(email))

            // Then
            assertThat(foundUser).isNotNull
            assertThat(foundUser?.email?.value).isEqualTo(email)
        }

        @Test
        @DisplayName("Should find user by phone successfully")
        fun shouldFindUserByPhoneSuccessfully() {
            // Given
            val phone = "5551234567"
            val authUser = createTestAuthUser(phone = phone)
            authUserRepository.save(authUser)
            entityManager.flush()
            entityManager.clear()

            // When
            val foundUser = authUserRepository.findByPhone(Phone(phone))

            // Then
            assertThat(foundUser).isNotNull
            assertThat(foundUser?.phone?.value).isEqualTo(phone)
        }

        @Test
        @DisplayName("Should return null when user not found by email")
        fun shouldReturnNullWhenUserNotFoundByEmail() {
            // Given
            val nonExistentEmail = "nonexistent@gasolinera.com"

            // When
            val foundUser = authUserRepository.findByEmail(Email(nonExistentEmail))

            // Then
            assertThat(foundUser).isNull()
        }

        @Test
        @DisplayName("Should return null when user not found by phone")
        fun shouldReturnNullWhenUserNotFoundByPhone() {
            // Given
            val nonExistentPhone = "9999999999"

            // When
            val foundUser = authUserRepository.findByPhone(Phone(nonExistentPhone))

            // Then
            assertThat(foundUser).isNull()
        }
    }

    @Nested
    @DisplayName("Query Operations")
    inner class QueryOperations {

        @Test
        @DisplayName("Should find all active users")
        fun shouldFindAllActiveUsers() {
            // Given
            val activeUser1 = createTestAuthUser(email = "active1@gasolinera.com")
            val activeUser2 = createTestAuthUser(email = "active2@gasolinera.com")
            val inactiveUser = createTestAuthUser(email = "inactive@gasolinera.com").apply {
                deactivate()
            }

            authUserRepository.saveAll(listOf(activeUser1, activeUser2, inactiveUser))
            entityManager.flush()
            entityManager.clear()

            // When
            val activeUsers = authUserRepository.findByIsActiveTrue()

            // Then
            assertThat(activeUsers).hasSize(2)
            assertThat(activeUsers.map { it.email.value }).containsExactlyInAnyOrder(
                "active1@gasolinera.com",
                "active2@gasolinera.com"
            )
        }

        @Test
        @DisplayName("Should find users by email verification status")
        fun shouldFindUsersByEmailVerificationStatus() {
            // Given
            val verifiedUser = createTestAuthUser(email = "verified@gasolinera.com").apply {
                verifyEmail()
            }
            val unverifiedUser = createTestAuthUser(email = "unverified@gasolinera.com")

            authUserRepository.saveAll(listOf(verifiedUser, unverifiedUser))
            entityManager.flush()
            entityManager.clear()

            // When
            val verifiedUsers = authUserRepository.findByIsEmailVerifiedTrue()
            val unverifiedUsers = authUserRepository.findByIsEmailVerifiedFalse()

            // Then
            assertThat(verifiedUsers).hasSize(1)
            assertThat(verifiedUsers.first().email.value).isEqualTo("verified@gasolinera.com")

            assertThat(unverifiedUsers).hasSize(1)
            assertThat(unverifiedUsers.first().email.value).isEqualTo("unverified@gasolinera.com")
        }

        @Test
        @DisplayName("Should find users created after specific date")
        fun shouldFindUsersCreatedAfterSpecificDate() {
            // Given
            val cutoffDate = LocalDateTime.now().minusDays(1)
            val oldUser = createTestAuthUser(email = "old@gasolinera.com")
            // Simulate old creation date
            entityManager.persistAndFlush(oldUser)
            entityManager.createQuery(
                "UPDATE AuthUser u SET u.createdAt = :oldDate WHERE u.id = :id"
            ).setParameter("oldDate", cutoffDate.minusHours(1))
             .setParameter("id", oldUser.id)
             .executeUpdate()

            val newUser = createTestAuthUser(email = "new@gasolinera.com")
            authUserRepository.save(newUser)
            entityManager.flush()
            entityManager.clear()

            // When
            val recentUsers = authUserRepository.findByCreatedAtAfter(cutoffDate)

            // Then
            assertThat(recentUsers).hasSize(1)
            assertThat(recentUsers.first().email.value).isEqualTo("new@gasolinera.com")
        }
    }

    @Nested
    @DisplayName("Update Operations")
    inner class UpdateOperations {

        @Test
        @DisplayName("Should update user information successfully")
        fun shouldUpdateUserInformationSuccessfully() {
            // Given
            val authUser = createTestAuthUser()
            val savedUser = authUserRepository.save(authUser)
            entityManager.flush()
            entityManager.clear()

            // When
            val userToUpdate = authUserRepository.findById(savedUser.id).get()
            userToUpdate.verifyEmail()
            userToUpdate.verifyPhone()
            userToUpdate.updateLastLogin()

            val updatedUser = authUserRepository.save(userToUpdate)
            entityManager.flush()
            entityManager.clear()

            // Then
            val finalUser = authUserRepository.findById(updatedUser.id).get()
            assertThat(finalUser.isEmailVerified).isTrue()
            assertThat(finalUser.isPhoneVerified).isTrue()
            assertThat(finalUser.lastLoginAt).isNotNull()
            assertThat(finalUser.emailVerifiedAt).isNotNull()
            assertThat(finalUser.phoneVerifiedAt).isNotNull()
        }

        @Test
        @DisplayName("Should update password hash successfully")
        fun shouldUpdatePasswordHashSuccessfully() {
            // Given
            val authUser = createTestAuthUser()
            val savedUser = authUserRepository.save(authUser)
            val originalPasswordHash = savedUser.passwordHash
            entityManager.flush()
            entityManager.clear()

            // When
            val userToUpdate = authUserRepository.findById(savedUser.id).get()
            val newPasswordHash = "newHashedPassword456"
            userToUpdate.updatePasswordHash(newPasswordHash)

            val updatedUser = authUserRepository.save(userToUpdate)
            entityManager.flush()
            entityManager.clear()

            // Then
            val finalUser = authUserRepository.findById(updatedUser.id).get()
            assertThat(finalUser.passwordHash).isEqualTo(newPasswordHash)
            assertThat(finalUser.passwordHash).isNotEqualTo(originalPasswordHash)
            assertThat(finalUser.passwordUpdatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    inner class DeleteOperations {

        @Test
        @DisplayName("Should delete user successfully")
        fun shouldDeleteUserSuccessfully() {
            // Given
            val authUser = createTestAuthUser()
            val savedUser = authUserRepository.save(authUser)
            entityManager.flush()
            entityManager.clear()

            assertThat(authUserRepository.findById(savedUser.id)).isPresent

            // When
            authUserRepository.deleteById(savedUser.id)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertThat(authUserRepository.findById(savedUser.id)).isEmpty
        }

        @Test
        @DisplayName("Should soft delete user by deactivation")
        fun shouldSoftDeleteUserByDeactivation() {
            // Given
            val authUser = createTestAuthUser()
            val savedUser = authUserRepository.save(authUser)
            entityManager.flush()
            entityManager.clear()

            // When
            val userToDeactivate = authUserRepository.findById(savedUser.id).get()
            userToDeactivate.deactivate()
            authUserRepository.save(userToDeactivate)
            entityManager.flush()
            entityManager.clear()

            // Then
            val deactivatedUser = authUserRepository.findById(savedUser.id).get()
            assertThat(deactivatedUser.isActive).isFalse()
            assertThat(deactivatedUser.deactivatedAt).isNotNull()

            // Should not appear in active users query
            val activeUsers = authUserRepository.findByIsActiveTrue()
            assertThat(activeUsers).doesNotContain(deactivatedUser)
        }
    }

    @Nested
    @DisplayName("Constraint Validation")
    inner class ConstraintValidation {

        @Test
        @DisplayName("Should enforce unique email constraint")
        fun shouldEnforceUniqueEmailConstraint() {
            // Given
            val email = "duplicate@gasolinera.com"
            val user1 = createTestAuthUser(email = email)
            val user2 = createTestAuthUser(email = email, phone = "5559876543")

            authUserRepository.save(user1)
            entityManager.flush()

            // When & Then
            assertThatThrownBy {
                authUserRepository.save(user2)
                entityManager.flush()
            }.isInstanceOf(Exception::class.java) // DataIntegrityViolationException or similar
        }

        @Test
        @DisplayName("Should enforce unique phone constraint")
        fun shouldEnforceUniquePhoneConstraint() {
            // Given
            val phone = "5551234567"
            val user1 = createTestAuthUser(phone = phone)
            val user2 = createTestAuthUser(email = "different@gasolinera.com", phone = phone)

            authUserRepository.save(user1)
            entityManager.flush()

            // When & Then
            assertThatThrownBy {
                authUserRepository.save(user2)
                entityManager.flush()
            }.isInstanceOf(Exception::class.java) // DataIntegrityViolationException or similar
        }
    }

    @Nested
    @DisplayName("Pagination and Sorting")
    inner class PaginationAndSorting {

        @Test
        @DisplayName("Should paginate users correctly")
        fun shouldPaginateUsersCorrectly() {
            // Given
            val users = (1..10).map { i ->
                createTestAuthUser(email = "user$i@gasolinera.com", phone = "555123456$i")
            }
            authUserRepository.saveAll(users)
            entityManager.flush()
            entityManager.clear()

            // When
            val pageRequest = org.springframework.data.domain.PageRequest.of(0, 5)
            val firstPage = authUserRepository.findAll(pageRequest)

            // Then
            assertThat(firstPage.content).hasSize(5)
            assertThat(firstPage.totalElements).isEqualTo(10)
            assertThat(firstPage.totalPages).isEqualTo(2)
            assertThat(firstPage.hasNext()).isTrue()
        }

        @Test
        @DisplayName("Should sort users by creation date")
        fun shouldSortUsersByCreationDate() {
            // Given
            val users = (1..5).map { i ->
                createTestAuthUser(email = "user$i@gasolinera.com", phone = "555123456$i")
            }
            authUserRepository.saveAll(users)
            entityManager.flush()
            entityManager.clear()

            // When
            val sortByCreatedAt = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
            )
            val sortedUsers = authUserRepository.findAll(sortByCreatedAt)

            // Then
            assertThat(sortedUsers).hasSize(5)
            // Verify descending order
            for (i in 0 until sortedUsers.size - 1) {
                assertThat(sortedUsers[i].createdAt)
                    .isAfterOrEqualTo(sortedUsers[i + 1].createdAt)
            }
        }
    }

    private fun createTestAuthUser(
        id: UUID = UUID.randomUUID(),
        email: String = "test@gasolinera.com",
        phone: String = "5551234567",
        firstName: String = "Juan",
        lastName: String = "Pérez"
    ): AuthUser {
        return AuthUser.create(
            id = id,
            email = email,
            phone = phone,
            firstName = firstName,
            lastName = lastName,
            passwordHash = "hashedPassword123"
        )
    }
}