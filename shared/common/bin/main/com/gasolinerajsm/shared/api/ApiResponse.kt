package com.gasolinerajsm.shared.api

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: ApiError? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val correlationId: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                message = message
            )
        }

        fun <T> success(message: String): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message
            )
        }

        fun <T> error(error: ApiError): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = error
            )
        }

        fun <T> error(message: String, code: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ApiError(
                    code = code ?: "GENERAL_ERROR",
                    message = message
                )
            )
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null,
    val field: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

// Pagination response
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val numberOfElements: Int
) {
    companion object {
        fun <T> of(content: List<T>, page: Int, size: Int, totalElements: Long): PagedResponse<T> {
            val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0
            return PagedResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
                first = page == 0,
                last = page >= totalPages - 1,
                numberOfElements = content.size
            )
        }
    }
}