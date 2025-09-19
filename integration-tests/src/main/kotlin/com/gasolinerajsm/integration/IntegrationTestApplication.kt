package com.gasolinerajsm.integration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = [
    "com.gasolinerajsm.authservice",
    "com.gasolinerajsm.couponservice",
    "com.gasolinerajsm.stationservice",
    "com.gasolinerajsm.integration"
])
class IntegrationTestApplication

fun main(args: Array<String>) {
    runApplication<IntegrationTestApplication>(*args)
}