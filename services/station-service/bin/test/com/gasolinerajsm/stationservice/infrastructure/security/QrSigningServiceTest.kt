package com.gasolinerajsm.stationservice.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.security.KeyPairGenerator

class QrSigningServiceTest {

    private lateinit var qrSigningService: QrSigningService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var keyPair: java.security.KeyPair

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        qrSigningService = QrSigningService(objectMapper)
        keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    }

    @Test
    fun `should create QrSigningService instance`() {
        assertNotNull(qrSigningService)
        assertNotNull(keyPair)
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
    }

    @Test
    fun `should generate key pair`() {
        val generatedKeyPair = qrSigningService.generateKeyPair()
        assertNotNull(generatedKeyPair)
        assertNotNull(generatedKeyPair.private)
        assertNotNull(generatedKeyPair.public)
    }

    @Test
    fun `should generate signed QR token`() {
        // Given
        val stationId = "JSM-01-ALAJUELITA"
        val dispenserId = "D03"
        val expirationHours = 1L

        // When
        val result = qrSigningService.generateSignedQrToken(
            stationId = stationId,
            dispenserId = dispenserId,
            privateKey = keyPair.private,
            expirationHours = expirationHours
        )

        // Then
        println("Result: $result")
        println("Is success: ${result.isSuccess}")
        if (result.isFailure) {
            println("Exception: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
        }

        assertTrue(result.isSuccess, "Should generate token successfully")

        val token = result.getOrNull()
        assertNotNull(token, "Token should not be null")
        assertTrue(token!!.contains("."), "Token should contain a dot separator")
    }
}