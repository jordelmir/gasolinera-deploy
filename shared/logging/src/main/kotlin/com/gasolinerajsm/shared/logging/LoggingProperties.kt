package com.gasolinerajsm.shared.logging

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Propiedades de configuración para el sistema de logging
 */
@ConfigurationProperties(prefix = "gasolinera.logging")
data class LoggingProperties(
    val level: String = "INFO",
    val format: LogFormat = LogFormat.JSON,
    val mdc: MdcProperties = MdcProperties(),
    val correlation: CorrelationProperties = CorrelationProperties(),
    val business: BusinessLoggingProperties = BusinessLoggingProperties(),
    val elk: ElkProperties = ElkProperties()
) {

    data class MdcProperties(
        val enabled: Boolean = true,
        val includeUserId: Boolean = true,
        val includeSessionId: Boolean = true,
        val includeTraceId: Boolean = true,
        val includeSpanId: Boolean = true
    )

    data class CorrelationProperties(
        val headerName: String = "X-Correlation-ID",
        val mdcKey: String = "correlationId",
        val generateIfMissing: Boolean = true,
        val includeInResponse: Boolean = true
    )

    data class BusinessLoggingProperties(
        val enabled: Boolean = true,
        val includeUserActions: Boolean = true,
        val includeCouponOperations: Boolean = true,
        val includeRedemptionOperations: Boolean = true,
        val includeRaffleOperations: Boolean = true,
        val includeStationOperations: Boolean = true
    )

    data class ElkProperties(
        val enabled: Boolean = false,
        val host: String = "localhost",
        val port: Int = 9200,
        val index: String = "gasolinera-logs",
        val username: String? = null,
        val password: String? = null
    )
}

enum class LogFormat {
    JSON, PLAIN
}