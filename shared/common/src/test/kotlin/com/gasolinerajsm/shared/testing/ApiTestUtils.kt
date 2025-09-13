package com.gasolinerajsm.shared.testing

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Utility class for API testing with MockMvc
 * Provides common methods for testing REST endpoints
 */
class ApiTestUtils(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    /**
     * Perform GET request
     */
    fun get(url: String, headers: Map<String, String> = emptyMap()): ResultActions {
        val requestBuilder = get(url)
            .contentType(MediaType.APPLICATION_JSON)

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        return mockMvc.perform(requestBuilder)
    }

    /**
     * Perform POST request with JSON body
     */
    fun post(url: String, body: Any? = null, headers: Map<String, String> = emptyMap()): ResultActions {
        val requestBuilder = post(url)
            .contentType(MediaType.APPLICATION_JSON)

        body?.let {
            requestBuilder.content(objectMapper.writeValueAsString(it))
        }

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        return mockMvc.perform(requestBuilder)
    }

    /**
     * Perform PUT request with JSON body
     */
    fun put(url: String, body: Any? = null, headers: Map<String, String> = emptyMap()): ResultActions {
        val requestBuilder = put(url)
            .contentType(MediaType.APPLICATION_JSON)

        body?.let {
            requestBuilder.content(objectMapper.writeValueAsString(it))
        }

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        return mockMvc.perform(requestBuilder)
    }

    /**
     * Perform DELETE request
     */
    fun delete(url: String, headers: Map<String, String> = emptyMap()): ResultActions {
        val requestBuilder = delete(url)
            .contentType(MediaType.APPLICATION_JSON)

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        return mockMvc.perform(requestBuilder)
    }

    /**
     * Create authorization header with JWT token
     */
    fun authHeader(token: String): Map<String, String> {
        return mapOf("Authorization" to "Bearer $token")
    }

    /**
     * Expect successful response (2xx)
     */
    fun ResultActions.expectSuccess(): ResultActions {
        return this.andExpect(status().is2xxSuccessful)
    }

    /**
     * Expect specific status code
     */
    fun ResultActions.expectStatus(statusCode: Int): ResultActions {
        return this.andExpect(status().`is`(statusCode))
    }

    /**
     * Expect JSON response with specific field value
     */
    fun ResultActions.expectJsonField(fieldPath: String, expectedValue: Any): ResultActions {
        return this.andExpect(jsonPath(fieldPath).value(expectedValue))
    }

    /**
     * Expect JSON array with specific size
     */
    fun ResultActions.expectJsonArraySize(fieldPath: String, expectedSize: Int): ResultActions {
        return this.andExpect(jsonPath("$fieldPath.length()").value(expectedSize))
    }

    /**
     * Extract response body as string
     */
    fun ResultActions.extractResponseBody(): String {
        return this.andReturn().response.contentAsString
    }

    /**
     * Extract response body as object
     */
    inline fun <reified T> ResultActions.extractResponseAs(): T {
        val responseBody = this.extractResponseBody()
        return objectMapper.readValue(responseBody, T::class.java)
    }
}