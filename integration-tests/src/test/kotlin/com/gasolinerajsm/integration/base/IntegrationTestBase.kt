package com.gasolinerajsm.integration.base

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest

/**
 * Base class for integration tests with simplified setup
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestBase {

    @Value("\${local.server.port}")
    protected lateinit var port: String

    protected fun baseUrl(): String = "http://localhost:$port"
}