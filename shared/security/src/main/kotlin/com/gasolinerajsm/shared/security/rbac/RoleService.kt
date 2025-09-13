package com.gasolinerajsm.shared.security.rbac

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Role Management Service
 * Handles CRUD operations for roles and permissions
 */
@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val auditService: AuditService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RoleService::class.java)
    }

    private val roleCache = ConcurrentHashMap<String, Role>()

    init {
        // Initialize system roles
        initializeSystemRoles()
    }

    /**
     * Initialize system roles in the repository
     */
    private fun initializeSystemRoles() {
        try {
            SystemRoles.getAllSystemRoles().forEach { role ->
                if (!roleRepository.existsById(role.id)) {
                    roleRepository.save(role)
                    logger.info("Initialized system role: ${role.name}")
                }
                roleCache[role.id] = role
            }
        } catch (ex: Exception) {
            logger.error("Failed to initialize system roles", ex)
        }
    }

    /**
     * Create a new role
     */
    fun createRole(
        name: String,
        displayName: String,
        description: String,
        permissions: Set<Permission>,
        createdBy: String
    ): Role {
        // Validate role name uniqueness
        if (roleRepository.existsByName(name)) {
            throw RoleException("Role with name '$name' already exists")
        }

        val role = Role.builder(generateRoleId(name), name)
            .displayName(displayName)
            .description(description)
            .addPermissions(permissions)
            .createdBy(createdBy)
            .build()

        val savedRole = roleRepository.save(role)
        roleCache[savedRole.id] = savedRole

        auditService.logRoleCreation(savedRole, createdBy)
        logger.info("Created role: ${savedRole.name} by $createdBy")

        return savedRole
    }

    /**
     * Update an existing role
     */
    fun updateRole(
        roleId: String,
        displayName: String? = null,
        description: String? = null,
        permissions: Set<Permission>? = null,
        isActive: Boolean? = null,
        updatedBy: String
    ): Role {
        val existingRole = getRoleById(roleId)
            ?: throw RoleException("Role not found: $roleId")

        // Prevent modification of system roles
        if (existingRole.isSystemRole) {
            throw RoleException("Cannot modify system role: ${existingRole.name}")
        }

        val updatedRole = existingRole.copy(
            displayName = displayName ?: existingRole.displayName,
            description = description ?: existingRole.description,
            permissions = permissions ?: existingRole.permissions,
            isActive = isActive ?: existingRole.isActive,
            updatedAt = LocalDateTime.now(),
            updatedBy = updatedBy
        )

        val savedRole = roleRepository.save(updatedRole)
        roleCache[savedRole.id] = savedRole

        auditService.logRoleUpdate(existingRole, savedRole, updatedBy)
        logger.info("Updated role: ${savedRole.name} by $updatedBy")

        return savedRole
    }

    /**
     * Delete a role
     */
    fun deleteRole(roleId: String, deletedBy: String) {
        val role = getRoleById(roleId)
            ?: throw RoleException("Role not found: $roleId")

        // Prevent deletion of system roles
        if (role.isSystemRole) {
            throw RoleException("Cannot delete system role: ${role.name}")
        }

        // Check if role is assigned to any users
        if (roleRepository.isRoleAssigned(roleId)) {
            throw RoleException("Cannot delete role that is assigned to users: ${role.name}")
        }

        roleRepository.deleteById(roleId)
        roleCache.remove(roleId)

        auditService.logRoleDeletion(role, deletedBy)
        logger.info("Deleted role: ${role.name} by $deletedBy")
    }

    /**
     * Get role by ID
     */
    fun getRoleById(roleId: String): Role? {
        return roleCache[roleId] ?: roleRepository.findById(roleId)?.also {
            roleCache[roleId] = it
        }
    }

    /**
     * Get role by name
     */
    fun getRoleByName(name: String): Role? {
        return roleCache.values.find { it.name == name }
            ?: roleRepository.findByName(name)?.also {
                roleCache[it.id] = it
            }
    }

    /**
     * Get all roles
     */
    fun getAllRoles(): List<Role> {
        return roleRepository.findAll()
    }

    /**
     * Get active roles
     */
    fun getActiveRoles(): List<Role> {
        return roleRepository.findByIsActive(true)
    }

    /**
     * Get system roles
     */
    fun getSystemRoles(): List<Role> {
        return roleRepository.findByIsSystemRole(true)
    }

    /**
     * Get custom roles (non-system)
     */
    fun getCustomRoles(): List<Role> {
        return roleRepository.findByIsSystemRole(false)
    }

    /**
     * Search roles by name or description
     */
    fun searchRoles(query: String): List<Role> {
        return roleRepository.searchByNameOrDescription(query)
    }

    /**
     * Get roles with specific permission
     */
    fun getRolesWithPermission(permission: Permission): List<Role> {
        return getAllRoles().filter { role ->
            RoleHierarchy.getEffectivePermissions(role).contains(permission)
        }
    }

    /**
     * Add permission to role
     */
    fun addPermissionToRole(roleId: String, permission: Permission, updatedBy: String): Role {
        val role = getRoleById(roleId)
            ?: throw RoleException("Role not found: $roleId")

        if (role.isSystemRole) {
            throw RoleException("Cannot modify system role permissions: ${role.name}")
        }

        val updatedPermissions = role.permissions.toMutableSet()
        updatedPermissions.add(permission)

        return updateRole(roleId, permissions = updatedPermissions, updatedBy = updatedBy)
    }

    /**
     * Remove permission from role
     */
    fun removePermissionFromRole(roleId: String, permission: Permission, updatedBy: String): Role {
        val role = getRoleById(roleId)
            ?: throw RoleException("Role not found: $roleId")

        if (role.isSystemRole) {
            throw RoleException("Cannot modify system role permissions: ${role.name}")
        }

        val updatedPermissions = role.permissions.toMutableSet()
        updatedPermissions.remove(permission)

        return updateRole(roleId, permissions = updatedPermissions, updatedBy = updatedBy)
    }

    /**
     * Assign role to user
     */
    fun assignRoleToUser(userId: String, roleId: String, assignedBy: String) {
        val role = getRoleById(roleId)
            ?: throw RoleException("Role not found: $roleId")

        if (!role.isActive) {
            throw RoleException("Cannot assign inactive role: ${role.name}")
        }

        roleRepository.assignRoleToUser(userId, roleId)

        auditService.logRoleAssignment(userId, roleId, assignedBy)
        logger.info("Assigned role ${role.name} to user $userId by $assignedBy")
    }

    /**
     * Remove role from user
     */
    fun removeRoleFromUser(userId: String, roleId: String, removedBy: String) {
        val role = getRoleById(roleId)
            ?: throw RoleException("Role not found: $roleId")

        roleRepository.removeRoleFromUser(userId, roleId)

        auditService.logRoleRemoval(userId, roleId, removedBy)
        logger.info("Removed role ${role.name} from user $userId by $removedBy")
    }

    /**
     * Get user roles
     */
    fun getUserRoles(userId: String): List<Role> {
        val roleIds = roleRepository.getUserRoleIds(userId)
        return roleIds.mapNotNull { getRoleById(it) }
    }

    /**
     * Get user effective permissions
     */
    fun getUserEffectivePermissions(userId: String): Set<Permission> {
        val userRoles = getUserRoles(userId)
        val allPermissions = mutableSetOf<Permission>()

        userRoles.forEach { role ->
            allPermissions.addAll(RoleHierarchy.getEffectivePermissions(role))
        }

        return allPermissions
    }

    /**
     * Validate role hierarchy
     */
    fun validateRoleHierarchy(roleId: String, parentRoleId: String): Boolean {
        return RoleHierarchy.validateRoleAssignment(roleId, parentRoleId)
    }

    /**
     * Get role statistics
     */
    fun getRoleStatistics(): RoleStatistics {
        val allRoles = getAllRoles()
        val activeRoles = allRoles.filter { it.isActive }
        val systemRoles = allRoles.filter { it.isSystemRole }
        val customRoles = allRoles.filter { !it.isSystemRole }

        return RoleStatistics(
            totalRoles = allRoles.size,
            activeRoles = activeRoles.size,
            inactiveRoles = allRoles.size - activeRoles.size,
            systemRoles = systemRoles.size,
            customRoles = customRoles.size,
            totalPermissions = Permission.values().size,
            mostUsedRole = findMostUsedRole(),
            leastUsedRole = findLeastUsedRole()
        )
    }

    /**
     * Find most used role
     */
    private fun findMostUsedRole(): String? {
        return roleRepository.findMostUsedRole()
    }

    /**
     * Find least used role
     */
    private fun findLeastUsedRole(): String? {
        return roleRepository.findLeastUsedRole()
    }

    /**
     * Generate role ID from name
     */
    private fun generateRoleId(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    /**
     * Clear role cache
     */
    fun clearCache() {
        roleCache.clear()
        logger.info("Role cache cleared")
    }

    /**
     * Refresh role cache
     */
    fun refreshCache() {
        clearCache()
        getAllRoles().forEach { role ->
            roleCache[role.id] = role
        }
        logger.info("Role cache refreshed with ${roleCache.size} roles")
    }
}

