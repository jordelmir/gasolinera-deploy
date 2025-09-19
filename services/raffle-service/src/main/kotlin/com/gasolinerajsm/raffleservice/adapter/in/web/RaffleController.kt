package com.gasolinerajsm.raffleservice.adapter.`in`.web

import com.gasolinerajsm.raffleservice.adapter.`in`.web.dto.CreateRaffleRequest
import com.gasolinerajsm.raffleservice.adapter.`in`.web.dto.DrawRaffleRequest
import com.gasolinerajsm.raffleservice.application.RaffleCreationService
import com.gasolinerajsm.raffleservice.application.RaffleDrawingService
import com.gasolinerajsm.raffleservice.model.Raffle
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/raffles")
class RaffleController(
    private val raffleCreationService: RaffleCreationService,
    private val raffleDrawingService: RaffleDrawingService
) {

    @PostMapping
    fun createRaffle(@RequestBody request: CreateRaffleRequest): ResponseEntity<Raffle> {
        val createdRaffle = raffleCreationService.createRaffle(request.period, request.pointEntries)
        return ResponseEntity(createdRaffle, HttpStatus.CREATED)
    }

    @PostMapping("/{raffleId}/draw")
    fun drawRaffle(
        @PathVariable raffleId: Long,
        @RequestBody request: DrawRaffleRequest
    ): ResponseEntity<Raffle> {
        val updatedRaffle = raffleDrawingService.drawWinner(raffleId, request.blockHeight)
        return ResponseEntity.ok(updatedRaffle)
    }
}
