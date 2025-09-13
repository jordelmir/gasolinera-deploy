package com.gasolinerajsm.testing.shared

import com.github.javafaker.Faker
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.random.Random

/**
 * Shared Test Data Factory for Gasolinera JSM
 * Provides consistent test data generation across all services
 */
object TestDataFactory {

    private val faker = Faker(Locale("es", "MX"))
    private val random = Random.Default

    // User Test Data
    object Users {
        fun createValidUser(
            id: UUID = UUID.randomUUID(),
            email: String = faker.internet().emailAddress(),
            phone: String = generateMexicanPhone(),
            firstName: String = faker.name().firstName(),
            lastName: String = faker.name().lastName(),
            isActive: Boolean = true,
            createdAt: LocalDateTime = LocalDateTime.now()
        ) = mapOf(
            "id" to id,
            "email" to email,
            "phone" to phone,
            "firstName" to firstName,
            "lastName" to lastName,
            "isActive" to isActive,
            "createdAt" to createdAt
        )

        fun createInvalidUser() = mapOf(
            "id" to UUID.randomUUID(),
            "email" to "invalid-email",
            "phone" to "123",
            "firstName" to "",
            "lastName" to "",
            "isActive" to false
        )

        private fun generateMexicanPhone(): String {
            val areaCode = listOf("55", "33", "81", "222", "442", "618").random()
            val number = (1000000..9999999).random()
            return "$areaCode$number"
        }
    }

    // Station Test Data
    object Stations {
        fun createValidStation(
            id: UUID = UUID.randomUUID(),
            name: String = "${faker.company().name()} Gas Station",
            address: String = faker.address().fullAddress(),
            latitude: Double = faker.address().latitude().toDouble(),
            longitude: Double = faker.address().longitude().toDouble(),
            isActive: Boolean = true,
            brandId: UUID = UUID.randomUUID()
        ) = mapOf(
            "id" to id,
            "name" to name,
            "address" to address,
            "latitude" to latitude,
            "longitude" to longitude,
            "isActive" to isActive,
            "brandId" to brandId
        )

        fun createStationWithFuelPrices(
            stationId: UUID = UUID.randomUUID(),
            regularPrice: BigDecimal = BigDecimal.valueOf(random.nextDouble(20.0, 25.0)),
            premiumPrice: BigDecimal = BigDecimal.valueOf(random.nextDouble(22.0, 27.0)),
            dieselPrice: BigDecimal = BigDecimal.valueOf(random.nextDouble(21.0, 26.0))
        ) = mapOf(
            "stationId" to stationId,
            "fuelPrices" to mapOf(
                "REGULAR" to regularPrice,
                "PREMIUM" to premiumPrice,
                "DIESEL" to dieselPrice
            ),
            "updatedAt" to LocalDateTime.now()
        )
    }

    // Coupon Test Data
    object Coupons {
        fun createValidCoupon(
            id: UUID = UUID.randomUUID(),
            userId: UUID = UUID.randomUUID(),
            stationId: UUID = UUID.randomUUID(),
            amount: BigDecimal = BigDecimal.valueOf(random.nextDouble(100.0, 1000.0)),
            fuelType: String = listOf("REGULAR", "PREMIUM", "DIESEL").random(),
            status: String = "ACTIVE",
            qrCode: String = generateQRCode(),
            expiresAt: LocalDateTime = LocalDateTime.now().plusDays(30)
        ) = mapOf(
            "id" to id,
            "userId" to userId,
            "stationId" to stationId,
            "amount" to amount,
            "fuelType" to fuelType,
            "status" to status,
            "qrCode" to qrCode,
            "expiresAt" to expiresAt,
            "createdAt" to LocalDateTime.now()
        )

        fun createExpiredCoupon(
            id: UUID = UUID.randomUUID(),
            userId: UUID = UUID.randomUUID()
        ) = createValidCoupon(
            id = id,
            userId = userId,
            status = "EXPIRED",
            expiresAt = LocalDateTime.now().minusDays(1)
        )

        fun createRedeemedCoupon(
            id: UUID = UUID.randomUUID(),
            userId: UUID = UUID.randomUUID()
        ) = createValidCoupon(
            id = id,
            userId = userId,
            status = "REDEEMED"
        )

        private fun generateQRCode(): String {
            return "QR_${UUID.randomUUID().toString().replace("-", "").uppercase()}"
        }
    }

    // Redemption Test Data
    object Redemptions {
        fun createValidRedemption(
            id: UUID = UUID.randomUUID(),
            couponId: UUID = UUID.randomUUID(),
            userId: UUID = UUID.randomUUID(),
            stationId: UUID = UUID.randomUUID(),
            amount: BigDecimal = BigDecimal.valueOf(random.nextDouble(100.0, 1000.0)),
            fuelType: String = listOf("REGULAR", "PREMIUM", "DIESEL").random(),
            ticketsGenerated: Int = random.nextInt(1, 10),
            multiplier: Double = random.nextDouble(1.0, 3.0)
        ) = mapOf(
            "id" to id,
            "couponId" to couponId,
            "userId" to userId,
            "stationId" to stationId,
            "amount" to amount,
            "fuelType" to fuelType,
            "ticketsGenerated" to ticketsGenerated,
            "multiplier" to multiplier,
            "redeemedAt" to LocalDateTime.now()
        )
    }

