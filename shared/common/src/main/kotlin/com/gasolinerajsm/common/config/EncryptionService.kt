package com.gasolinerajsm.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface EncryptionService {
    fun encrypt(plainText: String): String
    fun decrypt(encryptedText: String): String
    fun generateSecretKey(): String
    fun rotateKey(): KeyRotationResult
}

@Service
class AESEncryptionService(
    @Value("\${encryption.key:}") private val encryptionKey: String,
    @Value("\${encryption.algorithm:AES/GCM/NoPadding}") private val algorithm: String
) : EncryptionService {

    private val transformation = "AES/GCM/NoPadding"
    private val keyLength = 256
    private val ivLength = 12
    private val tagLength = 16

    private val secretKey: SecretKey by lazy {
        if (encryptionKey.isNotBlank()) {
            val decodedKey = Base64.getDecoder().decode(encryptionKey)
            SecretKeySpec(decodedKey, "AES")
        } else {
            generateAESKey()
        }
    }

    override fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(transformation)
            val iv = ByteArray(ivLength)
            SecureRandom().nextBytes(iv)

            val parameterSpec = GCMParameterSpec(tagLength * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

            val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val encryptedWithIv = iv + encryptedData

            Base64.getEncoder().encodeToString(encryptedWithIv)
        } catch (e: Exception) {
            throw EncryptionException("Failed to encrypt data: ${e.message}", e)
        }
    }

    override fun decrypt(encryptedText: String): String {
        return try {
            val encryptedWithIv = Base64.getDecoder().decode(encryptedText)
            val iv = encryptedWithIv.sliceArray(0 until ivLength)
            val encryptedData = encryptedWithIv.sliceArray(ivLength until encryptedWithIv.size)

            val cipher = Cipher.getInstance(transformation)
            val parameterSpec = GCMParameterSpec(tagLength * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            val decryptedData = cipher.doFinal(encryptedData)
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            throw EncryptionException("Failed to decrypt data: ${e.message}", e)
        }
    }

    override fun generateSecretKey(): String {
        val key = generateAESKey()
        return Base64.getEncoder().encodeToString(key.encoded)
    }

    override fun rotateKey(): KeyRotationResult {
        return try {
            val oldKeyId = getCurrentKeyId()
            val newKey = generateAESKey()
            val newKeyId = generateKeyId()

            // En una implementación real, aquí se almacenaría la nueva clave
            // y se marcaría la anterior para rotación

            KeyRotationResult(
                success = true,
                oldKeyId = oldKeyId,
                newKeyId = newKeyId,
                rotatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            KeyRotationResult(
                success = false,
                oldKeyId = getCurrentKeyId(),
                newKeyId = null,
                rotatedAt = System.currentTimeMillis(),
                error = e.message
            )
        }
    }

    private fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(keyLength)
        return keyGenerator.generateKey()
    }

    private fun getCurrentKeyId(): String {
        // En una implementación real, esto vendría de un almacén de claves
        return "key-${System.currentTimeMillis()}"
    }

    private fun generateKeyId(): String {
        return "key-${System.currentTimeMillis()}-${SecureRandom().nextInt(1000)}"
    }
}

data class KeyRotationResult(
    val success: Boolean,
    val oldKeyId: String,
    val newKeyId: String?,
    val rotatedAt: Long,
    val error: String? = null
)

class EncryptionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)