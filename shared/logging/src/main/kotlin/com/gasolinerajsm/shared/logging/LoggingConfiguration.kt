package com.gasolinerajsm.shared.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuración central de logging estructurado para todos los servicios
 */
@Configuration
@EnableConfigurationProperties(LoggingProperties::class)
class LoggingConfiguration : WebMvcConfigurer {

    @Bean
    fun correlationIdGenerator(): CorrelationIdGenerator {
        return CorrelationIdGenerator()
    }

    @Bean
    fun structuredLogger(): StructuredLogger {
        return StructuredLogger()
    }

    @Bean
    fun loggingInterceptor(
        correlationIdGenerator: CorrelationIdGenerator,
        structuredLogger: StructuredLogger
    ): LoggingInterceptor {
        return LoggingInterceptor(correlationIdGenerator, structuredLogger)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "gasolinera.logging",
        name = ["mdc.enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun mdcLoggingFilter(): MdcLoggingFilter {
        return MdcLoggingFilter()
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggingInterceptor(correlationIdGenerator(), structuredLogger()))
    }

    companion object {
        init {
            // Configurar Logback para mostrar información de configuración
            val context = LoggerFactory.getILoggerFactory() as LoggerContext
            StatusPrinter.printInCaseOfErrorsOrWarnings(context)
        }
    }
}