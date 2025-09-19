package com.gasolinerajsm.stationservice.config

import com.gasolinerajsm.stationservice.infrastructure.security.QrSigningService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Security Configuration for QR Code Signing
 */
@Configuration
class SecurityConfig {

    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun qrSigningKeys(
        qrSigningService: QrSigningService,
        qrKeyProperties: QrKeyProperties
    ): QrSigningKeys {
        return try {
            if (qrKeyProperties.useGeneratedKeys) {
                logger.warn("Using generated keys for development - NOT suitable for production!")
                val keyPair = qrSigningService.generateKeyPair()
                QrSigningKeys(keyPair.private, keyPair.public)
            } else {
                val privateKey = loadPrivateKey(qrKeyProperties.privateKeyPath, qrSigningService)
                val publicKey = loadPublicKey(qrKeyProperties.publicKeyPath, qrSigningService)
                QrSigningKeys(privateKey, publicKey)
            }
        } catch (e: Exception) {
            logger.error("Failed to load QR signing keys, falling back to generated keys", e)
            val keyPair = qrSigningService.generateKeyPair()
            QrSigningKeys(keyPair.private, keyPair.public)
        }
    }

    private fun loadPrivateKey(keyPath: String, qrSigningService: QrSigningService): PrivateKey {
        val resource: Resource = if (keyPath.startsWith("classpath:")) {
            ClassPathResource(keyPath.substring("classpath:".length))
        } else {
            org.springframework.core.io.FileSystemResource(keyPath)
        }

        val pemContent = resource.inputStream.bufferedReader().use { it.readText() }
        return qrSigningService.loadPrivateKeyFromPem(pemContent)
    }

    private fun loadPublicKey(keyPath: String, qrSigningService: QrSigningService): PublicKey {
        val resource: Resource = if (keyPath.startsWith("classpath:")) {
            ClassPathResource(keyPath.substring("classpath:".length))
        } else {
            org.springframework.core.io.FileSystemResource(keyPath)
        }

        val pemContent = resource.inputStream.bufferedReader().use { it.readText() }
        return qrSigningService.loadPublicKeyFromPem(pemContent)
    }
}

/**
 * Configuration properties for QR signing keys
 */
@Configuration
@ConfigurationProperties(prefix = "app.qr.signing")
data class QrKeyProperties(
    var privateKeyPath: String = "classpath:keys/qr-private-key.pem",
    var publicKeyPath: String = "classpath:keys/qr-public-key.pem",
    var useGeneratedKeys: Boolean = true, // Default to true for development
    var keyRotationEnabled: Boolean = false,
    var keyRotationIntervalHours: Long = 24 * 7 // Weekly rotation
)

/**
 * Container for QR signing keys
 */
data class QrSigningKeys(
    val privateKey: PrivateKey,
    val publicKey: PublicKey
)