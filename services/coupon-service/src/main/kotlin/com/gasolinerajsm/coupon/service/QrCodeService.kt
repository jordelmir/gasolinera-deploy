package com.gasolinerajsm.coupon.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64

@Service
class QrCodeService(
    @Value("\${app.qr.signature-secret}")
    private val qrSignatureSecret: String
) {

    private val QR_CODE_SIZE = 250 // pixels

    fun generateQrCodeImage(data: String): ByteArray {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints)

        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        return outputStream.toByteArray()
    }

    fun signData(data: String): String {
        val combinedData = data + qrSignatureSecret
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combinedData.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    fun verifySignature(data: String, signature: String): Boolean {
        return signData(data) == signature
    }
}
