package com.gasolinerajsm.stationservice.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*

/**
 * QR Code Signing Service for Gas Station Dispensers
 * Provides cryptographic signing for QR codes to ensure authenticity and prevent tampering
 */
@Service
class QrSigningService(
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(QrSigningService::class.java)

    companion object {
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        private const val KEY_ALGORITHM = "RSA"
        private const val DEFAULT_EXPIRATION_HOURS = 1L
    }

    /**
     * Generate a signed QR token for a gas station dispenser
     */
    fun generateSignedQrToken(
        stationId: String,
        dispenserId: String,
        privateKey: PrivateKey,
        expirationHours: Long = DEFAULT_EXPIRATION_HOURS
    ): Result<String> {
        return try {
            val now = Instant.now()
            val expiration = now.plusSeconds(expirationHours * 3600)

            val qrPayload = QrPayload(
                stationId = stationId,
                dispenserId = dispenserId,
                nonce = UUID.randomUUID().toString(),
                timestamp = now.epochSecond,
                expiration = expiration.epochSecond
            )

            val signedToken = signPayload(qrPayload, privateKey)
            logger.info("Generated signed QR token for station: {} dispenser: {}", stationId, dispenserId)

            Result.success(signedToken)
        } catch (e: Exception) {
            logger.error("Failed to generate signed QR token for station: {} dispenser: {}", stationId, dispenserId, e)
            Result.failure(e)
        }
    }

    /**
     * Verify a signed QR token
     */
    fun verifySignedQrToken(
        signedToken: String,
        publicKey: PublicKey
    ): Result<QrPayload> {
        return try {
            val parts = signedToken.split(".")
            if (parts.size != 2) {
                return Result.failure(IllegalArgumentException("Invalid token format"))
            }

            val payloadBase64 = parts[0]
            val signatureBase64 = parts[1]

            // Verify signature
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(publicKey)
            signature.update(payloadBase64.toByteArray())

            val signatureBytes = Base64.getUrlDecoder().decode(signatureBase64)
            val isValid = signature.verify(signatureBytes)

            if (!isValid) {
                return Result.failure(SecurityException("Invalid signature"))
            }

            // Decode payload
            val payloadJson = String(Base64.getUrlDecoder().decode(payloadBase64))
            val payload = objectMapper.readValue(payloadJson, QrPayload::class.java)

            // Check expiration
            val now = Instant.now().epochSecond
            if (payload.expiration < now) {
                return Result.failure(IllegalArgumentException("Token expired"))
            }

            logger.info("Successfully verified QR token for station: {} dispenser: {}", payload.stationId, payload.dispenserId)
            Result.success(payload)

        } catch (e: Exception) {
            logger.error("Failed to verify QR token", e)
            Result.failure(e)
        }
    }

    /**
     * Sign a payload with the private key
     */
    private fun signPayload(payload: QrPayload, privateKey: PrivateKey): String {
        val payloadJson = objectMapper.writeValueAsString(payload)
        val payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray())

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(payloadBase64.toByteArray())

        val signatureBytes = signature.sign()
        val signatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)

        return "$payloadBase64.$signatureBase64"
    }

    /**
     * Generate a new RSA key pair for development/testing
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Load private key from PEM string
     */
    fun loadPrivateKeyFromPem(pemString: String): PrivateKey {
        val privateKeyPEM = pemString
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val encoded = Base64.getDecoder().decode(privateKeyPEM)
        val keySpec = PKCS8EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)

        return keyFactory.generatePrivate(keySpec)
    }

    /**
     * Load public key from PEM string
     */
    fun loadPublicKeyFromPem(pemString: String): PublicKey {
        val publicKeyPEM = pemString
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val encoded = Base64.getDecoder().decode(publicKeyPEM)
        val keySpec = X509EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)

        return keyFactory.generatePublic(keySpec)
    }

    /**
     * Generate QR code content with embedded signed token
     */
    fun generateQrCodeContent(
        stationId: String,
        dispenserId: String,
        privateKey: PrivateKey,
        baseUrl: String = "https://gasolinera-jsm.com/redeem"
    ): Result<String> {
        return generateSignedQrToken(stationId, dispenserId, privateKey)
            .map { signedToken ->
                "$baseUrl?token=$signedToken"
            }
    }
}

/**
 * QR Payload data class matching the TypeScript implementation
 */
data class QrPayload(
    val stationId: String,      // s: Station ID
    val dispenserId: String,    // d: Dispenser ID
    val nonce: String,          // n: Unique nonce
    val timestamp: Long,        // t: Timestamp
    val expiration: Long        // exp: Expiration timestamp
) {
    // Convenience properties matching TypeScript field names
    val s: String get() = stationId
    val d: String get() = dispenserId
    val n: String get() = nonce
    val t: Long get() = timestamp
    val exp: Long get() = expiration
}