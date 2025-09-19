package com.gasolinerajsm.integration.suite

import com.gasolinerajsm.integration.e2e.CouponRedemptionFlowTest
import com.gasolinerajsm.integration.e2e.RaffleParticipationFlowTest
import com.gasolinerajsm.integration.e2e.UserAuthenticationFlowTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

/**
 * Test suite for all end-to-end integration tests
 *
 * This suite runs all E2E tests in a specific order to ensure proper test isolation
 * and data consistency across test scenarios.
 */
@Suite
@SelectClasses(
    UserAuthenticationFlowTest::class,
    CouponRedemptionFlowTest::class,
    RaffleParticipationFlowTest::class
)
@Tag("e2e")
@Tag("integration")
@DisplayName("End-to-End Integration Test Suite")
class E2ETestSuite