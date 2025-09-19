package com.gasolinerajsm.shared.security.util

import com.gasolinerajsm.shared.security.model.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.Authentication

object SecurityUtils {

    fun getCurrentUser(): UserPrincipal? {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        return when {
            authentication == null -> null
            !authentication.isAuthenticated -> null
            authentication.principal is UserPrincipal -> authentication.principal as UserPrincipal
            else -> null
        }
    }

    fun getCurrentUserId(): String? {
        return getCurrentUser()?.id?.toString()
    }

    fun hasRole(roleName: String): Boolean {
        return getCurrentUser()?.hasRole(roleName) ?: false
    }

    fun hasAnyRole(vararg roleNames: String): Boolean {
        val user = getCurrentUser() ?: return false
        return roleNames.any { user.hasRole(it) }
    }

    fun hasPermission(permissionName: String): Boolean {
        return getCurrentUser()?.hasPermission(permissionName) ?: false
    }

    fun hasAnyPermission(vararg permissionNames: String): Boolean {
        val user = getCurrentUser() ?: return false
        return permissionNames.any { user.hasPermission(it) }
    }

    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated
    }
}

// Extension functions for SecurityContextHolder
fun SecurityContextHolder.getCurrentUser(): UserPrincipal? = SecurityUtils.getCurrentUser()
fun SecurityContextHolder.getCurrentUserId(): String? = SecurityUtils.getCurrentUserId()
fun SecurityContextHolder.hasRole(roleName: String): Boolean = SecurityUtils.hasRole(roleName)
fun SecurityContextHolder.hasAnyRole(vararg roleNames: String): Boolean = SecurityUtils.hasAnyRole(*roleNames)
fun SecurityContextHolder.hasPermission(permissionName: String): Boolean = SecurityUtils.hasPermission(permissionName)