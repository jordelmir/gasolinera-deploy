package com.gasolinerajsm.raffleservice.application

import com.gasolinerajsm.raffleservice.adapter.out.seed.SeedProvider
import com.gasolinerajsm.raffleservice.model.Raffle
import com.gasolinerajsm.raffleservice.repository.RaffleRepository
import com.gasolinerajsm.raffleservice.model.RaffleStatus
import com.gasolinerajsm.raffleservice.util.HashingUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RaffleDrawingService(
    private val raffleRepository: RaffleRepository,
    private val seedProvider: SeedProvider
) {

    @Transactional
    fun drawWinner(raffleId: Long, blockHeight: Long): Raffle {
        val raffle = raffleRepository.findById(raffleId)
            .orElseThrow { NoSuchElementException("Raffle with ID $raffleId not found") }

        // 1. Get external seed
        val seedValue = seedProvider.getSeed(blockHeight)
        val seedSource = "Bitcoin Block Hash (Height: $blockHeight)"

        // 2. Get all point entries for the period
        // This is a simplification. In a real scenario, you'd filter by raffle period
        // and potentially by points that are eligible for this specific raffle.
        val allPointEntries = listOf("user1", "user2", "user3", "user4", "user5") // Mock data for now

        require(allPointEntries.isNotEmpty()) { "No point entries found to draw a winner." }

        // 3. Calculate winner index
        val combinedHash = HashingUtil.sha256("mockMerkleRoot", seedValue) // TODO: Use actual merkle root
        val winnerIndex = (combinedHash.toBigInteger(16) % allPointEntries.size.toBigInteger()).toInt()

        val winnerPointId = allPointEntries[winnerIndex]

        // 4. Update Raffle entity
        val updatedRaffle = raffle.copy(
            status = RaffleStatus.COMPLETED,
            updatedAt = LocalDateTime.now()
        )

        return raffleRepository.save(updatedRaffle)
    }
}