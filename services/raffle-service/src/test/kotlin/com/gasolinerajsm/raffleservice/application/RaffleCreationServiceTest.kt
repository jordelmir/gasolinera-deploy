package com.gasolinerajsm.raffleservice.application

import com.gasolinerajsm.raffleservice.model.Raffle
import com.gasolinerajsm.raffleservice.model.RaffleStatus
import com.gasolinerajsm.raffleservice.model.RaffleType
import com.gasolinerajsm.raffleservice.repository.RaffleRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class RaffleCreationServiceTest {

    @Mock
    private lateinit var raffleRepository: RaffleRepository

    private lateinit var raffleCreationService: RaffleCreationService

    @BeforeEach
    fun setUp() {
        raffleCreationService = RaffleCreationService(raffleRepository)
    }

    @Test
    fun `should create raffle successfully`() {
        // Given
        val period = "2024-01"
        val pointEntries = listOf("user1", "user2", "user3")
        val expectedRaffle = Raffle(
            name = "Raffle for period: $period",
            description = "Auto-generated raffle for period $period",
            raffleType = RaffleType.WEEKLY,
            status = RaffleStatus.DRAFT,
            registrationStart = LocalDateTime.now(),
            registrationEnd = LocalDateTime.now().plusDays(7),
            drawDate = LocalDateTime.now().plusDays(8),
            maxParticipants = 1000,
            minTicketsToParticipate = 1,
            maxTicketsPerUser = 10,
            isPublic = true,
            createdBy = "system"
        )

        `when`(raffleRepository.save(any(Raffle::class.java))).thenReturn(expectedRaffle)

        // When
        val result = raffleCreationService.createRaffle(period, pointEntries)

        // Then
        assertNotNull(result)
        assertEquals("Raffle for period: $period", result.name)
        assertEquals(RaffleType.WEEKLY, result.raffleType)
        assertEquals(RaffleStatus.DRAFT, result.status)
        verify(raffleRepository).save(any(Raffle::class.java))
    }

    @Test
    fun `should throw exception when point entries are empty`() {
        // Given
        val period = "2024-01"
        val emptyPointEntries = emptyList<String>()

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            raffleCreationService.createRaffle(period, emptyPointEntries)
        }
    }
}