package com.gasolinerajsm.shared.security.rbac

import com.gasolinerajsm.shared.security.model.SecurityContextHolder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Role Management Controller
 * Provides REST endpoints for role and permission management
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@SecuredController(auditAll = true)
class RoleController(
    private val roleService: RoleService,
    private val authorizationService: AuthorizationService
) {

    /**
     * Get all roles with pagination
     */
    @GetMapping
    @CanReadRoles
    fun getAllRoles(
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
        @RequestParam(defaultValue = "false") systemOnly: Boolean,
        pageable: Pageable
    ): ResponseEntity<PagedResponse<RoleDto>> {
        val roles = when {
            systemOnly -> roleService.getSystemRoles()
            includeInactive -> roleService.getAllRoles()
            else -> roleService.getActiveRoles()
        }

        val roleDtos = roles.map { it.toDto() }
        val pagedResponse = PagedResponse.of(roleDtos, pageable)

        return ResponseEntity.ok(pagedResponse)
    }

    /**
     * Get role by ID
     */
    @GetMapping("/{roleId}")
    @CanReadRoles
    fun getRoleById(@PathVariable roleId: String): ResponseEntity<RoleDto> {
        val role = roleService.getRoleById(roleId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(role.toDto())
    }

    /**
     * Search roles
     */
    @GetMapping("/search")
    @CanReadRoles
    fun searchRoles(
        @RequestParam query: String,
        pageable: Pageable
    ): ResponseEntity<PagedResponse<RoleDto>> {
        val roles = roleService.searchRoles(query)
        val roleDtos = roles.map { it.toDto() }
        val pagedResponse = PagedResponse.of(roleDtos, pageable)

        return ResponseEntity.ok(pagedResponse)
    }

    /**
     * Create new role
     */
    @PostMapping
    @CanCreateRoles
    @Audited(action = "CREATE_ROLE", resourceType = "ROLE", sensitive = true)
    fun createRole(@Valid @RequestBody request: CreateRoleRequest): ResponseEntity<RoleDto> {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        try {
            val permissions = request.permissions.mapNotNull { Permission.fromCode(it) }.toSet()

            val role = roleService.createRole(
                name = request.name,
                displayName = request.displayName,
                description = request.description,
                permissions = permissions,
                createdBy = currentUser.id
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(role.toDto())
        } catch (ex: RoleException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Update existing role
     */
    @PutMapping("/{roleId}")
    @CanUpdateRoles
    @Audited(action = "UPDATE_ROLE", resourceType = "ROLE", sensitive = true)
    fun updateRole(
        @PathVariable roleId: String,
        @Valid @RequestBody request: UpdateRoleRequest
    ): ResponseEntity<RoleDto> {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        try {
            val permissions = request.permissions?.mapNotNull { Permission.fromCode(it) }?.toSet()

            val role = roleService.updateRole(
                roleId = roleId,
                displayName = request.displayName,
                description = request.description,
                permissions = permissions,
                isActive = request.isActive,
                updatedBy = currentUser.id
            )

            return ResponseEntity.ok(role.toDto())
        } catch (ex: RoleException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Delete role
     */
    @DeleteMapping("/{roleId}")
    @CanDeleteRoles
    @Audited(action = "DELETE_ROLE", resourceType = "ROLE", sensitive = true)
    fun deleteRole(@PathVariable roleId: String): ResponseEntity<Void> {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        try {
            roleService.deleteRole(roleId, currentUser.id)
            return ResponseEntity.noContent().build()
        } catch (ex: RoleException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Get role permissions
     */
    @GetMapping("/{roleId}/permissions")
    @CanReadRoles
    fun getRolePermissions(@PathVariable roleId: String): ResponseEntity<List<PermissionDto>> {
        val role = roleService.getRoleById(roleId)
            ?: return ResponseEntity.notFound().build()

        val effectivePermissions = RoleHierarchy.getEffectivePermissions(role)
        val permissionDtos = effectivePermissions.map { it.toDto() }

        return ResponseEntity.ok(permissionDtos)
    }

    /**
     * Add permission to role
     */
    @PostMapping("/{roleId}/permissions")
    @CanUpdateRoles
    @Audited(action = "ADD_PERMISSION", resourceType = "ROLE", sensitive = true)
    fun addPermissionToRole(
        @PathVariable roleId: String,
        @Valid @RequestBody request: AddPermissionRequest
    ): ResponseEntity<RoleDto> {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val permission = Permission.fromCode(request.permissionCode)
            ?: return ResponseEntity.badRequest().build()

        try {
            val role = roleService.addPermissionToRole(roleId, permission, currentUser.id)
            return ResponseEntity.ok(role.toDto())
        } catch (ex: RoleException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Remove permission from role
     */
    @DeleteMapping("/{roleId}/permissions/{permissionCode}")
    @CanUpdateRoles
    @Audited(action = "REMOVE_PERMISSION", resourceType = "ROLE", sensitive = true)
    fun removePermissionFromRole(
        @PathVariable roleId: String,
        @PathVariable permissionCode: String
    ): ResponseEntity<RoleDto> {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val permission = Permission.fromCode(permissionCode)
            ?: return ResponseEntity.badRequest().build()

        try {
            val role = roleService.removePermissionFromRole(roleId, permission, currentUser.id)
            return ResponseEntity.ok(role.toDto())
        } catch (ex: RoleException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Assign role to user
     */
    @PostMapping("/{roleId}/users/{userId}")
    @RequirePermission("USER_ROLE_ASSIGN")
    @Audited(action = "ASSIGN_ROLE", resourceType = "USER", sensitive = true)
    fun assignRoleToUser(
        @PathVariable roleId: String,
        @PathVariable userId: String
    ): ResponseEntity<Void> {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        try {
            roleService.assignRoleToUser(userId, roleId, currentUser.id)
            return ResponseEntity.ok().build()
        } catch (ex: RoleException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Remove role from user
     */
    @DeleteMapping("/{roleId}/users/{userId}")
    @RequirePermission("USER_ROLE_ASSIGN")
    @Audited(action = "REMOVE_ROLE", resourceType = "USER", sensitive = true)
    fun removeRoleFromUser(
        @PathVariable roleId: String,
        @PathVariable userId: String
    ): ResponseEntity<Void> {
        val currentUser = SecurityContextHolder.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        try {
            roleService.removeRoleFromUser(userId, roleId, currentUser.id)
            return ResponseEntity.ok().build()
        } catch (ex: RoleException) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Get role statistics
     */
    @GetMapping("/statistics")
    @RequireAnyRole("ADMIN", "SUPER_ADMIN")
    fun getRoleStatistics(): ResponseEntity<RoleStatistics> {
        val statistics = roleService.getRoleStatistics()
        return ResponseEntity.ok(statistics)
    }
}

/**
 * Permission Management Controller
 */
@RestController
@RequestMapping("/api/v1/admin/permissions")
@SecuredController(auditAll = true)
class PermissionController {

    /**
     * Get all available permissions
     */
    @GetMapping
    @RequirePermission("PERMISSION_VIEW")
    fun getAllPermissions(
        @RequestParam(required = false) category: String?
    ): ResponseEntity<List<PermissionDto>> {
        val permissions = if (category != null) {
            val permissionCategory = PermissionCategory.values()
                .find { it.name.equals(category, ignoreCase = true) }
            if (permissionCategory != null) {
                Permission.byCategory(permissionCategory)
            } else {
                emptyList()
            }
        } else {
            Permission.values().toList()
        }

        val permissionDtos = permissions.map { it.toDto() }
        return ResponseEntity.ok(permissionDtos)
    }

    /**
     * Get permission categories
     */
    @GetMapping("/categories")
    @RequirePermission("PERMISSION_VIEW")
    fun getPermissionCategories(): ResponseEntity<List<PermissionCategoryDto>> {
        val categories = PermissionCategory.values().map { it.toDto() }
        return ResponseEntity.ok(categories)
    }

    /**
     * Get permissions by category
     */
    @GetMapping("/categories/{category}")
    @RequirePermission("PERMISSION_VIEW")
    fun getPermissionsByCategory(
        @PathVariable category: String
    ): ResponseEntity<List<PermissionDto>> {
        val permissionCategory = PermissionCategory.values()
            .find { it.name.equals(category, ignoreCase = true) }
            ?: return ResponseEntity.notFound().build()

        val permissions = Permission.byCategory(permissionCategory)
        val permissionDtos = permissions.map { it.toDto() }

        return ResponseEntity.ok(permissionDtos)
    }
}

/**
 * DTOs for API responses
 */
data class RoleDto(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val permissions: List<String>,
    val isSystemRole: Boolean,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val createdBy: String?,
    val updatedBy: String?
)

data class PermissionDto(
    val code: String,
    val description: String,
    val category: String,
    val level: String
)

data class PermissionCategoryDto(
    val name: String,
    val displayName: String,
    val description: String,
    val permissionCount: Int
)

data class CreateRoleRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 50)
    val name: String,

    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val displayName: String,

    @field:Size(max = 500)
    val description: String = "",

    @field:NotEmpty
    val permissions: List<String>
)

data class UpdateRoleRequest(
    @field:Size(min = 2, max = 100)
    val displayName: String? = null,

    @field:Size(max = 500)
    val description: String? = null,

    val permissions: List<String>? = null,

    val isActive: Boolean? = null
)

data class AddPermissionRequest(
    @field:NotBlank
    val permissionCode: String
)

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
) {
    companion object {
        fun <T> of(content: List<T>, pageable: Pageable): PagedResponse<T> {
            val startIndex = (pageable.pageNumber * pageable.pageSize).coerceAtMost(content.size)
            val endIndex = ((pageable.pageNumber + 1) * pageable.pageSize).coerceAtMost(content.size)
            val pageContent = if (startIndex < content.size) content.subList(startIndex, endIndex) else emptyList()

            val totalPages = (content.size + pageable.pageSize - 1) / pageable.pageSize

            return PagedResponse(
                content = pageContent,
                page = pageable.pageNumber,
                size = pageable.pageSize,
                totalElements = content.size.toLong(),
                totalPages = totalPages,
                first = pageable.pageNumber == 0,
                last = pageable.pageNumber >= totalPages - 1
            )
        }
    }
}

/**
 * Extension functions for DTO conversion
 */
fun Role.toDto(): RoleDto {
    return RoleDto(
        id = id,
        name = name,
        displayName = displayName,
        description = description,
        permissions = permissions.map { it.code },
        isSystemRole = isSystemRole,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )
}

fun Permission.toDto(): PermissionDto {
    return PermissionDto(
        code = code,
        description = description,
        category = category.name,
        level = level.name
    )
}

fun PermissionCategory.toDto(): PermissionCategoryDto {
    return PermissionCategoryDto(
        name = name,
        displayName = displayName,
        description = description,
        permissionCount = Permission.byCategory(this).size
    )
}