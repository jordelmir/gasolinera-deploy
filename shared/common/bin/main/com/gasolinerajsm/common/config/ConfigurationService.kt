package com.gasolinerajsm.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.util.*
import jakarta.annotation.PostConstruct

interface ConfigurationService {
    fun loadEnvironment(environment: EnvironmentType): ConfigurationResult
    fun validateConfiguration(): ValidationResult
    fun getSecureValue(key: String): SecureValue
    fun reloadConfiguration(): ReloadResult
    fun getCriticalVariables(): List<String>
    fun getEnvironmentType(): EnvironmentType
}

@Service
class DefaultConfigurationService(
    private val environment: Environment,
    private val encryptionService: EncryptionService,
    @Value("\${app.environment:development}") private val currentEnvironment: String
) : ConfigurationService {

    private val criticalVariables = listOf(
        "DB_HOST", "DB_NAME", "DB_USERNAME", "DB_PASSWORD",
        "JWT_SECRET", "REDIS_HOST", "RABBITMQ_HOST",
        "AUTH_SERVICE_URL", "STATION_SERVICE_URL", "COUPON_SERVICE_URL",
        "RAFFLE_SERVICE_URL", "API_GATEWAY_URL"
    )

    private var configurationCache = mutableMapOf<String, String>()
    private var lastReloadTime = System.currentTimeMillis()

    @PostConstruct
    fun initialize() {
        val validationResult = validateConfiguration()
        if (!validationResult.isValid) {
            throw ConfigurationException(
                "Critical configuration validation failed: ${validationResult.missingVariables}"
            )
        }
        loadConfigurationCache()
    }

    override fun loadEnvironment(environment: EnvironmentType): ConfigurationResult {
        return try {
            val loadedProperties = mutableMapOf<String, String>()
            val missingCriticalVars = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Cargar variables críticas
            criticalVariables.forEach { key ->
                val value = this.environment.getProperty(key)
                if (value != null) {
                    loadedProperties[key] = if (isSensitiveKey(key)) "[MASKED]" else value
                } else {
                    missingCriticalVars.add(key)
                }
            }

            // Cargar variables específicas del entorno
            loadEnvironmentSpecificVariables(environment, loadedProperties, warnings)

            ConfigurationResult(
                environment = environment,
                loadedProperties = loadedProperties,
                missingCriticalVars = missingCriticalVars,
                warnings = warnings
            )
        } catch (e: Exception) {
            ConfigurationResult(
                environment = environment,
                loadedProperties = emptyMap(),
                missingCriticalVars = criticalVariables,
                warnings = listOf("Failed to load environment: ${e.message}")
            )
        }
    }

    override fun validateConfiguration(): ValidationResult {
        val missingVariables = mutableListOf<String>()
        val invalidVariables = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validar variables críticas
        criticalVariables.forEach { key ->
            val value = environment.getProperty(key)
            if (value.isNullOrBlank()) {
                missingVariables.add(key)
            } else {
                // Validaciones específicas
                when (key) {
                    "JWT_SECRET" -> {
                        if (value.length < 32) {
                            invalidVariables.add("$key: Must be at least 32 characters long")
                        }
                        if (value == "your-super-secret-jwt-key-change-this-in-production") {
                            warnings.add("$key: Using default value, change in production")
                        }
                    }
                    "DB_PASSWORD" -> {
                        if (value.length < 8) {
                            warnings.add("$key: Password should be at least 8 characters")
                        }
                    }
                    else -> {
                        if (key.endsWith("_URL") && !isValidUrl(value)) {
                            invalidVariables.add("$key: Invalid URL format")
                        }
                    }
                }
            }
        }

        return ValidationResult(
            isValid = missingVariables.isEmpty() && invalidVariables.isEmpty(),
            missingVariables = missingVariables,
            invalidVariables = invalidVariables,
            warnings = warnings
        )
    }

    override fun getSecureValue(key: String): SecureValue {
        return try {
            val value = environment.getProperty(key)
            if (value != null) {
                if (isSensitiveKey(key)) {
                    SecureValue(
                        key = key,
                        value = encryptionService.decrypt(value),
                        isEncrypted = true,
                        lastAccessed = System.currentTimeMillis()
                    )
                } else {
                    SecureValue(
                        key = key,
                        value = value,
                        isEncrypted = false,
                        lastAccessed = System.currentTimeMillis()
                    )
                }
            } else {
                SecureValue(
                    key = key,
                    value = null,
                    isEncrypted = false,
                    lastAccessed = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            SecureValue(
                key = key,
                value = null,
                isEncrypted = false,
                lastAccessed = System.currentTimeMillis(),
                error = e.message
            )
        }
    }

    override fun reloadConfiguration(): ReloadResult {
        return try {
            val oldConfigSize = configurationCache.size
            configurationCache.clear()
            loadConfigurationCache()

            val validationResult = validateConfiguration()
            lastReloadTime = System.currentTimeMillis()

            ReloadResult(
                success = validationResult.isValid,
                reloadedAt = lastReloadTime,
                configurationCount = configurationCache.size,
                previousCount = oldConfigSize,
                errors = if (!validationResult.isValid) validationResult.missingVariables else emptyList()
            )
        } catch (e: Exception) {
            ReloadResult(
                success = false,
                reloadedAt = System.currentTimeMillis(),
                configurationCount = 0,
                previousCount = configurationCache.size,
                errors = listOf(e.message ?: "Unknown error during reload")
            )
        }
    }

    override fun getCriticalVariables(): List<String> = criticalVariables

    override fun getEnvironmentType(): EnvironmentType {
        return when (currentEnvironment.lowercase()) {
            "development", "dev" -> EnvironmentType.DEVELOPMENT
            "staging", "stage" -> EnvironmentType.STAGING
            "production", "prod" -> EnvironmentType.PRODUCTION
            "test" -> EnvironmentType.TEST
            else -> EnvironmentType.DEVELOPMENT
        }
    }

    private fun loadConfigurationCache() {
        environment.getProperty("spring.profiles.active")?.let { profiles ->
            profiles.split(",").forEach { profile ->
                loadProfileProperties(profile.trim())
            }
        }
    }

    private fun loadProfileProperties(profile: String) {
        // Cargar propiedades específicas del perfil
        val properties = Properties()
        val resourcePath = "application-$profile.properties"

        try {
            this.javaClass.classLoader.getResourceAsStream(resourcePath)?.use { stream ->
                properties.load(stream)
                properties.forEach { key, value ->
                    configurationCache[key.toString()] = value.toString()
                }
            }
        } catch (e: Exception) {
            // Log warning but don't fail
        }
    }

    private fun loadEnvironmentSpecificVariables(
        environment: EnvironmentType,
        loadedProperties: MutableMap<String, String>,
        warnings: MutableList<String>
    ) {
        when (environment) {
            EnvironmentType.DEVELOPMENT -> {
                loadedProperties["LOG_LEVEL"] = this.environment.getProperty("LOG_LEVEL", "DEBUG")
                loadedProperties["SWAGGER_ENABLED"] = this.environment.getProperty("SWAGGER_ENABLED", "true")
            }
            EnvironmentType.STAGING -> {
                loadedProperties["LOG_LEVEL"] = this.environment.getProperty("LOG_LEVEL", "INFO")
                loadedProperties["METRICS_ENABLED"] = this.environment.getProperty("METRICS_ENABLED", "true")
            }
            EnvironmentType.PRODUCTION -> {
                loadedProperties["LOG_LEVEL"] = this.environment.getProperty("LOG_LEVEL", "WARN")
                loadedProperties["SWAGGER_ENABLED"] = this.environment.getProperty("SWAGGER_ENABLED", "false")

                // Validaciones adicionales para producción
                if (this.environment.getProperty("HTTPS_ONLY") != "true") {
                    warnings.add("HTTPS_ONLY should be enabled in production")
                }
            }
            EnvironmentType.TEST -> {
                loadedProperties["LOG_LEVEL"] = this.environment.getProperty("LOG_LEVEL", "ERROR")
            }
        }
    }

    private fun isSensitiveKey(key: String): Boolean {
        val sensitivePatterns = listOf("PASSWORD", "SECRET", "KEY", "TOKEN", "PRIVATE")
        return sensitivePatterns.any { pattern -> key.uppercase().contains(pattern) }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}

enum class EnvironmentType {
    DEVELOPMENT, STAGING, PRODUCTION, TEST
}

data class ConfigurationResult(
    val environment: EnvironmentType,
    val loadedProperties: Map<String, String>,
    val missingCriticalVars: List<String>,
    val warnings: List<String>
)

data class ValidationResult(
    val isValid: Boolean,
    val missingVariables: List<String>,
    val invalidVariables: List<String> = emptyList(),
    val warnings: List<String>
)

data class SecureValue(
    val key: String,
    val value: String?,
    val isEncrypted: Boolean,
    val lastAccessed: Long,
    val error: String? = null
)

data class ReloadResult(
    val success: Boolean,
    val reloadedAt: Long,
    val configurationCount: Int,
    val previousCount: Int,
    val errors: List<String>
)

class ConfigurationException(message: String) : RuntimeException(message)