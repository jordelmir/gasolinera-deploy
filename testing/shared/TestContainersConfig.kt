package com.gasolinerajsm.testing.shared

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName

/**
 * Shared TestContainers Configuration for Gasolinera JSM
 * Provides consistent container setup across all integration tests
 */
@TestConfiguration
class TestContainersConfig {

    companion object {
        // PostgreSQL Container
        @JvmStatic
        val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer(
            DockerImageName.parse("postgres:15-alpine")
        ).apply {
            withDatabaseName("gasolinera_test")
            withUsername("test_user")
            withPassword("test_password")
            withInitScript("test-data/init-test-db.sql")
            withReuse(true)
            withLabel("service", "gasolinera-postgres-test")
        }

        // Redis Container
        @JvmStatic
        val redisContainer: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("redis:7.2-alpine")
        ).apply {
            withExposedPorts(6379)
            withCommand("redis-server", "--requirepass", "test_password")
            withReuse(true)
            withLabel("service", "gasolinera-redis-test")
        }

        // RabbitMQ Container
        @JvmStatic
        val rabbitMQContainer: RabbitMQContainer = RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.12-management-alpine")
        ).apply {
            withUser("test_user", "test_password")
            withVhost("test_vhost")
            withPermission("test_vhost", "test_user", ".*", ".*", ".*")
            withReuse(true)
            withLabel("service", "gasolinera-rabbitmq-test")
        }

        // Vault Container (for secrets testing)
        @JvmStatic
        val vaultContainer: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("vault:1.15.2")
        ).apply {
            withExposedPorts(8200)
            withEnv("VAULT_DEV_ROOT_TOKEN_ID", "test-token")
            withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            withCommand("vault", "server", "-dev")
            withReuse(true)
            withLabel("service", "gasolinera-vault-test")
        }

        // Elasticsearch Container (for logging tests)
        @JvmStatic
        val elasticsearchContainer: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("elasticsearch:8.11.0")
        ).apply {
            withExposedPorts(9200, 9300)
            withEnv("discovery.type", "single-node")
            withEnv("xpack.security.enabled", "false")
            withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            withReuse(true)
            withLabel("service", "gasolinera-elasticsearch-test")
        }

        // Jaeger Container (for tracing tests)
        @JvmStatic
        val jaegerContainer: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("jaegertracing/all-in-one:1.50")
        ).apply {
            withExposedPorts(16686, 14268)
            withEnv("COLLECTOR_OTLP_ENABLED", "true")
            withReuse(true)
            withLabel("service", "gasolinera-jaeger-test")
        }

        // Start all containers
        @JvmStatic
        fun startAllContainers() {
            postgresContainer.start()
            redisContainer.start()
            rabbitMQContainer.start()
            vaultContainer.start()
            elasticsearchContainer.start()
            jaegerContainer.start()
        }

        // Stop all containers
        @JvmStatic
        fun stopAllContainers() {
            postgresContainer.stop()
            redisContainer.stop()
            rabbitMQContainer.stop()
            vaultContainer.stop()
            elasticsearchContainer.stop()
            jaegerContainer.stop()
        }

        // Configure dynamic properties for Spring Boot tests
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // PostgreSQL properties
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }

            // Redis properties
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "test_password" }

            // RabbitMQ properties
            registry.add("spring.rabbitmq.host") { rabbitMQContainer.host }
            registry.add("spring.rabbitmq.port") { rabbitMQContainer.amqpPort }
            registry.add("spring.rabbitmq.username") { "test_user" }
            registry.add("spring.rabbitmq.password") { "test_password" }
            registry.add("spring.rabbitmq.virtual-host") { "test_vhost" }

            // Vault properties
            registry.add("spring.cloud.vault.host") { vaultContainer.host }
            registry.add("spring.cloud.vault.port") { vaultContainer.getMappedPort(8200) }
            registry.add("spring.cloud.vault.token") { "test-token" }
            registry.add("spring.cloud.vault.scheme") { "http" }

            // Elasticsearch properties
            registry.add("spring.elasticsearch.uris") {
                "http://${elasticsearchContainer.host}:${elasticsearchContainer.getMappedPort(9200)}"
            }

            // Jaeger properties
            registry.add("management.tracing.jaeger.endpoint") {
                "http://${jaegerContainer.host}:${jaegerContainer.getMappedPort(14268)}/api/traces"
            }

            // Test-specific properties
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.jpa.show-sql") { "true" }
            registry.add("logging.level.org.springframework.web") { "DEBUG" }
            registry.add("logging.level.com.gasolinerajsm") { "DEBUG" }
        }
    }

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        return postgresContainer
    }

    @Bean
    @ServiceConnection
    fun redisContainer(): GenericContainer<*> {
        return redisContainer
    }

    @Bean
    @ServiceConnection
    fun rabbitMQContainer(): RabbitMQContainer {
        return rabbitMQContainer
    }
}

/**
 * Base class for integration tests with TestContainers
 */
abstract class BaseIntegrationTest {

    companion object {
        init {
            TestContainersConfig.startAllContainers()
        }
    }

    @DynamicPropertySource
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestContainersConfig.configureProperties(registry)
        }
    }
}

/**
 * Annotation for integration tests
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@org.springframework.boot.test.context.SpringBootTest(
    webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
)
@org.springframework.test.context.ActiveProfiles("test")
annotation class IntegrationTest

/**
 * Annotation for repository tests
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
annotation class RepositoryTest

/**
 * Annotation for web layer tests
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
@org.springframework.test.context.ActiveProfiles("test")
annotation class WebLayerTest