package com.gasolinerajsm.raffleservice.application

import com.gasolinerajsm.raffleservice.model.Raffle
import com.gasolinerajsm.raffleservice.model.RaffleType
import com.gasolinerajsm.raffleservice.model.RaffleStatus
import com.gasolinerajsm.raffleservice.repository.RaffleRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RaffleCreationService(
    private val raffleRepository: RaffleRepository
) {

    fun createRaffle(period: String, pointEntries: List<String>): Raffle {
        require(pointEntries.isNotEmpty()) { "Point entries cannot be empty for raffle creation." }

        // Create a simple raffle entity
        val raffle = Raffle(
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

        return raffleRepository.save(raffle)
    }
}