/**
 * Role statistics data class
 */
data class RoleStatistics(
    val totalRoles: Int,
    val activeRoles: Int,
    val inactiveRoles: Int,
    val systemRoles: Int,
    val customRoles: Int,
    val totalPermissions: Int,
    val mostUsedRole: String?,
    val leastUsedRole: String?
)

/**
 * Role repository interface
 */
interface RoleRepository {
    fun save(role: Role): Role
    fun findById(id: String): Role?
    fun findByName(name: String): Role?
    fun findAll(): List<Role>
    fun findByIsActive(isActive: Boolean): List<Role>
    fun findByIsSystemRole(isSystemRole: Boolean): List<Role>
    fun deleteById(id: String)
    fun existsById(id: String): Boolean
    fun existsByName(name: String): Boolean
    fun searchByNameOrDescription(query: String): List<Role>
    fun isRoleAssigned(roleId: String): Boolean
    fun assignRoleToUser(userId: String, roleId: String)
    fun removeRoleFromUser(userId: String, roleId: String)
    fun getUserRoleIds(userId: String): List<String>
    fun findMostUsedRole(): String?
    fun findLeastUsedRole(): String?
}

/**
 * Role-specific exception
 */
class RoleException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Extended audit service for role operations
 */
fun AuditService.logRoleCreation(role: Role, createdBy: String) {
    logger.info("Role created: id=${role.id}, name=${role.name}, createdBy=$createdBy")
}

fun AuditService.logRoleUpdate(oldRole: Role, newRole: Role, updatedBy: String) {
    logger.info("Role updated: id=${newRole.id}, name=${newRole.name}, updatedBy=$updatedBy")
}

fun AuditService.logRoleDeletion(role: Role, deletedBy: String) {
    logger.info("Role deleted: id=${role.id}, name=${role.name}, deletedBy=$deletedBy")
}

fun AuditService.logRoleAssignment(userId: String, roleId: String, assignedBy: String) {
    logger.info("Role assigned: userId=$userId, roleId=$roleId, assignedBy=$assignedBy")
}

fun AuditService.logRoleRemoval(userId: String, roleId: String, removedBy: String) {
    logger.info("Role removed: userId=$userId, roleId=$roleId, removedBy=$removedBy")
}