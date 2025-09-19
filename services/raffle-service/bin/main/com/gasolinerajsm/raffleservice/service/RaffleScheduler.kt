
package com.gasolinerajsm.raffleservice.service

import com.gasolinerajsm.raffleservice.model.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RaffleScheduler(private val raffleService: RaffleService) {

    @Scheduled(cron = "0 0 0 * * MON")
    fun createWeeklyRaffle() {
        val now = LocalDateTime.now()
        val raffle = Raffle(
            name = "Weekly Raffle ${now.toLocalDate()}",
            description = "Weekly raffle created automatically",
            raffleType = RaffleType.WEEKLY,
            status = RaffleStatus.DRAFT,
            registrationStart = now,
            registrationEnd = now.plusDays(6),
            drawDate = now.plusDays(7),
            minTicketsToParticipate = 1,
            maxTicketsPerUser = 100,
            maxParticipants = 40000,
            isPublic = true,
            createdBy = "system"
        )
        raffleService.createRaffle(raffle)
    }

    @Scheduled(cron = "0 0 0 14 2 ?")
    fun createCarRaffle() {
        val now = LocalDateTime.now()
        val raffle = Raffle(
            name = "Car Raffle ${now.year}",
            description = "Special car raffle",
            raffleType = RaffleType.SPECIAL,
            status = RaffleStatus.DRAFT,
            registrationStart = now,
            registrationEnd = now.plusMonths(3),
            drawDate = now.plusMonths(3).plusDays(1),
            minTicketsToParticipate = 10,
            maxTicketsPerUser = 1000,
            maxParticipants = 10000,
            isPublic = true,
            createdBy = "system"
        )
        raffleService.createRaffle(raffle)
    }
}
