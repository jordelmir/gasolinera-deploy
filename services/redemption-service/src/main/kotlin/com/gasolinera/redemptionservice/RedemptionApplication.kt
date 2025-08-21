package com.gasolinera.redemptionservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RedemptionApplication

fun main(args: Array<String>) {
    runApplication<RedemptionApplication>(*args)
}
