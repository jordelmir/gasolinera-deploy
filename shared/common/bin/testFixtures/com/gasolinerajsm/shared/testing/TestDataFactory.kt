package com.gasolinerajsm.shared.testing

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

/**
 * Factory for creating test data objects
 * Provides consistent test data generation across all services
 */
object TestDataFactory {

    private val random = Random.Default

    /**
     * Generate a random UUID string
     */
    fun randomUuid(): String = UUID.randomUUID().toString()

    /**
     * Generate a random Long ID
     */
    fun randomId(): Long = random.nextLong(1, Long.MAX_VALUE)

    /**
     * Generate a random string with specified length
     */
    fun randomString(length: Int = 10): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Generate a random email
     */
    fun randomEmail(): String = "${randomString(8)}@${randomString(5)}.com"

    /**
     * Generate a random phone number
     */
    fun randomPhoneNumber(): String = "+1${random.nextInt(100, 999)}${random.nextInt(100, 999)}${random.nextInt(1000, 9999)}"

    /**
     * Generate a random BigDecimal amount
     */
    fun randomAmount(min: Double = 1.0, max: Double = 1000.0): BigDecimal {
        val value = random.nextDouble(min, max)
        return BigDecimal.valueOf(value).setScale(2, BigDecimal.ROUND_HALF_UP)
    }

    /**
     * Generate a random integer within range
     */
    fun randomInt(min: Int = 1, max: Int = 100): Int = random.nextInt(min, max + 1)

    /**
     * Generate a random boolean
     */
    fun randomBoolean(): Boolean = random.nextBoolean()

    /**
     * Generate a random LocalDateTime within the last year
     */
    fun randomDateTime(): LocalDateTime {
        val now = LocalDateTime.now()
        val daysAgo = random.nextLong(0, 365)
        return now.minusDays(daysAgo)
    }

    /**
     * Generate a random future LocalDateTime
     */
    fun randomFutureDateTime(): LocalDateTime {
        val now = LocalDateTime.now()
        val daysFromNow = random.nextLong(1, 365)
        return now.plusDays(daysFromNow)
    }

    /**
     * Generate a random past LocalDateTime
     */
    fun randomPastDateTime(): LocalDateTime {
        val now = LocalDateTime.now()
        val daysAgo = random.nextLong(1, 365)
        return now.minusDays(daysAgo)
    }

    /**
     * Pick a random element from a list
     */
    fun <T> randomFrom(items: List<T>): T = items.random()

    /**
     * Pick a random enum value
     */
    inline fun <reified T : Enum<T>> randomEnum(): T {
        val values = enumValues<T>()
        return values.random()
    }

    /**
     * Generate a list of random items
     */
    fun <T> randomList(size: Int = randomInt(1, 5), generator: () -> T): List<T> {
        return (1..size).map { generator() }
    }

    /**
     * Generate a random address
     */
    fun randomAddress(): String = "${randomInt(1, 9999)} ${randomString(8)} St, ${randomString(6)}, ${randomString(2).uppercase()} ${randomInt(10000, 99999)}"

    /**
     * Generate a random coupon code
     */
    fun randomCouponCode(): String = "${randomString(4).uppercase()}-${randomString(4).uppercase()}-${randomString(4).uppercase()}"

    /**
     * Generate a random QR code data
     */
    fun randomQrCode(): String = "QR_${randomUuid()}"

    /**
     * Generate a random station name
     */
    fun randomStationName(): String = "${randomString(8)} Gas Station"

    /**
     * Generate a random campaign name
     */
    fun randomCampaignName(): String = "${randomString(6)} Campaign ${randomInt(1, 100)}"

    /**
     * Generate a random description
     */
    fun randomDescription(): String = "This is a test description for ${randomString(8)} with some random content."
}