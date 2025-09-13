package com.gasolinerajsm.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import java.time.Duration

/**
 * Test configuration for integration tests
 */
@TestConfiguration
class IntegrationTestConfiguration {

    @Bean
    @Primary
    fun testRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(30))
            .requestFactory { HttpComponentsClientHttpRequestFactory() }
            .build()
    }
}