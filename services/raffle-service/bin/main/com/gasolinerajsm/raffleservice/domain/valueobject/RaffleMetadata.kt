package com.gasolinerajsm.raffleservice.domain.valueobject

import java.time.LocalDateTime

/**
 * Value object representing metadata for a raffle
 */
data class RaffleMetadata(
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val notes: String? = null,
    val tags: Set<String> = emptySet(),
    val additionalData: Map<String, String> = emptyMap()
) {

    /**
     * Check if metadata has a specific tag
     */
    fun hasTag(tag: String): Boolean = tag in tags

    /**
     * Add a tag to the metadata
     */
    fun addTag(tag: String): RaffleMetadata {
        return this.copy(tags = tags + tag)
    }

    /**
     * Remove a tag from the metadata
     */
    fun removeTag(tag: String): RaffleMetadata {
        return this.copy(tags = tags - tag)
    }

    /**
     * Update additional data
     */
    fun updateAdditionalData(key: String, value: String): RaffleMetadata {
        return this.copy(additionalData = additionalData + (key to value))
    }

    companion object {
        /**
         * Create empty metadata
         */
        fun empty(): RaffleMetadata {
            return RaffleMetadata()
        }

        /**
         * Create metadata with creator
         */
        fun createdBy(creator: String): RaffleMetadata {
            return RaffleMetadata(createdBy = creator)
        }
    }
}