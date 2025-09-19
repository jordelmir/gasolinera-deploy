package com.gasolinerajsm.shared.security.dto

import java.time.LocalDateTime
import java.util.*

data class RoleDto(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String?,
    val permissions: List<PermissionDto>,
    val isActive: Boolean,
    val isSystemRole: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val createdBy: String?,
    val updatedBy: String?
)

data class PermissionDto(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String?,
    val category: PermissionCategoryDto,
    val isActive: Boolean
)

data class PermissionCategoryDto(
    val name: String,
    val displayName: String
)

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val last: Boolean
)