package com.gasolinerajsm.adengine.domain.valueobject

import java.util.*

/**
 * Advertisement ID Value Object
 */
@JvmInline
value class AdvertisementId(val value: UUID) {
    companion object {
        fun generate(): AdvertisementId = AdvertisementId(UUID.randomUUID())

        fun from(value: String): AdvertisementId {
            return try {
                AdvertisementId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid AdvertisementId format: $value", e)
            }
        }

        fun from(uuid: UUID): AdvertisementId = AdvertisementId(uuid)

        fun fromLong(value: Long): AdvertisementId {
            val uuid = UUID(0L, value)
            return AdvertisementId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Campaign ID Value Object
 */
@JvmInline
value class CampaignId(val value: UUID) {
    companion object {
        fun generate(): CampaignId = CampaignId(UUID.randomUUID())

        fun from(value: String): CampaignId {
            return try {
                CampaignId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid CampaignId format: $value", e)
            }
        }

        fun from(uuid: UUID): CampaignId = CampaignId(uuid)

        fun fromLong(value: Long): CampaignId {
            val uuid = UUID(0L, value)
            return CampaignId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Engagement ID Value Object
 */
@JvmInline
value class EngagementId(val value: UUID) {
    companion object {
        fun generate(): EngagementId = EngagementId(UUID.randomUUID())

        fun from(value: String): EngagementId {
            return try {
                EngagementId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid EngagementId format: $value", e)
            }
        }

        fun from(uuid: UUID): EngagementId = EngagementId(uuid)
    }

    override fun toString(): String = value.toString()
}

/**
 * User ID Value Object
 */
@JvmInline
value class UserId(val value: UUID) {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())

        fun from(value: String): UserId {
            return try {
                UserId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid UserId format: $value", e)
            }
        }

        fun from(uuid: UUID): UserId = UserId(uuid)

        fun fromLong(value: Long): UserId {
            val uuid = UUID(0L, value)
            return UserId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Session ID Value Object
 */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Session ID cannot be blank" }
        require(value.length <= 100) { "Session ID cannot exceed 100 characters" }
    }

    companion object {
        fun generate(): SessionId {
            return SessionId(UUID.randomUUID().toString())
        }

        fun from(value: String): SessionId = SessionId(value)
    }

    override fun toString(): String = value
}

/**
 * Raffle Entry ID Value Object
 */
@JvmInline
value class RaffleEntryId(val value: UUID) {
    companion object {
        fun generate(): RaffleEntryId = RaffleEntryId(UUID.randomUUID())

        fun from(value: String): RaffleEntryId {
            return try {
                RaffleEntryId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid RaffleEntryId format: $value", e)
            }
        }

        fun from(uuid: UUID): RaffleEntryId = RaffleEntryId(uuid)

        fun fromLong(value: Long): RaffleEntryId {
            val uuid = UUID(0L, value)
            return RaffleEntryId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}