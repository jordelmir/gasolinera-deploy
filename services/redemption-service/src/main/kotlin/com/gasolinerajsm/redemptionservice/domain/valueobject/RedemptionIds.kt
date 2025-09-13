package com.gasolinerajsm.redemptionservice.domain.valueobject

import java.util.*

/**
 * Redemption ID Value Object
 */
@JvmInline
value class RedemptionId(val value: UUID) {
    companion object {
        fun generate(): RedemptionId = RedemptionId(UUID.randomUUID())

        fun from(value: String): RedemptionId {
            return try {
                RedemptionId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid RedemptionId format: $value", e)
            }
        }

        fun from(uuid: UUID): RedemptionId = RedemptionId(uuid)
    }

    override fun toString(): String = value.toString()
}

/**
 * Raffle Ticket ID Value Object
 */
@JvmInline
value class RaffleTicketId(val value: UUID) {
    companion object {
        fun generate(): RaffleTicketId = RaffleTicketId(UUID.randomUUID())

        fun from(value: String): RaffleTicketId {
            return try {
                RaffleTicketId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid RaffleTicketId format: $value", e)
            }
        }

        fun from(uuid: UUID): RaffleTicketId = RaffleTicketId(uuid)
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
            // Convert Long to UUID for compatibility with existing system
            val uuid = UUID(0L, value)
            return UserId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Station ID Value Object
 */
@JvmInline
value class StationId(val value: UUID) {
    companion object {
        fun generate(): StationId = StationId(UUID.randomUUID())

        fun from(value: String): StationId {
            return try {
                StationId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid StationId format: $value", e)
            }
        }

        fun from(uuid: UUID): StationId = StationId(uuid)

        fun fromLong(value: Long): StationId {
            val uuid = UUID(0L, value)
            return StationId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Employee ID Value Object
 */
@JvmInline
value class EmployeeId(val value: UUID) {
    companion object {
        fun generate(): EmployeeId = EmployeeId(UUID.randomUUID())

        fun from(value: String): EmployeeId {
            return try {
                EmployeeId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid EmployeeId format: $value", e)
            }
        }

        fun from(uuid: UUID): EmployeeId = EmployeeId(uuid)

        fun fromLong(value: Long): EmployeeId {
            val uuid = UUID(0L, value)
            return EmployeeId(uuid)
        }
    }

    override fun toString(): String = value.toString()

    fun toLong(): Long = value.leastSignificantBits
}

/**
 * Coupon ID Value Object
 */
@JvmInline
value class CouponId(val value: UUID) {
    companion object {
        fun generate(): CouponId = CouponId(UUID.randomUUID())

        fun from(value: String): CouponId {
            return try {
                CouponId(UUID.fromString(value))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid CouponId format: $value", e)
            }
        }

        fun from(uuid: UUID): CouponId = CouponId(uuid)

        fun fromLong(value: Long): CouponId {
            val uuid = UUID(0L, value)
            return CouponId(uuid)
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