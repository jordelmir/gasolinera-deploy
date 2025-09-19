package com.gasolinerajsm.raffleservice.util

import java.security.MessageDigest

object MerkleTreeGenerator {

    fun generateMerkleRoot(leaves: List<String>): String {
        if (leaves.isEmpty()) {
            return ""
        }
        if (leaves.size == 1) {
            return sha256(leaves[0])
        }

        var currentLevel = leaves.map { sha256(it) }

        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<String>()
            for (i in 0 until currentLevel.size step 2) {
                val left = currentLevel[i]
                val right = if (i + 1 < currentLevel.size) currentLevel[i + 1] else left // Duplicate last if odd
                nextLevel.add(sha256(left + right))
            }
            currentLevel = nextLevel
        }
        return currentLevel[0]
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
