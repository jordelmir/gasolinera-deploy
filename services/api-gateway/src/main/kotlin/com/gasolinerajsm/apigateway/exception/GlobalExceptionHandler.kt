package com.gasolinerajsm.apigateway.exception

import com.gasolinerajsm.apigateway.config.CustomMetrics
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.support.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

/**
 * Enhanced error response with additional context
 */
data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null,
    val correlationId: String? = null,
    val requestId: String? = null,
    val details: Map<String, Any>? = null
)

/**
 * Global exception handler for API Gateway
 * Handles various types of exceptions and provides structured error responses
 */
@ControllerAdvice
class GlobalExceptionHandler(
    private val customMetrics: CustomMetrics
) {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    private val auditLogger = LoggerFactory.getLogger("AUDIT")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
            .joinToString("; ")

        logger.warn("Validation error: {}", errors)

        val errorResponse = createErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "VALIDATION_ERROR",
            message = "Request validation failed",
            exchange = exchange,
            details = mapOf("validationErrors" to errors)
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Authentication error: {}", ex.message)

        customMetrics.recordSecurityViolation(
            "AUTHENTICATION_FAILED",
            exchange.request.headers.getFirst("X-User-ID") ?: "anonymous",
            exchange.request.path.value()
        )

        val errorResponse = createErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "AUTHENTICATION_FAILED",
            message = "Authentication required",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException::class)
    fun handleAuthenticationCredentialsNotFoundException(
        ex: AuthenticationCredentialsNotFoundException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Authentication credentials not found: {}", ex.message)

        val errorResponse = createErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "CREDENTIALS_NOT_FOUND",
            message = "Authentication credentials not provided",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: {}", ex.message)

        customMetrics.recordSecurityViolation(
            "ACCESS_DENIED",
            exchange.request.headers.getFirst("X-User-ID") ?: "anonymous",
            exchange.request.path.value()
        )

        auditLogger.warn(
            "ACCESS_DENIED - UserId: {}, Path: {}, Method: {}, IP: {}",
            exchange.request.headers.getFirst("X-User-ID") ?: "anonymous",
            exchange.request.path.value(),
            exchange.request.method,
            exchange.request.remoteAddress?.address?.hostAddress
        )

        val errorResponse = createErrorResponse(
            status = HttpStatus.FORBIDDEN,
            error = "ACCESS_DENIED",
            message = "Insufficient privileges to access this resource",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        ex: NotFoundException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Route not found: {}", ex.message)

        val errorResponse = createErrorResponse(
            status = HttpStatus.NOT_FOUND,
            error = "ROUTE_NOT_FOUND",
            message = "The requested route was not found",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(TimeoutException::class)
    fun handleTimeoutException(
        ex: TimeoutException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.error("Request timeout: {}", ex.message)

        val errorResponse = createErrorResponse(
            status = HttpStatus.GATEWAY_TIMEOUT,
            error = "REQUEST_TIMEOUT",
            message = "The request timed out. Please try again later.",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.GATEWAY_TIMEOUT)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Response status exception: {} - {}", ex.status, ex.reason)

        val errorResponse = createErrorResponse(
            status = ex.status,
            error = ex.status.name,
            message = ex.reason ?: "An error occurred",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, ex.status)
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(
        ex: InvalidRequestException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid request: {}", ex.message)

        val errorResponse = createErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "INVALID_REQUEST",
            message = ex.message ?: "Invalid request",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: {}", ex.message)

        val errorResponse = createErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "INVALID_ARGUMENT",
            message = ex.message ?: "Invalid argument provided",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred: {}", ex.message, ex)

        auditLogger.error(
            "UNEXPECTED_ERROR - Path: {}, Method: {}, Error: {}, IP: {}",
            exchange.request.path.value(),
            exchange.request.method,
            ex.message,
            exchange.request.remoteAddress?.address?.hostAddress
        )

        val errorResponse = createErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred. Please try again later.",
            exchange = exchange
        )

        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * Create standardized error response
     */
    private fun createErrorResponse(
        status: HttpStatus,
        error: String,
        message: String,
        exchange: ServerWebExchange,
        details: Map<String, Any>? = null
    ): ErrorResponse {
        return ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = error,
            message = message,
            path = exchange.request.path.value(),
            correlationId = exchange.request.headers.getFirst("X-Correlation-ID"),
            requestId = exchange.request.headers.getFirst("X-Request-ID"),
            details = details
        )
    }
}