package com.gasolinerajsm.integration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

/**
 * Spring Boot application for integration tests
 *
 * This application provides the necessary Spring context for running
 * integration tests with all required beans and configurations.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = [
        "com.gasolinerajsm.integration",
        "com.gasolinerajsm.shared"
    ]
)
class IntegrationTestApplication

fun main(args: Array<String>) {
    runApplication<IntegrationTestApplication>(*args)
}