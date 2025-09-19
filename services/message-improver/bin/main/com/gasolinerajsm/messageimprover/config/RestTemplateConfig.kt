package com.gasolinerajsm.messageimprover.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * RestTemplate configuration for external API calls
 */
@Configuration
class RestTemplateConfig {

    @Value("\${gemini.api.timeout:30000}")
    private val timeout: Int = 30000

    @Bean
    fun restTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(timeout)
        factory.setReadTimeout(timeout)

        return RestTemplate(factory)
    }

    @Bean
    fun geminiRestTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(timeout)
        factory.setReadTimeout(timeout)

        val restTemplate = RestTemplate(factory)

        // Add interceptors for logging and error handling
        restTemplate.interceptors.add { request, body, execution ->
            val startTime = System.currentTimeMillis()

            try {
                val response = execution.execute(request, body)
                val duration = System.currentTimeMillis() - startTime

                // Log successful requests
                if (request.uri.toString().contains("gemini")) {
                    println("Gemini API call successful - Duration: ${duration}ms")
                }

                response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                println("Gemini API call failed - Duration: ${duration}ms, Error: ${e.message}")
                throw e
            }
        }

        return restTemplate
    }
}