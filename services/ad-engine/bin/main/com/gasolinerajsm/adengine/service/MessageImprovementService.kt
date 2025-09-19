package com.gasolinerajsm.adengine.service

import com.gasolinerajsm.adengine.config.GeminiConfig
import com.gasolinerajsm.adengine.dto.MessageImprovementRequest
import com.gasolinerajsm.adengine.dto.MessageImprovementResponse
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.RestClientException

@Service
class MessageImprovementService(
    private val geminiConfig: GeminiConfig,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    private val logger = LoggerFactory.getLogger(MessageImprovementService::class.java)

    fun improveMessage(request: MessageImprovementRequest): MessageImprovementResponse {
        val prompt = buildPrompt(request)

        val geminiRequest = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to geminiConfig.temperature,
                "maxOutputTokens" to geminiConfig.maxTokens
            )
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(geminiRequest, headers)

        return try {
            val response = restTemplate.postForObject(
                "${geminiConfig.baseUrl}/models/${geminiConfig.model}:generateContent?key=${geminiConfig.key}",
                httpEntity,
                Map::class.java
            )

            val candidates = response?.get("candidates") as? List<Map<String, Any>>
            val firstCandidate = candidates?.firstOrNull()
            val content = firstCandidate?.get("content") as? Map<String, Any>
            val parts = content?.get("parts") as? List<Map<String, Any>>
            val firstPart = parts?.firstOrNull()
            val improvedMessage = firstPart?.get("text") as? String ?: ""

            MessageImprovementResponse(
                originalMessage = request.message,
                improvedMessage = improvedMessage.trim(),
                improvements = extractImprovements(improvedMessage),
                confidence = 0.85 // Placeholder, could be calculated based on response
            )
        } catch (e: RestClientException) {
            logger.error("Error calling Gemini API", e)
            MessageImprovementResponse(
                originalMessage = request.message,
                improvedMessage = request.message,
                improvements = listOf("Error al procesar la solicitud"),
                confidence = 0.0
            )
        }
    }

    private fun buildPrompt(request: MessageImprovementRequest): String {
        val basePrompt = """
            Mejora el siguiente mensaje optimizándolo para claridad, precisión y efectividad.
            Analiza el contenido en profundidad, incorpora contexto relevante basado en patrones comunes,
            elimina ambigüedades, reformula frases complejas en lenguaje claro y accesible,
            y propone mejoras en el tono o estructura según el contexto.

            Mensaje original: "${request.message}"

        """.trimIndent()

        val contextPart = request.context?.let { "Contexto: $it\n" } ?: ""
        val audiencePart = request.targetAudience?.let { "Audiencia objetivo: $it\n" } ?: ""
        val tonePart = request.tone?.let { "Tono deseado: $it\n" } ?: ""

        return basePrompt + contextPart + audiencePart + tonePart +
               "\nProporciona la versión mejorada del mensaje."
    }

    private fun extractImprovements(improvedMessage: String): List<String> {
        // Simple extraction of potential improvements
        // In a real implementation, you might use the AI to analyze differences
        return listOf(
            "Mejorada la claridad del mensaje",
            "Optimizada la estructura",
            "Ajustado el tono para mayor efectividad"
        )
    }
}