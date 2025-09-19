package com.gasolinerajsm.raffleservice.domain.service

import com.gasolinerajsm.raffleservice.util.HashingUtil

class MerkleTree(private val entries: List<String>) {

    lateinit var root: String
        private set

    init {
        require(entries.isNotEmpty()) { "Merkle Tree cannot be built from an empty list of entries." }
        build()
    }

    private fun build() {
        var currentLayer = entries.map { HashingUtil.sha256(it) }

        while (currentLayer.size > 1) {
            val nextLayer = mutableListOf<String>()
            var i = 0
            while (i < currentLayer.size) {
                val left = currentLayer[i]
                val right = if (i + 1 < currentLayer.size) currentLayer[i + 1] else left // Duplicate last if odd number
                nextLayer.add(HashingUtil.sha256(left, right))
                i += 2
            }
            currentLayer = nextLayer
        }
        root = currentLayer.first()
    }
}
