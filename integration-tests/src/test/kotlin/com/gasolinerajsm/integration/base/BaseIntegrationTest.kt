package com.gasolinerajsm.integration.base

import com.fasterxml.jackson.databind.ObjectMapper
import com.gasolinerajsm.integration.config.IntegrationTestConfiguration
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Base class for integration tests with TestContainers setup
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.profiles.active=integration-test"
    ]
)
@ActiveProfiles("integration-test")
@Testcontainers
@Import(IntegrationTestConfiguration::class)
abstract class BaseIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("gasolinera_test_db")
            withUsername("test_user")
            withPassword("test_password")
            withReuse(true)
        }

        @Container
        @JvmStatic
        val rabbitMQContainer = RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine")).apply {
            withUser("test_user", "test_password")
            withVhost("test_vhost")
            withReuse(true)
        }

        @Container
        @JvmStatic
        val redisContainer = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
            withCommand("redis-server", "--requirepass", "test_password")
            withReuse(true)
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Database properties
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)

            // RabbitMQ properties
            registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost)
            registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort)
            registry.add("spring.rabbitmq.username") { "test_user" }
            registry.add("spring.rabbitmq.password") { "test_password" }
            registry.add("spring.rabbitmq.virtual-host") { "test_vhost" }

            // Redis properties
            registry.add("spring.redis.host", redisContainer::getHost)
            registry.add("spring.redis.port", redisContainer::getFirstMappedPort)
            registry.add("spring.redis.password") { "test_password" }
        }
    }

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory { _, _ -> objectMapper }
            )
    }

    /**
     * Get base URL for the current test server
     */
    protected fun baseUrl(): String = "http://localhost:$port"

    /**
     * Get service URL for external service calls
     */
    protected fun serviceUrl(serviceName: String, servicePort: Int): String {
        return "http://localhost:$servicePort"
    }
}