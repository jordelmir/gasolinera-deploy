package com.gasolinerajsm.stationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StationServiceApplication

fun main(args: Array<String>) {
    runApplication<StationServiceApplication>(*args)
}
