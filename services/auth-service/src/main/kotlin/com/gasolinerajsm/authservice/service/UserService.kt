package com.gasolinerajsm.authservice.service

import com.gasolinerajsm.authservice.model.User
import com.gasolinerajsm.authservice.repository.UserRepository
import org.springframework.stereotype.Service
@Service
class UserService(private val userRepository: UserRepository) {

    fun findOrCreateUser(phone: String): User {
        return userRepository.findByPhone(phone) ?: userRepository.save(User(phone = phone))
    }

    fun findUserById(userId: Long): User? {
        return userRepository.findById(userId).orElse(null)
    }
}
