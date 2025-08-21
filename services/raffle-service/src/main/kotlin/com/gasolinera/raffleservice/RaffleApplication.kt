package com.gasolinera.raffleservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RaffleApplication

fun main(args: Array<String>) {
    runApplication<RaffleApplication>(*args)
}
