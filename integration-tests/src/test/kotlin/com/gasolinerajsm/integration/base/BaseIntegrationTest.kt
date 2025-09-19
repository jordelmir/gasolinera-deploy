package com.gasolinerajsm.integration.base

import org.springframework.beans.factory.annotation.Value

/**
 * Base class for integration tests with common utilities
 */
abstract class BaseIntegrationTest : IntegrationTestBase() {

    @Value("\${local.server.port}")
    override lateinit var port: String

    override fun baseUrl(): String = "http://localhost:$port"
}