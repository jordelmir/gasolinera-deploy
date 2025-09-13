package com.gasolinerajsm.shared.logging

import org.springframework.stereotype.Component
import java.util.*

/**
 * Generador de IDs de correlación para tracking de requests cross-service
 */
@Component
class CorrelationIdGenerator {

    /**
     * Genera un nuevo correlation ID único
     */
    fun generate(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    /**
     * Genera un correlation ID con prefijo personalizado
     */
    fun generateWithPrefix(prefix: String): String {
        return "${prefix}-${generate()}"
    }

    /**
     * Valida si un correlation ID tiene el formato correcto
     */
    fun isValid(correlationId: String?): Boolean {
        if (correlationId.isNullOrBlank()) return false

        // Formato básico: 16 caracteres alfanuméricos o con prefijo
        val basicPattern = "^[a-zA-Z0-9]{16}$".toRegex()
        val prefixPattern = "^[a-zA-Z0-9]+-[a-zA-Z0-9]{16}$".toRegex()

        return basicPattern.matches(correlationId) || prefixPattern.matches(correlationId)
    }

    /**
     * Extrae el ID base sin prefijo
     */
    fun extractBaseId(correlationId: String): String {
        return if (correlationId.contains("-")) {
            correlationId.substringAfterLast("-")
        } else {
            correlationId
        }
    }

    /**
     * Genera correlation ID para operaciones de negocio específicas
     */
    fun generateForBusinessOperation(operation: BusinessOperation): String {
        return generateWithPrefix(operation.prefix)
    }
}

/**
 * Tipos de operaciones de negocio para correlation IDs específicos
 */
enum class BusinessOperation(val prefix: String) {
    USER_REGISTRATION("usr-reg"),
    USER_AUTHENTICATION("usr-auth"),
    COUPON_GENERATION("cpn-gen"),
    COUPON_REDEMPTION("cpn-red"),
    RAFFLE_PARTICIPATION("rfl-part"),
    RAFFLE_DRAW("rfl-draw"),
    STATION_OPERATION("stn-op"),
    AD_ENGAGEMENT("ad-eng"),
    PAYMENT_PROCESSING("pay-proc")
}