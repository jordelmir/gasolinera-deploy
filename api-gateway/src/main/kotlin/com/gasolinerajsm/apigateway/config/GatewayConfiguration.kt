package com.gasolinerajsm.apigateway.config

import com.gasolinerajsm.apigateway.security.JwtProperties
import com.gasolinerajsm.shared.resilience.ResilienceConfiguration
import com.gasolinerajsm.shared.vault.VaultConfiguration
import com.gasolinerajsm.shared.monitoring.MetricsConfiguration
import com.gasolinerajsm.shared.logging.LoggingConfiguration
import com.gasolinerajsm.shared.tracing.TracingConfiguration
import com.gasolinerajsm.shared.health.HealthConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Configuración principal del API Gateway
 */
@Configuration
@EnableConfigurationProperties(JwtProperties::class)
@Import(
    ResilienceConfiguration::class,
    VaultConfiguration::class,
    MetricsConfiguration::class,
    LoggingConfiguration::class,
    TracingConfiguration::class,
    HealthConfiguration::class
)
class GatewayConfiguration