package com.gasolinerajsm.shared.vault

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * HashiCorp Vault Client for Gasolinera JSM
 * Provides secure access to secrets stored in Vault
 */
@Component
class VaultClient(
    @Value("\${vault.address:http://localhost:8200}")
    private val vaultAddress: String,

    @Value("\${vault.role-id:}")
    private val roleId: String,

    @Value("\${vault.secret-id:}")
    private val secretId: String,

    @Value("\${vault.token:}")
    private val staticToken: String,

    @Value("\${vault.namespace:gasolinera-jsm}")
    private val namespace: String,

    @Value("\${vault.token-renewal-threshold:300}")
    private val tokenRenewalThreshold: Long,

    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) {

    companion object {
        private val logger = LoggerFactory.getLogger(VaultClient::class.java)
        private const val AUTH_APPROLE_PATH = "/v1/auth/approle/login"
        private const val KV_PATH_PREFIX = "/v1/gasolinera-jsm/data"
        private const val DATABASE_CREDS_PATH = "/v1/database/creds"
        private const val TRANSIT_ENCRYPT_PATH = "/v1/transit/encrypt"
        private const val TRANSIT_DECRYPT_PATH = "/v1/transit/decrypt"
        private const val TOKEN_RENEW_PATH = "/v1/auth/token/renew-self"
        private const val TOKEN_LOOKUP_PATH = "/v1/auth/token/lookup-self"
    }

    private var currentToken: String? = null
    private var tokenExpiration: LocalDateTime? = null
    private val secretCache = ConcurrentHashMap<String, CachedSecret>()

    data class CachedSecret(
        val value: Any,
        val expiration: LocalDateTime
    )

    data class VaultResponse<T>(
        val data: T?,
        val errors: List<String>? = null,
        val warnings: List<String>? = null
    )

    data class DatabaseCredentials(
        val username: String,
        val password: String,
        val leaseId: String,
        val leaseDuration: Long
    )

    @PostConstruct
    fun initialize() {
        try {
            authenticate()
            logger.info("Vault client initialized successfully")
        } catch (ex: Exception) {
            logger.error("Failed to initialize Vault client", ex)
            throw VaultException("Vault initialization failed", ex)
        }
    }

    /**
     * Authenticate with Vault using AppRole or static token
     */
    private fun authenticate() {
        currentToken = when {
            staticToken.isNotBlank() -> {
                logger.info("Using static token for Vault authentication")
                staticToken
            }
            roleId.isNotBlank() && secretId.isNotBlank() -> {
                logger.info("Authenticating with Vault using AppRole")
                authenticateWithAppRole()
            }
            else -> {
                throw VaultException("No valid authentication method configured")
            }
        }

        // Get token information
        updateTokenExpiration()
    }

    /**
     * Authenticate using AppRole method
     */
    private fun authenticateWithAppRole(): String {
        val request = mapOf(
            "role_id" to roleId,
            "secret_id" to secretId
        )

        val response = makeVaultRequest<Map<String, Any>>(
            HttpMethod.POST,
            AUTH_APPROLE_PATH,
            request,
            useAuth = false
        )

        val auth = response.data?.get("auth") as? Map<*, *>
            ?: throw VaultException("Invalid AppRole authentication response")

        return auth["client_token"] as? String
            ?: throw VaultException("No client token in AppRole response")
    }

    /**
     * Update token expiration information
     */
    private fun updateTokenExpiration() {
        try {
            val response = makeVaultRequest<Map<String, Any>>(
                HttpMethod.GET,
                TOKEN_LOOKUP_PATH
            )

            val data = response.data ?: return
            val ttl = (data["ttl"] as? Number)?.toLong() ?: 0

            if (ttl > 0) {
                tokenExpiration = LocalDateTime.now().plusSeconds(ttl)
                logger.debug("Token expires at: ${tokenExpiration?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
            }
        } catch (ex: Exception) {
            logger.warn("Failed to get token information", ex)
        }
    }

    /**
     * Check if token needs renewal and renew if necessary
     */
    private fun ensureValidToken() {
        val expiration = tokenExpiration
        if (expiration != null &&
            LocalDateTime.now().plusSeconds(tokenRenewalThreshold).isAfter(expiration)) {

            logger.info("Token is about to expire, renewing...")
            renewToken()
        }
    }

    /**
     * Renew the current token
     */
    private fun renewToken() {
        try {
            val response = makeVaultRequest<Map<String, Any>>(
                HttpMethod.POST,
                TOKEN_RENEW_PATH
            )

            val auth = response.data?.get("auth") as? Map<*, *>
            if (auth != null) {
                val leaseDuration = (auth["lease_duration"] as? Number)?.toLong() ?: 0
                if (leaseDuration > 0) {
                    tokenExpiration = LocalDateTime.now().plusSeconds(leaseDuration)
                    logger.info("Token renewed successfully, new expiration: ${tokenExpiration?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
                }
            }
        } catch (ex: Exception) {
            logger.error("Failed to renew token, re-authenticating...", ex)
            authenticate()
        }
    }

    /**
     * Get secret from KV store
     */
    fun getSecret(path: String): Map<String, Any>? {
        val cacheKey = "kv:$path"
        val cached = secretCache[cacheKey]

        if (cached != null && LocalDateTime.now().isBefore(cached.expiration)) {
            @Suppress("UNCHECKED_CAST")
            return cached.value as Map<String, Any>
        }

        ensureValidToken()

        try {
            val response = makeVaultRequest<Map<String, Any>>(
                HttpMethod.GET,
                "$KV_PATH_PREFIX/$path"
            )

            val data = response.data?.get("data") as? Map<String, Any>

            if (data != null) {
                // Cache for 5 minutes
                secretCache[cacheKey] = CachedSecret(
                    value = data,
                    expiration = LocalDateTime.now().plusMinutes(5)
                )
            }

            return data
        } catch (ex: HttpClientErrorException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Secret not found at path: $path")
                return null
            }
            throw VaultException("Failed to get secret from path: $path", ex)
        }
    }

    /**
     * Get specific secret value
     */
    fun getSecretValue(path: String, key: String): String? {
        return getSecret(path)?.get(key) as? String
    }

    /**
     * Put secret to KV store
     */
    fun putSecret(path: String, data: Map<String, Any>) {
        ensureValidToken()

        val request = mapOf("data" to data)

        makeVaultRequest<Any>(
            HttpMethod.POST,
            "$KV_PATH_PREFIX/$path",
            request
        )

        // Invalidate cache
        secretCache.remove("kv:$path")

        logger.info("Secret stored at path: $path")
    }

    /**
     * Get database credentials
     */
    fun getDatabaseCredentials(role: String): DatabaseCredentials {
        val cacheKey = "db:$role"
        val cached = secretCache[cacheKey]

        if (cached != null && LocalDateTime.now().isBefore(cached.expiration)) {
            return cached.value as DatabaseCredentials
        }

        ensureValidToken()

        val response = makeVaultRequest<Map<String, Any>>(
            HttpMethod.GET,
            "$DATABASE_CREDS_PATH/$role"
        )

        val data = response.data ?: throw VaultException("No data in database credentials response")\n        \n        val username = data["username"] as? String\n            ?: throw VaultException("No username in database credentials")\n        val password = data["password"] as? String\n            ?: throw VaultException("No password in database credentials")\n        val leaseId = data["lease_id"] as? String ?: ""\n        val leaseDuration = (data["lease_duration"] as? Number)?.toLong() ?: 3600\n        \n        val credentials = DatabaseCredentials(\n            username = username,\n            password = password,\n            leaseId = leaseId,\n            leaseDuration = leaseDuration\n        )\n        \n        // Cache for 80% of lease duration\n        val cacheSeconds = (leaseDuration * 0.8).toLong()\n        secretCache[cacheKey] = CachedSecret(\n            value = credentials,\n            expiration = LocalDateTime.now().plusSeconds(cacheSeconds)\n        )\n        \n        return credentials\n    }\n    \n    /**\n     * Encrypt data using Transit engine\n     */\n    fun encrypt(keyName: String, plaintext: String): String {\n        ensureValidToken()\n        \n        val request = mapOf(\n            "plaintext" to java.util.Base64.getEncoder().encodeToString(plaintext.toByteArray())\n        )\n        \n        val response = makeVaultRequest<Map<String, Any>>(\n            HttpMethod.POST,\n            "$TRANSIT_ENCRYPT_PATH/$keyName",\n            request\n        )\n        \n        return response.data?.get("ciphertext") as? String\n            ?: throw VaultException("No ciphertext in encrypt response")\n    }\n    \n    /**\n     * Decrypt data using Transit engine\n     */\n    fun decrypt(keyName: String, ciphertext: String): String {\n        ensureValidToken()\n        \n        val request = mapOf("ciphertext" to ciphertext)\n        \n        val response = makeVaultRequest<Map<String, Any>>(\n            HttpMethod.POST,\n            "$TRANSIT_DECRYPT_PATH/$keyName",\n            request\n        )\n        \n        val plaintextBase64 = response.data?.get("plaintext") as? String\n            ?: throw VaultException("No plaintext in decrypt response")\n        \n        return String(java.util.Base64.getDecoder().decode(plaintextBase64))\n    }\n    \n    /**\n     * Make HTTP request to Vault\n     */\n    private fun <T> makeVaultRequest(\n        method: HttpMethod,\n        path: String,\n        body: Any? = null,\n        useAuth: Boolean = true\n    ): VaultResponse<T> {\n        val url = "$vaultAddress$path"\n        \n        val headers = HttpHeaders().apply {\n            contentType = MediaType.APPLICATION_JSON\n            if (useAuth && currentToken != null) {\n                set("X-Vault-Token", currentToken)\n            }\n        }\n        \n        val entity = HttpEntity(body, headers)\n        \n        try {\n            val response = restTemplate.exchange(\n                url,\n                method,\n                entity,\n                String::class.java\n            )\n            \n            val jsonResponse = if (response.body.isNullOrBlank()) {\n                objectMapper.createObjectNode()\n            } else {\n                objectMapper.readTree(response.body)\n            }\n            \n            @Suppress("UNCHECKED_CAST")\n            return VaultResponse(\n                data = objectMapper.convertValue(jsonResponse, Map::class.java) as? T,\n                errors = extractErrors(jsonResponse),\n                warnings = extractWarnings(jsonResponse)\n            )\n        } catch (ex: HttpClientErrorException) {\n            val errorBody = try {\n                objectMapper.readTree(ex.responseBodyAsString)\n            } catch (e: Exception) {\n                null\n            }\n            \n            val errors = extractErrors(errorBody) ?: listOf(ex.message ?: "Unknown error")\n            throw VaultException("Vault request failed: ${errors.joinToString(", ")}", ex)\n        }\n    }\n    \n    private fun extractErrors(jsonNode: JsonNode?): List<String>? {\n        return jsonNode?.get("errors")?.map { it.asText() }\n    }\n    \n    private fun extractWarnings(jsonNode: JsonNode?): List<String>? {\n        return jsonNode?.get("warnings")?.map { it.asText() }\n    }\n    \n    /**\n     * Clear secret cache\n     */\n    fun clearCache() {\n        secretCache.clear()\n        logger.info("Vault secret cache cleared")\n    }\n    \n    /**\n     * Get cache statistics\n     */\n    fun getCacheStats(): Map<String, Any> {\n        val now = LocalDateTime.now()\n        val validEntries = secretCache.values.count { now.isBefore(it.expiration) }\n        val expiredEntries = secretCache.size - validEntries\n        \n        return mapOf(\n            "total_entries" to secretCache.size,\n            "valid_entries" to validEntries,\n            "expired_entries" to expiredEntries,\n            "cache_hit_ratio" to if (secretCache.isEmpty()) 0.0 else validEntries.toDouble() / secretCache.size\n        )\n    }\n}\n\n/**\n * Vault-specific exception\n */\nclass VaultException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)