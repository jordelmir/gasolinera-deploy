package com.gasolinerajsm.adengine.domain.valueobject

import com.gasolinerajsm.adengine.domain.model.AdType
import java.time.LocalDateTime

/**
 * Value object representing advertisement content
 */
data class AdContent(
    val title: String,
    val description: String? = null,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val callToAction: String? = null,
    val targetUrl: String? = null,
    val durationSeconds: Int? = null,
    val fileSizeBytes: Long? = null,
    val contentType: String? = null,
    val language: String = "es",
    val tags: Set<String> = emptySet(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {

    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(title.length <= 200) { "Title cannot exceed 200 characters" }
        description?.let {
            require(it.length <= 1000) { "Description cannot exceed 1000 characters" }
        }
        mediaUrl?.let {
            require(it.length <= 2000) { "Media URL cannot exceed 2000 characters" }
        }
        thumbnailUrl?.let {
            require(it.length <= 2000) { "Thumbnail URL cannot exceed 2000 characters" }
        }
        targetUrl?.let {
            require(it.length <= 2000) { "Target URL cannot exceed 2000 characters" }
        }
        durationSeconds?.let {
            require(it > 0) { "Duration must be positive" }
        }
        fileSizeBytes?.let {
            require(it > 0) { "File size must be positive" }
        }
        require(language.length == 2) { "Language must be ISO 639-1 format" }
    }

    /**
     * Validate content for specific ad type
     */
    fun validateForAdType(adType: AdType): ValidationResult {
        val errors = mutableListOf<String>()

        when (adType) {
            AdType.VIDEO, AdType.REWARDED_VIDEO -> {
                if (mediaUrl == null) {
                    errors.add("Video ads must have media URL")
                }
                if (durationSeconds == null) {
                    errors.add("Video ads must have duration")
                }
                if (thumbnailUrl == null) {
                    errors.add("Video ads must have thumbnail")
                }
            }
            AdType.AUDIO -> {
                if (mediaUrl == null) {
                    errors.add("Audio ads must have media URL")
                }
                if (durationSeconds == null) {
                    errors.add("Audio ads must have duration")
                }
            }
            AdType.BANNER, AdType.INTERSTITIAL -> {
                if (mediaUrl == null && targetUrl == null) {
                    errors.add("Banner/Interstitial ads must have media URL or target URL")
                }
            }
            else -> {
                // Other types have more flexible requirements
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Content is valid for $adType")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Check if content is expired
     */
    fun isExpired(): Boolean {
        // Content could have expiration logic here
        return false
    }

    /**
     * Get content metadata
     */
    fun getMetadata(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "mediaUrl" to mediaUrl,
            "thumbnailUrl" to thumbnailUrl,
            "durationSeconds" to durationSeconds,
            "fileSizeBytes" to fileSizeBytes,
            "contentType" to contentType,
            "language" to language,
            "tags" to tags,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun empty(): AdContent {
            return AdContent(
                title = "",
                language = "es"
            )
        }
    }
}

/**
 * Validation result for content operations
 */
data class ValidationResult(
    val isSuccess: Boolean,
    val message: String
) {
    companion object {
        fun success(message: String) = ValidationResult(true, message)
        fun failure(message: String) = ValidationResult(false, message)
    }
}