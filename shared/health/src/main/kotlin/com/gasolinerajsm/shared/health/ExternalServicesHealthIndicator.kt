package com.gasolinerajsm.shared.health

import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Health indicator para servicios externos
 */
@Component
class ExternalServicesHealthIndicator(
    private val properties: HealthProperties.ExternalServicesHealthProperties
) : HealthIndicator {

    private val restTemplate = RestTemplate()

    override fun health(): Health {
        if (!properties.enabled) {
            return Health.up()
                .withDetail("status", "disabled")
                .build()
        }

        return try {
            val serviceResults = mutableMapOf<String, Any>()
            var allHealthy = true
            var anyCriticalDown = false

            // Verificar cada servicio externo configurado
            for ((serviceName, serviceConfig) in properties.services) {
                val serviceHealth = checkExternalService(serviceName, serviceConfig)
                serviceResults[serviceName] = serviceHealth.details

                if (!serviceHealth.isHealthy) {
                    allHealthy = false
                    if (serviceConfig.critical) {
                        anyCriticalDown = true
                    }
                }
            }

            // Verificar servicios predeterminados de Gasolinera JSM
            val defaultServices = getDefaultServices()
            for ((serviceName, serviceConfig) in defaultServices) {
                val serviceHealth = checkExternalService(serviceName, serviceConfig)
                serviceResults[serviceName] = serviceHealth.details

                if (!serviceHealth.isHealthy) {
                    allHealthy = false
                    if (serviceConfig.critical) {
                        anyCriticalDown = true
                    }
                }
            }

            // Determinar estado de salud general
            val healthBuilder = when {
                anyCriticalDown -> Health.down()
                allHealthy -> Health.up()
                else -> Health.up() // Servicios no críticos pueden fallar
            }

            healthBuilder
                .withDetail("externalServices", "Gasolinera JSM Dependencies")
                .withDetail("services", serviceResults)
                .withDetail("summary", mapOf(
                    "totalServices" to (properties.services.size + defaultServices.size),
                    "healthyServices" to serviceResults.values.count {
                        (it as Map<*, *>)["isHealthy"] == true
                    },
                    "criticalServicesDown" to anyCriticalDown,
                    "allServicesHealthy" to allHealthy
                ))
                .withDetail("timestamp", Instant.now().toString())
                .build()

        } catch (e: Exception) {
            Health.down()
                .withDetail("externalServices", "Gasolinera JSM Dependencies")
                .withDetail("error", e.message ?: "Unknown error")
                .withDetail("errorType", e.javaClass.simpleName)
                .withDetail("timestamp", Instant.now().toString())
                .withException(e)
                .build()
        }
    }

    private fun checkExternalService(
        serviceName: String,
        config: HealthProperties.ExternalServiceConfig
    ): HealthCheckResult {
        val startTime = Instant.now()

        return try {
            // Crear future para timeout
            val future = CompletableFuture.supplyAsync {
                performHealthCheck(config)
            }

            val result = future.get(config.timeout.toMillis(), TimeUnit.MILLISECONDS)
            val duration = Duration.between(startTime, Instant.now())

            HealthCheckResult(
                isHealthy = result.isHealthy,
                details = mapOf(
                    "url" to config.url,
                    "isHealthy" to result.isHealthy,
                    "statusCode" to result.statusCode,
                    "responseTimeMs" to duration.toMillis(),
                    "critical" to config.critical,
                    "expectedStatus" to config.expectedStatus,
                    "timeout" to config.timeout.toMillis(),
                    "timestamp" to Instant.now().toString(),
                    "error" to result.error
                ).filterValues { it != null }
            )

        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())

            HealthCheckResult(
                isHealthy = false,
                details = mapOf(
                    "url" to config.url,
                    "isHealthy" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "errorType" to e.javaClass.simpleName,
                    "responseTimeMs" to duration.toMillis(),
                    "critical" to config.critical,
                    "timeout" to config.timeout.toMillis(),
                    "timestamp" to Instant.now().toString()
                )
            )
        }
    }

    private fun performHealthCheck(config: HealthProperties.ExternalServiceConfig): ServiceHealthResult {
        return try {
            val uri = URI.create(config.url)

            // Configurar headers si existen
            val headers = org.springframework.http.HttpHeaders()
            config.headers.forEach { (key, value) ->
                headers.set(key, value)
            }

            val entity = org.springframework.http.HttpEntity<String>(headers)

            // Realizar request
            val response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                String::class.java
            )

            val isHealthy = response.statusCode.value() == config.expectedStatus

            ServiceHealthResult(
                isHealthy = isHealthy,
                statusCode = response.statusCode.value(),
                error = if (!isHealthy) "Unexpected status code: ${response.statusCode.value()}" else null
            )

        } catch (e: Exception) {
            ServiceHealthResult(
                isHealthy = false,
                statusCode = null,
                error = e.message ?: "Connection failed"
            )
        }
    }

    private fun getDefaultServices(): Map<String, HealthProperties.ExternalServiceConfig> {
        return mapOf(
            "auth-service" to HealthProperties.ExternalServiceConfig(
                url = "http://auth-service:8080/actuator/health",
                timeout = Duration.ofSeconds(5),
                critical = true,
                expectedStatus = 200
            ),
            "coupon-service" to HealthProperties.ExternalServiceConfig(
                url = "http://coupon-service:8080/actuator/health",
                timeout = Duration.ofSeconds(5),
                critical = true,
                expectedStatus = 200
            ),
            "station-service" to HealthProperties.ExternalServiceConfig(
                url = "http://station-service:8080/actuator/health",
                timeout = Duration.ofSeconds(5),
                critical = false,
                expectedStatus = 200
            ),
            "redemption-service" to HealthProperties.ExternalServiceConfig(
                url = "http://redemption-service:8080/actuator/health",
                timeout = Duration.ofSeconds(5),
                critical = true,
                expectedStatus = 200
            ),
            "raffle-service" to HealthProperties.ExternalServiceConfig(
                url = "http://raffle-service:8080/actuator/health",
                timeout = Duration.ofSeconds(5),
                critical = false,
                expectedStatus = 200
            ),
            "ad-engine" to HealthProperties.ExternalServiceConfig(
                url = "http://ad-engine:8080/actuator/health",
                timeout = Duration.ofSeconds(5),
                critical = false,
                expectedStatus = 200
            )
        )
    }

    /**
     * Verifica la salud de un servicio específico
     */
    fun checkSpecificService(serviceName: String): ServiceHealthDetails? {
        val config = properties.services[serviceName] ?: getDefaultServices()[serviceName]
        return config?.let {
            val result = checkExternalService(serviceName, it)
            ServiceHealthDetails(
                serviceName = serviceName,
                url = config.url,
                isHealthy = result.isHealthy,
                isCritical = config.critical,
                details = result.details,
                timestamp = Instant.now()
            )
        }
    }

    /**
     * Obtiene el estado de todos los servicios externos
     */
    fun getAllServicesHealth(): Map<String, ServiceHealthDetails> {
        val results = mutableMapOf<String, ServiceHealthDetails>()

        // Servicios configurados
        properties.services.forEach { (serviceName, config) ->
            val result = checkExternalService(serviceName, config)
            results[serviceName] = ServiceHealthDetails(
                serviceName = serviceName,
                url = config.url,
                isHealthy = result.isHealthy,
                isCritical = config.critical,
                details = result.details,
                timestamp = Instant.now()
            )
        }

        // Servicios por defecto
        getDefaultServices().forEach { (serviceName, config) ->
            if (!results.containsKey(serviceName)) {
                val result = checkExternalService(serviceName, config)
                results[serviceName] = ServiceHealthDetails(
                    serviceName = serviceName,
                    url = config.url,
                    isHealthy = result.isHealthy,
                    isCritical = config.critical,
                    details = result.details,
                    timestamp = Instant.now()
                )
            }
        }

        return results
    }
}

/**
 * Resultado de health check de un servicio
 */
data class ServiceHealthResult(
    val isHealthy: Boolean,
    val statusCode: Int? = null,
    val error: String? = null
)

/**
 * Detalles de salud de un servicio específico
 */
data class ServiceHealthDetails(
    val serviceName: String,
    val url: String,
    val isHealthy: Boolean,
    val isCritical: Boolean,
    val details: Map<String, Any>,
    val timestamp: Instant
)