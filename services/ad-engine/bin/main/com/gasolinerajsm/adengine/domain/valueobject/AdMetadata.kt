package com.gasolinerajsm.adengine.domain.valueobject

import java.time.LocalDateTime

/**
 * Value object representing advertisement metadata
 */
data class AdMetadata(
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val approvedBy: String? = null,
    val rejectedBy: String? = null,
    val notes: String? = null,
    val tags: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val priority: Priority = Priority.NORMAL,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val targetAudience: String? = null,
    val campaignNotes: String? = null,
    val technicalNotes: String? = null,
    val complianceNotes: String? = null,
    val approvalDate: LocalDateTime? = null,
    val rejectionDate: LocalDateTime? = null,
    val lastReviewDate: LocalDateTime? = null,
    val reviewCount: Int = 0,
    val version: Int = 1,
    val source: String? = null,
    val externalId: String? = null,
    val additionalData: Map<String, String> = emptyMap()
) {

    /**
     * Check if advertisement requires approval
     */
    fun requiresApproval(): Boolean {
        return sensitivity == Sensitivity.HIGH || priority == Priority.CRITICAL
    }

    /**
     * Check if advertisement is approved
     */
    fun isApproved(): Boolean {
        return approvalDate != null && rejectedBy == null
    }

    /**
     * Check if advertisement is rejected
     */
    fun isRejected(): Boolean {
        return rejectionDate != null && rejectedBy != null
    }

    /**
     * Check if advertisement needs review
     */
    fun needsReview(): Boolean {
        return !isApproved() && !isRejected() && requiresApproval()
    }

    /**
     * Update metadata with new values
     */
    fun update(
        updatedBy: String? = null,
        notes: String? = null,
        tags: Set<String>? = null,
        categories: Set<String>? = null,
        additionalData: Map<String, String>? = null
    ): AdMetadata {
        return this.copy(
            updatedBy = updatedBy ?: this.updatedBy,
            notes = notes ?: this.notes,
            tags = tags ?: this.tags,
            categories = categories ?: this.categories,
            additionalData = additionalData ?: this.additionalData,
            version = this.version + 1
        )
    }

    /**
     * Approve the advertisement
     */
    fun approve(approvedBy: String, notes: String? = null): AdMetadata {
        return this.copy(
            approvedBy = approvedBy,
            approvalDate = LocalDateTime.now(),
            notes = notes ?: this.notes,
            lastReviewDate = LocalDateTime.now(),
            reviewCount = this.reviewCount + 1
        )
    }

    /**
     * Reject the advertisement
     */
    fun reject(rejectedBy: String, reason: String): AdMetadata {
        return this.copy(
            rejectedBy = rejectedBy,
            rejectionDate = LocalDateTime.now(),
            notes = reason,
            lastReviewDate = LocalDateTime.now(),
            reviewCount = this.reviewCount + 1
        )
    }

    /**
     * Mark as reviewed
     */
    fun markReviewed(reviewedBy: String): AdMetadata {
        return this.copy(
            lastReviewDate = LocalDateTime.now(),
            reviewCount = this.reviewCount + 1,
            updatedBy = reviewedBy
        )
    }

    /**
     * Add tags to metadata
     */
    fun addTags(tags: Set<String>): AdMetadata {
        return this.copy(
            tags = this.tags + tags
        )
    }

    /**
     * Remove tags from metadata
     */
    fun removeTags(tags: Set<String>): AdMetadata {
        return this.copy(
            tags = this.tags - tags
        )
    }

    /**
     * Add categories to metadata
     */
    fun addCategories(categories: Set<String>): AdMetadata {
        return this.copy(
            categories = this.categories + categories
        )
    }

    /**
     * Remove categories from metadata
     */
    fun removeCategories(categories: Set<String>): AdMetadata {
        return this.copy(
            categories = this.categories - categories
        )
    }

    /**
     * Add additional data
     */
    fun addAdditionalData(key: String, value: String): AdMetadata {
        return this.copy(
            additionalData = this.additionalData + (key to value)
        )
    }

    /**
     * Remove additional data
     */
    fun removeAdditionalData(key: String): AdMetadata {
        return this.copy(
            additionalData = this.additionalData - key
        )
    }

    /**
     * Get metadata summary
     */
    fun getMetadataSummary(): Map<String, Any?> {
        return mapOf(
            "createdBy" to createdBy,
            "updatedBy" to updatedBy,
            "approvedBy" to approvedBy,
            "rejectedBy" to rejectedBy,
            "priority" to priority,
            "sensitivity" to sensitivity,
            "tags" to tags,
            "categories" to categories,
            "isApproved" to isApproved(),
            "isRejected" to isRejected(),
            "needsReview" to needsReview(),
            "version" to version,
            "reviewCount" to reviewCount,
            "lastReviewDate" to lastReviewDate
        )
    }

    /**
     * Validate metadata
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        notes?.let {
            if (it.length > 1000) {
                errors.add("Notes cannot exceed 1000 characters")
            }
        }

        tags.forEach { tag ->
            if (tag.isBlank()) {
                errors.add("Tags cannot be blank")
            }
            if (tag.length > 50) {
                errors.add("Tag '$tag' cannot exceed 50 characters")
            }
        }

        categories.forEach { category ->
            if (category.isBlank()) {
                errors.add("Categories cannot be blank")
            }
            if (category.length > 50) {
                errors.add("Category '$category' cannot exceed 50 characters")
            }
        }

        additionalData.forEach { (key, value) ->
            if (key.isBlank()) {
                errors.add("Additional data keys cannot be blank")
            }
            if (value.length > 500) {
                errors.add("Additional data value for key '$key' cannot exceed 500 characters")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Metadata is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    companion object {
        fun empty(): AdMetadata {
            return AdMetadata()
        }

        fun create(createdBy: String): AdMetadata {
            return AdMetadata(createdBy = createdBy)
        }
    }
}

/**
 * Priority levels for advertisements
 */
enum class Priority(val displayName: String, val level: Int) {
    LOW("Low", 1),
    NORMAL("Normal", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4);

    fun isHigherThan(other: Priority): Boolean {
        return this.level > other.level
    }

    fun isLowerThan(other: Priority): Boolean {
        return this.level < other.level
    }
}

/**
 * Sensitivity levels for advertisements
 */
enum class Sensitivity(val displayName: String, val requiresApproval: Boolean) {
    LOW("Low", false),
    NORMAL("Normal", false),
    HIGH("High", true),
    RESTRICTED("Restricted", true);

    fun requiresSpecialApproval(): Boolean {
        return this == HIGH || this == RESTRICTED
    }
}