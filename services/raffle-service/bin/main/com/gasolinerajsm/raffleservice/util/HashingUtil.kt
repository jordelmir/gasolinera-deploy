package com.gasolinerajsm.raffleservice.util

import java.security.MessageDigest

object HashingUtil {

    fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    fun sha256(hash1: String, hash2: String): String {
        // Ensure consistent order for hashing pairs
        val combined = if (hash1 < hash2) hash1 + hash2 else hash2 + hash1
        return sha256(combined)
    }
}
