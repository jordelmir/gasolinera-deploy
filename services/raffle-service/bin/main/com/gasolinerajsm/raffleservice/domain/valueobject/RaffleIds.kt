package com.gasolinerajsm.raffleservice.domain.valueobject

import java.util.*

/**
 * Raffle ID Value Object
 */
@JvmInline
value class RaffleId(val value: UUID) {
    companion object {
        fun generate(): RaffleId = RaffleId(UUID.randomUUID())

        fun from(value: String): RaffleId {
            return try {
                RaffleId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid RaffleId format: $value", e)
            }
        }

        fun from(uuid: UUID): RaffleId = RaffleId(uuid)

        fun fromLong(value: Long): RaffleId {
            val uuid = UUID(0L, value)
            return RaffleId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Prize ID Value Object
 */
@JvmInline
value class PrizeId(val value: UUID) {
    companion object {
        fun generate(): PrizeId = PrizeId(UUID.randomUUID())

        fun from(value: String): PrizeId {
            return try {
                PrizeId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid PrizeId format: $value", e)
            }
        }

        fun from(uuid: UUID): PrizeId = PrizeId(uuid)

        fun fromLong(value: Long): PrizeId {
            val uuid = UUID(0L, value)
            return PrizeId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Entry ID Value Object
 */
@JvmInline
value class EntryId(val value: UUID) {
    companion object {
        fun generate(): EntryId = EntryId(UUID.randomUUID())

        fun from(value: String): EntryId {
            return try {
                EntryId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid EntryId format: $value", e)
            }
        }

        fun from(uuid: UUID): EntryId = EntryId(uuid)

        fun fromLong(value: Long): EntryId {
            val uuid = UUID(0L, value)
            return EntryId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Winner ID Value Object
 */
@JvmInline
value class WinnerId(val value: UUID) {
    companion object {
        fun generate(): WinnerId = WinnerId(UUID.randomUUID())

        fun from(value: String): WinnerId {
            return try {
                WinnerId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid WinnerId format: $value", e)
            }
        }

        fun from(uuid: UUID): WinnerId = WinnerId(uuid)

        fun fromLong(value: Long): WinnerId {
            val uuid = UUID(0L, value)
            return WinnerId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
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
 * Ticket ID Value Object
 */
@JvmInline
value class TicketId(val value: UUID) {
    companion object {
        fun generate(): TicketId = TicketId(UUID.randomUUID())

        fun from(value: String): TicketId {
            return try {
                TicketId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid TicketId format: $value", e)
            }
        }

        fun from(uuid: UUID): TicketId = TicketId(uuid)

        fun fromLong(value: Long): TicketId {
            val uuid = UUID(0L, value)
            return TicketId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}