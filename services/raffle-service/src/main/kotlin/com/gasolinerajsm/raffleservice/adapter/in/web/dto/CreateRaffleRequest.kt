package com.gasolinerajsm.raffleservice.adapter.`in`.web.dto

data class CreateRaffleRequest(
    val period: String,
    val pointEntries: List<String>
)
