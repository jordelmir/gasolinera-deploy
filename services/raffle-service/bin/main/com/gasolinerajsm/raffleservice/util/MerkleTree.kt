
package com.gasolinerajsm.raffleservice.util

import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets

// Placeholder for a Merkle Tree implementation
class MerkleTree(val root: String) {
    companion object {
        fun build(entries: List<String>): MerkleTree {
            if (entries.isEmpty()) {
                return MerkleTree("")
            }
            var level = entries.map { hash(it) }
            while (level.size > 1) {
                level = (0 until level.size step 2).map { i ->
                    if (i + 1 < level.size) {
                        hash(level[i] + level[i+1])
                    } else {
                        level[i]
                    }
                }
            }
            return MerkleTree(level.first())
        }

        private fun hash(input: String): String {
            return Hashing.sha256().hashString(input, StandardCharsets.UTF_8).toString()
        }
    }
}
