package com.gasolinerajsm.shared.testing

import com.gasolinerajsm.shared.testcontainers.TestContainersConfig
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for integration tests
 * Provides common configuration for all integration tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@ContextConfiguration(classes = [TestContainersConfig::class])
@Testcontainers
@Tag("integration")
abstract class BaseIntegrationTest {

    companion object {
        init {
            // Configure TestContainers
            System.setProperty("testcontainers.reuse.enable", "true")
            System.setProperty("testcontainers.ryuk.disabled", "true")
        }
    }
}