package com.gasolinerajsm.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import com.gasolinerajsm.integration.IntegrationTestApplication
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import org.junit.jupiter.api.TestInstance
import java.util.Date
import javax.sql.DataSource

@SpringBootTest(
    classes = [IntegrationTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("integration-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedemptionFlowTest {

    @Autowired
    private lateinit var dataSource: DataSource

    @Value("\${local.server.port}")
    private lateinit var port: String

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RedemptionFlowTest::class.java)
        private const val JWT_SECRET = "test_secret_key_for_integration_tests_123456"
    }

    @BeforeAll
    fun setup() {
        logger.info("Setting up RedemptionFlowTest")
        logger.info("Server port: $port")
        logger.info("DataSource available: ${dataSource != null}")
        RestAssured.port = port.toInt()
        logger.info("RestAssured configured with port: ${RestAssured.port}")
        // Aquí se podrían ejecutar migraciones o seeders si fuera necesario
    }

    @Test
    fun `should complete redemption flow successfully`() {
        logger.info("Starting redemption flow test")
        val jdbcTemplate = JdbcTemplate(dataSource)
        val userId = "test-user-123"
        val qrCode = "valid-qr-code-for-testing"

        // 1. Generar un token JWT válido (simulando que auth-service lo emitió)
        logger.info("Step 1: Generating JWT token for user: $userId")
        val token = generateTestToken(userId)
        logger.info("Generated token: ${token.substring(0, 20)}...")

        // 2. Llamar al redemption-service con el QR y el token
        logger.info("Step 2: Calling redemption service with QR code: $qrCode")
        try {
            val response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $token")
                .body("""{"qrCode": "$qrCode"}""")
                .post("/api/v1/redemptions")

            logger.info("Redemption service response status: ${response.statusCode}")
            logger.info("Response body: ${response.body.asString()}")

            val adUrl = response
                .then()
                .statusCode(200)
                .extract()
                .path<String>("adUrl")

            assertThat(adUrl).isNotNull()
            logger.info("Received Ad URL: $adUrl")
        } catch (e: Exception) {
            logger.error("Error calling redemption service", e)
            throw e
        }

        // 3. Simular la visualización del anuncio y llamar al endpoint de confirmación
        logger.info("Step 3: Confirming redemption")
        val redemptionId = "mock-redemption-id"
        try {
            val confirmResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $token")
                .body("""{"redemptionId": "$redemptionId"}""")
                .post("/api/v1/redemptions/confirm")

            logger.info("Confirmation response status: ${confirmResponse.statusCode}")
            confirmResponse.then().statusCode(200)
            logger.info("Redemption confirmed successfully")
        } catch (e: Exception) {
            logger.error("Error confirming redemption", e)
            throw e
        }

        // 4. Verificar en la base de datos que los puntos y la impresión fueron registrados
        logger.info("Step 4: Verifying database state")
        try {
            val pointsAdded: Int? = jdbcTemplate.queryForObject(
                "SELECT points FROM users WHERE id = ?",
                Int::class.java,
                userId
            )
            logger.info("Query result for user points: $pointsAdded")
            assert(pointsAdded != null && pointsAdded > 0)
            logger.info("User points confirmed in DB: $pointsAdded")
        } catch (e: Exception) {
            logger.error("Error querying user points", e)
            throw e
        }

        try {
            val impressionCount: Int? = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ad_impressions WHERE user_id = ?",
                Int::class.java,
                userId
            )
            logger.info("Query result for ad impressions: $impressionCount")
            assert(impressionCount == 1)
            logger.info("Ad impression confirmed in DB: $impressionCount")
        } catch (e: Exception) {
            logger.error("Error querying ad impressions", e)
            throw e
        }

        logger.info("Redemption flow test completed successfully")
    }

    private fun generateTestToken(userId: String): String {
        logger.debug("Generating test JWT token for user: $userId")
        val claims = mapOf("sub" to userId, "roles" to listOf("USER"))
        val token = Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + 600000)) // 10 minutos de validez
            .signWith(SignatureAlgorithm.HS256, JWT_SECRET.toByteArray())
            .compact()
        logger.debug("Test token generated successfully")
        return token
    }
}