    // Raffle Test Data
    object Raffles {
        fun createValidRaffle(
            id: UUID = UUID.randomUUID(),
            name: String = "Rifa ${faker.commerce().productName()}",
            description: String = faker.lorem().paragraph(),
            prizeDescription: String = faker.commerce().productName(),
            prizeValue: BigDecimal = BigDecimal.valueOf(random.nextDouble(1000.0, 50000.0)),
            startDate: LocalDateTime = LocalDateTime.now(),
            endDate: LocalDateTime = LocalDateTime.now().plusDays(30),
            maxParticipants: Int = random.nextInt(100, 10000),
            status: String = "ACTIVE"
        ) = mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "prizeDescription" to prizeDescription,
            "prizeValue" to prizeValue,
            "startDate" to startDate,
            "endDate" to endDate,
            "maxParticipants" to maxParticipants,
            "status" to status,
            "createdAt" to LocalDateTime.now()
        )

        fun createRaffleTicket(
            id: UUID = UUID.randomUUID(),
            raffleId: UUID = UUID.randomUUID(),
            userId: UUID = UUID.randomUUID(),
            ticketNumber: String = generateTicketNumber(),
            isWinner: Boolean = false
        ) = mapOf(
            "id" to id,
            "raffleId" to raffleId,
            "userId" to userId,
            "ticketNumber" to ticketNumber,
            "isWinner" to isWinner,
            "createdAt" to LocalDateTime.now()
        )

        private fun generateTicketNumber(): String {
            return "T${System.currentTimeMillis()}${random.nextInt(1000, 9999)}"
        }
    }

    // Ad Campaign Test Data
    object AdCampaigns {
        fun createValidAdCampaign(
            id: UUID = UUID.randomUUID(),
            name: String = "Campaña ${faker.company().name()}",
            description: String = faker.lorem().paragraph(),
            imageUrl: String = faker.internet().url(),
            targetAudience: String = listOf("PREMIUM_USERS", "REGULAR_USERS", "ALL_USERS").random(),
            ticketMultiplier: Double = random.nextDouble(1.5, 3.0),
            startDate: LocalDateTime = LocalDateTime.now(),
            endDate: LocalDateTime = LocalDateTime.now().plusDays(15),
            isActive: Boolean = true
        ) = mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "imageUrl" to imageUrl,
            "targetAudience" to targetAudience,
            "ticketMultiplier" to ticketMultiplier,
            "startDate" to startDate,
            "endDate" to endDate,
            "isActive" to isActive
        )

        fun createAdEngagement(
            id: UUID = UUID.randomUUID(),
            campaignId: UUID = UUID.randomUUID(),
            userId: UUID = UUID.randomUUID(),
            engagementType: String = listOf("VIEW", "CLICK", "SHARE").random(),
            duration: Long = random.nextLong(1000, 30000)
        ) = mapOf(
            "id" to id,
            "campaignId" to campaignId,
            "userId" to userId,
            "engagementType" to engagementType,
            "duration" to duration,
            "timestamp" to LocalDateTime.now()
        )
    }

    // JWT Test Data
    object JWT {
        fun createValidJWTPayload(
            userId: UUID = UUID.randomUUID(),
            email: String = faker.internet().emailAddress(),
            roles: List<String> = listOf("USER"),
            permissions: List<String> = listOf("READ_COUPONS", "REDEEM_COUPONS"),
            exp: Long = System.currentTimeMillis() / 1000 + 3600, // 1 hour from now
            iat: Long = System.currentTimeMillis() / 1000
        ) = mapOf(
            "sub" to userId.toString(),
            "email" to email,
            "roles" to roles,
            "permissions" to permissions,
            "exp" to exp,
            "iat" to iat,
            "iss" to "gasolinera-jsm-auth"
        )

        fun createExpiredJWTPayload(
            userId: UUID = UUID.randomUUID()
        ) = createValidJWTPayload(
            userId = userId,
            exp = System.currentTimeMillis() / 1000 - 3600, // 1 hour ago
            iat = System.currentTimeMillis() / 1000 - 7200  // 2 hours ago
        )
    }

    // Database Test Data
    object Database {
        fun createTestDatabaseUrl(
            host: String = "localhost",
            port: Int = 5432,
            database: String = "test_db"
        ) = "jdbc:postgresql://$host:$port/$database"

        fun createRedisTestUrl(
            host: String = "localhost",
            port: Int = 6379
        ) = "redis://$host:$port"

        fun createRabbitMQTestUrl(
            host: String = "localhost",
            port: Int = 5672,
            username: String = "test",
            password: String = "test"
        ) = "amqp://$username:$password@$host:$port"
    }

    // Utility Methods
    fun randomUUID(): UUID = UUID.randomUUID()

    fun randomString(length: Int = 10): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    fun randomEmail(): String = faker.internet().emailAddress()

    fun randomBigDecimal(min: Double = 0.0, max: Double = 1000.0): BigDecimal {
        return BigDecimal.valueOf(random.nextDouble(min, max))
    }

    fun randomLocalDateTime(
        daysFromNow: Long = 0,
        hoursFromNow: Long = 0
    ): LocalDateTime {
        return LocalDateTime.now()
            .plusDays(daysFromNow)
            .plusHours(hoursFromNow)
    }

    fun randomPastDateTime(daysAgo: Long = 30): LocalDateTime {
        return LocalDateTime.now().minusDays(random.nextLong(1, daysAgo))
    }

    fun randomFutureDateTime(daysFromNow: Long = 30): LocalDateTime {
        return LocalDateTime.now().plusDays(random.nextLong(1, daysFromNow))
    }
}