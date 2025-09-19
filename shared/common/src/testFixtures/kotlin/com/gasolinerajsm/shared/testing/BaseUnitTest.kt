package com.gasolinerajsm.shared.testing

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import org.junit.jupiter.api.Tag

/**
 * Base class for unit tests using Kotest BehaviorSpec
 * Provides common configuration and utilities for unit tests
 */
@Tag("unit")
abstract class BaseUnitTest : BehaviorSpec() {

    init {
        // Clear all mocks before each test
        beforeEach {
            clearAllMocks()
        }
    }
}