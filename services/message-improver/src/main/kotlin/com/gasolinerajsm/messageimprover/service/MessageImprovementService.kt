package com.gasolinerajsm.messageimprover.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Service
class MessageImprovementService(
    private val geminiRestTemplate: RestTemplate,
    @Value("\${gemini.api.key}") private val geminiApiKey: String,
    @Value("\${gemini.api.base-url}") private val geminiBaseUrl: String,
    @Value("\${gemini.api.model}") private val geminiModel: String,
    @Value("\${gemini.generation.temperature}") private val temperature: Double,
    @Value("\${gemini.generation.max-output-tokens}") private val maxOutputTokens: Int
) {
    private val logger = LoggerFactory.getLogger(MessageImprovementService::class.java)

    private val geminiUrl = "$geminiBaseUrl/models/$geminiModel:generateContent?key=$geminiApiKey"

    fun improveMessage(request: com.gasolinerajsm.messageimprover.dto.MessageImprovementRequest): com.gasolinerajsm.messageimprover.dto.MessageImprovementResponse {
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
                "temperature" to temperature,
                "maxOutputTokens" to maxOutputTokens
            )
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(geminiRequest, headers)

        return try {
            val response = geminiRestTemplate.postForObject(geminiUrl, httpEntity, Map::class.java)

            val candidates = response?.get("candidates") as? List<Map<String, Any>>
            val firstCandidate = candidates?.firstOrNull()
            val content = firstCandidate?.get("content") as? Map<String, Any>
            val parts = content?.get("parts") as? List<Map<String, Any>>
            val text = parts?.firstOrNull()?.get("text") as? String ?: ""

            com.gasolinerajsm.messageimprover.dto.MessageImprovementResponse(
                originalMessage = request.message,
                improvedMessage = text.trim(),
                improvements = extractImprovements(text),
                confidence = 0.85
            )
        } catch (e: Exception) {
            logger.error("Error calling Gemini API", e)
            com.gasolinerajsm.messageimprover.dto.MessageImprovementResponse(
                originalMessage = request.message,
                improvedMessage = request.message,
                improvements = listOf("Error al procesar la solicitud con Gemini"),
                confidence = 0.0
            )
        }
    }

    private fun buildPrompt(request: com.gasolinerajsm.messageimprover.dto.MessageImprovementRequest): String {
        val projectContext = buildProjectContext(request)
        val serviceContext = detectServiceContext(request.message)

        val basePrompt = """
            Mejora el siguiente mensaje optimizándolo para claridad, precisión y efectividad.
            Analiza el contenido en profundidad, incorpora contexto relevante del proyecto gasolinera-jsm-ultimate,
            elimina ambigüedades, reformula frases complejas en lenguaje claro y accesible,
            y propone mejoras en el tono o estructura según el contexto técnico.

            $projectContext
            $serviceContext

            Mensaje original: "${request.message}"

        """.trimIndent()

        val contextPart = request.context?.let { "Contexto adicional: $it\n" } ?: ""
        val audiencePart = request.targetAudience?.let { "Audiencia objetivo: $it\n" } ?: ""
        val tonePart = request.tone?.let { "Tono deseado: $it\n" } ?: ""

        return basePrompt + contextPart + audiencePart + tonePart +
                "\nProporciona la versión mejorada del mensaje con mejoras específicas basadas en el contexto del proyecto."
    }

    private fun buildProjectContext(request: com.gasolinerajsm.messageimprover.dto.MessageImprovementRequest): String {
        return """
            CONTEXTO DEL PROYECTO GASOLINERA-JSM-ULTIMATE:

            Servicios disponibles:
            - auth-service: Autenticación y gestión de usuarios
            - ad-engine: Motor de anuncios y engagement
            - station-service: Gestión de estaciones de servicio
            - coupon-service: Sistema de cupones y campañas
            - raffle-service: Sistema de rifas y sorteos
            - redemption-service: Servicio de canje de premios
            - message-improver: Servicio de mejora de mensajes (este servicio)

            Eventos de dominio principales:
            - Auth: UserCreated, UserLoggedIn, UserPhoneVerified, UserAccountLocked
            - Ad-Engine: EngagementStarted, EngagementCompleted, TicketsAwarded, AdvertisementCreated
            - Raffle: RaffleCreated, RaffleActivated, RaffleDrawCompleted
            - Redemption: RedemptionCreated, RedemptionCompleted, RaffleTicketGenerated
            - Coupon: CouponCreated, CouponUsed, CouponExpired

            Tareas pendientes críticas (TODOs):
            - Implementar RequestOtpUseCase en auth-service
            - Completar integración SMS provider
            - Resolver dependencias faltantes en raffle-service (PointsLedgerRepository)
            - Implementar métodos faltantes en controladores (station-service, coupon-service)
            - Agregar validación de fraude en redemption-service

            Archivos modificados recientemente:
            - EngagementController.kt (ad-engine)
            - AuthController.kt (auth-service)
            - StationController.kt (station-service)
            - CouponController.kt (coupon-service)
            - RaffleController.kt (raffle-service)

            Tecnologías y dependencias:
            - Kotlin + Spring Boot (framework principal)
            - PostgreSQL para persistencia de datos
            - RabbitMQ para mensajería asíncrona
            - Redis para cache y sesiones
            - Docker para contenedorización
            - Gradle para gestión de builds
            - Gemini AI API para procesamiento de lenguaje natural

            Errores comunes y problemas conocidos:
            - Dependencias faltantes en raffle-service (PointsLedgerRepository)
            - Integración SMS pendiente en auth-service
            - Métodos no implementados en controladores (station-service, coupon-service)
            - Problemas de compilación en tests de integración
            - Configuración de mensajería RabbitMQ
            - Manejo de excepciones en servicios de dominio
        """.trimIndent()
    }

    private fun detectServiceContext(message: String): String {
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("auth") || lowerMessage.contains("login") || lowerMessage.contains("usuario") ->
                """
                CONTEXTO ESPECÍFICO - AUTH SERVICE:
                - Maneja autenticación OTP y JWT
                - Eventos: UserCreated, UserLoggedIn, UserPhoneVerified
                - TODO: Implementar RequestOtpUseCase, integración SMS
                """

            lowerMessage.contains("ad") || lowerMessage.contains("anuncio") || lowerMessage.contains("engagement") ->
                """
                CONTEXTO ESPECÍFICO - AD-ENGINE:
                - Gestiona anuncios y engagement de usuarios
                - Eventos: EngagementStarted, TicketsAwarded, AdvertisementCreated
                - TODO: Implementar lógica de performance real
                """

            lowerMessage.contains("station") || lowerMessage.contains("estacion") ->
                """
                CONTEXTO ESPECÍFICO - STATION SERVICE:
                - Gestiona estaciones de servicio y ubicaciones
                - TODO: Implementar métodos faltantes en StationController
                """

            lowerMessage.contains("coupon") || lowerMessage.contains("cupon") || lowerMessage.contains("campaña") ->
                """
                CONTEXTO ESPECÍFICO - COUPON SERVICE:
                - Sistema de cupones y campañas promocionales
                - TODO: Implementar métodos faltantes en CouponController
                """

            lowerMessage.contains("raffle") || lowerMessage.contains("rifa") || lowerMessage.contains("sorteo") ->
                """
                CONTEXTO ESPECÍFICO - RAFFLE SERVICE:
                - Gestiona rifas y sorteos con Merkle trees
                - Eventos: RaffleCreated, RaffleDrawCompleted
                - TODO: Resolver dependencias faltantes (PointsLedgerRepository)
                """

            lowerMessage.contains("redemption") || lowerMessage.contains("canje") || lowerMessage.contains("premio") ->
                """
                CONTEXTO ESPECÍFICO - REDEMPTION SERVICE:
                - Maneja canje de premios y tickets
                - Eventos: RedemptionCreated, RaffleTicketGenerated
                - TODO: Implementar detección de fraude
                """

            else -> ""
        }
    }

    private fun extractImprovements(improvedMessage: String): List<String> {
        val improvements = mutableListOf<String>()

        // Mejoras básicas
        improvements.add("Mejorada la claridad del mensaje")
        improvements.add("Optimizada la estructura")

        // Mejoras contextuales basadas en el contenido
        val lowerMessage = improvedMessage.lowercase()

        if (lowerMessage.contains("kotlin") || lowerMessage.contains("spring") || lowerMessage.contains("service")) {
            improvements.add("Incorporado contexto técnico específico del proyecto")
        }

        if (lowerMessage.contains("todo") || lowerMessage.contains("implementar") || lowerMessage.contains("faltante")) {
            improvements.add("Referenciado tareas pendientes críticas del proyecto")
        }

        if (lowerMessage.contains("evento") || lowerMessage.contains("domain") || lowerMessage.contains("event")) {
            improvements.add("Incluido contexto de eventos de dominio relevantes")
        }

        if (lowerMessage.contains("dependencia") || lowerMessage.contains("repository") || lowerMessage.contains("service")) {
            improvements.add("Mencionadas dependencias externas y posibles errores")
        }

        if (lowerMessage.contains("auth") || lowerMessage.contains("login") || lowerMessage.contains("usuario")) {
            improvements.add("Enfocado en contexto específico del auth-service")
        }

        if (lowerMessage.contains("ad") || lowerMessage.contains("anuncio") || lowerMessage.contains("engagement")) {
            improvements.add("Enfocado en contexto específico del ad-engine")
        }

        if (lowerMessage.contains("raffle") || lowerMessage.contains("rifa") || lowerMessage.contains("sorteo")) {
            improvements.add("Enfocado en contexto específico del raffle-service")
        }

        if (lowerMessage.contains("coupon") || lowerMessage.contains("cupon") || lowerMessage.contains("campaña")) {
            improvements.add("Enfocado en contexto específico del coupon-service")
        }

        if (lowerMessage.contains("station") || lowerMessage.contains("estacion")) {
            improvements.add("Enfocado en contexto específico del station-service")
        }

        if (lowerMessage.contains("redemption") || lowerMessage.contains("canje") || lowerMessage.contains("premio")) {
            improvements.add("Enfocado en contexto específico del redemption-service")
        }

        improvements.add("Ajustado el tono para mayor efectividad técnica")

        return improvements.take(5) // Limitar a 5 mejoras más relevantes
    }
}