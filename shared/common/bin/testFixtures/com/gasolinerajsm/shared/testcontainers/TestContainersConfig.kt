package com.gasolinerajsm.shared.testcontainers

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName

/**
 * TestContainers configuration for integration tests
 * Provides shared container instances across all services
 */
@TestConfiguration
class TestContainersConfig {

    companion object {
        // Shared container instances to avoid multiple containers
        private val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15"))
            .withDatabaseName("gasolinera_jsm_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true)

        private val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true)

        private val rabbitMQContainer = RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"))
            .withReuse(true)

        init {
            // Start containers once
            postgresContainer.start()
            redisContainer.start()
            rabbitMQContainer.start()

            // Set system properties for Spring Boot
            System.setProperty("spring.datasource.url", postgresContainer.jdbcUrl)
            System.setProperty("spring.datasource.username", postgresContainer.username)
            System.setProperty("spring.datasource.password", postgresContainer.password)
            System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver")

            System.setProperty("spring.redis.host", redisContainer.host)
            System.setProperty("spring.redis.port", redisContainer.getMappedPort(6379).toString())

            System.setProperty("spring.rabbitmq.host", rabbitMQContainer.host)
            System.setProperty("spring.rabbitmq.port", rabbitMQContainer.getMappedPort(5672).toString())
            System.setProperty("spring.rabbitmq.username", rabbitMQContainer.adminUsername)
            System.setProperty("spring.rabbitmq.password", rabbitMQContainer.adminPassword)
        }
    }

    @Bean
    @Primary
    fun testPostgreSQLContainer(): PostgreSQLContainer<*> = postgresContainer

    @Bean
    @Primary
    fun testRedisContainer(): GenericContainer<*> = redisContainer

    @Bean
    @Primary
    fun testRabbitMQContainer(): RabbitMQContainer = rabbitMQContainer
}