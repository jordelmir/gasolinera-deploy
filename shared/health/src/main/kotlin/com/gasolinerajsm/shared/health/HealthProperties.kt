package com.gasolinerajsm.shared.health

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Propiedades de configuración para health checks
 */
@ConfigurationProperties(prefix = "gasolinera.health")
data class HealthProperties(
    val enabled: Boolean = true,
    val database: DatabaseHealthProperties = DatabaseHealthProperties(),
    val redis: RedisHealthProperties = RedisHealthProperties(),
    val business: BusinessHealthProperties = BusinessHealthProperties(),
    val externalServices: ExternalServicesHealthProperties = ExternalServicesHealthProperties(),
    val systemResources: SystemResourcesHealthProperties = SystemResourcesHealthProperties(),
    val kubernetes: KubernetesHealthProperties = KubernetesHealthProperties()
) {

    data class DatabaseHealthProperties(
        val enabled: Boolean = true,
        val timeout: Duration = Duration.ofSeconds(5),
        val validationQuery: String = "SELECT 1",
        val connectionPoolThreshold: Int = 80, // Porcentaje
        val slowQueryThreshold: Duration = Duration.ofSeconds(2)
    )

    data class RedisHealthProperties(
        val enabled: Boolean = true,
        val timeout: Duration = Duration.ofSeconds(3),
        val testKey: String = "health:check",
        val memoryThreshold: Long = 85 // Porcentaje
    )

    data class BusinessHealthProperties(
        val enabled: Boolean = true,
        val checkCriticalOperations: Boolean = true,
        val errorRateThreshold: Double = 0.05, // 5%
        val responseTimeThreshold: Duration = Duration.ofSeconds(5)
    )

    data class ExternalServicesHealthProperties(
        val enabled: Boolean = true,
        val timeout: Duration = Duration.ofSeconds(10),
        val services: Map<String, ExternalServiceConfig> = emptyMap()
    )

    data class ExternalServiceConfig(
        val url: String,
        val timeout: Duration = Duration.ofSeconds(5),
        val critical: Boolean = false,
        val expectedStatus: Int = 200,
        val headers: Map<String, String> = emptyMap()
    )

    data class SystemResourcesHealthProperties(
        val enabled: Boolean = true,
        val cpuThreshold: Double = 80.0, // Porcentaje
        val memoryThreshold: Double = 85.0, // Porcentaje
        val diskThreshold: Double = 90.0, // Porcentaje
        val checkInterval: Duration = Duration.ofMinutes(1)
    )

    data class KubernetesHealthProperties(
        val readiness: ReadinessProperties = ReadinessProperties(),
        val liveness: LivenessProperties = LivenessProperties()
    )

    data class ReadinessProperties(
        val enabled: Boolean = true,
        val initialDelaySeconds: Int = 30,
        val periodSeconds: Int = 10,
        val timeoutSeconds: Int = 5,
        val failureThreshold: Int = 3,
        val successThreshold: Int = 1
    )

    data class LivenessProperties(
        val enabled: Boolean = true,
        val initialDelaySeconds: Int = 60,
        val periodSeconds: Int = 30,
        val timeoutSeconds: Int = 10,
        val failureThreshold: Int = 3,
        val successThreshold: Int = 1
    )
}