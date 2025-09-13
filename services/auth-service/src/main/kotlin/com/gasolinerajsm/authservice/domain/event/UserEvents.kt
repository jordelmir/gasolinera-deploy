package com.gasolinerajsm.authservice.domain.event

import com.gasolinerajsm.authservice.domain.model.UserRole
import com.gasolinerajsm.authservice.domain.valueobject.PhoneNumber
import com.gasolinerajsm.authservice.domain.valueobject.UserId
import java.time.LocalDateTime

/**
 * Event fired when a new user is created
 */
data class UserCreatedEvent(
    val userId: UserId,
    val phoneNumber: PhoneNumber,
    val role: UserRole,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserCreated",
    occurredAt = occurredAt
)

/**
 * Event fired when a user successfully logs in
 */
data class UserLoggedInEvent(
    val userId: UserId,
    val loginAt: LocalDateTime,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserLoggedIn",
    occurredAt = occurredAt
)

/**
 * Event fired when a user's phone number is verified
 */
data class UserPhoneVerifiedEvent(
    val userId: UserId,
    val phoneNumber: PhoneNumber,
    val verifiedAt: LocalDateTime,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserPhoneVerified",
    occurredAt = occurredAt
)

/**
 * Event fired when a user account is locked due to failed login attempts
 */
data class UserAccountLockedEvent(
    val userId: UserId,
    val lockedUntil: LocalDateTime,
    val reason: String = "Too many failed login attempts",
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserAccountLocked",
    occurredAt = occurredAt
)

/**
 * Event fired when a user account is unlocked
 */
data class UserAccountUnlockedEvent(
    val userId: UserId,
    val unlockedBy: UserId? = null,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserAccountUnlocked",
    occurredAt = occurredAt
)

/**
 * Event fired when a user profile is updated
 */
data class UserProfileUpdatedEvent(
    val userId: UserId,
    val updatedFields: Map<String, Any>,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserProfileUpdated",
    occurredAt = occurredAt
)

/**
 * Event fired when a user account is deactivated
 */
data class UserDeactivatedEvent(
    val userId: UserId,
    val deactivatedBy: UserId? = null,
    val reason: String? = null,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserDeactivated",
    occurredAt = occurredAt
)

/**
 * Event fired when a user account is reactivated
 */
data class UserReactivatedEvent(
    val userId: UserId,
    val reactivatedBy: UserId? = null,
    override val occurredAt: LocalDateTime = LocalDateTime.now()
) : BaseDomainEvent(
    eventType = "UserReactivated",
    occurredAt = occurredAt
)