package com.gasolinerajsm.adengine.domain.valueobject

/**
 * Value object representing error information
 */
data class ErrorInfo(
    val errorCode: String,
    val errorMessage: String,
    val errorType: String = "UNKNOWN",
    val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    val stackTrace: String? = null,
    val additionalData: Map<String, Any> = emptyMap()
) {
    init {
        require(errorCode.isNotBlank()) { "Error code cannot be blank" }
        require(errorMessage.isNotBlank()) { "Error message cannot be blank" }
    }

    fun isRetryable(): Boolean {
        return errorType in listOf("NETWORK", "TIMEOUT", "TEMPORARY")
    }

    fun isClientError(): Boolean {
        return errorCode.startsWith("4")
    }

    fun isServerError(): Boolean {
        return errorCode.startsWith("5")
    }

    companion object {
        fun networkError(message: String): ErrorInfo {
            return ErrorInfo(
                errorCode = "NETWORK_ERROR",
                errorMessage = message,
                errorType = "NETWORK"
            )
        }

        fun validationError(message: String): ErrorInfo {
            return ErrorInfo(
                errorCode = "VALIDATION_ERROR",
                errorMessage = message,
                errorType = "VALIDATION"
            )
        }

        fun timeoutError(message: String): ErrorInfo {
            return ErrorInfo(
                errorCode = "TIMEOUT_ERROR",
                errorMessage = message,
                errorType = "TIMEOUT"
            )
        }
    }
}