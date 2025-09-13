package com.gasolinerajsm.performance.config

import com.gasolinerajsm.performance.util.LoadTestExecutor
import com.gasolinerajsm.performance.util.MetricsCollector
import com.gasolinerajsm.performance.util.PerformanceReporter
import com.gasolinerajsm.performance.util.TestDataGenerator
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import java.time.Duration

/**
 * Configuration for performance tests
 */
@TestConfiguration
class PerformanceTestConfiguration {

    @Bean
    @Primary
    fun performanceTestRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(60))
            .requestFactory { HttpComponentsClientHttpRequestFactory() }
            .build()
    }

    @Bean
    fun metricsCollector(): MetricsCollector {
        return MetricsCollector()
    }

    @Bean
    fun performanceReporter(): PerformanceReporter {
        return PerformanceReporter()
    }

    @Bean
    fun loadTestExecutor(): LoadTestExecutor {
        return LoadTestExecutor()
    }

    @Bean
    fun testDataGenerator(): TestDataGenerator {
        return TestDataGenerator()
    }
}