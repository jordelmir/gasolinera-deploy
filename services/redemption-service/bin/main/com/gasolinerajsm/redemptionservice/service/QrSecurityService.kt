package com.gasolinerajsm.redemptionservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gasolinerajsm.redemptionservice.exception.InvalidQrException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Base64
import org.slf4j.LoggerFactory

data class QrPayload(
    val s: String, // stationId
    val d: String, // dispenserId
    val n: String, // nonce
    val t: Long,   // timestamp
    val exp: Long  // expiration
)

@Service
class QrSecurityService(
    @Value("\${qr.public.key}")
    private val qrPublicKeyPem: String,
    private val objectMapper: ObjectMapper // Inject ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(QrSecurityService::class.java)

    private val publicKey: PublicKey by lazy {
        val publicKeyPEM = qrPublicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
        val encoded = Base64.getDecoder().decode(publicKeyPEM)
        val keySpec = X509EncodedKeySpec(encoded)
        KeyFactory.getInstance("EC").generatePublic(keySpec)
    }

    fun validateAndParseToken(token: String): QrPayload {
        logger.info("Validating QR token: {}", token)
        val parts = token.split(".")
        if (parts.size != 2) {
            logger.warn("Invalid QR token format: {}", token)
            throw InvalidQrException("Invalid QR token format")
        }

        val (encodedPayload, receivedSignatureBase64Url) = parts

        val decodedPayloadBytes = try {
            Base64.getUrlDecoder().decode(encodedPayload)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid base64url encoding for payload: {}", encodedPayload)
            throw InvalidQrException("Invalid base64url encoding for payload")
        }

        val decodedReceivedSignatureBytes = try {
            Base64.getUrlDecoder().decode(receivedSignatureBase64Url)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid base64url encoding for signature: {}", receivedSignatureBase64Url)
            throw InvalidQrException("Invalid base64url encoding for signature")
        }

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initVerify(publicKey)
        signature.update(decodedPayloadBytes)

        if (!signature.verify(decodedReceivedSignatureBytes)) {
            logger.warn("Invalid QR signature for payload: {}", encodedPayload)
            throw InvalidQrException("Invalid QR signature")
        }
        logger.info("QR signature verified successfully.")

        val payloadJson = String(decodedPayloadBytes, Charsets.UTF_8)
        val qrPayload = try {
            objectMapper.readValue(payloadJson, QrPayload::class.java)
        } catch (e: Exception) {
            logger.warn("Invalid QR payload JSON: {}", payloadJson, e)
            throw InvalidQrException("Invalid QR payload JSON: ${e.message}")
        }

        if (qrPayload.exp < Instant.now().epochSecond) {
            logger.warn("QR token has expired: {}", qrPayload.exp)
            throw InvalidQrException("QR token has expired")
        }
        logger.info("QR token validated and parsed successfully for nonce: {}", qrPayload.n)

        return qrPayload
    }
}