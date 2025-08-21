
package com.gasolinerajsm.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * API Gateway Application using Spring Cloud Gateway
 *
 * This service acts as the main entry point for all client requests,
 * routing them to appropriate microservices and handling cross-cutting concerns
 * like authentication, logging, and circuit breaking.
 */
@SpringBootApplication
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}