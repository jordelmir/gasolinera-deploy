package com.gasolinerajsm.raffleservice.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class RaffleTest {

    @Test
    fun `should create raffle with valid data`() {
        // Given
        val name = "Test Raffle"
        val description = "Test raffle description"
        val raffleType = RaffleType.WEEKLY
        val status = RaffleStatus.DRAFT
        val registrationStart = LocalDateTime.now()
        val registrationEnd = LocalDateTime.now().plusDays(7)
        val drawDate = LocalDateTime.now().plusDays(8)

        // When
        val raffle = Raffle(
            name = name,
            description = description,
            raffleType = raffleType,
            status = status,
            registrationStart = registrationStart,
            registrationEnd = registrationEnd,
            drawDate = drawDate,
            maxParticipants = 1000,
            minTicketsToParticipate = 1,
            maxTicketsPerUser = 10,
            isPublic = true,
            createdBy = "test-user"
        )

        // Then
        assertEquals(name, raffle.name)
        assertEquals(description, raffle.description)
        assertEquals(raffleType, raffle.raffleType)
        assertEquals(status, raffle.status)
        assertEquals(registrationStart, raffle.registrationStart)
        assertEquals(registrationEnd, raffle.registrationEnd)
        assertEquals(drawDate, raffle.drawDate)
        assertEquals(1000, raffle.maxParticipants)
        assertEquals(1, raffle.minTicketsToParticipate)
        assertEquals(10, raffle.maxTicketsPerUser)
        assertTrue(raffle.isPublic)
        assertEquals("test-user", raffle.createdBy)
    }

    @Test
    fun `should create raffle with default values`() {
        // Given
        val name = "Simple Raffle"
        val registrationStart = LocalDateTime.now()
        val registrationEnd = LocalDateTime.now().plusDays(7)
        val drawDate = LocalDateTime.now().plusDays(8)

        // When
        val raffle = Raffle(
            name = name,
            registrationStart = registrationStart,
            registrationEnd = registrationEnd,
            drawDate = drawDate,
            createdBy = "test-user"
        )

        // Then
        assertEquals(name, raffle.name)
        assertEquals(RaffleType.WEEKLY, raffle.raffleType)
        assertEquals(RaffleStatus.DRAFT, raffle.status)
        assertEquals("test-user", raffle.createdBy)
    }
}