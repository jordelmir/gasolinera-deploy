package com.gasolinerajsm.shared.security.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

data class UserPrincipal(
    val id: UUID,
    val email: String,
    private val userPassword: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<Role> = emptySet(),
    val permissions: Set<Permission> = emptySet(),
    private val enabled: Boolean = true,
    private val accountNonExpired: Boolean = true,
    private val accountNonLocked: Boolean = true,
    private val credentialsNonExpired: Boolean = true
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()
        roles.forEach { role ->
            authorities.add(GrantedAuthority { "ROLE_${role.name}" })
            authorities.addAll(role.permissions.map { GrantedAuthority { it.name } })
        }
        permissions.forEach { permission ->
            authorities.add(GrantedAuthority { permission.name })
        }
        return authorities
    }

    override fun getPassword(): String = userPassword
    override fun getUsername(): String = email
    override fun isAccountNonExpired(): Boolean = accountNonExpired
    override fun isAccountNonLocked(): Boolean = accountNonLocked
    override fun isCredentialsNonExpired(): Boolean = credentialsNonExpired
    override fun isEnabled(): Boolean = enabled

    fun getFullName(): String = "$firstName $lastName"

    fun hasRole(roleName: String): Boolean = roles.any { it.name == roleName }

    fun hasPermission(permissionName: String): Boolean =
        permissions.any { it.name == permissionName } ||
        roles.any { role -> role.permissions.any { it.name == permissionName } }

    fun hasAnyPermission(vararg permissionNames: String): Boolean =
        permissionNames.any { hasPermission(it) }

    fun hasAllPermissions(vararg permissionNames: String): Boolean =
        permissionNames.all { hasPermission(it) }
}