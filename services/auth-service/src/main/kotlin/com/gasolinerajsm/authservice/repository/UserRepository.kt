package com.gasolinerajsm.authservice.repository

import com.gasolinerajsm.authservice.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByPhone(phone: String): User?
}
