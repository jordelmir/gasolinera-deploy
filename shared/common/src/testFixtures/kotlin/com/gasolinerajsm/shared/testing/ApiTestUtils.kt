package com.gasolinerajsm.shared.testing

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType.APPLICATION_JSON

/**
 * Utilidades para pruebas de APIs con MockMvc
 */
class ApiTestUtils(
    private val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {

    /**
     * Realiza un POST con JSON
     */
    fun postJson(url: String, body: Any): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
    }

    /**
     * Realiza un PUT con JSON
     */
    fun putJson(url: String, body: Any): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
    }

    /**
     * Realiza un GET
     */
    fun get(url: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON)
        )
    }

    /**
     * Realiza un DELETE
     */
    fun delete(url: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.delete(url)
                .contentType(MediaType.APPLICATION_JSON)
        )
    }

    /**
     * Método para validar respuesta HTTP 200 OK
     */
    fun ResultActions.expectOk(): ResultActions {
        return this.andExpect(status().isOk)
    }
}
