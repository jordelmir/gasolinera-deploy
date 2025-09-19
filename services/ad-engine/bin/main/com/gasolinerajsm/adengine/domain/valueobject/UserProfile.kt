package com.gasolinerajsm.adengine.domain.valueobject

import java.time.LocalDate

/**
 * Value object representing user profile information
 */
data class UserProfile(
    val userId: String,
    val age: Int? = null,
    val gender: String? = null,
    val location: String? = null,
    val interests: List<String> = emptyList(),
    val registrationDate: LocalDate? = null,
    val isPremium: Boolean = false,
    val ticketBalance: Int = 0,
    val userSegment: String = "REGULAR",
    val deviceType: String = "MOBILE",
    val language: String = "es"
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(age == null || age > 0) { "Age must be positive" }
        require(ticketBalance >= 0) { "Ticket balance cannot be negative" }
    }

    fun hasInterest(interest: String): Boolean = interests.contains(interest)

    fun isEligibleForPremiumContent(): Boolean = isPremium || ticketBalance > 0
}